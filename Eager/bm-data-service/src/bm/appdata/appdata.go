package main

import (
	"bm/bmutil"
	"bm/db"
	"bm/qbets"
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
	for _, line := range lines {
		segments := strings.Fields(line)
		val, err := strconv.ParseFloat(segments[1], 64)
		if err != nil {
			fmt.Println(err)
			return
		}
		ts = append(ts, int(val*1000.0))
	}
	fmt.Println("Loaded", len(ts), "data points from file")

	if *tr {
		analyzeTrace(ts, *q, *c)
		fmt.Println()
	}

	pred, err := qbets.PredictQuantile(ts, *q, *c, false)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Actual Quantile (QBETS):", pred)

	sort.Ints(ts)
	index := int(float64(len(ts)) * (*q))
	fmt.Println("Actual Quantile: ", ts[index-1], "(element =", index, ")")
}

func analyzeTrace(ts []int, q, c float64) {
	minIndex := int(math.Log(c)/math.Log(q)) + 10
	fmt.Println("Min index: ", minIndex)
	if len(ts) < minIndex+1 {
		fmt.Println("insufficient data in time series")
		return
	}

	results, err := qbets.PredictQuantileTrace(ts, q, c, false)
	if err != nil {
		fmt.Println(err)
		return
	}

	startIndex := len(ts) - len(results)

	for i := 0; i < len(results); i++ {
		fmt.Printf("[trace] %d %d %d\n", i+startIndex, int(results[i]), ts[i+startIndex])
	}
}
