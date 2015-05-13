#!/bin/sh
# Extract violation time stats from KittySLAEvolutionAnalyzer output (adaptive interval mode)
fgrep "[sla]" $1 | tail -n +2 | awk '{print $5}' > temp.txt
drstats -m simple temp.txt
rm temp.txt
