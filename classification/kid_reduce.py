#!/usr/bin/python
# encoding=utf8

import sys

def main():
  current = None
  
  # Read all lines from stdin
  for line in sys.stdin:
    # Decompose the compound key and value
    key, value = line.strip().split('\t')
    user, start, end = key.split(',')
    kid, itemstr = value.split(',', 1)

    # Create a data record for this user
    data = {}
    data['user'] = user
    data['kid'] = str2bool(kid)
    data['items'] = set(itemstr.split(","))

    if not current:
      current = data
    else:
      if current['user'] != user or \
        (data['kid'] != None and current['kid'] != None and data['kid'] != current['kid']):
        # If this is a new user or we have new information about whether the user is a kid
        # that conflicts with what we knew before, then print the current record and start
        # a new record.
        dump(current)
        current = data
      else:
        if data['kid'] != None and current['kid'] == None:
          # If we just found out whether the user is a kid, store that
          current['kid'] = data['kid']
          
        # Store the items
        current['items'].update(data['items'])
  
  # Print the record for the last user.
  dump(current)

"""
Emit the data record
"""
def dump(data):
  # Remove any empty items
  try:
    data['items'].remove('')
  except KeyError:
    pass

  # If there are still items in the record, emit it
  if len(data['items']) > 0:
    # Annotate the session ID if we know the user is an adult or child
    if data['kid'] == True:
      data['user'] += 'k'
    elif data['kid'] == False:
      data['user'] += 'a'

    print "%s\t%s" % (data['user'], ",".join(data['items']))

"""
Translate a string into a boolean, but return None if the string doesn't parse.
"""
def str2bool(str):
  b = None
  
  if str.lower() == 'true':
    b = True
  elif str.lower() == 'false':
    b = False
    
  return b

if __name__ == '__main__':
  main()
