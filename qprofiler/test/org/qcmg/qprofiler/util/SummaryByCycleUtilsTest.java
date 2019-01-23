package org.qcmg.qprofiler.util;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.qprofiler.summarise.SummaryByCycle;
import org.qcmg.qprofiler.summarise.SummaryByCycleNew2;
import org.qcmg.qprofiler.util.SummaryByCycleUtils;


public class SummaryByCycleUtilsTest {
	
	@Test
	public void testParseCharacterSummary() {
		
		SummaryByCycle<Character> seqByCycle = new SummaryByCycle<Character>();
		String dataString = "AAABBCCCCCCCCCDDXZY";
		
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString, null);
		
		Assert.assertEquals(1, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(1, seqByCycle.count(6, 'C').get());
		Assert.assertEquals(1, seqByCycle.count(18, 'Z').get());
		Assert.assertEquals(1, seqByCycle.count(19, 'Y').get());
		
		// parse the same string again - should get 2 of each
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString, null);
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(2, seqByCycle.count(6, 'C').get());
		Assert.assertEquals(2, seqByCycle.count(18, 'Z').get());
		Assert.assertEquals(2, seqByCycle.count(19, 'Y').get());
		
		
		// parse a different string and test
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "XXXXXXXXXXXXXXXXXXX", null);
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(1, seqByCycle.count(1, 'X').get());
		Assert.assertEquals(3, seqByCycle.count(17, 'X').get());
		
		// this time test the offset - means that T should not appear in the 1st cycle
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "TXXXXXXXXXXXXXXXXXX", null, 1);
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(2, seqByCycle.count(1, 'X').get());
		Assert.assertEquals(0, seqByCycle.count(1, 'T').get());
		Assert.assertEquals(2, seqByCycle.count(2, 'X').get());
		Assert.assertEquals(4, seqByCycle.count(17, 'X').get());
		
	}
	@Test
	public void testParseCharacterSummaryNew() {
		
		SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(Character.MIN_VALUE, 50);
		String dataString = "AAABBCCCCCCCCCDDXZY";
		
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString);
		
		Assert.assertEquals(1, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(1, seqByCycle.count(6, 'C').get());
		Assert.assertEquals(1, seqByCycle.count(18, 'Z').get());
		Assert.assertEquals(1, seqByCycle.count(19, 'Y').get());
		
		// parse the same string again - should get 2 of each
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString);
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(2, seqByCycle.count(6, 'C').get());
		Assert.assertEquals(2, seqByCycle.count(18, 'Z').get());
		Assert.assertEquals(2, seqByCycle.count(19, 'Y').get());
		
		
		// parse a different string and test
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "XXXXXXXXXXXXXXXXXXX");
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(1, seqByCycle.count(1, 'X').get());
		Assert.assertEquals(3, seqByCycle.count(17, 'X').get());
		
		// this time test the offset - means that T should not appear in the 1st cycle
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "TXXXXXXXXXXXXXXXXXX", null, 1);
		
		Assert.assertEquals(2, seqByCycle.count(1, 'A').get());
		Assert.assertEquals(2, seqByCycle.count(1, 'X').get());
		Assert.assertEquals(0, seqByCycle.count(1, 'T').get());
		Assert.assertEquals(2, seqByCycle.count(2, 'X').get());
		Assert.assertEquals(4, seqByCycle.count(17, 'X').get());
		
	}
	
	
	@Test
	public void testParseCharacterSummaryInvalidOffset() {
		
		SummaryByCycle<Character> seqByCycle = new SummaryByCycle<Character>();
		
		// offset if negative, and so will be set to 0
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "TXXX", null, -1);
		Assert.assertEquals(1, seqByCycle.count(1, 'T').get());
		Assert.assertEquals(1, seqByCycle.count(2, 'X').get());
		Assert.assertEquals(1, seqByCycle.count(3, 'X').get());
		Assert.assertEquals(1, seqByCycle.count(4, 'X').get());
		
		// offset is larger than the data item - offset will be set to 0, and so
		// counts will be incremented
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "TXXX", null, 5);
		Assert.assertEquals(2, seqByCycle.count(1, 'T').get());
		Assert.assertEquals(2, seqByCycle.count(2, 'X').get());
		Assert.assertEquals(2, seqByCycle.count(3, 'X').get());
		Assert.assertEquals(2, seqByCycle.count(4, 'X').get());
		
		SummaryByCycleUtils.parseCharacterSummary(seqByCycle, "TXXX", null, -100);
		Assert.assertEquals(3, seqByCycle.count(1, 'T').get());
		Assert.assertEquals(3, seqByCycle.count(2, 'X').get());
		Assert.assertEquals(3, seqByCycle.count(3, 'X').get());
		Assert.assertEquals(3, seqByCycle.count(4, 'X').get());
		
	}
	
	@Test
	public void testParseIntegerSummary() throws Exception{
		
		SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>();
		String dataString = "8 5 6 -1 7 -1 6 -1 -1 -1 8 13 -1 -1 10 9 8 7 -1 13 ";
		
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, dataString, "\\s");
		
		Assert.assertEquals(1, qualByCycle.count(1, 8).get());
		Assert.assertEquals(1, qualByCycle.count(6, -1).get());
		Assert.assertEquals(1, qualByCycle.count(18, 7).get());
		Assert.assertEquals(1, qualByCycle.count(20, 13).get());
		
		// parse the same string again - should get 2 of each
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, dataString, "\\s");
		
		Assert.assertEquals(2, qualByCycle.count(1, 8).get());
		Assert.assertEquals(2, qualByCycle.count(6, -1).get());
		Assert.assertEquals(2, qualByCycle.count(18, 7).get());
		Assert.assertEquals(2, qualByCycle.count(20, 13).get());
		
		
		// parse a different string and test
		String dataString2 = "4 6 5 6 4 -1 4 4 4 4 6 4 8 5 4 4 7 4 8 6 ";
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, dataString2, "\\s");
		Assert.assertEquals(2, qualByCycle.count(1, 8).get());
		Assert.assertEquals(1, qualByCycle.count(1, 4).get());
		Assert.assertEquals(2, qualByCycle.count(20, 13).get());
		Assert.assertEquals(1, qualByCycle.count(20, 6).get());
		Assert.assertEquals(3, qualByCycle.count(6, -1).get());
	}
	
	@Test
	public void testParseIntegerSummaryByteArray() throws Exception{
		
		SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>();
		byte[] data = new byte[] {8, 5, 6, -1, 7, -1, 6, -1, -1, -1, 8, 13, -1, -1, 10, 9, 8, 7, -1, 13};
		
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, data);
		
		Assert.assertEquals(1, qualByCycle.count(1, 8).get());
		// -1 get converted to 255 courtesy of & 0xFF
		Assert.assertEquals(1, qualByCycle.count(6, 255).get());
		Assert.assertEquals(1, qualByCycle.count(18, 7).get());
		Assert.assertEquals(1, qualByCycle.count(20, 13).get());
		
		// parse the same string again - should get 2 of each
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, data);
		
		Assert.assertEquals(2, qualByCycle.count(1, 8).get());
		// -1 get converted to 255 courtesy of & 0xFF
		Assert.assertEquals(2, qualByCycle.count(6, 255).get());
		Assert.assertEquals(2, qualByCycle.count(18, 7).get());
		Assert.assertEquals(2, qualByCycle.count(20, 13).get());
		
		
		// parse a different string and test
