package main

import (
	"bm/db"
	"bm/qbets"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
	"strconv"
	"sync"
)

type timeSeriesReq struct {
	MaxLength            int
	Operations           []string
	Start, End           int64
	Quantile, Confidence float64
}

type customPredictionReq struct {
	Data                 db.TimeSeries
	Quantile, Confidence float64
	Name                 string
}

func getTimeSeriesPredictionHandler(d db.Database) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		tsr := timeSeriesReq{
			Start:      -1,
			End:        -1,
			Quantile:   0.95,
			Confidence: 0.05,
		}
		if err := decoder.Decode(&tsr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result, err := d.Query(tsr.MaxLength, tsr.Operations, tsr.Start, tsr.End)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		predictions := make(map[string]int)
		c := make(chan bool, 4)
		var wg sync.WaitGroup
		var perr error
		for k, ts := range result {
			c <- true
			wg.Add(1)
			go func(key string, data db.TimeSeries) {
				defer func() { <-c }()
				defer wg.Done()
				p, err := qbets.PredictQuantile(data, tsr.Quantile, tsr.Confidence, false)
				if err != nil {
					perr = err
					return
				}
				fmt.Printf("%s (q = %f, c = %f) => %d (%d data points)\n", key, tsr.Quantile, tsr.Confidence, p, len(data))
				predictions[key] = p
			}(k, ts)
		}
		wg.Wait()

		if perr != nil {
			http.Error(w, perr.Error(), http.StatusInternalServerError)
			return
		}

		jsonString, err := json.Marshal(predictions)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write(jsonString)
	}
}

func getTimeSeriesHandler(d db.Database) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		tsr := timeSeriesReq{
			Start: -1,
			End:   -1,
		}
		if err := decoder.Decode(&tsr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result, err := d.Query(tsr.MaxLength, tsr.Operations, tsr.Start, tsr.End)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		jsonString, err := json.Marshal(result)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write(jsonString)
	}
}

func getTimestampHandler(d db.Database) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		limitString := r.FormValue("limit")
		limit, err := strconv.ParseInt(limitString, 10, 64)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result, err := d.GetTimestamp(limit)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		jsonString, err := json.Marshal(*result)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write(jsonString)
	}
}

func getCustomTimeSeriesPredictionHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		cpr := customPredictionReq{
			Quantile:   0.95,
			Confidence: 0.05,
			Name:       "Unknown",
		}
		if err := decoder.Decode(&cpr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		p, err := qbets.PredictQuantileTrace(cpr.Data, cpr.Quantile, cpr.Confidence, false)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		fmt.Printf("TracePrediction [%s] (q = %f, c = %f) => %d quantiles (%d data points)\n", cpr.Name, cpr.Quantile, cpr.Confidence, len(p), len(cpr.Data))
		predictions := map[string][]int{
			"Predictions": p,
		}

		jsonString, err := json.Marshal(predictions)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write(jsonString)
	}
}

func main() {
	url := flag.String("u", "", "URL/Path of the data files")
	dbType := flag.String("d", "ae", "Type of database to use")
	port := flag.Int("p", 8080, "Port of the data service")
	flag.Parse()
	if *url == "" {
		fmt.Println("URL/Path of the data files not specified.")
		return
	}
	var d db.Database
	var err error
	if *dbType == "ae" {
		d = &db.AEDatabase{
			BaseURL: *url,
		}
	} else if *dbType == "file" {
		d, err = db.NewFSDatabase(*url)
		if err != nil {
			fmt.Println(err)
			return
		}
	} else {
		fmt.Println("Invalid database type:", *dbType)
		return
	}
	fmt.Println("Loading TimeSeries data from", *url)

	http.HandleFunc("/predict", getTimeSeriesPredictionHandler(d))
	http.HandleFunc("/cpredict", getCustomTimeSeriesPredictionHandler())
	http.HandleFunc("/ts", getTimeSeriesHandler(d))
	http.HandleFunc("/tsinfo", getTimestampHandler(d))
	http.ListenAndServe(fmt.Sprintf("127.0.0.1:%d", *port), nil)
}
