package analysis

import (
	"bm/db"
	"io/ioutil"
	"strconv"
	"strings"
	"testing"
)

func TestPredictQuantile1(t *testing.T) {
	pred := DefaultQbetsPredictor()
	var ts db.TimeSeries
	for i := 0; i < 250; i++ {
		ts = append(ts, db.Datapoint{Timestamp: int64(i), Value: i%100})
	}
	q, err := pred.PredictQuantile(ts, 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}
	if q != 97 {
		t.Errorf("expected: 97, got: %d", q)
	}
}

func TestPredictQuantile2(t *testing.T) {
	pred := DefaultQbetsPredictor()
	ts, err := loadTS("testdata/ts1.txt")
	if err != nil {
		t.Error(err)
	}
	q, err := pred.PredictQuantile(ts, 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}
	if q != 55 {
		t.Errorf("expected: 55, got: %f", q)
	}
}

func TestPredictQuantileTrace(t *testing.T) {
	pred := DefaultQbetsPredictor()
	ts, err := loadTS("testdata/ts1.txt")
	if err != nil {
		t.Error(err)
	}

	results, err := pred.PredictQuantileTrace(db.TimeSeries(ts), 0.95, 0.05, false)
	if err != nil {
		t.Error(err)
	}

	for i := 60; i < 600; i+=100 {
		data := ts[0:i+1]
		q, err := pred.PredictQuantile(db.TimeSeries(data), 0.95, 0.05, false)
		if err != nil {
			t.Error(err)
		}
		if q != results[i - 60].Value {
			t.Fatalf("[%d] Expected: %f, Got: %f", i, results[i-60], q)
		}
	}
}

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
