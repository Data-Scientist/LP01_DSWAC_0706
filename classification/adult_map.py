#!/usr/bin/python
# coding=utf8

import sys

def main():
   if len(sys.argv) < 3:
      sys.stderr.write("Missing args: %s\n" % sys.argv)

   # Calculate conversion factor
   num_adults = float(sys.argv[1])
   num_kids = float(sys.argv[2])
   factor = -num_adults / num_kids
   
   # adult distribution vector
   # Apply the conversion to every record and emit it
   for line in sys.stdin:
      key, value = line.strip().split('\t')

      print "%s\t%.20f" % (key, float(value) * factor)
      
if __name__ == "__main__":
    main()
