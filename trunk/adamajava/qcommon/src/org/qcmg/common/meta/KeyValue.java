/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

public class KeyValue {
	
	public static final char TAB = '\t';
	private static final String LS = System.getProperty("line.separator");
	
	public static final String Q_DCC_META = "#Q_DCCMETA";
	public static final String Q_EXEC = "#Q_EXEC";
	public static final String Q_LIMS_META = "#Q_LIMSMETA_";
	public static final String Q_BAM_ID = "#Q_BAM_ID";
	
	private String key;
	private String value;

	public KeyValue (String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String toLogString() {
		return this.key + " " + this.value;
	}
	
	public String toExecString() {
		return Q_EXEC + TAB + this.key + TAB + this.value + LS; 
	}
	
	public String toDCCMetaString() {
		return Q_DCC_META + TAB + this.key + TAB + this.value + LS; 
	}
	
	public String toLimsMetaString(String prefix) {
		return Q_LIMS_META + prefix.toUpperCase() + TAB + this.key + TAB + this.value + LS; 
	}
	
	public String toBamIdString() {
		return Q_BAM_ID + TAB + this.key + TAB + this.value + LS; 
	}
	
	public String toToolString(String tool) {
		return "#Q_TOOL_" + tool.toUpperCase() + TAB + this.key + TAB + this.value + LS; 
	}
	
}
