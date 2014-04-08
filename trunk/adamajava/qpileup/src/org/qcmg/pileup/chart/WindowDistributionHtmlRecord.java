/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

public class WindowDistributionHtmlRecord extends HtmlRecord {
	
	private String hAxis;
	private String vAxis;
	private ConcurrentSkipListMap<Long, Long[]> map;
	private int total;
	private String reciprocalTitle;
	
	public WindowDistributionHtmlRecord(String title, String reciprocalTitle, String vAxis, String hAxis, ConcurrentSkipListMap<Long, Long[]> positionsCountMap, int total) {
		super(title, hAxis, vAxis);
		this.reciprocalTitle = reciprocalTitle;
		
		this.map = positionsCountMap;
		this.total = total;
	}

	@Override
	public String drawChart() {
		StringBuilder sb = new StringBuilder();
		sb.append(constructDataTable());
		
		//Options
        sb.append(getOptions("optionsA", title  + "", hAxis, vAxis));
        //Dataview all elements
        sb.append("var dataView = new google.visualization.DataView(data);").append(NEWLINE);
        sb.append("dataView.setColumns([0, 1]);").append(NEWLINE);
        sb.append("var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));").append(NEWLINE);
        sb.append("chart.draw(dataView, optionsA);").append(NEWLINE);
        sb.append("var table = new google.visualization.Table(document.getElementById('table_div'));").append(NEWLINE);
        sb.append("table.draw(data, null);").append(NEWLINE);
        
		return sb.toString();
	}	
	
	private String getOptions(String name, String title, String hAxis, String vAxis) {
		StringBuilder sb = new StringBuilder();
		sb.append("var "+name+" = {");
        sb.append("title: '"+title+"',").append(NEWLINE);
        sb.append("hAxis: {title: '"+hAxis+"', titleTextStyle: {color: 'red'}},").append(NEWLINE);
        sb.append("vAxis: {title: '"+vAxis+"'}").append(NEWLINE);
        sb.append("};").append(NEWLINE);
        return sb.toString();
	}

	private String constructDataTable() {
		StringBuilder sb = new StringBuilder();
		sb.append("var data = google.visualization.arrayToDataTable([").append(NEWLINE);
		sb.append("[");
		sb.append("'" + hAxis +"'," + "'"+ vAxis + "'," + "'percent_total_windows'," + "'"+reciprocalTitle+"'," + "'total_bases_in_windows'");
		//sb.append("'" + hAxis +"'," + "'"+ vAxis );
		
		sb.append("],").append(NEWLINE);
		int keycount = 0;
		for (Entry<Long, Long[]> entry : map.entrySet()) {
			keycount++;
			double percent = 0;
			Long[] longs = entry.getValue();
			if (longs[0] > 0 && total > 0) {
				percent = (double) longs[0] / (double) total * 100.0;
			}
			sb.append(" ["+entry.getKey()+",  "+entry.getValue()[0]+ "," + percent + "," +entry.getValue()[1]+"," +entry.getValue()[2] + "]");
			if (keycount != map.size()) {
				sb.append(",");
			}
		    sb.append(NEWLINE);
		}
		
	    
		sb.append("]);").append(NEWLINE);
		return sb.toString();
	}


	

	
	
	

}
