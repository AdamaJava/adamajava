package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Support;

public class SingleModeTest {
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@BeforeClass
	public static void createInput() {
		Support.createGatkVcf(IndelPositionTest.inputIndel);
		List<String> data = new ArrayList<>(2);
		data.add("ST" + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^TT123^TT10");
		Support.createBam(data, IndelPositionTest.normalBAM);
		Support.createBam(data, IndelPositionTest.tumourBAM);		
	}
	@AfterClass
	public static void tidy() {
		new File(IndelPositionTest.inputIndel).delete();
	}
	
	@Test
	public void GATKNormalTest() throws Exception {
		File iniFile = testFolder.newFile();
		File outputVcfFile = new File(testFolder.getRoot() + "/output.vcf"); 
				
		 IniFileTest.createIniFile(iniFile,  null,  new File(IndelPositionTest.normalBAM),
				 null, new File(IndelPositionTest.inputIndel), null, outputVcfFile);		
		 Support.runQ3IndelNoHom(iniFile.getAbsolutePath());
		 
		 try (VCFFileReader reader = new VCFFileReader(outputVcfFile)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					assertEquals(".", re.getSampleFormatRecord(2).getField("ACINDEL"));
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("1,1,1,1[1,0],1[1],0,0,1") );
				}
		}	
		new File(IniFileTest.output).delete(); 		 
	}

	@Test
	public void GATKTestTest() throws Exception{
		File iniFile = testFolder.newFile();
		File outputVcfFile = new File(testFolder.getRoot() + "/output.vcf"); 
		 IniFileTest.createIniFile(iniFile,  new File(IndelPositionTest.tumourBAM), null,
				  new File(IndelPositionTest.inputIndel),null, null, outputVcfFile);		
		 Support.runQ3IndelNoHom(iniFile.getAbsolutePath());
		 
		 try (VCFFileReader reader = new VCFFileReader(outputVcfFile)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals(".") );
					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("1,1,1,1[1,0],1[1],0,0,1") );
				}
		}	
		new File(IniFileTest.output).delete(); 
	}	
}
