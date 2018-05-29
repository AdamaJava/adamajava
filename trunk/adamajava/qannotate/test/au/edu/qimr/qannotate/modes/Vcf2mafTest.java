package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;


public class Vcf2mafTest {
	static String outputDir = new File(DbsnpModeTest.inputName).getAbsoluteFile().getParent() + "/output";
	static String inputName = DbsnpModeTest.inputName;		
	static String logName = "output.log";	
	static String outputMafName = "output.maf";
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	 @BeforeClass
	 public static void createIO(){
		 File out = new File(outputDir);
		 
		 if(  out.exists() && out.isDirectory()){
			 return;
		 }
		 
		 assertTrue(new File(outputDir).mkdir());
	 }
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 
		 File out = new File(outputDir);
		 if( ! out.exists() || ! out.isDirectory()) {
			 return;
		 }
		 
		String[] files = new File(outputDir).list();
		assertEquals(false, null == files);
		for(int i = 0; i < files.length; i++) {
			new File(outputDir, files[i]).delete();
		}

		assertTrue(new File(outputDir).delete());	 		 
	}
	 
	 @Test
	 public void isHC() {
		 assertEquals(false, Vcf2maf.isHighConfidence(null));
		 assertEquals(false, Vcf2maf.isHighConfidence(new SnpEffMafRecord()));
		 SnpEffMafRecord maf = new SnpEffMafRecord();
		 maf.setColumnValue(MafElement.Confidence, null);
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 
		 maf.setColumnValue(MafElement.Confidence, "");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "blah");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "high");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_1,HIGH");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_1,HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_2,HIGH_1");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_2,HIGH_2");
		 assertEquals(false, Vcf2maf.isHighConfidence(maf));
		 maf.setColumnValue(MafElement.Confidence, "HIGH_1,HIGH_2");
		 assertEquals(true, Vcf2maf.isHighConfidence(maf));
	 }
	 
	 @Test
	 public void getDetailsFromVcfHeader() {
		 VcfHeader h = new VcfHeader();
		 assertEquals(false, getBamid("blah", h).isPresent());
		 h = createMergedVcfHeader();
		 assertEquals(false, getBamid("##qDonorId", h).isPresent());
		 assertEquals(true, getBamid("##1:qDonorId", h).isPresent());
	 }
	 
	 
	 private Optional<String> getBamid(String key, VcfHeader header){
		for (final VcfHeaderRecord hr : header.getAllMetaRecords()) { 
			if( hr.toString().indexOf(key) != -1) {
				return Optional.ofNullable(StringUtils.getValueFromKey(hr.toString(), key));
			}
		}
		return Optional.empty(); 
	}
	 
	 private VcfHeader createMergedVcfHeader() {
		 VcfHeader h = new VcfHeader();
		 
//have to remove empty line "##"		 
		 Arrays.asList("##fileformat=VCFv4.2",
"##fileDate=20160523",
"##qUUID=209dec81-a127-4aa3-92b4-2c15c21b75c7",
"##qSource=qannotate-2.0 (1170)",
"##1:qUUID=7554fdcc-7230-400e-aefe-5c9a4c79907b",
"##1:qSource=qSNP v2.0 (1170)",
"##1:qDonorId=my_donor",
"##1:qControlSample=my_control_sample",
"##1:qTestSample=my_test_sample",
"##1:qControlBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9031/aligned_read_group_sets/dna_primarytumour_externpsar20150414090_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##1:qControlBamUUID=null",
"##1:qTestBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9014/aligned_read_group_sets/dna_primarytumour_externpsar20150414076_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##1:qTestBamUUID=null",
"##1:qAnalysisId=e3afda85-469f-412b-8919-10cd31d2ca52",
"##2:qUUID=aa7d805f-2ec8-4aea-b1e6-7bc410a41c4b",
"##2:qSource=qSNP v2.0 (1170)",
"##2:qDonorId=my_donor",
"##2:qControlSample=my_control_sample",
"##2:qTestSample=my_test_sample",
"##2:qControlBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9031/aligned_read_group_sets/dna_primarytumour_externpsar20150414090_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##2:qControlBamUUID=null",
"##2:qTestBam=/mnt/lustre/working/genomeinfo/study/uqccr_amplicon_ffpe/donors/psar_9014/aligned_read_group_sets/dna_primarytumour_externpsar20150414076_nolibkit_truseqampliconcancerpanel_bwakit0712_miseq.bam",
"##2:qTestBamUUID=null",
"##2:qAnalysisId=3334e934-cb45-4215-9eb5-84b63d96a502",
"##2:qControlVcf=/mnt/lustre/home/oliverH/q3testing/analysis/9/7/97b3715c-0a80-4115-844e-cc877b2cf409/controlGatkHCCV.vcf",
"##2:qControlVcfUUID=null",
"##2:qControlVcfGATKVersion=3.4-46-gbc02625",
"##2:qTestVcf=/mnt/lustre/home/oliverH/q3testing/analysis/c/f/cfccdb1c-6c26-48e9-bd73-ad4ebd806aa6/testGatkHCCV.vcf",
"##2:qTestVcfUUID=null",
"##2:qTestVcfGATKVersion=3.4-46-gbc02625",
"##INPUT=1,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/e/3/e3afda85-469f-412b-8919-10cd31d2ca52/e3afda85-469f-412b-8919-10cd31d2ca52.vcf",
//"##INPUT=2,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/3/3/3334e934-cb45-4215-9eb5-84b63d96a502/3334e934-cb45-4215-9eb5-84b63d96a502.vcf").stream().forEach(h::parseHeaderLine);
"##INPUT=2,FILE=/mnt/lustre/home/oliverH/q3testing/analysis/3/3/3334e934-cb45-4215-9eb5-84b63d96a502/3334e934-cb45-4215-9eb5-84b63d96a502.vcf").stream().forEach(h::addOrReplace);

		 return h;
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
	 public void getAltColumnsRealLife3() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS","0/1:A/C:A21[33.81],15[34.2],C5[37.2],0[0]:5:5");
		 String ref = "A";
		 String alt = "C";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, false);
		 assertEquals(7, altColumns.length);
		 assertEquals("5", altColumns[0]);
		 assertEquals("41", altColumns[1]);
		 assertEquals("36", altColumns[2]);
		 assertEquals("5", altColumns[3]);
		 assertEquals("A", altColumns[4]);
		 assertEquals("C", altColumns[5]);
		 assertEquals("A21[33.81],15[34.2],C5[37.2],0[0]", altColumns[6]);
	 }
	 @Test
	 public void converter() {
		 
		 final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
		 final String[] parms = {"chrY","32463932",".","A","C",".","SBIASALT;5BP1","FLANK=CCCAACTAGTT;IN=1;DB;HOM=3,CCAAGCCCAAcTAGTTTTTTG;CONF=ZERO;EFF=downstream_gene_variant(MODIFIER||2001|||RP11-626K17.3|lincRNA|NON_CODING|ENST00000565549||1),intergenic_region(MODIFIER||||||||||1)",
				 "GT:GD:AC:MR:NNS","0/1:A/C:A21[33.81],15[34.2],C5[37.2],0[0]:5:5","0/0:A/A:A58[34.55],58[35],C7[37.14],0[0]:7:6"};
		 
		 final VcfRecord vcf = new VcfRecord(parms);
		 final SnpEffMafRecord maf = v2m.converter(vcf);
		 assertEquals("A", maf.getColumnValue(MafElement.Reference_Allele));
		 assertEquals("A", maf.getColumnValue(MafElement.Tumor_Seq_Allele1));
		 assertEquals("A", maf.getColumnValue(MafElement.Tumor_Seq_Allele2));
		 assertEquals("A", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele1));
		 assertEquals("C", maf.getColumnValue(MafElement.Match_Norm_Seq_Allele2));
	 }
	 
	 @Test
	 public void getAltColumnsRealLifeMerged() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&0/1:A/C&A/C:A13[38.85],5[40],C6[36.83],0[0]&A15[38.27],7[38.43],C7[36.14],0[0]:6&7:6&7:8,6:14:99:141,0,189");
		 String ref = "A";
		 String alt = "C";
		 SVTYPE type = SVTYPE.SNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, true);
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
	 public void getAltColumnsRealLifeMergedCS() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "ACCS","AA,5,4,GG,52,47,_G,2,1&AA,5,4,GG,52,47,_G,2,1");
		 String ref = "GG";
		 String alt = "AA";
		 SVTYPE type = SVTYPE.DNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, true);
		 assertEquals(7, altColumns.length);
		 assertEquals("0", altColumns[0]);
		 assertEquals("111", altColumns[1]);
		 assertEquals("99", altColumns[2]);
		 assertEquals("9", altColumns[3]);
		 assertEquals("GG", altColumns[4]);
		 assertEquals("AA", altColumns[5]);
		 assertEquals("AA,5,4,GG,52,47,_G,2,1", altColumns[6]);
	 }
	 @Test
	 public void getAltColumnsRealLifeMergedCS2() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord( "ACCS","CC,10,12,TT,29,14,T_,0,1&CC,10,12,TT,29,14,T_,0,1");
		 String ref = "CC";
		 String alt = "TT";
		 SVTYPE type = SVTYPE.DNP;
		 String [] altColumns = Vcf2maf.getAltCounts(format, ref, alt, type, true);
		 assertEquals(7, altColumns.length);
		 assertEquals("0", altColumns[0]);
		 assertEquals("66", altColumns[1]);
		 assertEquals("22", altColumns[2]);
		 assertEquals("43", altColumns[3]);
		 assertEquals("TT", altColumns[4]);
		 assertEquals("CC", altColumns[5]);
		 assertEquals("CC,10,12,TT,29,14,T_,0,1", altColumns[6]);
	 }
	 
	 @Test
	 public void compoundSNPTest() throws Exception{
	          
		String[] array = {
				"chrY","2675826","rs75454623","TG","CA",".","COVN12;MIUN","SOMATIC;END=2675826;CONF=ZERO;EFF="
		        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr248Pro/c.742A>C|540|SAMD11|protein_coding|CODING|ENST00000455979|5|1|WARNING_TRANSCRIPT_NO_START_CODON),"		
		        		+ "missense_variant(MODERATE|MISSENSE|Acc/Ccc|p.Thr329Pro/c.985A>C|588|SAMD11|protein_coding|CODING|ENST00000341065|9|1|WARNING_TRANSCRIPT_NO_START_CODON)," 
		        		+ "downstream_gene_variant(MODIFIER||1446||749|NOC2L|protein_coding|CODING|ENST00000327044||1)"
		        		+ "","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1"		 
		};
		
		final VcfRecord vcf = new VcfRecord(array);
		final Vcf2maf mode = new Vcf2maf(2, 1, "TEST", "CONTROL");
		SnpEffMafRecord maf = mode.converter(vcf);

		assertFalse(maf == null);		
		assertEquals("TG", maf.getColumnValue(11));	
		assertEquals("TA", maf.getColumnValue(12));	
		assertEquals("CA", maf.getColumnValue(13));	
		assertEquals("TG", maf.getColumnValue(18));	
		assertEquals("TG", maf.getColumnValue(19));	
		assertTrue(maf.getColumnValue(14).equals("rs75454623"));	 
 		assertTrue(maf.getColumnValue(36).equals("TG,5,37,CA,0,2" ));			//ND
 		assertTrue(maf.getColumnValue(37).equals("AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1" ));	//TD
 		assertTrue(maf.getColumnValue(33).equals("TEST"));   //tumour sample
 		assertTrue(maf.getColumnValue(34).equals("CONTROL"));   //normal sample
 		
 		assertTrue(maf.getColumnValue(41).equals("CA:ND0:TD0"));   //NNS field is not existed at format		 		//		 		
 		assertTrue(maf.getColumnValue(45).equals("103"));  //t_deep ("AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,_G,0,1" ))
 		assertTrue(maf.getColumnValue(46).equals("4"));  //t_ref C8[7.62],2[2]
 		assertTrue(maf.getColumnValue(47).equals("5"));  //t_allel A2[8],28[31.18]	
 		assertTrue(maf.getColumnValue(48).equals("44"));  //n_deep "TG,5,37,CA,0,2"
 		assertTrue(maf.getColumnValue(49).equals("42")); //C6[6.67],0[0]
 		assertTrue(maf.getColumnValue(50).equals("2"));  //A1[5],0[0]
 		
 		//we get consequence with high rank, then long length, should be second annotation
 		assertTrue(maf.getColumnValue(51+1).equals("p.Thr329Pro"));
 		assertTrue(maf.getColumnValue(50+1).equals("ENST00000341065"));
 		assertTrue(maf.getColumnValue(52+1).equals("c.985A>C"));
	 }
	
	 //do it tomorrow
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
		 
		 maf.setColumnValue(MafElement.Transcript_BioType, "");
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
		 
		 maf.setColumnValue(MafElement.Transcript_BioType, "processed_transcript");
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
		 
		 maf.setColumnValue(MafElement.Transcript_BioType, null);
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 5));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 1));
		 assertEquals(false, Vcf2maf.isConsequence(maf.getColumnValue(55), 6));
		 
	 }
	 
	 @Test
	 public void Flank_NoteTest(){
		VcfRecord vcf = new VcfRecord.Builder("chrY",22012840,"C").allele("A").build();
		final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1			
		
		//get flank first
		vcf.setInfo("HOM=28,CTTTTCTTTCaTTTTTTTTTT;FLANK=CTTTCATTTTT");				
		SnpEffMafRecord maf = v2m.converter(vcf);
		maf.getColumnValue(MafElement.Var_Plus_Flank).equals("CTTTCATTTTT");
		maf.getColumnValue(MafElement.Notes).equals("HOM=28");
		
		//get flank from reference
		vcf.setInfo("HOM=28,CTTTTCTTTCaTTTTTTTTTT;TRF=10_6,20_3");				
		maf = v2m.converter(vcf);
		maf.getColumnValue(MafElement.Var_Plus_Flank).equals("CTTTTCTTTCaTTTTTTTTTT");
		maf.getColumnValue(MafElement.Notes).equals("TRF=10_6,20_3;HOM=28");
		
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
 			 		
	 		assertEquals("LOW", maf.getColumnValue(MafElement.Confidence));
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
	 		assertTrue(maf.getColumnValue(3).equals(SnpEffMafRecord.center ));		
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
	 			 		
	 		assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Null));	//tumour sample	
	 		assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Null ));	//normal sample
	 		
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
	 		
	 		assertEquals("1", maf.getColumnValue(MafElement.Input));   // IN=1,2
	 		
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
		 
		 assertTrue(maf.getColumnValue(MafElement.Confidence).equals("HIGH_1,ZERO_2" ));
	 	assertEquals(false, Vcf2maf.isHighConfidence(maf));
	 	
	 	assertEquals("MODIFIER", maf.getColumnValue(MafElement.Eff_Impact));
		 String ontology = "upstream_gene_variant";
		 assertTrue(maf.getColumnValue(59).equals(ontology ));
		 assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));	 		
		 assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));	 		
		 assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
		 
		 //for other columns after A.M confirmation
		 assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
		 assertTrue(maf.getColumnValue(3).equals(SnpEffMafRecord.center ));		
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
		 assertEquals(SnpEffMafRecord.Null, maf.getColumnValue(MafElement.DbSNP_Val_Status ));	//dbSNP validation
		 
		 assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Null ));	//tumour sample	
		 assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Null ));	//normal sample
		 
		 assertTrue(maf.getColumnValue(18).equals("A" ));		//normal allel1	
		 assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
		 assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
		 assertEquals("A5[41],13[39.31],C1[42],4[40.75]", maf.getColumnValue(MafElement.ND));	//ND
		 assertTrue(maf.getColumnValue(MafElement.TD).equals("A20[41],23[39.78],C2[42],15[40.67]" ));		
		 
		 assertEquals("1,2", maf.getColumnValue(MafElement.Input));   // IN=1,2
		 
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
		 
		 assertTrue(maf.getColumnValue(MafElement.Confidence).equals("HIGH_1,HIGH_2" ));
		 assertEquals(true, Vcf2maf.isHighConfidence(maf));
		 
		 assertEquals("MODIFIER", maf.getColumnValue(MafElement.Eff_Impact));
		 String ontology = "upstream_gene_variant";
		 assertTrue(maf.getColumnValue(59).equals(ontology ));
		 assertTrue(maf.getColumnValue(60).equals( SnpEffConsequence.getClassicName(ontology) ));	 		
		 assertTrue(maf.getColumnValue(40).equals(SnpEffConsequence.getConsequenceRank(ontology)+""));	 		
		 assertTrue(maf.getColumnValue(9).equals(SnpEffConsequence.getMafClassification(ontology) ));
		 
		 //for other columns after A.M confirmation
		 assertTrue(maf.getColumnValue(2).equals(Dmaf.getColumnValue(2) ));		
		 assertTrue(maf.getColumnValue(3).equals(SnpEffMafRecord.center));		
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
		 assertEquals(SnpEffMafRecord.Null, maf.getColumnValue(MafElement.DbSNP_Val_Status ));	//dbSNP validation
		 
		 assertTrue(maf.getColumnValue(16).equals(SnpEffMafRecord.Null ));	//tumour sample	
		 assertTrue(maf.getColumnValue(17).equals(SnpEffMafRecord.Null ));	//normal sample
		 
		 assertTrue(maf.getColumnValue(18).equals("A" ));		//normal allel1	
		 assertTrue(maf.getColumnValue(19).equals("C" ));	 	//normal allel2		
		 assertTrue(maf.getColumnValue(35).equals(parms[6] ));		//QFlag is filter column	
		 assertEquals("A5[41],13[39.31],C1[42],4[40.75]", maf.getColumnValue(MafElement.ND));	//ND
		 assertTrue(maf.getColumnValue(MafElement.TD).equals("A20[41],23[39.78],C2[42],15[40.67]" ));		
		 
		 assertEquals("1,2", maf.getColumnValue(MafElement.Input));   // IN=1,2
		 
		 //other column
		 assertTrue(maf.getColumnValue(MafElement.Mutation_Status).equals(VcfHeaderUtils.INFO_SOMATIC));  //somatic
	 }
	 
	 @Test
	 public void whyIsItNotInMySHCCFile() {
		 
	 }
	 
	 /**
	  * deal with p.Met1? column missing "/" 
	  * @throws Exception: unit tests will failed
	  */
	 @Test     
	 public void snpEffTest() {
 		 
		 String[] str = {"chr10","87489317","rs386746181","TG","CC",".","PASS","SOMATIC;DB;CONF=HIGH;"
		 + "EFF=start_lost(HIGH||atg/GGtg|p.Met1?|580|GRID1|protein_coding|CODING|ENST00000536331||1);"
		 + "LOF=(GRID1|ENSG00000182771|4|0.25);END=87489318","ACCS","TG,29,36,_G,0,1","CC,4,12,TG,15,12"};
		 
		 Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
		 SnpEffMafRecord maf  = v2m.converter(new VcfRecord(str));	
		 assertTrue(maf.getColumnValue(MafElement.Amino_Acid_Change  ).equals("p.Met1?")); //52
		 assertTrue(maf.getColumnValue(MafElement.CDS_Change).equals(SnpEffMafRecord.Null)); 		//53	 
	 }
	 
	 @Test     
	 public void cdsChange() {
		 
		 String[] str = {"chr1","240975611","rs7537530","C","G",".","PASS_1;PASS_2","FLANK=GATAGGCACTA;AC=2;AF=1.00;AN=2;DP=15;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=28.59;SOR=4.047;IN=1,2;DB;VLD;VAF=0.6979;HOM=2,TATGAGATAGgCACTATTAAT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||c.879-268G>C|451|RGS7|protein_coding|CODING|ENST00000331110|13|1),intron_variant(MODIFIER|||c.798-268G>C|424|RGS7|protein_coding|CODING|ENST00000348120|10|1),intron_variant(MODIFIER|||c.957-268G>C|469|RGS7|protein_coding|CODING|ENST00000366562|12|1),intron_variant(MODIFIER|||c.957-268G>C|477|RGS7|protein_coding|CODING|ENST00000366563|13|1) intron_variant(MODIFIER|||c.957-268G>C|469|RGS7|protein_coding|CODING|ENST00000366564|13|1),intron_variant(MODIFIER|||c.957-268G>C|487|RGS7|protein_coding|CODING|ENST00000366565|13|1),intron_variant(MODIFIER|||c.798-268G>C|424|RGS7|protein_coding|CODING|ENST00000401882|10|1),intron_variant(MODIFIER|||c.957-268G>C|495|RGS7|protein_coding|CODING|ENST00000407727|12|1),intron_variant(MODIFIER|||c.450-268G>C|326|RGS7|protein_coding|CODING|ENST00000440928|6|1|WARNING_TRANSCRIPT_NO_START_CODON),intron_variant(MODIFIER|||c.705-268G>C|393|RGS7|protein_coding|CODING|ENST00000446183|13|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","1/1&1/1:G/G&G/G:G1[33],14[32.93]&G1[33],14[32.93]:15&15:14&14:0,15:15:45:628,45,0","1/1&1/1:G/G&G/G:G1[32],7[33.43]&G1[32],7[33.43]:8&8:6&6:0,8:8:27:374,27,0"};
		 
		 Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
		 SnpEffMafRecord maf  = v2m.converter(new VcfRecord(str));	
		 assertEquals("ENST00000407727", maf.getColumnValue(MafElement.Transcript_ID));
		 assertEquals("c.957-268G>C", maf.getColumnValue(MafElement.CDS_Change));
		 assertEquals("null", maf.getColumnValue(MafElement.Amino_Acid_Change));
		 assertEquals("RGS7", maf.getColumnValue(MafElement.Hugo_Symbol));
		 assertEquals("protein_coding", maf.getColumnValue(MafElement.Transcript_BioType));
		 assertEquals("CODING", maf.getColumnValue(MafElement.Gene_Coding));
		 assertEquals("12", maf.getColumnValue(MafElement.Exon_Intron_Rank));
		 assertEquals("1", maf.getColumnValue(MafElement.Genotype_Number));
	 }
	 
	 /**
	  * 
	  * @throws Exception missing one sample column
	  */
	 @Test    (expected=Exception.class)
	 public void indexTest() throws Exception{
		 String[] str = {"chr1","204429212","rs71495004","AT","TG",".","SAT3;5BP4","DB;CONF=ZERO;"
		 		+ "EFF=upstream_gene_variant(MODIFIER||3793|||PIK3C2B|processed_transcript|CODING|ENST00000496872||1),"
		 		+ "downstream_gene_variant(MODIFIER||4368||172|PIK3C2B|protein_coding|CODING|ENST00000367184||1|WARNING_TRANSCRIPT_INCOMPLETE),"
		 		+ "intron_variant(MODIFIER|||c.1503-143AT>CA|1634|PIK3C2B|protein_coding|CODING|ENST00000367187|8|1),"
		 		+ "intron_variant(MODIFIER|||c.1503-143AT>CA|1606|PIK3C2B|protein_coding|CODING|ENST00000424712|8|1);"
		 		+ "END=204429213","ACCS","TG,1,3,_T,0,1"};
		 Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
		 v2m.converter(new VcfRecord(str));
		 
	 }
	 	 
	 @Test
	 public void consequenceTest() {
		 		 
		 String[] str = {"chr7","140453136","rs121913227","AC","TT",".","PASS","SOMATIC;DB;CONF=HIGH;"
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
		 		
		 Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
		 SnpEffMafRecord maf  = v2m.converter( new VcfRecord(str));
		 assertTrue(maf.getColumnValue(51+1).equals("p.Val600Lys"));
 		 assertTrue(maf.getColumnValue(50+1).equals("ENST00000288602"));
 		 assertTrue(maf.getColumnValue(52+1).equals("c.1798GT>AA"));
		 
	 }
	 	 
	 @Test
	 public void defaultValueTest() {
		 	final SnpEffMafRecord Dmaf = new SnpEffMafRecord();			
			final Vcf2maf v2m = new Vcf2maf(2,1, null, null);	//test column2; normal column 1
			final String[] parms = {"chrY","22012840",".","CT","AT","."  ,  "."  ,  "."  ,  "."  ,  "." ,  "."};

	 		final VcfRecord vcf = new VcfRecord(parms);
	 		final SnpEffMafRecord maf = v2m.converter(vcf);
	 		
	 		for(int i = 1 ; i < 63; i++) {
	 			int ii = i;
	 			if( i >= 5 && i < 8) ii = 100; //ignore. do nothing
	 				 			
	 			switch(ii){
	 				case 100 : break;  
	 				case 10: assertTrue(maf.getColumnValue(10).equals( IndelUtils.SVTYPE.DNP.name()) ); break; 
	 				case 11: assertTrue(maf.getColumnValue(11).equals("CT"));  break; 
	 				case 26: assertTrue(maf.getColumnValue(26).equals(VcfHeaderUtils.INFO_GERMLINE )); break; 
	 				case 31: assertTrue(maf.getColumnValue(31).equals(Dmaf.getColumnValue(i) + ":" + Dmaf.getColumnValue(i))); break;
	 				case 35: assertTrue(maf.getColumnValue(35).equals(Constants.MISSING_DATA_STRING )); break;
	 				case 41: assertTrue(maf.getColumnValue(41).equals("AT:ND0:TD0" ));  break;
	 				default : assertTrue(maf.getColumnValue(i).equals(Dmaf.getColumnValue(i) ));  	
	 			}
	 			
	 		} 
	 		
	 }
	
	 @Test
	 public void BAMidTest()throws IOException, Exception{
			String[] str = {        		
					VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",	
					VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
					VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",	
					VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",				
					VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample",
					"GL000236.1\t7127\t.\tT\tC\t.\tMR;MIUN\tSOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;CONF=ZERO\tGT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4\t0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5"};			
		        createVcf(str); 
	        	try {
					Vcf2mafTest.createVcf(str);
					final String[] command = {"--mode", "vcf2maf",  "--log", logName,  "-i", inputName , "-o" , outputMafName};
					au.edu.qimr.qannotate.Main.main(command);
				} catch ( Exception e) {
					e.printStackTrace(); 
		        	fail(); 
				}                
				try(BufferedReader br = new BufferedReader(new FileReader(outputMafName));) {
				    String line = null;
				    while ((line = br.readLine()) != null) {
					    	if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header
					    	
						SnpEffMafRecord maf =  Vcf2mafIndelTest.toMafRecord(line);		
		 				assertTrue(maf.getColumnValue(16).equals("TEST_bamID"));
		 				assertTrue(maf.getColumnValue(17).equals(MafElement.Matched_Norm_Sample_Barcode.getDefaultValue()));     
		 				assertTrue(maf.getColumnValue(33).equals("TEST_sample"));
				 		assertTrue(maf.getColumnValue(MafElement.BAM_File).equals("TEST_bamID:null"));
				    }	
				}
		 
	 }
	 
	 @Test
	 public  void singleSampleTest() throws IOException, Exception{
 		 
		String[] str = {
			"GL000236.1","7127",".","T","C",".","MR;MIUN","SOMATIC;MR=4;NNS=4;FS=CCAGCCTATTT;EFF=non_coding_exon_variant(MODIFIER|||n.1313T>C||CU179654.1|processed_pseudogene|NON_CODING|ENST00000400789|1|1);CONF=ZERO","GT:GD:AC:MR:NNS","0/0:T/T:T9[37.11],18[38.33]:.:4","0/1:C/T:C1[12],3[41],T19[35.58],30[33.63]:.:5"};			
		    
		 final Vcf2maf v2m = new Vcf2maf(1,1, null, null);	
		SnpEffMafRecord maf  = v2m.converter(new VcfRecord(str));
		assertTrue( maf.getColumnValue(36).equals(maf.getColumnValue(37)) );
		assertTrue( maf.getColumnValue(16).equals(SnpEffMafRecord.Null));
		assertTrue( maf.getColumnValue(33).equals(maf.getColumnValue(34)) );
		//T\tC	GT:GD:AC:MR:NNS\t0/0:T/T:T9[37.11],18[38.33]:.:4			
		assertTrue(maf.getColumnValue(41).equals("C:ND4:TD4"));   //NNS field is not existed at format		 		
		assertTrue(maf.getColumnValue(45).equals("27"));  //t_deep column2
		assertTrue(maf.getColumnValue(46).equals("27"));  //t_ref T9[37.11],18[38.33]
		assertTrue(maf.getColumnValue(47).equals("0"));  //t_allel  
		
		assertTrue(maf.getColumnValue(48).equals("27"));  //n_deep column1
		assertTrue(maf.getColumnValue(49).equals("27")); //T9[37.11],18[38.33]
		assertTrue(maf.getColumnValue(50).equals("0"));  
		
		//check BAM_FILE columne
		assertTrue(maf.getColumnValue(MafElement.BAM_File).equals(MafElement.Tumor_Sample_Barcode.getDefaultValue() + ":" + MafElement.Matched_Norm_Sample_Barcode.getDefaultValue()));
		assertTrue(maf.getColumnValue(MafElement.Tumor_Sample_Barcode).equals(MafElement.Tumor_Sample_Barcode.getDefaultValue()));
		assertTrue(maf.getColumnValue(MafElement.Matched_Norm_Sample_Barcode).equals(MafElement.Matched_Norm_Sample_Barcode.getDefaultValue()));
       
	 }	 
	 
	
	public static void createVcf(String[] str) throws IOException{
		createVcf(new File(inputName), str);
	}
	public static void createVcf(File outputFile, String[] str) throws IOException{
		try(PrintWriter out = new PrintWriter(new FileWriter(outputFile));) {
			out.println(Arrays.stream(str).collect(Collectors.joining(Constants.NL_STRING)));
		}  		
	}
 	

	@Test
	public void fileNameTest() {
		String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
				VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST"	
		};
		        
        try{
        	createVcf(str);       
        	File input = new File(DbsnpModeTest.inputName); 						
	       	try {					 
				final String[] args = {"--mode", "vcf2maf",  "--log", outputDir + "/output.log",  "-i", inputName , "--outdir" , outputDir};
				au.edu.qimr.qannotate.Main.main(args);
			} catch ( Exception e) {
				e.printStackTrace(); 
	        	fail(); 
			}    			
			
			assertTrue(new File(outputDir + "/MELA_0264.CONTROL.TEST.maf").exists());
			//below empty files will be deleted at last stage
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.Consequence.maf").exists());
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.maf").exists());
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.maf").exists());		
			assertFalse(new File(outputDir + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.Consequence.maf").exists());			
        }catch(Exception e){
        		fail(e.getMessage()); 
        }
	}
	
	@Test
	public void areVcfFilesCreated() throws Exception {
		String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID + "=ABCD_1234",
				VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST"	,
				"chr10\t87489317\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
						 + "EFF=start_lost(HIGH||atg/GGtg|p.Met1?|580|GRID1|protein_coding|CODING|ENST00000536331||1);"
						 + "LOF=(GRID1|ENSG00000182771|4|0.25);END=87489318\tACCS\tTG,29,36,_G,0,1\tCC,4,12,TG,15,12"
		};
		
			
		File vcf = testFolder.newFile();
		File output = testFolder.newFile();
		createVcf(vcf, str);
			
		String [] command = {"--mode", "vcf2maf", "--log" , output.getParent() + "/output.log",  "-i" , vcf.getAbsolutePath() , "-o" , output.getAbsolutePath()};			
		Options options = new Options(command);
		options.parseArgs(command);
        new Vcf2maf(options );
			
        String SHCC  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
		String SHC = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.maf") ;
		String GHCC  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
		String GHC = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.maf") ;
		String SHCCVcf  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.vcf") ;
		String SHCVcf = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.vcf") ;
		String GHCCVcf  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.vcf") ;
		String GHCVcf = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.vcf") ;
        
        
		assertEquals(true, output.exists());
		assertEquals(true, new File(output.getAbsolutePath().replaceAll("maf", ".vcf")).exists());
		assertEquals(true, new File(SHCC).exists());
		assertEquals(true, new File(SHC).exists());
		assertEquals(true, new File(GHCC).exists());
		assertEquals(true, new File(GHC).exists());
		assertEquals(true, new File(SHCCVcf).exists());
		assertEquals(true, new File(SHCVcf).exists());
		assertEquals(true, new File(GHCCVcf).exists());
		assertEquals(true, new File(GHCVcf).exists());
	}

	@Test
	public void fileNameWithNODonorTest() throws IOException{
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
	public void fileNameWithNoSampleidTest() throws IOException{
		String[] str = {"##fileformat=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID +"=MELA_0264",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE +"=TEST",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST"};

        createVcf(str);
        
        try {
			final String command = "--mode vcf2maf --control control  --log " + outputDir + "/output.log  -i " + inputName + " --outdir " + outputDir;	
//			String[] args = {"-m", "vcf2maf", "--control", "control"," --log", outputDir + "/output.log" ,"-i",  inputName , "--outdir " ,outputDir};
//			Main.main(args);
			final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");        	
			assertEquals(0, exec.getErrCode());	
        } catch (Exception e){
        	 fail(e.getMessage());
        }
		
	}

}