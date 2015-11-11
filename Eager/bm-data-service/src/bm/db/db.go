// Package db encapsulates the interfaces and functions necessary for
// loading and querying existing time series data.
package db

import (
	"bm/bmutil"
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"path"
	"strconv"
	"strings"
	"time"
)

// Datapoint represents an individual member of a time series.
type Datapoint struct {
	Timestamp int64
	Value     int
	Cwrong    int
}

// TimeSeries represents a sequence of time-ordered data
type TimeSeries []Datapoint

// Database interface defines what operations/queries should be supported
// by a database implementation.
type Database interface {
	Query(n int, ops []string, start, end int64) (map[string]TimeSeries, error)
}

// FSDatabase implements the Database interface using a set of data files
// located in the local file system.
type FSDatabase struct {
	data map[string]TimeSeries
}

// NewFSDatabase creates a new FSDatabase instance by loading the
// required time serties data from the specified directory in the file
// system.
func NewFSDatabase(root string) (*FSDatabase, error) {
	files, err := ioutil.ReadDir(root)
	if err != nil {
		return nil, err
	}

	fsd := &FSDatabase{
		data: make(map[string]TimeSeries),
	}
	for _, f := range files {
		if !f.IsDir() && strings.HasPrefix(f.Name(), "benchmark_") && strings.HasSuffix(f.Name(), ".txt") {
			name, ts, err := loadFile(root, f.Name())
			if err != nil {
				return nil, err
			}
			fsd.data[name] = ts
		}
	}
	return fsd, nil
}

// Query returns a set of TimeSeries instances as a map, keyed by the
// operation names. If n > 0, maximum length of each TimeSeries will be
// limited to n.
func (fsd *FSDatabase) Query(n int, ops []string, start, end int64) (map[string]TimeSeries, error) {
	result := make(map[string]TimeSeries)
	for _, op := range ops {
		ts, ok := fsd.data[op]
		if !ok {
			return nil, fmt.Errorf("no data available for %s", op)
		}
		if n > 0 && n <= len(ts) {
			result[op] = ts[len(ts)-n:]
		} else {
			result[op] = ts
		}
	}
	return result, nil
}

func loadFile(root, child string) (string, TimeSeries, error) {
	fmt.Println("Loading data from", child)
	lines, err := bmutil.ReadLines(path.Join(root, child))
	if err != nil {
		return "", nil, err
	}

	name := ""
	var ts TimeSeries
	for _, line := range lines {
		if name == "" {
			name = line
		} else {
			fields := strings.Fields(line)
			t, err := strconv.ParseInt(fields[0], 10, 64)
			if err != nil {
				return "", nil, err
			}
			val, err := strconv.Atoi(fields[1])
			if err != nil {
				return "", nil, err
			}
			ts = append(ts, Datapoint{
				Timestamp: t,
				Value:     val,
			})
		}
	}
	return name, ts, nil
}

// AEDatabase implements the Database interface, using a remote
// watchtower service hosted in the AppEngine cloud.
type AEDatabase struct {
	BaseURL string
}

// Query returns a set of TimeSeries instances as a map, keyed by the
// operation names. If n > 0, maximum length of each TimeSeries will be
// limited to n.
func (aed *AEDatabase) Query(n int, ops []string, start, end int64) (map[string]TimeSeries, error) {
	url := fmt.Sprintf("%s/query?ops=%s", aed.BaseURL, strings.Join(ops, ","))
	if start != -1 {
		url += fmt.Sprintf("&start=%d", start)
	}
	if end != -1 {
		url += fmt.Sprintf("&end=%d", end)
	}
	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	type queryResult struct {
		Timestamps    []int64
		BenchmarkData map[string][]int
	}
	var qr queryResult
	if err := json.Unmarshal(body, &qr); err != nil {
		return nil, err
	}

	result := make(map[string]TimeSeries)
	for op, values := range qr.BenchmarkData {
		var ts TimeSeries
		for index, value := range values {
			ts = append(ts, Datapoint{
				Timestamp: qr.Timestamps[index],
				Value:     value,
			})
		}
		result[op] = ts
	}
	return result, nil
}

type ElasticSearchDatabase struct {
	BaseURL string
	Index string
	Type string
	HistoryDays int
}

