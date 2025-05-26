package org.qcmg.sig;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;

public class CompareRatiosTest {
	
	
	@Test
	public void testCompareRatios() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		AtomicIntegerArray oldPositionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp1, new int[]{5, 10});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		float[] oldResults = compareRatiosOld(f1Ratios, f2Ratios, 20, oldPositionsUsed);
		
		
		assertEquals(20, results.length);
		assertEquals(20, oldResults.length);
		for (int i = 0 ; i < 10 ; i++) {
			assertEquals(0.0f, results[i],0.01);
			assertEquals(1, positionsUsed.get(i));
			assertEquals(0.0f, oldResults[i],0.01);
			assertEquals(1, oldPositionsUsed.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			assertEquals(0.0f, results[i],0.01);
			assertEquals(0, positionsUsed.get(i));
			assertEquals(0.0f, oldResults[i],0.01);
			assertEquals(0, oldPositionsUsed.get(i));
		}
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp1, new int[]{10, 10});
		positionsUsed = new AtomicIntegerArray(20); 
		results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		assertEquals(20, results.length);
		for (int i = 0 ; i < 10 ; i++) {
			assertEquals(0.5f, results[i],0.01);
			assertEquals(1, positionsUsed.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			assertEquals(0.0f, results[i],0.01);
			assertEquals(0, positionsUsed.get(i));
		}
	}
	@Test
	public void testCompareRatiosMisMatch() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		ChrPosition cp2 = ChrPointPosition.valueOf("chr2", 12345);
		
		f1Ratios.put(cp1, new int[]{5, 10});
		f2Ratios.put(cp2, new int[]{5, 10});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		assertEquals(20, results.length);
		for (int i = 0 ; i < 20 ; i++) {
			assertEquals(0.0f, results[i],0.01);
			assertEquals(0, positionsUsed.get(i));
		}
	}
	
	@Test
	public void testCompareRatiosLowCoverage() {
		Map<ChrPosition, int[]> f1Ratios = new HashMap<>();
		Map<ChrPosition, int[]> f2Ratios = new HashMap<>();
		AtomicIntegerArray positionsUsed = new AtomicIntegerArray(20); 
		ChrPosition cp1 = ChrPointPosition.valueOf("chr1", 12345);
		
		f1Ratios.put(cp1, new int[]{0, 0});
		f2Ratios.put(cp1, new int[]{0, 0});
		
		float[] results = compareRatios(f1Ratios, f2Ratios, 20, positionsUsed);
		
		assertEquals(20, results.length);
		for (int i = 0 ; i < 20 ; i++) {
			assertEquals(0.0f, results[i],0.01);
			assertEquals(0, positionsUsed.get(i));
		}
	}
	
	
	public static float[] compareRatios(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int minCoverage, AtomicIntegerArray noOfPositionsUsed) {
		
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
				noOfPositionsUsed.incrementAndGet(i);
			}
		}
		
		return results;
	}
	
	public static float[] compareRatiosOld(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int maxCoverage, AtomicIntegerArray noOfPositionsUsed) {
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
				noOfPositionsUsed.incrementAndGet(i);
			}
		}
		
		return totalDifference;
	}

}
