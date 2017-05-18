/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;

class RangePositionPileup {

	private final InputBAM inputBam;
	private final ChrRangePosition position;
	private Options options;
	private final QueryExecutor exec;
	private final Map<Integer, AtomicInteger> countMap ;
	private Integer maxCoverage = null;

	public RangePositionPileup(InputBAM i, ChrRangePosition position, Options options, QueryExecutor exec) {
		this.inputBam = i;
		this.position = position;
		this.setOptions(options);
		this.exec = exec;
		this.countMap = setUpCountMap();
		if (options.getMaxCoverage() != null) {
			this.maxCoverage = options.getMaxCoverage();
		}
		
	}

	private Map<Integer, AtomicInteger> setUpCountMap() {
		Map<Integer, AtomicInteger> map = new HashMap<>();
		for (int i=position.getStartPosition(); i<=position.getEndPosition(); i++) {
			map.put(i, new AtomicInteger());
		}
		return map;
	}

	public void pileup() throws Exception {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(inputBam.getBamFile(), "silent");) {
		
			SAMRecordIterator iterator = reader.queryOverlapping(position.getChromosome(), position.getStartPosition(), position.getEndPosition());
		
			while(iterator.hasNext()) {
				
				SAMRecord r = iterator.next();			
				
				//check it's a valid record
				if (SAMUtils.isSAMRecordValidForVariantCalling(r)) {
					if (exec != null) {
						if (exec.Execute(r)) {
							incrementMap(r);
						}
					} else {
						incrementMap(r);
					}
				}			
			}
			iterator.close();
		}
		
	}

	private void incrementMap(SAMRecord r) {
		int start = r.getAlignmentStart();
		int end = r.getAlignmentEnd();
		
		for (int i=start; i<=end; i++) {
			AtomicInteger ai = countMap.get(i);
			if (null != ai) {
				ai.incrementAndGet();
			}
		}		
	}
	
	private  boolean passesMaxCoverage(int coverageCount) {
		if (maxCoverage == null) {
			return true;
		} else {
			if (coverageCount >= maxCoverage.intValue()) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		List<Integer> keys = new ArrayList<>(countMap.keySet());
		keys.sort(null);
		
		for (Integer i : keys) {
			AtomicInteger ai = countMap.get(i);
			if (maxCoverage == null || ! passesMaxCoverage(ai.intValue())) {
				sb.append(position.getChromosome()).append("\t").append(i).append("\t").append(ai.get()).append("\t").append(inputBam.getBamFile().getAbsolutePath()).append("\n");
			}			
		}
		return sb.toString();		
	}

	public Options getOptions() {
		return options;
	}

	private void setOptions(Options options) {
		this.options = options;
	}

}
