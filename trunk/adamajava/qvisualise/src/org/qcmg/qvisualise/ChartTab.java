/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
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
}
