#!/bin/bash

count=0;
total=0; 

#awk '{print "scale=4;",$3"/"$4}' app1.txt|bc |sort -n > grot
#awk '{print "scale=4;",$3"/"$4}' app2.txt|bc |sort -n > grot2

#for i in $( awk '{ print $1; }' grot )
for i in $( awk '{ print $1; }' grot2 )
   do 
     total=$(echo $total+$i | bc )
     ((count++))
   done
echo "scale=2; $total / $count" | bc

#grot: 0.06
#grot2: 0
