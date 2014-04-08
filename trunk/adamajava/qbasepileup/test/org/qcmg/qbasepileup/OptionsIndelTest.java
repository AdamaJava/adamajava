package org.qcmg.qbasepileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OptionsIndelTest {
	
	String log;
	String tumourBam;
	String normalBam;
	String reference;
	String output;
	String input;
	private String inputGerm;
	private String outputGerm;
	final static String FILE_SEPERATOR = System.getProperty("file.separator");

	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	
	@Before
	public void setUp() throws IOException {
		log = testFolder.newFile("test.log").getAbsolutePath();
		tumourBam = testFolder.newFile("tumour.bam").getAbsolutePath();
		normalBam = testFolder.newFile("normal.bam").getAbsolutePath();
		reference = testFolder.newFile("reference.fa").getAbsolutePath();
		testFolder.newFile("reference.fa.fai").getAbsolutePath();
		input = testFolder.newFile("input.dcc1").getAbsolutePath();
		inputGerm = testFolder.newFile("inputGerm.dcc1").getAbsolutePath();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
		outputGerm = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
	}
	
	@After
	public void tearDown() throws IOException {
		new File(log).delete();
		new File(tumourBam).delete();
		new File(normalBam).delete();
		new File(input).delete();
		new File(inputGerm).delete();
		new File(reference).delete();
		new File(reference + ".fai").delete();	
		output = null;
		outputGerm = null;
	}
	
	@Test
	public void testGoodOptions() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
		"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel"};
		
		Options options = new Options(args);
		
		assertEquals(log, options.getLog());
		assertEquals(reference, options.getReference().getAbsolutePath());		
		assertTrue(options.hasPindelOption());
		assertEquals(tumourBam, options.getTumourBam().getBamFile().getAbsolutePath());
		assertEquals(normalBam, options.getNormalBam().getBamFile().getAbsolutePath());
		assertEquals(input, options.getSomaticIndelFile().getAbsolutePath());
		assertEquals(inputGerm, options.getGermlineIndelFile().getAbsolutePath());
		assertEquals(output, options.getSomaticOutputFile().getAbsolutePath());
		assertEquals(outputGerm, options.getGermlineOutputFile().getAbsolutePath());
		assertEquals("indel", options.getMode());
		
		//default windows
		assertEquals(13, options.getSoftClipWindow());
		assertEquals(3, options.getNearbyIndelWindow());
		assertEquals(10, options.getNearbyHomopolymerWindow());
	}
	
	@Test
	public void testGoodOptionsNewWindowCounts() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
		"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		
		Options options = new Options(args);
		
		//default windows
		assertEquals(10, options.getSoftClipWindow());
		assertEquals(4, options.getNearbyIndelWindow());
		assertEquals(7, options.getNearbyHomopolymerWindow());
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testBadTumourBamOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", "fake", "-in", normalBam, "-r", reference, "--pindel", 
				"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testBadNormalBamOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", "fake", "-r", reference, "--pindel", 
				"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testBadReferenceFileOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", "fake", "-r", "fake", "--pindel", 
				"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testSomaticInputFileOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
				"-is", "fake", "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testGermlineInputFileOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
				"-is", input, "-os", output, "-ig", "fake", "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testSomaticOutputFileOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
				"-is", input, "-os", testFolder.newFile().getAbsolutePath(), "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testGermlineOutputFileOptionThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, "--pindel", 
				"-is", input, "-os", output, "-ig", inputGerm, "-og", testFolder.newFile().getAbsolutePath(), "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class) 
	public void testFileModeThrowsException() throws Exception {
		String[] args = {"--log", log, "-it", tumourBam, "-in", normalBam, "-r", reference, 
				"-is", input, "-os", output, "-ig", inputGerm, "-og", outputGerm, "-m", "indel", "-sc", "10", "-hp", "7", "-n", "4"};
		new Options(args);
	}
	
	


}
