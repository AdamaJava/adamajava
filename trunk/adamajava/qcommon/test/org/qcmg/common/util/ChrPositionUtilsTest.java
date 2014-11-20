package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;

public class ChrPositionUtilsTest {
	
	@Test
	public void testOverlap() {
		ChrPosition cp1 = new ChrPosition("1", 100, 200);
		
		// true
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 199, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 101, 199)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 100, 200)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 100, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 200, 200)));
		
		// false
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 99, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 201, 202)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("2", 100, 202)));
	}
	
	@Test
	public void testOverlapPoint() {
		ChrPosition cp1 = new ChrPosition("1", 100, 100);
		
		// true
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 100, 200)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 100, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 99, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 99, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 100, 101)));
		
		// false
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 101, 199)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 50, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 99, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("1", 101, 101)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrPosition("2", 99, 101)));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullStringUsedToCreateChrPos() {
		ChrPositionUtils.getChrPositionFromString(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void emptyStringUsedToCreateChrPos() {
		ChrPositionUtils.getChrPositionFromString("");
	}
	@Test(expected=IllegalArgumentException.class)
	public void negativePositionStringUsedToCreateChrPos() {
		ChrPositionUtils.getChrPositionFromString("chr1:-1-0");
	}
	
	@Test
	public void getChrPos() {
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr1:1000-1001");
		assertEquals("chr1", cp.getChromosome());
		assertEquals(1000, cp.getPosition());
		assertEquals(1001, cp.getEndPosition());
	}
	
	@Test
	public void containedSameChr() {
		ChrPosition cp1 = new ChrPosition("chr1", 1000, 2000);
		ChrPosition cp2 = new ChrPosition("chr1", 1999, 2001);
		ChrPosition cp3 = new ChrPosition("chr1", 1999, 2000);
		ChrPosition cp4 = new ChrPosition("chr1", 2000, 2001);
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp2));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp1, cp3));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp2, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp4));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp2, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	@Test
	public void containedDifferentChr() {
		ChrPosition cp1 = new ChrPosition("chr1", 1000, 2000);
		ChrPosition cp2 = new ChrPosition("chr2", 1999, 2001);
		ChrPosition cp3 = new ChrPosition("chr3", 1999, 2000);
		ChrPosition cp4 = new ChrPosition("chr4", 2000, 2001);
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp2));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp2, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp2, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	@Test
	public void arePositionsAdjacent() {
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("1", 1, 10), new ChrPosition("2", 11, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("1", 1, 10), new ChrPosition("1", 12, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("1", 1, 10), new ChrPosition("1", 10, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("1", 10, 20), new ChrPosition("1", 1, 10)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("1", 123456, 123456), new ChrPosition("1", 123456, 123456)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("2", 123456, 123456), new ChrPosition("1", 123456, 123456)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrPosition("2", 123456, 123456), new ChrPosition("1", 123457, 123457)));
		
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("1", 1, 10), new ChrPosition("1", 11, 20)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("2", 1, 10), new ChrPosition("2", 11, 20)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("2", 11, 20), new ChrPosition("2", 1, 10)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("2", 11, 20), new ChrPosition("2", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("1", 123456, 123456), new ChrPosition("1", 123457, 123457)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrPosition("1", 123457, 123457), new ChrPosition("1", 123456, 123456)));
		
	}
	
	@Test
	public void positionOnlyOverlap() {
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 1, 10), new ChrPosition("1", 11, 20)));
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 11, 20), new ChrPosition("1", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 1, 10), new ChrPosition("1", 10, 20)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 10, 20), new ChrPosition("1", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 1, 100), new ChrPosition("1", 10, 20)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 10, 20), new ChrPosition("1", 1, 100)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrPosition("1", 123456, 123456), new ChrPosition("1", 123456, 123456)));
	}
}
