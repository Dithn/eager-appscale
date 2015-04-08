package analysis

import (
	"bm/db"

	"testing"
)

func TestSimplePredictor(t *testing.T) {
	pred := &SimplePredictor{}
	for index, item := range []struct {
		data []int
		q float64
		want []int
	}{
		{data: []int{1,2,3,4,5,6,7,8,9,10}, q: 0.5, want: []int{1,1,2,2,3,3,4,4,5,5}},
		{data: []int{1,2,3,4,5,6,7,8,9,10}, q: 0.95, want: []int{1,2,3,4,5,6,7,8,9,10}},
		{data: []int{10,9,8,7,6,5,4,3,2,1}, q: 0.5, want: []int{10,9,9,8,8,7,7,6,6,5}},
	}{
		got, err := pred.PredictQuantileTrace(getTimeSeries(item.data), item.q, 0, false)
		if err != nil {
			t.Error(err)
		}
		if !checkTimeSeries(got, item.want) {
			t.Errorf("[%d] want: %v, got: %v", index, getTimeSeries(item.want), got)
		}
	}
}

func checkTimeSeries(ts db.TimeSeries, want []int) bool {
	if len(want) != len(ts) {
		return false
	}
	for i,v := range ts {
		if v.Value != want[i] {
			return false
		}
	}
	return true
}

func getTimeSeries(data []int) db.TimeSeries {
	var ts db.TimeSeries
	for i, v := range data {
		ts = append(ts, db.Datapoint{Timestamp: int64(i), Value: v})
	}
	return ts
}
