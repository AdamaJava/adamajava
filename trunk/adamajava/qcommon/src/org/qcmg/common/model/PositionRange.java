/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public class PositionRange implements Comparable<PositionRange>{

	private final int start;
	private int end;
	
	public PositionRange(int start, int end) {
		if (start < 0)  throw new IllegalArgumentException("Start position cannot be less than zero");
		if (start > end) throw new IllegalArgumentException("Start position cannot be greater than the end position");
		
		this.start = start;
		this.end = end;
	}
	
	public boolean isAdjacentToEnd(int position) {
		return position == end + 1;
	}
	
	public void extendRange(int newEndPosition) {
		if (start > newEndPosition) throw new IllegalArgumentException("Start position cannot be greater than the end position");
		this.end = newEndPosition;
	}
	
	public boolean containsPosition(int position) {
		return position >= start && position <= end;
	}
	
	@Override
	public int compareTo(PositionRange arg0) {
		return start - arg0.start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PositionRange other = (PositionRange) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

}
