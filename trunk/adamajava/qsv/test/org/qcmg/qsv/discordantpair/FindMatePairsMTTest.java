package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.discordantpair.FindMatePairsMT;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.util.TestUtil;

public class FindMatePairsMTTest {

    
    private static final String FILE_SEPERATOR = System.getProperty("file.separator");
    private FindMatePairsMT findMatePairs = null;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private List<SAMRecord> records = new ArrayList<SAMRecord>();
	private String matePairDir;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
    		File tumorBam = TestUtil.createSamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), SortOrder.queryname, false);
        File normalBam = TestUtil.createSamFile(testFolder.newFile("normal.bam").getAbsolutePath(), SortOrder.queryname, false);
        QSVParameters p = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "none", "pair");
        matePairDir = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "matepair" + FILE_SEPERATOR;        
        
        findMatePairs = new FindMatePairsMT(Thread.currentThread(), countDownLatch, p, new AtomicInteger(), "test", matePairDir, true);

        records = TestUtil.createSamBodyRecords(SortOrder.unsorted);
        
    }

    @After
    public void tearDown() {
        findMatePairs = null;
        matePairDir = null;
    }

//    @Test
//    public void testRun() throws InterruptedException, IOException {
//    	 
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        executorService.execute(findMatePairs);
//        executorService.shutdown();
//        countDownLatch.await();
//       
//        File sortedFile = findMatePairs.getQsvParameters().getFilteredBamFile();
//        assertTrue(sortedFile.exists());
//        assertTrue(sortedFile.length() > 0);        
//        File matePairsFile = new File(matePairDir + FILE_SEPERATOR
//                + "AAC" + FILE_SEPERATOR + "chr7-1_test_AAC");
//
//        assertTrue(matePairsFile.exists());
//        assertTrue(matePairsFile.length() > 0);       
//        List<MatePair> pairs = TestUtil.readInMatePairs(matePairsFile);
//        assertEquals(1, pairs.size());
//
//        File matePairsFileCxx = new File(matePairDir + FILE_SEPERATOR + "Cxx" + FILE_SEPERATOR + "chr7-chrX-2_test_Cxx");
//        assertTrue(matePairsFileCxx.exists());
//        assertTrue(matePairsFileCxx.length() > 0);
//
//        List<MatePair> pairsCxx = TestUtil.readInMatePairs(matePairsFileCxx);
//        assertEquals(1, pairsCxx.size());
//    }

    @Test
    public void testSetUpPairClassificationWriters() throws Exception {
        assertEquals(0, findMatePairs.getMatePairWritersMap().size());
        findMatePairs.setUpPairingClassificationWriters();
        assertEquals(12, findMatePairs.getMatePairWritersMap().size());

        assertEquals(testFolder.getRoot().toString() + FILE_SEPERATOR + "matepair" + FILE_SEPERATOR + "AAC" 
        + FILE_SEPERATOR, findMatePairs.getMatePairWritersMap().get(PairClassification.AAC).getDirToWrite());
        assertEquals("_test_AAC", findMatePairs.getMatePairWritersMap().get(PairClassification.AAC).getFileName());
    }

    @Test
    public void testAddMatePairToWriter() throws Exception {
        findMatePairs.setUpPairingClassificationWriters();

        findMatePairs.addMatePairToWriter(records.get(0), records.get(1));        
        assertEquals(1, findMatePairs.getMatePairWritersMap().get(PairClassification.ABC).getMatePairs().get("chr7-2").size());
    }

    @Test
    public void testAddMatePairToWriterWithCxx() throws Exception {
        findMatePairs.setUpPairingClassificationWriters();

        findMatePairs.addMatePairToWriter(records.get(2), records.get(3));
        assertEquals(1, findMatePairs.getMatePairWritersMap().get(PairClassification.Cxx).getMatePairs().get("chr4-chr7-4").size());
    }
    
    @Test
    public void passesZPFilter() {
    	 	SAMRecord record1 = records.get(0);
    	 	assertEquals(true, FindMatePairsMT.passesZPFilter(record1));
    	 	record1.setAttribute("ZP", "Z**");
    	 	assertEquals(false, FindMatePairsMT.passesZPFilter(record1));
    	 	record1.setAttribute("ZP", "X**");
    	 	assertEquals(false, FindMatePairsMT.passesZPFilter(record1));
    	 	record1.setAttribute("ZP", "**X");
    	 	assertEquals(false, FindMatePairsMT.passesZPFilter(record1));
    	 	record1.setAttribute("ZP", "XXX");
    	 	assertEquals(false, FindMatePairsMT.passesZPFilter(record1));
    }

    @Test
    public void testMateFiltering() throws IOException {

        assertEquals(12, records.size());

        assertTrue(findMatePairs.passesMateFiltering(records.get(0)));
        assertTrue(findMatePairs.passesMateFiltering(records.get(1)));
        
        // Reference name not chromosome
        SAMRecord record = records.get(0);
		record.setReferenceName("GPL001");
        assertFalse(findMatePairs.passesMateFiltering(record));
        
        // Mate Reference name not chromosome
        SAMRecord record1 = records.get(0);
		record.setMateReferenceName("GPL001");
		assertFalse(findMatePairs.passesMateFiltering(record1));
        
		//Mate unmapped
        SAMRecord record2 = records.get(0);
		record.setMateUnmappedFlag(true);
		assertFalse(findMatePairs.passesMateFiltering(record2));        
        
        // Reference name is chrMT
		SAMRecord record3 = records.get(0);
		record.setReferenceName("chrMT");
		assertFalse(findMatePairs.passesMateFiltering(record3));
        
        // ZP contains X
		SAMRecord record4 = records.get(0);
		record.setAttribute("ZP", "AXB");
		assertFalse(findMatePairs.passesMateFiltering(record4));
    }
}
