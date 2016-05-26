import argparse
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Extracts response time vector data from Roots logs.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    args = parser.parse_args()
    if not args.file:
        print 'File argument is required'
        sys.exit(1)

    with open(args.file, 'r') as fp:
        for line in fp:
            start = line.index(': [') + 3
            end = line.index(']', start)
            numbers = [float(x) for x in line[start:end].split(',')]
            total = 0
            last = len(numbers) - 1
            for i in range(last):
                total += numbers[i]
                print numbers[i],
            print numbers[last] - total, numbers[last]
            
            
    
    
