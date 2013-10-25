package org.qcmg.common.model;

/**
 * Immutable class that refers to a genomic location
 * Can be used to represent SNV (single nucleotide variations), where the start and end positions would be the same, 
 * and also positions that span more than 1 base (eg. deletions, where the start and end positions are different)
 * <p>
 * Positions are inclusive
 *  
 * @author oholmes
 *
 */
public class ChrPosition  implements Comparable<ChrPosition> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private String chromosome;
	private int position;
	private int endPosition;
	private String name;
	
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
		if (null == chromosome || chromosome.isEmpty()) throw new IllegalArgumentException("null or empty chromosome supplied to ChrPosition");
		if (endPosition < position) throw new IllegalArgumentException("end position is before start position");
		
		this.chromosome = chromosome;
		this.position = position;
		this.endPosition = endPosition;
	}
	
	public ChrPosition(String chromosome, int position, int endPosition, String name) {
		if (null == chromosome || chromosome.isEmpty()) throw new IllegalArgumentException("null or empty chromosome supplied to ChrPosition");
		if (endPosition < position) throw new IllegalArgumentException("end position is before start position");
		
		this.chromosome = chromosome;
		this.position = position;
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
		return chromosome;
	}
	
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	
	/**
	 * Start position of this ChrPosition object 
	 * eg. 1234356
	 * 
	 * @return int corresponding to the start position
	 */
	public int getPosition() {
		return position;
	}
	
	/**
	 * End position of this ChrPosition object
	 * eg. 1234357
	 * @return int corresponding to the end position
	 */
	public int getEndPosition() {
		return endPosition;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}

	public void setEndPosition(int endPosition) {
		this.endPosition = endPosition;
	}
	
	public boolean isSinglePoint() {
		return position == endPosition;
	}
	
	/**
	 * Compares the chromosome, then the start position, and finally the end position
	 */
	@Override
	public int compareTo(ChrPosition o) {
		int chromosomeDiff = COMPARATOR.compare(chromosome, o.chromosome);
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		int positionDiff = position - o.position;
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
	public int hashCode() {
		
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((chromosome == null) ? 0 : chromosome.hashCode());
		result = prime * result + endPosition;
		result = prime * result + position;
		if (name != null) {
			result = prime * result + ((name == null) ? 0 : name.hashCode());
		}
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
		if (chromosome == null) {
			if (other.chromosome != null)
				return false;
		} else if (!chromosome.equals(other.chromosome))
			return false;
		if (endPosition != other.endPosition)
			return false;
		if (position != other.position)
			return false;
		if (name != null) {
			return name.equals(other.name);
		}
		return true;
	}
	@Override
	public String toString() {
		return "ChrPosition [chromosome=" + chromosome + ", startPosition="
				+ position + ", endPosition=" + endPosition + "]";
	}
	
	public String toIGVString() {
		return chromosome + ":" + position + "-" + endPosition;
	}

	public void setName(String name) {
		this.name = name;
		
	}
	
}
