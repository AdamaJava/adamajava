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
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

public class PerFeatureSequenceCoverageTest {
	static String inputBam;
	static String inputBai;
	static Path tmpDir;
	private File fOutput;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@BeforeClass
	 public static void setup() throws IOException {
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
		return "--log " + tmpDir + "/logfile --type seq --per-feature --input-gff3 " + tmpDir + "/test" + start + "-" + stop + ".gff3 --input-bam " + inputBam + " --bam-index " + inputBai + " --output" +fOutput.getAbsolutePath();
	}

	private File createGFF3File(final int start, final int end) throws IOException {
		Gff3Record record = new Gff3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setStart(start);
		record.setEnd(end);
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");

		File file = new File(tmpDir + "/test" + start +"-" + end + ".gff3");
		try (RecordWriter<Gff3Record> writer = new RecordWriter<>(file)) {
			writer.add(record);
		}
		
		return file;
	}

	private Executor execute(final String command) throws Exception {
		//return new Executor(command, "org.qcmg.coverage.Main");
		return new Executor(command, "org.qcmg.coverage.Coverage");

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
		
		assertEquals(3, fileContents.size());
		assertEquals("#chr1	.	exon	54000	54025	.	+	null	", fileContents.get(1));
		assertEquals("sequence	26	0x", fileContents.get(2));
		
	}

    @Test
	public final void rightDisjointRead() throws Exception {
    		createGFF3File(54077, 54120);

		ExpectedException.none();
		Executor exec = execute(getCmd(54077, 54120));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("#chr1	.	exon	54077	54120	.	+	null	", fileContents.get(1));
		assertEquals("sequence	44	0x", fileContents.get(2));
	}

    @Test
	public final void rightOnEndRead() throws Exception {
    		createGFF3File(54076, 54120);

		ExpectedException.none();
		Executor exec = execute(getCmd(54076, 54120));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("#chr1	.	exon	54076	54120	.	+	null	", fileContents.get(1));
		assertEquals("sequence	45	0x", fileContents.get(2));
	}

    @Test
	public final void leftOverlapRead() throws Exception {
    	createGFF3File(54000, 54036);

		ExpectedException.none();
		Executor exec = execute(getCmd(54000, 54036));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(4, fileContents.size());
		assertEquals("#chr1	.	exon	54000	54036	.	+	null	", fileContents.get(1));
		assertEquals("sequence	26	0x", fileContents.get(2));
		assertEquals("sequence	11	1x", fileContents.get(3));
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
		
		assertEquals(4, fileContents.size());
		assertEquals("#chr1	.	exon	54050	54120	.	+	null	", fileContents.get(1));
		assertEquals("sequence	50	0x", fileContents.get(2));
		assertEquals("sequence	21	1x", fileContents.get(3));
	}

    
    @Test
	public final void subsetRead() throws Exception {
    	createGFF3File(54030, 54070);

		ExpectedException.none();
		Executor exec = execute(getCmd(54030, 54070));
		
		assertTrue(0 == exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().collect(Collectors.toList());
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("#chr1	.	exon	54030	54070	.	+	null	", fileContents.get(1));
		assertEquals("sequence	41	1x", fileContents.get(2));
	}

}
