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

type customPredictionReq struct {
	Data                 db.TimeSeries
	Quantile, Confidence float64
	Name string
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

func getCustomTimeSeriesPredictionHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		cpr := customPredictionReq{
			Quantile:   0.95,
			Confidence: 0.05,
			Name: "Unknown",
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
	url := flag.String("u", "", "URL of the Watchtower service")
	port := flag.Int("p", 8080, "Port of the data service")
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
	http.HandleFunc("/cpredict", getCustomTimeSeriesPredictionHandler())
	http.HandleFunc("/ts", getTimeSeriesHandler(d))
	http.ListenAndServe(fmt.Sprintf(":%d", *port), nil)
}
