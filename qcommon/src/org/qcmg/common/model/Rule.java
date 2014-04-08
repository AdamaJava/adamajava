/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public class Rule {
	private final int minCoverage;
	private final int maxCoverage;
	private final int noOfVariants;
	
	/**
	 * The only constructor for the Rule class requires all int constituents to be supplied, 
	 * so that a number of checks can be carried out before object creation
	 * <p>
	 * Immutable object is returned
	 * 
	 * @param minCoverage
	 * @param maxCoverage
	 * @param noOfVariants
	 */
	public Rule(final int minCoverage, final int maxCoverage, final int noOfVariants) {
		if (0 > minCoverage)
			throw new IllegalArgumentException("Min coverage must be greater than or equal zero");
		if (maxCoverage < minCoverage)
			throw new IllegalArgumentException("Max coverage must be greater than or equal to the min coverage value");
		if (noOfVariants > maxCoverage)
			throw new IllegalArgumentException("No of variants must be less than or equal to the max coverage value");
		
		this.minCoverage = minCoverage;
		this.maxCoverage = maxCoverage;
		this.noOfVariants = noOfVariants;
	}
	
	public int getMinCoverage() {
		return minCoverage;
	}
	public int getMaxCoverage() {
		return maxCoverage;
	}
	public int getNoOfVariants() {
		return noOfVariants;
	}

	@Override
	public String toString() {
		return "Rule [maxCoverage=" + maxCoverage + ", minCoverage="
				+ minCoverage + ", noOfVariants=" + noOfVariants + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxCoverage;
		result = prime * result + minCoverage;
		result = prime * result + noOfVariants;
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
		Rule other = (Rule) obj;
		if (maxCoverage != other.maxCoverage)
			return false;
		if (minCoverage != other.minCoverage)
			return false;
		if (noOfVariants != other.noOfVariants)
			return false;
		return true;
	}
	
}
