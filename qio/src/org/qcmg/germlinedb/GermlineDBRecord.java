/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.germlinedb;


public class GermlineDBRecord {
	private String chromosome;
	private int position;
	private String variationId;
	private String normalGenotype;
	private String data;
	private String flag;
	
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
	public String getNormalGenotype() {
		return normalGenotype;
	}
	public void setNormalGenotype(String normalGenotype) {
		this.normalGenotype = normalGenotype;
	}
	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}
	public String getVariationId() {
		return variationId;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
}
