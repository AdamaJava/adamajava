/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.metrics.record.BaseDistributionRecord;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.ResultSummary;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;

public class Metric {
	
	protected String type;
	protected AtomicLong recordCount = new AtomicLong();
	protected int windowCount;
	protected double positionValue;	
	protected final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	protected Map<String,TreeMap<Integer,MetricRecord>> recordMap = new ConcurrentHashMap<>();
	protected Map<String,TreeMap<Integer,ResultSummary>> summaryMap = new ConcurrentHashMap<>();
    protected BaseDistributionRecord windowDistribution;
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	protected File wiggleFile;
	protected BaseDistributionRecord baseDistribution;
	protected double minBasesPerPatient;
	protected int totalPatients = 0;
	private BufferedWriter wiggleWriter;

	public Metric(String type, Double posvalue, int minWinCount, Integer minBasesPerPatient) {
		this.type = type;
		this.positionValue = posvalue;
		this.windowCount = minWinCount;	
		this.minBasesPerPatient = minBasesPerPatient;		
		this.windowDistribution = new BaseDistributionRecord(type, true);		
		
		if ( ! type.equals(PileupConstants.METRIC_SNP) && ! type.equals(PileupConstants.METRIC_STRAND_BIAS)) {
			this.baseDistribution = new BaseDistributionRecord(type, false);		
		}
	}	
	
	public BaseDistributionRecord getWindowDistribution() {
		return windowDistribution;
	}

	public void setWindowDistribution(BaseDistributionRecord windowDistribution) {
		this.windowDistribution = windowDistribution;
	}

	public boolean passesMinAvgBases(int totalReads) throws QPileupException {
		return getAvgBasesPerPatient(totalReads) > minBasesPerPatient;
	}
	
	public double getAvgBasesPerPatient(int totalReads) throws QPileupException {
		if (totalPatients == 0) {
			throw new QPileupException("TOTAL_PATIENTS_ERROR");
		}
		return (double) totalReads / totalPatients;
	}
	
	public int size(Chromosome chromosome) {
		return recordMap.get(chromosome.getName()).size();
	}
	
	public void clear(Chromosome chromosome) {		
		if (recordMap.containsKey(chromosome.getName())) {
			recordMap.get(chromosome.getName()).clear();	
		}
	}	
	
	public String getType() {
		return type;
	}

	public AtomicLong getRecordCount() {
		return recordCount;
	}

	public int getWindowCount() {
		return windowCount;
	}

	public double getPositionValue() {
		return positionValue;
	}

	public Map<String, TreeMap<Integer, MetricRecord>> getRecordMap() {
		return recordMap;
	}

	public Map<String, TreeMap<Integer, ResultSummary>> getSummaryMap() {
		return summaryMap;
	}

	public double getMinBasesPerPatient() {
		return minBasesPerPatient;
	}

	public int getTotalPatients() {
		return totalPatients;
	}	
	
	public BaseDistributionRecord getBaseDistribution() {
		return baseDistribution;
	}

	public File getWiggleFile() {
		return this.wiggleFile;
	}

	public File getBigWigFile(String wiggleDir) {
		return new File(wiggleDir + PileupConstants.FILE_SEPARATOR +  type + ".bw");
	}

	public void setTotalPatients(int totalPatients) {
		this.totalPatients = totalPatients;		
	}
	
	public void closeWiggleWriter() throws IOException {
		wiggleWriter.close();
	}

	public void openWiggleWriter() throws IOException {
		wiggleWriter = new BufferedWriter(new FileWriter(wiggleFile, true));		
	}

	public void writeWiggleHeader(Chromosome chromosome) throws IOException {
		wiggleWriter.write("variableStep chrom=" + chromosome.getName()+ " span=" + PileupConstants.STEP_SIZE  + PileupConstants.NEWLINE);		
	}

	public void writeToWiggle(String output) throws IOException {
		wiggleWriter.write(output);		
	}

	public void open() throws IOException {
		
	}
	
	public void close() throws IOException {
		
	}

	public void processRecord(QPileupRecord record, int totalReads) throws Exception {
		
	}
	
	public ResultRecord getWindow(String name, int start, int end, long total) {
		return null;
	}

	public void logOptions() {
		for (String s: getOptionsSummary()) {
			logger.info(s);
		}		
	}
	
	public List<String> getOptionsSummary() {
		List<String> list = new ArrayList<>(4);
		list.add("Metric type: " + type);
		list.add("Minimum value per position: " + positionValue);
		list.add("Minimum count per window: " + windowCount);
		return list;
	}
	
	public void writeHeader(String hdfHeader, String execHeader, String uuid, String wiggleDir) throws IOException {
		this.wiggleFile = new File(wiggleDir + PileupConstants.FILE_SEPARATOR +  type + ".wig");
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(wiggleFile, true));
		writer1.write("track type=wiggle_0 name="+ type + PileupConstants.NEWLINE);
		writer1.close();
	}

	public String getColumnHeaders() {
		return "element\tchr\tstartPos\tendPos\tno_of_event_positions\ttotal_counts_of_"+type+"\ttotal_bases\n";
	}

	public void addChromosome(String name) {
		if ( ! recordMap.containsKey(name)) {
			recordMap.put(name, new TreeMap<>());
		}
		if ( ! summaryMap.containsKey(name)) {
			summaryMap.put(name, new TreeMap<>());
		}
	}

	public void write(String pileupDir, String distributionDir) throws IOException {
		if (windowDistribution != null) {
			windowDistribution.write(distributionDir);
		}
		if (baseDistribution != null) {			
			baseDistribution.write(distributionDir);
		}
		
	}

}
