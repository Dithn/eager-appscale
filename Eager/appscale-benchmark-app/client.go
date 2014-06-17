package main

import (
	"fmt"
	"encoding/json"
	"net/http"
	"io/ioutil"
	"flag"
)

type Result struct {
	Operation string
	Iterations int
	Api string
	Average float64
	StdDev float64
}

var server string

func init() {
	const (
		defaultServer = "localhost"
		usage = "target AppScale server"
	)
	flag.StringVar(&server, "server", defaultServer, usage)
	flag.StringVar(&server, "s", defaultServer, usage + " (shorthand)")
}

func main() {
	flag.Parse()
	fmt.Println("Target Server: ", server)
	url := fmt.Sprintf("http://%s:%d", server, 8080)
	
	fmt.Println("Benchmarking datastore API")
	fmt.Println("==========================")
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

	fmt.Println()
	fmt.Println("Benchmarking datastore API (JDO)")
	fmt.Println("================================")
	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.makePersistent&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.getObjectById&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.close&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.execute&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.closeAll&count=100", url))
	printResult(result)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.deletePersistent&count=100", url))
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
