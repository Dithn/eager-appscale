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
          'terms' : {
            'field' : 'requestId.raw',
            'size': 0
          },
          'aggs': {
            'group_by_service': {
              'terms': {
                'field': 'service.raw'
              },
              'aggs': {
                'service_time': {
                  'sum': {
                    'field': 'elapsed'
                  }
                }
              }
            }
          }
        }
      }
    }

    path = '/{0}/apicall/_search?pretty'.format(index)
    conn = httplib.HTTPConnection(server, port)
    conn.request('POST', path, json.dumps(query))
    response = conn.getresponse()
    print 'Operation completed with status', response.status
    data = response.read()
    print data
    conn.close()
    

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyzes execution time of cloud services.')
    parser.add_argument('--server', '-s', dest='server', default='128.111.179.159')
    parser.add_argument('--port', '-p', type=int, dest='port', default=9200)
    parser.add_argument('--index', '-i', dest='index', default='appscale-internal')
    parser.add_argument('--app', '-a', dest='app', default='watchtower')
    args = parser.parse_args()
    get_request_info(args.server, args.port, args.index, args.app, 3600 * 1000)
