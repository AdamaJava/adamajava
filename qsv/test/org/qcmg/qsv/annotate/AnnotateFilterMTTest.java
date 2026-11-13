package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
		Thread mainThread = null;
		CountDownLatch wGoodLatch = null;

	    String[] args = new String[] {"--ini", iniFile.getAbsolutePath(), "--output-temporary",  testFolder.getRoot().toString()};
	    Options options = new Options(args);
	    options.parseIniFile();
        QSVParameters p = new QSVParameters(options, true, testFolder.getRoot().toString() , "test", null);
		AnnotateFilterMT afmt = new AnnotateFilterMT(wGoodLatch, p, null, null, options);
		afmt.new AnnotationFiltering(mainThread);
    	
    	try {
    		new QueryExecutor("and(Cigar_M > 35, option_SM > 10, MD_mismatch < 3, Flag_DuplicateRead == false)");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    @Test
    public void totalLengthIsTheSpanRead() {
        assertEquals(1000L, AnnotateFilterMT.totalLength(List.of(new Chromosome("chr1", 1000))));
        assertEquals(101L + 1000L, AnnotateFilterMT.totalLength(List.of(new Chromosome("chr1", 5000, 200, 300), new Chromosome("chr1", 5000, 1001, 2000))));
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
        AnnotateFilterMT w = new AnnotateFilterMT(countDownLatch, parameters, new AtomicInteger(), testFolder.getRoot().toString(), options);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(w);
        executorService.shutdown();
        countDownLatch.await();
		return exit;
	}

	/*
	 * Filtering test and control through the shared pool must give exactly the same
	 * filtered BAM records and .clip files as filtering each BAM on its own.
	 */
	@Test
	public void sharedPoolMatchesSeparateRuns() throws Exception {
		sharedPoolMatchesSeparateRuns(null);
	}

	/*
	 * as above, with the filter_threads option set (more threads than chromosome groups, and just 1)
	 */
	@Test
	public void sharedPoolWithFilterThreadsMatchesSeparateRuns() throws Exception {
		sharedPoolMatchesSeparateRuns(7);
		sharedPoolMatchesSeparateRuns(1);
	}

	private void sharedPoolMatchesSeparateRuns(Integer threads) throws Exception {
		File separateDir = testFolder.newFolder("separate" + threads);
		File sharedDir = testFolder.newFolder("shared" + threads);

		List<QSVParameters> separate = filterPair(separateDir, false, null);
		List<QSVParameters> shared = filterPair(sharedDir, true, threads);

		int pairRecords = 0;
		for (int i = 0; i < separate.size(); i++) {
			List<String> expectedPairs = samStrings(separate.get(i).getFilteredBamFile());
			pairRecords += expectedPairs.size();
			assertEquals(expectedPairs, samStrings(shared.get(i).getFilteredBamFile()));
			assertEquals(samStrings(separate.get(i).getClippedBamFile()), samStrings(shared.get(i).getClippedBamFile()));
		}
		assertTrue("expected some discordant pair records", pairRecords > 0);

		Map<String, String> expectedClips = clipFiles(separateDir);
		assertTrue("expected some .clip files", expectedClips.size() > 0);
		assertEquals(expectedClips, clipFiles(sharedDir));
	}

	private List<QSVParameters> filterPair(File dir, boolean sharedPool, Integer threads) throws Exception {
		File tumour = TestUtil.createHiseqBamFile(new File(dir, "tumour.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		File normal = TestUtil.createHiseqBamFile(new File(dir, "normal.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		QSVParameters tp = TestUtil.getQSVParameters(dir, normal.getAbsolutePath(), tumour.getAbsolutePath(), true, "both");
		QSVParameters np = TestUtil.getQSVParameters(dir, normal.getAbsolutePath(), tumour.getAbsolutePath(), false, "both");
		Options options = new Options(TestUtil.getValidOptions(dir, normal.getAbsolutePath(), tumour.getAbsolutePath(), "both", "both"));
		options.parseIniFile();

		AtomicInteger exit = new AtomicInteger();
		AnnotateFilterMT t = new AnnotateFilterMT(null, tp, exit, dir.getAbsolutePath(), options);
		AnnotateFilterMT n = new AnnotateFilterMT(null, np, exit, dir.getAbsolutePath(), options);
		if (sharedPool) {
			AnnotateFilterMT.runAll(List.of(t, n), threads);
		} else {
			t.run();
			n.run();
		}
		assertEquals(0, exit.intValue());
		return List.of(tp, np);
	}

	private static List<String> samStrings(File bam) throws IOException {
		List<String> records = new ArrayList<>();
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bam)) {
			for (SAMRecord r : reader) {
				records.add(r.getSAMString());
			}
		}
		Collections.sort(records);
		return records;
	}

	private static Map<String, String> clipFiles(File dir) throws IOException {
		Map<String, String> contents = new TreeMap<>();
		File[] files = dir.listFiles((d, name) -> name.endsWith(".clip"));
		if (null != files) {
			for (File f : files) {
				contents.put(f.getName(), Files.readString(f.toPath()));
			}
		}
		return contents;
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
