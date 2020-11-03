/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.pileup;


import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.vcf.VcfRecord;

public class QSnpRecord {
		
	private final VcfRecord vcf;
	private String alt;
	private GenotypeEnum controlGenotype;
	private GenotypeEnum testGenotype;
	private Accumulator control;
	private int normalNovelStartCount;
	private int tumourNovelStartCount;
	private Classification classification;
	private String flankingSequence;
	
	public QSnpRecord(VcfRecord vcf) {
		this.vcf = vcf;
	}
	
	public QSnpRecord(String chr, int position, String ref) {
		this(chr, position, ref, null);
	}
	
	public QSnpRecord(String chr, int position, String ref, String alt) {
		vcf = new VcfRecord.Builder(chr, position, ref).allele(alt).build();		
	}
	
	public void setAlt(String alt) {
		this.alt = alt;
	}
	
	public ChrPosition getChrPos() {
		return vcf.getChrPosition();
	}
	
	public String getRef() {
		return vcf.getRef();
	}
	public String getAlt() {
		return null != vcf.getAlt() ? vcf.getAlt() : alt;
	}
	
	public String getChromosome() {
		return vcf.getChromosome();
	}
	
	public int getPosition() {
		return vcf.getPosition();
	}
	
	public GenotypeEnum getNormalGenotype() {
		return controlGenotype;
	}
	
	public void setNormalGenotype(GenotypeEnum normalGenotype) {
		this.controlGenotype = normalGenotype;
	}
	
	public GenotypeEnum getTumourGenotype() {
		return testGenotype;
	}
	
	public void setTumourGenotype(GenotypeEnum tumourGenotype) {
		this.testGenotype = tumourGenotype;
	}
	
	public void setClassification(Classification classification) {
		this.classification = classification;
	}
	
	public Classification getClassification() {
		return classification;
	}	
	
	public String getAnnotation() {
		return vcf.getFilter();
	}
	
	public String getUnfilteredNormalPileup() {
		return null != control ? control.getFailedFilterPileup() : null;
	}
	
	public int getNovelStartCount() {
		return Classification.GERMLINE != classification ? tumourNovelStartCount : normalNovelStartCount;
	}
	
	public void setNormalNovelStartCount(int normalNovelStartCount) {
		this.normalNovelStartCount = normalNovelStartCount;
	}
	
	public int getNormalNovelStartCount() {
		return normalNovelStartCount;
	}
	public void setTumourNovelStartCount(int tumourNovelStartCount) {
		this.tumourNovelStartCount = tumourNovelStartCount;
	}
	
	public int getTumourNovelStartCount() {
		return tumourNovelStartCount;
	}
	
	public void setFlankingSequence(String flankingSequence) {
		this.flankingSequence = flankingSequence;
	}
	
	public String getFlankingSequence() {
		return flankingSequence;
	}

	public VcfRecord getVcfRecord() {
		return vcf;
	}
}
