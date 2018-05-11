package org.qcmg.common.math;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.math.FisherExact;

public class FisherExactTest {
	
	@Test
	public void testFactorial() {
		assertEquals(1, FisherExact.factorial(0));
		assertEquals(1, FisherExact.factorial(1));
		assertEquals(2, FisherExact.factorial(2));
		assertEquals(6, FisherExact.factorial(3));
		assertEquals(24, FisherExact.factorial(4));
		assertEquals(120, FisherExact.factorial(5));
		assertEquals(720, FisherExact.factorial(6));
		
		// and now some meatier numbers
		assertEquals(2432902008176640000l, FisherExact.factorial(20));
	}
	
	@Test
	public void testFactorialBigDecimal() {
		assertEquals(BigDecimal.ONE, FisherExact.factorialBigDecimal(0));
		assertEquals(BigDecimal.ONE, FisherExact.factorialBigDecimal(1));
		assertEquals(new BigDecimal(2), FisherExact.factorialBigDecimal(2));
		assertEquals(new BigDecimal(6), FisherExact.factorialBigDecimal(3));
		assertEquals(new BigDecimal(24), FisherExact.factorialBigDecimal(4));
		assertEquals(new BigDecimal(120), FisherExact.factorialBigDecimal(5));
		assertEquals(new BigDecimal(720), FisherExact.factorialBigDecimal(6));
		
		// and now some meatier numbers
		assertEquals(new BigDecimal(2432902008176640000l), FisherExact.factorialBigDecimal(20));
	}
	
	@Test
	public void testTwoTailedFET() {
		assertEquals(0.023, FisherExact.getTwoTailedFET(2, 7, 8, 2), 0.0001);		// http://dogsbody.psych.mun.ca/VassarStats/ch8a.html
		assertEquals(0.0476, FisherExact.getTwoTailedFET(5, 0, 1, 4), 0.0001);	// http://mathworld.wolfram.com/FishersExactTest.html
		assertEquals(0.002759, FisherExact.getTwoTailedFET(1, 9, 11, 3), 0.00001);
		assertEquals(true, FisherExact.fisherExact2(1,72,42,24) < 0.00001);
	}
	
	@Test
	public void testTwoTailedFET2() {
		// this example was taken from "Fundamentals of Biostatistics" p341 where they give the answer as 0.748, which is double the single tailed answer of 0.374
		// whereas the answer that the code gives (and that agrees with http://dogsbody.psych.mun.ca/VassarStats/ch8a.html) is 0.6881
		// hmmm
		assertEquals(0.6881, FisherExact.getTwoTailedFET(2, 23, 5, 30), 0.0001);
	}
	
	@Test
	public void testFisherExact() {
		assertEquals(0.001346076, FisherExact.fisherExact(1, 9, 11, 3), 0.00001);
		assertEquals(0.001346076, FisherExact.fisherExactMath(1, 9, 11, 3), 0.00001);
	}
	@Test
	public void testFisherExact2() {
		assertEquals(0.001346076, FisherExact.fisherExact2(1, 9, 11, 3), 0.00001);
	}
	@Test
	public void testFisherExact3() {
		assertEquals(0.182, FisherExact.fisherExact(2,5,3,1), 0.001);
		assertEquals(0.182, FisherExact.fisherExact2(2,5,3,1), 0.001);
	}
	
	@Test
	public void testTwoTailed() {
		assertEquals(0.075344, FisherExact.getTwoTailedFET(27,600,84,1241), 0.001);
		assertEquals(0.075344, FisherExact.getTwoTailedFETMath(27,600,84,1241), 0.001);
	}
	
	@Ignore
	public void testCachePerformance() {
		int noOfLoops = 1000;
		Random random = new Random();
		long start = System.currentTimeMillis();
		double result = 0.0;
		
		for (int i = 0 ; i < noOfLoops ; i++) {
			result += (FisherExact.getTwoTailedFET(random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100)));
		}
		System.out.println("with cache turned on: " + (System.currentTimeMillis() - start) + " - result: " + result);
		System.out.println("cache size: " + FisherExact.cache.size());
		FisherExact.cache.clear();
		
		FisherExact.USE_CACHE = false;
		result = 0.0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			result += (FisherExact.getTwoTailedFET(random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100)));
		}
		System.out.println("with cache turned off: " + (System.currentTimeMillis() - start) + " - result: " + result);
		System.out.println("cache size: " + FisherExact.cache.size());
		
		FisherExact.USE_CACHE = true;
		result = 0.0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			result += (FisherExact.getTwoTailedFET(random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100)));
		}
		System.out.println("with cache turned on: " + (System.currentTimeMillis() - start) + " - result: " + result);
		System.out.println("cache size: " + FisherExact.cache.size());
	}
	
	@Ignore
	public void testLargeNumbersSpeed() {
//		long start = System.currentTimeMillis();
//		double result = FisherExact.getTwoTailedFET(532, 1228, 1246, 2729);
//		System.out.println("time taken: " + (System.currentTimeMillis() - start) + ", result: " + result);
		
		 long start = System.currentTimeMillis();
		double result = FisherExact.getTwoTailedFETMath(1580, 31987, 3217, 52834);
		System.out.println("time taken Math: " + (System.currentTimeMillis() - start) + ", result: " + result);
		
	}

}
