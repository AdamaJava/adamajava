/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.model;

public class ChrPointPosition  implements ChrPosition , Comparable<ChrPosition> {
	
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
	
	public static ChrPointPosition valueOf(String chr, int position) {
		return new ChrPointPosition(chr, position);
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
		return position == other.position;
	}

	@Override
	public String toString() {
		return "ChrPointPosition [chr=" + chr + ", position="
				+ position + "]";
	}

	@Override
	public int compareTo(ChrPosition o) {
		int chromosomeDiff = COMPARATOR.compare(chr, o.getChromosome());
		if (chromosomeDiff != 0) {
			return chromosomeDiff;
		}
		return position - o.getStartPosition();
	}

	@Override
	public String getChromosome() {
		return chr;
	}

	@Override
	public int getStartPosition() {
		return position;
	}

}
