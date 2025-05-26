package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.vcf.VcfFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Support;

public class SingleModeTest {
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	File inputIndel;
	File normalBAM;
	File tumourBAM;
	
	@Before
	public void createInput() throws IOException {
		
		inputIndel = testFolder.newFile("indel.vcf");
		normalBAM = testFolder.newFile("ND.bam");
		tumourBAM = testFolder.newFile("TD.bam");
		
		Support.createGatkVcf(inputIndel );
		List<String> data = new ArrayList<>(2);
		data.add("ST" + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^TT123^TT10");
		Support.createBam(data, normalBAM );
		Support.createBam(data, tumourBAM);		
	}
		
	@Test
	public void GATKNormalTest() throws Exception {
		File iniFile = testFolder.newFile();
		File outputVcfFile = new File(testFolder.getRoot() + "/output.vcf"); 
				
		 IniFileTest.createIniFile(iniFile,  null,  normalBAM, null, inputIndel, null, outputVcfFile);		
		 Support.runQ3IndelNoHom(iniFile.getAbsolutePath());
		 
		 try (VcfFileReader reader = new VcfFileReader(outputVcfFile)) {				 
			 for (VcfRecord re : reader) { 											
				if(re.getChromosome().equals("chrY")){
					assertEquals(".", re.getSampleFormatRecord(2).getField("ACINDEL"));
                    assertEquals("1,1,1,1[1,0],1[1],0,0,1", re.getSampleFormatRecord(1).getField("ACINDEL"));
				}
			}
		}	
	 
	}

	@Test
	public void GATKTestTest() throws Exception{
		File iniFile = testFolder.newFile();
		File outputVcfFile = new File(testFolder.getRoot() + "/output.vcf"); 
		
		 IniFileTest.createIniFile(iniFile,  tumourBAM, null, inputIndel,null, null, outputVcfFile);		
		 Support.runQ3IndelNoHom(iniFile.getAbsolutePath());
		 
		 try (VcfFileReader reader = new VcfFileReader(outputVcfFile)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
                    assertEquals(".", re.getSampleFormatRecord(1).getField("ACINDEL"));
                    assertEquals("1,1,1,1[1,0],1[1],0,0,1", re.getSampleFormatRecord(2).getField("ACINDEL"));
				}
		}	
	}
}
