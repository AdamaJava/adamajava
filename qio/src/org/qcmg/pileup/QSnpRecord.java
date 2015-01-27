/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import static org.qcmg.common.util.Constants.TAB;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

public class QSnpRecord {
	public enum Classification{
		SOMATIC, GERMLINE, UNKNOWN
	}
	
	private int id;
	private final VcfRecord vcf;
//	private final ChrPosition chrPos;
//	private char ref;
//	private char alt;
	private String pileup;
	private GenotypeEnum normalGenotype;
	private GenotypeEnum tumourGenotype;
	private int normalCount;
	private int tumourCount;
	private int normalNovelStartCount;
	private int tumourNovelStartCount;
	private Classification classification;
//	private String annotation;
//	private String note;
	private String mutation;
	private String normalPileup;
	private String unfilteredNormalPileup;
//	private String dbSnpId;
//	private char dbSnpStrand;
//	private int dbSnpAltLength;
//	private GenotypeEnum dbSnpGenotype;
//	private GenotypeEnum illuminaNormalGenotype;
//	private GenotypeEnum illuminaTumourGenotype;
	private Double probablility;
	private String normalNucleotides;
	private String tumourNucleotides;
	private String flankingSequence;
	
	public QSnpRecord(VcfRecord vcf) {
		this.vcf = vcf;
	}
	
	public QSnpRecord(String chr, int position, String ref) {
		this(chr, position, ref, null);
	}
	public QSnpRecord(String chr, int position, String ref, String alt) {
		int length = StringUtils.isNullOrEmpty(ref) ? 1 : ref.length();
		vcf = VcfUtils.createVcfRecord(new ChrPosition(chr, position, (position + length) -1), null, ref, alt);
	}
	
	public ChrPosition getChrPos() {
		return vcf.getChrPosition();
	}
	
