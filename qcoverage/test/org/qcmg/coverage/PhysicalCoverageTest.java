package org.qcmg.coverage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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

public class PhysicalCoverageTest {
	final String inputSam1 = "coverage.sam"; 
	final String inputBam1 = "coverage.bam";
	final String inputIndex1 = "coverage.bai";
	final String inputSam2 = "coverage2.sam"; 
	final String inputBam2 = "coverage2.bam";
	final String inputIndex2 = "coverage2.bai";
	final String output = "output";
	final String gff3 = "test.gff3";
	static String inputBam;
	static String inputBai;
	static String cmd;
	static Path tmpDir;
	private File fOutput;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	 @BeforeClass
	 public static void setup() throws Exception {
		 tmpDir = Files.createTempDirectory(null);
		 inputBam = Files.createTempFile(tmpDir, null, ".bam").toString();
		 inputBai = inputBam.replace("bam", "bai");
		 SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
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
		return "--log " + tmpDir + "/logfile -t phys --gff3 " + tmpDir + "/test" + start + "-" + stop + ".gff3 --bam " + inputBam + " --bai " + inputBai + " -o " +fOutput.getAbsolutePath();
	}

	private File createGFF3File(final int start, final int end) throws IOException {
		GFF3Record record = new GFF3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setStart(start);
		record.setEnd(end);
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");

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
	public  void leftDisjointRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	26	0x", fileContents.get(1));
	}

    @Test
	public  void rightDisjointRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	44	0x", fileContents.get(1));
	}

   
    @Test
	public  void leftOnEndRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	26	0x", fileContents.get(1));
		assertEquals("physical	exon	1	1x", fileContents.get(2));

	}

    @Test
	public  void rightOnEndRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	45	0x", fileContents.get(1));
	}

    @Test
	public  void leftOverlapRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	26	0x", fileContents.get(1));
		assertEquals("physical	exon	11	1x", fileContents.get(2));

	}

    @Test
	public  void rightOverlapRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	50	0x", fileContents.get(1));
		assertEquals("physical	exon	21	1x", fileContents.get(2));
	}

    @Test
	public  void subsetRead() throws IOException, InterruptedException {
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
		assertEquals("physical	exon	41	1x", fileContents.get(1));
	}

}
