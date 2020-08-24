package org.qcmg.qsv.tiledaligner;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosBait;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.ListUtils;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qmule.SmithWatermanGotoh;
import org.qcmg.qsv.blat.BLATRecord;
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
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.SequenceUtil.SequenceListsDifferException;

public class TiledAlignerUtil {
	
	public static final int TILE_LENGTH = 13;
	public static final int REVERSE_COMPLEMENT_BIT = 62;
	public static final int COMMON_OCCURING_TILE_BIT = 63;
	public static final int POSITION_OF_TILE_IN_SEQUENCE_OFFSET = 40;
	public static final int SMITH_WATERMAN_BUFFER_SIZE_TIER_1 = 1000;
	public static final int SMITH_WATERMAN_BUFFER_SIZE_TIER_2 = 3000;
//	public static final int SMITH_WATERMAN_BUFFER_SIZE_TIER_1 = 100;
//	public static final int SMITH_WATERMAN_BUFFER_SIZE_TIER_2 = 500;
	public static final int INDEL_GAP = 50;
	
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
	public static final TIntObjectMap<AtomicInteger> leaderboardStats = new TIntObjectHashMap<>();
	
	public static final AtomicIntegerArray taClassifierArrayPosition1 = new AtomicIntegerArray(TAClassifier.getMaxPosition() + 1);
	public static final AtomicIntegerArray taClassifierArrayPosition2 = new AtomicIntegerArray(TAClassifier.getMaxPosition() + 1);
	
	public static final ConcurrentMap<String, List<String>> sequenceOriginatingMethodMap = new ConcurrentHashMap<>();
	
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
//		Map<String, String> map = new HashMap<>();
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
			System.out.println("created map with " + map.size() + " entries from input string: " + input + ", and its reverse complement!");
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
	
	
	public static Map<String, TLongList> convertMapOfStringsToLongList(Map<String, String> map) {
		Map<String, TLongList> positionalMap = new HashMap<>(map.size() * 2);
		
		for (Entry<String, String> entry : map.entrySet()) {
			positionalMap.put(entry.getKey(), convertStringToListOfLongs(entry.getValue()));
		}
		
		return positionalMap;
	}
	
	
	/**
	 * Method to convert a string containing a list of positions to a TLongList
	 * There is a special case where if the string starts with a 'C', it is actually 
	 * representing a tile that is in too many positions in the genome to list
	 * and so the value after the 'C' is a 'C'ount of the number of times the tile is seen
	 * 
	 * @param s
	 * @return
	 */
	public static TLongList convertStringToListOfLongs(String s) {
		TLongList list = new TLongArrayList(1);
		if ( ! StringUtils.isNullOrEmpty(s)) {
			
			/*
			 * If the string starts with a 'C', this corresponds to 'Count' and is followed by the
			 *  number of times in the genome this particular tile was found
			 * In this instance, we will return a list with a single element containing the count, 
			 * and the MSB set to 1, which will allow us to identify this entry as a count rather than a position
			 */
			if (s.charAt(0) == 'C') {
				list.add(setTooManyStartPositionsBit(Long.parseLong(s.substring(1))));
//				list.add(setMSB(Integer.parseInt(s.substring(1))));
			} else {
				String [] array = TabTokenizer.tokenize(s, Constants.COMMA);
				for (String ar : array) {
					list.add(Long.parseLong(ar));
				}
			}
		}
		return list;
	}
	
	public static long setReverseStrandBit(long l) {
		return NumberUtils.setBit(l, REVERSE_COMPLEMENT_BIT);
	}
	public static long setTooManyStartPositionsBit(long l) {
		return NumberUtils.setBit(l, COMMON_OCCURING_TILE_BIT);
	}
	
	public static Map<Integer, TLongList> map(Map<String, TLongList> map, String contig, int tileSize) {
		
		if ( ! StringUtils.isNullOrEmpty(contig)) {
			
			/*
			 * get forward strand results in map1, reverse strand results in map2
			 * perform the updating of the longs in map2, and add to map1
			 */
			Map<Integer, TLongList> map1 = getTiles(map, contig, tileSize, false);
			System.out.println("And now for the reverse complement: " + SequenceUtil.reverseComplement(contig));
			Map<Integer, TLongList> map2 = getTiles(map, SequenceUtil.reverseComplement(contig), tileSize, true);
			for (Entry<Integer, TLongList> entry : map2.entrySet()) {
				map1.computeIfAbsent(entry.getKey(), f -> new TLongArrayList()).addAll(entry.getValue());
			}
			return map1;
		}
		return Collections.emptyMap();
	}
	
	
	/**
	 * setting the reverse strand bit
	 * @param list
	 */
	public static void updateLongListWithReverseStrandBit(TLongList list) {
		list.transformValues(l -> setReverseStrandBit(l));
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
//				array[i] = rc ? addReverseComplementToArray(list.toArray()) : list.toArray();
				
				System.out.println("start positions for tile: " + tile + ": " + Arrays.toString(list.toArray()));
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
		
		
//		int length = contig.length();
//		int arraySize = length - tileSize + 1;
//		if (arraySize <= 0) {
//			System.out.println("array size is less than zero! contig: " + contig);
//			return Collections.emptyMap();
//		}
//		long [][] array = new long[arraySize][];
//		for (int i = 0 ; i < arraySize ; i++) {
//			String tile = contig.substring(i, i + tileSize);
//			int tileInt = NumberUtils.convertTileToInt(tile);
//			/*
//			 * if the tile contains a N, add all variants of this tile to the array
//			 */
//			TLongList list = new TLongArrayList();
//			int[] startPositionsForTile = cache.get(tileInt);
//			if (null != startPositionsForTile) {
//				if (startPositionsForTile.length > 0) {
//					list.addAll(Arrays.stream(startPositionsForTile).mapToLong(m -> Integer.toUnsignedLong(m)).toArray());
//				} else {
//					/*
//					 * empty array indicates a commonly occurring tile. Add -1 to the list, as that is what used to happen for common tiles
//					 */
//					list.add(-1);
//				}
//			}
////			if (null != startPositionsForTile && startPositionsForTile.length > 0) {
////				list.addAll(Arrays.stream(startPositionsForTile).mapToLong(m -> Integer.toUnsignedLong(m)).toArray());
////			}
//			if (tile.indexOf('N') > -1) {
//				List<String> alternativeTiles = getAlternativeSequences(tile);
//				for (String s : alternativeTiles) {
//					int tileInt2 = NumberUtils.convertTileToInt(s);
//					int[] altArray = cache.get(tileInt2);
//					if (null != altArray && altArray.length > 0) {
//						list.addAll(Arrays.stream(altArray).mapToLong(m -> Integer.toUnsignedLong(m)).toArray());
//					}
//				}
//			}
//			
//			if (list.isEmpty()) {
//				array[i] = null;
//				if (debug) {
//					System.out.println("NO start positions for tile: " + tile);
//				}
//			} else {
//				/*
//				 * check list, if we have a commonly occurring number (-ve) then remove all other numbers
//				 */
////				if (list.size() > 1) {
////					list.sort();
////					long l = list.get(0);
////					if (l < 0) {
////						list.clear();
////						list.add(l);
////					}
////				}
//				array[i] = rc ? addReverseComplementToArray(list.toArray()) : list.toArray();
//				if (debug) {
//					System.out.println("start positions for tile: " + tile + ": " + Arrays.toString(list.toArray()));
//				}
//			}
//		}
		return convertLongDoubleArrayToMap(array, rc);
	}
	
	public static long [][] getStartPositionsArray(TIntObjectMap<int[]> cache, String contig, int tileSize, boolean rc) {
		return getStartPositionsArray(cache, contig, tileSize, rc, false);
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
			System.out.println("array size is less than zero! contig: " + contig);
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
//			if (tile.indexOf('N') > -1) {
//				List<String> alternativeTiles = getAlternativeSequences(tile);
//				for (String s : alternativeTiles) {
//					int tileInt2 = NumberUtils.convertTileToInt(s);
//					int[] altArray = cache.get(tileInt2);
//					if (null != altArray && altArray.length > 0) {
//						list.addAll(Arrays.stream(altArray).mapToLong(m -> Integer.toUnsignedLong(m)).toArray());
//					}
//				}
//			}
			
			if (list.isEmpty()) {
				array[i] = null;
				if (debug) {
					System.out.println("NO start positions for tile: " + tile);
				}
			} else {
				array[i] = list.toArray();
//				array[i] = rc ? addReverseComplementToArray(list.toArray()) : list.toArray();
				if (debug) {
					System.out.println("start positions for tile: " + tile + ": " + Arrays.toString(list.toArray()));
				}
			}
		}
		return array;
	}
	
	
	/**
	 * sets the REVERSE_COMPLEMENT_BIT bit in the longs in the array
	 * If the bit has already been set - don't set it again, as this will cause an overflow...
	 * A new long array is created and returned for this.
	 * @param array
	 * @return
	 */
//	public static long[] addReverseComplementToArray(long [] array) {
//		long [] newArray = new long[array.length];
//		for (int i = 0, len = array.length ; i < len ; i++) {
//			newArray[i] = NumberUtils.isBitSet(array[i], REVERSE_COMPLEMENT_BIT) ? array[i] : NumberUtils.setBit(array[i], REVERSE_COMPLEMENT_BIT);
//		}
//		return newArray;
//	}
	
	
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
		/*
		 * sort the arrays so that Arrays search functions can be used
		 * 
		 * don't think we need to sort here as the start positions should be sorted in the file, and in the cache
		 * 
		 * Ha! we DO need to sort, because we have added the reverse complement flag onto long positions.....
		 * 
		 */
//		for (long [] subArray : array) {
//			if (null != subArray && subArray.length > 1) {
//				Arrays.sort(subArray);
//			}
//		}
		Map<Integer, TLongList> map = new HashMap<>();
		
		/*
		 * get number of commonly occurring tiles at beginning of 2D array 
		 */
//		int commonTileCount = 0;
//		boolean useCommonTileCount = true;
		TLongIntMap positionCountMap = new TLongIntHashMap(1024);
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
	
	
//	public static Map<Integer, TLongList> convertLongDoubleArrayToMap(long [][] array) {
//		/*
//		 * sort the arrays so that Arrays search functions can be used
//		 */
//		for (long [] subArray : array) {
//			if (null != subArray && subArray.length > 1) {
//				Arrays.sort(subArray);
//			}
//		}
//		Map<Integer, TLongList> map = new HashMap<>();
//		
//		/*
//		 * get number of commonly occurring tiles at beginning of 2D array 
//		 */
//		int commonTileCount = 0;
//		boolean useCommonTileCount = true;
//		TLongIntMap positionCountMap = new TLongIntHashMap(1024);
//		int currentMaxCount = 0;
//		
//		for (int i = 0 ; i < array.length - 1 ; i++) {
//			
//			long [] subArray = array[i];
//			if (null != subArray) {
//				for (long l : subArray) {
//					
//					/*
//					 * if long is negative, then it is a commonly occurring tile - ignore these for now
//					 */
//					if (l < 0) {
//						commonTileCount++;
//						continue;
//					}
//					
//					/*
//					 * check the next array to see if we can find a +1 match
//					 */
//					int tally = nonContinuousCount(array, l, i + 1);
//					if (tally > 65536) {
//						int exactMatchComponent = NumberUtils.getPartOfPackedInt(tally, true);
//						if (exactMatchComponent > currentMaxCount) {
//							currentMaxCount = exactMatchComponent;
//						}
//						if (exactMatchComponent > 5) {
//							
//							/*
//							 * check to see if we have already recorded this in the positionCountMap
//							 */
//							
//							boolean alreadyRepresented = false;
//							for (int z = 0 ; z <= i ; z++) {
//								if ( positionCountMap.get(l - z) >= exactMatchComponent + z) {
//									alreadyRepresented = true;
//									break;
//								}
//							}
//							if ( ! alreadyRepresented) {
//								/*
//								 * add
//								 * we want to add the tile position into the long as this will aid split contig sequences
//								 */
//								long longToUse = NumberUtils.addShortToLong(l, (short) i, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
//								map.computeIfAbsent(useCommonTileCount ? tally + (short)commonTileCount : tally, f -> new TLongArrayList()).add(longToUse);
//								positionCountMap.put(l, exactMatchComponent);
//							}
//						}
//					}
//				}
//			}
//		}
//		
//		return map;
//	}
	/**
	 * The 2D long array contains the start positions of tiles that match the string that was queried
	 * This method takes that array and return a map that has a count of the continuous blocks of tiles, along with a list of the start positions.
	 * 
	 * @param array
	 * @return map containing count of continuous matching tiles, along with list of start positions
	 */
