#!/bin/sh
fgrep "[sla]" $1 | awk '{print $5}' | grep -v violation > temp
drstats -m simple temp
rm temp
