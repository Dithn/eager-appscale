package main

import (
	"bm/db"
	"bm/qbets"
	"flag"
	"fmt"
)

func main() {
	url := flag.String("u", "", "URL of the Watchtower service")
	q := flag.Float64("q", 0.95, "Quantile to predict")
	c := flag.Float64("c", 0.05, "Upper confidence of the prediction")
	op := flag.String("o", "", "Operation for which the quantile should be predicted")
	l := flag.Int("l", -1, "Length of the time series to process")
	flag.Parse()
	if *url == "" {
		fmt.Println("URL of the Watchtower service not specified.")
		return
	}
	if *op == "" {
		fmt.Println("Operation name not specified.")
		return
	}

	d := &db.AEDatabase{
		BaseURL: *url,
	}
	fmt.Println("Loading TimeSeries data from the Watchtower service at", d.BaseURL)

	p, err := getQuantile(d, *q, *c, *op, *l)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Printf("Predicted %.2f Quantile: %d\n", *q, p)
}

func getQuantile(d db.Database, q, c float64, op string, l int) (int,error) {
	result, err := d.Query(-1, []string{op}, -1, -1)
	if err != nil {
		return -1, err
	}

	ts := result[op]
	if l > 0 {
		ts = ts[0:l]
	}
	return qbets.PredictQuantile(ts, q, c, true)
}
