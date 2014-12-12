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

	tsd, err := getTSData(*path)
	if err != nil {
		fmt.Println(err)
		return
	}

	for index := range tsd.APICallTime {
		fmt.Println("[timeseries]", tsd.APICallTime[index], tsd.OtherTime[index], tsd.APICallTime[index]+tsd.OtherTime[index])
	}
}

type tsData struct {
	APICallTime, OtherTime []int
}

func getTSData(path string) (*tsData,error) {
	lines, err := bmutil.ReadLines(path)
	if err != nil {
		return nil, err
	}

	result := &tsData{}
	for _, line := range lines {
		segments := strings.Fields(line)
		apiCallTime, err := intValue(segments[2])
		if err != nil {
			return nil, err
		}

		otherTime, err := intValue(segments[5])
		if err != nil {
			return nil, err
		}
		result.APICallTime = append(result.APICallTime, apiCallTime)
		result.OtherTime = append(result.OtherTime, otherTime)
	}
	return result, nil
}

func intValue(s string) (int,error) {
	val, err := strconv.Atoi(strings.TrimRight(s, ",}"))
	if err != nil {
		return -1, err
	}
	return val, nil
}
