Javabook app 
AppScale fault injection (datastore api modified to glitch between 30th and 45th minute of 3rd hour -- 3, 6, 9 etc)
Glitch: 10ms delay

Benchmark frequency: 60s
History length: 1hour
SLO: 95% under 32ms (at 75% window fill)

Root cause analysis: RelativeImportance, Percentile (0.99)

Results Summary
===============
Anomalies detected: 746
Root cause events detected: 4365
Confirmed false positives (LOCAL and user anomalies): 922
Anomalies without any root causes identified in RunQuery: 75

Problems:
* False positive rate is high
* Some root cause events are not identified correctly (percentile limits skewed by anomalies in training data)
* Hard to identify good training data
