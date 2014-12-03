package main 

import (
	"bm/db"
	"bm/qbets"
	"bufio"
	"flag"
	"fmt"
	"math"
	"os"
	"sort"
	"strconv"
	"strings"
	"sync"
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
	fmt.Println("Actual Quantile: ", ts[index - 1], "(element =", index, ")")
}

func analyzeTrace(ts []int, q, c float64) {
	minIndex := int(math.Log(c) / math.Log(q)) + 10
	fmt.Println("Min index: ", minIndex)
	if len(ts) < minIndex + 1 {
		fmt.Println("insufficient data in time series")
		return
	}

	results := make([]int, len(ts) - minIndex, len(ts) - minIndex)
	var perr error
	workers := make(chan bool, 8)
	var wg sync.WaitGroup

	for i := minIndex; i < len(ts); i++ {
		workers <- true
		wg.Add(1)
		go func(cnt int) {
			defer func(){ <- workers }()
			defer wg.Done()
			if perr != nil {
				return
			}
			data := ts[0:cnt+1]
			pred, err := qbets.PredictQuantile(data, q, c, false)
			if err != nil {
				perr = err
				return
			}
			results[cnt - minIndex] = int(pred)
			if cnt % 100 == 0 {
				fmt.Println("Computed the predictions for index:", cnt)
			}
		}(i)
	}
	wg.Wait()

	if perr != nil {
		fmt.Println(perr)
		return
	}
	for i := minIndex; i < len(results); i++ {
		fmt.Printf("[trace] %d %d %d\n", i, results[i], ts[i])
	}
}
