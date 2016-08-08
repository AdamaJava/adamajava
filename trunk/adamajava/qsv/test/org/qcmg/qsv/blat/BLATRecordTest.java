package org.qcmg.qsv.blat;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;


public class BLATRecordTest {
	
//	@Test
//	public void convertStringArrayToIntArray() {
//		assertEquals(null, BLATRecord.getIntArrayFromStringArray(null));
//		assertEquals(null, BLATRecord.getIntArrayFromStringArray(new String[]{}));
//		assertArrayEquals(new int[]{123}, BLATRecord.getIntArrayFromStringArray(new String[]{"123"}));
//		assertArrayEquals(new int[]{123,123}, BLATRecord.getIntArrayFromStringArray(new String[]{"123","123"}));
//		assertArrayEquals(new int[]{123,123,456}, BLATRecord.getIntArrayFromStringArray(new String[]{"123","123","456"}));
//	}
	
//	@Test
//	public void incrementIntArray() {
//		int [] iArray = new int [] {123};
//		 BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{124}, iArray);
//		BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{125}, iArray);
//		BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{126}, iArray);
//		
//	}


	@Test
	public void ctor() throws QSVException {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		assertEquals(41 - 1 - 1 - 1, br.getScore());
		assertEquals("12345_123455_12345", br.getName());
		assertEquals("chr9", br.getReference());
		assertEquals(2, br.getBlockCount());
		assertArrayEquals(new int[] {81359143,81359165}, br.gettStarts());
		assertEquals(QSVUtil.MINUS, br.getStrand());
		assertEquals(81359142 + 1, br.getStartPos());
		assertEquals(81359189, br.getEndPos());
		assertEquals(14 + 1, br.getQueryStart());
		assertEquals(60, br.getQueryEnd());
		assertEquals(70, br.getSize());
		assertArrayEquals(new int[] {17,25}, br.getBlockSizes());
	}
	
	@Test
	public void unmodifiedStarts() throws QSVException {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		int i = 70 - 17 - 10 + 1;
		int j = 70 - 25 - 31 + 1;
		assertArrayEquals(new int[] {i ,j }, br.getUnmodifiedStarts());
		
		/*
		 * flip strand - should get different results
		 */
		ctor = "41\t1\t0\t0\t1\t4\t1\t5\t+\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		i = 10 + 1;
		j = 31 + 1;
		assertArrayEquals(new int[] {i ,j}, br.getUnmodifiedStarts());
	}
}
