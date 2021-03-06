/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ProfileType;
import org.qcmg.qvisualise.ChartTab;

public class Report {
	
	private final ProfileType type;
	private final String fileName;
	private final String recordParsed;
	private final String duplicates;
	private List<ChartTab> tabs;	
	
	private String runBy = null;
	private String runOn = null;
	private String version = null;

	
	public Report(ProfileType type, String fileName, String records, String duplicates) {
		this.type = type;
		this.fileName = fileName;
		this.recordParsed = records;
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

	public String getRecordParsed() {
		return recordParsed;
	}
	
	public String getDuplicatesCount() {
		return duplicates;
	}

	public ProfileType getType() {
		return type;
	}
	
	public String getRunBy(){
		return runBy;
	}
	
	public String getRunOn(){
		return runOn;
	}
	
	public String getVersion(){
		return version;
	}
	
	public void setRunBy(String str){
		 runBy = str;
	}
	
	public void setRunOn(String str){
		 runOn = str;
	}
	
	public void setVersion(String str){
		 version = str;
	}
}
