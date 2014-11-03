package db

import (
	"testing"
)

func TestFSDatabase(t *testing.T) {
	fsd, err := NewFSDatabase("testdata/")
	if err != nil {
		t.Error(err)
	}

	got, err := fsd.Query(10, []string{"Test1()", "Test2()", "Test3()"})
	if err != nil {
		t.Error(err)
	}

	for _, want := range []struct {
		op string
		ts TimeSeries
	}{
		{op: "Test1()", ts: TimeSeries([]int{3, 4, 5, 6, 7, 8, 9, 10, 11, 12})},
		{op: "Test2()", ts: TimeSeries([]int{5, 4, 3, 2, 1})},
		{op: "Test3()", ts: TimeSeries([]int{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})},
	} {
		if !compareTS(got[want.op], want.ts) {
			t.Errorf("incorrect query results; want: %v, got %v", want.ts, got[want.op])
		}
	}

	if _, err := fsd.Query(10, []string{"Bogus()"}); err == nil {
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
