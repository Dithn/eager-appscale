import datetime
import httplib
import numpy
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
    windows = {
        'create': [10, 11, 20, 21, 30, 31],
        'update': [40, 41, 50, 51, 0, 1]
    }
    while True:
        now = datetime.datetime.now()
        value = 0
        choices = [0, 1, 2]
        if now.minute in windows.get('create', []):
            # CREATE
            value = numpy.random.choice(choices, p=[0.05, 0.9, 0.05])
        elif now.minute in windows.get('update', []):
            # UPDATE
            value = numpy.random.choice(choices, p=[0.05, 0.05, 0.9])
        else:
            # READ
            value = numpy.random.choice(choices, p=[0.9, 0.05, 0.05])
        run(now, server, value, read_id)
	time.sleep(1)        
        
