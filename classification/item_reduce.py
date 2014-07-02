#!/usr/bin/python

import sys

def main():
   last = None
   
   # Read all lines from stdin
   for line in sys.stdin:
      item, user = line.strip().split('\t')
      
      if item != last:
         if last != None:
            # Emit the previous key
            print "%s\t%s" % (last, ','.join(users))
            
         last = item
         users = set()

      users.add(user)

   # Emit the last key
   print "%s\t%s" % (last, ','.join(users))

if __name__ == '__main__':
   main()
