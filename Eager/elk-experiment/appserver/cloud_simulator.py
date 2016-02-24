import argparse
import numpy

class UniformGenerator:
    def __init__(self, lowest, highest):
        self.lowest = lowest
        self.highest = highest

    def next(self):
        return numpy.random.uniform(self.lowest, self.highest)

class NormalGenerator:
    def __init__(self, mean, stdev):
        self.mean = mean
        self.stdev = stdev

    def next(self):
        return numpy.random.normal(self.mean, self.stdev)

class CloudService:
    def __init__(self, config):
        if config.startswith('[service]'):
            config = config[9:].strip()
        else:
            raise Exception('Invalid service configuration')
        segments = config.split(',')
        self.name = segments[0]
        self.type = segments[1]
        if self.type == 'uniform':
            self.generator = UniformGenerator(int(segments[2]), int(segments[3]))
        elif self.type == 'normal':
            self.generator = NormalGenerator(int(segments[2]), int(segments[3]))
        else:
            raise Exception('Unsupported generator type: {0}'.format(self.type))
        print 'Configured service: {0}'.format(self.name)

    def invoke(self):
        return max(0, self.generator.next())

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Simulates the operation of a PaaS-hosted application.')
    parser.add_argument('--file', '-f', dest='file', default=None)
    args = parser.parse_args()

    if not args.file:
        print 'File argument is required'
        sys.exit(1)

    print 'Reading configuration from {0}'.format(args.file)
    fp = open(args.file, 'r')
    lines = fp.readlines()
    fp.close()

    iterations = int(lines[0].strip())
    services = []
    extra_gen = None
    for line in lines[1:]:
        if line.startswith('[service]'):
            services.append(CloudService(line.strip()))
        elif line.startswith('[extra]'):
            extra_gen = CloudService(line.replace('[extra]', '[service]'))

    print 'Starting simulation...'
    print 'Time',
    for service in services:
        print service.name,
    print 'Total'
    for i in range(iterations):
        print i,
        total = 0.0
        for service in services:
            current = service.invoke()
            print current,
            total += current
        if extra_gen:
            total += extra_gen.invoke()
        print total
