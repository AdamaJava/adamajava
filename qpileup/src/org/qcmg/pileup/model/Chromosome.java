/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import org.qcmg.pileup.QPileupException;


public class Chromosome implements Comparable<Chromosome>{
	
	private final String name;
	private final int totalLength;	
	private final int sectionLength;
	private final int startPos;
	private final int endPos;
	private final String hdfGroupName;

	public Chromosome(String name, int totalLength) throws QPileupException {
		this(name, totalLength, 1, totalLength);
	}
	
	public Chromosome(String name, int totalLength, int startPos, int endPos) throws QPileupException {
		this.name = name;
		this.hdfGroupName = "/" + name;
		this.totalLength = totalLength;	
		this.sectionLength = endPos - startPos + 1;
		this.startPos = startPos;
		this.endPos = endPos;
		if (startPos < 1 || endPos < 0 || endPos < startPos ) {
			throw new QPileupException("READ_RANGE_ERROR", startPos + "" + endPos);
		}
	}
	
	public String getName() {
		return name;
	}

	public Integer getTotalLength() {
		return Integer.valueOf(totalLength);
	}

	public Integer getSectionLength() {
		return Integer.valueOf(sectionLength);
	}

	public Integer getStartPos() {
		return Integer.valueOf(startPos);
	}

	public Integer getEndPos() {
		return Integer.valueOf(endPos);
	}
	
	public String getHdfGroupName() {
		return hdfGroupName;
	}

	@Override
	public String toString() {		
		return this.name + ":" + this.startPos + "_" + this.endPos;
	}

	@Override
	public int compareTo(Chromosome other) {
		return this.name.compareTo(other.getName());		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPos;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + startPos;
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
		Chromosome other = (Chromosome) obj;
		if (endPos != other.endPos)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (startPos != other.startPos)
			return false;
		return true;
	}
	
}
