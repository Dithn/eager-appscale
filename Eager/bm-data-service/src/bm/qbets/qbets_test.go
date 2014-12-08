package qbets

import (
	"bm/db"
	"io/ioutil"
	"strconv"
	"strings"
	"testing"
)

func TestPredictQuantile1(t *testing.T) {
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

func TestPredictQuantile2(t *testing.T) {
	ts, err := loadTS("testdata/ts1.txt")
	if err != nil {
		t.Error(err)
	}
	q, err := PredictQuantile(db.TimeSeries(ts), 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}
	if q != 55 {
		t.Errorf("expected: 55, got: %f", q)
	}
}

/*func TestPredictQuantileTrace(t *testing.T) {
	ts, err := loadTS("testdata/ts1.txt")
	if err != nil {
		t.Error(err)
	}

	results, err := PredictQuantileTrace(db.TimeSeries(ts), 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}

	for i := 60; i < 600; i++ {
		data := ts[0:i+1]
		q, err := PredictQuantile(db.TimeSeries(data), 0.95, 0.05, false)
		if err != nil {
			t.Error(err)
		}
		if q != results[i - 60] {
			t.Fatalf("[%d] Expected: %f, Got: %f", i, results[i-60], q)
		}
	}
}*/

func loadTS(file string) ([]int,error) {
	data, err := ioutil.ReadFile(file)
	if err != nil {
		return nil, err
	}
	lines := strings.Split(string(data), "\n")
	var ts []int
	for _, l := range lines {
		if l == "" {
			continue
		}
		v, err := strconv.Atoi(l)
		if err != nil {
			return nil, err
		}
		ts = append(ts, v)
	}
	return ts, nil
}
