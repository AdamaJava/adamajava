/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

public class ResultRecord {
	
	private final String name;
	private long numberPositions;
	private long totalCounts;
	private double totalRegularityScore;

	public ResultRecord(String name, long numberPositions, long totalCounts, double totalRegularityScore) {
		super();
		this.name = name;
		this.numberPositions = numberPositions;
		this.totalCounts = totalCounts;
		this.totalRegularityScore = totalRegularityScore;
	}
	
	public String getName() {
		return name;
	}
	
	public long getNumberPositions() {
		return numberPositions;
	}

	public long getTotalCounts() {
		return totalCounts;
	}

	public String toTmpString() {		
		return numberPositions + "\t" + totalCounts + "\t" + totalRegularityScore + "\t";
	}

	@Override
	public String toString() {
		return numberPositions + "\t" + totalCounts + "\t";
	}

	public double getTotalRegularityScore() {
		return totalRegularityScore;
	}

	public void mergeRecords(ResultRecord record) {	
		this.numberPositions += record.getNumberPositions();
		this.totalCounts += record.getTotalCounts();
		this.totalRegularityScore += record.getTotalRegularityScore();
		
	}

	public double getAverageScore() {		
		return (double)totalCounts/(double)numberPositions;
	}


	

}
