/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.text.DecimalFormat;

import org.qcmg.pileup.PileupConstants;

public class NonReferenceMetricRecord extends MetricRecord {
	
	private long forwardCount;
	private long reverseCount;
	
	public NonReferenceMetricRecord(String chromosome, int basePosition, int count, int forCount, int revCount, int totalReads) {
		super(PileupConstants.METRIC_NONREFBASE,chromosome, basePosition, count, totalReads);
		this.forwardCount = forCount;
		this.reverseCount = revCount;
	}
	
	public long getForwardCount() {
		return forwardCount;
	}

	public void setForwardCount(long forwardCount) {
		this.forwardCount = forwardCount;
	}

	public long getReverseCount() {
		return reverseCount;
	}

	public void setReverseCount(long reverseCount) {
		this.reverseCount = reverseCount;
	}

	
	@Override
	public boolean hasStrandBias() {
		if (MetricRecord.getPercentage(getCount().longValue(), getTotalReads()) > 20) {		
			double diff = getSBiasScore();
			if (diff > 30) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toTmpString() {
		//          0                1                  2                  3              4                5                     6                    7                    8                     9
		return chromosome + "\t" + position + "\t" + endPosition + "\t" + type + "\t" + count + "\t" + totalReads + "\t" + forwardCount + "\t" + reverseCount + "\t" + hasStrandBias() + "\n";
	}
	
	@Override
	public String toGFFString() {
		double perc = MetricRecord.getPercentage(getCount().longValue(), getTotalReads());
		DecimalFormat f = new DecimalFormat("##.00");
		String result = chromosome + "\t";
		result += "qpileup" + "\t";
		result += "." + "\t";
		result += position + "\t";
		result += endPosition + "\t";
		result += f.format(perc) + "\t";
		result += "." + "\t";
		result += "." + "\t";
		if (hasStrandBias()) {
			result += "Name=NR-SBIAS;color=" + "#C0C0C0" + ";PercentScore=" + f.format(perc) + ";SBiasScore=" + f.format(getSBiasScore())  + ";ForwardCount=" + forwardCount + ";ReverseCount=" + reverseCount + "\n";
		} else {
			result += "Name=NR;color=" + "#C0C0C0" + ";PercentScore=" + f.format(perc) + "\n";
		}
		return result;
	}

	private double getSBiasScore() {
		double forPercent = 0;
		if (forwardCount > 0) {
			forPercent = (double) forwardCount / count * 100;
		}
		double reversePercent = 0;
		if (reverseCount > 0) {
			reversePercent = (double) reverseCount / count * 100;
		}
		return Math.abs(forPercent - reversePercent);
	}

}