//		String dataString2 = "4 6 5 6 4 -1 4 4 4 4 6 4 8 5 4 4 7 4 8 6 ";
		byte[] data2 = new byte[] {4, 6, 5, 6, 4, -1, 4, 4, 4, 4, 6, 4, 8, 5, 4, 4, 7, 4, 8, 6 };
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, data2);
		Assert.assertEquals(2, qualByCycle.count(1, 8).get());
		Assert.assertEquals(1, qualByCycle.count(1, 4).get());
		Assert.assertEquals(2, qualByCycle.count(20, 13).get());
		Assert.assertEquals(1, qualByCycle.count(20, 6).get());
		// -1 get converted to 255 courtesy of & 0xFF
		Assert.assertEquals(3, qualByCycle.count(6, 255).get());
	}
	
	@Ignore
	public void testParseIntegerSummaryPerformance() throws Exception{
		// set to ignore for now
		//this is a performance measurement test - does not need to be conducted as part of the build process
		
		SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>();
		String dataString = "8 5 6 -1 7 -1 6 -1 -1 -1 8 13 -1 -1 10 9 8 7 -1 13 1";
		
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < 1000000 ; i++) {
		
			SummaryByCycleUtils.parseIntegerSummary(qualByCycle, dataString, "\\s");
		}
		System.out.println("old method: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 1000000 ; i++) {
		
			SummaryByCycleUtils.parseIntegerSummaryST(qualByCycle, dataString, " ");
		}
		System.out.println("new  method: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 1000000 ; i++) {
			
			SummaryByCycleUtils.parseIntegerSummary(qualByCycle, dataString, "\\s");
		}
		System.out.println("ols  method: " + (System.currentTimeMillis() - start));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public <T> void testIncrementCount() {
		
		ConcurrentMap<T, AtomicLong> testMap = new ConcurrentHashMap<T, AtomicLong>();
		
		SummaryByCycleUtils.incrementCount(testMap, (T)"test");
		Assert.assertEquals(1, testMap.get("test").get());
		
		// and again - count should be 2
		SummaryByCycleUtils.incrementCount(testMap, (T)"test");
		Assert.assertEquals(2, testMap.get("test").get());
		
		// new data element
		SummaryByCycleUtils.incrementCount(testMap, (T)Integer.valueOf(1));
		Assert.assertEquals(1, testMap.get(Integer.valueOf(1)).get());
		
		// null data element
		try {
			SummaryByCycleUtils.incrementCount(testMap, (T)null);
			Assert.fail("Should have thrown an assertion exception");
		} catch (AssertionError ae) {
		}
		
		// null map - will throw exception
		try {
		SummaryByCycleUtils.incrementCount(null, (T)null);
		Assert.fail("Should have thrown an AssertionError");
		} catch (AssertionError ae) {}
	}
	
	@SuppressWarnings("unchecked")
	@Ignore
