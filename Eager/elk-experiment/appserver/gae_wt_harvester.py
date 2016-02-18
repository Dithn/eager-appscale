import argparse
import json
import random
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Harvests Watchtower SDK call execution time information from GAE backups.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    args = parser.parse_args()

    if not args.file:
        print 'File argument is required'
        sys.exit(1)

    fp = open(args.file, 'r')
    records = json.load(fp)
    fp.close()

    sample_key = random.choice(records.keys())
    calls = sorted(records[sample_key].keys())
    for call in calls:
        print call,
    print 'Total'
    
    sorted_keys = sorted(records.keys())
    for key in sorted_keys:
        data = records[key]
        for call in calls:
            print data[call],
        print sum(data.values())
        
