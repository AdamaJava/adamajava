package org.qcmg.qbasepileup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OptionsTest {
	
	String log;
	String input;
	String reference;
	String output;
	String snps;
	final static String FILE_SEPERATOR = System.getProperty("file.separator");

	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		log = testFolder.newFile("test.log").getAbsolutePath();
		input = testFolder.newFile("input.bam").getAbsolutePath();
		reference = testFolder.newFile("reference.fa").getAbsolutePath();
		testFolder.newFile("reference.fa.fai").getAbsolutePath();
		snps = testFolder.newFile("snps.dcc").getAbsolutePath();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "pileup.txt";
		
	}
	
	@After
	public void tearDown() throws IOException {
		new File(log).delete();
		new File(input).delete();
		new File(reference).delete();
		new File(reference + ".fai").delete();	
		new File(snps).delete();		
	}
	
	@Test
	public void testStandardProfileOptions() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps};
		
		Options options = new Options(args);
		
		assertEquals(log, options.getLog());
		assertEquals(reference, options.getReference().getAbsolutePath());
		assertEquals(1, options.getInputBAMs().size());
		assertEquals(output, options.getOutput().getAbsolutePath());
		assertEquals(snps, options.getPositionsFile().getAbsolutePath());
		assertEquals("standard", options.getProfile());
		assertEquals("dcc1", options.getFormat());
		
		//filtering
		assertTrue(options.isStrandSpecific());
		assertTrue(options.includeIndel());
		assertTrue(options.includeIntron());
		assertNull(options.getMappingQuality());
		assertNull(options.getBaseQuality());	
		assertFalse(options.isNovelstarts());
	}
	
	@Test
	public void testTorrentProfileOptions() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps, "-p", "torrent"};
		
		Options options = new Options(args);
		
		assertEquals("torrent", options.getProfile());
		
		//filtering
		assertFalse(options.isStrandSpecific());
		assertTrue(options.includeIndel());
		assertTrue(options.includeIntron());
		assertFalse(options.isNovelstarts());
		assertEquals(new Integer(1), options.getMappingQuality());
		assertEquals(new Integer(0), options.getBaseQuality());		
	}
	
	@Test
	public void testTorrentProfileOptionsWithModifications() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, 
				"-s", snps, "-p", "torrent", "-strand", "y", "-ind", "n", "-intron", "n", "-bq", "5", "-mq", "10"};
		
		Options options = new Options(args);
		
		assertEquals("torrent", options.getProfile());
		
		//filtering
		assertTrue(options.isStrandSpecific());
		assertFalse(options.includeIndel());
		assertFalse(options.includeIntron());
		assertFalse(options.isNovelstarts());
		assertEquals(new Integer(10), options.getMappingQuality());
		assertEquals(new Integer(5), options.getBaseQuality());		
	}
	
	@Test
	public void testRNAProfileOptions() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps, "-p", "RNA"};
		
		Options options = new Options(args);
		
		assertEquals("RNA", options.getProfile());
		//filtering
		assertFalse(options.isStrandSpecific());
		assertTrue(options.includeIndel());
		assertTrue(options.includeIntron());
		assertTrue(options.isNovelstarts());
		assertEquals(new Integer(10), options.getMappingQuality());
		assertEquals(new Integer(7), options.getBaseQuality());			
	}
	
	@Test
	public void testDNAProfileOptions() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps,  "-p", "DNA"};
		
		Options options = new Options(args);
		
		assertEquals("DNA", options.getProfile());
				
		//filtering
		assertFalse(options.isStrandSpecific());
		assertTrue(options.includeIndel());
		assertFalse(options.includeIntron());
		assertFalse(options.isNovelstarts());
		assertEquals(new Integer(10), options.getMappingQuality());
		assertEquals(new Integer(10), options.getBaseQuality());		
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadOptionBamFile() throws Exception {
		String[] args = {"--log", log, "-i", "fake.bam", "-r", reference, "-o", output, "-s", snps};
		
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadProfile() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps, "-p", "profile"};
		
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadFormat() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps, "-f", "dccw"};
		
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadOptionPositionFile() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", "fakefile"};
		
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadOptionReferenceFile() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", "fakefile", "-o", output, "-s", snps,  "-p", "DNA"};
		
		new Options(args);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testBadOptionReferenceFileIndex() throws Exception {
		String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps,  "-p", "DNA"};
		
		new File(reference + ".fai").delete();
		new Options(args);
	}
	
	
}
