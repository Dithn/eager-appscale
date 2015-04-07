// Package analysis provides the necessary interfaces for analysing time
// series data.
package analysis

import (
	"bm/db"
	"bm/qbets"
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
	return qbets.PredictQuantileTrace(ts, q, c, debug)
}
