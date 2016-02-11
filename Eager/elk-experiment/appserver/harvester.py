import argparse
import path_finder as pf
import random

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Harvests SDK call execution time information for an app.')
    parser.add_argument('--server', '-s', dest='server', default='128.111.179.159')
    parser.add_argument('--port', '-p', type=int, dest='port', default=9200)
    parser.add_argument('--index', '-i', dest='index', default='appscale-internal')
    parser.add_argument('--app', '-a', dest='app', default='watchtower')
    parser.add_argument('--time_window', '-t', dest='time_window', default='1h')
    parser.add_argument('--filtered_services', '-fs', nargs='+', dest='filtered_services', default=[])
    parser.add_argument('--mock_total', '-m', dest='mock_total', action='store_true')
    args = parser.parse_args()
    
    time_window_ms = pf.parse_time_delta(args.time_window)
    requests = pf.get_request_info(args.server, args.port, args.index, args.app, time_window_ms)
    path = requests[random.sample(requests, 1)[0]]
    path_str = pf.path_to_string(path)

    
    for call in path:
        if call.service not in args.filtered_services:
            print '{0}:{1}'.format(call.service, call.operation),
    if args.mock_total:
        print 'Total',
    print '\n',
    
    for k, v in requests.items():
        if path_str != pf.path_to_string(v):
            continue
        total = 0
        for call in v:
            if call.service not in args.filtered_services:
                print '{0:4d}'.format(call.exectime),
                total += call.exectime
        if args.mock_total:
            print total + random.randint(1, 15),
        print '\n',
    
