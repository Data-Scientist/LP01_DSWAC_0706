#!/usr/bin/python
# coding=utf8

'''
'''

import sys

def main():
  if len(sys.argv) < 2:
    sys.stderr.write("Missing args: %s\n" % ":".join(sys.argv))
    sys.exit(1)

  # distribution vector
  v = {}

  # Read in the vector
  with open(sys.argv[1]) as f:
    for line in f:
      (key, value) = line.strip().split("\t")
      v[key] = float(value)

  # Now read the matrix from the mapper and do the math
  for line in sys.stdin:
    col, value = line.strip().split("\t")
    rows = value.split(',')

    for row in rows:
      try:
        # Add the product to the sum
        print "%s\t%.20f" % (row, v[col] / float(len(rows)))
      except KeyError:
        # KeyError equates to a zero, which we don't need to output.
        # 这里其实把所有的没有out-links的结点都给丢弃了
        pass

if __name__ == '__main__':
  main()
