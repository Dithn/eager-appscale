package main

import (
	"os"
	"fmt"
	"bufio"
	"io"
	"math"
	"strconv"
	"sort"
)

func main() {
	if len(os.Args) != 2 {
		fmt.Println("Usage: binned_histo <path>")
		return
	}
	fmt.Println("Reading input from:", os.Args[1])

	inputFile, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Println("Error while opening input file", err)
		return
	}

	defer inputFile.Close()
	bf := bufio.NewReader(inputFile)
	data := make(map[int]int)
	na := 0
	min := math.MaxFloat64
	max := -1.0
	for {
		line, isPrefix, err := bf.ReadLine()
                if err == io.EOF {
			break
		} else if err != nil {
			fmt.Println("Error while reading line", err)
			return
		} else if isPrefix {
			fmt.Println("Unexpected long line", err)
			return
		}

		if string(line) == "N/A" {
			na++
			continue
		}

		val, err := strconv.ParseFloat(string(line), 64)
		if err != nil {
			fmt.Println("Error while parsing string", err)
			return
		}

		if val > max {
			max = val
		}
		if val < min {
			min = val
		}

		key := int(val/3600.0)
		data[key] = data[key] + 1
	}

	var keys []int
	for k := range data {
		keys = append(keys, k)
	}
	sort.Ints(keys)

	for _, k := range keys {
		fmt.Printf("%dh-%dh %d\n", k, (k+1), data[k])
	}
	fmt.Println("NotFound", na)
	fmt.Println()
	fmt.Println("Min:", min)
	fmt.Println("Max:", max)
}
