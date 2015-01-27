package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

public class AbstractModeTest {
	public static String outputName = "output.vcf";
	public static String inputName = "input.vcf";
	
	@BeforeClass
	public static void createInput() throws IOException{	
		createVcf();
	}
	
	//test data
	@Test
	public void inputRecordCompoundSnp() throws Exception{
		
		//final String[] params =  {"chr1","10180",".","T","C","."," MIN;MIUN","SOMATIC","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};

		final String[] params =  {"chr1","10180",".","TA","CT","."," MIN;MIUN","SOMATIC;END=10181","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};
		final VcfRecord record = new VcfRecord(params);
		
		//System.out.println(record.getChrPosition().toString());
		 
	}
	
	@Test
	public void reHeaderTest() throws Exception{
		
	       try(  BufferedReader br = new BufferedReader(new FileReader(inputName))   ){
	    	   int i = 0;
	    	   while (  br.readLine() != null )  i++;
	    	   assertTrue(i == 2);
	       }
	       
		DbsnpMode db = new DbsnpMode();
		db.inputRecord(new File(inputName));
		db.reheader("testing run",   "inputTest.vcf");  //without .jar file can't pass unit test???????
		db.writeVCF(new File(outputName));
		
        try(VCFFileReader reader = new VCFFileReader(new File(outputName))) {
        	int i = 0;
        	for(VcfHeaderRecord re :  reader.getHeader()){ i ++;System.out.println(re.toString());}
        	assertTrue(i == 9);	
        	
        }		
		
		
	}
	
	@Test
	public void  retriveSampleColumnTest(){
		final String control = "Control";
		final String test = "Test";
		
		VcfHeader header = new VcfHeader();
		 
		header.add(new VcfHeaderRecord("##qControlSample=" + control) );
		header.add( new VcfHeaderRecord("##qTestSample=" + test)  );
		header.add( new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "\t"+ control + "\t" + "test")  );
		
		final ConfidenceMode mode = new ConfidenceMode("");		
		 
		mode.retriveSampleColumn(null,null, header);
		
		System.out.println(mode.control_column );
		 
		assertTrue( mode.control_column == 1);
		assertTrue( mode.test_column == 2);

	}
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");       
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line +"\n");                  
         }  

	}
	
	
}
