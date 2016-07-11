import sys

def handle_event(line):
    segments = line.split()
    id, p, p2, ri = segments[6], int(segments[14]), int(segments[16]), int(segments[18])
    count = 0
    if p == ri:
        count += 1
    if p2 == ri:
        count += 1
    if p == p2:
        count += 1
    print id, '[' + str(count) + ']', ri == p, ri == p2, p == p2, p, p2, ri

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'Usahe: secondary_verifier.py <file>'
        sys.exit(1)
    with open(sys.argv[1], 'r') as f:
        for line in f:
            if 'Secondary' in line:
                handle_event(line.strip())


            
            
