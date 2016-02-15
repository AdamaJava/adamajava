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

	public ChrRangePosition(ChrPointPosition cpp, int endPosition) {
		if (endPosition < cpp.getStartPosition()) {
			throw new IllegalArgumentException("end position: "+ endPosition + " is before start position: " + cpp.getStartPosition());
		}
		this.cpp = cpp;
		this.endPosition = endPosition;
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
	public ChrRangePosition(String chromosome, int position ) {		
		this( ChrPointPosition.valueOf(chromosome, position), position);
	}
	
	/**
	 * convert ChrPosition to ChrRangePosition
	 * @param cp: it can be ChrPointPosition, ChrRangePosition
	 * @return a ChrRangePosition
	 */
	public static ChrRangePosition valueOf(ChrPosition cp){
		if(cp.isPointPosition())
			return new ChrRangePosition((ChrPointPosition) cp, cp.getEndPosition()); 
		else if(cp.isRangPosition())
			return (ChrRangePosition) cp;		 
		return  new ChrRangePosition((ChrPointPosition) cp, cp.getEndPosition()); 		
	}

	/**
	 * create an instance with new endPosition
	 * @param cp: it can be ChrPointPosition, ChrRangePosition
	 * @param endPosition: endPosition will be replace by this value
	 * @return
	 */
	public static ChrRangePosition valueOf(ChrPosition cp, int endPosition){
		if(cp.isPointPosition())
			return new ChrRangePosition((ChrPointPosition) cp,  endPosition); 
		 
		return  new ChrRangePosition(cp.getChromosome(), cp.getStartPosition(), endPosition); 		
	}
	

	
	
//	public ChrRangePosition(String chromosome, int position) {
//		this(ChrPointPosition.valueOf(chromosome, position));
//	}
//	
//	public ChrRangePosition(ChrPointPosition cpp) {
//		this(cpp, cpp.getStartPosition());
//	}	
//	
//	public ChrRangePosition(String chromosome, int position, int endPosition) {
//		this(ChrPointPosition.valueOf(chromosome, position), endPosition);
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
	
	public ChrPointPosition getChrStartpos() {
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
	public String toStartPositionString() {
		return cpp.toString();
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
