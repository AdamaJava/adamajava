/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import static org.qcmg.common.util.Constants.TAB;

import java.util.function.UnaryOperator;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;

public class QSnpRecord {
	
	private static UnaryOperator<String> convertOABSToNucleotides = (String s) -> 
	{if (null != s) {
		String oabs = s.replaceAll(Constants.SEMI_COLON_STRING, Constants.EMPTY_STRING).replaceAll(Constants.CLOSE_SQUARE_BRACKET+"", Constants.CLOSE_SQUARE_BRACKET+Constants.COMMA_STRING);
		return oabs.substring(0,  oabs.length() -1);
	} else return null;};
	
	private int id;
	private final VcfRecord vcf;
	private GenotypeEnum normalGenotype;
	private GenotypeEnum tumourGenotype;
	private int normalCount;
	private int tumourCount;
	private int normalNovelStartCount;
	private int tumourNovelStartCount;
	private Classification classification;
	private String mutation;
	private String failedFilterControl;
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
//	public void setNormalPileup(String normalPileup) {
//		this.normalPileup = normalPileup;
//	}
//	public String getNormalPileup() {
//		return normalPileup;
//	}
	
	public String getFormattedString() {
		return (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB;
	}
	
	public String getGATKFormattedString() {
		String controlBases = getNormalNucleotides();
		String testBases = getTumourNucleotides();
		return vcf.getChromosome() + TAB
		+ vcf.getPosition() + TAB
		+ vcf.getRef() + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB
		+ (StringUtils.isNullOrEmpty(controlBases) ? "--" : controlBases) + TAB	 
		+ (StringUtils.isNullOrEmpty(testBases) ? "--" : testBases);
	}
	
	public String getDCCData(final String mutationIdPrefix, final String chr) {
		String controlBases = getNormalNucleotides();
		String testBases = getTumourNucleotides();
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
		sb.append(StringUtils.isNullOrEmpty(controlBases) ? "--" : controlBases).append(TAB);
		sb.append(StringUtils.isNullOrEmpty(testBases) ? "--" : testBases);
		
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
		return convertOABSToNucleotides.apply(normalOABS);
	}
	public String getTumourNucleotides() {
		return convertOABSToNucleotides.apply(tumourOABS);
	}
	public void setTumourOABS(String tumourOABS) {
		this.tumourOABS = tumourOABS;
	}
	public void setNormalOABS(String normalOABS) {
		this.normalOABS = normalOABS;
	}
	public String getNormalOABS() {
		return normalOABS;
	}
	public String getTumourOABS() {
		return tumourOABS;
	}
	public void setControlFailedFilter(String unfilteredNormalPileup) {
		this.failedFilterControl = unfilteredNormalPileup;
	}
	public String getControlFailedFilter() {
		return failedFilterControl;
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
