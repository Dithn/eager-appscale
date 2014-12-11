package main

import (
	"bm/bmutil"
	"flag"
	"fmt"
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

	lines, err := bmutil.ReadLines(*path)
	if err != nil {
		fmt.Println(err)
		return
	}

	for _, line := range lines {
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
}

func intValue(s string) (int,error) {
	val, err := strconv.Atoi(strings.TrimRight(s, ",}"))
	if err != nil {
		return -1, err
	}
	return val, nil
}
