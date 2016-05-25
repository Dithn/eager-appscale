Javabook app 
AppScale fault injection (datastore api modified to glitch between 30th and 45th minute of 3rd hour -- 3, 6, 9 etc; probability = 0.35)

Benchmark frequency: 60s
History length: 1hour
SLO: 95% (at 75% window fill)

Root cause analysis: RelativeImportance, Percentile (0.99)

NOTE: Same set up as 6 except with some logging improvements
