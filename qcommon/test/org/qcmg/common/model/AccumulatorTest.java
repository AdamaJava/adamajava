package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;
import gnu.trove.map.TIntCharMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class AccumulatorTest {
	
	@Test
	public void testAccumulator() {
		Accumulator acc = new Accumulator(1);
		
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		assertEquals(2, acc.getNovelStartsCountForBase('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		assertEquals(2, acc.getNovelStartsCountForBase('C'));
		
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		assertEquals(2, acc.getNovelStartsCountForBase('C'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		// try adding a base to the acc that has a different start position
		try {
			acc.addBase((byte)'C', (byte)10, true, 3, 2, 2, 1);
			Assert.fail("Should have thrown an IllegalArgExc");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testUnfilteredPileup() {
		Accumulator acc = new Accumulator(1);
		String basesString = "ACGTACGTGTACACT";
		for (byte b : basesString.getBytes()) acc.addUnfilteredBase(b);
//		Assert.assertEquals("ACGT", acc.getUnfilteredPileup());
		assertEquals("A4;C4;G3;T4", acc.getUnfilteredPileup());
	}
	
	@Test
	public void singleUnfilteredPileup() {
		Accumulator acc = new Accumulator(1);
		for (byte b : "ACGT".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A1;C1;G1;T1", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ACGTA".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A2;C1;G1;T1", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ACCGT".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A1;C2;G1;T1", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ATTTGT".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A1;G1;T4", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAATTTGT".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A4;G1;T4", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAACTTTCGT".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A4;C2;G1;T4", acc.getUnfilteredPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAACTTTCGTG".getBytes()) acc.addUnfilteredBase(b);
		Assert.assertEquals("A4;C2;G2;T4", acc.getUnfilteredPileup());
	}
	
	@Test
	public void testToSamtoolsPileupString() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals("..", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals("...", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals("....", acc.toSamtoolsPileupString('A'));
		assertEquals("AAAA", acc.toSamtoolsPileupString('C'));
		assertEquals("AAAA", acc.toSamtoolsPileupString('G'));
		assertEquals("AAAA", acc.toSamtoolsPileupString('T'));
	}
	
//	@Test
//	public void getAlleleicDistribution() {
//		Accumulator acc = new Accumulator(1);
//		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
//		
//		assertEquals("1,0", acc.getAlleleicFrequencies('A',null));
//		assertEquals("1,0,0", acc.getAlleleicFrequencies('A',"A"));
//		assertEquals("1,0,0", acc.getAlleleicFrequencies('A',"C"));
//		assertEquals("1,0,0", acc.getAlleleicFrequencies('A',"T"));
//		assertEquals("1,0,0", acc.getAlleleicFrequencies('A',"X"));
//		
//		for (int i = 0 ; i < 5 ; i++) acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, i);
//		assertEquals("1,5", acc.getAlleleicFrequencies('A',null));
//		assertEquals("1,0,5", acc.getAlleleicFrequencies('A',"A"));
//		assertEquals("1,0,5", acc.getAlleleicFrequencies('A',"C"));
//		assertEquals("1,5,0", acc.getAlleleicFrequencies('A',"G"));
//		assertEquals("1,0,5", acc.getAlleleicFrequencies('A',"X"));
//		
//		assertEquals("1,0,5,0", acc.getAlleleicFrequencies('A',"C,G"));
//		assertEquals("1,5,0,0", acc.getAlleleicFrequencies('A',"G,C"));
//		
//		for (int i = 0 ; i < 3 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, i);
//		assertEquals("1,0,5,3", acc.getAlleleicFrequencies('A',"C,G"));
//		assertEquals("1,0,5,3,0", acc.getAlleleicFrequencies('A',"C,G,T"));
//		assertEquals("1,3,5", acc.getAlleleicFrequencies('A',"T"));
//	}
	
	@Test
	public void testToSamtoolsPileupString2() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".C", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".CG", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".CGT", acc.toSamtoolsPileupString('A'));
		assertEquals("A.GT", acc.toSamtoolsPileupString('C'));
		assertEquals("AC.T", acc.toSamtoolsPileupString('G'));
		assertEquals("ACG.", acc.toSamtoolsPileupString('T'));
	}
	@Test
	public void testToSamtoolsPileupStringReverse() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",,", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",,,", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",,,,", acc.toSamtoolsPileupString('A'));
		assertEquals("aaaa", acc.toSamtoolsPileupString('C'));
		assertEquals("aaaa", acc.toSamtoolsPileupString('G'));
		assertEquals("aaaa", acc.toSamtoolsPileupString('T'));
	}
	
	@Test
	public void testToSamtoolsPileupStringReverse2() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'C', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",c", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'G', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",cg", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 1);
		assertEquals(",cgt", acc.toSamtoolsPileupString('A'));
		assertEquals("a,gt", acc.toSamtoolsPileupString('C'));
		assertEquals("ac,t", acc.toSamtoolsPileupString('G'));
		assertEquals("acg,", acc.toSamtoolsPileupString('T'));
	}
	
	@Test
	public void testToSamtoolsPileupStringCombined() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals(".,", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals("..,", acc.toSamtoolsPileupString('A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, false, 1, 1, 2, 1);
		assertEquals("..,,", acc.toSamtoolsPileupString('A'));
		assertEquals("AAaa", acc.toSamtoolsPileupString('C'));
		assertEquals("AAaa", acc.toSamtoolsPileupString('G'));
		assertEquals("AAaa", acc.toSamtoolsPileupString('T'));
	}
	@Test
	public void testToSamtoolsPileupStringCombined2() {
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'C', (byte)10, false, 1, 1, 2, 1);
		assertEquals(".c", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		assertEquals(".cG", acc.toSamtoolsPileupString('A'));
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 1);
		assertEquals(".cGt", acc.toSamtoolsPileupString('A'));
		assertEquals("A,Gt", acc.toSamtoolsPileupString('C'));
		assertEquals("Ac.t", acc.toSamtoolsPileupString('G'));
		assertEquals("AcG,", acc.toSamtoolsPileupString('T'));
	}
	
	@Test
	public void testGetGenotypeEnum() {
		Accumulator acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AA, acc.getGenotype('A', new Rule(0,20,3), false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 2 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AA, acc.getGenotype('A', new Rule(0,20,3), false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 2 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		 acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AC, acc.getGenotype('A', new Rule(0,20,3), false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 9 ; i++) acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		try {	// coverage exceeds rule
			assertEquals(GenotypeEnum.AC, acc.getGenotype('C', new Rule(0,20,3), false, 10.0));
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch(IllegalArgumentException e) {}
		
		assertEquals(GenotypeEnum.CG, acc.getGenotype('C', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CG, acc.getGenotype('G', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CG, acc.getGenotype('T', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CG, acc.getGenotype('A', new Rule(0,30,3), false, 10.0));
		
		// add another T - equals numbers of Ts and Gs 
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 9 ; i++) acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.CG, acc.getGenotype('C', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CG, acc.getGenotype('G', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CT, acc.getGenotype('T', new Rule(0,30,3), false, 10.0));
		assertEquals(GenotypeEnum.CG, acc.getGenotype('A', new Rule(0,30,3), false, 10.0));
	}
	
	@Test
	public void getGenotypeRealLife() {
		/*
		 * C1[42],5[42],G0[0],60[40.07]
		 */
		Accumulator acc = new Accumulator(1);
		for (int i = 1 ; i <= 60 ; i++) acc.addBase((byte)'G', (byte)40, false, 1, 1, 2, i);
		for (int i = 1 ; i <= 5 ; i++) acc.addBase((byte)'C', (byte)42, false, 1, 1, 2, i + 61);
		for (int i = 1 ; i <= 1 ; i++) acc.addBase((byte)'C', (byte)42, true, 1, 1, 2, i + 67);
		
		assertEquals("C1[42]5[42];G0[0]60[40]", acc.getObservedAllelesByStrand());
		/*
		 * first pass, we get hom wildtype
		 */
		assertEquals("GG", acc.getGenotype('G', new Rule(51, Integer.MAX_VALUE, 10), false, 10).toString());
		/*
		 * second pass, we get hom for alt!!!
		 */
		assertEquals("CC", acc.getGenotype('G', new Rule(51, Integer.MAX_VALUE, 10), true, 10).toString());
	}
	
//	@Test
//	public void testGetPileupElementString() {
//		Accumulator acc = new Accumulator(1);
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
//		assertEquals("A10[10],0[0]", acc.getPileupElementString());
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, false, 1, 1, 2, 1);
//		assertEquals("A10[10],0[0],C0[0],10[10]", acc.getPileupElementString());
//	}
//	@Test
//	public void testGetPileup() {
//		Accumulator acc = new Accumulator(1);
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
//		assertEquals("AAAAAAAAAA", acc.getPileup());
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, false, 1, 1, 2, 1);
//		assertEquals("AAAAAAAAAAcccccccccc", acc.getPileup());
//		for (int i = 0 ; i < 5 ; i++) acc.addBase((byte)'G', (byte)10, false, 1, 1, 2, 1);
//		assertEquals("AAAAAAAAAAccccccccccggggg", acc.getPileup());
//		for (int i = 0 ; i < 3 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
//		assertEquals("AAAAAAAAAAccccccccccgggggTTT", acc.getPileup());
//	}
	
//	@Test
//	public void testGetCompressedPileup() {
//		Accumulator acc = new Accumulator(1);
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
//		assertEquals("A", acc.getCompressedPileup());
//		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, false, 1, 1, 2, 1);
//		assertEquals("AC", acc.getCompressedPileup());
//		for (int i = 0 ; i < 5 ; i++) acc.addBase((byte)'G', (byte)10, false, 1, 1, 2, 1);
//		assertEquals("ACG", acc.getCompressedPileup());
//		for (int i = 0 ; i < 3 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
//		assertEquals("ACGT", acc.getCompressedPileup());
//	}
	
	@Test
	public void testContainsMutation() {
		Accumulator acc = new Accumulator(1010101);
		assertEquals(false, acc.containsMultipleAlleles());
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'T', (byte)10, (i % 2 == 0) ? false : true, 1010100, 1010101, 1010101, 1);
		assertEquals(false, acc.containsMultipleAlleles());
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'T', (byte)10, (i % 2 == 0) ? false : true, 1010100, 1010101, 1010101, 1);
		assertEquals(false, acc.containsMultipleAlleles());
		acc.addBase((byte)'G', (byte)10, true, 1010100, 1010101, 1010101, 1);
		assertEquals(true, acc.containsMultipleAlleles());
		acc.addBase((byte)'A', (byte)10, true, 1010100, 1010101, 1010101, 1);
		assertEquals(true, acc.containsMultipleAlleles());
		acc.addBase((byte)'C', (byte)10, true, 1010100, 1010101, 1010101, 1);
		assertEquals(true, acc.containsMultipleAlleles());
	}
	
	@Test
	public void testGetBase() {
		Accumulator acc = new Accumulator(1010101);
		assertEquals(false, acc.containsMultipleAlleles());
		assertEquals('\u0000', acc.getBase());
		acc.addBase((byte)'C', (byte)10, false, 1010100, 1010101, 1010102, 1);
		assertEquals('C', acc.getBase());
		acc.addBase((byte)'C', (byte)10, true, 1010100, 1010101, 1010102, 1);
		assertEquals('C', acc.getBase());
		acc.addBase((byte)'T', (byte)10, true, 1010100, 1010101, 1010102, 1);
		try {
			assertEquals('C', acc.getBase());
			Assert.fail("Should have thrown a wobbly");
		} catch (UnsupportedOperationException uoe) {}
	}
	
//	@Test
//	public void testGetPileupQualities() {
//		Accumulator acc = new Accumulator(1010101);
//		acc.addBase((byte)'G', (byte)'(', true, 1010100, 1010101, 1010102, 1);
//		assertEquals("I", acc.getPileupQualities());
//		acc.addBase((byte)'G', (byte)'(', true, 1010100, 1010101, 1010102, 1);
//		assertEquals("II", acc.getPileupQualities());
//		acc.addBase((byte)'A', (byte)'$', false, 1010100, 1010101, 1010102, 1);
//		assertEquals("EII", acc.getPileupQualities());
//		acc.addBase((byte)'T', (byte)'&', false, 1010100, 1010101, 1010102, 1);
//		acc.addBase((byte)'T', (byte)'&', false, 1010100, 1010101, 1010102, 1);
//		assertEquals("EIIGG", acc.getPileupQualities());
//		acc.addBase((byte)'C', (byte)'\'', false, 1010100, 1010101, 1010102, 1);
//		acc.addBase((byte)'C', (byte)'!', true, 1010100, 1010101, 1010102, 1);
//		acc.addBase((byte)'C', (byte)'!', false, 1010100, 1010101, 1010102, 1);
//		acc.addBase((byte)'C', (byte)'\'', true, 1010100, 1010101, 1010102, 1);
//		assertEquals("EBHBHIIGG", acc.getPileupQualities());
//	}
	
	@Test
	public void readIdBaseMap() {
		
		Accumulator acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, i);
		
		TIntCharMap map = acc.getReadIdBaseMap();
		assertEquals(10, map.size());
		
		for (int i = 10 ; i < 20 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, i);
		
		map = acc.getReadIdBaseMap();
		assertEquals(20, map.size());
		
		for (int i = 0 ; i < 20 ; i++ ) {
			if (i < 10) {
				assertEquals('A', map.get(i));
			} else if (i < 20) {
				assertEquals('C', map.get(i));
			}
		}
	}
	
	@Test
	public void getOABS() {
		Accumulator acc = new Accumulator(1);
		assertEquals(".", acc.getObservedAllelesByStrand());
		
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, i);
		assertEquals("A10[10]0[0]", acc.getObservedAllelesByStrand());
		for (int i = 0 ; i < 12 ; i++) acc.addBase((byte)'A', (byte)12, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12]", acc.getObservedAllelesByStrand());
		
		for (int i = 0 ; i < 15 ; i++) acc.addBase((byte)'C', (byte)15, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C0[0]15[15]", acc.getObservedAllelesByStrand());
		for (int i = 0 ; i < 18 ; i++) acc.addBase((byte)'C', (byte)18, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15]", acc.getObservedAllelesByStrand());
		
		for (int i = 0 ; i < 20 ; i++) acc.addBase((byte)'T', (byte)1, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];T0[0]20[1]", acc.getObservedAllelesByStrand());
		for (int i = 0 ; i < 19 ; i++) acc.addBase((byte)'G', (byte)33, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]0[0];T0[0]20[1]", acc.getObservedAllelesByStrand());
		
		for (int i = 0 ; i < 6 ; i++) acc.addBase((byte)'G', (byte)32, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]6[32];T0[0]20[1]", acc.getObservedAllelesByStrand());
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)3, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]6[32];T8[3]20[1]", acc.getObservedAllelesByStrand());
	}
	
	
	
	@Test
	public void readIdBaseMapStrand() {
		
		Accumulator acc = new Accumulator(1);
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 2);
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 3);
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 4);
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 5);
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 6);
		acc.addBase((byte)'T', (byte)10, false, 1, 1, 2, 7);
		 
		// should have both forward and reverse strand info here
		TIntCharMap map = acc.getReadIdBaseMap();
		assertEquals(7, map.size());
		for (int i = 1 ; i < 8 ; i++) {
			if (i < 4) {
				assertEquals('T', map.get(i));
			} else {
				assertEquals('t', map.get(i));
			}
		}
	}
	
	@Test
	public void testAddBase() {
		Accumulator acc = new Accumulator(100000);
		int noOfLoops = 1000000;
		Random random = new Random();
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			int whoKnows = random.nextInt(101);
			acc.addBase((byte)'A', (byte)random.nextInt(101), false, 99100 + whoKnows, 100000, 1000000 + whoKnows, 1);
		}
		System.out.println("time taken for " + noOfLoops + " addBases: " + (System.currentTimeMillis() - start ));
	}
	
	@Ignore
	public void testIntToCollection() {
		int noOfLoops = 1000000;
		Random random = new Random();
		List<Integer> list = new ArrayList<>(32);
		QCMGIntArray intArray = new QCMGIntArray(32);
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			list.add(random.nextInt(100));
		}
		System.out.println("time taken for " + noOfLoops + " adds to list: " + (System.currentTimeMillis() - start ));
		
		random = new Random();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			intArray.set(i, random.nextInt(100));
		}
		System.out.println("time taken for " + noOfLoops + " adds to QCMGIntArray: " + (System.currentTimeMillis() - start ));
		
		random = new Random();
		list.clear();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			list.add(random.nextInt(100));
		}
		System.out.println("time taken for " + noOfLoops + " adds to list: " + (System.currentTimeMillis() - start ));
	}
	

}
