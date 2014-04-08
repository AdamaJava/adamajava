/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

public class ResultRecord {
	
	public String name;
	public long numberPositions = 0;
	public long totalCounts = 0;
	public double totalRegularityScore = 0;

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

	public void setNumberPositions(long numberPositions) {
		this.numberPositions = numberPositions;
	}

	public long getTotalCounts() {
		return totalCounts;
	}

	public void setTotalCounts(long totalCounts) {
		this.totalCounts = totalCounts;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toTmpString() {		
		return numberPositions + "\t" + totalCounts + "\t" + totalRegularityScore + "\t";
	}

	public String toString() {
		return numberPositions + "\t" + totalCounts + "\t";
	}

	public double getTotalRegularityScore() {
		return totalRegularityScore;
	}

	public void setTotalRegularityScore(double regularityScore) {
		this.totalRegularityScore = regularityScore;
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
