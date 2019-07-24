/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.HashMap;
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
		
		return  ! (a.getStartPosition() > b.getEndPosition() + buffer);
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
	 * Returns a ChrPosition object - will be ChrPointPosition if start == end, and ChrRangePosition otherwise
	 */
	public static ChrPosition getChrPosition(String chr, int start, int end) {
		if (start == end) {
			return ChrPointPosition.valueOf(chr, start);
		} else {
			return new ChrRangePosition(chr, start, end);
		}
	}
	
	/**
	 * converts coords in the format 9:5073770-5073770 to a ChrPosition object complete with "chr"
	 * @param cosmicCoords
	 * @return
	 */
	public static ChrPosition createCPFromCosmic(String cosmicCoords) {
		if (StringUtils.isNullOrEmpty(cosmicCoords)) {
			return null;
		} else {
			int colonIndex = cosmicCoords.indexOf(':');
			int minusIndex = cosmicCoords.indexOf('-');
			int start = Integer.parseInt(cosmicCoords.substring(colonIndex + 1, minusIndex));
			int end = Integer.parseInt(cosmicCoords.substring(minusIndex + 1));
			return getChrPosition("chr" + cosmicCoords.substring(0, colonIndex), start, end); 
		}
	}
	
	public static boolean arePositionsWithinDelta(ChrPosition a, ChrPosition b, int delta) {
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
		
		return ! (a.getEndPosition() < b.getStartPosition());
	}
	

//	public static ChrPosition cloneWithNewChromosomeName(ChrPosition cp, String newChr) {
//		if (cp instanceof ChrPointPosition) {
//			return ChrPointPosition.valueOf(newChr, cp.getStartPosition());
//		} else if  (cp instanceof ChrRangePosition) {
//			return new ChrRangePosition(newChr, cp.getStartPosition(), cp.getEndPosition());
//		} else {
//			throw new UnsupportedOperationException("cloneWithNewName not yet implemented for any types other than ChrPointPosition and ChrRangePosition!!!");
//		}
//	}
	
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
	
	public static ChrPointPosition getChrPointPositionFromString(String position) {
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
		if (start != end) {
			throw new IllegalArgumentException("Start and end position in getChrPointPositionFromString are not the same. Start: " + start + ", end: " + end + ", from string: " + position);
		}
		
		return ChrPointPosition.valueOf(chr, start);
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
	 * Returns a map of ChrPointPosition objects based on the contents of the supplied String array
	 *  
	 * @param positions
	 * @return
	 */
	public static Map<ChrPosition, ChrPosition>  getChrPointPositionsFromStrings(String[] positions) {
		
		if (null == positions || positions.length == 0) 
			throw new IllegalArgumentException("null or empty string array passed to getChrPositionsFromStrings");
		
		Map<ChrPosition, ChrPosition> chrPositions = new HashMap<>();
		for (String s : positions) {
			ChrPosition cpp = getChrPointPositionFromString(s);
			chrPositions.put(cpp, cpp);
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
	
	/**
	 * 
	 * @param ref: chromosome name
	 * @return converted name, eg. M, MT, chrmt to chrMT; X,Y to chrX,chrY; 1 to chr1
	 */
	public static String ChrNameConveter(String ref) {
		if (ref == null ) return null; //stop exception
		
		
		String refName = ref.toLowerCase();
		
		if( refName.equals("m") || refName.equals("mt")|| refName.equals("chrm")|| refName.equals("chrmt")) {
			return Constants.CHR + "MT";
		}
		
		if (refName.equals("x") || refName.equals("y") ) {
			return Constants.CHR + ref.toUpperCase();
		}
		
        if (refName.startsWith(Constants.CHR)) {
        	return ref;
        }
				
		/*
		 * If ref is an integer less than 23, slap "chr" in front of it
		 */
		if (Character.isDigit(ref.charAt(0))) {
			try {
				int refInt = Integer.parseInt(ref);
				if (refInt < 23 && refInt > 0) {
					return Constants.CHR + ref;
				}
			} catch (NumberFormatException nfe) {
				// don't do anything here - will return the original reference
			}
		}
		
		return ref;
	}
	
//	public static boolean compareAmbiguousChrName(String chrA, String chrB) {
//		
//		if(chrA == null || chrB == null) { 
//			return false;
//		}	
//		
//		if(chrA.trim().equals(chrB.trim()) || ChrNameConveter(chrA).equals(ChrNameConveter(chrB))){
//			return true;
//		}
//				
//	 
//		return false;
//	}
	
	/**
	 * to convert chromosome name if accept ambiguous match, otherwise return original input. 
	 * In ambiguous match, it will add "chr" to "X", "Y" and digit between 1 and 23, convert "chrM", "MT","M", "chrMT" ignore case to "chrMT"
	 * @param cp input value of ChrPosition
	 * @param isStrict2chrName whether to match the chromosome name strictly
	 * @return the input ChrPosition if it require to match the chromosome name strictly. otherwise return a modified ChrPosition value.
	 */
	public static ChrPosition getNewchrNameIfStrict(ChrPosition cp, boolean isStrict2chrName) {
		
		//chr name convertion in ambiguous mode		
		if( !isStrict2chrName ) {
			String newChr = ChrPositionUtils.ChrNameConveter(  cp.getChromosome() );
			if ( ! newChr.equals(cp.getChromosome())) {				
				if (cp instanceof ChrPointPosition) {
					return ChrPointPosition.valueOf(newChr, cp.getStartPosition());
				} else if  (cp instanceof ChrRangePosition) {
					return new ChrRangePosition(newChr, cp.getStartPosition(), cp.getEndPosition());
				} else {
					throw new UnsupportedOperationException("cloneWithNewName not yet implemented for any types other than ChrPointPosition and ChrRangePosition!!!");
				}				
				 
			}		 
		}
		
		return cp;
	}	
}
