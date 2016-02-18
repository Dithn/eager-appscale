import argparse
import numpy as np
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Detects anomalies in columnar data by computing percentiles.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--column', '-c', type=int, dest='col', default=0)
    parser.add_argument('--percentile', '-p', type=int, dest='perc', default=95)
    args = parser.parse_args()

    if not args.file:
        print 'File argument is required'
        sys.exit(1)
    if args.col < 0:
        print 'Column index must not negative'
        sys.exit(1)
    if args.perc < 1 or args.perc > 99:
        print 'Percentile must be in the interval [1,99]'
        sys.exit(1)

    fp = open(args.file, 'r')
    lines = fp.readlines()
    fp.close()

    numbers = map(lambda line: int(line.strip().split()[args.col]), lines[1:])
    limit = np.percentile(numbers, args.perc)
    print '{0}th Percentile: {1}'.format(args.perc, limit)
    print '[anomaly] row value'
    for i in range(len(numbers)):
        if numbers[i] > limit:
            print '[anomaly] {0} {1}'.format(i, numbers[i])
