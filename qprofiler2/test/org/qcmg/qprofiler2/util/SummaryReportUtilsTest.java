package org.qcmg.qprofiler2.util;



import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SummaryReportUtilsTest {
	
	
	
	@Test
	public void testCompareWithSAMUtilsAgain() {
		String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
		QCMGAtomicLongArray qualCount = new QCMGAtomicLongArray(1000);
		byte [] bytes = inputString.getBytes();
		 		 
		SummaryReportUtils.tallyQualScores(bytes, qualCount);		
		for(int i = 0; i < 1000; i ++)
			if(qualCount.get(i) > 0)
				System.out.println(i + " :: " + qualCount.get(i));
		
		for(byte c : bytes)
			System.out.print(c+"(" + (char)c +  "),");
		System.out.println( "");
		for(byte c : bytes)
			if(c - 33 < 10)
				System.out.print((char) c+"(<10)");
		 
//		assertEquals(1, mapBytes.size());
//		int counter = 100000;
//		ConcurrentMap<Integer, AtomicLong> map = null;
//		ConcurrentMap<Integer, AtomicLong> mapBytes = null;
//		long start = 0;
//			
//		map = new ConcurrentHashMap<Integer, AtomicLong>();
//		start = System.currentTimeMillis();
//		for (int i = 0 ; i < counter ; i++) {
//			SummaryReportUtils.tallyQualScoresASCII(inputString, map, 0);
//		}
//		assertEquals(1, map.size());
//		
//		
//		mapBytes = new ConcurrentHashMap<Integer, AtomicLong>();
//		byte [] bytes = inputString.getBytes();
//		start = System.currentTimeMillis();
//		for (int i = 0 ; i < counter ; i++) {
//			SummaryReportUtils.tallyQualScores(bytes, mapBytes);
//		}
//		assertEquals(1, mapBytes.size());
//		
//		map = new ConcurrentHashMap<Integer, AtomicLong>();
//		start = System.currentTimeMillis();
//		for (int i = 0 ; i < counter ; i++) {
//			SummaryReportUtils.tallyQualScoresASCII(inputString, map, 0);
//		}
//		assertEquals(1, map.size());		
//		assertEquals(map.keySet(), mapBytes.keySet());
//		assertEquals(map.get(0).get(), mapBytes.get(0).get());
	}	
	
	
	@Test
	public void testTallyQualScoresValid() {

		QCMGAtomicLongArray badQualCount = new QCMGAtomicLongArray(15);
				
		byte[] qual = new byte[] { 1,1,1,1,1 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount );
		//so far one record with 5badbase
		assertEquals((1), badQualCount.get(5));
				
		qual =  new byte[] { 1,2,3,4,5 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount );
		//so far two records with 5badbase
		Assert.assertEquals((2), badQualCount.get(5));	
		
		qual = new byte[] { 1,2,3,9,9,10,11,12,13,14,15 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount);
		//so far three records with 5badbase
		Assert.assertEquals((3), badQualCount.get(5));	
 	
		qual =  new byte[] { 10,11,12,13,14,15 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount );
		//so far three records with 5badbase
		Assert.assertEquals((3), badQualCount.get( 5) );
		//one read with 0badbase which is the last read
		Assert.assertEquals((1), badQualCount.get( 0) );

	 		
	}
	
//	@Test
//	public void testTallyQualScoresInvalid() {
//		ConcurrentMap<Integer, AtomicLong> badQualCount = new ConcurrentHashMap<Integer, AtomicLong>();
//		
//		// null string and null seperator
//		SummaryReportUtils.tallyQualScores(null, badQualCount, null);
//		assertTrue(badQualCount.isEmpty());
//		
//		// empty string and null seperator
//		String badQual = "";
//		SummaryReportUtils.tallyQualScores(badQual, badQualCount, null);
//		assertTrue(badQualCount.isEmpty());
//		
//		// empty string
//		SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
//		assertFalse(badQualCount.isEmpty());
//		assertEquals(1, badQualCount.get(Integer.valueOf(0)).get());
//		
//		// valid string, but incorrect separator
//		badQual = "1,1,1,1,1";
//		try {
//			SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {
//			assertTrue(e.getMessage().startsWith("For input string"));
//		}
//	}
	
	@Test
	public void testAddPositionAndLengthToMap() {
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		addPositionAndLengthToMap(map, 10, 100);
		
		Assert.assertEquals(100, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(110));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(1, map.get(109).get());
		
		
		addPositionAndLengthToMap(map, 100, 50);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		

		// adding 0 positions and size - should not affect anything...
		addPositionAndLengthToMap(map, 0, 0);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		
		addPositionAndLengthToMap(map, 100, 10);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		
		
		addPositionAndLengthToMap(map, 10000, 2);
		Assert.assertEquals(142, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertNull(map.get(10002));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		Assert.assertEquals(1, map.get(10000).get());
		Assert.assertEquals(1, map.get(10001).get());
		
	}	
	/**
	 * 
	 * @param map
	 * @param position
	 * @param length
	 */
	public static void addPositionAndLengthToMap(ConcurrentMap<Integer, AtomicLong> map, int position, int length) {
		for (int i = position ; i < position + length ; i++) {
			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(i));
		}
	}
	

	
	

	
}
