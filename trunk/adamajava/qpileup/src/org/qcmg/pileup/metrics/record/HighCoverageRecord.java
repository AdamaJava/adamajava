/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import org.qcmg.pileup.PileupConstants;

public class HighCoverageRecord extends MetricRecord {

	private final double avgTotalReads;

	public HighCoverageRecord(String chromosome, Integer position, long count, int totalBases, double avgTotalReads) {
		super(PileupConstants.METRIC_HCOV, chromosome, position, count, totalBases);
		this.avgTotalReads = avgTotalReads;				
	}

	public double getAvgTotalReads() {
		return avgTotalReads;
	}

}
