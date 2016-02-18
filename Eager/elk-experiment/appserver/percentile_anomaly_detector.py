import argparse
import numpy as np
import sys

def rolling_detector(numbers, percentile):
    window = []
    limit = -1
    for i in range(len(numbers)):
        n = numbers[i]
        if limit > 0 and n > limit:
            print '[anomaly] {0} {1} {2}'.format(limit, i, n)
        window.append(n)
        limit = np.percentile(window, percentile)

def fixed_detector(numbers, percentile):
    limit = np.percentile(numbers, args.perc)
    for i in range(len(numbers)):
        if numbers[i] > limit:
            print '[anomaly] {0} {1} {2}'.format(limit, i, numbers[i])

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Detects anomalies in columnar data by computing percentiles.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--column', '-c', type=int, dest='col', default=0)
    parser.add_argument('--percentile', '-p', type=int, dest='perc', default=95)
    parser.add_argument('--rolling', '-r', action='store_true', dest='rolling', default=False)
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
    print '[anomaly] percentile row value'
    if args.rolling:
        rolling_detector(numbers, args.perc)
    else:
        fixed_detector(numbers, args.perc)
