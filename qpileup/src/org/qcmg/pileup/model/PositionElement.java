/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;


public class PositionElement {
	
	private final String chr;
	private final long position;
	private final char base;
	
	public PositionElement(String chr, long position, char base) {
		this.position = position;
		this.base = base;
		this.chr = chr;
	}
	
	public long getPosition() {
		return position;
	}
	public char getBase() {
		return base;
	}
	
	public String getChr() {
		return chr;
	}
	@Override
	public String toString() {
		return chr + "," + base + "," + position;
	}

}
