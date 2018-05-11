/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2.report;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.string.StringUtils;
import org.qcmg.qvisualise2.ChartTab;

public class HTMLReportGenerator {
	
	private static final String END_DIV = "\n</div>";
	
	private List<Report> reports = new ArrayList<Report>();
	
	public HTMLReportGenerator(List<Report> reports) {
		this.reports = reports;
	}
	
	private String getReportHeaderInfo(Report report) {
		final StringBuilder sb = new StringBuilder();
		
		for (ChartTab tab : report.getTabs()) {
			if (tab.getChildren().isEmpty() && null != tab.getData() &&  null != tab.getChartInfo()) {
				sb.append(tab.getData());
				sb.append(tab.getChartInfo());

			} else {
				for (ChartTab child : tab.getChildren()) {
					if (null != child.getData() &&  null != child.getChartInfo() ) {
						sb.append(child.getData());
						sb.append(child.getChartInfo());
					}
				}
			}
		}
		
		return sb.toString();
	}
	
	public String generate() {
		StringBuilder sb = new StringBuilder();
		
		// get the header info
		sb.append( "<html>\n<head>\n" );
		sb.append( HTMLReportUtils.getStyle() );
		sb.append( HTMLReportUtils.getJavaScript(reports) );
		sb.append("</head>\n<body>");
				
		//normally reports.size() == 1
		if (reports.size() > 1) {
			// don't want tab for report if it is the only one
			sb.append("\n<ul class=\"tabs\">");
			for (Report report : reports) {
				sb.append("<li><a href=\"#\">")
				.append(report.getType().getReportName())
				.append("</a></li>");				
			}
			sb.append("\n</ul>");
		}
		
		for (Report report : reports) {
			sb.append( getReportBodyInfo(report) );
		}

		// add google chart info
		HTMLReportUtils.generateChartScriptHeader( sb );
		
		// get header info from each report
		for (Report report : reports) { sb.append(getReportHeaderInfo(report)); }
		
		// end of chart info andscripts
		sb.append("}\n</script>\n");
		
		
		// end of file!!
		sb.append("\n</body>\n</html>");
		
		return sb.toString();
	}


	private String  getReportBodyInfo(Report report) {
		final StringBuilder sb = new StringBuilder();
		
		if (reports.size() > 1)
			sb.append("\n<div class=\"pane\"> ");
		
		
		sb.append("\n<div class=\"header\">File:").append(report.getFileName()).append(END_DIV);		
		if( ! StringUtils.isNullOrEmpty( report.getRecordParsed() ))
			sb.append(String.format("\n<div class=\"header\">RecordsParsed:%s; RunBy:%s; RunOn:%s; Version:qprofiler-%s",report.getRecordParsed(), report.getRunBy(), report.getRunOn(), report.getVersion())).append(END_DIV);		
		else
			sb.append(String.format("\n<div class=\"header\">RunBy:%s; RunOn:%s; Version:qprofiler-%s", report.getRunBy(), report.getRunOn(), report.getVersion())).append(END_DIV);		
		
		sb.append("\n<div class=\"header\">&nbsp</div>");		
		// and add the tabs to the body
		
		sb.append("\n<div id=\"tabs\">"); //with new jquery
		sb.append("\n<ul class=\"tabs\">");
		// display parent tabs
		for (ChartTab tab : report.getTabs()) {  
			sb.append( String.format( "\n<li><a href=\"#%s\">%s</a></li>", tab.getId(), tab.getTitle() ));
		}
		sb.append("\n</ul>");
				
		for (ChartTab tab : report.getTabs()) {					
			if (null == tab.getChildren() || tab.getChildren().isEmpty()) {
				if( null == tab.getRenderingInfo() ) {
					sb.append("\n<div id=\"" + tab.getId() + "\"" + "  class=\"pane\">");
					sb.append("<p id=\"" + tab.getName() + "Chart_div\"></p>");
					sb.append(END_DIV);					 
				} else  
					sb.append(tab.getRenderingInfo());
				 
			} else {
				sb.append("\n<div id=\"" + tab.getId() + "\"" + "  class=\"pane\">"); 
				sb.append("\n<ul class=\"tabs\">");
				for (ChartTab child : tab.getChildren()) {
					sb.append( String.format( "\n<li><a href=\"#%s\">%s</a></li>", child.getId(), child.getTitle() ));
				}
				sb.append("\n</ul>");
				for (ChartTab child : tab.getChildren()) {
					if (null != child.getRenderingInfo()) {
						sb.append(child.getRenderingInfo());  //xu code:: now child have rendering
					} else {
						//sb.append("\n<div class=\"pane\">");
						sb.append("\n<div id=\"" + child.getId() + "\"" + "  class=\"pane\">");
						sb.append(child.getDescritionButtonHtml());					 						
						sb.append("<p id=\"" + child.getName() + "Chart_div\"></p>");
						sb.append(END_DIV);
					}
				}
				sb.append(END_DIV);
			}
		}
		
		sb.append(END_DIV+"\n");
		if (reports.size() > 1)
			sb.append(END_DIV+"\n");
		
		return sb.toString();
	}

}
