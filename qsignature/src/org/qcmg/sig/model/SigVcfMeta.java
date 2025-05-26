/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2021.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.sig.model;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO use by the VcfProfiler class to serialise some key statistics 
 * 
 * @author oliverh
 *
 */
public class SigVcfMeta {
	
	public int getNumberOfPositions() {
		return numberOfPositions;
	}
	public void setNumberOfPositions(int numberOfPositions) {
		this.numberOfPositions = numberOfPositions;
	}

	public void setNumberOfPositionsPercentage(double numberOfPositionsPercentage) {
		this.numberOfPositionsPercentage = numberOfPositionsPercentage;
	}
	public int getUniqueGeneHitCount() {
		return uniqueGeneHitCount;
	}
	public void setUniqueGeneHitCount(int uniqueGeneHitCount) {
		this.uniqueGeneHitCount = uniqueGeneHitCount;
	}

	public void setUniqueGeneHitCountPercentage(double uniqueGeneHitCountPercentage) {
		this.uniqueGeneHitCountPercentage = uniqueGeneHitCountPercentage;
	}
	public int getGeneHitCount() {
		return geneHitCount;
	}
	public void setGeneHitCount(int geneHitCount) {
		this.geneHitCount = geneHitCount;
	}

	public void setGeneHitCountPercentage(double geneHitCountPercentage) {
		this.geneHitCountPercentage = geneHitCountPercentage;
	}
	public int getGeneHitCountPassingCoverage() {
		return geneHitCountPassingCoverage;
	}
	public void setGeneHitCountPassingCoverage(int geneHitCountPassingCoverage) {
		this.geneHitCountPassingCoverage = geneHitCountPassingCoverage;
	}

	public void setGeneHitCountPassingCoveragePercentage(double geneHitCountPassingCoveragePercentage) {
		this.geneHitCountPassingCoveragePercentage = geneHitCountPassingCoveragePercentage;
	}
	public Map<String, int[]> getGeneDist() {
		return geneDist;
	}
	public void setGeneDist(Map<String, int[]> geneDist) {
		this.geneDist = geneDist;
	}
	public Map<Integer, Integer> getHomLengthDistribution() {
		return homLengthDistribution;
	}
	public void setHomLengthDistribution(Map<Integer, Integer> homLengthDistribution) {
		this.homLengthDistribution = homLengthDistribution;
	}
	public int getNumberOfHetPositions() {
		return numberOfHetPositions;
	}
	public void setNumberOfHetPositions(int numberOfHetPositions) {
		this.numberOfHetPositions = numberOfHetPositions;
	}

	public void setNumberOfHomPositionsPercentage(double numberOfHomPositionsPercentage) {
		this.numberOfHomPositionsPercentage = numberOfHomPositionsPercentage;
	}
	public int getNumberOfHomPositions() {
		return numberOfHomPositions;
	}
	public void setNumberOfHomPositions(int numberOfHomPositions) {
		this.numberOfHomPositions = numberOfHomPositions;
	}

	public void setGeneDistDescription(String geneDistDescription) {
		this.geneDistDescription = geneDistDescription;
	}
	private int numberOfPositions;
	private double numberOfPositionsPercentage;
	private int uniqueGeneHitCount;
	private double uniqueGeneHitCountPercentage;
	private int geneHitCount;
	private double geneHitCountPercentage;
	private int geneHitCountPassingCoverage;
	private double geneHitCountPassingCoveragePercentage;
	/*
	 * gene name and int array containing counts of:
	 * [wildtype_hom, wildtype_het, no_wildtype_het, no_wildtype_hom, other] 
	 */
	private String geneDistDescription = "gene name: [wildtype_hom, wildtype_het, no_wildtype_het, no_wildtype_hom, other]";
	private Map<String, int[]> geneDist = new HashMap<>();
	private Map<Integer, Integer> homLengthDistribution;
	
	private int numberOfHetPositions;
	private int numberOfHomPositions;
	private double numberOfHomPositionsPercentage;
}
