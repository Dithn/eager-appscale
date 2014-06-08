package main

import (
	"os"
	"fmt"
	"bufio"
	"io"
	"strconv"
	"strings"
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

	for {
		line, err := bf.ReadBytes('\n')
                if err == io.EOF {
			break
		} else if err != nil {
			fmt.Println("Error while reading line", err)
			return
		} 

		segments := strings.Split(string(line), " ")
		lastValue := -1
		for i, v := range segments {
			if i == 0 || i == len(segments) - 1 {
				continue
			}
			currentValue, err := strconv.Atoi(v)
			if err != nil {
				fmt.Println("Error while parsing string", err)
				return
			}
			if lastValue == -1 {
				lastValue = currentValue
			} else if lastValue != currentValue {
				fmt.Print(string(line))
				break
			}
		}
	}
}