//	public static Map<Integer, TLongList> convertLongDoubleArrayToMapOld(long [][] array) {
////		long start = System.currentTimeMillis();
//		/*
//		 * sort the arrays so that Arrays search functions can be used
//		 */
//		for (long [] subArray : array) {
//			if (null != subArray && subArray.length > 1) {
//				Arrays.sort(subArray);
//			}
//		}
//		Map<Integer, TLongList> map = new HashMap<>();
//		
//		/*
//		 * get number of commonly occurring tiles at beginning of 2D array 
//		 */
////		int commonTileCount = 0;
////		boolean useCommonTileCount = true;
//		TLongSet startPositions = new TLongHashSet(4 * 1024);
//		int currentMaxCount = 0;
//		
//		for (int i = 0 ; i < array.length - 1 ; i++) {
//			
//			
//			/*
//			 * If we have a currentMaxCount that is greater than the remaining array length, then it is not possible to better that and so drop out
//			 * do that when currentMacCount is twice the remaining array length
//			 */
//			if ((array.length - i) * 2 < currentMaxCount) {
//				System.out.println("Breaking out of loop with currentMaxCount: " + currentMaxCount + ", and remaining array size: " + (array.length - i));
//				break;
//			}
//			
//			long [] subArray = array[i];
//			if (null != subArray) {
//				for (long l : subArray) {
//					
//					/*
//					 * if long is negative, then it is a commonly occurring tile - ignore these for now
//					 */
//					if (l < 0) {
////						commonTileCount++;
//						continue;
//					}
//					/*
//					 * If we already have this start position (plus any offset), don't bother examining it further
//					 */
//					boolean positionAlreadyNoted = false;
//					if ( ! startPositions.isEmpty()) {
//						for (int j = i ; j >= 0 ; j--) {
//							if (startPositions.contains(l - j)) {
//								positionAlreadyNoted = true;
//								break;	// change to break
//							}
//						}
//					}
//					if (positionAlreadyNoted) {
//						continue;
//					}
//					
//					/*
//					 * check the next array to see if we can find a +1 match
//					 */
//					int tally = nonContinuousCount(array, l, i + 1);
////					int tally = nonContinuousCount(array, l, i + 1) + (useCommonTileCount ? commonTileCount : 0);
//					//				int tally = continuousCount(array, l, i + 1);
//					int exactMatchComponent = NumberUtils.getPartOfPackedInt(tally, true);
//					if (exactMatchComponent > currentMaxCount) {
//						currentMaxCount = exactMatchComponent;
//					}
//					if (exactMatchComponent > 1) {
////						if (exactMatchComponent > 1) {
//						
//						map.computeIfAbsent(tally, f -> new TLongArrayList()).add(l);
////						map.computeIfAbsent(useCommonTileCount ? tally + (short)commonTileCount : tally, f -> new TLongArrayList()).add(l);
////						if (tally > 20) {
////							System.out.println("adding " + l + " to startPositions");
////						}
////						for (int k = 0 ; k < exactMatchComponent ; k++) {
////							allPositions.add(l + k);
////						}
//						startPositions.add(l);
//					}
//				}
//			}
//			
//			/*
//			 * Check to see if the highest key in the map is equal to the length of our array (minus 1 each time this iteration is performed)
//			 * if it is then we have a perfect match and we can exit
//			 * otherwise, keep looping....
//			 */
//			if (currentMaxCount == array.length - i ) {
//				break;
//			}
//		}
//		return map;
//	}
	
//	public static int continuousCount(long [][] array, long l, int arrayStartPosition) {
//		int tally = 0;
//		int arrayLength = array.length;
//		long [] nextArray = array[arrayStartPosition++];
//		int position = Arrays.binarySearch(nextArray, l + tally + 1);
//		
//		while (position > -1 || (nextArray.length == 1 && nextArray[0] < 0)) {
//			tally++;
//			if (arrayStartPosition >= arrayLength) {
//				break;
//			}
//			nextArray = array[arrayStartPosition++];
//			position = Arrays.binarySearch(nextArray, l + tally + 1);
//		}
//		
//		return tally;
//	}
	
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
					
					
					/*
					 * Lets try breaking out if we get a commonly occurring tile
					 */
					
//					break;
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
							if (null != nextArray && nextArray.length > 0 && nextArray[0] != -1) {
								position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 14);
								if (position > -1) {
									i += 12;
									tally += 13;
									mismatchTally++;
									continue;
								}
							}
						}
						
						
						
						/*
						 * no longer allow gaps - we want exact matches only, and then use splits mechanism to stitch together constituent chunks if necessary 
						 * 
						 * and so, if we don't have a match, or a commonly occurring tile, break out
						 */
						break;
						
						
//						int absPosition = Math.min(Math.abs(position) - 1, nextArray.length - 1);
//						if ((absPosition >= 0 && Math.abs((l + i + 1) - nextArray[absPosition]) < INDEL_GAP) 
//								|| (absPosition > 1 && Math.abs((l + i + 1) - nextArray[absPosition - 1]) < INDEL_GAP)) {
//							tally++;
//						}
					}
				}
			}
		}
		
		/*
		 * If the commonTallyCount is less than the tally, incorporate it into the tally
		 */
		if (tally > commonTileTally) {
			tally += commonTileTally;
//			commonTileTally = 0;
		}
		
		return NumberUtils.getTileCount(tally, mismatchTally);
	}
//	public static int nonContinuousCount(long [][] array, long l, int arrayStartPosition) {
//		int tally = 1;	// start at 1
//		int commonTileTally = 0;
//		int mismatchTally = 0;
//		int arrayLength = array.length;
//		
//		for (int i = 0 ; i < arrayLength - arrayStartPosition ; i++) {
//			long [] nextArray = array[arrayStartPosition + i];
//			if (null != nextArray) {
//				if (nextArray.length == 1 && nextArray[0] < 0) {
//					commonTileTally++;
//					
//					
//					/*
//					 * Lets try breaking out if we get a commonly occurring tile
//					 */
//					
////					break;
//				} else {
//					
//					/*
//					 * try and reduce the occurrences of binary searching
//					 */
//					int position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 1);
//					
//					if (position > -1) {
//						tally++;
//					} else {
//						
//						/*
//						 * no longer allow gaps - we want exact matches only, and then use splits mechanism to stitch together constituent chunks if necessary 
//						 * 
//						 * and so, if we don't have a match, or a commonly occurring tile, break out
//						 */
//						break;
//						
//						
////						int absPosition = Math.min(Math.abs(position) - 1, nextArray.length - 1);
////						if ((absPosition >= 0 && Math.abs((l + i + 1) - nextArray[absPosition]) < INDEL_GAP) 
////								|| (absPosition > 1 && Math.abs((l + i + 1) - nextArray[absPosition - 1]) < INDEL_GAP)) {
////							tally++;
////						}
//					}
//				}
//			}
//		}
//		
//		/*
//		 * If the commonTallyCount is less than the tally, incorporate it into the tally
//		 */
//		if (tally > commonTileTally) {
//			tally += commonTileTally;
//			commonTileTally = 0;
//		}
//		
//		return NumberUtils.getTileCount(tally, commonTileTally);
//	}
	
	
	
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
					System.out.println("hit " + (i / 1000000) + "M records, matches: " + matches);
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
			
			System.out.println("number of matches: " + matches);
				
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
	
	public static Map<String, BLATRecord> runTiledAligner(String tiledAlignerFile, String [] sequences) {
		return runTiledAligner(tiledAlignerFile, sequences, TILE_LENGTH);
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
						System.out.println("WARNING: ref.length() < positionInRef + lengthDiff, ref.length(): " + ref.length() + ", positionInRef: " + positionInRef + ", lengthDiff: " + lengthDiff + ", ref: " + ref + ", binSeq: " + binSeq);
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
					System.out.println("WARNING: refIndex = ref.indexOf(swRef) == -1!!!");
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
					System.out.println("WARNING: refIndex = ref.indexOf(swRef) == -1!!!");
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
		
		
//		int swScore =  getSWScore(diffs[1]);
//		if (rescueSequence && swScore < sequence.length() - 20) {
////			
//////			int swSeqLength = org.apache.commons.lang3.StringUtils.remove(diffs[2], Constants.MINUS).length();
//////			if (swSeqLength == sequence.length()) {
//////				/*
//////				 * Perfect Match!
//////				 */
//////			} else {
////				/*
////				 * try a more lenient sw calculation to see if we can get a better score
////				 */
//				nm = new SmithWatermanGotoh(ref, sequence, 4, -4, 4, 1);		// good for gaps
////				nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 1, 1);	// original
//////				nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 8, 2);
//				String [] lenientDiffs = nm.traceback();
////				
//				int swLenientScore =  getSWScore(lenientDiffs[1]);
//				System.out.println("swLenientScore: " + swLenientScore + ", swScore: " + swScore);
//				if (swLenientScore > swScore) {
//////					System.out.println("got a better score when using a more lenient sw setup!!!");
//////					for (String s : diffs) {
//////						System.out.println("old s: " + s);
//////					}
//////					System.out.println("sw score: " + getSWScore(diffs[1]));
//////					for (String s : lenientDiffs) {
//////						System.out.println("new s: " + s);
//////					}
//////					System.out.println("sw score: " + getSWScore(lenientDiffs[1]));
//					diffs = lenientDiffs;
//				}
//		}
		return diffs;
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
				System.out.println("found [" + c + "] in sw diffs string!!!");
			}
		}
//		int gapCount = getInsertionCount(diffs, ' ');
		return matchCount - mismatchCount - (1 * getInsertionCount(diffs, ' '));
