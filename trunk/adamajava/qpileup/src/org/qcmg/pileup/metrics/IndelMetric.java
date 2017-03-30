/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.pileup.PileupConstants;

import org.qcmg.pileup.metrics.record.IndelRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.ResultSummary;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;


public class IndelMetric extends Metric {
	
	final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	private final Map<String,TreeMap<Integer,IndelRecord>> insMap = new ConcurrentHashMap<>();
	private final Map<String,TreeMap<Integer,IndelRecord>> delStartMap = new ConcurrentHashMap<>();
	private final Map<String,TreeMap<Integer,IndelRecord>> delAllMap = new ConcurrentHashMap<>();

	public IndelMetric(Double posvalue, Integer winCount, Integer minTotalBases) {
		super(PileupConstants.METRIC_INDEL, posvalue, winCount, minTotalBases);		
	}
	
	public Map<String, TreeMap<Integer, IndelRecord>> getInsMap() {
		return insMap;
	}

	public Map<String, TreeMap<Integer, IndelRecord>> getDelStartMap() {
		return delStartMap;
	}

	public Map<String, TreeMap<Integer, IndelRecord>> getDelAllMap() {
		return delAllMap;
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {		
		
		long insForCount = record.getForwardElement(StrandEnum.cigarI.toString());
		long insRevCount = record.getReverseElement(StrandEnum.cigarI.toString());
		long delForCount = record.getForwardElement(StrandEnum.cigarDStart.toString());
		long delRevCount = record.getReverseElement(StrandEnum.cigarDStart.toString());
		long delAllCount = record.getElementCount(StrandEnum.cigarD.toString());
		long insCount = insForCount + insRevCount;
		long delCount = delForCount + delRevCount;
		
		baseDistribution.addBaseCounts(totalReads, insCount + delCount);
		String chr = record.getChromosome();
		int basePos = (int) record.getBasePosition();
		char base = record.getBase();
		IndelRecord inRecord = new IndelRecord(chr, basePos, basePos, base, PileupConstants.INS, insCount, insForCount, insRevCount, totalReads);	
		
		IndelRecord delRecord = new IndelRecord(chr, basePos, basePos, base, PileupConstants.DEL, delCount, delForCount, delRevCount, totalReads);	
		delRecord.setAllCount(delAllCount);
		
		
		if (passesMinAvgBases(totalReads)) {
			
			if (inRecord.getPercentTotalReads() > positionValue) {						
				addIndelRecord(insMap, inRecord, chr);
			} 
			//any del record if it's starts or any position is greater than position value
			if (delRecord.getPercentTotalReads() > positionValue) { 
				addIndelRecord(delStartMap, delRecord, chr);
				//addDeletionStartRecord(delRecord, chr);				
			} 
			if (delRecord.getAllCountPercentTotalReads() > positionValue) {						
				addIndelRecord(delAllMap, delRecord, chr);			
			}			
		}		
	}

	@Override
	public ResultRecord getWindow(String chromosomeName, int start, int end, long total) {
		
		ResultRecord rr = null;
		if (insMap.containsKey(chromosomeName) && delAllMap.containsKey(chromosomeName)) {			
			NavigableMap<Integer, IndelRecord> inSubMap = insMap.get(chromosomeName).subMap(start, true, end, true);
			NavigableMap<Integer, IndelRecord> delSubMap = delStartMap.get(chromosomeName).subMap(start, true, end, true);
			//find the total number of positionns are there may be ins and del at same position
			Set<Integer> keySet = new HashSet<Integer>(inSubMap.size());
			keySet.addAll(inSubMap.keySet());
			keySet.addAll(delSubMap.keySet());
			
			long size = keySet.size();		
			
			long totalCount = getTotalIndelCount(inSubMap) + getTotalIndelCount(delSubMap);
			double totalRegularityScore = getTotalRegularityScore(inSubMap) + getTotalRegularityScore(delSubMap);
			
			windowDistribution.addWindowCounts(size);			
			
			if (size >= windowCount) {					
				rr = new ResultRecord(type, size, totalCount, totalRegularityScore);
			} 			
		}
		
		return rr;
	}
	
	@Override
	public List<String> getOptionsSummary() {
		List<String> list = new ArrayList<>();
		list.add("Metric type: " + type);
		list.add("Minimum value per position: " + positionValue);
		list.add("Minimum count per window: " + windowCount);
		return list;
	}
	
	@Override
	public void addChromosome(String chromosome) {
		createChrMap(insMap, chromosome);
		createChrMap(delAllMap, chromosome);
		createChrMap(delStartMap, chromosome);
		summaryMap.putIfAbsent(chromosome, new TreeMap<>());
	}

	private void createChrMap(Map<String, TreeMap<Integer, IndelRecord>> map, String chromosome) {
		map.putIfAbsent(chromosome, new TreeMap<>());
	}

	@Override
	public void writeHeader(String hdfHeader, String execHeader, String uuid, String wiggleDir) throws IOException {
		this.wiggleFile = new File(wiggleDir + PileupConstants.FILE_SEPARATOR +  type + ".wig");
		try (BufferedWriter writer1 = new BufferedWriter(new FileWriter(wiggleFile, true));) {
			writer1.write("track type=wiggle_0 name="+ type + PileupConstants.NEWLINE);
		}
	}
	
	public void addIndelRecord(Map<String, TreeMap<Integer, IndelRecord>> map, IndelRecord record, String chr) {
		if (map.containsKey(record.getChromosome())) {
			map.get(record.getChromosome()).put(record.getPosition(), record);
		} else {
			TreeMap<Integer,IndelRecord> chrMap = new TreeMap<Integer, IndelRecord>();
			chrMap.put(record.getPosition(), record);
			map.put(record.getChromosome(), chrMap);
		}
	}

	@Override
	public void clear(Chromosome chromosome) {
		delAllMap.get(chromosome.getName()).clear();
		insMap.get(chromosome.getName()).clear();
		delStartMap.get(chromosome.getName()).clear();
	}

	private double getTotalRegularityScore(NavigableMap<Integer, IndelRecord> subMap) {
		double totalScore = 0;
		for (Entry<Integer, IndelRecord> entry: subMap.entrySet()) {
			totalScore += entry.getValue().getRegularityScore();
		}
		return totalScore;
	}

	private long getTotalIndelCount(NavigableMap<Integer, IndelRecord> subMap) {
		return subMap.values().stream().mapToLong(ir -> ir.getCount().longValue()).sum();
	}

	public IndelRecord getRegularInsRecord(String chr, int position) {
		IndelRecord r = (insMap.get(chr).get(position));
		if (r != null) {
			if (r.getPercentTotalReads() > PileupConstants.MIN_INDEL_REGULAR_PERCENT) {
				return r;
			}
			if (r.hasStrandBias()) {
				return r;
			}
		}
		
		return null;
	}
	
	public IndelRecord getRegularDelRecord(String chr, int position) {
		IndelRecord r = (delStartMap.get(chr).get(position));
		
		if (r != null) {
			for (int i = position+1; i<=delAllMap.get(chr).lastKey(); i++) {
				IndelRecord current = (delAllMap.get(chr).get(i));				
				if (current != null) {
					if (current.getPercentTotalReads() > PileupConstants.MIN_INDEL_REGULAR_PERCENT) {
						r.setEndPosition(current.getPosition());
						r.addIndelRecord(current);
					}
				} else {
					break;
				}
			}			
			if (r.getPercentTotalReads() > PileupConstants.MIN_INDEL_REGULAR_PERCENT) {				
				return r;
			}
			if (r.hasStrandBias()) {
				return r;
			}
		}		
		return null;
	}
	
	@Override
	public int size(Chromosome chromosome) {
		return insMap.get(chromosome.getName()).size() +  delAllMap.get(chromosome.getName()).size() +  delStartMap.get(chromosome.getName()).size();
	}
}
