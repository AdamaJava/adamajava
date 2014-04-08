/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.coverage;

public class RangePosition {
	
	final String chr;
	final int start;
	final int end;
	
	public RangePosition(String chr, int start, int end) {
		super();
		this.chr = chr;
		this.start = start;
		this.end = end;
	}
	public String getChr() {
		return chr;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	
	@Override
	public String toString() {
		return chr + "\t" + start + "\t" + end;
	}
	
}
