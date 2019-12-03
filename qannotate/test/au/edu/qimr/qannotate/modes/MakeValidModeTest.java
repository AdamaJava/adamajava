package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;

public class MakeValidModeTest {
	
	
	@Test
	public void makeValidSecondCaller() {
		//chr1    823538  rs375868960     G       T       63.77   MR;NNS  SOMATIC;IN=2;DB;GERM=60,185;HOM=0,TCTGGGCCTAtTCCTTCCTTT;CONF=ZERO  GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        .:.:.:.:.:G/G:G5[39.4],13[39.62]:.:0:0  0/1:53,6:59:92:92,0,2176:G/T:G11[37.82],31[40.45]:.:0:0
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","823538","rs375868960","G","T","63.77","MR;NNS","SOMATIC;IN=2;DB;GERM=60,185;HOM=0,TCTGGGCCTAtTCCTTCCTTT;CONF=ZERO","GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS","./.:.:.:.:.:G/G:G5[39.4],13[39.62]:.:0:0","0/1:53,6:59:92:92,0,2176:G/T:G11[37.82],31[40.45]:.:0:0"});
		 Map<String, short[]> positions = new HashMap<>();
		 positions.put("1", new short[]{1,2});
		 positions.put("2", new short[]{3,4});
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS", ffList.get(0));
		 assertEquals("./.:.:.:.:.:G/G:G5[39.4],13[39.62]:.:0:0", ffList.get(1));
		 assertEquals("0/1:53,6:59:92:92,0,2176:G/T:G11[37.82],31[40.45]:.:0:0", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.processVcfRecord(vcf1, positions);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:AC:AD:CCC:CCM:DP:FT:GD:GQ:INF:MR:NNS:OABS", ffListMV.get(0));
		 assertEquals("./.:.:.:.:1:.:.:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals("./.:.:.:.:1:.:.:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals("./.:G5[39.4],13[39.62]:.:.:3:.:.:G/G:.:.:0:0:.", ffListMV.get(3));
		 assertEquals("0/1:G11[37.82],31[40.45]:53,6:.:3:59:.:G/T:92:SOMATIC:0:0:.", ffListMV.get(4));
	}
	
	@Test
	public void makeValidBothCallers() {
		//chr1    883516  rs267598747     G       A       .       PASS_1;PASS_2   SOMATIC_1;FLANK=CTGCAAGACCA;SOMATIC_2;IN=1,2;DB;HOM=2,CACACCTGCAaGACCACAGGC;CONF=HIGH_1,HIGH_2   GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/0&.:G/G&G/G:G40[37.05],43[37.81]&G40[37.05],43[37.81]:83&.:G40[37.05]43[37.81]&.:0&0:0&0:.:.:.        0/1&0/1:A/G&A/G:A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]&A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]:77&90:A41[37.8]20[37.8];C1[12]0[0];G8[35.88]7[40.43]&.:61&61:55&55:18,72:99:2702,0,488
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","883516","rs267598747","G","A",".","PASS_1;PASS_2","SOMATIC_1;FLANK=CTGCAAGACCA;SOMATIC_2;IN=1,2;DB;HOM=2,CACACCTGCAaGACCACAGGC;CONF=HIGH_1,HIGH_2",
				"GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL",
				"0/0&.:G/G&G/G:G40[37.05],43[37.81]&G40[37.05],43[37.81]:83&.:G40[37.05]43[37.81]&.:0&0:0&0:.:.:.",
				"0/1&0/1:A/G&A/G:A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]&A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]:77&90:A41[37.8]20[37.8];C1[12]0[0];G8[35.88]7[40.43]&.:61&61:55&55:18,72:99:2702,0,488"});
		Map<String, short[]> positions = new HashMap<>();
		positions.put("1", new short[]{1,2});
		positions.put("2", new short[]{3,4});
		/*
		 * before
		 */
		List<String> ffList = vcf1.getFormatFields();
		assertEquals(3, ffList.size());
		assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		assertEquals("0/0&.:G/G&G/G:G40[37.05],43[37.81]&G40[37.05],43[37.81]:83&.:G40[37.05]43[37.81]&.:0&0:0&0:.:.:.", ffList.get(1));
		assertEquals("0/1&0/1:A/G&A/G:A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]&A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]:77&90:A41[37.8]20[37.8];C1[12]0[0];G8[35.88]7[40.43]&.:61&61:55&55:18,72:99:2702,0,488", ffList.get(2));
		
		/*
		 * after
		 */
		MakeValidMode.processVcfRecord(vcf1, positions);
		List<String> ffListMV = vcf1.getFormatFields();
		assertEquals(5, ffListMV.size());
		assertEquals("GT:AC:AD:CCC:CCM:DP:FT:GD:GQ:INF:MR:NNS:OABS", ffListMV.get(0));
		assertEquals("0/0:G40[37.05],43[37.81]:83,0:Reference:13:83:PASS:G/G:.:.:0:0:G40[37.05]43[37.81]", ffListMV.get(1));
		assertEquals("0/1:A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]:18,72:Somatic:13:77:PASS:A/G:99:SOMATIC:61:55:A41[37.8]20[37.8];C1[12]0[0];G8[35.88]7[40.43]", ffListMV.get(2));
		assertEquals(".:G40[37.05],43[37.81]:.:.:3:.:PASS:G/G:.:.:0:0:.", ffListMV.get(3));
		assertEquals("0/1:A41[37.8],20[37.8],C1[12],0[0],G8[35.88],7[40.43]:18,72:.:3:90:PASS:A/G:99:SOMATIC:61:55:.", ffListMV.get(4));
	}
	
