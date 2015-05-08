package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"strconv"
	"strings"
)

type WatchtowerData map[string]map[string]int

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

func main() {
	root := flag.String("p", "", "Path to the directory containing Watchtower backup files")
	flag.Parse()
	if *root == "" {
		fmt.Println("Path to the Watchtower backup directory not specified.")
		return
	}

	files, err := ioutil.ReadDir(*root)
	if err != nil {
		fmt.Println("Error reading from the directory", err)
		return
	}

	all := make(map[int64]map[string]int)
	ops := make(map[string]bool)
	for _, f := range files {
		if !f.IsDir() && strings.HasPrefix(f.Name(), "WT_BACKUP_") && strings.HasSuffix(f.Name(), ".txt") {
			data, err := loadJson(path.Join(*root, f.Name()))
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
			}
		}
	}

	fmt.Println("Total: ", len(all))
}
