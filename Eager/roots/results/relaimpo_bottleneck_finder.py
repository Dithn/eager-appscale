import argparse
import numpy
import pyRserve
import sys

def parse_line(line):
    index = line.index('vector: [') + 9
    vector = line[index:line.index(']', index)]
    return map(lambda x: float(x.strip()), vector.split(','))

def compute_importance(vectors, limit, percentile=99.0):
    anomalies, results = compute_importance_step1(vectors, limit)
    full_results = compute_importance_step2(vectors)
    print anomalies
    for i in sorted(full_results.keys()):
        print i, '    ', results.get(i, 'Anomaly'), '   ', full_results[i]

    training_data = [ v for k,v in results.items() if k >= 60 and isinstance(v, list) ]
    for api in range(len(vectors[0]) - 1):
        data = [ i[api] for i in training_data ]
        p = numpy.percentile(data, percentile)
        print 'API{0} Percentile: {1}'.format(api, p)
        for i in anomalies:
            val = full_results[i][api]
            s = ''
            if val > p:
                s = '***'
            print '\t{0} {1} {2}'.format(i, val, s)
    sys.exit(1)

def compute_importance_step1(vectors, limit):
    conn = pyRserve.connect()
    try:
        conn.eval('library(\'relaimpo\')', void=True)
        conn.eval('df <- data.frame()', void=True)
        size = 0
        anomalies = []
        results = {}
        for i in range(len(vectors)):
            v = vectors[i]
            if v[-1] <= limit:
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
            else:
                anomalies.append(i)
        return anomalies, results
    finally:
        conn.close()

def compute_importance_step2(vectors):
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

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Simulates relative importance based bottleneck identification on a Roots trace log.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    parser.add_argument('--limit', '-l', dest='limit', type=int, default=-1)
    args = parser.parse_args()
    if not args.file:
        print 'File argument is required'
        sys.exit(1)
    if args.limit <= 0:
        print 'Limit argument is required and must be positive'
        sys.exit(1)

    add_data = False
    vectors = []
    with open(args.file, 'r') as fp:
        for line in fp:
            if 'RelativeImportanceBasedFinder Received ' in line:
                if vectors:
                    print '\nData points:', len(vectors)
                    compute_importance(vectors, args.limit)
                    vectors = []
            if 'RelativeImportanceBasedFinder Response time vector' in line:
                vectors.append(parse_line(line.strip()))
