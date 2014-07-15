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
        # v[col]: 表示当前col结点的享有的访问概率，col中存储值即可能是用户ID，
        #   也可能是Content ID
        # len(rows): 表示col结点的outcoming links的数量，1/len(rows)也就是走
        #    'col -> row' 这条边的概率。

        # 这段代码看上去好像用矩阵M的每列，去和向量distribution vector做点积，
        # 其实不然，这里相乘完之后，以row为key把数据交给reducer处理，
        # reducer收到的数据是自动排序过的，这样所有同row的数据会聚合在一起，
        # 再相加，其实还是相当于用M的每行去和v做点积。
        print "%s\t%.20f" % (row, v[col] / float(len(rows)))
      except KeyError:
        # KeyError equates to a zero, which we don't need to output.
        pass

if __name__ == '__main__':
  main()
