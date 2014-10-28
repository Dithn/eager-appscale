package db

import (
	"bufio"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"strconv"
	"strings"
)

type TimeSeries []int

type Database interface {
	Query(n int, ops []string) map[string]TimeSeries
}

type FSDatabase struct {
	data map[string]TimeSeries
}

func NewFSDatabase(root string) (*FSDatabase,error) {
	files, err := ioutil.ReadDir(root)
	if err != nil {
		return nil, err
	}

	fsd := &FSDatabase {
		data: make(map[string]TimeSeries),
	}
	for _, f := range files {
		if !f.IsDir() && strings.HasPrefix(f.Name(), "benchmark_") && strings.HasSuffix(f.Name(), ".txt") {
			name, ts, err := loadFile(root, f.Name())
			if err != nil {
				return nil, err
			}
			fsd.data[name] = ts
		}
	}
	return fsd, nil
}

func (fsd *FSDatabase) Query(n int, ops []string) map[string]TimeSeries {
	result := make(map[string]TimeSeries)
	for _, op := range ops {
		ts, ok := fsd.data[op]
		if !ok {
			panic("no data available for " + op)
		}
		if n <= len(ts) {
			result[op] = ts[len(ts)-n:]
		} else {
			result[op] = ts
		}
	}
	return result
}

func loadFile(root, child string) (string,TimeSeries,error) {
	fmt.Println("Loading data from", child)
	fh, err := os.Open(path.Join(root, child))
	if err != nil {
		return "", nil, err
	}
	defer fh.Close()

	scanner := bufio.NewScanner(fh)
	name := ""
	var ts TimeSeries
	for scanner.Scan() {
		if name == "" {
			name = scanner.Text()
		} else {
			val, err := strconv.Atoi(scanner.Text())
			if err != nil {
				return "", nil, err
			}
			ts = append(ts, val)
		}
	}
	if err := scanner.Err(); err != nil {
		return "", nil, err
	}
	return name, ts, nil
}
