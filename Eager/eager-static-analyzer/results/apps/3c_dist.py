import sys
import numpy

if __name__ == '__main__':
    v_path = sys.argv[1] + '_exp.txt'
    print 'Using input file:', v_path

    bm_path = sys.argv[1] + '.txt'
    print 'Benchmark data extracted from:', bm_path
    print

    fh = open(bm_path, 'r')
    lines = fh.readlines()
    fh.close()
    last_line = ''
    for line in lines:
        if line.strip():
            last_line = line
    end_of_trace = long(last_line.split()[0])
    fh = open(v_path, 'r')
    lines = fh.readlines()
    fh.close()
    
    values = []
    na_count = 0
    for line in lines:
        if line.startswith('[validate]'):
            segments = line.split()
            v = segments[4]
            if v == '3c':
                continue
            elif v == 'N/A':
                ts = long(segments[1])
                values.append(float(end_of_trace - ts)/1000.0/3600.0)
                na_count += 1
            else:
                values.append(float(v)/3600.0)

    print 'Items:', len(values)
    print 'RightCensoredCount:', na_count
    print 'EndOfTrace:', end_of_trace
    print 'Mean:', numpy.mean(values, axis=0)
    print 'StdDev:', numpy.std(values, axis=0)
    print '0.05P:', numpy.percentile(values, 5)
    print '0.95P:', numpy.percentile(values, 95)
