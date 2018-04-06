package org.qcmg.sig;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.sig.model.BaseReadGroup;
import org.qcmg.vcf.VCFFileReader;

public class SignatureGeneratorBespokeTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	public SignatureGeneratorBespoke qss;
	
	@Before
	public void setup() {
		qss = new SignatureGeneratorBespoke();
		qss.logger = QLoggerFactory.getLogger(SignatureGeneratorBespokeTest.class);
	}
	
	@Test
	public void getEncodedDistString() {
		assertEquals(null, SignatureGeneratorBespoke.getEncodedDist(null));
		assertEquals(null, SignatureGeneratorBespoke.getEncodedDist(new ArrayList<>()));
		
		List<BaseReadGroup> list = new ArrayList<>();
		list.add(new BaseReadGroup('A',""));
		assertEquals("1-0-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('A',""));
		assertEquals("2-0-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('T',""));
		assertEquals("2-0-0-1", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('T',""));
		assertEquals("2-0-0-2", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('C',""));
		assertEquals("0-1-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('G',""));
		assertEquals("0-0-1-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('T',""));
		assertEquals("0-0-0-1", SignatureGeneratorBespoke.getEncodedDist(list));
		
	}
	
	@Test
    public void runProcessWithHG19BamFile() throws Exception {
	    	final File positionsOfInterestFile = testFolder.newFile("runProcessWithHG19BamFile.snps.txt");
	    	final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithHG19BamFile.illuminaarray.txt");
	    	final File bamFile = testFolder.newFile("runProcessWithHG19BamFile.bam");
	    	final File logFile = testFolder.newFile("runProcessWithHG19BamFile.log");
	    	final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
	    	final File outputFile = new File(outputFIleName);
	    	
	//    	writeSnpChipFile(snpChipFile);
	    SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
	    SignatureGeneratorTest.writeIlluminaArraysDesignFile(illuminaArraysDesignFile);
	    SignatureGeneratorTest.getBamFile(bamFile, true, false);
	    	
	    	final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(),  "-illuminaArraysDesign" , illuminaArraysDesignFile.getAbsolutePath()} );
	    	assertEquals(0, exitStatus);
	    	
	    	assertTrue(outputFile.exists());
	   	
	    	final List<VcfRecord> recs = new ArrayList<>();
	    	try (VCFFileReader reader = new VCFFileReader(outputFile);) {    			
		    	for (final VcfRecord rec : reader) {
		    		recs.add(rec);
		    		System.out.println("rec: " + rec.toString());
		    	}
	    	}
	       	
	    	assertEquals(6, recs.size());
    }

}
