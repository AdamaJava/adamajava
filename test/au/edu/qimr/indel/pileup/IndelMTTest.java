package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert.*;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.picard.SAMFileReaderFactory;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Options;
import au.edu.qimr.indel.Q3IndelException;
import au.edu.qimr.indel.pileup.IndelMT.contigPileup;


public class IndelMTTest {
	static final String TEST_BAM_NAME = "test.bam";
	static final String TEST_INI_NAME = "test1.ini";
	static final String query = "and (cigar_M > 50)";
	
	@Before
	public void before() {
		File bam = new File(TEST_BAM_NAME)	;
		contigPileupTest.createSam(TEST_BAM_NAME);
		
		ReadIndelsTest.createVcf();
		File vcf1 = new File(ReadIndelsTest.input1);
		File vcf2 = new File(ReadIndelsTest.input2);
		
		//fake ref and make test and control point to same bam
		File ini =  IniFileTest.fini;	
		IniFileTest.createIniFile(ini, ini, bam,bam,vcf1,vcf2,null);	
		//dodgy index file of reference
		contigPileupTest.createSam(IniFileTest.ini + ".fai");
		
		ini = new File(TEST_INI_NAME)	;	
		IniFileTest.createIniFile(ini, ini, bam,bam,vcf1,vcf2,query);			
		//dodgy index file of reference
		contigPileupTest.createSam(TEST_INI_NAME  + ".fai");
	}
	
	@After
	public void clear() throws IOException {
		File dir = new java.io.File( "." ).getCanonicalFile();		
		for(File f: dir.listFiles())
		    if(f.getName().endsWith(".fai")  ||  f.getName().endsWith(".ini")  ||f.getName().endsWith(".vcf") || f.getName().endsWith(".bam")    )
		        f.delete();
		
	}
	
	@Test
	public void myTestWithQuery(){
		String[] args = {"-i", IniFileTest.ini};
		 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery() == null);
			IndelMT mt = new IndelMT(options, options.getLogger());
			mt.process(2,false);
		//check output	
			
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
	void myTestNoQuery(){
		
		String[] args = {"-i", TEST_INI_NAME}; 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery().equals(query));
			IndelMT mt = new IndelMT(options, options.getLogger());
			mt.process(2,false);
			//check output
			
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
 
 
 

}
