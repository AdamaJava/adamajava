/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.model;

public class BaseStrandPosition {
	
	private final char base;
	private final boolean forwardStrand;
	private final int startPosition;
	
	public BaseStrandPosition(char base, boolean forwardStrand, int position) {
		this.base = base;
		this.forwardStrand = forwardStrand;
		this.startPosition = position;
	}

	public char getBase() {
		return base;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public boolean isForwardStrand() {
		return forwardStrand;
	}
}
