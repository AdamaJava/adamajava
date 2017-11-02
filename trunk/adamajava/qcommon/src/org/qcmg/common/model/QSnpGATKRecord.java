/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.List;

import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

public class QSnpGATKRecord {

	private final VcfRecord gatkVcfRecord;
	private GenotypeEnum genotypeEnum;
	private List<PileupElement> pileup;
	
	public QSnpGATKRecord(VcfRecord vcf) {
		this.gatkVcfRecord = vcf;
		
		if (null != gatkVcfRecord.getFormatFields() && ! gatkVcfRecord.getFormatFields().isEmpty()) {
			//	 set genotype
			genotypeEnum = VcfUtils.getGEFromGATKVCFRec(vcf);
		}
	}
	
	public VcfRecord getVCFRecord() {
		return gatkVcfRecord;
	}
	
	public String getChromosome() {
		return gatkVcfRecord.getChromosome();
	}
	public int getPosition() {
		return gatkVcfRecord.getPosition();
	}
	public String getRef() {
		return gatkVcfRecord.getRef();
	}
	public String getAlt() {
		return gatkVcfRecord.getAlt();
	}
	
	public GenotypeEnum getGenotypeEnum() {
		return genotypeEnum;
	}
	public void setPileup(List<PileupElement> pileup) {
		this.pileup = pileup;
	}
	public List<PileupElement> getPileup() {
		return pileup;
	}
}
