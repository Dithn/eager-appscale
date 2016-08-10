import os
import numpy
import sys

def parse_line(line):
    key = line[0:16]
    v_start = line.index('vector: [') + 9
    v_end = line.index(']', v_start)
    vector = line[v_start:v_end-1]
    values = map(lambda x: float(x.strip()), vector.split(','))
    local = values[-1] - sum(values[0:-1])
    return key, local

def analyze(data):
    limit = numpy.mean(data) + 1.65 * numpy.std(data)
    samples = 0
    fp = open('t_test.tmp', 'w')
    for value in data:
        if value <= limit:
            fp.write(str(value) + '\n')
            samples += 1
    fp.flush()
    fp.close()
    print 'Sample size:', samples
    os.system('Rscript t_test.R t_test.tmp')
    os.remove('t_test.tmp')

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usage: t_test_analysis.py <file>'
        sys.exit(1)

    grouped_data = {}
    with open(sys.argv[1], 'r') as f:
        for line in f:
            if 'RelativeImportanceBasedFinder Response time vector' in line:
                key, local = parse_line(line.strip())
                data = grouped_data.get(key, [])
                data.append(local)
                grouped_data[key] = data
    print 'Loaded {0} data sets'.format(len(grouped_data))
    sorted_keys = sorted(grouped_data.keys())
    for k in sorted_keys:
        print 'Analyzing data from', k
        print '==================================='
        analyze(grouped_data[k])
        print
    
