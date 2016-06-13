/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

public enum TorrentVerificationStatus {

	COVERAGE("coverage", false, false, "Unknown(Coverage)"),	// do nothing to MAF we can’t say if verified or not
	NOT_TESTED("not tested", false, false, "Unknown(Not Tested)"),	// do nothing to MAF we can’t say if verified or not
	YES("yes", true, false, "Valid"),	// position verified – update MAF and move low confidence to calls to high confidence MAF if possible
	NO("no", false, true, "False"),	// position did not verify – remove from MAF
	GERMLINE("germline", false, true, "False(Germline)");	// position is s germline variant – remove from MAF
	
	
	private final String name;
	private final boolean verified;
	private final boolean removeFromMaf;
	private final String mafDisplayName;
	
	private TorrentVerificationStatus(String name, boolean verified, boolean removeFromMaf, String mafDisplayName) {
		this.name = name;
		this.verified = verified;
		this.removeFromMaf = removeFromMaf;
		this.mafDisplayName = mafDisplayName;
	}
	
	public static final TorrentVerificationStatus getVerificationStatus(String name) {
		for (TorrentVerificationStatus verStatus : values())
			if (verStatus.name.equals(name)) return verStatus;
		
		return null;
	}
	
	public boolean removeFromMaf() {
		return removeFromMaf;
	}
	public boolean verified() {
		return verified;
	}

	public String getMafDisplayName() {
		return mafDisplayName;
	}
	
	public static final TorrentVerificationStatus getVerificationStatusFromDisplayName(String displayName) {
		for (TorrentVerificationStatus verStatus : values())
			if (verStatus.mafDisplayName.equals(displayName)) return verStatus;
		
		return null;
	}
}
