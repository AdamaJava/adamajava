package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPairs;
import au.edu.qimr.tiledaligner.model.TARecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.qmule.SmithWatermanGotoh;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TLongIntProcedure;
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
	public static final String[] REPEATS = new String[]{REPEAT_A, REPEAT_C, REPEAT_G, REPEAT_T};
	
	public static final int MINIMUM_BLAT_RECORD_SCORE = 20;
	
	public static AtomicInteger swCounter = new AtomicInteger();
	public static AtomicInteger splitconCounter = new AtomicInteger();
	
	public static Map<String, byte[]> referenceCache = new THashMap<>();
	public static SAMSequenceDictionary dictionary = null;
	
	public static final IntSummaryStatistics iss = new IntSummaryStatistics();
	
	private static final QLogger logger = QLoggerFactory.getLogger(TiledAlignerUtil.class);
	
	
	/**
	 * This method take an input string and splits it up into tiles of length tilsSize
	 * Each of these tiles is put into a map, with an empty string as the value.
	 * This map is returned.
	 * 
	 * @param input
	 * @param tileSize
	 * @return
	 */
	public static void tileInput(String input, int tileSize, Map<String, String> map) {
		if ( ! StringUtils.isNullOrEmpty(input)) {
			
			/*
			 * split the input into tiles of length tileSize
			 * insert into map with empty string as value
			 */
			
			int length = input.length();
			for (int i = 0 ; i <= length - tileSize ; i++) {
				String s = input.substring(i, i + tileSize);
				if (s.indexOf('N') > -1) {
					List<String> list = getAlternativeSequences(s);
					map.putAll(list.stream().collect(Collectors.toMap(k -> k, v -> "")));
				}
				map.put(s, "");
			}
			String rc = SequenceUtil.reverseComplement(input);
			for (int i = 0 ; i <= length - tileSize ; i++) {
				String s = rc.substring(i, i + tileSize);
				if (s.indexOf('N') > -1) {
					List<String> list = getAlternativeSequences(s);
					map.putAll(list.stream().collect(Collectors.toMap(k -> k, v -> "")));
				}
				map.put(s, "");
			}
			logger.info("created map with " + map.size() + " entries from input string: " + input + ", and its reverse complement!");
		}
	}
	
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
	
	public static void getTiledDataInMap(String tiledAlignerFile, int tileSize, String query) {
		Map<String, String> tileInput = new HashMap<>();
		tileInput(query, tileSize, tileInput);
		getTiledDataInMap(tiledAlignerFile, tileInput);
	}
	
	public static Map<Integer, TLongList> map(Map<String, TLongList> map, String contig, int tileSize) {
		
		if ( ! StringUtils.isNullOrEmpty(contig)) {
			
			/*
			 * get forward strand results in map1, reverse strand results in map2
			 * perform the updating of the longs in map2, and add to map1
			 */
			Map<Integer, TLongList> map1 = getTiles(map, contig, tileSize, false);
			logger.info("And now for the reverse complement: " + SequenceUtil.reverseComplement(contig));
			Map<Integer, TLongList> map2 = getTiles(map, SequenceUtil.reverseComplement(contig), tileSize, true);
			for (Entry<Integer, TLongList> entry : map2.entrySet()) {
				map1.computeIfAbsent(entry.getKey(), f -> new TLongArrayList()).addAll(entry.getValue());
			}
			return map1;
		}
		return Collections.emptyMap();
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
	
	public static Map<Integer, TLongList> getCountStartPositionsMapUsingCache(TIntObjectMap<int[]> cache, String contig, int tileSize, boolean rc) {
		return getCountStartPositionsMapUsingCache(cache, contig, tileSize, rc, false);
	}
	public static Map<Integer, TLongList> getCountStartPositionsMapUsingCache(TIntObjectMap<int[]> cache, String contig, int tileSize, boolean rc, boolean debug) {
		long [][] array = getStartPositionsArray( cache,  contig,  tileSize,  rc,  debug);
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
	 * unsets the REVERSE_COMPLEMENT_BIT bit in the longs in the array
	 * If the bit has been set - unset it. If it is not set, just return the original value
	 * A new long array is created and returned for this.
	 * @param array
	 * @return
	 */
	public static long[] removeReverseComplementFromArray(long [] array) {
		long [] newArray = new long[array.length];
		for (int i = 0, len = array.length ; i < len ; i++) {
			newArray[i] = NumberUtils.isBitSet(array[i], REVERSE_COMPLEMENT_BIT) ? NumberUtils.stripBitFromLong(array[i], REVERSE_COMPLEMENT_BIT) : array[i];
		}
		return newArray;
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
//						commonTileCount++;
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
//								map.computeIfAbsent(useCommonTileCount ? tally + (short)commonTileCount : tally, f -> new TLongArrayList()).add(longToUse);
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
	 * 
	 * @param array
	 * @return
	 */
	public static TLongList getShortCutPositionsForSmithwaterman(long [][] array) {
		TLongList results = new TLongArrayList();
		if (null != array && array.length > 0) {
			
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
						results.add(l);
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
	 * This method will run through the supplied tiled aligner file.
	 * If there are any tiles in the file that match the keys in the supplied map, 
	 * the values in the supplied map will be updated with the contents of the tiled aligner file for that particular tile
	 * And so the supplied map is modified by this method, and so it is NOT referentially transparent
	 * 
	 * @param tiledAlignerFile
	 * @param tiledInput
	 */
	public static void getTiledDataInMap(String tiledAlignerFile,  Map<String, String> tiledInput) {
		try (TabbedFileReader reader = new TabbedFileReader(new File(tiledAlignerFile))) {
			
			int i = 0;
			int matches = 0;
			int mapSize = tiledInput.size();
			for (TabbedRecord rec : reader) {
				if (++i % 1000000 == 0) {
					logger.info("hit " + (i / 1000000) + "M records, matches: " + matches);
				}
				String data = rec.getData();
				int tabindex = data.indexOf(Constants.TAB);
				if (tiledInput.containsKey(data.substring(0, tabindex))) {
					matches++;
					tiledInput.put(data.substring(0, tabindex), data.substring(tabindex + 1));
					
					/*
					 * If we have found all elements in the map - might as well stop looking
					 */
					if (matches == mapSize) {
						break;
					}
				}
			}
			
			logger.info("number of matches: " + matches);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static TabbedHeader getTiledAlignerHeader(String file) throws IOException {
		try (TabbedFileReader reader = new TabbedFileReader(new File(file))) {
			return reader.getHeader();
		}
	}
	
	public static String getLongListAsString(TLongList list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size() ; i++) {
			StringUtils.updateStringBuilder(sb, list.get(i) + "", Constants.COMMA);
		}
		return sb.toString();
	}
	
	public static String[] rescueSWData(String[] diffs, String ref, String binSeq) {
		if (null == diffs) {
			throw new IllegalArgumentException("Null String []  passed to CLinVarUtil.rescueSWData");
		}
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(ref)) {
			throw new IllegalArgumentException("Null or empty ref passed to CLinVarUtil.rescueSWData: " + ref);
		}
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(binSeq)) {
			throw new IllegalArgumentException("Null or empty binSeq passed to CLinVarUtil.rescueSWData: " + binSeq);
		}
		
		/*
		 * Check length of binSequence returned from SW calc, as leading/trailing mutations will have been dropped
		 */
		String swBinSeq = org.apache.commons.lang3.StringUtils.remove(diffs[2], Constants.MINUS);
		int lengthDiff = binSeq.length() - swBinSeq.length();
		if (lengthDiff > 0) {
			
			String swRef = org.apache.commons.lang3.StringUtils.remove(diffs[0], Constants.MINUS);
			
			if (binSeq.startsWith(swBinSeq)) {
				
				// need to get the last few bases
				String missingBinSeqBases = binSeq.substring(binSeq.length()  - lengthDiff);
				
				int refIndex = ref.indexOf(swRef); 
				if (refIndex > -1) {
					int positionInRef = refIndex + swRef.length();
					if (ref.length() < positionInRef + lengthDiff) {
						logger.info("WARNING: ref.length() < positionInRef + lengthDiff, ref.length(): " + ref.length() + ", positionInRef: " + positionInRef + ", lengthDiff: " + lengthDiff + ", ref: " + ref + ", binSeq: " + binSeq);
					} else {
						
						String missingRefBases = ref.substring(positionInRef, positionInRef + lengthDiff);
						
						
						if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
//							logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
							// oh dear
						} else {
//							if (lengthDiff > 1) {
//								logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
//							}
							diffs[0] += missingRefBases;
							StringBuilder sb = new StringBuilder(lengthDiff);
							for (int i = 0 ; i < lengthDiff ; i++) {
								sb.append((missingRefBases.charAt(i) == missingBinSeqBases.charAt(i)) ? '|' : Constants.MISSING_DATA);
//								diffs[1] += (missingRefBases.charAt(i) == missingBinSeqBases.charAt(i)) ? "|" : Constants.MISSING_DATA_STRING;
							}
							diffs[1] += sb.toString();
							diffs[2] += missingBinSeqBases;
						}
						
					}
				} else {
					logger.warn("WARNING: refIndex = ref.indexOf(swRef) == -1!!!");
				}
				
				
			} else if (binSeq.endsWith(swBinSeq)) {
				// need to get the first few bases
				String missingBinSeqBases = binSeq.substring(0, lengthDiff);
				
				int refIndex = ref.indexOf(swRef); 
				if (refIndex > -1) {
					if (refIndex - lengthDiff < 0) {
//						logger.warn("refIndex - lengthDiff is lt 0, refIndex:  " + refIndex + ", lengthDiff: " + lengthDiff + ", ref: " + ref + ", binSeq: " + binSeq);
//						for (String s : diffs) {
//							logger.warn("s: " + s);
//						}
					}
					String missingRefBases = ref.substring(Math.max(0, refIndex - lengthDiff), refIndex);
					
					if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
//						logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
						// oh dear
					} else {
//						if (lengthDiff > 1) {
//							logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
//						}
						diffs[0] = missingRefBases + diffs[0];
						StringBuilder sb = new StringBuilder(lengthDiff);
						for (int i = 0 ; i < lengthDiff; i++) {
							sb.append((missingRefBases.charAt(i) == missingBinSeqBases.charAt(i)) ? '|' : Constants.MISSING_DATA);
//							diffs[1] =  ((missingRefBases.charAt(i) == missingBinSeqBases.charAt(i)) ? "|" : Constants.MISSING_DATA_STRING) + diffs[1];
						}
						diffs[1] = sb.toString() + diffs[1];
						diffs[2] = missingBinSeqBases + diffs[2];
					}
						
				} else {
					logger.warn("WARNING: refIndex = ref.indexOf(swRef) == -1!!!");
				}
				
//			} else {
//				logger.warn("binSeq neither startsWith norEndsWith swBinSeq. binSeq: " + binSeq + ", swBinSeq: " + swBinSeq);
			}
		}
		return diffs;
	}
	
	public static String[] getSwDiffs(String ref, String sequence, boolean rescueSequence, boolean optimiseForGaps) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(ref)
				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(sequence)) {
			throw new IllegalArgumentException("ref or sequence (or both) supplied to ClinVarUtil.getSwDiffs were null. ref: " + ref + ", sequence: " + sequence);
		}
		
//		SmithWatermanGotoh nm = optimiseForGaps ? new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4) : new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 1);
		
		/*
		 * If sequence has more than a couple of N's, use the original SW values
		 * which are: 5, -4, 16, 4
		 * 
		 */
		int nCount = getInsertionCount(sequence, 'N');
		boolean useOriginalValues = nCount > 2;
		
		SmithWatermanGotoh nm = null;
		
		swCounter.incrementAndGet();
		if (useOriginalValues) {
//			System.out.println("using original sw values as nCount is: " + nCount);
			nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
		} else {
			nm = new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 1);
		}
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 0);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 1);
//		SmithWatermanGotoh nm = optimiseForGaps ? new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 0) : new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);		// good for gaps
//		SmithWatermanGotoh nm = optimiseForGaps ? new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 1) : new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);		// good for gaps
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);	// original
		String [] diffs = nm.traceback();
