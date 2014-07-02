#!/usr/bin/python
# coding=utf8

import sys

def main():
   # Read all lines from stdin
   for line in sys.stdin:
      key, value = line.strip().split('\t')
      items = value.split(',')
      
      # Emit every item in the set paired with the user ID
      for item in items:
         print "%s\t%s" % (item, key)

if __name__ == '__main__':
   main()
