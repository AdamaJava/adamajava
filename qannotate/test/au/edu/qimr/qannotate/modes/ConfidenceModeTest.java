package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.qcmg.common.util.Constants.VCF_MERGE_DELIM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.SampleColumn;

public class ConfidenceModeTest {
	
	final static String VerifiedFileName = "verify.vcf";
	final static String patient = "APGI_2001";
	
//	@BeforeClass
//	public static void createInput() throws IOException{
//		createVerifiedFile();
//	}
	
	 @AfterClass
	 public static void deleteIO(){
		 new File(DbsnpModeTest.inputName).delete();
		 new File(VerifiedFileName).delete();
		 new File(DbsnpModeTest.outputName).delete();		 
	 }
	 
	 
	 @Test
	 public void realLifeFail() {
		 //chr1    4985568 rs10753395      A       C       .       PASS_1;PASS_2   FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816   GT:GD:AC:MR:NNS:AD:DP:GQ:PL     0/1:A/C:A8[33.75],11[38.82],C3[42],5[40],A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18,8:26:99:274,0,686      1/1:C/C:A1[37],0[0],C23[38.96],19[41.21],A1[37],0[0],C24[38.88],23[40.26]:42,47:38,42:1,44:45:94:1826,94,0
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 4985568, 4985568), "rs10753395","A", "C");
		 vcf.setFilter("PASS_1;PASS_2");
		 vcf.setInfo("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816");
		 List<String> ff =  Arrays.asList("GT:GD:AC:MR:NNS:AD:DP:GQ:PL", "0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]"+VCF_MERGE_DELIM+"A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18"+VCF_MERGE_DELIM+"8:26:99:274,0,686", "1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]"+VCF_MERGE_DELIM+"A1[37],0[0],C24[38.88],23[40.26]:42"+VCF_MERGE_DELIM+"47:38"+VCF_MERGE_DELIM+"42:1,44:45:94:1826,94,0");
		 vcf.setFormatFields(ff);
		 
		 assertEquals(true, ConfidenceMode.checkNovelStarts(7, vcf.getSampleFormatRecord(1)));
		 assertEquals(true, ConfidenceMode.checkNovelStarts(8, vcf.getSampleFormatRecord(1)));
		 assertEquals(false, ConfidenceMode.checkNovelStarts(9, vcf.getSampleFormatRecord(1)));
		 
		 assertEquals(true, ConfidenceMode.checkNovelStarts(9, vcf.getSampleFormatRecord(2)));
	 }
	 
	 
	 @Test
	 public void HomoplymerTest() {
		 //chr1	152281007	.	AA	GG	.	PASS_1;PASS_2	IN=1,2;CONF=ZERO_1,ZERO_2;EFF=missense_variant(MODERATE||cattat/caCCat|p.HisTyr2118HisHis/c.6354TT>CC|4061|FLG|protein_coding|CODING|ENST00000368799||1),sequence_feature[compositionally_biased_region:Ser-rich](LOW|||c.6354AA>GG|4061|FLG|protein_coding|CODING|ENST00000368799|3|1),upstream_gene_variant(MODIFIER||4928|||FLG-AS1|antisense|NON_CODING|ENST00000392688||1),intron_variant(MODIFIER|||n.463-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000420707|4|1),intron_variant(MODIFIER|||n.377-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000593011|2|1)	ACCS	AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1	AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 152281007, 152281007), "rs10753395","AA", "GG");
		 vcf.setFilter("PASS_1;PASS_2");
		 vcf.setInfo("IN=1,2;HOM=5,ATGCAggAATGC");
		 List<String> ff =  Arrays.asList("ACCS", "AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1", "AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1");
		 vcf.setFormatFields(ff);
		 
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 		 
 		 String conf = vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH_1,HIGH_2", conf);
		 		 
		 vcf.setInfo("IN=1,2;HOM=6,ATGAAggAATGC");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.addAnnotation();		 		 
		 conf = vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("LOW_1,LOW_2", conf);
	 }
	 
	 @Test
	 public void willMultipleACValuesWork() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord("GT:GD:AC:MR:NNS:AD:DP:GQ:PL", "0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]"+VCF_MERGE_DELIM+"A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18"+VCF_MERGE_DELIM+"8:26:99:274,0,686");
		 assertEquals(19, VcfUtils.getAltFrequency(format, "A"));
	 }
	 @Test
	 public void willMultipleACCSValuesWork() {
		 //chr1	152281007	.	AA	GG	.	PASS_1;PASS_2	IN=1,2;CONF=ZERO_1,ZERO_2;EFF=missense_variant(MODERATE||cattat/caCCat|p.HisTyr2118HisHis/c.6354TT>CC|4061|FLG|protein_coding|CODING|ENST00000368799||1),sequence_feature[compositionally_biased_region:Ser-rich](LOW|||c.6354AA>GG|4061|FLG|protein_coding|CODING|ENST00000368799|3|1),upstream_gene_variant(MODIFIER||4928|||FLG-AS1|antisense|NON_CODING|ENST00000392688||1),intron_variant(MODIFIER|||n.463-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000420707|4|1),intron_variant(MODIFIER|||n.377-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000593011|2|1)	ACCS	AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1	AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 152281007, 152281007), "rs10753395","AA", "GG");
		 vcf.setFilter("PASS_1;PASS_2");
		 vcf.setInfo("IN=1,2;EFF=missense_variant(MODERATE||cattat/caCCat|p.HisTyr2118HisHis/c.6354TT>CC|4061|FLG|protein_coding|CODING|ENST00000368799||1),sequence_feature[compositionally_biased_region:Ser-rich](LOW|||c.6354AA>GG|4061|FLG|protein_coding|CODING|ENST00000368799|3|1),upstream_gene_variant(MODIFIER||4928|||FLG-AS1|antisense|NON_CODING|ENST00000392688||1),intron_variant(MODIFIER|||n.463-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000420707|4|1),intron_variant(MODIFIER|||n.377-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000593011|2|1)	");
		 List<String> ff =  java.util.Arrays.asList("ACCS", "AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1", "AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1");
		 vcf.setFormatFields(ff);
		 
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 VcfInfoFieldRecord info = vcf.getInfoRecord();
		 String conf = info.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH_1,HIGH_2", conf);
	 }
	 
	 @Test
	 public void confidenceWhenVAFIsEqualToDot() {
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 152281007, 152281007), "rs10753395","AA", "GG");
		 vcf.setFilter("PASS_1;PASS_2");
		 vcf.setInfo("FLANK=ATATAGACATG;AC=1;AF=0.500;AN=2;BaseQRankSum=0.035;ClippingRankSum=-0.354;DP=34;FS=1.377;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.425;QD=12.14;ReadPosRankSum=-0.921;SOR=1.061;IN=1,2;DB;VAF=.");
		 List<String> ff =  java.util.Arrays.asList("ACCS", "AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1", "AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1");
		 vcf.setFormatFields(ff);
		 
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 VcfInfoFieldRecord info = vcf.getInfoRecord();
		 String conf = info.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH_1,HIGH_2", conf);
		 String vaf = info.getField(VcfHeaderUtils.INFO_VAF);
		 assertEquals(".", vaf);
		 
	 }
	 @Test
	 public void confidenceRealLifeSingle() {
		 //chr8	12306635	rs28428895	C	T	.	PASS	FLANK=ACACATACATA;DB;CONF=HIGH;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)	GT:GD:AC:MR:NNS	0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8	0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".","PASS","FLANK=ACACATACATA;DB;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:GD:AC:MR:NNS","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8","0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1"});
		 VcfRecord vcf2 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T","57.77","MIN;MR;NNS","SOMATIC;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:AD:DP:GQ:PL:GD:AC:MR:NNS",".:.:.:.:.:C/C:C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]:15:15","0/1:4,3:7:86:86,0,133:C/T:C22[36.23],22[36.91],T2[26.5],1[42]:3:2"});
		 
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf1.getChrPosition(), java.util.Arrays.asList(vcf1, vcf2));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
		 VcfInfoFieldRecord info1 = vcf1.getInfoRecord();
		 String conf1 = info1.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH", conf1);
		 VcfInfoFieldRecord info2 = vcf2.getInfoRecord();
		 String conf2 = info2.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("ZERO", conf2);
	 }
	 
	 @Test
	 public void confidenceRealLifeSingle2() throws Exception {
		 //chr1    88259110        .       C       G       40.1    PASS    AC=2;AF=1.00;AN=2;DP=4;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=10.02;SOR=3.258;IN=2;CONF=ZERO;EFF=intergenic_region(MODIFIER||||||||||1)        GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        1/1:0,4:4:11:68,11,0:G/G:G4[25],4[36]:.:8:2
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","88259110",".","C","G","40.1","PASS","AC=2;AF=1.00;AN=2;DP=4;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=10.02;SOR=3.258;IN=2;EFF=intergenic_region(MODIFIER||||||||||1)","GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS","1/1:0,4:4:11:68,11,0:G/G:G4[25],4[36]:.:8:2"});
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.setMRPercentage(5d);
		 cm.setNNSCount(1);
		 cm.positionRecordMap.put(vcf1.getChrPosition(), java.util.Arrays.asList(vcf1));
		 cm.setSampleColumn(1,1);
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 VcfInfoFieldRecord info1 = vcf1.getInfoRecord();
		 String conf1 = info1.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH", conf1);
	 }
	 @Test
	 public void confidenceRealLifeSingle3() throws Exception {
		 //chr1    197323972       .       A       C       .       SBIASCOV        FLANK=CGCCCCCCCCC;IN=1   GT:GD:AC:DP:OABS:MR:NNS 0/1:A/C:A0[0],8[14],C0[0],7[19.71],G0[0],1[14]:16:A0[0]8[14];C0[0]7[19.71];G0[0]1[14]:7:4
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","197323972",".","A","C",".","SBIASCOV","FLANK=CGCCCCCCCCC;IN=1","GT:GD:AC:DP:OABS:MR:NNS","0/1:A/C:A0[0],8[14],C0[0],7[19.71],G0[0],1[14]:16:A0[0]8[14];C0[0]7[19.71];G0[0]1[14]:7:4"});
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.setMRPercentage(5d);
		 cm.setNNSCount(1);
		 cm.setFiltersToIgnore(Arrays.asList("SBIASCOV"));
		 cm.positionRecordMap.put(vcf1.getChrPosition(), Arrays.asList(vcf1));
		 cm.setSampleColumn(1,1);
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 VcfInfoFieldRecord info1 = vcf1.getInfoRecord();
		 String conf1 = info1.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH", conf1);
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged() {
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".","PASS_1;MIN_2;MR_2;NNS_2","FLANK=ACACATACATA;SOMATIC_2;IN=1,2;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&.:C/T&C/C:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]&C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]:8&15:8&15:.:.:.:.","0/0&0/1:C/C&C/T:C19[36.11],20[38.45],T1[42],0[0]&C22[36.23],22[36.91],T2[26.5],1[42]:1&3:1&2:4,3:7:86:86,0,133"});
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 VcfInfoFieldRecord info = vcf.getInfoRecord();
		 String conf = info.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH_1,ZERO_2", conf);
	 }
	 
	 @Test
	 public void confidenceRealLife2() {
		 //chr9	126129715	rs57014689	C	A	205.77	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB	GT:AD:DP:GQ:PL:GD:AC:MR:NNS	0/1:6,5:11:99:234,0,331:A/C:A0[0],4[24.25],C243[17.06],65[18.88],G2[7],0[0]:4:4	1/1:1,18:19:46:841,46,0:A/A:A2[7],15[28.73],C179[15.92],121[14.76],G0[0],1[7]:17:16
		 //chr9	126129715	rs57014689	C	A	.	PASS	SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185	GT:GD:AC:MR:NNS	0/0:C/C:A0[0],4[24.25],C128[17.64],30[20.9],G2[7],0[0]:4:4	0/1:A/C:A2[7],13[28.23],C96[17.22],54[14.22]:15:15
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A","205.77","PASS","AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB","GT:AD:DP:GQ:PL:GD:AC:MR:NNS","0/1:6,5:11:99:234,0,331:A/C:A0[0],4[24.25],C243[17.06],65[18.88],G2[7],0[0]:4:4","1/1:1,18:19:46:841,46,0:A/A:A2[7],15[28.73],C179[15.92],121[14.76],G0[0],1[7]:17:16"});
		 VcfRecord vcf2 = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A",".","PASS","SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185","GT:GD:AC:MR:NNS","0/0:C/C:A0[0],4[24.25],C128[17.64],30[20.9],G2[7],0[0]:4:4","0/1:A/C:A2[7],13[28.23],C96[17.22],54[14.22]:15:15"});
		 
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf1.getChrPosition(), Arrays.asList(vcf1, vcf2));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
		 VcfInfoFieldRecord info1 = vcf1.getInfoRecord();
		 String conf1 = info1.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("LOW", conf1);
		 VcfInfoFieldRecord info2 = vcf2.getInfoRecord();
		 String conf2 = info2.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("HIGH", conf2);
	 }
	 @Test
	 public void confidenceRealLifeMerged2() throws Exception {
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A",".","PASS_1;PASS_2","SOMATIC_1;FLANK=CCCCCACACCC;AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;IN=1,2;DB;GERM=5,185","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/0&0/1:C/C&A/C:A0[0],4[24.25],C128[17.64],30[20.9],G2[7],0[0]&A0[0],4[24.25],C243[17.06],65[18.88],G2[7],0[0]:4&4:4&4:6,5:11:99:234,0,331","0/1&1/1:A/C&A/A:A2[7],13[28.23],C96[17.22],54[14.22]&A2[7],15[28.73],C179[15.92],121[14.76],G0[0],1[7]:15&17:15&16:1,18:19:46:841,46,0"});
		 ConfidenceMode cm =new ConfidenceMode("");
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.setSampleColumn(2,1);
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 VcfInfoFieldRecord info = vcf.getInfoRecord();
		 String conf = info.getField(VcfHeaderUtils.INFO_CONFIDENCE);
		 assertEquals("LOW_1,LOW_2", conf);
	 }
	 
	 @Test
	 public void classB() {
		 try {
			 assertEquals(false, ConfidenceMode.isClassB(null));
			 Assert.fail();
		 } catch (IllegalArgumentException iae) {}
		 
		 assertEquals(true, ConfidenceMode.isClassB(""));
		 assertEquals(true, ConfidenceMode.isClassB(SnpUtils.PASS));
		 assertEquals(true, ConfidenceMode.isClassB(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
		 assertEquals(true, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_12_READS_NORMAL));
		 assertEquals(true, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_3_READS_NORMAL));
		 assertEquals(true, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_3_READS_NORMAL + ";" + SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
		 assertEquals(false, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_3_READS_NORMAL + ";" + SnpUtils.MUTATION_IN_UNFILTERED_NORMAL + ";" + SnpUtils.LESS_THAN_12_READS_NORMAL));
		 assertEquals(false, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_8_READS_TUMOUR));
		 assertEquals(false, ConfidenceMode.isClassB(SnpUtils.LESS_THAN_8_READS_TUMOUR + ";" + SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
	 }
	 
	 @Test
	 public void classBMerged() {
		 VcfRecord rec = VcfUtils.createVcfRecord("1", 1);
		 rec.setFilter(SnpUtils.MERGE_PASS);
		 String filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 String filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(true, ConfidenceMode.isClassB(filter1));
		 assertEquals(true, ConfidenceMode.isClassB(filter2));
		 rec.setFilter("PASS_1;MIN_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(true, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("PASS_1;SAN3_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(true, ConfidenceMode.isClassB(filter1));
		 assertEquals(true, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("PASS_1;NCIT_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(true, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("5BP2_1;MIN_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(false, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("SBIASALT_1;5BP1_1;MIN_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(false, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("COVN8_1;5BP1_1;SBIASCOV_1;COVN8_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(false, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("SAN3_1;SBIASCOV_1;COVN12_2;MIN_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(false, ConfidenceMode.isClassB(filter1));
		 assertEquals(false, ConfidenceMode.isClassB(filter2));
		 
		 rec.setFilter("SAN3_1;SBIASCOV_1;COVN12_2");
		 filter1 = VcfUtils.getFiltersEndingInSuffix(rec, "_1").replace("_1", "");
		 filter2 = VcfUtils.getFiltersEndingInSuffix(rec, "_2").replace("_2", "");
		 assertEquals(false, ConfidenceMode.isClassB(filter1));
		 assertEquals(true, ConfidenceMode.isClassB(filter2));
		 
	 }
	 
	 @Test
	 public void novelStarts() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord("ACCS","GA,1,2,GT,1,0,TG,43,51,GG,0,1,TA,0,2");
		 assertEquals(true, ConfidenceMode.checkNovelStarts(0, format));
		 format = new VcfFormatFieldRecord("GT:GD:AC:MR:NNS","1/1:G/G:A2[40.5],0[0],G2[34],1[35]:3:3");
		 assertEquals(true, ConfidenceMode.checkNovelStarts(0, format));
		 assertEquals(true, ConfidenceMode.checkNovelStarts(1, format));
		 assertEquals(true, ConfidenceMode.checkNovelStarts(2, format));
		 assertEquals(true, ConfidenceMode.checkNovelStarts(3, format));
		 assertEquals(false, ConfidenceMode.checkNovelStarts(4, format));
	 }
	
	 @Test
	 public void altFrequence() {
		 /*
		  * Compound snps first
		  */
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord("ACCS","GA,1,2,GT,1,0,TG,43,51,GG,0,1,TA,0,2");
		 assertEquals(101, VcfUtils.getAltFrequency(format, null));
		 assertEquals(3, VcfUtils.getAltFrequency(format, "GA"));
		 assertEquals(1, VcfUtils.getAltFrequency(format, "GT"));
		 assertEquals(94, VcfUtils.getAltFrequency(format, "TG"));
		 assertEquals(1, VcfUtils.getAltFrequency(format, "GG"));
		 assertEquals(2, VcfUtils.getAltFrequency(format, "TA"));
		 assertEquals(0, VcfUtils.getAltFrequency(format, "AB"));
		 
		 /*
		  * regular snps
		  */
		 format = new VcfFormatFieldRecord("AC","C1[35],2[39],T2[40],1[7]");
		 assertEquals(6, VcfUtils.getAltFrequency(format, null));
		 assertEquals(3, VcfUtils.getAltFrequency(format, "C"));
		 assertEquals(3, VcfUtils.getAltFrequency(format, "T"));
		 assertEquals(0, VcfUtils.getAltFrequency(format, "A"));
		 assertEquals(0, VcfUtils.getAltFrequency(format, ""));
		 assertEquals(0, VcfUtils.getAltFrequency(format, "XYZ"));
		 
		 
	 }
	
	//@Ignore 
	 @Test (expected=Exception.class)
	public void SampleColumnTest() throws IOException, Exception{
		final ConfidenceMode mode = new ConfidenceMode(patient);		
		mode.inputRecord(new File(DbsnpModeTest.inputName));
		mode.addAnnotation();
				
		//new SampleColumn();
	}

	 @Test
	 public void ConfidenceTest() throws IOException, Exception{	
	 	DbsnpModeTest.createVcf();
		final ConfidenceMode mode = new ConfidenceMode(patient);		
		mode.inputRecord(new File(DbsnpModeTest.inputName));

		String Scontrol = "EXTERN-MELA-20140505-001";
		String Stest = "EXTERN-MELA-20140505-002";
		mode.header.addOrReplace("##qControlSample=" + Scontrol);
		mode.header.addOrReplace("##qTestSample="+ Stest);	
		mode.header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tEXTERN-MELA-20140505-001\tEXTERN-MELA-20140505-002");			
		
		SampleColumn column = SampleColumn.getSampleColumn(Stest, Scontrol, mode.header);
		mode.setSampleColumn(column.getTestSampleColumn(), column.getControlSampleColumn() );
		
		mode.addAnnotation();
		mode.reheader("unitTest", DbsnpModeTest.inputName);
		mode.writeVCF(new File(DbsnpModeTest.outputName)  );
		
		try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){				 				 
			for (final VcfRecord re : reader) {		
				final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo()); 				
				if(re.getPosition() == 2675826) 
					//compound SNPs
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.LOW.name())); 
				else if(re.getPosition() == 22012840)
					//isClassB
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.LOW.name())); 
//				else if(re.getPosition() == 14923588)
				else if(re.getPosition() == 14923588 || re.getPosition() == 2675825)
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.ZERO.name())); 
				else
					//"chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21"
					//TG alleles is 13 > 5 filter is PASS
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.HIGH.name())); 
			}
		 }		 	 	 
  			
	 }
	 
	 
	 @Test
	 public void SampleColumnNoIDTest() throws IOException, Exception{	
	 	DbsnpModeTest.createVcf();
		final ConfidenceMode mode = new ConfidenceMode(patient);		
		mode.inputRecord(new File(DbsnpModeTest.inputName));

		String Scontrol = "EXTERN-MELA-20140505-001";
		String Stest = "EXTERN-MELA-20140505-002";
		mode.header.addOrReplace("##qControlSample=" + Scontrol);
		mode.header.addOrReplace("##qTestSample="+ Stest);	
		mode.header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqControlSample\tqTestSample");			
		
		SampleColumn column = SampleColumn.getSampleColumn(Stest, Scontrol, mode.header);
		mode.setSampleColumn(column.getTestSampleColumn(), column.getControlSampleColumn() );
		
		mode.addAnnotation();
		mode.reheader("unitTest", DbsnpModeTest.inputName);
		mode.writeVCF(new File(DbsnpModeTest.outputName)  );
		
		try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){				 				 
			for (final VcfRecord re : reader) {		
				final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo()); 				
				if(re.getPosition() == 2675826) 
					//compound SNPs
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.LOW.name())); 
				else if(re.getPosition() == 22012840)
					//isClassB
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.LOW.name())); 
//				else if(re.getPosition() == 14923588)
				else if(re.getPosition() == 14923588 || re.getPosition() == 2675825)
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.ZERO.name())); 
				else
					//"chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21"
					//TG alleles is 13 > 5 filter is PASS
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.HIGH.name())); 
			}
		 }		 	 	 
  			
	 }

