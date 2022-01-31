/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IntLongPairs {

	@Override
	public String toString() {
		return "IntLongPairs [pairs=" + Arrays.toString(pairs) + "]";
	}
	
	public String toDetailedString() {
		return "ILPs [pairs=" + Arrays.stream(pairs).map(IntLongPair::toDetailedString).collect(Collectors.joining(",")) + "]";
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
		this.pairs = Arrays.copyOf(pairs, pairs.length);
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
}
