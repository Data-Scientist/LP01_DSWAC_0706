#!/usr/bin/python
# coding=utf8

import sys

BETA = 0.8

def main():
  if len(sys.argv) < 2:
    sys.stderr.write("Missing args: %s\n" % ":".join(sys.argv))
    sys.exit(1)

  
  # These are the known labels in the data and hence the sources of rank
  with open(sys.argv[1]) as f:
    sources = set(f.read().splitlines())

  # Calculate contribution from the teleport set
  # http://i.stanford.edu/~ullman/mmds/ch5.pdf
  # when there are dead ends, the sum of the components of v may be less than 1, but it will never reach 0
  teleport = (1.0 - BETA) / float(len(sources))
  
  # For every row of data, sum it up, multiply by BETA and
  # add the teleport contribution
  last = None
  sum = 0.0

  for line in sys.stdin:
    row, value = line.strip().split("\t")

    if row != last:
      if last != None:
        dump(last, sum, sources, teleport)

      last = row
      sum = 0.0
          
    # Add the product to the sum
    sum += float(value)

  # Print the last value
  dump(last, sum, sources, teleport)
  
  # For any source row that we didn't see data for,
  # print just the teleport contribution
  # output rows that have a teleport contribution but no contribution from the adjacency matrix
  for row in sources:
    print "%s\t%.20f" % (row, teleport)

"""
Print the sum for the row times BETA, with the teleport
contribution added if it's a source row.
"""
def dump(last, sum, sources, teleport):
  # make sure you only add the teleport contribution to rows that should get it
  if last in sources:
    print "%s\t%.20f" % (last, sum * BETA + teleport)
    sources.remove(last)
  else:
    print "%s\t%.20f" % (last, sum * BETA)

if __name__ == '__main__':
  main()
