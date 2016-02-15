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
	
	public String toIGVString() {
		return getChromosome() + ":" + getStartPosition() + "-" + getEndPosition();
	}
	
}
