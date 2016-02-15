package org.qcmg.sig;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;

public class CompareRatiosTest {
	
	
	@Test
	public void testCompareRatios() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<ChrPosition, int[]>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		AtomicIntegerArray oldPositionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp1, new int[]{5, 10});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		float[] oldResults = compareRatiosOld(f1Ratios, f2Ratios, 20, oldPositionsUsed);
		
		
		Assert.assertEquals(20, results.length);
		Assert.assertEquals(20, oldResults.length);
		for (int i = 0 ; i < 10 ; i++) {
			Assert.assertEquals(0.0f, results[i]);
			Assert.assertEquals(1, positionsUsed.get(i));
			Assert.assertEquals(0.0f, oldResults[i]);
			Assert.assertEquals(1, oldPositionsUsed.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			Assert.assertEquals(0.0f, results[i]);
			Assert.assertEquals(0, positionsUsed.get(i));
			Assert.assertEquals(0.0f, oldResults[i]);
			Assert.assertEquals(0, oldPositionsUsed.get(i));
		}
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp1, new int[]{10, 10});
		positionsUsed = new AtomicIntegerArray(20); 
		results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		Assert.assertEquals(20, results.length);
		for (int i = 0 ; i < 10 ; i++) {
			Assert.assertEquals(0.5f, results[i]);
			Assert.assertEquals(1, positionsUsed.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			Assert.assertEquals(0.0f, results[i]);
			Assert.assertEquals(0, positionsUsed.get(i));
		}
	}
	@Test
	public void testCompareRatiosMisMatch() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<ChrPosition, int[]>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		ChrPosition cp2 = ChrPointPosition.valueOf("chr2", 12345);
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp2, new int[]{5, 10});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		Assert.assertEquals(20, results.length);
		for (int i = 0 ; i < 20 ; i++) {
			Assert.assertEquals(0.0f, results[i]);
			Assert.assertEquals(0, positionsUsed.get(i));
		}
	}
	
	@Test
	public void testCompareRatiosLowCoverage() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<ChrPosition, int[]>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		
		f1Ratios.put(cp1, new int[]{0, 0});
		f2Ratios.put(cp1, new int[]{0, 0});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		Assert.assertEquals(20, results.length);
		for (int i = 0 ; i < 20 ; i++) {
			Assert.assertEquals(0.0f, results[i]);
			Assert.assertEquals(0, positionsUsed.get(i));
		}
	}
	
	
	public static float[] compareRatios(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int minCoverage, AtomicIntegerArray noOfPOsitionsUsed) {
		
		float[] results = new float[minCoverage];
		
		for (Entry<ChrPosition, int[]> file1RatiosEntry : file1Ratios.entrySet()) {
			
			// if coverage is zero, skip
			final int[] file1Ratio = file1RatiosEntry.getValue();
			final int f1TotalCount = file1Ratio[1];
			if (f1TotalCount == 0) continue;
			
			// get corresponding entry from file 2 array
			// if null, skip
			final int[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
			if (file2Ratio == null) continue;
			// if coverage is zero, skip
			final int f2TotalCount = file2Ratio[1];
			if (f2TotalCount == 0) continue;
			
			// have both positions with non-zero coverage - calculate diff
			final int f2NonRefCount = file2Ratio[0];
			final int f1NonRefCount = file1Ratio[0];
			float diffAtPos = Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
			
			int minCov = Math.min(minCoverage, Math.min(f1TotalCount, f2TotalCount));
			
			for (int i = 0 ; i < minCov ; i++) {
				results[i] += diffAtPos;
				noOfPOsitionsUsed.incrementAndGet(i);
			}
		}
		
		return results;
		
		
		
		
//		public static float compareRatios(final String s1, final String s2,
//				final Map<ChrPosition, int[]> file1Ratios,
//				final Map<ChrPosition, int[]> file2Ratios, final int minCoverage) {
//		float totalDifference = 0f;
//		
//		for (Entry<ChrPosition, int[]> file1RatiosEntry : file1Ratios.entrySet()) {
//			final int[] file1Ratio = file1RatiosEntry.getValue();
//			final int f1TotalCount = file1Ratio[1];
//			// both total counts must be above the minCoverage value
//			if (f1TotalCount < minCoverage) continue;
//			
//			final int[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
//			if (file2Ratio == null) continue;
//			
//			// first entry in array is the non-ref count, the second is the total count
//			final int f2TotalCount = file2Ratio[1];
//			if (f2TotalCount < minCoverage) continue;
//			
//			final int f2NonRefCount = file2Ratio[0];
//			final int f1NonRefCount = file1Ratio[0];
//			
//			noOfPOsitionsUsed.incrementAndGet();
//			
//			totalDifference += Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
//		}
//		
//		return totalDifference;
	}
	public static float[] compareRatiosOld(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int maxCoverage, AtomicIntegerArray noOfPOsitionsUsed) {
//		public static float compareRatios(final String s1, final String s2,
//				final Map<ChrPosition, int[]> file1Ratios,
//				final Map<ChrPosition, int[]> file2Ratios, final int minCoverage) {
		float [] totalDifference = new float[maxCoverage];
		
		for (Entry<ChrPosition, int[]> file1RatiosEntry : file1Ratios.entrySet()) {
			final int[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
			if (file2Ratio == null) continue;
			
			final int[] file1Ratio = file1RatiosEntry.getValue();
			final int f1TotalCount = file1Ratio[1];
			final int f2TotalCount = file2Ratio[1];
			
			final int minCoverageAcrossTheBoard = Math.min(Math.min(f1TotalCount, f2TotalCount), maxCoverage);
			
			if (minCoverageAcrossTheBoard == 0) continue;

			final int f2NonRefCount = file2Ratio[0];
			final int f1NonRefCount = file1Ratio[0];
			float differenceAtThisPosition = Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
			
			for (int i = 0 ; i < minCoverageAcrossTheBoard ; i++) {
				totalDifference[i] += differenceAtThisPosition;
				noOfPOsitionsUsed.incrementAndGet(i);
			}
//			
//			for (int i = maxCoverage ; i > 0 ; i--) {
//				// both total counts must be above the minCoverage value
//				if (f1TotalCount < i) continue;
//				
//				// only calculate differenceAtThisPosition if it is zero, as this will be the same regardless of coverage
//				if (differenceAtThisPosition == 0f)
//					differenceAtThisPosition = Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
//				
//			}
		}
		
		return totalDifference;
	}

}
