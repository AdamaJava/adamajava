package org.qcmg.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.PileupElementLite;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;

public class AccumulatorUtilsTest {
	
//	@Test
//	public void getReadCount() {
//		Accumulator acc = new Accumulator(1);
//		
//		assertEquals(0, AccumulatorUtils.getReadCount(acc));
//		acc.addBase((byte)'C', (byte) 1, true, 1, 1, 2, 1);
//		assertEquals(1, AccumulatorUtils.getReadCount(acc));
//		acc.addBase((byte)'C', (byte) 1, true, 1, 1, 2, 2);
//		assertEquals(2, AccumulatorUtils.getReadCount(acc));
////		assertEquals(0, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('C')));
////		acc.addBase((byte)'T', (byte) 1, true, 1, 1, 2, 2);
////		assertEquals(1, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('C')));
////		assertEquals(2, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('T')));
////		assertEquals(3, AccumulatorUtils.getReadCount(acc, acc.getPelForBase('X')));
//	}
	
	@Test
	public void baseDetails() {
		AccumulatorUtils.BaseDetails bd1 = new AccumulatorUtils.BaseDetails('A', 0, 0, false);
		AccumulatorUtils.BaseDetails bd2 = new AccumulatorUtils.BaseDetails('B', 0, 0, false);
		List<AccumulatorUtils.BaseDetails> list = Arrays.asList(bd2, bd1);
		
		list.sort(null);
		assertEquals(bd2, list.get(0));
		
		bd1 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
		list = Arrays.asList(bd2, bd1);
		list.sort(null);
		assertEquals(bd1, list.get(0));
		
		
		bd1 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
		bd2 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
		list = Arrays.asList(bd2, bd1);
		list.sort(null);
		assertEquals(bd2, list.get(0));
		
		bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, false);
		list = Arrays.asList(bd2, bd1);
		list.sort(null);
		assertEquals(bd1, list.get(0));
		
		
		bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
		bd2 = new AccumulatorUtils.BaseDetails('A', 1, 1, false);
		list = Arrays.asList(bd2, bd1);
		list.sort(null);
		assertEquals(bd1, list.get(0));
		
		bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
		bd2 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
		list = Arrays.asList(bd2, bd1);
		list.sort(null);
		assertEquals(bd2, list.get(0));
	}
	
	@Test
	public void getLong() {
		long l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, true, (byte)'A', (byte) 30, 1);
		assertEquals(true, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) != 0);	// strand
		assertEquals(true, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) != 0);	// end of read
		assertEquals(true, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)) != 0);	// A
		assertEquals(true, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)) == 0);	// C
		assertEquals(true, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)) == 0);	// G
		assertEquals(true, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)) == 0);	// T
		assertEquals(1, (int)l);
		assertEquals(30, AccumulatorUtils.getQualityFromLong(l));
		assertEquals(Accumulator.A_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));
		
		l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte)'C', (byte) 60, 100);
		assertEquals(true, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) == 0);	// strand
		assertEquals(true, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) == 0);	// end of read
		assertEquals(true, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)) == 0);	// A
		assertEquals(true, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)) != 0);	// C
		assertEquals(true, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)) == 0);	// G
		assertEquals(true, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)) == 0);	// T
		assertEquals(100, (int)l);
		assertEquals(60, AccumulatorUtils.getQualityFromLong(l));
		assertEquals(Accumulator.C_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));
		
		l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, true, (byte)'G', (byte) 40, 1234567890);
		assertEquals(true, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) == 0);	// strand
		assertEquals(true, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) != 0);	// end of read
		assertEquals(true, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)) == 0);	// A
		assertEquals(true, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)) == 0);	// C
		assertEquals(true, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)) != 0);	// G
		assertEquals(true, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)) == 0);	// T
		assertEquals(1234567890, (int)l);
		assertEquals(40, AccumulatorUtils.getQualityFromLong(l));
		assertEquals(Accumulator.G_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));
		
		l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte)'T', (byte) 12, 1);
		assertEquals(true, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) != 0);	// strand
		assertEquals(true, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) == 0);	// end of read
		assertEquals(true, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)) == 0);	// A
		assertEquals(true, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)) == 0);	// C
		assertEquals(true, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)) == 0);	// G
		assertEquals(true, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)) != 0);	// T
		assertEquals(1, (int)l);
		assertEquals(12, AccumulatorUtils.getQualityFromLong(l));
		assertEquals(Accumulator.T_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));
	}
	
	@Test
	public void largestVariant() {
		
		Accumulator acc = new Accumulator(150);
		for (int i = 0 ; i < 10 ; i++) {
			acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, i);
		}
		assertArrayEquals(new int[] {0,0,0,0,0,0}, AccumulatorUtils.getLargestVariant(acc, 'G'));
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'C'));
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'A'));
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'T'));
		
		for (int i = 20 ; i < 25 ; i++) {
			acc.addBase((byte)'T', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 25 ; i < 30 ; i++) {
			acc.addBase((byte)'T', (byte) 1, false, 100, 150, 200, i);
		}
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'T'));
		assertArrayEquals(new int[] {5,5,0,5,5,0}, AccumulatorUtils.getLargestVariant(acc, 'G'));
		
		/*
		 * add another base - 10 reads, but quality is better - this should be returned when ref is 'G'
		 */
		for (int i = 30 ; i < 35 ; i++) {
			acc.addBase((byte)'A', (byte) 2, true, 100, 150, 200, i);
		}
		for (int i = 35 ; i < 40 ; i++) {
			acc.addBase((byte)'A', (byte) 2, false, 100, 150, 200, i);
		}
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'T'));
		assertArrayEquals(new int[] {10,10,0,10,10,0}, AccumulatorUtils.getLargestVariant(acc, 'A'));
		assertArrayEquals(new int[] {5,10,0,5,10,0}, AccumulatorUtils.getLargestVariant(acc, 'G'));
	}
	
	@Test
	public void getReadNameHashStartPositionMap() {
		Accumulator acc = new Accumulator(150);
		for (int i = 0 ; i < 10 ; i++) {
			acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, i);
		}
		
		TLongIntMap map = AccumulatorUtils.getReadNameHashStartPositionMap(acc);
		assertEquals(20, map.size());
		for (int i = 0 ; i < 10 ; i++) {
			assertEquals(100, map.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			assertEquals(-200, map.get(i));		// reverse strand, and so negative
		}
	}
	
	@Test
	public void getNNs() {
		Accumulator acc = new Accumulator(150);
		for (int i = 0 ; i < 10 ; i++) {
			acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, i);
		}
		
		assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'G'));
		assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'A'));
		assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'T'));
		assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));
		
		for (int i = 20 ; i < 30 ; i++) {
			acc.addBase((byte)'C', (byte) 1, true, 100 + i, 150, 200, i);
		}
		for (int i = 30 ; i < 40 ; i++) {
			acc.addBase((byte)'C', (byte) 1, false, 100, 150, 200 + i, i);
		}
		assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'G'));
		assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'A'));
		assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'T'));
		assertEquals(20, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));
	}
	
	@Test
	public void getOABS() {
		assertEquals(null, AccumulatorUtils.toObservedAlleleByStrand(null, null));
		assertEquals(null, AccumulatorUtils.toObservedAlleleByStrand(null, new int[] {}));
		assertEquals(null, AccumulatorUtils.toObservedAlleleByStrand("blah", null));
		assertEquals(null, AccumulatorUtils.toObservedAlleleByStrand("blah", new int[] {}));
		assertEquals("blah1[2]4[1.25]", AccumulatorUtils.toObservedAlleleByStrand("blah", new int[] {1,2,3,4,5,6}));
		assertEquals("blah210[20]4[25]", AccumulatorUtils.toObservedAlleleByStrand("blah2", new int[] {10,200,0,4,100,0}));
	}
	
	@Test
	public void getReadNameHashStartPositionMapForMultipleAccs() {
		Accumulator acc1 = new Accumulator(150);
		for (int i = 0 ; i < 10 ; i++) {
			acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc1.addBase((byte)'G', (byte) 1, false, 100, 150, 200, i);
		}
		Accumulator acc2 = new Accumulator(151);
		for (int i = 0 ; i < 10 ; i++) {
			acc2.addBase((byte)'A', (byte) 1, true, 100, 151, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc2.addBase((byte)'A', (byte) 1, false, 100, 151, 200, i);
		}
		
		TLongIntMap map = AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc1, acc2));
		assertEquals(20, map.size());
		for (int i = 0 ; i < 10 ; i++) {
			assertEquals(100, map.get(i));
		}
		for (int i = 10 ; i < 20 ; i++) {
			assertEquals(-200, map.get(i));		// reverse strand, and so negative
		}
	}
	
	
	@Test
	public void getBaseCountByStrand() {
		Accumulator acc1 = new Accumulator(150);
		for (int i = 0 ; i < 10 ; i++) {
			acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc1.addBase((byte)'G', (byte) 1, false, 100, 150, 200, i);
		}
		assertArrayEquals(new int[]{0,0,0,0,10,10,0,0}, AccumulatorUtils.getBaseCountByStrand(acc1));
		Accumulator acc2 = new Accumulator(151);
		for (int i = 0 ; i < 10 ; i++) {
			acc2.addBase((byte)'A', (byte) 1, true, 100, 151, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc2.addBase((byte)'A', (byte) 1, false, 100, 151, 200, i);
		}
		assertArrayEquals(new int[]{10,10,0,0,0,0,0,0}, AccumulatorUtils.getBaseCountByStrand(acc2));
		
		for (int i = 0 ; i < 10 ; i++) {
			acc2.addBase((byte)'G', (byte) 1, true, 100, 151, 200, i);
		}
		for (int i = 10 ; i < 20 ; i++) {
			acc2.addBase((byte)'G', (byte) 1, false, 100, 151, 200, i);
		}
		assertArrayEquals(new int[]{10,10,0,0,10,10,0,0}, AccumulatorUtils.getBaseCountByStrand(acc2));
	}
	
	@Test
	public void getBaseCountByStrand2() {
		int [] array = new int [] {1,2,3,4,5,6,7,8};
		assertArrayEquals(new int[]{1,2}, AccumulatorUtils.getBaseCountByStrand(array, 'A'));
		assertArrayEquals(new int[]{3,4}, AccumulatorUtils.getBaseCountByStrand(array, 'C'));
		assertArrayEquals(new int[]{5,6}, AccumulatorUtils.getBaseCountByStrand(array, 'G'));
		assertArrayEquals(new int[]{7,8}, AccumulatorUtils.getBaseCountByStrand(array, 'T'));
	}
	
	@Test
	public void decrementBaseCountByStrandArray() {
		int [] array = new int [] {1,2,3,4,5,6,7,8};
		AccumulatorUtils.decrementBaseCountByStrandArray(array, 'A', false);
		assertArrayEquals(new int[]{1,1,3,4,5,6,7,8}, array);
		AccumulatorUtils.decrementBaseCountByStrandArray(array, 'T', true);
		assertArrayEquals(new int[]{1,1,3,4,5,6,6,8}, array);
		AccumulatorUtils.decrementBaseCountByStrandArray(array, 'C', true);
		assertArrayEquals(new int[]{1,1,2,4,5,6,6,8}, array);
		AccumulatorUtils.decrementBaseCountByStrandArray(array, 'G', true);
		assertArrayEquals(new int[]{1,1,2,4,4,6,6,8}, array);
		AccumulatorUtils.decrementBaseCountByStrandArray(array, 'G', false);
		assertArrayEquals(new int[]{1,1,2,4,4,5,6,8}, array);
	}
	
	@Test
	public void longToKeepFromOverlapPair() {
		int [] array = new int [] {1,2,3,4,5,6,7,8};
		TLongList list = new TLongArrayList();
		long l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte)'T', (byte)10, 12345);
		long l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte)'T', (byte)10, 12345);
		list.add(l1);
		list.add(l2);
		assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
		assertArrayEquals(new int [] {1,2,3,4,5,6,6,8}, array);
		
		list = new TLongArrayList();
		l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte)'T', (byte)10, 12345);
		l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte)'T', (byte)10, 12345);
		list.add(l1);
		list.add(l2);
		assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
		assertArrayEquals(new int [] {1,2,3,4,5,6,6,7}, array);
		
		list = new TLongArrayList();
		l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte)'G', (byte)10, 12345);
		l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte)'G', (byte)20, 12345);
		list.add(l1);
		list.add(l2);
		assertEquals(l2, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
		assertArrayEquals(new int [] {1,2,3,4,5,5,6,7}, array);
		
		list = new TLongArrayList();
		l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte)'G', (byte)10, 12345);
		l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte)'G', (byte)20, 12345);
		list.add(l1);
		list.add(l2);
		assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
		assertArrayEquals(new int [] {1,2,3,4,5,4,6,7}, array);
		
	}
	
	@Test
	public void overlapsTakingIntoAccountStrand() {
		Accumulator acc = new Accumulator(150);
		/*
		 * add different reads all on same strand
		 */
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 1);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 3);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 4);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 5);
		assertEquals(5, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(5, acc.getCoverage());
		assertArrayEquals(new int[]{0,5,0,0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));
		
		/*
		 * add read with same id on same strand - should be removed
		 */
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 3);
		assertEquals(6, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(5, acc.getCoverage());
		assertArrayEquals(new int[]{0,5,0,0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));
		
		/*
		 * this time add read with same id on opposite strand - will keep that one and remove the original read
		 */
		acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 3);
		assertEquals(6, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(5, acc.getCoverage());
		assertArrayEquals(new int[]{1,4,0,0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));
		
	}
	
	@Test
	public void overlaps() {
		Accumulator acc = new Accumulator(150);
		acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
		assertEquals(2, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(2, acc.getCoverage());
		
		/*
		 * add another base with the same id
		 * count will go up, but back down once removeOverlappingReads is called
		 */
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
		assertEquals(3, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(2, acc.getCoverage());
		
		/*
		 * add a different base with the same id
		 * count will go up, but back down by 2 once removeOverlappingReads is called
		 */
		acc.addBase((byte)'A', (byte) 1, false, 100, 150, 200, 2);
		assertEquals(3, acc.getCoverage());
		AccumulatorUtils.removeOverlappingReads(acc);
		assertEquals(1, acc.getCoverage());
		assertEquals('G', AccumulatorUtils.getBaseAsCharFromLong(acc.getData().get(1)));
	}
	
	@Test
	public void getBitSet() {
		TLongList list = new TLongArrayList();
		list.add(576460756598390984l);
		list.add(576460756598390984l);
		list.add(2305843013508661448l);
		list.add(2305843013508661448l);
		BitSet bs1 = AccumulatorUtils.getUniqueBases(list);
		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
	}
	
	@Test
	public void getBitSetAll() {
		TLongList list = new TLongArrayList();
		BitSet bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
		assertEquals(false, AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
		list.add(0);
		bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
		assertEquals(false, AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
		list.add(576460756598390984l);
		bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
		assertEquals(false, AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
		list.add(2305843013508661448l);
		bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
	}
	
	@Test
	public void getUniqueBases() {
		Accumulator acc = new Accumulator(150);
		acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
		BitSet bs = AccumulatorUtils.getUniqueBases(acc);
		assertEquals(false, bs.get(0));
		assertEquals(false, bs.get(1));
		assertEquals(true, bs.get(2));
		assertEquals(false, bs.get(3));
		assertEquals(false, AccumulatorUtils.doesBitSetContainMoreThan1True(bs));
		
		acc.addBase((byte)'A', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'A', (byte) 1, false, 100, 150, 200, 2);
		bs = AccumulatorUtils.getUniqueBases(acc);
		assertEquals(true, bs.get(0));
		assertEquals(false, bs.get(1));
		assertEquals(true, bs.get(2));
		assertEquals(false, bs.get(3));
		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(bs));
		
		acc.addBase((byte)'T', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'T', (byte) 1, false, 100, 150, 200, 2);
		bs = AccumulatorUtils.getUniqueBases(acc);
		assertEquals(true, bs.get(0));
		assertEquals(false, bs.get(1));
		assertEquals(true, bs.get(2));
		assertEquals(true, bs.get(3));
		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(bs));
		
		acc.addBase((byte)'C', (byte) 1, true, 100, 150, 200, 1);
		acc.addBase((byte)'C', (byte) 1, false, 100, 150, 200, 2);
		bs = AccumulatorUtils.getUniqueBases(acc);
		assertEquals(true, bs.get(0));
		assertEquals(true, bs.get(1));
		assertEquals(true, bs.get(2));
		assertEquals(true, bs.get(3));
		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(bs));
	}
	
	@Test
	public void doBitSetsMatch() {
		assertEquals(false, AccumulatorUtils.doBitSetsHaveSameBitsSet(null, null));
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(new BitSet(4), new BitSet(4)));
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(new BitSet(4), new BitSet(5)));
		BitSet bs1 = new BitSet(4);
		bs1.set(0);
		BitSet bs2 = new BitSet(4);
		bs2.set(0);
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
		bs2.set(1);
		assertEquals(false, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
		bs1.set(1);
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
		
		bs1.set(0, 3);
		bs2.set(0, 3);
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
		bs1.set(0, 3, false);
		assertEquals(false, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
		bs2.set(0, 3, false);
		assertEquals(true, AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
	}
	
	@Test
	public void bothStrandsAsPerc() {
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(0, 0, 0));
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(0, 1, 0));
		assertEquals(true, AccumulatorUtils.areBothStrandsRepresented(1, 1, 0));
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(1, 1, 100));
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(10, 10, 100));
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(10, 10, 50));
		assertEquals(false, AccumulatorUtils.areBothStrandsRepresented(10, 10, 51));
		assertEquals(true, AccumulatorUtils.areBothStrandsRepresented(10, 10, 49));
	}
	
