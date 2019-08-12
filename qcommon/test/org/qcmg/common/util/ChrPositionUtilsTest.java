package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;

public class ChrPositionUtilsTest {
	
	
	@Test
	public void testDelta() {
		ChrRangePosition cp1 = new ChrRangePosition("1", 100, 200);
		ChrRangePosition cp2 = new ChrRangePosition("1", 100, 200);
		assertEquals(true, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		
		cp2 = new ChrRangePosition("1", 110, 200);
		assertEquals(false, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 100, 205);
		assertEquals(false, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 102, 203);
		assertEquals(false, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 103, 202);
		assertEquals(false, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 98, 203);
		assertEquals(false, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
		cp2 = new ChrRangePosition("1", 98, 202);
		assertEquals(true, ChrPositionUtils.arePositionsWithinDelta(cp1, cp2, 4));
	}
	
//	@Test
//	public void cloneWithNewName() {
//		ChrRangePosition cp = new ChrRangePosition("1", 100, 200);
//		assertEquals("chr1", ChrPositionUtils.cloneWithNewChromosomeName(cp, "chr1").getChromosome());
//		assertEquals("chrXY", ChrPositionUtils.cloneWithNewChromosomeName(cp, "chrXY").getChromosome());
//		assertEquals("myCP", ChrPositionUtils.cloneWithNewChromosomeName(cp, "myCP").getChromosome());
//		assertEquals("1", ChrPositionUtils.cloneWithNewChromosomeName(cp, "1").getChromosome());
//		
//	}
	
	@Test
	public void getNewchrName() {

		assertEquals("1", ChrPositionUtils.getNewchrName(new ChrRangePosition("1", 100, 200), true ).getChromosome());
		assertEquals("chr1", ChrPositionUtils.getNewchrName(new ChrRangePosition("1", 100, 200), false ).getChromosome());
		assertEquals("CHRXY", ChrPositionUtils.getNewchrName( new ChrRangePosition("CHRXY", 100, 200), true ).getChromosome());
		assertEquals("chrXY", ChrPositionUtils.getNewchrName( new ChrRangePosition("CHRXY", 100, 200), false ).getChromosome());		
		assertEquals("CHRM", ChrPositionUtils.getNewchrName( new ChrRangePosition("CHRM", 100, 200), true ).getChromosome());
		assertEquals("chrMT", ChrPositionUtils.getNewchrName( new ChrRangePosition("CHRM", 100, 200), false ).getChromosome());		
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
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 300, 400), 100));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlap(new ChrRangePosition("1", 300, 400), cp1,100));
		
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlap(cp1, new ChrRangePosition("1", 300, 400), 99));
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlap(new ChrRangePosition("1", 300, 400), cp1,99));
		
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
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp2));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp1, cp3));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp2, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp4));
		assertEquals(true, ChrPositionUtils.isChrPositionContained(cp2, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	@Test
	public void containedDifferentChr() {
		ChrRangePosition cp1 = new ChrRangePosition("chr1", 1000, 2000);
		ChrRangePosition cp2 = new ChrRangePosition("chr2", 1999, 2001);
		ChrRangePosition cp3 = new ChrRangePosition("chr3", 1999, 2000);
		ChrRangePosition cp4 = new ChrRangePosition("chr4", 2000, 2001);
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp2));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp2, cp3));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp1, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp2, cp4));
		assertEquals(false, ChrPositionUtils.isChrPositionContained(cp4, cp2));
	}
	
	
	@Test
	public void arePositionsAdjacent() {
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("2", 11, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 12, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 10, 20)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 10)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
		assertEquals(false, ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 123456, 123456), new ChrRangePosition("1", 123457, 123457)));
		
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 11, 20)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 1, 10), new ChrRangePosition("2", 11, 20)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 11, 20), new ChrRangePosition("1", 1, 10)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("2", 11, 20), new ChrRangePosition("2", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123457, 123457)));
		assertEquals(true, ChrPositionUtils.areAdjacent(new ChrRangePosition("1", 123457, 123457), new ChrRangePosition("1", 123456, 123456)));
		
	}
	
	@Test
	public void positionOnlyOverlap() {
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 11, 20)));
		assertEquals(false, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 11, 20), new ChrRangePosition("1", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 10), new ChrRangePosition("1", 10, 20)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 10)));
		
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 1, 100), new ChrRangePosition("1", 10, 20)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 10, 20), new ChrRangePosition("1", 1, 100)));
		assertEquals(true, ChrPositionUtils.doChrPositionsOverlapPositionOnly(new ChrRangePosition("1", 123456, 123456), new ChrRangePosition("1", 123456, 123456)));
	}
	
	@Test
	public void getPreceedingPosition() {
		ChrRangePosition cp = new ChrRangePosition("1", 10, 10);
		ChrPosition preceedingCP = ChrPositionUtils.getPrecedingChrPosition(cp);
		ChrPosition preceedingCP2 = ChrPositionUtils.getPrecedingChrPosition(preceedingCP);
		assertEquals(true, ChrPositionUtils.areAdjacent(cp, preceedingCP));
		assertEquals(true, ChrPositionUtils.areAdjacent(preceedingCP2, preceedingCP));
		assertEquals(false, ChrPositionUtils.areAdjacent(preceedingCP2, cp));
		assertEquals(false, ChrPositionUtils.areAdjacent(cp, preceedingCP2));
	}
	
	
	@Test
	public void testGetFullChromosome() {
		
		assertEquals("chr10", ChrPositionUtils.ChrNameConveter("10"));
		assertEquals("chr10", ChrPositionUtils.ChrNameConveter("chr10"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("M"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("MT"));
		assertEquals("GL12345", ChrPositionUtils.ChrNameConveter("GL12345"));
	}
	
	@Test
	public void testAddChromosomeReference() {
		
		assertEquals("chr10", ChrPositionUtils.ChrNameConveter("10"));
		assertEquals("23", ChrPositionUtils.ChrNameConveter("23"));
		assertEquals("chr22", ChrPositionUtils.ChrNameConveter("22"));
		assertEquals("99", ChrPositionUtils.ChrNameConveter("99"));
		assertEquals("100", ChrPositionUtils.ChrNameConveter("100"));
		assertEquals("chr1", ChrPositionUtils.ChrNameConveter("1"));
		assertEquals("chrY", ChrPositionUtils.ChrNameConveter("Y"));
		assertEquals("chrX", ChrPositionUtils.ChrNameConveter("X"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("M"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("MT"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("chrMT"));
		assertEquals("chrMT", ChrPositionUtils.ChrNameConveter("chrM"));
		assertEquals("MTT", ChrPositionUtils.ChrNameConveter("MTT"));
		assertEquals("GL123", ChrPositionUtils.ChrNameConveter("GL123"));
		assertEquals("chr10", ChrPositionUtils.ChrNameConveter("chr10"));
	}
	
}
