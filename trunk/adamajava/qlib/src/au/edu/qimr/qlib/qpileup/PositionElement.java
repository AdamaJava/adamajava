/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;

public class PositionElement {
	
	String chr;
	long position;
	char base;
	
	public PositionElement(String chr, long position, char base) {
		super();
		this.position = position;
		this.base = base;
		this.chr = chr;
	}
	
	public long getPosition() {
		return position;
	}
	public void setPosition(long position) {
		this.position = position;
	}
	public char getBase() {
		return base;
	}
	public void setBase(char base) {
		this.base = base;
	}	
	
	public String getChr() {
		return chr;
	}

	public void setChr(String chr) {
		this.chr = chr;
	}
	
	@Override
	public String toString() {
		return chr + "," + base + "," + position;
	}

}
