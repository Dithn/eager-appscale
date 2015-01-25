package qbets

import (
	"bm/db"
	"io/ioutil"
	"strconv"
	"strings"
	"testing"
)

func TestPredictQuantile1(t *testing.T) {
	var ts db.TimeSeries
	for i := 0; i < 250; i++ {
		ts = append(ts, db.Datapoint{Timestamp: int64(i), Value: i%100})
	}
	q, err := PredictQuantile(ts, 0.95, 0.05, false)
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
	q, err := PredictQuantile(ts, 0.95, 0.05, false)
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

func loadTS(file string) (db.TimeSeries, error) {
	data, err := ioutil.ReadFile(file)
	if err != nil {
		return nil, err
	}
	lines := strings.Split(string(data), "\n")
	var ts db.TimeSeries
	for _, l := range lines {
		if l == "" {
			continue
		}
		fields := strings.Fields(l)
		t, err := strconv.ParseInt(fields[0], 10, 64)
		if err != nil {
			return nil, err
		}
		v, err := strconv.Atoi(fields[1])
		if err != nil {
			return nil, err
		}
		ts = append(ts, db.Datapoint{Timestamp: t, Value: v})
	}
	return ts, nil
}
