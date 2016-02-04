import argparse
import httplib
import json
import re
import time

class SDKCall:
    def __init__(self, source):
        self.service = source['service']
        self.operation = source['operation']

def get_digit(delta_str):
    return int(delta_str[:len(delta_str)-1])

def parse_time_delta(delta_str):
    pattern = re.compile('^(\d+[dhms]\s*)+$')
    if pattern.match(delta_str):
        segments = re.split('(\d+[dhms]\s*)', delta_str)
        segments = map(lambda s: s.strip(), filter(lambda s: len(s) > 0, segments))
        result = 0
        for segment in segments:
            if segment.endswith('s'):
                result += get_digit(segment) * 1000
            elif segment.endswith('m'):
                result += get_digit(segment) * 1000 * 60
            elif segment.endswith('h'):
                result += get_digit(segment) * 1000 * 60 * 60
            elif segment.endswith('d'):
                result += get_digit(segment) * 1000 * 60 * 60 * 24
            else:
                raise ValueError('Invalid time delta string ' + segment)
        return result
    else:
        raise ValueError('Invalid time delta string ' + delta_str)

def make_http_call(server, port, path, payload):
    conn = httplib.HTTPConnection(server, port)
    conn.request('POST', path, json.dumps(payload))
    response = conn.getresponse()
    data = response.read()
    conn.close()
    if response.status != 200:
        error_message = 'Server returned unexpected status: {0}\n{1}'.format(response.status, data)
        raise RuntimeError(error_message)
    return json.loads(data)
    
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
      'size' : 1500,
      'sort': { 'timestamp' : { 'order' : 'asc'}}
    }

    path = '/{0}/apicall/_search?scroll=1m'.format(index)
    output = make_http_call(server, port, path, query)
    total_hits = output['hits']['total']
    scroll_id = output['_scroll_id']
    result = {}
    received = 0
    while True:
        requests = output['hits']['hits']
        for req in requests:
            source = req['_source']
            req_id = source['requestId']
            if not result.has_key(req_id):
                result[req_id] = []
            result[req_id].append(SDKCall(source))
            received += 1
        if received < total_hits:
            query = {
                'scroll' : '1m',
                'scroll_id' : scroll_id
            }
            output = make_http_call(server, port, '/_search/scroll', query)
        else:
            break
    return result

def path_to_string(path):
    path_str = ''
    for sdk_call in path:
        if path_str:
            path_str += ', '
        path_str += sdk_call.service + ':' + sdk_call.operation
    return path_str

def invert_map(map):
    invert = {}
    for k,v in map.items():
        if not invert.has_key(v):
            invert[v] = []
        invert[v].append(k)
    return invert

def print_request_list(requests):
    line = ''
    for i in range(len(requests)):
        if i % 3 == 0:
            print line
            line = ''
        line += requests[i] + '  '
    if line:
        print line
                       
if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Identifies paths of execution through an app from SDK calls.')
    parser.add_argument('--server', '-s', dest='server', default='128.111.179.159')
    parser.add_argument('--port', '-p', type=int, dest='port', default=9200)
    parser.add_argument('--index', '-i', dest='index', default='appscale-internal')
    parser.add_argument('--app', '-a', dest='app', default='watchtower')
    parser.add_argument('--time_window', '-t', dest='time_window', default='1h')
    args = parser.parse_args()
    time_window_ms = parse_time_delta(args.time_window)
    requests = get_request_info(args.server, args.port, args.index, args.app, time_window_ms)
    request_paths = {k: path_to_string(v) for k,v in requests.items()}
    path_requests = invert_map(request_paths)
    index = 1
    for k,v in path_requests.items():
        print 'Path', index
        print '========='
        print k, '\n'
        print '[requests]', len(v)
        print_request_list(v)
        print
        index += 1
        
