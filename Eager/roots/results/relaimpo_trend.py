import argparse
import numpy
import pyRserve
import sys

def compute_importance(vectors):
    conn = pyRserve.connect()
    try:
        conn.eval('library(\'relaimpo\')', void=True)
        conn.eval('df <- data.frame()', void=True)
        size = 0
        results = {}
        for i in range(len(vectors)):
            v = vectors[i]
            conn.r.x = v
            conn.eval('df <- rbind(df,x)', void=True)
            size += 1
            if size == 1:
                col_names = map(lambda x: 'API' + str(x), range(len(v) - 1))
                col_names.append('Total')
                conn.r.df_names = col_names
                conn.eval('names(df) <- df_names', void=True)
            if size > len(v):
                results[i] = compute_importance_internal(conn)
            else:
                results[i] = 'Insufficient Data'
        return results
    finally:
        conn.close()

def compute_importance_internal(conn):
    try:
        conn.eval('model <- lm(Total ~ ., data=df)', void=True)
        conn.eval('rankings <- calc.relimp(model, type=c(\'lmg\'))', void=True)
        rankings = conn.eval('rankings$lmg')
        result = []
        for i in range(len(rankings)):
            result.append(rankings[i])
        return result
    except Exception as ex:
        return -1

def parse_line(line):
    index = line.index('vector: [') + 9
    vector = line[index:line.index(']', index)]
    return map(lambda x: float(x.strip()), vector.split(','))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Simulates relative importance based bottleneck identification on a Roots trace log.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    args = parser.parse_args()
    if not args.file:
        print 'File argument is required'
        sys.exit(1)

    add_data = False
    vectors = []
    with open(args.file, 'r') as fp:
        for line in fp:
            if 'RelativeImportanceBasedFinder Response time vector' in line:
                vectors.append(parse_line(line.strip()))

    results = compute_importance(vectors)
    for i in range(len(results[0])):
        print '\nRelative importance trend for API', i
        for result in results:
            print result[i]
