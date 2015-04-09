package main

import (
	"bm/db"
	"bm/analysis"
	"flag"
	"fmt"
)

func main() {
	url := flag.String("u", "", "URL of the Watchtower service")
	q := flag.Float64("q", 0.95, "Quantile to predict")
	c := flag.Float64("c", 0.05, "Upper confidence of the prediction")
	op := flag.String("o", "", "Operation for which the quantile should be predicted")
	l := flag.Int("l", -1, "Length of the time series to process")
	t := flag.Bool("t", false, "Dump the time series data without making predictions")
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

	if *t {
		if err := dump(d, *op, *l); err != nil {
			fmt.Println(err)
		}
	} else {
		p, err := getQuantile(d, *q, *c, *op, *l, analysis.DefaultQbetsPredictor())
		if err != nil {
			fmt.Println(err)
			return
		}
		fmt.Printf("Predicted %.2f Quantile: %d\n", *q, p)
	}
}

func dump(d db.Database, op string, l int) error {
	result, err := d.Query(-1, []string{op}, -1, -1)
	if err != nil {
		return err
	}

	ts := result[op]
	if l > 0 {
		ts = ts[0:l]
	}
	for _,v := range ts {
		fmt.Println(v.Timestamp, v.Value)
	}
	return nil
}

func getQuantile(d db.Database, q, c float64, op string, l int, pred analysis.Predictor) (int,error) {
	result, err := d.Query(-1, []string{op}, -1, -1)
	if err != nil {
		return -1, err
	}

	ts := result[op]
	if l > 0 {
		ts = ts[0:l]
	}
	return pred.PredictQuantile(ts, q, c, true)
}
