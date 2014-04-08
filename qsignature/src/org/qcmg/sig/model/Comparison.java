/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.model;

import java.io.File;

import org.qcmg.common.util.DonorUtils;

public class Comparison implements Comparable<Comparison> {
	
	final File main;
	final File test;
	
	final int mainCoverage;
	final int testCoverage;
	final int overlapCoverage;
	final double totalScore;
	final double score;
	final int numberOfCalculations;
	
	public File getMain() {
		return main;
	}

	public File getTest() {
		return test;
	}

	public int getMainCoverage() {
		return mainCoverage;
	}

	public int getTestCoverage() {
		return testCoverage;
	}

	public int getOverlapCoverage() {
		return overlapCoverage;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public int getNumberOfCalculations() {
		return numberOfCalculations;
	}
	
	public double getScore() {
		return score;
	}

	public Comparison(File file1, int size, File file2, int size2,
			double finalTally, int count, int noOfCalculations) {
		
		if (null == file1 || null == file2) 
			throw new IllegalArgumentException("null files passed to Comparison constructor");
		
		if (finalTally > 0.0 && count == 0)
			throw new IllegalArgumentException("non-zero tally but zero count passed to Comparison constructor");
		
		if (size < 0 || size2 < 0 || finalTally < 0.0 || count < 0 || noOfCalculations < 0)
			throw new IllegalArgumentException("negative value(s) passed to Comparison constructor");
		
		this.main = file1;
		this.test = file2;
		this.mainCoverage = size;
		this.testCoverage = size2;
		this.totalScore = finalTally;
		this.overlapCoverage = count;
		this.numberOfCalculations = noOfCalculations;
		this.score = totalScore / overlapCoverage;
	}
	
	public String toSummaryString() {
		StringBuilder sb = new StringBuilder();
		sb.append(score == Double.NaN ? "NaN" : score);
		sb.append(" (").append(overlapCoverage).append(") : ");
		sb.append(test.getAbsolutePath());
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(DonorUtils.getDonorFromFilename(main.getAbsolutePath())).append(":");
		sb.append(main.getName()).append(" (").append(mainCoverage).append(") vs ");
		sb.append(DonorUtils.getDonorFromFilename(test.getAbsolutePath())).append(":");
		sb.append(test.getName()).append(" (").append(testCoverage).append(") : ");
		sb.append(score == Double.NaN ? "NaN" : score);
		sb.append(", ").append(overlapCoverage);
		sb.append(", ").append(numberOfCalculations);
		return sb.toString();
	}

	@Override
	public int compareTo(Comparison o) {
		
		if (score < o.score) return -1;
		if (score > o.score) return 1;
		
		// scores are equal - check overlapCoverage - greatest coverage wins
		return overlapCoverage - o.overlapCoverage;
	}
	 
}
