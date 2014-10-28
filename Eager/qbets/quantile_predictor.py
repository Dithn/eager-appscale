import math
from decimal import Decimal
import time

cache = {}

def factorial(n):
    return Decimal(math.factorial(n))

def nCk(n,k):
    if cache.has_key((n,k)):
        return cache[(n,k)]
    val = factorial(n) / (factorial(k) * factorial(n - k))
    cache[(n,k)] = val
    return val

def prob(n, q, k):
    sum = Decimal(0.0)
    for j in range(0, k+1):
        sum += nCk(n,j) * (Decimal(1 - q) ** Decimal(j)) * (Decimal(q) ** Decimal(n - j))
    return Decimal(1.0) - sum

if __name__ == '__main__':
    n = 10000
    q = 0.95
    for k in range(1, n+1, 1):
        p = prob(n, q, k)
        print k, p
        if p < 0.01:
            break
