/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.HighCoverageRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;

public class HighCoverageMetric extends Metric {
	
	protected AtomicLong recordCount = new AtomicLong();
	protected final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	private final Map<String,TreeMap<Integer,HighCoverageRecord>> qualRecordMap = new ConcurrentHashMap<String,TreeMap<Integer,HighCoverageRecord>>();

	public HighCoverageMetric(double minPosCount, int minWinCount, int minTotalBases) {
		super(PileupConstants.METRIC_HCOV, minPosCount, minWinCount, minTotalBases);
	}
	
	@Override
	public AtomicLong getRecordCount() {
		return recordCount;
	}


	@Override
	public List<String> getOptionsSummary() {
		List<String> list = new ArrayList<String>(4);
		list.add("Metric type: " + type);
		list.add("Maximum average quality per position: " + positionValue);
		list.add("Minimum count per window: " + windowCount);
		return list;
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {		
		
		//high coverage	
		if (getAvgBasesPerPatient(totalReads) > positionValue) {
			HighCoverageRecord r = new HighCoverageRecord(record.getChromosome(), (int)record.getBasePosition(), totalReads, totalReads, (getAvgBasesPerPatient(totalReads)));
			qualRecordMap.get(record.getChromosome()).put((int)record.getBasePosition(), r);
		}
	}	

	@Override
	public ResultRecord getWindow(String name, int start, int end, long total) {
		
		ResultRecord rr = null;
		
		if (qualRecordMap.containsKey(name)) {			
			NavigableMap<Integer, HighCoverageRecord> subMap = qualRecordMap.get(name).subMap(start, true, end, true);			
			long actualSize = 0;
			long actualTotal = 0;

			for (Entry<Integer, HighCoverageRecord> entry: subMap.entrySet()) {				
				actualSize++;
				actualTotal += entry.getValue().getAvgTotalReads();
												
			}
			windowDistribution.addWindowCounts(actualSize);	
			
			if (actualSize >= windowCount) {				
				rr = new ResultRecord(type, actualSize, actualTotal, 0);
			}			
		}
		
		return rr;
	}
	
	@Override
	public void clear(Chromosome chromosome) {
		TreeMap<Integer, HighCoverageRecord> map  = qualRecordMap.get(chromosome.getName());
		if (map != null) {
			map.clear();
		}
	}
	
	@Override
	public int size(Chromosome chromosome) {
		TreeMap<Integer, HighCoverageRecord> map  = qualRecordMap.get(chromosome.getName());
		if (map != null) {
			return map.size();
		}
		return 0;
	}

	@Override
	public void addChromosome(String name) {
		qualRecordMap.computeIfAbsent(name, v -> new TreeMap<>());
		summaryMap.computeIfAbsent(name, v -> new TreeMap<>());
	}
}
