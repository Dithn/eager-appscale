#!/bin/sh
egrep "\[violation\]|\[eot\]" $1 | tail -n +2  | awk '{print $3" "$5}' > temp
go run cdf_2col.go temp
rm temp
