package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.qcmg.common.util.Constants.EQ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderQPG;
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
		createVcf();
		createDbsnp();   
 	}
	 
	 
	 @AfterClass
	 public static void deleteIO(){

	//	 new File(inputName).delete();
	//	 new File(dbSNPName).delete();
	//	 new File(outputName).delete();
		 
	 }
	 
	
	@Test
	public void CompoundTest() throws IOException, Exception{
		final DbsnpMode mode = new DbsnpMode();		
		mode.inputRecord(new File(inputName));
		mode.addAnnotation(dbSNPName);
		mode.reheader("testing run",   "inputTest.vcf");
		mode.writeVCF(new File(outputName));
		
		int[] count = {0,0,0,0,0,0,0,0,0,0,0}; // 0-8th for non-blank header; 9th for blank header; 10th for exception case  
		
		 try(VCFFileReader reader = new VCFFileReader(outputName)){
			int i = 0; 
			VcfHeader header = reader.getHeader();
		 
			for (final VcfHeaderRecord re : header){
					System.out.println(i + re.toString());
				if( re.getMetaType().equals(MetaType.META)  && re.toString().startsWith(VcfHeaderUtils.CURRENT_FILE_VERSION)) 
					count[0] ++;
				else if( re.getMetaType().equals(MetaType.META) && re.toString().startsWith( VcfHeaderUtils.STANDARD_FILE_DATE))  
					count[1] ++;						
				else if(re.getMetaType().equals(MetaType.META) && re.toString().startsWith( VcfHeaderUtils.STANDARD_UUID_LINE))  
					count[2] ++;
					
				else if(re.getMetaType().equals(MetaType.META) && re.toString().startsWith( VcfHeaderUtils.STANDARD_SOURCE_LINE))  
					count[3] ++;					
				else if(re.getMetaType().equals(MetaType.META) && re.toString().startsWith( VcfHeaderUtils.STANDARD_INPUT_LINE))  
					count[4] ++;	
				else if(re.getMetaType().equals(MetaType.QPG)){
					VcfHeaderQPG re1 = (VcfHeaderQPG) re;
					  assertTrue( re1.getOrder() == 1);
					  assertTrue(re1.getTool().equalsIgnoreCase("qanno"));
					  assertTrue(re1.getISODate()!= null);
					  assertTrue(re1.getCommandLine()!= null);
					count[5] ++;	
				}
				else if(re.getMetaType().equals(MetaType.CHROM) && re.toString().startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE))
					count[6] ++; 
				else if(re.getMetaType().equals(MetaType.INFO) && re.getId().equals(VcfHeaderUtils.INFO_CAF))
					count[7] ++; 
				else if(re.getMetaType().equals(MetaType.INFO) && re.getId().equals(VcfHeaderUtils.INFO_DB))
					count[8] ++; 
				else if(re.getMetaType().equals(MetaType.OTHER) && re.equals(VcfHeaderUtils.BLANK_HEADER_LINE))
					count[9] ++;
				else
					count[10] ++;
			}
			
			for (i = 0; i < 9; i++)
				assertTrue(count[i] == 1);
			assertTrue(count[9] == 2);
			assertTrue(count[10] == 0);
			
			i = 0;
			for (final VcfRecord re : reader) {	
 				i ++;
				if(re.getPosition() == 2675826){
					assertTrue(re.getId().equals("rs71432129"));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_GMAF));
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_CAF).equals("0.39") );	
					
				}
				if(re.getPosition() == 22012840){
					assertTrue(re.getId().equals("rs111477956"));
					assertTrue(re.getInfo().replace(VcfHeaderUtils.INFO_VLD,"").replace(VcfHeaderUtils.INFO_DB, "").equals(Constants.SEMI_COLON_STRING));
				}
			}
			assertTrue(i == 4);
		 }
		  
		
	}
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
//<<<<<<< .mine
       // data.add("chrY\t14923588\t.\tG\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC\tGT:GD:AC\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]"); 
       // data.add("chrY\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;NNS=4;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
       // data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\t.\tGT:GD:AC\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]"); 
 
        data.add("chrY\t14923588\t.\tG\tA\t.\tSBIAS\t;FS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        data.add("chrY\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
        data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\t.\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13"); 
 
        data.add("chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21");
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}
	
	/**
	 * a mini dbSNP vcf file 
	 */
	public static void createDbsnp() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add("##dbSNP_BUILD_ID=135");  
        data.add("##INFO=<ID=CAF,Number=.,Type=String,Description=\"An ordered, comma delimited list of allele frequencies based on 1000Genomes, starting with the reference allele followed by alternate alleles as ordered in the ALT column. "
        		+ "Where a 1000Genomes alternate allele is not in the dbSNPs alternate allele set, the allele is added to the ALT column. "
        		+ " The minor allele is the second largest value in the list, and was previuosly reported in VCF as the GMAF.  This is the GMAF reported on the RefSNP and EntrezSNP pages and VariationReporter\">");  
        data.add("Y\t2675825\trs71432129\tTTG\tTGG,TCA\t.\t.\tRSPOS=2675826;RV;dbSNPBuildID=130;SSR=0;SAO=0;VP=050100000008000100000800;WGT=0;VC=MNV;SLO;CFL;GNO;CAF=0.33,0.23,0.39");
        data.add("Y\t2675829\trs112502114\tA\tC\t.\t.\tRSPOS=2675829;RV;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000008000100000100;WGT=0;VC=SNV;SLO;CFL;GNO");
        data.add("Y\t22012840\trs111477956\tC\tA\t.\t.\tRSPOS=22012840;RV;GMAF=0.113802559414991;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000000000100000100;WGT=0;VC=SNV;SLO;GNO;VLD");  
        data.add("Y\t77242677\trs386662672\tCCA\tCTG\t.\t.\tRS=386662672;RSPOS=77242678;dbSNPBuildID=138;SSR=0;SAO=0;VP=0x050000080005000002000800;WGT=1;VC=MNV;INT;ASP;OTHERKG");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbSNPName));) {          
           for (final String line : data)  out.write(line + "\n");
        }  
          
	}

}
