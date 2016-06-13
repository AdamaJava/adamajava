/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import java.text.DecimalFormat;



public class Base implements Comparable<Base>{
	
	final static int BEFORE = -1;
	final static int EQUAL = 0;
	final static int AFTER = 1;
	
	final char base;
	final long count;
	long totalQual;
	double avgBaseQual;
	private final long forwardCount;
	private final long reverseCount;
	private long forTotalQual;

	private long revTotalQual;
	private double forAvgBaseQual;
	private double revAvgBaseQual;
	
	public Base(char base, long forwardCount, long reverseCount, long forTotalQual, long revTotalQual, double forAvgBaseQual, double revAvgBaseQual) {
		this.base = base;
		this.count = forwardCount + reverseCount;
		this.totalQual = forTotalQual + revTotalQual;
		this.avgBaseQual = forAvgBaseQual + revAvgBaseQual;
		this.forwardCount = forwardCount;
		this.reverseCount = reverseCount;
		this.forTotalQual = forTotalQual;
		this.revTotalQual = revTotalQual;
		this.forAvgBaseQual = forAvgBaseQual;
		this.revAvgBaseQual = revAvgBaseQual;
	}

	public Base(char base, long forCount, long revCount) {
		this.base = base;
		this.count = forCount + revCount;
		this.forwardCount = forCount;
		this.reverseCount = revCount;
	}

	public char getBase() {
		return base;
	}

//	public void setBase(char base) {
//		this.base = base;
//	}


	public long getCount() {
		return count;
	}


//	public void setCount(long count) {
//		this.count = count;
//	}


	public long getTotalQual() {
		return totalQual;
	}


	public void setTotalQual(long totalQual) {
		this.totalQual = totalQual;
	}


	public double getAvgBaseQual() {
		return avgBaseQual;
	}


	public void setAvgBaseQual(double avgBaseQual) {
		this.avgBaseQual = avgBaseQual;
	}

	public long getForwardCount() {
		return forwardCount;
	}

//	public void setForwardCount(long forwardCount) {
//		this.forwardCount = forwardCount;
//	}

	public long getReverseCount() {
		return reverseCount;
	}

//	public void setReverseCount(long reverseCount) {
//		this.reverseCount = reverseCount;
//	}


	public double getForAvgBaseQual() {
		return forAvgBaseQual;
	}

	public void setForAvgBaseQual(double forAvgBaseQual) {
		this.forAvgBaseQual = forAvgBaseQual;
	}

	public double getRevAvgBaseQual() {
		return revAvgBaseQual;
	}

	public void setRevAvgBaseQual(double revAvgBaseQual) {
		this.revAvgBaseQual = revAvgBaseQual;
	}


	@Override
	public int compareTo(Base other) {

	    //this optimization is usually worthwhile, and can
	    //always be added
	    if ( this == other ) return EQUAL;

	    //primitive numbers follow this form
	    if (this.base < other.base) return BEFORE;
	    if (this.base > other.base) return AFTER;
	    
	    if (this.count < other.count) return BEFORE;
	    if (this.count > other.count) return AFTER;
	    
	    if (this.totalQual < other.totalQual) return BEFORE;
	    if (this.totalQual > other.totalQual) return AFTER;	    
	    	    
	    if (this.avgBaseQual < other.avgBaseQual) return BEFORE;
	    if (this.avgBaseQual > other.avgBaseQual) return AFTER;
	    
	    return EQUAL;
	}
	
	@Override
	public boolean equals(Object o) {
		 if (!(o instanceof Base)) {
			 return false;
		 }
	            
		 Base other = (Base) o;
	     
		 return ( this.base == other.base) &&
	       ( this.count == other.count ) &&
	       ( this.avgBaseQual == other.avgBaseQual ) &&
	       ( this.totalQual == other.totalQual );
	}
	
	@Override
	public int hashCode() {
		 int result = 31;
	     result += base;
	     result += totalQual;
	     result += count;
	     result += (int) avgBaseQual;
	     return result;
	}
	
	
	public String getDCCString() {	
				
		return base + ":" + forwardCount + "[" + formatQual(forAvgBaseQual) + "]," + 
				 + reverseCount + "[" + formatQual(revAvgBaseQual) + "]";
	}

	private String formatQual(double avgQual) {
		if (avgQual == 0) {
			return new String("0");
		} else {
			DecimalFormat df = new DecimalFormat("####0.00");
			return df.format(avgQual);
		}		
	}

	public boolean trueBase() {
		if (base == 'N' || base == 'n') {
			return false;
		}
		return true;		
	}
	
	public long getForTotalQual() {
		return forTotalQual;
	}

	public void setForTotalQual(long forTotalQual) {
		this.forTotalQual = forTotalQual;
	}

	public long getRevTotalQual() {
		return revTotalQual;
	}

	public void setRevTotalQual(long revTotalQual) {
		this.revTotalQual = revTotalQual;
	}

}
