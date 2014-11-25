/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.List;

import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.VcfUtils;

public class QSnpGATKRecord {

	private final VCFRecord gatkVcfRecord;
	private String genotype;
	private GenotypeEnum genotypeEnum;
	private String annotation;
	private List<PileupElement> pileup;
//	private List<PileupElement> novelStartPileup;
//	private int novelStartCount;
	
	public QSnpGATKRecord(VCFRecord vcf) {
		this.gatkVcfRecord = vcf;
		
		if (null != gatkVcfRecord.getFormatFields() && ! gatkVcfRecord.getFormatFields().isEmpty()) {
			//	 set genotype
			genotype = VcfUtils.getGenotypeFromGATKVCFRecord(vcf);
			genotypeEnum = VcfUtils.getGEFromGATKVCFRec(vcf);
		}
	}
	
	public VCFRecord getVCFRecord() {
		return gatkVcfRecord;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getDPFromFormatField() {
		if (null != gatkVcfRecord) {
			List<String> extraFields = gatkVcfRecord.getFormatFields();
			if (extraFields.size() > 1)
				return VcfUtils.getDPFromFormatField(extraFields.get(1));
		}
		return -1;
	}
	
	public String getChromosome() {
		return gatkVcfRecord.getChromosome();
	}
	public int getPosition() {
		return gatkVcfRecord.getPosition();
	}
//	public void setPosition(final int position) {
//		if (null == gatkVcfRecord) gatkVcfRecord = new VCFRecord();
//		gatkVcfRecord.setPosition(position);
//	}
	public String getRef() {
		return gatkVcfRecord.getRef();
	}
	public String getAlt() {
		return gatkVcfRecord.getAlt();
	}
	
	public String getGenotype() {
		return genotype;
	}
//	public void setGenotype(String genotype) {
//		this.genotype = genotype;
//	}
	
	public GenotypeEnum getGenotypeEnum() {
		return genotypeEnum;
	}
//	public void setGenotypeEnum(GenotypeEnum genotypeEnum) {
//		this.genotypeEnum = genotypeEnum;
//	}
	public String getAnnotation() {
		return annotation;
	}
//	public void setAnnotation(String annotation) {
//		this.annotation = annotation;
//	}
//	public void addAnnotation(String annotation) {
//		if (null == this.annotation)
//			this.annotation = annotation;
//		else
//			this.annotation += "; " +  annotation;
//	}
	public void setPileup(List<PileupElement> pileup) {
		this.pileup = pileup;
	}
	public List<PileupElement> getPileup() {
		return pileup;
	}
//	public void setNovelStartPileup(List<PileupElement> novelStartPileup) {
//		this.novelStartPileup = novelStartPileup;
//	}
//	public List<PileupElement> getNovelStartPileup() {
//		return novelStartPileup;
//	}

//	public void setNovelStartCount(int novelStartCount) {
//		this.novelStartCount = novelStartCount;
//	}

//	public int getNovelStartCount() {
//		return novelStartCount;
//	}
	
}
