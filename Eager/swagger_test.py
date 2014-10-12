import json
import sys
import time

from apimgt.swagger import *

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print 'Usage: swagger_test.py src tgt'
        sys.exit(1)

    with open(sys.argv[1]) as data_file:
        src = json.load(data_file)
    with open(sys.argv[2]) as data_file:
        target = json.load(data_file)

    iterations = 1000
    start = time.time()
    for i in range(iterations):
        is_api_compatible(src, target)
    end = time.time()
    elapsed = (end - start) * 1000
    
    print iterations, 'iterations'
    print 'Total time elapsed:', elapsed, 'ms'
    print 'Time per round:', elapsed / iterations, 'ms'
