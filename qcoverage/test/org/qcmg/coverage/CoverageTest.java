package org.qcmg.coverage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

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

import htsjdk.samtools.SAMFileHeader.SortOrder;

public class CoverageTest {
	static String inputBam;
	static String inputBai;
	static String inputGff3;
	static Path tmpDir;
	private String fname;
	static Gff3Record record;

	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	
	@BeforeClass
	 public static void setup() throws IOException {
		 tmpDir = Files.createTempDirectory(null);
		 inputBam = Files.createTempFile(tmpDir, null, ".bam").toString();
		 inputBai = inputBam.replace("bam", "bai");
		 SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
		 
		record = new Gff3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");
		
		 
		File file = new File(tmpDir + "/test.gff3");
		try (RecordWriter<Gff3Record> writer = new RecordWriter<>(file)) {
			writer.add(record);
		}
		inputGff3 = file.getAbsolutePath();
	 }
	 
	 @AfterClass
	 public static void tearDown() {
		tmpDir.toFile().delete();
	 }

	@Before
	public final void before() {
		Random rand = new Random();
		int int_random = rand.nextInt(1000); 
	 	fname = tmpDir.toString() + "/output" + int_random;
	}
	
	@Test
	public final void DefaultTest() throws Exception {
		//default value txt output only
		Executor exec = execute(getCmd(""));		
		assertTrue(0 == exec.getErrCode());
				
		File fOutput = new File(fname + ".txt");
		//debug
		System.out.println("output: " + fOutput.getAbsolutePath());
		
		assertTrue(fOutput.exists());
	}
	
	@Test
	public final void XmlTest() throws Exception {
		//xml output only
		Executor exec = execute(getCmd(" --output-format xml"));		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		//debug
		System.out.println("output: " + fOutput.getAbsolutePath());

		assertFalse(fOutput.exists());		
		fOutput = new File(fname + ".xml");
		//debug
		System.out.println("output: " + fOutput.getAbsolutePath());

		assertTrue(fOutput.exists());
	}	
	

 
	
	private String getCmd( String format ) {
		//debug
		System.out.println("getCmd fname: " + fname);
		
		return "--log " + tmpDir + "/logfile --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam +  " --output " +fname + format;
	}

	private Executor execute(final String command) throws Exception {
		//debug
		System.out.println("execute command: " + command);

		return new Executor(command, "org.qcmg.coverage.Coverage");

	}


}
