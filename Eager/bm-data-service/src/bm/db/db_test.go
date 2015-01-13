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
		{n: 10, op: "Test1()", ts: TimeSeries([]int{3, 4, 5, 6, 7, 8, 9, 10, 11, 12})},
		{n: 10, op: "Test2()", ts: TimeSeries([]int{5, 4, 3, 2, 1})},
		{n: 10, op: "Test3()", ts: TimeSeries([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})},
		{n: 0, op: "Test1()", ts: TimeSeries([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})},
		{n: -1, op: "Test2()", ts: TimeSeries([]int{5, 4, 3, 2, 1})},
		{n: -1, op: "Test3()", ts: TimeSeries([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})},
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
