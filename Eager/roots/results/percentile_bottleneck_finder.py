import argparse
import numpy
import sys

def learn_data(line, history):
    if '(learn)' not in line:
        return
    index = line.index('(learn): [') + 10
    vector = line[index:line.index(']', index)]
    numbers = vector.split(',')
    for i in range(len(numbers)):
        values = history.get(i, [])
        values.append(int(numbers[i].strip()))
        history[i] = values

def compute_percentiles(history, percentile):
    percentiles = []
    for i in range(len(history)):
        value = numpy.percentile(history.get(i), percentile)
        percentiles.append(value)
    return percentiles

def check_data(line, percentiles, limit):
    if '(check)' not in line:
        return
    index = line.index('(check): [') + 10
    vector = line[index:line.index(']', index)]
    numbers = map(lambda x: int(x.strip()), vector.split(','))
    total = sum(numbers)
    if total > limit:
        print line
        for i in range(len(numbers)):
            if numbers[i] > percentiles[i]:
                print '\tAnomalous execution at index {0}: {1} > {2}'.format(i, numbers[i], percentiles[i])
    

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Simulates percentile based bottleneck identification on a Roots trace log.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--limit', '-l', dest='limit', type=int, default=-1)
    parser.add_argument('--percentile', '-p', dest='percentile', type=float, default=95.0)
    args = parser.parse_args()
    if not args.file:
        print 'File argument is required'
        sys.exit(1)
    if args.limit <= 0:
        print 'Limit argument is required and must be positive'
        sys.exit(1)
    if args.percentile <= 0 or args.percentile >= 100:
        print 'Percentile must be in the interval (0,100)'
        sys.exit(1)

    STATE_STANDBY = 0
    STATE_LEARN = 1
    STATE_CHECK = 2
        
    state = STATE_STANDBY
    history = {}
    percentiles = []
    with open(args.file, 'r') as fp:
        for line in fp:
            if state == STATE_STANDBY:
                if '(learn)' in line:
                    state = STATE_LEARN            
            if state == STATE_LEARN:
                learn_data(line.strip(), history)
                if '(check)' in line:
                    state = STATE_CHECK
                    percentiles = compute_percentiles(history, args.percentile)
                    print 'Computed percentiles:', percentiles                
            if state == STATE_CHECK:
                check_data(line.strip(), percentiles, args.limit)
                if 'Detected' in line:
                    state = STATE_STANDBY
                    history = {}
                    percentiles = []
                    print
                
            
        
