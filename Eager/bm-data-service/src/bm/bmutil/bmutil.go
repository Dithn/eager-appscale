// Package bmutil contains common utility methods for handling various
// I/O activities.
package bmutil

import (
	"bufio"
	"os"
)

// ReadLines reads a line-oriented file and returns the file content
// as a sequence of lines.
func ReadLines(path string) ([]string,error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	var lines []string
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}
	return lines, nil
}
