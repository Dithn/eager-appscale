package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"sort"
	"time"
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
	fmt.Println("Cleaned up old test data in datastore")

	dsURL := url + "/datastore"

	testOp(dsURL, "put", "com.google.appengine.api.datastore.DatastoreService#put()", samples)
	testOp(dsURL, "get", "com.google.appengine.api.datastore.DatastoreService#get()", samples)
	testOp(dsURL, "asList", "com.google.appengine.api.datastore.PreparedQuery#asList()", samples)
	testOp(dsURL, "asIterable", "com.google.appengine.api.datastore.PreparedQuery#asIterable()", samples)
	testOp(dsURL, "delete", "com.google.appengine.api.datastore.DatastoreService#delete()", samples)

	/*testOp(dsURL, "jdo.makePersistent", "javax.jdo.PersistenceManager#makePersistent()", samples)
	testOp(dsURL, "jdo.getObjectById", "javax.jdo.PersistenceManager#getObjectById()", samples)
	testOp(dsURL, "jdo.close", "javax.jdo.PersistenceManager#close()", samples)
	testOp(dsURL, "jdo.execute", "javax.jdo.Query#execute()", samples)
	testOp(dsURL, "jdo.closeAll", "javax.jdo.Query#closeAll()", samples)
	testOp(dsURL, "jdo.deletePersistent", "javax.jdo.PersistenceManager#deletePersistent()", samples)*/
}

func testOp(url, op, method string, samples int) {
	fmt.Printf("Benchmarking %s method\n", method)
	tr := &http.Transport{
		DisableKeepAlives: false,
		MaxIdleConnsPerHost: 10,
	}
	client := &http.Client{Transport: tr}

	var buffer bytes.Buffer
	buffer.WriteString(method + "\n")
	var data []int
	for i := 0; i < samples; i++ {
		if (i+1)%100 == 0 {
			fmt.Printf("Collected %d samples...\n", i+1)
		}
		result := doGet(client, fmt.Sprintf("%s?op=%s&obj=%d", url, op, i))
		if result.RawData[0] < 4 {
			time.Sleep(150 * time.Millisecond)
		}
		for _, r := range result.RawData {
			data = append(data, r)
		}
	}
	sort.Sort(sort.Reverse(sort.IntSlice(data)))
	for _, d := range data {
		buffer.WriteString(fmt.Sprintf("%d\n", d))
	}
	ioutil.WriteFile("benchmark_"+op+".txt", buffer.Bytes(), 0644)
}

func doGet(client *http.Client, url string) *Result {
	//start := time.Now()
	resp, err := client.Get(url)
	//fmt.Println("Time elapsed:", time.Now().Sub(start))
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
