package db

import (
	"testing"
)

func TestFSDatabase(t *testing.T) {
	fsd, err := NewFSDatabase("testdata/")
	if err != nil {
		t.Error(err)
	}

	for _, want := range []struct {
		n  int
		op string
		ts TimeSeries
	}{
		{n: 10, op: "Test1()", ts: createTS([]int{3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, 3)},
		{n: 10, op: "Test2()", ts: createTS([]int{5, 4, 3, 2, 1}, 1)},
		{n: 10, op: "Test3()", ts: createTS([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 1)},
		{n: 0, op: "Test1()", ts: createTS([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, 1)},
		{n: -1, op: "Test2()", ts: createTS([]int{5, 4, 3, 2, 1}, 1)},
		{n: -1, op: "Test3()", ts: createTS([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 1)},
	} {
		got, err := fsd.Query(want.n, []string{want.op}, -1, -1)
		if err != nil {
			t.Error(err)
		}
		if !compareTS(got[want.op], want.ts) {
			t.Errorf("incorrect query results; want: %v, got %v", want.ts, got[want.op])
		}
	}

	if _, err := fsd.Query(10, []string{"Bogus()"}, -1, -1); err == nil {
		t.Error("no error returned on query with invalid operation")
	}
}

func createTS(val []int, start int) TimeSeries {
	var ts TimeSeries
	for i,v := range val {
		ts = append(ts, Datapoint{
			Timestamp: int64(start+i),
			Value: v,
		})
	}
	return ts
}

func compareTS(ts1, ts2 TimeSeries) bool {
	if len(ts1) != len(ts2) {
		return false
	}
	for i, v := range ts1 {
		if v != ts2[i] {
			return false
		}
	}
	return true
}
