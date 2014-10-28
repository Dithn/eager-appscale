package main

import (
	"flag"
	"fmt"
	"math"
	"math/big"
)

var samples int64
var step int64
var quantile float64

var cache map[int64]*big.Int

func init() {
	flag.Int64Var(&samples, "n", 100, "Number of samples in the distribution")
	flag.Float64Var(&quantile, "q", 0.95, "Quantile to calculate")
	flag.Int64Var(&step, "s", 1, "Step size for increasing k")
	cache = make(map[int64]*big.Int)
}

func main() {
	flag.Parse()
	fmt.Printf("Samples = %d; Quantile = %f\n", samples, quantile)
	var k int64
	for k = 1; k <= samples; k+=step {
		p := prob(samples, k, quantile)
		fmt.Println(k, p)
		if p < 0.01 {
			break
		}
	}
}

func prob(n, k int64, q float64) float64 {
	var j int64
	var sum float64
	for j = 0; j <= k; j++ {
		a,b := new(big.Rat), new(big.Rat)
		a.SetFloat64(math.Pow(1.0 - q, float64(j)))
		b.SetFloat64(math.Pow(q, float64(n - j)))
		c := comb(n,j)
		c = c.Mul(c,a)
		c = c.Mul(c,b)
		f, _ := c.Float64()
		sum += f
	}
	return 1.0 - sum
}

func comb(n, k int64) *big.Rat {
	a := factorial(n)
	b := factorial(k)
	c := factorial(n - k)
	denom := b.Mul(b,c)
	result := new(big.Rat)
	return result.SetInt(a.Div(a, denom))
}

func factorial(n int64) *big.Int {
	val, ok := cache[n]
	if ok {
		return big.NewInt(0).SetBits(val.Bits())
	}

	result := big.NewInt(1)
	if n == 0 || n == 1 {
		return result
	}

	var i int64
	for i = 1; i <= n; i++ {
		result = result.Mul(result, big.NewInt(i))
	}
	cache[n] = result
	return big.NewInt(0).SetBits(result.Bits())
}