//		return matchCount - mismatchCount - (2 * gapCount);
	}
	
	public static List<String []> getSmithWaterman(List<ChrPosition> tiledAlignerPositions, String sequence, String name) {
		return getSmithWaterman(tiledAlignerPositions, sequence, name, false);
	}
	public static List<String []> getSmithWaterman(List<ChrPosition> tiledAlignerPositions, String sequence, String name,  boolean debug) {
		List<String [] > swResults = new ArrayList<>();
		int bufferToUse = sequence.length() < 100 ? SMITH_WATERMAN_BUFFER_SIZE_TIER_1 : SMITH_WATERMAN_BUFFER_SIZE_TIER_2;
		int maxScore = sequence.length();
		int passingPercentScore = (int) (maxScore * 0.99);
		
		/*
		 * sort list of cps by contig(s) that appears in name
		 */
		boolean optimiseForGaps = false;
		if (null != name) {
			String [] nameArray = name.split("_");
			String contig = nameArray[0];
			if (contig.startsWith("splitcon")) {
				/*
				 * also want different chromosomes
				 */
//				if ( ! nameArray[1].equals(nameArray[3])) {
				optimiseForGaps = true;
					splitconCounter.incrementAndGet();
//				}
				contig = nameArray[1];
			}
			tiledAlignerPositions.sort(Comparator.comparing(ChrPosition::getChromosome, ChrPositionComparator.getChrNameComparatorForSingleString(contig)));
		}
		
		int perfectMatchCount = 0;
		
		for (ChrPosition cp : tiledAlignerPositions) {
			ChrPosition bufferedCP = new ChrRangePosition(cp.getChromosome(),  Math.max(1,cp.getStartPosition() - bufferToUse), cp.getStartPosition() + bufferToUse + sequence.length());
//			System.out.println("will SW: " + bufferedCP.toIGVString() + ", cp: " + cp.toIGVString());
			String bufferedReference = getRefFromChrPos(bufferedCP);
			
			boolean forwardStrand =  ((ChrPositionName) cp).getName().equals("F");
			
			/*
			 * if  fragment exists in buffered reference, job done, otherwise use SW
			 */
			String fragString = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
			int index = bufferedReference.indexOf(fragString);
			if (index > -1) {
				perfectMatchCount++;
				if (debug) {
					System.out.println("Got a perfect match using indexOf!!! " + cp.toIGVString());
				}
				/*
				 * create matching swdiffs array
				 */
				String[] swMatches = new String[3];
				swMatches[0] = fragString;
				swMatches[1] = org.apache.commons.lang3.StringUtils.repeat('|', fragString.length());
				swMatches[2] = fragString;
				String [] blatDetails = getDetailsForBLATRecord(bufferedCP, swMatches, name, sequence, forwardStrand, bufferedReference);
				swResults.add(blatDetails);
				/*
				 * If we have a perfect match, don't bother examining the other positions (should there be any)
				 */
//				break;
			} else {
			
				if (debug) {
					System.out.println("about to sw ref: " + bufferedReference + ", fragString: " + fragString + " for cp: " + cp.getChromosome() + ":" + cp.getStartPosition());
				}
				String [] swDiffs = getSwDiffs(bufferedReference, fragString, true, optimiseForGaps);
				int score = getSWScore(swDiffs[1]);
				
				if (debug) {
					if (score > 30) {
						for (String s : swDiffs) {
							System.out.println("swDiffs: " + s);
						}
						System.out.println("sw score: " + score + ", fragString length: " + fragString.length());
					}
				}
				
				String [] blatDetails = getDetailsForBLATRecord(bufferedCP, swDiffs, name, sequence, forwardStrand, bufferedReference);
				swResults.add(blatDetails);
				if (score == maxScore) {
					System.out.println("max score [" + maxScore + "] (seq length: " + sequence.length() + "), has been reached - exiting sw! " + Arrays.deepToString(blatDetails));
					break;
				} else if (score > passingPercentScore) {
					System.out.println("passing percentage score [" + passingPercentScore + "] (seq length: " + sequence.length() + "), has been reached - exiting sw! " + Arrays.deepToString(blatDetails));
					break;
				}
				
				
//				String swFragmentMinusDeletions = org.apache.commons.lang3.StringUtils.remove(swDiffs[2], Constants.MINUS);
//				
//				if (fragString.equals(swFragmentMinusDeletions)) {
//					/*
//					 * Fragment is wholly contained in swdiffs, and so can get actual position based on that
//					 */
//					System.out.println(" Fragment is wholly contained in swdiffs, and so can get actual position based on that");
//					String swRefMinusDeletions = org.apache.commons.lang3.StringUtils.remove(swDiffs[0], Constants.MINUS);
//					int offset = bufferedReference.indexOf(swRefMinusDeletions);
//					int refLength = swRefMinusDeletions.length();
////					setActualCP(bufferedCP, offset, f, refLength);
////					actualCPReadCount.addAndGet(f.getRecordCount());
//					
//				} else {
//					System.out.println("fragment length: " + sequence.length() + ", differs from swDiffs: " + swFragmentMinusDeletions.length());
//					System.out.println("frag: " + fragString);
//					System.out.println("swDiffs[2]: " + swDiffs[2]);
//					System.out.println("bufferedRef: " + bufferedReference);
////					noActualCPReadCount.addAndGet(f.getRecordCount());
//				}
			}
		}
		
		
		/*
		 * this is a little contentious - may need to be removed
		 */
		if (perfectMatchCount > 1) {
			return Collections.emptyList();
		}
		
		
		return swResults;
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
	 * @deprecated Please only use this method with a single ChrPosition in the list
	 * results are unintuitive if more than 1 is used....
	 * 
	 * @param positions
	 * @param name
	 * @param sequence
	 * @param forwardStrand
	 * @return
	 */
	public static String[] getDetailsForBLATRecord(List<ChrPosition> positions, String name,  String sequence, boolean forwardStrand) {
		System.out.println("getDetailsForBLATRecord with positions: " + positions.stream().map(ChrPosition::toString).collect(Collectors.joining(",")) + ", name: " + name + ", sequence: " + sequence + ", fs: " + forwardStrand);
		
//		for (ChrPosition cp : positions) {
//			System.out.println("in getDetailsForBLATRecord with cp: " + cp.toString());
//		}
		
		int totalGapSize = 0;
		int totalMatches = 0;
		int lastEnd = 0;
		int numberOfPositions = positions.size();
		List<ChrPosition> positionsToUse = positions;
		
		if (numberOfPositions > 1) {
			/*
			 * want the ChrPositions sorted
			 */
			positions.sort(null);
			
			positionsToUse = checkForOverlappingSequence(positions, sequence);
			numberOfPositions = positionsToUse.size();
		}
		for (ChrPosition cp : positionsToUse) {
			if (lastEnd > 0) {
				totalGapSize += cp.getStartPosition() - lastEnd;
			}
			totalMatches += cp.getLength() - 1;
			lastEnd = cp.getEndPosition();
		}
		
		String [] array = new String[21];
		array[0] = "" + totalMatches;		//number of matches
		array[1] = "0";		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";	// T gap count
		array[5] = "0";		// T gap bases
		array[6] = "" + (numberOfPositions - 1);		// Q gap count
		array[7] = "" + totalGapSize;				// Q gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequence.length() + "";			// Q size
		array[11] = "" + sequence.indexOf(positionsToUse.get(0).getName());					// Q start
		array[12] = "" + (sequence.indexOf(positionsToUse.get(numberOfPositions - 1).getName()) + positionsToUse.get(numberOfPositions - 1).getLength() - 1);	// Q end
		array[13] = positionsToUse.get(0).getChromosome();			// T name
		array[14] = "12345";								// T size
		int tStart = positionsToUse.get(0).getStartPosition();
//		int tStart = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW) + bufferredCP.getStartPosition();
		
//		if (bufferredCP.getChromosome().equals("chr20")) {
//			System.out.println("bufferedReference: " + bufferedReference);
//			System.out.println("swDiffs[0]: " + swDiffs[0]);
//			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
//			System.out.println("tStart: " + tStart);
//		}
		
		array[15] = "" + tStart;								// T start
		array[16] = "" + (positions.get(numberOfPositions - 1).getEndPosition());		// T end
		
		array[17] = "" + numberOfPositions;					// block count
		array[18] = positionsToUse.stream().map(b -> "" + (b.getLength() - 1)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = positionsToUse.stream().map(b -> "" + sequence.indexOf(b.getName())).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
		array[20] = positionsToUse.stream().map(b -> "" + b.getStartPosition()).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
		
		return array;
	}
	
	public static String[] getDetailsForBLATRecordNew(List<ChrPosition> positions, String name,  String sequence) {
		System.out.println("getDetailsForBLATRecord with positions: " + positions.stream().map(ChrPosition::toString).collect(Collectors.joining(",")) + ", name: " + name + ", sequence: " + sequence);
		
//		for (ChrPosition cp : positions) {
//			System.out.println("in getDetailsForBLATRecord with cp: " + cp.toString());
//		}
		
		int totalGapSize = 0;
		int totalMatches = 0;
		int lastEnd = 0;
		int numberOfPositions = positions.size();
		List<ChrPosition> positionsToUse = positions;
		
		/*
		 * check to see if we can find the reference sequence from the first position (which will be forward stranded) in the sequence
		 * If yes, then sequence is on forward strand, if not, sequence is on reverse strand and needs to be reverse complemented
		 */
		String firstCPReferenceSequence = positionsToUse.get(0).getName();
		String recCompSeq = null;
		boolean forwardStrand = sequence.contains(firstCPReferenceSequence);
		if ( ! forwardStrand) {
			/*
			 * check that it is indeed present if the sequence is reverse coplemented
			 */
			recCompSeq = SequenceUtil.reverseComplement(sequence);
			if ( ! recCompSeq.contains(firstCPReferenceSequence)) {
				System.out.println("WARNING, sequence and reference do not match! - can't create string array for blat object");
				return null;
			}
		}
		final String sequenceToUse = forwardStrand ? sequence : recCompSeq;
//		System.out.println("sequenceToUse: " + sequenceToUse);
		
		if (numberOfPositions > 1) {
			/*
			 * want the ChrPositions sorted
			 */
			positions.sort(null);
			
			positionsToUse = checkForOverlappingSequence(positions, sequenceToUse);
			numberOfPositions = positionsToUse.size();
		}
		for (ChrPosition cp : positionsToUse) {
			if (lastEnd > 0) {
				totalGapSize += cp.getStartPosition() - lastEnd;
			}
			totalMatches += cp.getLength() - 1;
			lastEnd = cp.getEndPosition();
		}
		
		
		ChrPosition firstPosition = positionsToUse.get(0);
		
		
		
		String [] array = new String[21];
		array[0] = "" + totalMatches;		//number of matches
		array[1] = "0";		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";	// T gap count
		array[5] = "0";		// T gap bases
		array[6] = "" + (numberOfPositions - 1);	// Q gap count
		array[7] = "" + totalGapSize;				// Q gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequenceToUse.length() + "";			// Q size
		array[11] = "" + sequenceToUse.indexOf(firstPosition.getName());					// Q start
		array[12] = "" + (sequenceToUse.indexOf(positionsToUse.get(numberOfPositions - 1).getName()) + positionsToUse.get(numberOfPositions - 1).getLength() - 1);	// Q end
		array[13] = firstPosition.getChromosome();			// T name
		array[14] = "12345";								// T size
		int tStart = firstPosition.getStartPosition();
//		int tStart = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW) + bufferredCP.getStartPosition();
		
//		if (bufferredCP.getChromosome().equals("chr20")) {
//			System.out.println("bufferedReference: " + bufferedReference);
//			System.out.println("swDiffs[0]: " + swDiffs[0]);
//			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
//			System.out.println("tStart: " + tStart);
//		}
		
		array[15] = "" + tStart;								// T start
		array[16] = "" + (positions.get(numberOfPositions - 1).getEndPosition());		// T end
		
		array[17] = "" + numberOfPositions;					// block count
		array[18] = positionsToUse.stream().map(b -> "" + (b.getLength() - 1)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = positionsToUse.stream().map(b -> "" + sequenceToUse.indexOf(b.getName())).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
		array[20] = positionsToUse.stream().map(b -> "" + b.getStartPosition()).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
		
		return array;
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
//					if (diff < 0) {
						System.out.println("in checkForOverlappingSequence, diff: " + diff + ", previousEnd: " + previousEnd + ", startIndex: " + startIndex + ", cpSeq: " + cpSeq + ", sequence: " + sequence);
//					}
					updatedCP = new ChrPositionName(cp.getChromosome(), cp.getStartPosition() + diff, cp.getEndPosition(), cpSeq.substring(diff));
				}
			}
			updatedList.add(updatedCP);
		}
		return updatedList;
	}
