package bmutil

import (
	"testing"
)

func TestReadLines(t *testing.T) {
	lines, err := ReadLines("testdata/1.txt")
	if err != nil {
		t.Error(err)
	}

	out := []string{"Hello", "World", "123"}
	if len(lines) != len(out) {
		t.Errorf("Length mismatch -- want: %d, got %d", len(out), len(lines))
	}
	for index, line := range lines {
		if out[index] != line {
			t.Errorf("Value mismatch at index %d -- want: %s, got %s", index, out[index], line)
		}
	}
}
