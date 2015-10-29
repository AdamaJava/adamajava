/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

/*
 * A wrapper class to allow ease of returning both a read with its matched position
 */
public class ReadMatch {
	private final Read read;
	private final int matchedPos;
	
	public ReadMatch (Read read, int matchedPos) {
		this.read = read;
		this.matchedPos = matchedPos;
	}

	public Read read() {
		return read;
	}

	public int matchedPos() {
		return matchedPos;
	}

		
}
