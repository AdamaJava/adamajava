/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ProfileType;
import org.qcmg.qvisualise.ChartTab;

public class Report {
	
	private final ProfileType type;
	private final String fileName;
	private final long recordCount;
	private final long duplicates;
	private List<ChartTab> tabs;
	
	public Report(ProfileType type, String fileName, long records, long duplicates) {
		this.type = type;
		this.fileName = fileName;
		this.recordCount = records;
		this.duplicates = duplicates;
		tabs = new ArrayList<ChartTab>();
	}
	
	public void addTab(ChartTab tab) {
		tabs.add(tab);
	}

	public List<ChartTab> getTabs() {
		return tabs;
	}

	public void setTabs(List<ChartTab> tabs) {
		this.tabs = tabs;
	}

	public String getFileName() {
		return fileName;
	}

	public long getRecordCount() {
		return recordCount;
	}
	
	public long getDuplicatesCount() {
		return duplicates;
	}

	public ProfileType getType() {
		return type;
	}
}