//	public static String[] getDetailsForBLATRecord(ChrPosition bufferredCP, String [] swDiffs, String name, String sequence, boolean forwardStrand, String bufferedReference) {
//		
//		String refFromSW = swDiffs[0].replaceAll("-", "");
//		List<Range<Integer>> blocks = NumberUtils.getBlockStartPositions(swDiffs[2], forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence));
//		List<int[]> blocksArray = NumberUtils.getBlockStartPositionsNew(swDiffs[2], forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence));
//		List<int[]> blocksArrayReference = NumberUtils.getBlockStartPositionsNew(swDiffs[2], refFromSW);
//		List<int[]> allStartPositions = NumberUtils.getAllStartPositions(swDiffs);
//		int [] queryBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[2], '-');
//		int [] targetBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[0], '-');
//		int [] queryStartPositions = NumberUtils.getActualStartPositions(allStartPositions, true, swDiffs[2].replaceAll("-", ""), forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence), 0);
//		int [] targetStartPositions = NumberUtils.getActualStartPositions(allStartPositions, false, refFromSW, bufferedReference, bufferredCP.getStartPosition());
//		
//		int nCount =  StringUtils.getCount(swDiffs[2], 'N');
//		int misMatchCount =  StringUtils.getCount(swDiffs[1], '.');
//		if (nCount > 0 && misMatchCount > 0) {
//			misMatchCount -= nCount;
//		}
//		String [] array = new String[21];
//		array[0] = "" + StringUtils.getCount(swDiffs[1], '|');		//number of matches
//		array[1] = "" + misMatchCount;					//number of mis-matches
//		array[2] = "0";									//number of rep. matches
//		array[3] = "" + nCount;							//number of N's
//		array[4] = "" + queryBlockCountAndCounts[0];	// Q gap count
////		array[4] = "" + (blocksArray.size() - 1);	// Q gap count
////		array[4] = "" + getInsertionCount(swDiffs[0]);	// Q gap count
//		array[5] = "" + queryBlockCountAndCounts[1];		// Q gap bases
//		array[6] = "" + targetBlockCountAndCounts[0];			// T gap count
//		array[7] = "" + targetBlockCountAndCounts[1];		// T gap bases
//		array[8] = forwardStrand ? "+" : "-";			// strand
//		array[9] = name;								// Q name
//		array[10] = sequence.length() + "";				// Q size
//		
//		/*
//		 * start and end are strand dependent
//		 * if we are on the forward, its the beginning of the first bloack, and end of the last
//		 * if we are on reverse, need to reverse!
//		 */
//		int start = forwardStrand ?  blocks.get(0).getMinimum().intValue() : (sequence.length() - blocks.get(blocks.size() - 1).getMaximum().intValue());
//		int end = forwardStrand ?  blocks.get(blocks.size() - 1).getMaximum().intValue() : (sequence.length() - blocks.get(0).getMinimum().intValue());
//		
//		array[11] = "" + start;							// Q start
//		array[12] = "" + end;	// Q end
//		array[13] = bufferredCP.getChromosome();			// T name
//		array[14] = "12345";								// T size
//		int indexOfRefInBufferedRef = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW);
//		int tStart = indexOfRefInBufferedRef + bufferredCP.getStartPosition();
//		
////		if (bufferredCP.getChromosome().equals("chr20")) {
////			System.out.println("bufferedReference: " + bufferedReference);
////			System.out.println("swDiffs[0]: " + swDiffs[0]);
////			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
////			System.out.println("tStart: " + tStart);
////		}
//		
//		array[15] = "" + tStart;								// T start
//		array[16] = "" + (refFromSW.length() + tStart);			// T end
//		
//		array[17] = "" + allStartPositions.size();					// block count
//		array[18] = allStartPositions.stream().map(b -> "" + (b[1])).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
//		array[19] = NumberUtils.getArrayAsCommaSeperatedString(queryStartPositions);					// Q block starts
//		array[20] = NumberUtils.getArrayAsCommaSeperatedString(targetStartPositions);;			// T block starts
////		array[17] = "" + allStartPositions.size();					// block count
////		array[18] = allStartPositions.stream().map(b -> "" + (b[1])).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
////		array[19] = allStartPositions.stream().map(b -> "" + b[0]).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
////		array[20] = allStartPositions.stream().map(b -> "" + (b[2] + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
////		array[17] = "" + blocks.size();					// block count
////		array[18] = blocks.stream().map(b -> "" + (b.getMaximum() - b.getMinimum())).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
////		array[19] = blocks.stream().map(b -> "" + b.getMinimum()).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
////		array[20] = blocks.stream().map(b -> "" + (b.getMinimum() + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
//		
//		return array;
//	}
	public static String[] getDetailsForBLATRecord(ChrPosition bufferredCP, String [] swDiffs, String name, String sequence, boolean forwardStrand, String bufferedReference) {
		
		String refFromSW = swDiffs[0].replaceAll("-", "");
		String seqFromSW = swDiffs[2].replaceAll("-", "");
		String sequenceToUse = forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence);
//		List<Range<Integer>> blocks = NumberUtils.getBlockStartPositions(swDiffs[2], sequenceToUse);
		List<int[]> allStartPositions = NumberUtils.getAllStartPositions(swDiffs);
		int [] queryBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[2], '-');
		int [] targetBlockCountAndCounts = NumberUtils.getBlockCountAndCount(swDiffs[0], '-');
		
		int seqOffset = sequenceToUse.indexOf(seqFromSW);
		
		
//		int [] queryStartPositions = NumberUtils.getActualStartPositions(allStartPositions, true, swDiffs[2].replaceAll("-", ""), forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence), 0);
//		int [] targetStartPositions = NumberUtils.getActualStartPositions(allStartPositions, false, refFromSW, bufferedReference, bufferredCP.getStartPosition());
		int nCount =  StringUtils.getCount(swDiffs[2], 'N');
		int misMatchCount =  StringUtils.getCount(swDiffs[1], '.');
		if (nCount > 0 && misMatchCount > 0) {
			misMatchCount -= nCount;
		}
		String [] array = new String[21];
		array[0] = "" + StringUtils.getCount(swDiffs[1], '|');		//number of matches
		array[1] = "" + misMatchCount;					//number of mis-matches
		array[2] = "0";									//number of rep. matches
		array[3] = "" + nCount;							//number of N's
		array[4] = "" + queryBlockCountAndCounts[0];	// Q gap count
		array[5] = "" + queryBlockCountAndCounts[1];		// Q gap bases
		array[6] = "" + targetBlockCountAndCounts[0];			// T gap count
		array[7] = "" + targetBlockCountAndCounts[1];		// T gap bases
		array[8] = forwardStrand ? "+" : "-";			// strand
		array[9] = name;								// Q name
		array[10] = sequence.length() + "";				// Q size
		
		/*
		 * start and end are strand dependent
		 * if we are on the forward, its the beginning of the first bloack, and end of the last
		 * if we are on reverse, need to reverse!
		 */
		int start =  forwardStrand ?  seqOffset + allStartPositions.get(0)[0] : (sequence.length() - (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]));
		int end =  forwardStrand ?  (seqOffset + allStartPositions.get(allStartPositions.size() - 1)[0] + allStartPositions.get(allStartPositions.size() - 1)[1]) : (sequence.length() - (seqOffset + allStartPositions.get(0)[0]));
