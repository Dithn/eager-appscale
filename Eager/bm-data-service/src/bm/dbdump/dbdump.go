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

	result, err := d.Query(-1, []string{*op})
	if err != nil {
		fmt.Println(err)
		return
	}

	p, err := qbets.PredictQuantile(result[*op], *q, *c, true)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Printf("Predicted %.2f Quantile: %f\n", *q, p)
}
