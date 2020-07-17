package org.qcmg.qsv.tiledaligner;

import java.util.Arrays;
import java.util.Comparator;

import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtils {
	
	public static final int SHORT_OFFSET_IN_LONG = TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
	public static final int REVERSE_COMPLEMENT_BIT = TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
	public static final int TILE_LENGTH = TiledAlignerUtil.TILE_LENGTH;
	
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

}
