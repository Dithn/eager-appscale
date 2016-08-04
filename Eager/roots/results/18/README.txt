Javabook app 
AppScale fault injection (user api modified to glitch between 30th and 45th minute of 3rd hour -- 3, 6, 9 etc)
Glitch: 10ms delay

Benchmark frequency: 15s
History length: 1hour
SLO: 95% under 30ms (at 75% window fill)

Root cause analysis: RelativeImportance (with primary and seconday percentile verifications at 99th percentile)
  With anomaly filter on

