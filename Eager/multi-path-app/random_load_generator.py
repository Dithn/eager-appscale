import datetime
import os
import random

if __name__ == '__main__':
    while True:
        now = datetime.datetime.now()
        timestamp = '{0}:{1}:{2}'.format(now.hour, now.minute, now.second)
        total = 0
        rate = 0
        if random.random() < 0.99:
            total = random.normalvariate(500, 10)
            rate = int(total / 60.0)
            print timestamp, 'LOW', total, rate
        else:
            rate = 600
            total = rate * 60 * 4
            print timestamp, 'HIGH', total, rate
        status = os.system('bash load_gen.sh {0} {1}'.format(total, rate))
            
