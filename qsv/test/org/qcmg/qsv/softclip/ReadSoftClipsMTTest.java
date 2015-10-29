//package org.qcmg.qsv.softclip;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.File;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import htsjdk.samtools.SAMFileHeader.SortOrder;
//
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TemporaryFolder;
//import org.qcmg.qsv.QSVParameters;
//import org.qcmg.qsv.discordantpair.PairGroup;
//import org.qcmg.qsv.softclip.ReadSoftClipsMT;
//import org.qcmg.qsv.util.QSVUtil;
//import org.qcmg.qsv.util.TestUtil;
//
//public class ReadSoftClipsMTTest {
//
//	File file;
//	
//	@Rule
//    public TemporaryFolder testFolder = new TemporaryFolder();
//
//	
//	@Test
//	public void testReadSoftClipsWorker() throws Exception {
//		File tumorBam = TestUtil.createHiseqBamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
//		TestUtil.createHiseqBamFile(testFolder.newFile("tumor.softclip.filtered.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
//        File normalBam = TestUtil.createHiseqBamFile(testFolder.newFile("normal.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
//
//        QSVParameters tumor = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "pair");
//        File softClipDir = testFolder.getRoot(); 
//		CountDownLatch countDownLatch = new CountDownLatch(1);
//		ReadSoftClipsMT worker = new ReadSoftClipsMT(countDownLatch, tumor, softClipDir.getAbsolutePath(), false, "both");
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        executorService.submit(worker);
//        executorService.shutdown();
//        countDownLatch.await();
//        assertEquals(0, worker.getExitStatus().intValue());     
//        assertTrue(new File(testFolder.getRoot().toString() + QSVUtil.getFileSeperator() + "TD.chr19.clip").exists());
//        assertTrue(new File(testFolder.getRoot().toString() + QSVUtil.getFileSeperator() + "TD.chr7.clip").exists());
//        assertTrue(new File(testFolder.getRoot().toString() + QSVUtil.getFileSeperator() + "TD.chr10.clip").exists());
//        assertTrue(new File(testFolder.getRoot().toString() + QSVUtil.getFileSeperator() + "TD.chr10.clip").length() > 0);
//        assertTrue(new File(testFolder.getRoot().toString() + QSVUtil.getFileSeperator() + "TD.chr7.clip").length() == 0);
//	}
//	
//	
//
//}
