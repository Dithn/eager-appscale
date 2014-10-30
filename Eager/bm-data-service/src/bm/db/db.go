// Package db encapsulates the interfaces and functions necessary for
// loading and querying existing time series data.
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

// TimeSeries represents a sequence of time-ordered data
type TimeSeries []int

// Database interface defines what operations/queries should be supported
// by a database implementation.
type Database interface {
	Query(n int, ops []string) map[string]TimeSeries
}

// FSDatabase implements the Database interface using a set of data files
// located in the local file system.
type FSDatabase struct {
	data map[string]TimeSeries
}

// NewFSDatabase creates a new FSDatabase instance by loading the 
// required time serties data from the specified directory in the file
// system.
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

// Query returns a set of TimeSeries instances as a map, keyed by the
// operation names. The maximum length of each TimeSeries will be limited
// to n.
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
