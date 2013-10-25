package org.qcmg.common.util;

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

}
