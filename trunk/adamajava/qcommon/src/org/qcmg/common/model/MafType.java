/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public enum MafType {
	
	SNV_SOMATIC (true, true),
	SNV_GERMLINE (true, false),
	INDEL_SOMATIC (false, true),
	INDEL_GERMLINE (false, false);

	
	private boolean isSnv;
	private boolean isSomatic;
	
	private MafType(boolean isSnv, boolean isSomatic) {
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