//		int start = forwardStrand ?  blocks.get(0).getMinimum().intValue() : (sequence.length() - blocks.get(blocks.size() - 1).getMaximum().intValue());
//		int end = forwardStrand ?  blocks.get(blocks.size() - 1).getMaximum().intValue() : (sequence.length() - blocks.get(0).getMinimum().intValue());
		
		array[11] = "" + start;							// Q start
		array[12] = "" + end;	// Q end
		array[13] = bufferredCP.getChromosome();			// T name
		array[14] = "12345";								// T size
		int indexOfRefInBufferedRef = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW);
		int tStart = indexOfRefInBufferedRef + bufferredCP.getStartPosition();
		
		array[15] = "" + tStart;								// T start
		array[16] = "" + (refFromSW.length() + tStart);			// T end
		
		array[17] = "" + allStartPositions.size();					// block count
		array[18] = allStartPositions.stream().map(b -> "" + (b[1])).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
		array[19] = allStartPositions.stream().map(b -> "" + (b[0] + seqOffset)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes					// Q block starts
		array[20] = allStartPositions.stream().map(b -> "" + (b[2] + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes			// T block starts
//		array[19] = NumberUtils.getArrayAsCommaSeperatedString(queryStartPositions);					// Q block starts
//		array[20] = NumberUtils.getArrayAsCommaSeperatedString(targetStartPositions);;			// T block starts
		
		return array;
	}
	
//	public static String[] getDetailsForBLATRecord(List<ChrPosition> compoundStartPositions, String name, String sequence, boolean forwardStrand ) {
////		public static String[] getDetailsForBLATRecord(TLongIntMap compoundStartPositions, String name, String sequence, int tileLength) {
//		
//		
//		int matches = 0;
//		for (ChrPosition cp : compoundStartPositions) {
//			matches += cp.getLength();
//		}
//		compoundStartPositions.sort(new ChrPositionComparator());
//		
//		String [] array = new String[21];
//		array[0] = "" + matches;						//number of matches
//		array[1] = "0";									//number of mis-matches
//		array[2] = "0";									//number of rep. matches
//		array[3] = "0";									//number of N's
//		array[4] = "" + getInsertionCount(swDiffs[0]);	// T gap count
//		array[5] = "" + StringUtils.getCount(swDiffs[0], '-');		// T gap bases
//		array[6] = "" + (startPOsitions.length - 1);			// Q gap count
//		array[7] = "" + StringUtils.getCount(swDiffs[2], '-');		// Q gap bases
//		array[8] = forwardStrand ? "+" : "-";			// strand
//		array[9] = name;								// Q name
//		array[10] = sequence.length() + "";				// Q size
//		array[11] = "" + compoundStartPositions.get(0).getStartPosition();								// Q start
//		array[12] = "" + compoundStartPositions.get(compoundStartPositions.size() - 1).getEndPosition();	// Q end
//		array[13] = compoundStartPositions.get(0).getChromosome(); 		// T name
//		array[14] = "12345";								// T size
//		int tStart = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW) + bufferredCP.getStartPosition();
//		
////		if (bufferredCP.getChromosome().equals("chr20")) {
////			System.out.println("bufferedReference: " + bufferedReference);
////			System.out.println("swDiffs[0]: " + swDiffs[0]);
////			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
////			System.out.println("tStart: " + tStart);
////		}
//		
//		array[15] = "" + compoundStartPositions.get(0).getStartPosition();						// T start
//		array[16] = "" + (refFromSW.length() + tStart);			// T end
//		
//		array[17] = "" + compoundStartPositions.size();					// block count
//		array[18] = compoundStartPositions.stream().map(b -> "" + b.getLength()).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
//		array[19] = blocksArray.stream().map(b -> "" + b[0]).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
//		array[20] = blocksArray.stream().map(b -> "" + (b[0] + b[2] + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
////		array[17] = "" + blocks.size();					// block count
////		array[18] = blocks.stream().map(b -> "" + (b.getMaximum() - b.getMinimum())).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
////		array[19] = blocks.stream().map(b -> "" + b.getMinimum()).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
////		array[20] = blocks.stream().map(b -> "" + (b.getMinimum() + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
//		
//		return array;
//	}
//	public static String[] getDetailsForBLATRecord(ChrPosition bufferredCP, String [] swDiffs, String name, String sequence, boolean forwardStrand, String bufferedReference) {
//		
//		String refFromSW = swDiffs[0].replaceAll("-", "");
//		List<Range<Integer>> blocks = getBlockStartPositions(swDiffs[2], forwardStrand ? sequence : SequenceUtil.reverseComplement(sequence));
//		
//		int nCount =  StringUtils.getCount(swDiffs[2], 'N');
//		int misMatchCount =  StringUtils.getCount(swDiffs[1], '.');
//		if (nCount > 0 && misMatchCount > 0) {
//			misMatchCount -= nCount;
//		}
//		String [] array = new String[21];
//		array[0] = "" + StringUtils.getCount(swDiffs[1], '|');		//number of matches
//		array[1] = "" + misMatchCount;					//number of mis-matches
//		array[2] = "0";									//number of rep. matches
//		array[3] = "" + nCount;							//number of N's
//		array[4] = "" + getInsertionCount(swDiffs[0]);	// T gap count
//		array[5] = "" + StringUtils.getCount(swDiffs[0], '-');		// T gap bases
//		array[6] = "" + (blocks.size() - 1);			// Q gap count
//		array[7] = "" + StringUtils.getCount(swDiffs[2], '-');		// Q gap bases
//		array[8] = forwardStrand ? "+" : "-";			// strand
//		array[9] = name;								// Q name
//		array[10] = sequence.length() + "";				// Q size
//		array[11] = "" + blocks.get(0).getMinimum().intValue();							// Q start
//		array[12] = "" + blocks.get(blocks.size() - 1).getMaximum().intValue();	// Q end
//		array[13] = bufferredCP.getChromosome();			// T name
//		array[14] = "12345";								// T size
//		int tStart = StringUtils.indexOfSubStringInString(bufferedReference, refFromSW) + bufferredCP.getStartPosition();
//		
////		if (bufferredCP.getChromosome().equals("chr20")) {
////			System.out.println("bufferedReference: " + bufferedReference);
////			System.out.println("swDiffs[0]: " + swDiffs[0]);
////			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
////			System.out.println("tStart: " + tStart);
////		}
//		
//		array[15] = "" + tStart;								// T start
//		array[16] = "" + (refFromSW.length() + tStart);			// T end
//		
//		array[17] = "" + blocks.size();					// block count
//		array[18] = blocks.stream().map(b -> "" + (b.getMaximum() - b.getMinimum())).collect(Collectors.joining(Constants.COMMA_STRING));	// block sizes
//		array[19] = blocks.stream().map(b -> "" + b.getMinimum()).collect(Collectors.joining(Constants.COMMA_STRING));						// Q block starts
//		array[20] = blocks.stream().map(b -> "" + (b.getMinimum() + tStart)).collect(Collectors.joining(Constants.COMMA_STRING));			// T block starts
//		
//		return array;
//	}
	
	public static String[] getDetailsForBLATRecordPerfectMatch(ChrPosition cp, String name, String sequence, boolean forwardStrand) {
		
		int sequenceLength = sequence.length();
		String [] array = new String[21];
		array[0] = "" + sequenceLength; 		//number of matches
		array[1] = "0" ;		//number of mis-matches
		array[2] = "0";		//number of rep. matches
		array[3] = "0";		//number of N's
		array[4] = "0";		// Q gap count
		array[5] = "0";		// Q gap bases
		array[6] = "0";		// T gap count
		array[7] = "0";		// T gap bases
		array[8] = forwardStrand ? "+" : "-";		// strand
		array[9] = name;							// Q name
		array[10] = sequenceLength + "";			// Q size
		array[11] = "0";							// Q start
		array[12] = "" + (sequenceLength - 1);		// Q end
		array[13] = cp.getChromosome();				// T name
		array[14] = "12345";						// T size
		int tStart = cp.getStartPosition();
		
//		if (bufferredCP.getChromosome().equals("chr20")) {
//			System.out.println("bufferedReference: " + bufferedReference);
//			System.out.println("swDiffs[0]: " + swDiffs[0]);
//			System.out.println("bufferredCP.getStartPosition(): " + bufferredCP.getStartPosition());
//			System.out.println("tStart: " + tStart);
//		}
		
		array[15] = "" + tStart;					// T start
		array[16] = "" + (sequenceLength + tStart);	// T end
		
		array[17] = "1";							// block count
		array[18] = "" + sequenceLength;			// block sizes
		array[19] = "0";							// Q block starts
		array[20] = "" + cp.getStartPosition();		// T block starts
		return array;
	}
	
	public static String getRefFromChrPos(ChrPosition cp) {
		return getRefFromChrStartStop(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition());
	}
	
	public static String getRefFromChrStartStop(String contig, int start, int stop) {
		
		byte[] ref = referenceCache.get(contig);
		if ( null == ref) {
			loadIntoReferenceCache(contig);
			ref = referenceCache.get(contig);
			if (null == ref) {
				//hmmm....
				System.err.println("Unable to load contig: " + contig + " into cache");
				System.out.println("Unable to load contig: " + contig + " into cache");
			}
		}
		if (start <= 0 || stop > ref.length) {
			System.out.println("ChrPosition goes beyond edge of contig: " + contig + ":" + start + "-" + stop + ", ref length: " + ref.length);
		}
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(contig), start, (stop >= ref.length ? ref.length - 1 : stop));
		String referenceSeq = new String(refPortion);
		
		return referenceSeq;
	}
	
	public static void loadIntoReferenceCache(String contig) {
		String refFileName = "/reference/genomes/GRCh37_ICGC_standard_v2/indexes/BWAKIT_0.7.12/GRCh37_ICGC_standard_v2.fa";
		
		FastaSequenceIndex index = new FastaSequenceIndex(new File(refFileName + ".fai"));
		try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(refFileName), index);) {
			if (null == dictionary) {
				dictionary = refFile.getSequenceDictionary();
			}
			ReferenceSequence refSeq = refFile.getSequence(contig);
			byte[] ref = refSeq.getBases();
			referenceCache.put(contig, ref);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<String, ChrPosition> getDirectMatch(String [] seq) {
		// loop through all contigs looking for a match
		if (null == dictionary) {
			/*
			 * this should trigger the loading of the dictionary
			 */
			loadIntoReferenceCache("chr1");
		}
		
		Map<String, ChrPosition> matches = new HashMap<>();
		int counter = 0;
		int sequencesWithNs = 0;
		for (String s : seq) {
			System.out.println("will look for " + s);
		}
		
		List<SAMSequenceRecord> sortedSSRs = new ArrayList<>(dictionary.getSequences());
		sortedSSRs.sort((SAMSequenceRecord ssr1, SAMSequenceRecord ssr2) -> ssr1.getSequenceLength() - ssr2.getSequenceLength());

		for (SAMSequenceRecord ssr : sortedSSRs) {
			int matchCount = 0;
			int noMatchCount = 0;
			
			
			if (matches.size() == seq.length) {
				/*
				 * all found - exit
				 */
				break;
			}
			System.out.println("looking in contig " + ssr.getSequenceName());
			byte[] ref = referenceCache.get(ssr.getSequenceName());
			if ( null == ref) {
				loadIntoReferenceCache(ssr.getSequenceName());
				ref = referenceCache.get(ssr.getSequenceName());
				if (null == ref) {
					//hmmm....
					System.err.println("Unable to load contig: " + ssr.getSequenceName() + " into cache");
				}
			}
				
			String sRef = new String(ref);
			for (String s : seq) {
				if ( ! StringUtils.isNullOrEmpty(s)) {
						
					/*
					 * check that we haven't already matched this
					 */
					String rc = SequenceUtil.reverseComplement(s);
					if ( ! matches.containsKey(s) && ! matches.containsKey(rc)) {
				
						int index = sRef.indexOf(s);
						if (index > -1) {
							// we have a match!
							ChrPosition cp = new ChrRangePosition(ssr.getSequenceName(), index, index + s.length());
							System.out.println("We have a match in contig " + cp.toIGVString() + ", s: " + s);
							if (matches.containsKey(s)) {
								System.out.println("already have a match for s: " + s);
							}
							matches.put(s, cp);
							matchCount++;
						} else {
							/*
							 * try the rc
							 */
							index = sRef.indexOf(rc);
							if (index > -1) {
								// we have a match!
								ChrPosition cp = new ChrRangePosition(ssr.getSequenceName(), index, index + rc.length());
								System.out.println("We have a (rc) match in contig " + cp.toIGVString() + ", rc: " + rc);
								if (matches.containsKey(rc)) {
									System.out.println("already have a match for rc: " + rc);
								}
								matches.put(rc, cp);
								matchCount++;
							} else {
								noMatchCount++;
//									System.out.println("no match in " + ssr.getSequenceName() + ", matches: " + matches.size() + " from " + counter + ", sequencesWithNs: " + sequencesWithNs);
							}
						}
						counter++;
					}
				}
			}
			
			System.out.println("Searched " + ssr.getSequenceName() + ", found " + matchCount + " matches, and " + noMatchCount + " no matches");
		}
		return matches;
	}
	
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(String tiledAlignerFile, String [] sequences, int tileLength, int bufferSize, String originatingMethod) throws IOException {
		return runTiledAlignerCache(tiledAlignerFile, sequences, tileLength, bufferSize, null, originatingMethod);
	}
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(String tiledAlignerFile, String [] sequences, int tileLength, int bufferSize, String outputFile, String originatingMethod) throws IOException {
		/*
		 * populate cache
		 */
		TIntObjectMap<int[]> cache = TiledAlignerLongMap.getCache(tiledAlignerFile, bufferSize);
		return runTiledAlignerCache(cache, sequences, tileLength, originatingMethod);
	}
//	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, String sequence, int tileLength, String originatingMethod, boolean log) throws IOException {
//		return runTiledAlignerCache( cache, new String [] {sequence}, tileLength, originatingMethod, log);
//	}
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, String [] sequences, int tileLength, String originatingMethod) throws IOException {
		return runTiledAlignerCache(cache, sequences, tileLength, originatingMethod, false);
	}
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, String [] sequences, int tileLength, String originatingMethod, boolean debug) throws IOException {
		Map<String, String> sequencesNameMap = new HashMap<>(sequences.length * 2);
		for (String s : sequences) {
			sequencesNameMap.put(s,  "nameSuppliedBy_runTiledAlignerCache");
			if (debug) {
				System.out.println("added " + s + " to map");
			}
		}
		return runTiledAlignerCache( cache, sequencesNameMap,  tileLength,  originatingMethod, debug);
	}
