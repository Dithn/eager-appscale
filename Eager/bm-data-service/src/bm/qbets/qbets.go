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
// quantile with c upper-confidence. It returns the quantile predicted at
// the last data point of the time series.
func PredictQuantile(ts db.TimeSeries, q, c float64, debug bool) (float64, error) {
	file, err := ioutil.TempFile(os.TempDir(), "_qbets_")
	if err != nil {
		return -1, err
	}
	defer os.Remove(file.Name())

	return getLastPrediction(ts, file.Name(), q, c, debug)
}

// PredictQuantileTrace runs QBETS on the given TimeSeries to predict its q-th
// quantile with c upper-confidence at each data point. It returns the quantiles
// calculated at each data point of the time series.
func PredictQuantileTrace(ts db.TimeSeries, q, c float64, debug bool) ([]float64, error) {
	file, err := ioutil.TempFile(os.TempDir(), "_qbets_")
	if err != nil {
		return nil, err
	}
	defer os.Remove(file.Name())

	return getAllPredictions(ts, file.Name(), q, c, debug)
}

func runQBETS(ts db.TimeSeries, file string, q, c float64, debug bool) ([]byte, error) {
	var buffer bytes.Buffer
	for _, v := range ts {
		buffer.WriteString(fmt.Sprintf("%d\n", v))
	}
	if err := ioutil.WriteFile(file, buffer.Bytes(), 0644); err != nil {
		return nil, err
	}

	// TODO: How to set the -t parameter properly?
	t := "0"
	if len(ts) < 20 {
		return nil, fmt.Errorf("not enough data points in the TimeSeries")
	} else if len(ts) < 200 {
		t = "10"
	}
	return exec.Command(qbetsBin, "-f", file, "-q", fmt.Sprintf("%f", q), "-c", fmt.Sprintf("%f", c), "-t", t, "-T").Output()
}

func getLastPrediction(ts db.TimeSeries, file string, q, c float64, debug bool) (float64, error) {
	out, err := runQBETS(ts, file, q, c, debug)
	if err != nil {
		return -1, err
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
	return strconv.ParseFloat(segments[5], 64)
}

func getAllPredictions(ts db.TimeSeries, file string, q, c float64, debug bool) ([]float64, error) {
	out, err := runQBETS(ts, file, q, c, debug)
	if err != nil {
		return nil, err
	}

	lines := strings.Split(string(out), "\n")
	var p []float64
	for _, l := range lines {
		if debug {
			fmt.Println(l)
		}
		if strings.HasPrefix(l, "time:") {
			segments := strings.Fields(l)
			val, err := strconv.ParseFloat(segments[5], 64)
			if err != nil {
				return nil, err
			}
			p = append(p, val)
		}
	}
	return p, nil
}
