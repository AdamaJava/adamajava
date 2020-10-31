/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;

import org.qcmg.common.util.TabTokenizer;

public class TabbedRecord {
	private String data;
	
	public TabbedRecord(String line) { this.data = line;};
	
	public String getData() {
		return data;
	}
	public String[] getDataArray() {
		return TabTokenizer.tokenize(data);
				
			//	data.replace("\"", "").split("\t");
	}
	public void setData(String data) {
		this.data = data;
	}
}
