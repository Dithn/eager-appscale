import json
import httplib
import subprocess

def get_haproxy_stats():
  return subprocess.check_output(['/root/haproxy_stats.sh'])

def report_to_elasticsearch(host, port, data):
  server = httplib.HTTPConnection(host, port)
  headers = {'Content-type': 'application/json'}
  server.request('POST', '/haproxy/watchtower', data, headers)
  response = server.getresponse()
  print 'Operation completed with status:', response.status

if __name__ == '__main__':
  stats = get_haproxy_stats()
  lines = stats.split('\n')
  data = {'timestamp':int(time.time() * 1000)}
  for line in lines:
    if line.strip() == '':
      continue
    segments = line.split(",")
    if segments[1] == 'FRONTEND':
      data['bytes_in'] = long(segments[8])
      data['bytes_out'] = long(segments[9])
      data['hrsp_1xx'] = int(segments[39]) 
      data['hrsp_2xx'] = int(segments[40])
      data['hrsp_3xx'] = int(segments[41])
      data['hrsp_4xx'] = int(segments[42])
      data['hrsp_5xx'] = int(segments[43])
      data['req_tot'] = int(segments[48])
    elif segments[1] == 'BACKEND':
      data['curr_queue'] = int(segments[2])
  report_to_elasticsearch('128.111.179.159', 9200, json.dumps(data))
