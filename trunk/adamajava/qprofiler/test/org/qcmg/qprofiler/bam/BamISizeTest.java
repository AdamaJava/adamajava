package org.qcmg.qprofiler.bam;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qvisualise.util.QProfilerCollectionsUtils;

public class BamISizeTest {
	private static Random random = new Random();
	
//	@Test
//	public void testParseISize() {
//		BamSummaryReport bsr = new BamSummaryReport();
//		bsr.parseISize(0, "rg1");
//
//		Assert.assertEquals(1, bsr.getISizeLengths().size());
//		Assert.assertEquals(1, bsr.getISizeLengths().get(0).get());
//		
//		bsr.parseISize(0, "rg2");
//		Assert.assertEquals(1, bsr.getISizeLengths().size());
//		Assert.assertEquals(2, bsr.getISizeLengths().get(0).get());
//		
//		// if value is below 50000, will but in buckets of 10, eg. 99 will be in bucket 90
//		bsr.parseISize(99, "rg1");
//		Assert.assertEquals(2, bsr.getISizeLengths().size());
//		Assert.assertEquals(2, bsr.getISizeLengths().get(0).get());
//		Assert.assertEquals(1, bsr.getISizeLengths().get(90).get());
//		
//		bsr.parseISize(40, "rg1");
//		Assert.assertEquals(3, bsr.getISizeLengths().size());
//		Assert.assertEquals(1, bsr.getISizeLengths().get(40).get());
//		
//		bsr.parseISize(31, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(30).get());
//		
//		bsr.parseISize(1234, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(1230).get());
//		
//		bsr.parseISize(49999, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(49990).get());
//		
//		// values over 50000 will be in buckets of 1000000!! eg. 50001 will be in 50000
//		bsr.parseISize(50000, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(50000).get());
//		
//		bsr.parseISize(50001, "rg3");
//		Assert.assertEquals(2, bsr.getISizeLengths().get(50000).get());
//		
//		bsr.parseISize(50010, "rg1");
//		Assert.assertEquals(3, bsr.getISizeLengths().get(50000).get());
//		bsr.parseISize(999999, "rg1");
//		Assert.assertEquals(4, bsr.getISizeLengths().get(50000).get());
//		
//		bsr.parseISize(1000000, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(1000000).get());
//		bsr.parseISize(1050001, "rg1");
//		Assert.assertEquals(2, bsr.getISizeLengths().get(1000000).get());
//		bsr.parseISize(1999999, "rg2");
//		Assert.assertEquals(3, bsr.getISizeLengths().get(1000000).get());
//		
//		bsr.parseISize(2999999, "rg1");
//		Assert.assertEquals(1, bsr.getISizeLengths().get(2000000).get());
//	}
	
//	private IntWrapper getIntWrapper(int value) {
//		return new IntWrapper(value);
//	}
	
	@Test
	public void testNewISizeProcess() {
		int noOfISizes = 1000;
		int maxISizeValue = 201;
		int noOfBins = 3;
		createBinnedMap(noOfISizes, maxISizeValue, noOfBins);
		
		//0 reads, 0 max iSize, 0 bins 
		createBinnedMap(0, 0, 0);
		
		// 0 reads, max iSize of 10, 10 bins
		createBinnedMap(0, 10, 10);
		
		// 1 reads, max iSize of 1, 0 bins
		createBinnedMap(1, 1, 0);
		
		// 1 reads, max iSize of 1, 1 bin
		createBinnedMap(1, 1, 1);
		
		// 1000 reads, max iSize of 1, 1 bin
		createBinnedMap(1000, 1, 1);
		
		// 1 read, max iSize of 10000000, 1 bin
		createBinnedMap(1, 10000000, 1);
		
		// 1 read, max iSize of 10000000, 10 bins
		createBinnedMap(1, 10000000, 10);
		
		// 1 read, max iSize of 10000000, 10000 bins
		createBinnedMap(1, 10000000, 10000);
		
		// 2 read, max iSize of 10000000, 10000 bins
		createBinnedMap(2, 10000000, 10000);
		
		// 100 read, max iSize of 100, 100 bins
		createBinnedMap(100, 100, 100);
		
		// 100000 read, max iSize of 10000, 200 bins
		createBinnedMap(100000, 10000, 200);
		
	}

