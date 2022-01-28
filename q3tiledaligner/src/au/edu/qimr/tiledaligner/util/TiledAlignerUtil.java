/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.sw.SmithWatermanGotoh;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.NumberUtils;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPairs;
import au.edu.qimr.tiledaligner.model.TARecord;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.SequenceUtil;

public class TiledAlignerUtil {
	
	public static final int TILE_LENGTH = 13;
	public static final int TILE_LENGTH_MINUS_ONE = TILE_LENGTH - 1;
	public static final int REVERSE_COMPLEMENT_BIT = 62;
	public static final int COMMON_OCCURING_TILE_BIT = 63;
	public static final int POSITION_OF_TILE_IN_SEQUENCE_OFFSET = 40;
	
	public static final String REPEAT_A = "AAAAAAAAAAAAAAAAAAAAAAA";
	public static final String REPEAT_C = "CCCCCCCCCCCCCCCCCCCCCCC";
	public static final String REPEAT_G = "GGGGGGGGGGGGGGGGGGGGGGG";
	public static final String REPEAT_T = "TTTTTTTTTTTTTTTTTTTTTTT";
	static final String[] REPEATS = new String[]{REPEAT_A, REPEAT_C, REPEAT_G, REPEAT_T};
	
	public static final int MINIMUM_BLAT_RECORD_SCORE = 20;
	
	public static final AtomicInteger swCounter = new AtomicInteger();
	
	public static Map<String, byte[]> referenceCache = new THashMap<>();
	public static SAMSequenceDictionary dictionary = null;
	
	public static final IntSummaryStatistics iss = new IntSummaryStatistics();
	
	private static final QLogger logger = QLoggerFactory.getLogger(TiledAlignerUtil.class);
	
	public static List<String> getAlternativeSequences(String seq) {
		List<String> list = new ArrayList<>();
		
		/*
		 * just deal with 1 N for now
		 */
		int nIndex = seq.indexOf('N');
		if (nIndex > -1) {
			String beforeString = seq.substring(0, nIndex);
			String afterString = seq.substring(nIndex + 1);
			
			list.add(beforeString + 'A' + afterString);
			list.add(beforeString + 'C' + afterString);
			list.add(beforeString + 'G' + afterString);
			list.add(beforeString + 'T' + afterString);
		}
		return list;
	}
	
	public static Map<Integer, TLongList> getTiles(Map<String, TLongList> map, String contig, int tileSize, boolean rc) {
		int length = contig.length();
		int arraySize = length - tileSize + 1;
		long [][] array = new long[arraySize][];
		for (int i = 0 ; i < arraySize ; i++) {
			String tile = contig.substring(i, i + tileSize);
			/*
			 * if the tile contains a N, add all variants of this tile to the array
			 */
			TLongList list = map.get(tile);
			if (tile.indexOf('N') > -1) {
				List<String> alternativeTiles = getAlternativeSequences(tile);
				for (String s : alternativeTiles) {
					TLongList l2 = map.get(s);
					if (null != l2 && ! l2.isEmpty()) {
						list.addAll(l2);
					}
				}
			}
			if (null != list) {
				
				/*
				 * check list, if we have a commonly occurring number (-ve) then remove all other numbers
				 */
				if (list.size() > 1) {
					list.sort();
					long l = list.get(0);
					if (l < 0) {
						list.clear();
						list.add(l);
					}
				}
				
				array[i] = list.toArray();
				
				logger.debug("start positions for tile: " + tile + ": " + Arrays.toString(list.toArray()));
			} else {
				array[i] = null;
			}
		}
		return convertLongDoubleArrayToMap(array, rc);
	}
	
	/**
	 * Returns the start positions for the tiles that make up this sequence
	 * 
	 * @param cache
	 * @param contig
	 * @param tileSize
	 * @param rc
	 * @param debug
	 * @return
	 */
	public static long [][] getStartPositionsArray(TIntObjectMap<int[]> cache, String contig, int tileSize, boolean rc, boolean debug) {
		int length = contig.length();
		int arraySize = (length - tileSize) + 1;
		if (arraySize <= 0) {
			logger.info("array size is less than zero! contig: " + contig);
			return new long[0][];
		}
		long [][] array = new long[arraySize][];
		for (int i = 0 ; i < arraySize ; i++) {
			String tile = contig.substring(i, i + tileSize);
			int tileInt = NumberUtils.convertTileToInt(tile);
			/*
			 * if the tile contains a N, add all variants of this tile to the array
			 */
			int[] startPositionsForTile = cache.get(tileInt);
			TLongList list = new TLongArrayList(null != startPositionsForTile ? startPositionsForTile.length + 1 : 2);
			if (null != startPositionsForTile) {
				if (startPositionsForTile.length > 0) {
					list.addAll(Arrays.stream(startPositionsForTile).mapToLong(m -> Integer.toUnsignedLong(m)).toArray());
				} else {
					/*
					 * empty array indicates a commonly occurring tile. Add -1 to the list, as that is what used to happen for common tiles
					 */
					list.add(-1);
				}
			}
			
			if (list.isEmpty()) {
				array[i] = null;
				if (debug) {
					logger.debug("NO start positions for tile: " + tile);
				}
			} else {
				array[i] = list.toArray();
				if (debug) {
					logger.debug("start positions for tile: " + tile + ": " + Arrays.toString(list.toArray()));
				}
			}
		}
		return array;
	}
	
