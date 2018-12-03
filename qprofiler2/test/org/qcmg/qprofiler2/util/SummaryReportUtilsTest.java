package org.qcmg.qprofiler2.util;

import static org.junit.Assert.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.qprofiler2.util.SummaryReportUtils;

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
		assertEquals((2), badQualCount.get(5));	
		
		qual = new byte[] { 1,2,3,9,9,10,11,12,13,14,15 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount);
		//so far three records with 5badbase
		assertEquals((3), badQualCount.get(5));	
 	
		qual =  new byte[] { 10,11,12,13,14,15 };
		SummaryReportUtils.tallyQualScores( qual, badQualCount );
		//so far three records with 5badbase
		assertEquals((3), badQualCount.get( 5) );
		//one read with 0badbase which is the last read
		assertEquals((1), badQualCount.get( 0) );	 		
	}

	
	@Test
	public void testAddPositionAndLengthToMap() {
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		addPositionAndLengthToMap(map, 10, 100);
		
		assertEquals(100, map.size());
		assertNull(map.get(0));
		assertNull(map.get(9));
		assertNull(map.get(110));
		assertEquals(1, map.get(10).get());
		assertEquals(1, map.get(109).get());
				
		addPositionAndLengthToMap(map, 100, 50);
		assertEquals(140, map.size());
		assertNull(map.get(0));
		assertNull(map.get(9));
		assertNull(map.get(150));
		assertEquals(1, map.get(10).get());
		assertEquals(2, map.get(109).get());
		
		// adding 0 positions and size - should not affect anything...
		addPositionAndLengthToMap(map, 0, 0);
		assertEquals(140, map.size());
		assertNull(map.get(0));
		assertNull(map.get(9));
		assertNull(map.get(150));
		assertEquals(1, map.get(10).get());
		assertEquals(2, map.get(109).get());
		
		addPositionAndLengthToMap(map, 100, 10);
		assertEquals(140, map.size());
		assertNull(map.get(0));
		assertNull(map.get(9));
		assertNull(map.get(150));
		assertEquals(1, map.get(10).get());
		assertEquals(3, map.get(109).get());
				
		addPositionAndLengthToMap(map, 10000, 2);
		assertEquals(142, map.size());
		assertNull(map.get(0));
		assertNull(map.get(9));
		assertNull(map.get(150));
		assertNull(map.get(10002));
		assertEquals(1, map.get(10).get());
		assertEquals(3, map.get(109).get());
		assertEquals(1, map.get(10000).get());
		assertEquals(1, map.get(10001).get());
		
	}	
	/**
	 * 
	 * @param map
	 * @param position
	 * @param length
	 */
	public static void addPositionAndLengthToMap(ConcurrentMap<Integer, AtomicLong> map, int position, int length) {
		for (int i = position ; i < position + length ; i++) 
			//SummaryByCycleUtils.incrementCount(map, Integer.valueOf(i));			
			map.computeIfAbsent( Integer.valueOf(i), k -> new AtomicLong(0)).incrementAndGet();		
	}
	
}