//	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod) throws IOException {
//		return runTiledAlignerCache(cache, sequencesNameMap, tileLength,  originatingMethod, false);
//	}
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod, boolean log) throws IOException {
		return runTiledAlignerCache(cache,  sequencesNameMap, tileLength, originatingMethod,  log, false);
	}
	public static Map<String, List<BLATRecord>> runTiledAlignerCache(TIntObjectMap<int[]> cache, Map<String, String> sequencesNameMap, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) throws IOException {
		Map<String, List<BLATRecord>> results = new HashMap<>();
		
		for (Entry<String, String> entry : sequencesNameMap.entrySet()) {
			if (log) {
				System.out.println("about to call getBlatRecords for " + entry.getKey());
			}
			String name = entry.getValue();
			if (null != name && name.contains(":")) {
				System.out.println("got more than 1 name for sequence. names: " + name + ", will use first one to dictate sw strategy");
				String [] names = entry.getValue().split(":");
				name = names[0];
			}
			System.out.println("in runTiledAlignerCache, name: " + name + ", seq: " + entry.getKey());
			
			List<BLATRecord> blatties = getBlatRecords(cache, entry.getKey(), entry.getValue(), tileLength, originatingMethod, log, recordsMustComeFromChrInName);
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
	
	/**
	 * Looking to see if the sequence is full of single base repeats (ie. AAAAA)
	 * If there are single base repeat regions of greater then 23 bases in length, we examine them to see if the length of the single base repeat is greater than 25% of the sequence length.
	 * If it is, false is returned, true otherwise
	 * 
	 * 
	 * @param sequence
	 * @return
	 */
	public static boolean doesSequenceHaveMostlySibgleBaseRepeats(String sequence) {
		
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
	
	public static List<BLATRecord> getBlatRecords(TIntObjectMap<int[]> cache, String sequence, final String name, int tileLength, String originatingMethod, boolean log, boolean recordsMustComeFromChrInName) {
		if (null == cache || cache.isEmpty()) {
			throw new IllegalArgumentException("Null or empty cache passed to getBlatRecords");
		}
		if (null == sequence || sequence.isEmpty()) {
			throw new IllegalArgumentException("Null or empty sequence passed to getBlatRecords");
		}
		if (sequence.length() <= tileLength) {
			throw new IllegalArgumentException("sequence length is less than or equals to the tile length! sequence: " + sequence + ", tile length: " + tileLength);
		}
		
		if ( ! doesSequenceHaveMostlySibgleBaseRepeats(sequence)) {
			System.out.println("too much repetition in sequence to proceed: " + sequence);
			return Collections.emptyList();
		}
		
		sequenceOriginatingMethodMap.computeIfAbsent(sequence, f -> new ArrayList<>()).add(originatingMethod);
		
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
			
			System.out.println("we have " + ranges.size() + " ranges for this name: " + name + ", updated map size: " + updatedMap.size() + ", going with map of size: " + map1.size());
		}
		
		if (log) {
			List<Integer> keys = new ArrayList<>(map1.keySet());
			keys.sort(null);
			for (int i = keys.size() - 1 ; i >= 0 ; i--) {
				TLongList value = map1.get(keys.get(i));
				System.out.println("match count: "  + Arrays.toString(NumberUtils.splitIntInto2(keys.get(i))) + ", number of starts: " + value.size());
				if (value.size() < 100) {
					value.forEach((p) -> {System.out.println("p: " + p); return true;});
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
			System.out.println("Perfect match? " + (null != perfectMatches && ! perfectMatches.isEmpty()));
		}
		if (null != perfectMatches && ! perfectMatches.isEmpty()) {
			int maxNumberOfPerfectMatches = 5;
			if (sequence.indexOf('N') > -1) {
				if (log) {
					System.out.println("Can't find perfect match - sequence has an N! seq: " + sequence);
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
							String ref =  getRefFromChrStartStop(cp.getChromosome(), cp.getStartPosition() - buffer, cp.getStartPosition() + sequence.length() + buffer);
							Optional<ChrPosition> optionalCP = getChrPositionWithReference(cp.getChromosome(), cp.getStartPosition(), forwardStrand ? sequence : revCompSequence, ref);
							if (optionalCP.isPresent()) {
								System.out.println("got a perfect match!  cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
								needToRunSW = false;
								results.add(new BLATRecord(getDetailsForBLATRecord(Arrays.asList(optionalCP.get()), name, forwardStrand ? sequence : revCompSequence, forwardStrand)));
								/*
								 * If we have reached the max number of perfect matches, bail
								 */
								if (results.size() == maxNumberOfPerfectMatches) {
									break;
								}
							} else {
								System.out.println("got perfect match on tiles, but not on sequence! cp: " + cp.toIGVString() + ", ref: " + ref + ", sequence: " + sequence);
							}
						}
					}
				}
			}
		} else {
			
			/*
			 * see if we have any compound records (these are records where part of the sequence maps somewhere, and another part maps somewhere else - too far away for SW to put it down (our window is currently 20000)
			 * These will be returned AS WELL AS the SW positions
			 */
//			TLongIntMap compoundStartPositions = rec.getCompoundStartPositions(20000);
//			System.out.println("Number of entries in counpoundStartPositions: " + compoundStartPositions.size());
////			boolean foundPerfectMatchCompoundPosition = false;
//			if ( ! compoundStartPositions.isEmpty()) {
//				List<List<ChrPosition>> groupedCompoundPositions = getListOfCompoundRecordPositionsFromMap(compoundStartPositions, headerMap, sequence);
//				System.out.println("Number of entries in groupedCompoundPositions: " + groupedCompoundPositions.size());
//				for (List<ChrPosition> cps :  groupedCompoundPositions) {
//					long startPos = headerMap.getLongStartPositionFromChrPosition(cps.get(0)) + 1;
////						System.out.println("startPos from ChrPos: " + startPos + ", map positions: " + Arrays.toString(compoundStartPositions.keys()));
//					boolean forwardStrand = compoundStartPositions.containsKey(startPos);
//					BLATRecord br = new BLATRecord(getDetailsForBLATRecord(cps, "name", forwardStrand ? sequence : revCompSequence, forwardStrand));
//					if (br.getMatchCount() == sequence.length()) {
//						needToRunSW = false;
//					}
//					results.add(br);
//				}
//			}
		}
		if (needToRunSW) {
			
			
			
			
			/*
			 * check to see if we have compound start positions
			 * set window to be 10000 for now
			 */
//			TLongIntMap compoundStartPositions = taRec.getBestCompoundStartPositions(10000, 100);
////			System.out.println("compoundStartPositions: " + compoundStartPositions.)
//			if (compoundStartPositions.size() > 0) {
//				
//				if (log) {
//					System.out.println("compoundStartPositions size: " + compoundStartPositions.size());
//					compoundStartPositions.forEachEntry((l,i) -> {
//						System.out.println("compoundStartPositions l: " + l + ", i: " + i);
//						return true;
//					});
//				}
//				
//				List<ChrPosition> cpCountList = new ArrayList<>();
//				compoundStartPositions.forEachKey(key -> {
//					ChrPosition cp = headerMap.getChrPositionFromLongPosition(key, compoundStartPositions.get(key) + TILE_LENGTH - 1);		// add in the tile length uses
//					cpCountList.add(cp);
//					return true;
//				});
//				
//				if (log) {
//					System.out.println("cpCountList size: " + cpCountList.size());
//					
//					for (ChrPosition cp : cpCountList) {
//						System.out.println("cpCountList cp: " + ((ChrPositionName)cp).toString());
//					}
//				}
//				
//				/*
//				 * now create a list of ChePositions with the associated reference as the name
//				 */
//				boolean forwardStrand = false;
//				List<ChrPosition> cpCountListWithRef = new ArrayList<>();
//				for (ChrPosition cp : cpCountList) {
//					String ref =  getRefFromChrStartStop(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition());
//					forwardStrand = "F".equals(((ChrPositionName)cp).getName());
//					if (log) {
//						System.out.println("cpCountList cp: " + ((ChrPositionName)cp).toString() + ", ref: " + ref + ",fs: " + forwardStrand);
//					}
//					Optional<ChrPosition> optionalCP = getChrPositionWithReference(cp.getChromosome(), cp.getStartPosition(), forwardStrand ? sequence : revCompSequence, ref);
//					if (optionalCP.isPresent()) {
//						cpCountListWithRef.add(optionalCP.get());
//						if (log) {
//							System.out.println("cpCountList optional cp exists: " + ((ChrPositionName)optionalCP.get()).toString());
//						}
//					}
//				}
//				
//				if (cpCountListWithRef.size() > 0) {
//					BLATRecord br = new BLATRecord(getDetailsForBLATRecordNew(cpCountListWithRef, name, sequence));
//					System.out.println("adding br: " + br.toString());
//					results.add(br);
//				} else {
//					System.out.println("couldn't get enough cps for: " + compoundStartPositions.forEachEntry((l,i) -> {System.out.println("l: " + l + ", i: " + i);return true;}));
//				}
//				
//			}
			
			
			
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
				System.out.println("Found no tile matches for sequence: " + sequence);
				return Collections.emptyList();
			}
			int currentMaxLength = TARecordUtil.getExactMatchOnlyLengthFromPackedInt(taRec.getHightestTileCount());
			int commonTileCount = NumberUtils.getPartOfPackedInt(taRec.getHightestTileCount(), false);
			int nCount = org.apache.commons.lang3.StringUtils.countMatches(sequence, 'N');
			
//			int maxTileMatch = TARecordUtil.getExactMatchOnlyLengthFromPackedInt(taRec.getHightestTileCount());
			
			if (currentMaxLength < (0.7 * sequence.length()) && (currentMaxLength) >= (0.5 * sequence.length())) {
				System.out.println("name: " + name + ", currentMaxLength: " + currentMaxLength + ", sequence.length(): " + sequence.length() + ", less then 70% of bases covered ( && >= 50%) - will look for splits!");
				runSplits = true;
			} else {
				if ( ! runSplits) {
					System.out.println("name: " + name + ", currentMaxLength: " + currentMaxLength + ", sequence.length(): " + sequence.length() + ", will not run splits");
				}
			}
			
			if (commonTileCount > (0.1 * sequence.length())) {
				if (runSplits) {
					System.out.println("Too many commonly occurring tiles to be able to run splts, commonTileCount: " + commonTileCount + ", sequence.length(): " + sequence.length() + ", maxTileMatch: " + currentMaxLength);
				}
				runSplits = false;
			}
			
			/*
			 * randomly chosen value for nCount cutoff
			 */
			if (nCount > 5) {
				if (runSplits) {
					System.out.println("Too many N's in sequence - won't run splits! nCount: " + nCount);
				}
				runSplits = false;
			}
			
			
			
			if (runSplits) {
				
				System.out.println("about to run some splits, no of positions in TARecord:  name: " + name + ", " + taRec.getCountDist() + "");
				TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(taRec);
				List<IntLongPairs> potentialSplits = new ArrayList<>();
				splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
				/*
				 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
				 */
				int passingScore = (int)(0.9 * sequence.length());
				results.addAll(potentialSplits.stream()
						.filter(ilp -> IntLongPairsUtils.isIntLongPairsAValidSingleRecord(ilp))
						.map(ilp ->  TARecordUtil.blatRecordFromSplits(ilp, name, sequence.length(), headerMap, TILE_LENGTH))
						.filter(sa -> sa.length > 0)
						.map(s -> new BLATRecord(s))
//						.filter(br -> br.getScore() > passingScore)
						.collect(Collectors.toList()));
				
				System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size() + ", # of records in results after examining potentialSplits: " + results.size());
				
				/*
				 * check to see if we have a record that meets the passing score
				 * if so, set getSplits to true, which means no need to smith waterman
				 */
				
				gotSplits = results.stream().anyMatch(br -> br.getScore() > passingScore);
				
//				if (results.size() > 0) {
//					gotSplits = true;
//				}
				
//				if (splits.size() == 1 && splits.get(splits.keys()[0]).size() == 1) {
//					System.out.println("Have a sinlge splits - will examine to see if it is suitable for single record  name: " + name + ", " + taRec.getCountDist() + "");
//					String [] singleRecordSplits = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(splits.get(splits.keys()[0]).iterator().next(), name,  sequence.length(), headerMap, TILE_LENGTH);
//					if (null != singleRecordSplits && singleRecordSplits.length > 0) {
//						BLATRecord br = new BLATRecord(singleRecordSplits);
//						if (br.getScore() > (0.9 * sequence.length())) {
//							results.add(br);
//							gotSplits = true;
//						} else {
//							System.out.println("Single blat record from splits not good enough!!: " + br.toString() + ", sequence.length(): " + sequence.length() + ", seq: " + sequence);
//						}
//						
//					}
//				} else {
//					System.out.println("about to run some splits, no of positions in TARecord:  name: " + name + ", " + taRec.getCountDist() + " DONE, breakdown of splits:");
//					System.out.println("splits.size(): " + splits.size());
//					for (int key : splits.keys()) {
//						Set<IntLongPairs> setOfPairs = splits.get(key);
//						System.out.println("splits key: " + key + ", setOfPairs.size(): " + setOfPairs.size());
//						for (IntLongPairs ilp : setOfPairs) {
//							System.out.println("splits key: " + key + ", IntLongPairs[0]: " + ilp.getPairs()[0].toString() +  ", IntLongPairs[1]: " + ilp.getPairs()[1].toString());
//						}
//					}
//					
//				}
				
				/*
				 * If we have our split from the areSplitsCloseEnoughToBeSingleRecord method, no need to go looking for more
				 */
				if ( ! gotSplits) {
					
					List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, taRec.getSequence().length(), headerMap);
					System.out.println("splits blat record count: " + blatRecs.size() + " for " + name);
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
								System.out.println("No need to smith waterman - have some records, combined score: " + combinedScore + ", seqLength: " + taRec.getSequence().length()  + " for " + name);
								for (BLATRecord bt : recs) {
									results.add(bt);
								}
								gotSplits = true;
							}
						}
//					}
						
					} else {
						System.out.println("No split record found for name: " + name + ", gotSplits: " + gotSplits);
					}
				}
			}
			
			
			if ( ! gotSplits) {
				
				/*
				 * get best start positions
				 */
				TLongList bestStartPositions = taRec.getStartPositions(12, true, 20);
				
//				TLongList bestStartPositionsUpdated = ListUtils.removeAdjacentPositionsInList(bestStartPositions, 200);
	//		System.out.println("Number of entries in bestStartPositions: " + bestStartPositions.size() + ", Number of entries in bestStartPositionsUpdated (removed adjacent positions): " + bestStartPositionsUpdated.size());
				/*
				 * smith waterman
				 */
				List<ChrPosition> tiledAlignerCPs = Arrays.stream(bestStartPositions.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).collect(Collectors.toList());
				List<ChrPosition> tiledAlignerCPsUpdated =  ListUtils.removeAdjacentPositionsInList(tiledAlignerCPs, sequence.length() < 100 ? 350 : 1700);
				if (log) {
					for (ChrPosition cp : tiledAlignerCPsUpdated) {
						System.out.println("will look at " + cp.toIGVString() + ", strand: " + ((ChrPositionName)cp).getName());
					}
				}
				
				List<String[]> swResults = getSmithWaterman(tiledAlignerCPsUpdated, sequence, name != null ? name : "name", log);
				for (String [] array : swResults) {
	//				if (debug) {
	//					System.out.println("about to be turned into a blat record: " + Arrays.deepToString(array));
	//				}
					BLATRecord br = new BLATRecord(array);
					if (br.getScore() > MINIMUM_BLAT_RECORD_SCORE) {
						results.add(br);
					} else if (log) {
						System.out.println("BLAT record score is below the cutoff of " + MINIMUM_BLAT_RECORD_SCORE + ": " + br);
					}
				}
			}
		}
		
		
		/*
		 * only want unique results
		 */
		List<BLATRecord> uniqueResults = results.stream().distinct().collect(Collectors.toList());
		
		
		uniqueResults.sort(null);
		
