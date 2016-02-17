import sys

if __name__ == '__main__':
    col = int(sys.argv[2])
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()

    numbers = map(lambda x: int(x.strip().split()[col]), lines[1:])
    filtered = filter(lambda x: x > 1000, numbers)
    print 'Total numbers:', len(numbers)
    print 'Altered numbers:', len(filtered)
    print 'Modified proportion:', float(len(filtered))/len(numbers)
