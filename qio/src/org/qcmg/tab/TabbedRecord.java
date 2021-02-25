/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.tab;


public class TabbedRecord {
	private String data;
	
	public String getData() {
		return data;
	}
	public String[] getDataArray() {
		return data.replace("\"", "").split("\t");
	}
	public void setData(String data) {
		this.data = data;
	}
}
