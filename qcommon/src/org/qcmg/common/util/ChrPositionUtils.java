/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;

public class ChrPositionUtils {
	
	public static boolean doChrPositionsOverlap(ChrPosition a, ChrPosition b) {
		
		// check chromosome first
		if ( ! a.getChromosome().equals(b.getChromosome())) return false;
		
		// now positions
		if (a.getEndPosition() < b.getPosition()) return false;
		if (a.getPosition() > b.getEndPosition()) return false;
		
		return true;
	}
	
	
	public static boolean isChrPositionContained(ChrPosition a, ChrPosition b) {
		
		if (doChrPositionsOverlap(a, b)) {
			return a.getPosition() <= b.getPosition() && a.getEndPosition() >= b.getEndPosition();
		}
		return false;
		
	}
	
	public static boolean doChrPositionsOverlapPositionOnly(ChrPosition a, ChrPosition b) {
		// positions
		if (a.getPosition() > b.getEndPosition()) return false;
		if (a.getEndPosition() < b.getPosition()) return false;
		
		return true;
	}
	
	/**
	 * ASsumes that String is in the IGV notation.
	 * eg. chr1:123456-2345678
	 * 
	 * @param position
	 * @return
	 */
	public static ChrPosition getChrPositionFromString(String position) {
		return getChrPositionFromString(position, null);
	}
	public static ChrPosition getChrPositionFromString(String position, String name) {
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
		
		return new ChrPosition(chr, start, end, name);
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
		
		List<ChrPosition> chrPositions = new ArrayList<ChrPosition>();
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
			
			if (cp1.getPosition() == cp2.getEndPosition() + 1) {
				return true;
			}
			if (cp1.getEndPosition() == cp2.getPosition() - 1) {
				return true;
			}
		}
		return false;
	}
}
