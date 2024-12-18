/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;

public class MotifUtils {

	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	public static final char M_D = Constants.COLON;
	
	public static Map<String, AtomicInteger> convertStringArrayToMap(String data) {
		if (StringUtils.isNullOrEmpty(data)) {
			throw new IllegalArgumentException("Null or empty String array passed to convertStringArrayToMap");
		}
		
		String [] arrayData = data.indexOf(M_D) == -1 ? new String[] {data} : TabTokenizer.tokenize(data, M_D);
		
		Map<String, AtomicInteger> results = new HashMap<>(arrayData.length * 2);
		
		for (String s : arrayData) {
			results.computeIfAbsent(s, v -> new AtomicInteger()).incrementAndGet();
		}
		
		return results;
	}
	
	public static List<ChrPosition> getPositionsForChromosome(ChrPosition cp, List<ChrPosition> positions) {
		if (cp == null) throw new IllegalArgumentException("null ChrPosition object passed to getPositionsForChromosome");
		
		return positions.stream().filter(myCP -> ChrPositionUtils.doChrPositionsOverlap(cp, myCP)).collect(Collectors.toList());
	}
	
	public static Map<ChrPosition, RegionCounter> getRegionMap(ChrPosition contig, int windowSize, List<ChrPosition> includes, List<ChrPosition> excludes) {
		return  getRegionMap(contig, windowSize, includes, excludes, false);
	}
	
	public static Map<ChrPosition, RegionCounter> getRegionMap(ChrPosition contig, int windowSize, List<ChrPosition> includes, List<ChrPosition> excludes, boolean isUnmapped) {
		/*
		 * Add buffer around the CP as we are getting reads that overlap the cp rather than reads that are wholly contained within the CP.
		 * And so we need to cater for reads that start/end outwith the CP
		 * Assuming that a buffer of 1000 should be sufficient for now...
		 */
		int startPos = contig.getStartPosition();
		// we don't want a -ve startPos
		int stopPos = contig.getEndPosition();
		int length = stopPos - startPos  + 1;
		
		int noOfBins = (length / windowSize) + 1;
		
		// create an initial map of OTHER ChrPos objects
		Map<ChrPosition, RegionCounter> results = new HashMap<>();

		for (int i = 0 ; i < noOfBins ; i++) {
			if ( ((i * windowSize) + startPos) <= length) {
				ChrPosition cp = new ChrPositionName(contig.getChromosome(), (i * windowSize) + startPos, Math.min((i + startPos) * windowSize, stopPos));
				results.put(cp, new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
			}
		}
		
		// now go through the includes/excludes and if there are any overlaps - remove the OTHER ones and keep the inc/exc
		
		if (null != includes && ! includes.isEmpty()) {
			List<ChrPosition> overlapIncludes = getExistingOverlappingPositions(new ArrayList<>(results.keySet()), includes);
			if ( ! overlapIncludes.isEmpty()) {
				
				// darn....
				// need to determine if the overlapping position needs to be removed (ie, is entirely enclosed by an includes chrpos, or if it needs to be trimmed
				resizeOverlappingPositions(results, overlapIncludes, includes, isUnmapped);
				
			}
			// now add in the includes
			for (ChrPosition includesCP : includes) results.put(includesCP, new RegionCounter(RegionType.INCLUDES));
		}
		
		if (null != excludes && ! excludes.isEmpty()) {
			List<ChrPosition> overlapExcludes = getExistingOverlappingPositions(new ArrayList<>(results.keySet()), excludes);
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
					
					if (overlapCP.getStartPosition() < newCP.getStartPosition()) {
						results.put(new ChrRangePosition(overlapCP.getChromosome(), overlapCP.getStartPosition(), newCP.getStartPosition() -1), new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
					}
					if (overlapCP.getEndPosition() > newCP.getEndPosition()) {
						results.put(new ChrRangePosition(overlapCP.getChromosome(), newCP.getEndPosition() + 1, overlapCP.getEndPosition()), new RegionCounter(isUnmapped ? RegionType.UNMAPPED : RegionType.GENOMIC));
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
					
					if (overlapCP.getStartPosition() < newCP.getStartPosition()) {
						results.put(new ChrPositionName(overlapCP.getChromosome(), overlapCP.getStartPosition(), newCP.getStartPosition() -1), new RegionCounter(RegionType.GENOMIC));
					}
					if (overlapCP.getEndPosition() > newCP.getEndPosition()) {
						results.put(new ChrPositionName(overlapCP.getChromosome(), newCP.getEndPosition() + 1, overlapCP.getEndPosition()), new RegionCounter(RegionType.GENOMIC));
					}
					// remove overlapCP
					results.remove(overlapCP);
				}
			}
		}
	}
	
	public static List<ChrPosition> getExistingOverlappingPositions(List<ChrPosition> existingPositions, List<ChrPosition> newPositions) {
		// sort both collections first
		existingPositions.sort(COMPARATOR);
		newPositions.sort(COMPARATOR);
		
		List<ChrPosition> existingOverlaps = new ArrayList<>();
		
		for(ChrPosition newCP : newPositions) {
			existingOverlaps.addAll(existingPositions.stream().filter(cp -> ChrPositionUtils.doChrPositionsOverlap(newCP, cp)).toList());
		}
		return existingOverlaps;
	}

	static void addMotifToString(StringBuilder existingString, String motif) {
		if (null != existingString && ! StringUtils.isNullOrEmpty(motif)) {
			if (!existingString.isEmpty()) {
				existingString.append(M_D);
			}
			existingString.append(motif);
		}
	}
}
