package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

public class DbsnpModeTest {
	public final static String inputName = "./input.vcf";
	final static String dbSNPName = "./dbSNP.vcf";
	final static String outputName = "./output.vcf";
	
	 @BeforeClass
	public static void createInput() throws IOException{
		createVcf();
 	}
	 
	 @AfterClass
	 public static void deleteIO(){
		 new File(inputName).delete();
		 new File(dbSNPName).delete();
		 new File(outputName).delete();
	 }
	 
	 @Test
	 public void indelTest(){
		 				 
		 VcfRecord dbSNPVcf = new VcfRecord( new String[] {"chrY","14923588","rs100","GT","GA,GATT,G",".","SBIA","RSPOS=14923589;VLD;dbSNPBuildID=129;VC=DIV;CAF=[0.4558,0.4,0.1442,.]"});
		
		 //seek RSPOS=14923589
		 VcfRecord inputVcf = new VcfRecord( new String[] {"chrY","14923589",".","T","A",".","SBIA","FS=GTGATATTCCC"});
		 DbsnpMode.annotateDBsnp(inputVcf, dbSNPVcf);		 		 
		 assertEquals("0.4", inputVcf.getInfoRecord().getField("VAF"));
		 
		 //seek 14923588
		 inputVcf = new VcfRecord( new String[] {"chrY","14923588",".","GT","G",".","SBIA","FS=GTGATATTCCC"});
		 DbsnpMode.annotateDBsnp(inputVcf, dbSNPVcf);
		 assertTrue(inputVcf.getInfoRecord().getField("VAF").equals( "."));		
		 
		 //seek 14923589, at moment don't support it 
		 inputVcf = new VcfRecord( new String[] {"chrY","14923589",".","T","-",".","SBIA","FS=GTGATATTCCC"});
		 DbsnpMode.annotateDBsnp(inputVcf, dbSNPVcf);
		 assertFalse(inputVcf.getId().equals( "rs100" ));	
	 }	 
	 @Test
	 public void multiAllelesTest(){
		 				 
		 VcfRecord inputVcf = new VcfRecord( new String[] {"chrY","14923588",".","G","A,T",".","SBIA","FS=GTGATATTCCC"});
		 VcfRecord dbSNPVcf = new VcfRecord( new String[] {"chrY","14923588","rs100","G","A,ATT",".","SBIA","VLD;dbSNPBuildID=129;CAF=[0.4558,0.4,0.1442]"});
		
		 DbsnpMode.annotateDBsnp(inputVcf, dbSNPVcf);
		 		 
		 assertTrue(inputVcf.getInfoRecord().getField("VAF").equals( "[0.4,.]"));
		 assertTrue(inputVcf.getInfoRecord().getField("DB").equals(Constants.EMPTY_STRING) );
		 assertTrue(inputVcf.getInfoRecord().getField("VLD").equals(Constants.EMPTY_STRING) );
		 assertTrue(inputVcf.getId().equals( "rs100" ));
	 }
	 
	 @Test
	 public void emptyVAFField() {
		 VcfRecord vcf = new VcfRecord(new String[] {"chr1","240119182","rs150561860","A","G",".","PASS_1;PASS_2","FLANK=ATATAGACATG;AC=1;AF=0.500;AN=2;BaseQRankSum=0.035;ClippingRankSum=-0.354;DB;DP=34;FS=1.377;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.425;QD=12.14;ReadPosRankSum=-0.921;SOR=1.061;IN=1,2","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&0/1:A/G&A/G:A9[42],7[42],G3[42],5[33.8]&A10[42],11[41.55],G5[42],9[37.44]:8&14:8&14:21,13:34:99:441,0,840","0/1&0/1:A/G&A/G:A19[40.16],22[40.86],C0[0],1[11],G10[41.5],6[33.67]&A26[39.88],31[39.06],C0[0],1[11],G18[40.33],19[36.42]:16&37:16&36:56,35:91:99:1184,0,2031"});
		 VcfRecord dbSNPVcf = new VcfRecord( new String[] {"1","240119182","rs150561860","A","G,C",".",".","RS=150561860;RSPOS=240119182;dbSNPBuildID=134;SSR=0;SAO=0;VP=0x050000000005000014000100;WGT=1;VC=SNV;ASP;KGPhase1;KGPROD;CAF=[0.9945,.,0.00551];COMMON=1"});
		 
		 DbsnpMode.annotateDBsnp(vcf, dbSNPVcf);
		 assertTrue(vcf.getInfoRecord().getField("VAF").equals("."));
		 assertTrue(vcf.getInfoRecord().getField("DB").equals(Constants.EMPTY_STRING) );
	 }
	      
