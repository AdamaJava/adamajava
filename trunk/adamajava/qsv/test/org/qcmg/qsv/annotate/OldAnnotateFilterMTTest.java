//package org.qcmg.qsv.annotate;
//
//
//import static org.junit.Assert.*;
//
//import java.io.File;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import net.sf.samtools.SAMFileHeader.SortOrder;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TemporaryFolder;
//import org.qcmg.qsv.QSVParameters;
//import org.qcmg.qsv.annotate.OldAnnotateFilterMT;
//import org.qcmg.qsv.discordantpair.PairGroup;
//import org.qcmg.qsv.util.TestUtil;
//
//
//public class OldAnnotateFilterMTTest {
//    
//    private File normalBam;
//    private File tumorBam;
//    private final CountDownLatch countDownLatch = new CountDownLatch(1);
//    QSVParameters parameters;
//    
//    @Rule
//    public TemporaryFolder testFolder = new TemporaryFolder();
//
//    @Before
//    public void setUp() throws Exception {
//        
//        normalBam = TestUtil.createSamFile(testFolder.newFile("normalBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate, false);
//    	tumorBam = TestUtil.createSamFile(testFolder.newFile("tumorBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate, false);
//    	parameters = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "none");
//    }
//    
//    @After
//    public void tearDown() {
//        parameters = null;
//    }
//
//    @Test
//    public void testRunFilter() throws Exception {        
//        AtomicInteger exit = new AtomicInteger();
//        OldAnnotateFilterMT w = new OldAnnotateFilterMT(Thread.currentThread(), countDownLatch, parameters, exit, "");
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        executorService.execute(w);
//        executorService.shutdown();
//        countDownLatch.await();        
//        assertEquals(0, exit.intValue());
//        assertTrue(parameters.getFilteredBamFile().exists());
//        assertTrue(parameters.getFilteredBamFile().length() > 100);
//    }
//    
//    @Test
//    public void testAnnotationFilterAdd2Queue() throws Exception {        
//        AtomicInteger exit = new AtomicInteger();
//        OldAnnotateFilterMT w = new OldAnnotateFilterMT(Thread.currentThread(), countDownLatch, parameters, exit, "");
//        w.setCheckPoint(5);
//        w.setMaxRecord(10);
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        executorService.execute(w);
//        executorService.shutdown();
//        countDownLatch.await();        
//        assertEquals(0, exit.intValue());
//        assertTrue(parameters.getFilteredBamFile().exists());
//        assertTrue(parameters.getFilteredBamFile().length() > 100);
//    }
//}
