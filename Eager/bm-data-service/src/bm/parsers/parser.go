package main

import (
	"bufio"
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
)

func main() {
	path := flag.String("p", "", "Path to the input file")
	
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

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		segments := strings.Fields(line)
		apiCallTime, err := intValue(segments[2])
		if err != nil {
			fmt.Println(err)
			return
		}

		otherTime, err := intValue(segments[5])
		if err != nil {
			fmt.Println(err)
			return
		}
		fmt.Println("[timeseries]", apiCallTime, otherTime)
	}

	if err := scanner.Err(); err != nil {
		fmt.Println(err)
		return
	}
}

func intValue(s string) (int,error) {
	val, err := strconv.Atoi(strings.TrimRight(s, ",}"))
	if err != nil {
		return -1, err
	}
	return val, nil
}
