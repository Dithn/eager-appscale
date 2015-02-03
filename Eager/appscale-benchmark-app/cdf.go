package main

import (
	"os"
	"fmt"
	"bufio"
	"io"
	"strconv"
	"sort"
)

func main() {
	if len(os.Args) != 2 {
		fmt.Println("Usage: binned_histo <path>")
		return
	}

	inputFile, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Println("Error while opening input file", err)
		return
	}

	defer inputFile.Close()
	bf := bufio.NewReader(inputFile)
	na := 0
	var values []float64
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
		values = append(values, val)
	}

	sort.Float64s(values)
	lv := len(values)
	total := float64(lv + na)
	for i,v := range values {
		fmt.Println(v/3600.0, float64(i+1)/total)
	}

	last := (values[lv - 1] + 1.0)/3600.0
	for i := 0; i < na; i++ {
		fmt.Println(last, float64(i + lv + 1)/total)
	}
}
