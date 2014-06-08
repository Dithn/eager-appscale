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
		fmt.Println("Usage: histo <path>")
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

		key, err := strconv.Atoi(string(line))
		if err != nil {
			fmt.Println("Error while parsing string", err)
			return
		}
		data[key] = data[key] + 1
	}

	var keys []int
	for k := range data {
		keys = append(keys, k)
	}
	sort.Ints(keys)

	for _, k := range keys {
		fmt.Println(k, data[k])
	}
}
