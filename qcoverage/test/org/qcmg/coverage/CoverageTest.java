package org.qcmg.coverage;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.qio.gff3.Gff3Record;
import org.qcmg.qio.record.RecordWriter;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

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
		fname = testFolder.getRoot().getAbsolutePath()+"/output.txt";
	 } 
	
	@Test
	public final void defaultTest() throws Exception {
		
 	 	
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " + fname ;

		//default value txt output only
		Executor exec = execute(cmd);
		assertEquals(0, exec.getErrCode());
				
		File fOutput = new File(fname);		
		assertTrue(fOutput.exists());
	}
	
	@Test
	public final void xmlTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + " --output-format xml";

		//xml output only
		Executor exec = execute(cmd);
		assertEquals(0, exec.getErrCode());
		
		File fOutput = new File(fname);
		assertFalse(fOutput.exists());
		fOutput = new File(fname + ".txt");
		assertFalse(fOutput.exists());
		
		
		fOutput = new File(fname + ".xml");
		assertTrue(fOutput.exists());
	}

	@Test
	public void coverageStatsXml() throws JAXBException {
		jakarta.xml.bind.JAXBContext context = JAXBContextFactory
				.createContext(new Class[] {CoverageReport.class, CoverageModel.class, QCoverageStats.class}, null);
		final Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

		CoverageModel cm = new CoverageModel();
		cm.setAt(String.valueOf(1));

		cm.setBases(BigInteger.valueOf(1));
		CoverageReport cr = new CoverageReport();
		cr.getCoverage().add(cm);
		cr.setFeature("feature_1");
		cr.setType(CoverageType.PHYSICAL);

		QCoverageStats qcs = new QCoverageStats();
		qcs.getCoverageReport().add(cr);
		StringWriter sw = new StringWriter();

		m.marshal(qcs, sw);
		StringBuffer sb = sw.getBuffer();
		assertEquals(0, sb.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?><QCoverageStats><coverageReport feature=\"feature_1\" " +
				"type=\"PHYSICAL\"><coverage bases=\"1\" at=\"1\"/></coverageReport></QCoverageStats>"));
	}

	@Test
	public void writeXmlOutput() throws IOException, ParserConfigurationException, jakarta.xml.bind.JAXBException {
		QCoverageStats qcs = new QCoverageStats();
		CoverageReport cr = new CoverageReport();
		for (int i = 0 ; i < 10 ; i++) {
			CoverageModel cm = new CoverageModel();
			cm.setAt(String.valueOf(i));
			cm.setBases(BigInteger.valueOf(i));
			cr.getCoverage().add(cm);
		}
		qcs.getCoverageReport().add(cr);
		File file = testFolder.newFile("writeXmlOutput.xml");
		Coverage.writeXMLCoverageReport(qcs, file.getAbsolutePath());
		List<String> fileOutput = Files.readAllLines(file.toPath());
		assertEquals(15, fileOutput.size());
	}
	
	@Test
	public final void vcfTest() throws Exception {
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + " --output-format vcf";

		//run fail for vcf without per-feature
		Executor exec = execute(cmd);
		assertEquals(1, exec.getErrCode());
		
		File fOutput = new File(fname );
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
		assertEquals(0, exec.getErrCode());
		
		File fOutput = new File(fname );
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
				" --output-format xml  --output-format txt";

		//two types output
		Executor exec = execute(cmd);
		assertEquals(0, exec.getErrCode());
		
		File fOutput = new File(fname );
		assertTrue(fOutput.exists());	
		fOutput = new File(fname + ".xml");
		assertTrue(fOutput.exists());			
		fOutput = new File(fname + ".vcf");
		assertFalse(fOutput.exists());
		
		//run fail for vcf without per-feature
		cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " +fname  + 
				" --output-format vcf  --output-format xml  --output-format txt";
		exec = execute(cmd);
		assertEquals(1, exec.getErrCode());
		
	}	

	
	@Test
	public final void allFeatureTest() throws Exception {
		String foutput = fname + ".xml";
		String cmd = "--log " + log + " --type phys  --input-gff3 " + inputGff3 + " --input-bam " + inputBam + 
				" --input-bai " + inputBai +  " --output " + foutput + 
				" --per-feature --output-format vcf  --output-format xml  --output-format txt";

		//three types output
		Executor exec = execute(cmd);
		assertEquals(0, exec.getErrCode());
		
		//append txt to output.txt.xml  
		File fOutput = new File(foutput + ".txt");
		assertTrue(fOutput.exists());	
		
		//keep output.txt.xml 
		fOutput = new File(foutput );
		assertTrue(fOutput.exists());
		
		//append vcf to output.txt.xml  
		fOutput = new File(foutput + ".vcf");
		assertTrue(fOutput.exists());
	}	

	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.coverage.Coverage");
	}
}
