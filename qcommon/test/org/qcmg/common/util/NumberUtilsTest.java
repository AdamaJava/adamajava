package org.qcmg.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import gnu.trove.list.TIntList;


public class NumberUtilsTest {
	
	@Test
	public void pack2IntsInto1() {
		assertEquals(0, NumberUtils.pack2IntsInto1(0, 0));
		assertEquals(1, NumberUtils.pack2IntsInto1(0, 1));
		assertEquals(1 << 16, NumberUtils.pack2IntsInto1(1, 0));
		assertEquals(65536, NumberUtils.pack2IntsInto1(1, 0));
		assertEquals(65537, NumberUtils.pack2IntsInto1(1, 1));
		assertEquals(1 << 17, NumberUtils.pack2IntsInto1(2, 0));
		assertEquals(2 << 16, NumberUtils.pack2IntsInto1(2, 0));
		assertEquals(851974, NumberUtils.pack2IntsInto1(13, 6));
	}
	
	@Test
	public void splitIntInto2() {
		assertArrayEquals(new int[] {0,0}, NumberUtils.splitIntInto2(0));
		assertArrayEquals(new int[] {0,1}, NumberUtils.splitIntInto2(1));
		assertArrayEquals(new int[] {1,0}, NumberUtils.splitIntInto2(65536));
		assertArrayEquals(new int[] {1,1}, NumberUtils.splitIntInto2(65537));
		assertArrayEquals(new int[] {2,0}, NumberUtils.splitIntInto2(1 << 17));
		assertArrayEquals(new int[] {2,2}, NumberUtils.splitIntInto2((1 << 17) + 2));
		assertArrayEquals(new int[] {6,16}, NumberUtils.splitIntInto2(393232));
	}
	
	@Test
	public void getPartOfPackedInt() {
		int packedInt = NumberUtils.pack2IntsInto1(10, 15);
		assertEquals(10, NumberUtils.getPartOfPackedInt(packedInt, true));
		assertEquals(15, NumberUtils.getPartOfPackedInt(packedInt, false));
		assertEquals(51, NumberUtils.getPartOfPackedInt(3342336, true));
		assertEquals(0, NumberUtils.getPartOfPackedInt(3342336, false));
		assertEquals(35, NumberUtils.getPartOfPackedInt(2293760, true));
		//3342336={4398963712090}
	}
	
