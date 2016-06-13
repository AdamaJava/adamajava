/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.maf;

import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.MafType;
import org.qcmg.common.string.StringUtils;

public class MAFRecord {
	
	protected final static char T = '\t';

	protected String hugoSymbol;
	protected String entrezGeneId;
	protected String center;
	protected Number ncbiBuild;
	protected String chromosome;
	protected int startPosition;
	protected int endPosition;
	protected char strand;
	protected String variantClassification;
	protected MutationType variantType;
//	protected String variantType;
	protected String ref;
	protected String tumourAllele1;
	protected String tumourAllele2;
	protected String dbSnpId;
	protected String dbSnpValStatus;
	protected String tumourSampleBarcode;
	protected String normalSampleBarcode;
	protected String normalAllele1;
	protected String normalAllele2;
	protected String tumourValidationAllele1;
	protected String tumourValidationAllele2;
	protected String normalValidationAllele1;
	protected String normalValidationAllele2;
	protected String verificationStatus;
	protected String validationStatus;
	protected String mutationStatus;
	protected String sequencingPhase;
	protected String sequencingSource;
	protected String validationMethod;
	protected String score;
	protected String bamFile;
	protected String sequencer;
	
	// QCMG specific fields
	protected String flag;
	protected String nd;
	protected String td;
	protected String canonicalTranscriptId;
	protected String canonicalAAChange;
	protected String canonicalBaseChange;
	protected String alternateTranscriptId;
	protected String alternateAAChange;
	protected String alternateBaseChange;
	
	
	protected String cpg;  //eg. FS=AGAGAGTAATT
	protected String gffBait;
//	protected String confidence;
	protected String ranking;
	protected int novelStartCount;
	protected String novelStartBases;
	
