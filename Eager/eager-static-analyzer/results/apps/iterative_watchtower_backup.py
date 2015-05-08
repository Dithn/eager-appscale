import json
import os
import sys

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: iterative_watchtower_backup.py <watchtower_url>'
        sys.exit(1)
    url = sys.argv[1]
    print 'Backing up from', url
    
    round = 0
    start = -1
    total = 0
    limit = 10000
    global_max = -1
    global_min = float("inf")
    while True:
        print 'Downloading batch: {0}'.format(round + 1)
        file_name = 'WT_BACKUP_{0}.txt'.format(round)
        cmd = 'curl -o {0} "{1}/backup?start={2}&limit={3}"'.format(file_name, url, start, limit)
        os.system(cmd)
        fh = open(file_name, 'r')
        data = json.load(fh)
        fh.close()
        max_key = -1
        for key_str in data.keys():
            key = long(key_str)
            if key > max_key:
                max_key = key
            if key > global_max:
                global_max = key
            if key < global_min:
                global_min = key
        total += len(data)
        if len(data) < limit:
            break
        round += 1
        start = max_key
        print '\n'
        
    print 'Downloaded {0} data points'.format(total)
    print 'From {0} to {1}'.format(global_min, global_max)
            
        
        
