/**
 * 
 */
package org.qcmg.common.model;

import org.qcmg.common.util.Constants;

/**
 * This class aims to provide a useful representation of a mutation.
 * The alt String field is not final, and can be set and appended to.
 * It is deliberately not included in the hashcode and equals method, as it is useful to match on position and ref, whereas alts may differ
 * 
 * @author oliverh
 *
 */
public class ChrPositionRefAlt implements ChrPosition {
	
	private final ChrPositionName cpRef;
	private String alt;
	
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
	public void setAlt(String alt) {
		this.alt = alt;
	}
	public void addAlt(String alt) {
		this.alt += Constants.COMMA + alt;
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
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cpRef == null) ? 0 : cpRef.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChrPositionRefAlt other = (ChrPositionRefAlt) obj;
		if (cpRef == null) {
			if (other.cpRef != null)
				return false;
		} else if (!cpRef.equals(other.cpRef))
			return false;
		return true;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChrPositionRefAlt [cpRef=" + cpRef + ", alt=" + alt + "]";
	}

}
