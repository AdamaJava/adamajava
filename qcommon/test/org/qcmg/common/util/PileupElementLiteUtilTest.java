package org.qcmg.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.Rule;


public class PileupElementLiteUtilTest {
	
	
	@Test
	public void testPassesWeightedVotingCheck() {
		int totalQualityScore = 0;
		int variantQualityScore = 0;
		int percentage = 0;
		assertEquals(false, PileupElementLiteUtil.passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage));
		
		totalQualityScore = 10;
		variantQualityScore = 1;
		percentage = 10;
		assertEquals(true, PileupElementLiteUtil.passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage));
		
		totalQualityScore = 10;
		variantQualityScore = 1;
		percentage = 11;
		assertEquals(false, PileupElementLiteUtil.passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage));
		
		assertEquals(false, PileupElementLiteUtil.passesWeightedVotingCheck(100, 0, 10));
		assertEquals(false, PileupElementLiteUtil.passesWeightedVotingCheck(100, 9, 10));
		assertEquals(true, PileupElementLiteUtil.passesWeightedVotingCheck(100, 1, 1));
		assertEquals(true, PileupElementLiteUtil.passesWeightedVotingCheck(100, 10, 1));
		assertEquals(true, PileupElementLiteUtil.passesWeightedVotingCheck(100, 100, 1));
		
		assertEquals(false, PileupElementLiteUtil.passesWeightedVotingCheck(100, 2, (double)5/2));
		assertEquals(true, PileupElementLiteUtil.passesWeightedVotingCheck(100, 3, (double)5/2));
	}
	
	
	@Test
	public void convertToLong() {
		long x = 0;
//		x |= 0x8000000000000000l;
		x |= 0x4000000000000000l;
//		x += 1l << 62;
		System.out.println("x (binary): " + Long.toBinaryString(x));
		System.out.println("x (hex): " + Long.toHexString(x));
		System.out.println("x (dec): " + x);
		System.out.println("((x >>> 63) & 1): " + ((x >>> 63) & 1));
		System.out.println("((x >>> 62) & 1): " + ((x >>> 62) & 1));
		System.out.println("((x >>> 61) & 1): " + ((x >>> 61) & 1));
		
		
		
		long l = PileupElementLite.convertStrandQualAndPositionToLong(true, true, (byte) 10, 1);
		System.out.println("l forward strand: " + l);
		System.out.println("l forward strand: " + Long.toBinaryString(l));
		System.out.println("l Long.parseLong: " + Long.parseLong("0100000000000000000000000000000000000000000000000000000000000000", 2));
		System.out.println("l Long.toBinaryString: " + Long.toBinaryString(Long.parseLong("0100000000000000000000000000000000000000000000000000000000000000", 2)));
		assertEquals(true, l < 0);
		assertArrayEquals(new int[] {1,1,10,1}, PileupElementLite.convertLongToStrandQualAndPosition(l));
		l = PileupElementLite.convertStrandQualAndPositionToLong(false, true, (byte) 10, 1);
		System.out.println("l reverse strand: " + l);
		System.out.println("l reverse strand: " + Long.toBinaryString(l));
		assertEquals(true, l >= 0);
		assertArrayEquals(new int[] {0,1,10,1}, PileupElementLite.convertLongToStrandQualAndPosition(l));
	}
	
	@Test
	public void getDetailsFromCombinedListInMap() {
		TIntList l = new TIntArrayList();
		l.add(1);	// readid
		l.add(2);	// startPos
		l.add(3);	// qual
		
		assertEquals(1, PileupElementLiteUtil.getDetailsFromCombinedListInMap(l, 3, 0,1).size());
		assertEquals(2, PileupElementLiteUtil.getDetailsFromCombinedListInMap(l, 3, 0,1).get(1));
		
		l.add(2);	// readid
		l.add(2);	// startPos
		l.add(3);	// qual
		
		assertEquals(2, PileupElementLiteUtil.getDetailsFromCombinedListInMap(l, 3, 0,1).size());
		assertEquals(2, PileupElementLiteUtil.getDetailsFromCombinedListInMap(l, 3, 0,1).get(1));
		assertEquals(2, PileupElementLiteUtil.getDetailsFromCombinedListInMap(l, 3, 0,1).get(2));
	}
	
	@Test
	public void strandBias() {
		try {
			PileupElementLiteUtil.areBothStrandsRepresented(null, -1);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		PileupElementLite pel = new PileupElementLite();
		try {
			PileupElementLiteUtil.areBothStrandsRepresented(pel, -1);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,0));
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,10));
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,100));
		
		// add some data to pel
		pel.add(1l, true, (byte) 64, 100, false);
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,0));
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,10));
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,100));
		
		// now add in some reverse strand data
		pel.add(2l, false, (byte) 64, 100, false);
		assertEquals(true, PileupElementLiteUtil.areBothStrandsRepresented(pel,0));
		assertEquals(true, PileupElementLiteUtil.areBothStrandsRepresented(pel,49));
		assertEquals(true, PileupElementLiteUtil.areBothStrandsRepresented(pel,50));
		assertEquals(false, PileupElementLiteUtil.areBothStrandsRepresented(pel,51));
	}
	
	@Test
	public void testPassesCountCheckFirstPass() {
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(0, 0, null));
		try {
			PileupElementLiteUtil.passesCountCheck(1, 0, new Rule(0,20,3));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			PileupElementLiteUtil.passesCountCheck(0, -1, new Rule(0,20,3), false);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(0, 0, new Rule(0,0,0), false));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(0, 0, new Rule(0,20,3)));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(1, 10, new Rule(0,20,3), false));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(2, 10, new Rule(0,20,3)));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(3, 10, new Rule(0,20,3), false));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(4, 10, new Rule(0,20,3)));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(10, 10, new Rule(0,20,3), false));
	}
	
	@Test
	public void testRealLifeData() {
		// should this pass the second pass check?
		// T:A22[32.91],28[34.79],T1[35],4[39.75]
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(5, 55, new Rule(51,Integer.MAX_VALUE,10), false));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(5, 55, new Rule(51,Integer.MAX_VALUE,10), true));
	}
	
	@Test
	public void testPassesCountCheckSecondPass() {
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(0, 0, null, true));
		try {
			PileupElementLiteUtil.passesCountCheck(1, 0, new Rule(0,20,3), true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			PileupElementLiteUtil.passesCountCheck(0, -1, new Rule(0,20,3), true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(0, 0, new Rule(0,0,0), true));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(0, 0, new Rule(0,20,3), true));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(1, 10, new Rule(0,20,3), true));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(2, 10, new Rule(0,20,3), true));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(3, 10, new Rule(0,20,3), true));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(4, 10, new Rule(0,20,3), true));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(10, 10, new Rule(0,20,3), true));
		
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(0, 100, new Rule(0,Integer.MAX_VALUE,10), true));
		assertEquals(false, PileupElementLiteUtil.passesCountCheck(4, 100, new Rule(0,Integer.MAX_VALUE,10), true));
		// should pas at 5%
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(5, 100, new Rule(0,Integer.MAX_VALUE,10), true));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(10, 100, new Rule(0,Integer.MAX_VALUE,10), true));
		assertEquals(true, PileupElementLiteUtil.passesCountCheck(100, 100, new Rule(0,Integer.MAX_VALUE,10), true));
		
	}
	
	@Test
	public void testIsAccumulatorAKeeper() {
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(null, '\u0000', null, 0)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(null, '\u0000', null, 0)[1]);
		Accumulator accum = new Accumulator(1);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, '\u0000', null, 0)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, '\u0000', null, 0)[1]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, '\u0000', new Rule(0,20,3), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, '\u0000', new Rule(0,20,3), 10)[1]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[1]);
		byte C = 'C';
		accum.addBase(C, (byte) 1, true, 1, 1, 2, 1);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[1]);
		accum = new Accumulator(1);
		accum.addBase(C, (byte) 1, true, 1, 1, 2, 1);
		accum.addBase(C, (byte) 1, true, 1, 1, 2, 1);
		accum.addBase(C, (byte) 1, false, 1, 1, 2, 1);
		// second pass pass
		assertEquals(true, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,3), 10)[1]);
		
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,4), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,20,4), 10)[1]);
		
		assertEquals(true, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,4), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,4), 10)[1]);
		// add in some reference bases
		accum = new Accumulator(1);
		accum.addBase(C, (byte) 1, true, 1, 1, 2, 1);
		accum.addBase(C, (byte) 1, true, 1, 1, 2, 1);
		accum.addBase(C, (byte) 1, false, 1, 1, 2, 1);
		byte A = 'A';
		for (int i = 0 ; i < 97 ; i++) {
			accum.addBase(A, (byte) 1, true, 1, 1, 2, 1);
		}
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,6), 3)[0]);
		assertEquals(true, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,6), 3)[1]);
		
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,6), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,6), 10)[1]);
		
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,7), 3)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,7), 3)[1]);
		
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,7), 10)[0]);
		assertEquals(false, PileupElementLiteUtil.isAccumulatorAKeeper(accum, 'A', new Rule(0,Integer.MAX_VALUE,7), 10)[1]);
	}
}
