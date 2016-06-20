package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.modes.DbsnpModeTest;
import au.edu.qimr.qannotate.modes.Vcf2maf;
import au.edu.qimr.qannotate.modes.Vcf2mafTest;

public class SampleColumnTest {
	public static String input = DbsnpModeTest.inputName;
	private String[] Sheader = {
		    		VcfHeaderUtils.STANDARD_FILE_VERSION + "=VCFv4.0",			
					VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
					VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_bam",
					VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_bam",
					VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",
					VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",
					VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample"			
		    };
	
	@Test
	public void fileNameTest() throws IOException {
        	Vcf2mafTest.createVcf(Sheader);                
        	try(VCFFileReader reader = new VCFFileReader(input); ){
    			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
    			assertTrue(column.getControlSample().equals("CONTROL_bam"));
    			assertTrue(column.getTestSample().equals("TEST_bam"));
    			
    			column = SampleColumn.getSampleColumn("alignment#test", "grfli:control" , reader.getHeader());
       			assertTrue(column.getControlSample().equals("control"));
       			assertTrue(column.getTestSample().equals("test")); 		
       			
       			column = SampleColumn.getSampleColumn("alignment,test", "grfli:align#control" , reader.getHeader());
       			assertTrue(column.getControlSample().equals("control"));
      			assertTrue(column.getTestSample().equals("alignment,test")); 	

	        }catch(Exception e){
	        	fail(e.getMessage()); 
	        }
	        
	        new File(input).delete();		
	}
	
	
}
