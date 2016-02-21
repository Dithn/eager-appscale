import sys

if __name__ == '__main__':
    count = 1
    for i in (1,2,3,4):
        fh = open('out{0}.txt'.format(i), 'r')
        lines = fh.readlines()
        fh.close()
        for line in lines:
            line = line.replace('\n', '')
            if line.startswith('  -->'):
                print line
            else:
                index = line.index(' ')
                print '{0} {1}'.format(count, line[index + 1:])
                count += 1
            sys.stdout.flush()
