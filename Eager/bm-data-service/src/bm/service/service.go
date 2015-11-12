package main

import (
	"bm/analysis"
	"bm/db"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
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

func getTimeSeriesPredictionHandler(d db.Database, pred analysis.Predictor, debug bool) http.HandlerFunc {
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
				p, err := pred.PredictQuantile(data, tsr.Quantile, tsr.Confidence, debug)
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

func getCustomTimeSeriesPredictionHandler(pred analysis.Predictor, debug bool) http.HandlerFunc {
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

		p, err := pred.PredictQuantileTrace(cpr.Data, cpr.Quantile, cpr.Confidence, debug)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		fmt.Printf("TracePrediction [%s] (q = %f, c = %f) => %d quantiles (%d data points)\n", cpr.Name, cpr.Quantile, cpr.Confidence, len(p), len(cpr.Data))
		predictions := map[string]db.TimeSeries{
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
	qbetsPath := flag.String("q", "", "Path to QBETS executable")
	simplePred := flag.Bool("s", false, "Use simple predictor instead of QBETS")
	debug := flag.Bool("v", false, "Enable verbose (debug) mode")

	esIndex := flag.String("i", "watchtower-prod*", "Name of the ElasticSearch index")
	esType := flag.String("t", "appengine", "Name of the ElasticSearch type")
	esHistory := flag.Int("hd", -1, "ElasticSearch history to search (in days)")

	flag.Parse()
	if *url == "" {
		fmt.Println("URL/Path of the data files not specified.")
		return
	}
	var d db.Database
	var err error
	if *dbType == "ae" {
		fmt.Println("Using AppEngine database")
		d = &db.AEDatabase{
			BaseURL: *url,
		}
	} else if *dbType == "file" {
		fmt.Println("Using file system database")
		d, err = db.NewFSDatabase(*url)
		if err != nil {
			fmt.Println(err)
			return
		}
	} else if *dbType == "es" {
		fmt.Println("Using ElasticSearch database")
		d = &db.ElasticSearchDatabase {
			BaseURL: *url,
			Index: *esIndex,
			Type: *esType,
			HistoryDays: *esHistory,
		}
	} else {
		fmt.Println("Invalid database type:", *dbType)
		return
	}
	fmt.Println("Loading TimeSeries data from", *url)

	var pred analysis.Predictor
	if *simplePred {
		fmt.Println("Using simple predictor (no QBETS)")
		pred = &analysis.SimplePredictor{}
	} else {
		if *qbetsPath == "" {
			pred = analysis.DefaultQbetsPredictor()
		} else {
			pred = &analysis.QbetsPredictor{QbetsPath: *qbetsPath}
		}
		fmt.Printf("Using QBETS-based predictor [exec: %s]\n", pred.(*analysis.QbetsPredictor).QbetsPath)
	}

	http.HandleFunc("/predict", getTimeSeriesPredictionHandler(d, pred, *debug))
	http.HandleFunc("/cpredict", getCustomTimeSeriesPredictionHandler(pred, *debug))
	http.HandleFunc("/ts", getTimeSeriesHandler(d))
	http.ListenAndServe(fmt.Sprintf("127.0.0.1:%d", *port), nil)
}