	 @Test
	public void annotationTest() throws IOException, Exception{
		createDbsnp(); 
		
		final DbsnpMode mode = new DbsnpMode(false);	
		mode.loadVcfRecordsFromFile(new File(inputName),false);
		mode.addAnnotation(dbSNPName);
		mode.reheader("testing run",   inputName);
		mode.writeVCF( new File(outputName));
		
		 try(VCFFileReader reader = new VCFFileReader(outputName)){
			VcfHeader header = reader.getHeader();
			
			assertTrue (null != header.getFileFormat()) ;  
			assertTrue (null != header.getFileDate()) ; 	 
			assertTrue (null != header.getUUID()) ;   
			assertTrue (null != header.getSource()) ;  
			
			assertTrue(header.getRecords(VcfHeaderUtils.STANDARD_FILE_FORMAT).size() == 1);
			assertTrue(header.getRecords(VcfHeaderUtils.STANDARD_FILE_DATE ).size() == 1);
			assertTrue(header.getRecords(VcfHeaderUtils.STANDARD_UUID_LINE ).size() == 1);
			assertTrue(header.getRecords(VcfHeaderUtils.STANDARD_SOURCE_LINE ).size() == 1);
			assertTrue(header.getRecords(VcfHeaderUtils.STANDARD_INPUT_LINE ).size() == 1);
						
			int ii = 0;
			for(VcfHeaderRecord re : VcfHeaderUtils.getqPGRecords(header)){
				assertEquals( VcfHeaderUtils.getQPGTool(re)  , Constants.NULL_STRING_UPPER_CASE);
				assertNotNull(VcfHeaderUtils.getQPGDate(re) );
				assertNotNull( VcfHeaderUtils.getQPGCommandLine(re) );
				assertEquals(1, VcfHeaderUtils.getQPGOrder(re) );
				ii ++;	
			}
			assertTrue(ii == 1);
						
			VcfHeaderRecord chrom = header.getChrom();
			assertTrue(chrom.toString().startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE)) ;			
			assertTrue(header.getInfoRecord(VcfHeaderUtils.INFO_VAF) != null);  
			assertTrue(header.getInfoRecord(VcfHeaderUtils.INFO_DB) != null) ;
			  						
			int i = 0;
			for (final VcfRecord re : reader) {	
 				i ++;
				if(re.getPosition() == 2675826){
					assertTrue(re.getId().equals("rs71432129"));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_GMAF));
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_VAF).equals("0.39") );	
				}else if(re.getPosition() == 2675825){
					assertTrue(re.getId().equals("rs71432129"));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_GMAF));
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_VAF).equals("0.23") );	
				}else if(re.getPosition() == 22012840){
					assertTrue(re.getId().equals("rs111477956"));
					assertTrue(re.getInfo().replace(VcfHeaderUtils.INFO_VLD,"").replace(VcfHeaderUtils.INFO_DB, "").replace(VcfHeaderUtils.INFO_SOMATIC,"").equals(Constants.SEMI_COLON_STRING + Constants.SEMI_COLON_STRING));
				}else if(re.getPosition() == 77242678){
					assertTrue(re.getId().equals("rs386662672"));
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_VAF) == null );	
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) == null );	
					assertTrue(re.getInfo().contains(VcfHeaderUtils.INFO_DB));

				}else{
					assertTrue(re.getId().equals("."));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_VLD));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_VAF));
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_DB));
				}				
			}
			assertTrue(i == 5);
		 }
	}
	
	/**
	 * The VLD info should add to output vcf if it appear on the dbSNP header
	 * @throws IOException
	 * @throws Exception
	 */
	
	@Test
	public void vLDandStrictTest() throws IOException, Exception{
		createDbsnpHeader();
		
		final DbsnpMode mode = new DbsnpMode(true);		
		mode.loadVcfRecordsFromFile(new File(inputName), true);
		mode.addAnnotation(dbSNPName);		
		mode.reheader("testing run",   inputName);
		mode.writeVCF(new File(outputName));

		try (VCFFileReader reader = new VCFFileReader(outputName)) {
			//won't affect header
			VcfHeader header = reader.getHeader();	
			assertEquals( header.getInfoRecord(VcfHeaderUtils.INFO_VLD)!= null, true);
			
			
			int i = 0;
			for (final VcfRecord re : reader) {	 				
 				assertTrue(re.getId().equals(Constants.MISSING_DATA_STRING));
 				i ++;
			}
			assertTrue(i == 5);
		}		 
	}		
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1\ts2");
 
        data.add("chrY\t14923588\t.\tG\tA\t.\tSBIAS\t;FS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        data.add("chrY\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
        data.add("chrY\t2675825\t.\tTTG\tTGG\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");

        data.add("chrMT\t22012840\t.\tC\tA\t.\tMIUN\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        data.add("chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21");
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}
	
	/**
	 * a mini dbSNP vcf file 
	 */
	public static void createDbsnp() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add("##dbSNP_BUILD_ID=135");  
        data.add("##INFO=<ID=CAF,Number=.,Type=String,Description=\"An ordered, comma delimited list of allele frequencies based on 1000Genomes, starting with the reference allele followed by alternate alleles as ordered in the ALT column. "
        		+ "Where a 1000Genomes alternate allele is not in the dbSNPs alternate allele set, the allele is added to the ALT column. "
        		+ " The minor allele is the second largest value in the list, and was previuosly reported in VCF as the GMAF.  This is the GMAF reported on the RefSNP and EntrezSNP pages and VariationReporter\">");  
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
        data.add("Y\t2675825\trs71432129\tTTG\tTGG,TCA\t.\t.\tRSPOS=2675826;RV;dbSNPBuildID=130;SSR=0;SAO=0;VP=050100000008000100000800;WGT=0;VC=MNV;SLO;CFL;GNO;CAF=[0.33,0.23,0.39]");
        data.add("Y\t2675829\trs112502114\tA\tC\t.\t.\tRSPOS=2675829;RV;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000008000100000100;WGT=0;VC=SNV;SLO;CFL;GNO");
        data.add("M\t22012840\trs111477956\tC\tA\t.\t.\tRSPOS=22012840;RV;GMAF=0.113802559414991;dbSNPBuildID=132;SSR=0;SAO=0;VP=050100000000000100000100;WGT=0;VC=SNV;SLO;GNO;VLD");  
        data.add("Y\t77242677\trs386662672\tCCA\tCTG\t.\t.\tRS=386662672;RSPOS=77242678;dbSNPBuildID=138;SSR=0;SAO=0;VP=0x050000080005000002000800;WGT=1;VC=MNV;INT;ASP;OTHERKG");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbSNPName));) {          
           for (final String line : data)  out.write(line + "\n");
        }  
	}
	
	public static void createDbsnpHeader() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add("##dbSNP_BUILD_ID=135");  
        data.add("##INFO=<ID=CAF,Number=.,Type=String,Description=\"An ordered, comma delimited list of allele frequencies based on 1000Genomes, starting with the reference allele followed by alternate alleles as ordered in the ALT column. "
        		+ "Where a 1000Genomes alternate allele is not in the dbSNPs alternate allele set, the allele is added to the ALT column. "
        		+ " The minor allele is the second largest value in the list, and was previuosly reported in VCF as the GMAF.  This is the GMAF reported on the RefSNP and EntrezSNP pages and VariationReporter\">");  
      
        data.add("##INFO=<ID=VLD,Number=0,Type=Flag,Description=\"Is Validated. This bit is set if the variant has 2+ minor allele count based on frequency or genotype data.\">");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbSNPName));) {          
           for (final String line : data)  out.write(line + "\n");
        }  
	}	
}
