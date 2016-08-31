Stocks (trendsapi) app 
AppScale fault injection once every 2 hours (Even numbered hours on minutes 0, 1, 2)
Fault: 45ms delay to (first|second) RunQuery operations

Benchmark frequency: 15s
History length: 1hour
SLO: 95% under (177ms)

Root cause analysis: RelativeImportance (with primary and seconday percentile verifications at 99th percentile)
New filter on (mean + 1.65 sd on local)
