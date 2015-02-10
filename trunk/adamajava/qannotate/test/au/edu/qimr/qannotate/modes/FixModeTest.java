package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;
import org.qcmg.common.util.Constants;

import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class FixModeTest {
 
	 @AfterClass
	 public static void deleteIO(){

		 new File(DbsnpModeTest.inputName).delete();	 }
	 
	 @Test
	 public void FixTest() throws Exception{
		 
		 createVcf();
			final FixMode mode = new FixMode();		
			
			try(VCFFileReader reader = new VCFFileReader(new File( DbsnpModeTest.inputName))){					
				 
		       	for (final VcfRecord vcf : reader) 
	        			 mode.fixVcf(vcf);
			}
				
 
	 }
	 

	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST");
        data.add("chr1\t166002628\t.\tTG\tCA\t.\t5BP1;SAT3\tCONF=ZERO;END=166002629\tACCS\tCA,2,2");    
        data.add("chr2\t166002628\t.\tTG\tCA\t.\t5BP1;SAT3\tCONF=ZERO;END=166002629\tACCS\tCA,2,2\t");
        data.add("chr3\t166002628\t.\tTG\tCA\t.\t5BP1;SAT3\tCONF=ZERO;END=166002629\tACCS\t\tCA,2,2");
        data.add("chr4\t7127\t.\tT\tC\t.\tMR;MIUN\tSOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;CONF=ZERO\tGT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4\t0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5");       
        
        
           try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}
	


}
