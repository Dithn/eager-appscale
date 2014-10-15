package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
)

type Result struct {
	Operation  string
	Iterations int
	Api        string
	Average    float64
	StdDev     float64
	RawData    []int
}

var server string
var port uint64
var samples int

func init() {
	const (
		defaultServer  = "localhost"
		serverUsage    = "target AppScale server"
		defaultPort    = 8080
		portUsage      = "target AppScale port"
		defaultSamples = 100
		samplesUsage   = "number of data samples to collect"
	)
	flag.StringVar(&server, "server", defaultServer, serverUsage)
	flag.StringVar(&server, "s", defaultServer, serverUsage+" (shorthand)")
	flag.Uint64Var(&port, "port", defaultPort, portUsage)
	flag.Uint64Var(&port, "p", defaultPort, portUsage+" (shorthand)")
	flag.IntVar(&samples, "samples", defaultSamples, samplesUsage)
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

	dsURL := url + "/datastore"

	testOp(dsURL, "put", "com.google.appengine.api.datastore.DatastoreService#put()", samples)
	testOp(dsURL, "get", "com.google.appengine.api.datastore.DatastoreService#get()", samples)
	testOp(dsURL, "asList", "com.google.appengine.api.datastore.PreparedQuery#asList()", samples)
	testOp(dsURL, "asIterable", "com.google.appengine.api.datastore.PreparedQuery#asIterable()", samples)
	testOp(dsURL, "delete", "com.google.appengine.api.datastore.DatastoreService#delete()", samples)

	/*fmt.Println()
	fmt.Println("Benchmarking datastore API (JDO)")
	fmt.Println("================================")
	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.makePersistent&count=%d", url, samples))
	printResult(result, result.Operation)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.getObjectById&count=%d", url, samples))
	printResult(result, result.Operation)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.close&count=%d", url, samples))
	printResult(result, result.Operation)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.execute&count=%d", url, samples))
	printResult(result, result.Operation)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.closeAll&count=%d", url, samples))
	printResult(result, result.Operation)

	result = doGet(fmt.Sprintf("%s/datastore?op=jdo.deletePersistent&count=%d", url, samples))
	printResult(result, result.Operation)*/
}

func testOp(url, op, method string, samples int) {
	fmt.Printf("Benchmarking %s method\n", method)
	var buffer bytes.Buffer
	buffer.WriteString(method + "\n")
	for i := 0; i < samples; i++ {
		if (i + 1) % 100 == 0 {
			fmt.Printf("Collected %d samples...\n", i + 1)
		}
		result := doGet(fmt.Sprintf("%s?op=%s&count=1", url, op))
		for _, r := range result.RawData {
			buffer.WriteString(fmt.Sprintf("%d\n", r))
		}
	}
	ioutil.WriteFile("benchmark_" + op + ".txt", buffer.Bytes(), 0644)
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
