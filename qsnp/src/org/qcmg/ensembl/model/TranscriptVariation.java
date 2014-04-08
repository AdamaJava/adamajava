/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;


@Entity
@Table(name = "homo_sapiens_variation_55_37.transcript_variation")
public class TranscriptVariation implements Serializable {

	private static final long serialVersionUID = -9046436290767482828L;

	@Id
	@Column(name="transcript_variation_id")
	private Long id;
	
//	@Column(name="transcript_id")
//	private Long transcriptId;
	
	@ManyToOne
	@JoinColumn(name="variation_feature_id")
	private VariationFeature variationFeature;
	
	@Column(name="cdna_start")
	private Integer start;
	
	@Column(name="cdna_end")
	private Integer end;
	
	@Column(name="translation_start")
	private Integer translationStart;
	
	@Column(name="translation_end")
	private Integer translationEnd;
	
	@Column(name = "peptide_allele_string")
	private String peptideAllele;
	
	@Column(name = "consequence_type", length = 239)
	public String consequenceType;
	
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="transcript_id")
    private TranscriptStable transcriptStable;
//	
//	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
//	@JoinColumn(name="transcript_id")
//    public Transcript transcript;
	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

//	public Long getTranscriptId() {
//		return transcriptId;
//	}
//
//	public void setTranscriptId(Long transcript) {
//		this.transcriptId = transcript;
//	}

	public VariationFeature getVariationFeature() {
		return variationFeature;
	}

	public void setVariationFeature(VariationFeature variationFeature) {
		this.variationFeature = variationFeature;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Integer getEnd() {
		return end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}

	public Integer getTranslationStart() {
		return translationStart;
	}

	public void setTranslationStart(Integer translationStart) {
		this.translationStart = translationStart;
	}

	public Integer getTranslationEnd() {
		return translationEnd;
	}

	public void setTranslationEnd(Integer translationEnd) {
		this.translationEnd = translationEnd;
	}

	public String getPeptideAllele() {
		return peptideAllele;
	}

	public void setPeptideAllele(String peptideAllele) {
		this.peptideAllele = peptideAllele;
	}

	public String getConsequenceType() {
		return consequenceType;
	}

	public void setConsequenceType(String consequenceType) {
		this.consequenceType = consequenceType;
	}

//	public void setTranscript(Transcript transcript) {
//		this.transcript= transcript;
//	}
//
//	public Transcript getTranscript() {
//		return transcript;
//	}
	public void setTranscriptStable(TranscriptStable transcriptStable) {
		this.transcriptStable = transcriptStable;
	}
	
	public TranscriptStable getTranscriptStable() {
		return transcriptStable;
	}
	
	public String getFormattedString() {
		return id + "\t" + variationFeature.getId() + "\t" + start + "\t" + end + "\t" + translationStart + "\t"
		+ translationEnd + "\t" + peptideAllele + "\t" + consequenceType  + "\t" +  transcriptStable.getStableId();
//		return id + "\t" + variationFeature.getId() + "\t" + start + "\t" + end + "\t" + translationStart + "\t"
//		+ translationEnd + "\t" + peptideAllele + "\t" + consequenceType  + "\t" +  transcript.getGene().getGeneId();
	}
}