	@Test
	public void makeValidBothCallersSingleSample() {
		//chr1    822939  .       C       T       .       PASS_1;PASS_2   FLANK=AATTTTATTTC;AC=1;AF=0.500;AN=2;BaseQRankSum=0.499;ClippingRankSum=-1.448;DP=90;FS=2.163;MLEAC=1;MLEAF=0.500;MQ=59.72;MQRankSum=1.367;QD=31.01;ReadPosRankSum=1.074;SOR=1.002;IN=1,2;HOM=3,TCAGCAATTTtATTTCCAGAA;CONF=HIGH_1,HIGH_2   GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/1&0/1:C/T&C/T:C8[37.25],8[40.5],T30[40],36[38.81]&C8[37.25],8[40.5],T30[40],36[38.81]:82&90:C8[37.25]8[40.5];T30[40]36[38.81]&.:66&66:59&59:18,72:99:2819,0,478
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","822939",".","C","T",".","PASS_1;PASS_2","FLANK=AATTTTATTTC;AC=1;AF=0.500;AN=2;BaseQRankSum=0.499;ClippingRankSum=-1.448;DP=90;FS=2.163;MLEAC=1;MLEAF=0.500;MQ=59.72;MQRankSum=1.367;QD=31.01;ReadPosRankSum=1.074;SOR=1.002;IN=1,2;HOM=3,TCAGCAATTTtATTTCCAGAA;CONF=HIGH_1,HIGH_2"
				,"GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL"
				,"0/1&0/1:C/T&C/T:C8[37.25],8[40.5],T30[40],36[38.81]&C8[37.25],8[40.5],T30[40],36[38.81]:82&90:C8[37.25]8[40.5];T30[40]36[38.81]&.:66&66:59&59:18,72:99:2819,0,478"});
		Map<String, short[]> positions = new HashMap<>();
		positions.put("1", new short[]{0,1});
		positions.put("2", new short[]{0,2});
		/*
		 * before
		 */
		List<String> ffList = vcf1.getFormatFields();
		assertEquals(2, ffList.size());
		assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		assertEquals("0/1&0/1:C/T&C/T:C8[37.25],8[40.5],T30[40],36[38.81]&C8[37.25],8[40.5],T30[40],36[38.81]:82&90:C8[37.25]8[40.5];T30[40]36[38.81]&.:66&66:59&59:18,72:99:2819,0,478", ffList.get(1));
		
		/*
		 * after
		 */
		MakeValidMode.processVcfRecord(vcf1, positions, true);
		List<String> ffListMV = vcf1.getFormatFields();
		assertEquals(3, ffListMV.size());
		assertEquals("GT:AC:AD:DP:FT:GD:GQ:INF:MR:NNS:OABS", ffListMV.get(0));
		assertEquals("0/1:C8[37.25],8[40.5],T30[40],36[38.81]:18,72:82:PASS:C/T:99:.:66:59:C8[37.25]8[40.5];T30[40]36[38.81]", ffListMV.get(1));
		assertEquals("0/1:C8[37.25],8[40.5],T30[40],36[38.81]:18,72:90:PASS:C/T:99:.:66:59:.", ffListMV.get(2));
	}
	@Test
	public void makeValidSecondCallerSingleSample() {
		//chr1    823260  rs199973779     G       T       30.77   MR;NNS  AC=1;AF=0.500;AN=2;BaseQRankSum=-1.869;ClippingRankSum=1.147;DP=97;FS=13.216;MLEAC=1;MLEAF=0.500;MQ=58.20;MQRankSum=-4.675;QD=0.32;ReadPosRankSum=1.711;SOR=3.575;IN=2;DB;HOM=3,AGAGTGTGTAtAAATGCATAG;CONF=ZERO    GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        0/1:89,8:97:59:59,0,3932:G/T:G54[39.35],35[39.14]:.:0:0
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","823260","rs199973779","G","T","30.77","MR;NNS","AC=1;AF=0.500;AN=2;BaseQRankSum=-1.869;ClippingRankSum=1.147;DP=97;FS=13.216;MLEAC=1;MLEAF=0.500;MQ=58.20;MQRankSum=-4.675;QD=0.32;ReadPosRankSum=1.711;SOR=3.575;IN=2;DB;HOM=3,AGAGTGTGTAtAAATGCATAG;CONF=ZERO"
				,"GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS"
				,"0/1:89,8:97:59:59,0,3932:G/T:G54[39.35],35[39.14]:.:0:0"});
		Map<String, short[]> positions = new HashMap<>();
		positions.put("1", new short[]{0,1});
		positions.put("2", new short[]{0,2});
		/*
		 * before
		 */
		List<String> ffList = vcf1.getFormatFields();
		assertEquals(2, ffList.size());
		assertEquals("GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS", ffList.get(0));
		assertEquals("0/1:89,8:97:59:59,0,3932:G/T:G54[39.35],35[39.14]:.:0:0", ffList.get(1));
		
		/*
		 * after
		 */
		MakeValidMode.processVcfRecord(vcf1, positions, true);
		List<String> ffListMV = vcf1.getFormatFields();
		assertEquals(3, ffListMV.size());
		assertEquals("GT:AC:AD:DP:FT:GD:GQ:INF:MR:NNS:OABS", ffListMV.get(0));
		assertEquals("./.:.:.:.:.:.:.:.:.:.:.", ffListMV.get(1));
		assertEquals("0/1:G54[39.35],35[39.14]:89,8:97:.:G/T:59:.:0:0:.", ffListMV.get(2));
	}
	@Test
	public void makeValidFirstCallerSingleSample() {
		//chr1    884101  rs4970455       A       C       .       PASS    FLANK=TGGTCCCCCTG;IN=1;DB;HOM=3,CACCCTGGTCcCCCTGGTCCT;CONF=HIGH   GT:GD:AC:DP:OABS:MR:NNS 1/1:C/C:C12[35.92],9[38.56]:21:C12[35.92]9[38.56]:21:15
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","884101","rs4970455","A","C",".","PASS","FLANK=TGGTCCCCCTG;IN=1;DB;HOM=3,CACCCTGGTCcCCCTGGTCCT;CONF=HIGH"
				,"GT:GD:AC:DP:OABS:MR:NNS","1/1:C/C:C12[35.92],9[38.56]:21:C12[35.92]9[38.56]:21:15"});
		Map<String, short[]> positions = new HashMap<>();
		positions.put("1", new short[]{0,1});
		positions.put("2", new short[]{0,2});
		/*
		 * before
		 */
		List<String> ffList = vcf1.getFormatFields();
		assertEquals(2, ffList.size());
		assertEquals("GT:GD:AC:DP:OABS:MR:NNS", ffList.get(0));
		assertEquals("1/1:C/C:C12[35.92],9[38.56]:21:C12[35.92]9[38.56]:21:15", ffList.get(1));
		
		/*
		 * after
		 */
		MakeValidMode.processVcfRecord(vcf1, positions, true);
		List<String> ffListMV = vcf1.getFormatFields();
		assertEquals(3, ffListMV.size());
		assertEquals("GT:AC:AD:DP:FT:GD:INF:MR:NNS:OABS", ffListMV.get(0));
		assertEquals("1/1:C12[35.92],9[38.56]:0,21:21:PASS:C/C:.:21:15:C12[35.92]9[38.56]", ffListMV.get(1));
		assertEquals("./.:.:.:.:.:.:.:.:.:.", ffListMV.get(2));
	}
	
	 @Test
	 public void getupdatedGTs() {
		 assertEquals("0/0", MakeValidMode.getUpdatedGT("A", "G", "A/A"));
		 assertEquals("0/1", MakeValidMode.getUpdatedGT("G", "C,T", "C/G"));
		 assertEquals("2/2", MakeValidMode.getUpdatedGT("G", "C,T", "T/T"));
		 assertEquals("1/1", MakeValidMode.getUpdatedGT("G", "C,T", "C/C"));
		 assertEquals("1/2", MakeValidMode.getUpdatedGT("G", "C,T", "C/T"));
	 }
	 
	 @Test
	 public void getRefAndAltsAsList() {
		 List<String> refAndAlts = MakeValidMode.getRefAndAltsAsList("ref","alts");
		 assertEquals(2, refAndAlts.size());
		 assertEquals("ref", refAndAlts.get(0));
		 assertEquals("alts", refAndAlts.get(1));
		 
		 refAndAlts = MakeValidMode.getRefAndAltsAsList("AC","CC,GG,TT,AA");
		 assertEquals(5, refAndAlts.size());
		 assertEquals("AC", refAndAlts.get(0));
		 assertEquals("CC", refAndAlts.get(1));
		 assertEquals("GG", refAndAlts.get(2));
		 assertEquals("TT", refAndAlts.get(3));
		 assertEquals("AA", refAndAlts.get(4));
	 }
	 
	 @Test
	 public void fixFilters() {
		 VcfRecord v = new VcfRecord(new String[]{"chr1","10051",".","A","C",".","5BP10","."});
		 assertEquals("5BP10", v.getFilter());
		 MakeValidMode.fixFilters(v);
		 assertEquals("5BP", v.getFilter());
		 
		 v = new VcfRecord(new String[]{"chr1","10051",".","A","C",".","MIN","."});
		 assertEquals("MIN", v.getFilter());
		 MakeValidMode.fixFilters(v);
		 assertEquals("MIN", v.getFilter());
		 
		 v = new VcfRecord(new String[]{"chr1","10051",".","A","C",".","MIN;5BP1","."});
		 assertEquals("MIN;5BP1", v.getFilter());
		 MakeValidMode.fixFilters(v);
		 assertEquals("MIN;5BP", v.getFilter());
		 
	 }
	 
	 @Test
	 public void getGTForCompoundSnp() {
		 assertEquals("0/1", MakeValidMode.getGTForCS("AA", "CC", "AACC"));
		 assertEquals("0/0", MakeValidMode.getGTForCS("AA", "CC", "AAGG"));
		 assertEquals("1/1", MakeValidMode.getGTForCS("AA", "CC", "GGCC"));
	 }
	 
	 @Test
	 public void splitFormatField() {
		 assertArrayEquals(new String[]{".","."}, MakeValidMode.splitFormatField(".&."));
		 assertArrayEquals(new String[]{".:.",".:."}, MakeValidMode.splitFormatField(".&.:.&."));
		 assertArrayEquals(new String[]{"0/1:.","1/1:."}, MakeValidMode.splitFormatField("0/1&1/1:.&."));
		 assertArrayEquals(new String[]{"0/1:105","1/1:99"}, MakeValidMode.splitFormatField("0/1&1/1:105&99"));
		 assertArrayEquals(new String[]{"0/1:105:.","1/1:99:10,3"}, MakeValidMode.splitFormatField("0/1&1/1:105&99:.&10,3"));
	 }
	 
	 @Test
	 public void setupArrayWithMissingData() {
		 try {
			 assertArrayEquals(new String[0], MakeValidMode.createMissingDataArray(-1));
			 fail("Should have thrown an exception");
		 } catch (IllegalArgumentException iae) {}
		 assertArrayEquals(new String[0], MakeValidMode.createMissingDataArray(0));
		 assertArrayEquals(new String[]{"."}, MakeValidMode.createMissingDataArray(1));
		 assertArrayEquals(new String[]{".","."}, MakeValidMode.createMissingDataArray(2));
		 assertArrayEquals(new String[]{".",".",".",".","."}, MakeValidMode.createMissingDataArray(5));
	 }
	 
	 @Test
	 public void refAndAltsInvalid() {
		assertEquals(false, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","A","C",".",".","."}))); 
		assertEquals(true, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","A","A",".",".","."}))); 
		assertEquals(true, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","M","C",".",".","."}))); 
		assertEquals(true, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","RR","C",".",".","."}))); 
		assertEquals(false, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","XX","C",".",".","."}))); 
		assertEquals(false, MakeValidMode.invalidRefAndAlt( new VcfRecord(new String[]{"chr1","10051",".","XX","XXX",".",".","."}))); 
	 }
	 
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
		 assertEquals("./.:.:.:.:.:.:.", ffListMV.get(3));
		 assertEquals("./.:.:.:.:.:.:.", ffListMV.get(4));
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
		 assertEquals("./.:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals("./.:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals("0/0:A/A:A61[38.72],13[21.62],C0[0],2[17]:76:A61[38.72]13[21.62];C0[0]2[17]:2:2", ffListMV.get(3));
		 assertEquals("0/1:A/C:A55[39.51],18[20.28],C3[33.33],4[12]:80:A55[39.51]18[20.28];C3[33.33]4[12]:7:7", ffListMV.get(4));
	 }
	 
	 @Test
	 public void makeValidBothCalers() {
		 //chr1    553742  rs140182652     G       A       .       COVT_1;NCIT_2   FLANK=CTGTCACCCAG;AC=1;AF=0.500;AN=2;BaseQRankSum=1.743;ClippingRankSum=1.460;DP=38;FS=3.132;MLEAC=1;MLEAF=0.500;MQ=40.30;MQ0=0;MQRankSum=-2.308;QD=8.55;ReadPosRankSum=0.204;SOR=1.445;IN=1,2;DB;HOM=3,TCACTCTGTCaCCCAGGCTGG;CONF=ZERO_1,ZERO_2     GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795        0/0&.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:.
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","553742","rs140182652","G","A",".","COVT_1;NCIT_2","FLANK=CTGTCACCCAG;AC=1;AF=0.500;AN=2;BaseQRankSum=1.743;ClippingRankSum=1.460;DP=38;FS=3.132;MLEAC=1;MLEAF=0.500;MQ=40.30;MQ0=0;MQRankSum=-2.308;QD=8.55;ReadPosRankSum=0.204;SOR=1.445;IN=1,2;DB;HOM=3,TCACTCTGTCaCCCAGGCTGG;CONF=ZERO_1,ZERO_2","GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL","0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795","0/0&./.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:."});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		 assertEquals("0/1&0/1:A/G&A/G:A5[35.2],9[38.11],G11[36],13[37.54]&A5[35.2],9[38.11],G11[36],13[37.54]:38&38:A5[35.2]9[38.11];G11[36]13[37.54]&.:14&14:13&13:26,12:99:353,0,795", ffList.get(1));
		 assertEquals("0/0&./.:G/G&.:G4[40],3[38]&G4[40],3[38]:7&.:G4[40]3[38]&.:0&0:0&0:.:.:.", ffList.get(2));
		 
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
		 assertEquals("./.:.:G4[40],3[38]:.:.:0:0:.:.", ffListMV.get(4));
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
		 assertEquals("./.:.:.:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals("./.:.:.:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals("0/1:60,8:68:99:C/G:G27[39.48],31[37.94]:.:0:0", ffListMV.get(3));
		 assertEquals("0/2:120,28:148:99:G/T:G55[39.82],61[36.74]:.:0:0", ffListMV.get(4));
	 }
	 
	 @Test
	 public void arrayIndexOOB() {
		 
		 List<String> data = new ArrayList<>();
		data.add("##fileformat=VCFv4.2");
		data.add("##fileDate=20190726");;
		data.add("##reference=file:///mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa");;
		data.add("##qDonorId=qasim_70p");;
		data.add("##qControlSample=qasim_chr1_70p_T");;
		data.add("##qTestSample=qasim_chr1_70p_B");;
		data.add("##qINPUT_GATK_TEST=/mnt/lustre/working/genomeinfo/analysis/b/f/bf77f9bf-f597-4d00-b0f2-7b669016cf84/bf77f9bf-f597-4d00-b0f2-7b669016cf84.vcf");;
		data.add("##qINPUT_GATK_CONTROL=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/75fd3e43-ef28-4968-8ed5-08a979e3f873.vcf");;
		data.add("##qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");;
		data.add("##qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");;
		data.add("##qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");;
		data.add("##qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");;
		data.add("##qAnalysisId=1bca1f69-26db-40be-90da-77149f308a21");
		data.add("##SnpEffVersion=\"4.0e (build 2014-09-13), by Pablo Cingolani\"");
		data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf \"");
		data.add("##qUUID=609e8bd3-a7c0-4934-8c21-e2fdee9e5352");
		data.add("##qSource=qannotate-2.0.1 (2566)");
		data.add("##qINPUT=609e8bd3-a7c0-4934-8c21-e2fdee9e5352:/mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf");
		data.add("##FILTER=<ID=TRF,Description=\"at least one of the repeat is with repeat sequence length less than six; and the repeat frequence is more than 10 (or more than six for homoplymers repeat), , or less than 20% of informative reads are strong supporting in case of indel variant\">");
		data.add("##FILTER=<ID=TPART,Description=\"The number in the tumour partials column is >=3 and is >10% of the total reads at that position\">");
		data.add("##FILTER=<ID=TBIAS,Description=\"For somatic calls: the supporting tumour reads value is >=3 and the count on one strand is =0 or >0 and is either <10% of supporting reads or >90% of supporting reads\">");
		data.add("##FILTER=<ID=REPEAT,Description=\"this variants is fallen into the repeat region\">");
		data.add("##FILTER=<ID=NPART,Description=\"The number in the normal partials column is >=3 and is >5% of the total reads at that position\">");
		data.add("##FILTER=<ID=NNS,Description=\"For somatic calls: less than 4 novel starts not considering read pair in tumour BAM\">");
		data.add("##FILTER=<ID=NBIAS,Description=\"For germline calls: the supporting normal reads value is >=3 and the count on one strand is =0 or >0 and is either <5% of supporting reads or >95% of supporting reads\">");
		data.add("##FILTER=<ID=MIN,Description=\"For somatic calls: mutation also found in pileup of normal BAM\">");
		data.add("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		data.add("##FILTER=<ID=HCOVT,Description=\"more than 1000 reads in tumour BAM\">");
		data.add("##FILTER=<ID=HCOVN,Description=\"more than 1000 reads in normal BAM\">");
		data.add("##FILTER=<ID=COVT,Description=\"For germline calls: less than 8 reads coverage in tumour\">");
		data.add("##FILTER=<ID=COVN8,Description=\"For germline calls: less than 8 reads coverage in normal\">");
		data.add("##FILTER=<ID=COVN12,Description=\"For somatic calls: less than 12 reads coverage in normal BAM\">");
		data.add("##INFO=<ID=VLD,Number=0,Type=Flag,Description=\"Is Validated.  This bit is set if the variant has 2+ minor allele count based on frequency or genotype data.\">");
		data.add("##INFO=<ID=VAF,Number=.,Type=String,Description=\"Variant allele frequencies based on 1000Genomes from dbSNP as the CAF. CAF starting with the reference allele followed by alternate alleles as ordered in the ALT column.   Here we only take the related allel frequency.\">");
		data.add("##INFO=<ID=TRF,Number=1,Type=String,Description=\"List all repeat reported by TRFFinder,  crossing over the variant position.all repeat follow <repeat sequence Length>_<repeat frequency>, separated by ';'\">");
		data.add("##INFO=<ID=SSOI,Number=1,Type=String,Description=\"counts of strong support indels compare with total informative reads coverage\">");
		data.add("##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">");
		data.add("##INFO=<ID=SOMATIC,Number=1,Type=String,Description=\"There are more than 2 novel starts  or more than 0.05 soi (number of supporting informative reads /number of informative reads) on control BAM\">");
		data.add("##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">");
		data.add("##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">");
		data.add("##INFO=<ID=NMD,Number=.,Type=String,Description=\"Predicted nonsense mediated decay effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=NIOC,Number=1,Type=String,Description=\"counts of nearby indels compare with total coverage\">");
		data.add("##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">");
		data.add("##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">");
		data.add("##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">");
		data.add("##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=LOF,Number=.,Type=String,Description=\"Predicted loss of function effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">");
		data.add("##INFO=<ID=IN,Number=1,Type=String,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		data.add("##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">");
		data.add("##INFO=<ID=HOM,Number=.,Type=String,Description=\"nearby reference sequence fallen in a specified widow size,  leading by the number of homopolymers base.\">");
		data.add("##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">");
		data.add("##INFO=<ID=EFF,Number=.,Type=String,Description=\"Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_B	ioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )'\">");
		data.add("##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">");
		data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");
		data.add("##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\",Source=/mnt/lustre/reference/dbsnp/141/00-All.vcf,Version=141>");
		data.add("##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">");
		data.add("##INFO=<ID=CONF,Number=1,Type=String,Description=\"set to HIGH if the variants passed all filter, nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants\">");
		data.add("##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">");
		data.add("##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">");
		data.add("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">");
		data.add("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		data.add("##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">");
		data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles\">");
		data.add("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">");
		data.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		data.add("##FORMAT=<ID=ACINDEL,Number=.,Type=String,Description=\"counts of indels, follow formart:novelStarts,totalCoverage,informativeReadCount,strongSuportReadCount[forwardsuportReadCount,backwardsuportReadCount],suportReadCount[novelStarts],partialReadCount,nearbyIndelCount,nearybySoftclipCount\">");
		data.add("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Thu Jul 25 14:13:49 AEST 2019\",Epoch=1564028029841,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[chr1] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/tmp_75fd3e43-ef28-4968-8ed5-08a979e3f873_0.vcf.log help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/mnt/lustre/reference/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		data.add("##contig=<ID=chrY,length=59373566>");
		data.add("##contig=<ID=chrX,length=155270560>");
		data.add("##contig=<ID=chrMT,length=16569>");
		data.add("##contig=<ID=chr9,length=141213431>");
		data.add("##contig=<ID=chr8,length=146364022>");
		data.add("##contig=<ID=chr7,length=159138663>");
		data.add("##contig=<ID=chr6,length=171115067>");
		data.add("##contig=<ID=chr5,length=180915260>");
		data.add("##contig=<ID=chr4,length=191154276>");
		data.add("##contig=<ID=chr3,length=198022430>");
		data.add("##contig=<ID=chr22,length=51304566>");
		data.add("##contig=<ID=chr21,length=48129895>");
		data.add("##contig=<ID=chr20,length=63025520>");
		data.add("##contig=<ID=chr2,length=243199373>");
		data.add("##contig=<ID=chr19,length=59128983>");
		data.add("##contig=<ID=chr18,length=78077248>");
		data.add("##contig=<ID=chr17,length=81195210>");
		data.add("##contig=<ID=chr16,length=90354753>");
		data.add("##contig=<ID=chr15,length=102531392>");
		data.add("##contig=<ID=chr14,length=107349540>");
		data.add("##contig=<ID=chr13,length=115169878>");
		data.add("##contig=<ID=chr12,length=133851895>");
		data.add("##contig=<ID=chr11,length=135006516>");
		data.add("##contig=<ID=chr10,length=135534747>");
		data.add("##contig=<ID=chr1,length=249250621>");
		data.add("##contig=<ID=GL000249.1,length=38502>");
		data.add("##contig=<ID=GL000248.1,length=39786>");
		data.add("##contig=<ID=GL000247.1,length=36422>");
		data.add("##contig=<ID=GL000246.1,length=38154>");
		data.add("##contig=<ID=GL000245.1,length=36651>");
		data.add("##contig=<ID=GL000244.1,length=39929>");
		data.add("##contig=<ID=GL000243.1,length=43341>");
		data.add("##contig=<ID=GL000242.1,length=43523>");
		data.add("##contig=<ID=GL000241.1,length=42152>");
		data.add("##contig=<ID=GL000240.1,length=41933>");
		data.add("##contig=<ID=GL000239.1,length=33824>");
		data.add("##contig=<ID=GL000238.1,length=39939>");
		data.add("##contig=<ID=GL000237.1,length=45867>");
		data.add("##contig=<ID=GL000236.1,length=41934>");
		data.add("##contig=<ID=GL000235.1,length=34474>");
		data.add("##contig=<ID=GL000234.1,length=40531>");
		data.add("##contig=<ID=GL000233.1,length=45941>");
		data.add("##contig=<ID=GL000232.1,length=40652>");
		data.add("##contig=<ID=GL000231.1,length=27386>");
		data.add("##contig=<ID=GL000230.1,length=43691>");
		data.add("##contig=<ID=GL000229.1,length=19913>");
		data.add("##contig=<ID=GL000228.1,length=129120>");
		data.add("##contig=<ID=GL000227.1,length=128374>");
		data.add("##contig=<ID=GL000226.1,length=15008>");
		data.add("##contig=<ID=GL000225.1,length=211173>");
		data.add("##contig=<ID=GL000224.1,length=179693>");
		data.add("##contig=<ID=GL000223.1,length=180455>");
		data.add("##contig=<ID=GL000222.1,length=186861>");
		data.add("##contig=<ID=GL000221.1,length=155397>");
		data.add("##contig=<ID=GL000220.1,length=161802>");
		data.add("##contig=<ID=GL000219.1,length=179198>");
		data.add("##contig=<ID=GL000218.1,length=161147>");
		data.add("##contig=<ID=GL000217.1,length=172149>");
		data.add("##contig=<ID=GL000216.1,length=172294>");
		data.add("##contig=<ID=GL000215.1,length=172545>");
		data.add("##contig=<ID=GL000214.1,length=137718>");
		data.add("##contig=<ID=GL000213.1,length=164239>");
		data.add("##contig=<ID=GL000212.1,length=186858>");
		data.add("##contig=<ID=GL000211.1,length=166566>");
		data.add("##contig=<ID=GL000210.1,length=27682>");
		data.add("##contig=<ID=GL000209.1,length=159169>");
		data.add("##contig=<ID=GL000208.1,length=92689>");
		data.add("##contig=<ID=GL000207.1,length=4262>");
		data.add("##contig=<ID=GL000206.1,length=41001>");
		data.add("##contig=<ID=GL000205.1,length=174588>");
		data.add("##contig=<ID=GL000204.1,length=81310>");
		data.add("##contig=<ID=GL000203.1,length=37498>");
		data.add("##contig=<ID=GL000202.1,length=40103>");
		data.add("##contig=<ID=GL000201.1,length=36148>");
		data.add("##contig=<ID=GL000200.1,length=187035>");
		data.add("##contig=<ID=GL000199.1,length=169874>");
		data.add("##contig=<ID=GL000198.1,length=90085>");
		data.add("##contig=<ID=GL000197.1,length=37175>");
		data.add("##contig=<ID=GL000196.1,length=38914>");
		data.add("##contig=<ID=GL000195.1,length=182896>");
		data.add("##contig=<ID=GL000194.1,length=191469>");
		data.add("##contig=<ID=GL000193.1,length=189789>");
		data.add("##contig=<ID=GL000192.1,length=547496>");
		data.add("##contig=<ID=GL000191.1,length=106433>");
		data.add("##qPG=<ID=6,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:25:33,CL=\"qannotate --mode snpeff -d /mnt/lustre/reference/software/snpEff/GRCh37.75 -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpeff.log\">");
		data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:51,CL=\"qannotate --mode indelConfidence -d /mnt/lustre/reference/genomeinfo/qannotate/indel.repeat.mask -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf.log\">");
		data.add("##qPG=<ID=4,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:35,CL=\"qannotate --mode hom -d /mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom.log\">");
		data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:33,CL=\"qannotate --mode trf -d /mnt/lustre/reference/genomeinfo/qannotate/GRCh37_ICGC_standard_v2.fa.2.7.7.80.10.20.2000_simple.txt -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=1,Tool=q3indel,Version=1.0 (9971),Date=2019-07-26 11:19:21,CL=\"q3indel -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.ini -log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.log [runMode: gatk]\">");
		data.add("##1:qUUID=8a12dd2e-a856-4247-9a96-cec09f3fd784");
		data.add("##1:qSource=qSNP v2.0 (2566)");
		data.add("##1:qDonorId=http://purl.org/net/grafli/donor#01b9ca75-2aec-4ac6-929f-8f127a51556e");
		data.add("##1:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		data.add("##1:qTestSample=23d282c5-5996-4903-8576-515139830265");
		data.add("##1:qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");
		data.add("##1:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		data.add("##1:qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");
		data.add("##1:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		data.add("##1:qAnalysisId=4019018e-4a54-4fc7-834b-d858e9e8981a");
		data.add("##2:qUUID=7d6847f0-edba-45d5-81fb-c03b3cd0fa28");
		data.add("##2:qSource=qSNP v2.0 (2566)");
		data.add("##2:qDonorId=http://purl.org/net/grafli/donor#01b9ca75-2aec-4ac6-929f-8f127a51556e");
		data.add("##2:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		data.add("##2:qTestSample=23d282c5-5996-4903-8576-515139830265");
		data.add("##2:qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");
		data.add("##2:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		data.add("##2:qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");
		data.add("##2:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		data.add("##2:qAnalysisId=e90fd4e3-a6de-40be-8458-60dca990c0d7");
		data.add("##2:qControlVcf=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/75fd3e43-ef28-4968-8ed5-08a979e3f873.vcf");
		data.add("##2:qControlVcfUUID=null");
		data.add("##2:qControlVcfGATKVersion=3.3-0-g37228af");
		data.add("##2:qTestVcf=/mnt/lustre/working/genomeinfo/analysis/b/f/bf77f9bf-f597-4d00-b0f2-7b669016cf84/bf77f9bf-f597-4d00-b0f2-7b669016cf84.vcf");
		data.add("##2:qTestVcfUUID=null");
		data.add("##2:qTestVcfGATKVersion=3.3-0-g37228af");
		data.add("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/analysis/4/0/4019018e-4a54-4fc7-834b-d858e9e8981a/4019018e-4a54-4fc7-834b-d858e9e8981a.vcf");
		data.add("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/analysis/e/9/e90fd4e3-a6de-40be-8458-60dca990c0d7/e90fd4e3-a6de-40be-8458-60dca990c0d7.vcf");
		data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf\"");
		data.add("##qUUID=e8eadd70-0f21-4592-8186-76851c99f4b0");
		data.add("##qINPUT=e8eadd70-0f21-4592-8186-76851c99f4b0:/mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf");
		data.add("##FILTER=<ID=SBIASCOV,Description=\"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)\">");
		data.add("##FILTER=<ID=SBIASALT,Description=\"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)\">");
		data.add("##FILTER=<ID=SAT3,Description=\"Less than 3 reads of same allele in tumour\">");
		data.add("##FILTER=<ID=SAN3,Description=\"Less than 3 reads of same allele in normal\">");
		data.add("##FILTER=<ID=NNS,Description=\"Less than 4 novel starts not considering read pair\">");
		data.add("##FILTER=<ID=NCIT,Description=\"No call in test\">");
		data.add("##FILTER=<ID=MR,Description=\"Less than 5 mutant reads\">");
		data.add("##FILTER=<ID=MIUN,Description=\"Mutation also found in pileup of (unfiltered) normal\">");
		data.add("##FILTER=<ID=MIN,Description=\"Mutation also found in pileup of normal\">");
		data.add("##FILTER=<ID=MER,Description=\"Mutation equals reference\">");
		data.add("##FILTER=<ID=GERM,Description=\"Mutation is a germline variant in another patient\">");
		data.add("##FILTER=<ID=COVT,Description=\"Less than 8 reads coverage in tumour\">");
		data.add("##FILTER=<ID=COVN8,Description=\"Less than 8 reads coverage in normal\">");
		data.add("##FILTER=<ID=COVN12,Description=\"Less than 12 reads coverage in normal\">");
		data.add("##INFO=<ID=SOMATIC_n,Number=0,Type=Flag,Description=\"Indicates that the nth input file considered this record to be somatic. Multiple values are allowed which indicate that more than 1 input file consider this record to be somatic\">");
		data.add("##INFO=<ID=SOMATIC,Number=0,Type=Flag,Description=\"Indicates that the record is a somatic mutation\">");
		data.add("##INFO=<ID=IN,Number=.,Type=Integer,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		data.add("##INFO=<ID=GERM,Number=2,Type=Integer,Description=\"Counts of donor occurs this mutation, total recorded donor number\",Source=/mnt/lustre/reference/genomeinfo/qannotate/icgc_germline_qsnp_PUBLIC.vcf,FileDate=null>");
		data.add("##INFO=<ID=FLANK,Number=1,Type=String,Description=\"Flanking sequence either side of variant\">");
		data.add("##INFO=<ID=CONF,Number=.,Type=String,Description=\"set to HIGH if the variants passed all filter, appeared on more than 4 novel stars reads and more than 5 reads contains variants, is adjacent to reference sequence with less than 6 homopolymer base; Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than 4 novel stars reads and more than 4 reads contains variants;Otherwise set to Zero if the variants didn't matched one of above conditions.\">");
		data.add("##FORMAT=<ID=OABS,Number=1,Type=String,Description=\"Observed Alleles By Strand: semi-colon separated list of observed alleles with each one in this format: forward_strand_count[avg_base_quality]reverse_strand_count[avg_base_quality], e.g. A18[39]12[42]\">");
		data.add("##FORMAT=<ID=NNS,Number=1,Type=Integer,Description=\"Number of novel starts not considering read pair\">");
		data.add("##FORMAT=<ID=MR,Number=1,Type=Integer,Description=\"Number of mutant/variant reads\">");
		data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">");
		data.add("##FORMAT=<ID=ACCS,Number=.,Type=String,Description=\"Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)\">");
		data.add("##FORMAT=<ID=AC,Number=.,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">");
		data.add("##qPG=<ID=6,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:24:03,CL=\"qannotate --mode snpeff -d /mnt/lustre/reference/software/snpEff/GRCh37.75 -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.snpeff.log\">");
		data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:35,CL=\"qannotate --mode Confidence -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf.log\">");
		data.add("##qPG=<ID=4,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:08,CL=\"qannotate --mode hom -d /mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom.log\">");
		data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:02,CL=\"qannotate --mode germline -d /mnt/lustre/reference/genomeinfo/qannotate/icgc_germline_qsnp_PUBLIC.vcf -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:20:36,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=1,Tool=q3vcftools MergeSameSample,Version=0.1 (9971),Date=2019-07-26 15:18:18,CL=\"q3vcftools MergeSameSample -vcf /mnt/lustre/working/genomeinfo/analysis/4/0/4019018e-4a54-4fc7-834b-d858e9e8981a/4019018e-4a54-4fc7-834b-d858e9e8981a.vcf -vcf /mnt/lustre/working/genomeinfo/analysis/e/9/e90fd4e3-a6de-40be-8458-60dca990c0d7/e90fd4e3-a6de-40be-8458-60dca990c0d7.vcf -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged.log\">");
		data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqasim_chr1_70p_T\tqasim_chr1_70p_B");

		VcfHeader header = new VcfHeader();
		for (String s : data) {
			header.addOrReplace(s);
		}
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
		
		//reheader
		VcfHeader outputHeader = MakeValidMode.reheader(header, "unit test", "testInput",  "reference");
		VcfFileMeta newMeta = new VcfFileMeta(outputHeader);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, newMeta.getType());
		 
		 //chr1    251169  .       G       T       .       PASS_1;PASS_2   SOMATIC_1;FLANK=AAAGCTGAATT;SOMATIC_2;IN=1,2;HOM=2,GTTGGAAAGCtGAATTTATCT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||n.263+7848C>A||AP006222.2|lincRNA|NON_CODING|ENST00000424587|2|1)     
		 //GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.      0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","251169",".","G","T",".","PASS_1;PASS_2","SOMATIC_1;FLANK=AAAGCTGAATT;SOMATIC_2;IN=1,2;HOM=2,GTTGGAAAGCtGAATTTATCT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||n.263+7848C>A||AP006222.2|lincRNA|NON_CODING|ENST00000424587|2|1)","GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL","0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.","0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		 assertEquals("0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.", ffList.get(1));
		 assertEquals("0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ", ffListMV.get(0));
		 assertEquals("0/0:G/G:G19[38.95],14[35.36]:33:G19[38.95]14[35.36]:0:0:.:.", ffListMV.get(1));
		 assertEquals("0/1:G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]:71:G22[37.45]22[38.09];T11[36.64]16[37.56]:27:25:43,28:99", ffListMV.get(2));
		 assertEquals(".:G/G:G19[38.95],14[35.36]:.:G19[38.95]14[35.36]:0:0:.:.", ffListMV.get(3));
		 assertEquals("0/1:G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]:71:G22[37.45]22[38.09];T11[36.64]16[37.56]:27:25:43,28:99", ffListMV.get(4));
		 
		 // ccm...
		 Map<String, short[]> callerPositionsMap = newMeta.getCallerSamplePositions();
		 MakeValidMode.addCCM(vcf1, callerPositionsMap);
	 }
	 
	 @Test
	 public void arrayIndexOOB2() {
		 
		 List<String> data = new ArrayList<>();
		data.add("##fileformat=VCFv4.2");
		data.add("##fileDate=20190726");
		data.add("##reference=file:///mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa");
		data.add("##qDonorId=qasim_70p");
		data.add("##qControlSample=qasim_chr1_70p_T");
		data.add("##qTestSample=qasim_chr1_70p_B");
		data.add("##qINPUT_GATK_TEST=/mnt/lustre/working/genomeinfo/analysis/b/f/bf77f9bf-f597-4d00-b0f2-7b669016cf84/bf77f9bf-f597-4d00-b0f2-7b669016cf84.vcf");
		data.add("##qINPUT_GATK_CONTROL=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/75fd3e43-ef28-4968-8ed5-08a979e3f873.vcf");
		data.add("##qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");
		data.add("##qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		data.add("##qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");
		data.add("##qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		data.add("##qAnalysisId=1bca1f69-26db-40be-90da-77149f308a21");
		data.add("##SnpEffVersion=\"4.0e (build 2014-09-13), by Pablo Cingolani\"");
		data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf \"");
		data.add("##qUUID=609e8bd3-a7c0-4934-8c21-e2fdee9e5352");
		data.add("##qSource=qannotate-2.0.1 (2566)");
		data.add("##qINPUT=609e8bd3-a7c0-4934-8c21-e2fdee9e5352:/mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf");
		data.add("##FILTER=<ID=TRF,Description=\"at least one of the repeat is with repeat sequence length less than six; and the repeat frequence is more than 10 (or more than six for homoplymers repeat), , or less than 20% of informative reads are strong supporting in case of indel variant\">");
		data.add("##FILTER=<ID=TPART,Description=\"The number in the tumour partials column is >=3 and is >10% of the total reads at that position\">");
		data.add("##FILTER=<ID=TBIAS,Description=\"For somatic calls: the supporting tumour reads value is >=3 and the count on one strand is =0 or >0 and is either <10% of supporting reads or >90% of supporting reads\">");
		data.add("##FILTER=<ID=REPEAT,Description=\"this variants is fallen into the repeat region\">");
		data.add("##FILTER=<ID=NPART,Description=\"The number in the normal partials column is >=3 and is >5% of the total reads at that position\">");
		data.add("##FILTER=<ID=NNS,Description=\"For somatic calls: less than 4 novel starts not considering read pair in tumour BAM\">");
		data.add("##FILTER=<ID=NBIAS,Description=\"For germline calls: the supporting normal reads value is >=3 and the count on one strand is =0 or >0 and is either <5% of supporting reads or >95% of supporting reads\">");
		data.add("##FILTER=<ID=MIN,Description=\"For somatic calls: mutation also found in pileup of normal BAM\">");
		data.add("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		data.add("##FILTER=<ID=HCOVT,Description=\"more than 1000 reads in tumour BAM\">");
		data.add("##FILTER=<ID=HCOVN,Description=\"more than 1000 reads in normal BAM\">");
		data.add("##FILTER=<ID=COVT,Description=\"For germline calls: less than 8 reads coverage in tumour\">");
		data.add("##FILTER=<ID=COVN8,Description=\"For germline calls: less than 8 reads coverage in normal\">");
		data.add("##FILTER=<ID=COVN12,Description=\"For somatic calls: less than 12 reads coverage in normal BAM\">");
		data.add("##INFO=<ID=VLD,Number=0,Type=Flag,Description=\"Is Validated.  This bit is set if the variant has 2+ minor allele count based on frequency or genotype data.\">");
		data.add("##INFO=<ID=VAF,Number=.,Type=String,Description=\"Variant allele frequencies based on 1000Genomes from dbSNP as the CAF. CAF starting with the reference allele followed by alternate alleles as ordered in the ALT column.   Here we only take the related allel frequency.\">");
		data.add("##INFO=<ID=TRF,Number=1,Type=String,Description=\"List all repeat reported by TRFFinder,  crossing over the variant position.all repeat follow <repeat sequence Length>_<repeat frequency>, separated by ';'\">");
		data.add("##INFO=<ID=SSOI,Number=1,Type=String,Description=\"counts of strong support indels compare with total informative reads coverage\">");
		data.add("##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">");
		data.add("##INFO=<ID=SOMATIC,Number=1,Type=String,Description=\"There are more than 2 novel starts  or more than 0.05 soi (number of supporting informative reads /number of informative reads) on control BAM\">");
		data.add("##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">");
		data.add("##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">");
		data.add("##INFO=<ID=NMD,Number=.,Type=String,Description=\"Predicted nonsense mediated decay effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=NIOC,Number=1,Type=String,Description=\"counts of nearby indels compare with total coverage\">");
		data.add("##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">");
		data.add("##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">");
		data.add("##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">");
		data.add("##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=LOF,Number=.,Type=String,Description=\"Predicted loss of function effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">");
		data.add("##INFO=<ID=IN,Number=1,Type=String,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		data.add("##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">");
		data.add("##INFO=<ID=HOM,Number=.,Type=String,Description=\"nearby reference sequence fallen in a specified widow size,  leading by the number of homopolymers base.\">");
		data.add("##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">");
		data.add("##INFO=<ID=EFF,Number=.,Type=String,Description=\"Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_B	ioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )'\">");
		data.add("##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">");
		data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");
		data.add("##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\",Source=/mnt/lustre/reference/dbsnp/141/00-All.vcf,Version=141>");
		data.add("##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">");
		data.add("##INFO=<ID=CONF,Number=1,Type=String,Description=\"set to HIGH if the variants passed all filter, nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants\">");
		data.add("##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">");
		data.add("##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">");
		data.add("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">");
		data.add("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		data.add("##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">");
		data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles\">");
		data.add("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">");
		data.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		data.add("##FORMAT=<ID=ACINDEL,Number=.,Type=String,Description=\"counts of indels, follow formart:novelStarts,totalCoverage,informativeReadCount,strongSuportReadCount[forwardsuportReadCount,backwardsuportReadCount],suportReadCount[novelStarts],partialReadCount,nearbyIndelCount,nearybySoftclipCount\">");
		data.add("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Thu Jul 25 14:13:49 AEST 2019\",Epoch=1564028029841,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[chr1] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/tmp_75fd3e43-ef28-4968-8ed5-08a979e3f873_0.vcf.log help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/mnt/lustre/reference/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		data.add("##contig=<ID=chrY,length=59373566>");
		data.add("##contig=<ID=chrX,length=155270560>");
		data.add("##contig=<ID=chrMT,length=16569>");
		data.add("##contig=<ID=chr9,length=141213431>");
		data.add("##contig=<ID=chr8,length=146364022>");
		data.add("##contig=<ID=chr7,length=159138663>");
		data.add("##contig=<ID=chr6,length=171115067>");
		data.add("##contig=<ID=chr5,length=180915260>");
		data.add("##contig=<ID=chr4,length=191154276>");
		data.add("##contig=<ID=chr3,length=198022430>");
		data.add("##contig=<ID=chr22,length=51304566>");
		data.add("##contig=<ID=chr21,length=48129895>");
		data.add("##contig=<ID=chr20,length=63025520>");
		data.add("##contig=<ID=chr2,length=243199373>");
		data.add("##contig=<ID=chr19,length=59128983>");
		data.add("##contig=<ID=chr18,length=78077248>");
		data.add("##contig=<ID=chr17,length=81195210>");
		data.add("##contig=<ID=chr16,length=90354753>");
		data.add("##contig=<ID=chr15,length=102531392>");
		data.add("##contig=<ID=chr14,length=107349540>");
		data.add("##contig=<ID=chr13,length=115169878>");
		data.add("##contig=<ID=chr12,length=133851895>");
		data.add("##contig=<ID=chr11,length=135006516>");
		data.add("##contig=<ID=chr10,length=135534747>");
		data.add("##contig=<ID=chr1,length=249250621>");
		data.add("##contig=<ID=GL000249.1,length=38502>");
		data.add("##contig=<ID=GL000248.1,length=39786>");
		data.add("##contig=<ID=GL000247.1,length=36422>");
		data.add("##contig=<ID=GL000246.1,length=38154>");
		data.add("##contig=<ID=GL000245.1,length=36651>");
		data.add("##contig=<ID=GL000244.1,length=39929>");
		data.add("##contig=<ID=GL000243.1,length=43341>");
		data.add("##contig=<ID=GL000242.1,length=43523>");
		data.add("##contig=<ID=GL000241.1,length=42152>");
		data.add("##contig=<ID=GL000240.1,length=41933>");
		data.add("##contig=<ID=GL000239.1,length=33824>");
		data.add("##contig=<ID=GL000238.1,length=39939>");
		data.add("##contig=<ID=GL000237.1,length=45867>");
		data.add("##contig=<ID=GL000236.1,length=41934>");
		data.add("##contig=<ID=GL000235.1,length=34474>");
		data.add("##contig=<ID=GL000234.1,length=40531>");
		data.add("##contig=<ID=GL000233.1,length=45941>");
		data.add("##contig=<ID=GL000232.1,length=40652>");
		data.add("##contig=<ID=GL000231.1,length=27386>");
		data.add("##contig=<ID=GL000230.1,length=43691>");
		data.add("##contig=<ID=GL000229.1,length=19913>");
		data.add("##contig=<ID=GL000228.1,length=129120>");
		data.add("##contig=<ID=GL000227.1,length=128374>");
		data.add("##contig=<ID=GL000226.1,length=15008>");
		data.add("##contig=<ID=GL000225.1,length=211173>");
		data.add("##contig=<ID=GL000224.1,length=179693>");
		data.add("##contig=<ID=GL000223.1,length=180455>");
		data.add("##contig=<ID=GL000222.1,length=186861>");
		data.add("##contig=<ID=GL000221.1,length=155397>");
		data.add("##contig=<ID=GL000220.1,length=161802>");
		data.add("##contig=<ID=GL000219.1,length=179198>");
		data.add("##contig=<ID=GL000218.1,length=161147>");
		data.add("##contig=<ID=GL000217.1,length=172149>");
		data.add("##contig=<ID=GL000216.1,length=172294>");
		data.add("##contig=<ID=GL000215.1,length=172545>");
		data.add("##contig=<ID=GL000214.1,length=137718>");
		data.add("##contig=<ID=GL000213.1,length=164239>");
		data.add("##contig=<ID=GL000212.1,length=186858>");
		data.add("##contig=<ID=GL000211.1,length=166566>");
		data.add("##contig=<ID=GL000210.1,length=27682>");
		data.add("##contig=<ID=GL000209.1,length=159169>");
		data.add("##contig=<ID=GL000208.1,length=92689>");
		data.add("##contig=<ID=GL000207.1,length=4262>");
		data.add("##contig=<ID=GL000206.1,length=41001>");
		data.add("##contig=<ID=GL000205.1,length=174588>");
		data.add("##contig=<ID=GL000204.1,length=81310>");
		data.add("##contig=<ID=GL000203.1,length=37498>");
		data.add("##contig=<ID=GL000202.1,length=40103>");
		data.add("##contig=<ID=GL000201.1,length=36148>");
		data.add("##contig=<ID=GL000200.1,length=187035>");
		data.add("##contig=<ID=GL000199.1,length=169874>");
		data.add("##contig=<ID=GL000198.1,length=90085>");
		data.add("##contig=<ID=GL000197.1,length=37175>");
		data.add("##contig=<ID=GL000196.1,length=38914>");
		data.add("##contig=<ID=GL000195.1,length=182896>");
		data.add("##contig=<ID=GL000194.1,length=191469>");
		data.add("##contig=<ID=GL000193.1,length=189789>");
		data.add("##contig=<ID=GL000192.1,length=547496>");
		data.add("##contig=<ID=GL000191.1,length=106433>");
		data.add("##qPG=<ID=6,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:25:33,CL=\"qannotate --mode snpeff -d /mnt/lustre/reference/software/snpEff/GRCh37.75 -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpeff.log\">");
		data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:51,CL=\"qannotate --mode indelConfidence -d /mnt/lustre/reference/genomeinfo/qannotate/indel.repeat.mask -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf.log\">");
		data.add("##qPG=<ID=4,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:35,CL=\"qannotate --mode hom -d /mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom.log\">");
		data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:33,CL=\"qannotate --mode trf -d /mnt/lustre/reference/genomeinfo/qannotate/GRCh37_ICGC_standard_v2.fa.2.7.7.80.10.20.2000_simple.txt -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=1,Tool=q3indel,Version=1.0 (9971),Date=2019-07-26 11:19:21,CL=\"q3indel -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.ini -log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.log [runMode: gatk]\">");
		data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqasim_chr1_70p_T\tqasim_chr1_70p_B");

		VcfHeader header = new VcfHeader();
		for (String s : data) {
			header.addOrReplace(s);
		}
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
		
		//reheader
		VcfHeader outputHeader = MakeValidMode.reheader(header, "unit test", "testInput",  "reference");
		VcfFileMeta newMeta = new VcfFileMeta(outputHeader);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, newMeta.getType());
		 
		 //chr1    251169  .       G       T       .       PASS_1;PASS_2   SOMATIC_1;FLANK=AAAGCTGAATT;SOMATIC_2;IN=1,2;HOM=2,GTTGGAAAGCtGAATTTATCT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||n.263+7848C>A||AP006222.2|lincRNA|NON_CODING|ENST00000424587|2|1)     
		 //GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.      0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","251169",".","G","T",".","PASS_1;PASS_2","SOMATIC_1;FLANK=AAAGCTGAATT;SOMATIC_2;IN=1,2;HOM=2,GTTGGAAAGCtGAATTTATCT;CONF=HIGH_1,HIGH_2;EFF=intron_variant(MODIFIER|||n.263+7848C>A||AP006222.2|lincRNA|NON_CODING|ENST00000424587|2|1)","GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL","0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.","0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176"});
		 
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL", ffList.get(0));
		 assertEquals("0/0&.:G/G&G/G:G19[38.95],14[35.36]&G19[38.95],14[35.36]:33&.:G19[38.95]14[35.36]&G19[38.95]14[35.36]:0&0:0&0:.:.:.", ffList.get(1));
		 assertEquals("0/1&0/1:G/T&G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]&G22[37.45],22[38.09],T11[36.64],16[37.56]:71&71:G22[37.45]22[38.09];T11[36.64]16[37.56]&G22[37.45]22[38.09];T11[36.64]16[37.56]:27&27:25&25:43,28:99:648,0,1176", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.makeValid(vcf1);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:GD:AC:DP:OABS:MR:NNS:AD:GQ", ffListMV.get(0));
		 assertEquals("0/0:G/G:G19[38.95],14[35.36]:33:G19[38.95]14[35.36]:0:0:.:.", ffListMV.get(1));
		 assertEquals("0/1:G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]:71:G22[37.45]22[38.09];T11[36.64]16[37.56]:27:25:43,28:99", ffListMV.get(2));
		 assertEquals(".:G/G:G19[38.95],14[35.36]:.:G19[38.95]14[35.36]:0:0:.:.", ffListMV.get(3));
		 assertEquals("0/1:G/T:G22[37.45],22[38.09],T11[36.64],16[37.56]:71:G22[37.45]22[38.09];T11[36.64]16[37.56]:27:25:43,28:99", ffListMV.get(4));
		 
		 // ccm...
		 Map<String, short[]> callerPositionsMap = newMeta.getCallerSamplePositions();
		 MakeValidMode.addCCM(vcf1, callerPositionsMap);
	 }
	 
	 @Test
	 public void needValidMakeover() {
		 VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileDate=20171117");
		header.addOrReplace("##qTestBamUUID=T");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tT");
		assertEquals(true, MakeValidMode.doesMakeValidNeedToBeRun(header)); 
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tT\tT");
		assertEquals(false, MakeValidMode.doesMakeValidNeedToBeRun(header));
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tT_1\tT_2");
		assertEquals(false, MakeValidMode.doesMakeValidNeedToBeRun(header));
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tN_1\tT_2");
		assertEquals(true, MakeValidMode.doesMakeValidNeedToBeRun(header));
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tN_1\tT_1\tN_2\tT_2");
		assertEquals(false, MakeValidMode.doesMakeValidNeedToBeRun(header));
	 }
	 
	 @Test
	 public void reheader2() {
		 VcfHeader header = new VcfHeader();
		 header.addOrReplace("##1:qControlSample=5b516b78-78a0-4562-8be6-60875d2abac3");
		 header.addOrReplace("##1:qTestSample=e2d4e67a-92d7-405e-b055-f3053c81f079");
		 header.addOrReplace("##1:qControlBamUUID=b3d0f8ad-4ffa-4b90-835b-8d4f9c11f32d");
		 header.addOrReplace("##1:qTestBamUUID=d56d8b14-aa9c-4ac7-955d-707720fcce76");
		 header.addOrReplace("##1:qAnalysisId=754b98fe-5034-403c-a8c0-14292ac1e625");
		 header.addOrReplace("##2:qControlSample=5b516b78-78a0-4562-8be6-60875d2abac3");
		 header.addOrReplace("##2:qTestSample=e2d4e67a-92d7-405e-b055-f3053c81f079");
		header.addOrReplace("##2:qControlBamUUID=b3d0f8ad-4ffa-4b90-835b-8d4f9c11f32d");
		header.addOrReplace("##2:qTestBamUUID=d56d8b14-aa9c-4ac7-955d-707720fcce76");
		header.addOrReplace("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/analysis/7/5/754b98fe-5034-403c-a8c0-14292ac1e625/754b98fe-5034-403c-a8c0-14292ac1e625.vcf");
		header.addOrReplace("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/analysis/c/9/c979fb09-15cc-4c14-ae34-6621c2e3d496/c979fb09-15cc-4c14-ae34-6621c2e3d496.vcf");
		 header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tb3d0f8ad-4ffa-4b90-835b-8d4f9c11f32d\td56d8b14-aa9c-4ac7-955d-707720fcce76");
		 VcfFileMeta meta = new VcfFileMeta(header);
		 assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
		 VcfHeader updatedHeader = MakeValidMode.reheader(header, "test", "mu_test.vcf", null);
		meta = new VcfFileMeta(updatedHeader);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tb3d0f8ad-4ffa-4b90-835b-8d4f9c11f32d_1\td56d8b14-aa9c-4ac7-955d-707720fcce76_1\tb3d0f8ad-4ffa-4b90-835b-8d4f9c11f32d_2\td56d8b14-aa9c-4ac7-955d-707720fcce76_2", updatedHeader.getChrom().toString());
	 }
	 
	 @Test
	 public void rehead() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileDate=20171117");
		header.addOrReplace("##1:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##1:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##2:qUUID=36419b4c-8bd6-4383-9ac0-bb2d8e243ae0");
		header.addOrReplace("##2:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##2:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/3/d/3d4ecf27-fc71-4853-ade6-4451a3771c7a/3d4ecf27-fc71-4853-ade6-4451a3771c7a.vcf.gz");
		header.addOrReplace("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/f/a/fa34f968-5fe3-4bff-a54f-c08813091e77/fa34f968-5fe3-4bff-a54f-c08813091e77.vcf.gz");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0f443106-e17d-4200-87ec-bd66fe91195f");
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_SINGLE_SAMPLE, meta.getType());
		
		VcfHeader updatedHeader = MakeValidMode.reheader(header, "test", "mu_test.vcf", null);
		meta = new VcfFileMeta(updatedHeader);
		assertEquals(ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE, meta.getType());
		assertEquals("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0f443106-e17d-4200-87ec-bd66fe91195f_1\t0f443106-e17d-4200-87ec-bd66fe91195f_2", updatedHeader.getChrom().toString());
	 }
	  			
}
