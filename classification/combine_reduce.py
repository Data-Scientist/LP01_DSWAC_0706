#!/usr/bin/python

import re
import sys

def main():
   last = None
   last_root = None
   sum = 0.0
   p = re.compile(r'^(\d{7,8})[ak]?$')
   
   # Read all the lines from stdin
   for line in sys.stdin:
      key, value = line.strip().split('\t')
      m = p.match(key)
      
      # Ignore anything that's not a user ID
      if m:
         if key != last:
            # Dump the previous user ID's label if it's not
            # the same real user as the current user.
            if last != None and last_root != m.group(1):
               dump(last, sum)

            last = key
            last_root = m.group(1)
            sum = 0.0
         
         sum += float(value)
      
   dump(last, sum)

def dump(key, sum):
   # sum is between -1 and 1, so adding one and truncating gets us 0 or 1.
   # We want ties to go to adults, though.
   if sum != 0:
      print "%s\t%d" % (key, int(sum + 1))
   else:
      print "%s\t0" % key

if __name__ == "__main__":
    main()
