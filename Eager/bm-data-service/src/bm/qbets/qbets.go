package qbets

import (
	"bm/db"
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strconv"
	"strings"
)

const (
	qbetsBin = "/Users/hiranya/Projects/eager/sandbox/qbets/run_qbets.sh"
)

func PredictQuantile(ts db.TimeSeries, q, c float64) (int,error) {
	file, err := ioutil.TempFile(os.TempDir(), "_qbets_")
	if err != nil {
		return -1, err
	}
	defer os.Remove(file.Name())

	out, err := runQBETS(ts, file.Name(), q, c)
	if err != nil {
		return -1, err
	}

	quantile, err := strconv.ParseFloat(out, 64)
	if err != nil {
		return -1, err
	}
	return int(quantile), nil
}

func runQBETS(ts db.TimeSeries, file string, q, c float64) (string,error) {
	var buffer bytes.Buffer
	for _, v := range ts {
		buffer.WriteString(fmt.Sprintf("%d\n", v))
	}
	if err := ioutil.WriteFile(file, buffer.Bytes(), 0644); err != nil {
		return "", err
	}

	out, err := exec.Command(qbetsBin, file, fmt.Sprintf("%f", q), fmt.Sprintf("%f", c)).Output()
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(string(out)), nil
}
