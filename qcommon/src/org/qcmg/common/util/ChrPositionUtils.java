/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;

public class ChrPositionUtils {
	
	public static boolean doChrPositionsOverlap(ChrPosition a, ChrPosition b) {
		return doChrPositionsOverlap(a,b,0);
	}
	
	public static boolean doChrPositionsOverlap(ChrPosition a, ChrPosition b, int buffer) {
		
		// check chromosome first
		if ( ! a.getChromosome().equals(b.getChromosome())) return false;
		
		// now positions
		if (a.getEndPosition() < b.getStartPosition() - buffer) return false;
		if (a.getStartPosition() > b.getEndPosition() + buffer) return false;
		
		return true;
	}
	
	/**
	 * All of ChrPosition b must be within ChrPosition a
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isChrPositionContained(ChrPosition a, ChrPosition b) {
		
		if (doChrPositionsOverlap(a, b)) {
			return a.getStartPosition() <= b.getStartPosition() && a.getEndPosition() >= b.getEndPosition();
		}
		return false;
		
	}
	
	public static Map<ChrRangePosition, Set<ChrRangePosition>> getAmpliconsFromFragments(List<ChrRangePosition> fragments) {
		
		Map<ChrRangePosition, Set<ChrRangePosition>> ampliconFragmentMap = fragments.stream()
			.collect(Collectors.groupingBy(cp -> {
				return new ChrRangePosition(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition());
			}, Collectors.toSet()));
//		Map<ChrPosition, Set<ChrPosition>> ampliconFragmentMap = fragments.stream()
//				.collect(Collectors.groupingBy(cp -> {
//					return new ChrPosition(cp.getChromosome(), cp.getPosition());
//				}, Collectors.toSet()));
		
		
		return ampliconFragmentMap;
		
	}
	
	/**
	 * converts coords in the format 9:5073770-5073770 to a ChrPosition object complete with "chr"
	 * @param cosmicCoords
	 * @return
	 */
	public static ChrRangePosition createCPFromCosmic(String cosmicCoords) {
		if (StringUtils.isNullOrEmpty(cosmicCoords)) {
			return null;
		} else {
			int colonIndex = cosmicCoords.indexOf(':');
			int minusIndex = cosmicCoords.indexOf('-');
			return new ChrRangePosition( "chr" + cosmicCoords.substring(0, colonIndex), 
					Integer.parseInt(cosmicCoords.substring(colonIndex + 1, minusIndex)),
					Integer.parseInt(cosmicCoords.substring(minusIndex + 1)));
		}
	}
	
	public static boolean arePositionsWithinDelta(ChrRangePosition a, ChrRangePosition b, int delta) {
		// check chromosome first
		if ( ! a.getChromosome().equals(b.getChromosome())) return false;
		
		int diff = Math.abs(a.getStartPosition() - b.getStartPosition());
		if (diff > delta) {
			return false;
		}
		
		diff += Math.abs(a.getEndPosition() - b.getEndPosition());
		return diff <= delta;
	}
	
	public static boolean doChrPositionsOverlapPositionOnly(ChrPosition a, ChrPosition b) {
		// positions
		if (a.getStartPosition() > b.getEndPosition()) return false;
		if (a.getEndPosition() < b.getStartPosition()) return false;
		
		return true;
	}
	

	public static ChrPosition cloneWithNewChromosomeName(ChrPosition cp, String newChr) {
		
		if (cp instanceof ChrPointPosition) {
			return ChrPointPosition.valueOf(newChr, cp.getStartPosition());
		} else if  (cp instanceof ChrRangePosition) {
			return new ChrRangePosition(newChr, cp.getStartPosition(), cp.getEndPosition());
		} else {
			throw new UnsupportedOperationException("cloneWithNewName not yet implemented for any types other than ChrPointPosition and ChrRangePosition!!!");
		}
		
	}
	
	/**
	 * ASsumes that String is in the IGV notation.
	 * eg. chr1:123456-2345678
	 * 
	 * @param position
	 * @return
	 */
	public static ChrRangePosition getChrPositionFromString(String position) {
		if (StringUtils.isNullOrEmpty(position)) 
			throw new IllegalArgumentException("Null or empty string passed to getChrPositionFromString()");
		
		int colonPos = position.indexOf(':');
		int minusPos = position.indexOf('-');
		
		if (colonPos == -1 || minusPos == -1) {
			throw new IllegalArgumentException("invalid string passed to getChrPositionFromString() - must be in chr1:12345-23456 format: " + position);
		}
		
		String chr = position.substring(0, colonPos);
		int start = Integer.parseInt(position.substring(colonPos + 1, minusPos));
		int end = Integer.parseInt(position.substring(minusPos + 1));
		
		return new ChrRangePosition(chr, start, end);
	}
	public static ChrPositionName getChrPositionNameFromString(String position, String name) {
		if (StringUtils.isNullOrEmpty(position)) 
			throw new IllegalArgumentException("Null or empty string passed to getChrPositionNameFromString()");
		
		int colonPos = position.indexOf(':');
		int minusPos = position.indexOf('-');
		
		if (colonPos == -1 || minusPos == -1) {
			throw new IllegalArgumentException("invalid string passed to getChrPositionNameFromString() - must be in chr1:12345-23456 format: " + position);
		}
		
		String chr = position.substring(0, colonPos);
		int start = Integer.parseInt(position.substring(colonPos + 1, minusPos));
		int end = Integer.parseInt(position.substring(minusPos + 1));
		
		return new ChrPositionName(chr, start, end, name);
	}
	
	public static ChrPosition getPrecedingChrPosition(ChrPosition cp) {
		return new ChrRangePosition(cp.getChromosome(), cp.getStartPosition() - 1, cp.getEndPosition() - 1);
	}
	
	/**
	 * Returns a list of ChrPosition objects based on the contents of the supplied String array
	 *  
	 * @param positions
	 * @return
	 */
	public static List<ChrPosition> getChrPositionsFromStrings(String[] positions) {
		
		if (null == positions || positions.length == 0) 
			throw new IllegalArgumentException("null or empty string array passed to getChrPositionsFromStrings");
		
		List<ChrPosition> chrPositions = new ArrayList<>();
		for (String s : positions) {
			chrPositions.add(getChrPositionFromString(s));
		}
		return chrPositions;
	}
	
	/**
	 * Returns true if the two supplied chrpos objects are on the same chromosome and are adjacent
	 * that is, the end position of 1 is next to the start position of the other
	 * 
	 * if they overlap, return false
	 * 
	 * @param cp1
	 * @param cp2
	 * @return
	 */
	public static boolean areAdjacent(ChrPosition cp1, ChrPosition cp2) {
		// need to be on the same chromosome
		if (cp1.getChromosome().equals(cp2.getChromosome())) {
			
			if (cp1.getStartPosition() == cp2.getEndPosition() + 1) {
				return true;
			}
			if (cp1.getEndPosition() == cp2.getStartPosition() - 1) {
				return true;
			}
		}
		return false;
	}
}