//	public static void createVerifiedFile() throws IOException{
//        final List<String> data = new ArrayList<String>();
//        data.add("#version 3.0 all SNP and indel verification (not including APGI_1830)");
//        data.add("##dbSNP_BUILD_ID=135");  
//        data.add("PatientID\tSampleID\tChrPos\tInputType\tGeneID\tMutationClass\tbase_change\tAmpliconStart\tAmpliconEnd\tAmpliconSize\tSeqDirection\tPrimerTM\tPrimerBarcode\tPrimerPlateID\tPlatePos\tPlateIDTf\tPlateIDTr\tIonTorrentRunID\tTool\tConseq\tQCMGflag\tTD\tND\tNumMutReads\tpatient\tChr\tPos\tref\tND_A\tND_C\tND_G\tND_T\tND_N\tND_TOTAL\tlocation\tpatient\tChr\tPos\tref\tTD_A\tTD_C\tTD_G\tTD_T\tTD_N\tTD_TOTAL\tlocation\tPatientID\tChrPos\tbase_change\tref\tmutant\tref_ND\tmut_ND\tref_TD\tmut_TD\tfreq_ND\tfreq_TD\tverification"); 
//        data.add("APGI_1127\txxx\tchr10:70741311-70741311\tTD\tDDX21\tsomaticclassA\tC>T\txxx\txxx\t84\tF\t\"59.547,59.22\"\tACGCGAGTAT\txxx\txxx\txxx\txxx\txxx\tqSNP\tNON_SYNONYMOUS_CODING\t--\t\"C:14[38.53],30[30.86],T:0[0],6[40]\"\t\"C:26[39.58],25[36.13]\"\t6\tAPGI_1127\tchr10\t70741311\tC\tA:1\tC:1603\tG:0\tT:0\tN:0\t1604\tchr10:70741311-70741311\tAPGI_1127\tchr10\t70741311\tC\tA:0\tC:1264\tG:0\tT:0\tN:0\t1264\tchr10:70741311-70741311\tAPGI_1127\tchr10:70741311-70741311\tC>T\tC\tT\t1603\t0\t1264\t0\t0\t0\tno");
//        data.add(patient +"\txxx\tchrY:14923588-14923588\tTD\tUSP9Y\tsomaticclassA\tG>A\txxx\txxx\t98\tF\t\"61.078,59.793\"\tTACTCTCGTG\txxx\txxx\txxx\txxx\txxx\tqSNP\tNON_SYNONYMOUS_CODING\t--\t\"A:23[40],0[0],G:12[36.84],10[36.91]\"\t\"G:29[34.2],11[35.19]\"\t23\tAPGI_2001\tchrY\t14923588\tG\tA:0\tC:1\tG:1283\tT:0\tN:0\t1284\tchrY:14923588-14923588\tAPGI_2001\tchrY\t14923588\tG\tA:0\tC:0\tG:33\tT:0\tN:0\t33\tchrY:14923588-14923588\tAPGI_2001\tchrY:14923588-14923588\tG>A\tG\tA\t1283\t0\t33\t0\t0\t0\tyes");
//               
//        try(BufferedWriter out = new BufferedWriter(new FileWriter(VerifiedFileName));){      
//           for (final String line : data)  out.write(line + "\n");
//        }          
//	}
}
