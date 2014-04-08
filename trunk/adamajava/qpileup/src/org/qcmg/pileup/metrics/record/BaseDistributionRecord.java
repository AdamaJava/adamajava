/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.pileup.PileupConstants;



public class BaseDistributionRecord {
	
	private String type;
	private ConcurrentSkipListMap<BigDecimal, AtomicLong> baseDistributionCountMap = new ConcurrentSkipListMap<BigDecimal, AtomicLong>();	
	private long totalPositions = 0;
	private List<String> htmlFiles = new ArrayList<String>();
	int MIN_TOTAL_BASES = 100;
	private boolean isWindow;
	
	public BaseDistributionRecord(String type, boolean isWindow) {
		this.type = type;
		this.isWindow = isWindow;
		
		if (isWindow) {
			for (int i=0; i<=PileupConstants.WINDOW_SIZE; i++) {
				BigDecimal bd = new BigDecimal(i);
				BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);	
				baseDistributionCountMap.put(rounded, new AtomicLong(0));	
			}			
		} else {
			for (int i=0; i<=1000; i++) {
				BigDecimal bd = new BigDecimal(i * 0.1);
				BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);			
				baseDistributionCountMap.put(rounded, new AtomicLong(0));			
			}
		}
	}
	
	public ConcurrentSkipListMap<BigDecimal, AtomicLong> getBaseDistributionCountMap() {
		return baseDistributionCountMap;
	}

	public void setBaseDistributionCountMap(
			ConcurrentSkipListMap<BigDecimal, AtomicLong> baseDistributionCountMap) {
		this.baseDistributionCountMap = baseDistributionCountMap;
	}

	
	public String getPositionHeader() {
		return "percent_non_reference\tno_of_positions\n";
	}
	
	public List<String> getHtmlFiles() {
		return htmlFiles;
	}

	public String getType() {
		return this.type;
	}		

	public synchronized void addBaseCounts(long totalReads, long totalMetricCounts) {			
		if (type.equals(PileupConstants.METRIC_SNP) || type.equals(PileupConstants.METRIC_STRAND_BIAS)) {
			BigDecimal bd = new BigDecimal(Long.toString(totalMetricCounts));
			BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
			addToMap(rounded);
		} else if (totalMetricCounts > 0) {		
			if (type.equals(PileupConstants.METRIC_MAPPING)) {
				double percent = (double) totalMetricCounts / (double) totalReads;
				BigDecimal bd = new BigDecimal(percent);
				BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
				addToMap(rounded);
			} else {
				//only count if int above the min base threshold
				if ((totalReads > MIN_TOTAL_BASES ) && totalMetricCounts < totalReads) {
					double percent = (double) totalMetricCounts / (double) totalReads * 100;					
					BigDecimal bd = new BigDecimal(percent);
					BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
					addToMap(rounded);
				} else {
					if (totalMetricCounts > totalReads) {
						BigDecimal bd = new BigDecimal(100);
						BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);		
						addToMap(rounded);
					} else {
						BigDecimal bd = new BigDecimal(0);
						BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);	
						addToMap(rounded);
					}
				}
			}			
		} else {
			BigDecimal bd = new BigDecimal(0);
			BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);	
			addToMap(rounded);
		}		
	}
	
	public void addWindowCounts(long positionCount) {
		BigDecimal bd = new BigDecimal(positionCount);
		BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
		addToMap(rounded);
	}

	private synchronized void addToMap(BigDecimal percent) {		
		if (baseDistributionCountMap.containsKey(percent)) {
			baseDistributionCountMap.get(percent).incrementAndGet();
		} else {			
			baseDistributionCountMap.put(percent, new AtomicLong(1));
		}		
	}
	
	public void finish() {
		totalPositions = getFinalTotal(baseDistributionCountMap);
	}
	
	public long getFinalTotal(ConcurrentSkipListMap<BigDecimal, AtomicLong> baseDistributionCountMap) {
		long windowTotal = 0;
		for (Entry<BigDecimal, AtomicLong> entry: baseDistributionCountMap.entrySet()) {
			windowTotal += entry.getValue().longValue();
		}
		return windowTotal;
	}

	
	public void write(String dir) throws IOException {
		if (isWindow) {
			writeDistributionTabFile(dir, ".window.distribution.txt");
		} else {
			writeDistributionTabFile(dir, ".base.distribution.txt");
		}		
	}

	private void writeDistributionTabFile(String dir, String fileEnd) throws IOException {
		File file = new File(dir + PileupConstants.FILE_SEPARATOR + type + fileEnd);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(toString());
		writer.close();		
	}

	private String getDistributionString(ConcurrentSkipListMap<BigDecimal, AtomicLong> baseDistributionCountMap,
			long totalPositions, String headerType, String header) {
		StringBuilder sb = new StringBuilder();

		final String tab = PileupConstants.TAB_DELIMITER;

		//header
		
		sb.append("#BASELINE_DISTRIBUTION: " + type + "\n");
		
		String desc = "positions";
		if (isWindow) {
			desc = "windows";
		}
		
		if (type.equals(PileupConstants.METRIC_MAPPING)) {
			sb.append("#average_"+ type + "\tnumber_positions\t%_total_"+desc+"\t%_total_"+desc+"\n");
			
		} else if (type.equals(PileupConstants.METRIC_SNP)) {
			sb.append("#snp_count\tnumber_positions\t%_total_"+desc+"\t%_total_"+desc+"\n");			
		} else {
			sb.append("#%"+ type + "\tnumber_positions\t%_total_"+desc+"\t%_total_"+desc+"\n");
		}
		
		long count = 0;
		DecimalFormat f = new DecimalFormat("0.0000");
		
		if (baseDistributionCountMap.size() > 0) {
			
			for (Entry<BigDecimal, AtomicLong> entry : baseDistributionCountMap.entrySet()) {
				long value = entry.getValue().longValue();				
				double percent = ((double)value/(double) totalPositions) * 100;
				count += value;
				if (entry.getKey().doubleValue() % 1 == 0) {
					double countPercent = ((double) count/ (double)totalPositions) * 100;
					sb.append(entry.getKey().doubleValue() + tab + value + tab + f.format(percent) + tab + f.format(countPercent) + "\n" );
					count = 0;
				} else {				
					sb.append(entry.getKey().doubleValue() + tab + value + tab + f.format(percent)  + tab + "\n" );
				}
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		finish();
		String NEWLINE = "\n";
		sb.append(getDistributionString(baseDistributionCountMap, totalPositions, "no_of_"+type+"_positions_per_window", getPositionHeader()));
		sb.append(NEWLINE);
		sb.append(NEWLINE);	
		return sb.toString();
	}

}
