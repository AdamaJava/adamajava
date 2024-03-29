package org.qcmg.pileup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.metrics.Metric;
import org.qcmg.pileup.util.TestUtil;

import java.io.File;
import java.util.Objects;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptionsTest {
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private final String reference = Objects.requireNonNull(getClass().getResource("/resources/test-reference.fa")).getFile();
	private final String bam = Objects.requireNonNull(getClass().getResource("/resources/test.bam")).getFile();
	private final String hdf = Objects.requireNonNull(getClass().getResource("/resources/test.h5")).getFile();
	
	@Test
	public void testValidBootstrapOptions() throws Exception {		
		Options options = TestUtil.getValidOptions(testFolder, "bootstrap", reference, "test.h5", bam, testFolder.getRoot().toString(), "all", "");
		options.parseIniFile();
		assertEquals("bootstrap", options.getMode());
		assertEquals("INFO", options.getLogLevel());
		assertEquals("test.h5", options.getHdfFile());
		assertEquals(reference, options.getReferenceFile());
		
		//we don't know the sub folder name of temporaryFolder, have to work around
		String tmpDir = new File( options.getIniFile()).getParent();
		assertEquals(tmpDir + PileupConstants.FILE_SEPARATOR + "log", options.getLog());
		
	}
	
	@Test
	public void testValidAddOptions() throws Exception {
		
		Options options = TestUtil.getValidOptions(testFolder, "add", reference, hdf, bam, testFolder.getRoot().toString(), "all", "");
		
		options.parseIniFile();
		assertEquals("add", options.getMode());
		assertEquals("INFO", options.getLogLevel());
		assertEquals(hdf, options.getHdfFile());
		assertEquals(1, options.getBamFiles().size());
		
		//we don't know the sub folder name of temporaryFolder, have to work around
		String tmpDir = new File( options.getIniFile()).getParent();
		assertEquals(tmpDir + PileupConstants.FILE_SEPARATOR + "log", options.getLog());

	}
	
	@Test
	public void testValidViewOptions() throws Exception {
		Options options = TestUtil.getValidOptions(testFolder, "view", reference, hdf, bam, testFolder.getRoot().toString(), "all", "");
		options.parseIniFile();
		assertEquals("view", options.getMode());
		assertEquals("INFO", options.getLogLevel());
		assertEquals(hdf, options.getHdfFile());
		assertEquals(testFolder.getRoot().toString(), options.getOutputDir());
		assertEquals("all", options.getReadRanges().get(0));
		
		//we don't know the sub folder name of temporaryFolder, have to work around
		String tmpDir = new File( options.getIniFile()).getParent();
		assertEquals(tmpDir + PileupConstants.FILE_SEPARATOR + "log", options.getLog());		
	}
	
	@Test
	public void testValidMergeOptions() throws Exception {
		String mergeHDF = testFolder.getRoot().toString() + PileupConstants.FILE_SEPARATOR + "merge.h5";
		Options options = TestUtil.getValidOptions(testFolder, "merge", reference, mergeHDF, bam, testFolder.getRoot().toString(), "all", hdf);
		options.parseIniFile();
		assertEquals("merge", options.getMode());
		assertEquals("INFO", options.getLogLevel());
		assertEquals(mergeHDF, options.getHdfFile());
		assertEquals(2, options.getInputHDFFiles().size());
		assertEquals(hdf, options.getInputHDFFiles().get(0));
		
		//we don't know the sub folder name of temporaryFolder, have to work around
		String tmpDir = new File( options.getIniFile()).getParent();
		assertEquals(tmpDir + PileupConstants.FILE_SEPARATOR + "log", options.getLog());
		
	}
	
	@Test
	public void testValidMetricsOptions() throws Exception {		
		Options options = TestUtil.getValidOptions(testFolder, "metrics", reference, hdf, bam, testFolder.getRoot().toString(), "all", null);
		options.parseIniFile();
		assertEquals("metrics", options.getMode());
		assertEquals("INFO", options.getLogLevel());
		assertEquals(hdf, options.getHdfFile());
		TreeMap<String, Metric> metrics = options.getSummaryMetric().getMetrics();
		assertEquals(6, metrics.size());
		assertTrue(metrics.containsKey(PileupConstants.METRIC_CLIP));
		assertTrue(metrics.containsKey(PileupConstants.METRIC_INDEL));
		assertTrue(metrics.containsKey(PileupConstants.METRIC_NONREFBASE));
		assertTrue(metrics.containsKey(PileupConstants.METRIC_MAPPING));
		assertTrue(metrics.containsKey(PileupConstants.METRIC_STRAND_BIAS));
		assertTrue(metrics.containsKey(PileupConstants.METRIC_SNP));	
		
		//we don't know the sub folder name of temporaryFolder, have to work around
		String tmpDir = new File( options.getIniFile()).getParent();
		assertEquals(tmpDir + PileupConstants.FILE_SEPARATOR + "log", options.getLog());
	}
	
	@Test(expected=QPileupException.class)
	public void testBadOutputDir() throws Exception {
		assertQPileupException("view", reference, hdf, bam, "a", "all");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadHDF() throws Exception {
		assertQPileupException("add", reference, "test.h5", bam, testFolder.getRoot().toString(), "all");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadModeOptions() throws Exception {
		assertQPileupException("badmode", reference, hdf, bam, testFolder.getRoot().toString(), "all");
	}
	
	@Test(expected=QPileupException.class)
	public void testNoBamFile() throws Exception {
		assertQPileupException("add", reference, hdf, "a", testFolder.getRoot().toString(), "all");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadReadRanges() throws Exception {
		assertQPileupException("read", reference, hdf, bam, testFolder.getRoot().toString(), "chr1;4");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadReadRangesBadPatternA() throws Exception {
		assertQPileupException("read", reference, hdf, bam, testFolder.getRoot().toString(), "chr1:14");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadMergeHDFExists() throws Exception {		
		assertQPileupException("merge", reference, hdf, bam, testFolder.getRoot().toString(), "chr1:1-100");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadBootstrapHDFExists() throws Exception {		
		assertQPileupException("bootstrap", reference, hdf, bam, testFolder.getRoot().toString(), "chr1:1-100");
	}
	
	@Test(expected=QPileupException.class)
	public void testBadViewOptionNoHDF() throws Exception {		
		assertQPileupViewException(TestUtil.getViewArgs(testFolder, "a", "chr1:1-100", false));
	}
	
	@Test(expected=QPileupException.class)
	public void testBadViewOptionBadReadRange() throws Exception {
		assertQPileupViewException(TestUtil.getViewArgs(testFolder, hdf, "all", false));
	}
	
	@Test(expected=QPileupException.class)
	public void testBadViewOptionBadElement() throws Exception {
		String[] args = {"--view", "--hdf", hdf, "--range", "chr1", "--element", "B"};
		assertQPileupViewException(args);
	}	
	
	@Test(expected=QPileupException.class)
	public void testBadViewOptionBadGroup() throws Exception {
		String[] args = {"--view", "--hdf", hdf, "--range", "chr1", "--group", "B"};
		assertQPileupViewException(args);
	}
	
	@Test//(expected=QPileupException.class)
	public void testBothViewINIOption() throws Exception {
		for (String opt : new String[] {"view",  "hdf", "range", "group", "element"}) {
			String[] args = {"--" + opt, "--ini", "ini"};
			try {
				assertQPileupViewException(args);
			} catch (QPileupException exp) {
				assertEquals(exp.getMessage(), new QPileupException("INI_VIEW_OPTIONE_ERROR", opt).getMessage() );
			}
		}
	}	
	
	private void assertQPileupViewException(String[] args) throws Exception {				
		Options options = new Options (args);
		options.detectBadOptions();		
	}
		
	private void assertQPileupException(String mode, String reference, String hdf, String bam, String outputDir, String readRange) throws Exception {
		String[] args = TestUtil.getArgs(testFolder, mode, reference, hdf, bam, outputDir, readRange, "");
	
		Options options = new Options (args);
		options.parseIniFile();
	}

}
