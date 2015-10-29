/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.coverage;

import java.util.Map.Entry;
import java.util.TreeMap;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;

public class RangePositionPileup {

	private final InputBAM inputBam;
	private final RangePosition position;
	private Options options;
	private final QueryExecutor exec;
	private TreeMap<Integer, Integer> countMap = new TreeMap<Integer, Integer>();
	private Integer maxCoverage = null;

	public RangePositionPileup(InputBAM i, RangePosition position,
			Options options, QueryExecutor exec) {
		this.inputBam = i;
		this.position = position;
		this.setOptions(options);
		this.exec = exec;
		this.countMap = setUpCountMap();
		if (options.getMaxCoverage() != null) {
			this.maxCoverage = options.getMaxCoverage();
		}
		
	}

	private TreeMap<Integer, Integer> setUpCountMap() {
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
		for (int i=position.getStart(); i<=position.getEnd(); i++) {
			map.put(i, 0);
		}
		return map;
	}

	public void pileup() throws Exception {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(inputBam.getBamFile(), "silent");) {
		
			SAMRecordIterator iterator = reader.queryOverlapping(position.getChr(), position.getStart(), position.getEnd());
		
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
			if (countMap.containsKey(i)) {
				int count = countMap.get(i) + 1;			
				countMap.put(i, count);
			}
		}		
	}
	
	public boolean passesMaxCoverage(Integer coverageCount) {
		if (maxCoverage == null) {
			return true;
		} else {
			if (coverageCount >= maxCoverage) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Entry<Integer, Integer> entry: countMap.entrySet()) {
			if (maxCoverage == null || !passesMaxCoverage(entry.getValue())) {
				sb.append(position.getChr() + "\t" +entry.getKey() + "\t" + entry.getValue() + "\t" + inputBam.getBamFile().getAbsolutePath() + "\n");
			}			
		}
		
		return sb.toString();		
	}

	public Options getOptions() {
		return options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

}
