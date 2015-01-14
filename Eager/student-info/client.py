import httplib
import urllib
import sys
import time
import datetime
from urlparse import urlparse

class InvocationResult:
    def __init__(self, output, error, duration):
        self.output = output
        self.error = error
        self.duration = duration

def get_all_students(url):
    result = urlparse(url)
    conn = httplib.HTTPConnection(result.netloc)
    try:
        conn.request("GET", result.path)
        response = conn.getresponse()
        if response.status != 200:
            return InvocationResult(None, 'Unexpected status code', 0)
        else:
            return InvocationResult(url, None, get_time(result.netloc).output)
    finally:
        conn.close()
        
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
                    return InvocationResult(header[1], None, get_time(result.netloc).output)
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
            return InvocationResult(url, None, get_time(result.netloc).output)
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
            return InvocationResult(url, None, get_time(result.netloc).output)
    finally:
        conn.close()

def fatal(msg):
    print msg
    sys.exit(1)

def get_time(netloc):
    conn = httplib.HTTPConnection(netloc)
    try:
        conn.request("GET", "/resources/timing")
        response = conn.getresponse()
        if response.status != 200:
            return InvocationResult(None, 'Unexpected status code', 0)
        else:
            data = response.read()
            return InvocationResult(int(data), None, 0)
    finally:
        conn.close()

def timestamp():
    return int((datetime.datetime.utcnow() - datetime.datetime(1970, 1, 1)).total_seconds() * 1000)
    
if __name__ == '__main__':
    root_url = sys.argv[1]
    print 'Benchmarking URL:', root_url

    resp = get_all_students(root_url)
    if resp.error is None:
        print timestamp(), 'GetAll', resp.output, resp.duration
    else:
        fatal(resp.error)
    
    resp = add_student(root_url)
    if resp.error is None:
        print timestamp(), 'Create', resp.output, resp.duration
    else:
        fatal(resp.error)

    time.sleep(0.1)
    resp = get_student(resp.output)
    if resp.error is None:
        print timestamp(), 'Get', resp.output, resp.duration
    else:
        fatal(resp.error)

    time.sleep(0.1)
    resp = delete_student(resp.output)
    if resp.error is None:
        print timestamp(), 'Delete', resp.output, resp.duration
    else:
        fatal(resp.error)
