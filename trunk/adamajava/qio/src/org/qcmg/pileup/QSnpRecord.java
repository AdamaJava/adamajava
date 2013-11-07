package org.qcmg.pileup;

import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;

public class QSnpRecord {
	public enum Classification{
		SOMATIC, GERMLINE
	}

	private static final char TAB = '\t';
	private static final char SC = ';';
	
	private int id;
	private String chromosome;
	private int position;
	private char ref;
	private char alt;
	private String pileup;
	private GenotypeEnum normalGenotype;
	private GenotypeEnum tumourGenotype;
	private int normalCount;
	private int tumourCount;
	private int normalNovelStartCount;
	private int tumourNovelStartCount;
	private Classification classification;
	private String annotation;
	private String note;
	private String mutation;
	private String normalPileup;
	private String unfilteredNormalPileup;
	private String dbSnpId;
	private char dbSnpStrand;
	private int dbSnpAltLength;
	private GenotypeEnum dbSnpGenotype;
	private GenotypeEnum illuminaNormalGenotype;
	private GenotypeEnum illuminaTumourGenotype;
	
	private Double probablility;
	
	private String normalNucleotides;
	private String tumourNucleotides;
	
	private String flankingSequence;
	
	public char getRef() {
		return ref;
	}
	public void setRef(char ref) {
		this.ref = ref;
	}
	public char getAlt() {
		return alt;
	}
	public void setAlt(char alt) {
		this.alt = alt;
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
	
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	public void addAnnotation(String annotation) {
		if (StringUtils.isNullOrEmpty(this.annotation))
			this.annotation = annotation;
		else if ( ! this.annotation.contains(annotation))
			this.annotation += SC + annotation;
	}
	public void removeAnnotation(String annotation) {
		if (null != this.annotation && this.annotation.contains(annotation)) {
			if (this.annotation.equals(annotation)) {
				this.annotation = null;
			} else if (this.annotation.startsWith(annotation)) {	// need to remove semi-colon along with annotation
				this.annotation = this.annotation.replace(annotation + SC, "");
			} else {
				this.annotation = this.annotation.replace(SC + annotation, "");
			}
		}
	}
	public String getAnnotation() {
		return annotation;
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
	public void setDbSnpId(String dbSnpId) {
		this.dbSnpId = dbSnpId;
	}
	public String getDbSnpId() {
		return dbSnpId;
	}
	public void setDbSnpStrand(char dbSnpStrand) {
		this.dbSnpStrand = dbSnpStrand;
	}
	public char getDbSnpStrand() {
		return dbSnpStrand;
	}
	public void setDbSnpGenotype(GenotypeEnum dbSnpGenotype) {
		this.dbSnpGenotype = dbSnpGenotype;
	}
	public GenotypeEnum getDbSnpGenotype() {
		return dbSnpGenotype;
	}
	
	public String getFormattedString() {
		return pileup + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (null != annotation ? annotation : "") + TAB
		+ (null != note ? note : "") + TAB
		+ (null != dbSnpId ? dbSnpId : "--") + TAB
		+ (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
		+ getStatus()  + TAB
		+ (null != illuminaNormalGenotype ? illuminaNormalGenotype.getDisplayString() : "") + TAB
		+ (null != illuminaTumourGenotype ? illuminaTumourGenotype.getDisplayString() : "") + TAB
		+ getValidStatus() + TAB
		+ getValidPlatform();
	}
	
	public String getGATKFormattedString() {
		return chromosome + TAB
		+ position + TAB
		+ ref + TAB
//		+ (null != normalPileup ? normalPileup : "--") + TAB
//		+ normalCount+ TAB
//		+ tumourCount+ TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB 
		+ classification + TAB
		+ (null != mutation ? mutation : "") + TAB
		+ (null != annotation ? annotation : "") + TAB
		+ (null != note ? note : "") + TAB
		+ (null != dbSnpId ? dbSnpId : "--") + TAB
		+ (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
		+ getStatus()  + TAB
		+ (null != illuminaNormalGenotype ? illuminaNormalGenotype.getDisplayString() : "") + TAB
		+ (null != illuminaTumourGenotype ? illuminaTumourGenotype.getDisplayString() : "") + TAB
		+ getValidStatus() + TAB
		+ getValidPlatform() + TAB
		+ (StringUtils.isNullOrEmpty(normalNucleotides) ? "--" : normalNucleotides) + TAB	 
		+ (StringUtils.isNullOrEmpty(tumourNucleotides) ? "--" : tumourNucleotides);
	}
	
	public String getDCCData(final String mutationIdPrefix, final String chr) {
		StringBuilder sb = new StringBuilder();
		sb.append(mutationIdPrefix + id).append(TAB);
		sb.append("1").append(TAB);
		sb.append(chr).append(TAB);
		sb.append(position).append(TAB);
		sb.append(position).append(TAB);
		sb.append(1).append(TAB);
		sb.append(null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888").append(TAB);
		sb.append(('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))).append(TAB);
		sb.append(ref).append(TAB);
		sb.append(null != normalGenotype ? normalGenotype.getDisplayString() : "--").append(TAB);
		sb.append(null != tumourGenotype ? tumourGenotype.getDisplayString() : "--").append(TAB);
		if (Classification.SOMATIC == classification)
			sb.append(null != mutation ? mutation : "").append(TAB);
		sb.append("-999").append(TAB);		// expressed_allele
		sb.append("-999").append(TAB);		// quality_score
		sb.append(null != probablility ? probablility.toString() : "-999").append(TAB);	// probability
		sb.append(Classification.SOMATIC == classification ? tumourCount : normalCount).append(TAB);
		sb.append(getStatus()).append(TAB);
		sb.append(getValidStatus()).append(TAB);
		sb.append(getValidPlatform()).append(TAB);
		sb.append(getXRef()).append(TAB);
		sb.append(null != note ? note : "-999").append(TAB);
		sb.append(null != annotation ? annotation : "--").append(TAB);
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
		return  position + TAB
		+ position + TAB
		+ 1 + TAB	// strand - always set to 1 ???
		+  (null != dbSnpGenotype ? dbSnpGenotype.getDisplayString() : "-888") + TAB
		+ (('+' == dbSnpStrand ? "1" : ('-' == dbSnpStrand? "-1" : "-888"))) + TAB
		+ ref + TAB
		+ (null != normalGenotype ? normalGenotype.getDisplayString() : "") + TAB
		+ (null != tumourGenotype ? tumourGenotype.getDisplayString() : "") + TAB
		+ "-999\t"		// quality_score
		+ "-999\t"		// probability
		+ (normalCount + tumourCount) + TAB	// read count 
		+ getStatus() + TAB		// annotation status
		+ getValidStatus() + TAB	// validation status
		+ getValidPlatform() + TAB	// validation platform
		+ getXRef() + TAB
		+ (null != note ? note : "-999");
	}
	
	
	public void setIlluminaNormalGenotype(GenotypeEnum illuminaNormalGenotype) {
		this.illuminaNormalGenotype = illuminaNormalGenotype;
	}
	public GenotypeEnum getIlluminaNormalGenotype() {
		return illuminaNormalGenotype;
	}
	public void setIlluminaTumourGenotype(GenotypeEnum illuminaTumourGenotype) {
		this.illuminaTumourGenotype = illuminaTumourGenotype;
	}
	public GenotypeEnum getIlluminaTumourGenotype() {
		return illuminaTumourGenotype;
	}
	
	public String getXRef() {
		return 1 == getStatus() ? dbSnpId : "-999";
	}
	public int getStatus() {
		return null != dbSnpId ? 1 : 2;
	}
	public int getValidStatus() {
		if (null == illuminaNormalGenotype || null == illuminaTumourGenotype)
			return 2;	//not tested
		if ( illuminaNormalGenotype == illuminaTumourGenotype)
			return 1;	//validated
		else return 3;	//not valid
	}
	public int getValidPlatform() {
		if (null == illuminaNormalGenotype || null == illuminaTumourGenotype)
			return -888;
		else return 48;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public String getNote() {
		return note;
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
	public void setUnfilteredNormalPileup(String unfilteredNormalPileup) {
		this.unfilteredNormalPileup = unfilteredNormalPileup;
	}
	public String getUnfilteredNormalPileup() {
		return unfilteredNormalPileup;
	}
	public void setDbSnpAltLength(int dbSnpAltLength) {
		this.dbSnpAltLength = dbSnpAltLength;
	}
	public int getDbSnpAltLength() {
		return dbSnpAltLength;
	}
	public int getNovelStartCount() {
		return Classification.SOMATIC == classification ? tumourNovelStartCount : normalNovelStartCount;
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
}
