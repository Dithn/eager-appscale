#!/bin/sh
python ../cloud_simulator.py -f $1/simulation.conf > $1/output.txt
Rscript relimp_finder.R $1/output.txt > $1/relimp_finder.txt
fgrep "[relimp]" $1/relimp_finder.txt > $1/relimp.txt
Rscript relimp_plotter.R $1/relimp.txt
mv Rplots.pdf $1/relimp_vs_time.pdf