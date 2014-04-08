/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

public enum MotifMode {
	
	// StageOne_StageTwo
	
	STRING_STRING (true, true),
	STRING_REGEX (true, false),
	REGEX_STRING (false, true),
	REGEX_REGEX (false, false);
	
	private boolean stageOneString;
	private boolean stageTwoString;
	
	private MotifMode(boolean stageOneString, boolean stageTwoString) {
		this.stageOneString = stageOneString;
		this.stageTwoString = stageTwoString;
	}
	
	
	public boolean stageOneString() {
		return stageOneString;
	}
	public boolean stageTwoString() {
		return stageTwoString;
	}

}
