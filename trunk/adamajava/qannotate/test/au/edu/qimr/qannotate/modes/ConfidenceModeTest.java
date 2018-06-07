package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.qcmg.common.util.Constants.VCF_MERGE_DELIM;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SampleColumn;

public class ConfidenceModeTest {
	
	 @AfterClass
	 public static void deleteIO(){
		 new File(DbsnpModeTest.inputName).delete();
		 new File(DbsnpModeTest.outputName).delete();		 
	 }
	 
	 @Test
	 public void testThresholds() {
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 9));
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{10,9}, 9));
		 assertEquals(false, ConfidenceMode.allValuesAboveThreshold(new int[]{10,9,8}, 9));
		 assertEquals(false, ConfidenceMode.allValuesAboveThreshold(new int[]{8,9,8}, 9));
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{8,9,8}, 8));
	 }
	 @Test
	 public void testThresholdsPercentage() {
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 9f));
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 10f));
		 assertEquals(false, ConfidenceMode.allValuesAboveThreshold(new int[]{10}, 100, 10.1f));
		 assertEquals(true, ConfidenceMode.allValuesAboveThreshold(new int[]{10,11,23}, 100, 10.0f));
		 assertEquals(false, ConfidenceMode.allValuesAboveThreshold(new int[]{10,11,23}, 100, 20.1f));
	 }
	 
	 @Test
	 public void endOfReads() {
		 /*
		  * chr2    31044381        .       C       A       .       .       FLANK=CACCCAACAAC;BaseQRankSum=-1.769;ClippingRankSum=0.078;DP=38;FS=0.000;MQ=59.43;MQRankSum=-0.078;QD=9.20;ReadPosRankSum=-0.204;SOR=0.315;IN=1,2;HOM=2,CCCCCCACCCaACAACAGTCC GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL   0/0:28,0:Reference:13:28:C2[]2[]:C4:PASS:.:.:.:C4[34.25]24[36.67]:.     0/1:28,12:Somatic:13:40:A1[]1[];C1[]2[]:A6;C4:PASS:.:SOMATIC:12:A1[32]11[33.55];C3[31.33]25[35.04]:.    ./.:.:.:3:.:.:.:PASS:.:NCIG:.:.:.       0/1:23,14:.:3:37:.:.:PASS:99:SOMATIC:.:.:349.77
		  */
		 VcfRecord r = new VcfRecord(new String[]{"chr2","31044381",".","C","A",".",".","FLANK=CACCCAACAAC;BaseQRankSum=-1.769;ClippingRankSum=0.078;DP=38;FS=0.000;MQ=59.43;MQRankSum=-0.078;QD=9.20;ReadPosRankSum=-0.204;SOR=0.315;IN=1,2;HOM=2,CCCCCCACCCaACAACAGTCC;"
				 ,"GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL"
				 ,"0/0:28,0:Reference:13:28:C2[]2[]:C4:.:.:.:.:C4[34.25]24[36.67]:."
				 ,"0/1:28,12:Somatic:13:40:A1[]1[];C1[]2[]:A6;C4:.:.:SOMATIC:12:A1[32]11[33.55];C3[31.33]25[35.04]:."
				 ,"./.:.:.:3:.:.:.:.:.:NCIG:.:.:."
				 ,"0/1:23,14:.:3:37:.:.:.:99:SOMATIC:.:.:349.77"});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 
		 assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("5BP=2", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
		 
	 }
	 
	 
	 @Test
	 public void newCoverageCutoffs() {
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 4985568, 4985568), "rs10753395","A", "C");
		 vcf.setInfo("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816");
		 vcf.setFormatFields(java.util.Arrays.asList(
				 "GT:AD:DP:FT:INF:MR:NNS", 
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5"));
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
		 
		 
		 
		 vcf.setFormatFields(java.util.Arrays.asList(
				 "GT:AD:DP:FT:INF:MR:NNS", 
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:2,5:7:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:2,5:7:.:.:5:5"));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("COV", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("COV", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
		 
		 vcf.setFormatFields(java.util.Arrays.asList(
				 "GT:AD:DP:FT:INF:MR:NNS", 
				 "0/1:2,5:7:.:.:5:5",
				 "0/1:2,5:7:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5",
				 "0/1:3,5:8:.:.:5:5"));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 
		 assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("COV", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void realLifeFail3() {
		 //GL000224.1	34563	.	C	T	.	.	FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=7,GCAATTCTTTtTTTAATGATC;EFF=upstream_gene_variant(MODIFIER||3648|||AL591856.4|miRNA|NON_CODING|ENST00000581903||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/0:.:Reference:13:240:PASS:.:.:.:.:C117[38.72]123[39.17]	0/1:.:Somatic:13:516:.:.:SOMATIC:64:60:C226[39.34]226[39.23];T33[40.76]31[40.16]	0/0:.:Reference:13:240:PASS:.:.:.:.:C117[38.72]123[39.17]	0/1:450,62:Somatic:13:512:.:99:SOMATIC:64:60:C226[39.34]226[39.23];T33[40.76]31[40.16]
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000224.1", 34563, 34563), ".","C", "T");
		 vcf.setInfo("FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=7,GCAATTCTTTtTTTAATGATC");
		 List<String> ff =  java.util.Arrays.asList(
				 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
				 "0/0:240:Reference:13:240:.:.:.:.:C117[38.72]123[39.17]	",
				 "0/1:452,64:Somatic:13:516:.:.:SOMATIC:60:C226[39.34]226[39.23];T33[40.76]31[40.16]",
				 "0/0:240:Reference:13:240:.:.:.:.:.",
				 "0/1:450,62:Somatic:13:512:.:99:SOMATIC:.:.");
		 vcf.setFormatFields(ff);
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("HOM", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("HOM", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
		 
		 /*
		  * set HOM to 5, and we should be all PASSES
		  */
		 vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000224.1", 34563, 34563), ".","C", "T");
		 vcf.setInfo("FLANK=TCTTTTTTTAA;BaseQRankSum=4.387;ClippingRankSum=-0.431;DP=512;FS=0.000;MQ=59.99;MQRankSum=-1.755;QD=2.17;ReadPosRankSum=1.855;SOR=0.676;IN=1,2;HOM=5,GCAATTCTTTtTTTAATGATC");
		 ff =  java.util.Arrays.asList(
				 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
				 "0/0:240:Reference:13:240:.:.:.:.:C117[38.72]123[39.17]	",
				 "0/1:452,64:Somatic:13:516:.:.:SOMATIC:60:C226[39.34]226[39.23];T33[40.76]31[40.16]",
				 "0/0:240:Reference:13:240:.:.:.:.:.",
				 "0/1:450,62:Somatic:13:512:.:99:SOMATIC:.:.");
		 vcf.setFormatFields(ff);
		 cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void realLifeFail4() {
		 //GL000247.1	152	.	A	G	.	.	FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:27:PASS:.:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:.:Germline:23:76:SBIASALT:.:.:29:25:A43[39.23]4[41];G28[39.39]1[41]	0/1:20,7:Germline:23:27:PASS:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:45,20:Germline:23:65:SBIASALT:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000247.1", 152, 152), ".","A", "G");
		 vcf.setInfo("FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG");
		 List<String> ff =  java.util.Arrays.asList(
				 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
				 "0/1:20,7:Germline:23:27:.:.:.:7:A20[38.1]0[0];G6[40.33]1[41]",
				 "0/1:47,29:Germline:23:76:.:.:.:25:A43[39.23]4[41];G28[39.39]1[41]",
				 "0/1:20,7:Germline:23:27:.:99:.:.:.",
				 "0/1:45,20:Germline:23:65:.:99:.:.:.");
//		 "0/1:20,7:Germline:23:27:.:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]",
//		 "0/1:45,20:Germline:23:65:.:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]");
		 vcf.setFormatFields(ff);
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 @Test
	 public void realLifeFail5() {
		 //GL000247.1	152	.	A	G	.	.	FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:27:PASS:.:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:.:Germline:23:76:SBIASALT:.:.:29:25:A43[39.23]4[41];G28[39.39]1[41]	0/1:20,7:Germline:23:27:PASS:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]	0/1:45,20:Germline:23:65:SBIASALT:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("GL000247.1", 152, 152), ".","A", "G");
		 vcf.setInfo("FLANK=TGTAAGTTGTT;BaseQRankSum=0.083;ClippingRankSum=0.360;DP=27;FS=5.863;MQ=49.73;MQRankSum=-3.458;QD=3.44;ReadPosRankSum=0.415;SOR=0.027;IN=1,2;HOM=0,GTGAGTGTAAgTTGTTTCCAG");
		 List<String> ff =  java.util.Arrays.asList(
				 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
				 "0/1:20,7:Germline:23:27:.:.:.:7:A20[38.1]0[0];G6[40.33]1[41]",
				 "0/1:47,29:Germline:23:76:.:.:.:25:A43[39.23]4[41];G28[39.39]1[41]",
				 "0/1:20,4:Germline:23:27:.:99:.:.:.",
				 "0/1:45,20:Germline:23:65:.:99:.:.:.");
//		 "0/1:20,7:Germline:23:27:.:99:.:7:7:A20[38.1]0[0];G6[40.33]1[41]",
//		 "0/1:45,20:Germline:23:65:.:99:.:29:25:A43[39.23]4[41];G28[39.39]1[41]");
		 vcf.setFormatFields(ff);
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("MR", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void realLifeFail() {
		 //chr1    4985568 rs10753395      A       C       .       PASS_1;PASS_2   FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816   GT:GD:AC:MR:NNS:AD:DP:GQ:PL     0/1:A/C:A8[33.75],11[38.82],C3[42],5[40],A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18,8:26:99:274,0,686      1/1:C/C:A1[37],0[0],C23[38.96],19[41.21],A1[37],0[0],C24[38.88],23[40.26]:42,47:38,42:1,44:45:94:1826,94,0
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 4985568, 4985568), "rs10753395","A", "C");
		 vcf.setFilter("PASS_1;PASS_2");
		 vcf.setInfo("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693;IN=1,2;DB;VAF=0.4816");
		 List<String> ff =  java.util.Arrays.asList("GT:GD:AC:MR:NNS:AD:DP:GQ:PL", "0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]:8:8:18:26:99:274,0,686","0/1:A/C:A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:8:26:99:274,0,686", "1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]:42:38:1,44:45:94:1826,94,0", "1/1:C/C:A1[37],0[0],C24[38.88],23[40.26]:47:42:1,44:45:94:1826,94,0");
		 vcf.setFormatFields(ff);
		 assertEquals(8, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(1), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
		 assertEquals(8, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(2), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
		 assertEquals(38, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(3), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
		 assertEquals(42, ConfidenceMode.getFieldOfInts(vcf.getSampleFormatRecord(4), VcfHeaderUtils.FORMAT_NOVEL_STARTS)[0]);
	 }
	 
	 @Test
	 public void realLifeFail2() {
		 //chr1	73551390	rs12142252	T	C	.	.	FLANK=ATCAACGGTCT;BaseQRankSum=-1.162;ClippingRankSum=1.214;DP=54;FS=0.000;MQ=60.00;MQRankSum=0.330;QD=17.29;ReadPosRankSum=0.590;SOR=0.596;IN=1,2;DB;VLD;VAF=0.3462;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:22:36:PASS:.:.:7:6:C3[41]4[38.75];T17[40.29]12[35.58]	0/0:.:LOH:22:63:PASS:.:.:.:.:C1[37]1[41];T31[39.03]30[38.87]	0/1:29,25:Germline:23:54:PASS:99:.:7:6:C3[41]4[38.75];T17[40.29]12[35.58]	0/1:61,8:Germline:23:69:.:99:.:2:2:C1[37]1[41];T31[39.03]30[38.87]
		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 73551390, 73551390), "rs12142252","T", "C");
		 vcf.setInfo("FLANK=ATCAACGGTCT;BaseQRankSum=-1.162;ClippingRankSum=1.214;DP=54;FS=0.000;MQ=60.00;MQRankSum=0.330;QD=17.29;ReadPosRankSum=0.590;SOR=0.596;IN=1,2;DB;VLD;VAF=0.3462;EFF=intergenic_region(MODIFIER||||||||||1)");
		 List<String> ff =  java.util.Arrays.asList(
				 "GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS",
				 "0/1:29,7:Germline:22:36:.:.:.:6:C3[41]4[38.75];T17[40.29]12[35.58]",
				 "0/0:61:LOH:22:63:.:.:.:.:C1[37]1[41];T31[39.03]30[38.87]",
				 "0/1:29,25:Germline:23:54:.:99:.:.:.",
				 "0/1:61,8:Germline:23:69:.:99:.:.:.");
		 vcf.setFormatFields(ff);
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));	// fails on MR and NNS
	 }
	 
	 @Test
	 public void passHomozygosLoss() {
		 /*
		  * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"GL000225.1","6859",".","T","G",".",".","FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)"
				 ,"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS"
				 ,"0/1:.:Germline:21:24:.:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]"
				 ,"./.:.:HomozygousLoss:21:14:.:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]"
				 ,"0/1:5,18:Germline:21:23:.:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]"
				 ,"./.:3,5:HomozygousLoss:21:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]"});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 @Test
	 public void noCallInGATK() {
		 /*
		  * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"chr1","696644",".","G","A",".",".","FLANK=AAACAAAAACT;BaseQRankSum=-0.677;ClippingRankSum=-0.710;DP=49;FS=0.000;MQ=25.07;MQRankSum=-0.215;QD=21.19;ReadPosRankSum=0.974;SOR=0.446;IN=1,2;HOM=5,AACTAAAACAaAAACTCCTGA"
		 ,"GT:AD:DP:FF:FT:GQ:INF:NNS:OABS:QL"
		 ,"0/0:39,0:39:G17:.:.:.:.:G16[40.25]23[37.22]:."
		 ,"0/1:5,43:48:.:.:.:SOMATIC:41:A19[39.95]24[37.83];G2[39]3[39.67]:."
		 ,"0/0:.:.:.:.:.:NCIG:.:.:."
		 ,"0/1:5,44:49:.:.:3:.:.:.:1038.53"});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void  areCoverageChecksWorking(){
		 /*
		  * GL000225.1	6859	.	T	G	.	.	FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:24:PASS:.:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:.:Germline:23:14:PASS:.:.:7:7:G4[34.25]3[39];T4[38.25]3[33]	0/1:5,18:Germline:23:23:PASS:99:.:16:15:G5[38]11[38.09];T4[34.25]4[35.5]	0/1:3,5:Germline:23:8:.:99:.:7:7:G4[34.25]3[39];T4[38.25]3[33]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"GL000225.1","6859",".","T","G",".",".","FLANK=CCCTTGAAGCA;BaseQRankSum=1.006;ClippingRankSum=-0.335;DP=23;FS=4.649;MQ=27.53;MQRankSum=-0.186;QD=29.47;ReadPosRankSum=1.304;SOR=1.389;IN=1,2;EFF=intergenic_region(MODIFIER||||||||||1)"
				 ,"GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/1:8,16:Germline:23:24:.:.:.:15:G5[38]11[38.09];T4[34.25]4[35.5]"
				 ,"0/1:7,7:Germline:23:14:.:.:.:7:G4[34.25]3[39];T4[38.25]3[33]"
				 ,"0/1:5,18:Germline:23:23:.:99:.:.:."
				 ,"0/1:3,5:Germline:23:8:.:99:.:.:."});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void areCoverageChecksWorking2() {
		 /*
		  * GL000231.1	21863	.	C	T	.	.	FLANK=AATCCTTTCAT;BaseQRankSum=-0.085;ClippingRankSum=-0.751;DP=90;FS=0.832;MQ=50.50;MQRankSum=-3.000;QD=11.64;ReadPosRankSum=0.564;SOR=0.554;IN=1,2;EFF=downstream_gene_variant(MODIFIER||15|||CT867977.1|miRNA|NON_CODING|ENST00000581649||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:56:PASS:.:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:.:Germline:23:38:.:.:.:4:3:C9[37.78]25[36.6];T3[40]1[41]	0/1:50,40:Germline:23:90:PASS:99:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:40,27:Germline:23:67:.:99:.:4:3:C9[37.78]25[36.6];T3[40]1[41]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"GL000231.1","21863",".","C","T",".",".","."
				 ,"GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/1:45,11:Germline:23:56:.:.:.:9:C16[37.38]29[37.34];T8[34.5]3[39.33]"
				 ,"0/1:34,4:Germline:23:38:.:.:.:3:C9[37.78]25[36.6];T3[40]1[41]"
				 ,"0/1:50,40:Germline:23:90:.:99:.:.:."
				 ,"0/1:40,27:Germline:23:67:.:99:.:.:."});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("NNS;MR", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void ifGermlineJustLookAtControl() {
		 /*
		  * GL000231.1	21863	.	C	T	.	.	FLANK=AATCCTTTCAT;BaseQRankSum=-0.085;ClippingRankSum=-0.751;DP=90;FS=0.832;MQ=50.50;MQRankSum=-3.000;QD=11.64;ReadPosRankSum=0.564;SOR=0.554;IN=1,2;EFF=downstream_gene_variant(MODIFIER||15|||CT867977.1|miRNA|NON_CODING|ENST00000581649||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS	0/1:.:Germline:23:56:PASS:.:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:.:Germline:23:38:.:.:.:4:3:C9[37.78]25[36.6];T3[40]1[41]	0/1:50,40:Germline:23:90:PASS:99:.:11:9:C16[37.38]29[37.34];T8[34.5]3[39.33]	0/1:40,27:Germline:23:67:.:99:.:4:3:C9[37.78]25[36.6];T3[40]1[41]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"GL000231.1","21863",".","C","T",".",".","."
				 ,"GT:AD:CCC:CCM:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/1:45,11:Germline:23:56:.:.:.:9:C16[37.38]29[37.34];T8[34.5]3[39.33]"
				 ,"0/1:34,4:Germline:23:38:.:.:.:3:C9[37.78]25[36.6];T3[40]1[41]"
				 ,"0/1:50,40:Germline:23:90:.:99:.:.:."
				 ,"0/1:40,27:Germline:23:67:.:99:.:.:."});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 assertEquals("PASS", r.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("NNS;MR", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confIsSomatic() {
		 /*
		  * chr1    2245570 rs2843152       C       G       .       .       FLANK=GATGCGAGGAG;DP=4;FS=0.000;MQ=60.00;MQ0=0;QD=17.76;SOR=0.693;IN=1,2;DB;VLD;VAF=0.6276;EFF=downstream_gene_variant(MODIFIER||4012||728|SKI|protein_coding|CODING|ENST00000378536||1),intergenic_region(MODIFIER||||||||||1) GT:AD:DP:FT:GQ:INF:MR:NNS:OABS  .:.:.:.:.:.:.:.:.       1/1:.:4:5BP=1;COVT:.:SOMATIC;GERM=53,185;CONF=SOMATIC;GERM=53,185;ZERO:4:4:G2[35]2[35]  .:.:.:.:.:.:.:.:.       1/1:0,4:4:5BP=1;COVT:12:SOMATIC;GERM=53,185;CONF=SOMATIC;GERM=53,185;ZERO:4:4:G2[35]2[35]
		  */
		 VcfRecord r = new VcfRecord(new String[]{"chr1","2245570","rs2843152","C","G",".",".","FLANK=GATGCGAGGAG;DP=4;FS=0.000;MQ=60.00;MQ0=0;QD=17.76;SOR=0.693;IN=1,2;DB;VLD;VAF=0.6276;EFF=downstream_gene_variant(MODIFIER||4012||728|SKI|protein_coding|CODING|ENST00000378536||1),intergenic_region(MODIFIER||||||||||1)"
				 ,"GT:AD:DP:FT:GQ:INF:MR:NNS:OABS"
				 ,".:.:.:.:.:.:.:.:."
				 ,"1/1:.:4:5BP=1;COVT:.:SOMATIC;GERM=53,185:4:4:G2[35]2[35]"
				 ,".:.:.:.:.:.:.:.:."
				 ,"1/1:0,4:4:5BP=1;COVT:12:SOMATIC;GERM=53,185:4:4:G2[35]2[35]"});
		 
		 assertEquals(true, VcfUtils.isRecordSomatic(r));
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(r.getChrPosition(), java.util.Arrays.asList(r));
		 cm.addAnnotation();
		 r = cm.positionRecordMap.get(r.getChrPosition()).get(0);
		 assertEquals(true, r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_INFO).contains( "SOMATIC"));
		 assertEquals(true, r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_INFO).contains( "GERM=53,185"));
		 assertEquals("5BP=1;COVT", r.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("5BP=1;COVT", r.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 
//	 @Test
//	 public void homoplymerTest() {
//		 //chr1	152281007	.	AA	GG	.	PASS_1;PASS_2	IN=1,2;CONF=ZERO_1,ZERO_2;EFF=missense_variant(MODERATE||cattat/caCCat|p.HisTyr2118HisHis/c.6354TT>CC|4061|FLG|protein_coding|CODING|ENST00000368799||1),sequence_feature[compositionally_biased_region:Ser-rich](LOW|||c.6354AA>GG|4061|FLG|protein_coding|CODING|ENST00000368799|3|1),upstream_gene_variant(MODIFIER||4928|||FLG-AS1|antisense|NON_CODING|ENST00000392688||1),intron_variant(MODIFIER|||n.463-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000420707|4|1),intron_variant(MODIFIER|||n.377-6375AA>GG||FLG-AS1|antisense|NON_CODING|ENST00000593011|2|1)	ACCS	AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1	AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1
//		 VcfRecord vcf = VcfUtils.createVcfRecord(ChrPositionUtils.getChrPosition("chr1", 152281007, 152281007), "rs10753395","AA", "GG");
//		 vcf.setFilter("PASS_1;PASS_2");
//		 vcf.setInfo("IN=1,2;HOM=5,ATGCAggAATGC");
//		 List<String> ff =  java.util.Arrays.asList("ACCS", "AA,12,16,GG,4,6,_A,0,1&AA,13,17,GG,7,8,_A,0,1", "AA,33,37,GG,10,8,CA,0,1&AA,39,40,GC,1,0,GG,21,13,G_,1,0,TG,1,0,CA,0,1");
//		 vcf.setFormatFields(ff);
//		 
//		 ConfidenceMode cm =new ConfidenceMode(2,1);
//		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
////		 cm.setSampleColumn(2,1);
//		 cm.addAnnotation();
//		 		 
// 		 String conf = vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENT);
//		 assertEquals("HIGH_1,HIGH_2", conf);
//		 		 
//		 vcf.setInfo("IN=1,2;HOM=6,ATGAAggAATGC");
//		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
//		 cm.addAnnotation();		 		 
//		 conf = vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENT);
//		 assertEquals("LOW_1,LOW_2", conf);
//	 }
//	 
	 @Test
	 public void willMultipleACValuesWork() {
		 VcfFormatFieldRecord format = new VcfFormatFieldRecord("GT:GD:AC:MR:NNS:AD:DP:GQ:PL", "0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]"+VCF_MERGE_DELIM+"A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:18"+VCF_MERGE_DELIM+"8:26:99:274,0,686");
		 assertEquals(19, VcfUtils.getAltFrequency(format, "A"));
	 }
	 @Test
	 public void confidenceRealLifeSingle() {
		 //chr8	12306635	rs28428895	C	T	.	PASS	FLANK=ACACATACATA;DB;CONF=HIGH;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)	GT:GD:AC:MR:NNS	0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8	0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".",".","FLANK=ACACATACATA;DB;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:AC:AD:DP:NNS:FT","0/1:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:13,8:22:8:.","0/0:C19[36.11],20[38.45],T1[42],0[0]:39,1:40:1:."});
		 VcfRecord vcf2 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T","57.77",".","SOMATIC;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:AD:DP:GQ:PL:GD:AC:MR:NNS:FT",".:.:.:.:.:C/C:C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]:15:15:MIN","0/1:4,3:7:86:86,0,133:C/T:C22[36.23],22[36.91],T2[26.5],1[42]:3:2:MR;NNS"});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf1.getChrPosition(), java.util.Arrays.asList(vcf1, vcf2));
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
		 assertEquals("PASS", vcf1.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf1.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("MIN", vcf2.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("MR;NNS", vcf2.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged() {
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".",".","FLANK=ACACATACATA;IN=1,2;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)",
				 "GT:OABS:NNS:AD:DP:GQ:PL:FT",
				 "0/1:C10[39]3[30]G1[11]0[0];T7[41.29]1[42]:8:13,8:22:.:.:.",
				 "0/1:C14[38.79]3[30];G1[11]0[0];T11[39.27]4[25.25]:15:17,15:33:.:.:.",
				 "0/0:.:.:.:40:.:.:.",
				 "0/1:.:.:4,3:47:86:86,0,133:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.addAnnotation();
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("MR", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLife2() {
		 //chr9	126129715	rs57014689	C	A	205.77	PASS	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB	GT:AD:DP:GQ:PL:GD:AC:MR:NNS	0/1:6,5:11:99:234,0,331:A/C:A0[0],4[24.25],C243[17.06],65[18.88],G2[7],0[0]:4:4	1/1:1,18:19:46:841,46,0:A/A:A2[7],15[28.73],C179[15.92],121[14.76],G0[0],1[7]:17:16
		 //chr9	126129715	rs57014689	C	A	.	PASS	SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185	GT:GD:AC:MR:NNS	0/0:C/C:A0[0],4[24.25],C128[17.64],30[20.9],G2[7],0[0]:4:4	0/1:A/C:A2[7],13[28.23],C96[17.22],54[14.22]:15:15
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A","205.77",".","AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;DB",
				 "GT:AD:DP:GQ:PL:OABS:NNS:FT",
				 "0/1:6,5:11:99:234,0,331:A0[0]4[24.25];C243[17.06]65[18.88];G2[7]0[0]:.:.",
				 "1/1:1,18:19:46:841,46,0:A2[7]15[28.73];C179[15.92]121[14.76];G0[0]1[7]:.:."});
		 VcfRecord vcf2 = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A",".",".","SOMATIC;FLANK=CCCCCACACCC;DB;GERM=5,185",
				 "GT:AD:DP:OABS:NNS:FT",
				 "0/0:158,4:164:A0[0]4[24.25];C128[17.64]30[20.9];G2[7]0[0]:4:.",
				 "0/1:150,15:165:A2[7]13[28.23];C96[17.22]54[14.22]:15:."});
		 
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf1.getChrPosition(), Arrays.asList(vcf1, vcf2));
		 cm.addAnnotation();
		 
		 vcf1 = cm.positionRecordMap.get(vcf1.getChrPosition()).get(0);
		 vcf2 = cm.positionRecordMap.get(vcf2.getChrPosition()).get(1);
		 assertEquals("PASS", vcf1.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf1.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf2.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf2.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged2() throws Exception {
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr9","126129715","rs57014689","C","A",".",".","FLANK=CCCCCACACCC;AC=1;AF=0.500;AN=2;BaseQRankSum=-1.408;ClippingRankSum=-1.932;DP=48;FS=3.424;MLEAC=1;MLEAF=0.500;MQ=41.89;MQ0=0;MQRankSum=0.717;QD=4.29;ReadPosRankSum=-0.717;SOR=0.120;IN=1,2;DB;GERM=5,185",
				 "GT:OABS:NNS:AD:DP:GQ:PL:FT:INF",
				 "0/0:A0[0]4[24.25];C128[17.64]30[20.9];G2[7]0[0]:4:.:164:.:.:.:.",
				 "0/1:A0[0]4[24.25];C243[17.06]65[18.88];G2[7]0[0]:4:6,5:300:99:234,0,331:.:SOMATIC",
				 "0/0:.:.:.:.:.:.:.:NCIG",
				 "1/1:.:16:1,18:319:46:841,46,0:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged3() {
		 /*
		  * chr17	76354679       	.      	G      	A      	.      	.      	FLANK=TAGATATAATA;BaseQRankSum=-0.735;ClippingRankSum=-0.385;DP=23;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.175;QD=20.34;ReadPosRankSum=-0.245;SOR=1.061;IN=1,2     	GT:DP:FT:INF:MR:NNS:OABS:AD:GQ 	0/0:36:.:SOMATIC:.:.:A0[0]1[34];G19[34.79]16[35.25]:.:.	0/1:22:.:SOMATIC:16:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:.:. 	.:.:.:.:.:.:A0[0]1[34];G19[34.79]16[35.25]:.:. 	0/1:23:.:.:16:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:6,17:99
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr17","76354679",".","G","A",".",".","FLANK=TAGATATAATA;BaseQRankSum=-0.735;ClippingRankSum=-0.385;DP=23;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.175;QD=20.34;ReadPosRankSum=-0.245;SOR=1.061;IN=1,2"
				 ,"GT:DP:FT:INF:NNS:OABS:AD:GQ"
				 ,"0/0:36:.:.:.:A0[0]1[34];G19[34.79]16[35.25]:36:."
				 ,"0/1:22:.:SOMATIC:16:A9[33.11]7[33.86];G3[34.33]3[35.67]:6,16:."
				 ,"0/0:.:.:NCIG:.:.:.:."
				 ,"0/1:23:.:SOMATIC:.:.:6,17:99"});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged4() {
		 /*
		  * chr1    	1654058	rs61777495     	C      	T      	.      	.      	FLANK=CTTCATCGAAG;DP=17;FS=0.000;MQ=46.03;MQ0=0;QD=28.60;SOR=4.294;IN=1,2;DB;VLD;VAF=0.6313	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	1/1:.:13:.:.:CONF=ZERO:13:11:T10[39.7]3[32.67] 	1/1:.:10:SBIASALT:.:.:9:8:C0[0]1[28];T9[39.67]0[0]     	1/1:0,17:17:.:57:CONF=ZERO:13:11:T10[39.7]3[32.67]     	0/1:1,10:11:SBIASALT:46:.:9:8:C0[0]1[28];T9[39.67]0[0]
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr1","1654058","rs61777495","C","T",".",".","FLANK=CTTCATCGAAG;DP=17;FS=0.000;MQ=46.03;MQ0=0;QD=28.60;SOR=4.294;IN=1,2;DB;VLD;VAF=0.6313"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"1/1:0,13:13:.:.:.:11:T10[39.7]3[32.67]"
				 ,"1/1:1,9:10:.:.:.:8:C0[0]1[28];T9[39.67]0[0]"
				 ,"1/1:0,17:17:.:57:.:.:."
				 ,"0/1:1,10:11:.:46:.:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("SBIASALT", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged5() {
		 /*
		  * chr1   	22332008       	rs56968853     	T      	C      	.      	.      	FLANK=CCCGACTGGGT;BaseQRankSum=-2.031;ClippingRankSum=-0.750;DP=39;FS=11.226;MQ=56.26;MQ0=0;MQRankSum=-4.117;QD=1.40;ReadPosRankSum=-0.457;SOR=1.429;IN=1,2;DB;EFF=synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|121|CELA3A|protein_coding|CODING|ENST00000374663|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|5|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|2|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|7|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|8|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|6|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|4|1),sequence_feature[disulfide_bond](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),upstream_gene_variant(MODIFIER||1935||75|CELA3A|protein_coding|CODING|ENST00000400271||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1647|||RN7SL768P|misc_RNA|NON_CODING|ENST00000584415||1)     	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/1:.:64:.:.:CONF=HIGH:7:7:C2[34]5[34.8];T41[33.2]16[35.19]    	0/1:.:39:.:.:.:7:7:C2[35]5[34];T23[33.39]9[35.44]      	0/0:.:57:MIN:.:SOMATIC;GERM=49,185:.:.:C2[34]5[34.8];T41[33.2]16[35.19]	0/1:32,7:39:.:83:SOMATIC;GERM=49,185;CONF=HIGH:7:7:C2[35]5[34];T23[33.39]9[35.44]
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr1","22332008","rs56968853","T","C",".",".","FLANK=CCCGACTGGGT;BaseQRankSum=-2.031;ClippingRankSum=-0.750;DP=39;FS=11.226;MQ=56.26;MQ0=0;MQRankSum=-4.117;QD=1.40;ReadPosRankSum=-0.457;SOR=1.429;IN=1,2;DB;EFF=synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),synonymous_variant(LOW|SILENT|gaT/gaC|p.Asp66Asp/c.198T>C|121|CELA3A|protein_coding|CODING|ENST00000374663|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|5|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|2|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|7|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|8|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|6|1),sequence_feature[domain:Peptidase_S1](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|4|1),sequence_feature[disulfide_bond](LOW|||c.198T>C|270|CELA3A|protein_coding|CODING|ENST00000290122|3|1),upstream_gene_variant(MODIFIER||1935||75|CELA3A|protein_coding|CODING|ENST00000400271||1|WARNING_TRANSCRIPT_NO_START_CODON),upstream_gene_variant(MODIFIER||1647|||RN7SL768P|misc_RNA|NON_CODING|ENST00000584415||1)"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/1:57,7:64:.:.:.:7:C2[34]5[34.8];T41[33.2]16[35.19]"
				 ,"0/1:32,7:39:.:.:.:7:C2[35]5[34];T23[33.39]9[35.44]"
				 ,"0/0:.:.:.:.:NCIG:.:."
				 ,"0/1:32,7:39:.:83:SOMATIC;GERM=49,185:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged6() {
		 /*
		  * COVN12 filter in normal needs to affect the somatic confidence 
		  * 
		  * chr1   	176992676      	rs10798496     	C      	T      	.      	.      	FLANK=TCCAGTTGGCT;BaseQRankSum=0.851;ClippingRankSum=0.676;DP=10;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.336;QD=14.78;ReadPosRankSum=-0.336;SOR=0.250;IN=1,2;DB;VLD;VAF=0.2466	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/0:.:8:COVN12:.:SOMATIC;GERM=117,185:.:.:C0[0]8[39.62]	0/1:.:10:.:.:SOMATIC;GERM=117,185;CONF=HIGH:5:5:C1[32]4[35.5];T2[34]3[38]      	0/0:.:8:COVN12:.:SOMATIC;GERM=117,185:.:.:C0[0]8[39.62]	0/1:5,5:10:.:99:SOMATIC;GERM=117,185;CONF=HIGH:5:5:C1[32]4[35.5];T2[34]3[38]
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr1","176992676","rs10798496","C","T",".",".","FLANK=TCCAGTTGGCT"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/0:8:8:.:.:GERM=117,185:.:C0[0]8[39.62"
				 ,"0/1:5,5:10:.:.:SOMATIC;GERM=117,185:5:C1[32]4[35.5];T2[34]3[38]"
				 ,"0/0:.:.:.:.:NCIG:.:."
				 ,"0/1:5,5:10:.:99:SOMATIC;GERM=117,185:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
//		 ConfidenceMode cm =new ConfidenceMode(new short[] {1,3}, new short[]{2,4});
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged7() {
		 /*
		  * SAN3 filter in normal needs to affect the somatic confidence 
		  * chr5   	101570491      	rs4703217      	A      	G      	.      	.      	FLANK=GACAAGGAAAG;BaseQRankSum=1.262;ClippingRankSum=1.926;DP=19;FS=0.000;MQ=50.85;MQ0=0;MQRankSum=-2.192;QD=29.86;ReadPosRankSum=-0.332;SOR=1.061;IN=1,2;DB;VLD;VAF=0.1832;EFF=synonymous_variant(LOW|SILENT|aaA/aaG|p.Lys15Lys/c.45A>G|90|AC008948.1|protein_coding|CODING|ENST00000597120|1|1),3_prime_UTR_variant(MODIFIER||2071|c.*2071T>C|724|SLCO4C1|protein_coding|CODING|ENST00000310954|13|1)	GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	.:.:.:.:.:SOMATIC;GERM=11,185:.:.:.    	1/1:.:19:.:.:SOMATIC;GERM=11,185;CONF=HIGH:17:17:A1[39]1[39];G7[37.43]10[38.4] 	.:.:.:SAN3:.:SOMATIC;GERM=11,185:.:.:. 	1/1:2,17:19:.:8:SOMATIC;GERM=11,185;CONF=HIGH:17:17:A1[39]1[39];G7[37.43]10[38.4]
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr5","101570491","rs4703217","A","G",".",".","FLANK=GACAAGGAAAG;BaseQRankSum=1.262;ClippingRankSum=1.926;DP=19;FS=0.000;MQ=50.85;MQ0=0;MQRankSum=-2.192;QD=29.86;ReadPosRankSum=-0.332;SOR=1.061;IN=1,2;DB;VLD;VAF=0.1832;EFF=synonymous_variant(LOW|SILENT|aaA/aaG|p.Lys15Lys/c.45A>G|90|AC008948.1|protein_coding|CODING|ENST00000597120|1|1),3_prime_UTR_variant(MODIFIER||2071|c.*2071T>C|724|SLCO4C1|protein_coding|CODING|ENST00000310954|13|1)"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"./.:.:0:.:.:SOMATIC;GERM=11,185:.:."
				 ,"1/1:2,17:19:.:.:SOMATIC;GERM=11,185:17:A1[39]1[39];G7[37.43]10[38.4]"
				 ,"0/0:.:.:.:.:NCIG:.:."
				 ,"1/1:2,17:19:.:8:SOMATIC;GERM=11,185:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged8() {
		 /*
		  * MIUN filter in normal needs to affect the somatic confidence 
		  * chr11  	1016978	rs76461263     	T      	G      	.      	.      	FLANK=GTGTGGTTGGG		GT:AD:DP:FT:GQ:INF:MR:NNS:OABS 	0/0:.:63:MIUN:.:SOMATIC;GERM=40,185:.:.:G1[40]0[0];T35[33.11]27[35.33] 	0/1:.:69:.:.:SOMATIC;GERM=40,185;CONF=HIGH:7:5:G1[35]6[39.67];T32[32.12]30[36] 	0/0:.:62:.:.:SOMATIC;GERM=40,185:.:.:G1[40]0[0];T35[33.11]27[35.33]    	0/1:58,15:73:.:99:SOMATIC;GERM=40,185;CONF=HIGH:7:5:G1[35]6[39.67];T32[32.12]30[36]
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr11","1016978","rs76461263","T","G",".",".","FLANK=GTGTGGTTGGG"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/0:62,1:63:.:.:.;GERM=40,185:.:G1[40]0[0];T35[33.11]27[35.33]"
				 ,"0/1:62,7:69:.:.:SOMATIC;GERM=40,185:5:G1[35]6[39.67];T32[32.12]30[36]"
				 ,"0/0:.:.:.:.:NCIG:.:."
				 ,"0/1:58,15:73:.:99:SOMATIC;GERM=40,185:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("PASS", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
	 }
	 
	 @Test
	 public void confidenceRealLifeMerged9() {
		 /*
		  * COVN8 filter in normal needs to affect the somatic confidence 
		  * chr17  	42254527       	rs7217858      	T      	G      	.      	.      	FLANK=AGGACGCCCCC
		  */
		 //now try the merged record
		 VcfRecord vcf = new VcfRecord(new String[]{"chr17","42254527","rs7217858","T","G",".",".","FLANK=AGGACGCCCCC"
				 ,"GT:AD:DP:FT:GQ:INF:NNS:OABS"
				 ,"0/0:3:3:.:.:SOMATIC;GERM=30,185:.:T2[35]1[35]"
				 ,"1/1:1,11:12:.:.:SOMATIC;GERM=30,185:10:G5[33.6]6[35.67];T0[0]1[38]"
				 ,"0/0:.:.:.:.:NCIG:.:."
				 ,"0/1:1,11:12:.:7:SOMATIC;GERM=30,185:.:."});
		 ConfidenceMode cm =new ConfidenceMode();
		 cm.positionRecordMap.put(vcf.getChrPosition(), java.util.Arrays.asList(vcf));
		 cm.addAnnotation();
		 
		 vcf = cm.positionRecordMap.get(vcf.getChrPosition()).get(0);
		 assertEquals("COV", vcf.getSampleFormatRecord(1).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(2).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(3).getField(VcfHeaderUtils.FORMAT_FILTER));
		 assertEquals("PASS", vcf.getSampleFormatRecord(4).getField(VcfHeaderUtils.FORMAT_FILTER));
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
	
	 @Ignore
	 public void confidenceTest() throws IOException, Exception{	
	 	DbsnpModeTest.createVcf();
		final ConfidenceMode mode = new ConfidenceMode();		
		mode.loadVcfRecordsFromFile(new File(DbsnpModeTest.inputName));

		String Scontrol = "EXTERN-MELA-20140505-001";
		String Stest = "EXTERN-MELA-20140505-002";
		mode.header.addOrReplace("##qControlSample=" + Scontrol);
		mode.header.addOrReplace("##qTestSample="+ Stest);	
		mode.header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tEXTERN-MELA-20140505-001\tEXTERN-MELA-20140505-002");			
		
		mode.addAnnotation();
		mode.reheader("unitTest", DbsnpModeTest.inputName);
		mode.writeVCF(new File(DbsnpModeTest.outputName)  );
		
		try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){			 
			for (final VcfRecord re : reader) {
				String ff = re.getFormatFieldStrings();
				if(re.getPosition() == 2675826) {
					//compound SNPs
					assertEquals(true, ff.contains(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.LOW.name())); 
				} else if(re.getPosition() == 22012840) {
					//isClassB
				assertEquals(true, ff.contains(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.LOW.name())); 
//				else if(re.getPosition() == 14923588)
				} else if(re.getPosition() == 14923588 || re.getPosition() == 2675825) {
					assertEquals(true, ff.contains(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.ZERO.name())); 
				} else {
					//"chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21"
					//TG alleles is 13 > 5 filter is PASS
					assertEquals(true, ff.contains(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ + MafConfidence.HIGH.name()));
				}
			}
		 }		 	 	 
	 }
	 
	 
	 @Ignore
	 public void sampleColumnNoIDTest() throws IOException, Exception{	
	 	DbsnpModeTest.createVcf();
		final ConfidenceMode mode = new ConfidenceMode();		
		mode.loadVcfRecordsFromFile(new File(DbsnpModeTest.inputName));

		String Scontrol = "EXTERN-MELA-20140505-001";
		String Stest = "EXTERN-MELA-20140505-002";
		mode.header.addOrReplace("##qControlSample=" + Scontrol);
		mode.header.addOrReplace("##qTestSample="+ Stest);	
		mode.header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqControlSample\tqTestSample");			
		
		SampleColumn column = SampleColumn.getSampleColumn(Stest, Scontrol, mode.header);
		
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
				else if(re.getPosition() == 14923588 || re.getPosition() == 2675825)
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.ZERO.name())); 
				else
					//"chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21"
					//TG alleles is 13 > 5 filter is PASS
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.HIGH.name())); 
			}
		 }		 	 	 
	 }
}
