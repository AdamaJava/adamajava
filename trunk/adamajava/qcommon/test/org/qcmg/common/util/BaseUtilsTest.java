package org.qcmg.common.util;


import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Genotype;
import org.qcmg.common.model.GenotypeEnum;

public class BaseUtilsTest {
	
	@Test
	public void testGetComplement() {
		Assert.assertTrue('A' == BaseUtils.getComplement('T'));
		Assert.assertTrue('C' == BaseUtils.getComplement('G'));
		Assert.assertTrue('G' == BaseUtils.getComplement('C'));
		Assert.assertTrue('T' == BaseUtils.getComplement('A'));
		try {
			BaseUtils.getComplement('.');
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			BaseUtils.getComplement('X');
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			BaseUtils.getComplement('Y');
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			BaseUtils.getComplement('Z');
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			BaseUtils.getComplement('\u0000');
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testAreGenotypesEqual() {
		Assert.assertFalse(BaseUtils.areGenotypesEqual(null, null));
		try {
			BaseUtils.areGenotypesEqual("","");
			Assert.fail("Should have throws an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		Assert.assertTrue(BaseUtils.areGenotypesEqual("AA","AA"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A","A/A"));
		Assert.assertFalse(BaseUtils.areGenotypesEqual("A/A","A/C"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("C/A","A/C"));
		Assert.assertFalse(BaseUtils.areGenotypesEqual("A/T","A/C"));
		Assert.assertFalse(BaseUtils.areGenotypesEqual("A/T","G/C"));
		Assert.assertFalse(BaseUtils.areGenotypesEqual("C/G","A/C"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("T/T","TT"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A/A","A/A/A"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("A/C/G","A/C/G"));
		Assert.assertTrue(BaseUtils.areGenotypesEqual("A/A/A/T","A/A/A/T"));
	}
	
	@Test
	public void testGetGenotype() {
		Assert.assertFalse(new Genotype('A','C').equals(BaseUtils.getGenotype("AG")));
		Assert.assertTrue(new Genotype('A','C').equals(BaseUtils.getGenotype("CA")));
		Assert.assertFalse(new Genotype('G','G','G').equals(BaseUtils.getGenotype("GGG")));
	}
	
	@Test
	public void testGetGenotypeFromVcf() {
		Assert.assertNull(BaseUtils.getGenotypeFromVcf(null, 'C', 'T'));
		
		// 0/0, 0/1, 1/1 are valid input strings for this method
		assertEquals(new Genotype('C','C'), BaseUtils.getGenotypeFromVcf("0/0", 'C', 'T'));
		assertEquals(new Genotype('C','T'), BaseUtils.getGenotypeFromVcf("0/1", 'C', 'T'));
		assertEquals(new Genotype('T','T'), BaseUtils.getGenotypeFromVcf("1/1", 'C', 'T'));
		
		assertEquals(new Genotype('A','A'), BaseUtils.getGenotypeFromVcf("0/0", 'A', 'A'));
		assertEquals(new Genotype('A','A'), BaseUtils.getGenotypeFromVcf("0/1", 'A', 'A'));
		assertEquals(new Genotype('A','A'), BaseUtils.getGenotypeFromVcf("1/1", 'A', 'A'));
		
		assertEquals(new Genotype('A','A'), BaseUtils.getGenotypeFromVcf("0/0", 'A', 'G'));
		assertEquals(new Genotype('A','G'), BaseUtils.getGenotypeFromVcf("0/1", 'A', 'G'));
		assertEquals(new Genotype('G','G'), BaseUtils.getGenotypeFromVcf("1/1", 'A', 'G'));
		
	}
	
	@Test
	public void testIsValid() {
		assertEquals(false, BaseUtils.isACGT('\u0000'));
		assertEquals(true, BaseUtils.isACGT('A'));
		assertEquals(true, BaseUtils.isACGT('C'));
		assertEquals(true, BaseUtils.isACGT('G'));
		assertEquals(true, BaseUtils.isACGT('T'));
		assertEquals(false, BaseUtils.isACGT('N'));
		assertEquals(false, BaseUtils.isACGT('.'));
		assertEquals(false, BaseUtils.isACGT('X'));
		assertEquals(false, BaseUtils.isACGT('Y'));
		assertEquals(false, BaseUtils.isACGT('Z'));
		assertEquals(false, BaseUtils.isACGT('a'));
		assertEquals(false, BaseUtils.isACGT('c'));
		assertEquals(false, BaseUtils.isACGT('g'));
		assertEquals(false, BaseUtils.isACGT('t'));
	}
	
	@Test
	public void testIsValidIncludeDotN() {
		assertEquals(false, BaseUtils.isACGTNDot('\u0000'));
		assertEquals(true, BaseUtils.isACGTNDot('A'));
		assertEquals(true, BaseUtils.isACGTNDot('C'));
		assertEquals(true, BaseUtils.isACGTNDot('G'));
		assertEquals(true, BaseUtils.isACGTNDot('T'));
		assertEquals(true, BaseUtils.isACGTNDot('N'));
		assertEquals(true, BaseUtils.isACGTNDot('.'));
		assertEquals(false, BaseUtils.isACGTNDot('X'));
		assertEquals(false, BaseUtils.isACGTNDot('Y'));
		assertEquals(false, BaseUtils.isACGTNDot('Z'));
		assertEquals(false, BaseUtils.isACGTNDot('a'));
		assertEquals(false, BaseUtils.isACGTNDot('c'));
		assertEquals(false, BaseUtils.isACGTNDot('g'));
		assertEquals(false, BaseUtils.isACGTNDot('t'));
		assertEquals(false, BaseUtils.isACGTNDot('n'));
		assertEquals(false, BaseUtils.isACGTNDot(','));
	}
	
	@Test
	public void testIsValidIncludeDotNMR() {
		assertEquals(false, BaseUtils.isACGTNDotMR('\u0000'));
		assertEquals(true, BaseUtils.isACGTNDotMR('M'));
		assertEquals(true, BaseUtils.isACGTNDotMR('R'));
		assertEquals(false, BaseUtils.isACGTNDotMR('Y'));
		assertEquals(false, BaseUtils.isACGTNDotMR('Z'));
		assertEquals(false, BaseUtils.isACGTNDotMR('m'));
		assertEquals(false, BaseUtils.isACGTNDotMR('r'));
	}
	
	@Test
	public void testGetGenotypeEnum() {
		// null string returns null
		Assert.assertNull(BaseUtils.getGenotypeEnum(null));
		Assert.assertNull(BaseUtils.getGenotypeEnum("X/Y"));
		Assert.assertNull(BaseUtils.getGenotypeEnum("a/c"));
		
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("A/A"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("[A/A]"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("[AA]"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("[AA"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("[A/A"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("A/A]"));
		assertEquals(GenotypeEnum.AA,BaseUtils.getGenotypeEnum("AA]"));
		
		
	}

}
