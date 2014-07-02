#!/usr/bin/python
# coding=utf8

import json
import sys

def main():
  # Read all lines from stdin
  for line in sys.stdin:
    data = json.loads(line)

    # Collect all items touched
    items = set()
    items.update(data['played'].keys())
    items.update(data['rated'].keys())
    items.update(data['reviewed'].keys())

    # Generate a comma-separated list
    if items:
      itemstr = ','.join(items)
    else:
      itemstr = ','

    # Emit a compound key and compound value
    print "%s,%010d,%010d\t%s,%s" % (data['user'], long(data['start']), long(data['end']), data['kid'], itemstr)
    
if __name__ == '__main__':
  main()