type searchResult struct {
	ScrollID string `json:"_scroll_id"`
	Took int `json:"took"`
	TimedOut bool `json:"timed_out"`
	Shards struct {
		Total int `json:"total"`
		Successful int `json:"successful"`
		Failed int `json:"failed"`
	} `json:"_shards"`
	Hits struct {
		Total int `json:"total"`
		MaxScore float64 `json:"max_score"`
		Hits []struct {
			Index string `json:"_index"`
			Type string `json:"_type"`
			ID string `json:"_id"`
			Score float64 `json:"_score"`
			Source struct {
				Timestamp int64 `json:"timestamp"`
				Values map[string]int
			} `json:"_source"`
		} `json:"hits"`
	} `json:"hits"`
}

type IndicesQuery struct {
	Query struct {
		Indices struct {
			Indices []string `json:"indices"`
			Query interface{} `json:"query"`
		} `json:"indices"`
	} `json:"query"`
	Sort []map[string]interface{} `json:"sort"`
	Filter struct {
		Type struct {
			Value string `json:"value"`
		} `json:"type"`
	} `json:"filter"`
}

type MatchAllQuery struct {
	Match_all map[string]string `json:"match_all"`
}

type RangeQuery struct {
	Range struct {
		Timestamp map[string]int64 `json:"timestamp"`
	} `json:"range"`
}

type searchContext struct {
	Total int
	Current int
	ScrollID string
}

func (es *ElasticSearchDatabase) Query(n int, ops []string, start, end int64) (map[string]TimeSeries, error) {
	var queryObj IndicesQuery
	queryObj.Sort = append(queryObj.Sort, map[string]interface{} {
		"timestamp": map[string]string {
			"order": "asc",
		},
	})
	queryObj.Filter.Type.Value = es.Type
	if es.HistoryDays > 0 {
		indexPrefix := strings.TrimRight(es.Index, "*")
		now := time.Now()
		then := now.Add(-time.Hour * time.Duration(24 * es.HistoryDays))
		//thenTimestamp := then.UnixNano() / 1000000
		thenYear := then.Year()
		thenMonth := then.Month()
		var indices []string
		for {
			indices = append(indices, fmt.Sprintf("%s_%d-%02d", indexPrefix, thenYear, thenMonth))
			if now.Year() == thenYear && now.Month() == thenMonth {
				break
			}
			if thenMonth < 12 {
				thenMonth++
			} else {
				thenMonth = 1
				thenYear++
			}
		}
		queryObj.Query.Indices.Indices = indices
	} else {
		queryObj.Query.Indices.Indices = []string{es.Index}
	}

	queryObj.Query.Indices.Query = getQuery(start, end)

	queryStringBytes, err := json.Marshal(queryObj)
	if err != nil {
		return nil, err
	}
	queryString := string(queryStringBytes)
	fmt.Println(queryString)
	url := fmt.Sprintf("%s/_search?scroll=1m&size=1000", es.BaseURL)
	result := make(map[string]TimeSeries)

	context, err := queryElasticSearch(url, queryString, ops, result)
	if err != nil {
		return nil, err
	}

	count := context.Current
	for count < context.Total {
		url = fmt.Sprintf("%s/_search/scroll?scroll=1m&size=1000", es.BaseURL)
		queryString = context.ScrollID
		context, err = queryElasticSearch(url, queryString, ops, result)
		if err != nil {
			return nil, err
		}
		count += context.Current
	}
	return result, nil
}

func getQuery(start, end int64) interface{} {
	if start != -1 || end != -1 {
		var rq RangeQuery
		rq.Range.Timestamp = make(map[string]int64)
		if start != -1 {
			rq.Range.Timestamp["gte"] = start
		}
		if end != -1 {
			rq.Range.Timestamp["lte"] = end
		}
		return rq
	}
	return MatchAllQuery{Match_all: make(map[string]string)}
}

func queryElasticSearch(url string, queryString string, ops []string, result map[string]TimeSeries) (searchContext,error) {
	var context searchContext
	resp, err := http.Post(url, "application/json", bytes.NewBufferString(queryString))
	if err != nil {
		return context, err
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return context, err
	}
	var sr searchResult
	if err := json.Unmarshal(body, &sr); err != nil {
		return context, err
	}
	context.Total = sr.Hits.Total
	context.Current = len(sr.Hits.Hits)
	context.ScrollID = sr.ScrollID
	last := int64(-1)
	for _, hit := range sr.Hits.Hits {
		timestamp := hit.Source.Timestamp
		if timestamp <= last {
			panic("Out of order results")
		}
		values := hit.Source.Values
		for _, op := range ops {
			p := Datapoint{Timestamp: timestamp, Value: values[op]}
			result[op] = append(result[op], p)
		}
		last = timestamp
	}
	return context, nil
}
