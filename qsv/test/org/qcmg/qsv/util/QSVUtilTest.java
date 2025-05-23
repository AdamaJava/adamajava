package org.qcmg.qsv.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Date;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;

public class QSVUtilTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Rule
	  public ExpectedException exception = ExpectedException.none();

	@Test
	public void testWriteTime() throws IOException {

		Date date1 = new Date(1323319335382L);
		Date date2 = new Date(1323319339382L);
		assertEquals("Test: 00:00:04", QSVUtil.writeTime("Test: ", date1, date2));
	}

	@Test
	public void testSecondsToString() {
		assertEquals("00:00:04", QSVUtil.secondsToString(4));
		assertEquals("00:05:10", QSVUtil.secondsToString(310));
		assertEquals("00:05:12", QSVUtil.secondsToString(312));
		assertEquals("00:15:10", QSVUtil.secondsToString(910));
		assertEquals("00:05:04", QSVUtil.secondsToString(304));
		assertEquals("01:05:10", QSVUtil.secondsToString(3910));
		assertEquals("11:05:10", QSVUtil.secondsToString(39910));        
	}

	@Test
	public void testRemoveEmptyDirectory() {
		File file = new File(testFolder.getRoot().toString());        

		assertTrue(file.exists());
		assertTrue(file.isDirectory());        
		assertTrue(QSVUtil.removeDirectory(file));
		assertFalse(file.exists());
	}

	@Test
	public void testRemoveListContainingDirectory() throws IOException {
		File file = new File(testFolder.getRoot().toString());        
		File testFile = testFolder.newFile("testFile");
		File testDir = testFolder.newFolder("testDir");

		assertTrue(file.exists());
		assertTrue(testFile.exists());
		assertTrue(testFile.isFile());
		assertTrue(testDir.exists());
		assertTrue(testDir.isDirectory());
		assertTrue(file.isDirectory());        
		assertTrue(QSVUtil.removeDirectory(file));
		assertFalse(file.exists());
	}

	@Test
	public void testRemoveDirectoryWithNonExistingDir() {
		File file = new File("/fake/dir");

		assertFalse(file.exists());
		assertFalse(file.isDirectory());        
		assertFalse(QSVUtil.removeDirectory(file));

		//null directory
		assertFalse(QSVUtil.removeDirectory(null));
	}

	@Test
	public void testRemoveDirectoryWithFile() throws IOException {
		File file = testFolder.newFile("test");

		assertTrue(file.exists());
		assertFalse(file.isDirectory());        
		assertFalse(QSVUtil.removeDirectory(file));       
	}

	@Test
	public void testReorderByChromosome() {
		assertTrue(QSVUtil.reorderByChromosomes("chr2", "chr1"));
		assertFalse(QSVUtil.reorderByChromosomes("chr1", "chr2"));
		assertTrue(QSVUtil.reorderByChromosomes("chr10", "chr2"));
		assertTrue(QSVUtil.reorderByChromosomes("chr12", "chr1"));
		assertFalse(QSVUtil.reorderByChromosomes("chr12", "chr12"));
		assertFalse(QSVUtil.reorderByChromosomes("chr12", "chr13"));
		assertFalse(QSVUtil.reorderByChromosomes("chr12", "chr21"));
		assertTrue(QSVUtil.reorderByChromosomes("chr21", "chr12"));
		assertTrue(QSVUtil.reorderByChromosomes("chrY", "chrX"));
		assertTrue(QSVUtil.reorderByChromosomes("chrY", "chr1"));
		assertFalse(QSVUtil.reorderByChromosomes("chrY", "chrMT"));
		assertFalse(QSVUtil.reorderByChromosomes("chr1", "chrX"));
		assertFalse(QSVUtil.reorderByChromosomes("X", "MT"));
		assertFalse(QSVUtil.reorderByChromosomes("X", "Y"));
		assertTrue(QSVUtil.reorderByChromosomes("GL4", "GL1"));
		assertFalse(QSVUtil.reorderByChromosomes("GL1", "GL4"));
	}
	
	@Test
	public void isReferenceNameComparatorFaster() {
		int count = 100000;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			assertTrue(QSVUtil.reorderByChromosomes("chr2", "chr1"));
		}
		System.out.println("time taken: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < count ; i++) {
			assertTrue(QSVUtil.reorderByChromosomes("chr2", "chr1"));
		}
		System.out.println("time taken2: " + (System.currentTimeMillis() - start));
	}

	@Test
	public void testGetPairGroupByZP() {    	
		assertEquals(PairGroup.AAC, QSVUtil.getPairGroupByZP(PairClassification.AAC));
		assertEquals(PairGroup.BAA_BBA, QSVUtil.getPairGroupByZP(PairClassification.BAA));
		assertEquals(PairGroup.BAA_BBA, QSVUtil.getPairGroupByZP(PairClassification.BBA));
		assertEquals(PairGroup.BAB_BBB, QSVUtil.getPairGroupByZP(PairClassification.BAB));
		assertEquals(PairGroup.BAB_BBB, QSVUtil.getPairGroupByZP(PairClassification.BBB));
		assertEquals(PairGroup.BAC_BBC, QSVUtil.getPairGroupByZP(PairClassification.BAC));
		assertEquals(PairGroup.BAC_BBC, QSVUtil.getPairGroupByZP(PairClassification.BBC));
		assertEquals(PairGroup.AAB, QSVUtil.getPairGroupByZP(PairClassification.AAB));
		assertEquals(PairGroup.ABB, QSVUtil.getPairGroupByZP(PairClassification.ABB));
	}

	@Test
	public void getMutationByPairClassification() {
		assertEquals("DEL/ITX", QSVUtil.getMutationByPairClassification(PairClassification.AAC));
		assertEquals("CTX", QSVUtil.getMutationByPairClassification(PairClassification.Cxx));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BAA));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BBA));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BAB));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BBB));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BAC));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairClassification(PairClassification.BBC));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairClassification(PairClassification.AAB));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairClassification(PairClassification.ABB));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairClassification(PairClassification.ABA));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairClassification(PairClassification.ABC));
	}

	@Test
	public void testGetMutationByPairGroup() {
		assertEquals("DEL/ITX", QSVUtil.getMutationByPairGroup("AAC"));
		assertEquals("CTX", QSVUtil.getMutationByPairGroup("Cxx"));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairGroup("BAC_BBC"));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairGroup("BAA_BBA"));
		assertEquals("INV/ITX", QSVUtil.getMutationByPairGroup("BAB_BBB"));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairGroup("ABC"));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairGroup("ABA"));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairGroup("AAB"));
		assertEquals("DUP/INS/ITX", QSVUtil.getMutationByPairGroup("ABB"));
	}

	@Test
	public void testReverseComplement() {    	
		assertEquals("NCAGT", QSVUtil.reverseComplement("ACTGN"));
	}

	@Test 
	public void testGetAnalysisId() {
		/*
		 * Due to timezone differences, it is safest just to dictate which year we expect the analysis id to start with
		 */
        assertTrue(QSVUtil.getAnalysisId(false, "test", new Date(1352958269803L)).startsWith("qSV_test_2012"));
        assertEquals(36, QSVUtil.getAnalysisId(true, "test", new Date(1352958269803L)).length());
	}

	@Test
	public void testGetPairQueryLMP() {
		String expected = "and(Cigar_M > 35, option_SM > 14, MD_mismatch < 3, flag_DuplicateRead == false)";
		assertEquals(expected, QSVUtil.getPairQuery("lmp", "bioscope"));
	}

	@Test
	public void testGetPairQueryLifescope() {
		String expected = "and(Cigar_M > 35, MD_mismatch < 3, MAPQ > 0, flag_DuplicateRead == false)";
		assertEquals(expected, QSVUtil.getPairQuery("lmp", "lifescope"));
	}

	@Test
	public void testGetPairQueryBWA() {
		String expected = "and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)";
		assertEquals(expected, QSVUtil.getPairQuery("pe", "bwa"));

	}

	@Test
	public void highNCountEmptyString() throws QSVException{
		exception.expect(QSVException.class);
		QSVUtil.highNCount("", 0.0);
	}
	
	@Test
	public void highNCountNullString() throws QSVException {
		exception.expect(QSVException.class);
		QSVUtil.highNCount(null, 0.0);
	}
	
	@Test
	public void highNCountNoNs() throws QSVException {
        assertFalse(QSVUtil.highNCount("Hello there", 0.01));
		// double limit is a percentage, so if set to 0.0, will return true
        assertTrue(QSVUtil.highNCount("Hello there", 0.0));
	}
	
	@Test
	public void highNCountSingleN() throws QSVException {
        assertTrue(QSVUtil.highNCount("Hello, anybody there?", 0.01));	// 1 in 21 >= 1%
        assertTrue(QSVUtil.highNCount("Hello, aNybody there?", 0.01));	// 1 in 21 >= 1%
        assertFalse(QSVUtil.highNCount("Hello, anybody there?", 0.1));	// 1 in 21 ! >= 10%
        assertFalse(QSVUtil.highNCount("Hello, aNybody there?", 0.1));	// 1 in 21 ! >= 10%
	}

	@Test
	public void highNCountAllNs() throws QSVException {
		// shouldn't matter what value is used for the limit
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", 0.01));
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", 0));
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", 1));
	}
	
	@Test
	public void highNCountInvalidLimit() throws QSVException {
		exception.expect(QSVException.class);
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", -0.1));
		exception.expect(QSVException.class);
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", -1));
		exception.expect(QSVException.class);
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", -100));
		exception.expect(QSVException.class);
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", 1.00000001));
		exception.expect(QSVException.class);
        assertTrue(QSVUtil.highNCount("NNnnnNNNNNnnNnNnN", 100000001));
	}
	
	@Test
	public void doesMPOverlapRegion() {
		
		MatePair mp = new MatePair("254_166_1407:20110221052813657,chr7,1000,1100,AAC,129,false,254_166_1407:20110221052813657,chr7,5000,5150,AAC,65,false,F2F1,\n");
        assertTrue(QSVUtil.doesMatePairOverlapRegions(mp, 0, 2000, 3000, 6000));
        assertTrue(QSVUtil.doesMatePairOverlapRegions(mp, 0, 1000, 3000, 6000));
        assertTrue(QSVUtil.doesMatePairOverlapRegions(mp, 0, 1000, 1000, 6000));
        assertTrue(QSVUtil.doesMatePairOverlapRegions(mp, 0, 1000, 5149, 6000));
        assertTrue(QSVUtil.doesMatePairOverlapRegions(mp, 1000, 1100, 5000, 5150));

        assertFalse(QSVUtil.doesMatePairOverlapRegions(mp, 1001, 1099, 5000, 5150));
        assertFalse(QSVUtil.doesMatePairOverlapRegions(mp, 0, 999, 1000, 6000));
        assertFalse(QSVUtil.doesMatePairOverlapRegions(mp, 0, 1000, 6000, 6001));
	}
	
	@Test
	public void createRecord() {
        assertTrue(QSVUtil.createRecord(0, 0, 0));
        assertTrue(QSVUtil.createRecord(0, 0, 10));
        assertTrue(QSVUtil.createRecord(0, -10, 10));

        assertFalse(QSVUtil.createRecord(0, 1, 10));
        assertFalse(QSVUtil.createRecord(0, 10, 10));
        assertFalse(QSVUtil.createRecord(0, 20, 10));
	}
}
