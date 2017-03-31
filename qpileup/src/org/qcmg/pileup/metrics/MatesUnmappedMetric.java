/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.util.NavigableMap;
import java.util.Map.Entry;

import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class MatesUnmappedMetric extends Metric{
	
	public MatesUnmappedMetric(Double posvalue, int minWinCount, Integer minTotalBases) {
		super(PileupConstants.METRIC_UNMAPPED_MATE, posvalue, minWinCount, minTotalBases);		
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {
		
		int count = 0;
		count += record.getElementCount(StrandEnum.mateUnmapped.toString());		
		baseDistribution.addBaseCounts(totalReads, count);		
		
		String chr = record.getChromosome();
		int basePos =  (int)record.getBasePosition();
		
		MetricRecord r = new MetricRecord(type, chr, basePos, count, totalReads);
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


}
