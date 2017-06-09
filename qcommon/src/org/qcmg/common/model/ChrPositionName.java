/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

/**
 * Immutable class that refers to a genomic location
 * Can be used to represent SNV (single nucleotide variations), where the start and end positions would be the same, 
 * and also positions that span more than 1 base (eg. deletions, where the start and end positions are different)
 * 
 * Composed of a ChrStartPosition immutable object that contains just the chr name and start position
 * 
 * <p>
 * Positions are inclusive
 *  
 * @author oholmes
 *
 */
public class ChrPositionName extends ChrRangePosition implements ChrPosition {
	
	
	private final String name;

	public ChrPositionName(String chromosome, int position, int endPosition, String name) {
		super(chromosome, position, endPosition);
		this.name = name;
	}
	public ChrPositionName(String chromosome, int position, int endPosition) {
		this(chromosome, position, endPosition, null);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	
	@Override
	public String toString() {
		return "ChrPositionName [chromosome=" + getChromosome() + ", startPosition="
				+ getStartPosition() + ", endPosition=" + getEndPosition() +  ", name=" + name + "]";
	}
	
	@Override
	public String toIGVString() {
		return getChromosome() + ":" + getStartPosition() + "-" + getEndPosition();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChrPositionName other = (ChrPositionName) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
