/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import org.qcmg.common.string.StringUtils;

public class RegionCounter {
	
	private final RegionType type;
//	private Map<String, AtomicInteger> motifsFS;
//	private Map<String, AtomicInteger> motifsRS;
	private StringBuilder motifsFS;
	private StringBuilder motifsRS;
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
				if (null == motifsFS) {
					motifsFS = new StringBuilder();
				}
				MotifUtils.addMotifToString(motifsFS, motif);
			} else {
				if (null == motifsRS) {
					motifsRS =  new StringBuilder();
				}
				MotifUtils.addMotifToString(motifsRS, motif);
			}
		}
	}
	
	public boolean hasMotifs() {
		
//		return null != motifsFS && ! motifsFS.isEmpty() 
//				|| null != motifsRS && ! motifsRS.isEmpty();
		
		return (null != motifsFS && motifsFS.length() > 0)
				|| (null != motifsRS && motifsRS.length() > 0);
		
		
//		return ! StringUtils.isNullOrEmpty(motifsFS.toString()) || ! StringUtils.isNullOrEmpty(motifsRS.toString()); 
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
//		public Map<String, AtomicInteger> getMotifsForwardStrand() {
//		return motifsFS ;
		return null == motifsFS ? null :  motifsFS.toString();
	}
	public  String getMotifsReverseStrand() {
//		public  Map<String, AtomicInteger> getMotifsReverseStrand() {
//		return motifsRS;
		return null == motifsRS ? null :  motifsRS.toString();
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
