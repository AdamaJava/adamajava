package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

public class DbsnpModeTest {
	final static String inputName = "input.vcf";
	final static String dbSNPName = "dbSNP.vcf";
	final static String outputName = "output.vcf";
	
	 @BeforeClass
	public static void createInput() throws IOException{
		
        final List<String> data = new ArrayList<String>();
        
        //dbSNP
        data.add("##fileformat=VCFv4.0");
        data.add("##dbSNP_BUILD_ID=135");  
        data.add("##INFO=<ID=GMAF,Number=1,Type=Float,Description=\"Global Minor Allele Frequency [0, 0.5]; global population is 1000GenomesProject phase 1 genotype data from 629 individuals, released in the 08-04-2010 dataset\">"); 
        data.add("Y\t2675825\trs71432129\tTTG\tTCA\t.\t.\tRSPOS=2675826;RV;dbSNPBuildID=130;SSR=0;SAO=0;VP=050100000008000100000800;WGT=0;VC=MNV;SLO;CFL;GNO");
        data.add("Y\t2675829\trs112502114\tA\tC\t.\t.\tRSPOS=2675829;RV;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000008000100000100;WGT=0;VC=SNV;SLO;CFL;GNO");
        data.add("Y\t22012840\trs111477956\tC\tA\t.\t.\tRSPOS=22012840;RV;GMAF=0.113802559414991;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000000000100000100;WGT=0;VC=SNV;SLO;GNO");
       
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbSNPName));) {          
           for (final String line : data)  out.write(line + "\n");
        }  
        
        //input
        data.clear();
        data.add("##fileformat=VCFv4.0");
        data.add("chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1");
        data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC\tGT:GD:AC\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]"); 
		
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  	
 	}
	 
	 
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 new File(dbSNPName).delete();
		 new File(outputName).delete();
		 
	 }
	 
	
	@Test
	public void CompoundTest() throws IOException, Exception{

		final DbsnpMode mode = new DbsnpMode();		
		mode.inputRecord(new File(inputName));
		mode.addAnnotation(dbSNPName);
		mode.writeVCF(new File(outputName));
		
		 try(VCFFileReader reader = new VCFFileReader(outputName)){ 
			int i = 0; 
			for (final VcfHeaderRecord re : reader.getHeader()){
				if(i == 0)  
					assertTrue(re.getMetaType().equals(MetaType.META)  && re.toString().equals( "##fileformat=VCFv4.0"));
				else if(i == 1)  
					assertTrue(re.getMetaType().equals(MetaType.META) && re.toString().equals( "##dbSNP_BUILD_ID=135"));
				else if(i == 2)  
					assertTrue(re.getMetaType().equals(MetaType.INFO) && re.toString().equals("##INFO=<ID=GMAF,Number=1,Type=Float,Description=\"Global Minor Allele Frequency [0, 0.5]; global population is 1000GenomesProject phase 1 genotype data from 629 individuals, released in the 08-04-2010 dataset\">"));
				else
					assertFalse(true);
				
				i ++;
			} 
			 
			for (final VCFRecord re : reader) {					
				if(re.getPosition() == 2675825){
					assertTrue(re.getId().equals("rs71432129"));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_GMAF));
				}
				if(re.getPosition() == 22012840){
					assertTrue(re.getId().equals("rs111477956"));
					assertTrue(re.getInfo().contains(VcfHeaderUtils.INFO_GMAF));
				}
			}
		 }
		  
		
	}

}
