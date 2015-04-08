// Package analysis provides the necessary interfaces for analysing time
// series data.
package analysis

import (
	"bm/db"
	"bm/qbets"
	"errors"
	"math"
	"sort"
)

// Predictor interface contains the functions for making quantile predictions
// on time series data.
type Predictor interface {
	PredictQuantileTrace(ts db.TimeSeries, q, c float64, debug bool) (db.TimeSeries,error)
	PredictQuantile(ts db.TimeSeries, q, c float64, debug bool) (int,error)
}

// QbetsPredictor uses QBETS to analyze given time series data, and make
// quantile predictions on them.
type QbetsPredictor struct {}

func (pred *QbetsPredictor) PredictQuantileTrace(ts db.TimeSeries, q, c float64, debug bool) (db.TimeSeries,error) {
	return qbets.PredictQuantileTrace(ts, q, c, debug)
}

func (pred *QbetsPredictor) PredictQuantile(ts db.TimeSeries, q, c float64, debug bool) (int,error) {
	return qbets.PredictQuantile(ts, q, c, debug)
}

// SimplePredictor makes quantile predictions on time series data by simply
// computing the true quantile of the time series at each data point.
type SimplePredictor struct {}

func (pred *SimplePredictor) PredictQuantileTrace(ts db.TimeSeries, q, c float64, debug bool) (db.TimeSeries,error) {
	data := sortData(ts)
	var err error
	result := make(db.TimeSeries, len(ts))
	for i := len(ts) - 1; i >= 0; i-- {
		result[i].Timestamp = ts[i].Timestamp
		result[i].Value = quantile(data, q)
		result[i].Cwrong = 3
		if data, err = remove(data, ts[i].Value); err != nil {
			return nil, err
		}
	}
	return result, nil
}

func (pred *SimplePredictor) PredictQuantile(ts db.TimeSeries, q, c float64, debug bool) (int,error) {
	data := sortData(ts)
	return quantile(data, q), nil
}

func sortData(ts db.TimeSeries) []int {
	data := make([]int, len(ts))
	for i,v := range ts {
		data[i] = v.Value
	}
	sort.Ints(data)
	return data
}

func quantile(data []int, q float64) int {
	index := int(math.Ceil(q * float64(len(data))))
	return data[index - 1]
}

func find(data []int, v int) int {
	imax := len(data) - 1
	imin := 0
	for imax >= imin {
		imid := (imin + imax) / 2
		if data[imid] == v {
			return imid
		} else if data[imid] < v {
			imin = imid + 1
		} else {
			imax = imid - 1
		}
	}
	return -1
}

func remove(data []int, v int) ([]int, error) {
	index := find(data, v)
	if index == -1 {
		return nil, errors.New("value not found in slice")
	}
	return append(data[:index], data[index+1:]...), nil
}
