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
public class ChrPosition  implements Comparable<ChrPosition> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final ChrStartPosition csp;
	
//	private final String chromosome;
//	private final int position;
	private final int endPosition;
	private final String name;

	
	/**
	 * Constructor that takes in the chromosome and a single position, which is used to populate the start and end positions
	 * 
	 * @param chromosome String representation of the genomic sequence/chromosome
	 * @param position int corresponding to the position within the specified sequence
	 */
	public ChrPosition(String chromosome, int position) {
		this(chromosome, position, position);
	}
	
	/**
	 * Constructor that takes in the chromosome and a start and stop (1-based) position
	 * 
	 * 
	 * @param chromosome
	 * @param position
	 * @param endPosition
	 */
	public ChrPosition(String chromosome, int position, int endPosition) {
		this(chromosome, position, endPosition, null);
	}
	
	public ChrPosition(String chromosome, int position, int endPosition, String name) {
		if (null == chromosome || chromosome.isEmpty()) throw new IllegalArgumentException("null or empty chromosome supplied to ChrPosition");
		if (endPosition < position) 
			throw new IllegalArgumentException("end position is before start position: chr: " + chromosome + ":" + position + "-" + endPosition);
		
		this.csp = new ChrStartPosition(chromosome, position);
//		this.chromosome = chromosome;
//		this.position = position;
		this.endPosition = endPosition;
		this.name = name;
	}
	
	/**
	 * Returns the chromosome that this positon relates to
	 * eg. chr1, MT, GL20000123.25
	 * 
	 * @return String chromosome 
	 */
	public String getChromosome() {
		return csp.getChr();
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Start position of this ChrPosition object 
	 * eg. 1234356
	 * 
	 * @return int corresponding to the start position
	 */
	public int getPosition() {
		return csp.getStartPosition();
	}
	
	/**
	 * End position of this ChrPosition object
	 * eg. 1234357
	 * @return int corresponding to the end position
	 */
	public int getEndPosition() {
		return endPosition;
	}
	
	public boolean isSinglePoint() {
		return getPosition() == endPosition;
	}
	
	public int getLength() {
		return (endPosition - getPosition()) + 1;
	}
	
	/**
	 * Compares the chromosome, then the start position, and finally the end position
	 */
	@Override
	public int compareTo(ChrPosition o) {
		int chromosomeDiff = COMPARATOR.compare(getChromosome(), o.getChromosome());
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		int positionDiff = getPosition() - o.getPosition();
		if (positionDiff != 0)
			return  positionDiff;
		
		int endpositionDiff = endPosition - o.endPosition;
		if (name == null) {
			return  endpositionDiff;
		} else {
			if (endpositionDiff != 0) {
				return  endpositionDiff;
			} else {
				return name.compareTo(o.name);
			}
		}
	}
	
	
	@Override
	public String toString() {
		return "ChrPosition [chromosome=" + getChromosome() + ", startPosition="
				+ getPosition() + ", endPosition=" + endPosition +  ", name= " + name + "]";
	}
	
	public String toIGVString() {
		return getChromosome() + ":" + getPosition() + "-" + endPosition;
	}
	public String toStartPositionString() {
		return csp.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((csp == null) ? 0 : csp.hashCode());
		result = prime * result + endPosition;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ChrPosition other = (ChrPosition) obj;
		if (csp == null) {
			if (other.csp != null)
				return false;
		} else if (!csp.equals(other.csp))
			return false;
		if (endPosition != other.endPosition)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
