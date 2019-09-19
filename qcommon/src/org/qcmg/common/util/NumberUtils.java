package org.qcmg.common.util;

import java.util.Arrays;

public class NumberUtils {
	
	public static final int SHORT_DIVIDER = 16;
	
	
	public static int getTileCount(int actualCount, int commonTileCount) {
		return pack2IntsInto1(actualCount, commonTileCount);
	}
	
	/**
	 * 
	 * Packs 2 ints into 1.
	 * Assumes that both ints will fit inside a short without any overflow (note that this is not checked)
	 * First int occupies the MSB 16 bits, and the second int occupies the LSB 16 bits.
	 * 
	 * @param int1
	 * @param int2
	 * @return
	 */
	public static int pack2IntsInto1(int int1, int int2) {
		int result = (short) int1 << SHORT_DIVIDER;
		result += (short) int2;
		return result;
	}
	
	
	/**
	 * splits the supplied int into 2 parts, returning an int array containing both parts
	 * @param toSplit
	 * @return
	 */
	public static int[] splitIntInto2(int toSplit) {
		return new int[] {toSplit >> SHORT_DIVIDER, (short)toSplit};
	}
	
	public static int sumPackedInt(int toSum) {
		return sumPackedInt(toSum, SHORT_DIVIDER);
	}
	public static int sumPackedInt(int toSum, int divider) {
		return (toSum >> divider) + (short) toSum;
	}
	
	public static int getPartOfPackedInt(int packedInt, boolean firstPart) {
		return getPartOfPackedInt(packedInt, firstPart, SHORT_DIVIDER);
	}
	public static int getPartOfPackedInt(int packedInt, boolean firstPart, int divider) {
		if (firstPart) {
			return packedInt >> divider;
		} else {
			return (short) packedInt;
		}
	}
	
	public static long setBit(long value, int numberOfBits) {
		return (1l << numberOfBits) + value;
	}

	public static long stripBitFromLong(long value, int bit) {
			return value - (1l << bit);
		}

	public static boolean isBitSet(long value, int bit) {
		return (value & 1l << bit) != 0;
	}
	
	public static byte setBit(byte value, int numberOfBits) {
		return (byte) ((1 << numberOfBits) + value);
	}
	public static boolean isBitSetOnByte(byte value, int bit) {
		return (value & 1l << bit) != 0;
	}

}
