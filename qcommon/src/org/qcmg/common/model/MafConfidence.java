/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public enum MafConfidence {
	
	HIGH(true, false),
	HIGH_CONSEQUENCE(true, false),
	LOW(false, true),
	LOW_CONSEQUENCE(false, true),
	ZERO(false, false);
	
	private final boolean isHighConf;
	private final boolean isLowConf;
	
	MafConfidence(boolean isHighConfidence, boolean isLowConfidence) {
		this.isHighConf = isHighConfidence;
		this.isLowConf = isLowConfidence;
	}
	
	public boolean isHighConfidence() {
		return isHighConf;
	}
	public boolean isLowConfidence() {
		return isLowConf;
	}
	public boolean isHighOrLowConfidence() {
		return isHighConf || isLowConf;
	}
}
