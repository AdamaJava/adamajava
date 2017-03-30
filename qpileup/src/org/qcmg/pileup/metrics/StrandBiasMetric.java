/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.ResultSummary;
import org.qcmg.pileup.metrics.record.StrandBiasRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;


public class StrandBiasMetric extends Metric {
	
	
	private BufferedWriter tabFileWriter;	
	final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;		
	private final Map<String,TreeMap<Integer,StrandBiasRecord>> strandBiasMap = new ConcurrentHashMap<String,TreeMap<Integer,StrandBiasRecord>>();
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final File strandBiasOutputFile;
	private final Integer minPercentDifference;
	private final int minNonReferenceBases;
	private final String hdfFilePath;
	
	public StrandBiasMetric(String hdfName, String hdfFilePath, String pileupDir, 
			Integer minPercentDifference, Integer minTotalBases, Integer minNonReferenceBases) {
		super(PileupConstants.METRIC_STRAND_BIAS, 1.0, 1, minTotalBases);		
		this.hdfFilePath = hdfFilePath;
		this.minPercentDifference = minPercentDifference;
		this.minNonReferenceBases = minNonReferenceBases;
		this.strandBiasOutputFile = new File(pileupDir + PileupConstants.FILE_SEPARATOR + hdfName + "_strand_bias.txt");
	}
	
	public Integer getMinPercentDifference() {
		return minPercentDifference;
	}

	public Map<String, TreeMap<Integer, StrandBiasRecord>> getStrandBiasMap() {
		return strandBiasMap;
	}

	@Override
	public void logOptions() {
		logger.info("Metric type: " + type);
		logger.info("Output text file: " + strandBiasOutputFile);
	}

	@Override
	public void addChromosome(String chromosome) {
		if (!strandBiasMap.containsKey(chromosome)) {
			TreeMap<Integer, StrandBiasRecord> map = new TreeMap<Integer, StrandBiasRecord>();
			strandBiasMap.put(chromosome, map);
		}
		if (!summaryMap.containsKey(chromosome)) {
			TreeMap<Integer, ResultSummary> map = new TreeMap<Integer, ResultSummary>();
			summaryMap.put(chromosome, map);
		}
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {		
		
		int totalForwardBases = record.getTotalBases(true, false);
		int totalReverseBases = record.getTotalBases(false, true);	
		int totalNonreference = (int) record.getElementCount("nonreferenceNo");
		if (passesMinAvgBases(totalForwardBases) &&  passesMinAvgBases(totalReverseBases) && totalNonreference > minNonReferenceBases) {
			
			StrandBiasRecord sb = record.getStrandBiasRecord(minPercentDifference);
			if (sb !=null) {				
				addRecord(sb);
			}
		}
	}


	@Override
	public ResultRecord getWindow(String name, int start, int end, long total) {
		ResultRecord rr = null;
		if (strandBiasMap.containsKey(name)) {
			
			NavigableMap<Integer, StrandBiasRecord> subMap = strandBiasMap.get(name).subMap(start, true, end, true);
			long size = subMap.size();			
			long totalCount = size;				
			windowDistribution.addBaseCounts(total, totalCount);	
			if (size >= windowCount) {	
				rr = new ResultRecord(type, size, totalCount, 0);
			} 			
		}
		
		return rr;
	}


	@Override
	public void writeHeader(String hdfHeader, String execHeader, String uuid, String wiggleDir) throws IOException {
		tabFileWriter.write("#" + hdfFilePath + "\n");
		tabFileWriter.write(getColumnHeaders());		
		
		this.wiggleFile = new File(wiggleDir + PileupConstants.FILE_SEPARATOR +  type + ".wig");
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(wiggleFile, true));
		writer1.write("track type=wiggle_0 name="+ type + PileupConstants.NEWLINE);
		writer1.close();
	}

	@Override
	public String getColumnHeaders() {
		return "#Chr"+ TAB_DELIMITER +"PosStart"+ TAB_DELIMITER + "PosEnd"+ TAB_DELIMITER 
				+"RefBase"+ TAB_DELIMITER +"ForAltBase"
		+ TAB_DELIMITER + "ForRefCount"+ TAB_DELIMITER +"ForAltBaseCount"+TAB_DELIMITER +"ForTotalBase"
				+ TAB_DELIMITER + "ForPercentNonRef" + TAB_DELIMITER
			 + "RefBase"+ TAB_DELIMITER +"RevAltBase"+ TAB_DELIMITER + "RevRefCount"  
			 + TAB_DELIMITER +"RevAltBaseCount"+TAB_DELIMITER + "RevTotalBase"+TAB_DELIMITER + "RevPercentNonRef" +  
				TAB_DELIMITER + "Difference" + TAB_DELIMITER + "HasStrandBias" + TAB_DELIMITER 
				+ "HasDifferentAltBases" + PileupConstants.NEWLINE;
	}

	@Override
	public void open() throws IOException {		
		tabFileWriter = new BufferedWriter(new FileWriter(strandBiasOutputFile));
	}

	@Override
	public void close() throws IOException {
		tabFileWriter.close();		
	}
	
	public void addRecord(StrandBiasRecord record) throws Exception {
		
		if (strandBiasMap.containsKey(record.getChromosome())) {
			
			strandBiasMap.get(record.getChromosome()).put(record.getPosition(), record);
		} else {
			TreeMap<Integer,StrandBiasRecord> map = new TreeMap<>();
			map.put(record.getPosition(), record);
			strandBiasMap.put(record.getChromosome(), map);
		}
	}
	
	@Override
	public void clear(Chromosome chromosome) {
		if (strandBiasMap.containsKey(chromosome.getName())) {
			strandBiasMap.get(chromosome.getName()).clear();
		}
	}

	public synchronized void writeRecords(Chromosome chromosome) throws Exception {
		
		TreeMap<Integer, StrandBiasRecord> map = strandBiasMap.get(chromosome.getName());
		
		if (map != null) {
			
			for (Entry<Integer, StrandBiasRecord> currentEntry: map.entrySet()) {					
				tabFileWriter.write(currentEntry.getValue().toString());
			}
			//clear values after writing
			map.clear();
		}	
	}
	
	@Override
	public int size(Chromosome chromosome) {
		return strandBiasMap.get(chromosome.getName()).size();
	}

}
