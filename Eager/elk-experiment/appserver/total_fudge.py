import argparse
import random

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates a fudged total from columnar data.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    args = parser.parse_args()

    if not args.file:
        print 'File argument is required'
        sys.exit(1)

    fp = open(args.file, 'r')
    lines = fp.readlines()
    fp.close()

    print lines[0],
    for line in lines[1:]:
        total = sum([int(x) for x in line.strip().split()]) + random.randint(0,9)
        print line.strip(), total


    
