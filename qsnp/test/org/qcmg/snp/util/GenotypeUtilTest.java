package org.qcmg.snp.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.model.Classification;

public class GenotypeUtilTest {
	
	
	@Test
	public void getFormatFilters() {
		try {
			assertEquals("./.:.:.:.:.:.:.:.", GenotypeUtil.getFormatValues(null, null, null, '\u0000', false, 0, 0,Classification.UNKNOWN, false));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException  iae) {}
		assertEquals("./.:.:.:.:.:.:.:.:.", GenotypeUtil.getFormatValues(null, null, "A", '\u0000', false, 0, 0,Classification.UNKNOWN, false));
		assertEquals("./.:.:.:.:.:.:.:.:.", GenotypeUtil.getFormatValues(null, null, "B", '\u0000', false, 0, 0,Classification.UNKNOWN, true));
		
		String gt = "1/1";
		Accumulator acc = new Accumulator(100);
		acc.addBase((byte)'A', (byte)40, true, 90, 100, 200, 1);
		assertEquals(gt + ":0,1:1:.:.:.:SOMATIC:1:A1[40]0[0]", GenotypeUtil.getFormatValues(acc, gt, "A", 'C', false, 0,0, Classification.SOMATIC, false));
//		assertEquals("1/1:1:SBIASALT:1:1:A1[40]0[0]", GenotypeUtil.getFormatValues(acc, GenotypeEnum.AA, "A", 'C', true, 0, 0,Classification.SOMATIC, false));
		
		acc = new Accumulator(100);
		acc.addBase((byte)'C', (byte)40, true, 90, 100, 200, 1);
		 gt = "0/0";
		assertEquals(gt + ":1,0:1:.:.:.:.:.:C1[40]0[0]", GenotypeUtil.getFormatValues(acc, gt, "A", 'C', false, 0, 0,Classification.SOMATIC, true));
//		assertEquals("1/1:1:SBIASALT;COVN12:1:1:A1[40]0[0]", GenotypeUtil.getFormatValues(acc, GenotypeEnum.AA, "A", 'C', true, 0,0,Classification.SOMATIC, true));
	}
	
