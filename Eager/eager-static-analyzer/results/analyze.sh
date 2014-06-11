#!/bin/sh
grep "Distinct paths through the code" $1 | egrep -o "[0-9]+$" > temp.txt
echo "Distinct Paths Through Code Histogram"
echo "====================================="
go run histo.go temp.txt

echo ""

echo "Loop Analysis"
echo "============="
value=`grep "Loops\: 0" $1 | wc -l`
echo "Method with no loops: $value"

value=`grep "Nesting Level" $1 | wc -l `
echo "Total Loops: $value"

value=`grep "Nesting Level" $1 | grep -v "\: 0" | wc -l`
echo "Loops with API calls: $value"

value=`grep "Nesting Level\: 1" $1 | wc -l`
echo "Nesting level 1: $value"

value=`grep "Nesting Level\: 2" $1 | wc -l`
echo "Nesting level 2: $value"

echo ""

echo "API Calls in Loops"
echo "=================="
grep "Nesting Level" $1 | egrep -o "[0-9]+$" > temp.txt
go run histo.go temp.txt

echo ""

echo "Method with Different API Call Counts"
echo "====================================="
grep "API calls in paths" $1 | egrep -o "\[.*\]" > temp.txt
value=`wc -l temp.txt`
echo "Total methods: $value"

value=`go run api_call_counter.go temp.txt | wc -l`
echo "Methods with different API call counts: $value"

grep "allocations in paths" $1 | egrep -o "\[.*\]" > temp.txt
value=`go run api_call_counter.go temp.txt | wc -l`
echo "Method with different allocation counts: $value"

echo ""

echo "GAE API Usage"
echo "============="
grep -o "\[cloud\].*" $1 > temp.txt
go run gae_histo.go temp.txt

echo ""

echo "API Call Counts"
echo "================"
grep "API calls in paths" $1 | egrep -o "\[.*\]" > temp.txt
go run api_call_count_list.go temp.txt

echo ""

echo "Allocation Counts"
echo "================="
grep "allocations in paths" $1 | egrep -o "\[.*\]" > temp.txt
go run api_call_count_list.go temp.txt