//		return nm.traceback();
		
		/*
		 * get number of insertions - if more than 5, go for traditional sw
		 *
		 */
		
		int insertionCount = getInsertionCount(diffs[1], ' ');
		if (insertionCount >= 4 &&  ! useOriginalValues) {
//			if (insertionCount >= 4 && optimiseForGaps && ! useOriginalValues) {
			swCounter.incrementAndGet();
			nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);	// original
//			nm = new SmithWatermanGotoh(ref, sequence, 5, -6, 16, 10);	// increase gap penalty
			String [] newDiffs = nm.traceback();
			int insertionCountForNewDiffs = getInsertionCount(newDiffs[1], ' ');
			if (insertionCountForNewDiffs < insertionCount) {
//				if (insertionCountForNewDiffs < insertionCount && getSWScore(newDiffs[1])  >= getSWScore(diffs[1]) ) {
				diffs = newDiffs;
			}
		}
		return diffs;
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
				if ((scoresLenient[0] - scoresLenient[1] - scoresLenient[2]) > (scoresStrict[0] - scoresStrict[1] - scoresStrict[2])) {
					return diffsLenient;
				} else {
					return diffsStrict;
				}
			} else if (lenientPassesTest) {
				return diffsLenient;
			} else if (strictPassesTest) {
				return diffsStrict;
			} else {
				logger.warn("sw diffs did not pass the mismatch (" + scoresLenient[1] + " and allowed up to " + maxMisMatchCount + "), or block (" + maxBlockCount + "), cutoff requirements");
				for (String s : diffsLenient) {
					logger.info("diffsLenient: " + s);
				}
				for (String s : diffsStrict) {
					logger.info("diffsStrict: " + s);
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

	/**
	 * try to mirror this on the BLATRecord score which is matches - mismatches - number of insertions in ref - number of insertions in query
	 * @param diffs
	 * @return
	 */
	public static int getSWScore(String diffs) {
		int matchCount = 0, mismatchCount = 0, gapCount = 0;
		for (char c : diffs.toCharArray()) {
			if ('|' == c) {
				matchCount++;
			} else if ('.' == c) {
				mismatchCount++;
			} else if (' ' == c) {
//				gapCount++;
			} else {
				logger.warn("found [" + c + "] in sw diffs string!!!");
			}
		}
		return matchCount - mismatchCount - (1 * getInsertionCount(diffs, ' '));
	}
	
	
	public static List<String []> getSmithWaterman(String refFile, TLongIntMap tiledAlignerPositions, String sequence, String name, PositionChrPositionMap pcpm, boolean debug, int misMatchCutoffPercentage, int blockCountCutoff) {
		List<String [] > swResults = new ArrayList<>();
		int maxScore = sequence.length();
		int passingPercentScore = (int) (maxScore * 0.99);
		float misMatchCutoff = (((float)misMatchCutoffPercentage) / 100) * maxScore;
		
		boolean preferStrictSW = name != null && name.startsWith("splitcon");
		
		/*
		 * sort positions by tile count so that we have highest matches first
		 */
		TIntObjectMap<TLongList> tiledAlignerPositionsMap = new TIntObjectHashMap<>();
		tiledAlignerPositions.forEachEntry(new TLongIntProcedure() {
			@Override
			public boolean execute(long l, int i) {
				TLongList longList = tiledAlignerPositionsMap.get(i);
				if (null == longList) {
					longList = new TLongArrayList();
					tiledAlignerPositionsMap.put(i,  longList);
				}
				longList.add(l);
				return true;
			}
		});
		int [] sortedKeys = TARecordUtil.sortTileCount(tiledAlignerPositionsMap.keys());
		
		String sequenceRC = SequenceUtil.reverseComplement(sequence);
		Set<ChrPosition> swsAlreadyPerformed = new HashSet<>(sortedKeys.length * 2);
		
		for (int key : sortedKeys) {
			int matchLength = NumberUtils.getPartOfPackedInt(key, true) + TILE_LENGTH_MINUS_ONE;
			for (long l : tiledAlignerPositionsMap.get(key).toArray()) {
				
				boolean reverseComplement = NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
				
				ChrPosition bufferedCP = getBufferedChrPosition(l, sequence.length(), matchLength, pcpm, 20);
				
				if ( ! swsAlreadyPerformed.contains(bufferedCP)) {
					swsAlreadyPerformed.add(bufferedCP);
					String bufferedReference = getRefFromChrPos(bufferedCP, refFile);
					String fragString = reverseComplement ? sequenceRC : sequence;
					
					/*
					 * perform and indexOf to see if we have an exact match!
					 */
					int index = bufferedReference.indexOf(fragString);
					
					if (index > -1) {
						if (debug) {
							logger.info("Got a perfect match using indexOf!!! " + bufferedCP.getChromosome() + ":" + (bufferedCP.getStartPosition()));
						}
						/*
						 * create matching swdiffs array
						 */
						String[] swMatches = new String[3];
						swMatches[0] = fragString;
						swMatches[1] = org.apache.commons.lang3.StringUtils.repeat('|', fragString.length());
						swMatches[2] = fragString;
						String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swMatches, name, sequence, ! reverseComplement, bufferedReference);
						swResults.add(blatDetails);
					} else {
						
						if (debug) {
							logger.info("about to sw ref: " + bufferedReference + ", fragString: " + fragString + " for cp: " + bufferedCP.getChromosome() + ":" +  (bufferedCP.getStartPosition()));
						}
						
						/*
						 * this method will only return a populated string array if valid results have been found (ie. passed the filters)
						 * and so can add directly to collection
						 */
						String [] swDiffs = getIntelligentSwDiffs(bufferedReference, fragString, misMatchCutoff, blockCountCutoff, preferStrictSW);
						if (swDiffs.length > 0) {
							int score = getSWScore(swDiffs[1]);
							if (debug) {
								if (score > 30) {
									for (String s : swDiffs) {
										logger.info("swDiffs: " + s);
									}
									logger.info("sw score: " + score + ", fragString length: " + fragString.length());
								}
							}
							
							String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, name, sequence, ! reverseComplement, bufferedReference);
							swResults.add(blatDetails);
							if (score == maxScore) {
								logger.info("max score [" + maxScore + "] (seq length: " + sequence.length() + "), has been reached - exiting sw! " + Arrays.deepToString(blatDetails));
								break;
							} else if (score > passingPercentScore) {
								logger.info("passing percentage score [" + passingPercentScore + "] (seq length: " + sequence.length() + "), has been reached - exiting sw! " + Arrays.deepToString(blatDetails));
								break;
							}
						}
					}
				}
			}
		}
		
		return swResults;
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
		
		short sequenceOffset = NumberUtils.getShortFromLong(packedLong, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
		int lhsBuffer = sequenceOffset == 0 ? 0 : sequenceOffset + buffer;
		int rhsBuffer = sequenceOffset + matchLength == sequenceLength ? 0 : buffer;
		
		ChrPosition bufferedCP = pcpm.getBufferedChrPositionFromLongPosition(packedLong, sequenceLength - sequenceOffset, lhsBuffer, rhsBuffer);
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
//					startPosition = i + 1;
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
		return getInsertionCount(s, '-');
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
			
			List<BLATRecord> blatties = getBlatRecords(refFile, cache, entry.getKey(), entry.getValue(), tileLength, originatingMethod, log, recordsMustComeFromChrInName);
			/*
			 * populate the name field on the BLATRecord with the value, if present - otherwise just leave as the default
			 */
			if (null != entry.getValue()) {
				for (BLATRecord b : blatties) {
					b.setName(entry.getValue());
				}
			}
			results.put(entry.getKey(), blatties);
		}
		return results;
	}
	
	public static List<BLATRecord> getBlatRecords(String refFile, TIntObjectMap<int[]> cache, String sequence, final String name, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) {
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
		
		List<BLATRecord> results = new ArrayList<>();
		String revCompSequence = SequenceUtil.reverseComplement(sequence);
		
		long [][] startPositions = getStartPositionsArray(cache, sequence, tileLength, false, log);
		long [][] startPositionsRC = getStartPositionsArray(cache, revCompSequence, tileLength, true, log);
		
		TLongList potentialMatches = getShortCutPositionsForSmithwaterman(startPositions);
		TLongList potentialMatchesRC = getShortCutPositionsForSmithwaterman(startPositionsRC);
		
		
		
		Map<Integer, TLongList> map1 = convertLongDoubleArrayToMap(startPositions, false);
		Map<Integer, TLongList> map2 = convertLongDoubleArrayToMap(startPositionsRC, true);
//		Map<Integer, TLongList> map1 = getCountStartPositionsMapUsingCache(cache, sequence, tileLength, false, log);
//		Map<Integer, TLongList> map2 = getCountStartPositionsMapUsingCache(cache, revCompSequence, tileLength, true, log);
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
		if (recordsMustComeFromChrInName) {
			/*
			 * get chromosome names from name
			 */
			String [] nameArray = name.split("_");
			List<long[]> ranges = new ArrayList<>();
			for (String s : nameArray) {
				if (s.startsWith("chr") || s.startsWith("GL")) {
					ranges.add(headerMap.getLongStartAndStopPositionFromChrPosition(new ChrPointPosition(s, 1)));
				}
			}
			Map<Integer, TLongList> updatedMap =  NumberUtils.getUpdatedMapWithLongsFallingInRanges(map1, ranges, REVERSE_COMPLEMENT_BIT);
			
			if ( ! updatedMap.isEmpty()) {
				map1 = updatedMap;
			}
			
			logger.info("we have " + ranges.size() + " ranges for this name: " + name + ", updated map size: " + updatedMap.size() + ", going with map of size: " + map1.size());
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
		
		/*
		 * If we have a perfect match here, don't bother doing any SW
		 */
		TLongList perfectMatches = taRec.getPerfectMatchPositions();
		if (log) {
			logger.info("Perfect match? " + (null != perfectMatches && ! perfectMatches.isEmpty()));
		}
		if (null != perfectMatches && ! perfectMatches.isEmpty()) {
			int maxNumberOfPerfectMatches = 5;
			if (sequence.indexOf('N') > -1) {
				if (log) {
					logger.info("Can't find perfect match - sequence has an N! seq: " + sequence);
				}
			} else {
				/*
				 * only proceed if all bases are represented in this string
				 */
				int seqComplexity = StringUtils.determineSequenceComplexity(sequence).cardinality();
				if (seqComplexity == 4 || (seqComplexity >= 2 && sequence.length() >= 50)) {
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
								logger.info("got a perfect match!  cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
								needToRunSW = false;
								results.add(new BLATRecord(BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(optionalCP.get()), name, forwardStrand ? sequence : revCompSequence, forwardStrand)));
								/*
								 * If we have reached the max number of perfect matches, bail
								 */
								if (results.size() == maxNumberOfPerfectMatches) {
									break;
								}
							} else {
								logger.info("got perfect match on tiles, but not on sequence! cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
							}
						}
					}
				}
			}
		}
		if (needToRunSW) {
			
			/*
			 * OK, no perfect match, so here is the plan
			 * 
			 * If our top match is less than 70% but more than 50%, we will look for splits
			 * If possible, will create a single BLATRec from the splits, but must be on same chromosome, same strand, and not too far away (around 10000 bases)
			 * If not possible, then will create multiple BLATRecords.
			 * If we don't get any splits, fall through to smith-waterman
			 * 
			 */
			
			boolean gotSplits = false;
			boolean runSplits = null != name && name.startsWith("splitcon");
			
			/*
			 * If the max tile count is low, try running the split to see if its made up of multiple entries.
			 * If it is, and its not a split, may need to combine into a single BLATRecord
			 */
			int highestTileCount = taRec.getHightestTileCount();
			if (highestTileCount <= 0) {
				/*
				 * we're done
				 */
				logger.info("Found no tile matches for sequence: " + sequence);
				return Collections.emptyList();
			}
			int currentMaxLength = TARecordUtil.getExactMatchOnlyLengthFromPackedInt(taRec.getHightestTileCount());
			int commonTileCount = NumberUtils.getPartOfPackedInt(taRec.getHightestTileCount(), false);
			int nCount = org.apache.commons.lang3.StringUtils.countMatches(sequence, 'N');
			
//			int maxTileMatch = TARecordUtil.getExactMatchOnlyLengthFromPackedInt(taRec.getHightestTileCount());
			
			if (currentMaxLength < (0.7 * sequence.length()) && (currentMaxLength) >= (0.5 * sequence.length())) {
				logger.info("name: " + name + ", currentMaxLength: " + currentMaxLength + ", sequence.length(): " + sequence.length() + ", less then 70% of bases covered ( && >= 50%) - will look for splits!");
				runSplits = true;
			} else {
				if ( ! runSplits) {
					logger.info("name: " + name + ", currentMaxLength: " + currentMaxLength + ", sequence.length(): " + sequence.length() + ", will not run splits");
				}
			}
			
			if (commonTileCount > (0.1 * sequence.length())) {
				if (runSplits) {
					logger.info("Too many commonly occurring tiles to be able to run splts, commonTileCount: " + commonTileCount + ", sequence.length(): " + sequence.length() + ", maxTileMatch: " + currentMaxLength);
				}
				runSplits = false;
			}
			
			/*
			 * randomly chosen value for nCount cutoff
			 */
			if (nCount > 5) {
				if (runSplits) {
					logger.info("Too many N's in sequence - won't run splits! nCount: " + nCount);
				}
				runSplits = false;
			}
			
			if (runSplits) {
				
				logger.info("about to run some splits, no of positions in TARecord:  name: " + name + ", " + taRec.getCountDist() + "");
				TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(taRec);
				List<IntLongPairs> potentialSplits = new ArrayList<>();
				splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
				/*
				 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
				 */
				int passingScore = (int)(0.9 * sequence.length());
				results.addAll(potentialSplits.stream()
						.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
						.map(ilp ->  TARecordUtil.blatRecordFromSplits(ilp, name, sequence.length(), headerMap, TILE_LENGTH))
						.filter(sa -> sa.length > 0)
						.map(s -> new BLATRecord(s))
//						.filter(br -> br.getScore() > passingScore)
						.collect(Collectors.toList()));
				
				logger.info("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size() + ", # of records in results after examining potentialSplits: " + results.size());
				
				/*
				 * check to see if we have a record that meets the passing score
				 * if so, set getSplits to true, which means no need to smith waterman
				 */
				
				gotSplits = results.stream().anyMatch(br -> br.getScore() > passingScore);
				
				/*
				 * If we have our split from the areSplitsCloseEnoughToBeSingleRecord method, no need to go looking for more
				 */
				if ( ! gotSplits) {
					
					List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, taRec.getSequence().length(), headerMap);
					logger.info("splits blat record count: " + blatRecs.size() + " for " + name);
					if ( ! blatRecs.isEmpty()) {
						
						for (BLATRecord[] recs : blatRecs) {
							int combinedScore = 0;
							for (BLATRecord bt : recs) {
								combinedScore += bt.getScore();
							}
							/*
							 * if we have more then 80% of bases covered, we cool, otherwise SW
							 */
							if (((double)combinedScore / taRec.getSequence().length()) >= 0.9) {
								logger.info("No need to smith waterman - have some records, combined score: " + combinedScore + ", seqLength: " + taRec.getSequence().length()  + " for " + name);
								for (BLATRecord bt : recs) {
									results.add(bt);
								}
								gotSplits = true;
							}
						}
//					}
						
					} else {
						logger.info("No split record found for name: " + name + ", gotSplits: " + gotSplits);
					}
				}
			}
			
			if ( ! gotSplits) {
				
				/*
				 * get best start positions
				 */
//				TLongList bestStartPositions = taRec.getStartPositions(12, true, 20);
				TLongIntMap bestStartPositionsMap = taRec.getStartPositions();
				
				
//				TLongList bestStartPositionsUpdated = ListUtils.removeAdjacentPositionsInList(bestStartPositions, 200);
	//		System.out.println("Number of entries in bestStartPositions: " + bestStartPositions.size() + ", Number of entries in bestStartPositionsUpdated (removed adjacent positions): " + bestStartPositionsUpdated.size());
				/*
				 * smith waterman
				 */
				List<ChrPosition> tiledAlignerCPs = Arrays.stream(bestStartPositionsMap.keys()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).collect(Collectors.toList());
//				List<ChrPosition> tiledAlignerCPsUpdated =  ListUtils.removeAdjacentPositionsInList(tiledAlignerCPs, sequence.length() < 100 ? 350 : 1700);
				if (log) {
					for (ChrPosition cp : tiledAlignerCPs) {
						logger.info("will look at " + cp.toIGVString() + ", strand: " + ((ChrPositionName)cp).getName());
					}
					logger.info("Number of entries in bestStartPositionsMap: " + bestStartPositionsMap.size());
				}
				
				int misMatchCutoff = (int) (0.1 * sequence.length());
				
				List<String[]> swResults = getSmithWaterman(refFile, bestStartPositionsMap, sequence, name != null ? name : "name", headerMap, log, 10, 6);
				for (String [] array : swResults) {
					BLATRecord br = new BLATRecord(array);
					if (br.getScore() > MINIMUM_BLAT_RECORD_SCORE && br.getMisMatches() <= misMatchCutoff && br.getBlockCount() <= 6) {
						results.add(br);
					} else if (log) {
						logger.debug("BLAT record score is below the cutoff of " + MINIMUM_BLAT_RECORD_SCORE + " or has too many mismatches, or too many (>6) blocks " + br);
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
		if (potentialMatches.size() > 0 || potentialMatchesRC.size() > 0) {
			logger.info("shortcut!!! potentialMatches.size: " + potentialMatches.size() + ", potentialMatchesRC.size(): " + potentialMatchesRC.size() + ", uniqueResults.size(): " + uniqueResults.size() + ", name: " + name + ", seq length: " + sequence.length() + ", seq: " + sequence);
		}
		
		if (null != name && name.contains("splitcon")) {
			/*
			 * really only care if we have different chromosomes
			 */
			String[] nameArray = name.split("_");
			if (nameArray.length > 4 &&  ! nameArray[1].equals(nameArray[3])) {
				logger.info("got a splitcon: " + name + " with no of recs: " + uniqueResults.size() + ", taRec: " + (null != taRec ? taRec.toString() : "null"));
				for (BLATRecord br : uniqueResults) {
					logger.info("splitcon rec: " + br);
				}
			}
		}
		
		
		return uniqueResults;
	}
	
	public static short[] getBaseDistribution(String sequence) {
		short[] baseDist = new short[5];
		if ( ! StringUtils.isNullOrEmpty(sequence)) {
			for (byte b : sequence.getBytes()) {
				if (b == 'A') {
					baseDist[0]++;
				} else if (b == 'C') {
					baseDist[1]++;
				} else if (b == 'G') {
					baseDist[2]++;
				} else if (b == 'T') {
					baseDist[3]++;
				} else if (b == 'N') {
					baseDist[4]++;
				}
			}
		}
		return baseDist;
	}
	
	/**
	 * This method will examine the first 4 elements in the short array to ensure that they are equal to or higher than the percentage value that is calculated based on the sequence length and percentage
	 * @param dist
	 * @param seqLength
	 * @param percentage
	 * @return
	 */
	public static boolean isDistEvenlySpread(short[] dist, int seqLength, double percentage) {
		double percentageValue = seqLength * (percentage / 100);
		for (int i = 0 ; i < 4 ; i++) {
			if (dist[i] < percentageValue) {
				return false;
			}
		}
		return true;
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
	
//	public static Map<String, BLATRecord> runTiledAligner(String tiledAlignerFile, String [] sequences, int tileLength) {
//		
//		/*
//		 * get header and convert into object that will give a ChrPosition when supplied with a long
//		 */
//		PositionChrPositionMap headerMap = new PositionChrPositionMap();
//		try {
//			TabbedHeader header = getTiledAlignerHeader(tiledAlignerFile);
//			List<String> list = new ArrayList<>();
//			header.iterator().forEachRemaining(list::add);
//			headerMap.loadMap(list);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Map<String, BLATRecord> output = new HashMap<>(sequences.length * 2);
//		
//		/*
//		 * try and get direct matches for the supplied sequences and their rcs
//		 */
//		List<String> sequencesToMatch = new ArrayList<>();
//		for (String s : sequences) {
//			if (null != s && s.indexOf('N') == -1) {
//				sequencesToMatch.add(s);
//			}
//		}
//		System.out.println("will send " + sequencesToMatch.size() + " sequence for direct matching!");
//		Map<String, ChrPosition> matches = getDirectMatch(sequencesToMatch.toArray(new String[] {}));
//		System.out.println("got " + matches.size() + " direct matches!!!");
//		
//		for (Entry<String, ChrPosition> match : matches.entrySet()) {
//			output.put(match.getKey(), new BLATRecord(getDetailsForBLATRecordPerfectMatch(match.getValue(), "name", match.getKey(), true)));
//			System.out.println("matches:" + match.getKey());
//		}
//		
//		int newSequenceSize = sequences.length - matches.size();
//		if (newSequenceSize > 0) {
//			System.out.println("sequences.length: " + sequences.length);
//			System.out.println("matches.size(): " + matches.size());
//			System.out.println("newSequenceSize: " + newSequenceSize);
//			String [] newSequences = new String[newSequenceSize];
//			int k = 0;
//			for (String s : sequences) {
//				
//				if (matches.containsKey(s)) {
//					System.out.println("in matches: " + s);
//				} else if (matches.containsKey(SequenceUtil.reverseComplement(s))) {
//					System.out.println("in matches (rc): " + SequenceUtil.reverseComplement(s));
//				} else {
//					System.out.println("putting into array: " + s);
//					newSequences[k++] = s;
//				}
//			}
//		
//			Map<String, String> tiledInput = new HashMap<>();
//			for (String s : newSequences) {
//				tileInput(s, tileLength, tiledInput);
//			}
//			getTiledDataInMap(tiledAlignerFile, tiledInput);
//			// call convertMapOfStringsToLongList to get map of tile and list of positions it appears
//			Map<String, TLongList> tilesAndPositions = convertMapOfStringsToLongList(tiledInput);
//		
//			for (String s : newSequences) {
//				if (null != s && s.length() > 0) {
//					Map<Integer, TLongList> results = map(tilesAndPositions, s, tileLength);
//					
//					List<Integer> highestCounts = new ArrayList<>(results.keySet());
//					highestCounts.sort(null);
//					for (int i = highestCounts.size() - 1 ; i >= 0 ; i--) {
//						System.out.println("tile count: " + highestCounts.get(i) + ", number of start positions: " + results.get( highestCounts.get(i)).size());
//					}
//				
//					TARecord rec = new TARecord(s, results);
//					
//					
//					/*
//					 * remove adjacent positions as they will just render the same result
//					 */
//					TLongList bestStartPositions = rec.getStartPositions(12, true, 20);
//					TLongList bestStartPositionsUpdated = ListUtils.removeAdjacentPositionsInList(bestStartPositions);
//					System.out.println("Number of entries in bestStartPositions: " + bestStartPositions.size() + ", Number of entries in bestStartPositionsUpdated (removed adjacent positions): " + bestStartPositionsUpdated.size());
//					if (bestStartPositions.isEmpty()) {
//						System.out.println("Found no start positions for sequence: " + s);
//					} else {
//						
//						/*
//						 * check to see if we have both strands
//						 */
//						boolean directMatchFound = false;
//						
//						if ( ! directMatchFound) {
//							List<ChrPosition> tiledAlignerCPs = Arrays.stream(bestStartPositionsUpdated.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).collect(Collectors.toList());
//							for (ChrPosition cp : tiledAlignerCPs) {
//								System.out.println("will look at " + cp.toIGVString() + ", strand: " + ((ChrPosBait)cp).getBait());
//							}
//							
//							List<String[]> swResults = getSmithWaterman(tiledAlignerCPs, s, null);
//							List<BLATRecord> blatRecs = new ArrayList<>();
//							for (String [] array : swResults) {
//								blatRecs.add(new BLATRecord(array));
//							}
//							
//							blatRecs.sort(null);
//							System.out.println("number of blat records for s: " + s +", " + blatRecs.size());
//							
//							output.put(s, blatRecs.get(blatRecs.size() - 1));
//						}
//					}
//				}
//			}
//		}
//		return output;
//	}
	
}
