package org.qcmg.picard.util;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMSequenceDictionary;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.qcmg.common.util.Constants;

public class QueryIntervalUtils {
	
	/**
	 * Converts a list of string regions (eg. chr1:1-10000 into a list of QueryIntervals that are ordered and optimised
	 * @param regions
	 * @return List<QueryInterval>
	 */
	public static List<QueryInterval> getQueryIntervalsFromRegions(List<String> regions, SAMSequenceDictionary dictionary) {
		if (null == regions || regions.isEmpty() || null == dictionary) {
			return Collections.emptyList();
		}
		
		List<QueryInterval> intervals = new ArrayList<>();
		for (String region : regions) {
			/*
			 * region could be composed of contig, start and end, or just contig....
			 */
			int colonIndex = region.indexOf(Constants.COLON);
			String contig;
			int start = 1;
			int end = -1;
			if (colonIndex > -1) {
				contig = region.substring(0, colonIndex);
				int dashIndex = region.indexOf(Constants.MINUS);
				if (dashIndex > -1) {
					start = Integer.parseInt(region.substring(colonIndex + 1, dashIndex));
					end = Integer.parseInt(region.substring(dashIndex + 1));
				}
			} else {
				contig = region;
			}
			int sequenceIndex = dictionary.getSequenceIndex(contig);
			
			if (sequenceIndex > -1) {
				intervals.add( new QueryInterval(sequenceIndex, start, end));
			}
		}
		
		return intervals;
	}
}
