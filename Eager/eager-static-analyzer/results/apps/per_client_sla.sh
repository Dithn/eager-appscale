#!/bin/sh
go run per_client_sla_change_analysis.go $1 > temp
drstats -m cdf -c temp
rm temp
