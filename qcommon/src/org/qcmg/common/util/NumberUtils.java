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
	
	
	/**
	 * Returns the position of the value in the long array.
	 * If it is not present, returns a negative value that corresponds to where in the array it would sit should it exist
	 * assumes that the array is sorted.
	 * 
	 * Try and minimise the number of calls to Arrays.binarySearch by checking size and boundary positions
	 * Should not be more than 500 entries in the array 
	 * 
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
	public static int getPositionOfLongInArray(long [] array, long value) {
		int length = array.length;
		if (length <= 0) {
			return -1;
		}
		if (array[0] == value) {
			return 0;
		} else if (array[0] > value) {
			return -1;
		} else if (array[length - 1] == value) {
			return length - 1;
		} else if (array[length - 1] < value) {
			return - length - 1;
		} else {
			/*
			 * ok, time for binary search
			 */
			return Arrays.binarySearch(array, 1, length - 1, value);
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
