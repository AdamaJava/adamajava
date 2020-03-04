/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.model;

import java.io.File;

import org.qcmg.common.util.DonorUtils;

public class Comparison implements Comparable<Comparison> {
	
	private final String main;
	private final String test;
	
	private final int mainCoverage;
	private final int testCoverage;
	private final int overlapCoverage;
	private final double score;
	
	public String getMain() {
		return main;
	}

	public String getTest() {
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

	public double getScore() {
		return score;
	}
	
	public Comparison(File file1, int size, File file2, int size2, double finalTally, int count) {
		this(file1 != null ? file1.getAbsolutePath() : null, size, file2 != null ? file2.getAbsolutePath() : null, size2, finalTally, count);
	}

	public Comparison(String file1, int size, String file2, int size2, double finalTally, int count) {
		
		if (null == file1 || null == file2) { 
			throw new IllegalArgumentException("null files passed to Comparison constructor");
		}
		if (finalTally > 0.0 && count == 0) {
			throw new IllegalArgumentException("non-zero tally but zero count passed to Comparison constructor");
		}
		if (size < 0 || size2 < 0 || finalTally < 0.0 || count < 0) {
			throw new IllegalArgumentException("negative value(s) passed to Comparison constructor");
		}
		this.main = file1;
		this.test = file2;
		this.mainCoverage = size;
		this.testCoverage = size2;
		this.overlapCoverage = count;
		this.score = finalTally / overlapCoverage;
	}
	
	public String toSummaryString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Double.isNaN(score) ? "NaN" : score);
		sb.append(" (").append(overlapCoverage).append(") : ");
		sb.append(test);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(DonorUtils.getDonorFromFilename(main)).append(":");
		sb.append(main).append(" (").append(mainCoverage).append(") vs ");
		sb.append(DonorUtils.getDonorFromFilename(test)).append(":");
		sb.append(test).append(" (").append(testCoverage).append(") : ");
		sb.append(Double.isNaN(score) ? "NaN" : score);
		sb.append(", ").append(overlapCoverage);
		return sb.toString();
	}

	@Override
	public int compareTo(Comparison o) {
		
		if (score == o.score) {
			// scores are equal - check overlapCoverage - greatest coverage wins
			return overlapCoverage - o.overlapCoverage;
		}
		return score > o.score ? 1 : -1;
	}
	 
}
