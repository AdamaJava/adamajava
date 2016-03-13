package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Q3IndelException;
import au.edu.qimr.indel.Support;

public class singleModeTest {
	@BeforeClass
	public static void createInput() {
		Support.createGatkVcf(IndelPositionTest.inputIndel);
	 List<String> data = new ArrayList<String>();
	 data.add("ST" + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");
	 Support.createBam(data, IndelPositionTest.normalBAM);
	 Support.createBam(data, IndelPositionTest.tumourBAM);		
		
	}
	
	@Test
	public void GATKNormalTest() throws Exception{
		
		 IniFileTest.createIniFile(new File(IndelMTTest.ini_noquery),  null,  new File(IndelPositionTest.normalBAM),
				 null, new File(IndelPositionTest.inputIndel), null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					System.out.println(re.toString()); 
					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals(".") );
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("1,1,1,1[1,0],0,0,1") );
				}
		}	
		new File(IniFileTest.output).delete(); 		 
	}

	@Test
	public void GATKTestTest() throws Exception{
		
		 IniFileTest.createIniFile(new File(IndelMTTest.ini_noquery),  new File(IndelPositionTest.tumourBAM), null,
				  new File(IndelPositionTest.inputIndel),null, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					System.out.println(re.toString()); 
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals(".") );
					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("1,1,1,1[1,0],0,0,1") );
				}
		}	
		new File(IniFileTest.output).delete(); 
		 
	}	
	
	
}
