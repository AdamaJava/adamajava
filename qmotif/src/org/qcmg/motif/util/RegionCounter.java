/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.string.StringUtils;

import gnu.trove.map.hash.THashMap;

public class RegionCounter {
	
	private final static String MOTIF_DELIMITER = MotifUtils.M_D + "";
	
	private final RegionType type;
	private int stage1Cov;
	private int stage2Cov;
	
	private final Map<String, AtomicInteger> motifsFS = new THashMap<>(32);
	private final Map<String, AtomicInteger> motifsRS = new THashMap<>(32);
	
	
	public RegionCounter(RegionType type) {
		this.type = type;
	}
	
	public void addMotif(String motif, boolean forwardStrand, boolean isMapped) {
		if (StringUtils.isNullOrEmpty(motif)) {
			throw new IllegalArgumentException("Null or empty motif passed to addMotif");
		}
		
		// should we check the type here and only increment if its an incremental type? - I think so
		if (type.acceptRead(isMapped)) {
			String [] motifs = new String[] {motif};
			if (motif.indexOf(MotifUtils.M_D) > -1) {
				motifs = motif.split(MOTIF_DELIMITER);
			}
			if (forwardStrand) {
				for (String m : motifs) {
					motifsFS.computeIfAbsent(m, f -> new AtomicInteger()).incrementAndGet();
				}
			} else {
				for (String m : motifs) {
					motifsRS.computeIfAbsent(m, f -> new AtomicInteger()).incrementAndGet();
				}
			}
		}
	}
	
	public boolean hasMotifs() {
		return motifsFS.size() > 0 || motifsRS.size() > 0;
	}
	
	public RegionType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "RegionCounter [type=" + type + ", motifsFS=" + motifsFS
				+ ", motifsRS=" + motifsRS + "]";
	}

	public Map<String, AtomicInteger> getMotifsForwardStrand() {
		return motifsFS;
	}
	public  Map<String, AtomicInteger> getMotifsReverseStrand() {
		return motifsRS;
	}
	
	public void updateStage1Coverage() {
		stage1Cov ++;
	}
	public void updateStage2Coverage() {
		stage2Cov ++;
	}
	
	public int getStage1Coverage() {
		return stage1Cov;
	}
	public int getStage2Coverage() {
		return stage2Cov;
	}
	
}
