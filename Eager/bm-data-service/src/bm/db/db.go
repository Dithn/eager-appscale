// Package db encapsulates the interfaces and functions necessary for
// loading and querying existing time series data.
package db

import (
	"bm/bmutil"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"path"
	"strconv"
	"strings"
)

// TimeSeries represents a sequence of time-ordered data
type TimeSeries []int

// Database interface defines what operations/queries should be supported
// by a database implementation.
type Database interface {
	Query(n int, ops []string) (map[string]TimeSeries, error)
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
func (fsd *FSDatabase) Query(n int, ops []string) (map[string]TimeSeries, error) {
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
			val, err := strconv.Atoi(line)
			if err != nil {
				return "", nil, err
			}
			ts = append(ts, val)
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
func (aed *AEDatabase) Query(n int, ops []string) (map[string]TimeSeries, error) {
	url := fmt.Sprintf("%s/query?ops=%s", aed.BaseURL, strings.Join(ops, ","))
	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	type queryResult map[string][]int
	var qr queryResult
	if err := json.Unmarshal(body, &qr); err != nil {
		return nil, err
	}

	result := make(map[string]TimeSeries)
	for k, v := range qr {
		if n > 0 && n <= len(v) {
			result[k] = TimeSeries(v[len(v)-n:])
		} else {
			result[k] = TimeSeries(v)
		}
	}
	return result, nil
}
