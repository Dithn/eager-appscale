// Package qbets contains the utility functions for running QBETS over a
// time series to predict its quantiles.
package qbets

import (
	"bm/db"
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strconv"
	"strings"
)

const (
	qbetsBin = "/Users/hiranya/Projects/eager/sandbox/qbets/bmbp_ts"
)

// PredictQuantile runs QBETS on the given TimeSeries to predict its q-th
// quantile with c upper-confidence.
func PredictQuantile(ts db.TimeSeries, q, c float64, debug bool) (float64, error) {
	file, err := ioutil.TempFile(os.TempDir(), "_qbets_")
	if err != nil {
		return -1, err
	}
	defer os.Remove(file.Name())

	out, err := runQBETS(ts, file.Name(), q, c, debug)
	if err != nil {
		return -1, err
	}

	return strconv.ParseFloat(out, 64)
}

func runQBETS(ts db.TimeSeries, file string, q, c float64, debug bool) (string, error) {
	var buffer bytes.Buffer
	for _, v := range ts {
		buffer.WriteString(fmt.Sprintf("%d\n", v))
	}
	if err := ioutil.WriteFile(file, buffer.Bytes(), 0644); err != nil {
		return "", err
	}

	// TODO: How to set the -t parameter properly?
	t := "0"
	if len(ts) < 20 {
		return "", fmt.Errorf("not enough data points in the TimeSeries")
	} else if len(ts) < 200 {
		t = "10"
	}
	out, err := exec.Command(qbetsBin, "-f", file, "-q", fmt.Sprintf("%f", q), "-c", fmt.Sprintf("%f", c), "-t", t, "-T").Output()
	if err != nil {
		return "", err
	}
	lines := strings.Split(string(out), "\n")
	var last string
	for _, l := range lines {
		if debug {
			fmt.Println(l)
		}
		if strings.HasPrefix(l, "time:") {
			last = l
		}
	}
	segments := strings.Fields(last)
	return segments[5], nil
}
