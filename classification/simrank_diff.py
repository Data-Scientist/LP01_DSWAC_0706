#!/usr/bin/python
# coding=utf8

import subprocess
import sys

def main():
   if len(sys.argv) < 4:
      sys.stderr.write('Usage: python simrank_diff.py v1 v2 threshold')
      exit(-1)
   
   v = {}

   # Read the first file from HDFS   
   with subprocess.Popen(['hadoop', 'fs', '-cat', sys.argv[1]], stdout=subprocess.PIPE).stdout as f:
      for line in f:
         row, value = line.strip().split('\t')
         v[row] = float(value)
   
   diff = 0
   
   # Read the second file from HDFS
   with subprocess.Popen(['hadoop', 'fs', '-cat', sys.argv[2]], stdout=subprocess.PIPE).stdout as f:
      for line in f:
         row, value = line.strip().split('\t')
         
         try:
            diff += abs(v[row] - float(value))
         except KeyError:
            diff += float(value)
   
   # If the amount of difference exceeds our threshold, exit with 1
   if diff < float(sys.argv[3]):
      exit(1)
   else:
      exit(0)

if __name__ == "__main__":
   main()
