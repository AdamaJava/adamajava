/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.io.IOException;

public abstract class HtmlRecord {
	
	protected String title;
	protected final static String NEWLINE = "\n";
	protected String hAxis;
	protected String vAxis;
	
	public HtmlRecord(String title, String hAxis, String vAxis) {
		this.title = title;
		this.hAxis = hAxis;
		this.vAxis = vAxis;
	}	
	
	
	
	public String writeChartDiv() {
		return "<div id=\"chart_div\" style=\"width: 1350px; height: 750px;\"></div></br>";		
	}
	
	public String writeTableDiv() {
		return "<div id=\"table_div\" style=\"width: 1350px; height: 500px;\"></div></br>";	
	}
	
	public String writeHeadingDiv() {
		return "<div id=\"title_div\" style=\"width: 1350px; height: 40px;\">"+title+"</div></br>";	
	}


	
	public abstract String drawChart() throws IOException;

	public String drawDisplayDivs() {
		StringBuilder sb = new StringBuilder();

		sb.append(writeHeadingDiv());
		sb.append(writeTableDiv());
		sb.append(writeChartDiv()).append(NEWLINE);
		return sb.toString();
	}

	



	public String getPageTitle() {	
		return "<div>"+title.toUpperCase()+ "</div></br>" + NEWLINE;		
	}

}
