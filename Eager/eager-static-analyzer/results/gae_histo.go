package main

import (
	"os"
	"fmt"
	"bufio"
	"io"
	"strings"
)

func main() {
	if len(os.Args) != 2 {
		fmt.Println("Usage: histo <path>")
		return
	}
	fmt.Println("Reading input from:", os.Args[1])

	inputFile, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Println("Error while opening input file", err)
		return
	}

	defer inputFile.Close()
	bf := bufio.NewReader(inputFile)
	data := make(map[string]int)
	total := 0
	for {
		line, err := bf.ReadBytes('\n')
                if err == io.EOF {
			break
		} else if err != nil {
			fmt.Println("Error while reading line", err)
			return
		} 

		segments := strings.Split(string(line), " ")
		method := segments[1]
		if strings.Contains(method, ".datastore.") || strings.Contains(method, ".persistence.") || strings.Contains(method, ".jdo.") || strings.Contains(method, ".datanucleus.") {
			data["datastore"] += 1
		} else if strings.Contains(method, ".files.") {
			data["files"] += 1
		} else if strings.Contains(method, ".taskqueue.") {
			data["taskqueue"] += 1
		} else if strings.Contains(method, ".urlfetch.") {
			data["urlfetch"] += 1
		} else if strings.Contains(method, ".users.") {
			data["users"] += 1
		} else if strings.Contains(method, ".xmpp.") {
			data["xmpp"] += 1
		} else if strings.Contains(method, ".memcache.") {
			data["memcache"] += 1
		} else if strings.Contains(method, ".channel.") {
			data["channel"] += 1
		} else {
			fmt.Println(method)
		}
		total++
	}

	for k, v := range data {
		fmt.Println(k, v)
	}
	fmt.Println("Total:", total)
}
