#!/bin/bash
c=1
echo "Load testing: $1"
echo "Iterations: $2"
rm results.txt
while [ $c -le $2 ]
do
  echo "Iteration $c"
  VAL=`curl -w "@curl-format.txt" -o /dev/null -s $1 | grep "time_total"`
  echo "get $VAL" >> results.txt
  VAL=`curl -w "@curl-format.txt" -o /dev/null -s $1 | grep "time_total"`
  echo "put $VAL" >> results.txt
  VAL=`curl -w "@curl-format.txt" -o /dev/null -s $1 | grep "time_total"`
  echo "delete $VAL" >> results.txt
  #curl -w "@curl-format.txt" -o /dev/null -X POST -d "comment=Foo" -s $1 | grep "time_total" >> results.txt
  #curl -w "@curl-format.txt" -o /dev/null -X POST -d "firstName=Foo&lastName=Bar" -s $1 | grep "time_total" >> results.txt
  sleep $3
  (( c++ ))
done
