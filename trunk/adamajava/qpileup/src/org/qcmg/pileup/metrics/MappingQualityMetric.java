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
import org.qcmg.pileup.metrics.record.MappingQualityRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class MappingQualityMetric extends Metric {
	
	protected AtomicLong recordCount = new AtomicLong();
	protected final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	private Map<String,TreeMap<Integer,MappingQualityRecord>> qualRecordMap = new ConcurrentHashMap<>();

	public MappingQualityMetric(double minPosCount, int minWinCount, int minTotalBases) {
		super(PileupConstants.METRIC_MAPPING, minPosCount, minWinCount, minTotalBases);
	}
	
	@Override
	public AtomicLong getRecordCount() {
		return recordCount;
	}

	@Override
	public void setRecordCount(AtomicLong recordCount) {
		this.recordCount = recordCount;
	}

	public Map<String, TreeMap<Integer, MappingQualityRecord>> getQualRecordMap() {
		return qualRecordMap;
	}

	public void setQualRecordMap(
			Map<String, TreeMap<Integer, MappingQualityRecord>> qualRecordMap) {
		this.qualRecordMap = qualRecordMap;
	}


	@Override
	public List<String> getOptionsSummary() {
		List<String> list = new ArrayList<>(4);
		list.add("Metric type: " + type);
		list.add("Maximum average quality per position: " + positionValue);
		list.add("Minimum count per window: " + windowCount);
		return list;
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {		
		
		int count = (int) record.getElementCount(StrandEnum.mapQual.toString());	
		baseDistribution.addBaseCounts(totalReads, count);		
		String chr = record.getChromosome();
		int basePos = (int) record.getBasePosition();
		MappingQualityRecord r = new MappingQualityRecord(chr, basePos, count, totalReads);
		
		//low mapping quality or low coverage	
		if (r.getAvgMappingQual() <= positionValue || !passesMinAvgBases(totalReads)) {
			qualRecordMap.get(chr).put(basePos, r);
		}
	}	

	@Override
	public ResultRecord getWindow(String name, int start, int end, long total) {
		
		ResultRecord rr = null;
		
		if (qualRecordMap.containsKey(name)) {			
			NavigableMap<Integer, MappingQualityRecord> subMap = qualRecordMap.get(name).subMap(start, true, end, true);			
			long actualSize = 0;
			long actualTotal = 0;

			for (Entry<Integer, MappingQualityRecord> entry: subMap.entrySet()) {				
				actualSize++;
				actualTotal += entry.getValue().getAvgMappingQual();												
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
		TreeMap<Integer,MappingQualityRecord> map = qualRecordMap.get(chromosome.getName());
		if (null != map) {
			map.clear();
		}
	}
	
	@Override
	public int size(Chromosome chromosome) {
		return qualRecordMap.get(chromosome.getName()).size();
	}

	@Override
	public void addChromosome(String name) {
		if ( ! qualRecordMap.containsKey(name)) {
			qualRecordMap.put(name,  new TreeMap<>());
		}
		if ( ! summaryMap.containsKey(name)) {
			summaryMap.put(name, new TreeMap<>());
		}		
	}
}
