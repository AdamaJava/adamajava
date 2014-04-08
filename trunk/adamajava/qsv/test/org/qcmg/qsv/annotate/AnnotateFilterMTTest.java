package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.ini4j.InvalidFileFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.annotate.AnnotateFilterMT;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;


public class AnnotateFilterMTTest {
    
    private File normalBam;
    private File tumorBam;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    QSVParameters parameters;
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {        
      	normalBam = testFolder.newFile("normal.bam");    	
    }
    
    @After
    public void tearDown() {
        parameters = null;
        tumorBam = null;
        normalBam = null;
    }

    @Test
    public void testRunPairFilter() throws Exception { 
    	
        AtomicInteger exit = runFilterTest("both", true);        
        assertEquals(0, exit.intValue());
        assertTrue(parameters.getFilteredBamFile().exists());
        assertTrue(parameters.getFilteredBamFile().toString().contains("discordantpair"));
        assertTrue(parameters.getFilteredBamFile().length() > 100);
        assertRecordsFound(17, parameters.getFilteredBamFile()); 
    }
    
    @Test
    public void testRunClipFilter() throws Exception {        
        AtomicInteger exit = runFilterTest("clip", true);
        
        File f = new File(testFolder.getRoot().getAbsolutePath());
        assertEquals(0, exit.intValue());
        int count = 0;
        for (File file: f.listFiles()) {
        	if (file.getName().endsWith(".clip")) {        		
        		count++;
        	}
        }
        assertEquals(10, count); 
    }
    
    @Test
    public void testRunBothFilter() throws Exception {        
    	AtomicInteger exit = runFilterTest("both", true);
    	assertEquals(0, exit.intValue()); assertEquals(0, exit.intValue());
        assertTrue(parameters.getFilteredBamFile().exists());
        assertTrue(parameters.getFilteredBamFile().toString().contains("discordantpair"));
        assertTrue(parameters.getFilteredBamFile().length() > 100);
        assertRecordsFound(17, parameters.getFilteredBamFile()); 
        File f = new File(testFolder.getRoot().getAbsolutePath());
        assertEquals(0, exit.intValue());
        int count = 0;
        for (File file: f.listFiles()) {
        	if (file.getName().endsWith(".clip")) {        		
        		count++;
        	}
        }
        assertEquals(10, count); 
    }
    

	private AtomicInteger runFilterTest(String mode, boolean isHiseq) throws IOException, Exception,
			QSVException, InvalidFileFormatException, InterruptedException {
		AtomicInteger exit = new AtomicInteger();
		if (isHiseq) {
			 tumorBam = TestUtil.createHiseqBamFile(testFolder.newFile("tumorBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		} else {
			tumorBam = TestUtil.createBamFile(testFolder.newFile("tumorBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		}
       	
        
        parameters = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "both");

        String[] args = TestUtil.getValidOptions(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), mode, "both");
        Options options = new Options(args);
        options.parseIniFile();
        AnnotateFilterMT w = new AnnotateFilterMT(Thread.currentThread(), countDownLatch, parameters, new AtomicInteger(), testFolder.getRoot().toString(), options);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(w);
        executorService.shutdown();
        countDownLatch.await();
		return exit;
	}

	private void assertRecordsFound(int total, File bam) {
		SAMFileReader reader = new SAMFileReader(bam);
        
        int count = 0;
        
        for (SAMRecord r: reader) {
        	count++;
        }
        
        reader.close();
        assertEquals(total, count);
		
	}
}
