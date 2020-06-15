package org.qcmg.qsv.tiledaligner;

import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtils {
	
	public static final int getBasesCoveredByIntLongPairs(IntLongPairs pairs, int tileLength) {
		int lengthTally = 0;
		if (null != pairs) {
			IntLongPair[] pairsArray = pairs.getPairs();
			for (IntLongPair ilp : pairsArray) {
				lengthTally += NumberUtils.getPartOfPackedInt(ilp.getInt(), true) + (tileLength - 1);
			}
		}
		return lengthTally;
	}

}
