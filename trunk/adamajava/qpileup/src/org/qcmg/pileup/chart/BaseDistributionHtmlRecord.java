/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class BaseDistributionHtmlRecord extends HtmlRecord {
	

	private ConcurrentSkipListMap<BigDecimal, AtomicLong> map;
	private long total;
	
	
	public BaseDistributionHtmlRecord(String title, String reciprocalTitle, String vAxis, String hAxis, ConcurrentSkipListMap<BigDecimal, AtomicLong> baseDistributionCountMap, long totalPositions) {
		super(title, hAxis, vAxis);
		this.hAxis = hAxis;
		this.vAxis = vAxis;
		this.map = baseDistributionCountMap;
		this.total = totalPositions;
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
        sb.append("dataView.setRows(dataView.getFilteredRows([{column: 0, minValue: 0.1}]));").append(NEWLINE);
       
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
		sb.append("'" + hAxis +"'," + "'"+ vAxis + "'," + "'% total'");
		//sb.append("'" + hAxis +"'," + "'"+ vAxis );
		
		sb.append("],").append(NEWLINE);
		int keycount = 0;
		for (Entry<BigDecimal, AtomicLong> entry : map.entrySet()) {
			keycount++;
			//if (!entry.getKey().equals(new Double(0))) {
				
				double percent = ((double)entry.getValue().longValue() / (double)total) * 100;
				DecimalFormat df = new DecimalFormat("#.##");
				
				sb.append(" ["+entry.getKey()+ "," + entry.getValue() + "," + df.format(percent) + "]");
				if (keycount != map.size()) {
					sb.append(",");
				}
				sb.append(NEWLINE);
			//} 
		}
		
	    
		sb.append("]);").append(NEWLINE);
		return sb.toString();
	}

}
