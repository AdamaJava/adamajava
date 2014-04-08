/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.text.DecimalFormat;

public class StrandBiasRecord {
	
	String chromosome; 
	int position;	
	char refBase;
	private char forwardAltBase;
	private int forwardAltCount = 0;
	private int forwardRefCount = 0;
	private char reverseAltBase;
	private int reverseAltCount = 0;
	private int reverseRefCount = 0;
	private double percentForwardAlt = 0;
	private double percentReverseAlt = 0;
	private double MIN_PERCENT_DIFFERENCE;
	private int forwardTotalBases;
	private int reverseTotalBases;
	
	
	public StrandBiasRecord(String chr, int pos, char refBase, Integer minPercentDifference) {
		this.chromosome = chr;
		this.position = pos;
		this.refBase = refBase;
		this.MIN_PERCENT_DIFFERENCE = minPercentDifference;
	}
	
	public String getChromosome() {
		return chromosome;
	}

	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public char getRefBase() {
		return refBase;
	}

	public void setRefBase(char refBase) {
		this.refBase = refBase;
	}

	public char getForwardAltBase() {
		return forwardAltBase;
	}

	public void setForwardAltBase(char forwardAltBase) {
		this.forwardAltBase = forwardAltBase;
	}

	public int getForwardAltCount() {
		return forwardAltCount;
	}

	public void setForwardAltCount(int forwardAltCount) {
		this.forwardAltCount = forwardAltCount;
	}

	public int getForwardRefCount() {
		return forwardRefCount;
	}

	public void setForwardRefCount(int forwardRefCount) {
		this.forwardRefCount = forwardRefCount;
	}

	public char getReverseAltBase() {
		return reverseAltBase;
	}

	public void setReverseAltBase(char reverseAltBase) {
		this.reverseAltBase = reverseAltBase;
	}

	public int getReverseAltCount() {
		return reverseAltCount;
	}

	public void setReverseAltCount(int reverseAltCount) {
		this.reverseAltCount = reverseAltCount;
	}

	public int getReverseRefCount() {
		return reverseRefCount;
	}

	public void setReverseRefCount(int reverseRefCount) {
		this.reverseRefCount = reverseRefCount;
	}
	
	public double getPercentForwardAlt() {
		return percentForwardAlt;
	}

	public void setPercentForwardAlt(double percentForwardAlt) {
		this.percentForwardAlt = percentForwardAlt;
	}

	public double getPercentReverseAlt() {
		return percentReverseAlt;
	}

	public void setPercentReverseAlt(double percentReverseAlt) {
		this.percentReverseAlt = percentReverseAlt;
	}
	
	public void addForwardBaseCounts(char altBase, int refCount, int altCount, int totalBases) {
		this.forwardAltBase = altBase;
		this.forwardAltCount = altCount;
		this.forwardRefCount = refCount;
		this.forwardTotalBases = totalBases;
		if (forwardAltCount > 0 && forwardTotalBases > 0) {
			this.percentForwardAlt = (double)forwardAltCount/(double) forwardTotalBases * 100;
		}
	}
	
	public void addReverseBaseCounts(char altBase, int refCount, int altCount, int totalBases) {
		this.reverseAltBase = altBase;
		this.reverseAltCount = altCount;
		this.reverseRefCount = refCount;
		this.reverseTotalBases = totalBases;
		if (reverseAltCount > 0 && reverseTotalBases > 0) {
			this.percentReverseAlt = (double)reverseAltCount/(double) reverseTotalBases * 100;
		}
	}
	
	public boolean hasDifferentAltBase() {
		if (forwardAltBase != reverseAltBase) {
			return true;
		}
		return false;
	}

	public boolean hasStrandBias() {
		double altBaseDifference = getAltBaseCountDifference();
		
		if (altBaseDifference > MIN_PERCENT_DIFFERENCE ) {
			return true;
		}
		return false;
	}

	public double getAltBaseCountDifference() {		
		if (percentReverseAlt > percentForwardAlt) {
			return percentReverseAlt - percentForwardAlt;
		} else {
			return percentForwardAlt - percentReverseAlt;
		}
	}
	
	public String toString() {
		 DecimalFormat df = new DecimalFormat("#.##");
		return chromosome + "\t"+ position + "\t"+ position + "\t" + refBase  + "\t" + forwardAltBase + "\t" + forwardRefCount + "\t"+ forwardAltCount + "\t" +  forwardTotalBases + "\t" +
				 df.format(percentForwardAlt) + "\t" +   refBase + "\t" + reverseAltBase + "\t" + reverseRefCount + "\t" 
				 + reverseAltCount + "\t" + reverseTotalBases + "\t" + df.format(percentReverseAlt) +
				 "\t" + df.format(getAltBaseCountDifference()) + "\t" + hasStrandBias() + "\t" + hasDifferentAltBase() + "\n";
	}

}
