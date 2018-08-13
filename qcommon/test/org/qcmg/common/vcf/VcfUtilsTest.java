package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class VcfUtilsTest {
	
	@Test
	public void getAltFrequencyTest() throws Exception{
		
        //"chrY\t14923588\t.\tG\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC\tGT:GD:AC\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]"); 
        //"chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1");

		String str = "chrY\t14923588\t.\tG\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC\tGT:GD:AC\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]" ; 
		VcfRecord  vcf  = new VcfRecord(str.split("\t"));				
		VcfFormatFieldRecord format = vcf.getSampleFormatRecord(1);
		
		//debug		
		format = new VcfFormatFieldRecord(vcf.getFormatFields().get(0), vcf.getFormatFields().get(1));	 
		
		int count = VcfUtils.getAltFrequency(format, null);
		assertEquals(count,35);
		
		count = VcfUtils.getAltFrequency(format, "G");
		assertEquals(count,20);
		
		count = VcfUtils.getAltFrequency(format, "W");
		assertEquals(count,0);
		
		count = VcfUtils.getAltFrequency(format, "");
		assertEquals(count,0);
		

		//test coumpound snp
		str =  "chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1" ;
		vcf  = new VcfRecord(str.split("\t"));
		format = new VcfFormatFieldRecord(vcf.getFormatFields().get(0), vcf.getFormatFields().get(2));
		count = VcfUtils.getAltFrequency(format, "TCT");
		assertEquals(count,4);
		
		count = VcfUtils.getAltFrequency(format, null);
		assertEquals(count,106);
		count = VcfUtils.getAltFrequency(format, "_CA");
		assertEquals(count,3);		;
	}
	
	@Test
	public void getAlleles() {
		 String gt = "0/0";
		 String ref = "R";
		 String alt = "A";
		 List<String> alleles = VcfUtils.getAlleles(gt, ref, alt);
		 assertEquals(2, alleles.size());
		 assertEquals("R", alleles.get(0));
		 assertEquals("R", alleles.get(1));
		 
		 gt = "0/1";
		 alleles = VcfUtils.getAlleles(gt, ref, alt);
		 assertEquals(2, alleles.size());
		 assertEquals("R", alleles.get(0));
		 assertEquals("A", alleles.get(1));
		 
		 gt = "1/1";
		 alleles = VcfUtils.getAlleles(gt, ref, alt);
		 assertEquals(2, alleles.size());
		 assertEquals("A", alleles.get(0));
		 assertEquals("A", alleles.get(1));
		 
		 alt = "A,B";
		 alleles = VcfUtils.getAlleles(gt, ref, alt);
		 assertEquals(2, alleles.size());
		 assertEquals("A", alleles.get(0));
		 assertEquals("A", alleles.get(1));
		 
		 gt = "1/2";
		 alleles = VcfUtils.getAlleles(gt, ref, alt);
		 assertEquals(2, alleles.size());
		 assertEquals("A", alleles.get(0));
		 assertEquals("B", alleles.get(1));
	}
	
	@Test
	public void decomposeOABS() {
		String oabs = "A0[0]33[35.73];G6[30.5]2[34]";
		Map<String, Integer> m = VcfUtils.getAllelicCoverage(oabs);
		assertEquals(2, m.size());
		assertEquals(33, m.get("A").intValue());
		assertEquals(8, m.get("G").intValue());
		
		oabs = "AB10[0]33[35.73];GH26[30.5]12[34]";
		m = VcfUtils.getAllelicCoverage(oabs);
		assertEquals(2, m.size());
		assertEquals(43, m.get("AB").intValue());
		assertEquals(38, m.get("GH").intValue());
	}
	
	@Test
	public void decomposeEOR() {
		String oabs = "A0[]33[];G6[]2[]";
		Map<String, Integer> m = VcfUtils.getAllelicCoverage(oabs);
		assertEquals(2, m.size());
		assertEquals(33, m.get("A").intValue());
		assertEquals(8, m.get("G").intValue());
		
		oabs = "AB10[]33[];GH26[]12[]";
		m = VcfUtils.getAllelicCoverage(oabs);
		assertEquals(2, m.size());
		assertEquals(43, m.get("AB").intValue());
		assertEquals(38, m.get("GH").intValue());
	}
	
	@Test
	public void decomposeEORWithStrand() {
		String eor = "A0[]33[];G6[]2[]";
		Map<String, int[]> m = VcfUtils.getAllelicCoverageWithStrand(eor);
		assertEquals(2, m.size());
		assertArrayEquals(new int[]{0,33}, m.get("A"));
		assertArrayEquals(new int[]{6,2}, m.get("G"));
		
		eor = "AB10[]33[];GH26[]12[]";
		m = VcfUtils.getAllelicCoverageWithStrand(eor);
		assertEquals(2, m.size());
		assertArrayEquals(new int[]{10,33}, m.get("AB"));
		assertArrayEquals(new int[]{26,12}, m.get("GH"));
	}
	
	@Test
	public void mergeGATKControlOnly() {
		String s = "chr1\t13550\t.\tG\tA\t367.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.943;ClippingRankSum=0.411;DP=25;FS=36.217;MLEAC=1;MLEAF=0.500;MQ=27.62;MQRankSum=3.312;QD=14.71;ReadPosRankSum=1.670;SOR=3.894\tGT:AD:DP:GQ:PL\t0/1:11,14:25:99:396,0,223";
		VcfRecord  vcf  = new VcfRecord(s.split("\t"));
		Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(vcf, null);
		assertEquals(true, oMergedVcf.isPresent());
		VcfRecord mergedVcf = oMergedVcf.get();
		assertEquals(3, mergedVcf.getFormatFields().size());
		assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(2));
	}
	
	@Test
	public void mergeGATKControlOnlyRealLife() {
		String s = "chr1\t881627\trs2272757\tG\tA\t164.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-2.799;ClippingRankSum=1.555;DB;DP=18;FS=7.375;MLEAC=1;MLEAF=0.500;MQ=59.94;MQ0=0;MQRankSum=0.400;QD=9.15;ReadPosRankSum=0.755;SOR=0.044\tGT:AD:DP:GQ:PL\t0/1:10,8:18:99:193,0,370";
		VcfRecord  vcf  = new VcfRecord(s.split("\t"));
		Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(vcf, null);
		assertEquals(true, oMergedVcf.isPresent());
		VcfRecord mergedVcf = oMergedVcf.get();
		assertEquals(3, mergedVcf.getFormatFields().size());
		assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(2));
		assertEquals(true, mergedVcf.getFormatFields().get(1).contains("0/1"));
	}
	
	@Test
	public void mergeGATKTestOnly() {
		String s = "chr1\t13550\t.\tG\tA\t367.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=1.943;ClippingRankSum=0.411;DP=25;FS=36.217;MLEAC=1;MLEAF=0.500;MQ=27.62;MQRankSum=3.312;QD=14.71;ReadPosRankSum=1.670;SOR=3.894\tGT:AD:DP:GQ:PL\t0/1:11,14:25:99:396,0,223";
		VcfRecord  vcf  = new VcfRecord(s.split("\t"));
		Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(null, vcf);
		assertEquals(true, oMergedVcf.isPresent());
		VcfRecord mergedVcf = oMergedVcf.get();
		assertEquals(3, mergedVcf.getFormatFields().size());
		assertEquals("./.:.:.:.:.", mergedVcf.getFormatFields().get(1));
	}
	
	@Test
	public void mergeGATKSameAlts() {
		String s = "chr1\t13838\t.\tC\tT\t49.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124\tGT:AD:DP:GQ:PL\t0/1:10,3:13:78:78,0,370";
		VcfRecord  cVcf  = new VcfRecord(s.split("\t"));
		s = "chr1\t13838\t.\tC\tT\t157.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.817;ClippingRankSum=-0.696;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=26.49;MQRankSum=-1.545;QD=12.14;ReadPosRankSum=0.000;SOR=1.179\tGT:AD:DP:GQ:PL\t0/1:8,5:13:99:186,0,330";
		VcfRecord  tVcf  = new VcfRecord(s.split("\t"));
		Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(cVcf, tVcf);
		assertEquals(true, oMergedVcf.isPresent());
		VcfRecord mergedVcf = oMergedVcf.get();
		assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getQualString());
		assertEquals("BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124", mergedVcf.getInfo());
		assertEquals(3, mergedVcf.getFormatFields().size());
		assertEquals("0/1:10,3:13:78:49.77", mergedVcf.getFormatFields().get(1));
		assertEquals("0/1:8,5:13:99:157.77", mergedVcf.getFormatFields().get(2));
		assertEquals(null, mergedVcf.getInfoRecord().getField("AC"));
		assertEquals(null, mergedVcf.getInfoRecord().getField("AF"));
		assertEquals(null, mergedVcf.getInfoRecord().getField("MLEAC"));
		assertEquals(null, mergedVcf.getInfoRecord().getField("MLEAF"));
	}
	
	@Test
	public void mergeGATKDiffAlts() {
		String s = "chr1\t13838\t.\tC\tT\t49.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.085;ClippingRankSum=0.761;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=25.77;MQRankSum=-2.620;QD=3.83;ReadPosRankSum=-1.437;SOR=0.124\tGT:AD:DP:GQ:PL\t0/1:10,3:13:78:78,0,370";
		VcfRecord  cVcf  = new VcfRecord(s.split("\t"));
		s = "chr1\t13838\t.\tC\tG\t157.77\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.817;ClippingRankSum=-0.696;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=26.49;MQRankSum=-1.545;QD=12.14;ReadPosRankSum=0.000;SOR=1.179\tGT:AD:DP:GQ:PL\t0/1:8,5:13:99:186,0,330";
		VcfRecord  tVcf  = new VcfRecord(s.split("\t"));
		Optional<VcfRecord> oMergedVcf = VcfUtils.mergeGATKVcfRecs(cVcf, tVcf);
		assertEquals(true, oMergedVcf.isPresent());
		VcfRecord mergedVcf = oMergedVcf.get();
		assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getQualString());
		assertEquals(Constants.MISSING_DATA_STRING, mergedVcf.getInfo());
		assertEquals(3, mergedVcf.getFormatFields().size());
		assertEquals("0/1:10,3:13:78:49.77", mergedVcf.getFormatFields().get(1));
		assertEquals("0/2:8,5:13:99:157.77", mergedVcf.getFormatFields().get(2));
	}
	
	@Test
	public void getAlleleDistMap() {
		String ac = "A7[41.29],9[38.56],C14[38.43],12[37.83]";
		Map<String, Integer> m = VcfUtils.getAllelicCoverageFromAC(ac);
		assertEquals(2, m.size());
		assertEquals(16, m.get("A").intValue());
		assertEquals(26, m.get("C").intValue());
		
		ac = "A7[],9[],C14[],12[],G1[],10[],T100[],0[]";
		m = VcfUtils.getAllelicCoverageFromAC(ac);
		assertEquals(4, m.size());
		assertEquals(16, m.get("A").intValue());
		assertEquals(26, m.get("C").intValue());
		assertEquals(11, m.get("G").intValue());
		assertEquals(100, m.get("T").intValue());
	}
	
	@Test
	public void createVcf() {
		VcfRecord r = VcfUtils.createVcfRecord(new ChrPointPosition("1",1), "id", "ref", "alt");
		assertEquals("1", r.getChromosome());
		assertEquals(1, r.getPosition());
		assertEquals(1 + r.getRef().length() -1 , r.getChrPosition().getEndPosition());
		assertEquals("id", r.getId());
		assertEquals("ref", r.getRef());
		assertEquals("alt", r.getAlt());
		
		r = VcfUtils.createVcfRecord(new ChrPointPosition("1",1), "id", "A", "alt");
		assertEquals("1", r.getChromosome());
		assertEquals(1, r.getPosition());
		assertEquals(1 + r.getRef().length() -1 , r.getChrPosition().getEndPosition());
		assertEquals("id", r.getId());
		assertEquals("A", r.getRef());
		assertEquals("alt", r.getAlt());
		
		r = VcfUtils.createVcfRecord(new ChrRangePosition("1",1,2), "id", "AA", "alt");
		assertEquals("1", r.getChromosome());
		assertEquals(1, r.getPosition());
		assertEquals(1 + r.getRef().length() -1 , r.getChrPosition().getEndPosition());
		assertEquals("id", r.getId());
		assertEquals("AA", r.getRef());
		assertEquals("alt", r.getAlt());
		
		r = VcfUtils.createVcfRecord(new ChrRangePosition("1",1,2), "id", "AAA", "alt");
		assertEquals("1", r.getChromosome());
		assertEquals(1, r.getPosition());
		assertEquals(1 + r.getRef().length() -1 , r.getChrPosition().getEndPosition());
		assertEquals("id", r.getId());
		assertEquals("AAA", r.getRef());
		assertEquals("alt", r.getAlt());
	}
	
	@Test
	public void mergeAlts() {
		assertEquals("A", VcfUtils.mergeAlts("A", "A"));
		assertEquals("A,B", VcfUtils.mergeAlts("A", "B"));
		assertEquals("A,B", VcfUtils.mergeAlts("A,B", "B"));
		assertEquals("A,B,C", VcfUtils.mergeAlts("A,B,C", "B"));
		assertEquals("A,B,C", VcfUtils.mergeAlts("A,B,C", "B,C"));
		assertEquals("A,B,C", VcfUtils.mergeAlts("A,B", "B,C"));
		assertEquals("A,B,C", VcfUtils.mergeAlts("A", "B,C"));
		assertEquals("A,D,B,C", VcfUtils.mergeAlts("A,D", "B,C"));
		assertEquals("AA", VcfUtils.mergeAlts("AA", "AA"));
		assertEquals("AA,XX", VcfUtils.mergeAlts("AA", "XX"));
		assertEquals("AA,BB,XX", VcfUtils.mergeAlts("AA,BB", "XX"));
		assertEquals("AA,BB,XX", VcfUtils.mergeAlts("AA,BB", "BB,XX"));
		assertEquals("AA,BB,XX", VcfUtils.mergeAlts("AA", "BB,XX"));
		assertEquals("AA,BB,XX", VcfUtils.mergeAlts("AA", "BB,XX"));
	}
	
	@Test
	public void mergeAltsDiffLEngths() {
		assertEquals("AA,A", VcfUtils.mergeAlts("AA", "A"));
		assertEquals("A,AA", VcfUtils.mergeAlts("A", "AA"));
	}
	
	@Test
	public void getOABSDetails() {
		Map<String, int[]> map = VcfUtils.getAllelicCoverageFromOABS("A1[10]0[0]");
		assertEquals(1, map.size());
		assertEquals(1, map.get("A")[0]);
		assertEquals(0, map.get("A")[1]);
		
		map = VcfUtils.getAllelicCoverageFromOABS("A1[10]0[0];B0[0]10[2]");
		assertEquals(2, map.size());
		assertEquals(1, map.get("A")[0]);
		assertEquals(0, map.get("A")[1]);
		assertEquals(0, map.get("B")[0]);
		assertEquals(10, map.get("B")[1]);
		
		map = VcfUtils.getAllelicCoverageFromOABS("A1[10]0[0];B0[0]10[2];C12[44]21[33]");
		assertEquals(3, map.size());
		assertEquals(1, map.get("A")[0]);
		assertEquals(0, map.get("A")[1]);
		assertEquals(0, map.get("B")[0]);
		assertEquals(10, map.get("B")[1]);
		assertEquals(12, map.get("C")[0]);
		assertEquals(21, map.get("C")[1]);
	}
	
	@Test
	public void getUpdatedGT() {
		assertEquals("0/0", VcfUtils.getUpdatedGT("", "", "0/0"));
		assertEquals("0/0", VcfUtils.getUpdatedGT("A", "A", "0/0"));
		assertEquals("0/1", VcfUtils.getUpdatedGT("A", "A", "0/1"));
		assertEquals("1/1", VcfUtils.getUpdatedGT("A", "A", "1/1"));
		assertEquals("1/2", VcfUtils.getUpdatedGT("A", "A", "1/2"));
		assertEquals("0/1", VcfUtils.getUpdatedGT("A,C", "A", "0/1"));
		assertEquals("0/2", VcfUtils.getUpdatedGT("A,C", "C", "0/1"));
		assertEquals("2/2", VcfUtils.getUpdatedGT("A,C", "C", "1/1"));
		assertEquals("1/2", VcfUtils.getUpdatedGT("A,C", "A,C", "1/2"));
		assertEquals("1/1", VcfUtils.getUpdatedGT("A,C", "A", "1/1"));
		assertEquals("1/1", VcfUtils.getUpdatedGT("A,C", "A", "1/1"));
		assertEquals("2/3", VcfUtils.getUpdatedGT("A,C,G", "C,G", "1/2"));
		assertEquals("1/2", VcfUtils.getUpdatedGT("A,C,G", "A,C", "1/2"));
		assertEquals("2/1", VcfUtils.getUpdatedGT("A,C", "C,A", "1/2"));
	}
	
	@Test
	public void getFormatField() {
		assertEquals(null, VcfUtils.getFormatField((List<String>)null, null, 0));
		assertEquals(null, VcfUtils.getFormatField((List<String>)null, "", 0));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList(""), "", 0));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList("s"), "s", 0));
		assertEquals("hello?", VcfUtils.getFormatField(Arrays.asList("s","hello?"), "s", 0));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList("s","hello?"), "s", 1));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList("s","hello?"), "t", 1));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList("s","hello?"), "t", 0));
		assertEquals("there", VcfUtils.getFormatField(Arrays.asList("s:t","hello?:there"), "t", 0));
		assertEquals(null, VcfUtils.getFormatField(Arrays.asList("s:t","hello?:there"), "t", 1));
		assertEquals("again", VcfUtils.getFormatField(Arrays.asList("s:t","hello?:there",":again"), "t", 1));
		assertEquals("", VcfUtils.getFormatField(Arrays.asList("s:t","hello?:there",":again"), "s", 1));
	}
	
	
	@Test
	public void getGEFromGATKVcf() {
		VcfRecord r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","1/1:0,45:45:99:1848,135,0"});
		assertEquals(GenotypeEnum.TT, VcfUtils.getGEFromGATKVCFRec(r));
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","0/0:0,45:45:99:1848,135,0"});
		assertEquals(GenotypeEnum.GG, VcfUtils.getGEFromGATKVCFRec(r));
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","0/1:0,45:45:99:1848,135,0"});
		assertEquals(GenotypeEnum.GT, VcfUtils.getGEFromGATKVCFRec(r));
	}
	
	@Test
	public void getGTData() {
		VcfRecord r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","1/1:0,45:45:99:1848,135,0"});
		assertEquals("1/1", VcfUtils.getGenotypeFromGATKVCFRecord(r));
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","0/1:0,45:45:99:1848,135,0"});
		assertEquals("0/1", VcfUtils.getGenotypeFromGATKVCFRecord(r));
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","0/0:0,45:45:99:1848,135,0"});
		assertEquals("0/0", VcfUtils.getGenotypeFromGATKVCFRecord(r));
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL","1/2:0,45:45:99:1848,135,0"});
		assertEquals("1/2", VcfUtils.getGenotypeFromGATKVCFRecord(r));
	}
	
