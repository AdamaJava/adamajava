/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public record Rule(int minCoverage, int maxCoverage, int noOfVariants) {
	/**
	 * The only constructor for the Rule class requires all int constituents to be supplied,
	 * so that a number of checks can be carried out before object creation
	 * <p>
	 * Immutable object is returned
	 */
	public Rule {
		if (0 > minCoverage) {
			throw new IllegalArgumentException("Min coverage must be greater than or equal zero");
		}
		if (maxCoverage < minCoverage) {
			throw new IllegalArgumentException("Max coverage must be greater than or equal to the min coverage value");
		}
		if (noOfVariants > maxCoverage) {
			throw new IllegalArgumentException("No of variants must be less than or equal to the max coverage value");
		}

	}


	@Override
	public String toString() {
		return "Rule [maxCoverage=" + maxCoverage + ", minCoverage="
				+ minCoverage + ", noOfVariants=" + noOfVariants + "]";
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
		return noOfVariants == other.noOfVariants;
	}

}
