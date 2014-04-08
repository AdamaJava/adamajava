/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.model;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="homo_sapiens_variation_55_37.variation_feature")
public class VariationFeature implements Serializable {
	private static final long serialVersionUID = -6120704072793262868L;
	
	@Id
	@Column(name = "variation_feature_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "seq_region_id")
	private SeqRegion seqRegion;
	
	@Column(name = "seq_region_start")
	private Integer seqRegionStart;
	
	@Column(name = "seq_region_end")
	private Integer seqRegionEnd;
	
	@Column(name = "seq_region_strand")
	private Integer seqRegionStrand;
	
	@Column(name = "variation_id")
	private Long variationId;
	
	@Column(name = "allele_string", length = 65535)
	private String allele;
	
	@Column(name = "variation_name", length = 255)
	private String name;
	
	@Column(name = "map_weight")
	private Integer mapWeight;
	
	@Column(name = "flags", length = 9)
	private String flags;
	
	@Column(name = "source_id")
	private Long sourceId;
	
	@Column(name = "validation_status", length = 39)
	private String validationStatus;
	
	@Column(name = "consequence_type", length = 265)
	private String consequenceType;
	
	@OneToMany(mappedBy="variationFeature", fetch=FetchType.EAGER)
    private Set<TranscriptVariation> transcriptVariations;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SeqRegion getSeqRegion() {
		return seqRegion;
	}

	public void setSeqRegion(SeqRegion seqRegion) {
		this.seqRegion = seqRegion;
	}

	public Integer getSeqRegionStart() {
		return seqRegionStart;
	}

	public void setSeqRegionStart(Integer seqRegionStart) {
		this.seqRegionStart = seqRegionStart;
	}

	public Integer getSeqRegionEnd() {
		return seqRegionEnd;
	}

	public void setSeqRegionEnd(Integer seqRegionEnd) {
		this.seqRegionEnd = seqRegionEnd;
	}

	public Integer getSeqRegionStrand() {
		return seqRegionStrand;
	}

	public void setSeqRegionStrand(Integer seqRegionStrand) {
		this.seqRegionStrand = seqRegionStrand;
	}

	public Long getVariationId() {
		return variationId;
	}

	public void setVariationId(Long variationId) {
		this.variationId = variationId;
	}

	public String getAllele() {
		return allele;
	}

	public void setAllele(String allele) {
		this.allele = allele;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMapWeight() {
		return mapWeight;
	}

	public void setMapWeight(Integer mapWeight) {
		this.mapWeight = mapWeight;
	}

	public String getFlags() {
		return flags;
	}

	public void setFlags(String flags) {
		this.flags = flags;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}

	public String getValidationStatus() {
		return validationStatus;
	}

	public void setValidationStatus(String validationStatus) {
		this.validationStatus = validationStatus;
	}

	public String getConsequenceType() {
		return consequenceType;
	}

	public void setConsequenceType(String consequenceType) {
		this.consequenceType = consequenceType;
	}
	
	@Transient
	public String getFormattedDisplay() {
		return id + ", "
		+ seqRegion + ", "
		+ seqRegionStart + ", "
		+ seqRegionEnd + ", "
		+ seqRegionStrand + ", "
		+ variationId + ", "
		+ allele + ", "
		+ name + ", "
		+ mapWeight + ", "
		+ flags + ", "
		+ sourceId + ", "
		+ validationStatus + ", "
		+ consequenceType + ", "
		+ "no of transcriptVariations: " + transcriptVariations.size() ;
	}

	public Set<TranscriptVariation> getTranscriptVariations() {
		return transcriptVariations;
	}

	public void setTranscriptVariations(
			Set<TranscriptVariation> transcriptVariations) {
		this.transcriptVariations = transcriptVariations;
	}

}

