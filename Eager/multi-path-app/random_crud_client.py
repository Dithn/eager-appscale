import datetime
import httplib
import numpy
import random
import time
import urllib

def make_http_request(server, path, data):
    conn = httplib.HTTPConnection(server)
    params = urllib.urlencode(data)
    headers = {'Content-Type' : 'application/x-www-form-urlencoded'}
    conn.request('POST', path, params, headers)
    resp = conn.getresponse()
    status, output = resp.status, resp.read()
    conn.close()
    return status, output

def run(now, server, option, read_id):
    timestamp = str(now.hour) + ':' + str(now.minute)
    if option == 0:
        print 'R', timestamp, make_http_request(server, '/student', {'operation':'read', 'id':read_id})
    elif option == 1:
        print 'C', timestamp, make_http_request(server, '/student', {'operation':'create', 'firstName':'Foo', 'lastName':'Bar'})
    elif option == 2:
        print 'U', timestamp, make_http_request(server, '/student', {'operation':'update', 'id':read_id})

if __name__ == '__main__':
    server = 'localhost:8080'
    read_id = '943522a1-6ae3-48d4-a757-e35945d70a97'
    write_mode = False
    write_mode_counter = 0
    choices = [0, 1, 2]
    value = 0
    while True:
        now = datetime.datetime.now()
        if write_mode:
            if write_mode_counter == 0:
                print 'W {0}:{1} Entering write mode'.format(now.hour, now.minute)
            value = numpy.random.choice(choices, p=[0.05, 0.05, 0.9])
            write_mode_counter += 1
            if write_mode_counter == 120:
                write_mode_counter = 0
                write_mode = False
        else:
            value = numpy.random.choice(choices, p=[0.9, 0.05, 0.05])
            if random.random() > 0.9995:
                write_mode = True
        run(now, server, value, read_id)
	time.sleep(1)        
        
