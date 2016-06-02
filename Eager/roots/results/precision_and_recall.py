import argparse
import sys

def parse_line(line):
    index = line.index('(check): [') + 10
    vector = line[index:line.index(']', index)]
    return map(lambda x: int(x.strip()), vector.split(','))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Calculates precision and recall for percentile based bottleneck identification on a Roots trace log.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--limit', '-l', dest='limit', type=int, default=-1)
    args = parser.parse_args()
    if not args.file:
        print 'File argument is required'
        sys.exit(1)
    if args.limit <= 0:
        print 'Limit argument is required and must be positive'
        sys.exit(1)

    anomaly = False
    merge = False
    anomalous_events = 0.0
    identified_events = 0.0
    correct_identifications = 0.0
    with open(args.file, 'r') as fp:
        for line in fp:
            if 'PercentileBasedFinder Anomaly' in line:
                if merge:
                    continue
                merge = True
                identified_events += 1
                if anomaly:
                    correct_identifications += 1
            else:
                merge = False
            anomaly = False
            if '(check)' in line:
                vector = parse_line(line.strip())
                total = sum(vector)
                if total > args.limit:
                    anomalous_events += 1
                    anomaly = True

    print 'True anomalies:', anomalous_events
    print 'Identified anomalies:', identified_events
    print 'Correct identifications:', correct_identifications
    print 'Precision:', (correct_identifications * 100.0/identified_events)
    print 'Recall:', (correct_identifications * 100.0/anomalous_events)
