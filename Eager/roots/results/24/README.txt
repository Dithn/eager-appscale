Javabook app 
AppScale fault injection (datastore api modified to glitch every half an hour for 2 minutes)
Glitch: 45ms delay

Benchmark frequency: 15s
History length: 1hour
SLO: 95% under 45ms

Root cause analysis: RelativeImportance (with primary and seconday percentile verifications at 99th percentile)
New filter on (mean + 1.65 sd on local)
