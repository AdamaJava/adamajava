package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;
import gnu.trove.map.TLongCharMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.AccumulatorUtils;

public class AccumulatorTest {
	
	public static final Rule ruleZeroToTwentyThree = new Rule(0,20,3);
	public static final Rule ruleZeroToThirtyThree = new Rule(0,30,3);
	
	@Test
	public void testAccumulator() {
		Accumulator acc = new Accumulator(1);
		
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'A'));
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));
		
		
		acc = new Accumulator(1);
		acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'A', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 2, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		acc.addBase((byte)'C', (byte)10, true, 3, 1, 2, 1);
		assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));
		
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
	public void endOfReads() {
		Accumulator acc = new Accumulator(100);
		assertEquals(".", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 100, 100, 200, 1);
		assertEquals("C1[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 1, 100, 200, 1);
		assertEquals("C1[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 99, 100, 200, 1);
		assertEquals("C2[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 95, 100, 200, 1);
		assertEquals("C2[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 1, 100, 106, 1);
		assertEquals("C2[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 1, 100, 105, 1);
		assertEquals("C2[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 1, 100, 104, 1);
		assertEquals("C3[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'C', (byte)10, true, 1, 100, 100, 1);
		assertEquals("C4[]0[]", AccumulatorUtils.getEndOfReadsPileup(acc));
		acc.addBase((byte)'T', (byte)10, false, 98, 100, 104, 2);
		assertEquals("C4[]0[];T0[]1[]", AccumulatorUtils.getEndOfReadsPileup(acc));
	}
	
	@Test
	public void testUnfilteredPileup() {
		Accumulator acc = new Accumulator(1);
		String basesString = "ACGT";
		for (byte b : basesString.getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A1;C1;G1;T1", acc.getFailedFilterPileup());
		for (byte b : basesString.getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A2;C2;G2;T2", acc.getFailedFilterPileup());
		for (byte b : basesString.getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A3;C3;G3;T3", acc.getFailedFilterPileup());
	}
	
	@Test
	public void testUnfilteredPileupPercentage() {
		Accumulator acc = new Accumulator(1);
		for (int i = 0 ; i < 100 ; i ++) {
			acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, i);
		}
		
		String basesString = "GG";
		for (byte b : basesString.getBytes()) {
			acc.addFailedFilterBase(b);
		}
		assertEquals("G2", acc.getFailedFilterPileup());
		/*
		 * need 3 percent
		 */
		basesString = "G";
		for (byte b : basesString.getBytes()) {
			acc.addFailedFilterBase(b);
		}
		assertEquals("G3", acc.getFailedFilterPileup());
	}
	
	
	@Test
	public void singleUnfilteredPileup() {
		Accumulator acc = new Accumulator(1);
		for (byte b : "ACGT".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A1;C1;G1;T1", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ACGTA".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A2;C1;G1;T1", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ACCGT".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A1;C2;G1;T1", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "ATTTGT".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A1;G1;T4", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAATTTGT".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A4;G1;T4", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAACTTTCGT".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A4;C2;G1;T4", acc.getFailedFilterPileup());
		
		acc = new Accumulator(1);
		for (byte b : "AAAACTTTCGTG".getBytes()) acc.addFailedFilterBase(b);
		assertEquals("A4;C2;G2;T4", acc.getFailedFilterPileup());
	}
	
	@Test
	public void canContributeToGenotype() {
		assertEquals(false, AccumulatorUtils.canContributeToGenotype(null, 0, 0, null, false, 0.0));
		assertEquals(false, AccumulatorUtils.canContributeToGenotype(new int[] {}, 0, 0, null, false, 0.0));
		assertEquals(false, AccumulatorUtils.canContributeToGenotype(new int[] {1,2,3,4,5,6}, 0, 0, null, false, 0.0));
		assertEquals(true, AccumulatorUtils.canContributeToGenotype(new int[] {1,2,3,4,5,6}, 5, 7, ruleZeroToTwentyThree, false, 10.0));
		assertEquals(true, AccumulatorUtils.canContributeToGenotype(new int[] {10,100,1,0,0,0}, 10, 100, ruleZeroToTwentyThree, false, 10.0));
		assertEquals(true, AccumulatorUtils.canContributeToGenotype(new int[] {3,30,1,0,0,0}, 13, 130, ruleZeroToTwentyThree, false, 10.0));
	}
	
	@Test
	public void getCountAndQualCombo() {
		assertEquals(0l, AccumulatorUtils.getCountAndQualCombo(null));
		assertEquals(0l, AccumulatorUtils.getCountAndQualCombo(new int[] {}));
		assertEquals(0l, AccumulatorUtils.getCountAndQualCombo(new int[] {1,2,3,4,5}));
		
		// this should be 5 << 32 and 7
		assertEquals(21474836487l, AccumulatorUtils.getCountAndQualCombo(new int[] {1,2,3,4,5,6}));
		// this should be 5 << 32 and 0
		assertEquals(21474836480l, AccumulatorUtils.getCountAndQualCombo(new int[] {1,0,3,4,0,6}));
	}
	
	@Test
	public void testGetGenotypeEnum() {
		Accumulator acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AA, AccumulatorUtils.getGenotype(acc, 'A', ruleZeroToTwentyThree, false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 2 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AA, AccumulatorUtils.getGenotype(acc, 'A', ruleZeroToTwentyThree, false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 2 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		 acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.AC, AccumulatorUtils.getGenotype(acc, 'A', ruleZeroToTwentyThree, false, 10.0));
		
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 9 ; i++) acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		try {	// coverage exceeds rule
			assertEquals(GenotypeEnum.AC, AccumulatorUtils.getGenotype(acc, 'C', ruleZeroToTwentyThree, false, 10.0));
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch(IllegalArgumentException e) {}
		
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'C', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'G', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'T', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'A', ruleZeroToThirtyThree, false, 10.0));
		
		// add another T - equals numbers of Ts and Gs 
		acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 9 ; i++) acc.addBase((byte)'G', (byte)10, true, 1, 1, 2, 1);
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		acc.addBase((byte)'T', (byte)10, true, 1, 1, 2, 1);
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'C', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'G', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CT, AccumulatorUtils.getGenotype(acc, 'T', ruleZeroToThirtyThree, false, 10.0));
		assertEquals(GenotypeEnum.CG, AccumulatorUtils.getGenotype(acc, 'A', ruleZeroToThirtyThree, false, 10.0));
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
		
		assertEquals("C1[42]5[42];G0[0]60[40]", AccumulatorUtils.getOABS(acc));
		/*
		 * first pass, we get hom wildtype
		 */
		assertEquals("GG", AccumulatorUtils.getGenotype(acc, 'G', new Rule(51, Integer.MAX_VALUE, 10), false, 10).toString());
		/*
		 * second pass, we get hom for alt!!!
		 */
		assertEquals("CC", AccumulatorUtils.getGenotype(acc, 'G', new Rule(51, Integer.MAX_VALUE, 10), true, 10).toString());
	}
	
	@Test
	public void readIdBaseMap() {
		
		Accumulator acc = new Accumulator(1);
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, i);
		
		TLongCharMap map = AccumulatorUtils.getReadNameHashBaseMap(acc);
		assertEquals(10, map.size());
		
		for (int i = 10 ; i < 20 ; i++) acc.addBase((byte)'C', (byte)10, true, 1, 1, 2, i);
		
		map = AccumulatorUtils.getReadNameHashBaseMap(acc);
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
		assertEquals(".", AccumulatorUtils.getOABS(acc));
		
		for (int i = 0 ; i < 10 ; i++) acc.addBase((byte)'A', (byte)10, true, 1, 1, 2, i);
		assertEquals("A10[10]0[0]", AccumulatorUtils.getOABS(acc));
		for (int i = 0 ; i < 12 ; i++) acc.addBase((byte)'A', (byte)12, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12]", AccumulatorUtils.getOABS(acc));
		
		for (int i = 0 ; i < 15 ; i++) acc.addBase((byte)'C', (byte)15, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C0[0]15[15]", AccumulatorUtils.getOABS(acc));
		for (int i = 0 ; i < 18 ; i++) acc.addBase((byte)'C', (byte)18, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15]", AccumulatorUtils.getOABS(acc));
		
		for (int i = 0 ; i < 20 ; i++) acc.addBase((byte)'T', (byte)1, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];T0[0]20[1]", AccumulatorUtils.getOABS(acc));
		for (int i = 0 ; i < 19 ; i++) acc.addBase((byte)'G', (byte)33, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]0[0];T0[0]20[1]", AccumulatorUtils.getOABS(acc));
		
		for (int i = 0 ; i < 6 ; i++) acc.addBase((byte)'G', (byte)32, false, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]6[32];T0[0]20[1]", AccumulatorUtils.getOABS(acc));
		for (int i = 0 ; i < 8 ; i++) acc.addBase((byte)'T', (byte)3, true, 1, 1, 2, i);
		assertEquals("A10[10]12[12];C18[18]15[15];G19[33]6[32];T8[3]20[1]", AccumulatorUtils.getOABS(acc));
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
		TLongCharMap map = AccumulatorUtils.getReadNameHashBaseMap(acc);
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
