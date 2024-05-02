package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.ini4j.InvalidFileFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.TestUtil;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;


public class AnnotateFilterMTTest {
    
    private File normalBam;
    private File tumorBam;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    QSVParameters parameters;
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    public static File iniFile;
    public static File logFile;
    public static File outputDir;

    @Before
    public void setUp() throws IOException {        
        normalBam = testFolder.newFile("normal.bam"); 
        iniFile = testFolder.newFile("test.ini");
        setUpIniFile();
    }
    
    @After
    public void tearDown() {
        parameters = null;
        tumorBam = null;
        normalBam = null;
    }
    
    private void setUpIniFile() throws IOException {
		
		File logFile = testFolder.newFile("test.log");
		File controlBam =  testFolder.newFile("control.bam");
		File testBam =  testFolder.newFile("test.bam");
		outputDir = testFolder.newFolder();
		File reference = testFolder.newFile("reference_file");
				
		try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile))) {
			out.write("[general]" + Constants.NL);
			out.write("log=" + logFile.getAbsolutePath() + Constants.NL);
			out.write("loglevel=DEBUG" + Constants.NL);
			out.write("sample=test" + Constants.NL);
			out.write("platform=illumina" + Constants.NL);		
			out.write("sv_analysis=pair"+ Constants.NL);		
			out.write("output="+outputDir.getAbsolutePath() + Constants.NL);
			out.write("reference=" + reference.getAbsolutePath() + Constants.NL);
			out.write("tiled_aligner=" + reference.getAbsolutePath() + Constants.NL);
			out.write("platform=illumina" + Constants.NL);
			out.write("min_insert_size=50" + Constants.NL);
			
			out.write("[pair]" + Constants.NL);
			out.write("pair_query=and(MAPQ >= 30, Cigar_M > 35, MD_mismatch < 3, Flag_DuplicateRead == false)" + Constants.NL);
			out.write("pairing_type=pe" + Constants.NL);
			out.write("cluster_size=3" + Constants.NL);
			out.write("filter_size=3" + Constants.NL);
			out.write("mapper=bwa" + Constants.NL);
			
			out.write("["+ QSVConstants.DISEASE_SAMPLE +"]" + Constants.NL);
			out.write("name=TD" + Constants.NL);
			out.write("input_file=" + testBam.getAbsolutePath()+ Constants.NL);
			
			out.write("["+ QSVConstants.DISEASE_SAMPLE +"/size_1]" + Constants.NL);
	    	out.write("rgid=20110221052813657" + Constants.NL);
	    	out.write("lower=640" + Constants.NL);
	    	out.write("upper=2360" + Constants.NL + Constants.NL);
	    	
	    	out.write("["+ QSVConstants.CONTROL_SAMPLE +"]" + Constants.NL);
	    	out.write("name=ND" + Constants.NL);
	    	out.write("sample_id=ICGC-DBLG-20110506-01-ND" + Constants.NL);
			out.write("input_file=" + controlBam.getAbsolutePath() + Constants.NL);
	    	out.write("["+ QSVConstants.CONTROL_SAMPLE +"/size_1]" + Constants.NL);
	    	out.write("rgid=20110221052813657" + Constants.NL);
	    	out.write("lower=640" + Constants.NL);
	    	out.write("upper=2360" + Constants.NL + Constants.NL);
	    	out.write("name=seq_mapped_1" + Constants.NL);
		}
		 
	}
    
    /*
     * doesn't test anything as far as I can tell
     */
    @Ignore
    public void setupQueryExecutor() throws Exception {
    	AbstractQueue<List<Chromosome>> readQueue = null;
		AbstractQueue<SAMRecord> writeQueue = null;
		AbstractQueue<SAMRecord> writeClipQueue = null;
		Thread mainThread = null;
		CountDownLatch fLatch = null;
		CountDownLatch wGoodLatch = null;
		
	    String[] args = new String[] {"--ini", iniFile.getAbsolutePath(), "--output-temporary",  testFolder.getRoot().toString()};
	    Options options = new Options(args);
	    options.parseIniFile();
	    String matepairsDir = null;
		QSVParameters p = new QSVParameters(options, true, testFolder.getRoot().toString() , "test", null);
		AnnotateFilterMT afmt = new AnnotateFilterMT(Thread.currentThread(), wGoodLatch, p, null, null, options);
		afmt.new AnnotationFiltering(readQueue, writeQueue, writeClipQueue, mainThread, fLatch, wGoodLatch);
    	
    	try {
    		new QueryExecutor("and(Cigar_M > 35, option_SM > 10, MD_mismatch < 3, Flag_DuplicateRead == false)");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
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
        File [] files =  f.listFiles();
        assertEquals(false, null == files);
        for (File file: files) {
        	if (file.getName().endsWith(".clip")) {
        		count++;
        	}
        }
        assertEquals(10, count); 
    }
    
    @Ignore
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
        File [] files =  f.listFiles();
        assertEquals(false, null == files);
        for (File file: files) {
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
       	
        
        parameters = TestUtil.getQSVParameters(testFolder.getRoot(), normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "both");

        String[] args = TestUtil.getValidOptions(testFolder.getRoot(), normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), mode, "both");
        Options options = new Options(args);
        options.parseIniFile();
        AnnotateFilterMT w = new AnnotateFilterMT(Thread.currentThread(), countDownLatch, parameters, new AtomicInteger(), testFolder.getRoot().toString(), options);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(w);
        executorService.shutdown();
        countDownLatch.await();
		return exit;
	}

	private void assertRecordsFound(int total, File bam) throws IOException {
		int count = 0;
		try (SamReader reader =  SAMFileReaderFactory.createSAMFileReader(bam)) {
	        for (SAMRecord r: reader) {
        		count++;
	        }
		}
        assertEquals(total, count);
	}
}
