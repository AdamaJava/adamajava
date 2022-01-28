package org.qcmg.coverage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

import htsjdk.samtools.SAMFileHeader.SortOrder;

public class CoverageTest {
	private String inputBam;
	private String inputBai;
	private String inputGff3;
	private String fname;
	private String log;

	
	 @Rule
	 public TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Before
	 public void setup() throws IOException {
		 inputBam = testFolder.newFile("coverage.bam").getAbsolutePath();
		 inputBai = inputBam.replace("bam", "bai");
		 SequenceCoverageTest.createCoverageBam(inputBam, SequenceCoverageTest.getAACSAMRecords(SortOrder.coordinate), SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
		 
			Gff3Record record = new Gff3Record();
			record.setSeqId("chr1");
			record.setType("exon");
			record.setStart(100);
			record.setEnd(102);
			record.setScore(".");
			record.setSource(".");
			record.setStrand("+");
		
		 
		File file = testFolder.newFile("test.gff3");  
		try (RecordWriter<Gff3Record> writer = new RecordWriter<>(file)) {
			writer.add(record);
		}
		inputGff3 = file.getAbsolutePath();
		log = inputBam.replace("bam", "log");
		fname = testFolder.getRoot().getAbsolutePath()+"/output";
	 } 
	
	@Test
	public final void defaultTest() throws Exception {
		
 	 	
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname ;

		//default value txt output only
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
				
		File fOutput = new File(fname + ".txt");		
		assertTrue(fOutput.exists());
	}
	
	@Test
	public final void xmlTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + " --output-format xml";

		//xml output only
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		assertFalse(fOutput.exists());		
		fOutput = new File(fname + ".xml");
		assertTrue(fOutput.exists());
	}
	
	@Test
	public final void vcfTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + " --output-format vcf";

		//no output only
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		assertFalse(fOutput.exists());	
		fOutput = new File(fname + ".xml");
		assertFalse(fOutput.exists());			
		fOutput = new File(fname + ".vcf");
		assertFalse(fOutput.exists());
	}	
	
	@Test
	public final void vcfFeatureTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + " --per-feature --output-format vcf";
	
		//vcf output only
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		assertFalse(fOutput.exists());	
		fOutput = new File(fname + ".xml");
		assertFalse(fOutput.exists());	
		
		fOutput = new File(fname + ".vcf");
		assertTrue(fOutput.exists());
	}		
	
	@Test
	public final void allTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + 
				" --output-format vcf  --output-format xml  --output-format txt";

		//two types output
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		assertTrue(fOutput.exists());	
		fOutput = new File(fname + ".xml");
		assertTrue(fOutput.exists());			
		fOutput = new File(fname + ".vcf");
		assertFalse(fOutput.exists());
	}	

	
	@Test
	public final void allFeatureTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + 
				" --per-feature --output-format vcf  --output-format xml  --output-format txt";

		//three types output
		Executor exec = execute(cmd);		
		assertTrue(0 == exec.getErrCode());
		
		File fOutput = new File(fname + ".txt");
		assertTrue(fOutput.exists());	
		fOutput = new File(fname + ".xml");
		assertTrue(fOutput.exists());			
		fOutput = new File(fname + ".vcf");
		assertTrue(fOutput.exists());
	}	

	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.coverage.Coverage");
	}
}
