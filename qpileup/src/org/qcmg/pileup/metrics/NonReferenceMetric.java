/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.util.NavigableMap;
import java.util.Map.Entry;


import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.NonReferenceMetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class NonReferenceMetric extends Metric{
	

	public NonReferenceMetric(Double posvalue, int minWinCount, Integer minTotalBases) {
		super(PileupConstants.METRIC_NONREFBASE, posvalue, minWinCount, minTotalBases);		
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {		

		int forwardCount = (int) record.getForwardElement(StrandEnum.nonreferenceNo.toString());
		int reverseCount = (int) record.getReverseElement(StrandEnum.nonreferenceNo.toString());
		int count = forwardCount + reverseCount;				
		baseDistribution.addBaseCounts(totalReads, count);
		String chr = record.getChromosome();
		int basePos = (int) record.getBasePosition();
		NonReferenceMetricRecord r = new NonReferenceMetricRecord(chr, basePos, count, forwardCount, reverseCount, totalReads);
		if (MetricRecord.getPercentage(r.getCount().longValue(), r.getTotalReads()) >= positionValue && passesMinAvgBases(totalReads)) {			
			r.setTotalReads(totalReads);
			recordMap.get(chr).put(basePos, r);
		}				
	}
	
	@Override
	public ResultRecord getWindow(String name, int start, int end, long total) {
		ResultRecord rr = null;
		if (recordMap.containsKey(name)) {

			NavigableMap<Integer, MetricRecord> subMap = recordMap.get(name).subMap(start, true, end, true);				

			long actualSize = 0;
			long actualTotal = 0;
			
			double regularityScore = 0;
			//score for record
			for (Entry<Integer, MetricRecord> entry: subMap.entrySet()) {									
				actualSize++;					
				actualTotal += entry.getValue().getCount();
				regularityScore += entry.getValue().getRegularityScore();							
			}
			
			if (windowDistribution !=null) {
				windowDistribution.addWindowCounts(actualSize);
			}
			
			if (actualSize >= windowCount) {				
				rr = new ResultRecord(type, actualSize, actualTotal, regularityScore);
			}			
		}
		
		return rr;
	}

	public MetricRecord getRegularRecord(String chr, int position) {
		MetricRecord r = (recordMap.get(chr).get(position));
		if (r != null) {			
			if (MetricRecord.getPercentage(r.getCount().longValue(), r.getTotalReads()) > PileupConstants.MIN_NONREF_REGULAR_PERCENT) {
				return r;
			}
			if (r.hasStrandBias()) {
				return r;
			}
		}
		return null;		
	}



}
