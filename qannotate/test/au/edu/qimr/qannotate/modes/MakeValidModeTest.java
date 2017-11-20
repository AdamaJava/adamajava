package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

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
		VcfRecord vcf1 = new VcfRecord(new String[]{"chr1","823538","rs375868960","G","T","63.77","MR;NNS","SOMATIC;IN=2;DB;GERM=60,185;HOM=0,TCTGGGCCTAtTCCTTCCTTT;CONF=ZERO","GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS",".:.:.:.:.:G/G:G5[39.4],13[39.62]:.:0:0","0/1:53,6:59:92:92,0,2176:G/T:G11[37.82],31[40.45]:.:0:0"});
		 Map<String, short[]> positions = new HashMap<>();
		 positions.put("1", new short[]{1,2});
		 positions.put("2", new short[]{3,4});
		 /*
		  * before
		  */
		 List<String> ffList = vcf1.getFormatFields();
		 assertEquals(3, ffList.size());
		 assertEquals("GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS", ffList.get(0));
		 assertEquals(".:.:.:.:.:G/G:G5[39.4],13[39.62]:.:0:0", ffList.get(1));
		 assertEquals("0/1:53,6:59:92:92,0,2176:G/T:G11[37.82],31[40.45]:.:0:0", ffList.get(2));
		 
		 /*
		  * after
		  */
		 MakeValidMode.processVcfRecord(vcf1, positions);
		 List<String> ffListMV = vcf1.getFormatFields();
		 assertEquals(5, ffListMV.size());
		 assertEquals("GT:AC:AD:CCC:CCM:DP:FT:GD:GQ:INF:MR:NNS:OABS", ffListMV.get(0));
		 assertEquals(".:.:.:.:1:.:.:.:.:.:.:.:.", ffListMV.get(1));
		 assertEquals(".:.:.:.:1:.:.:.:.:.:.:.:.", ffListMV.get(2));
		 assertEquals(".:G5[39.4],13[39.62]:.:.:3:.:.:G/G:.:.:0:0:.", ffListMV.get(3));
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
		assertEquals(".:.:.:.:.:.:.:.:.:.:.", ffListMV.get(1));
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
		assertEquals(".:.:.:.:.:.:.:.:.:.", ffListMV.get(2));
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
	 public void rehead() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileDate=20171117");
		header.addOrReplace("##1:qUUID=1e7f8938-7965-4cfc-85c9-f3197fa150d3");
		header.addOrReplace("##1:qSource=qSNP v2.0 (2269)");
		header.addOrReplace("##1:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
		header.addOrReplace("##1:qControlSample=null");
		header.addOrReplace("##1:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##1:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##1:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##1:qAnalysisId=3d4ecf27-fc71-4853-ade6-4451a3771c7a");
		header.addOrReplace("##2:qUUID=36419b4c-8bd6-4383-9ac0-bb2d8e243ae0");
		header.addOrReplace("##2:qSource=qSNP v2.0 (2269)");
		header.addOrReplace("##2:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
		header.addOrReplace("##2:qControlSample=null");
		header.addOrReplace("##2:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##2:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##2:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##2:qAnalysisId=fa34f968-5fe3-4bff-a54f-c08813091e77");
		header.addOrReplace("##2:qTestVcf=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/2/0/20b6e966-9b36-4bf5-b598-0da638e6e6dd/testGatkHCCV.vcf.gz");
		header.addOrReplace("##2:qTestVcfUUID=null");
		header.addOrReplace("##2:qTestVcfGATKVersion=null");
		header.addOrReplace("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/3/d/3d4ecf27-fc71-4853-ade6-4451a3771c7a/3d4ecf27-fc71-4853-ade6-4451a3771c7a.vcf.gz");
		header.addOrReplace("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/f/a/fa34f968-5fe3-4bff-a54f-c08813091e77/fa34f968-5fe3-4bff-a54f-c08813091e77.vcf.gz");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0f443106-e17d-4200-87ec-bd66fe91195f");
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_SINGLE_SAMPLE, meta.getType());
		
		VcfHeader updatedHeader = MakeValidMode.reheader(header, "test", "mu_test.vcf", null);
		meta = new VcfFileMeta(updatedHeader);
		assertEquals(ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE, meta.getType());
	 }
	  			
}
