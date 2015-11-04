package main

import (
	"bm/bmutil"
	"bm/db"
	"bm/analysis"
	"flag"
	"fmt"
	"math"
	"sort"
	"strconv"
	"strings"
)

func main() {
	path := flag.String("p", "", "Path to the input file")
	q := flag.Float64("q", 0.95, "Quantile to predict")
	c := flag.Float64("c", 0.05, "Upper confidence of the prediction")
	tr := flag.Bool("t", false, "Do a trace analysis")

	flag.Parse()
	if *path == "" {
		fmt.Println("input file path not specified")
		return
	}

	lines, err := bmutil.ReadLines(*path)
	if err != nil {
		fmt.Println(err)
		return
	}

	var ts db.TimeSeries
	var tsval []int
	for l_index, line := range lines {
		segments := strings.Fields(line)
		val, err := strconv.ParseFloat(segments[1], 64)
		if err != nil {
			fmt.Println(err)
			return
		}
		ts = append(ts, db.Datapoint{Timestamp: int64(l_index), Value:int(val*1000.0)})
		tsval = append(tsval, int(val*1000.0))
	}
	fmt.Println("Loaded", len(ts), "data points from file")

	pred := analysis.DefaultQbetsPredictor()
	if *tr {
		analyzeTrace(ts, *q, *c, pred)
		fmt.Println()
	}

	prediction, err := pred.PredictQuantile(ts, *q, *c, false)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Actual Quantile (QBETS):", prediction)

	sort.Ints(tsval)
	index := int(float64(len(tsval)) * (*q))
	fmt.Println("Actual Quantile: ", tsval[index-1], "(element =", index, ")")
}

func analyzeTrace(ts db.TimeSeries, q, c float64, pred analysis.Predictor) {
	minIndex := int(math.Log(c)/math.Log(q)) + 10
	fmt.Println("Min index: ", minIndex)
	if len(ts) < minIndex+1 {
		fmt.Println("insufficient data in time series")
		return
	}

	results, err := pred.PredictQuantileTrace(ts, q, c, false)
	if err != nil {
		fmt.Println(err)
		return
	}

	startIndex := len(ts) - len(results)

	for i := 0; i < len(results); i++ {
		fmt.Printf("[trace] %d %d %d\n", i+startIndex, int(results[i].Value), ts[i+startIndex])
	}
}
