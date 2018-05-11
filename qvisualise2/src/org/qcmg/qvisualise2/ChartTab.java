/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2;

import java.util.ArrayList;
import java.util.List;

public class ChartTab {
	
	private String name;
	private String title;
	private String data;
	private String chartInfo;
	private String renderingInfo;
//	private String description;
	private final List<ChartTab> children = new ArrayList<ChartTab>();
	private List<Description> descriptionInfo = new ArrayList<Description>(); 
		
	private class Description{
		String dataName;
		String buttonName;
		String description;
		Boolean isPopWindow = false;
		
		Description(String dataN, String buttonN, String des, boolean popUp){
			this.dataName = dataN;
			this.buttonName = buttonN;
			this.description = des.replace("\"", "'");
			this.isPopWindow = popUp;			
		}
				
		String getButtonHtmlBody(){			
			if(isPopWindow)
				return String.format(  "<button onclick=\"toggleWindow( %s, '%s')\" class=\"butt\">%s</button>", dataName, buttonName, buttonName);	
			
			//add description to html body
			String desc_div = dataName  + "_div";	
			return  String.format(  "<button onclick=\"toggleDiv('%s')\" class=\"butt\">%s</button><div id=\"%s\" class=\"desc\"><br>%s</div>",
					desc_div, buttonName, desc_div, description);			
		}
		
		String getHideDescription(){
			
			if(isPopWindow) return "";			
			return "$(\"#" + dataName + "_div\").toggle(false);\n";
				
		}
	}

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
	//for new jquery
	public String getId() {
		return title.replace(" ", "").toLowerCase();
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

//	public void setDescription(String description) {
//		this.description = description;
//	}
//
//	public String getDescription() {
//		return description;
//	}
	
	public void setDescritionButton(String buttonName, String descritpion, boolean isPopUp){
		int order = descriptionInfo.size() + 1;
		descriptionInfo.add( new Description( "des_"+name + order,  buttonName,   descritpion,   isPopUp));		
	}
	
	public String getDescritionButtonHtml(){
		StringBuilder sb = new  StringBuilder();
		for(Description des: descriptionInfo)
			sb.append(des.getButtonHtmlBody());		
		return sb.append("<BR>").toString() ;
	}
	
	public String getPopDescritpionJavaScript(){
		StringBuilder sb = new  StringBuilder();
		for(Description des: descriptionInfo){
			if(des.isPopWindow ) 				
				sb.append("\nvar ").append(des.dataName).append(" = \"").append(des.description).append("\";");
		}		
		return sb.toString();		
	}
	/**
	 * set to hide description string before click button
	 * @return eg.  $("#summ11Desc_div").toggle(false);
	 */
	public String getToggleFlaseDescritpion() {
		StringBuilder sb = new  StringBuilder();
		for(Description des: descriptionInfo) 
			sb.append(des.getHideDescription());
		
		return sb.toString();
	}
	
}
