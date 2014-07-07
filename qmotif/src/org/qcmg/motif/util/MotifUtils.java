/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.TabTokenizer;

public class MotifUtils {

	
	public static final char M_D = ':';
	
	public static Map<String, AtomicInteger> convertStringArrayToMap(String data) {
		if (StringUtils.isNullOrEmpty(data))
			throw new IllegalArgumentException("Null or empty String array passed to convertStringArrayToMap");
		
		Map<String, AtomicInteger> results = new HashMap<>();
		
		String [] arrayData = data.indexOf(M_D) == -1 ? new String[] {data} : TabTokenizer.tokenize(data, M_D);
		
		for (String s : arrayData) {
			AtomicInteger ai = results.get(s);
			if (null == ai) {
				ai = new AtomicInteger();
				results.put(s, ai);
			}
			ai.incrementAndGet();
		}
		
		return results;
	}
	
	public static List<ChrPosition> getPositionsForChromosome(String chr, List<ChrPosition> positions) {
		if (StringUtils.isNullOrEmpty(chr)) throw new IllegalArgumentException("null or empty chromosome passed to getPositionsForChromosome");
		
		List<ChrPosition> results = new ArrayList<>();
		for (ChrPosition cp : positions) {
			if (cp.getChromosome().equalsIgnoreCase(chr)) results.add(cp);
		}
		return results;
	}
	
	public static Map<ChrPosition, RegionCounter> getRegionMap(String chr, int length, int windowSize, List<ChrPosition> includes, List<ChrPosition> excludes) {
		return  getRegionMap(chr, length, windowSize, includes, excludes, false);
	}
	
	public static Map<ChrPosition, RegionCounter> getRegionMap(String chr, int length, int windowSize, List<ChrPosition> includes, List<ChrPosition> excludes, boolean isUnmapped) {
		
		int noOfBins = (length / windowSize) + 1;
		
		// create an initial map of OTHER ChrPos objects
		Map<ChrPosition, RegionCounter> results = new HashMap<>();

		for (int i = 0 ; i < noOfBins ; i++) {
			ChrPosition cp = new ChrPosition(chr, (i * windowSize) + 1, (i + 1) * windowSize);
			results.put(cp, new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
		}
		
		// now go through the includes/excludes and if there are any overlaps - remove the OTHER ones and keep the inc/exc
		
		if (null != includes && ! includes.isEmpty()) {
			List<ChrPosition> overlapIncludes = getExistingOverlappingPositions(new ArrayList<ChrPosition>(results.keySet()), includes);
			if ( ! overlapIncludes.isEmpty()) {
				
				// darn....
				// need to determine if the overlapping position needs to be removed (ie, is entirely enclosed by an includes chrpos, or if it needs to be trimmed
				resizeOverlappingPositions(results, overlapIncludes, includes, isUnmapped);
				
			}
			// now add in the includes
			for (ChrPosition includesCP : includes) results.put(includesCP, new RegionCounter(RegionType.INCLUDES));
		}
		
		if (null != excludes && ! excludes.isEmpty()) {
			List<ChrPosition> overlapExcludes = getExistingOverlappingPositions(new ArrayList<ChrPosition>(results.keySet()), excludes);
			if ( ! overlapExcludes.isEmpty()) {
				// darn....
				// need to determine if the overlapping position needs to be removed (ie, is entirely enclosed by an includes chrpos, or if it needs to be trimmed
				
				resizeOverlappingPositions(results, overlapExcludes, excludes, isUnmapped);
				
			}
			// now add in the excludes
			for (ChrPosition excludeCP : excludes) results.put(excludeCP, new RegionCounter(RegionType.EXCLUDES));
		}
		
		return results;
	}
	
	
	public static void resizeOverlappingPositions(Map<ChrPosition, RegionCounter> results, 
			List<ChrPosition> overlappingPositions, List<ChrPosition> newPositions, boolean isUnmapped) {
		
		for (ChrPosition overlapCP : overlappingPositions) {
			
			for (ChrPosition newCP : newPositions) {
				// EQUALS
				if (overlapCP.equals(newCP)) results.remove(overlapCP);
				
				// overlapCP encloses newCP
				else if (ChrPositionUtils.isChrPositionContained(overlapCP, newCP)) {
					//create new CP(s)
					
					if (overlapCP.getPosition() < newCP.getPosition()) {
						results.put(new ChrPosition(overlapCP.getChromosome(), overlapCP.getPosition(), newCP.getPosition() -1), new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
					}
					if (overlapCP.getEndPosition() > newCP.getEndPosition()) {
						results.put(new ChrPosition(overlapCP.getChromosome(), newCP.getEndPosition() + 1, overlapCP.getEndPosition()), new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
					}
					
					// remove overlapCP
					results.remove(overlapCP);
				}
				
				// newCP encloses overlapCP
				else if (ChrPositionUtils.isChrPositionContained(overlapCP, newCP)) {
					// remove overlapCP
					results.remove(overlapCP);
				}
				
				// standard overlap
				else if (ChrPositionUtils.doChrPositionsOverlap(overlapCP, newCP)){
					
					if (overlapCP.getPosition() < newCP.getPosition()) {
						results.put(new ChrPosition(overlapCP.getChromosome(), overlapCP.getPosition(), newCP.getPosition() -1), new RegionCounter(RegionType.GENOMIC));
					}
					if (overlapCP.getEndPosition() > newCP.getEndPosition()) {
						results.put(new ChrPosition(overlapCP.getChromosome(), newCP.getEndPosition() + 1, overlapCP.getEndPosition()), new RegionCounter(RegionType.GENOMIC));
					}
					// remove overlapCP
					results.remove(overlapCP);
					
				}
			}
		}
		
	}
	
	public static List<ChrPosition> getExistingOverlappingPositions(List<ChrPosition> existingPositions, List<ChrPosition> newPositions) {
		// sort both collections first
		Collections.sort(existingPositions);
		Collections.sort(newPositions);
		
		List<ChrPosition> exisitingOverlaps = new ArrayList<>();
		
		for(ChrPosition newCP : newPositions) {
			
			// loop through existing and see if they overlap
			for (ChrPosition existingCP : existingPositions) {
				
				if (ChrPositionUtils.doChrPositionsOverlap(newCP, existingCP)) {
					exisitingOverlaps.add(existingCP);
				}
			}
		}
		
		return exisitingOverlaps;
	}

	static String addMotifToString(String existingString, String motif) {
		if (StringUtils.isNullOrEmpty(existingString)) {
			existingString = motif;
		} else {
			existingString += M_D + motif;
		}
		return existingString;
	}
}
