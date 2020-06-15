package org.qcmg.qsv.tiledaligner;

import java.util.Arrays;

public class IntLongPairs {
	

	@Override
	public String toString() {
		return "IntLongPairs [pairs=" + Arrays.toString(pairs) + "]";
	}
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
	private IntLongPair[] pairs;
	
	public IntLongPairs(IntLongPair pair1, IntLongPair pair2) {
		pairs = new IntLongPair[2];
		pairs[0] = pair1;
		pairs[1] = pair2;
	}
	public IntLongPairs(IntLongPair[] pairs) {
		if (pairs.length < 2) {
			throw new IllegalArgumentException("attempt to instantiate IntLongPairs with array of invalid length! " + Arrays.deepToString(pairs));
		}
		/*
		 * maybe need to check that we are not adding an array that is not exactly 3 in length
		 */
		this.pairs = pairs;
	}
	
	/**
	 * Resize the array as we run at full capacity
	 * @param ilp
	 */
	public void addPair(IntLongPair ilp) {
		IntLongPair[] newArray = new IntLongPair[pairs.length + 1];
		System.arraycopy(pairs, 0, newArray, 0, pairs.length);
		pairs = newArray;
		pairs[pairs.length - 1] = ilp;
	}
	
	public IntLongPair[] getPairs() {
		return pairs;
	}
	
	public int getIntTally() {
		int tally = 0;
		for (IntLongPair ilp : pairs) {
			tally += ilp.getInt();
		}
		return tally;
	}
}
