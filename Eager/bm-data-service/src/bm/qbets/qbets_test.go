package qbets

import (
	"bm/db"
	"testing"
)

func TestPredictQuantile(t *testing.T) {
	slice := make([]int, 0, 1000)
	for i := 0; i < 250; i++ {
		slice = append(slice, i%100)
	}
	q, err := PredictQuantile(db.TimeSeries(slice), 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}
	if q != 99 {
		t.Errorf("expected: 99, got: %f", q)
	}
}
