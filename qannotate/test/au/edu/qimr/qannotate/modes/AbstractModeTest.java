package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SampleColumn;


public class AbstractModeTest {
	public static String outputName = "output.vcf";
	public static String inputName = "input.vcf";
	
	@BeforeClass
	public static void createInput() throws IOException{	
		createVcf();
	}
	
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 new File(outputName).delete();
		 
	 }	
	
	//test data
	@Test
	public void inputRecordCompoundSnp() throws Exception{
		
		final String[] params =  {"chr1","10180",".","TA","CT","."," MIN;MIUN","SOMATIC;END=10181","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};
		final VcfRecord record = new VcfRecord(params);
		assertEquals(10180, record.getPosition());
		assertEquals(10181, record.getChrPosition().getEndPosition());
		 
	}
	

	
	@Test
	public void reHeaderTest() throws Exception{
		
	   try (BufferedReader br = new BufferedReader(new FileReader(inputName))){
	    	   int i = 0;
	    	   while (  br.readLine() != null ) {
	    		   i++;
	    	   }
	    	   assertTrue(i == 3);
	   }
	       
		DbsnpMode db = new DbsnpMode(true);
		db.loadVcfRecordsFromFile(new File(inputName),false);		
		db.reheader("testing run",   inputName);
		db.writeVCF(new File(outputName));
		
		
        try (VCFFileReader reader = new VCFFileReader(new File(outputName))) {
	        	int i = 0;
	        	for (VcfHeaderRecord re :  reader.getHeader()) {
	        		if (re.toString().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE)) {
	        			// new UUID should have been inserted by now
	        			assertEquals(false, "abcd_12345678_xzy_999666333".equals(StringUtils.getValueFromKey(re.getMetaValue(), VcfHeaderUtils.STANDARD_UUID_LINE)));
	        		}
	        		i ++;
	        	}
	        	assertEquals(7, i);	// removed blank lines
        }		
	}
	
	 @Test
	 public void sampleColumnTest()throws Exception{
			VcfHeader header = new VcfHeader();		 
			header.addOrReplace("##qControlSample=control");
			header.addOrReplace("##qTestSample=test");
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "qControlSample" + "\tqTestSample");
 
			SampleColumn column = SampleColumn.getSampleColumn(null,null, header);
			assertTrue( column.getControlSampleColumn() == 1);
			assertTrue( column.getTestSampleColumn() == 2);		
			assertEquals( column.getControlSample() , "control");
			assertEquals( column.getTestSample() , "test");	
			
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "qControlSample" + "\ttest");
			column = SampleColumn.getSampleColumn(null,null, header);
			assertTrue( column.getControlSampleColumn() == 1);
			assertTrue( column.getTestSampleColumn() == 2);		
	 }
	 
	
	@Test
	public void  retriveSampleColumnTest(){
		final String control = "Control";
		final String test = "Test";
		
		VcfHeader header = new VcfHeader();		 
		header.addOrReplace("##qControlSample=" + control);
		header.addOrReplace("##qTestSample=" + test);
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + control + "\t" + "test");
		
		SampleColumn column = SampleColumn.getSampleColumn(null,null, header);
		assertTrue( column.getControlSampleColumn() == 1);
		assertTrue( column.getTestSampleColumn() == 2);		
		assertEquals( column.getControlSample() , control);
		assertEquals( column.getTestSample() , test);		
		
		//point to sample column 1: "control"	
		column = SampleColumn.getSampleColumn(control,control, header);
		assertTrue( column.getControlSampleColumn() == 1);
		assertTrue( column.getTestSampleColumn() == 1);		
		assertEquals( column.getControlSample() , control);
		assertEquals( column.getTestSample() , control);		
		
		//point to sample column 1: "test"	 
		column = SampleColumn.getSampleColumn(test,test, header);
		assertTrue( column.getControlSampleColumn() == 2);
		assertTrue( column.getTestSampleColumn() == 2);
		assertEquals( column.getControlSample() , test);
		assertEquals( column.getTestSample() , test);		
				
		//point to unexsit sample id 
		try{
			column = SampleColumn.getSampleColumn(test+control,test, header);
			/*
			 * add in assertion that columns are the same - single sample mode...
			 */
//			fail( "My method didn't throw when I expected it to" );
		}catch(Exception e){
		}


	}
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line +"\n");                  
         }  

	}
	
	
}
