package qbets

import (
	"bm/db"
	"testing"
)

func TestPredictQuantile(t *testing.T) {
	slice := make([]int, 0, 1000)
	for i := 0; i < 1000; i++ {
		slice = append(slice, i%100)
	}
	q, err := PredictQuantile(db.TimeSeries(slice), 0.95, 0.05)
	if err != nil {
		t.Error(err)
	}
	if q != 96 {
		t.Errorf("expected: 96, got: %f", q)
	}
}
