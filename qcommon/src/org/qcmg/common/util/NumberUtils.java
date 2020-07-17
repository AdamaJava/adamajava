package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Range;

import com.amazonaws.util.StringUtils;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
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
		long l = 1l << numberOfBits;
		return l += value;
	}
	public static long unsetBit(long value, int numberOfBits) {
		return value &= ~(1l << numberOfBits - 1);
//		return value &= ~numberOfBits;
//		long l = 0l << numberOfBits;
//		return l += value;
	}

	public static long stripBitFromLong(long value, int bit) {
			return value - (1l << bit);
	//		return value &= ~(1 << bit);
		}

	public static boolean isBitSet(long value, int bit) {
		return (value & 1l << bit) != 0;
	}

	public static long convertTileToLong(String tile) {
		long l = 0;
		for (int i = 0, len = tile.length() ; i < len ; i++) {
			long val = convertCharToInt(tile.charAt(i));
			l += (val << (i * 3));
		}
		return l;
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

	public static String convertLongToTile(final long l, int tileLength) {
		char [] tileArray = new char[tileLength];
		for (int i = 0 ; i < tileLength ; i++) {
			int val = 7 & ((int)(l >> (i * 3)));
			tileArray[i] = convertIntToChar(val);
		}
		return new String(tileArray);
	}
	
	public static short getShortFromLong(long l, int offset) {
		return (short) (l >> offset);
	}
	public static long addShortToLong(long l, short s, int offset) {
		return l + ((long)s << offset);
	}
	public static long removeShortFromLong(long l) {
		return removeShortFromLong(l, OFFSET);
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

	public static int convertCharToInt(char c, boolean allowNs) {
		switch (c) {
		case 'A': return 0;
		case 'C': return 1;
		case 'G': return 2;
		case 'T': return 3;
		case 'N': return allowNs ? 4 : -1;
		default: return -1;
		}
	}

	public static int convertCharToInt(char c) {
		return convertCharToInt(c, true);
	}

	public static char convertIntToChar(int i) {
		switch (i) {
		case 0: return 'A';
		case 1: return 'C';
		case 2: return 'G';
		case 3: return 'T';
		case 4: return 'N';
		default: return '\u0000';
		}
	}
	
//	public static long getRefPositionFromLong(long l) {
//		return (long)((int)l);
//	}

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

	public static List<Range<Integer>> getBlockStartPositions(String swSeq, String seqeuence) {
		List<Range<Integer>> ranges = new ArrayList<>();
		int dashIndex = swSeq.indexOf('-');
		if (dashIndex == -1) {
			int start = seqeuence.indexOf(swSeq);
			ranges.add(Range.between(start, start + swSeq.length()));
		} else {
			int previousIndex = -1;
			while (dashIndex > -1) {
				if (dashIndex > previousIndex + 1) {
					
					String s = swSeq.substring(previousIndex + 1, dashIndex);
					
					int start = seqeuence.indexOf(s);
					ranges.add(Range.between(start, start + s.length()));
				}
				
				/*
				 * update indexes
				 */
				previousIndex = dashIndex;
				dashIndex = swSeq.indexOf('-', dashIndex + 1);
			}
			/*
			 * add last value
			 */
			String s = swSeq.substring(previousIndex + 1);
			int start = seqeuence.indexOf(s);
			ranges.add(Range.between(start, start + s.length()));
			
		}
		return ranges;
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
		return new int[]{0,0};
	}
	
	public static int[] getGapCounts(List<int[]> startPositionsAndLengths) {
		if (null == startPositionsAndLengths || startPositionsAndLengths.size() == 1) {
			return new int[]{0,0,0,0};
		}
		
		int firstGapCount = 0;
		int firstGapSizes = 0;
		int firstLastGapEnd = 0;
		int secondGapCount = 0;
		int secondGapSizes = 0;
		int secondLastGapEnd = 0;
		for (int[] startPositionAndLength : startPositionsAndLengths) {
			int firstStart = startPositionAndLength[0];
			int secondStart = startPositionAndLength[2];
			if (firstStart - firstLastGapEnd >= 1) {
				firstGapCount++;
				firstGapSizes += firstStart - firstLastGapEnd;
			}
			if (secondStart - secondLastGapEnd >= 1) {
				secondGapCount++;
				secondGapSizes += secondStart - secondLastGapEnd;
			}
			
			firstLastGapEnd = firstStart + startPositionAndLength[1];
			secondLastGapEnd = secondStart + startPositionAndLength[3];
		}
		
		return new int[]{firstGapCount, firstGapSizes, secondGapCount, secondGapSizes};
	}
	
	/**
	 * @deprecated DONT USE THIS - MASSIVE BUG
	 * @param positions
	 * @param query
	 * @param swString
	 * @param target
	 * @param offset
	 * @return
	 */
	public static int[] getActualStartPositions(List<int[]> positions, boolean query, String swString, String target, int offset) {
		int [] starts = new int[positions.size()];
		int i = 0;
		for (int[] position : positions) {
			int start = query ? position[0] : position[2];
			int length = query ? position[1] : position[3];
			
			String swSubString = swString.substring(start, start + length);
			starts[i++] = target.indexOf(swSubString) + offset;
		}
		return starts;
	}
	
	public static String getArrayAsCommaSeperatedString(int [] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < array.length ; i++) {
			sb.append(array[i]);
			if (i < array.length - 1) {
				sb.append(Constants.COMMA_STRING);
			}
		}
		return sb.toString();
	}

	/**
	 * Given a sequence string and corresponding smith waterman sequence string, this method will return a list of int arrays that correspond to the following for each block:
	 * 
	 * start position of block in sequence string (0 based)
	 * length of block
	 * cumulative count of dashes within swRef, which gives you the template length
	 * 
	 * @param swSeq
	 * @param seqeuence
	 * @return
	 */
	public static List<int[]> getBlockStartPositionsNew(String swSeq, String seqeuence) {
		List<int[]> ranges = new ArrayList<>();
		int dashIndex = swSeq.indexOf('-');
		int dashCount = 0;
		if (dashIndex == -1) {
			int start = seqeuence.indexOf(swSeq);
			ranges.add(new int[]{start, start + swSeq.length(), 0});
		} else {
			int previousIndex = -1;
			while (dashIndex > -1) {
				if (dashIndex > previousIndex + 1) {
					
					String s = swSeq.substring(previousIndex + 1, dashIndex);
					int start = seqeuence.indexOf(s);
					ranges.add(new int[]{start, start + s.length(), dashCount});
				}
				
				/*
				 * update indexes
				 */
				previousIndex = dashIndex;
				dashIndex = swSeq.indexOf('-', dashIndex + 1);
				dashCount++;
			}
			/*
			 * add last value
			 */
			String s = swSeq.substring(previousIndex + 1);
			int start = seqeuence.indexOf(s);
			ranges.add(new int[]{start, start + s.length(), dashCount});
		}
		return ranges;
	}
	
	public static TIntList getStartPositions(String [] swDiffs) {
		TIntList startPositions = new TIntArrayList();
		int spaceIndex = swDiffs[1].indexOf(' ');
		if (spaceIndex == -1) {
			startPositions.add(0);
		} else {
			int previousIndex = -1;
			while (spaceIndex > -1) {
				if (spaceIndex > previousIndex + 1) {
					startPositions.add(previousIndex + 1);
				}
				
				/*
				 * update indexes
				 */
				previousIndex = spaceIndex;
				spaceIndex = swDiffs[1].indexOf(' ', spaceIndex + 1);
			}
			/*
			 * add last value
			 */
			startPositions.add(previousIndex + 1);
		}
		return startPositions;
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

}
