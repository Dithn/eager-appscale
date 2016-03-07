import argparse
import numpy as np
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Detects changes in percentile values in columnar data.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--column', '-c', type=int, dest='col', default=0)
    parser.add_argument('--percentile', '-p', type=int, dest='perc', default=95)
    parser.add_argument('--threshold', '-t', type=int, dest='threshold', default=10)
    parser.add_argument('--mean_threshold', '-m', action='store_true', dest='mean_threshold', default=False)
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
    if args.threshold < 1:
        print 'Threshold must be a positive integer (% value)'
        sys.exit(1)

    fp = open(args.file, 'r')
    lines = fp.readlines()
    fp.close()

    numbers = []
    index = 0
    prev_percentile = -1.0
    prev_mean = -1.0
    print 'index value mean percentile change anomaly'
    for line in lines[1:]:
        n = int(line.strip().split()[args.col])
        numbers.append(n)
        if len(numbers) > 100:
            current_percentile = np.percentile(numbers, args.perc)
            mean = np.mean(numbers)
            change = 0.0
            anomaly = False
            if prev_percentile > 0:
                change = (current_percentile - prev_percentile)*100.0/float(prev_percentile)
                anomaly = change > args.threshold
            if args.mean_threshold and prev_mean > 0:
                anomaly = (current_percentile - prev_percentile) > prev_mean * args.threshold/100.0
            print '{0} {1} {2:.4f} {3:.4f} {4:.4f} {5}'.format(index, n, mean, current_percentile, change, anomaly)
            prev_percentile = current_percentile
            prev_mean = mean
        index += 1