//	@Test
//	public void idDPPResent() {
//		VcfRecord r = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:GQ:PL","1/1:9:135,9,0"});
//		assertEquals(false, VcfUtils.isDPFormatFieldPresent(r));
//		r = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:DP:GQ:PL","1/1:.:9:135,9,0"});
//		assertEquals(true, VcfUtils.isDPFormatFieldPresent(r));
//		r = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:DP:GQ:PL","1/1:0:9:135,9,0"});
//		assertEquals(true, VcfUtils.isDPFormatFieldPresent(r));
//		r = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:DP:GQ:PL","1/1:1:9:135,9,0"});
//		assertEquals(true, VcfUtils.isDPFormatFieldPresent(r));
//	}
	
	@Test
	public void getGTDataDuffRealLifeData() {
		VcfRecord r = new VcfRecord(new String[]{"GL000222.1","80278",".","C","A","427.77",".","AC=1;AF=0.500;AN=2;BaseQRankSum=0GL000222.1","80426",".","C","A","579.77",".","AC=1;AF=0.500;AN=2;BaseQRankSum=1.845;ClippingRankSum=0.329;DP=81;FS=46.164;MLEAC=1;MLEAF=0.500;MQ=35.11;MQ0=0;MQRankSum=4.453;QD=7.16;ReadPosRankSum=1.477;SOR=5.994","GT:AD:DP:GQ:PL","0/1:62,19:81:99:608,0,2576"});
		try {
			VcfUtils.getGenotypeFromGATKVCFRecord(r);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT:AD:DP:GQ:PL",""});
		try {
			VcfUtils.getGenotypeFromGATKVCFRecord(r);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT","0/"});
		try {
			VcfUtils.getGenotypeFromGATKVCFRecord(r);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GG","0/1"});
		try {
			VcfUtils.getGenotypeFromGATKVCFRecord(r);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		r = new VcfRecord(new String[]{"GL000192.1","228788",".","G","T","1819.77",".","AC=2;AF=1.00;AN=2;DP=45;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=53.11;MQ0=0;QD=31.96;SOR=1.038","GT","0/1"});
		assertEquals("0/1",VcfUtils.getGenotypeFromGATKVCFRecord(r));
	}
	
	@Test
	public void isCS() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		assertEquals(false, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("",""));
		assertEquals(false, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("ABCD","1"));
		assertEquals(false, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("ACDC","1"));
		assertEquals(false, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("ACCR","1"));
		assertEquals(false, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("ACCS","1"));
		assertEquals(true, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("ACCS:GT","1:1"));
		assertEquals(true, VcfUtils.isCompoundSnp(rec));
		rec.setFormatFields(Arrays.asList("GT:ACCS:GD","0:1:1"));
		assertEquals(true, VcfUtils.isCompoundSnp(rec));
	}
	
	@Test
	public void getConfidence() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		assertNull(VcfUtils.getConfidence(rec));
		rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ +  MafConfidence.LOW.toString());
		assertEquals("LOW", VcfUtils.getConfidence(rec));
		rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ +  MafConfidence.HIGH.toString());
		assertEquals("HIGH", VcfUtils.getConfidence(rec));
		rec.setInfo(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ +  MafConfidence.ZERO.toString());
		assertEquals("ZERO", VcfUtils.getConfidence(rec));
	}
	@Test
	public void getConfidenceMergedRec() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		assertNull(VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=HIGH_1,ZERO_2");
		assertEquals("HIGH_1,ZERO_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=HIGH_1,HIGH_2");
		assertEquals("HIGH_1,HIGH_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=LOW_1,HIGH_2");
		assertEquals("LOW_1,HIGH_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=ZERO_1,HIGH_2");
		assertEquals("ZERO_1,HIGH_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=LOW_1,LOW_2");
		assertEquals("LOW_1,LOW_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=ZERO_1,LOW_2");
		assertEquals("ZERO_1,LOW_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=LOW_1,ZERO_2");
		assertEquals("LOW_1,ZERO_2", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=LOW_2,ZERO_1");
		assertEquals("LOW_2,ZERO_1", VcfUtils.getConfidence(rec));
		rec.setInfo("CONF=ZERO_2,ZERO_1");
		assertEquals("ZERO_2,ZERO_1", VcfUtils.getConfidence(rec));
	}
	
	
	@Test
	public void getMapOfFFs() {
		String ff = "AB:CD:EF\t.:.:.";
		Map<String, String[]> m = VcfUtils.getFormatFieldsAsMap(ff);
		assertEquals(3, m.size());
		assertEquals(1, m.get("AB").length);
		assertEquals(1, m.get("CD").length);
		assertEquals(1, m.get("EF").length);
		assertEquals(".", m.get("AB")[0]);
		assertEquals(".", m.get("CD")[0]);
		assertEquals(".", m.get("EF")[0]);
		
		 ff = "AB:CD:EF\t.:.:.\tThis:is:thebest";
		m = VcfUtils.getFormatFieldsAsMap(ff);
		assertEquals(3, m.size());
		assertEquals(2, m.get("AB").length);
		assertEquals(2, m.get("CD").length);
		assertEquals(2, m.get("EF").length);
		assertEquals(".", m.get("AB")[0]);
		assertEquals(".", m.get("CD")[0]);
		assertEquals(".", m.get("EF")[0]);
		assertEquals("This", m.get("AB")[1]);
		assertEquals("is", m.get("CD")[1]);
		assertEquals("thebest", m.get("EF")[1]);
	}
	
	
	@Test
	public void mapToList() {
		Map<String, String[]> map = new HashMap<>();
		
		map.put("AB", new String[]{".","This"});
		map.put("CD", new String[]{".","is"});
		map.put("EF", new String[]{".","thebest"});
		
		List<String> l = VcfUtils.convertFFMapToList(map);
		assertEquals(3, l.size());
		assertEquals("AB:CD:EF\t.:.:.\tThis:is:thebest", l.stream().collect(Collectors.joining(Constants.TAB_STRING)));
	}
	
	@Test
	public void mapToListWithGT() {
		Map<String, String[]> map = new HashMap<>();
		
		map.put("AB", new String[]{".","This"});
		map.put("CD", new String[]{".","is"});
		map.put("GT", new String[]{".","REALLY"});
		map.put("EF", new String[]{".","thebest"});
		
		List<String> l = VcfUtils.convertFFMapToList(map);
		assertEquals(3, l.size());
		assertEquals("GT:AB:CD:EF\t.:.:.:.\tREALLY:This:is:thebest", l.stream().collect(Collectors.joining(Constants.TAB_STRING)));
	}
	
	@Test
	public void isRecordSomatic() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		assertEquals(false, VcfUtils.isRecordSomatic(rec));
		
		rec.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1");
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1,2");
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		
		rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1,2");
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		
		rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2;SOMATIC");
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		rec.setInfo("FLANK=ACCCTGGAAGA;IN=1,2");
		assertEquals(false, VcfUtils.isRecordSomatic(rec));
		
		/*
		 * needs to be set in ff
		 */
		rec.setFormatFields(Arrays.asList("INF",".","."));
		assertEquals(false, VcfUtils.isRecordSomatic(rec));
		rec.setFormatFields(Arrays.asList("INF",".","SOMATIC"));
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		rec.setFormatFields(Arrays.asList("INF",".","SOMATIC",".","."));
		assertEquals(false, VcfUtils.isRecordSomatic(rec));
		rec.setFormatFields(Arrays.asList("INF",".","SOMATIC",".","SOMATIC"));
		assertEquals(true, VcfUtils.isRecordSomatic(rec));
		rec.setFormatFields(Arrays.asList("INF",".","SOMATIC",".","SOMATIC","."));
		assertEquals(false, VcfUtils.isRecordSomatic(rec));
		
	}
	
	@Test
	public void hasRecordBeenMerged() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		
		rec.setInfo(".");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("SOMATIC");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("THIS_SHOULD_BE_FALSE_IN=1,2");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("THIS_SHOULD_BE_TRUE;IN=1,2");
		assertEquals(true, VcfUtils.isMergedRecord(rec));
		rec.setInfo("IN=1");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("SOMATIC;FLANK=ACCCTGGAAGA;IN=1");
		assertEquals(false, VcfUtils.isMergedRecord(rec));
		rec.setInfo("FLANK=TGTCCATTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.212;ClippingRankSum=1.855;DP=13;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=21.57;MQ0=0;MQRankSum=-0.533;QD=10.83;ReadPosRankSum=0.696;SOR=1.402;IN=1,2");
		assertEquals(true, VcfUtils.isMergedRecord(rec));
	}
	
	@Test
	public void getFiltersWithSuffix() {
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
		rec.setFilter(".");
		assertEquals("", VcfUtils.getFiltersEndingInSuffix(rec, "_1"));
		assertEquals(".", VcfUtils.getFiltersEndingInSuffix(rec, "."));
		rec.setFilter("PASS_1;PASS_2");
		assertEquals("PASS_1", VcfUtils.getFiltersEndingInSuffix(rec, "_1"));
		assertEquals("PASS_2", VcfUtils.getFiltersEndingInSuffix(rec, "_2"));
		
	}
	
	@Test
	public void testMissingDataInFormatField() {
		VcfRecord r = new VcfRecord(new String[]{"chr1","52924633","rs12072217","C","T","671.77","NCIT","AC=1;AF=0.500;AN=2;BaseQRankSum=0.655;ClippingRankSum=-1.179;DB;DP=33;FS=1.363;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-1.067;QD=20.36;ReadPosRankSum=-0.655;SOR=0.990","GT:AD:DP:GQ:PL","0/1:12,21:33:99:700,0,339"});
		VcfUtils.addMissingDataToFormatFields(r, 2);
		assertEquals(3, r.getFormatFields().size());
		assertEquals("0/1:12,21:33:99:700,0,339", r.getFormatFields().get(1));
		assertEquals("./.:.:.:.:.", r.getFormatFields().get(2));
	}
	@Test
	public void controlMissingDataInFormatField() {
		VcfRecord r = new VcfRecord(new String[]{"chr1","52924633","rs12072217","C","T","671.77","NCIT","AC=1;AF=0.500;AN=2;BaseQRankSum=0.655;ClippingRankSum=-1.179;DB;DP=33;FS=1.363;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-1.067;QD=20.36;ReadPosRankSum=-0.655;SOR=0.990","GT:AD:DP:GQ:PL","0/1:12,21:33:99:700,0,339"});
		VcfUtils.addMissingDataToFormatFields(r, 1);
		assertEquals(3, r.getFormatFields().size());
		assertEquals("0/1:12,21:33:99:700,0,339", r.getFormatFields().get(2));
		assertEquals("./.:.:.:.:.", r.getFormatFields().get(1));
	}
	
	@Test
	public void missingDataToFormatField() {
		try {
			VcfUtils.addMissingDataToFormatFields(null, 0);
			Assert.fail("Should have thrown an illegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		VcfRecord rec =  new VcfRecord( new String[] {"1","1",".","A","."});
				
				//VcfUtils.createVcfRecord("1", 1, "A");
		VcfUtils.addMissingDataToFormatFields(rec, 1);
		assertEquals(0, rec.getFormatFields().size());
		
		// add in an empty list for the ff
		List<String> ff = new ArrayList<>();
		ff.add("header info here");
		ff.add("first bit of data");
		rec.setFormatFields(ff);
		
		try {
			VcfUtils.addMissingDataToFormatFields(rec, 0);
			Assert.fail("Should have thrown an illegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		try {
			VcfUtils.addMissingDataToFormatFields(rec, 10);
			Assert.fail("Should have thrown an illegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		VcfUtils.addMissingDataToFormatFields(rec, 1);
		ff = rec.getFormatFields();
		assertEquals(3, ff.size());
		assertEquals("header info here", ff.get(0));
		assertEquals(".", ff.get(1));
		assertEquals("first bit of data", ff.get(2));
		
		VcfUtils.addMissingDataToFormatFields(rec, 3);
		ff = rec.getFormatFields();
		assertEquals(4, ff.size());
		assertEquals("header info here", ff.get(0));
		assertEquals(".", ff.get(1));
		assertEquals("first bit of data", ff.get(2));
		assertEquals(".", ff.get(3));
		
		VcfUtils.addMissingDataToFormatFields(rec, 1);
		ff = rec.getFormatFields();
		assertEquals(5, ff.size());
		assertEquals("header info here", ff.get(0));
		assertEquals(".", ff.get(1));
		assertEquals(".", ff.get(2));
		assertEquals("first bit of data", ff.get(3));
		assertEquals(".", ff.get(4));
	}
	
	@Test
	public void missingDataAgain() {
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","A","."});//VcfUtils.createVcfRecord("1", 1, "A");
		VcfUtils.addMissingDataToFormatFields(rec, 1);
		assertEquals(0, rec.getFormatFields().size());
		
		// add in an empty list for the ff
		List<String> ff = new ArrayList<>();
		ff.add("AC:DC:12:3");
		ff.add("0/1:1/1:45,45,:xyz");
		rec.setFormatFields(ff);
		
		VcfUtils.addMissingDataToFormatFields(rec, 1);
		ff = rec.getFormatFields();
		assertEquals(3, ff.size());
		assertEquals("AC:DC:12:3", ff.get(0));
		assertEquals(".:.:.:.", ff.get(1));
	}
	
	@Test
	public void testCalculateGenotypeEnum() {
		
		assertEquals(null, VcfUtils.calculateGenotypeEnum(null, '\u0000', '\u0000'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("", '\u0000', '\u0000'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("", 'X', 'Y'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("0/1", 'X', 'Y'));
		
		assertEquals(GenotypeEnum.AA, VcfUtils.calculateGenotypeEnum("0/0", 'A', 'C'));
		assertEquals(GenotypeEnum.CC, VcfUtils.calculateGenotypeEnum("1/1", 'A', 'C'));
		assertEquals(GenotypeEnum.AC, VcfUtils.calculateGenotypeEnum("0/1", 'A', 'C'));
		
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/0", 'G', 'G'));
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/1", 'G', 'G'));
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("1/1", 'G', 'G'));
		
	}
	
	@Test
	public void testGetPileupElementAsString() {
		assertEquals("FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0", VcfUtils.getPileupElementAsString(null, false));
		assertEquals("NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0", VcfUtils.getPileupElementAsString(null, true));
		final List<PileupElement> pileups = new ArrayList<PileupElement>();
		final PileupElement pA = new PileupElement('A');
		pA.incrementForwardCount();
		final PileupElement pC = new PileupElement('C');
		pC.incrementForwardCount();
		pileups.add(pA);
		assertEquals("NOVELCOV=A:1,C:0,G:0,T:0,N:0,TOTAL:1", VcfUtils.getPileupElementAsString(pileups, true));
		pileups.add(pC);
		assertEquals("NOVELCOV=A:1,C:1,G:0,T:0,N:0,TOTAL:2", VcfUtils.getPileupElementAsString(pileups, true));
		assertEquals("FULLCOV=A:1,C:1,G:0,T:0,N:0,TOTAL:2", VcfUtils.getPileupElementAsString(pileups, false));
	}
	
//	@Test
//	public void testGetMutationAndGTsRealLife() {
//		assertArrayEquals(new String[] {"C", "0/1","0/1"} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.CG, GenotypeEnum.CG));
//		assertArrayEquals(new String[] {"C", "1/1","1/1"} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.CC, GenotypeEnum.CC));
//	}
	
	
	@Test
	public void getGTStringWithCommas() {
		assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas(null, '\u0000', null));
		assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas("", '\u0000', null));
		assertEquals("./.", VcfUtils.getGTStringWhenAltHasCommas("A", 'C', null));
		assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A", 'C', GenotypeEnum.CC));
		assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.CC));
		assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'T', GenotypeEnum.TT));
		assertEquals("0/1", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.AC));
		assertEquals("1/2", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.AG));
		assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G", 'C', GenotypeEnum.CG));
		assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CG));
		assertEquals("1/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AT));
		assertEquals("0/1", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AC));
		assertEquals("1/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AG));
		assertEquals("1/1", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.AA));
		assertEquals("0/0", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CC));
		assertEquals("0/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CG));
		assertEquals("0/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.CT));
		assertEquals("2/2", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.GG));
		assertEquals("2/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.GT));
		assertEquals("3/3", VcfUtils.getGTStringWhenAltHasCommas("A,G,T", 'C', GenotypeEnum.TT));
	}
	
	@Test
	public void updateAltString() {
		assertEquals("C", VcfUtils.getUpdateAltString("CT", "CT", "C"));
	}
	@Test
	public void updateAltStringRealData() {
		assertEquals("CTT", VcfUtils.getUpdateAltString("CTTT", "CT", "C"));
		assertEquals("CT", VcfUtils.getUpdateAltString("CTTT", "CTT", "C"));
		assertEquals("CTTTT", VcfUtils.getUpdateAltString("CTTT", "C", "CT"));
		assertEquals("C", VcfUtils.getUpdateAltString("CTTT", "CTTT", "C"));
		
		assertEquals("T", VcfUtils.getUpdateAltString("TAAA", "TAAA", "T"));
		assertEquals("TAA", VcfUtils.getUpdateAltString("TAAA", "TA", "T"));
		assertEquals("TAAAA", VcfUtils.getUpdateAltString("TAAA", "T", "TA"));
		
		assertEquals("A", VcfUtils.getUpdateAltString("ATTTG", "ATTTG", "A"));
		assertEquals("TTTTG", VcfUtils.getUpdateAltString("ATTTG", "A", "T"));
		assertEquals("CTTTG", VcfUtils.getUpdateAltString("ATTTG", "A", "C"));
		
		/*
		 * r7	151921032	.	CAT	C	.	.	END=151921034	
09:52:23.555 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTT	C	.	.	END=151921036	
09:52:23.555 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTTG	C	.	.	END=151921037	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATTTGT	C	.	.	END=151921038	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CATT	C	.	.	END=151921035	
09:52:23.556 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - vcf: chr7	151921032	.	CA	C	.	.	END=151921033	
		 */
		assertEquals("CGT", VcfUtils.getUpdateAltString("CATTTGT", "CATTT", "C"));
		assertEquals("CT", VcfUtils.getUpdateAltString("CATTTGT", "CATTTG", "C"));
		assertEquals("C", VcfUtils.getUpdateAltString("CATTTGT", "CATTTGT", "C"));
		assertEquals("CTGT", VcfUtils.getUpdateAltString("CATTTGT", "CATT", "C"));
		assertEquals("CTTTGT", VcfUtils.getUpdateAltString("CATTTGT", "CA", "C"));
		assertEquals("CTTGT", VcfUtils.getUpdateAltString("CATTTGT", "CAT", "C"));
	}
	
	
	
	@Test
	public void isRecordAMnp() {
		
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","A",null});
		//		VcfUtils.createVcfRecord("1", 1, "A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = VcfUtils.resetAllel(rec, "A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = new VcfRecord( new String[] {"1","1",".","AC", null});
				 //VcfUtils.createVcfRecord("1", 1, "AC");
		rec = VcfUtils.resetAllel(rec,"A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = VcfUtils.resetAllel(rec,"ACG");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = new VcfRecord( new String[] {"1","1",".","G", null});
				//VcfUtils.createVcfRecord("1", 1, "G");
		rec = VcfUtils.resetAllel(rec,"G");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));		// ref == alt
		rec = new VcfRecord( new String[] {"1","1",".","CG", null});
				//VcfUtils.createVcfRecord("1", 1, "CG");
		rec = VcfUtils.resetAllel(rec,"GA");
		assertEquals(true, VcfUtils.isRecordAMnp(rec));
		
		rec = new VcfRecord( new String[] {"1","1",".","CGTTT", null});
				//VcfUtils.createVcfRecord("1", 1, "CGTTT");
		rec = VcfUtils.resetAllel(rec,"GANNN");
		assertEquals(true, VcfUtils.isRecordAMnp(rec));
	}
	@Test
	public void isRecordAMnpCheckIndels() {
		
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC",null});
				//VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = VcfUtils.resetAllel(rec,"A,AACCACC");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
	}
	
	@Test
	public void isRecordASnpOrMnp() {
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","A","G"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","A","C,G"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","A","C,G,T"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","AC","CG,GA,TT"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","AC","TT"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","ACGT","TGCA"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		rec = new VcfRecord( new String[] {"1","1",".","ACGT","TGCA,VVVV"});
		assertEquals(true, VcfUtils.isRecordASnpOrMnp(rec));
		
		/*
		 * and now the nays
		 */
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","A","TG"})));
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","AG","C"})));
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","A","C,CT"})));
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","A","CG,T"})));
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","AG","C,GT"})));
		assertEquals(false, VcfUtils.isRecordASnpOrMnp(new VcfRecord( new String[] {"1","1",".","AG","CC,GT,T"})));
	}
	
	@Test
	public void testAdditionalSampleFF() {
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC","."});
				//VcfUtils.createVcfRecord("1", 1, "");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));
		
		// now add another sample with the same ffs
		VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:6,3:9:62:62,0,150"));
		
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));
		assertEquals("1/1:6,3:9:62:62,0,150", rec.getFormatFields().get(2));
		
		// and now one a sample with some extra info
		VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL:OH", "1/1:6,3:9:62:62,0,150:blah"));
		assertEquals("GT:AD:DP:GQ:OH:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:.:62,0,150", rec.getFormatFields().get(1));
		assertEquals("1/1:6,3:9:62:.:62,0,150", rec.getFormatFields().get(2));
		assertEquals("1/1:6,3:9:62:blah:62,0,150", rec.getFormatFields().get(3));
		
		// start afresh
		 rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC","."});
				 //VcfUtils.createVcfRecord("1", 1, "");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("AB:DP:OH", "anythinghere:0:blah"));
		assertEquals("GT:AB:AD:DP:GQ:OH:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:.:6,3:9:62:.:62,0,150", rec.getFormatFields().get(1));
		assertEquals("./.:anythinghere:.:0:.:blah:.", rec.getFormatFields().get(2));
	}
	
	
	@Test
	public void additionalSamplesRealLife() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1","3418618",".","G","A",".",".","FLANK=CCGGCACCTCC;DB;DP=3;FS=0.000;MQ=60.00;MQ0=0;QD=32.74;SOR=2.833;SOMATIC;IN=1,2"
				,"GT:DP:FT:MR:NNS:OABS:AD:GQ:INF"
				,"0/1:20:.:14:14:A3[23]11[24.18];G2[35]4[32]:.:.:."
				,"0/0:7:COVT:.:.:A0[0]2[23.5];G2[36]3[35]:.:.:."
				,"0/0:6:MIN:A3[23]11[24.18];G2[35]4[32]:.:.:SOMATIC"});
		//VcfUtils.createVcfRecord("1", 1, "");
		VcfUtils.addAdditionalSamplesToFormatField(rec, Arrays.asList("GT:AD:DP:FT:GQ:INF:MR:NNS:OABS","1/1:0,3:3:SBIASALT;COVT:9:SOMATIC:2:2:A0[0]2[23.5];G2[36]3[35]"));
		assertEquals(5, rec.getFormatFields().size());
		
	}
	@Test
	public void additionalSamples() {
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC","."});
		//VcfUtils.createVcfRecord("1", 1, "");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:GD:AC:MR:NNS","0/1:A/C:A38[31.42],32[25],C11[27.64],5[36.6]:16:16","0/1:A/C:A75[31.96],57[29.32],C12[35.25],6[38]:18:16"));
		VcfUtils.addAdditionalSamplesToFormatField(rec, Arrays.asList("GT:AD:DP:GQ:PL:GD:AC:MR:NNS","0/1:2,2:4:69:72,0,69:A/C:A101[29.56],51[27.63],C30[30.83],21[37.29],G1[12],0[0]:51:44",".:.:.:.:.:.:A191[31.2],147[27.37],C70[30.29],92[37.47],T0[0],1[37]:162:101"));
		assertEquals(5, rec.getFormatFields().size());
		
	}
	
	@Test
	public void testAdditionalSampleFFRealLifeData() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:0,22:22:75:1124,75,0"));
		VcfUtils.addAdditionalSampleToFormatField(rec, Arrays.asList("GT:GQ:PL", "1/1:6:86,6,0"));
		
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("1/1:0,22:22:75:1124,75,0", rec.getFormatFields().get(1));
		assertEquals("1/1:.:.:6:86,6,0", rec.getFormatFields().get(2));
	}
	
	@Test
	public void isPassSingleSample() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:FT", "1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:ANYTHING_ELSE???"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
	}
	@Test
	public void isPassSingleSampleMerged() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."});
		rec.setInfo("IN=1,2");		// tells us its merged
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN","1/1:MIN"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:."));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:PASS"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","0/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS","1/2:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
	}
	
	@Test
	public void isPassControlTestMerged() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."});
		rec.setInfo("IN=1,2");		// tells us its merged
		rec.setFormatFields(Arrays.asList("GT:FT", "0/0:MIN","1/1:MIN", "0/0:MIN","1/1:MIN"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/0:.","1/1:PASS", "0/0:PASS","1/1:PASS"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/0:PASS","1/1:PASS", "0/0:PASS","1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.","1/1:PASS:SOMATIC", "0/0:PASS:.","1/1:PASS:SOMATIC"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.","1/1:PASS:.", "0/0:PASS:.","1/1:PASS:."));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.","./.:.:.", "0/1:PASS:.","./.:.:."));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:PASS:.","./.:.:.", "0/1:MIN:.","./.:.:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:MIUN:.","./.:.:.", "0/1:PASS:.","./.:.:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:MIUN:.","./.:.:.", "0/1:SBIASALT:.","./.:.:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT:INF", "0/1:.:.","./.:.:.", "0/1:.:.","./.:.:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS","1/1:PASS", "0/1:PASS","1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
	}
	
	@Test
	public void isPassRealLife() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1","13302","rs180734498","C","T",".",".","FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
				"0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
				"0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
				"0/0:.:LOH:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		
		rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
				"0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
				"0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
		"0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		
		rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
				"0/0:.:LOH:22:159:.:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
				"0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
		"0/0:.:LOH:22:159:.:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		
		rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:22:54:MIN:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
				"0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
				"0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
		"0/0:.:LOH:22:159:PASS:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
		assertEquals(false, VcfUtils.isRecordAPass(rec));
	}
	
	@Test
	public void isPassRealLifeOS() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1","13302","rs180734498","C","T",".",".","FLANK=GGACATGCTGT;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:23:34:PASS:.:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
				"0/1:.:Germline:23:80:PASS:.:.:9:8:C35[40.11]36[39.19];T8[38.88]1[42]",
				"0/1:26,10:Germline:22:36:PASS:99:.:10:9:C13[39.69]11[39.73];T9[37]1[42]",
		"0/0:.:LOH:22:80:PASS:.:.:.:.:C35[40.11]36[39.19];T8[38.88]1[42]"});
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
		
		rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/1:.:Germline:22:54:PASS:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
				"0/0:.:LOH:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]",
				"0/1:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
		"0/0:.:LOH:22:159:SBIASALT:.:.:.:.:A4[39.5]2[42];G81[38.6]72[37.33]"});
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
		
		rec = new VcfRecord( new String[] {"chr1","13418",".","G","A",".",".","FLANK=ACCCCAAGATC;IN=1,2;DB;VAF=0.1143",
				"GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS",
				"0/0:.:Germline:22:54:SBIASALT:.:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
				"0/1:.:LOH:22:159:PASS:.:SOMATIC:6:6:A4[39.5]2[42];G81[38.6]72[37.33]",
				"0/0:45,6:Germline:22:51:PASS:65:.:6:6:A5[42]1[37];G26[38.92]22[37.36]",
		"0/1:.:LOH:22:159:PASS:.:SOMATIC:6:6:A4[39.5]2[42];G81[38.6]72[37.33]"});
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
	}
	@Test
	public void isPassControlTest() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN","1/1:MIN"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:."));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:."));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:PASS"));
		assertEquals(false, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","0/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS","1/2:PASS"));
		assertEquals(true, VcfUtils.isRecordAPass(rec));
	}
	
	@Test
	public void keepGatkGQAndQualScores() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."});
		rec.setQualString("100");
		rec.setFormatFields(Arrays.asList("GT:FT:GQ:PL", "1/1:MIN:40:1,2,3"));
		rec.appendInfo("AN=1");
		rec.appendInfo("AC=1.000");
		rec.appendInfo("AF=1.000");
		rec.appendInfo("MLEAF=2.000");
		rec.appendInfo("MLEAC=3.000");
		
		/*
		 * should remove PL and all the info fields
		 */
		VcfUtils.prepareGATKVcfForMerge(rec);
		assertEquals(".", rec.getInfo());
		assertEquals(".", rec.getQualString());
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(rec.getFormatFieldStrings());
		assertEquals(6, ffMap.size());
		assertEquals(false, ffMap.containsKey("PL"));
		assertEquals(true, ffMap.containsKey("DP"));
		assertEquals(".", ffMap.get("DP")[0]);
		assertEquals(true, ffMap.containsKey("AD"));
		assertEquals(".", ffMap.get("AD")[0]);
		assertEquals(true, ffMap.containsKey("GT"));
		assertEquals("1/1", ffMap.get("GT")[0]);
		assertEquals(true, ffMap.containsKey("GQ"));
		assertEquals("40", ffMap.get("GQ")[0]);
		assertEquals(true, ffMap.containsKey("FT"));
		assertEquals("MIN", ffMap.get("FT")[0]);
		assertEquals(true, ffMap.containsKey("QL"));
		assertEquals("100", ffMap.get("QL")[0]);
	}
	
	@Test
	public void samplesPass() {
		assertEquals(false, VcfUtils.samplesPass(null, null, null, null, null));
		assertEquals(false, VcfUtils.samplesPass("", "", "", "", ""));
		assertEquals(false, VcfUtils.samplesPass(".", ".", ".", ".", "."));
//		assertEquals(false, VcfUtils.samplesPass("PASS", "5", "PASS", "SOMATIC", "5"));
	}
	
	@Test
	public void isPassControlTestOldSkool() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:MIN","1/1:MIN"));
		assertEquals(false, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:."));
		assertEquals(false, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:."));
		assertEquals(false, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:.","1/1:PASS"));
		assertEquals(false, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","1/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "1/1:PASS","0/1:PASS"));
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
		rec.setFormatFields(Arrays.asList("GT:FT", "0/1:PASS","1/2:PASS"));
		assertEquals(true, VcfUtils.isRecordAPassOldSkool(rec));
	}
	
	@Test
	public void getAD() {
		assertEquals(".", VcfUtils.getAD(null, null, null));
		assertEquals(".", VcfUtils.getAD(null, null, ""));
		assertEquals(".", VcfUtils.getAD(null, null, "."));
		assertEquals(".", VcfUtils.getAD(null, null, "asdasfg"));
		assertEquals("20,0", VcfUtils.getAD("A", "G", "A10[10]10[20]"));
		assertEquals("20,0", VcfUtils.getAD("A", "C", "A10[10]10[20]"));
		assertEquals("20,0", VcfUtils.getAD("A", "T", "A10[10]10[20]"));
		
		assertEquals("20,12", VcfUtils.getAD("A", "T", "A10[10]10[20];T1[]11[]"));
		assertEquals("12,20", VcfUtils.getAD("T", "A", "A10[10]10[20];T1[]11[]"));
		assertEquals("12,0", VcfUtils.getAD("T", "C", "A10[10]10[20];T1[]11[]"));
		
		assertEquals("12,20", VcfUtils.getAD("T", "A", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
		assertEquals("12,20,55", VcfUtils.getAD("T", "A,C", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
		assertEquals("0,20,55", VcfUtils.getAD("G", "A,C", "A10[10]10[20];T1[]11[];C22[1]33[5]"));
		
	}
	
	@Test
	public void getADCompundSnp() {
		assertEquals("20,0", VcfUtils.getAD("AC", "GT", "AC10[10]10[20]"));
		assertEquals("20,7", VcfUtils.getAD("AC", "GT", "AC10[10]10[20];GT4[7]3[6]"));
		assertEquals("7,20", VcfUtils.getAD("GT", "AC", "AC10[10]10[20];GT4[7]3[6]"));
		assertEquals("7,0", VcfUtils.getAD("GT", "AA", "AC10[10]10[20];GT4[7]3[6]"));
		assertEquals("20,0", VcfUtils.getAD("AC", "GG", "AC10[10]10[20];GT4[7]3[6]"));
		assertEquals("0,0", VcfUtils.getAD("GG", "TT", "AC10[10]10[20];GT4[7]3[6]"));
		assertEquals("0,2", VcfUtils.getAD("GG", "TT", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
		assertEquals("0,2,20", VcfUtils.getAD("GG", "TT,AC", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
		assertEquals("0,2,20,7", VcfUtils.getAD("GG", "TT,AC,GT", "AC10[10]10[20];GT4[7]3[6];TT1[1]1[1]"));
	}
	
	@Test
	public void add5BPToFormat() {
		VcfRecord rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5"));
		assertEquals("5BP", rec.getFormatFields().get(0));
		assertEquals("5", rec.getFormatFields().get(1));
		
		rec = new VcfRecord( new String[] {"chr1", "1066816",".","A","."}); 
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "1/1:0,22:22:75:1124,75,0", "1/1:0,22:22:75:1124,75,0"));
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5"));
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("1/1:0,22:22:75:1124,75,0", rec.getFormatFields().get(1));
		/*
		 * need to add 2 columns - 1 for each sample
		 */
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("5BP", "5","."));
		assertEquals("GT:AD:DP:GQ:PL:5BP", rec.getFormatFields().get(0));
		assertEquals("1/1:0,22:22:75:1124,75,0:5", rec.getFormatFields().get(1));
		assertEquals("1/1:0,22:22:75:1124,75,0:.", rec.getFormatFields().get(2));
	}
	
	@Test
	public void addFormatFields() throws Exception {
		VcfRecord rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC","."});
				//VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		
		List<String> newStuff = new ArrayList<>();
		newStuff.add("GT");
		newStuff.add("blah");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));
		
		newStuff = new ArrayList<>();
		newStuff.add("QT");
		newStuff.add("blah");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL:QT", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150:blah", rec.getFormatFields().get(1));
		
		// and again
		rec = new VcfRecord( new String[] {"1","1",".","ACCACCACC","."}); 
				//VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		
		newStuff = new ArrayList<>();
		newStuff.add("GT:GD:AC");
		newStuff.add("0/1:A/C:A10[12.5],2[33],C20[1],30[2]");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL:GD:AC", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150:A/C:A10[12.5],2[33],C20[1],30[2]", rec.getFormatFields().get(1));
	}
	
	@Test
	public void minWithAM() {
		//static boolean mutationInNorma(int altCount, int totalReadCount, int percentage, int minCoverage) {
		assertEquals(true, VcfUtils.mutationInNorma(1, 10, 5, 0));
		assertEquals(true, VcfUtils.mutationInNorma(1, 20, 5, 0));
		assertEquals(true, VcfUtils.mutationInNorma(1, 30, 5, 0));
		assertEquals(true, VcfUtils.mutationInNorma(1, 30, 5, 1));
		assertEquals(false, VcfUtils.mutationInNorma(1, 30, 5, 2));
		assertEquals(false, VcfUtils.mutationInNorma(1, 30, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(3, 300, 5, 3));
	}
	
	@Test
	public void getFFAsMap() {
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(Arrays.asList("GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","0/0:.:Reference:14:.:PASS:.:NCIG:.","1/1:0,120:SomaticNoReference:14:120:PASS:99:SOMATIC:5348.77"));
		assertEquals(9, ffMap.size());
		assertArrayEquals(new String[]{".","5348.77"}, ffMap.get(VcfHeaderUtils.FORMAT_QL));
	}
	
	@Test
	public void getFFAsMapSpeedTest() {
		List<String> ffs = Arrays.asList("GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL","0/1:27,19:46:C1[]2[];T1[]0[]:C6;T5:PASS:.:.:18:C10[40.2]9[35.33];T12[38.75]15[38]:.","0/1:9,39:48:C3[]2[]:C4;T1:PASS:.:.:37:C19[38]20[36.9];T5[39.2]4[37.75]:.","0/1:31,24:55:.:.:PASS:99:.:.:.:746.77","0/1:10,41:51:.:.:PASS:99:.:.:.:1468.77");
		int counter = 1000000;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ffs);
			assertEquals(11, ffMap.size());
		}
		System.out.println("Time taken old: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ffs);
			assertEquals(11, ffMap.size());
		}
		System.out.println("Time taken new: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ffs);
			assertEquals(11, ffMap.size());
		}
		System.out.println("Time taken old: " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void min() {
		assertEquals(false, VcfUtils.mutationInNorma(0, 0, 0, 0));
		assertEquals(false, VcfUtils.mutationInNorma(0, 10, 1, 2));
		assertEquals(true, VcfUtils.mutationInNorma(1, 10, 1, 2));
		assertEquals(true, VcfUtils.mutationInNorma(2, 10, 1, 2));
		assertEquals(true, VcfUtils.mutationInNorma(2, 10, 1, 3));
		assertEquals(true, VcfUtils.mutationInNorma(2, 10, 25, 2));
		assertEquals(false, VcfUtils.mutationInNorma(2, 10, 25, 3));
		
		
		assertEquals(false, VcfUtils.mutationInNorma(1, 24, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(3, 63, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(4, 79, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(4, 99, 5, 3));
		
		assertEquals(false, VcfUtils.mutationInNorma(1, 30, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(1, 10, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(2, 10, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(3, 10, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(4, 10, 5, 3));
		
		assertEquals(false, VcfUtils.mutationInNorma(1, 100, 5, 3));
		assertEquals(false, VcfUtils.mutationInNorma(2, 100, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(3, 100, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(4, 100, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(5, 100, 5, 3));
		assertEquals(true, VcfUtils.mutationInNorma(6, 100, 5, 3));
	}
}
