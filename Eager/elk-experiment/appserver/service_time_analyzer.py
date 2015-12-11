import argparse
import httplib
import json
import time

class RequestInfo:
    pass

def get_request_info(server, port, index, app, time_window):
    start_time = long(time.time() * 1000) - time_window
    filtered_query = {
      'filtered' : {
         'query' : { 'term' : { 'appId' : app }},
         'filter' : { 'range' : { 'timestamp' : { 'gte' : start_time}}}
       }
    }
    query = {
      'query' : filtered_query,
      'sort': { 'timestamp' : { 'order' : 'asc'}},
      'aggs': {
        'group_by_request' : {
          'terms' : { 'field' : 'requestId.raw', 'size': 0, 'order': {'request_timestamp': 'asc'} },
          'aggs': {
            'request_timestamp': {
              'min': { 'field': 'timestamp'}
            },
            'group_by_service': {
              'terms': { 'field': 'service.raw' },
              'aggs': {
                'service_time': {
                  'sum': { 'field': 'elapsed' }
                }
              }
            }
          }
        }
      }
    }

    path = '/{0}/apicall/_search'.format(index)
    conn = httplib.HTTPConnection(server, port)
    conn.request('POST', path, json.dumps(query))
    response = conn.getresponse()
    data = response.read()
    conn.close()
    if response.status != 200:
        print 'Server returned unexpected status', response.status
        print data
        return
    output = json.loads(data)
    requests = output['aggregations']['group_by_request']['buckets']
    service_names = [ 'datastore_v3', 'memcache', 'urlfetch' ]
    if not requests:
        print 'No requests found'
        return
    print 'requestId  datastore_v3 datastore_v3%  memcache memcache%  urlfetch urlfetch%  total'
    for req in requests:
        services = req['group_by_service']['buckets']
        service_times = {}
        for service in services:
            name = service['key']
            value = service['service_time']['value']
            service_times[name] = value
        total = sum(service_times.values())
        record = '{0}  {1}  '.format(req['request_timestamp']['value_as_string'], req['key'])
        for k in service_names:
            value = service_times.get(k, 0.0)
            record += '{0}  ({1:.2f})  '.format(value, (value/total) * 100.0)
        record += '{0}'.format(total)
        print record
        

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyzes execution time of cloud services.')
    parser.add_argument('--server', '-s', dest='server', default='128.111.179.159')
    parser.add_argument('--port', '-p', type=int, dest='port', default=9200)
    parser.add_argument('--index', '-i', dest='index', default='appscale-internal')
    parser.add_argument('--app', '-a', dest='app', default='watchtower')
    args = parser.parse_args()
    get_request_info(args.server, args.port, args.index, args.app, 3600 * 1000)
