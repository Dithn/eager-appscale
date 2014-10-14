package main

import (
	"bytes"
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
	RawData []int
}

var server string
var port uint64

func init() {
	const (
		defaultServer = "localhost"
		serverUsage = "target AppScale server"
		defaultPort = 8080
		portUsage = "target AppScale port"
	)
	flag.StringVar(&server, "server", defaultServer, serverUsage)
	flag.StringVar(&server, "s", defaultServer, serverUsage + " (shorthand)")
	flag.Uint64Var(&port, "port", defaultPort, portUsage)
	flag.Uint64Var(&port, "p", defaultPort, portUsage + " (shorthand)")
}

func main() {
	flag.Parse()
	fmt.Println("Target Server: ", server)
	fmt.Println("Target Port: ", port)
	fmt.Println()

	url := fmt.Sprintf("http://%s:%d", server, port)
	
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
	var buffer bytes.Buffer
	buffer.WriteString(result.Operation + "\n")
	for _, r := range result.RawData {
		buffer.WriteString(fmt.Sprintf("%d\n", r))
	}
	ioutil.WriteFile("benchmark_" + result.Operation + ".txt", buffer.Bytes(), 0644)
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
