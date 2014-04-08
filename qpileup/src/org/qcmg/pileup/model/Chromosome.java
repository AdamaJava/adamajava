/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import org.qcmg.pileup.QPileupException;


public class Chromosome implements Comparable<Chromosome>{
	
	String name;
	Integer totalLength;	
	Integer sectionLength;
	Integer startPos;
	Integer endPos;
	private String hdfGroupName;

	public Chromosome(String name, int totalLength) {
		this.name = name;
		this.hdfGroupName = "/"+ name;
		this.totalLength = totalLength;	
		this.sectionLength = this.totalLength;
		this.startPos = 1;
		this.endPos = this.totalLength;	
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

	public void setName(String name) {
		this.name = name;
	}

	public Integer getTotalLength() {
		return totalLength;
	}

	public void setTotalLength(Integer totalLength) {
		this.totalLength = totalLength;
	}

	public Integer getSectionLength() {
		return sectionLength;
	}

	public void setSectionLength(Integer sectionLength) {
		this.sectionLength = sectionLength;
	}

	public Integer getStartPos() {
		return startPos;
	}

	public void setStartPos(Integer startPos) {
		this.startPos = startPos;
	}

	public Integer getEndPos() {
		return endPos;
	}

	public void setEndPos(Integer endPos) {
		this.endPos = endPos;
	}
	
	
	public String getHdfGroupName() {
		return hdfGroupName;
	}

	public void setHdfGroupName(String hdfGroupName) {
		this.hdfGroupName = hdfGroupName;
	}
	
	public String toString() {		
		return this.name + ":" + this.startPos + "_" + this.endPos;
	}

	@Override
	public int compareTo(Chromosome other) {
		return this.name.compareTo(other.getName());		
	}
	
	@Override
	public boolean equals(Object o) {
		 if (!(o instanceof Chromosome)) {
			 return false;
		 }
	            
		 Chromosome c = (Chromosome) o;
	     
		 return (c.getName().equals(name)) && c.getStartPos().equals(startPos) && c.getEndPos().equals(endPos);
	}
	
	public int hashCode() {
		return 31*name.hashCode() + startPos.hashCode() + endPos.hashCode();
	}
}
