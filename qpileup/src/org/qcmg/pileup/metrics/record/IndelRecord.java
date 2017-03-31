/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.text.DecimalFormat;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.pileup.PileupConstants;

public class IndelRecord extends MetricRecord {
	
	private final char referenceBase;
	private final String mutationType;	
	private long allDelCount;	
	private final StringBuilder referenceBaseString = new StringBuilder();
	private long forwardCount;
	private long reverseCount;
	
	public IndelRecord(String chromosome, int basePosition, int endPosition, char base, String mutationType, long count, long forCount, long revCount, int totalBases) {
		super(PileupConstants.METRIC_INDEL,chromosome, basePosition, count, totalBases);
		this.mutationType = mutationType;
		this.referenceBase= base;
		this.forwardCount = forCount;
		this.reverseCount = revCount;
		this.referenceBaseString.append(referenceBase);		
		this.endPosition = endPosition;
	}
	
	public IndelRecord(String chromosome, int basePosition, int endPosition, char base, String mutationType, int count, int totalReads) {
		super(PileupConstants.METRIC_INDEL,chromosome, basePosition, count, totalReads);
		this.mutationType = mutationType;
		this.endPosition = endPosition;	
		this.referenceBase = base;
		this.referenceBaseString.append(referenceBase);	
	}
	
	public long getForwardCount() {
		return forwardCount;
	}
	
	public long getAllDelCount() {
		return allDelCount;
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
		if ( getPercentage(count.longValue(), totalReads) > 20) {		
			double diff = getSBiasScore();
			if (diff > PileupConstants.SBIAS_MIN) {
				return true;
			}
		}
		return false;
	}

	public char getReferenceBase() {
		return referenceBase;
	}
	public String getChromosome() {
		return chromosome;
	}
	public String getMutationType() {
		return mutationType;
	}

	private int length() {
		return referenceBaseString.length();		
	}

	public String getKey() {
		return chromosome + "," + position;
	}

	public ChrRangePosition getChrPosition() {
		return new ChrRangePosition(chromosome, position, position);
	}

	public boolean addIndelRecord(IndelRecord record) {		
		if (record.getPosition().intValue() == getNextPosition()) {
			referenceBaseString.append(record.getReferenceBase());			
			count += record.getCount();
			totalReads += record.getTotalReads();
			return true;
		}		
		return false;
	}

	private int getNextPosition() {
		return position + length();
	}

	public int getStartPosition() {
			return position;		
	}

	public double getAllCountPercentTotalReads() {
		if (allDelCount > totalReads) {
			return 100;
		} else if (allDelCount == 0 || totalReads == 0) {
			return 0;
		} else {
			return ((double)allDelCount/(double)totalReads * 100);
		}	
	}

	public void setAllCount(long delCount) {
		this.allDelCount = delCount;		
	}
	
	@Override
	public String toTmpString() {
		//          0                1                  2                  3              4                5                     6                    7                    8                     9
		return chromosome + "\t" + position + "\t" + endPosition + "\t" + type + "\t" + count + "\t" + totalReads + "\t" + mutationType + "\t" + forwardCount + "\t" + reverseCount + "\t" + hasStrandBias() + "\n";
	}
	
	@Override
	public String toGFFString() {
		double perc = getPercentage(count.longValue(), totalReads);
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
			result += "Name=" + mutationType + ";color=" + "#C0C0C0" + ";PercentScore=" + f.format(perc) + ";SBiasScore=" + f.format(getSBiasScore())  + ";ForwardCount=" + forwardCount + ";ReverseCount=" + reverseCount + "\n";
		} else {
			result += "Name=" + mutationType + ";color=" + "#C0C0C0" + ";PercentScore=" + f.format(perc) + "\n";
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
