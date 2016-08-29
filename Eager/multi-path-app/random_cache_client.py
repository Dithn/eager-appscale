import datetime
import httplib
import numpy
import random
import sys
import time
import urllib

def make_http_request(server, path, query):
    conn = httplib.HTTPConnection(server)
    params = urllib.urlencode(query)
    conn.request('GET', path + '?' + params)
    resp = conn.getresponse()
    status, output = resp.status, resp.read()
    conn.close()
    return status, output

def run(now, server, option, id_list):
    timestamp = str(now.hour) + ':' + str(now.minute)
    if option == 0:
        print 'H', timestamp, make_http_request(server, '/caching', {'id':id_list[0]})
    elif option == 1:
        print 'M', timestamp, make_http_request(server, '/caching', {'id': random.sample(id_list[1:], 1)[0]})

if __name__ == '__main__':
    server = 'localhost:8080'
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()

    id_list = map(lambda x: x.strip(), lines)
    write_mode = False
    write_mode_counter = 0
    while True:
        now = datetime.datetime.now()
        value = 0
        choices = [0, 1]
        if write_mode:
            # MISS
            if write_mode_counter == 0:
                print 'W {0}:{1} Entering write mode'.format(now.hour, now.minute)
            value = numpy.random.choice(choices, p=[0.05, 0.95])
            write_mode_counter += 1
            if write_mode_counter == 120:
                print 'W {0}:{1} Exiting write mode'.format(now.hour, now.minute)
                write_mode_counter = 0
                write_mode = False
        else:
            # HIT
            value = numpy.random.choice(choices, p=[0.95, 0.05])
            if random.random() > 0.9995:
                write_mode = True
        run(now, server, value, id_list)
        time.sleep(1)
