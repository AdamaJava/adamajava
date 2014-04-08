/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;


/**
 * Class that represents a chromosome name and length. It can also represent a subsection of a chromosome. 
 */
public class Chromosome implements Comparable<Chromosome>{
	
	private final String name;
	private final Integer totalLength;	
	private final Integer sectionLength;
	private final Integer startPosition;
	private final Integer endPosition;

	/**
	 * Instantiates a new chromosome.
	 *
	 * @param name the name
	 * @param totalLength the total length
	 */
	public Chromosome(String name, int totalLength) {
		this.name = name;
		this.totalLength = totalLength;	
		this.sectionLength = this.totalLength;
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
		this.sectionLength = endPosition - startPosition + 1;
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
	 * Sets the name.
	 *
	 * @param name the new name
	 */
//	public void setName(String name) {
//		this.name = name;
//	}

	/**
	 * Gets the total length.
	 *
	 * @return the total length
	 */
	public Integer getTotalLength() {
		return totalLength;
	}

	/**
	 * Sets the total length.
	 *
	 * @param totalLength the new total length
	 */
//	public void setTotalLength(Integer totalLength) {
//		this.totalLength = totalLength;
//	}

	/**
	 * Gets the section length.
	 *
	 * @return the section length
	 */
	public Integer getSectionLength() {
		return sectionLength;
	}

	/**
	 * Sets the section length.
	 *
	 * @param sectionLength the new section length
	 */
//	public void setSectionLength(Integer sectionLength) {
//		this.sectionLength = sectionLength;
//	}

	/**
	 * Gets the start position.
	 *
	 * @return the start position
	 */
	public Integer getStartPosition() {
		return startPosition;
	}

	/**
	 * Sets the start position.
	 *
	 * @param startPosition the new start position
	 */
//	public void setStartPosition(Integer startPosition) {
//		this.startPosition = startPosition;
//	}

	/**
	 * Gets the end position.
	 *
	 * @return the end position
	 */
	public Integer getEndPosition() {
		return endPosition;
	}

	/**
	 * Sets the end position.
	 *
	 * @param endPosition the new end position
	 */
//	public void setEndPosition(Integer endPosition) {
//		this.endPosition = endPosition;
//	}
	
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
					return this.startPosition.compareTo(other.startPosition);
				} else {
					return thisName.compareTo(otherName);
				}
			}
		} else if (otherName.contains("chrmt")) {
			if (thisName.contains("x") || thisName.contains("y")) {
				return -1;
			} else {
				if (thisName.equals(otherName)) {
					return this.startPosition.compareTo(other.startPosition);
				} else {
					return thisName.compareTo(otherName);
				}
			}			
		} else if (thisName.contains("x") || thisName.contains("y") || otherName.contains("x") || otherName.contains("y")) {
			if (thisName.equals(otherName)) {
				return this.startPosition.compareTo(other.startPosition);
			} else {
				return thisName.compareTo(otherName);
			}
		} else {
			Integer a = Integer.valueOf(name.substring(3));
			Integer b = Integer.valueOf(otherName.substring(3));
			
			if (a.equals(b)) {
				return this.startPosition.compareTo(other.startPosition);
			} else {
				return (a.compareTo(b));
			}
		}		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		 if (!(o instanceof Chromosome)) {
			 return false;
		 }
	            
		 Chromosome c = (Chromosome) o;
	     
		 return (c.name.equals(name)) && c.startPosition.equals(startPosition) && c.endPosition.equals(endPosition);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 31*name.hashCode() + startPosition.hashCode() + endPosition.hashCode();
	}
}

