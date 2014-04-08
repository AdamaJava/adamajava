/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import org.qcmg.common.string.StringUtils;

public class RegionCounter {
	
	private final RegionType type;
	private String motifsFS;
	private String motifsRS;
	private int totalCoverage;
	private int stage1Cov;
	private int stage2Cov;
	
	
	public RegionCounter(RegionType type) {
		this.type = type;
	}
	
	public void addMotif(String motif, boolean forwardStrand, boolean isMapped) {
		if (StringUtils.isNullOrEmpty(motif)) throw new IllegalArgumentException("Null or empty motif passed to addMotif");
		
		// should we check the type here and only increment if its an incremental type? - I think so
		if (type.acceptRead(isMapped)) {
			if (forwardStrand) {
				motifsFS = MotifUtils.addMotifToString(motifsFS, motif);
			} else {
				motifsRS = MotifUtils.addMotifToString(motifsRS, motif);
			}
		}
	}
	
	public boolean hasMotifs() {
		return ! StringUtils.isNullOrEmpty(motifsFS) || ! StringUtils.isNullOrEmpty(motifsRS); 
	}
	
	public RegionType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "RegionCounter [type=" + type + ", motifsFS=" + motifsFS
				+ ", motifsRS=" + motifsRS + "]";
	}

	public String getMotifsForwardStrand() {
		return motifsFS;
	}
	public String getMotifsReverseStrand() {
		return motifsRS;
	}
	
	public void updateTotalCoverage() {
		totalCoverage ++;
	}
	public void updateStage1Coverage() {
		stage1Cov ++;
	}
	public void updateStage2Coverage() {
		stage2Cov ++;
	}
	
	public int getTotalCoverage() {
		return totalCoverage;
	}
	public int getStage1Coverage() {
		return stage1Cov;
	}
	public int getStage2Coverage() {
		return stage2Cov;
	}
	
	
}
