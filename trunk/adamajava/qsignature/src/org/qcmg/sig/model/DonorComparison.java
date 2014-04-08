/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.model.ChrPosition;

public class DonorComparison {
	
	private final String donor;
	private final List<File> snpChipFiles;
	private final List<File> bamFiles;
	private Map<File, Map<ChrPosition, double[]>> rawData;
	private final List<Comparison> results = new ArrayList<>();
	
	
	public DonorComparison(String donor) {
		this.donor = donor;
		this.snpChipFiles = new ArrayList<>();;
		this.bamFiles = new ArrayList<>();
		this.rawData = new HashMap<>();
	}
	public DonorComparison(String donor, List<File> snpChips, List<File> bams) {
		this.donor = donor;
		this.snpChipFiles = snpChips;
		this.bamFiles = bams;
		this.rawData = new HashMap<>();	
	}
	
	public void setRawData(Map<File, Map<ChrPosition, double[]>> data) {
		this.rawData = data;
	}
	
//	public void addDataToCache(File f, Map<ChrPosition, double[]> ratios) {
//		rawData.put(f, ratios);
//	}
	
	public Map<File, Map<ChrPosition, double[]>> getCache() {
		return rawData;
	}
	
	public String getDonor() {
		return donor;
	}
	
	public void addSnpChipFile(File f) {
		snpChipFiles.add(f);
	}
	public void addBamFile(File f) {
		bamFiles.add(f);
	}
	
	public List<File> getSnpChipFiles() {
		return snpChipFiles;
	}
	public List<File> getBamFiles() {
		return bamFiles;
	}
	
	public void addComparison(Comparison comp) {
		// quick check to make sure we've got the right comparisons here
		if ( ! comp.getMain().getAbsolutePath().contains(donor)) {
			throw new IllegalArgumentException("Incorrect comparison object passed to donorComp: " + donor + ", " + comp.getMain().getAbsolutePath());
		}
		results.add(comp);
	}
	
	public List<Comparison> getComparisons() {
		return results;
	}
	@Override
	public String toString() {
		
		int snpChipFileSize = null != snpChipFiles ? snpChipFiles.size() : 0;
		int bamFilesSize = null != bamFiles ? bamFiles.size() : 0;
		int resultsSize = null != results ? results.size() : 0;
		
		
		return "DonorComparison [donor=" + donor + ", snpChipFiles size="
				+ snpChipFileSize + ", bamFiles size=" + bamFilesSize + ", results size="
				+ resultsSize + "]";
	}
	
	

}
