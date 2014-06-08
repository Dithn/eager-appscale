package main

import (
	"fmt"
	"os"
	"bufio"
	"io"
	"strings"
)

func main() {
	inputFile, err := os.Open("uniq_gcalls.txt")
	if err != nil {
		fmt.Println("Failed to open the input file", err)
		return
	}
	defer inputFile.Close()

	bf := bufio.NewReader(inputFile)
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
		segments := strings.Split(string(line), " ")
		fmt.Println("\"" + segments[1] + "\",")
	}
}
