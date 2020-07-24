package org.qcmg.qsv.tiledaligner;

import java.util.Arrays;
import java.util.Comparator;

import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtils {
	
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
	
	public static IntLongPair[] sortIntLongPairs(IntLongPairs pairs, int seqLength) {
		return sortIntLongPairs(pairs, seqLength, TILE_LENGTH);
	}
	public static IntLongPair[] sortIntLongPairs(IntLongPairs pairs, int seqLength, int tileLength) {
		
		IntLongPair[] pairsArray = pairs.getPairs();
		/*
		 * sort pairs by position in sequence
		 */
		Arrays.sort(pairsArray, (Comparator<? super IntLongPair>) (IntLongPair o1, IntLongPair o2) -> {
				/*
				 * start positions in sequence are always reported on the forward strand, and so no need to do any reverse strand magic (I think)
				*/
//				return NumberUtils.getShortFromLong(o1.getLong(), SHORT_OFFSET_IN_LONG) -  NumberUtils.getShortFromLong(o2.getLong(), SHORT_OFFSET_IN_LONG);
				
				return getStartPositionInSequence(o1, seqLength) - getStartPositionInSequence(o2, seqLength); 
			
//				boolean olReverse = NumberUtils.isBitSet(o1.getLong(), REVERSE_COMPLEMENT_BIT);
//				boolean o2Reverse = NumberUtils.isBitSet(o2.getLong(), REVERSE_COMPLEMENT_BIT);
//				short olPositionInSeq = NumberUtils.getShortFromLong(o1.getLong(), SHORT_OFFSET_IN_LONG);
//				short o2PositionInSeq = NumberUtils.getShortFromLong(o2.getLong(), SHORT_OFFSET_IN_LONG);
////			
//				if (olReverse) {
//					int o1TileCount = NumberUtils.getPartOfPackedInt(o1.getInt(), true);
//					olPositionInSeq = (short) (seqLength - olPositionInSeq - (o1TileCount + tileLength - 1));
//				}
//				if (o2Reverse) {
//					int o2TileCount = NumberUtils.getPartOfPackedInt(o2.getInt(), true);
//					o2PositionInSeq = (short) (seqLength - o2PositionInSeq - (o2TileCount + tileLength - 1));
//				}
//				return olPositionInSeq - o2PositionInSeq;
//			
//				int olInt =  (olReverse ? seqLength - olPositionInSeq - (o1TileCount + tileLength - 1) : olPositionInSeq) ;
//				int o2Int =  (o2Reverse ? seqLength - o2PositionInSeq - (o2TileCount + tileLength - 1) : o2PositionInSeq);
//				return olInt - o2Int;
//				 (NumberUtils.isBitSet(o1.getLong(), REVERSE_COMPLEMENT_BIT) ? seqLength - NumberUtils.getShortFromLong(o1.getLong(), SHORT_OFFSET_IN_LONG) - (NumberUtils.getPartOfPackedInt(o1.getInt(), true) + tileLength) - 1 : NumberUtils.getShortFromLong(o1.getLong(), SHORT_OFFSET_IN_LONG)) 
//						- (NumberUtils.isBitSet(o2.getLong(), REVERSE_COMPLEMENT_BIT) ? seqLength - NumberUtils.getShortFromLong(o2.getLong(), SHORT_OFFSET_IN_LONG) - (NumberUtils.getPartOfPackedInt(o1.getInt(), true) + tileLength) - 1 : NumberUtils.getShortFromLong(o2.getLong(), SHORT_OFFSET_IN_LONG))
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

}
