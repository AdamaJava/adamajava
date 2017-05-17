/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qlib.qpileup;


public class NonReferenceRecord {
	
	String reference;
	int[] totalBases;
	int[] nonReferences;
	boolean isReverse;
	private boolean isLowReadCount;
	private boolean isHighNonRef;
	private final int lowReadCount;
	private final double highNonRefThreshold;

	public NonReferenceRecord(String reference, int datasetLength, boolean isReverse, int lowReadCount, int highNonRef) {
		this.reference = reference;
		this.totalBases = new int[datasetLength];
		this.nonReferences = new int[datasetLength];
		this.isReverse = isReverse;
		this.isLowReadCount = false;
		this.isHighNonRef = false;
		this.lowReadCount = lowReadCount;
		this.highNonRefThreshold = highNonRef/100.0;
	}
	
	public int[] getTotalBases() {
		return totalBases;
	}

	public void setTotalBases(int[] totalBases) {
		this.totalBases = totalBases;
	}

	public int[] getNonReferences() {
		return nonReferences;
	}


	public void setNonReferences(int[] nonReferences) {
		this.nonReferences = nonReferences;
	}

	public int getNonReference(int i) {
		return nonReferences[i];
	}
	
	public int getTotalBases(int i) {
		return totalBases[i];
	}

	public boolean isLowReadCount() {
		return isLowReadCount;
	}


	public void setLowReadCount(boolean isLowReadCount) {
		this.isLowReadCount = isLowReadCount;
	}

	public boolean isHighNonRef() {
		return isHighNonRef;
	}

	public void setHighNonRef(boolean isHighNonRef) {
		this.isHighNonRef = isHighNonRef;
	}

	public void addNonReferenceMetrics(PileupDataRecord dataRecord, int index) {
			totalBases[index] += dataRecord.getTotalBases();
			nonReferences[index] += dataRecord.getNonReferenceNo();
	}

	public void defineNonReferenceMetrics(int index) {
		
		int nonReferenceCount = nonReferences[index];
		int totalBaseCount = totalBases[index];

		if (totalBaseCount < lowReadCount) {
			isLowReadCount = true;
		} else {
			isLowReadCount = false;
			Double total = new Double(totalBaseCount);
			Double nonRef = new Double(nonReferenceCount);
			Double percent = nonRef/total;
			isHighNonRef = percent >= highNonRefThreshold;
		}
	}
}