//	@Test	// no need to run regularly
	public <T> void testIncrementCountSpeed() {
		Random random = new Random();
		Map<T, Integer> testMap = null;
		Map<T, AtomicLong> testMapMI = null;

		//reduce count size to speed up tests
		int count = 100000;
//		int count = 1000000;
		
		// new mthod
		testMap = new TreeMap<T, Integer>();
		Long start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			newIncremenetCountMethod(testMap, (T)("blah" + random.nextInt(10)));
		}
		System.out.println("new method (String) took " + (System.currentTimeMillis() - start) + " ms");
		
		testMap = new TreeMap<T, Integer>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			newIncremenetCountMethod(testMap, (T)Integer.valueOf(random.nextInt(10)));
		}
		System.out.println("new method (Integer) took " + (System.currentTimeMillis() - start) + " ms");
		
		testMap = new TreeMap<T, Integer>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			oldIncremenetCountMethod(testMap, (T)("test" + random.nextInt(10)));
		}
		System.out.println("old method (String) took " + (System.currentTimeMillis() - start) + " ms");
		
		testMap = new TreeMap<T, Integer>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			oldIncremenetCountMethod(testMap, (T)Integer.valueOf(random.nextInt(10)));
		}
		System.out.println("old method (Integer) took " + (System.currentTimeMillis() - start) + " ms");
		
		
		
		testMapMI = new TreeMap<T, AtomicLong>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			newIncremenetCountMethodAtomicInt(testMapMI, (T)("book" + random.nextInt(10)));
		}
		System.out.println("new method (String, AtomicInt) took " + (System.currentTimeMillis() - start) + " ms");
		
		testMapMI = new TreeMap<T, AtomicLong>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			newIncremenetCountMethodAtomicInt(testMapMI, (T)Integer.valueOf(random.nextInt(10)));
		}
		System.out.println("new method (Integer, AtomicInt) took " + (System.currentTimeMillis() - start) + " ms");
	}
	
	@Test
	public void testGetLengthsFromSummaryByCycle() {
		SummaryByCycle<Character> seqByCycle = new SummaryByCycle<Character>();
		String dataString = "AAABBCCCCCCCCCDDAAABBCCCCCCCCCDDAAABBCCCCCCCCCDDXX";

		// reduce noOfRecords for test speedup
		int noOfRecords = 10000;
//		int noOfRecords = 100000;
		for (int i = 0 ; i < noOfRecords ; i++)
			SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString, null);
		
		Map<Integer, AtomicLong> output = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, noOfRecords);
		
		Assert.assertEquals(1, output.size());
		Assert.assertEquals(noOfRecords, output.get(50).get());
		
		// add some string of varying length
		int noOfShortenedRecords = 5000;
		int minLength = 25;
		for (int j = 1 ; j <= minLength ; j++) {
			String shortenedDataString = dataString.substring(j);
			for (int i = 0 ; i < noOfShortenedRecords ; i++)
				SummaryByCycleUtils.parseCharacterSummary(seqByCycle, shortenedDataString, null);
			
		}
		
		int totalCount = 0;
		for (Entry<Character, AtomicLong> entry : seqByCycle.getValue(1).entrySet()) {
			totalCount += entry.getValue().get();
		}
		
		Assert.assertEquals(noOfRecords + (minLength * noOfShortenedRecords), totalCount);
		
		output = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, noOfRecords + (minLength * noOfShortenedRecords));
		
		Assert.assertEquals(minLength + 1, output.size());
		Assert.assertEquals(noOfRecords, output.get(50).get());
		
		for (int k = 1 ; k <= minLength ; k++)
			Assert.assertEquals(noOfShortenedRecords, output.get(50-k).get());
	}
	
	@Test
	public void testGetLengthsFromSummaryByCycleNew2() {
		SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(Character.MIN_VALUE, 50);
		String dataString = "AAABBCCCCCCCCCDDAAABBCCCCCCCCCDDAAABBCCCCCCCCCDDXX";
		
		// reduce noOfRecords for test speedup
		int noOfRecords = 10000;
//		int noOfRecords = 100000;
		for (int i = 0 ; i < noOfRecords ; i++)
			SummaryByCycleUtils.parseCharacterSummary(seqByCycle, dataString);
		
		Map<Integer, AtomicLong> output = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle);
		
		Assert.assertEquals(1, output.size());
		Assert.assertEquals(noOfRecords, output.get(50).get());
		
		// add some string of varying length
		int noOfShortenedRecords = 5000;
		int minLength = 25;
		for (int j = 1 ; j <= minLength ; j++) {
			String shortenedDataString = dataString.substring(j);
			for (int i = 0 ; i < noOfShortenedRecords ; i++)
				SummaryByCycleUtils.parseCharacterSummary(seqByCycle, shortenedDataString);
			
		}
		
		int totalCount = 0;
		for (Entry<Character, AtomicLong> entry : seqByCycle.getValue(1).entrySet()) {
			totalCount += entry.getValue().get();
		}
		
		Assert.assertEquals(noOfRecords + (minLength * noOfShortenedRecords), totalCount);
		
		output = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle);
		
		Assert.assertEquals(minLength + 1, output.size());
		Assert.assertEquals(noOfRecords, output.get(50).get());
		
		for (int k = 1 ; k <= minLength ; k++)
			Assert.assertEquals(noOfShortenedRecords, output.get(50-k).get());
		
	}
	
	
	private <T> void oldIncremenetCountMethod(Map<T, Integer> map, T data) {
		if (null == map)
			throw new AssertionError("Null map found in oldIncrementCountMethod");
		
		// Make sure we are initialized for this data item
		if (!map.containsKey(data)) {
			map.put(data, 0);
		}

		// Now increment count for this data item
		Integer currentCount = map.get(data);
		map.put(data, currentCount + 1);
		
	}
	
	private <T> void newIncremenetCountMethod(Map<T, Integer> map, T data) {
		if (null == map)
			throw new AssertionError("Null map found in newIncremenetCountMethod");
		
		// Now increment count for this data item
		Integer currentCount = map.get(data);
		if (null == currentCount) {
			map.put(data, Integer.valueOf(1));
		} else {
			map.put(data, currentCount + 1);
		}
	}
	
	private <T> void newIncremenetCountMethodAtomicInt(Map<T, AtomicLong> map, T data) {
		if (null == map)
			throw new AssertionError("Null map found in newIncremenetCountMethod");
		
		// Now increment count for this data item
		AtomicLong currentCount = map.get(data);
		if (null == currentCount) {
			map.put(data, new AtomicLong(1));
		} else {
			currentCount.incrementAndGet();
		}
	}

}
