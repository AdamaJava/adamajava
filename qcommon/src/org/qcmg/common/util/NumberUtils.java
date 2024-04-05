/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/


package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.string.StringUtils;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.THashMap;

public class NumberUtils {
	
	public static final int SHORT_DIVIDER = 16;
	public static final int OFFSET = 40;
	public static final int REV_COMP_BIT = 62;
	
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
		return (1L << numberOfBits) + value;
	}

	public static long stripBitFromLong(long value, int bit) {
		return value - (1L << bit);
	}

	public static boolean isBitSet(long value, int bit) {
		return (value & 1L << bit) != 0;
	}
	
	public static byte setBit(byte value, int numberOfBits) {
		return (byte) ((1 << numberOfBits) + value);
	}

	public static short getShortFromLong(long l, int offset) {
		return (short) (l >> offset);
	}
	
	public static long addShortToLong(long l, short s, int offset) {
		return l + ((long)s << offset);
	}

	public static long removeShortFromLong(long l, int offset) {
		/*
		 * first of all, get the short, then subtract from the long
		 */
		short s = getShortFromLong(l, offset);
		return l - ((long)s << offset);
	}
	
	public static long getLongPositionValueFromPackedLong(long l){
		return getLongPositionValueFromPackedLong(l, OFFSET, REV_COMP_BIT);
	}
	
	public static long getLongPositionValueFromPackedLong(long l, int offset, int reverseCompBit){
		/*
		 * Need to loop through our map values, and check each one to see if the position falls within the range.
		 * Should only every have 1 range that encompasses a position....
		 */
		if (NumberUtils.isBitSet(l, reverseCompBit)) {
			/*
			 * strip the strand from the long
			 */
			l = NumberUtils.stripBitFromLong(l, reverseCompBit);
		}
		
		/*
		 * remove the tile position in sequence short
		 */
		return NumberUtils.removeShortFromLong(l, offset);
	}
	
	public static int convertTileToInt(String tile) {
		int l = 0;
		for (int i = 0, len = tile.length() ; i < len ; i++) {
			int val = convertCharToInt(tile.charAt(i), false);
			if (val == -1) {
				return -1;
			}
			l += (val << (i * 2));
		}
		return l;
	}
	
	public static int convertCharToInt(char c, boolean allowNs) {
        return switch (c) {
            case 'A' -> 0;
            case 'C' -> 1;
            case 'G' -> 2;
            case 'T' -> 3;
            case 'N' -> allowNs ? 4 : -1;
            default -> -1;
        };
	}

	/**
	 * Returns the position of the value in the long array.
	 * If it is not present, returns a negative value that corresponds to where in the array it would sit should it exist
	 * assumes that the array is sorted.
	 * 
	 * Try and minimise the number of calls to Arrays.binarySearch by checking size and boundary positions
	 * Should not be more than 5000 entries in the array 
	 * 
	 * 
	 * @param array
	 * @param value
	 * @return
	 */
	public static int getPositionOfLongInArray(long [] array, long value) {
		int length = array.length;
		if (length == 0) {
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
	
	public static int[] getBlockCountAndCount(String s, char c) {
		if ( ! StringUtils.isNullOrEmpty(s)) {
			int charIndex = s.indexOf(c);
			if (charIndex > -1) {
				int blockCount = 0;
				int charCount = 0;
				int previousIndex = -1;
				while (charIndex > -1) {
					if (charIndex > previousIndex + 1) {
						blockCount++;
					}
					
					/*
					 * update indexes
					 */
					previousIndex = charIndex;
					charIndex = s.indexOf(c, charIndex + 1);
					charCount++;
				}
				return new int[]{blockCount, charCount};
			}
		}
		return new int[] {0, 0};
	}
	

	/**
	 * List of int arrays
	 * int arrays contain:
	 * query start position
	 * query length
	 * target (reference) start position
	 * target length
	 * @param swSeq
	 * @return
	 */
	public static List<int[]> getAllStartPositions(String [] swSeq) {
		List<int[]> ranges = new ArrayList<>();
		
		int spaceIndex = swSeq[1].indexOf(' ');
		
		if (spaceIndex == -1) {
			/*
			 * assuming that we just have a single block here, in which case query and template start positions are zero, as is the dashCount
			 * length is the length of the query (minus dashes)
			 */
			ranges.add(new int[]{0, swSeq[2].length(), 0, swSeq[0].length()});
		} else {
			String refSeq = swSeq[0].replace("-", "");
			String querySeq = swSeq[2].replace("-", "");
			
			int queryStart = 0;
			int queryDashCount = 0;
			int targetDashCount = 0;
			int queryLength = 0;
			int targetStart = 0;
			int targetLength = 0;
			boolean previousCharASpace = false;
			for (int i = 0 ; i < swSeq[1].length() ; i++) {
				char c = swSeq[1].charAt(i);
				if (' ' == c) {
					if ( ! previousCharASpace) {
						/*
						 * add an entry to the list, and reset lengths
						 */
						ranges.add(new int[]{queryStart - queryDashCount, queryLength, targetStart - targetDashCount, targetLength});
						queryLength = 0;
						targetLength = 0;
					}
					/*
					 * increment the length of either the target of  the query
					 */
					if (swSeq[0].charAt(i) == '-') {
						targetDashCount++;
					}
					if (swSeq[2].charAt(i) == '-') {
						queryDashCount++;
					}
					previousCharASpace = true;
				} else {
					if (previousCharASpace) {
						/*
						 * start of a new block
						 */
						queryStart = i;
						targetStart = i;
					}
					
					/*
					 * increment the length of BOTH the target and the query
					 */
					queryLength++;
					targetLength++;
					previousCharASpace = false;
				}
			}
			
			/*
			 * add last value
			 */
			ranges.add(new int[]{queryStart - queryDashCount, querySeq.length() - (queryStart - queryDashCount), targetStart - targetDashCount, refSeq.length() - (targetStart - targetDashCount)});
		}
		return ranges;
	}
	
	
	public static int[][] getRemainingRanges(int totalLength, int[][] existingRanges, int minSize) {
		
		/*
		 * sort the array
		 */
		Arrays.sort(existingRanges, Comparator.comparingInt(arr -> arr[0]));
		
		/*
		 * now loop through and add to list if there the gap between ranges or boundaries is large enough
		 */
		List<int[]> ranges = new ArrayList<>(4);
		int lastEndPosition = 0;
		for (int [] existingRange : existingRanges) {
				
			if (existingRange[0] - lastEndPosition > minSize) {
				ranges.add(new int[]{lastEndPosition, existingRange[0] - 1});
			}
			lastEndPosition = existingRange[1];
		}
			
		if (totalLength - lastEndPosition > minSize) {
			ranges.add(new int[]{lastEndPosition, totalLength});
		}
		return ranges.toArray(new int[][]{});
	}
	
	/**
	 * Looks for value in the supplied array.
	 * If it is present, then walk from that point in the array onwards and check for an incremental (+1) value. Tally the number of consecutive incremental values and return the tally.
	 * If the supplied value is not present in the supplied array, return 0;
	 */
	public static int getContinuousCountFromValue(int value, int [] array) {
		return getContinuousCountFromValue(value, array, true);
	}
	public static int getContinuousCountFromValue(int value, int [] array, boolean leftToRight) {
		/*
		 * need to get position in array of value - use binary search as array is sorted
		 */
		int startPosition = Arrays.binarySearch(array, value);
		if (startPosition < 0) {
			return 0;
		}
		/*
		 * have a match, so start with 1
		 */
		int continuousCount = 1;
		if (leftToRight) {
			for (int i = startPosition + 1, j = 1 ; i < array.length ; i++, j++) {
				if (array[i] == value + j) {
					continuousCount++;
				} else {
					break;
				}
			}
		} else {
			/*
			 * right to left
			 */
			for (int i = startPosition - 1, j = 1 ; i >= 0 ; i--, j++) {
				if (array[i] == value - j) {
					continuousCount++;
				} else {
					break;
				}
			}
		}
		return continuousCount;
	}
	
	public static int minusPackedInt(int toSum) {
		return minusPackedInt(toSum, SHORT_DIVIDER);
	}
	
	public static int minusPackedInt(int toSum, int divider) {
		return (toSum >> divider) - (short) toSum;
	}
	
	public static long getOverlap(long start1, long end1, long start2, long end2) {
		
		if (start1 >= start2 && start1 <= end2) {
			return Math.min(end1, end2) - start1;
		}
		if (start2 >= start1 && start2 <= end1) {
			return Math.min(end1, end2) - start2;
		}
		
		return 0;
	}
	
	public static Map<Integer, TLongList> getUpdatedMapWithLongsFallingInRanges(Map<Integer, TLongList> originalMap, List<long[]> ranges, int reverseComplementBit) {
		
		Map<Integer, TLongList> newMap = new THashMap<>();
		
		for (Entry<Integer, TLongList> entry : originalMap.entrySet()) {
			TLongList newList = new TLongArrayList();
			for (long l : entry.getValue().toArray()) {
				
				/*
				 * long value will have the tile position in it - need to remove this for the purposes of examining it against the range
				 */
				
				long lToUse = isBitSet(l, reverseComplementBit) ? stripBitFromLong(l, reverseComplementBit) : l;
				lToUse = NumberUtils.removeShortFromLong(lToUse, 40);
				
				for (long[] range: ranges) {
					if (lToUse >= range[0] && lToUse <= range[1]) {
						newList.add(l);
						break;
					}
				}
			}
			
			if ( ! newList.isEmpty()) {
				newMap.put(entry.getKey(), newList);
			}
		}
		
		return newMap;
	}

}