	public String getRef() {
		return vcf.getRef();
	}
//	public void setRef(char ref) {
//		this.ref = ref;
//	}
	public String getAlt() {
		return vcf.getAlt();
	}
//	public void setAlt(char alt) {
//		this.alt = alt;
//	}
	public String getChromosome() {
		return vcf.getChromosome();
	}
	public int getPosition() {
		return vcf.getPosition();
	}
	public String getPileup() {
		return pileup;
	}
	public void setPileup(String pileup) {
		this.pileup = pileup;
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
	
//	public void setAnnotation(String annotation) {
//		this.annotation = annotation;
//	}
//	public void addAnnotation(String newAnnotation) {
//		this.annotation = StringUtils.addToString(annotation, newAnnotation, SC);
//	}
//	public void removeAnnotation(String annotationToRemove) {
//		this.annotation = StringUtils.removeFromString(this.annotation, annotationToRemove, SC);
//	}
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
//	public void setDbSnpId(String dbSnpId) {
//		this.dbSnpId = dbSnpId;
//	}
//	public String getDbSnpId() {
//		return dbSnpId;
//	}
//	public void setDbSnpStrand(char dbSnpStrand) {
//		this.dbSnpStrand = dbSnpStrand;
//	}
//	public char getDbSnpStrand() {
//		return dbSnpStrand;
//	}
//	public void setDbSnpGenotype(GenotypeEnum dbSnpGenotype) {
//		this.dbSnpGenotype = dbSnpGenotype;
//	}
//	public GenotypeEnum getDbSnpGenotype() {
//		return dbSnpGenotype;
//	}
	
	public String getFormattedString() {
		return pileup + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB;
//		+ (null != note ? note : "") + TAB;
//		+ (null != dbSnpId ? dbSnpId : "--") + TAB
//		+ (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
//		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
//		+ getStatus()  + TAB
//		+ (null != illuminaNormalGenotype ? illuminaNormalGenotype.getDisplayString() : "") + TAB
//		+ (null != illuminaTumourGenotype ? illuminaTumourGenotype.getDisplayString() : "") + TAB
//		+ getValidStatus() + TAB
//		+ getValidPlatform();
	}
	
	public String getGATKFormattedString() {
		return vcf.getChromosome() + TAB
		+ vcf.getPosition() + TAB
		+ vcf.getRef() + TAB
//		+ (null != normalPileup ? normalPileup : "--") + TAB
//		+ normalCount+ TAB
//		+ tumourCount+ TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (StringUtils.isNullOrEmpty(vcf.getFilter()) ? "" : vcf.getFilter()) + TAB
//		+ (null != note ? note : "") + TAB
//		+ (null != dbSnpId ? dbSnpId : "--") + TAB
//		+ (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
//		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
//		+ getStatus()  + TAB
//		+ (null != illuminaNormalGenotype ? illuminaNormalGenotype.getDisplayString() : "") + TAB
//		+ (null != illuminaTumourGenotype ? illuminaTumourGenotype.getDisplayString() : "") + TAB
//		+ getValidStatus() + TAB
//		+ getValidPlatform() + TAB
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
//		sb.append(null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888").append(TAB);
//		sb.append(('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))).append(TAB);
		sb.append(vcf.getRef()).append(TAB);
		sb.append(null != normalGenotype ? normalGenotype.getDisplayString() : "--").append(TAB);
		sb.append(null != tumourGenotype ? tumourGenotype.getDisplayString() : "--").append(TAB);
		if (Classification.GERMLINE != classification) {
			sb.append(null != mutation ? mutation : "").append(TAB);
		}
		sb.append("-999").append(TAB);		// expressed_allele
		sb.append("-999").append(TAB);		// quality_score
		sb.append(null != probablility ? probablility.toString() : "-999").append(TAB);	// probability
		sb.append(Classification.GERMLINE != classification ? tumourCount : normalCount).append(TAB);
//		sb.append(getStatus()).append(TAB);
//		sb.append(getValidStatus()).append(TAB);
//		sb.append(getValidPlatform()).append(TAB);
//		sb.append(getXRef()).append(TAB);
//		sb.append(null != note ? note : "-999").append(TAB);
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
		//analysis Id
		//tumour sample id
		//mutation id
		// chromosome
		return   vcf.getPosition() + TAB
		+  vcf.getPosition() + TAB
		+ 1 + TAB	// strand - always set to 1 ???
//		+  (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
//		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
		+ vcf.getRef() + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB
		+ "-999\t"		// quality_score
		+ "-999\t"		// probability
		+ (normalCount + tumourCount) + TAB;	// read count 
//		+ getStatus() + TAB		// annotation status
//		+ getValidStatus() + TAB	// validation status
//		+ getValidPlatform() + TAB	// validation platform
//		+ getXRef() + TAB
//		+ (null != note ? note : "-999");
	}
	
	
//	public void setIlluminaNormalGenotype(GenotypeEnum illuminaNormalGenotype) {
//		this.illuminaNormalGenotype = illuminaNormalGenotype;
//	}
//	public GenotypeEnum getIlluminaNormalGenotype() {
//		return illuminaNormalGenotype;
//	}
//	public void setIlluminaTumourGenotype(GenotypeEnum illuminaTumourGenotype) {
//		this.illuminaTumourGenotype = illuminaTumourGenotype;
//	}
//	public GenotypeEnum getIlluminaTumourGenotype() {
//		return illuminaTumourGenotype;
//	}
	
//	public String getXRef() {
//		return 1 == getStatus() ? dbSnpId : "-999";
//	}
//	public int getStatus() {
//		return null != dbSnpId ? 1 : 2;
//	}
//	public int getValidStatus() {
//		if (null == illuminaNormalGenotype || null == illuminaTumourGenotype)
//			return 2;	//not tested
//		if ( illuminaNormalGenotype == illuminaTumourGenotype)
//			return 1;	//validated
//		else return 3;	//not valid
//	}
//	public int getValidPlatform() {
//		if (null == illuminaNormalGenotype || null == illuminaTumourGenotype)
//			return -888;
//		else return 48;
//	}
//	public void setNote(String note) {
//		this.note = note;
//	}
//	public String getNote() {
//		return note;
//	}
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
	public void setUnfilteredNormalPileup(String unfilteredNormalPileup) {
		this.unfilteredNormalPileup = unfilteredNormalPileup;
	}
	public String getUnfilteredNormalPileup() {
		return unfilteredNormalPileup;
	}
//	public void setDbSnpAltLength(int dbSnpAltLength) {
//		this.dbSnpAltLength = dbSnpAltLength;
//	}
//	public int getDbSnpAltLength() {
//		return dbSnpAltLength;
//	}
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
	public double getProbability() {
		return probablility;
	}
	public void setProbability(double probability) {
		this.probablility = Double.valueOf(probability);
	}

	public VcfRecord getVcfRecord() {
		return vcf;
	}
}
