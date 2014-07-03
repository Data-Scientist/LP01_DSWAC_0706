#!/bin/bash

# Store the training file name
s=$1
# Count the number of lines
n=`hadoop fs -cat $s | wc -l | awk '{ print $1 }'`
# Calculate the inverse number of lines
e=`perl -e "print 1 / $n;"`
# Get my user id
u=`id -u -n`

# Remove bits from previous runs
rm v

# Build the initial SimRank vector
for line in `hadoop fs -cat $s`
do
   echo -e "$line\t$e" >> v
done

# Upload it to HDFS
hadoop fs -rm v
hadoop fs -put v

i=0
j=0

# Iterate until we converge (more or less)
while :
do
   # Update the counters
   i=$j
   j=`expr $i + 1`

   # Run the job
   echo Beginning pass $j
   hadoop jar $STREAMING -files "hdfs:///user/$u/v,hdfs:///user/$u/$s" \
                         -mapper "simrank_map.py v" -file simrank_map.py \
                         -reducer "simrank_reduce.py $s" -file simrank_reduce.py \
                         -input kid -input item -output simrank$j

   # If this isn't the first pass, test for convergence
   if [ $i != 0 ]
   then
      # Since we don't need perfect results, we'll converge at 0.01
      python simrank_diff.py simrank$i/part-00000 simrank$j/part-00000 0.01
      
      if [ $? = 1 ]
      then
         exit
      fi
   fi

   # Prepare the SimRank vector for the next pass
   echo Copying vector for next round
   hadoop fs -rm v
   hadoop fs -cp simrank$j/part-00000 v
done
