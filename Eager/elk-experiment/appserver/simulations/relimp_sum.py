import sys

if __name__ == '__main__':
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()

    minimum = 1.0
    for line in lines[1:]:
        total = sum([float(x) for x in line.split()[2:]])
        print total
        if total < minimum:
            minimum = total
    print
    print 'Smallest total relimp: {0}'.format(minimum)
        