	@Test
	public void addShortToLong() {
		long l = 1234567890l;
		short s = 1;
		int offset = 40;
		assertEquals(1100746195666l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(1, NumberUtils.getShortFromLong(1100746195666l, offset));
		assertEquals(1234567890l, NumberUtils.removeShortFromLong(1100746195666l, offset));
		
		/*
		 * now for a number that has the RC bit set
		 */
		l = NumberUtils.setBit(l, 62);
		assertEquals(4611687119173583570l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(1, NumberUtils.getShortFromLong(4611687119173583570l, offset));
		assertEquals(l, NumberUtils.removeShortFromLong(4611687119173583570l, offset));
		
		/*
		 * try a larger short value
		 */
		s = 1000;
		l = 1234567890l;
		assertEquals(1099512862343890l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(1000, NumberUtils.getShortFromLong(1099512862343890l, offset));
		assertEquals(l, NumberUtils.removeShortFromLong(1099512862343890l, offset));
		
		l = NumberUtils.setBit(l, 62);
		assertEquals(4612785531289731794l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(1000, NumberUtils.getShortFromLong(4612785531289731794l, offset));
		assertEquals(l, NumberUtils.removeShortFromLong(4612785531289731794l, offset));
//		assertEquals(l, NumberUtils.getRefPositionFromLong(4612785531289731794l));
		
		/*
		 * try an even larger short value
		 */
		s = 30000;
		l = 1234567890l;
		assertEquals(32985350067847890l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(30000, NumberUtils.getShortFromLong(32985350067847890l, offset));
		assertEquals(l, NumberUtils.removeShortFromLong(32985350067847890l, offset));
		
		l = NumberUtils.setBit(l, 62);
		assertEquals(4644671368495235794l, NumberUtils.addShortToLong(l, s, offset));
		assertEquals(30000, NumberUtils.getShortFromLong(4644671368495235794l, offset));
		assertEquals(l, NumberUtils.removeShortFromLong(4644671368495235794l, offset));
		
		assertEquals(4, NumberUtils.getShortFromLong(4398963712090l, offset));
		assertEquals(917200986, NumberUtils.removeShortFromLong(4398963712090l, offset));
	}
	
	@Test
	public void getPositionOfLongInArray() {
		long [] array = new long[] {};
		assertEquals(-1, NumberUtils.getPositionOfLongInArray(array, 1));
		
		array = new long[] {1};
		assertEquals(0, NumberUtils.getPositionOfLongInArray(array, 1));
		assertEquals(-2, NumberUtils.getPositionOfLongInArray(array, 2));
		assertEquals(-1, NumberUtils.getPositionOfLongInArray(array, 0));
		assertEquals(-2, NumberUtils.getPositionOfLongInArray(array, 10));
		
		array = new long[] {1, 10};
		assertEquals(0, NumberUtils.getPositionOfLongInArray(array, 1));
		assertEquals(1, NumberUtils.getPositionOfLongInArray(array, 10));
		assertEquals(-2, NumberUtils.getPositionOfLongInArray(array, 2));
		assertEquals(-1, NumberUtils.getPositionOfLongInArray(array, 0));
		assertEquals(-3, NumberUtils.getPositionOfLongInArray(array, 11));
		
		array = new long[] {1, 10, 100};
		assertEquals(0, NumberUtils.getPositionOfLongInArray(array, 1));
		assertEquals(1, NumberUtils.getPositionOfLongInArray(array, 10));
		assertEquals(-2, NumberUtils.getPositionOfLongInArray(array, 2));
		assertEquals(-1, NumberUtils.getPositionOfLongInArray(array, 0));
		assertEquals(-3, NumberUtils.getPositionOfLongInArray(array, 11));
		assertEquals(2, NumberUtils.getPositionOfLongInArray(array, 100));
		assertEquals(-4, NumberUtils.getPositionOfLongInArray(array, 101));
	}
	
	
	
	@Test
	public void blocks() {
		String seq = "ABCD";
		String swSeq = "ABCD";
		List<int[]> results = NumberUtils.getBlockStartPositionsNew(swSeq, seq);
		assertEquals(1, results.size());
		assertArrayEquals(new int[]{0, 4, 0}, results.get(0));
	}
	
	@Test
	public void blocksRS() {
		String seq = "CCCCGGGTAAAATGAGTTTTTTGGTCCAATCTTTTAATCCACTCCCTACCCTCCTAGCAAG";
		String swSeq =   "GGGTAAAATGAGTTTTTT--------------ATCCACTCCCTACCCTCCTA";
		List<int[]> results = NumberUtils.getBlockStartPositionsNew(swSeq, seq);
		assertEquals(2, results.size());
		assertArrayEquals(new int[]{4, 22, 0}, results.get(0));
		assertArrayEquals(new int[]{36, 56, 14}, results.get(1));
		
		/*
		 * and if reported on the +ve strand
		 */
		seq = "CTTGCTAGGAGGGTAGGGAGTGGATTAAAAGATTGGACCAAAAAACTCATTTTACCCGGGG";
		swSeq =    "TAGGAGGGTAGGGAGTGGAT--------------AAAAAACTCATTTTACCC";
		results = NumberUtils.getBlockStartPositionsNew(swSeq, seq);
		assertEquals(2, results.size());
		assertArrayEquals(new int[]{5, 25, 0}, results.get(0));
		assertArrayEquals(new int[]{39, 57, 14}, results.get(1));
	}
	
	@Test
	public void getAllPositions() {
		String [] swDiffs = new String[] {
				"AAAAAAAAAA----------GGGGGGGGGGTTTTTTTTTT",
                "||||||||||          ||||||||||||||||||||",
                "AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT"};
		List<int[]> startPositions = NumberUtils.getAllStartPositions(swDiffs);
		assertEquals(2, startPositions.size());
		assertArrayEquals(new int[]{0,10,0,10}, startPositions.get(0));
		assertArrayEquals(new int[]{20,20,10,20}, startPositions.get(1));
		
		swDiffs = new String[] {
        		"AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT",
				"||||||||||          ||||||||||||||||||||",
		        "AAAAAAAAAA----------GGGGGGGGGGTTTTTTTTTT"};
		startPositions = NumberUtils.getAllStartPositions(swDiffs);
		assertEquals(2, startPositions.size());
		assertArrayEquals(new int[]{0,10,0,10}, startPositions.get(0));
		assertArrayEquals(new int[]{10,20,20,20}, startPositions.get(1));
		
		swDiffs = new String[] {
				"AAAAAAAAAAGGGGGGGGGGGGGGGGGGGG-----TTTTT",
				"||||||||||          ||||||||||     |||||",
		        "AAAAAAAAAA----------GGGGGGGGGGTTTTTTTTTT"};
		startPositions = NumberUtils.getAllStartPositions(swDiffs);
		assertEquals(3, startPositions.size());
		assertArrayEquals(new int[]{0,10,0,10}, startPositions.get(0));
		assertArrayEquals(new int[]{10,10,20,10}, startPositions.get(1));
		assertArrayEquals(new int[]{25,5,30,5}, startPositions.get(2));
	}
	
	@Test
	public void getActualStarts() {
		String [] swDiffs = new String[] {
				"AAAAAAAAAA----------GGGGGGGGGGTTTTTTTTTT",
                "||||||||||          ||||||||||||||||||||",
                "AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT"};
		String swString = swDiffs[2].replaceAll("-", "");
		String target = "AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT";
		List<int[]> startPositions = NumberUtils.getAllStartPositions(swDiffs);
		
		assertArrayEquals(new int[]{0, 20}, NumberUtils.getActualStartPositions(startPositions, true, swString, target, 0));
		target = "CCCCCCCAAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTTCCCCC";
		assertArrayEquals(new int[]{7, 27}, NumberUtils.getActualStartPositions(startPositions, true, swString, target, 0));
		
		
		
		
		swString = swDiffs[0].replaceAll("-", "");
		target = swDiffs[0].replaceAll("-", "");
		assertArrayEquals(new int[]{0, 10}, NumberUtils.getActualStartPositions(startPositions, false, swString, target, 0));
		target = "CCCCCAAAAAAAAAAGGGGGGGGGGTTTTTTTTTTCCCCC";
		assertArrayEquals(new int[]{5, 15}, NumberUtils.getActualStartPositions(startPositions, false, swString, target, 0));
		
		swDiffs = new String[] {     "TAGGAGGGTAGGGAGTGGATATTTTCTAACCTGGAAAAAACTCATTTTACCC",
                "||||||||||||||||||||              ||||||||||||||||||",
                "TAGGAGGGTAGGGAGTGGAT--------------AAAAAACTCATTTTACCC"};
		swString = swDiffs[2].replaceAll("-", "");
		target = "CTTGCTAGGAGGGTAGGGAGTGGATTAAAAGATTGGACCAAAAAACTCATTTTACCCGGGG";
		startPositions = NumberUtils.getAllStartPositions(swDiffs);
		assertArrayEquals(new int[]{5, 39}, NumberUtils.getActualStartPositions(startPositions, true, swString, target, 0));
		
		
	}
	
	@Test
	public void testStarts() {
		String [] swDiffs = new String[] {"GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACG",
                "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||",
                "GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG"};
		TIntList startPositions = NumberUtils.getStartPositions(swDiffs);
		assertEquals(1, startPositions.size());
		assertEquals(0, startPositions.get(0));
		
		swDiffs = new String[] {"AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT",
                "||||||||||..........||||||||||||||||||||",
                "AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT"};
		startPositions = NumberUtils.getStartPositions(swDiffs);
		assertEquals(1, startPositions.size());
		assertEquals(0, startPositions.get(0));
		
		swDiffs = new String[] {"AAAAAAAAAA----------GGGGGGGGGGTTTTTTTTTT",
				                "||||||||||          ||||||||||||||||||||",
		                        "AAAAAAAAAAGGGGGGGGGGGGGGGGGGGGTTTTTTTTTT"};
		startPositions = NumberUtils.getStartPositions(swDiffs);
		assertEquals(2, startPositions.size());
		assertEquals(0, startPositions.get(0));
		assertEquals(20, startPositions.get(1));
	}
	
	@Test
	public void getBlockCountAndCount() {
		assertArrayEquals(new int[]{0,0}, NumberUtils.getBlockCountAndCount(null, ' '));
		assertArrayEquals(new int[]{0,0}, NumberUtils.getBlockCountAndCount(null, '-'));
		assertArrayEquals(new int[]{0,0}, NumberUtils.getBlockCountAndCount("ABCD", '-'));
		assertArrayEquals(new int[]{1,3}, NumberUtils.getBlockCountAndCount("ABCD---", '-'));
		assertArrayEquals(new int[]{1,3}, NumberUtils.getBlockCountAndCount("ABCD---EFGH", '-'));
		assertArrayEquals(new int[]{0,0}, NumberUtils.getBlockCountAndCount("ABCD---EFGH", ' '));
		assertArrayEquals(new int[]{2,6}, NumberUtils.getBlockCountAndCount("ABCD---EFGH---", '-'));
		assertArrayEquals(new int[]{2,6}, NumberUtils.getBlockCountAndCount("ABCD---EFGH---XXX", '-'));
	}
	
	@Test
	public void unset() {
		long l = 1l;
		long updatedL =  NumberUtils.unsetBit(l, 1);
		assertEquals(0, updatedL);
		
		
		l = NumberUtils.setBit(l, 62);
		System.out.println("l: " + l);
		
		updatedL =  NumberUtils.stripBitFromLong(l, 62);
		assertEquals(1, updatedL);
	}
	
	@Test
	public void sumPackedInt() {
		assertEquals(0, NumberUtils.sumPackedInt(0));
		assertEquals(1, NumberUtils.sumPackedInt(1));
		assertEquals(2, NumberUtils.sumPackedInt(2));
		assertEquals(1, NumberUtils.sumPackedInt(65536));
		assertEquals(2, NumberUtils.sumPackedInt(65537));
		assertEquals(367, NumberUtils.sumPackedInt(23658502));
	}
	
	@Test
	public void getIndexInArray() {
		assertEquals(0, NumberUtils.getPositionOfLongInArray(new long[] {12345}, 12345));
		assertEquals(-1, NumberUtils.getPositionOfLongInArray(new long[] {12345}, 12344));
		assertEquals(-1, Arrays.binarySearch(new long[] {12345}, 12344));
		assertEquals(-2, Arrays.binarySearch(new long[] {12345}, 12346));
		assertEquals(-2, NumberUtils.getPositionOfLongInArray(new long[] {12345}, 12346));
	}

}
