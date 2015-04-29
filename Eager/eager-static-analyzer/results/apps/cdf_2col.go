package main

import (
	"bufio"
	"fmt"
	"os"
	"sort"
	"strings"
	"strconv"
)

func main() {
	if len(os.Args) != 2 {
		fmt.Println("Usage: cdf_2col <path>")
		return
	}

	inputFile, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Println("Error while opening input file", err)
		return
	}

	defer inputFile.Close()
	scanner := bufio.NewScanner(inputFile)
	data := make(map[float64]int)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		var key float64
		if fields[0] == "---" {
			key = 0.0
		} else {
			key, err = strconv.ParseFloat(fields[0], 64)
			if err != nil {
				fmt.Println("Error while parsing string", err)
				return
			}
		}

		val, err := strconv.Atoi(fields[1])
		if err != nil {
			fmt.Println("Error while parsing string", err)
			return
		}
		data[key] += val
	}
	if err := scanner.Err(); err != nil {
		fmt.Println("Error while reading from input file", err)
		return
	}

	var keys []float64
	total := 0
	for k, v := range data {
		keys = append(keys, k)
		total += v
	}
	sort.Float64s(keys)
	
	cumulative := 0
	for _,k := range keys {
		cumulative += data[k]
		fmt.Printf("%.4f %.4f\n", k, float64(cumulative)/float64(total))
	}
}
