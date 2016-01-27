package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"strings"
)

func analyzeMethod(bf *bufio.Reader) {
	paths := make(map[string][]string)
	for {
		line, err := bf.ReadString('\n')
		if err == io.EOF {
			return
		}
		if err != nil {
			fmt.Println("Error while parsing line", err)
			return
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
			for k,v := range pathCounts {
				fmt.Println("[path]", k, v)
			}
			fmt.Println()
			return
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
	for {
		line, err := bf.ReadString('\n')
		if err == io.EOF {
			return
		}
		if err != nil {
			fmt.Println("Error while parsing line", err)
			return
		} else if strings.HasPrefix(line, "----------------- PROJECT: ") {
			fmt.Print("\n", line)
		} else if strings.HasPrefix(line, "Analyzing: ") {
			fmt.Print(line)
			analyzeMethod(bf)
		}
	}
}
