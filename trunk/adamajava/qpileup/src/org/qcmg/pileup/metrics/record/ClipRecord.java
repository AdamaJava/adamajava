/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import org.qcmg.pileup.PileupUtil;


public class ClipRecord  extends MetricRecord{	
	
	private int startCount = 0;

	public ClipRecord(String type, String chromosome, Integer position, long count, int totalBases) {
		super(type, chromosome, position, count, totalBases);
	}	
	
	@Override
	public String toString() {
		return this.type + "\t" + chromosome + " " + position + " " + count;
	}
	
	@Override
	public double getRegularityScore() {
		if (PileupUtil.isRegularityType(type)) {			
			double percent = getStartPercentTotalReads();
			if (percent > 100) {
				return 100 * 100;
			} else {
				return percent * percent;
			}
		} 
		return 0;
	}

	@Override
	public String toTmpString() {
		return chromosome + "\t" + position + "\t" + endPosition + "\t" + type + "\t" + count + "\t" + startCount + "\t" + totalReads + "\n";
	}

	public void setStartCount(int startCount) {
		this.startCount = startCount;		
	}

	public double getStartPercentTotalReads() {
		if (startCount > totalReads) {
			return 100;
		} else if (startCount == 0 || totalReads == 0) {
			return 0;
		} else {
			return ((double)startCount/totalReads * 100);
		}
	}
}
