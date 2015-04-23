#!/bin/sh
egrep -o "d=(\d+)\b" $1 | cut -c 3- > temp
drstats -m simple temp
rm temp
