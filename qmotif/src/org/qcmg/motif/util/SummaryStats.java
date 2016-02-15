/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.Map;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.ChrPositionName;

public class SummaryStats {
	
	private Map<ChrPosition, RegionCounter> results;
	private long totalReadCount;
	private long windowSize;
	private long cutoff;
	private long uniqueMotifCount;
	private String bamFileName;
	
	private long rawUnmapped;
	private long rawIncludes;
	private long rawGenomic;
	
	private long scaledUnmapped;
	private long scaledIncludes;
	private long scaledGenomic;
	
	
	
	public Map<ChrPosition, RegionCounter> getResults() {
		return results;
	}
	public long getTotalReadCount() {
		return totalReadCount;
	}
	public long getWindowSize() {
		return windowSize;
	}
	public long getCutoff() {
		return cutoff;
	}
	public long getUniqueMotifCount() {
		return uniqueMotifCount;
	}
	public String getBamFileName() {
		return bamFileName;
	}
	public long getRawUnmapped() {
		return rawUnmapped;
	}
	public long getRawIncludes() {
		return rawIncludes;
	}
	public long getRawGenomic() {
		return rawGenomic;
	}
	public long getScaledUnmapped() {
		return scaledUnmapped;
	}
	public long getScaledIncludes() {
		return scaledIncludes;
	}
	public long getScaledGenomic() {
		return scaledGenomic;
	}
	public void setResults(Map<ChrPosition, RegionCounter> results) {
		this.results = results;
	}
	public void setTotalReadCount(long totalReadCount) {
		this.totalReadCount = totalReadCount;
	}
	public void setWindowSize(long windowSize) {
		this.windowSize = windowSize;
	}
	public void setCutoff(long cutoff) {
		this.cutoff = cutoff;
	}
	public void setUniqueMotifCount(long uniqueMotifCount) {
		this.uniqueMotifCount = uniqueMotifCount;
	}
	public void setBamFileName(String bamFileName) {
		this.bamFileName = bamFileName;
	}
	public void setRawUnmapped(long rawUnmapped) {
		this.rawUnmapped = rawUnmapped;
	}
	public void setRawIncludes(long rawIncludes) {
		this.rawIncludes = rawIncludes;
	}
	public void setRawGenomic(long rawGenomic) {
		this.rawGenomic = rawGenomic;
	}
	public void setScaledUnmapped(long scaledUnmapped) {
		this.scaledUnmapped = scaledUnmapped;
	}
	public void setScaledIncludes(long scaledIncludes) {
		this.scaledIncludes = scaledIncludes;
	}
	public void setScaledGenomic(long scaledGenomic) {
		this.scaledGenomic = scaledGenomic;
	}
	
	

}
