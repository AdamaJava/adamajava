package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class PileupUtilsTest {
	
	@Test
	public void testGetStartPositions() {
		// zero's
		int[] startPositions = PileupUtils.getStartPositions(0, 0, true);
		assertEquals(0, startPositions.length);
		startPositions = PileupUtils.getStartPositions(0, 0, false);
		assertEquals(0, startPositions.length);
		
		// single normal file
		startPositions = PileupUtils.getStartPositions(1, 0, true);
		assertEquals(1, startPositions.length);
		assertEquals(3, startPositions[0]);
		startPositions = PileupUtils.getStartPositions(1, 0, false);
		assertEquals(0, startPositions.length);
		
		// single tumour file
		startPositions = PileupUtils.getStartPositions(0, 1, false);
		assertEquals(1, startPositions.length);
		assertEquals(3, startPositions[0]);
		startPositions = PileupUtils.getStartPositions(0, 1, true);
		assertEquals(0, startPositions.length);
		
		// many normal files
		startPositions = PileupUtils.getStartPositions(16, 0, true);
		assertEquals(16, startPositions.length);
		assertEquals(3, startPositions[0]);
		assertEquals(6, startPositions[1]);
		assertEquals(48, startPositions[15]);
		startPositions = PileupUtils.getStartPositions(16, 0, false);
		assertEquals(0, startPositions.length);
		
		// many tumour files
		startPositions = PileupUtils.getStartPositions(0, 16, false);
		assertEquals(16, startPositions.length);
		assertEquals(3, startPositions[0]);
		assertEquals(6, startPositions[1]);
		assertEquals(48, startPositions[15]);
		startPositions = PileupUtils.getStartPositions(0, 16, true);
		assertEquals(0, startPositions.length);
		
		// 1 of each
		startPositions = PileupUtils.getStartPositions(1, 1, true);
		assertEquals(1, startPositions.length);
		assertEquals(3, startPositions[0]);
		startPositions = PileupUtils.getStartPositions(1, 1, false);
		assertEquals(1, startPositions.length);
		assertEquals(6, startPositions[0]);
		
		// many of each
		startPositions = PileupUtils.getStartPositions(4, 105, true);
		assertEquals(4, startPositions.length);
		assertEquals(3, startPositions[0]);
		assertEquals(12, startPositions[3]);
		startPositions = PileupUtils.getStartPositions(5, 3, false);
		assertEquals(3, startPositions.length);
		assertEquals(18, startPositions[0]);
		assertEquals(24, startPositions[2]);
	}
	
	@Test
	public void testGetCoverageCount() {
		String[] params = null;
		int[] positions = null;
		
		try {
			PileupUtils.getCoverageCount(params, positions);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(0, PileupUtils.getCoverageCount(new String[] {"0"}, new int[] {0}));
		assertEquals(1, PileupUtils.getCoverageCount(new String[] {"1"}, new int[] {0}));
		assertEquals(17, PileupUtils.getCoverageCount(new String[] {"17"}, new int[] {0}));
		assertEquals(3, PileupUtils.getCoverageCount(new String[] {"1","2"}, new int[] {0,1}));
		assertEquals(10, PileupUtils.getCoverageCount(new String[] {"1","2","3","4","5","6","7","8","9","0"}, new int[] {0,8}));
		assertEquals(5, PileupUtils.getCoverageCount(new String[] {"1","2","3","4","5","6","7","8","9","0"}, new int[] {4}));
		assertEquals(0, PileupUtils.getCoverageCount(new String[] {"1","2","3","4","5","6","7","8","9","0"}, new int[] {9}));
		assertEquals(45, PileupUtils.getCoverageCount(new String[] {"1","2","3","4","5","6","7","8","9","0"}, new int[] {0,1,2,3,4,5,6,7,8,9}));
		assertEquals(0, PileupUtils.getCoverageCount(new String[] {"1","2","3","4","5","6","7","8","9","0"}, new int[] {9,9,9,9,9,9,9,9,9,9}));
		
		// try to access a non-existant position in string array
		try {
			assertEquals(3, PileupUtils.getCoverageCount(new String[] {"1","2"}, new int[] {10}));
			Assert.fail("should have thrown an exception");
		} catch (ArrayIndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testGetBases() {
		String[] params = null;
		int[] positions = null;
		
		try {
			PileupUtils.getBases(params, positions);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		params = new String[] {"0","Hello","World","1","Goodbye","Everyone"};
		assertEquals("Hello", PileupUtils.getBases(params, new int[] {0}));
		assertEquals("World", PileupUtils.getBases(params, new int[] {1}));
		assertEquals("Goodbye", PileupUtils.getBases(params, new int[] {3}));
		assertEquals("HelloGoodbye", PileupUtils.getBases(params, new int[] {0,3}));
		
		params = new String[] {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
		assertEquals("b", PileupUtils.getBases(params, new int[] {0}));
		assertEquals("f", PileupUtils.getBases(params, new int[] {4}));
		assertEquals("cd", PileupUtils.getBases(params, new int[] {1,2}));
		assertEquals("bcdefghijk", PileupUtils.getBases(params, new int[] {0,1,2,3,4,5,6,7,8,9}));
		assertEquals("kkkkkkkkkk", PileupUtils.getBases(params, new int[] {9,9,9,9,9,9,9,9,9,9}));
		
		// try to access a non-existant position in string array
		try {
			Assert.assertEquals("3", PileupUtils.getBases(new String[] {"1","2"}, new int[] {10}));
			Assert.fail("should have thrown an exception");
		} catch (ArrayIndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testGetQualities() {
		String[] params = null;
		int[] positions = null;
		
		try {
			PileupUtils.getQualities(params, positions);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		params = new String[] {"0","Hello","World","1","Goodbye","Everyone"};
		assertEquals("World", PileupUtils.getQualities(params, new int[] {0}));
		assertEquals("1", PileupUtils.getQualities(params, new int[] {1}));
		assertEquals("Everyone", PileupUtils.getQualities(params, new int[] {3}));
		assertEquals("WorldEveryone", PileupUtils.getQualities(params, new int[] {0,3}));
		
		params = new String[] {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
		assertEquals("c", PileupUtils.getQualities(params, new int[] {0}));
		assertEquals("g", PileupUtils.getQualities(params, new int[] {4}));
		assertEquals("de", PileupUtils.getQualities(params, new int[] {1,2}));
		assertEquals("cdefghijkl", PileupUtils.getQualities(params, new int[] {0,1,2,3,4,5,6,7,8,9}));
		assertEquals("llllllllll", PileupUtils.getQualities(params, new int[] {9,9,9,9,9,9,9,9,9,9}));
		
		// try to access a non-existant position in string array
		try {
			assertEquals("3", PileupUtils.getQualities(new String[] {"1","2"}, new int[] {10}));
			Assert.fail("should have thrown an exception");
		} catch (ArrayIndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testRealLifeExample() {
		String[] params = {"chr1","10491","C","15",",$,,.,.,,...,,,^f.","IIIAIDIII1IG.II","13",".,..,,.,.,,^|.^#,","III1IIIIII9IG"};
		int[] normalPositions = PileupUtils.getStartPositions(1,1, true);
		int[] tumourPositions = PileupUtils.getStartPositions(1,1, false);
		
		assertEquals(15, PileupUtils.getCoverageCount(params, normalPositions));
		assertEquals(13, PileupUtils.getCoverageCount(params, tumourPositions));
		
		assertEquals(",$,,.,.,,...,,,^f.", PileupUtils.getBases(params, normalPositions));
		assertEquals(".,..,,.,.,,^|.^#,", PileupUtils.getBases(params, tumourPositions));
		
		assertEquals("IIIAIDIII1IG.II", PileupUtils.getQualities(params, normalPositions));
		assertEquals("III1IIIIII9IG", PileupUtils.getQualities(params, tumourPositions));
	}
	
	@Test
	public void testGetNoOfFilesFromPileupFormat() {
		String pileupFormat = null;
		assertEquals(0, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, '\u0000'));
		pileupFormat = "";
		assertEquals(0, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, '\u0000'));
		pileupFormat = "TESTING";
		assertEquals(1, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'N'));
		assertEquals(2, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'T'));
		pileupFormat = "NNNNTTT";
		assertEquals(4, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'N'));
		assertEquals(3, PileupUtils.getNoOfFilesFromPileupFormat(pileupFormat, 'T'));
	}
	
	@Test
	public void testDoesPileupContainIndel() {
		try {
			assertEquals(false, PileupUtils.doesPileupContainIndel(null));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, PileupUtils.doesPileupContainIndel(""));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(false, PileupUtils.doesPileupContainIndel("..."));
		assertEquals(false, PileupUtils.doesPileupContainIndel("...,,,"));
		assertEquals(true, PileupUtils.doesPileupContainIndel("...,,,+1a"));
		assertEquals(false, PileupUtils.doesPileupContainIndel("...,,,^+."));
		assertEquals(true, PileupUtils.doesPileupContainIndel("...,,,^+.,..,.+2ac"));
		assertEquals(false, PileupUtils.doesPileupContainIndel("...,,,^+.,..,.^-.,"));
		assertEquals(false, PileupUtils.doesPileupContainIndel(",,tt,,T.,.T...,T...........T....,T..T.T,t..T......,^PT^M.^-.^F,"));
		assertEquals(true, PileupUtils.doesPileupContainIndel("....+2CG.....G.........ggG.....,,.g.."));
	}
	
	@Test
	public void testContainsCharacterNotPrecededByNewRead() {
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("ABC", 'A'));
		assertEquals(false, PileupUtils.containsCharacterNotPrecededByNewRead("^ABC", 'A'));
		assertEquals(false, PileupUtils.containsCharacterNotPrecededByNewRead("BBC", 'A'));
		assertEquals(false, PileupUtils.containsCharacterNotPrecededByNewRead("^BBC", 'A'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("BBC1", '1'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("^BBC1", '1'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("BB^C1", '1'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("B^B^C1", '1'));
		assertEquals(false, PileupUtils.containsCharacterNotPrecededByNewRead("B^B^C^1", '1'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("B^B^C^11", '1'));
		assertEquals(false, PileupUtils.containsCharacterNotPrecededByNewRead("BBC^1^1", '1'));
		assertEquals(true, PileupUtils.containsCharacterNotPrecededByNewRead("B1BC^1^1", '1'));
	}
	
}
