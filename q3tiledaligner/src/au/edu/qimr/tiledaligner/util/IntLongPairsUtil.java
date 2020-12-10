/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import au.edu.qimr.tiledaligner.model.IntLongPair;
import au.edu.qimr.tiledaligner.model.IntLongPairs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtil {
	
	public static final int SHORT_OFFSET_IN_LONG = TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
	public static final int REVERSE_COMPLEMENT_BIT = TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
	public static final int TILE_LENGTH = TiledAlignerUtil.TILE_LENGTH;
	public static final int TILE_OFFSET = TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
	
	public static final int SINGLE_RECORD_MAX_GAP = 500000;
	
	/**
	 * 
	 * @param pairs
	 * @param tileLength
	 * @return
	 */
	public static final int getBasesCoveredByIntLongPairs(IntLongPairs pairs, int seqLength, int tileLength) {
		int lengthTally = 0;
		if (null != pairs) {
			
			IntLongPair[] pairsArray = sortIntLongPairs(pairs, seqLength);
			
			int lastStop = 0;
			for (IntLongPair ilp : pairsArray) {
				int start = getStartPositionInSequence(ilp, seqLength);
				int length = NumberUtils.getPartOfPackedInt(ilp.getInt(), true) + (tileLength - 1);
				int diff = 0;
				if (start < lastStop) {
					diff += (lastStop - start);
				}
				lengthTally += (length - diff);
				lastStop = length + start;
			}
		}
		return lengthTally;
	}
	
	/**
	 * Naive method that just calculates the number of matches of the constituent ILPs in the ILPS
	 * DOES NOT take into account any overlap, and so should not be used
	 * @param pairs
	 * @return
	 */
	public static final int getNaiveBasesCoveredByIntLongPairs(IntLongPairs pairs) {
		if (null != pairs) {
			int tally = 0;
			for (IntLongPair ilp : pairs.getPairs()) {
				tally += NumberUtils.minusPackedInt(ilp.getInt());
			}
			return tally;
		}
		return 0;
	}
	
	public static IntLongPair[] sortIntLongPairs(IntLongPairs pairs, int seqLength) {
		return sortIntLongPairs(pairs, seqLength, TILE_LENGTH);
	}
	public static IntLongPair[] sortIntLongPairs(IntLongPairs pairs, int seqLength, int tileLength) {
		
		IntLongPair[] pairsArray = pairs.getPairs();
		/*
		 * sort pairs by position in sequence
		 */
		Arrays.sort(pairsArray, (Comparator<? super IntLongPair>) (IntLongPair o1, IntLongPair o2) -> {
				
				return getStartPositionInSequence(o1, seqLength) - getStartPositionInSequence(o2, seqLength); 
			
		});
		return pairsArray;
	}
	
	public static IntLongPair[] sortIntLongPairsByPositionInSequence(IntLongPairs pairs) {
		
		IntLongPair[] pairsArray = pairs.getPairs();
		/*
		 * sort pairs by position in sequence
		 */
		Arrays.sort(pairsArray, (Comparator<? super IntLongPair>) (IntLongPair o1, IntLongPair o2) -> {
			/*
			 * start positioms in sequence are always reported on the forward strand, and so no need to do any reverse strand magic (I think)
			 */
				return NumberUtils.getShortFromLong(o1.getLong(), SHORT_OFFSET_IN_LONG) -  NumberUtils.getShortFromLong(o2.getLong(), SHORT_OFFSET_IN_LONG);
		});
		return pairsArray;
	}
	
	/**
	 * If the reverse complement bit has been set, need to flip the start position over, using the length of the sequence and the tile count
	 * 
	 * @param pair
	 * @param seqLength
	 * @return
	 */
	public static int getStartPositionInSequence(IntLongPair pair, int seqLength) {
		if (NumberUtils.isBitSet(pair.getLong(), REVERSE_COMPLEMENT_BIT)) {
			/*
			 * sequence length - reverse complement start position - fragment length
			 */
			return seqLength - NumberUtils.getShortFromLong(pair.getLong(), SHORT_OFFSET_IN_LONG) - ( NumberUtils.getPartOfPackedInt(pair.getInt(), true) + TILE_LENGTH - 1 );
		} else {
			/*
			 * simps - return the value stored for the start position within the sequence
			 */
			return NumberUtils.getShortFromLong(pair.getLong(), SHORT_OFFSET_IN_LONG);
		}
	}
	
	/**
	 * 
	 * Add the entry in the list of IntLongPair's that most suitably matches the existing IntLongPairs.
	 * Need to match on strand, sequence location and genomic location
	 * 
	 * If its the case that the existing IntLongPair elements in the IntLongPairs are not matching (ie. on different strands, or far apart), then the strand matching and genomic position matching are not important.
	 * Sequence matching should always be considered.
	 * 
	 * 
	 * @param pairs
	 * @param list
	 */
	public static void addBestILPtoPairs(IntLongPairs pairs, List<IntLongPair> list) {
		
		boolean potentialSingleSample = isIntLongPairsAValidSingleRecord(pairs.getPairs());
		
		if (potentialSingleSample) {
			IntLongPair [] expandedArray = Arrays.copyOf(pairs.getPairs(), pairs.getPairs().length + 1);
			
			for (IntLongPair ilp : list) {
				/*
				 * first check that this ilp is not already in the pair
				 */
				if (Arrays.binarySearch(pairs.getPairs(), ilp) < 0) {
					
					/*
					 * also check that this ilp is not totally encompassed by other ilps in this pair
					 */
					int potentialILPSeqStart = NumberUtils.getShortFromLong(ilp.getLong(), SHORT_OFFSET_IN_LONG);
					int potentialILPSeqEnd = potentialILPSeqStart + NumberUtils.getPartOfPackedInt(ilp.getInt(), true) + TILE_LENGTH - 1;
					int potentialBasesThisCouldFill = potentialILPSeqEnd - potentialILPSeqStart;
					for (IntLongPair pILP : pairs.getPairs()) {
						int thispILPSeqStart = NumberUtils.getShortFromLong(pILP.getLong(), SHORT_OFFSET_IN_LONG);
						int thispILPSeqEnd = thispILPSeqStart + NumberUtils.getPartOfPackedInt(pILP.getInt(), true) + TILE_LENGTH - 1;
						
						if (potentialILPSeqStart >= thispILPSeqEnd || potentialILPSeqEnd <= thispILPSeqStart) {
							/*
							 * all good
							 */
						} else {
							if (potentialILPSeqStart >= thispILPSeqStart && potentialILPSeqStart < thispILPSeqEnd) {
								potentialBasesThisCouldFill -= (thispILPSeqEnd - potentialILPSeqStart);
							} else if (potentialILPSeqEnd >= thispILPSeqStart && potentialILPSeqEnd < thispILPSeqEnd) {
								potentialBasesThisCouldFill -= (potentialILPSeqEnd - thispILPSeqStart);
							} else {
								potentialBasesThisCouldFill -= (thispILPSeqEnd - thispILPSeqStart);
							}
						}
						
						
					}
					
					if (potentialBasesThisCouldFill >= 10){
				
						expandedArray[pairs.getPairs().length] = ilp;
						if (isIntLongPairsAValidSingleRecord(expandedArray)) {
							pairs.addPair(ilp);
							/*
							 * resize expanded Array as there may be more that can fit!
							 */
							expandedArray = Arrays.copyOf(pairs.getPairs(), pairs.getPairs().length + 1);
						}
					}
				}
			}
		} else {
			/*
			 * just add the first one in list as it is sorted by size
			 */
			pairs.addPair(list.get(0));
		}
	}
	
	/**
	 * To be a valid single record, the following must hold:
	 * 
	 * all pairs:
	 *  must be on the same strand
	 *  be within 500 kbp of each other
	 *  be in both genomic and sequence order eg. If first pair is at sequence position 0, and genomic position 1000. Next pair must be sequence position > 0 and genomic position > 1000
	 * 
	 * @param pairs
	 * @return
	 */
	public static boolean isIntLongPairsAValidSingleRecord(IntLongPairs pairs) {
		return isIntLongPairsAValidSingleRecord(pairs.getPairs());
	}
	public static boolean isIntLongPairsAValidSingleRecord(IntLongPairs pairs, IntLongPair potentialNewPartOfPair) {
		IntLongPair[] pairsArray = pairs.getPairs();
		pairsArray = Arrays.copyOf(pairsArray, pairsArray.length + 1);
		pairsArray[pairsArray.length - 1] = potentialNewPartOfPair;
		return isIntLongPairsAValidSingleRecord(pairsArray);
	}
	
	public static boolean isIntLongPairsAValidSingleRecord(IntLongPair ... pairsArray) {
		
		boolean firstPositionIsRevComp = NumberUtils.isBitSet(pairsArray[0].getLong(), REVERSE_COMPLEMENT_BIT);
		long firstPosition = NumberUtils.getLongPositionValueFromPackedLong(pairsArray[0].getLong());
		boolean isValid = true;
		for (int i = 1 ; i < pairsArray.length ; i++) {
			if (NumberUtils.isBitSet(pairsArray[i].getLong(), REVERSE_COMPLEMENT_BIT) != firstPositionIsRevComp) {
				isValid = false;
				break;
			}
			long thisPosition = NumberUtils.getLongPositionValueFromPackedLong(pairsArray[i].getLong());
			if (Math.abs(thisPosition - firstPosition) > SINGLE_RECORD_MAX_GAP) {
				isValid = false;
				break;
			}
		}
		if (isValid) {
			
			/*
			 * final check here is to sort the positions genomically, and then make sure that each pair occurs in the correct order in the sequence
			 */
			Arrays.sort(pairsArray, (pair1, pair2) -> Long.compare(NumberUtils.getLongPositionValueFromPackedLong(pair1.getLong()), NumberUtils.getLongPositionValueFromPackedLong(pair2.getLong())));
			int lastSeqPosition = -1;
			for (IntLongPair pair : pairsArray) {
				int thisSeqPosition = NumberUtils.getShortFromLong(pair.getLong(), TILE_OFFSET);
				if (lastSeqPosition > -1) {
					if (thisSeqPosition < lastSeqPosition) {
						isValid = false;
						break;
					}
				}
				lastSeqPosition = thisSeqPosition;
			}
		}
		return isValid;
	}
	
	/**
	 * 
	 * Tallies up the mismatches in the supplied IntLongPairs object.
	 * Returns 0 if supplied IntLongPairs object is null/empty/contains no mismatches...
	 * 
	 * @param ilp
	 * @return
	 */
	public static int getMismatches(IntLongPairs ilp) {
		int mismatchTally = 0;
		if (null != ilp) {
			IntLongPair[] array = ilp.getPairs();
			for (IntLongPair a : array) {
				mismatchTally += NumberUtils.getPartOfPackedInt(a.getInt(), false);
			}
		}
		return mismatchTally;
	}
	
	/**
	 * This method will take an existing IntLongPairs object and try to make a single BLAT record from it.
	 * It may be that all elements in the IntLongPairs object contribute towards the single BLAT record, and it may be that none of them do (in which case an empty Optional will be returned).
	 * 
	 * @param existingPairs
	 * @return
	 */
	public static Optional<IntLongPairs> getSingleBLATRecordFromILPs(IntLongPairs existingPairs) {
		
		if (isIntLongPairsAValidSingleRecord(existingPairs)) {
			return Optional.of(existingPairs);
		}
		
		IntLongPair[] constituents = existingPairs.getPairs();
		Arrays.sort(constituents);
		
		List<IntLongPairs> results = new ArrayList<>();
		
		for (int i = constituents.length - 1; i >= 0 ; i--) {
			IntLongPairs newPairs = null;
			IntLongPair ilp = constituents[i];
			for (int j = i - 1 ; j >= 0 ; j--) {
				IntLongPair ilp2 = constituents[j];
				if (null == newPairs) {
					if (isIntLongPairsAValidSingleRecord(ilp, ilp2)) {
						newPairs = new IntLongPairs(ilp, ilp2);
					}
				} else {
					if (isIntLongPairsAValidSingleRecord(newPairs, ilp2)) {
						newPairs.addPair(ilp2);
					}
				}
			}
			if (newPairs != null) {
				results.add(newPairs);
			}
		}
		
		/*
		 * check the results list
		 * return the one with the highest coverage
		 */
		if (results.size() == 1) {
			return Optional.of(results.get(0));
		} else if (results.size() > 1) {
			results.sort((ilp1, ilp2) -> Integer.compare(getNaiveBasesCoveredByIntLongPairs(ilp1), getNaiveBasesCoveredByIntLongPairs(ilp2)));
			return Optional.of(results.get(results.size() - 1));
		}
		
		return Optional.empty();
	}

	/**
	 * This method will return true if the potentialPair is part of a larger pair in the supplied existingPairs list
	 * 
	 * eg. potentialPair could be [IntLongPair(1,100), IntLongPair(2,200)]
	 * and in the existingPairs list could be [IntLongPair(1,100), IntLongPair(2,200), IntLongPair(3,300)]
	 * 
	 * If this was the case, then true would be returned.
	 * 
	 * @param existingPairs
	 * @param potentialPair
	 * @return
	 */
	public static boolean isPairsASubSetOfExistingPairs(List<IntLongPairs> existingPairs, IntLongPairs potentialPair) {
		
		for (IntLongPairs existingPair : existingPairs) {
			
			/*
			 * need to sort, or binary search will not work
			 */
			Arrays.sort(existingPair.getPairs());
			int matchesThisPair = 0;
			for (IntLongPair ilp : potentialPair.getPairs()) {
				if (Arrays.binarySearch(existingPair.getPairs(), ilp) >= 0) {
					matchesThisPair++;
				}
			}
			if (matchesThisPair == potentialPair.getPairs().length) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isILPinILPS(IntLongPairs pairs, IntLongPair ilp) {
		for (IntLongPair pairsILP : pairs.getPairs()) {
			if (pairsILP.equals(ilp)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Lists the ILPs that were in the original ILPS but not in the single record ILPS
	 * @param originalILPS
	 * @param singleRecordILPS
	 * @return
	 */
	public static List<IntLongPair> getRejectedILPs(IntLongPairs originalILPS, IntLongPairs singleRecordILPS) {
		List<IntLongPair> rejects = new ArrayList<>();
		
		for (IntLongPair ilp : originalILPS.getPairs()) {
			if ( ! isILPinILPS(singleRecordILPS, ilp)) {
				rejects.add(ilp);
			}
		}
		return rejects;
	}
}
