package org.qcmg.qsv.tiledaligner;

import java.util.Arrays;

public class IntLongPairs {
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(pairs);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntLongPairs other = (IntLongPairs) obj;
		if (!Arrays.equals(pairs, other.pairs))
			return false;
		return true;
	}
	/*
	 * limiting to 2 for now
	 */
	private final IntLongPair[] pairs = new IntLongPair[2];
	
	public IntLongPairs(IntLongPair pair1, IntLongPair pair2) {
		pairs[0] = pair1;
		pairs[1] = pair2;
//		Arrays.sort(pairs);
	}
	public IntLongPair[] getPairs() {
		return pairs;
	}
	
	

	
}
