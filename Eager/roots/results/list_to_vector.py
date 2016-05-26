import re
import sys

def parse_line(line):
    if re.search(r'\s', line):
        numbers = line.split()
        return [ int(number) for number in numbers ]
    else:
        return int(line)

if __name__ == '__main__':
    input = sys.stdin.read()
    lines = input.split('\n')
    vector = [ parse_line(line.strip()) for line in lines if line ]
    multi_item_input = [ item for item in vector if isinstance(item, list) ]
    if multi_item_input:
        for item in vector:
            print str(item).translate(None, '[,]')
    else:
        print vector
