/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * @deprecated since it is no longer used.
 */

package org.qcmg.genesymbol;

@Deprecated
public class GeneSymbolRecord {
	
	private String geneId;
	private String transcriptId;
	private String symbol;
	
	public String getGeneId() {
		return geneId;
	}
	public void setGeneId(String geneId) {
		this.geneId = geneId;
	}
	public String getTranscriptId() {
		return transcriptId;
	}
	public void setTranscriptId(String transcriptId) {
		this.transcriptId = transcriptId;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
}
