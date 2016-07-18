package org.qcmg.sig.model;

public class BaseReadGroup {
	
	private final char base;
	private final String rg;
	
	public BaseReadGroup(char base, String rg) {
		this.base = base;
		this.rg = rg;
	}
	
	public char getBase() {
		return base;
	}

	public String getReadGroup() {
		return rg;
	}
}
