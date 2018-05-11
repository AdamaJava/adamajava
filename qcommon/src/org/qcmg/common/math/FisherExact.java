/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;

public class FisherExact {
	
	public static ConcurrentMap<Integer, BigDecimal> cache = new ConcurrentHashMap<Integer, BigDecimal>();
	public static ConcurrentMap<String, BigDecimal> binomialCoefficientCache = new ConcurrentHashMap<String, BigDecimal>();
	
	public static final int FACTORIAL_CACHE_SIZE = 1024 * 32;
	public static double [] logFactorialCache = new double[FACTORIAL_CACHE_SIZE];
	static {
		// populate logFactorialCache
		Arrays.fill(logFactorialCache, 0.0);
	}
	
	public static boolean USE_CACHE = true;
	
	public static long factorial(int number) {
		long result = 1;
		while (number > 1) {
			result *= number--;
		}
		return result;
	}
	public static BigDecimal factorialBigDecimal(final int number) {
		
		if (USE_CACHE && cache.containsKey(number)) {
			return cache.get(number);
		}
		
		BigDecimal result = null;
		
		// avoid multiple object creation if we don't need to
		// if number is <= 20 use longs and convert to BD at the end
		if (number <= 20)  {
			result = new BigDecimal(factorial(number));
		} else {
		
			result = BigDecimal.ONE;
			int tempNumber = number;
			while (tempNumber > 1) {
				result = result.multiply(new BigDecimal(tempNumber--));
			}
		}
		
		if (USE_CACHE) {
			// add to cache
//			System.out.println("adding to cache: " + number + " : " + result.doubleValue());
			cache.putIfAbsent(number,result);
		}
		
		return result;
	}
	
	
	public static double fisherExact(int a, int b, int c, int d) {
		BigDecimal aFactorial = factorialBigDecimal(a);
		BigDecimal bFactorial = factorialBigDecimal(b);
		BigDecimal cFactorial = factorialBigDecimal(c);
		BigDecimal dFactorial = factorialBigDecimal(d);
		
//		System.out.println("aFactorial: " + a + " : " + aFactorial.longValueExact());
//		System.out.println("bFactorial: " + b + " : "+ bFactorial.longValueExact());
//		System.out.println("cFactorial: " + c + " : "+ cFactorial.longValueExact());
//		System.out.println("dFactorial: " + d + " : "+ dFactorial.longValueExact());
		
		BigDecimal abFactorial = factorialBigDecimal(a+b);
		BigDecimal cdFactorial = factorialBigDecimal(c+d);
		BigDecimal acFactorial = factorialBigDecimal(a+c);
		BigDecimal bdFactorial = factorialBigDecimal(b+d);
//		System.out.println("abFactorial: " + (a + b) + " : "+ abFactorial.longValueExact());
//		System.out.println("cdFactorial: " + (c+d) + " : "+ cdFactorial.toString());
//		System.out.println("acFactorial: " + (a+c) + " : "+ acFactorial.longValueExact());
//		System.out.println("bdFactorial: " + (b+d) + " : "+ bdFactorial.toString());
		
		BigDecimal top = abFactorial.multiply(cdFactorial).multiply(acFactorial).multiply(bdFactorial);
		BigDecimal bottom = aFactorial.multiply(bFactorial).multiply(cFactorial).multiply(dFactorial).multiply(factorialBigDecimal(a+b+c+d));
		return top.divide(bottom, MathContext.DECIMAL128).doubleValue();
	}
	
	public static double getLogFactorial(int i) {
		if (i < FACTORIAL_CACHE_SIZE) {
			double result = logFactorialCache[i];
			if (result == 0.0) {
				result = ArithmeticUtils.factorialLog(i);
				logFactorialCache[i] = result;
			}
			return result;
		}
		return ArithmeticUtils.factorialLog(i);
	}
	
	public static double fisherExactMath(int a, int b, int c, int d) {
		double logFactorialA = getLogFactorial(a);
		double logFactorialB = getLogFactorial(b);
		double logFactorialC = getLogFactorial(c);
		double logFactorialD = getLogFactorial(d);
//		
//		System.out.println("logFactorialA: " + logFactorialA + ", logFactorialB: " + logFactorialB + ", logFactorialC: " + logFactorialC + ", logFactorialD: " + logFactorialD);
//		
		
		
		double logFactorialAB = getLogFactorial(a+b);
		double logFactorialCD = getLogFactorial(c+d);
		double logFactorialAC = getLogFactorial(a+c);
		double logFactorialBD = getLogFactorial(b+d);
		
		double top = logFactorialAB + logFactorialCD + logFactorialAC + logFactorialBD;
		double bottom = logFactorialA + logFactorialB + logFactorialC + logFactorialD + getLogFactorial(a+b+c+d);
		
		double result = FastMath.exp(top - bottom);
		
//		System.out.println("top: " + top + ", bottom: " + bottom + ", result: " + result);
		return result;
		
	}
	
