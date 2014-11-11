package main 

import (
	"bm/db"
	"bm/qbets"
	"bufio"
	"flag"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
)

func main() {
	path := flag.String("p", "", "Path to the input file")
	q := flag.Float64("q", 0.95, "Quantile to predict")
	c := flag.Float64("c", 0.05, "Upper confidence of the prediction")

	flag.Parse()
	if *path == "" {
		fmt.Println("input file path not specified")
		return
	}
	
	file, err := os.Open(*path)
	if err != nil {
		fmt.Println(err)
		return
	}
	defer file.Close()

	var ts db.TimeSeries
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		segments := strings.Fields(line)
		val, err := strconv.ParseFloat(segments[1], 64)
		if err != nil {
			fmt.Println(err)
			return
		}
		ts = append(ts, int(val * 1000.0))
	}
	fmt.Println("Loaded", len(ts), "data points from file")

	if err := scanner.Err(); err != nil {
		fmt.Println(err)
		return
	}

	pred, err := qbets.PredictQuantile(ts, *q, *c)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Actual Quantile (QBETS):", pred)

	sort.Ints(ts)
	index := int(float64(len(ts)) * (*q))
	fmt.Println("Actual Quantile: ", ts[index - 1], "(element =", index, ")")
}
