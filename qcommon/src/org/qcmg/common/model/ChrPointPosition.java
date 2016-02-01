package org.qcmg.common.model;

public class ChrPointPosition  implements Comparable<ChrPointPosition> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final String chr;
	private final int position;
	
	public ChrPointPosition(String chr, int position) {
		if (null == chr || chr.isEmpty()) {
			throw new IllegalArgumentException("null or empty chromosome supplied to ChrPointPosition");
		}
		this.chr = chr;
		this.position = position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chr == null) ? 0 : chr.hashCode());
		result = prime * result + position;
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
		ChrPointPosition other = (ChrPointPosition) obj;
		if (chr == null) {
			if (other.chr != null)
				return false;
		} else if (!chr.equals(other.chr))
			return false;
		if (position != other.position)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ChrPointPosition [chr=" + chr + ", position="
				+ position + "]";
	}

	@Override
	public int compareTo(ChrPointPosition o) {
		int chromosomeDiff = COMPARATOR.compare(chr, o.chr);
		if (chromosomeDiff != 0) {
			return chromosomeDiff;
		}
		return position - o.position;
	}

	public String getChr() {
		return chr;
	}

	public int getPosition() {
		return position;
	}
}
