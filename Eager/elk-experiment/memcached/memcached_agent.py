from memcached_stats import MemcachedStats

import httplib
import json
import time

def report_to_elasticsearch(host, port, data):
  server = httplib.HTTPConnection(host, port)
  headers = {'Content-type': 'application/json'}
  server.request('POST', '/datastore-memcache/appscale', json.dumps(data), headers)
  response = server.getresponse()
  print 'Operation completed with status:', response.status

if __name__ == '__main__':
  mem = MemcachedStats()
  result = { 'timestamp' : int(time.time() * 1000) }
  data = mem.stats()
  for k,v in data.items():
    try:
      result[k] = int(v)
      continue
    except ValueError:
      pass
    try:
      result[k] = float(v)
    except ValueError:
      result[k] = v
  report_to_elasticsearch('128.111.179.159', 9200, result)
