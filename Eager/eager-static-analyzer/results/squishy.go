package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"sort"
	"strings"
)

func analyzeMethod(bf *bufio.Reader) int {
	paths := make(map[string][]string)
	for {
		line, err := bf.ReadString('\n')
		if err == io.EOF {
			return -1
		}
		if err != nil {
			fmt.Println("Error while parsing line", err)
			return -1
		}
		if strings.HasPrefix(line, "  [path") {
			segments := strings.Fields(line)
			paths[segments[0]] = append(paths[segments[0]], segments[1])
		} else if strings.TrimSpace(line) == "" {
			fmt.Println("Distinct paths:", len(paths))
			sdkPaths := make(map[string]string)
			for k,v := range paths {
				if v[0] != "--" {
					sdkPaths[k] = strings.Join(v, "->")
				}
			}
			fmt.Println("Paths with API calls:", len(sdkPaths))
			pathCounts := make(map[string]int)
			for _,v := range sdkPaths {
				pathCounts[v] += 1
			}
			maxValue := 0
			for k,v := range pathCounts {
				fmt.Println("[path]", k, v)
				if v > maxValue {
					maxValue = v
				}
			}
			fmt.Println()
			return maxValue
		}
	}
}

func main() {
	if len(os.Args) != 2 {
		fmt.Println("Usage: squishy <path>")
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
	var maxValues []int
	for {
		line, err := bf.ReadString('\n')
		if err == io.EOF {
			return
		}
		if err != nil {
			fmt.Println("Error while parsing line", err)
			return
		} else if strings.HasPrefix(line, "----------------- PROJECT: ") {
			if len(maxValues) > 0 {
				sort.Ints(maxValues)
				fmt.Println("[largest]", maxValues[len(maxValues) - 1])
				maxValues = maxValues[:0]
			}
			fmt.Print("\n", line)
		} else if strings.HasPrefix(line, "Analyzing: ") {
			fmt.Print(line)
			maxValues = append(maxValues, analyzeMethod(bf))
		}
	}
}