//		if ( ! uniqueResults.isEmpty()) {
//			BLATRecord bestBR = uniqueResults.get(uniqueResults.size() - 1);
//			long position = headerMap.getLongStartPositionFromChrPosition(new ChrPointPosition(bestBR.getReference(), bestBR.getStartPos()));
//			updateBestRecordDistribution(position, bestBR.getStrand() == '+', taRec, iss, headerMap, bestBR.getScore());
//		}
		
		
		if (log) {
			System.out.println("number of blat records for seq: " + sequence +", " + uniqueResults.size() +", winner: " + (uniqueResults.size() > 0 ? uniqueResults.get(uniqueResults.size() - 1).toString() : "-"));
		}
		if (potentialMatches.size() > 0 || potentialMatchesRC.size() > 0) {
//			String potentialMatchesString = Arrays.stream(potentialMatches.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).map(ChrPosition::toIGVString).collect(Collectors.joining(","));
//			String potentialMatchesStringRC = Arrays.stream(potentialMatchesRC.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).map(ChrPosition::toIGVString).collect(Collectors.joining(","));
//			
//			System.out.println("shortcut!!! potentialMatches.size: " + potentialMatches.size() + ", potentialMatchesRC.size(): " 
//		+ potentialMatchesRC.size() + ", winning blat: " + (uniqueResults.size() > 0 ? uniqueResults.get(uniqueResults.size() - 1).toString() : "-") + ", potentialMatchesString: " + potentialMatchesString + ", potentialMatchesStringRC: " + potentialMatchesStringRC);
			System.out.println("shortcut!!! potentialMatches.size: " + potentialMatches.size() + ", potentialMatchesRC.size(): " + potentialMatchesRC.size() + ", uniqueResults.size(): " + uniqueResults.size() + ", name: " + name + ", seq length: " + sequence.length() + ", seq: " + sequence);
		}
		
		
		
		if (null != name && name.contains("splitcon")) {
			/*
			 * really only care if we have different chromosomes
			 */
			String[] nameArray = name.split("_");
			if ( ! nameArray[1].equals(nameArray[3])) {
				System.out.println("got a splitcon: " + name + " with no of recs: " + uniqueResults.size() + ", taRec: " + (null != taRec ? taRec.toString() : "null"));
				for (BLATRecord br : uniqueResults) {
					System.out.println("splitcon rec: " + br);
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
	 * gets the long position that relates to the supplied BLATRecord, uses this to search the TARecord to see where this position came in the leaderboard
	 * @param br
	 * @param tr
	 * @param iss
	 */
	private static DecimalFormat df2 = new DecimalFormat("#.##");
//	public static void updateBestRecordDistribution(long position, boolean isForwardStrand, TARecord tr, IntSummaryStatistics iss, PositionChrPositionMap headerMap, int score) {
//		if ( ! isForwardStrand) {
//			position = setReverseStrandBit(position);
//		}
//		int leaderBoardPosition = tr.getLeaderBoardPositionOfPosition(position);
//		if (leaderBoardPosition == -1) {
//			ChrPosition cp = headerMap.getChrPositionFromLongPosition(position);
//			System.out.println("Leaderboard == -1, position: " + position + ", TARecord: " + tr + ", cp: " + cp.toIGVString());
////			System.exit(1);
//		}
//		if (leaderBoardPosition > 1) {
//			double perc = ((double)score / tr.getSequence().length()) * 100 ;
//			System.out.println("Leaderboard = " + leaderBoardPosition + ", score: " + score + ", length: " + tr.getSequence().length() + ", perc: " + df2.format(perc) + ", position: " + position + ", TARecord: " + tr.getCountDist());
//		}
//		AtomicInteger ai = leaderboardStats.putIfAbsent(leaderBoardPosition, new AtomicInteger(1));
//		if (null != ai) {
//			ai.incrementAndGet();
//		}
//		iss.accept(leaderBoardPosition);
//		
//		if (leaderBoardPosition == 1) {
//			taClassifierArrayPosition1.incrementAndGet(tr.getClassification().getPosition());
//		} else if (leaderBoardPosition == 2) {
//			taClassifierArrayPosition2.incrementAndGet(tr.getClassification().getPosition());
//		}
//	}
	
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
//				int newStart = start + index;
				return Optional.of(new ChrPositionName(contig, start, start + sequence.length(), sequence));
			}
		}
		
		return Optional.empty();
	}
	
//	public static int getPositionOfRefInSequence(ChrPosition cp, String sequence) {
//		return sequence.indexOf(cp.getName());
//	}
	
//	public static ChrPosition getNewChrPositionWithReference(ChrPosition cp) {
//		String ref = getRefFromChrPos(cp);
//		return new ChrPositionName(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition(), ref);
//	}
	
