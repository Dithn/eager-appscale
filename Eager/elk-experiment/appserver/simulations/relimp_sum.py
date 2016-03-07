import sys

if __name__ == '__main__':
    fp = open(sys.argv[1], 'r')
    lines = fp.readlines()
    fp.close()

    for line in lines[1:]:
        print sum([float(x) for x in line.split()[2:]])
        
        
