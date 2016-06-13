/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


public class PileupElement implements Comparable<PileupElement> {

	private char base;
	private int forwardCount;
	private int reverseCount;
	private final Collection<Byte> forwardQualities = new ArrayList<Byte>();
	private final Collection<Byte> reverseQualities = new ArrayList<Byte>();
	
	public PileupElement(char base) {
		this.base = base;
	}
	
	public PileupElement(byte a) {
		 this((char) a);
	}

	public char getBase() {
		return base;
	}
	public void setBase(char base) {
		this.base = base;
	}
	public int getForwardCount() {
		return forwardCount;
	}
	
	/**
	 * If b is set to Byte.MIN_VALUE, then don't add to the quality
	 * this is used when we are dealing with bases that don't have quality info
	 * @param b
	 */
	public void incrementForwardCount(byte b) {
		forwardCount++;
		if (b != Byte.MIN_VALUE)
			addForwardQuality(b);
	}
	public void incrementForwardCount() {
		forwardCount++;
	}
	public int getReverseCount() {
		return reverseCount;
	}
	
	/**
	 * If b is set to Byte.MIN_VALUE, then don't add to the quality
	 * this is used when we are dealing with bases that don't have quality info
	 * @param b
	 */
	public void incrementReverseCount(byte b) {
		reverseCount++;
		if (b != Byte.MIN_VALUE)
			addReverseQuality(b);
	}
	public void incrementReverseCount() {
		reverseCount++;
	}
	public int getTotalCount() {
		return forwardCount + reverseCount;
	}
	public boolean isFoundOnBothStrands() {
		return forwardCount > 0 && reverseCount > 0;
	}

	public int compareTo(PileupElement o) {
		// only interested in the total count for the purposes or ordering
		return o.getTotalCount() - getTotalCount();
	}
	
	public void addForwardQuality(byte b) {
		forwardQualities.add(b);
	}
	public void addReverseQuality(byte b) {
		reverseQualities.add(b);
	}
	
	public int getTotalForwardQualityScore() {
		int total = 0;
		for (Byte b : forwardQualities) total += b;
		return total;
	}
	public int getTotalReverseQualityScore() {
		int total = 0;
		for (Byte b : reverseQualities) total += b;
		return total;
	}
	
	public int getTotalQualityScore() {
		return getTotalForwardQualityScore() + getTotalReverseQualityScore();
	}
	
	public Collection<Byte> getForwardQualities() {
		return forwardQualities;
	}
	public Collection<Byte> getReverseQualities() {
		return reverseQualities;
	}
	public Collection<Byte> getQualities() {
		Collection<Byte> allQuals = new ArrayList<Byte>();
		allQuals.addAll(forwardQualities);
		allQuals.addAll(reverseQualities);
		return allQuals;
	}
	
	public String getQualitiesAsString() {
		return Arrays.deepToString(getQualities().toArray());
	}
	
	public String getFormattedString() {
		return "base: " + base + " count: " + getTotalCount() + " [" + forwardCount + "," + reverseCount + "]";
	}

}