//	public static List<List<ChrPosition>> getListOfCompoundRecordPositionsFromMap(TLongIntMap map, PositionChrPositionMap headerMap, String sequence) {
//		
//		long [] keys = map.keys();
//		Arrays.sort(keys);
//		String revCompSeq = SequenceUtil.reverseComplement(sequence);
//		
//		List<List<ChrPosition>> results = new ArrayList<>();
//		List<ChrPosition> subList = new ArrayList<>();
//		ChrPosition previousCP = null;
//		long previousKey = 0l;
//		for (long k : keys) {
////			System.out.println("In getListOfCompoundRecordPositionsFromMap with position: " + k + " and value: " + map.get(k));	
//			ChrPosition cp = headerMap.getChrPositionFromLongPosition(k);
//			boolean forwardStrand = "F".equals(((ChrPosBait)cp).getBait());
//			
//			String ref = getRefFromChrStartStop(cp.getChromosome(), cp.getStartPosition(), cp.getStartPosition() + (int)map.get(k));
////			System.out.println("about to call getChrPositionWithReference with cp: " + cp.toIGVString() + ", ref: " + ref + ", seq: " +  (forwardStrand ? sequence : revCompSeq) + ", fs: " + forwardStrand);
//			Optional<ChrPosition> optionalCP = getChrPositionWithReference(cp.getChromosome(), cp.getStartPosition(), (forwardStrand ? sequence : revCompSeq), ref);
//			
//			/*
//			 * if the optional exists, then carry on, otherwise, next...
//			 * (It may not exist if the corresponding reference does not appear in the sequence. This could be due to the commonly occurring tiles phenomena)
//			 */
//			if (optionalCP.isPresent()) {
//			
//				if (null != previousCP) {
//					if (previousKey != 0 && k > previousKey + 20000) {
//						/*
//						 * new sublist
//						 */
//						if (subList.size() > 1) {
//							results.add(subList);
////							System.out.println("adding sublist to results. sublist has" + subList.size() + " elements");
//						}
//						subList  = new ArrayList<>();
//						
//					}
//				}
////				System.out.println("adding cp to sublist " + cp.toIGVString());
//				subList.add(optionalCP.get());
//				previousCP = optionalCP.get();
//				previousKey = k;
//			} else {
////				System.out.println("optional cp not present: " + cp.toIGVString());
//			}
//		}
//		/*
//		 * add last sub list
//		 */
//		if (subList.size() > 1) {
//			results.add(subList);
//		}
//		
//		return results;
//	}
	
	public static Map<String, BLATRecord> runTiledAligner(String tiledAlignerFile, String [] sequences, int tileLength) {
		
		/*
		 * get header and convert into object that will give a ChrPosition when supplied with a long
		 */
		PositionChrPositionMap headerMap = new PositionChrPositionMap();
		try {
			TabbedHeader header = getTiledAlignerHeader(tiledAlignerFile);
			List<String> list = new ArrayList<>();
			header.iterator().forEachRemaining(list::add);
			headerMap.loadMap(list);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<String, BLATRecord> output = new HashMap<>(sequences.length * 2);
		
		/*
		 * try and get direct matches for the supplied sequences and their rcs
		 */
		List<String> sequencesToMatch = new ArrayList<>();
		for (String s : sequences) {
			if (null != s && s.indexOf('N') == -1) {
				sequencesToMatch.add(s);
			}
		}
		System.out.println("will send " + sequencesToMatch.size() + " sequence for direct matching!");
		Map<String, ChrPosition> matches = getDirectMatch(sequencesToMatch.toArray(new String[] {}));
		System.out.println("got " + matches.size() + " direct matches!!!");
		
		for (Entry<String, ChrPosition> match : matches.entrySet()) {
			output.put(match.getKey(), new BLATRecord(getDetailsForBLATRecordPerfectMatch(match.getValue(), "name", match.getKey(), true)));
			System.out.println("matches:" + match.getKey());
		}
		
		int newSequenceSize = sequences.length - matches.size();
		if (newSequenceSize > 0) {
			System.out.println("sequences.length: " + sequences.length);
			System.out.println("matches.size(): " + matches.size());
			System.out.println("newSequenceSize: " + newSequenceSize);
			String [] newSequences = new String[newSequenceSize];
			int k = 0;
			for (String s : sequences) {
				
				if (matches.containsKey(s)) {
					System.out.println("in matches: " + s);
				} else if (matches.containsKey(SequenceUtil.reverseComplement(s))) {
					System.out.println("in matches (rc): " + SequenceUtil.reverseComplement(s));
				} else {
					System.out.println("putting into array: " + s);
					newSequences[k++] = s;
				}
			}
		
			Map<String, String> tiledInput = new HashMap<>();
			for (String s : newSequences) {
				tileInput(s, tileLength, tiledInput);
			}
			getTiledDataInMap(tiledAlignerFile, tiledInput);
			// call convertMapOfStringsToLongList to get map of tile and list of positions it appears
			Map<String, TLongList> tilesAndPositions = convertMapOfStringsToLongList(tiledInput);
		
			for (String s : newSequences) {
				if (null != s && s.length() > 0) {
					Map<Integer, TLongList> results = map(tilesAndPositions, s, tileLength);
					
					List<Integer> highestCounts = new ArrayList<>(results.keySet());
					highestCounts.sort(null);
					for (int i = highestCounts.size() - 1 ; i >= 0 ; i--) {
						System.out.println("tile count: " + highestCounts.get(i) + ", number of start positions: " + results.get( highestCounts.get(i)).size());
					}
//				for (Entry<Integer, TLongList> entry : results.entrySet()) {
//					System.out.println("key: " + entry.getKey() + ", value: " + Arrays.toString(entry.getValue().toArray()));
//				}
				
				
	//			Map<Integer, TLongList> rsResults = map(tilesAndPositions, SequenceUtil.reverseComplement(s), tileLength);
				TARecord rec = new TARecord(s, results);
	//			int [] maxTileCounts = rec.getTopNCounts(15, 3);
	//			
	//			if (maxTileCounts == null || maxTileCounts.length == 0) {
	//				System.out.println("WARNING! Didn't find any tile counts for seq: " + s);
	//				continue;
	//			}
				
				
	//			int maxTileCount =  (maxTileCounts[0] >= maxTileCounts[1]) ? maxTileCounts[0] : maxTileCounts[1];
	//			boolean forwardStrand =  (maxTileCounts[0] >= maxTileCounts[1]);
	//			int [] maxTileCounts = rec.getMaxCount();
	//			int maxTileCount =  (maxTileCounts[0] >= maxTileCounts[1]) ? maxTileCounts[0] : maxTileCounts[1];
	//			boolean forwardStrand =  (maxTileCounts[0] >= maxTileCounts[1]);
				
	//			TLongList bestStartPositions = rec.getStartPositionsForCount(maxTileCounts);
	//			System.out.println("bestStartPositions:");
	//			for (int i = 0 ; i < bestStartPositions.size() ; i++) {
	//				System.out.println(bestStartPositions.get(i));
	//			}
	//			BitSet bs = TARecord.areBothStrandsRepresented(bestStartPositions);
	//			if (bs.cardinality() < 2) {
	//				if ( ! bs.get(0)) {
	//					TLongList fsList = TARecord.getStartPositionsByStrand(rec.getCounts(), true);
	//					System.out.println("Missing start positions on the fs - adding " + fsList.size() + " positions");
	//					bestStartPositions.addAll(fsList);
	//				}
	//				if ( ! bs.get(1)) {
	//					TLongList rsList = TARecord.getStartPositionsByStrand(rec.getCounts(), false);
	//					System.out.println("Missing start positions on the rs - adding " + rsList.size() + " positions");
	//					bestStartPositions.addAll(rsList);
	//				}
	//			}
	//			if (bestStartPositions.size() > 100) {
	//				 maxTileCounts = rec.getTopNCounts(2, 3);
	//				 bestStartPositions = rec.getStartPositionsForCount(maxTileCounts);
	//			}
				
				/*
				 * remove adjacent positions as they will just render the same result
				 */
				TLongList bestStartPositions = rec.getStartPositions(12, true, 20);
				TLongList bestStartPositionsUpdated = ListUtils.removeAdjacentPositionsInList(bestStartPositions);
				System.out.println("Number of entries in bestStartPositions: " + bestStartPositions.size() + ", Number of entries in bestStartPositionsUpdated (removed adjacent positions): " + bestStartPositionsUpdated.size());
				if (bestStartPositions.isEmpty()) {
					System.out.println("Found no start positions for sequence: " + s);
				} else {
	//			TLongList bestStartPositions = rec.getStartPositionsForCount(maxTileCount, forwardStrand);
	//			StringBuilder sb = new StringBuilder();
	//			sb.append("seq: ").append(s).append(Constants.TAB);
	//			sb.append("score: ").append(maxTileCounts[2] + tileLength).append(Constants.TAB);		// score is tile count * tile size
	//			if (bestStartPositions.size() < 10) {
					
					/*
					 * check to see if we have both strands
					 */
					boolean directMatchFound = false;
//					BitSet bs = TARecord.areBothStrandsRepresented(bestStartPositionsUpdated);
//					if ( ! bs.get(0)) {
//						Optional<ChrPosition> optionalCP = getDirectMatch(s);
//						if (optionalCP.isPresent()) {
//							System.out.println("Found a direct match!!! " + optionalCP.get().toIGVString());
//							directMatchFound = true;
//							output.put(s, new BLATRecord(getDetailsForBLATRecordPerfectMatch(optionalCP.get(), "name",s, true)));
//						}
//					}
//					if ( ! directMatchFound &&  ! bs.get(1)) {
//						
//						Optional<ChrPosition> optionalCP = getDirectMatch(SequenceUtil.reverseComplement(s));
//						if (optionalCP.isPresent()) {
//							System.out.println("Found a direct match!!! " + optionalCP.get().toIGVString());
//							directMatchFound = true;
//							output.put(s, new BLATRecord(getDetailsForBLATRecordPerfectMatch(optionalCP.get(), "name",s, false)));
//						}
//					}
					
					if ( ! directMatchFound) {
						List<ChrPosition> tiledAlignerCPs = Arrays.stream(bestStartPositionsUpdated.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l)).filter(cp -> null != cp).collect(Collectors.toList());
						for (ChrPosition cp : tiledAlignerCPs) {
							System.out.println("will look at " + cp.toIGVString() + ", strand: " + ((ChrPosBait)cp).getBait());
						}
						
						List<String[]> swResults = getSmithWaterman(tiledAlignerCPs, s, null);
						List<BLATRecord> blatRecs = new ArrayList<>();
						for (String [] array : swResults) {
		//					System.out.println("about to be turned into a blat record: " + Arrays.deepToString(array));
							blatRecs.add(new BLATRecord(array));
						}
						
						blatRecs.sort(null);
						System.out.println("number of blat records for s: " + s +", " + blatRecs.size());
	//					System.out.println("number of blat records for s: " + s +", " + blatRecs.size() + ", showing the top 3 by score");
	//					for (int j = blatRecs.size() - 1, min = Math.max(0, blatRecs.size() - 3); j >= min ; j--) {
	//						System.out.println("blat record: " + blatRecs.get(j));
	//					}
						
						output.put(s, blatRecs.get(blatRecs.size() - 1));
					}
				}
			}
		}
				
				
//				String chrPositionsString = tiledAlignerCPs.stream().map(ChrPosition::toIGVString).collect(Collectors.joining(","));
//				sb.append(chrPositionsString);
//			} else {
//				sb.append("more than 10 start positions with this tile count");
//			}
//			output.add(sb.toString());
		}
		
		return output;
	}
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		Map<String, BLATRecord> output = runTiledAligner(args[0], Arrays.copyOfRange(args, 1, args.length));
		for (Entry<String, BLATRecord> s : output.entrySet()) {
			System.out.println(s.getValue());
		}
		
//		/*
//		 * get header and convert into object that will give a ChrPosition when supplied with a long
//		 */
//		PositionChrPositionMap headerMap = new PositionChrPositionMap();
//		try {
//			TabbedHeader header = getTiledAlignerHeader(args[0]);
//			List<String> list = new ArrayList<>();
//			header.iterator().forEachRemaining(list::add);
//			headerMap.loadMap(list);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Map<String, String> tiledInput = new HashMap<>();
//		for (int i = 1 ; i < args.length ; i++) {
//			tileInput( args[i], 13, tiledInput);
//		}
//		getTiledDataInMap(args[0], tiledInput);
//		// call convertMapOfStringsToLongList to get map of tile and list of positions it appears
//		Map<String, TLongList> tilesAndPositions = convertMapOfStringsToLongList(tiledInput);
//		// now see if we can find a contiguous block!
//		
//		
//		for (int i = 1 ; i < args.length ; i++) {
//			System.out.println("looking for location of: " + args[i]);
//			Map<Integer, TLongList> fsResults = map(tilesAndPositions, args[i], 13);
//			Map<Integer, TLongList> rsResults = map(tilesAndPositions, SequenceUtil.reverseComplement(args[i]), 13);
//			TARecord rec = new TARecord(args[i], fsResults, rsResults);
//			System.out.println("TARecord: " + rec.toString());
//			List<Integer> fsOrderedList = new ArrayList<>(fsResults.keySet());
//			fsOrderedList.sort((c1, c2) -> Integer.compare(c2,  c1));
//			List<Integer> rsOrderedList = new ArrayList<>(rsResults.keySet());
//			rsOrderedList.sort((c1, c2) -> Integer.compare(c2,  c1));
//			
//			int j = 0;
//			for (Integer integer : fsOrderedList) {
//				if (integer.intValue() > 2) {
//					if (++j > 3) {
//						break;
//					}
//					TLongList startPositions = fsResults.get(integer);
//					boolean tooManyInList = startPositions.size() > 10;
//					String positionsString = tooManyInList ? " > 10" : Arrays.toString(startPositions.toArray());
//					String chrPositionsString = startPositions.size() == 1 && startPositions.get(0) < 0 ? "common entry" :  tooManyInList ? " > 10" : Arrays.stream(startPositions.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l).toIGVString()).collect(Collectors.joining(","));
//					System.out.println("FS: maxNumberOfMatches: " + integer + ", startPosiitons size: " + startPositions.size() + ", startPosiitons: " + positionsString + ", startChrPosiitons: " + chrPositionsString);
//				}
//			}
//			j = 0;
//			for (Integer integer : rsOrderedList) {
//				if (integer.intValue() > 2) {
//					if (++j > 3) {
//						break;
//					}
//					TLongList startPositions = rsResults.get(integer);
//					boolean tooManyInList = startPositions.size() > 10;
//					String positionsString = tooManyInList ? " > 10" : Arrays.toString(startPositions.toArray());
//					String chrPositionsString = startPositions.size() == 1 && startPositions.get(0) < 0 ? "common entry" :  tooManyInList ? " > 10" : Arrays.stream(startPositions.toArray()).mapToObj(l -> headerMap.getChrPositionFromLongPosition(l).toIGVString()).collect(Collectors.joining(","));
//					System.out.println("RS: maxNumberOfMatches: " + integer + ", startPosiitons size: " + startPositions.size() + ", startPosiitons: " + positionsString + ", startChrPosiitons: " + chrPositionsString);
//				}
//			}
//		}
//		
		
		System.out.println("time taken: " + (System.currentTimeMillis() - start) / 1000 + "s");
	}

	public static String[] deClutterDiffs(String[] swDiffs) {
		int numberOfInsertions = getInsertionCount(swDiffs[1], ' ');
		if (numberOfInsertions <= 2) {
			return swDiffs;
		}
		
		
		return null;
	}
}