	private void createBinnedMap(int noOfISizes, int maxISizeValue, int binSize) {
		ConcurrentMap<Integer, AtomicLong> iSizeCount = createInitialMap(noOfISizes, maxISizeValue);
		Map<String, AtomicLong> binnedMap = QProfilerCollectionsUtils.convertMapIntoBinnedMap(iSizeCount, binSize, false);
		
		// check that binnedMap contains only the supplied no of bins
		if (binSize  < 1) {
			Assert.assertTrue(noOfISizes == binnedMap.size() );
//			Assert.assertTrue(noOfISizes > 0 ? 1 == binnedMap.size() : binnedMap.isEmpty());
		} else {
			
			
		}
		// check that the totals of the counts is equal to noOfISize
		int noOfReads = 0;
		for (Map.Entry<String, AtomicLong> mapEntry : binnedMap.entrySet()) {
			noOfReads += mapEntry.getValue().get();
		}
		Assert.assertEquals(noOfISizes, noOfReads);
	}
	
	private ConcurrentMap<Integer, AtomicLong> createInitialMap(int noOfISizes, int maxISizeValue) {
		ConcurrentSkipListMap<Integer, AtomicLong> iSizeCount = new ConcurrentSkipListMap<Integer, AtomicLong>();
		
		for (int i = 0 ; i < noOfISizes; i++) {
			int randomNumber = random.nextInt(maxISizeValue);
			if (i % 2 == 0)
				randomNumber = -randomNumber;
			
			SummaryByCycleUtils.incrementCount(iSizeCount, Math.abs(randomNumber));
		}
		
		// do some basic checking
		Assert.assertTrue( noOfISizes > 0 ? iSizeCount.lastKey() < maxISizeValue : iSizeCount.isEmpty());
		
		int noOfReads = 0;
		for (Map.Entry<Integer, AtomicLong> mapEntry : iSizeCount.entrySet()) {
			noOfReads += mapEntry.getValue().get();
		}
		Assert.assertEquals(noOfISizes, noOfReads);
		
		return iSizeCount;
	}
	

	@Ignore
	public void testNewISizeProcessPOC() {
		ConcurrentMap<Integer, AtomicLong> iSizeCount = new ConcurrentSkipListMap<Integer, AtomicLong>();
		
		// generate 500,000,000 random numbers between -2,000,000 and 2,000,000
		Random r = new Random();
		int noOfISizes = 50000000;
		int maxISizeValue = 200001;
		int noOfBins = 200;
		
		for (int i = 0 ; i < noOfISizes; i++) {
			int randomNumber = r.nextInt(maxISizeValue);
			if (i % 2 == 0) {
				randomNumber = -randomNumber;
			}
			
			if (i % 1000000 == 0) {
				System.out.println("created " + i + " randoms...");
			}
			
			SummaryByCycleUtils.incrementCount(iSizeCount, Math.abs(randomNumber));
		}
		
		Assert.assertFalse(iSizeCount.isEmpty());
		
		// get max number from map
		int maxValue = ((TreeMap<Integer, AtomicLong>) iSizeCount).lastKey();
		System.out.println("maxValue = " + maxValue);
		Assert.assertTrue(maxValue < maxISizeValue);
		
		
		// need to get the number of reads (yeah - we know its 50000000 in this case but we wont in real life..
		int noOfReads = 0;
		for (Map.Entry<Integer, AtomicLong> mapEntry : iSizeCount.entrySet()) {
			noOfReads += mapEntry.getValue().get();
		}
		System.out.println("noOfReads = " + noOfReads);
		Assert.assertEquals(noOfISizes, noOfReads);
		
		// divide by 200 to get bin size
		int binSize = maxValue / noOfBins;
		System.out.println("binSize = " + binSize);
		
		int totalCount = 0;
		
		for (int i = 1 ; i <= noOfBins ; i++) {
			int fromBinSize = i == 1 ? 0 : ((i-1) * binSize) + 1;
			NavigableMap<Integer, AtomicLong> tempMap = ((TreeMap<Integer,AtomicLong>) iSizeCount).subMap(fromBinSize, true,  binSize * i, true);
			//TODO get total count
			// need to loop through the maps in this range and sum the counts.
			int count = 0;
			for (Map.Entry<Integer, AtomicLong> mapEntry : tempMap.entrySet()) {
				count += mapEntry.getValue().get();
			}
			System.out.println("bin number: " + i + ", min value = " + tempMap.firstKey() + "[" 
					+ tempMap.get(tempMap.firstKey()) + "], max value = " + tempMap.lastKey() + "[" 
					+ tempMap.get(tempMap.lastKey()) + "] with count = " + count);
			
			totalCount += count;
		}
		System.out.println("totalCount = " + totalCount);
		Assert.assertEquals(noOfISizes, totalCount);
	}
	
//	@Ignore
//	public void testHist4JISize() {
//		AdaptiveHistogram ah = new AdaptiveHistogram();
//		
//		for (int i = 0 ; i < 10000000; i++) {
//			int randomNumber = random.nextInt(1000000);
//			ah.addValue(randomNumber);
//		}
//		
//		ah.show();
//	}
}
