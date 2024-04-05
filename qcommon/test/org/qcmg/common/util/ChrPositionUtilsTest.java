package org.qcmg.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.model.ChrRangePosition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;

public class ChrPositionUtilsTest {
	
	
	@Test
	public void testDelta() {
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 200);
		ChrRangePosition cp2 = new ChrRangePosition("1", 100, 200);
        assertTrue(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		
		cp2 = new ChrRangePosition("1", 110, 200);
        assertFalse(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 100, 205);
        assertFalse(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 102, 203);
        assertFalse(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 103, 202);
        assertFalse(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 98, 203);
        assertFalse(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 98, 202);
        assertTrue(ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
	}

	@Test
	public void testConvertChrPositionToLong() {
		long expected = ((long) 4 << 32) + 9;
		long actual = ChrPositionUtils.convertContigAndPositionToLong("4", 9);
		assertEquals(expected, actual);

		ChrPosition cp = ChrPositionUtils.convertLongToChrPosition(actual);
		assertEquals("4", cp.getChromosome());
		assertEquals(9, cp.getStartPosition());

	}
	@Test
	public void toVcfStringShouldReturnCorrectFormat() {
		ChrPosition cp = new ChrRangePosition("chr1", 1000, 2000);
		String id = "id";
		String ref = "A";
		String alt = "T";
		String qual = "30";
		String filter = "PASS";
		String info = "DP=100";

		String expected = "chr1\t1000\tid\tA\tT\t30\tPASS\tDP=100";
		String actual = ChrPositionUtils.toVcfString(cp, id, ref, alt, qual, filter, info);
		assertEquals(expected, actual);

		ChrPosition cp1 = new ChrPositionRefAlt("chr1", 1000, 1001, "C", "G");
		assertEquals("chr1\t1000\t.\tC\tG\t.\t.\t.", ChrPositionUtils.toVcfString(cp1, MISSING_DATA_STRING, null, null, MISSING_DATA_STRING, MISSING_DATA_STRING, MISSING_DATA_STRING));
	}
	
	@Test
	public void cloneWithNewName() {
		ChrRangePosition cp = new ChrRangePosition("1", 100, 200);
		assertEquals("chr1", ChrPositionUtils.cloneWithNewChromosomeName(cp, "chr1").getChromosome());
		assertEquals("chrXY", ChrPositionUtils.cloneWithNewChromosomeName(cp, "chrXY").getChromosome());
		assertEquals("myCP", ChrPositionUtils.cloneWithNewChromosomeName(cp, "myCP").getChromosome());
		assertEquals("1", ChrPositionUtils.cloneWithNewChromosomeName(cp, "1").getChromosome());
	}
	
	@Test
	public void getAmplicons() {
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 200);
		ChrRangePosition cp2 = new ChrRangePosition("1", 100, 200);
		List<ChrRangePosition> frags = new ArrayList<>();
		frags.add(cp1);
		frags.add(cp2);
		
		assertEquals(1, ChrPositionUtils.getAmpliconsFromFragments(frags).size());
		ChrRangePosition cp3 = new ChrRangePosition("1", 101, 200);
		frags.add(cp3);
		assertEquals(2, ChrPositionUtils.getAmpliconsFromFragments(frags).size());
	}
	
	
	@Test
	public void overlapWithBuffer() {
		
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 200);
		
		// true
        assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 300, 400), 100));
        assertTrue(ChrPositionUtils.doChrPositionsOverlap(new ChrRangePosition("1", 300, 400), cp1, 100));

        assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 300, 400), 99));
        assertFalse(ChrPositionUtils.doChrPositionsOverlap(new ChrRangePosition("1", 300, 400), cp1, 99));
		
	}
	
	@Test
	public void testOverlap() {
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 200);
		
		// true
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 199, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 101, 199)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 100, 200)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 100, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 200, 200)));
		
		// false
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 99, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 201, 202)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("2", 100, 202)));
	}
	
	@Test
	public void testOverlapPoint() {
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 100);
		
		// true
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 201)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 100, 200)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 100, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 99, 100)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 99, 101)));
		Assert.assertTrue(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 100, 101)));
		
		// false
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 101, 199)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 50, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 99, 99)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 101, 101)));
		Assert.assertFalse(ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("2", 99, 101)));
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
		ChrRangePosition cp = ChrPositionUtils.getChrPositionFromString("chr1:1000-1001");
		assertEquals("chr1", cp.getChromosome());
		assertEquals(1000, cp.getStartPosition());
		assertEquals(1001, cp.getEndPosition());
	}
	
	@Test
	public void containedSameChr() {
		ChrRangePosition cp1 = new ChrRangePosition("chr1", 1000, 2000);
		ChrRangePosition cp2 = new ChrRangePosition("chr1", 1999, 2001);
		ChrRangePosition cp3 = new ChrRangePosition("chr1", 1999, 2000);
		ChrRangePosition cp4 = new ChrRangePosition("chr1", 2000, 2001);
        assertFalse(ChrPositionUtils.isChrPositionContained(cp1, cp2));
        assertTrue(ChrPositionUtils.isChrPositionContained(cp1, cp3));
        assertTrue(ChrPositionUtils.isChrPositionContained(cp2, cp3));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp1, cp4));
        assertTrue(ChrPositionUtils.isChrPositionContained(cp2, cp4));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	@Test
	public void containedDifferentChr() {
		ChrRangePosition cp1 = new ChrRangePosition("chr1", 1000, 2000);
		ChrRangePosition cp2 = new ChrRangePosition("chr2", 1999, 2001);
		ChrRangePosition cp3 = new ChrRangePosition("chr3", 1999, 2000);
		ChrRangePosition cp4 = new ChrRangePosition("chr4", 2000, 2001);
        assertFalse(ChrPositionUtils.isChrPositionContained(cp1, cp2));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp1, cp3));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp2, cp3));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp1, cp4));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp2, cp4));
        assertFalse(ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	
	@Test
	public void arePositionsAdjacent() {
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("2", 11, 20)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 12, 20)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 10, 20)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 10)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
        assertFalse(ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 123456, 123456), new ChrRangePosition("1", 123457, 123457)));

        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 11, 20)));
        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 1, 10), new ChrRangePosition("2", 11, 20)));
        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 11, 20), new ChrRangePosition("1", 1, 10)));
        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 11, 20), new ChrRangePosition("2", 1, 10)));

        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123457, 123457)));
        assertTrue(ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123457, 123457), new ChrRangePosition("1", 123456, 123456)));
		
	}
	
	@Test
	public void positionOnlyOverlap() {
        assertFalse(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 11, 20)));
        assertFalse(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 11, 20), new ChrRangePosition("1", 1, 10)));

        assertTrue(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 10, 20)));
        assertTrue(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 10)));

        assertTrue(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 100), new ChrRangePosition("1", 10, 20)));
        assertTrue(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 100)));
        assertTrue(ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
	}
	
	@Test
	public void getPreceedingPosition() {
		ChrRangePosition cp = new ChrRangePosition("1", 10, 10);
		ChrPosition preceedingCP = ChrPositionUtils.getPrecedingChrPosition(cp);
		ChrPosition preceedingCP2 = ChrPositionUtils.getPrecedingChrPosition(preceedingCP);
        assertTrue(ChrPositionUtils.areAdjacent(cp, preceedingCP));
        assertTrue(ChrPositionUtils.areAdjacent(preceedingCP2, preceedingCP));
        assertFalse(ChrPositionUtils.areAdjacent(preceedingCP2, cp));
        assertFalse(ChrPositionUtils.areAdjacent(cp, preceedingCP2));
	}
	
}
