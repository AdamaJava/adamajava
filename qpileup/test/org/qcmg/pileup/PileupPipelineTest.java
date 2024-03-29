package org.qcmg.pileup;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.util.TestUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PileupPipelineTest {

	private final String reference = Objects.requireNonNull(getClass().getResource("/resources/test-reference.fa")).getFile();
	private final String bam = Objects.requireNonNull(getClass().getResource("/resources/test.bam")).getFile();
	private final String existingHDF = Objects.requireNonNull(getClass().getResource("/resources/test.h5")).getFile();
	private String hdf;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		hdf = testFolder.getRoot().getAbsolutePath() + PileupConstants.FILE_SEPARATOR + "test.h5";
	}
	
	@After
	public void tearDown() {
		new File(hdf).delete();
		
	}
	
	@Test
	public void testModes() throws Exception {
		
		testBootstrapMode();
		
		testAddMode();	
			
		testReadMode();	
		
		testRemoveMode();	
		
	}
	
	@Test
	public void testMergeMode() throws Exception {
		var path = System.getProperty("java.library.path");
		System.out.println("path:" + path);
		String mergeHDF = testFolder.getRoot().toString() + PileupConstants.FILE_SEPARATOR + "merge.h5";
		Options options = TestUtil.getValidOptions(testFolder, "merge", reference, mergeHDF, bam, testFolder.getRoot().toString(), "all", existingHDF);
		options.parseIniFile();
		PileupPipeline pipeline = new PileupPipeline(options, 1234);
		pipeline.runPipeline();
		assertTrue(new File(mergeHDF).exists());

		PrintStream defaultOutstream = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        QPileup pileup = new QPileup();
		pileup.runPileup(TestUtil.getViewArgs(testFolder, mergeHDF, "chr1:12000-12300", false), 1234);
        baos.flush();
        String whatWasPrinted = baos.toString();
        String expected = "chr1\t12000\tT\t0\t0\t0\t8\t0\t0\t0\t0\t320\t0\t8\t8\t0\t0\t2\t0\t0\t0\t6\t8\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t2\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t2\t0\t0\t0";
        String[] linesOfOutput = whatWasPrinted.split(System.getProperty("line.separator"));
        System.setOut(defaultOutstream);
        assertEquals(expected, linesOfOutput[17]);
        
	}	

	private void testBootstrapMode() throws Exception {
		runPipeline("bootstrap");		
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		pileupHDF.open();
		List<String> list = pileupHDF.getRootGroupMembers();
		assertEquals(2, list.size());
		assertEquals("chr1", list.get(0));
		assertEquals("chr11", list.get(1));
		pileupHDF.close();	
	}

	private void testAddMode() throws Exception {
		runPipeline("add");
		
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		pileupHDF.open();
		int [] array = (int[]) pileupHDF.readDatasetBlock("chr1/forward/baseG", 35741, 1);
		assertEquals(2, array[0]);		
		pileupHDF.close();		
	}
	
	private void testReadMode() throws Exception {
		runPipeline("view");		
		File[] listFiles = new File(testFolder.getRoot().getAbsolutePath()).listFiles();
		String s = "";

		assert listFiles != null;
		for (File f: listFiles) {
			if (f.getAbsolutePath().contains("qpileup")) {
				s = f.getAbsolutePath();
			}
		}
		if (!s.equals("")) {	
			assertTrue(new File(s + PileupConstants.FILE_SEPARATOR + "chr1_1_69930.qpileup.csv").exists());
			assertTrue(new File(s + PileupConstants.FILE_SEPARATOR + "chr11_1_69930.qpileup.csv").exists());
		} else {
			Assert.fail();
		}
	}
	
	private void testRemoveMode() throws Exception {
		runPipeline("remove");
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		pileupHDF.open();
		int [] array = (int[]) pileupHDF.readDatasetBlock("chr1/forward/baseG", 35741, 1);
		assertEquals(0, array[0]);		
		pileupHDF.close();		
	}
	
	private void runPipeline(String mode) throws Exception {
		Options options = TestUtil.getValidOptions(testFolder, mode, reference, hdf, bam, testFolder.getRoot().toString(), "all", "");
		options.parseIniFile();
		PileupPipeline pipeline = new PileupPipeline(options, 1234);
		pipeline.runPipeline();
		assertTrue(new File(hdf).exists());
	}
}
