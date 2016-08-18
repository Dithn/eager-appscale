import numpy
import sys

def in_fault_window(line):
    line = line.strip()
    timestamp = line.split()[1]
    time_segments = timestamp.split(':')
    hour = int(time_segments[0])
    minute = int(time_segments[1])
    return hour % 3 == 0 and minute >= 30 and minute <= 45

def print_summary(data):
    print 'Data points:', len(data)
    print 'Min:', min(data)
    print 'Max:', max(data)
    print 'Mean:', numpy.mean(data)
    print 'Std Dev:', numpy.std(data)
    print '95th Percentile:', numpy.percentile(data, 95.0)
    print '99th Percentile:', numpy.percentile(data, 99.0)

if __name__ == '__main__':
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()
    data = []

    output = open('vector.tmp', 'w')
    for line in lines:
        line = line.strip()
        if 'Benchmark result' in line:
            if not in_fault_window(line):
                segments = line.strip().split()
                value = int(segments[-2])
                data.append(value)
                output.write(str(value) + '\n')

    output.flush()
    output.close()
    
    print 'All data points summary'
    print '======================='
    print_summary(data)

    print
    print 'Filterd (< 1000) data points summary'
    print '===================================='
    print_summary(filter(lambda x: x < 1000, data))

    mean = numpy.mean(data)
    sd = numpy.std(data)
    print
    print 'Filtered (< mean + 2sd) data points summary'
    print '==========================================='
    print_summary(filter(lambda x: x < mean + 2 * sd, data))