	public static BigDecimal binomialCoefficient(int n, int k) {
		String key = n + ":" + k;
		// check cache to see if we have an existing entry for this binomial coefficient
		BigDecimal result = null;
		
		if (USE_CACHE && binomialCoefficientCache.containsKey(key)) {
			result = binomialCoefficientCache.get(key);
//			System.out.println("found in biCoef cache!!");
		} else {
//		System.out.println("n:" + n +", k: " + k);
		
			BigDecimal top = factorialBigDecimal(n);
			BigDecimal bottom1 = factorialBigDecimal(k);
			BigDecimal bottom2 = factorialBigDecimal(n-k);
		
//		System.out.println("bc: top: " + top.toString());
//		System.out.println("bc: bottom1: " + bottom1.toString());
//		System.out.println("bc: bottom2: " + bottom2.toString());
			result = top.divide(bottom1.multiply(bottom2), MathContext.DECIMAL128);
			
			if (USE_CACHE) {
//				System.out.println("adding to binomial cache: " + key + " : " + result.doubleValue());
				binomialCoefficientCache.putIfAbsent(key, result);
			}
		}
		
		return result;
		
		
//		BigDecimal top = new BigDecimal(n).pow(k);
//		
//		return top.divide(new BigDecimal(factorial(k)), MathContext.DECIMAL128);
	}
	
	public static double fisherExact2(int a, int b, int c, int d) {
		
		BigDecimal top1 = binomialCoefficient((a+b), a);
		BigDecimal top2 = binomialCoefficient((c+d), c);
		BigDecimal bottom = binomialCoefficient((a+b+c+d), a+c);

//		System.out.println("top1: " + top1.toString());
//		System.out.println("top2: " + top2.toString());
//		System.out.println("bottom: " + bottom.toString());
		
		
		BigDecimal top = top1.multiply(top2);
//		System.out.println("top: " + top.toString());
		
		BigDecimal result = top.divide(bottom, MathContext.DECIMAL128);
//		System.out.println("result: " + result.toString());
		return result.doubleValue();
		
	}
	
	
	public static double getTwoTailedFET(final int a, final int b, final int c, final int d) {
		
		// shortcuts - if c and d are zero, return 1
		if (0 == c && 0 == d) return 1.0;

		
		final double pCutoff = fisherExact(a, b, c, d);
		double pValue = pCutoff;
		// get disproportion value
		double disproportionCutoff = getDisproportion(a, b, c, d);
		
		int tempA = a;
		int tempB = b;
		int tempC = c;
		int tempD = d;
		// now get LHS values that are higher than cutoff
		while (--tempA >= 0 && --tempD >= 0) {
			tempB++;
			tempC++;
			double tempDisproportion = getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fisherExact(tempA, tempB, tempC, tempD);
			}
		}
		
		// and now the RHS
		tempA = a;
		tempB = b;
		tempC = c;
		tempD = d;
		// now get RHS values that are higher than cutoff
		while (--tempC >= 0 && --tempB >= 0) {
			tempA++;
			tempD++;
			double tempDisproportion = getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fisherExact(tempA, tempB, tempC, tempD);
			}
		}
		
		// done
		return pValue;
	}
	
	public static double getDisproportion(int a, int b, int c, int d) {
		int aAndb = a+b;
		int cAndd = c+d;
		double lhs = (double)a / aAndb;
		double rhs = (double)c / cAndd;
		return Math.abs(lhs - rhs);
	}

	public static double getTwoTailedFETMath(int a, int b, int c, int d) {
		// shortcuts - if c and d are zero, return 1
		if (0 == c && 0 == d) return 1.0;

		
		final double pCutoff = fisherExactMath(a, b, c, d);
		double pValue = pCutoff;
		// get disproportion value
		double disproportionCutoff = getDisproportion(a, b, c, d);
		
		int tempA = a;
		int tempB = b;
		int tempC = c;
		int tempD = d;
		// now get LHS values that are higher than cutoff
		while (--tempA >= 0 && --tempD >= 0) {
			tempB++;
			tempC++;
			double tempDisproportion = getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fisherExactMath(tempA, tempB, tempC, tempD);
			}
		}
		
		// and now the RHS
		tempA = a;
		tempB = b;
		tempC = c;
		tempD = d;
		// now get RHS values that are higher than cutoff
		while (--tempC >= 0 && --tempB >= 0) {
			tempA++;
			tempD++;
			double tempDisproportion = getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fisherExactMath(tempA, tempB, tempC, tempD);
			}
		}
		
		// done
		return pValue;
	}
}
