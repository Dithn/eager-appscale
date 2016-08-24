Stocks (trendsapi) app 
No AppScale fault injection

Benchmark frequency: 15s
History length: 1hour
SLO: 95% under (175ms)

Root cause analysis: RelativeImportance (with primary and seconday percentile verifications at 99th percentile)
New filter on (mean + 1.65 sd on local)
