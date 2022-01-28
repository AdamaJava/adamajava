/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.NumberUtils;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPair;
import au.edu.qimr.tiledaligner.model.IntLongPairs;
import au.edu.qimr.tiledaligner.model.TARecord;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TARecordUtil {
	
	public static final int TILE_OFFSET = TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
	public static final int TILE_LENGTH = TiledAlignerUtil.TILE_LENGTH;
	public static final int REVERSE_COMPLEMENT_BIT = TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
	public static final int MIN_BLAT_SCORE = TiledAlignerUtil.MINIMUM_BLAT_RECORD_SCORE;
	public static final int MIN_TILE_COUNT = MIN_BLAT_SCORE - TILE_LENGTH;
	public static final int MAX_GAP_FOR_SINGLE_RECORD = 500000;
	public static final int BUFFER = 10;
	public static final int RANGE_BUFFER = 5;
	public static final int MIN_BLAT_SCORE_MINUS_BUFFER = MIN_BLAT_SCORE - BUFFER;
	public static final int MIN_BLAT_SCORE_MINUS_RANGE_BUFFER = MIN_BLAT_SCORE - RANGE_BUFFER;
	public static final PositionChrPositionMap pcpm = new PositionChrPositionMap();
	
	private static final QLogger logger = QLoggerFactory.getLogger(TARecordUtil.class);
	
	
	/**
	 * What is being examined here is if parts of the sequence are mapping relatively close by (say within 10kb of each section of sequence
	 * that would mean that smithwaterman wouldn't work if the highest start position count was used (as is currently the case).
	 * 
	 *  A great example is here:
	 *  00000003 agaatgtaattatatctagtgctgcagaaagg 00000034
		>>>>>>>> |||||||||||||||||||||||||||||||| >>>>>>>>
		92655209 agaatgtaattatatctagtgctgcagaaagg 92655240
		
		00000035 cctttagaaataagagggccatatgacgtggcaaatct 00000072
		>>>>>>>> |||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92655637 cctttagaaataagagggccatatgacgtggcaaatct 92655674
		
		00000073 aggcttgctgtttgggctctctgaaagtgacgccaaggctgcggtgtcca 00000122
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92656070 aggcttgctgtttgggctctctgaaagtgacgccaaggctgcggtgtcca 92656119
		
		00000123 ccaactgccgagcagcgcttctccatggag 00000152
		>>>>>>>> |||||||||||||||||||||||||||||| >>>>>>>>
		92656120 ccaactgccgagcagcgcttctccatggag 92656149
		
		00000153 aaactagaaaaactgcttttggaattatctctacagtgaagaaacctcgg 00000202
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660327 aaactagaaaaactgcttttggaattatctctacagtgaagaaacctcgg 92660376
		
		00000203 ccatcagaaggagatgaagattgtcttccagcttccaagaaagccaagtg 00000252
		>>>>>>>> |||||||||||||||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660377 ccatcagaaggagatgaagattgtcttccagcttccaagaaagccaagtg 92660426
		
		00000253 tgagggctgaaaagaatgccccagtctctgtcagcac 00000289
		>>>>>>>> ||||||||||||||||||||||||||||||||||||| >>>>>>>>
		92660427 tgagggctgaaaagaatgccccagtctctgtcagcac 92660463

		This record has 4 blocks, but they are far enough away from each other that running smith waterman against it as a whole wouldn't work.
		
		The plan is to identify these sequences and try and find a better means of reporting them.
	 * @param record
	 * @return
	 */
	
	
	public static boolean inRange(long position, long startOne, long stopOne, long startTwo, long stopTwo) {
		return (position >= startOne && position <= stopOne) || (position >= startTwo && position <= stopTwo);
	}
	
	public static TLongList getLongList(long ... list) {
		TLongList listToReturn = new TLongArrayList(list.length + 1);
		for (long l : list) {
			listToReturn.add(l);
		}
		return listToReturn;
	}
	
	public static List<BLATRecord[]> blatRecordsFromSplits(TIntObjectMap<Set<IntLongPairs>> splits, String name, int seqLength, PositionChrPositionMap headerMap) {
		if (null != splits && ! splits.isEmpty()) {
		
			/*
			 * get the highest scoring list of splits
			 */
			int [] keys = splits.keys();
			Arrays.sort(keys);
			int maxKey = keys[keys.length - 1];
			Set<IntLongPairs> maxSplits = splits.get(maxKey);
			
			List<BLATRecord[]> blats = new ArrayList<>(maxSplits.size() + 1);
			for (IntLongPairs maxSplit : maxSplits) {
				IntLongPair[] pairs = maxSplit.getPairs();
				BLATRecord [] blatties = new BLATRecord[pairs.length];
				for (int i = 0 ; i < pairs.length ; i++) {
					blatties[i] = BLATRecordUtil.blatRecordFromSplit(pairs[i], name, seqLength, headerMap);
				}
				Arrays.sort(blatties);
				blats.add(blatties);
			}
			return blats;
		}
		return Collections.emptyList();
		
	}
	
	/**
	 * If the ILPs can form a single BLAT record, then return that, otherwise, return as few BLAT records as possible. ie. if there are 3 ILPs in the pair, and 2 can be combined to form a single BLAT rec then do so
	 * 
	 * @param splits
	 * @param name
	 * @param seqLength
	 * @param headerMap
	 * @return
	 */
	public static List<BLATRecord[]> blatRecordsFromSplitsNew(TIntObjectMap<Set<IntLongPairs>> splits, String name, int seqLength, PositionChrPositionMap headerMap) {
		if (null != splits && ! splits.isEmpty()) {
			
			/*
			 * get the highest scoring list of splits
			 */
			int [] keys = splits.keys();
			Arrays.sort(keys);
			int maxKey = keys[keys.length - 1];
			
			Set<IntLongPairs> maxSplits = splits.get(maxKey);
			logger.info("Number of splits: " + splits.size() + ", number of splits with max coverage: " + maxSplits.size());
			
			List<BLATRecord[]> blats = new ArrayList<>(maxSplits.size() + 1);
			for (IntLongPairs maxSplitILPs : maxSplits) {
				BLATRecord [] blatties = null;
				/*
				 * will attempt to create a single BLAT record
				 */
				if (IntLongPairsUtil.isIntLongPairsAValidSingleRecord(maxSplitILPs)) {
					Optional<BLATRecord> oBR = BLATRecordUtil.blatRecordFromSplits(maxSplitILPs, name, seqLength, headerMap, TILE_LENGTH);
					if (oBR.isPresent()) {
						blatties = new BLATRecord[] {oBR.get()};
					}
				} else {
					
					IntLongPair[] pairs = maxSplitILPs.getPairs();
					logger.info("createing BLAT records, number of constituent ILPs in ILPS: " + pairs.length);
					if (pairs.length < 3) {
						/*
						 * return a BLAT record for each constituent in the maxSplitILPs
						 */
						blatties = new BLATRecord[pairs.length];
						for (int i = 0 ; i < pairs.length ; i++) {
							blatties[i] = BLATRecordUtil.blatRecordFromSplit(pairs[i], name, seqLength, headerMap);
						}
						Arrays.sort(blatties);
					} else {
						
						/*
						 * from constituent ILPs, find largst, and then see if any combination of adding other ILPs results in a single BLAT record
						 */
						Optional<IntLongPairs> oSingleBLATRec = IntLongPairsUtil.getSingleBLATRecordFromILPs(maxSplitILPs);
						if (oSingleBLATRec.isPresent()) {
							/*
							 * need to find the ILPs that didn't make it into the ILPS so that they can be added as separate BLAT records
							 */
							List<IntLongPair> rejectedILPs = IntLongPairsUtil.getRejectedILPs(maxSplitILPs, oSingleBLATRec.get());
							logger.info("found optional singleBLATRecord! Number of rejected ILPs: " + rejectedILPs.size());
							
							blatties = new BLATRecord[rejectedILPs.size() + 1];
							for (int i = 0 ; i < rejectedILPs.size() ; i++) {
								blatties[i] = BLATRecordUtil.blatRecordFromSplit(rejectedILPs.get(i), name, seqLength, headerMap);
							}
							Optional<BLATRecord> oBR = BLATRecordUtil.blatRecordFromSplits(oSingleBLATRec.get(), name, seqLength, headerMap, TILE_LENGTH);	
							if (oBR.isPresent()) {
								blatties[blatties.length - 1] = oBR.get();
							}
							
							if (blatties.length > 1) {
								Arrays.sort(blatties);
							}
						}
					}
				}
				if (null != blatties && blatties.length > 0) {
					blats.add(blatties);
				}
			}
			return blats;
		}
		return Collections.emptyList();
	}
	
	/**
	 * This method takes a IntLongPairs object and returns a ChrPosition to int[] map, which is how the IntLongPairs is represented in a BLATRecord
	 * The keys in the map can be sorted to get the ChrPositions in order.
	 * 
	 * This method also trims any overlaps that can commonly occur due to the nature of the tiled aligner approach
	 * 
	 * 
	 * @param splits
	 * @param seqLength
	 * @param headerMap
	 * @return
	 */
	public static Map<ChrPosition, int[]> getChrPositionAndBlocksFromSplits(IntLongPairs splits, int seqLength, PositionChrPositionMap headerMap) {
		IntLongPair[] pairs = IntLongPairsUtil.sortIntLongPairs(splits, seqLength);
		
		int [][] ranges = new int[pairs.length][];
		ChrPosition[] cps = new ChrPosition[pairs.length];
		int i = 0;
		for (IntLongPair ilp : pairs) {
			
			/*
			 * start position is dependent on whether the reverse complement bit has been set.
			 */
			ranges[i] = new int[]{ IntLongPairsUtil.getStartPositionInSequence(ilp, seqLength), getExactMatchOnlyLengthFromPackedInt(ilp.getInt())};
			cps[i] = headerMap.getChrPositionFromLongPosition(ilp.getLong(), ranges[i][1]);
			i++;
		}
		
		trimRangesToRemoveOverlap(ranges, cps);
		
		Map<ChrPosition, int[]> results = new THashMap<>();
		
		for (int j = 0 ; j < pairs.length ; j++) {
			results.put(cps[j], ranges[j]);
		}
		return results;
	}

	/**
	 * NOT SIDE EFFECT FREE
	 * <br>
	 * This method will update the values in the passed in  2D int array if there are overlapping values
	 * It will attempt to remove any overlap from the larger of the 2 ranges should an overlap exist
	 * 
	 * It requires that the ranges array be sorted by position in query string
	 * 
	 * Also need to check to see if the genomic ranges overlap, and trim accordingly if they do
	 * 
	 * @param ranges
	 */
	public static void trimRangesToRemoveOverlap(int[][] ranges, ChrPosition[] cps) {
		/*
		 * trim ranges if there is an overlap
		 */
		for (int j = 0 ; j < ranges.length - 1; j++) {
			if (j + 1 < ranges.length) {
				int [] thisIntArray = ranges[j];
				int [] nextIntArray = ranges[j + 1];
				ChrPosition thisCP = cps[j];
				ChrPosition nextCP = cps[j + 1];
				
				int diff = (thisIntArray[0] + thisIntArray[1]) - nextIntArray[0];
				if (diff > 0) {
					if ( thisIntArray[1] >= nextIntArray[1]) {
						/*
						 * update this, take diff away from length
						 */
						thisIntArray[1] = thisIntArray[1] - diff;
						if (diff >= thisCP.getLength()) {
							logger.warn("Diff is greater the cp length! diff: " + diff + ", cp length: " + thisCP.getLength());
							for (int [] range : ranges) {
								logger.warn("range: " + Arrays.toString(range));
							}
							for (ChrPosition cp : cps) {
								logger.warn("cp: " + cp.toIGVString());
							}
						} else {
							cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition(), thisCP.getEndPosition() - diff, thisCP.getName());
						}
					} else {
						/*
						 * update next array, take diff away from start (and length)
						 */
						nextIntArray[0] = nextIntArray[0] + diff;
						nextIntArray[1] = nextIntArray[1] - diff;
						
						if (diff >= nextCP.getLength()) {
							logger.warn("Diff is greater the cp length! diff: " + diff + ", cp length: " + nextCP.getLength());
							for (int [] range : ranges) {
								logger.warn("range: " + Arrays.toString(range));
							}
							for (ChrPosition cp : cps) {
								logger.warn("cp: " + cp.toIGVString());
							}
						} else {
						
							cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition() + diff, nextCP.getEndPosition(), nextCP.getName());
						}
					}
				} else {
					/*
					 * now need to check that the reference positions don't overlap (ChrPosition array)
					 * ALSO need to take into account whether the sequence was reverse complemented
					 */
					boolean reverseComplemented = "R".equals(thisCP.getName());
					int diffRef = reverseComplemented ? nextCP.getEndPosition() - thisCP.getStartPosition() :  thisCP.getEndPosition() - nextCP.getStartPosition();
					if (diffRef > 0) {
						if ( thisIntArray[1] >= nextIntArray[1]) {
							/*
							 * update this, take diff away from length
							 */
							thisIntArray[1] = thisIntArray[1] - diffRef;
							if (diffRef >= thisCP.getLength()) {
								logger.warn("Diff is greater the cp length! diff: " + diffRef + ", cp length: " + thisCP.getLength());
								for (int [] range : ranges) {
									logger.warn("range: " + Arrays.toString(range));
								}
								for (ChrPosition cp : cps) {
									logger.warn("cp: " + cp.toIGVString());
								}
							} else {
								if (reverseComplemented) {
									cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition() + diffRef, thisCP.getEndPosition(), thisCP.getName());
								} else {
									cps[j] = new ChrPositionName(thisCP.getChromosome(), thisCP.getStartPosition(), thisCP.getEndPosition() - diffRef, thisCP.getName());
								}
							}
						} else {
							/*
							 * update next array, take diff away from start (and length)
							 */
							nextIntArray[0] = nextIntArray[0] + diffRef;
							nextIntArray[1] = nextIntArray[1] - diffRef;
							
							if (diffRef >= nextCP.getLength()) {
								logger.warn("Diff is greater the cp length! diff: " + diffRef + ", cp length: " + nextCP.getLength());
								for (int [] range : ranges) {
									logger.warn("range: " + Arrays.toString(range));
								}
								for (ChrPosition cp : cps) {
									logger.warn("cp: " + cp.toIGVString());
								}
							} else {
								if (reverseComplemented) {
									cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition(), nextCP.getEndPosition() - diffRef, nextCP.getName());
								} else {
									cps[j + 1] = new ChrPositionName(nextCP.getChromosome(), nextCP.getStartPosition() + diffRef, nextCP.getEndPosition(), nextCP.getName());
								}
							}
						}
						
					}
				}
			}			
		}
	}
	
	/**
	 * tile counts hold a combination of tile matches and mismatch count.
	 * Need to take the mismatch count away from the tile count.
	 * 
	 * Favour tile counts that have zero mismatches over those that have mismatches
	 * eg.
	 * tile count of 40 is better than tile count of 41 with a mismatch count of 1.
	 * 
	 * @param tileCounts
	 * @return
	 */
	public static int[] sortTileCount(int[] tileCounts) {
		
		return Arrays.stream(tileCounts)
				.mapToObj(k -> NumberUtils.splitIntInto2(k))
				.sorted((a,b) -> {int diff = Integer.compare((a[0] - a[1]), (b[0] - b[1]));
				                if (diff == 0) {
				                	diff = b[1] - a[1];
				                }
								return diff;})
				.mapToInt(a -> NumberUtils.pack2IntsInto1(a[0], a[1]))
				.toArray();
	}
	
	
	public static TIntObjectMap<Set<IntLongPairs>> getSplitStartPositions(TARecord record) {
		TIntObjectMap<Set<IntLongPairs>> results = new TIntObjectHashMap<>();
		TIntObjectMap<TLongList> countsAndStartPositions = record.getCounts();
		
		if (countsAndStartPositions.size() > 1 || (countsAndStartPositions.size() == 1 && countsAndStartPositions.get(countsAndStartPositions.keys()[0]).size() > 1 )) {
		
			int [] keys = countsAndStartPositions.keys();
			keys = sortTileCount(keys);
			int maxTileCount = getLengthFromPackedInt(keys[keys.length - 1]);
			int seqLength = record.getSequence().length();
			
			/*
			 * if our max tile count is less than 1/3 (randomly plucked..) then don't proceed as we will just end up with a range of splits that don't cover much of the sequence.
			 */
			if (maxTileCount >= (seqLength / 4)) {
			
				/*
				 * we don't want to look for splits right down in the weeds (where the tile count is low, but the number of start positions are high), as there will likely be plenty of low quality splits
				 * and so set a limit for the starting split - it should be in the top 3 (say) tile counts 
				 */
				int minTileCountCutoff = Math.max(keys.length - 3, 0);
				boolean areWeDone = false;
				boolean checkResults = true;
				for (int i = keys.length - 1 ; i >= minTileCountCutoff ; i--) {
					if (areWeDone) {
						break;
					}
					int tileCountAndCommon = keys[i];
					int tileCount = NumberUtils.getPartOfPackedInt(tileCountAndCommon, true);
					/*
					 * only proceed if tileCount and tile length would give a score of 20
					 */
					if (tileCount >= MIN_TILE_COUNT) {
						TLongList list =  countsAndStartPositions.get(tileCountAndCommon);
						for (int j = 0 ; j < list.size() ; j ++) {
							long l = list.get(j);
							boolean isForwardStrand =  ! NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
							short tilePositionInSequence = NumberUtils.getShortFromLong(l, TILE_OFFSET);
							long positionInGenome = NumberUtils.getLongPositionValueFromPackedLong(l);
							long positionInGenomeEnd = positionInGenome + tileCount + (TILE_LENGTH - 1);
							
							/*
							 * see if there are any possible ranges
							 */
							List<int[]> ranges = getPossibleTileRanges(seqLength, tilePositionInSequence, TILE_LENGTH, tileCount, MIN_BLAT_SCORE_MINUS_RANGE_BUFFER,  ! isForwardStrand);
							if ( ! ranges.isEmpty()) {
								/*
								 * deal with 2 scenarios here
								 * First is where we have a single range. In this case if a match is found, and there is still room for another match, another attempt at a match is made
								 * Second case is where we have 2 ranges.
								 * 
								 * Both of these scenarios could result in IntLongPairs that contain 3 IntLongPair objects
								 */
								
								if (ranges.size() == 1) {
									List<IntLongPair> resultsForRange = getPositionsThatFitInRange(ranges.get(0), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									if ( ! resultsForRange.isEmpty()) {
										
										IntLongPairs pairs = getILPSFromLists(resultsForRange, null, new IntLongPair(tileCountAndCommon, l));
												
										/*
										 * check to see if we could fit an additional bit of sequence in here
										 */
										int[][] remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
										if (remainingRanges.length > 0) {
//												System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
											for (int[] remainingRange : remainingRanges) {
												List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
												if ( ! resultsForRemainingRange.isEmpty()) {
													IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
												}
											}
											
											/*
											 * go again....
											 */
											remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
											if (remainingRanges.length > 0) {
//													System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
												for (int[] remainingRange : remainingRanges) {
													List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
													if ( ! resultsForRemainingRange.isEmpty()) {
														
														/*
														 * sort and take largest again
														 */
														IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
													}
												}
											}
										}
										
										/*
										 * check that pairs is not a subset of existing pairs
										 */
										if ( ! IntLongPairsUtil.isPairsASubSetOfExistingPairs(getPairsFromMap(results), pairs)) {
											Set<IntLongPairs> resultsListList = results.putIfAbsent(IntLongPairsUtil.getBasesCoveredByIntLongPairs(pairs, seqLength, TILE_LENGTH), new HashSet<>(Arrays.asList(pairs)));
											if (null != resultsListList) {
												resultsListList.add(pairs);
											}
											checkResults = true;
										}
									}
							
								} else if (ranges.size() == 2) {
									/*
									 * 3 IntLongPair objects here
									 * create the IntLongPairs object with ordered INtLongPair objects - makes the IntLongPairs.equals() and hashcode() valid
									 */
									List<IntLongPair> resultsForRange1 = getPositionsThatFitInRange(ranges.get(0), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									List<IntLongPair> resultsForRange2 = getPositionsThatFitInRange(ranges.get(1), positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
									if ( ! resultsForRange1.isEmpty() ||  ! resultsForRange2.isEmpty()) {
										IntLongPairs pairs = getILPSFromLists(resultsForRange1, resultsForRange2, new IntLongPair(tileCountAndCommon, l));
										
										
										/*
										 * check to see if we could fit an additional bit of sequence in here
										 */
										int[][] remainingRanges = getRemainingRangeFromIntLongPairs(pairs, seqLength);
										if (remainingRanges.length > 0) {
//												System.out.println("Found " + remainingRanges.length + " remaining ranges for pairs: " + pairs.toString() + ", remaining ranges[0]: " + Arrays.toString(remainingRanges[0]));
											for (int[] remainingRange : remainingRanges) {
												List<IntLongPair> resultsForRemainingRange = getPositionsThatFitInRange(remainingRange, positionInGenome, positionInGenomeEnd, countsAndStartPositions, keys, TILE_LENGTH, seqLength, i);
												if ( ! resultsForRemainingRange.isEmpty()) {
													
													/*
													 * no need to sort as they are added to the list in a sorted manner
													 */
													IntLongPairsUtil.addBestILPtoPairs(pairs, resultsForRemainingRange);
												}
											}
										}
										
										/*
										 * check that pairs is not a subset of existing pairs
										 */
										if ( ! IntLongPairsUtil.isPairsASubSetOfExistingPairs(getPairsFromMap(results), pairs)) {
											Set<IntLongPairs> resultsListList = results.putIfAbsent(IntLongPairsUtil.getBasesCoveredByIntLongPairs(pairs, seqLength, TILE_LENGTH), new HashSet<>(Arrays.asList(pairs)));
											if (null != resultsListList) {
												resultsListList.add(pairs);
											}
											checkResults = true;
										}
									}
								}
							}
						}
					}
				}
					
					
				/*
				 * check results, if we have covered all bases in seqLength with our splits, no need to go looking for more
				 */
				if (checkResults &&  ! results.isEmpty()) {
					for (int key : results.keys()) {
						if (key == seqLength) {
							/*
							 * done
							 */
							areWeDone = true;
							break;
						}
					}
					checkResults = false;
				}
			}
		}
		return results;
	}
	
	public static List<IntLongPairs> getPairsFromMap(TIntObjectMap<Set<IntLongPairs>> map) {
		List<IntLongPairs> list = new ArrayList<>();
		map.forEachValue(s -> list.addAll(s));
		return list;
	}
	
	public static IntLongPairs getILPSFromLists(List<IntLongPair> list1, List<IntLongPair> list2, IntLongPair pair) {
		List<IntLongPair> results = new ArrayList<>(4);
		results.add(pair);
		Optional<IntLongPair> list1ILP = getBestILPFromList(list1, pair);
		list1ILP.ifPresent(ilp -> results.add(ilp));
		Optional<IntLongPair> list2ILP = getBestILPFromList(list2, pair);
		list2ILP.ifPresent(ilp -> results.add(ilp));
		results.sort(null);
		return new IntLongPairs(results.toArray(new IntLongPair[]{}));
	}
	
	/**
	 * This will only return an OPtional that is not empty if it is within <code>MAX_GAP_FOR_SINGLE_RECORD</code> bases of the primary <code>IntLongPair</code>
	 * 
	 * @param list
	 * @param p1 Primary <code>IntLongPair</code>
	 * @return
	 */
	public static Optional<IntLongPair> getBestILPFromList(List<IntLongPair> list, IntLongPair p1) {
		/*
		 * check list, if only 1 entry, easy.
		 * If more than one, sort by ability to make single record, tile count, and then by strand and closeness to originating IntLongPair (p1 here)
		 */
		if (null != list && ! list.isEmpty()) {
			if (list.size() > 1) {
			
				/*
				 * sort by tile count, strand, and location
				 */
				boolean p1OnForwardStrand = NumberUtils.isBitSet(p1.getLong(), REVERSE_COMPLEMENT_BIT);
				long p1Position = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
				list.sort((IntLongPair ilp1, IntLongPair  ilp2) -> {
					/*
					 * valid for single record first
					 */
					boolean ilp1Valid = IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p1, ilp1);
					boolean ilp2Valid = IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p1, ilp2);
					if (ilp1Valid && ! ilp2Valid) {
						return -1;
					} else 	if ( ! ilp1Valid && ilp2Valid) {
						return 1;
					}
					
					/*
					 * tile count
					 */
					int [] ilp1Array = NumberUtils.splitIntInto2(ilp1.getInt());
					int [] ilp2Array = NumberUtils.splitIntInto2(ilp2.getInt());
					int diff = 	Integer.compare(ilp2Array[0] - ilp2Array[1], ilp1Array[0] - ilp1Array[1]);
					if (diff == 0) {
						diff = ilp1Array[1] - ilp2Array[1];
					}
					if (diff != 0) {
						return diff;
					}
					
					/*
					 * strand
					 */
					if ( NumberUtils.isBitSet(ilp1.getLong(), REVERSE_COMPLEMENT_BIT) == p1OnForwardStrand
							&& NumberUtils.isBitSet(ilp2.getLong(), REVERSE_COMPLEMENT_BIT) != p1OnForwardStrand) {
						diff = -1;
					} else if ( NumberUtils.isBitSet(ilp2.getLong(), REVERSE_COMPLEMENT_BIT) == p1OnForwardStrand
							&& NumberUtils.isBitSet(ilp1.getLong(), REVERSE_COMPLEMENT_BIT) != p1OnForwardStrand) {
						diff = 1;
					}
					if (diff != 0) {
						return diff;
					}
					/*
					 * proximity to original ILP
					 */
					long ilp1Diff = Math.abs( NumberUtils.getLongPositionValueFromPackedLong(ilp1.getLong()) - p1Position);
					long ilp2Diff = Math.abs( NumberUtils.getLongPositionValueFromPackedLong(ilp2.getLong()) - p1Position);
					return (ilp1Diff < ilp2Diff) ? -1 : 1;
				
				});
			}
			
			/*
			 * if leading candidate is too far away from original entry, return empty optional
			 */
			IntLongPair ilp = list.get(0);
			return Optional.of(ilp);
			
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * 
	 * @param ilp
	 * @param seqLength
	 * @return
	 */
	public static int[][] getRemainingRangeFromIntLongPairs(IntLongPairs ilp, int seqLength) {
		return getRemainingRangeFromIntLongPairs(ilp, seqLength, TILE_LENGTH);
	}
	public static int[][] getRemainingRangeFromIntLongPairs(IntLongPairs ilp, int seqLength, int tileLength) {
		
		int tileLengthMinusOne = tileLength - 1;
		/*
		 * get tile start position and length for each IntLongPair
		 */
		
		int basesCovered = IntLongPairsUtil.getBasesCoveredByIntLongPairs(ilp, seqLength, tileLength);
		
		if (seqLength - basesCovered > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
			IntLongPair[] pairs = IntLongPairsUtil.sortIntLongPairs(ilp, seqLength);
			
			
			/*
			 * now loop through and add to list if there the gap between ranges or boundaries is large enough
			 */
			List<int[]> ranges = new ArrayList<>(4);
			int lastEndPosition = 0;
			for (IntLongPair pair : pairs) {
				
				int startPosition = NumberUtils.getShortFromLong(pair.getLong(), TILE_OFFSET);
				boolean reverseStrand = NumberUtils.isBitSet(pair.getLong(), REVERSE_COMPLEMENT_BIT);
				int tileCount = NumberUtils.getPartOfPackedInt(pair.getInt(), true);
				if (reverseStrand) {
					int [] forwardStrandStartAndStopPositions = getForwardStrandStartAndStop(startPosition, tileCount, TILE_LENGTH, seqLength);
					startPosition = (short) forwardStrandStartAndStopPositions[0];
				}
				
				if (startPosition - lastEndPosition > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
					ranges.add(new int[]{lastEndPosition, startPosition - 1});
				}
				
				lastEndPosition = startPosition + tileCount + tileLengthMinusOne;
			}
			
			if (seqLength - lastEndPosition > MIN_BLAT_SCORE_MINUS_RANGE_BUFFER) {
				ranges.add(new int[]{lastEndPosition, seqLength});
			}
			return ranges.toArray(new int[][]{});
		}
		
		return new int[][]{};
	}
	
	
	/**
	 * Given a length, a tile start position, the tile length, the tile count and the minimum acceptable size of a chunk, 
	 * this method will return a list of ranges that correspond to the parts of the length that are not covered by this match
	 * 
	 * For example, if the length of the sequence is 100, the match that we are dealing with is 50 in length, starting at position 0
	 * then there will be one range returned and it will start at 50 + tile length (63 typically) and end ot the length - 1.
	 * 
	 * If any unrepresented regions are smaller than the minLength value, then they will not be returned as a range.
	 * It is possible that an empty lit is returned and this would tell that user that there was not space for another match at either the start or end of the sequence.
	 * 
	 * The ranges will be made up of an int array with 2 elements.
	 * The first element is the start position of any potential matching region
	 * The second element is the end position of any potential matching region
	 * 
	 * eg. consider the following:
	 * length = 100
	 * startTilePosition = 30
	 * tileLength = 13
	 * tileCount = 20
	 * minLength = 20
	 * 
	 * In this instance, we should have 2 ranges returned.
	 * The first would correspond to the gap at the beginning of the sequence (int[]{0,29})
	 * and the second would correspond to the gap at the end of the sequence (int[]{30 + 13 + 20 ,99})
	 * 
	 * @param totalLength
	 * @param startTilePosition
	 * @param tileLength
	 * @param tileCount
	 * @param minLength
	 * @return
	 */
	public static List<int[]> getPossibleTileRanges(int totalLength, int startTilePosition, int tileLength, int tileCount, int minLength) {
		return getPossibleTileRanges(totalLength, startTilePosition, tileLength, tileCount, minLength, false);
	}
	public static List<int[]> getPossibleTileRanges(int totalLength, int startTilePosition, int tileLength, int tileCount, int minLength, boolean reverseStrand) {
		
		return getPossibleTileRanges(totalLength, startTilePosition, tileLength + tileCount, minLength, reverseStrand);
	}
	
	public static List<int[]> getPossibleTileRanges(int totalLength, int startPosition, int length, int minLength, boolean reverseStrand) {
		List<int[]> results = new ArrayList<>(3);
		
		/*
		 * check to see if there is space for a range before the startTilePosition
		 */
		int endPosition = startPosition + length;
		if (reverseStrand) {
			/*
			 * startTilePosition is now end position
			 * and start position is tileCount + tileLength from the end
			 */
			endPosition = totalLength - startPosition;
			startPosition = totalLength - (startPosition + length);
		}
		
		
		if (startPosition >= (minLength)) {
			results.add(new int[]{0, startPosition - 1});
		}
		if (endPosition < (totalLength - minLength)) {
			results.add(new int[]{endPosition, totalLength});
		}
		
		return results;
	}
	
	public static List<IntLongPair> getPositionsThatFitInRange(int[] range, long genomicPositionStart, long genomicPositionEnd, TIntObjectMap<TLongList> countsAndStartPositions, int [] sortedKeys, int tileLength, int seqLength, int positionInArray) {
		List<IntLongPair> results = new ArrayList<>();
		int maxTileCount = range[1] - range[0] - (tileLength - 1);
		
		/*
		 * need to add a buffer to the maxTileCount
		 * as there are instances where there is an overlap
		 */
		
		int bufferToUse = (int)(seqLength  * 0.4) + 1;
		
		maxTileCount += bufferToUse;
		
		for (int i = positionInArray ; i >= 0 ; i--) {
			int tileCount = NumberUtils.getPartOfPackedInt(sortedKeys[i], true);
				if (tileCount >= MIN_TILE_COUNT && tileCount <= maxTileCount) {
				
				/*
				 * with the tile count, now need to work out possible start positions
				 */
				
				TLongList list =  countsAndStartPositions.get(sortedKeys[i]);
				for (int j = 0 ; j < list.size() ; j ++) {
					long l = list.get(j);
					boolean reverseStrand = NumberUtils.isBitSet(l, REVERSE_COMPLEMENT_BIT);
					short tileStartPosition = NumberUtils.getShortFromLong(l, TILE_OFFSET);
					
					
					if (reverseStrand) {
						int [] forwardStrandStartAndStopPositions = getForwardStrandStartAndStop(tileStartPosition, tileCount, tileLength, seqLength);
						tileStartPosition = (short) forwardStrandStartAndStopPositions[0];
						
					}
					if (doesPositionFitWithinRange(range, tileStartPosition, tileCount, bufferToUse)) {
						/*
						 * now need to check to see if the genomic coordinates have too great an overlap
						 * This happens when sequences contain repetitive regions
						 */
						long thisGenomicPositionStart = NumberUtils.getLongPositionValueFromPackedLong(l);
						long thisGenomicPositionEnd = thisGenomicPositionStart + tileCount + (tileLength - 1);
						
						int buffer = (int)Math.max((thisGenomicPositionEnd - thisGenomicPositionStart) / 2, (genomicPositionEnd - genomicPositionStart) / 2);
						
						if ( ! doGenomicPositionsOverlap(thisGenomicPositionStart, thisGenomicPositionEnd, genomicPositionStart, genomicPositionEnd, buffer)) {
							/*
							 * we have a keeper!
							 */
							results.add(new IntLongPair(sortedKeys[i], l));
						}
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * If the overlap is more than 50% of both ranges, return true
	 * 
	 * @param positionOneStart
	 * @param positionOneEnd
	 * @param positionTwoStart
	 * @param positionTwoEnd
	 * @param buffer
	 * @return
	 */
	public static boolean doGenomicPositionsOverlap(long positionOneStart, long positionOneEnd, long positionTwoStart, long positionTwoEnd, int buffer) {
		long overlap = NumberUtils.getOverlap(positionOneStart, positionOneEnd, positionTwoStart, positionTwoEnd);
		
		if ((positionOneEnd - positionOneStart) - overlap <= TILE_LENGTH) {
			return true;
		}
		
		return overlap >= buffer;
	}
	
	public static int getRangesOverlap(int [] range, int range2Start, int range2End) {
		long overlap = NumberUtils.getOverlap(range[0], range[1], range2Start, range2End);
		return (int)overlap;
	}
	
	public static boolean doesPositionFitWithinRange(int [] range, int startPosition, int tileCount, int buffer) {
		int rangesOverlap = getRangesOverlap(range, startPosition, startPosition + tileCount + (TILE_LENGTH - 1));
		/*
		 * no point in adding this position if it is not going to increase the coverage of the pairs
		 */
		if (rangesOverlap <= TILE_LENGTH) {
			return false;
		}
		
		return startPosition >= (range[0] - buffer)
				&& (startPosition + tileCount + TILE_LENGTH - 1) <= (range[1] + buffer);
	}
	
	public static int[] getForwardStrandStartAndStop(int tileStartPositionRS, int tileCount, int tileLength, int seqLength) {
		return getForwardStrandStartAndStop(tileStartPositionRS, tileCount, tileLength, seqLength, false);
	}
	public static int[] getForwardStrandStartAndStop(int tileStartPositionRS, int tileCount, int tileLength, int seqLength, boolean forwardStrand) {
		if (forwardStrand) {
			return new int[] {tileStartPositionRS, tileStartPositionRS + (tileCount + tileLength - 1)};
		} else {
			return new int[] {seqLength - (tileStartPositionRS + tileCount + tileLength - 1), seqLength - tileStartPositionRS};
		}
	}
	
	/**
	 * returns exact match tile count AND common occurring tile count
	 * @param packedInt
	 * @return
	 */
	public static int getLengthFromPackedInt(int packedInt) {
		return getLengthFromPackedInt(packedInt, TILE_LENGTH);
	}
	public static int getLengthFromPackedInt(int packedInt, int tileLength) {
		int tileCounts = NumberUtils.minusPackedInt(packedInt);
		return tileCounts + tileLength - 1;
	}
	
	/**
	 * Only take into account exact match count
	 * @param packedInt
	 * @return
	 */
	public static int getExactMatchOnlyLengthFromPackedInt(int packedInt) {
		return getExactMatchOnlyLengthFromPackedInt(packedInt, TILE_LENGTH);
	}
	public static int getExactMatchOnlyLengthFromPackedInt(int packedInt, int tileLength) {
		int tileCounts = NumberUtils.getPartOfPackedInt(packedInt, true);
		return tileCounts + tileLength - 1;
	}
}

