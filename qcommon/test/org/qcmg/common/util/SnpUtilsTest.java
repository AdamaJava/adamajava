package org.qcmg.common.util;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SnpUtilsTest {
	
	@Test
	public void testIsClassA() {
		String annotation = null;
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "";
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "hello there";
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "- -";	// 2 dashes with space between them
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		assertEquals(false, SnpUtils.isClassA("---")); // 3 dashes
		annotation = " --";	// 2 dashes with leading space
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "-- ";	// 2 dashes with trailing space
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "--123";
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		annotation = "123--";
		Assert.assertFalse(SnpUtils.isClassA(annotation));
		assertEquals(false, SnpUtils.isClassA("--"));
		assertEquals(true, SnpUtils.isClassA("PASS"));
		assertEquals(false, SnpUtils.isClassA("1PASS"));
		assertEquals(false, SnpUtils.isClassA("PASS1"));
		assertEquals(false, SnpUtils.isClassA("PASS--"));
		assertEquals(false, SnpUtils.isClassA("--PASS"));
	}
	
	
	@Test
	public void getBaseCounts() {
		String bases = "A1[11],0[0],C19[20.63],4[24],G3[20],2[24],T192[28.12],97[34.96]";
		assertEquals(1, SnpUtils.getCountFromNucleotideString(bases, "A"));
		assertEquals(23, SnpUtils.getCountFromNucleotideString(bases, "C"));
		assertEquals(5, SnpUtils.getCountFromNucleotideString(bases, "G"));
		assertEquals(289, SnpUtils.getCountFromNucleotideString(bases, "T"));
	}
	
	@Test
	public void getTotalBaseCounts() {
		String bases = "A1[11],0[0],C19[20.63],4[24],G3[20],2[24],T192[28.12],97[34.96]";
		assertEquals(289 + 23 + 5+ 1, SnpUtils.getTotalCountFromNucleotideString(bases, false));
		
		assertEquals(1, SnpUtils.getTotalCountFromNucleotideString("A1[11],0[0]", false));
		assertEquals(2, SnpUtils.getTotalCountFromNucleotideString("A1[11],1[0]", false));
		assertEquals(200, SnpUtils.getTotalCountFromNucleotideString("A100[11],100[0]", false));
		assertEquals(201, SnpUtils.getTotalCountFromNucleotideString("A100[11],100[0],B1[0]", false));
		assertEquals(201, SnpUtils.getTotalCountFromNucleotideString("A100[11],100[0],B1[0],0[1]", false));
		assertEquals(211, SnpUtils.getTotalCountFromNucleotideString("A100[11],100[0],B1[0],10[1]", false));
	}
	@Test
	public void getTotalBaseCountsCS() {
		String bases = "CA,17,17,C_,2,0,GG,10,8";
		assertEquals(17 + 17 + 2+ 0 + 10 +8, SnpUtils.getTotalCountFromNucleotideString(bases, true));
		
		assertEquals(1, SnpUtils.getTotalCountFromNucleotideString("CA,1,0", true));
		assertEquals(2, SnpUtils.getTotalCountFromNucleotideString("CA,1,1", true));
		assertEquals(2, SnpUtils.getTotalCountFromNucleotideString("CA,1,1,AB,0,0", true));
		assertEquals(12, SnpUtils.getTotalCountFromNucleotideString("CA,1,1,AB,0,10", true));
		
		bases = "AC,2,2,AT,2,4,A_,3,3,CA,2,1,CC,16,17,CG,2,1,CT,1,2,C_,16,16,GC,1,0,_C,0,1,G_,0,1";
		assertEquals(19 + 16 + 17 + 6 + 35, SnpUtils.getTotalCountFromNucleotideString(bases, true));
	}
	
	@Test
	public void getBaseCountsACCSs() {
		String bases = "CA,17,17,C_,2,0,GG,10,8";
		assertEquals(34, SnpUtils.getCountFromNucleotideString(bases, "CA", true));
		assertEquals(2, SnpUtils.getCountFromNucleotideString(bases, "C_", true));
		assertEquals(18, SnpUtils.getCountFromNucleotideString(bases, "GG", true));
		
		bases = "AC,2,2,AT,2,4,A_,3,3,CA,2,1,CC,16,17,CG,2,1,CT,1,2,C_,16,16,GC,1,0,_C,0,1,G_,0,1";
		assertEquals(4, SnpUtils.getCountFromNucleotideString(bases, "AC", true));
		assertEquals(6, SnpUtils.getCountFromNucleotideString(bases, "AT", true));
		assertEquals(6, SnpUtils.getCountFromNucleotideString(bases, "A_", true));
		assertEquals(3, SnpUtils.getCountFromNucleotideString(bases, "CA", true));
		assertEquals(33, SnpUtils.getCountFromNucleotideString(bases, "CC", true));
		assertEquals(3, SnpUtils.getCountFromNucleotideString(bases, "CG", true));
		assertEquals(3, SnpUtils.getCountFromNucleotideString(bases, "CT", true));
		assertEquals(32, SnpUtils.getCountFromNucleotideString(bases, "C_", true));
		assertEquals(1, SnpUtils.getCountFromNucleotideString(bases, "GC", true));
		assertEquals(1, SnpUtils.getCountFromNucleotideString(bases, "_C", true));
		assertEquals(1, SnpUtils.getCountFromNucleotideString(bases, "G_", true));
		
	}
	
	@Test
	public void getCSDist() {
		assertEquals(true, SnpUtils.getCompoundSnpDistribution(null).isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("").isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("asdfsgahjsfkahs").isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("asdfsgahjsfkahs1232345").isEmpty());
		
		String bases = "CA,17,17,C_,2,0,GG,10,8";
		Map<String,Integer> map = SnpUtils.getCompoundSnpDistribution(bases);
		assertEquals(3, map.size());
		assertEquals(true, map.values().contains(34));
		assertEquals(true, map.values().contains(18));
		assertEquals(true, map.values().contains(2));
		assertEquals(true, map.containsKey("C_"));
		assertEquals(true, map.containsKey("GG"));
		assertEquals(true, map.containsKey("CA"));
	}
	
	@Test
	public void getCSDistMinCov() {
		assertEquals(true, SnpUtils.getCompoundSnpDistribution(null, 1).isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("", 1).isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("asdfsgahjsfkahs", 1).isEmpty());
		assertEquals(true, SnpUtils.getCompoundSnpDistribution("asdfsgahjsfkahs1232345", 1).isEmpty());
		
		String bases = "CA,17,17,C_,2,0,GG,10,8";
		Map<String,Integer> map = SnpUtils.getCompoundSnpDistribution(bases, 3);
		assertEquals(2, map.size());
		assertEquals(true, map.values().contains(34));
		assertEquals(true, map.values().contains(18));
		assertEquals(true, map.containsKey("CA"));
		assertEquals(true, map.containsKey("GG"));
		map.clear();
		
		map = SnpUtils.getCompoundSnpDistribution(bases, 20);
		assertEquals(1, map.size());
		assertEquals(true, map.containsValue(34));
		assertEquals(true, map.containsKey("CA"));
		map.clear();
		
		bases = "CA,17,17,_C_,2,0,GG,10,8";
		map = SnpUtils.getCompoundSnpDistribution(bases, 1);
		assertEquals(3, map.size());
		assertEquals(true, map.values().contains(34));
		assertEquals(true, map.values().contains(18));
		assertEquals(true, map.values().contains(2));
		assertEquals(true, map.containsKey("CA"));
		assertEquals(true, map.containsKey("GG"));
		assertEquals(true, map.containsKey("_C_"));
		map.clear();
	}
	
	@Test
	public void testIsClassAIndel() {
		try {
			assertEquals(false, SnpUtils.isClassAIndel(null, -1));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, SnpUtils.isClassAIndel("", -1));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		// PASS
		assertEquals(false, SnpUtils.isClassAIndel("pass", -1));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.PASS, -1));
		
		//INDEL_HOM_ADJ
		try {
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ, 3));
		Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 9, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 8, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 7, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 4, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 3, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 1, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 0, 3));
		
		//INDEL_HOM_CON
		try {
			assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON, 3));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 9, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 8, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 7, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 4, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 3, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 1, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 0, 3));
		
		//INDEL_HOM_EMB
		try {
			assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB, 3));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 9, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 8, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 7, 3));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 4, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 3, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 1, 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 0, 3));
	}
	
	@Test
	public void testIsClassAOrB() {
		String annotation = null;
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "";
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "hello there";
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "- -";	// 2 dashes with space between them
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		assertEquals(false, SnpUtils.isClassAorB("---"));
		annotation = " --";	// 2 dashes with leading space
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "-- ";	// 2 dashes with trailing space
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "--123";
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		annotation = "123--";
		Assert.assertFalse(SnpUtils.isClassAorB(annotation));
		assertEquals(false, SnpUtils.isClassAorB("--"));
		assertEquals(true, SnpUtils.isClassAorB("PASS"));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.PASS));
		
		Assert.assertFalse(SnpUtils.isClassAorB(SnpUtils.LESS_THAN_8_READS_NORMAL));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.LESS_THAN_12_READS_NORMAL));
		assertEquals(false, SnpUtils.isClassAorB("; " +  SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.LESS_THAN_3_READS_NORMAL));
		assertEquals(false, SnpUtils.isClassAorB("; " +  SnpUtils.MUTATION_IN_UNFILTERED_NORMAL));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.LESS_THAN_12_READS_NORMAL_AND_UNFILTERED));
		assertEquals(true, SnpUtils.isClassAorB(SnpUtils.LESS_THAN_3_READS_NORMAL_AND_UNFILTERED));
		Assert.assertFalse(SnpUtils.isClassAorB(SnpUtils.LESS_THAN_3_READS_NORMAL + "; " + SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertFalse(SnpUtils.isClassAorB(SnpUtils.LESS_THAN_12_READS_NORMAL + "; " + SnpUtils.MUTATION_IN_NORMAL));
	}
	
	@Test
	public void testGetAltFromMutationString() {
		
		try {
			SnpUtils.getAltFromMutationString(null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			SnpUtils.getAltFromMutationString("");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals("C", SnpUtils.getAltFromMutationString("A" + Constants.MUT_DELIM + "C"));
		assertEquals("A", SnpUtils.getAltFromMutationString("A" + Constants.MUT_DELIM + "A"));
		assertEquals("X", SnpUtils.getAltFromMutationString("A" + Constants.MUT_DELIM + "X"));
		assertEquals("Y", SnpUtils.getAltFromMutationString("X" + Constants.MUT_DELIM + "Y"));
		
	}
	
	
	@Test
	public void testGetVariantCountFromNucleotideString() {
		assertEquals(0, SnpUtils.getCountFromNucleotideString(null, null));
		assertEquals(0, SnpUtils.getCountFromNucleotideString("", null));
		assertEquals(0, SnpUtils.getCountFromNucleotideString("ABCD", null));
		assertEquals(56, SnpUtils.getCountFromNucleotideString("A56[29.05],0[0],C0[0],4[25.43],T0[0],2[25.74],G1[29],0[0]","A"));
		assertEquals(4, SnpUtils.getCountFromNucleotideString("A56[29.05],0[0],C0[0],4[25.43],T0[0],2[25.74],G1[29],0[0]","C"));
		assertEquals(2, SnpUtils.getCountFromNucleotideString("A56[29.05],0[0],C0[0],4[25.43],T0[0],2[25.74],G1[29],0[0]","T"));
		assertEquals(1, SnpUtils.getCountFromNucleotideString("A56[29.05],0[0],C0[0],4[25.43],T0[0],2[25.74],G1[29],0[0]","G"));
		
	}
	
	@Test
	public void missingMRField() {
		assertEquals(29, SnpUtils.getCountFromNucleotideString("C22[36.91],21[26.67],G27[36.93],2[31]","G"));
	}
	
	@Test
	public void testGetNNumberFromAnnotation() {
		assertEquals(9, SnpUtils.getNNumberFromAnnotation("PASS;HOMCON_9;", SnpUtils.INDEL_HOM_CON));
		assertEquals(10, SnpUtils.getNNumberFromAnnotation("PASS;MIN;HOMCON_10;", SnpUtils.INDEL_HOM_CON));
		assertEquals(7, SnpUtils.getNNumberFromAnnotation("PASS;Simple_repeat::(TCC)n;HOMCON_7;", SnpUtils.INDEL_HOM_CON));
		assertEquals(0, SnpUtils.getNNumberFromAnnotation("PASS;MIN;HOMADJ_0;", SnpUtils.INDEL_HOM_ADJ));
	}
	
	@Test
	public void testIsAnnotationAlone() {
		assertEquals(false, SnpUtils.isAnnotationAlone("PASS;Low_complexity::G-rich;NNS;", SnpUtils.INDEL_LOW_COMPLEXITY));
		assertEquals(false, SnpUtils.isAnnotationAlone("PASS;Satellite::HSATII;HOMCON_10;", SnpUtils.INDEL_SATELLITE));
		assertEquals(false, SnpUtils.isAnnotationAlone("PASS;Simple_repeat::(GAAA)n;NNS;", SnpUtils.INDEL_SIMPLE_REPEAT));
		assertEquals(false, SnpUtils.isAnnotationAlone("PASS;Simple_repeat::(TC)n;;Simple_repeat::(TG)n;MIN;HOMCON_8;", SnpUtils.INDEL_SIMPLE_REPEAT));
		assertEquals(false, SnpUtils.isAnnotationAlone("PASS", SnpUtils.INDEL_SIMPLE_REPEAT));
		assertEquals(true, SnpUtils.isAnnotationAlone("PASS", SnpUtils.PASS));
		assertEquals(true, SnpUtils.isAnnotationAlone("PASS;PASS", SnpUtils.PASS));
		assertEquals(true, SnpUtils.isAnnotationAlone("PASS;" + SnpUtils.INDEL_LOW_COMPLEXITY, SnpUtils.INDEL_LOW_COMPLEXITY));
	}
	
	@Test
	public void testGetCountFromIndelNucleotideString() {
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString(null, 0));
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString("", 0));
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString("", -1));
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString("", 1));
		
		//ND field
		assertEquals(4, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 0));
		assertEquals(39, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 1));
		assertEquals(24, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 2));
		assertEquals(4, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 3));
		assertEquals(4, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 4));
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 5));
		assertEquals(5, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 6));
		assertEquals(0, SnpUtils.getCountFromIndelNucleotideString("4;39;24;4;4;0;5", 7));
		
		//TD field
		assertEquals(4, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 0));
		assertEquals(35, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 1));
		assertEquals(26, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 2));
		assertEquals(5, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 3));
		assertEquals(5, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 4));
		assertEquals(1, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 5));
		assertEquals(9, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 6));
		try {
			assertEquals(9, SnpUtils.getCountFromIndelNucleotideString("4;35;26;5;5;1;9;\"9 contiguous ACCCCCCCCC______TTTTTTTCTG\"", 7));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
	}
	
	@Test
	public void testIndels() {
		assertEquals(false, SnpUtils.isClassAIndel("PASS;NNS;HCOVT;HCOVN;MIN;HOMCON_7", 3));
	}
	
	@Test
	public void oneStrandOnly() {
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],0[0]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],0[0]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],0[0]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5],C0[0],1[34],G0[0],888[33.34]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5],C0[0],1[34],G0[0],888[33.34]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],2[33.5],C0[0],1[34],G0[0],888[33.34]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C1[34],0[0],G888[33.34],0[0]", 1));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C1[34],0[0],G888[33.34],0[0]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C1[34],0[0],G888[33.34],0[0]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],306[38.77],G0[0],179[38.5],T0[0],2[27.5]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],306[38.77],G0[0],179[38.5],T0[0],2[27.5]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],306[38.77],G0[0],179[38.5],T0[0],2[27.5]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],848[37.74],C0[0],3[23],G0[0],393[38.01],T0[0],4[21.5]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],848[37.74],C0[0],3[23],G0[0],393[38.01],T0[0],4[21.5]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A0[0],848[37.74],C0[0],3[23],G0[0],393[38.01],T0[0],4[21.5]", 100));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A:0[0],1[33],C:0[0],29[32.55],T:0[0],156[31.74]", 2));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A:0[0],1[33],C:0[0],29[32.55],T:0[0],156[31.74]", 20));
	}
	@Test
	public void twoStrands() {
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 0));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 1));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 10));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 16));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 17));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 20));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A10[10],2[33.5]", 100));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 1));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 10));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 20));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 33));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 34));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("C8[34.62],0[0],T0[0],4[38]", 100));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C0[0],1[34],G888[33.34],0[0]", 0));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C0[0],1[34],G888[33.34],0[0]", 1));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C0[0],1[34],G888[33.34],0[0]", 10));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],0[0],C0[0],1[34],G888[33.34],0[0]", 100));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 0));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 1));
		assertTrue(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 2));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 3));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 30));
		assertFalse(SnpUtils.doesNucleotideStringContainReadsOnBothStrands("A2[33.5],10[10],C10[10],1[34],G888[33.34],10[10]", 300));
	}
	
}
