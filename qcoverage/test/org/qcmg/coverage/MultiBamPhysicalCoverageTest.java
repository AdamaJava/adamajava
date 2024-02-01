package org.qcmg.coverage;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import org.junit.*;
import org.qcmg.common.commandline.Executor;
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiBamPhysicalCoverageTest {
	
	static String inputBam;
	static String inputBai;
	static String inputBam2;
	static String inputBai2;
	static Path tmpDir;
	private File fOutput;
	private String fname;
	static Gff3Record record;
	
	@BeforeClass
	 public static void setup() throws IOException {
		 tmpDir = Files.createTempDirectory(null);
		 inputBam = Files.createTempFile(tmpDir, null, ".bam").toString();
		 inputBai = inputBam.replace("bam", "bai");
		 inputBam2 = Files.createTempFile(tmpDir, null, "2.bam").toString();
		 inputBai2 = inputBam.replace("bam", "bai");
		 SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
		 SequenceCoverageTest.createCoverageBam(inputBam2, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
		 
		record = new Gff3Record();
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
		 	fname = tmpDir.toString() + "/output";
			fOutput = new File(fname + ".txt");
		}

		@After
		public final void after() {
			fOutput.delete();
		}
		
		private String getCmd(int start, int stop) {
			return "--log " + tmpDir + "/logfile --type phys --input-gff3 " + tmpDir + "/test" + start + "-" + stop + ".gff3 --input-bam " + inputBam + " --input-bai " + inputBai + " --input-bam " + inputBam2 + " --input-bai " + inputBai2 + " --output " +fname;
		}

		private void createGFF3File(final int start, final int end) throws IOException {
			record.setStart(start);
			record.setEnd(end);

			File file = new File(tmpDir + "/test" + start +"-" + end + ".gff3");
			try (RecordWriter<Gff3Record> writer = new RecordWriter<>(file)) {
				writer.add(record);
			}
		}

	private Executor execute(final String command) throws IOException, InterruptedException {
		//return new Executor(command, "org.qcmg.coverage.Main");
		return new Executor(command, "org.qcmg.coverage.Coverage");
	}
	
    @Test
	public final void leftDisjointRead() throws IOException, InterruptedException {
		createGFF3File(54000, 54025);
		
		Executor exec = execute(getCmd(54000, 54025));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().toList();
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("physical	exon	26	0x", fileContents.get(1));
	}

    @Test
	public final void rightDisjointRead() throws IOException, InterruptedException {
		createGFF3File(54077, 54120);

		Executor exec = execute(getCmd(54077, 54120));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().toList();
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("physical	exon	44	0x", fileContents.get(1));
	}

    @Test
	public final void leftOnEndRead() throws IOException, InterruptedException {
		createGFF3File(54000, 54026);

		Executor exec = execute(getCmd(54000, 54026));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
	 	List<String> fileContents;
	    	try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
	    		fileContents = r.lines().toList();
	    	}
	    	
	    	assertEquals(3, fileContents.size());
	    	assertEquals("physical	exon	26	0x", fileContents.get(1));
	    	assertEquals("physical	exon	1	2x", fileContents.get(2));
	}
    
    @Test
  	public final void rightOnEndRead() throws Exception {
      	createGFF3File(54076, 54120);

  		Executor exec = execute(getCmd(54076, 54120));

        assertEquals(0, exec.getErrCode());
  		assertTrue(fOutput.exists());
  		
  		List<String> fileContents;
  		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
  			fileContents = r.lines().toList();
  		}
  		
  		assertEquals(2, fileContents.size());
  		assertEquals("physical	exon	45	0x", fileContents.get(1));
  	}

    @Test
	public final void leftOverlapRead() throws IOException, InterruptedException {
    	createGFF3File(54000, 54036);

		Executor exec = execute(getCmd(54000, 54036));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().toList();
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("physical	exon	26	0x", fileContents.get(1));
		assertEquals("physical	exon	11	2x", fileContents.get(2));
	}

    @Test
	public final void rightOverlapRead() throws IOException, InterruptedException {
		createGFF3File(54050, 54120);

		Executor exec = execute(getCmd(54050, 54120));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().toList();
		}
		
		assertEquals(3, fileContents.size());
		assertEquals("physical	exon	50	0x", fileContents.get(1));
		assertEquals("physical	exon	21	2x", fileContents.get(2));

	}
    
    @Test
	public final void subsetRead() throws IOException, InterruptedException {
    	createGFF3File(54030, 54070);

		Executor exec = execute(getCmd(54030, 54070));

        assertEquals(0, exec.getErrCode());
		assertTrue(fOutput.exists());
		
		List<String> fileContents;
		try (BufferedReader r= new BufferedReader( new FileReader(fOutput))) {
			fileContents = r.lines().toList();
		}
		
		assertEquals(2, fileContents.size());
		assertEquals("physical	exon	41	2x", fileContents.get(1));
	}
}
