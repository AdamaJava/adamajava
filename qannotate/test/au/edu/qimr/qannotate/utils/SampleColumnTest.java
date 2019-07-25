package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import au.edu.qimr.qannotate.modes.Vcf2mafTest;

public class SampleColumnTest {
	
	private final String[] header0 = {
    		VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
			VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
			VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
			VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample" ,
			VcfHeaderUtils.STANDARD_CONTROL_BAM + "=CONTROL_bam",
			VcfHeaderUtils.STANDARD_TEST_BAM + "=TEST_bam",
			VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",
			VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",
			null
			//VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample"			
    };	
	
	
	@Test
	public void sampleNameTest1() throws IOException {
		String[] sHeader = header0.clone(); 
		sHeader[sHeader.length-1] = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample";	
		
    	File input = new Vcf2mafTest().createVcf(sHeader);                
    	try(VCFFileReader reader = new VCFFileReader(input); ){
			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
			assertTrue(column.getControlSample().equals("CONTROL_sample"));
			assertTrue(column.getTestSample().equals("TEST_sample"));
			
			column = SampleColumn.getSampleColumn("alignment#test", "grfli:control" , reader.getHeader());
   			assertTrue(column.getControlSample().equals("control"));
   			assertTrue(column.getTestSample().equals("test")); 		
   			
   			column = SampleColumn.getSampleColumn("alignment,test", "grfli:align#control" , reader.getHeader());
   			assertTrue(column.getControlSample().equals("control"));
  			assertTrue(column.getTestSample().equals("alignment,test")); 	
        }catch(Exception e){
        	fail(e.getMessage()); 
        }
	         
	}
	
	@Test
	public void sampleNameTest2() throws IOException {
		String[] sHeader = header0.clone(); 
		sHeader[sHeader.length-1] = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_bamID\tTEST_bamID";
		 			
		File input = new Vcf2mafTest().createVcf(  sHeader );                
    	try(VCFFileReader reader = new VCFFileReader(input); ){
			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
			assertTrue(column.getControlSample().equals("CONTROL_sample"));
			assertTrue(column.getTestSample().equals("TEST_sample"));
			assertTrue(column.getControlBamId() .equals("CONTROL_bamID"));
			assertTrue(column.getTestBamId().equals("TEST_bamID"));
			assertTrue(column.getTestSampleColumn() == 2);
			assertTrue(column.getControlSampleColumn() == 1);
        }catch(Exception e){
        	fail(e.getMessage()); 
        }
	}
	
	@Test
	/**
	 * first sample column (Sample:CONTROL_sample) matches header qControlSample; return substring after ':' as controlsampleuuid
	 * second sample column (TEST_bamID) matches header qTestBamUUID;  
	 * @throws IOException
	 */
	public void sampleNameTest3() throws IOException {
		String[] Sheader = header0.clone(); 		
		Sheader[2] = VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=Sample:CONTROL_sample";
		Sheader[Sheader.length-1] = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tSample:CONTROL_sample\tTEST_bamID";
		 			
		File input = new Vcf2mafTest().createVcf(  Sheader );                
    	try(VCFFileReader reader = new VCFFileReader(input); ){
			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
			assertTrue(column.getControlSample().equals("CONTROL_sample"));
			assertTrue(column.getTestSample().equals("TEST_sample"));
			assertTrue(column.getControlBamId() .equals("CONTROL_bamID"));
			assertTrue(column.getTestBamId().equals("TEST_bamID"));
			assertTrue(column.getTestSampleColumn() == 2);
			assertTrue(column.getControlSampleColumn() == 1);
        }catch(Exception e){
        	fail(e.getMessage()); 
        }
	}	
	
	@Test
	public void sampleNameTest4() throws IOException {
		String[] sHeader = header0.clone(); 
		sHeader[6] = VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=null";
		sHeader[7] = VcfHeaderUtils.STANDARD_TEST_BAMID + "=";
		sHeader[sHeader.length-1] = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_bam\tTEST_bam";
		 			
		File input = new Vcf2mafTest().createVcf(  sHeader );                
    	try(VCFFileReader reader = new VCFFileReader(input); ){
			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
			assertTrue(column.getControlSample().equals("CONTROL_sample"));
			assertTrue(column.getTestSample().equals("TEST_sample"));
			assertTrue(column.getControlBamId() .equals("CONTROL_bam"));
			assertTrue(column.getTestBamId().equals("TEST_bam"));
			assertTrue(column.getTestSampleColumn() == 2);
			assertTrue(column.getControlSampleColumn() == 1);
        }catch(Exception e){ fail(e.getMessage());   }
	}	
	
	@Test
	public void sampleNameTest5() throws IOException {
		String[] sHeader = header0.clone(); 
		sHeader[6] = VcfHeaderUtils.STANDARD_CONTROL_BAMID_1 + "=bamid_1";
		sHeader[7] = VcfHeaderUtils.STANDARD_TEST_BAMID + "=";
		sHeader[sHeader.length-1] = VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tbamid_1\tTEST_bam";
		 			
		File input = new Vcf2mafTest().createVcf(  sHeader );                
    	try(VCFFileReader reader = new VCFFileReader(input); ){
			SampleColumn column = SampleColumn.getSampleColumn(null, null , reader.getHeader());
			assertTrue(column.getControlSample().equals("CONTROL_sample"));
			assertTrue(column.getTestSample().equals("TEST_sample"));
			assertTrue(column.getControlBamId() .equals("bamid_1"));
			assertTrue(column.getTestBamId().equals("TEST_bam"));
			assertTrue(column.getTestSampleColumn() == 2);
			assertTrue(column.getControlSampleColumn() == 1);
        }catch(Exception e){ fail(e.getMessage());   }
	}		
	
}
