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
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

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
	public void doesInputRecordWork() throws IOException {
		AbstractModeImpl impl = new AbstractModeImpl();
		assertEquals(null, impl.inputUuid);
		impl.inputRecord(new File(inputName));
		
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
	       
		DbsnpMode db = new DbsnpMode();
		db.inputRecord(new File(inputName));
		assertEquals("abcd_12345678_xzy_999666333", db.inputUuid);
		
		db.reheader("testing run",   "inputTest.vcf");  //without .jar file can't pass unit test???????
		db.writeVCF(new File(outputName));
		
        try (VCFFileReader reader = new VCFFileReader(new File(outputName))) {
	        	int i = 0;
	        	for (VcfHeader.Record re :  reader.getHeader()) {
	        		if (re.getData().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE)) {
	        			// new UUID should have been inserted by now
	        			assertEquals(false, "abcd_12345678_xzy_999666333".equals(StringUtils.getValueFromKey(re.getData(), VcfHeaderUtils.STANDARD_UUID_LINE)));
	        		}
	        		i ++;
	        	}
	        	assertEquals(9, i);	
        }		
	}
	
	@Test
	public void  retriveSampleColumnTest(){
		final String control = "Control";
		final String test = "Test";
		
		VcfHeader header = new VcfHeader();		 
		header.parseHeaderLine("##qControlSample=" + control);
		header.parseHeaderLine("##qTestSample=" + test);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + control + "\t" + "test");
		
		ConfidenceMode mode = new ConfidenceMode("");			 
		mode.retriveSampleColumn(null,null, header);
				 
		assertTrue( mode.control_column == 1);
		assertTrue( mode.test_column == 2);
		
		//point to sample column 1: "control"	 
		mode.retriveSampleColumn(control,control, header);
		assertTrue( mode.control_column == 1);
		assertTrue( mode.test_column == 1);
		
		
		//point to sample column 1: "test"	 
		mode.retriveSampleColumn(test,test, header);
		assertTrue( mode.control_column == 2);
		assertTrue( mode.test_column == 2);
		
		
		//point to unexsit sample id 
		try{
			mode.retriveSampleColumn(test+control,test, header);
		}catch(Exception e){
			System.out.println(e.getMessage());
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
