package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import scala.actors.threadpool.Arrays;
import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;


public class Vcf2mafTest {
	static String outputDir = new File(DbsnpModeTest.inputName).getAbsoluteFile().getParent() + "/output";
	static String inputName = DbsnpModeTest.inputName;		
	
	 @BeforeClass
	 public static void createIO(){
		 File out = new File(outputDir);
		 
		 if(  out.exists() && out.isDirectory())
			 return;
		 
		 assertTrue(new File(outputDir).mkdir());
	 }
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 
		 File out = new File(outputDir);
		 if(! out.exists() || !out.isDirectory())
			 return;
		 
		String[] files = new File(outputDir).list(); 
		for(int i = 0; i < files.length; i++)
			new File(outputDir, files[i]).delete();

		assertTrue(new File(outputDir).delete());	 		 
	}
	 
	 @Test
	 public void isHC() {
		 assertEquals(false, Vcf2maf.isHighConfidence(null));
		 assertEquals(false, Vcf2maf.isHighConfidence(new SnpEffMafRecord()));
		 SnpEffMafRecord maf = new SnpEffMafRecord();
		 maf.setColumnValue(MafElement.confidence, null);
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 
		 maf.setColumnValue(MafElement.confidence, "");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "blah");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "high");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_1,HIGH");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_1,HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_2,HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_2,HIGH_2");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.confidence, "HIGH_1,HIGH_2");
		 assertEquals(true, Vcf2maf.isHighConfidence(maf));
	 }
	 
	 
	 @Test
	 public void getAltColumns() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS", ".:.:A1[39],0[0],T1[35],0[0]:1:nns:nns2");
		 String ref = "A";
		 String alt = "T";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, false);
		 assertNotNull(altColumns);
		 assertEquals(7, altColumns.length);
	 }
	 @Test
	 public void getAltColumnsRealLife() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS","0/0:T/T:C0[0],2[34.5],T13[38.15],19[35.58]:2:2");
		 String ref = "T";
		 String alt = "C";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, false);
		 assertNotNull(altColumns);
		 assertEquals(7, altColumns.length);
		 assertEquals("2", altColumns[0]);
		 assertEquals("34", altColumns[1]);
		 assertEquals("32", altColumns[2]);
		 assertEquals("2", altColumns[3]);
		 assertEquals("T", altColumns[4]);
		 assertEquals("T", altColumns[5]);
		 assertEquals("C0[0],2[34.5],T13[38.15],19[35.58]", altColumns[6]);
	 }
	 
	 @Test
	 public void getAltColumnsRealLife2() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS","0/1:C/G:C6[39.5],2[42],G12[39.5],15[34.87]:8:8");
		 String ref = "C";
		 String alt = "G";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, false);
		 assertNotNull(altColumns);
		 assertEquals(7, altColumns.length);
		 assertEquals("8", altColumns[0]);
		 assertEquals("35", altColumns[1]);
		 assertEquals("8", altColumns[2]);
		 assertEquals("27", altColumns[3]);
		 assertEquals("C", altColumns[4]);
		 assertEquals("G", altColumns[5]);
		 assertEquals("C6[39.5],2[42],G12[39.5],15[34.87]", altColumns[6]);
	 }
	 
	 @Test
	 public void getAltColumnsRealLifeMerged() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&0/1:A/C&A/C:A13[38.85],5[40],C6[36.83],0[0]&A15[38.27],7[38.43],C7[36.14],0[0]:6&7:6&7:8,6:14:99:141,0,189");
		 String ref = "A";
		 String alt = "C";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, true);
		 assertNotNull(altColumns);
		 assertEquals(7, altColumns.length);
		 assertEquals("6", altColumns[0]);
		 assertEquals("24", altColumns[1]);
		 assertEquals("18", altColumns[2]);
		 assertEquals("6", altColumns[3]);
		 assertEquals("A", altColumns[4]);
		 assertEquals("C", altColumns[5]);
		 assertEquals("A13[38.85],5[40],C6[36.83],0[0]", altColumns[6]);
	 }
	 
	 @Test
	 public void compoundSNPTest() throws Exception{
		 //create vcf with compoundSNP variant and header
	 
	          
		String[] str = {  "##fileformat=VCFv4.0",VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST",
				"chrY\t2675826\trs75454623\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826;CONF=ZERO;EFF="
		        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr248Pro/c.742A>C|540|SAMD11|protein_coding|CODING|ENST00000455979|5|1|WARNING_TRANSCRIPT_NO_START_CODON),"		
		        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr329Pro/c.985A>C|588|SAMD11|protein_coding|CODING|ENST00000341065|9|1|WARNING_TRANSCRIPT_NO_START_CODON)," 
		        		+ "downstream_gene_variant(MODIFIER||1446||749|NOC2L|protein_coding|CODING|ENST00000327044||1)"
		        		+ "\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1"		 
		 };		 
		createVcf(str);
	          
	          
	          
		final Vcf2maf mode = new Vcf2maf(2, 1, "TEST", "CONTROL");		
		SnpEffMafRecord maf = null;
		
		try(VCFFileReader reader = new VCFFileReader(new File( inputName))){					
			//get control and test sample column
		//	mode.retriveSampleColumn("TEST", "CONTROL", reader.getHeader());					
			// SnpEffMafRecord.getSnpEffMafHeaderline();
		   	for (final VcfRecord vcf : reader) 
					maf = mode.converter(vcf);
		}

		assertFalse(maf == null);		
		assertTrue(maf.getColumnValue(14).equals("rs75454623"));					 
 		assertTrue(maf.getColumnValue(36).equals("TG,5,37,CA,0,2" ));			//ND
 		assertTrue(maf.getColumnValue(37).equals("AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1" ));	//TD
 		assertTrue(maf.getColumnValue(12).equals(SnpEffMafRecord.Null));   //TD allel1
 		assertTrue(maf.getColumnValue(13).equals(SnpEffMafRecord.Null));   //TD allel2		 		
 		assertTrue(maf.getColumnValue(18).equals(SnpEffMafRecord.Null));   //ND allel1
 		assertTrue(maf.getColumnValue(19).equals(SnpEffMafRecord.Null));   //ND allel2
 		assertTrue(maf.getColumnValue(33).equals("TEST"));   //tumour sample
 		assertTrue(maf.getColumnValue(34).equals("CONTROL"));   //normal sample
 		
 		assertTrue(maf.getColumnValue(41).equals("CA:ND0:TD0"));   //NNS field is not existed at format		 		//		 		
 		assertTrue(maf.getColumnValue(45).equals("103"));  //t_deep ("AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1" ))
 		assertTrue(maf.getColumnValue(46).equals("4"));  //t_ref C8[7.62],2[2]
 		assertTrue(maf.getColumnValue(47).equals("5"));  //t_allel A2[8],28[31.18]	
 		assertTrue(maf.getColumnValue(48).equals("44"));  //n_deep "TG,5,37,CA,0,2"
 		assertTrue(maf.getColumnValue(49).equals("42")); //C6[6.67],0[0]
 		assertTrue(maf.getColumnValue(50).equals("2"));  //A1[5],0[0]
 		
 		//we get consequnce with high rank, then long length, should be second annotation
 		assertTrue(maf.getColumnValue(51+1).equals("p.Thr329Pro"));
 		assertTrue(maf.getColumnValue(50+1).equals("ENST00000341065"));
 		assertTrue(maf.getColumnValue(52+1).equals("c.985A>C"));
	 }
	 
	 @Test
	 public void confidenceTest() {
		 
		 final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
		 final String[] parms = {"chrY","22012840",".","C","A",".","SBIAS","VLD;FLANK=GTGATATTCCC;VAF=0.11;"
				 + "CONF=HIGH_1,ZERO_2;EFF=sequence_feature[compositionally_biased_region:Glu/Lys-rich](LOW|||c.1252G>C|591|CCDC148|protein_coding|CODING|ENST00000283233|10|1),"
				 + "splice_acceptor_variant(HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1)",
				 "GT:GD:AC","0/0:C/C:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]","1/0:A/C:C8[7.62],2[2],A2[8],28[31.18]"};
		 
		 final VcfRecord vcf = new VcfRecord(parms);
		 final SnpEffMafRecord maf = v2m.converter(vcf);
		 
		 assertEquals("HIGH_1,ZERO_2", maf.getColumnValue(38));
		 
	 }
	 
	 @Test
	 public void isConsequence() {
		 SnpEffMafRecord maf = new SnpEffMafRecord();		 
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
		 maf.setColumnValue(MafElement.Transcript_BioType, "protein_coding");
		 assertEquals(true, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
		 assertEquals(true, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
		 
	 }
	 
	 @Test 
	 public void converterTest() {
		 
		 	final SnpEffMafRecord Dmaf = new SnpEffMafRecord();			
			final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
			final String[] parms = {"chrY","22012840",".","C","A",".","SBIAS","VLD;CONF=LOW;FLANK=GTGATATTCCC;VAF=0.11;"
					+ "IN=1;EFF=sequence_feature[compositionally_biased_region:Glu/Lys-rich](LOW|||c.1252G>C|591|CCDC148|protein_coding|CODING|ENST00000283233|10|1),"
					+ "splice_acceptor_variant(HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1)",
					"GT:GD:AC","0/0:C/C:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]","1/0:A/C:C8[7.62],2[2],A2[8],28[31.18]"};
			
	 		final VcfRecord vcf = new VcfRecord(parms);
	 		final SnpEffMafRecord maf = v2m.converter(vcf);
//	 		final String eff = new VcfInfoFieldRecord(vcf.getInfo()).getField(VcfHeaderUtils.INFO_EFFECT);	 			
	 		//select the annotation with "HIGH" impact
	 		//str: HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1
	 		//array: 0| 1|2|3     |4|5         |6        |7         |8              |9|10	 
 			 		
	 		assertEquals("LOW", maf.getColumnValue(MafElement.confidence));
	 		assertEquals(false, Vcf2maf.isHighConfidence(maf));
	 		
	 		assertEquals("HIGH", maf.getColumnValue(MafElement.Eff_Impact));
	 		assertTrue(maf.getColumnValue(52).equals(Dmaf.getColumnValue(52) ));
	 		assertTrue(maf.getColumnValue(53).equals(Dmaf.getColumnValue(53) ));
	 		assertTrue(maf.getColumnValue(54).equals(Dmaf.getColumnValue(54) ));
	 		assertTrue(maf.getColumnValue(1).equals("CCDC148-AS1" ));
	 		assertTrue(maf.getColumnValue(55).equals("antisense" ));
	 		assertTrue(maf.getColumnValue(56).equals("NON_CODING"));
	 		assertTrue(maf.getColumnValue(51).equals("ENST00000412781" ));
	 		assertTrue(maf.getColumnValue(57).equals("5" ));
	 		assertTrue(maf.getColumnValue(58).equals("1" ));	
	 		String ontology = "splice_acceptor_variant";
	 		assertTrue(maf.getColumnValue(59).equals(ontology ));
	 		assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));	 		
	 		assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));	 		
	 		assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
	 			 		
	 		//for other columns after A.M confirmation
	 		assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
	 		assertTrue(maf.getColumnValue(3).equals(Vcf2mafOptions.default_center ));		
	 		assertTrue(maf.getColumnValue(4).equals(Dmaf.getColumnValue(4) ));		
	 		assertTrue(maf.getColumnValue(5).equals("Y"));		
	 		assertTrue(maf.getColumnValue(6).equals(parms[1] ));		
	 		assertTrue(maf.getColumnValue(7).equals(parms[1] ));		
	 		assertTrue(maf.getColumnValue(8).equals(Dmaf.getColumnValue(8) ));		
	 		assertTrue(maf.getColumnValue(10).equals(IndelUtils.SVTYPE.SNP.name()));	
	 		assertTrue(maf.getColumnValue(11).equals(parms[3] ));		
	 		
	 		//check format field	
			assertTrue(maf.getColumnValue(12).equals("A" ));			
	 		assertTrue(maf.getColumnValue(13).equals("C" ));	 		
	 		assertTrue(maf.getColumnValue(14).equals(SnpEffMafRecord.novel ));	//dbsnp		
	 		assertTrue(maf.getColumnValue(15).equals("VLD" ));	//dbSNP validation
	 			 		
	 		assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Unknown ));	//tumour sample	
	 		assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Unknown ));	//normal sample
	 		
			assertTrue(maf.getColumnValue(18).equals("C" ));		//normal allel1	
	 		assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
	 		assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
	 		assertTrue(maf.getColumnValue(36).equals("A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]" ));	//ND
	 		assertTrue(maf.getColumnValue(37).equals("C8[7.62],2[2],A2[8],28[31.18]" ));		
	 		assertTrue(maf.getColumnValue(41).equals("A:ND0:TD0" )); //NNS unkown
	 		assertTrue(maf.getColumnValue(42).equals("GTGATATTCCC"  )); //Var_Plus_Flank	 
	 		assertTrue(maf.getColumnValue(43).equals("0.11"  )); //Var_Plus_Flank	 
	 		assertTrue(maf.getColumnValue(44).equals(Dmaf.getColumnValue(44)  )); //Germ=0,185 
	 		
	 		//"chrY","22012840",".","C","A",
	 		//"GT:GD:AC","0/0:T/C:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]","0/0:A/C:C8[7.62],2[2],A2[8],28[31.18]"};
	 		assertTrue(maf.getColumnValue(45).equals("40"));  // t_deep column2
	 		assertTrue(maf.getColumnValue(46).equals("10"));  // t_ref C8[7.62],2[2]
	 		assertTrue(maf.getColumnValue(47).equals("30"));  // t_allel A2[8],28[31.18]
	 		
	 		assertTrue(maf.getColumnValue(48).equals("29"));  // n_deep column1
	 		assertTrue(maf.getColumnValue(49).equals("6"));   // C6[6.67],0[0]
	 		assertTrue(maf.getColumnValue(50).equals("1"));   // A1[5],0[0]
	 		
	 		assertEquals("1", maf.getColumnValue(MafElement.INPUT));   // IN=1,2
	 		
	 		//other column
	 		assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.INFO_GERMLINE));  //somatic
	 }
	 
	 @Test 
	 public void converterMergedRecAll() {
		 
		 final SnpEffMafRecord Dmaf = new SnpEffMafRecord();			
		 final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
		 final String[] parms = {"chr1","625903",".","A","C",".","PASS_1;MIN_2","FLANK=TAATACTTTGG;SOMATIC_2;IN=1,2;CONF=HIGH_1,ZERO_2;EFF=upstream_gene_variant(MODIFIER||3850||312|OR4F16|protein_coding|CODING|ENST00000332831||1),intron_variant(MODIFIER|||n.169+29509T>G||RP5-857K21.4|lincRNA|NON_CODING|ENST00000440200|1|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&.:A/C&A/A:A5[41],13[39.31],C1[42],4[40.75]&A5[41],13[39.31],C1[42],4[40.75]:5&5:5&5:.:.:.:.","0/1&0/1:A/C&A/C:A20[41],23[39.78],C2[42],15[40.67]&A20[41],23[39.78],C2[42],15[40.67]:17&17:17&17:41,15:56:99:212,0,1149"};
		 
		 final VcfRecord vcf = new VcfRecord(parms);
		 final SnpEffMafRecord maf = v2m.converter(vcf);
		 
		 assertTrue(maf.getColumnValue(MafElement.confidence).equals("HIGH_1,ZERO_2" ));
	 	assertEquals(false, Vcf2maf.isHighConfidence(maf));
	 	
	 	assertEquals("MODIFIER", maf.getColumnValue(MafElement.Eff_Impact));
		 String ontology = "upstream_gene_variant";
		 assertTrue(maf.getColumnValue(59).equals(ontology ));
		 assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));	 		
		 assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));	 		
		 assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
		 
		 //for other columns after A.M confirmation
		 assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
		 assertTrue(maf.getColumnValue(3).equals(Vcf2mafOptions.default_center ));		
		 assertTrue(maf.getColumnValue(4).equals(Dmaf.getColumnValue(4) ));		
		 assertEquals("1", maf.getColumnValue(MafElement.Chromosome));
		 assertTrue(maf.getColumnValue(6).equals(parms[1] ));		
		 assertTrue(maf.getColumnValue(7).equals(parms[1] ));		
		 assertTrue(maf.getColumnValue(8).equals(Dmaf.getColumnValue(8) ));		
		 assertTrue(maf.getColumnValue(10).equals(IndelUtils.SVTYPE.SNP.name()));	
		 assertTrue(maf.getColumnValue(11).equals(parms[3] ));		
		 
		 //check format field	
		 assertTrue(maf.getColumnValue(12).equals("A" ));			
		 assertTrue(maf.getColumnValue(13).equals("C" ));	 		
		 assertTrue(maf.getColumnValue(14).equals(SnpEffMafRecord.novel ));	//dbsnp		
		 assertEquals(SnpEffMafRecord.Null, maf.getColumnValue(MafElement.dbSNP_Val_Status));	//dbSNP validation
		 
		 assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Unknown ));	//tumour sample	
		 assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Unknown ));	//normal sample
		 
		 assertTrue(maf.getColumnValue(18).equals("A" ));		//normal allel1	
		 assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
		 assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
		 assertEquals("A5[41],13[39.31],C1[42],4[40.75]", maf.getColumnValue(MafElement.ND));	//ND
		 assertTrue(maf.getColumnValue(MafElement.TD).equals("A20[41],23[39.78],C2[42],15[40.67]" ));		
		 
		 assertEquals("1,2", maf.getColumnValue(MafElement.INPUT));   // IN=1,2
		 
		 //other column
		 assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.INFO_GERMLINE));  //somatic
	 }
	 
	 @Test 
	 public void converterMergedRecHC() {
		 
		 final SnpEffMafRecord Dmaf = new SnpEffMafRecord();			
		 final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
		 final String[] parms = {"chr1","625903",".","A","C",".","PASS_1;PASS_2","FLANK=TAATACTTTGG;SOMATIC_1;SOMATIC_2;IN=1,2;CONF=HIGH_1,HIGH_2;EFF=upstream_gene_variant(MODIFIER||3850||312|OR4F16|protein_coding|CODING|ENST00000332831||1),intron_variant(MODIFIER|||n.169+29509T>G||RP5-857K21.4|lincRNA|NON_CODING|ENST00000440200|1|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&.:A/C&A/A:A5[41],13[39.31],C1[42],4[40.75]&A5[41],13[39.31],C1[42],4[40.75]:5&5:5&5:.:.:.:.","0/1&0/1:A/C&A/C:A20[41],23[39.78],C2[42],15[40.67]&A20[41],23[39.78],C2[42],15[40.67]:17&17:17&17:41,15:56:99:212,0,1149"};
		 
		 final VcfRecord vcf = new VcfRecord(parms);
		 final SnpEffMafRecord maf = v2m.converter(vcf);
		 
		 assertTrue(maf.getColumnValue(MafElement.confidence).equals("HIGH_1,HIGH_2" ));
		 assertEquals(true, Vcf2maf.isHighConfidence(maf));
		 
		 assertEquals("MODIFIER", maf.getColumnValue(MafElement.Eff_Impact));
		 String ontology = "upstream_gene_variant";
		 assertTrue(maf.getColumnValue(59).equals(ontology ));
		 assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));	 		
		 assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));	 		
		 assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
		 
		 //for other columns after A.M confirmation
		 assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
		 assertTrue(maf.getColumnValue(3).equals(Vcf2mafOptions.default_center ));		
		 assertTrue(maf.getColumnValue(4).equals(Dmaf.getColumnValue(4) ));		
		 assertEquals("1", maf.getColumnValue(MafElement.Chromosome));
		 assertTrue(maf.getColumnValue(6).equals(parms[1] ));		
		 assertTrue(maf.getColumnValue(7).equals(parms[1] ));		
		 assertTrue(maf.getColumnValue(8).equals(Dmaf.getColumnValue(8) ));		
		 assertTrue(maf.getColumnValue(10).equals(IndelUtils.SVTYPE.SNP.name()));	
		 assertTrue(maf.getColumnValue(11).equals(parms[3] ));		
		 
		 //check format field	
		 assertTrue(maf.getColumnValue(12).equals("A" ));			
		 assertTrue(maf.getColumnValue(13).equals("C" ));	 		
		 assertTrue(maf.getColumnValue(14).equals(SnpEffMafRecord.novel ));	//dbsnp		
		 assertEquals(SnpEffMafRecord.Null, maf.getColumnValue(MafElement.dbSNP_Val_Status));	//dbSNP validation
		 
		 assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Unknown ));	//tumour sample	
		 assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Unknown ));	//normal sample
		 
		 assertTrue(maf.getColumnValue(18).equals("A" ));		//normal allel1	
		 assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
		 assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
		 assertEquals("A5[41],13[39.31],C1[42],4[40.75]", maf.getColumnValue(MafElement.ND));	//ND
		 assertTrue(maf.getColumnValue(MafElement.TD).equals("A20[41],23[39.78],C2[42],15[40.67]" ));		
		 
		 assertEquals("1,2", maf.getColumnValue(MafElement.INPUT));   // IN=1,2
		 
		 //other column
		 assertTrue(maf.getColumnValue(MafElement.Mutation_Status).equals(VcfHeaderUtils.INFO_SOMATIC));  //somatic
	 }
	 
	 /**
	  * deal with p.Met1? column missing "/" 
	  * @throws Exception: unit tests will failed
	  */
	 @Test     
	 public void snpEffTest() throws Exception{
 		 
		 String[] str = {"chr10\t87489317\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
		 + "EFF=start_lost(HIGH||atg/GGtg|p.Met1?|580|GRID1|protein_coding|CODING|ENST00000536331||1);"
		 + "LOF=(GRID1|ENSG00000182771|4|0.25);END=87489318\tACCS\tTG,29,36,_G,0,1\tCC,4,12,TG,15,12"};
		 
		 createVcf(str);
		 
		 final File input = new File( DbsnpModeTest.inputName);
		 final Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
	 	 try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);	
	 			assertTrue(maf.getColumnValue(MafElement.Amino_Acid_Change  ).equals("p.Met1?")); //52
	 			assertTrue(maf.getColumnValue(MafElement.CDS_change).equals(SnpEffMafRecord.Null)); 		//53	 
	 		}	
         }	  	 
	 }
	 
	 /**
	  * 
	  * @throws Exception missing one sample column
	  */
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
		 final Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
	 	 try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);
	 		}	
         }	 
		 
	 }
	 	 
	 @Test
	 public void ConsequenceTest() throws Exception{
		 		 
		 String[] str = {"chr7\t140453136\trs121913227\tAC\tTT\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
		 		+ "EFF=missense_variant(MODERATE||gtg/AAg|p.Val207Lys/c.619GT>AA|374|BRAF|protein_coding|CODING|ENST00000496384||1|WARNING_TRANSCRIPT_NO_START_CODON),"
		 		+ "missense_variant(MODERATE||gtg/AAg|p.Val600Lys/c.1798GT>AA|766|BRAF|protein_coding|CODING|ENST00000288602||1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|16|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|12|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|14|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|18|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|15|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|11|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|17|1),"
		 		+ "sequence_feature[domain:Protein_kinase](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|13|1),"
		 		+ "sequence_feature[beta_strand](LOW|||c.1798AC>TT|766|BRAF|protein_coding|CODING|ENST00000288602|15|1),"
		 		+ "3_prime_UTR_variant(MODIFIER||54958|n.*1248GT>AA||BRAF|nonsense_mediated_decay|CODING|ENST00000497784|16|1),"
		 		+ "non_coding_exon_variant(MODIFIER|||n.82GT>AA||BRAF|nonsense_mediated_decay|CODING|ENST00000479537|2|1);END=140453137	"
		 		+ "ACCS	AC,14,20,A_,1,0	AC,33,36,A_,1,0,TC,1,0,TT,8,10,_C,0,2"};
		 		
		 createVcf(str);
		 
		 final File input = new File( DbsnpModeTest.inputName);
		 final Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
	 	 try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);
		 		//we get consequnce with high rank, then long length, should be second annotation
		 		assertTrue(maf.getColumnValue(51+1).equals("p.Val600Lys"));
		 		assertTrue(maf.getColumnValue(50+1).equals("ENST00000288602"));
		 		assertTrue(maf.getColumnValue(52+1).equals("c.1798GT>AA"));
	 		}	
         }			 
	 }
	 	 
	 @Test
	 public void defaultValueTest() throws Exception{
		 	final SnpEffMafRecord Dmaf = new SnpEffMafRecord();			
			final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1
			final String[] parms = {"chrY","22012840",".","CT","AT","."  ,  "."  ,  "."  ,  "."  ,  "." ,  "."};

	 		final VcfRecord vcf = new VcfRecord(parms);
	 		final SnpEffMafRecord maf = v2m.converter(vcf);
	 		
	 		for(int i = 1 ; i < 5; i++) 
	 			assertTrue(  maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)) );  	
	 		 
	 		for(int i = 8 ; i < 9; i++)
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i)) );  
 
	 		assertTrue(maf.getColumnValue(10).equals( IndelUtils.SVTYPE.DNP.name()) );  
	 		assertTrue(maf.getColumnValue(11).equals("CT"));  
	 		
	 		for(int i = 12 ; i < 26; i++) 
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));  
	 		 
	 		assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.INFO_GERMLINE ));  
	 		
	 		for(int i = 27 ; i < 35; i++)
 	 				assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));  

	 		assertTrue(maf.getColumnValue(35).equals(Constants.MISSING_DATA_STRING ));  
	 		
	 		for(int i = 36 ; i < 41; i++) 
	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));  
	 			 		
	 		assertTrue(maf.getColumnValue(41).equals("AT:ND0:TD0" ));  
	 	
	 		for(int i = 42 ; i < 61; i++) 			
 	 			assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));  	 		
 
	 }
	
	 @Test
	 public  void singleSampleTest() throws IOException, Exception{
 		 
			String[] str = {VcfHeaderUtils.STANDARD_FILE_VERSION + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\tFORMAT\tCONTROL\tTEST",
				"GL000236.1\t7127\t.\tT\tC\t.\tMR;MIUN\tSOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;EFF=non_coding_exon_variant(MODIFIER|||n.1313T>C||CU179654.1|processed_pseudogene|NON_CODING|ENST00000400789|1|1);CONF=ZERO\tGT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4\t0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5"};			
	        createVcf(str); 
	        
		 final File input = new File( inputName);
		 final Vcf2maf v2m = new Vcf2maf(1,1, null, null);	
	 	 try(VCFFileReader reader = new VCFFileReader(input); ){
	 		for (final VcfRecord vcf : reader){  		
	 			SnpEffMafRecord maf  = v2m.converter(vcf);
	 			assertTrue( maf.getColumnValue(36).equals(maf.getColumnValue(37)) );
	 			assertTrue( maf.getColumnValue(16).equals("Unknown"));
	 			assertTrue( maf.getColumnValue(33).equals(maf.getColumnValue(34)) );
 //		 	T\tC	GT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4			
		 		assertTrue(maf.getColumnValue(41).equals("C:ND4:TD4"));   //NNS field is not existed at format		 		
 		 		assertTrue(maf.getColumnValue(45).equals("27"));  //t_deep column2
		 		assertTrue(maf.getColumnValue(46).equals("27"));  //t_ref T9[37.11],18[38.33]
		 		assertTrue(maf.getColumnValue(47).equals("0"));  //t_allel  
		 		
 		 		assertTrue(maf.getColumnValue(48).equals("27"));  //n_deep column1
		 		assertTrue(maf.getColumnValue(49).equals("27")); //T9[37.11],18[38.33]
		 		assertTrue(maf.getColumnValue(50).equals("0"));  

	 		}	
         }
       
	 }	 
	 
	
	public static void createVcf(String[] str) throws IOException{
		 final List<String> data = new ArrayList<String>();
		 for(int i = 0; i < str.length; i++)
			 data.add(str[i]);
		 
         try(BufferedWriter out = new BufferedWriter(new FileWriter(inputName));) {          
             for (final String line : data)   out.write(line + "\n");                  
          }  		
	}
 	

	@Test
	public void FileNameTest() {
		String[] str = {VcfHeaderUtils.STANDARD_FILE_VERSION + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
				VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST"	
		};
		        
        try{
        	createVcf(str);       
        	File input = new File(DbsnpModeTest.inputName); 

 			final String command = "--mode vcf2maf --log " + outputDir + "/output.log  -i " + inputName + " --outdir " + outputDir;			
			final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");    
			assertEquals(0, exec.getErrCode());
			assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
			
			assertTrue(new File(outputDir + "/MELA_0264.CONTROL.TEST.maf").exists());
			//below empty files will be deleted at last stage
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.Consequence.maf").exists());
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.Consequence.maf").exists());
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.maf").exists());
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.maf").exists());			
        }catch(Exception e){
        	fail(e.getMessage()); 
        }
		
	}

	@Test
	public void FileNameWithNODonorTest() throws IOException{
		String[] str = {"##fileformat=VCFv4.0",			
				"##qControlSample=CONTROL",
				"##qTestSample=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST" };

        createVcf(str);
        
        try{        	
			final String command = "--mode vcf2maf --log " + outputDir + "/output.log  -i " + DbsnpModeTest.inputName + " --outdir " + outputDir;			
			final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");        	
			assertEquals(1, exec.getErrCode());			
			
        }catch(Exception e){
        	 fail(e.getMessage());
        }	
	}
		
	@Test
	public void FileNameWithNoSampleidTest() throws IOException{
		String[] str = {"##fileformat=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID +"=MELA_0264",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE +"=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST"};

        createVcf(str);
        
        try{  
			final String command = "--mode vcf2maf --control control  --log " + outputDir + "/output.log  -i " + inputName + " --outdir " + outputDir;			
			final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");        	
			assertEquals(0, exec.getErrCode());	
        }catch(Exception e){
        	 fail(e.getMessage());
        }
		
	}

}
