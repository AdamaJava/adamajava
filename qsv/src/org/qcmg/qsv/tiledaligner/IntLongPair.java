package org.qcmg.qsv.tiledaligner;

import java.util.Arrays;

import org.qcmg.common.util.NumberUtils;

public class IntLongPair implements Comparable<IntLongPair> {
	@Override
	public String toString() {
		return "IntLongPair [i=" + i + ", l=" + l + "]";
	}
	public String toDetailedString() {
		return "ILP [i=" + Arrays.toString(NumberUtils.splitIntInto2(i)) + ", l strand: " + NumberUtils.isBitSet(l, 62) + ", position in seq:" + NumberUtils.getShortFromLong(l, 40) + ", position: " + NumberUtils.getLongPositionValueFromPackedLong(l) + "]";
		
	}
	private final int i;
	private final long l;
	public IntLongPair(int i, long l) {
		this.i = i;
		this.l = l;
	}
	public int getInt() {
		return i;
	}
	public long getLong() {
		return l;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i;
		result = prime * result + (int) (l ^ (l >>> 32));
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
		IntLongPair other = (IntLongPair) obj;
		if (i != other.i)
			return false;
		if (l != other.l)
			return false;
		return true;
	}
	@Override
	public int compareTo(IntLongPair o) {
		if (i != o.i) {
			return Integer.compare(i, o.i);
		}
		if (l != o.l) {
			return Long.compare(l, o.l);
		}
		return 0;
	}
	
	

}
