import httplib
import urllib
import sys
from urlparse import urlparse

class InvocationResult:
    def __init__(self, output, error, duration):
        self.output = output
        self.error = error
        self.duration = duration

def add_student(url):
    result = urlparse(url)
    params = urllib.urlencode({'firstName':'Peter', 'lastName':'Parker'})
    headers = {"Content-type": "application/x-www-form-urlencoded"}
    conn = httplib.HTTPConnection(result.netloc)
    try:
        conn.request("POST", result.path, params, headers)
        response = conn.getresponse()
        if response.status != 201:
            return InvocationResult(None, 'Unexpected status code', 0)
        else:
            headers = response.getheaders()
            for header in headers:
                if header[0] == 'location':
                    return InvocationResult(header[1], None, 0)
            return InvocationResult(None, 'No location header', 0)
    finally:
        conn.close()

def get_student(url):
    result = urlparse(url)
    conn = httplib.HTTPConnection(result.netloc)
    try:
        conn.request("GET", result.path)
        response = conn.getresponse()
        if response.status != 200:
            return InvocationResult(None, 'Unexpected status code', 0)
        else:
            return InvocationResult(url, None, 0)
    finally:
        conn.close()

def delete_student(url):
    result = urlparse(url)
    conn = httplib.HTTPConnection(result.netloc)
    conn.request("DELETE", result.path)
    try:
        response = conn.getresponse()
        if response.status != 200:
            return InvocationResult(None, 'Unexpected status code', 0)
        else:
            return InvocationResult(url, None, 0)
    finally:
        conn.close()

def fatal(msg):
    print msg
    sys.exit(1)

def get_time(netloc):
    conn = httplib.HTTPConnection(netloc)
    conn.request("GET", "/timing")
    response = conn.getresponse()
    if response.status != 200:
        return InvocationResult(None, 'Unexpected status code', 0)
    else:
        data = response.read()
        return InvocationResult(int(data), None, 0)
    
if __name__ == '__main__':
    root_url = 'http://128.111.179.151:8080/resources/students'
    resp = add_student(root_url)
    if resp.error is None:
        print 'Created student resource: ', resp.output
    else:
        fatal(resp.error)
    
    resp = get_student(resp.output)
    if resp.error is None:
        print 'Student resource read successfully'
    else:
        fatal(resp.error)

    resp = delete_student(resp.output)
    if resp.error is None:
        print 'Student resource deleted'
    else:
        fatal(resp.error)
