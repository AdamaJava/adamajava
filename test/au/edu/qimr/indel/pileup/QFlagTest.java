package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Support;

public class QFlagTest {
	
	public static final String test_vcf = "test.vcf";  
	public static final String control_vcf = "control.vcf";
	public static final String TEST_BAM_NAME = "test.bam";
	public static final String CONTROL_BAM_NAME = "control.bam";
	public static final String ini_noquery = "test1.ini";
	public static final String ini_query = "test2.ini";
	public static final String query = "and (Flag_DuplicateRead==false, CIGAR_M>150, MD_mismatch <= 3)";
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void before() {
		//input VCF
		List<String> data = new ArrayList<>();
		Support.createVcf(data, control_vcf);  //no records
        data.add("chrY	2672735	.	ATT	A	123.86	.	GATKINFO	GT	0/1"); 	
        Support.createVcf(data, test_vcf);  //potential somatic record
	}
	
	@After
	public void clear() throws IOException {		
		Support.clear();	
	}
	
	@Test
	public void  gematic_soiTest() throws IOException{
        
        //getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
        IndelMTTest.createDelBam( TEST_BAM_NAME);
        
        //control bam only contain one read which is supporting but not strong
        List<String> data = new ArrayList<>();
        data.add("ST-E00139:1112:a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^AT100G0G21^TT9T");
        Support.createBam(data, CONTROL_BAM_NAME);
        
		File ini = testFolder.newFile();
		File outputVcfFile = new File(testFolder.getRoot() + "/output.vcf"); 
		IniFileTest.createIniFile(ini, new File(TEST_BAM_NAME), new File(CONTROL_BAM_NAME), new File(test_vcf), new File(control_vcf), null , outputVcfFile);	        
		Support.runQ3IndelNoHom( ini.getAbsolutePath());
		 
		//gemline since control 100% supporting even only one record	
		try (VCFFileReader reader = new VCFFileReader(outputVcfFile)){ 
			for (VcfRecord re : reader) {				
				assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
				assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("0,1,1,0[0,0],1[1],0,0,1"));
				
				//germline reads
				assertFalse(re.getInfo().contains("SOMATIC"));					
				assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_NIOC).equals("0"));
				assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_SSOI).equals("0"));
								
				assertTrue(re.getFilter().contains("COVN8"));						
				assertFalse(re.getFilter().contains("MIN"));	
				assertTrue(re.getFilter().contains("NNS"));						
				assertFalse(re.getFilter().contains("NPART"));
				assertFalse(re.getFilter().contains("NBIAS"));
				assertFalse(re.getFilter().contains("TPART"));
				assertFalse(re.getFilter().contains("TBIAS"));				
			}
		}	
		
		outputVcfFile.delete();
		
		//add three more partial supporting to control
		data.add("ST-E00139:2121:c:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2121:c_1:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2121:c_2:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2212:h:101\t83\tchrY\t2672728\t60\t24S8M1D119M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");
		data.add("ST-E00139:2212:h_1:101\t83\tchrY\t2672728\t60\t24S8M1D119M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");
		data.add("ST-E00139:2212:h_2:101\t83\tchrY\t2672728\t60\t24S8M1D119M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");
		Support.createBam(data, CONTROL_BAM_NAME);
		Support.runQ3IndelNoHom( ini.getAbsolutePath());	
		try (VCFFileReader reader = new VCFFileReader(outputVcfFile)){ 
			for (VcfRecord re : reader) {				
				assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
				assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("0,7,7,0[0,0],1[1],6,0,4"));
				//somatic reads
				assertTrue(re.getInfo().contains("SOMATIC"));				
				assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_NIOC).equals("0.333"));
				assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_SSOI).equals("0.273"));
				
				assertFalse(re.getFilter().contains("COVN8"));	
				assertTrue(re.getFilter().contains("MIN"));	
				assertTrue(re.getFilter().contains("NNS"));						
				assertTrue(re.getFilter().contains("NPART"));				
				assertFalse(re.getFilter().contains("NBIAS"));
				assertFalse(re.getFilter().contains("TPART"));
				assertFalse(re.getFilter().contains("TBIAS"));				
			}
		}	
	}	
}
