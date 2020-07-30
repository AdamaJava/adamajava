package org.qcmg.qsv.tiledaligner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.NumberUtils;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class TARecord {
	
	public static final long CUTOFF = 1l << TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
	
	
	/**
	 * Tiled Aligner Record that captures all the useful information from a tiled aligner match
	 */
	private final String sequence;
	private final TIntObjectMap<TLongList> countAndStartPositionsMap;
	
	public TARecord(String seq, Map<Integer, TLongList> results) {
		if (StringUtils.isNullOrEmpty(seq)) {
			throw new IllegalArgumentException("Null or empty sequence string passed to ctor");
		}
		if (null == results) {
			throw new IllegalArgumentException("Null map passed to ctor");
		}
		
		this.sequence = seq;
		
		/*
		 * populate the map with the forward and reverse strand maps
		 */
		countAndStartPositionsMap = new TIntObjectHashMap<TLongList>((results.size()) * 2);
		
		for (Entry<Integer, TLongList> entry : results.entrySet()) {
			TLongList list = countAndStartPositionsMap.get(entry.getKey());
			if (null == list) {
				list = new TLongArrayList();
				countAndStartPositionsMap.put(entry.getKey(), list);
			}
			list.addAll(entry.getValue());
		}
	}
	
	public TIntObjectMap<TLongList> getCounts() {
		return countAndStartPositionsMap;
	}
	
	public String getSequence() {
		return sequence;
	}
	
	
	public int [] getTopNCounts(int number, int minValue) {
		int totalLength = countAndStartPositionsMap.size();
		if (totalLength == 0) {
			return new int[] {};
		}
		
		int [] allStartPositions = countAndStartPositionsMap.keys();
		Arrays.sort(allStartPositions);
		
		if (totalLength > number) {
			int [] results = Arrays.copyOfRange(allStartPositions, totalLength - number, totalLength);
			if (results[0] >= minValue) {
				return results;
			} else {
				/*
				 * need to remove elements from array that are less than minValue
				 */
				TIntList list = new TIntArrayList();
				for (int i : results) {
					if (i > minValue) {
						list.add(i);
					}
				}
				return list.toArray();
			}
 		} else {
 			return allStartPositions;
 		}
	}
	
	/**
	 * If the returned TLongList is neither null nor empty, then it will contain the start positions where the sequence matches perfectly to the reference
	 */
	public TLongList getPerfectMatchPositions() {
		int perfectMatchCount = sequence.length() - TiledAlignerUtil.TILE_LENGTH + 1;
//		System.out.println("in getPerfectMatchPositions with sequence.length(): " + sequence.length() + " and perfectMatchCount: " + perfectMatchCount);
		
		/*
		 * convert this number to tile count that includes common tiles, although set the common tile count to zero for exact match
		 */
		return countAndStartPositionsMap.get(NumberUtils.getTileCount(perfectMatchCount, 0));
	}
	
	/**
	 * This method will return a map of start and stop positions that are within the window as specified by the user.
	 * It attempts to deal with sequences that map to the reference with large(ish) gaps 
	 * 
	 * @param window
	 * @return
	 */
	public TLongIntMap getCompoundStartPositions(int window) {
		TLongIntMap map = new TLongIntHashMap();
		
		int seqLength = sequence.length();
		int minCutoff = seqLength / 3;
		/*
		 * start with the largest, and work our way down
		 */
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		
		for (int i = keys.length - 1 ; i >= 0; i--) {
			/*
			 * don't want to look at positions that only matched a couple of times
			 * 15 seems like a good number to start with
			 */
			if (keys[i] > 15) {
				TLongList list = countAndStartPositionsMap.get(keys[i]);
				
				System.out.println("in getCompoundStartPositions looking at matching tile count: " + keys[i] + " which has " + list.size() + " start positions");
				for (int j = 0 ; j < list.size() ; j++) {
					long l = list.get(j);
					
					for (int k = 0 ; k < i ; k++) {
						if (keys[k] > 10) {
							TLongList subList = countAndStartPositionsMap.get(keys[k]);
							for (int m = 0 ; m < subList.size() ; m++) {
								long l2 = subList.get(m);
								long absoluteDiff = Math.abs(l - l2);
								if (absoluteDiff < window) {
//									if (absoluteDiff < window && absoluteDiff > TiledAlignerUtil.TILE_LENGTH * (i - k)) {
									/*
									 * Check to see if the combined lengths of these regions equal more than 30% of the sequence length
									 * if so, insert an entry into the map!
									 */
									if (keys[i] + keys[k] + (2 * TiledAlignerUtil.TILE_LENGTH) - 2 > minCutoff) {
										map.put(l, keys[i] + TiledAlignerUtil.TILE_LENGTH - 1);
										map.put(l2, keys[k] + TiledAlignerUtil.TILE_LENGTH - 1);
									}
//									System.out.println("added " + l + ":" + (keys[i] + TiledAlignerUtil.TILE_LENGTH - 1) + ", and " + l2 + ":" + (keys[k] + TiledAlignerUtil.TILE_LENGTH - 1) + " to map!");
								}
							}
						}
					}
				}
			}
		}
		System.out.println("size of getCompoundStartPositions map: " + map.size());
		return map;
	}
	
	public TLongIntMap getBestCompoundStartPositions(int window, int minimumDistanceApart) {
		List<TLongIntMap> listOfMaps = new ArrayList<>();
		TLongSet alreadyAddedLongs = new TLongHashSet();
		
		int seqLength = sequence.length();
		int minCutoff = seqLength / 3;
		/*
		 * start with the largest, and work our way down
		 */
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		
		for (int i = keys.length - 1 ; i >= 0; i--) {
			/*
			 * don't want to look at positions that only matched a couple of times
			 * 15 seems like a good number to start with
			 */
			int numberOfMatchesForI = NumberUtils.getPartOfPackedInt(keys[i], true);
			if (numberOfMatchesForI > 15) {
				TLongList list = countAndStartPositionsMap.get(keys[i]);
				
				System.out.println("in getBestCompoundStartPositions looking at matching tile count: " + keys[i] + " which has " + list.size() + " start positions");
				for (int j = 0 ; j < list.size() ; j++) {
					long l = list.get(j);
					
					/*
					 * only proceed with this long if its not already been added to a map
					 */
					if ( ! alreadyAddedLongs.contains(l)) {
						TLongIntMap map = new TLongIntHashMap();
					
						for (int k = 0 ; k < i ; k++) {
							int numberOfMatchesForK = NumberUtils.getPartOfPackedInt(keys[k], true);
							if (numberOfMatchesForK > 10) {
								TLongList subList = countAndStartPositionsMap.get(keys[k]);
								for (int m = 0 ; m < subList.size() ; m++) {
									long l2 = subList.get(m);
									long absoluteDiff = Math.abs(l - l2);
									if ((absoluteDiff < window) && (absoluteDiff > minimumDistanceApart)) {
	//									if (absoluteDiff < window && absoluteDiff > TiledAlignerUtil.TILE_LENGTH * (i - k)) {
										/*
										 * Check to see if the combined lengths of these regions equal more than 30% of the sequence length
										 * if so, insert an entry into the map!
										 */
										if (numberOfMatchesForI + numberOfMatchesForK + (2 * TiledAlignerUtil.TILE_LENGTH) - 2 > minCutoff) {
											map.put(l, numberOfMatchesForI);
											map.put(l2, numberOfMatchesForK);
											alreadyAddedLongs.add(l);
											alreadyAddedLongs.add(l2);
										}
	//									System.out.println("added " + l + ":" + (keys[i] + TiledAlignerUtil.TILE_LENGTH - 1) + ", and " + l2 + ":" + (keys[k] + TiledAlignerUtil.TILE_LENGTH - 1) + " to map!");
									}
								}
							}
						}
						if (map.size() > 0) {
							listOfMaps.add(map);
						}
					}
				}
			}
		}
		
		/*
		 * now need to find the best map in the list
		 */
		System.out.println("Finding best map, listOfMaps size: " + listOfMaps.size());
		TLongIntMap bestMap = new TLongIntHashMap();
		int bestScore = 0;
		for (TLongIntMap m : listOfMaps) {
			int thisMapScore = Arrays.stream(m.values()).sum();
			System.out.println("Finding best map, thisMap size: " + m.size() + ", score: " + thisMapScore);
			if (thisMapScore > bestScore) {
				bestMap = m;
				bestScore = thisMapScore;
			}
		}
		
		System.out.println("size of getBestCompoundStartPositions map: " + (null != bestMap ? bestMap.size() : 0));
		return bestMap;
	}
	
	public int getLeaderBoardPositionOfPosition(long position) {
		int cutoff = 500;
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		int j = 1;
		for (int i = keys.length - 1 ; i >= 0; i--) {
			TLongList subList = countAndStartPositionsMap.get(keys[i]);
			
			if (subList.size() == 1) {
				if (subList.get(0) == position || Math.abs(subList.get(0) - position) <= cutoff) {
					return j;
				}
			} else {
				subList.sort();
				int positionInList = subList.binarySearch(position);
				if (positionInList > 0) {
					return j;
				} else {
					/*
					 * check entries either side of insertionPoint to see if we are close enough
					 */
					positionInList = Math.abs(positionInList) - 1;
					if (positionInList >= subList.size()) {
						positionInList = subList.size() - 1;
					} else if (positionInList < 0) {
						positionInList = 0;
					}
					long prevLong = subList.get(positionInList);
					if (prevLong == position || Math.abs(prevLong - position) <= cutoff) {
						return j;
					}
					if (prevLong > position && positionInList > 0) {
						prevLong = subList.get(positionInList - 1);
						if (prevLong == position || Math.abs(prevLong - position) <= cutoff) {
							return j;
						}
					}
				}
			}
			j++;
		}
		return -1;
	}
	
	
	public TLongList getStartPositions(int matchingTileMinimumCount, boolean bothStrandsMustBePresent, int maxNumberOfStartPositions) {
		int maxCutoff = 50;
		int topN = 2;
		TLongList list = new TLongArrayList(maxCutoff + 10);
		
		if (null == countAndStartPositionsMap || countAndStartPositionsMap.isEmpty()) {
			return list;
		}
		
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		
		/*
		 * get the top 3 (if present)
		 * or less if by adding the next set, the total size of list will exceed lets saaaaaaaay 50
		 */
		int size = keys.length;
		
		
		for (int i = size - 1, min = Math.max(0,  size - 1 - topN) ; i >= min ; i--) {
			if (i == size - 1) {
				list.addAll(countAndStartPositionsMap.get(keys[i]));
			} else {
				
				if (countAndStartPositionsMap.get(keys[i]).size() + list.size() < maxCutoff) {
					list.addAll(countAndStartPositionsMap.get(keys[i]));
				}
			}
		}
		return list;
	}
	
	public TAClassifier getClassification() {
		int perfectMatchCount = sequence.length() - TiledAlignerUtil.TILE_LENGTH + 1;
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		
		
		int maxCount = keys[keys.length - 1];
		TLongList list = countAndStartPositionsMap.get(maxCount);
		int listSize = list.size();
		
		if (maxCount == perfectMatchCount) {
			if (listSize == 1) {
				return TAClassifier.PM_SE;
			} else if (listSize < 100) {
				return TAClassifier.PM_LTOHE;
			} else {
				return TAClassifier.PM_MTOHE;
			}
		}
		
		int quartile = perfectMatchCount / 4;
		
		for (int i = 1 ; i < 5 ; i++) {
			if (maxCount > perfectMatchCount - ( i * quartile)) {
				if (listSize == 1) {
					return i == 1 ? TAClassifier.FIRST_Q_SE : (i == 2 ? TAClassifier.SECOND_Q_SE : (i == 3 ? TAClassifier.THIRD_Q_SE : TAClassifier.FOURTH_Q_SE)) ;
				} else if (listSize < 100) {
					return i == 1 ? TAClassifier.FIRST_Q_LTOHE : (i == 2 ? TAClassifier.SECOND_Q_LTOHE : (i == 3 ? TAClassifier.THIRD_Q_LTOHE : TAClassifier.FOURTH_Q_LTOHE)) ;
				} else {
					return i == 1 ? TAClassifier.FIRST_Q_MTOHE : (i == 2 ? TAClassifier.SECOND_Q_MTOHE : (i == 3 ? TAClassifier.THIRD_Q_MTOHE : TAClassifier.FOURTH_Q_MTOHE)) ;
				}
			}
		}
		return TAClassifier.UNKNOWN;
	}
	
	
	public TLongList getStartPositionsForCount(int count) {
		return countAndStartPositionsMap.get(count);
	}
	public TLongList getStartPositionsForCount(int [] counts) {
		TLongList list = new TLongArrayList();
		
		for (int i : counts) {
			if (countAndStartPositionsMap.containsKey(i)) {
				list.addAll(countAndStartPositionsMap.get(i));
			}
		}
		
		return list;
	}
	
	/**
	 * Get the top tanking start position on a particular strand
	 * @param map
	 * @param fs
	 * @return
	 */
	public static TLongList getStartPositionsByStrand(TIntObjectMap<TLongList> map, boolean fs) {
		TLongList list = new TLongArrayList();
		
		int [] sortedKeys = map.keys();
		Arrays.sort(sortedKeys);
		
		for (int i = sortedKeys.length -1 ; i > 0 ; i--) {
			TLongList startPositions = map.get(sortedKeys[i]);
			TLongList results = getStartPositionsByStrand(startPositions, fs);
			if ( ! results.isEmpty()) {
				return results;
			}
		}
		
		return list;
	}
	
	public static TLongList getStartPositionsByStrand(TLongList list, boolean fs) {
		TLongList results = new TLongArrayList();
		list.forEach(l -> {
			if ((l < CUTOFF && fs) || (l > CUTOFF && ! fs)) {
				results.add(l);
			}
			return true;
		});
		return results;
	}
	
	/**
	 * sort list, look at either end to see if we have values above and below cutoff
	 * @param list
	 * @return
	 */
	public static BitSet areBothStrandsRepresented(TLongList list) {
		BitSet bs = new BitSet(2);
		if (null == list || list.isEmpty()) {
			return bs; 
		}
		list.sort();
		if (list.get(0) < CUTOFF)
			bs.set(0);
		if (list.get(list.size() - 1) > CUTOFF)
			bs.set(1);
		return bs;
	}
	
	public int getHightestTileCount() {
		int [] keys = countAndStartPositionsMap.keys();
		if (keys.length == 0) {
			return -1;
		} else if (keys.length > 1) {
			Arrays.sort(keys);
		}
		return keys[keys.length - 1];
	}
	
	public String getCountDist() {
		StringBuilder sb = new StringBuilder();
		int [] keys = countAndStartPositionsMap.keys();
		Arrays.sort(keys);
		for (int i = keys.length - 1 ; i >= 0; i--) {
			sb.append(Arrays.toString(NumberUtils.splitIntInto2(keys[i]))).append(":").append(countAndStartPositionsMap.get(keys[i]).size()).append(",");
		}
		return sb.toString();
	}
	
	
	@Override
	public String toString() {
		int [] counts = getTopNCounts(3, 3);
		
		return sequence + ", top 3 tile counts: " + Arrays.toString(counts) 
				+ ", number of start positions: " + countAndStartPositionsMap.get(counts[counts.length - 1]).size();
	}

}
