/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import org.qcmg.pileup.PileupConstants;

public class MappingQualityRecord extends MetricRecord {

	private final double avgMappingQual;
	

	public MappingQualityRecord(String chromosome, Integer position, long count, int totalBases) {
		super(PileupConstants.METRIC_MAPPING, chromosome, position, count, totalBases);
		this.avgMappingQual = count > 0 && totalBases > 0 ? (double) count / (double) totalBases : 0;
	}
	
	public double getAvgMappingQual() {
		return avgMappingQual;
	}

}