	/**
	 * The 2D long array contains the start positions of tiles that match the string that was queried
	 * This method takes that array and return a map that has a count of the continuous blocks of tiles, along with a list of the start positions.
	 * 
	 * @param array
	 * @return map containing count of continuous matching tiles, along with list of start positions
	 */
	public static Map<Integer, TLongList> convertLongDoubleArrayToMap(long [][] array, boolean rc) {
		Map<Integer, TLongList> map = new HashMap<>();
		
		/*
		 * get number of commonly occurring tiles at beginning of 2D array 
		 */
		TLongIntMap positionCountMap = new TLongIntHashMap();
		int currentMaxCount = 0;
		
		for (int i = 0 ; i < array.length - 1 ; i++) {
			
			long [] subArray = array[i];
			if (null != subArray) {
				for (long l : subArray) {
					
					/*
					 * if long is negative, then it is a commonly occurring tile - ignore these for now
					 */
					if (l < 0) {
						continue;
					}
					
					/*
					 * check the next array to see if we can find a +1 match
					 */
					int tally = nonContinuousCount(array, l, i + 1);
					if (tally > 65536) {
						int exactMatchComponent = NumberUtils.getPartOfPackedInt(tally, true);
						if (exactMatchComponent > currentMaxCount) {
							currentMaxCount = exactMatchComponent;
						}
						if (exactMatchComponent > 5) {
							
							/*
							 * check to see if we have already recorded this in the positionCountMap
							 */
							
							boolean alreadyRepresented = false;
							for (int z = 0 ; z <= i ; z++) {
								if ( positionCountMap.get(l - z) >= exactMatchComponent + z) {
									alreadyRepresented = true;
									break;
								}
							}
							if ( ! alreadyRepresented) {
								/*
								 * add
								 * we want to add the tile position into the long as this will aid split contig sequences
								 */
								long longToUse = NumberUtils.addShortToLong(l, (short) i, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
								if (rc) {
									longToUse = NumberUtils.setBit(longToUse, REVERSE_COMPLEMENT_BIT);
								}
								map.computeIfAbsent(tally, f -> new TLongArrayList()).add(longToUse);
								positionCountMap.put(l, exactMatchComponent);
							}
						}
					}
				}
			}
		}
		
		return map;
	}
	
	
	/**
	 * Returns a map of position (key) length (value) entries
	 * If the buffer is aero, then the length will be up to the length of the array
	 * If the buffer is non-zero (+ve), then it will be the distance from the entry in the first array to the entry in the last array
	 * 
	 * eg.
	 * long [] firstArray = long[]{1000}
	 * long [] lastArray = long[] {3000}
	 * int sequenceLength = 100;
	 * if the buffer is zero, then the entry in the map will be <1000,100>
	 * If the buffer is larger (say 10000), then the map entry would be <1000,2000>
	 * 
	 * @param array
	 * @return
	 */
	public static TLongIntMap getShortCutPositionsForSmithwaterman(long [][] array) {
		return getShortCutPositionsForSmithwaterman(array, 0);
	}
	public static TLongIntMap getShortCutPositionsForSmithwaterman(long [][] array, int buffer) {
		
		TLongIntMap results = new TLongIntHashMap();
		if (null != array && array.length > 0) {
			logger.debug("array.length: " + array.length);
			
			int lengthMinusOne = array.length - 1;
			long [] firstArray = array[0];
			long [] lastArray = array[lengthMinusOne];
			if (null != firstArray && null != lastArray) {

				for (long l : firstArray) {
					int position = NumberUtils.getPositionOfLongInArray(lastArray, l + lengthMinusOne);
					if (position >= 0) {
						
						/*
						 * we have a match!
						 */
						results.putIfAbsent(l, array.length);
					} else if (buffer > 0) {
						/*
						 * look to see if we have a match that is within the buffer range
						 */
						int absolutePosition = - position;
						if (absolutePosition <= lastArray.length) {
							
							long nearbyValueAbove = lastArray[absolutePosition - 1];
							if ((nearbyValueAbove - l) <= buffer) {
								results.putIfAbsent(l, (int)(nearbyValueAbove - l));
							} else {
								if (absolutePosition - 2 >= 0) {
									long nearbyValueBelow = lastArray[absolutePosition - 2];
									long diff = Math.abs(l - nearbyValueBelow);
									if (diff <= buffer) {
										results.putIfAbsent(l, (int)diff);
									}
								}
							}
						}
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * The 2D long array contains the start positions of tiles that match the string that was queried
	 * This method takes that array and return a map that has a count of the continuous blocks of tiles, along with a list of the start positions.
	 * 
	 * @param array
	 * @return map containing count of continuous matching tiles, along with list of start positions
	 */
	
	
	public static int nonContinuousCount(long [][] array, long l, int arrayStartPosition) {
		int tally = 1;	// start at 1
		int commonTileTally = 0;
		int mismatchTally = 0;
		int arrayLength = array.length;
		
		for (int i = 0 ; i < arrayLength - arrayStartPosition ; i++) {
			long [] nextArray = array[arrayStartPosition + i];
			if (null != nextArray) {
				if (nextArray.length == 1 && nextArray[0] < 0) {
					commonTileTally++;
					
				} else {
					
					/*
					 * try and reduce the occurrences of binary searching
					 */
					int position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 1);
					
					if (position > -1) {
						tally++;
						/*
						 * if we have commonly occurring tiles, add to tally, and reset common tile count
						 */
						if (commonTileTally > 0) {
							tally += commonTileTally;
							commonTileTally = 0;
						}
					} else {
						
						
						/*
						 * check to see if this position was a mismatch
						 * by jumping ahead 13
						 */
						if (arrayStartPosition + i + 13 < arrayLength) {
							nextArray = array[arrayStartPosition + i + 13];
							if (null != nextArray && nextArray.length > 0) {
								if (nextArray[0] != -1) {
							
									position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 14);
									if (position > -1) {
										i += 13;
										tally += 14;
										mismatchTally++;
										continue;
									}
								} else if (nextArray.length == 1 && nextArray[0] == -1){
									/*
									 * allow a common occurring tile after the snp
									 */
									if (arrayStartPosition + i + 14 < arrayLength) {
										nextArray = array[arrayStartPosition + i + 14];
										if (null != nextArray && nextArray.length > 0 && nextArray[0] != -1) {
											position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 15);
											if (position > -1) {
												commonTileTally++;
												i += 14;	// plus one
												tally += 14;	// kept the same, as we will add the common tile count at the end
												mismatchTally++;
												continue;
											}
										}
									}
								}
							}
						}
						
						
						
						/*
						 * no longer allow gaps - we want exact matches only, and then use splits mechanism to stitch together constituent chunks if necessary 
						 * 
						 * and so, if we don't have a match, or a commonly occurring tile, break out
						 */
						break;
					}
				}
			}
		}
		
		/*
		 * If the commonTallyCount is less than the tally, incorporate it into the tally
		 */
		if (commonTileTally > 0 && tally > commonTileTally) {
			tally += commonTileTally;
		}
		
		return NumberUtils.getTileCount(tally, mismatchTally);
	}
	
	/**
	 * returns an int array containing the Smith Waterman scores
	 * score[0] = match count
	 * score[1] = mismatch count
	 * score[2] = bases involved in gaps count (note this is not the actual gap count, but rather the number of bases that all the gaps cover)
	 * @param swDiff
	 * @return
	 */
	public static int[] getCountsFromSWString(String swDiff) {
		int [] counts = new int[3];
		for (char c1 : swDiff.toCharArray()) {
			if (c1 == '|') {
				counts[0]++;
			} else if (c1 == '.') {
				counts[1]++;
			} else if (c1 == ' ') {
				counts[2]++;
			}
		}
		return counts;
	}
	
	/**
	* There are 2 different SW modes, strict and lenient.
	* Reason being is that there are instances where we want to try and perform a strict match (eg. splitcon) and other instances where we want to match as much as possible.
	* This method will perform the mode as dictated by the supplied parameter preferStrict.
	* If, however, it turns out that the result from the desired mode does not pass the filters (based on supplied maxMisMatchCount and maxBlockCount), then the alternative mode will be run.
	* If that mode does also not pass the filters, then an empty String array is returned.
	* 
	* The default values for the filters are used in this instance, which are: 10% of the sequence length for the mismatch cutoff, and 6 for the number of blocks
	* 
	* @param ref
	* @param sequence
	* @return
	*/
	public static String[] getIntelligentSwDiffs(String ref, String sequence) {
		return getIntelligentSwDiffs(ref, sequence, (0.1f * sequence.length()), 6, false);
	}
	
	/**
	 * 
	 * There are 2 different SW modes, strict and lenient.
	 * Reason being is that there are instances where we want to try and perform a strict match (eg. splitcon) and other instances where we want to match as much as possible.
	 * This method will perform the mode as dictated by the supplied parameter preferStrict.
	 * If, however, it turns out that the result from the desired mode does not pass the filters (based on supplied maxMisMatchCount and maxBlockCount), then the alternative mode will be run.
	 * If that mode does also not pass the filters, then an empty String array is returned.
	 * 
	 * 
	 * @param ref
	 * @param sequence
	 * @param maxMisMatchCount
	 * @param maxBlockCount
	 * @param preferStrict
	 * @return
	 */
	public static String[] getIntelligentSwDiffs(String ref, String sequence, float maxMisMatchCount, int maxBlockCount, boolean preferStrict) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(ref)
				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(sequence)) {
			throw new IllegalArgumentException("ref or sequence (or both) supplied to ClinVarUtil.getSwDiffs were null. ref: " + ref + ", sequence: " + sequence);
		}
		int nCount = StringUtils.getCount(sequence, 'N');
		maxMisMatchCount += nCount;
		boolean preferLenient = nCount > 2;
		
		/*
		 * if there is no preference, or they are both preferred (!?!#) return based on score
		 */
		if (( ! preferLenient && ! preferStrict) || (preferLenient && preferStrict)) {
			SmithWatermanGotoh nmLenient = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
			String [] diffsLenient = nmLenient.traceback();
			int [] scoresLenient = getCountsFromSWString(diffsLenient[1]);
			
			swCounter.addAndGet(2);
			boolean lenientPassesTest = scoresLenient[1] < maxMisMatchCount && getInsertionCount(diffsLenient[1]) < maxBlockCount;
			
			SmithWatermanGotoh nmStrict = new SmithWatermanGotoh(ref, sequence, 4, -14, 14, 1);
			String [] diffsStrict = nmStrict.traceback();
			int [] scoresStrict = getCountsFromSWString(diffsStrict[1]);
			boolean strictPassesTest = scoresStrict[1] < maxMisMatchCount && getInsertionCount(diffsStrict[1]) < maxBlockCount;
			
			if (lenientPassesTest && strictPassesTest) {
				/*
				 * both pass the test, return the one with the highest score
				 */
				if ((scoresLenient[0] - scoresLenient[1] - (1.5 * scoresLenient[2])) >= (scoresStrict[0] - scoresStrict[1] - (1.5 * scoresStrict[2]))) {
					return diffsLenient;
				} else {
					return diffsStrict;
				}
			} else if (lenientPassesTest) {
				return diffsLenient;
			} else if (strictPassesTest) {
				return diffsStrict;
			} else {
				logger.debug("sw diffs did not pass the mismatch (" + scoresLenient[1] + " and allowed up to " + maxMisMatchCount + "), or block (" + maxBlockCount + "), cutoff requirements");
				for (String s : diffsLenient) {
					logger.debug("diffsLenient: " + s);
				}
				for (String s : diffsStrict) {
					logger.debug("diffsStrict: " + s);
				}
				return new String[]{};
			}
			
		} else if (preferLenient) {
			SmithWatermanGotoh nmLenient = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
			String [] diffsLenient = nmLenient.traceback();
			int [] scoresLenient = getCountsFromSWString(diffsLenient[1]);
			
			swCounter.addAndGet(1);
			boolean lenientPassesTest = scoresLenient[1] < maxMisMatchCount && getInsertionCount(diffsLenient[1]) < maxBlockCount;
			if (lenientPassesTest) {
				return diffsLenient;
			}
			
			/*
			 * If we are here then the lenient mode did not result in a result that passed the test
			 * now run strict to see if we can get a pass
			 */
			SmithWatermanGotoh nmStrict = new SmithWatermanGotoh(ref, sequence, 4, -14, 14, 1);
			String [] diffsStrict = nmStrict.traceback();
			int [] scoresStrict = getCountsFromSWString(diffsStrict[1]);
			boolean strictPassesTest = scoresStrict[1] < maxMisMatchCount && getInsertionCount(diffsStrict[1]) < maxBlockCount;
			
			swCounter.addAndGet(1);
			if (strictPassesTest) {
				return diffsStrict;
			}
		} else if (preferStrict) {
			SmithWatermanGotoh nmStrict = new SmithWatermanGotoh(ref, sequence, 4, -14, 14, 1);
			String [] diffsStrict = nmStrict.traceback();
			int [] scoresStrict = getCountsFromSWString(diffsStrict[1]);
			boolean strictPassesTest = scoresStrict[1] < maxMisMatchCount && getInsertionCount(diffsStrict[1]) < maxBlockCount;
			
			swCounter.addAndGet(1);
			if (strictPassesTest) {
				return diffsStrict;
			}
			/*
			 * now run lenient
			 */
			SmithWatermanGotoh nmLenient = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
			String [] diffsLenient = nmLenient.traceback();
			int [] scoresLenient = getCountsFromSWString(diffsLenient[1]);
			
			swCounter.addAndGet(1);
			boolean lenientPassesTest = scoresLenient[1] < maxMisMatchCount && getInsertionCount(diffsLenient[1]) < maxBlockCount;
			if (lenientPassesTest) {
				return diffsLenient;
			}
		}
		return new String[]{};
	}

	public static ChrPosition getBufferedChrPosition(long packedLong, int sequenceLength, int matchLength, PositionChrPositionMap pcpm) {
		return getBufferedChrPosition(packedLong, sequenceLength, matchLength, pcpm, 20);
	}
	/**
	 * 
	 * @param packedLong
	 * @param sequenceLength
	 * @param matchLength
	 * @param pcpm
	 * @param buffer
	 * @return
	 */
	public static ChrPosition getBufferedChrPosition(long packedLong, int sequenceLength, int matchLength, PositionChrPositionMap pcpm, int buffer) {
		return getBufferedChrPosition(packedLong, sequenceLength, matchLength, pcpm,  buffer, false);
	}
	public static ChrPosition getBufferedChrPosition(long packedLong, int sequenceLength, int matchLength, PositionChrPositionMap pcpm, int buffer, boolean splits) {
		
		int bufferToUse = splits ? buffer / 2 : buffer;
		short sequenceOffset = NumberUtils.getShortFromLong(packedLong, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
		int lhsBuffer = sequenceOffset == 0 ? 0 : sequenceOffset + bufferToUse;
		int rhsBuffer = sequenceOffset + matchLength == sequenceLength ? 0 : bufferToUse;
		
		ChrPosition bufferedCP = pcpm.getBufferedChrPositionFromLongPosition(packedLong, sequenceLength - sequenceOffset, lhsBuffer, rhsBuffer);
		return bufferedCP;
	}
	public static ChrPosition getBufferedChrPosition(long packedLong, int matchLength, PositionChrPositionMap pcpm, int [] buffers) {
		
		int lhsBuffer = buffers[0];
		int rhsBuffer = buffers[1];
		
		ChrPosition bufferedCP = pcpm.getBufferedChrPositionFromLongPosition(packedLong, matchLength, lhsBuffer, rhsBuffer);
		return bufferedCP;
	}
	
	/**
	 * 
	 * Examine the diff string and determine the start positions and lengths of blocks.
	 * Blocks are chunks of sequence separated by a gap.
	 * 
	 * @param swDiffs
	 * @return
	 */
	public static List<Range<Integer>> getBlockStartPositions(String [] swDiffs) {
		
		List<Range<Integer>> ranges = new ArrayList<>();
		char previousChar = swDiffs[1].charAt(0);
		int startPosition = 1;
		for (int i = 0, len = swDiffs[1].length() ; i < len ; i++) {
			char thisChar = swDiffs[1].charAt(i);
			if (thisChar == ' ') {
				if (previousChar != ' ') {
					ranges.add(Range.between(startPosition - 1, i - 1));
				}
			} else {
				if (previousChar == ' ') {
					startPosition = i + 1;
				}
			}
			previousChar = thisChar;
		}
		/*
		 * add last entry
		 */
		ranges.add(Range.between(startPosition - 1, swDiffs[1].length() - 1));
		return ranges;
	}
	
	public static int getInsertionCount(String s) {
		return getInsertionCount(s, ' ');
	}
	public static int getInsertionCount(String s, char c) {
		int dashIndex = s.indexOf(c);
		if (dashIndex == -1) {
			return 0;
		} else {
			int previousIndex = -1;
			int blockCount = 0;
			while (dashIndex > -1) {
				if (dashIndex > previousIndex + 1) {
					blockCount++;
				}
				
				/*
				 * update indexes
				 */
				previousIndex = dashIndex;
				dashIndex = s.indexOf(c, dashIndex + 1);
			}
			/*
			 * add last value
			 */
			return blockCount;
		}
	}

	/**
	 * need to check that the strings in the positions don't overlap
	 * This could happen when one position ends in a certain string, and the next position starts with the same string.
	 * eg.chr7:100866788-100867067
	 * The first position is ATTTTAAACT TCGCTTCCGA AAAAACTTTC AGGCCCTGTT GGAGGAGCAG
	 * note that is ends in GCAG
	 * The second position is GCAGAACTTGAGTG TGGCCGAGGG CCCTAACTAC CTGACGGCCT GTGCGGGACC CCCATCGCGG CCCCAGCGCC CCTTCTGTGC TGTCTGTGGC TTCCCATCCC CCTACACCTG TGTC
	 * note that is start in GCAG
	 * This overlap needs to be removed in one of the positions
	 */
	public static List<ChrPosition> checkForOverlappingSequence(List<ChrPosition> positions, String sequence) {
		List<ChrPosition> updatedList = new ArrayList<>(positions.size() + 1);
		int previousEnd = 0;
		for (ChrPosition cp : positions) {
			String cpSeq = cp.getName();
			int startIndex = sequence.indexOf(cpSeq);
			if (startIndex == -1) {
				continue;
			}
			int endIndex = cpSeq.length() + startIndex;
			ChrPosition updatedCP = cp;
			if (previousEnd == 0) {
				previousEnd = endIndex;
			} else {
				if (endIndex <= previousEnd) {
					/*
					 * no good - is before previous entry in seq, but after in cp
					 */
					continue;
				} else if (startIndex < previousEnd) {
					/*
					 * need to trim
					 */
					int diff = previousEnd - startIndex;
					logger.info("in checkForOverlappingSequence, diff: " + diff + ", previousEnd: " + previousEnd + ", startIndex: " + startIndex + ", cpSeq: " + cpSeq + ", sequence: " + sequence);
					updatedCP = new ChrPositionName(cp.getChromosome(), cp.getStartPosition() + diff, cp.getEndPosition(), cpSeq.substring(diff));
				}
			}
			updatedList.add(updatedCP);
		}
		return updatedList;
	}
	
	public static String getRefFromChrPos(ChrPosition cp, String refFile) {
		return ReferenceUtil.getRefFromChrStartStop(refFile, cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition());
	}
	
	
	/**
	 * Looking to see if the sequence is full of single base repeats (ie. AAAAA)
	 * If there are single base repeat regions of greater then 23 bases in length, we examine them to see if the length of the single base repeat is greater than 25% of the sequence length.
	 * If it is, false is returned, true otherwise
	 * 
	 * 
	 * @param sequence
	 * @return
	 */
	public static boolean doesSequenceHaveMostlySingleBaseRepeats(String sequence) {
		
		return  ! Arrays.stream(REPEATS).filter(s -> sequence.contains(s)).anyMatch(s -> {
			int index = sequence.indexOf(s);
			int tally = s.length();
			while (index != -1) {
				index = sequence.indexOf(s, index + 1);
				if (index > -1) {
					tally ++;
				}
			}
			return  ((double)tally / sequence.length()) > 0.25;
		});
	}
	
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(String refFile, TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod, boolean log) throws IOException {
		return runTiledAlignerCache(refFile, cache,  sequencesNameMap, tileLength, originatingMethod,  log, false);
	}
	
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(String refFile, TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) throws IOException {
		Map<String, List<BLATRecord>> results = new HashMap<>();
		
		for (Entry<String, String> entry : sequencesNameMap.entrySet()) {
			if (log) {
				logger.info("about to call getBlatRecords for " + entry.getKey());
			}
			String name = entry.getValue();
			if (null != name && name.contains(":")) {
				logger.info("got more than 1 name for sequence. names: " + name + ", will use first one to dictate sw strategy");
				String [] names = entry.getValue().split(":");
				name = names[0];
			}
			logger.info("in runTiledAlignerCache, name: " + name + ", seq: " + entry.getKey());
			
			List<BLATRecord> blatties = getBlatRecordsSWAll(refFile, cache, entry.getKey(), entry.getValue(), tileLength, originatingMethod, log, recordsMustComeFromChrInName);
			blatties.sort(null);
			/*
			 * populate the name field on the BLATRecord with the value, if present - otherwise just leave as the default
			 */
			if (null != entry.getValue()) {
				for (BLATRecord b : blatties) {
					b.setQName(entry.getValue());
				}
			}
			results.put(entry.getKey(), blatties);
		}
		return results;
	}
	
	public static Map<String, List<BLATRecord>> runTiledAlignerCacheSWAll(String refFile, TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) throws IOException {
		Map<String, List<BLATRecord>> results = new HashMap<>();
		
		for (Entry<String, String> entry : sequencesNameMap.entrySet()) {
			if (log) {
				logger.info("about to call getBlatRecords for " + entry.getKey());
			}
			String name = entry.getValue();
			if (null != name && name.contains(":")) {
				logger.info("got more than 1 name for sequence. names: " + name + ", will use first one to dictate sw strategy");
				String [] names = entry.getValue().split(":");
				name = names[0];
			}
			logger.info("in runTiledAlignerCache, name: " + name + ", seq: " + entry.getKey());
			
			List<BLATRecord> blatties = getBlatRecordsSWAll(refFile, cache, entry.getKey(), entry.getValue(), tileLength, originatingMethod, log, recordsMustComeFromChrInName);
			/*
			 * populate the name field on the BLATRecord with the value, if present - otherwise just leave as the default
			 */
			if (null != entry.getValue()) {
				for (BLATRecord b : blatties) {
					b.setQName(entry.getValue());
				}
			}
			results.put(entry.getKey(), blatties);
		}
		return results;
	}
	
	public static int[] getCommonTilePositions(long [][] startPositions) {
		TIntList is = new TIntArrayList();
		if (null != startPositions) {
			for (int i = 0 ; i < startPositions.length ; i++) {
				/*
				 * commonly occurring tiles have a single entry, that is -1
				 */
				long [] sp = startPositions[i];
				if (null != sp && sp.length == 1 && sp[0] == -1) {
					is.add(i);
				}
			}
		}
		return is.toArray();
	}
	
	public static List<BLATRecord> getBlatRecordsSWAll(String refFile, TIntObjectMap<int[]> cache, String sequence, final String name, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) {
		if (null == cache || cache.isEmpty()) {
			throw new IllegalArgumentException("Null or empty cache passed to getBlatRecords");
		}
		if (null == sequence || sequence.isEmpty()) {
			throw new IllegalArgumentException("Null or empty sequence passed to getBlatRecords");
		}
		if (sequence.length() <= tileLength) {
			throw new IllegalArgumentException("sequence length is less than or equals to the tile length! sequence: " + sequence + ", tile length: " + tileLength);
		}
		
		if ( ! doesSequenceHaveMostlySingleBaseRepeats(sequence)) {
			logger.warn("too much repetition in sequence to proceed: " + sequence);
			return Collections.emptyList();
		}
		int seqLength = sequence.length();
		
		List<BLATRecord> results = new ArrayList<>();
		String revCompSequence = SequenceUtil.reverseComplement(sequence);
		
		long [][] startPositions = getStartPositionsArray(cache, sequence, tileLength, false, log);
		long [][] startPositionsRC = getStartPositionsArray(cache, revCompSequence, tileLength, true, log);
		int [] allCommonTiles = getCommonTilePositions(startPositions);
		int [] allCommonTilesRC = getCommonTilePositions(startPositionsRC);
		
		int commonTileCountAtStart = NumberUtils.getContinuousCountFromValue(0, allCommonTiles);
		int commonTileCountAtStartRC = NumberUtils.getContinuousCountFromValue(0, allCommonTilesRC);
		if (commonTileCountAtStart > 0 || commonTileCountAtStartRC > 0) {
			logger.debug("commonTileCountAtStart: " + commonTileCountAtStart + " (total: " + allCommonTiles.length + "), commonTileCountAtStartRC: " + commonTileCountAtStartRC + " (total: " + allCommonTilesRC.length + ")");
		}
		
		
		if (null != allCommonTiles && allCommonTiles.length > 0) {
			logger.debug("common tile positions: " + Arrays.stream(allCommonTiles).mapToObj(i -> "" + i).collect(Collectors.joining(",")));
		}
		if (null != allCommonTilesRC && allCommonTilesRC.length > 0) {
			logger.debug("RS common tile positions: " + Arrays.stream(allCommonTilesRC).mapToObj(i -> "" + i).collect(Collectors.joining(",")));
		}
		
		TLongIntMap potentialMatches = getShortCutPositionsForSmithwaterman(startPositions);
		TLongIntMap potentialMatchesRC = getShortCutPositionsForSmithwaterman(startPositionsRC);
		
		Map<Integer, TLongList> map1 = convertLongDoubleArrayToMap(startPositions, false);
		Map<Integer, TLongList> map2 = convertLongDoubleArrayToMap(startPositionsRC, true);
		for (Entry<Integer, TLongList> entry : map2.entrySet()) {
			map1.computeIfAbsent(entry.getKey(), f -> new TLongArrayList()).addAll(entry.getValue());
		}
		
		
		PositionChrPositionMap headerMap = new PositionChrPositionMap();
		headerMap.loadMap(PositionChrPositionMap.grch37Positions);
		
		
		/*
		 * if we have recordsMustComeFromChrInName then we must get the chromosomes from the name, and then get the start and stop positions for those contigs
		 * we will then go through the map and remove entries that are not within these ranges
		 * as this should reduce the number of positions that are sent to the TARecord
		 */
		List<long[]> acceptableRanges = new ArrayList<>(4);
		if (recordsMustComeFromChrInName) {
			/*
			 * get chromosome names from name
			 */
			String [] nameArray = name.split("_");
			for (String s : nameArray) {
				if (s.startsWith("chr") || s.startsWith("GL")) {
					acceptableRanges.add(headerMap.getLongStartAndStopPositionFromChrPosition(new ChrPointPosition(s, 1)));
				}
			}
			Map<Integer, TLongList> updatedMap =  NumberUtils.getUpdatedMapWithLongsFallingInRanges(map1, acceptableRanges, REVERSE_COMPLEMENT_BIT);
			
			if ( ! updatedMap.isEmpty()) {
				map1 = updatedMap;
			}
			
			logger.info("we have " + acceptableRanges.size() + " ranges for this name: " + name + ", updated map size: " + updatedMap.size() + ", going with map of size: " + map1.size());
		}
		
		if (log) {
			List<Integer> keys = new ArrayList<>(map1.keySet());
			keys.sort(null);
			for (int i = keys.size() - 1 ; i >= 0 ; i--) {
				TLongList value = map1.get(keys.get(i));
				logger.info("match count: "  + Arrays.toString(NumberUtils.splitIntInto2(keys.get(i))) + ", number of starts: " + value.size());
				if (value.size() < 10) {
					value.forEach((p) -> {logger.info("p: " + p); return true;});
				}
			}
		}
		
		TARecord taRec = new TARecord(sequence, map1);
		boolean needToRunSW = true;
		
		TLongList perfectMatches = new TLongArrayList();
		/*
		 * Check to see if we have any potential perfect matches taking into account the common tile count
		 */
		int maxTileCount = sequence.length() - (TILE_LENGTH_MINUS_ONE);
		int halfMaxTileCount = maxTileCount / 2;
		
		/*
		 * if the common tile count is >= half of the max tile count, don't add to perfect match
		 */
		addEntriesToListIfExistInMap(commonTileCountAtStart, map1, perfectMatches, maxTileCount, halfMaxTileCount, false);
		addEntriesToListIfExistInMap(commonTileCountAtStartRC, map1, perfectMatches, maxTileCount, halfMaxTileCount, true);
		
		/*
		 * If we have a perfect match here, don't bother doing any SW
		 */
		if (log) {
			logger.info("Perfect match? " + (null != perfectMatches && ! perfectMatches.isEmpty()));
		}
		if (null != perfectMatches && ! perfectMatches.isEmpty()) {
			if (sequence.indexOf('N') > -1) {
				if (log) {
					logger.info("Can't find perfect match - sequence has an N! seq: " + sequence);
				}
			} else {
				/*
				 * only proceed if all bases are represented in this string
				 */
				int seqComplexity = StringUtils.determineSequenceComplexity(sequence).cardinality();
				if (seqComplexity == 4 || (seqComplexity >= 2 && seqLength >= 50)) {
					int buffer = 10;
					/*
					 * only want to return BLATRecords that have a score of greater than MINIMUM_BLAT_RECORD_SCORE (20), and in the case of the perfect match, than means sequence length needs to be more than 20
					 */
					if (sequence.length() > MINIMUM_BLAT_RECORD_SCORE) {
						for (int i = 0 ; i < perfectMatches.size() ; i++) {
							long l = perfectMatches.get(i);
							
							ChrPosition cp = headerMap.getChrPositionFromLongPosition(l);
							boolean forwardStrand = "F".equals(((ChrPositionName)cp).getName());
							
							/*
							 * need to check that reference for this position matches the sequence we have
							 * This is because we may have some errors due to our commonly occurring tiles.....
							 */
							String ref =  ReferenceUtil.getRefFromChrStartStop(refFile, cp.getChromosome(), cp.getStartPosition() - buffer, cp.getStartPosition() + sequence.length() + buffer);
							Optional<ChrPosition> optionalCP = getChrPositionWithReference(cp.getChromosome(), cp.getStartPosition(), forwardStrand ? sequence : revCompSequence, ref);
							if (optionalCP.isPresent()) {
								logger.debug("got a perfect match!  cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
								needToRunSW = false;
								Optional<BLATRecord> oBR = BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(optionalCP.get()), name, forwardStrand ? sequence : revCompSequence, forwardStrand);
								oBR.ifPresent(br -> results.add(br));
							} else {
								logger.debug("got perfect match on tiles, but not on sequence! cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
							}
						}
					}
				}
			}
		}
		if (needToRunSW) {
			
			/*
			 * get sorted keys so that we can run some SWs
			 */
			List<Integer> sortedKeys = new ArrayList<>(map1.keySet());
			sortedKeys.sort((i1, i2) -> Integer.compare(NumberUtils.minusPackedInt(i2), NumberUtils.minusPackedInt(i1)));
			int totalNumberOfTiledPositions = map1.values().stream().mapToInt(l -> l.size()).sum();
			boolean splits = null != name && name.startsWith("splitcon");
			
			int currentMaxScore = 0;
			int thisIterationMaxScore = 0;
			int passingScore = (int)(seqLength * 0.95);
			int passingScore99 = (int)(seqLength * 0.99);
			boolean finalIteration = false;
			int iterationCount = 0;
			
			logger.debug("seqLength: " + seqLength + ",  splits: " + splits + ", totalNumberOfTiledPositions: " + totalNumberOfTiledPositions + ", shortcut positions: " + potentialMatches.size() + ", shortcut positions RC: " + potentialMatchesRC.size());
			
			/*
			 * look at shortcut positions first
			 */
			if (potentialMatches.size() > 0) {
				for (long l : potentialMatches.keys()) {
					BLATRecord br = smithWaterman(l, seqLength, seqLength, name, sequence, revCompSequence,  refFile, allCommonTiles, headerMap, false);
					if (null != br) {
						logger.debug("found record from shortcut: " + br.toString());
						results.add(br);
					}
				}
			} else {
				/*
				 * quick check to see if we can find a shortcut with buffer
				 */
				TLongIntMap splitReadShortCuts = getShortCutPositionsForSmithwaterman(startPositions, 500000);
				logger.debug("Have " + splitReadShortCuts.size() + " potential split read shortcuts available!");
				
				
				for (long l : splitReadShortCuts.keys()) {
					long positionPartOfLong = NumberUtils.getLongPositionValueFromPackedLong(l);
					/*
					 * need to check that this l is within the acceptable range
					 */
					boolean goodToGo = false;
					for (long[] range : acceptableRanges) {
						if (positionPartOfLong > range[0] && positionPartOfLong < range[1]) {
							goodToGo = true;
							break;
						}
					}
					if (goodToGo) {
						
						BLATRecord br =  investigateSplitShortCut(l, splitReadShortCuts.get(l), name, sequence, revCompSequence, refFile, allCommonTiles, headerMap, false);
						if (null != br) {
							logger.debug("found record from split read shortcut: " + br.toString());
							results.add(br);
						}
					}
				}
			}
			if (potentialMatchesRC.size() > 0) {
				for (long l : potentialMatchesRC.keys()) {
					BLATRecord br = smithWaterman(l, seqLength, seqLength, name, sequence, revCompSequence, refFile, allCommonTilesRC, headerMap, true);
					if (null != br) {
						logger.debug("found record from shortcutRC: " + br.toString());
						results.add(br);
					}
				}
			} else {
				/*
				 * quick check to see if we can find a shortcut with buffer
				 */
				TLongIntMap splitReadShortCuts = getShortCutPositionsForSmithwaterman(startPositionsRC, 500000);
				logger.debug("Have " + splitReadShortCuts.size() + "RC, split read shortcuts available!");

				for (long l : splitReadShortCuts.keys()) {
					
					long positionPartOfLong = NumberUtils.getLongPositionValueFromPackedLong(l);
					
					/*
					 * need to check that this l is within the acceptable range
					 */
					boolean goodToGo = false;
					for (long[] range : acceptableRanges) {
						if (positionPartOfLong > range[0] && positionPartOfLong < range[1]) {
							goodToGo = true;
							break;
						}
					}
					if (goodToGo) {
						BLATRecord br =  investigateSplitShortCut(l, splitReadShortCuts.get(l), name, sequence, revCompSequence, refFile, allCommonTilesRC, headerMap, true);
						if (null != br) {
							logger.debug("found record from split read shortcutRC: " + br.toString());
							results.add(br);
						}
					}
				}
			}
			logger.debug("after shortcut mode, results is following size: " + results.size());
			
			
			boolean runSW = true;
			
			/*
			 * splits next is highest record is not yet over the passing score
			 */
			List<BLATRecord> splitsList = new ArrayList<>();
			if (results.isEmpty() || results.get(results.size() - 1).getScore() < passingScore99) {
				
				logger.debug("about to run some splits, no of positions in TARecord:  name: " + name + ", " + taRec.getCountDist());
				TIntObjectMap<Set<IntLongPairs>> splitsMap = TARecordUtil.getSplitStartPositions(taRec);
				List<IntLongPairs> potentialSplits = new ArrayList<>();
				splitsMap.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
				/*
				 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
				 */
				splitsList.addAll(potentialSplits.stream()
						.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
						.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, sequence.length(), headerMap, TILE_LENGTH))
						.filter(obr -> obr.isPresent())
						.map(obr -> obr.get())
						//				.filter(br -> br.getScore() > passingScore)
						.collect(Collectors.toList()));
				logger.debug("after splits mode, splitsList is following size: " + splitsList.size());
				splitsList.sort(null);
				
				if ( ! splitsList.isEmpty() && splitsList.get(splitsList.size() - 1).getScore() >= passingScore99) {
					logger.debug("top score from splits: " + splitsList.get(splitsList.size() - 1).getScore() + ", no need to run sw!");
					runSW = false;
					results.addAll(splitsList);
				} else {
					if ( ! splitsList.isEmpty()) {
						logger.debug("top score from splits: " + splitsList.get(splitsList.size() - 1).getScore() + ", will run sw!");
					}
				}
			}
			
			if (runSW && sortedKeys.size() > 0) {
				int swCount = 0;
				int swCountCutoff = 80;
				
				boolean limitReached = false;
				
				for (Integer i : sortedKeys) {
					
					logger.debug("Looping through sortedKeys, i: " + i.intValue());
					
					if (limitReached) {
						/*
						 * need to break out of this look once we are in the weeds.
						 * Definition of weeds is currently less than half the max value
						 */
						logger.debug("limitReached is true, swCount: " + swCount + ", results.size(): " + results.size());
						break;
					}
					
					/*
					 * get list of longs at this position
					 * If there are more than 100, break
					 */
					TLongList list = map1.get(i);
					
					/*
					 * loop through and take a squizz
					 */
					thisIterationMaxScore = 0;
					for (long l : list.toArray()) {
						
						
						if (swCount++ > swCountCutoff) {
							/*
							 * need to break out of this look once we are in the weeds.
							 * Definition of weeds is currently less than half the max value
							 */
							limitReached = true;
							break;
						}
						
						
						int length = NumberUtils.getPartOfPackedInt(i, true) + TILE_LENGTH_MINUS_ONE;
						int mismatchCount = NumberUtils.getPartOfPackedInt(i, false);
						boolean reverseComplement = NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
						BLATRecord br = smithWaterman(l, length, seqLength, name, sequence, revCompSequence, refFile, (reverseComplement ? allCommonTilesRC : allCommonTiles), headerMap, reverseComplement);
						if (null == br) {
							/*
							 * try creating a record just using the tile data
							 */
							br = getRecordFromTileCountAndMismatches(l, length, mismatchCount, seqLength, name, sequence, revCompSequence, headerMap);
						}
						if (null != br) {
							logger.debug("created br: " + br.toString());
							if (br.getScore() > thisIterationMaxScore) {
								thisIterationMaxScore = br.getScore();
							}
							results.add(br);
						}
					}
					iterationCount++;
					/*
					 * check to see if we need to do any more here
					 * check results list - if we have a score of > 95% and have done a few iterations to ensure we are not going to miss some  - then break.
					 * 
					 * for splitcons - need another strategy as it is highly unlikely to get a 95% pass
					 */
					if ( ! results.isEmpty()) {
						results.sort(null);
						int maxScore = results.get(results.size() - 1).getScore();
						if (finalIteration) {
							/*
							 * if we got a higher score in this iteration than in the first, go once more
							 */
							if (thisIterationMaxScore < currentMaxScore) {
								iterationCount++;
								if (iterationCount >= 5) {
									break;
								}
							}
						}
						if (maxScore > currentMaxScore) {
							currentMaxScore = maxScore;
						}
						if (currentMaxScore >= passingScore) {
							/*
							 * already have a passing score - perform one more iteration to see
							 */
							finalIteration = true;
						}
						
						/*
						 * for splits, hit an iteration limit of 10, then bail
						 */
						if (splits && iterationCount >= 10) {
							break;
						}
					}
					
				}
				
				/*
				 * if we have splits, see if we can weed out results that would make a good splt
				 */
				if (splits || currentMaxScore < passingScore) {
					/*
					 * get largest
					 * find missing range
					 * loop through remaining
					 * find best match for missing range
					 */
					if ( ! results.isEmpty()) {
						results.sort(null);
						
						List<BLATRecord> splitsResults = new ArrayList<>();
						BLATRecord largestSplitBR = results.get(results.size() - 1);
						int [] [] missingRanges = NumberUtils.getRemainingRanges(seqLength, new int[][] {new int[] {largestSplitBR.getQueryStart(),largestSplitBR.getQueryEnd() }}, 10);
						
						if (null != missingRanges && missingRanges.length > 0) {
							logger.debug("missing ranges: " + Arrays.deepToString(missingRanges));
							for (int [] missingRange : missingRanges) {
								logger.debug("missing ranges: " + Arrays.deepToString(missingRanges));
								
								
								Optional<BLATRecord> optionalBR = BLATRecordUtil.findRecordInRange(results, missingRange[0], missingRange[1]);
								if (optionalBR.isPresent()) {
									/*
									 * if largest and this optional give us good coverage, then return and be done
									 */
									if (optionalBR.get().getScore() + largestSplitBR.getScore() > passingScore) {
										splitsResults.add(optionalBR.get());
										splitsResults.add(largestSplitBR);
									}
								}
							}
							
						}
						if ( ! splitsResults.isEmpty()) {
							splitsResults.sort(null);
							boolean returnSplits = true;
							logger.debug("will look for a possible merged rec with splitResults: " + splitsResults.stream().map(BLATRecord::toString).collect(Collectors.joining(",")));
								Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(splitsResults, org.apache.commons.lang3.StringUtils.countMatches(sequence, 'N'));
								if (mergedRec.isPresent()) {
									logger.debug("merged rec is present!!!: " + mergedRec.get().toString());
									results.add(mergedRec.get());
									returnSplits = false;
								}
							if (returnSplits) {
								Optional<int []> coverageAndOverlapO = BLATRecordUtil.getCombinedNonOverlappingScore(splitsResults);
								if (coverageAndOverlapO.isPresent()) {
									int [] coverageAndOverlap = coverageAndOverlapO.get();
									if (coverageAndOverlap[0] > passingScore && coverageAndOverlap[0] > (coverageAndOverlap[1] * 2)) {
										logger.info("Returning splits results, coverageAndOverlap[0] > passingScore && coverageAndOverlap[0] > (coverageAndOverlap[1] * 2)");
										return splitsResults;
									}
								}
								results.addAll(splitsResults);
							}
						}
					}
				}
				
				if ( ! splitsList.isEmpty() ) {
					int topSPlitsScore = splitsList.get(splitsList.size() - 1).getScore();
					results.sort(null);
					int topOtherRecordsScore =  results.get(results.size() - 1).getScore();
					if (topSPlitsScore > topOtherRecordsScore) {
						logger.debug("adding splitList entries to results");
						results.addAll(splitsList);
					}
				}
			}
		}
		
		/*
		 * only want unique results
		 */
		List<BLATRecord> uniqueResults = results.stream().distinct().collect(Collectors.toList());
		uniqueResults.sort(null);
		
		if (log) {
			logger.debug("number of blat records for seq: " + sequence +", " + uniqueResults.size() +", winner: " + (uniqueResults.size() > 0 ? uniqueResults.get(uniqueResults.size() - 1).toString() : "-"));
		}
		
		if (null != name && name.contains("splitcon")) {
			/*
			 * really only care if we have different chromosomes
			 */
			String[] nameArray = name.split("_");
			if (nameArray.length > 4 &&  ! nameArray[1].equals(nameArray[3])) {
				logger.debug("got a splitcon: " + name + " with no of recs: " + uniqueResults.size() + ", taRec: " + taRec.toString());
				for (BLATRecord br : uniqueResults) {
					logger.debug("splitcon rec: " + br);
				}
			}
		}
		/*
		 * remove BLATRecords that are overlapping
		 * Don't want to do this for splicon records, as overlapping records is a feature...
		 */
		List<BLATRecord> nonOverlappingRecs = BLATRecordUtil.removeOverlappingRecords(uniqueResults);
		nonOverlappingRecs.sort(null);
		logger.info("Removed overlapping records. Previous count: " + uniqueResults.size() + ", new count (nonOverlappingRecs size): " + nonOverlappingRecs.size() + " top record: " + (nonOverlappingRecs.size() > 0 ? nonOverlappingRecs.get(nonOverlappingRecs.size() - 1).toString() : null));
		
		return nonOverlappingRecs;
	}
	
	public static BLATRecord smithWaterman(long l, int length, int seqLength, String name, String sequence, String revCompSequence, String refFile, int[] allCommonTiles, PositionChrPositionMap headerMap, boolean reverseComplement) {
		short startPositionInSequence = NumberUtils.getShortFromLong(l, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
		/*
		 * look to see if we have commonly occurring tiles at beginning or end
		 */
		int startCommonTileCount = NumberUtils.getContinuousCountFromValue(startPositionInSequence - 1, allCommonTiles);
		int endCommonTileCount = NumberUtils.getContinuousCountFromValue(startPositionInSequence + length, allCommonTiles);
		boolean preferStrictSW = name != null && name.startsWith("splitcon");
		
		logger.debug("details so far... seqLength: " + seqLength + ", startPositionInSequence: " + startPositionInSequence + ", length: " + length + ", revComp: " + reverseComplement + ", startCommonTileCount: " + startCommonTileCount + ", endCommonTileCount: " + endCommonTileCount);
		
		
		int[] buffersToUse = BLATRecordUtil.getBuffers(seqLength, startPositionInSequence, startPositionInSequence + length, preferStrictSW, allCommonTiles, 60, 5);
		
		
		ChrPosition bufferedCP = getBufferedChrPosition(l, length, headerMap,buffersToUse);
		String bufferedReference = getRefFromChrPos(bufferedCP, refFile);
		String fragString = reverseComplement ? revCompSequence : sequence;
			
		/*
		 * perform and indexOf to see if we have an exact match!
		 */
		int index = bufferedReference.indexOf(fragString);
		
		BLATRecord br = null;
			
		if (index > -1) {
			logger.debug("Got a perfect match using indexOf!!! " + bufferedCP.getChromosome() + ":" + (bufferedCP.getStartPosition()));
			/*
			 * create matching swdiffs array
			 */
			String[] swMatches = new String[3];
			swMatches[0] = fragString;
			swMatches[1] = org.apache.commons.lang3.StringUtils.repeat('|', seqLength);
			swMatches[2] = fragString;
			br = BLATRecordUtil.getBLATRecord(bufferedCP, swMatches, name, sequence, ! reverseComplement, bufferedReference);
		} else {
				
			logger.debug("about to sw ref: " + bufferedReference + ", fragString: " + fragString + " for cp: " + bufferedCP.getChromosome() + ":" +  (bufferedCP.getStartPosition()));
			/*
			 * this method will only return a populated string array if valid results have been found (ie. passed the filters)
			 * and so can add directly to collection
			 */
			float misMatchCutoff = 0.1f * seqLength;			// looking at a 10% max mismatch rate
			
			String [] swDiffs = getIntelligentSwDiffs(bufferedReference, fragString, misMatchCutoff, 6, preferStrictSW);
			if (swDiffs.length > 0) {
				br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, name, sequence, ! reverseComplement, bufferedReference);
			}
		}
		return br;
	}
	
	public static BLATRecord investigateSplitShortCut(long l, int length, String name, String sequence, String revCompSequence, String refFile, int[] allCommonTiles, PositionChrPositionMap headerMap, boolean reverseComplement) {
		/*
		 * get ChrPos with no bufferring at start and end
		 */
		ChrPosition bufferedCP =  headerMap.getBufferedChrPositionFromLongPosition(l, length + TILE_LENGTH, 0, 0);
		
		String bufferedReference = getRefFromChrPos(bufferedCP, refFile);
		String fragString = reverseComplement ? revCompSequence : sequence;
		
		/*
		 * lets see what our home grown method gives us
		 */
		logger.debug("about to call StringUtils.getStartPositionsAndLengthOfSubStrings with bufferedReference: " + (bufferedReference.length() > 1000 ? ("[too_long: " + bufferedReference.length() + " bases]") :  bufferedReference) + ", and fragString: " + fragString);
		int[][] startPositionsAndLengths = StringUtils.getStartPositionsAndLengthOfSubStrings(bufferedReference, fragString);
		int splitsLengthTally = 0;
		for (int [] array : startPositionsAndLengths) {
			splitsLengthTally += array[2];
		}
		logger.debug("name: " + name + ", splits length: " + length + ", seqLength: + " + sequence.length() + ", number of blocks: " + startPositionsAndLengths.length + ", tally: " + splitsLengthTally);
		
		BLATRecord br = BLATRecordUtil.getRecordFromStartPositionsAndLengths(bufferedCP, startPositionsAndLengths, name, sequence, ! reverseComplement);
		return br;
	}
	
	public static BLATRecord getRecordFromTileCountAndMismatches(long l, int length, int mismatchCount, int seqLength, String name, String sequence, String revCompSequence, PositionChrPositionMap headerMap) {
		/*
		 * SW wasn't able to return records within the filtering criteria
		 * Lets see if we can manually create one based on the tile count
		 */
		if ((length - mismatchCount) > MINIMUM_BLAT_RECORD_SCORE) {
			
			logger.debug("SW couldn't put this down whilst passing the filters - will attempt to create based on tile counts alone. l: " + l + ", length: " + length + ", mismatchCount: " + mismatchCount);
			
			ChrPosition cp = headerMap.getChrPositionFromLongPosition(l);
			boolean forwardStrand = "F".equals(((ChrPositionName)cp).getName());
			/*
			 * need to check that reference for this position matches the sequence we have
			 * This is because we may have some errors due to our commonly occurring tiles.....
			 */
			int startPositionsInSequence = NumberUtils.getShortFromLong(l, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
			int stopPositionInSequence = startPositionsInSequence + length;
			String subSequence = sequence.substring(startPositionsInSequence, stopPositionInSequence);
			String subSequenceRC = revCompSequence.substring(seqLength - stopPositionInSequence, (seqLength - stopPositionInSequence) + length);
			
			ChrPosition cp2 = new ChrPositionName(cp.getChromosome(), cp.getStartPosition(), cp.getStartPosition() + length, forwardStrand ? subSequence : subSequenceRC);
			return BLATRecordUtil.getDetailsForBLATRecord(cp2, mismatchCount, NumberUtils.getShortFromLong(l, POSITION_OF_TILE_IN_SEQUENCE_OFFSET), name, sequence, forwardStrand);
		}
		return null;
	}

	private static void addEntriesToListIfExistInMap(int commonTileCountAtStart, Map<Integer, TLongList> map1, TLongList perfectMatches, int maxTileCount, int halfMaxTileCount, boolean rc) {
		if (commonTileCountAtStart < halfMaxTileCount) {
			int tileCountWithCommonCount = NumberUtils.pack2IntsInto1(maxTileCount - commonTileCountAtStart, 0);
			TLongList tileCountWithCommonCountPositions = map1.get(tileCountWithCommonCount);
			if (null != tileCountWithCommonCountPositions) {
				/*
				 * only add positions that DO NOT have RC bit set with rc = false, or DO have RC bit set and rc = true
				 */
				int tally = 0;
				for (long l : tileCountWithCommonCountPositions.toArray()) {
					boolean isRC = NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
					if ((rc && isRC) || (! rc && ! isRC)) {
						perfectMatches.add(l);
						tally++;
					}
				}
				logger.debug("added " + tally + " positions to perfect matches list, rc: " + rc);
			}
		}
	}
	
	/**
	 * Will perform a match of the cpRef against the supplied sequence, if they match then a new ChrPositionName object will be created and returned.
	 * If there is no match, an empty Optional will be returned
	 * 
	 * @param contig
	 * @param start
	 * @param stop
	 * @param sequence must be on the forward strand as will be compared against reference (which is on the forward strand)
	 * @return
	 */
	public static Optional<ChrPosition> getChrPositionWithReference(String contig, int start, String sequence, String cpRef) {
		if (sequence.length() > cpRef.length()) {
			/*
			 * cp will cover part of sequence only
			 */
			int index = sequence.indexOf(cpRef);
			if (index > -1) {
				return Optional.of(new ChrPositionName(contig, start, start + cpRef.length(), cpRef));
			}
		} else {
			int index = cpRef.indexOf(sequence);
			if (index > -1) {
				return Optional.of(new ChrPositionName(contig, start, start + sequence.length(), sequence));
			}
		}
		
		return Optional.empty();
	}

	/**
	 * Converts a String consisting of a list of longs into an int array.
	 * If the String starts with a 'C' this corresponds to 'Count' and is followed by the
	 *  number of times in the genome this particular tile was found
	 *   In this instance, we will return an empty array
	 * @param s
	 * @return
	 */
	public static int[] convertStringToIntArray(String s) {
		if ( ! StringUtils.isNullOrEmpty(s)) {
			
			if (s.charAt(0) != 'C') {
				int commaIndex = s.indexOf(Constants.COMMA);
				int oldCommaIndex = 0;
				int [] positionsArray = new int[org.apache.commons.lang3.StringUtils.countMatches(s, Constants.COMMA) + 1];
				int i = 0;
				while (commaIndex > -1) {
					positionsArray[i++] = ((int)Long.parseLong(s.substring(oldCommaIndex, commaIndex)));
					oldCommaIndex = commaIndex + 1;
					commaIndex = s.indexOf(Constants.COMMA, oldCommaIndex);
				}
				/*
				 * add last entry
				 */
				positionsArray[i] = ((int)Long.parseLong(s.substring(oldCommaIndex)));
				return positionsArray;
			}
		}
		return new int[] {};
	}
}
