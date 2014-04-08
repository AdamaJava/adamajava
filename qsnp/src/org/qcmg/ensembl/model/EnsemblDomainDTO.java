/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.model;

public class EnsemblDomainDTO {

	private String geneStableId;
	private String transcriptStableId;
	private String translationStableId;
	private String hitName;
	private int start;
	private int end;
	private String logicName;
	private String interproAc;
	private String displayLabel;
	private String description;
	public String getGeneStableId() {
		return geneStableId;
	}
	public void setGeneStableId(String geneStableId) {
		this.geneStableId = geneStableId;
	}
	public String getTranscriptStableId() {
		return transcriptStableId;
	}
	public void setTranscriptStableId(String transcriptStableId) {
		this.transcriptStableId = transcriptStableId;
	}
	public String getTranslationStableId() {
		return translationStableId;
	}
	public void setTranslationStableId(String translationStableId) {
		this.translationStableId = translationStableId;
	}
	public String getHitName() {
		return hitName;
	}
	public void setHitName(String hitName) {
		this.hitName = hitName;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getLogicName() {
		return logicName;
	}
	public void setLogicName(String logicName) {
		this.logicName = logicName;
	}
	public String getInterproAc() {
		return interproAc;
	}
	public void setInterproAc(String interproAc) {
		this.interproAc = interproAc;
	}
	public String getDisplayLabel() {
		return displayLabel;
	}
	public void setDisplayLabel(String displayLabel) {
		this.displayLabel = displayLabel;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getFormattedString() {
		return geneStableId + "\t"
		+ transcriptStableId + "\t"
		+ translationStableId + "\t"
		+ hitName + "\t"
		+ start + "\t"
		+ end + "\t"
		+ logicName + "\t"
		+ interproAc + "\t"
		+ displayLabel + "\t"
		+ description;
	}
	
}
