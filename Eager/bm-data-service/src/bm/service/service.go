package main

import (
	"encoding/json"
	"fmt"
	"bm/db"
	"bm/qbets"
	"net/http"
)

type timeSeriesReq struct {
	MaxLength int
	Operations []string
	Quantile, Confidence float64
}

func getTimeSeriesPredictionHandler(d db.Database) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		decoder := json.NewDecoder(r.Body)
		tsr := timeSeriesReq{
			MaxLength: 1000,
			Quantile: 0.95,
			Confidence: 0.05,
		}
		if err := decoder.Decode(&tsr); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		result := d.Query(tsr.MaxLength, tsr.Operations)
		predictions := make(map[string]int)
		for k, ts := range result {
			p, err := qbets.PredictQuantile(ts, tsr.Quantile, tsr.Confidence)
			if err != nil {
				http.Error(w, err.Error(), http.StatusInternalServerError)
				return
			}
			predictions[k] = p
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

		result := d.Query(tsr.MaxLength, tsr.Operations)
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
	fsd, err := db.NewFSDatabase("/Users/hiranya/Projects/eager/impl/eager-appscale/Eager/appscale-benchmark-app/latest_results")
	if err != nil {
		fmt.Println(err)
		return
	}

	http.HandleFunc("/predict", getTimeSeriesPredictionHandler(fsd))
	http.HandleFunc("/ts", getTimeSeriesHandler(fsd))
	http.ListenAndServe(":8080", nil)
}
