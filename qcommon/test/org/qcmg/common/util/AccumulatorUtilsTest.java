package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.qcmg.common.model.Accumulator;

public class AccumulatorUtilsTest {
	
	@Test
	public void getReadCount() {
		Accumulator acc = new Accumulator(1);
		
		assertEquals(0, AccumulatorUtils.getReadCount(acc));
		acc.addBase((byte)'C', (byte) 1, true, 1, 1, 2, 1);
		assertEquals(1, AccumulatorUtils.getReadCount(acc));
		acc.addBase((byte)'C', (byte) 1, true, 1, 1, 2, 2);
		assertEquals(2, AccumulatorUtils.getReadCount(acc));
		assertEquals(0, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('C')));
		acc.addBase((byte)'T', (byte) 1, true, 1, 1, 2, 2);
		assertEquals(1, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('C')));
		assertEquals(2, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('T')));
		assertEquals(3, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('X')));
	}
	
	@Test
	public void getReadIdStartPosMap() {
		Accumulator acc = new Accumulator(150);
		acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(acc).size());
		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(acc).get(1));
		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(acc).get(-2));
		
		Accumulator acc2 = new Accumulator(151);
		acc2.addBase((byte)'G', (byte) 1, true, 100, 151, 200, 1);
		acc2.addBase((byte)'G', (byte) 1, false, 100, 151, 200, 2);
		
		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(acc2).size());
		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(acc2).get(1));
		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(acc2).get(-2));
		
		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).size());
		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).get(1));
		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).get(-2));
	}
	
	@Test
	public void bothStrands() {
		Accumulator acc = new Accumulator(1);
		assertEquals(false, AccumulatorUtils.bothStrands(acc));
		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
		assertEquals(false, AccumulatorUtils.bothStrands(acc));
		acc.addBase((byte)'G', (byte) 1, false, 1, 1, 2, 1);
		assertEquals(true, AccumulatorUtils.bothStrands(acc));
	}
	
	@Test
	public void bothStrandsPercentage() {
		Accumulator acc = new Accumulator(1);
		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
		acc.addBase((byte)'G', (byte) 1, false, 1, 1, 2, 1);
		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 20));
		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 30));
		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 40));
	}
	
	@Test
	public void strandBias() {
		Accumulator acc = AccumulatorUtils.createFromOABS("G0[0]3[26];T1[2]4[36.75]", 100);
		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 5));
	}
	
	@Test
	public void createFromOABS() {
		String oabs = "A10[20]0[0];C2[10]10[9];G23[1]1[40];T0[0]40[1]";
		Accumulator acc = AccumulatorUtils.createFromOABS(oabs, 100);
		assertEquals(true, acc.containsMultipleAlleles());
		assertEquals(10, acc.getBaseCountForBase('A'));
		assertEquals(12, acc.getBaseCountForBase('C'));
		assertEquals(24, acc.getBaseCountForBase('G'));
		assertEquals(40, acc.getBaseCountForBase('T'));
	}
}
