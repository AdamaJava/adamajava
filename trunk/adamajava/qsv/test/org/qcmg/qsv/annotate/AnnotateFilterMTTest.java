package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

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


public class AnnotateFilterMTTest {
    
    private File normalBam;
    private File tumorBam;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    QSVParameters parameters;
    
    @Rule
    public static TemporaryFolder testFolder = new TemporaryFolder();
    public static File iniFile;
    public static File logFile;
    public static File outputDir;

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
    
    private static String setUpIniFile() throws IOException {
		
		iniFile = testFolder.newFile("test.ini");
		logFile = testFolder.newFile("test.log");
		File controlBam =  testFolder.newFile("control.bam");
		File testBam =  testFolder.newFile("test.bam");
		outputDir = testFolder.newFolder();
		File reference = testFolder.newFile("reference_file");
		if (iniFile.exists()) {
			iniFile.delete();
		}		
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));) {
			out.write("[general]" + Constants.NL);
			out.write("log=" + logFile.getAbsolutePath() + Constants.NL);
			out.write("loglevel=DEBUG" + Constants.NL);
			out.write("sample=test" + Constants.NL);
			out.write("platform=illumina" + Constants.NL);		
			out.write("sv_analysis=pair"+ Constants.NL);		
			out.write("output="+outputDir.getAbsolutePath() + Constants.NL);
			out.write("reference=" + reference.getAbsolutePath() + Constants.NL);
			out.write("platform=illumina" + Constants.NL);
			out.write("min_insert_size=50" + Constants.NL);
	//		out.write("qcmg=true" + Constants.NL);
			
			out.write("[pair]" + Constants.NL);
			out.write("pair_query=and(MAPQ >= 30, Cigar_M > 35, MD_mismatch < 3, Flag_DuplicateRead == false)" + Constants.NL);
			out.write("pairing_type=pe" + Constants.NL);
			out.write("cluster_size=3" + Constants.NL);
			out.write("filter_size=3" + Constants.NL);
	//		out.write("primer_size=3" + Constants.NL);
			out.write("mapper=bwa" + Constants.NL);
	//		out.write("[clip]" + Constants.NL);
	//		out.write("clip_query=and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)" + Constants.NL);
	//		out.write("clip_size=3" + Constants.NL);
	//		out.write("consensus_length=20" + Constants.NL);
	//		out.write("blatpath=/home/Software/BLAT" + Constants.NL);
	//		out.write("blatserver=localhost" + Constants.NL);
	//		out.write("blatport=50000" + Constants.NL);
			
			out.write("["+ QSVConstants.DISEASE_SAMPLE +"]" + Constants.NL);
			out.write("name=TD" + Constants.NL);
	//		out.write("sample_id=ICGC-DBLG-20110506-01-TD" + Constants.NL);
			out.write("input_file=" + testBam.getAbsolutePath()+ Constants.NL);
			
			out.write("["+ QSVConstants.DISEASE_SAMPLE +"/size_1]" + Constants.NL);
		    	out.write("rgid=20110221052813657" + Constants.NL);
		    	out.write("lower=640" + Constants.NL);
		    	out.write("upper=2360" + Constants.NL + Constants.NL);
		//    	out.write("name=seq_mapped_1" + Constants.NL);
		    	
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
		return iniFile.getAbsolutePath();
	}
    
	public static String[] getValidOptions() throws IOException {
			String iniFile = setUpIniFile();
            return new String[] {"--ini", iniFile, "--tmp",  testFolder.getRoot().toString()};
    }
    
    @Test
    public void setupQueryExecutor() throws Exception {
    	AbstractQueue<List<Chromosome>> readQueue = null;
		AbstractQueue<SAMRecord> writeQueue = null;
		AbstractQueue<SAMRecord> writeClipQueue = null;
		Thread mainThread = null;
		CountDownLatch readLatch = null;
		CountDownLatch fLatch = null;
		CountDownLatch wGoodLatch = null;
		
		   String[] args = getValidOptions();
	        Options options = new Options(args);
	        options.parseIniFile();
	        String matepairsDir = null;
			QSVParameters p = new QSVParameters(options, true, testFolder.getRoot().toString() , matepairsDir , new Date(), "test");
		AnnotateFilterMT afmt = new AnnotateFilterMT(Thread.currentThread(), wGoodLatch, p, null, null, options);
    		AnnotateFilterMT.AnnotationFiltering af = afmt.new AnnotationFiltering(readQueue, writeQueue, writeClipQueue, mainThread, readLatch, fLatch, wGoodLatch);
    	
    	try {
    		QueryExecutor exec = new QueryExecutor("and(Cigar_M > 35, option_SM > 10, MD_mismatch < 3, Flag_DuplicateRead == false)");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    @Ignore
    public void testRunPairFilter() throws Exception { 
    	
        AtomicInteger exit = runFilterTest("both", true);        
        assertEquals(0, exit.intValue());
        assertTrue(parameters.getFilteredBamFile().exists());
        assertTrue(parameters.getFilteredBamFile().toString().contains("discordantpair"));
        assertTrue(parameters.getFilteredBamFile().length() > 100);
        assertRecordsFound(17, parameters.getFilteredBamFile()); 
    }
    
    @Ignore
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

	private void assertRecordsFound(int total, File bam) throws IOException {
		SamReader reader =  SAMFileReaderFactory.createSAMFileReader(bam);//new SAMFileReader(bam);
        
        int count = 0;
        
        for (SAMRecord r: reader) {
        	count++;
        }
        
        reader.close();
        assertEquals(total, count);
		
	}
}