//	@Test
//	public void getReadIdStartPosMap() {
//		Accumulator acc = new Accumulator(150);
//		acc.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
//		acc.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 2);
//		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(acc).size());
//		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(acc).get(1));
//		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(acc).get(-2));
//		
//		Accumulator acc2 = new Accumulator(151);
//		acc2.addBase((byte)'G', (byte) 1, true, 100, 151, 200, 1);
//		acc2.addBase((byte)'G', (byte) 1, false, 100, 151, 200, 2);
//		
//		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(acc2).size());
//		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(acc2).get(1));
//		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(acc2).get(-2));
//		
//		assertEquals(2, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).size());
//		assertEquals(100, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).get(1));
//		assertEquals(200, AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc, acc2)).get(-2));
//	}
	
//	@Test
//	public void bothStrands() {
//		Accumulator acc = new Accumulator(1);
//		assertEquals(false, AccumulatorUtils.bothStrands(acc));
//		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
//		assertEquals(false, AccumulatorUtils.bothStrands(acc));
//		acc.addBase((byte)'G', (byte) 1, false, 1, 1, 2, 1);
//		assertEquals(true, AccumulatorUtils.bothStrands(acc));
//	}
	
//	@Test
//	public void bothStrandsPercentage() {
//		Accumulator acc = new Accumulator(1);
//		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
//		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
//		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
//		acc.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
//		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
//		acc.addBase((byte)'G', (byte) 1, false, 1, 1, 2, 1);
//		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 10));
//		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 20));
//		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 30));
//		assertEquals(false, AccumulatorUtils.bothStrandsByPercentage(acc, 40));
//	}
//	
//	@Test
//	public void strandBias() {
//		Accumulator acc = AccumulatorUtils.createFromOABS("G0[0]3[26];T1[2]4[36.75]", 100);
//		assertEquals(true, AccumulatorUtils.bothStrandsByPercentage(acc, 5));
//	}
	
//	@Test
//	public void createFromOABS() {
//		String oabs = "A10[20]0[0];C2[10]10[9];G23[1]1[40];T0[0]40[1]";
//		Accumulator acc = AccumulatorUtils.createFromOABS(oabs, 100);
//		assertEquals(true, AccumulatorUtils.doesBitSetContainMoreThan1True(AccumulatorUtils.getUniqueBases(acc)));
//		assertEquals(10, acc.getBaseCountForBase('A'));
//		assertEquals(12, acc.getBaseCountForBase('C'));
//		assertEquals(24, acc.getBaseCountForBase('G'));
//		assertEquals(40, acc.getBaseCountForBase('T'));
//	}
}
