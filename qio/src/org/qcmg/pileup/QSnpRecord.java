/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import static org.qcmg.common.util.Constants.TAB;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;

public class QSnpRecord {
	
	
	private int id;
	private final VcfRecord vcf;
//	private String pileup;
	private GenotypeEnum normalGenotype;
	private GenotypeEnum tumourGenotype;
	private int normalCount;
	private int tumourCount;
	private int normalNovelStartCount;
	private int tumourNovelStartCount;
	private Classification classification;
	private String mutation;
	private String normalPileup;
	private String unfilteredNormalPileup;
//	private Double probablility;
	private String normalNucleotides;
	private String tumourNucleotides;
	private String normalOABS;
	private String tumourOABS;
	private String flankingSequence;
	
	public QSnpRecord(VcfRecord vcf) {
		this.vcf = vcf;
	}
	
	public QSnpRecord(String chr, int position, String ref) {
		this(chr, position, ref, null);
	}
	public QSnpRecord(String chr, int position, String ref, String alt) {
//		int length = StringUtils.isNullOrEmpty(ref) ? 1 : ref.length();
		//vcf = VcfUtils.createVcfRecord(new ChrPosition(chr, position, (position + length) -1), null, ref, alt);
		vcf = new VcfRecord.Builder(chr, position, ref).allele(alt).build();
		
	}
	
	public ChrPosition getChrPos() {
		return vcf.getChrPosition();
	}
	
	public String getRef() {
		return vcf.getRef();
	}
	public String getAlt() {
		return vcf.getAlt();
	}
	public String getChromosome() {
		return vcf.getChromosome();
	}
	public int getPosition() {
		return vcf.getPosition();
	}
//	public String getPileup() {
//		return pileup;
//	}
//	public void setPileup(String pileup) {
//		this.pileup = pileup;
//	}
	public GenotypeEnum getNormalGenotype() {
		return normalGenotype;
	}
	public void setNormalGenotype(GenotypeEnum normalGenotype) {
		this.normalGenotype = normalGenotype;
	}
	public GenotypeEnum getTumourGenotype() {
		return tumourGenotype;
	}
	public void setTumourGenotype(GenotypeEnum tumourGenotype) {
		this.tumourGenotype = tumourGenotype;
	}
	public void setNormalCount(int normalCount) {
		this.normalCount = normalCount;
	}
	public int getNormalCount() {
		return normalCount;
	}
	public void setClassification(Classification classification) {
		this.classification = classification;
	}
	public Classification getClassification() {
		return classification;
	}
	public void setTumourCount(int tumourCount) {
		this.tumourCount = tumourCount;
	}
	public int getTumourCount() {
		return tumourCount;
	}
	
	public String getAnnotation() {
		return vcf.getFilter();
	}
	public void setMutation(String mutation) {
		this.mutation = mutation;
	}
	public String getMutation() {
		return mutation;
	}
	
	public void setNormalPileup(String normalPileup) {
		this.normalPileup = normalPileup;
	}
	public String getNormalPileup() {
		return normalPileup;
	}
	
	public String getFormattedString() {
//		return pileup + TAB
		return (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB;
	}
	
	public String getGATKFormattedString() {
		return vcf.getChromosome() + TAB
		+ vcf.getPosition() + TAB
		+ vcf.getRef() + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB
		+ (StringUtils.isNullOrEmpty(normalNucleotides) ? "--" : normalNucleotides) + TAB	 
		+ (StringUtils.isNullOrEmpty(tumourNucleotides) ? "--" : tumourNucleotides);
	}
	
	public String getDCCData(final String mutationIdPrefix, final String chr) {
		StringBuilder sb = new StringBuilder();
		sb.append(mutationIdPrefix + id).append(TAB);
		sb.append("1").append(TAB);
		sb.append(chr).append(TAB);
		sb.append( vcf.getPosition()).append(TAB);
		sb.append( vcf.getPosition()).append(TAB);
		sb.append(1).append(TAB);
		sb.append(vcf.getRef()).append(TAB);
		sb.append(null != normalGenotype ? normalGenotype.getDisplayString() : "--").append(TAB);
		sb.append(null != tumourGenotype ? tumourGenotype.getDisplayString() : "--").append(TAB);
		if (Classification.GERMLINE != classification) {
			sb.append(null != mutation ? mutation : "").append(TAB);
		}
		sb.append("-999").append(TAB);		// expressed_allele
		sb.append("-999").append(TAB);		// quality_score
		sb.append("-999").append(TAB);		// probability
//		sb.append(null != probablility ? probablility.toString() : "-999").append(TAB);	// probability
		sb.append(Classification.GERMLINE != classification ? tumourCount : normalCount).append(TAB);
		sb.append(StringUtils.isNullOrEmpty(vcf.getFilter()) ? "--" : vcf.getFilter()).append(TAB);
		sb.append(StringUtils.isNullOrEmpty(normalNucleotides) ? "--" : normalNucleotides).append(TAB);
		sb.append(StringUtils.isNullOrEmpty(tumourNucleotides) ? "--" : tumourNucleotides);
		
		return sb.toString();
	}
	
	public String getDCCDataNS(final String mutationId, final String chr) {
		StringBuilder sb = new StringBuilder(getDCCData(mutationId, chr));
		sb.append(TAB).append(getNovelStartCount());
		return sb.toString();
	}
	public String getDCCDataNSFlankingSeq(final String mutationId, final String chr) {
		StringBuilder sb = new StringBuilder(getDCCDataNS(mutationId, chr));
		sb.append(TAB).append(null != getFlankingSequence() ? getFlankingSequence() :"--");
		return sb.toString();
	}
	
	public String getGermlineDBData() {
		return   vcf.getPosition() + TAB
		+  vcf.getPosition() + TAB
		+ 1 + TAB	// strand - always set to 1 ???
		+ vcf.getRef() + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB
		+ "-999\t"		// quality_score
		+ "-999\t"		// probability
		+ (normalCount + tumourCount) + TAB;	// read count 
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public String getNormalNucleotides() {
		return normalNucleotides;
	}
	public void setNormalNucleotides(String normalNucleotides) {
		this.normalNucleotides = normalNucleotides;
	}
	public String getTumourNucleotides() {
		return tumourNucleotides;
	}
	public void setTumourNucleotides(String tumourNucleotides) {
		this.tumourNucleotides = tumourNucleotides;
	}
	public void setTumourOABS(String tumourOABS) {
		this.tumourOABS = tumourOABS;
	}
	public void setNormalOABS(String normalOABS) {
		this.normalOABS = normalOABS;
	}
	public String getNormalOABS() {
		return normalOABS;
//		return null == normalNucleotides ? null : normalNucleotides.replaceAll(Constants.COMMA_STRING, Constants.EMPTY_STRING);
	}
	public String getTumourOABS() {
		return tumourOABS;
//		return null == tumourNucleotides ? null : tumourNucleotides.replaceAll(Constants.COMMA_STRING, Constants.EMPTY_STRING);
	}
	public void setUnfilteredNormalPileup(String unfilteredNormalPileup) {
		this.unfilteredNormalPileup = unfilteredNormalPileup;
	}
	public String getUnfilteredNormalPileup() {
		return unfilteredNormalPileup;
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
//	public double getProbability() {
//		return probablility;
//	}
//	public void setProbability(double probability) {
//		this.probablility = Double.valueOf(probability);
//	}

	public VcfRecord getVcfRecord() {
		return vcf;
	}
}
