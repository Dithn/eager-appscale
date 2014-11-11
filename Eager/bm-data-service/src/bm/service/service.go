package main

import (
	"bm/db"
	"bm/qbets"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
	"sync"
)

type timeSeriesReq struct {
	MaxLength            int
	Operations           []string
	Quantile, Confidence float64
}

func getTimeSeriesPredictionHandler(d db.Database) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		tsr := timeSeriesReq{
			Quantile:   0.95,
			Confidence: 0.05,
		}
		if err := decoder.Decode(&tsr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result, err := d.Query(tsr.MaxLength, tsr.Operations)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		predictions := make(map[string]float64)
		c := make(chan bool, 4)
		var wg sync.WaitGroup
		var perr error
		for k, ts := range result {
			c <- true
			wg.Add(1)
			go func(key string, data db.TimeSeries){
				defer func(){ <- c }()
				defer wg.Done()
				p, err := qbets.PredictQuantile(data, tsr.Quantile, tsr.Confidence)
				if err != nil {
					perr = err
					return
				}
				fmt.Printf("%s (q = %f, c = %f) => %f (%d data points)\n", key, tsr.Quantile, tsr.Confidence, p, len(data))
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
		var tsr timeSeriesReq
		if err := decoder.Decode(&tsr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result, err := d.Query(tsr.MaxLength, tsr.Operations)
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

func main() {
	/*d, err := db.NewFSDatabase("/Users/hiranya/Projects/eager/impl/eager-appscale/Eager/appscale-benchmark-app/latest_results")
	if err != nil {
		fmt.Println(err)
		return
	}*/

	url := flag.String("u", "", "URL of the Watchtower service")
	flag.Parse()
	if *url == "" {
		fmt.Println("URL of the Watchtower service not specified.")
		return
	}
	d := &db.AEDatabase{
		BaseURL: *url,
	}
	fmt.Println("Loading TimeSeries data from the Watchtower service at", d.BaseURL)

	http.HandleFunc("/predict", getTimeSeriesPredictionHandler(d))
	http.HandleFunc("/ts", getTimeSeriesHandler(d))
	http.ListenAndServe(":8080", nil)
}
