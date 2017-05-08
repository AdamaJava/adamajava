package org.qcmg.coverage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import htsjdk.samtools.BAMIndexer;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.common.commandline.Executor;
import org.qcmg.gff3.GFF3FileWriter;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.picard.SAMFileReaderFactory;

public class QuerySequenceCoverageTest {
 	
	static String inputBam;
	static String inputBai;
	static Path tmpDir;
	private File fOutput;
	static GFF3Record record;

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	 @BeforeClass
	 public static void setup() throws Exception {
		 tmpDir = Files.createTempDirectory(null);
		 inputBam = Files.createTempFile(tmpDir, null, ".bam").toString();
		 inputBai = inputBam.replace("bam", "bai");
		 SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
		 
		 record = new GFF3Record();
		 record.setSeqId("chr1");
		 record.setType("exon");
		 record.setScore(".");
		 record.setSource(".");
		 record.setStrand("+");
	 }
	 
	 @AfterClass
	 public static void tearDown() {
		tmpDir.toFile().delete();
	 }

	@Before
	public final void before() {
		fOutput = new File(tmpDir.toString() + "/output");
	}

	@After
	public final void after() {
		fOutput.delete();
	}
	
	private String getCmd(int start, int stop) {
		return "--log " + tmpDir + "/logfile -t seq --query Cigar_M>35 --gff3 " + tmpDir + "/test" + start + "-" + stop + ".gff3 --bam " + inputBam + " --bai " + inputBai + " -o " +fOutput.getAbsolutePath();
	}
	private String getExCmd(int start, int stop) {
		return "--log " + tmpDir + "/logfile -t seq --query Cigar_M>45 --gff3 " + tmpDir + "/test" + start + "-" + stop + ".gff3 --bam " + inputBam + " --bai " + inputBai + " -o " +fOutput.getAbsolutePath();
	}

	private File createGFF3File(final int start, final int end) throws IOException {
		record.setStart(start);
		record.setEnd(end);

		File file = new File(tmpDir + "/test" + start +"-" + end + ".gff3");
		try (GFF3FileWriter writer = new GFF3FileWriter(file)) {
			writer.add(record);
		}
		return file;
	}

	private Executor execute(final String command) throws IOException, InterruptedException {
		return new Executor(command, "org.qcmg.coverage.Main");
	}
	
	@Test
	public final void leftDisjointReadSeqCov() throws Exception {
		createGFF3File(54000, 54025);

		ExpectedException.none();
		Executor exec = execute(getCmd(54000, 54025));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("sequence	exon	26	0x", fileContents.get(1));
	}
	
	@Test
	public final void leftDisjointReadSeqCovEx() throws IOException, InterruptedException {
		createGFF3File(54000, 54025);
		
		ExpectedException.none();
		Executor exec = execute(getExCmd(54000, 54025));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("sequence	exon	26	0x", fileContents.get(1));
	}

	@Test
	public final void rightDisjointRead() throws IOException, InterruptedException {
    		createGFF3File(54077, 54120);

		ExpectedException.none();
		Executor exec = execute(getCmd(54077, 54120));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("sequence	exon	44	0x", fileContents.get(1));
		
	}
    
    @Test
    public final void rightDisjointReadEx() throws IOException, InterruptedException {
	    	createGFF3File(54077, 54120);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54077, 54120));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	44	0x", fileContents.get(1));
    }
   
    @Test
	public final void leftOnEndRead() throws IOException, InterruptedException {
    		createGFF3File(54000, 54026);

		ExpectedException.none();
		Executor exec = execute(getCmd(54000, 54026));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("sequence	exon	26	0x", fileContents.get(1));
		assertEquals("sequence	exon	1	1x", fileContents.get(2));
	}
    
    @Test
    public final void leftOnEndReadEx() throws IOException, InterruptedException {
	    	createGFF3File(54000, 54026);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54000, 54026));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	27	0x", fileContents.get(1));
    }

    @Test
	public final void rightOnEndRead() throws IOException, InterruptedException {
    		createGFF3File(54076, 54120);

		ExpectedException.none();
		Executor exec = execute(getCmd(54076, 54120));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("sequence	exon	45	0x", fileContents.get(1));
	}
    
    @Test
    public final void rightOnEndReadEx() throws IOException, InterruptedException {
	    	createGFF3File(54076, 54120);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54076, 54120));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	45	0x", fileContents.get(1));
    }

    @Test
	public final void leftOverlapRead() throws IOException, InterruptedException {
    	createGFF3File(54000, 54036);

		ExpectedException.none();
		Executor exec = execute(getCmd(54000, 54036));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("sequence	exon	26	0x", fileContents.get(1));
		assertEquals("sequence	exon	11	1x", fileContents.get(2));
	}
    
    @Test
    public final void leftOverlapReadEx() throws IOException, InterruptedException {
	    	createGFF3File(54000, 54036);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54000, 54036));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	37	0x", fileContents.get(1));
    }

    @Test
	public final void rightOverlapRead() throws Exception {
    		createGFF3File(54050, 54120);

		ExpectedException.none();
		Executor exec = execute(getCmd(54050, 54120));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("sequence	exon	50	0x", fileContents.get(1));
		assertEquals("sequence	exon	21	1x", fileContents.get(2));
	}
    
    @Test
    public final void rightOverlapReadEx() throws Exception {
	    	createGFF3File(54050, 54120);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54050, 54120));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	71	0x", fileContents.get(1));
    }

    @Test
	public final void subsetRead() throws IOException, InterruptedException {
   		createGFF3File(54030, 54070);

		ExpectedException.none();
		Executor exec = execute(getCmd(54030, 54070));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("sequence	exon	41	1x", fileContents.get(1));
	}
    
    @Test
    public final void subsetReadEx() throws IOException, InterruptedException {
	    	createGFF3File(54030, 54070);
	    	
	    	ExpectedException.none();
	    	Executor exec = execute(getExCmd(54030, 54070));
	    	
	    	assertTrue(0 == exec.getErrCode());
	    	assertTrue(fOutput.exists());
	    	
	    	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().collect(Collectors.toList());
	    	}
	    	
	    	assertEquals(2, fileContents.size());
	    	assertEquals("sequence	exon	41	0x", fileContents.get(1));
    }	

}
