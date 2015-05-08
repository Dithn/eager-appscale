package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"sort"
	"strconv"
	"strings"
)

type WatchtowerData map[string]map[string]int

type Int64Slice []int64
func (a Int64Slice) Len() int { return len(a) }
func (a Int64Slice) Swap(i, j int) { a[i], a[j] = a[j], a[i] }
func (a Int64Slice) Less(i, j int) bool { return a[i] < a[j] }

func loadJson(filePath string) (WatchtowerData, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	decoder := json.NewDecoder(file)
	var result WatchtowerData
	if err = decoder.Decode(&result); err != nil {
		return nil, err
	}
	return result, nil
}

func writeOutput(all map[int64]map[string]int, timestamps Int64Slice, op, filePath string) error {
	output, err := os.Create(filePath)
	if err != nil {
		return err
	}
	defer output.Close()

	w := bufio.NewWriter(output)
	if _, err = w.WriteString(fmt.Sprintf("%s\n", op)); err != nil {
		return err
	}
	for _, ts := range timestamps {
		if value, ok := all[ts][op]; ok {
			if _, err = w.WriteString(fmt.Sprintf("%d %d\n", ts, value)); err != nil {
				return err
			}
		}
	}
	w.Flush()
	return nil
}

func main() {
	inputDir := flag.String("i", "", "Input directory containing Watchtower backup files")
	outputDir := flag.String("o", "", "Output directory where the bm-data-svc files will be placed")
	
	flag.Parse()
	if *inputDir == "" {
		fmt.Println("Watchtower backup directory not specified.")
		return
	}
	if *outputDir == "" {
		fmt.Println("Output directory not specified.")
		return
	}

	files, err := ioutil.ReadDir(*inputDir)
	if err != nil {
		fmt.Println("Error reading from the directory", err)
		return
	}

	all := make(map[int64]map[string]int)
	ops := make(map[string]bool)
	var timestamps Int64Slice
	for _, f := range files {
		if !f.IsDir() && strings.HasPrefix(f.Name(), "WT_BACKUP_") && strings.HasSuffix(f.Name(), ".txt") {
			data, err := loadJson(path.Join(*inputDir, f.Name()))
			if err != nil {
				fmt.Println("Error reading JSON file", err)
				return
			}
			fmt.Printf("Loaded %d entries from %s\n", len(data), f.Name())
			for k,v := range data {
				ts, err := strconv.ParseInt(k, 10, 64)
				if err != nil {
					fmt.Println("Failed to parse timestamp", err)
					return
				}
				all[ts] = v
				for op := range v {
					ops[op] = true
				}
				timestamps = append(timestamps, ts)
			}
		}
	}

	sort.Sort(timestamps)
	for op := range ops {
		outputFile := fmt.Sprintf("benchmark_%s.txt", op)
		if err = writeOutput(all, timestamps, op, path.Join(*outputDir, outputFile)); err != nil {
			fmt.Println("Error while writing output file", err)
			return
		}
		fmt.Printf("Wrote output to %s\n", outputFile)
	}
}
