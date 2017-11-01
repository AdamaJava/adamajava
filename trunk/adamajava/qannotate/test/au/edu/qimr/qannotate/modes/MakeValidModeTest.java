package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;

public class MakeValidModeTest {
	
	 
	 
	 @Test
	 public void makeValidFirstCallerOnly() {
		 //chr1    10051   .       A       C       .       MIUN    SOMATIC;FLANK=CCCTACCCCTA;IN=1;HOM=3,CCTAACCCTAcCCCTAACCCT;CONF=LOW;EFF=upstream_gene_variant(MODIFIER||1818|||DDX11L1|processed_transcript|NON_CODING|ENST00000456328||1),upstream_gene_variant(MODIFIER||1821|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000515242||1),upstream_gene_variant(MODIFIER||1823|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000518655||1),upstream_gene_variant(MODIFIER||1959|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000450305||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000423562||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000438504||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000541675||1),downstream_gene_variant(MODIFIER||4353|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000488147||1),downstream_gene_variant(MODIFIER||4360|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000538476||1),intergenic_region(MODIFIER||||||||||1)      GT:GD:AC:DP:OABS:MR:NNS 0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2  0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","10051",".","A","C",".","MIUN","SOMATIC;FLANK=CCCTACCCCTA;IN=1;HOM=3,CCTAACCCTAcCCCTAACCCT;CONF=LOW","GT:GD:AC:DP:OABS:MR:NNS","0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2","0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS", ffList.get(0));
		 assertEquals("0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2", ffList.get(1));
		 assertEquals("0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS", ffListMV.get(0));
		 assertEquals("0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2", ffListMV.get(1));
		 assertEquals("0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7", ffListMV.get(2));
		 assertEquals(".:.:.:.:.:.:.", ffListMV.get(3));
		 assertEquals(".:.:.:.:.:.:.", ffListMV.get(4));
	 }
	 
