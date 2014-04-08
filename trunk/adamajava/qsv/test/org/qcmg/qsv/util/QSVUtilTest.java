package org.qcmg.qsv.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;

public class QSVUtilTest {
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

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
    	assertTrue(QSVUtil.reorderByChromosomes("chrY", "chrX"));
    	assertTrue(QSVUtil.reorderByChromosomes("chrY", "chr1"));
    	assertTrue(QSVUtil.reorderByChromosomes("chrY", "chrMT"));
    	assertFalse(QSVUtil.reorderByChromosomes("chr1", "chrX"));
    	assertTrue(QSVUtil.reorderByChromosomes("X", "MT"));
    	assertFalse(QSVUtil.reorderByChromosomes("X", "Y"));
    	assertTrue(QSVUtil.reorderByChromosomes("GL4", "GL1"));
    	assertFalse(QSVUtil.reorderByChromosomes("GL1", "GL4"));
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
    	assertTrue(QSVUtil.getAnalysisId(false, "test", new Date(1352958269803L)).contains("qSV_test_20121511_1544"));
    	assertTrue(QSVUtil.getAnalysisId(true, "test", new Date(1352958269803L)).length() == 36);
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
    
    

}
