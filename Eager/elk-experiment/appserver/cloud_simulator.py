import argparse
import numpy
import random

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

class FaultInjector:
    def __init__(self, config):
        segments = config.split(',')
        self.start = int(segments[0])
        self.end = int(segments[1])
        self.type = segments[2]
        if self.start > self.end:
            raise Exception('Fault injector time interval invalid')
        if self.type not in ('A','M'):
            raise Exception('Invalid fault injection method: {0}'.format(self.type))
        self.probability = float(segments[3])
        self.factor = float(segments[4])

    def mutate(self, time, value):
        if self.start <= time <= self.end:
            if random.random() < self.probability:
                if self.type == 'A':
                    return value + self.factor
                elif self.type == 'M':
                    return value * self.factor
        return value

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
        self.time = 0
        self.fault_injectors = []
        print 'Configured service: {0}'.format(self.name)

    def add_fault_injector(self, config):
        self.fault_injectors.append(FaultInjector(config))
        
    def invoke(self):
        value = max(0, self.generator.next())
        for fi in self.fault_injectors:
            value = fi.mutate(self.time, value)
        self.time += 1
        return value

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
        elif line.strip():
            if services:
                services[-1].add_fault_injector(line.strip())
            else:
                raise Exception('Fault injector configuration must follow a service configuration')

    print 'Starting simulation...'
    print
    for service in services:
        print service.name,
    print 'Total'
    for i in range(iterations):
        total = 0.0
        for service in services:
            current = service.invoke()
            print current,
            total += current
        if extra_gen:
            total += extra_gen.invoke()
        print total