	 @Test
	 public void makeValidSecondCallerOnly() {
		 //chr1    10051   .       A       C       .       MIUN    SOMATIC;FLANK=CCCTACCCCTA;IN=1;HOM=3,CCTAACCCTAcCCCTAACCCT;CONF=LOW;EFF=upstream_gene_variant(MODIFIER||1818|||DDX11L1|processed_transcript|NON_CODING|ENST00000456328||1),upstream_gene_variant(MODIFIER||1821|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000515242||1),upstream_gene_variant(MODIFIER||1823|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000518655||1),upstream_gene_variant(MODIFIER||1959|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000450305||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000423562||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000438504||1),downstream_gene_variant(MODIFIER||4312|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000541675||1),downstream_gene_variant(MODIFIER||4353|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000488147||1),downstream_gene_variant(MODIFIER||4360|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000538476||1),intergenic_region(MODIFIER||||||||||1)      GT:GD:AC:DP:OABS:MR:NNS 0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2  0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","10051",".","A","C",".","MIUN","SOMATIC;FLANK=CCCTACCCCTA;IN=2;HOM=3,CCTAACCCTAcCCCTAACCCT;CONF=LOW","GT:GD:AC:DP:OABS:MR:NNS","0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2","0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS", ffList.get(0));
		 assertEquals("0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2", ffList.get(1));
		 assertEquals("0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS", ffListMV.get(0));
		 assertEquals(".:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals(".:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals("0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2", ffListMV.get(3));
		 assertEquals("0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7", ffListMV.get(4));
	 }
	 
	 @Test
	 public void makeValidBothCalers() {
		 //chr1    553742  rs140182652     G       A       .       COVT_1;NCIT_2   FLANK=CTGTCACCCAG;AC=1;AF=0.500;AN=2;BaseQRankSum=1.743;ClippingRankSum=1.460;DP=38;FS=3.132;MLEAC=1;MLEAF=0.500;MQ=40.30;MQ0=0;MQRankSum=-2.308;QD=8.55;ReadPosRankSum=0.204;SOR=1.445;IN=1,2;DB;HOM=3,TCACTCTGTCaCCCAGGCTGG;CONF=ZERO_1,ZERO_2     GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795        0/0&.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:.
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","553742","rs140182652","G","A",".","COVT_1;NCIT_2","FLANK=CTGTCACCCAG;AC=1;AF=0.500;AN=2;BaseQRankSum=1.743;ClippingRankSum=1.460;DP=38;FS=3.132;MLEAC=1;MLEAF=0.500;MQ=40.30;MQ0=0;MQRankSum=-2.308;QD=8.55;ReadPosRankSum=0.204;SOR=1.445;IN=1,2;DB;HOM=3,TCACTCTGTCaCCCAGGCTGG;CONF=ZERO_1,ZERO_2","GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL","0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795","0/0&.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:."});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		 assertEquals("0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795", ffList.get(1));
		 assertEquals("0/0&.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:.", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ", ffListMV.get(0));
		 assertEquals("0/1:A/G:A5[35.2],9[38.11],G11[36],13[37.54]:38:A5[35.2]9[38.11];G11[36]13[37.54]:14:13:26,12:99", ffListMV.get(1));
		 assertEquals("0/0:G/G:G4[40],3[38]:7:G4[40]3[38]:0:0:.:.", ffListMV.get(2));
		 assertEquals("0/1:A/G:A5[35.2],9[38.11],G11[36],13[37.54]:38:.:14:13:26,12:99", ffListMV.get(3));
		 assertEquals(".:.:G4[40],3[38]:.:.:0:0:.:.", ffListMV.get(4));
	 }
	 
	 @Test
	 public void makeValidInBothCompoundSnp() {
		 //chr1	14221527	rs386628664	TG	CA	.	PASS_1;PASS_2	IN=1,2;DB;HOM=2,TGTAAAACGGcaCTACTAGGCA;CONF=HIGH_1,HIGH_2;EFF=intergenic_region(MODIFIER||||||||||1)	ACCS	CA,13,10,CG,8,9,C_,2,0&CA,13,10,CG,8,9,C_,2,0	CA,3,3,CG,23,21,_G,1,1&CA,3,3,CG,23,21,_G,1,1
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","14221527","rs386628664","TG","CA",".","PASS_1;PASS_2","IN=1,2;DB;HOM=2,TGTAAAACGGcaCTACTAGGCA;CONF=HIGH_1,HIGH_2;EFF=intergenic_region(MODIFIER||||||||||1)","ACCS","CA,13,10,CG,8,9,C_,2,0&CA,13,10,CG,8,9,C_,2,0","CA,3,3,CG,23,21,_G,1,1&CA,3,3,CG,23,21,_G,1,1"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("ACCS", ffList.get(0));
		 assertEquals("CA,13,10,CG,8,9,C_,2,0&CA,13,10,CG,8,9,C_,2,0", ffList.get(1));
		 assertEquals("CA,3,3,CG,23,21,_G,1,1&CA,3,3,CG,23,21,_G,1,1", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:ACCS", ffListMV.get(0));
		 assertEquals("1/1:CA,13,10,CG,8,9,C_,2,0", ffListMV.get(1));
		 assertEquals("1/1:CA,3,3,CG,23,21,_G,1,1", ffListMV.get(2));
		 assertEquals("1/1:CA,13,10,CG,8,9,C_,2,0", ffListMV.get(3));
		 assertEquals("1/1:CA,3,3,CG,23,21,_G,1,1", ffListMV.get(4));
	 }
	 
	 @Test
	 public void correctGT() {
		 //chr1	142609531	.	G	C,T	123.77	MR;NNS	SOMATIC;IN=2;HOM=3,ATCTGACAAAc,tGGCTAATATC;CONF=ZERO;EFF=intergenic_region(MODIFIER||||||||||1),intergenic_region(MODIFIER||||||||||2)	GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS	0/1:60,8:68:99:152,0,2506:C/G:G27[39.48],31[37.94]:.:0:0	0/1:120,28:148:99:540,0,5236:G/T:G55[39.82],61[36.74]:.:0:0
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","142609531",".","G","C,T","123.77","MR;NNS","SOMATIC;IN=2;HOM=3,ATCTGACAAAc,tGGCTAATATC;CONF=ZERO;EFF=intergenic_region(MODIFIER||||||||||1),intergenic_region(MODIFIER||||||||||2)","GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS","0/1:60,8:68:99:152,0,2506:C/G:G27[39.48],31[37.94]:.:0:0","0/1:120,28:148:99:540,0,5236:G/T:G55[39.82],61[36.74]:.:0:0"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS", ffList.get(0));
		 assertEquals("0/1:60,8:68:99:152,0,2506:C/G:G27[39.48],31[37.94]:.:0:0", ffList.get(1));
		 assertEquals("0/1:120,28:148:99:540,0,5236:G/T:G55[39.82],61[36.74]:.:0:0", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:AD:DP:GQ:GD:AC:OABS:MR:NNS", ffListMV.get(0));
		 assertEquals(".:.:.:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals(".:.:.:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals("0/1:60,8:68:99:C/G:G27[39.48],31[37.94]:.:0:0", ffListMV.get(3));
		 assertEquals("0/2:120,28:148:99:G/T:G55[39.82],61[36.74]:.:0:0", ffListMV.get(4));
		 
	 }
	  			
}
