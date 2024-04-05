package org.qcmg.common.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.PileupElementLiteUtil;

public class PileupElementLiteTest {
	
	@Test
	public void testNovelStartCounter() {
		PileupElementLite pel = new PileupElementLite();
		
		// add some forward quals
		pel.add(1L, true, (byte) 0, 1, false);
		assertEquals(1, pel.getForwardCount());
		assertEquals(0, pel.getReverseCount());
		assertEquals(1, pel.getTotalCount());
		assertEquals(1, PileupElementLiteUtil.getNovelStarts(pel));
		
		pel.add(2L, true, (byte) 0, 1, false);
		
		assertEquals(2, pel.getForwardCount());
		assertEquals(0, pel.getReverseCount());
		assertEquals(2, pel.getTotalCount());
		assertEquals(1, PileupElementLiteUtil.getNovelStarts(pel));
		
		pel.add(3L, true, (byte) 0, 1, false);
		pel.add(4L, true, (byte) 0, 1, false);
		pel.add(5L, true, (byte) 0, 2, false);
		assertEquals(2, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(5, pel.getForwardCount());
		assertEquals(5, pel.getTotalCount());
		assertEquals(0, pel.getReverseCount());
		
		pel.add(6L, false, (byte) 0, 1, false);
		pel.add(7L, false, (byte) 0, 1, false);
		pel.add(8L, false, (byte) 0, 2, false);
		assertEquals(4, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(5, pel.getForwardCount());
		assertEquals(8, pel.getTotalCount());
		assertEquals(3, pel.getReverseCount());
	}
	
	@Test
	public void convert() {
		assertArrayEquals(new int[] {1,1,1,1}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(true, true, (byte)1, 1)));
		assertArrayEquals(new int[] {0,0,1,1}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(false, false, (byte)1, 1)));
		assertArrayEquals(new int[] {0,0,0,0}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(false, false, (byte)0, 0)));
		assertArrayEquals(new int[] {0,0,40,100}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(false, false, (byte)40, 100)));
		assertArrayEquals(new int[] {1,0,40,100}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(true, false, (byte)40, 100)));
		assertArrayEquals(new int[] {0,1,40,100}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(false, true, (byte)40, 100)));
		assertArrayEquals(new int[] {1,1,40,100}, PileupElementLite.convertLongToStrandQualAndPosition(
				PileupElementLite.convertStrandQualAndPositionToLong(true, true, (byte)40, 100)));
	}
	
	@Test
	public void testNSCounterBothStrands() {
		PileupElementLite pel = new PileupElementLite();
		// add some forward quals
		pel.add(1L, true, (byte) 10, 1, false);
		assertEquals(1, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(10, PileupElementLiteUtil.getTotalQuality(pel));
		pel.add(2L, false, (byte) 10, 1, false);
		assertEquals(20, PileupElementLiteUtil.getTotalQuality(pel));
		assertEquals(2, PileupElementLiteUtil.getNovelStarts(pel));
		pel.add(3L, true, (byte) 10, 2, false);
		assertEquals(30, PileupElementLiteUtil.getTotalQuality(pel));
		assertEquals(3, PileupElementLiteUtil.getNovelStarts(pel));
		pel.add(4L, false, (byte) 10, 1, false);
		assertEquals(3, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(40, PileupElementLiteUtil.getTotalQuality(pel));
		
		pel.add(5L, false, (byte) 10, 99, false);
		assertEquals(4, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(50, PileupElementLiteUtil.getTotalQuality(pel));
		pel.add(6L, true, (byte) 20, 100, false);
		assertEquals(5, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(70, PileupElementLiteUtil.getTotalQuality(pel));
		pel.add(7L, false, (byte) 15, 100, false);
		assertEquals(6, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(85, PileupElementLiteUtil.getTotalQuality(pel));
		
		pel.add(7L, false, (byte) 0, 100, false);
		assertEquals(6, PileupElementLiteUtil.getNovelStarts(pel));
		assertEquals(85, PileupElementLiteUtil.getTotalQuality(pel));
	}

}
