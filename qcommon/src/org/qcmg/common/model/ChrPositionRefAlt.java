/**
 * 
 */
package org.qcmg.common.model;


import org.qcmg.common.util.Constants;

/**
 * This class aims to provide a useful representation of a mutation.
 * It is an immutable representation of a mutation
 * 
 * @author oliverh
 *
 */
public class ChrPositionRefAlt implements ChrPosition, Comparable<ChrPositionRefAlt> {
	
	private final ChrPositionName cpRef;
	private final String alt;
	
	public ChrPositionRefAlt(ChrPositionName cpn, String alt) {
		this.cpRef = cpn;
		this.alt = alt;
	}
	public ChrPositionRefAlt(String chr, int start, int end, String ref, String alt) {
		this(new ChrPositionName(chr, start, end, ref), alt);
	}
	

	public String getAlt() {
		return alt;
	}
	
	public String toTabSeperatedString() {
		return cpRef.getChromosome() + Constants.TAB + cpRef.getStartPosition() + Constants.TAB + cpRef.getName() + Constants.TAB +  alt;
	}
	
	
	/* (non-Javadoc)
	 * @see org.qcmg.common.model.ChrPosition#getChromosome()
	 */
	@Override
	public String getChromosome() {
		return cpRef.getChromosome();
	}

	/* (non-Javadoc)
	 * @see org.qcmg.common.model.ChrPosition#getStartPosition()
	 */
	@Override
	public int getStartPosition() {
		return cpRef.getStartPosition();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChrPositionRefAlt [cpRef=" + cpRef + ", alt=" + alt + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alt == null) ? 0 : alt.hashCode());
		result = prime * result + ((cpRef == null) ? 0 : cpRef.hashCode());
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
		ChrPositionRefAlt other = (ChrPositionRefAlt) obj;
		if (alt == null) {
			if (other.alt != null)
				return false;
		} else if (!alt.equals(other.alt))
			return false;
		if (cpRef == null) {
			if (other.cpRef != null)
				return false;
		} else if (!cpRef.equals(other.cpRef))
			return false;
		return true;
	}
	@Override
	public int compareTo(ChrPositionRefAlt o) {
		int diff = this.cpRef.compareTo(o.cpRef);
		if (diff == 0) {
			return this.alt.compareTo(o.alt);
		}
		return diff;
	}

}
