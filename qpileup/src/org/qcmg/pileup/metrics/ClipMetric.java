/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.util.Map.Entry;
import java.util.NavigableMap;

import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.ClipRecord;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class ClipMetric extends Metric{
	
	

	public ClipMetric(Double posvalue, int minWinCount, int minBasesPerPatient) {
		super(PileupConstants.METRIC_CLIP, posvalue, minWinCount, minBasesPerPatient);		
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {
		
		int count = 0;
		int startCount = 0;
		count += record.getElementCount(StrandEnum.cigarS.toString());
		count += record.getElementCount(StrandEnum.cigarH.toString());
		startCount += record.getElementCount(StrandEnum.cigarSStart.toString());
		startCount += record.getElementCount(StrandEnum.cigarHStart.toString());
		
		baseDistribution.addBaseCounts(totalReads, count);
		String chr = record.getChromosome();
		int basePos = (int) record.getBasePosition();
		ClipRecord r = new ClipRecord(type, chr, basePos, count, totalReads);

		if (MetricRecord.getPercentage(r.getCount().longValue(), r.getTotalReads()) >= positionValue && passesMinAvgBases(totalReads)) {		
			r.setTotalReads(totalReads);
			r.setStartCount(startCount);			
			recordMap.get(chr).put(basePos, r);
		}				
	}

	@Override
	public ResultRecord getWindow(String name, int start, int end, long totalBases) {
		ResultRecord rr = null;
		if (recordMap.containsKey(name)) {

			NavigableMap<Integer, MetricRecord> subMap = recordMap.get(name).subMap(start, true, end, true);				

			long actualSize = 0;
			long actualTotal = 0;			
			double regularityScore = 0;
			
			//score for record
			for (Entry<Integer, MetricRecord> entry: subMap.entrySet()) {									
				actualSize++;			
				ClipRecord r = (ClipRecord) entry.getValue();
				actualTotal += r.getCount();
				regularityScore += r.getRegularityScore();							
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

	public MetricRecord getRegularRecord(String chrName, int position) {
		ClipRecord r = (ClipRecord) (recordMap.get(chrName).get(position));		
		if (r != null) {
			if (r.getStartPercentTotalReads() > PileupConstants.MIN_CLIP_REGULAR_PERCENT) {				
				return r;
			}
		}
		return null;
	}
}
