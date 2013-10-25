package org.qcmg.common.util;
import static org.junit.Assert.assertEquals;

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
	public void testIsClassAIndel() {
		try {
			assertEquals(false, SnpUtils.isClassAIndel(null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, SnpUtils.isClassAIndel(""));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		// PASS
		assertEquals(false, SnpUtils.isClassAIndel("pass"));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.PASS));
		
		//INDEL_HOM_ADJ
		try {
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ));
		Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 9));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 8));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 7));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 4));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 1));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_ADJ + 0));
		
		//INDEL_HOM_CON
		try {
			assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 9));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 8));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 7));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 4));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 1));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_CON + 0));
		
		//INDEL_HOM_EMB
		try {
			assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 9));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 8));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 7));
		assertEquals(false, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 4));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 3));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 1));
		assertEquals(true, SnpUtils.isClassAIndel(SnpUtils.INDEL_HOM_EMB + 0));
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
			Assert.fail("Should have thrown an eception");
		} catch (IllegalArgumentException iae) {}
		try {
			SnpUtils.getAltFromMutationString("");
			Assert.fail("Should have thrown an eception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals('C', SnpUtils.getAltFromMutationString("A/C"));
		assertEquals('A', SnpUtils.getAltFromMutationString("A/A"));
		assertEquals('X', SnpUtils.getAltFromMutationString("A/X"));
		assertEquals('Y', SnpUtils.getAltFromMutationString("X/Y"));
		
	}
	
	
	@Test
	public void testGetVariantCountFromNucleotideString() {
		assertEquals(0, SnpUtils.getCountFromNucleotideString(null,'\u0000'));
		assertEquals(0, SnpUtils.getCountFromNucleotideString("",'\u0000'));
		assertEquals(0, SnpUtils.getCountFromNucleotideString("ABCD",'\u0000'));
		assertEquals(56, SnpUtils.getCountFromNucleotideString("A:56[29.05],0[0],C:0[0],4[25.43],T:0[0],2[25.74],G:1[29],0[0]",'A'));
		assertEquals(4, SnpUtils.getCountFromNucleotideString("A:56[29.05],0[0],C:0[0],4[25.43],T:0[0],2[25.74],G:1[29],0[0]",'C'));
		assertEquals(2, SnpUtils.getCountFromNucleotideString("A:56[29.05],0[0],C:0[0],4[25.43],T:0[0],2[25.74],G:1[29],0[0]",'T'));
		assertEquals(1, SnpUtils.getCountFromNucleotideString("A:56[29.05],0[0],C:0[0],4[25.43],T:0[0],2[25.74],G:1[29],0[0]",'G'));
		
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
		assertEquals(false, SnpUtils.isClassAIndel("PASS;NNS;HCOVT;HCOVN;MIN;HOMCON_7"));
	}
	
}
