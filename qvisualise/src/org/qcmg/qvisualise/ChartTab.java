/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise;

import java.util.ArrayList;
import java.util.List;

public class ChartTab {
	
	private String name;
	private String title;
	private String data;
	private String chartInfo;
	private String renderingInfo;
	private String description;
	private final List<ChartTab> children = new ArrayList<ChartTab>();
	private boolean includeInSummary;

	///////////////////////////////////////////////////////////////////////////
	// Constructors
	///////////////////////////////////////////////////////////////////////////
	public ChartTab() {}
	
	public ChartTab(String title) {
		this.title = title;
	}
	public ChartTab(String title, String name) {
		this.title = title;
		this.name = name;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getChartInfo() {
		return chartInfo;
	}
	public void setChartInfo(String chartInfo) {
		this.chartInfo = chartInfo;
	}
	public List<ChartTab> getChildren() {
		return children;
	}
//	public void setChildren(List<ChartTab> children) {
//		this.children = children;
//	}
	public void addChild(ChartTab child) {
		children.add(child);
	}

	public void setRenderingInfo(String renderingInfo) {
		this.renderingInfo = renderingInfo;
	}

	public String getRenderingInfo() {
		return renderingInfo;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isIncludeInSummary() {
		return includeInSummary;
	}

	public void setIncludeInSummary(boolean includeInSummary) {
		this.includeInSummary = includeInSummary;
	}

	public String getChartInfoSummary() {
		String summaryChartInfo = chartInfo.replaceAll(name + "Chart", name + "ChartSummary");
		
		// reduce size
		int widthPos = summaryChartInfo.indexOf("width: ");
		
		if (widthPos > -1) {
			
			widthPos += 7;
		
			int origWidth = Integer.parseInt(summaryChartInfo.substring(widthPos, summaryChartInfo.indexOf(",", widthPos)));
			summaryChartInfo = summaryChartInfo.replaceAll("width: " + origWidth , "width: " + 450);
	//		summaryChartInfo = summaryChartInfo.replaceAll("width: " + origWidth , "width: " + origWidth/2);
			
			int heightPos = summaryChartInfo.indexOf("height: ") + 8;
			
			int origHeight = Integer.parseInt(summaryChartInfo.substring(heightPos, summaryChartInfo.indexOf(",", heightPos)));
			summaryChartInfo = summaryChartInfo.replaceAll("height: " + origHeight , "height: " + 300);
	//		summaryChartInfo = summaryChartInfo.replaceAll("height: " + origHeight , "height: " + origHeight/2);
		}

		return summaryChartInfo;
	}

}
