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
	private static final int FACTORIAL_CACHE_SIZE = 1024 * 32;
	
	private ConcurrentMap<Integer, BigDecimal> cache ;
	private ConcurrentMap<String, BigDecimal> binomialCoefficientCache;	
	private double [] logFactorialCache = new double[FACTORIAL_CACHE_SIZE];

	public FisherExact(boolean useCach) { 
		if(useCach) {
			this.cache = new ConcurrentHashMap<Integer, BigDecimal>();
			this.binomialCoefficientCache = new ConcurrentHashMap<String, BigDecimal>();
		}
		
		// populate logFactorialCache
		Arrays.fill(logFactorialCache, 0.0);		
	}
	
	/*
	 * default set cach to true
	 */
	public FisherExact() { this(true); }
	
	public BigDecimal factorialBigDecimal(final int number) {
		if (cache == null) {return null; }
		
		if (cache.containsKey(number)) {
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
				 
		cache.put(number,result);		
		return result;
	}
	
	
	public double exact(int a, int b, int c, int d) {
		BigDecimal aFactorial = factorialBigDecimal(a);
		BigDecimal bFactorial = factorialBigDecimal(b);
		BigDecimal cFactorial = factorialBigDecimal(c);
		BigDecimal dFactorial = factorialBigDecimal(d);
				
		BigDecimal abFactorial = factorialBigDecimal(a+b);
		BigDecimal cdFactorial = factorialBigDecimal(c+d);
		BigDecimal acFactorial = factorialBigDecimal(a+c);
		BigDecimal bdFactorial = factorialBigDecimal(b+d);
		
		BigDecimal top = abFactorial.multiply(cdFactorial).multiply(acFactorial).multiply(bdFactorial);
		BigDecimal bottom = aFactorial.multiply(bFactorial).multiply(cFactorial).multiply(dFactorial).multiply(factorialBigDecimal(a+b+c+d));
		return top.divide(bottom, MathContext.DECIMAL128).doubleValue();
	}
	
	public double exact2(int a, int b, int c, int d) {
		
		BigDecimal top1 = binomialCoefficient((a+b), a);
		BigDecimal top2 = binomialCoefficient((c+d), c);
		BigDecimal bottom = binomialCoefficient((a+b+c+d), a+c);
		
		BigDecimal top = top1.multiply(top2);
		
		BigDecimal result = top.divide(bottom, MathContext.DECIMAL128);
		
		return result.doubleValue();		
	}	
	
	public double getLogFactorial(int i) {
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
	
	public double exactMath(int a, int b, int c, int d) {
		double logFactorialA = getLogFactorial(a);
		double logFactorialB = getLogFactorial(b);
		double logFactorialC = getLogFactorial(c);
		double logFactorialD = getLogFactorial(d);		
		
		double logFactorialAB = getLogFactorial(a+b);
		double logFactorialCD = getLogFactorial(c+d);
		double logFactorialAC = getLogFactorial(a+c);
		double logFactorialBD = getLogFactorial(b+d);
		
		double top = logFactorialAB + logFactorialCD + logFactorialAC + logFactorialBD;
		double bottom = logFactorialA + logFactorialB + logFactorialC + logFactorialD + getLogFactorial(a+b+c+d);
		
		double result = FastMath.exp(top - bottom);
		
		return result;
		
	}
	
	private BigDecimal binomialCoefficient(int n, int k) {
		if (binomialCoefficientCache == null) { return null; }
		
		String key = n + ":" + k;
		// check cache to see if we have an existing entry for this binomial coefficient
		BigDecimal result = binomialCoefficientCache.get(key);
		
		// create a new entry if not exists
		if (result == null) {			 		
			BigDecimal top = factorialBigDecimal(n);
			BigDecimal bottom1 = factorialBigDecimal(k);
			BigDecimal bottom2 = factorialBigDecimal(n-k);		
			result = top.divide(bottom1.multiply(bottom2), MathContext.DECIMAL128);			 
			binomialCoefficientCache.put(key, result);			 
		}
		
		return result;
	}
	

	public static double getTwoTailedFET(final int a, final int b, final int c, final int d) {
		return getTwoTailedFET( a, b, c, d, true );
	}
	
	public static double getTwoTailedFET(final int a, final int b, final int c, final int d, boolean useCach) {
		
		// shortcuts - if c and d are zero, return 1
		if (0 == c && 0 == d) return 1.0;

		FisherExact fish = new FisherExact(useCach);
		final double pCutoff = fish.exact(a, b, c, d);
		double pValue = pCutoff;
		// get disproportion value
		double disproportionCutoff = fish.getDisproportion(a, b, c, d);
		
		int tempA = a;
		int tempB = b;
		int tempC = c;
		int tempD = d;
		// now get LHS values that are higher than cutoff
		while (--tempA >= 0 && --tempD >= 0) {
			tempB++;
			tempC++;
			double tempDisproportion = fish.getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fish.exact(tempA, tempB, tempC, tempD);
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
			double tempDisproportion = fish.getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fish.exact(tempA, tempB, tempC, tempD);
			}
		}
		
		// done
		return pValue;
	}
	
	double getDisproportion(int a, int b, int c, int d) {
		int aAndb = a+b;
		int cAndd = c+d;
		double lhs = (double)a / aAndb;
		double rhs = (double)c / cAndd;
		return Math.abs(lhs - rhs);
	}

	
	public static double getTwoTailedFETMath(int a, int b, int c, int d) {
		// shortcuts - if c and d are zero, return 1
		if (0 == c && 0 == d) return 1.0;

		FisherExact fish = new FisherExact(true);
		final double pCutoff = fish.exactMath(a, b, c, d);
		double pValue = pCutoff;
		// get disproportion value
		double disproportionCutoff = fish.getDisproportion(a, b, c, d);
		
		int tempA = a;
		int tempB = b;
		int tempC = c;
		int tempD = d;
		// now get LHS values that are higher than cutoff
		while (--tempA >= 0 && --tempD >= 0) {
			tempB++;
			tempC++;
			double tempDisproportion = fish.getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fish.exactMath(tempA, tempB, tempC, tempD);
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
			double tempDisproportion = fish.getDisproportion(tempA, tempB, tempC, tempD);
			if (tempDisproportion >= disproportionCutoff) {
				// add to p value
				pValue += fish.exactMath(tempA, tempB, tempC, tempD);
			}
		}
		
		// done
		return pValue;
	}
	
	public static long factorial(int number) {
		long result = 1;
		while (number > 1) {
			result *= number--;
		}
		return result;
	}
}