	protected String patient;	
	protected MafConfidence confidence;
	protected MafType mafType;
	
	
	//COSMIC fields
	protected String cosmicId;
	protected int cosmicIdFreq;
	protected int cosmicFreq;
	protected int cosmicGene;
	
	
	public String getHugoSymbol() {
		return hugoSymbol;
	}
	public void setHugoSymbol(String hugoSymbol) {
		this.hugoSymbol = hugoSymbol;
	}
	public void addHugoSymbol(String hugoSymbol) {
		this.hugoSymbol = StringUtils.isNullOrEmpty(this.hugoSymbol) ? hugoSymbol : this.hugoSymbol + ";" + hugoSymbol;
	}
	public String getEntrezGeneId() {
		return entrezGeneId;
	}
	public void setEntrezGeneId(String entrezGeneId) {
		this.entrezGeneId = entrezGeneId;
	}
	public void addEntrezGeneId(String entrezGeneId) {
		this.entrezGeneId = StringUtils.isNullOrEmpty(this.entrezGeneId) ? entrezGeneId : this.entrezGeneId + ";" + entrezGeneId;
	}
	public String getCenter() {
		return center;
	}
	public void setCenter(String center) {
		this.center = center;
	}
	public Number getNcbiBuild() {
		return ncbiBuild;
	}
	public void setNcbiBuild(Number ncbiBuild) {
		this.ncbiBuild = ncbiBuild;
	}
	public String getChromosome() {
		return chromosome;
	}
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	public int getStartPosition() {
		return startPosition;
	}
	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}
	public int getEndPosition() {
		return endPosition;
	}
	public void setEndPosition(int endPosition) {
		this.endPosition = endPosition;
	}
	public char getStrand() {
		return strand;
	}
	public void setStrand(char strand) {
		this.strand = strand;
	}
	public String getVariantClassification() {
		return variantClassification;
	}
	public void setVariantClassification(String variantClassification) {
		this.variantClassification = variantClassification;
	}
	public void addVariantClassification(String variantClassification) {
		this.variantClassification = StringUtils.isNullOrEmpty(this.variantClassification) 
		? variantClassification : this.variantClassification + ";" + variantClassification;
	}
	public MutationType getVariantType() {
		return variantType;
	}
	public void setVariantType(MutationType variantType) {
		this.variantType = variantType;
	}
	public String getRef() {
		return ref;
	}
	public void setRef(String ref) {
		this.ref = ref;
	}
	public String getTumourAllele1() {
		return tumourAllele1;
	}
	public void setTumourAllele1(String tumourAllele1) {
		this.tumourAllele1 = tumourAllele1;
	}
	public String getTumourAllele2() {
		return tumourAllele2;
	}
	public void setTumourAllele2(String tumourAllele2) {
		this.tumourAllele2 = tumourAllele2;
	}
	public String getDbSnpId() {
		return dbSnpId;
	}
	public void setDbSnpId(String dbSnpId) {
		this.dbSnpId = dbSnpId;
	}
	public String getDbSnpValStatus() {
		return dbSnpValStatus;
	}
	public void setDbSnpValStatus(String dbSnpValStatus) {
		this.dbSnpValStatus = dbSnpValStatus;
	}
	public String getTumourSampleBarcode() {
		return tumourSampleBarcode;
	}
	public void setTumourSampleBarcode(String tumourSampleBarcode) {
		this.tumourSampleBarcode = tumourSampleBarcode;
	}
	public String getNormalSampleBarcode() {
		return normalSampleBarcode;
	}
	public void setNormalSampleBarcode(String normalSampleBarcode) {
		this.normalSampleBarcode = normalSampleBarcode;
	}
	public String getNormalAllele1() {
		return normalAllele1;
	}
	public void setNormalAllele1(String normalAllele1) {
		this.normalAllele1 = normalAllele1;
	}
	public String getNormalAllele2() {
		return normalAllele2;
	}
	public void setNormalAllele2(String normalAllele2) {
		this.normalAllele2 = normalAllele2;
	}
	public String getTumourValidationAllele1() {
		return tumourValidationAllele1;
	}
	public void setTumourValidationAllele1(String tumourValidationAllele1) {
		this.tumourValidationAllele1 = tumourValidationAllele1;
	}
	public String getTumourValidationAllele2() {
		return tumourValidationAllele2;
	}
	public void setTumourValidationAllele2(String tumourValidationAllele2) {
		this.tumourValidationAllele2 = tumourValidationAllele2;
	}
	public String getNormalValidationAllele1() {
		return normalValidationAllele1;
	}
	public void setNormalValidationAllele1(String normalValidationAllele1) {
		this.normalValidationAllele1 = normalValidationAllele1;
	}
	public String getNormalValidationAllele2() {
		return normalValidationAllele2;
	}
	public void setNormalValidationAllele2(String normalValidationAllele2) {
		this.normalValidationAllele2 = normalValidationAllele2;
	}
	public String getVerificationStatus() {
		return verificationStatus;
	}
	public void setVerificationStatus(String verificationStatus) {
		this.verificationStatus = verificationStatus;
	}
	public String getValidationStatus() {
		return validationStatus;
	}
	public void setValidationStatus(String validationStatus) {
		this.validationStatus = validationStatus;
	}
	public String getMutationStatus() {
		return mutationStatus;
	}
	public void setMutationStatus(String mutationStatus) {
		this.mutationStatus = mutationStatus;
	}
	public String getSequencingPhase() {
		return sequencingPhase;
	}
	public void setSequencingPhase(String sequencingPhase) {
		this.sequencingPhase = sequencingPhase;
	}
	public String getSequencingSource() {
		return sequencingSource;
	}
	public void setSequencingSource(String sequencingSource) {
		this.sequencingSource = sequencingSource;
	}
	public String getValidationMethod() {
		return validationMethod;
	}
	public void setValidationMethod(String validationMethod) {
		this.validationMethod = validationMethod;
	}
	public String getScore() {
		return score;
	}
	public void setScore(String score) {
		this.score = score;
	}
	public String getBamFile() {
		return bamFile;
	}
	public void setBamFile(String bamFile) {
		this.bamFile = bamFile;
	}
	public String getSequencer() {
		return sequencer;
	}
	public void setSequencer(String sequencer) {
		this.sequencer = sequencer;
	}
	public String getCosmicId() {
		return cosmicId;
	}
	public int getCosmicIdFreq() {
		return cosmicIdFreq;
	}
	public int getCosmicFreq() {
		return cosmicFreq;
	}
	public int getCosmicGene() {
		return cosmicGene;
	}
	public void setCosmicId(String cosmicId) {
		this.cosmicId = cosmicId;
	}
	public void setCosmicIdFreq(int cosmicIdFreq) {
		this.cosmicIdFreq = cosmicIdFreq;
	}
	public void setCosmicFreq(int cosmicFreq) {
		this.cosmicFreq = cosmicFreq;
	}
	public void setCosmicGene(int noOfGeneHits) {
		this.cosmicGene = noOfGeneHits;
	}
	@Override
	public String toString() {
		return "MAFRecord [bamFile=" + bamFile + ", center=" + center
				+ ", chromosome=" + chromosome + ", dbSnpId=" + dbSnpId
				+ ", dbSnpValStatus=" + dbSnpValStatus + ", endPosition="
				+ endPosition + ", entrezGeneId=" + entrezGeneId + ", flag="
				+ flag + ", hugoSymbol=" + hugoSymbol + ", mutationStatus="
				+ mutationStatus + ", ncbiBuild=" + ncbiBuild
				+ ", normalAllele1=" + normalAllele1 + ", normalAllele2="
				+ normalAllele2 + ", normalSampleBarcode="
				+ normalSampleBarcode + ", normalValidationAllele1="
				+ normalValidationAllele1 + ", normalValidationAllele2="
				+ normalValidationAllele2 + ", ref=" + ref + ", score=" + score
				+ ", sequencer=" + sequencer + ", sequencingPhase="
				+ sequencingPhase + ", sequencingSource=" + sequencingSource
				+ ", startPosition=" + startPosition + ", strand=" + strand
				+ ", td=" + td + ", tumourAllele1=" + tumourAllele1
				+ ", tumourAllele2=" + tumourAllele2 + ", tumourSampleBarcode="
				+ tumourSampleBarcode + ", tumourValidationAllele1="
				+ tumourValidationAllele1 + ", tumourValidationAllele2="
				+ tumourValidationAllele2 + ", validationMethod="
				+ validationMethod + ", validationStatus=" + validationStatus
				+ ", variantClassification=" + variantClassification
				+ ", variantType=" + variantType + ", verificationStatus="
				+ verificationStatus + "]";
	}
	
	public String toFormattedStringBasic() {
		final StringBuilder sb = new StringBuilder();
		sb.append(hugoSymbol).append(T);
		sb.append(entrezGeneId).append(T);
		sb.append(center).append(T);
		sb.append(ncbiBuild).append(T);
		sb.append(chromosome).append(T);
		sb.append(startPosition).append(T);
		sb.append(endPosition).append(T);
		sb.append(strand).append(T);
		sb.append(variantClassification).append(T);
		sb.append(variantType).append(T);
		sb.append(ref).append(T);
		sb.append(tumourAllele1).append(T);
		sb.append(tumourAllele2).append(T);
		sb.append(dbSnpId).append(T);
		sb.append(dbSnpValStatus).append(T);
		sb.append(tumourSampleBarcode).append(T);
		sb.append(normalSampleBarcode).append(T);
		sb.append(normalAllele1).append(T);
		sb.append(normalAllele2).append(T);
		sb.append(tumourValidationAllele1).append(T);
		sb.append(tumourValidationAllele2).append(T);
		sb.append(normalValidationAllele1).append(T);
		sb.append(normalValidationAllele2).append(T);
		sb.append(verificationStatus).append(T);
		sb.append(validationStatus).append(T);
		sb.append(mutationStatus).append(T);
		sb.append(sequencingPhase).append(T);
		sb.append(sequencingSource).append(T);
		sb.append(validationMethod).append(T);
		sb.append(score).append(T);
		sb.append(bamFile).append(T);
		sb.append(sequencer).append(T);
		sb.append(flag).append(T);
		sb.append(nd).append(T);
		sb.append(td).append(T);
		sb.append(canonicalTranscriptId).append(T);
		sb.append(canonicalAAChange).append(T);
		sb.append(canonicalBaseChange).append(T);
		sb.append(alternateTranscriptId).append(T);
		sb.append(alternateAAChange).append(T);
		sb.append(alternateBaseChange);
		return sb.toString();
		
		
		
		

 
	}
	
	public String toFormattedString() {
		final StringBuilder sb = new StringBuilder(toFormattedStringBasic());
		if (novelStartCount > 0) {
			sb.append(T);
			sb.append(novelStartCount);
		}
		
		return sb.toString();		
	}
	
	public String toFormattedStringExtraFields() {
		return toFormattedStringBasic() + T +
		confidence + T +
		cpg + T +
		gffBait + T +
		novelStartCount;
	}
	public String toFormattedStringExtraFieldsCosmic() {
		return toFormattedStringExtraFields() + T +
				cosmicId + T +
				cosmicIdFreq + T +
				cosmicFreq + T +
				cosmicGene;
	}
	
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getTd() {
		return td;
	}
	public void setTd(String td) {
		this.td = td;
	}
	public String getNd() {
		return nd;
	}
	public void setNd(String nd) {
		this.nd = nd;
	}
	public String getCanonicalTranscriptId() {
		return canonicalTranscriptId;
	}
	public void setCanonicalTranscriptId(String transcriptId) {
		this.canonicalTranscriptId = transcriptId;
	}
	public void addCanonicalTranscriptId(String transcriptId) {
		this.canonicalTranscriptId = StringUtils.isNullOrEmpty(this.canonicalTranscriptId) 
		? transcriptId : this.canonicalTranscriptId + ";" + transcriptId;
	}
	public String getCanonicalAAChange() {
		return canonicalAAChange;
	}
	public void setCanonicalAAChange(String canonicalAAChange) {
		this.canonicalAAChange = canonicalAAChange;
	}
	public void addCanonicalAAChange(String canonicalAAChange) {
		this.canonicalAAChange = StringUtils.isNullOrEmpty(this.canonicalAAChange) 
		? canonicalAAChange : this.canonicalAAChange + ";" + canonicalAAChange;
	}
	public String getCanonicalBaseChange() {
		return canonicalBaseChange;
	}
	public void setCanonicalBaseChange(String canonicalBaseChange) {
		this.canonicalBaseChange = canonicalBaseChange;
	}
	public void addCanonicalBaseChange(String canonicalBaseChange) {
		this.canonicalBaseChange = StringUtils.isNullOrEmpty(this.canonicalBaseChange) 
		? canonicalBaseChange : this.canonicalBaseChange + ";" + canonicalBaseChange;
	}
	public String getAlternateTranscriptId() {
		return alternateTranscriptId;
	}
	public void setAlternateTranscriptId(String alternateTranscriptId) {
		this.alternateTranscriptId = alternateTranscriptId;
	}
	public void addAlternateTranscriptId(String alternateTranscriptId) {
		this.alternateTranscriptId = StringUtils.isNullOrEmpty(this.alternateTranscriptId) 
		? alternateTranscriptId : this.alternateTranscriptId + ";" + alternateTranscriptId;
	}
	public String getAlternateAAChange() {
		return alternateAAChange;
	}
	public void setAlternateAAChange(String alternateAAChange) {
		this.alternateAAChange = alternateAAChange;
	}
	public void addAlternateAAChange(String alternateAAChange) {
		this.alternateAAChange = StringUtils.isNullOrEmpty(this.alternateAAChange) 
		? alternateAAChange : this.alternateAAChange + ";" + alternateAAChange;
	}
	public String getAlternateBaseChange() {
		return alternateBaseChange;
	}
	public void setAlternateBaseChange(String alternateBaseChange) {
		this.alternateBaseChange = alternateBaseChange;
	}
	public void addAlternateBaseChange(String alternateBaseChange) {
		this.alternateBaseChange = StringUtils.isNullOrEmpty(this.alternateBaseChange) 
		? alternateBaseChange : this.alternateBaseChange + ";" + alternateBaseChange;
	}
	public void setCpg(String cpg) {
		this.cpg = cpg;
	}
	public String getCpg() {
		return cpg;
	}
	
	// ranking
	public void setRanking(String ranking) {
		this.ranking = ranking;
	}
	public boolean isHighConf() {
		return "high".equals(ranking);
	}
	public boolean isLowConf() {
		return "low".equals(ranking);
	}
	public void setNovelStartCount(int novelStartCount) {
		this.novelStartCount = novelStartCount;
	}
	public int getNovelStartCount() {
		return novelStartCount;
	}
	public void setConfidence(MafConfidence string) {
		this.confidence = string;
	}
	public MafConfidence getConfidence() {
		return confidence;
	}
	public void setGffBait(String gffBait) {
		this.gffBait = gffBait;
	}
	public String getGffBait() {
		return gffBait;
	}
	public void setNovelStartBases(String novelStartBases) {
		this.novelStartBases = novelStartBases;
	}
	public String getNovelStartBases() {
		return novelStartBases;
	}
	public void setPatient(String patient) {
		this.patient = patient;
	}
	public String getPatient() {
		return patient;
	}
	public void setMafType(MafType mafType) {
		this.mafType = mafType;
	}
	public MafType getMafType() {
		return mafType;
	}
}
