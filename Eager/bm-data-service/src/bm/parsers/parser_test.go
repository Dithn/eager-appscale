package main

import (
	"testing"
)

func TestGetTSData(t *testing.T) {
	got, err := getTSData("testdata/1.txt")
	if err != nil {
		t.Error(err)
	}
	want := tsData{
		APICallTime: []int{5,4,3,2,1},
		OtherTime: []int{1,2,3,4,5},
	}
	if !equals(got.APICallTime, want.APICallTime) {
		t.Errorf("want: %v, got: %v", want.APICallTime, got.APICallTime)
	}
	if !equals(got.OtherTime, want.OtherTime) {
		t.Errorf("want: %v, got: %v", want.OtherTime, got.OtherTime)
	}
}

func equals(a, b []int) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