	@Test
	public void nnsAndMr() {
		List<String> mrNns = GenotypeUtil.getMRandNNS("0/0", new String [] {"A"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals(".", mrNns.get(0));
		assertEquals(".", mrNns.get(1));
		
		mrNns = GenotypeUtil.getMRandNNS("0/1", new String [] {"A"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals("14", mrNns.get(0));
		assertEquals("2", mrNns.get(1));
		
		mrNns = GenotypeUtil.getMRandNNS("1/1", new String [] {"A"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals("14", mrNns.get(0));
		assertEquals("2", mrNns.get(1));
		
		mrNns = GenotypeUtil.getMRandNNS("0/2", new String [] {"A","G"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals("6", mrNns.get(0));
		assertEquals("2", mrNns.get(1));
		
		mrNns = GenotypeUtil.getMRandNNS("1/2", new String [] {"A","G"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals("14,6", mrNns.get(0));
		assertEquals("2,2", mrNns.get(1));
		
		mrNns = GenotypeUtil.getMRandNNS("2/2", new String [] {"A","G"}, AccumulatorUtils.createFromOABS("A3[23]11[24.18];G2[35]4[32]", 3418618));
		assertEquals(2, mrNns.size());
		assertEquals("6", mrNns.get(0));
		assertEquals("2", mrNns.get(1));
	}
	
	
	
	@Test
	public void dontPutSAN3IfCoverageExists() {
		/*
		 * .:.:.:SAN3:.:SOMATIC:.:.:T57[38.09]62[36.97]
		 * This should not have the SAN3 filter
		 */
		Accumulator acc = AccumulatorUtils.createFromOABS("T57[38.09]62[36.97]", 15986933);
		assertEquals(Constants.MISSING_DATA_STRING, GenotypeUtil.getFormatFilter(acc, null, new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, true));
	}
	
	/*
	 * no longer populate coverage based filters
	 */
	@Ignore
	public void whereIsSAM3() {
		assertEquals("SAN3", GenotypeUtil.getFormatFilter(null, null, new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("SAT3", GenotypeUtil.getFormatFilter(null, null, new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, false));
		assertEquals("SAN3", GenotypeUtil.getFormatFilter(null, "0/0", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("SAT3", GenotypeUtil.getFormatFilter(null, "0/0", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, false));
		assertEquals("SAN3", GenotypeUtil.getFormatFilter(null, "0/1", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("SAT3", GenotypeUtil.getFormatFilter(null, "0/1", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, false));
		assertEquals("SAN3", GenotypeUtil.getFormatFilter(null, "1/1", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("SAT3", GenotypeUtil.getFormatFilter(null, "1/1", new String[]{"C"}, 'T', true, 3, 3, Classification.SOMATIC, false));
		assertEquals("SAN3", GenotypeUtil.getFormatFilter(null, "1/2", new String[]{"C","G"}, 'T', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("SAT3", GenotypeUtil.getFormatFilter(null, "1/2", new String[]{"C","G"}, 'T', true, 3, 3, Classification.SOMATIC, false));
	}
	
	@Test
	public void fivePercentMIN() {
		
		assertEquals("MIN", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C10[36.4]23[39.61];T2[11]1[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
		assertEquals("MIN", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C10[36.4]23[39.61];T2[11]0[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
		assertEquals(".", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C10[36.4]23[39.61];T1[11]0[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
		
		assertEquals(".", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C14[40.21]19[41.74];T1[11]0[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
		assertEquals(".", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C18[41.17]15[39.33];T0[0]1[11]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
		assertEquals(".", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("A0[0]1[11];G13[38.15]20[40]", 1), "0/0", new String[]{"A"}, 'G', true, 3, 3, Classification.SOMATIC, true));
	}
	
	@Test
	public void nullAccumulatorFormatFilter() {
		assertEquals(Constants.MISSING_DATA_STRING, GenotypeUtil.getFormatFilter(null, "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
	}
	
//	@Test
//	public void fivePercentMINAM() {
//		
//		assertEquals("MIN", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C9[0]0[0];T0[0]1[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
//		assertEquals("MIN", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C19[0]0[0];T0[0]1[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
//		assertEquals(".", GenotypeUtil.getFormatFilter(AccumulatorUtils.createFromOABS("C29[0]0[0];T0[0]1[0]", 1), "0/0", new String[]{"T"}, 'C', true, 3, 3, Classification.SOMATIC, true));
//	}
	
	@Test
	public void  isSomatic() {
		Accumulator control = AccumulatorUtils.createFromOABS("A2[37]0[0];G2[37]8[36.5]", 985450);
		String cGt = "./.";
		String tGt = "0/1";
		
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control.getCompressedPileup(), cGt, tGt, "A"));
	}
	
	@Test
	public void  isSomatic2() {
		Accumulator control = AccumulatorUtils.createFromOABS("A0[0]1[34];G19[34.79]16[35.25]", 76354679);
		String cGt = "0/0";
		String tGt = "0/1";
		
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification(control.getCompressedPileup(), cGt, tGt, "A"));
	}
	
	@Test
	public void isSomatic3() {
		/*
		 * chr10	54817257	rs386743785	AG	GA	.	.	IN=1,2;DB;HOM=0,TTTAACCTTCgaCTTGCCCACA;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:FT:INF:MR:NNS:OABS	1/1:0,19:Germline:32:34:PASS:.:19:19:AA8[]6[];GA8[]11[];TA0[]1[]	0/0:2,0:ReferenceNoVariant:32:55:PASS:SOMATIC:.:.:AA33[]20[];AG1[]1[];A_2[]0[]	1/1:0,19:Germline:32:34:PASS:.:19:19:AA8[]6[];GA8[]11[];TA0[]1[]	0/0:2,0:ReferenceNoVariant:32:55:PASS:SOMATIC:.:.:AA33[]20[];AG1[]1[];A_2[]0[]
		 * 
		 * need to decomose this cs to see if the individual snps are somatic - should be germline
		 */
		
		//AA8[]6[];GA8[]11[];TA0[]1[]
		Accumulator control = AccumulatorUtils.createFromOABS("A8[37]6[11];G8[37]11[36.5];T0[0]1[40]", 54817257);
		//AA33[]20[];AG1[]1[];A_2[]0[]
//		Accumulator test = AccumulatorUtils.createFromOABS("A36[37]21[11]", 54817257);
		String cGt = "0/1";
		String tGt = "0/0";
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control.getCompressedPileup(), cGt, tGt, "G"));
		
		//TODO awaiting discussion about whether 1/1 to 0/0 is GERMLINE or SOMATIC
//		Accumulator control2 = AccumulatorUtils.createFromOABS("A16[37]18[11]", 54817258);
//		String cGt2 = "1/1";
//		String tGt2 = "0/0";
//		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control2.getCompressedPileup(), cGt2, tGt2, "A"));
	}
	
	@Test
	public void getFilters() {
		/*
		 *chr1    13116   .       T       G       .       COVT;SBIASALT;5BP2      FLANK=GAAAGGGAGGT       GT:GD:AC:MR:NNS 0/1:G/T:G0[0],3[26],T1[2],4[36.75]:3:2  0/1:G/T:G0[0],3[35],T1[24],3[31]:3:3
		 */
		Accumulator control = AccumulatorUtils.createFromOABS("G0[0]3[26];T1[2]4[36.75]", 13116);
		Accumulator test = AccumulatorUtils.createFromOABS("G0[0]3[35];T1[24]3[31]", 13116);
		String gt = "0/1";
		
		assertEquals(gt + ":5,3:" + control.getCoverage() + ":.:.:.:.:1:G0[0]3[26];T1[2]4[36]", GenotypeUtil.getFormatValues(control, gt, "G", 'T', true, 5, 5,Classification.GERMLINE, true));
		gt = "0/1";
		assertEquals(  gt +  ":4,3:" + test.getCoverage() + ":.:.:.:.:1:G0[0]3[35];T1[24]3[31]", GenotypeUtil.getFormatValues(test, gt, "G", 'T', true, 5, 5,Classification.GERMLINE, false));
		
		/*
		 * chr1    13118   .       A       G       .       COVN12;MIN;MR;NNS;SBIASALT      SOMATIC;FLANK=AAGTGGGGTTG       GT:GD:AC:MR:NNS 0/0:A/A:A1[2],4[37.5],G0[0],1[38]:1:1   0/1:A/G:A1[31],3[26.33],G0[0],3[35]:3:3
		 */
		control = AccumulatorUtils.createFromOABS("A1[2]4[37.5];G0[0]1[38]", 13118);
		test = AccumulatorUtils.createFromOABS("A1[31]3[26.33];G0[0]3[35]", 13118);
		
		assertEquals(gt + ":4,3:" + test.getCoverage() + ":.:.:.:SOMATIC:1:A1[31]3[26];G0[0]3[35]", GenotypeUtil.getFormatValues(test, gt, "G", 'A', true, 5, 5,Classification.SOMATIC, false));
		gt = "0/0";
		assertEquals(gt + ":5,1:" + control.getCoverage() + ":.:.:.:.:.:A1[2]4[37];G0[0]1[38]", GenotypeUtil.getFormatValues(control, gt, "G", 'A', true, 5, 5,Classification.SOMATIC, true));
		
		/*
		 * chr1    14653   .       C       T       .       PASS    FLANK=AGCAATGGCCC       GT:GD:AC:MR:NNS 0/1:C/T:C8[30.75],2[24.5],T3[34.33],3[26.67]:6:5        0/1:C/T:C7[27.14],3[15.33],T4[28.5],0[0]:4:4
		 */
		String cOABS = "C8[30]2[24];T3[34]3[26]";
		String tOABS = "C7[27]3[15];T4[28]0[0]";
		control = AccumulatorUtils.createFromOABS(cOABS, 13118);
		test = AccumulatorUtils.createFromOABS(tOABS, 13118);
		gt = "0/1";
		assertEquals("0/1:10,6:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "T", 'C', true, 5, 5,Classification.GERMLINE, true));
		assertEquals("0/1:10,4:"+test.getCoverage()+":.:.:.:.:1:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "T", 'C', true, 5, 5,Classification.GERMLINE, false));
		
	}
	
	@Test
	public void mutationEqualsReference() {
		/*
		 * chr1    16534   .       C       T       .       MER;COVN12;MR;NNS;MIUN  SOMATIC;FLANK=CTTAACAAACC       GT:GD:AC:MR:NNS 1/1:T/T:T2[32],1[2]:0:3 0/0:C/C:C2[34],1[2],T1[34],1[2]:3:2
		 */
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/1", "0/0"));
//		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification(GenotypeEnum.TT, GenotypeEnum.CC, 'C'));
		
		String cOABS = "T2[32]1[2]";
		String tOABS = "C2[34]1[2];T1[34]1[2]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 16534);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 16534);
		String gt = "1/1";
		assertEquals(gt + ":0,3:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "T", 'C', true, 5, 5,Classification.SOMATIC, true));
		gt = "0/0";
		assertEquals(gt + ":3,2:"+test.getCoverage()+":.:.:.:SOMATIC:.:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "T", 'C', true, 5, 5,Classification.SOMATIC, false));
	}
	
	@Test
	public void san3() {
		/*
		 * chr1    16571   .       G       A       .       SAN3;SBIASCOV   FLANK=AGCACACCAGA       GT:GD:AC:MR:NNS .:.:A0[0],1[34],C0[0],1[1],G0[0],2[32.5]:1:1    1/1:A/A:A0[0],3[18.33],G0[0],2[30.5]:3:3
		 */
		String cOABS = "A0[0]1[34];C0[0]1[1];G0[0]2[32]";
		String tOABS = "A0[0]3[18];G0[0]2[30]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 16571);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 16571);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("ACG", ".", "1/1","A"));
//		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control, null, test, GenotypeEnum.AA, 'G'));
		String gt = "1/1";
		assertEquals("./.:2,1:"+control.getCoverage()+":.:.:.:.:.:"+cOABS, GenotypeUtil.getFormatValues(control, null, "A", 'G', true, 5, 5,Classification.GERMLINE, true));
		assertEquals(gt + ":2,3:"+test.getCoverage()+":.:.:.:.:1:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "A", 'G', true, 5, 5,Classification.GERMLINE, false));
		
		cOABS = "A0[0]1[34];C0[0]1[1]";
		tOABS = "A0[0]3[18];G0[0]2[30]";
		control = AccumulatorUtils.createFromOABS(cOABS, 16571);
		test = AccumulatorUtils.createFromOABS(tOABS, 16571);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("ACG", ".", "1/1","A"));
//		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control, null, test, GenotypeEnum.AA, 'G'));
		assertEquals("./.:0,1:"+control.getCoverage()+":.:.:.:.:.:"+cOABS, GenotypeUtil.getFormatValues(control, null, "A", 'G', true, 5, 5,Classification.GERMLINE, true));
		assertEquals(gt + ":2,3:"+test.getCoverage()+":.:.:.:.:1:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "A", 'G', true, 5, 5,Classification.GERMLINE, false));
	}
	
	@Test
	public void sat3() {
		/*
		 * chr1    133129  .       G       A       .       SAT3    FLANK=TGCCTATACAG       GT:GD:AC:MR:NNS 1/1:A/A:A2[30.5],1[2],G0[0],1[36]:3:3   .:.:A1[21],0[0],G1[35],1[23]:1:1
		 */
		String cOABS = "A2[30]1[2];G0[0]1[36]";
		String tOABS = "A1[21]0[0];G1[35]1[23]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 133129);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 133129);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("AG", "1/1", ".","A"));
//		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control, GenotypeEnum.AA, test, null, 'G'));
		String gt = "1/1";
		assertEquals(gt + ":1,3:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "A", 'G', true, 5, 5,Classification.GERMLINE, true));
		assertEquals("./.:2,1:"+test.getCoverage()+":.:.:.:.:.:"+tOABS, GenotypeUtil.getFormatValues(test, null, "A", 'G', true, 5, 5,Classification.GERMLINE, false));
		
		cOABS = "A2[30]1[2];G0[0]1[36]";
		tOABS = "A1[21]0[0];G0[0]1[23]";
		control = AccumulatorUtils.createFromOABS(cOABS, 133129);
		test = AccumulatorUtils.createFromOABS(tOABS, 133129);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("AG", "1/1", ".","A"));
//		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(control, GenotypeEnum.AA, test, null, 'G'));
		assertEquals(gt + ":1,3:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "A", 'G', true, 5, 5,Classification.GERMLINE, true));
		assertEquals("./.:1,1:"+test.getCoverage()+":.:.:.:.:.:"+tOABS, GenotypeUtil.getFormatValues(test, null, "A", 'G', true, 5, 5,Classification.GERMLINE, false));
	}
	
	@Test
	public void doubleMIN() {
		/*
		 * chr1    15274   .       A       G,T     .       .       SOMATIC;FLANK=TGTACGATGGG       GT:DP:FT:MR:NNS:OABS    2/2:65:SBIASCOV;MIN;MIN:61:45:G0[0]4[39.5];T3[37.33]58[39.22]   1/2:56:SBIASALT;.:9,47:9,36:G0[0]9[40.33];T3[35.33]44[39.25]
		 */
		String cOABS = "G0[0]4[39];T3[37]58[39]";
		String tOABS = "G0[0]9[40];T3[35]44[39]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 15274);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 15274);
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("GT", "2/2", "1/2","G,T"));
		String gt = "2/2";
		assertEquals(gt + ":0,4,61:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "G,T", 'A', true, 5, 5,Classification.SOMATIC, true));
		gt = "1/2";
		assertEquals(gt + ":0,9,47:"+test.getCoverage()+":.:.:.:SOMATIC:1,2:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "G,T", 'A', true, 5, 5,Classification.SOMATIC, false));
	}
	
	@Test
	public void getAD() {
		/*
		 * chr1    15274   .       A       G,T     .       .       SOMATIC;FLANK=TGTACGATGGG       GT:DP:FT:MR:NNS:OABS    2/2:65:SBIASCOV;MIN;MIN:61:45:G0[0]4[39.5];T3[37.33]58[39.22]   1/2:56:SBIASALT;.:9,47:9,36:G0[0]9[40.33];T3[35.33]44[39.25]
		 */
		String cOABS = "G0[0]4[39];T3[37]58[39]";
		String tOABS = "G0[0]9[40];T3[35]44[39]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 15274);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 15274);
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("GT", "2/2", "1/2","G,T"));
		String gt = "2/2";
		assertEquals(gt + ":0,4,61:"+control.getCoverage()+":.:.:.:.:2:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "G,T", 'A', true, 5, 5,Classification.SOMATIC, true));
		gt = "1/2";
		assertEquals(gt + ":0,9,47:"+test.getCoverage()+":.:.:.:SOMATIC:1,2:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "G,T", 'A', true, 5, 5,Classification.SOMATIC, false));
	}
	
	@Test
	public void miun() {
		/*
		 *chr1    802026  .       G       A       .       MIUN    SOMATIC;FLANK=CTGACATCCTC       GT:GD:AC:MR:NNS 0/0:G/G:A5[33],1[25],C0[0],3[6.67],G115[22.77],241[27.5],T0[0],3[4.67]:6:5      0/1:A/G:A10[32.8],6[22.5],G73[21.21],161[24.96],T0[0],1[2]:16:14 
		 */
		String cOABS = "A5[33]1[25];C0[0]3[6];G115[22]241[27];T0[0]3[4]";
		String tOABS = "A10[32]6[22];G73[21]161[24];T0[0]1[2]";
		Accumulator control = AccumulatorUtils.createFromOABS(cOABS, 802026);
		Accumulator test = AccumulatorUtils.createFromOABS(tOABS, 802026);
		Classification c = Classification.SOMATIC;
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("GT", "2/2", "1/2","G,T"));
//		assertEquals(c, GenotypeUtil.getClassification(control, GenotypeEnum.GG, test, GenotypeEnum.AG, 'G'));
		String gt = "0/0";
		assertEquals(gt + ":356,6:"+control.getCoverage()+":.:.:.:.:.:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "A", 'G', true, 5, 5,c, true));
		gt = "0/1";
		assertEquals(gt + ":234,16:"+test.getCoverage()+":.:.:.:SOMATIC:2:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "A", 'G', true, 5, 5,c, false));
		
		/*
		 * add in the unfiltered alt - need 3% of these to trigger (which is 11 in this case
		 */
		control.addFailedFilterBase((byte)'A');
		control.addFailedFilterBase((byte)'A');
		 gt = "0/0";
		assertEquals(gt + ":356,6:"+control.getCoverage()+":.:A2:.:.:.:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "A", 'G', true, 5, 5,c, true));
		gt = "0/1";
		assertEquals(gt + ":234,16:"+test.getCoverage()+":.:.:.:SOMATIC:2:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "A", 'G', true, 5, 5,c, false));
		
		/*
		 * add 9 more
		 */
		for (int i = 0 ; i < 9 ; i++) {
			control.addFailedFilterBase((byte)'A');
		}
		gt = "0/0";
		assertEquals(gt + ":356,6:"+control.getCoverage()+":.:A11:.:.:.:"+cOABS, GenotypeUtil.getFormatValues(control, gt, "A", 'G', true, 5, 5,c, true));
		gt = "0/1";
		assertEquals(gt + ":234,16:"+test.getCoverage()+":.:.:.:SOMATIC:2:"+tOABS, GenotypeUtil.getFormatValues(test, gt, "A", 'G', true, 5, 5,c, false));
	}
	
	@Test
	public void getClassificationGermline() {
		/*
		 * If the genotypes are equal, its germline
		 */
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/0", "0/0"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "0/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "1/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/2", "1/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("2/2", "2/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("2/3", "2/3"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("3/3", "3/3"));
	}
	
	@Test
	public void getClassificationGermlineHomHom() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "1/1"));
	}
	@Test
	public void getClassificationGermlineHomHet() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "0/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("2/2", "0/2"));
	}
	
	@Test
	public void getClassificationGermlineHetHet() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "0/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/2", "1/2"));
	}
	
	@Test
	public void compareGenotypesSame() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/0", "0/0"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "1/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "0/1"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/2", "0/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/2", "1/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("2/2", "2/2"));
	}

	@Test
	public void notSameBothHom() {
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/0", "1/1"));
		assertEquals("G", GenotypeUtil.getAltAlleles(GenotypeEnum.AA, GenotypeEnum.GG, 'A'));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/0", "1/1"));
		assertEquals("C", GenotypeUtil.getAltAlleles(GenotypeEnum.CC, GenotypeEnum.TT, 'T'));
	}
	
	@Test
	public void notSameBothHet() {
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/1", "2/3"));
		assertEquals("C,G,T", GenotypeUtil.getAltAlleles(GenotypeEnum.AC, GenotypeEnum.GT, 'A'));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/2", "0/3"));
		assertEquals("C,T,A", GenotypeUtil.getAltAlleles(GenotypeEnum.CT, GenotypeEnum.AG, 'G'));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/2", "0/1"));
		assertEquals("C,T", GenotypeUtil.getAltAlleles(GenotypeEnum.CT, GenotypeEnum.CG, 'G'));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/2", "1/3"));
		assertEquals("C,T,A", GenotypeUtil.getAltAlleles(GenotypeEnum.CT, GenotypeEnum.AC, 'G'));
	}
	
	@Test
	public void homHet() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "0/1"));
		assertEquals("C", GenotypeUtil.getAltAlleles(GenotypeEnum.CC, GenotypeEnum.CG, 'G'));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/1", "2/3"));
		assertEquals("A,C,T", GenotypeUtil.getAltAlleles(GenotypeEnum.AA, GenotypeEnum.CT, 'G'));
		
	}
	
	@Test
	public void hetHom() {
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/1", "2/2"));
		assertEquals("G,T", GenotypeUtil.getAltAlleles(GenotypeEnum.AG, GenotypeEnum.TT, 'A'));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "1/1"));
		assertEquals("G", GenotypeUtil.getAltAlleles(GenotypeEnum.AG, GenotypeEnum.GG, 'A'));
	}
	
	@Test
	public void wikiPageDetails() {
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "0/1"));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/1", "1/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "1/1"));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/1", "2/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("1/1", "1/1"));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("1/1", "2/2"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("0/1", "0/1"));
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification("0/1", "0/2"));
	}
	
	@Test
	public void testSingleGenotypeSameAsRef() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		assertEquals(Classification.UNKNOWN, GenotypeUtil.getClassification(null, null, null , "A"));
		
		assertEquals(Classification.UNKNOWN, GenotypeUtil.getClassification(acc.getCompressedPileup() , "0/0", null,  "C"));
		assertEquals(Classification.UNKNOWN, GenotypeUtil.getClassification(acc.getCompressedPileup() ,null,  "0/0",  "C"));
	}
	
	@Test
	public void singleLowNormalCov() {
		Accumulator controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'G', (byte)40, false, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("AG", null, "0/1" , "G"));
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,null,  "0/1",  "G"));
		assertEquals("G", GenotypeUtil.getAltAlleles(null, GenotypeEnum.AG, 'A'));
		
		controlAcc.addBase((byte)'G', (byte)40, false, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,null,  "0/1",  "G"));
		assertEquals("G", GenotypeUtil.getAltAlleles(null, GenotypeEnum.AG, 'A'));
		
		controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,null,  "0/1",  "G"));
		assertEquals("G", GenotypeUtil.getAltAlleles(null, GenotypeEnum.AG, 'A'));
		
		controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'C', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'C', (byte)40, false, 1, 1, 2, 1);
		assertEquals(Classification.SOMATIC, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,null,  "1/2",  "G,T"));
		assertEquals("G,T", GenotypeUtil.getAltAlleles(null, GenotypeEnum.GT, 'A'));
		
		controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'T', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'G', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'G', (byte)40, false, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,null,  "1/1",  "T"));
		assertEquals("T", GenotypeUtil.getAltAlleles(null, GenotypeEnum.TT, 'A'));
	}
	
	@Test
	public void singleLowTestCov() {
		Accumulator controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'G', (byte)40, false, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,"0/1", null, "G"));
		assertEquals("G", GenotypeUtil.getAltAlleles(GenotypeEnum.AG, null, 'A'));
		
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,"1/1", null, "C"));
		assertEquals("C", GenotypeUtil.getAltAlleles(GenotypeEnum.CC, null, 'A'));
		
		assertEquals(Classification.UNKNOWN, GenotypeUtil.getClassification(controlAcc.getCompressedPileup() ,"0/0", null, ""));
	}

	@Test
	public void realLifeData() {
		Accumulator controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'A', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'A', (byte)40, false, 1, 1, 2, 1);
		controlAcc.addBase((byte)'T', (byte)40, true, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("AT", "0/1", ".", "T"));
		assertEquals("T", GenotypeUtil.getAltAlleles(null, GenotypeEnum.AT, 'A'));
		
		
		controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'C', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'C', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'T', (byte)40, true, 1, 1, 2, 1);
		controlAcc.addBase((byte)'T', (byte)40, true, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("CT", "1/2", ".", "C,T"));
		assertEquals("C,T", GenotypeUtil.getAltAlleles(null, GenotypeEnum.CT, 'G'));
		
		controlAcc = new Accumulator(1);
		controlAcc.addBase((byte)'G', (byte)40, true, 1, 1, 2, 1);
		assertEquals(Classification.GERMLINE, GenotypeUtil.getClassification("G", "1/1", ".", "G"));
		assertEquals("G", GenotypeUtil.getAltAlleles(null, GenotypeEnum.GG, 'A'));
	}
}
