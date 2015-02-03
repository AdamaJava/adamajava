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
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2mafTest {
 
	 @AfterClass
	 public static void deleteIO(){

		 new File(DbsnpModeTest.inputName).delete();	 }
	 
	 @Test
	 public void compoundSNPTest() throws Exception{
		 //create vcf with compoundSNP variant and header
	        final List<String> data = new ArrayList<String>();
	        data.add("##fileformat=VCFv4.0");
	        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST");
	        data.add("chrY\t2675826\trs75454623\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826;CONF=ZERO;EFF="
	        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr248Pro/c.742A>C|540|SAMD11|protein_coding|CODING|ENST00000455979|5|1|WARNING_TRANSCRIPT_NO_START_CODON),"		
	        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr329Pro/c.985A>C|588|SAMD11|protein_coding|CODING|ENST00000341065|9|1|WARNING_TRANSCRIPT_NO_START_CODON)," 
	        		+ "downstream_gene_variant(MODIFIER||1446||749|NOC2L|protein_coding|CODING|ENST00000327044||1)"
	        		+ "\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1");
	          try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
	              for (final String line : data)   out.write(line + "\n");                  
	           }  	    
	 
	          
				final Vcf2maf mode = new Vcf2maf(2, 1);		
				SnpEffMafRecord maf = null;
				
				try(VCFFileReader reader = new VCFFileReader(new File( DbsnpModeTest.inputName))){					
					//get control and test sample column
					mode.retriveSampleColumn("TEST", "CONTROL", reader.getHeader());					
					// SnpEffMafRecord.getSnpEffMafHeaderline();
			       	for (final VcfRecord vcf : reader) 
		        			maf = mode.converter(vcf);
				}
				
				assertFalse(maf == null);
				
				assertTrue(maf.getColumnValue(14).equals("rs75454623"));
					 
		 		assertTrue(maf.getColumnValue(36).equals("TG,5,37,CA,0,2" ));			//ND
		 		assertTrue(maf.getColumnValue(37).equals("AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1" ));	//TD
		 		assertTrue(maf.getColumnValue(12).equals("TA"));   //TD allel1
		 		assertTrue(maf.getColumnValue(13).equals("CA"));   //TD allel2		 		
		 		assertTrue(maf.getColumnValue(18).equals("TG"));   //ND allel1
		 		assertTrue(maf.getColumnValue(19).equals("CA"));   //ND allel2
		 		
		 		
		 		//we get consequnce with high rank, then long length, should be second annotation
		 		maf.getColumnValue(51).equals("p.Thr329Pro");
		 		maf.getColumnValue(50).equals("ENST00000341065");
		 		maf.getColumnValue(52).equals("c.985A>C");
		 		
	 }
	 
	 @Test 
	 public void converterTest() throws Exception{
		 
		 	final SnpEffMafRecord Dmaf = new SnpEffMafRecord();
			Dmaf.setDefaultValue();
			
			final Vcf2maf v2m = new Vcf2maf(2,1);	//test column2; normal column 1
			final String[] parms = {"chrY","22012840",".","C","A",".","SBIAS","VLD;FS=GTGATATTCCC;VAF=0.11;"
					+ "EFF=sequence_feature[compositionally_biased_region:Glu/Lys-rich](LOW|||c.1252G>C|591|CCDC148|protein_coding|CODING|ENST00000283233|10|1),"
					+ "splice_acceptor_variant(HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1)",
					"GT:GD:AC","0/0:T/C:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]","0/0:A/C:C8[7.62],2[2],A2[8],28[31.18]"};

			
	 		final VcfRecord vcf = new VcfRecord(parms);
	 		final SnpEffMafRecord maf = v2m.converter(vcf);
	
	 		
//	 		final String eff = new VcfInfoFieldRecord(vcf.getInfo()).getField(VcfHeaderUtils.INFO_EFFECT);
	 			
	 		//select the annotation with "HIGH" impact
	 		//str: HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1
	 		//array: 0| 1|2|3     |4|5         |6        |7         |8              |9|10	 
	 		assertTrue(maf.getColumnValue(39).equals("HIGH" ));
	 		assertTrue(maf.getColumnValue(9).equals("Splice_Site" ));
	 		
	 		assertTrue(maf.getColumnValue(51).equals(Dmaf.getColumnValue(50) ));
	 		assertTrue(maf.getColumnValue(52).equals(Dmaf.getColumnValue(51) ));
	 		assertTrue(maf.getColumnValue(1).equals("CCDC148-AS1" ));
	 		assertTrue(maf.getColumnValue(54).equals("antisense" ));
	 		assertTrue(maf.getColumnValue(55).equals("NON_CODING"));
	 		assertTrue(maf.getColumnValue(50).equals("ENST00000412781" ));
	 		assertTrue(maf.getColumnValue(56).equals("5" ));
	 		assertTrue(maf.getColumnValue(57).equals("1" ));		
	 		
	 		//for other columns after A.M confirmation
	 		assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
	 		assertTrue(maf.getColumnValue(3).equals(Vcf2mafOptions.default_center ));		
	 		assertTrue(maf.getColumnValue(4).equals(Dmaf.getColumnValue(4) ));		
	 		assertTrue(maf.getColumnValue(5).equals("Y"));		
	 		assertTrue(maf.getColumnValue(6).equals(parms[1] ));		
	 		assertTrue(maf.getColumnValue(7).equals(parms[1] ));		
	 		assertTrue(maf.getColumnValue(8).equals(Dmaf.getColumnValue(8) ));		
	 		assertTrue(maf.getColumnValue(10).equals(Dmaf.getColumnValue(10) ));	
	 		assertTrue(maf.getColumnValue(11).equals(parms[3] ));		
	 		
	 		//check format field	
			assertTrue(maf.getColumnValue(12).equals("A" ));			
	 		assertTrue(maf.getColumnValue(13).equals("C" ));	 		
	 		assertTrue(maf.getColumnValue(14).equals(SnpEffMafRecord.novel ));	//dbsnp		
	 		assertTrue(maf.getColumnValue(15).equals("VLD" ));	//dbSNP validation
	 		assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Unknown ));	//tumour sample	
	 		assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Unknown ));	//normal sample
	 		
			assertTrue(maf.getColumnValue(18).equals("T" ));		//normal allel1	
	 		assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
	 		assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
	 		assertTrue(maf.getColumnValue(36).equals("A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]" ));			//ND
	 		assertTrue(maf.getColumnValue(37).equals("C8[7.62],2[2],A2[8],28[31.18]" ));		
	 		assertTrue(maf.getColumnValue(38).equals(Dmaf.getColumnValue(38)  )); //not yet run confidence
	 		assertTrue(maf.getColumnValue(41).equals(Dmaf.getColumnValue(41)  )); //NNS unkown
	 		assertTrue(maf.getColumnValue(42).equals("GTGATATTCCC"  )); //Var_Plus_Flank	 
	 		assertTrue(maf.getColumnValue(43).equals("0.11"  )); //Var_Plus_Flank	 	
	 }

	 @Test    (expected=Exception.class)
	 public void indexTest() throws Exception{
		 String[] str = {"chr1\t204429212\trs71495004\tAT\tTG\t.\tSAT3;5BP4\tDB;CONF=ZERO;"
		 		+ "EFF=upstream_gene_variant(MODIFIER||3793|||PIK3C2B|processed_transcript|CODING|ENST00000496872||1),"
		 		+ "downstream_gene_variant(MODIFIER||4368||172|PIK3C2B|protein_coding|CODING|ENST00000367184||1|WARNING_TRANSCRIPT_INCOMPLETE),"
		 		+ "intron_variant(MODIFIER|||c.1503-143AT>CA|1634|PIK3C2B|protein_coding|CODING|ENST00000367187|8|1),"
		 		+ "intron_variant(MODIFIER|||c.1503-143AT>CA|1606|PIK3C2B|protein_coding|CODING|ENST00000424712|8|1);"
		 		+ "END=204429213\tACCS\tTG,1,3,_T,0,1"};
		 createVcf(str);
		 
		 final File input = new File( DbsnpModeTest.inputName);
		 final Vcf2maf v2m = new Vcf2maf(1,2);	
	 	 try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);
	 			System.out.println(maf.getMafLine());
	 		}	
         }	 
		 
	 }
	 
	 
	 
	 @Test
	 public void DefaultValueTest() throws Exception{
		 	final SnpEffMafRecord Dmaf = new SnpEffMafRecord();
			Dmaf.setDefaultValue();
			
			final Vcf2maf v2m = new Vcf2maf(2,1);	//test column2; normal column 1
			final String[] parms = {"chrY","22012840",".","C","A","."  ,  "."  ,  "."  ,  "."  ,  "." ,  "."};

	 		final VcfRecord vcf = new VcfRecord(parms);
	 		final SnpEffMafRecord maf = v2m.converter(vcf);
	 		
	 		
//	 		for(int i = 1 ; i < 58; i++)
//	 			System.out.println( i + ">>>> " + maf.getColumnValue(i) + " : " + Dmaf.getColumnValue(i));
	 		
	 		//5:Y, 6:22012840, 7:22012840, 11:C, 35:.
	 		
	 		for(int i = 1 ; i < 5; i++) 			
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)  ));  	

	 		for(int i = 8 ; i < 11; i++)
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)  ));  
	 		
	 		assertTrue(maf.getColumnValue(11).equals("C"  ));  
	 		
	 		for(int i = 12 ; i < 26; i++)
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)  ));  
	 		
	 		assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.FILTER_GERMLINE ));  
	 		
	 		for(int i = 27 ; i < 35; i++)
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)  ));  

	 		assertTrue(maf.getColumnValue(35).equals(Constants.MISSING_DATA_STRING ));  
	 		
	 		for(int i = 36 ; i < 58; i++)
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)  )); //not yet run confidence

	 }
	
	 @Test
	 public  void singleSampleTest() throws IOException, Exception{
 		 createVcf();
		 final File input = new File( DbsnpModeTest.inputName);
		 final Vcf2maf v2m = new Vcf2maf(1,1);	
	 	try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);
	 			assertTrue( maf.getColumnValue(36).equals(maf.getColumnValue(37)) );
	 			
	 			
	 		}	
        }
       
	 }	 
	 

	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST");
       // data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC;EFF=sequence_feature[compositionally_biased_region:Glu/Lys-rich](LOW|||c.1252G>C|591|CCDC148|protein_coding|CODING|ENST00000283233|10|1),splice_acceptor_variant(HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1)\tGT:GD:AC\t0/0:C/A:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]\t0/1:C/A:C8[7.62],2[2],A2[8],28[31.18]");        
       // data.add("chr2\t91888700\trs78405093\tG\tA\t.\tPASS\tMR=1217;NNS=1;FS=TGAGCACCTAC;GMAF=0.113802559414991;EFF=intron_variant(MODIFIER|||n.376-566C>T||AC027612.3|processed_transcript|NON_CODING|ENST00000436174|4|1),intron_variant(MODIFIER|||n.478-123C>T||AC027612.3|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000445955|4|1)\tGT:GD:AC\t1/1:G/T:G0[0],1217[35.41],T0[0],5[26.8],A0[0],7[34],C0[0],7[31]\t1/1:A/A:A0[0],1217[35.41],C0[0],5[26.8],G0[0],7[34],T0[0],7[31]"); 
    //    data.add("chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1");
    
        data.add("GL000236.1\t7127\t.\tT\tC\t.\tMR;MIUN\tSOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;EFF=non_coding_exon_variant(MODIFIER|||n.1313T>C||CU179654.1|processed_pseudogene|NON_CODING|ENST00000400789|1|1);CONF=ZERO\tGT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4\t0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5");
           try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}
	
	public static void createVcf(String[] str) throws IOException{
		 final List<String> data = new ArrayList<String>();
		 for(int i = 0; i < str.length; i++)
			 data.add(str[i]);
		 
         try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
             for (final String line : data)   out.write(line + "\n");                  
          }  
		
	}

}
