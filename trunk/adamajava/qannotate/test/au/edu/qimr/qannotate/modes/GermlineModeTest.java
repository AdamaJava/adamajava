package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

public class GermlineModeTest {
	
	final static String GermlineFileName = "germline.vcf";
	final static String inputName = "input.vcf"; 
	final static String outputName = "output.vcf";
  	
	@BeforeClass
	public static void createInput() throws IOException{	
	
		createGermlineFile();
	}
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 new File(GermlineFileName).delete();
		 new File(outputName).delete();
	 }	
	 
		@Test
		public void germlineModeTest() throws Exception {
			createVcf();
			final GermlineMode mode = new GermlineMode();		
			mode.inputRecord(new File(inputName));
			mode.addAnnotation(GermlineFileName);						
			mode.reheader("testing run",   inputName);
			mode.writeVCF( new File(outputName));
			
			try(VCFFileReader reader = new VCFFileReader(outputName)){
				 
				 //check header
				VcfHeader header = reader.getHeader();					
				assertEquals(false, header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE) != null);
				assertEquals(true, header.getInfoRecord(VcfHeaderUtils.INFO_GERMLINE) != null);

				//check records
 				int inputs = 0;
 				int germNo = 0;
 				
				for (final VcfRecord re : reader) {	
	 				inputs ++;
	 				if(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) != null)
	 					germNo ++;
	 				
					if(re.getPosition() == 2675826){						
						assertTrue(re.getId().equals(Constants.MISSING_DATA_STRING));
						assertTrue(re.getFilter().equals("COVN12;MIUN"));							
						assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) == null );	
					}else if(re.getPosition() == 14923588)
						//assertTrue(re.getFilter().equals(VcfHeaderUtils.FILTER_GERMLINE));	
						assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE).equals("6,185"));	
					else if(re.getPosition() == 22012840 || re.getPosition() == 22012841 ||re.getPosition() == 22012842 ){
						assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE).equals("86,185"));	
					}else if(re.getPosition() == 22012843){
						assertTrue(re.getFilter().equals(Constants.MISSING_DATA_STRING));	
						assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) ==null );	
					}
				}
				assertTrue(inputs == 6);
				assertTrue(germNo == 4);
			 }
		}	 
	 
		@Test
		public void checkNonSomatic()throws Exception{
		    final List<String> data = new ArrayList<String>();
	        data.add("##fileformat=VCFv4.0");
	       
 	        //germ position but not somatic
	        data.add("chrY\t14923588\t.\tG\tA\t.\t.\tFS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
	        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
	            for (final String line : data)   out.write(line + "\n");                  
	         }
	        
			final GermlineMode mode = new GermlineMode();		
			mode.inputRecord(new File(inputName));
			mode.addAnnotation(GermlineFileName);
			mode.writeVCF( new File(outputName));
	        
			 try(VCFFileReader reader = new VCFFileReader(outputName)){				 
				//check records
 				int i = 0;
				for (final VcfRecord re : reader) {	
	 				i ++;
	 				if (re.getPosition() == 14923588) {
						assertTrue(re.getFilter().equals(Constants.MISSING_DATA_STRING));
						//assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE).equals("86,185"));
						assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) == null);
	 				}
				}
				assertTrue( i == 1 );
			 }	        
		}
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        //not germ: ref diferent
        data.add("chrY\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
        //. = > GERM
        data.add("chrY\t14923588\t.\tG\tA\t.\t.\tSOMATIC;FS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        //MIUN => MIUN;GERM
        data.add("chrY\t22012840\t.\tC\tA\t.\tMIUN\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        //MIUN;PASS => MIUN;GERM
        data.add("chrY\t22012841\t.\tC\tA\t.\tMIUN;PASS\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        //PASS;MIUN => MIUN;GERM
        data.add("chrY\t22012842\t.\tC\tA\t.\tPASS;MIUN\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        //not germ: position different
        data.add("chrY\t22012843\t.\tC\tA\t.\t.\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}	
	
	/**
	 * a mini dbSNP vcf file 
	 */
	public static void createGermlineFile() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add("##search_string=GermlineSNV.dcc1");  
        data.add("##dornorNumber=185");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        data.add("Y\t2675826\t.\tC\tA\t.\t.\t86");
        data.add("Y\t14923588\t.\tG\tA,T\t.\t.\t6");
        data.add("Y\t22012840\t.\tC\tA\t.\t.\t86");
        data.add("Y\t22012841\t.\tC\tA\t.\t.\t86");
        data.add("Y\t22012842\t.\tC\tA\t.\t.\t86");

        try(BufferedWriter out = new BufferedWriter(new FileWriter(GermlineFileName));) {          
           for (final String line : data)  out.write(line + "\n");
        }  
	}	
}
