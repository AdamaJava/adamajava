package org.qcmg.common.model;

public class ChrStartPosition  implements Comparable<ChrStartPosition> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final String chr;
	private final int startPosition;
	
	public ChrStartPosition(String chr, int position) {
		this.chr = chr;
		this.startPosition = position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chr == null) ? 0 : chr.hashCode());
		result = prime * result + startPosition;
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
		ChrStartPosition other = (ChrStartPosition) obj;
		if (chr == null) {
			if (other.chr != null)
				return false;
		} else if (!chr.equals(other.chr))
			return false;
		if (startPosition != other.startPosition)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ChrStartPosition [chr=" + chr + ", startPosition="
				+ startPosition + "]";
	}

	@Override
	public int compareTo(ChrStartPosition o) {
		int chromosomeDiff = COMPARATOR.compare(chr, o.chr);
		if (chromosomeDiff != 0) {
			return chromosomeDiff;
		}
		return startPosition - o.startPosition;
	}

	public String getChr() {
		return chr;
	}

	public int getStartPosition() {
		return startPosition;
	}
}
