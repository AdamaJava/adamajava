/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf.util;

public class FilterOptions {
	
	public static final int HOMOPOLYMER_CUTOFF = 6;
	
	private int homopolymerCutoff = HOMOPOLYMER_CUTOFF;

	public int getHomopolymerCutoff() {
		return homopolymerCutoff;
	}

	public void setHomopolymerCutoff(int homopolymerCutoff) {
		this.homopolymerCutoff = homopolymerCutoff;
	}
	
	

}
