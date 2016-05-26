import sys

if __name__ == '__main__':
    input = sys.stdin.read()
    lines = input.split('\n')
    vector = [ int(line) for line in lines if line ]
    print vector
