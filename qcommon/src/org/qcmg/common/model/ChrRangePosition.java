/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
public class ChrRangePosition  implements ChrPosition , Comparable<ChrPosition> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	private final ChrPointPosition cpp;
	private final int endPosition;


	
	public ChrRangePosition(ChrPosition cpp, int endPosition) {
		if (endPosition < cpp.getStartPosition()) {
			throw new IllegalArgumentException("end position: "+ endPosition + " is before start position: " + cpp.getStartPosition());
		}
		if (cpp instanceof ChrPointPosition) {
			this.cpp = (ChrPointPosition) cpp;
		} else {
			this.cpp = ChrPointPosition.valueOf(cpp.getChromosome(), cpp.getStartPosition());
		}
		this.endPosition = endPosition;
	}
	
	public ChrRangePosition(ChrPosition cp ) {		
		this(cp, cp.getEndPosition());
	}	
	
		
	/**
	 * create an instance
	 * @param chromosome: variant occurred reference name
	 * @param position : variant start position
	 * @param endPosition: variant end position
	 * @return
	 */
	public ChrRangePosition(String chromosome, int position, int endPosition) {		
		this( ChrPointPosition.valueOf(chromosome, position), endPosition);
	}

	/**
	 * create an instance with same start and end position
	 * @param chromosome
	 * @param position: same for start and end position
	 * @return
	 */
//	public ChrRangePosition(String chromosome, int position ) {		
//		this( ChrPointPosition.valueOf(chromosome, position), position);
//	}
	
	
	
	/**
	 * @return String chromosome 
	 */
	@Override
	public String getChromosome() {
		return cpp.getChromosome();
	}
	
	
	/**
	 * 
	 * @return int corresponding to the start position
	 */
	@Override
	public int getStartPosition() {
		return cpp.getStartPosition();
	}
	
	public ChrPosition getChrPointPosition() {
		return cpp;
	}
	
	/**
	 * @return  corresponding to the variant end position
	 */
	@Override
	public int getEndPosition() {
		return endPosition;
	}
	
	
	/**
	 * Compares the chromosome, then the start position, and finally the end position
	 */
	@Override
	public int compareTo(ChrPosition o) {
		int chromosomeDiff = COMPARATOR.compare(getChromosome(), o.getChromosome());
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		int positionDiff = getStartPosition() - o.getStartPosition();
		if (positionDiff != 0)
			return  positionDiff;
		
		return  endPosition - o.getEndPosition();
	}
	
	
	@Override
	public String toString() {
		return "ChrRangePosition [chromosome=" + getChromosome() + ", startPosition="
				+ getStartPosition() + ", endPosition=" + endPosition +  "]";
	}
	
	@Override
	public String toIGVString() {
		return getChromosome() + ":" + getStartPosition() + "-" + endPosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cpp == null) ? 0 : cpp.hashCode());
		result = prime * result + endPosition;
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
		ChrRangePosition other = (ChrRangePosition) obj;
		if (cpp == null) {
			if (other.cpp != null)
				return false;
		} else if (!cpp.equals(other.cpp))
			return false;
		if (endPosition != other.endPosition)
			return false;
		return true;
	}

	
}
