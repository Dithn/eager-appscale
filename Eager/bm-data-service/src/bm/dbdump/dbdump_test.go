package main

import (
	"bm/db"
	"testing"
)

func TestDBDump(t *testing.T) {
	d, err := db.NewFSDatabase("testdata")
	if err != nil {
		t.Fatal(err)
	}
	p, err := getQuantile(d, 0.95, 0.05, "Test1()", -1)
	if err != nil {
		t.Fatal(err)
	}
	if p != 55 {
		t.Fatalf("expected: %d, got: %d", 55, p)
	}
}
