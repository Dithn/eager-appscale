package main

import (
	"fmt"
	"encoding/json"
	"net/http"
	"io/ioutil"
)

type Result struct {
	Operation string
	Iterations int
	Api string
	Average float64
	StdDev float64
}

func main() {
	url := fmt.Sprintf("http://%s:%d", "localhost", 8080)
	

	fmt.Println("Benchmarking datastore API...")
	req, err := http.NewRequest("DELETE", fmt.Sprintf("%s/datastore", url), nil)
	if err != nil {
		fmt.Println(err)
		return
	}
	_, err = http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println(err)
		return
	}
	
	result := doGet(fmt.Sprintf("%s/datastore?op=put&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=get&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=asList&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=asIterable&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=delete&count=100", url))
	printResult(result)
}

func printResult(result *Result) {
	fmt.Println(result.Operation, ": Iterations =", result.Iterations, "; Average =", result.Average, "ms ; StdDev =", result.StdDev)
}

func doGet(url string) *Result {
	resp, err := http.Get(url)
	if err != nil {
		fmt.Println(err)
		panic("Error making HTTP GET on " + url)
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		fmt.Println(err)
		panic("Error while parsing response body")
	}

	var r Result
	err = json.Unmarshal(body, &r)
	if err != nil {
		fmt.Println(err)
		panic("Error parsing json")
	}
	return &r
}
