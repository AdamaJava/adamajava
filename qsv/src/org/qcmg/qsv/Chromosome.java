/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;


/**
 * Class that represents a chromosome name and length. It can also represent a subsection of a chromosome. 
 */
public class Chromosome implements Comparable<Chromosome>{
	
	private final String name;
	private final int totalLength;	
	private final int startPosition;
	private final int endPosition;

	/**
	 * Instantiates a new chromosome.
	 *
	 * @param name the name
	 * @param totalLength the total length
	 */
	public Chromosome(String name, int totalLength) {
		this.name = name;
		this.totalLength = totalLength;	
		this.startPosition = 1;
		this.endPosition = this.totalLength;	
	}
	
	/**
	 * Instantiates a new chromosome.
	 *
	 * @param name the name
	 * @param totalLength the total length
	 * @param startPosition the start position
	 * @param endPosition the end position
	 */
	public Chromosome(String name, int totalLength, int startPosition, int endPosition) {
		this.name = name;
		this.totalLength = totalLength;	
		this.startPosition = startPosition;
		this.endPosition = endPosition;
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the total length.
	 *
	 * @return the total length
	 */
	public int getTotalLength() {
		return totalLength;
	}

	/**
	 * Gets the start position.
	 *
	 * @return the start position
	 */
	public int getStartPosition() {
		return startPosition;
	}

	/**
	 * Gets the end position.
	 *
	 * @return the end position
	 */
	public int getEndPosition() {
		return endPosition;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {		
		return this.name + ":" + this.startPosition + "_" + this.endPosition;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Chromosome other) {
		String thisName = name.toLowerCase();
		String otherName = other.name.toLowerCase();		
		
		if (thisName.contains("gl") || otherName.contains("gl")) {
			return thisName.compareTo(otherName);
		} else if (thisName.contains("chrmt")) {
			if (otherName.contains("x") || otherName.contains("y")) {
				return 1;
			} else {
				if (thisName.equals(otherName)) {
					return Integer.compare(startPosition, other.startPosition);
				} else {
					return thisName.compareTo(otherName);
				}
			}
		} else if (otherName.contains("chrmt")) {
			if (thisName.contains("x") || thisName.contains("y")) {
				return -1;
			} else {
				if (thisName.equals(otherName)) {
					return Integer.compare(startPosition, other.startPosition);
				} else {
					return thisName.compareTo(otherName);
				}
			}			
		} else if (thisName.contains("x") || thisName.contains("y") || otherName.contains("x") || otherName.contains("y")) {
			if (thisName.equals(otherName)) {
				return Integer.compare(startPosition, other.startPosition);
			} else {
				return thisName.compareTo(otherName);
			}
		} else {
			Integer a = Integer.valueOf(name.substring(3));
			Integer b = Integer.valueOf(otherName.substring(3));
			
			if (a.equals(b)) {
				return Integer.compare(startPosition, other.startPosition);
			} else {
				return (a.compareTo(b));
			}
		}		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPosition;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + startPosition;
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
		if (endPosition != other.endPosition)
			return false;
		if (startPosition != other.startPosition)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}

