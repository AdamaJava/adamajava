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
 * <p>
 * Positions are inclusive
 *  
 * @author oholmes
 *
 */
public class ChrPositionMutation  implements Comparable<ChrPositionMutation> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final String chromosome;
	private final int position;
	private final char mutation;
	
	public ChrPositionMutation(String chromosome, int position, char mutation) {
		if (null == chromosome || chromosome.isEmpty()) throw new IllegalArgumentException("null or empty chromosome supplied to ChrPosition");
		
		this.chromosome = chromosome;
		this.position = position;
		this.mutation = mutation;
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
	 * Compares the chromosome, then the start position, and finally the end position
	 */
	@Override
	public int compareTo(ChrPositionMutation o) {
		int chromosomeDiff = COMPARATOR.compare(chromosome, o.chromosome);
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		int positionDiff = position - o.position;
		if (positionDiff != 0)
			return  positionDiff;
		
		return  mutation - o.mutation;
		
		
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((chromosome == null) ? 0 : chromosome.hashCode());
		result = prime * result + mutation;
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
		ChrPositionMutation other = (ChrPositionMutation) obj;
		if (chromosome == null) {
			if (other.chromosome != null)
				return false;
		} else if (!chromosome.equals(other.chromosome))
			return false;
		if (mutation != other.mutation)
			return false;
		if (position != other.position)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ChrPosition [chromosome=" + chromosome + ", startPosition="
				+ position + ", mutation=" + mutation + "]";
	}
	
	public String toIGVString() {
		return chromosome + ":" + position + "-" + position;
	}

	
}
