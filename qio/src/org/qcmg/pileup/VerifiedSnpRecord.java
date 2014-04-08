/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import org.qcmg.pileup.QSnpRecord.Classification;


public class VerifiedSnpRecord {
	
	private String id;
	private String chromosome;
	private int position;
	private String mutation;
	private Classification classification;
	private String clazz;
	private String status;
	
	private String analysis;	// should be either exome or wgs_only
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getClazz() {
		return clazz;
	}
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getChromosome() {
		return chromosome;
	}
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public void setClassification(Classification classification) {
		this.classification = classification;
	}
	public Classification getClassification() {
		return classification;
	}
	public void setMutation(String mutation) {
		this.mutation = mutation;
	}
	public String getMutation() {
		return mutation;
	}
	
	public String getFormattedString() {
		return id + "\t"
		+ chromosome + ":" + position + "-" + position + "\t"
		+ mutation + "\t"
		+ (null != classification ? classification.toString().toLowerCase() + "\t" : "")
		 + (null !=clazz ? clazz + "\t": "")
		+ status + "\t"
		+ analysis;
	}
	public void setAnalysis(String analysis) {
		this.analysis = analysis;
	}
	public String getAnalysis() {
		return analysis;
	}
	
}
