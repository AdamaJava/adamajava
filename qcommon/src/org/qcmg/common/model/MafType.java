/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public enum MafType {
	
	SNV_SOMATIC (true, true),
	SNV_GERMLINE (true, false),
	INDEL_SOMATIC (false, true),
	INDEL_GERMLINE (false, false);

	
	private final boolean isSnv;
	private final boolean isSomatic;
	
	MafType(boolean isSnv, boolean isSomatic) {
		this.isSnv = isSnv;
		this.isSomatic = isSomatic;
	}
	
	public boolean isSomatic() {
		return isSomatic;
	}
	public boolean isGermline() {
		return ! isSomatic;
	}
	public boolean isIndel() {
		return ! isSnv;
	}
}
