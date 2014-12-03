/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.SummaryByCycle;

public class HTMLReportUtils {
	public static final String BAR_CHART = "BarChart";
	public static final String COLUMN_CHART = "ColumnChart";
	public static final String PIE_CHART = "PieChart";
	public static final String SCATTER_CHART = "ScatterChart";
	public static final String AREA_CHART = "AreaChart";
	public static final String HEAT_MAP_CHART = "HeatMap";
	public static final String TABLE_CHART = "Table";

	public static void generateHTMLHeader(StringBuilder sb) {
//		sb.append("<html>\n<head>\n");
		
		sb.append("<script type=\"text/javascript\" "
				+ "src=\"http://www.google.com/jsapi\"></script>\n");
		sb.append("<script type=\"text/javascript\">" +
				"google.load(\"visualization\", \"1\", {packages:[\"corechart\",\"table\"]});" +
				"google.load(\"prototype\", \"1.6\");" +
		"</script>\n");
		
		sb.append("<script type=\"text/javascript\" src=\"http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/js/load.js\"></script>\n" +
	    "<script type=\"text/javascript\">" +
	        "systemsbiology.load(\"visualization\", \"1.0\", {packages:[\"bioheatmap\"]});" +
	    "</script>\n");
		
		// new tabbing lib
		sb.append("<script src=\"http://cdn.jquerytools.org/1.2.5/full/jquery.tools.min.js\"></script>\n");
		
//		sb.append("<script type=\"text/javascript\" src='http://magic-table.googlecode.com/svn/trunk/magic-table/javascript/magic_table.js'></script>\n");
		
//		sb.append(createStyleSheet());
//		sb.append(getStyle());
		
		
		// ext js info
//		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/home/oholmes/development/thirdparty/ext-3.2.1/resources/css/ext-all.css\" />\n");
//		sb.append("<script type=\"text/javascript\" src=\"/home/oholmes/development/thirdparty/ext-3.2.1/adapter/ext/ext-base.js\"></script>\n");
//		sb.append("<script type=\"text/javascript\" src=\"/home/oholmes/development/thirdparty/ext-3.2.1/ext-all-debug.js\"></script>\n");
//		sb.append("<script type=\"text/javascript\" src=\"/home/oholmes/development/thirdparty/ext-3.2.1/examples/ux/TabCloseMenu.js\"></script>\n ");
//		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"tabs-example.css\" />");
		// END of Ext js
		
		sb.append("<script type=\"text/javascript\">")
		.append("google.setOnLoadCallback(drawChart);\n")
		.append("function drawChart() \n{");
	}
	
	public static String getStyle() {
		return "<style> " +
"\nul.tabs { " +
"	margin:0 !important;" + 
"	padding:0;" +
"	height:30px;" +
"	border-bottom:1px solid #666;" +	
"}" +

/* single tab */
"\nul.tabs li {" + 
"	float:left;" +	 
"	text-indent:0;" +
"	padding:0;" +
"	margin:0 !important;" +
"	list-style-image:none !important;" + 
"}" +

/* link inside the tab. uses a background image */
"\nul.tabs a {" + 
"float:left;" +
"	font-family: Verdana, Helvetica, Arial;" +
"	font-size:12px;" +
"	display:block;" +
"	padding:5px 30px;" +	
"	text-decoration:none;" +
"	border:1px solid #666;	" +
"	border-bottom:0px;" +
"	height:18px;" +
"	background-color:rgb(234,242,255);" +
"	color:rgb(0,66,174);" +
"	margin-right:2px;" +
"	position:relative;" +
"	top:1px;	" +
"	outline:0;" +
"	border-radius:4px 4px 0 0;" +	
"	-moz-border-radius:4px 4px 0 0;" +	
"}" +

"\nul.tabs a:active {" +
"	background-color:#ddd;" +
"	border-bottom:1px solid #ddd;" +	
"	color:#CCCCCC;	" +
"	cursor:default;" +
"}" +

/* when mouse enters the tab move the background image */
"\nul.tabs a:hover {" +
"	background-position: -420px -31px;" +
"	background-color:#CCFFFF;" +
"	color:#333;	" +
"}" +

/* active tab uses a class name "current". its highlight is also done by moving the background image. */
"\nul.tabs a.current, ul.tabs a.current:hover, ul.tabs li.current a {" +
	"background-position: -420px -62px;		" +
	"cursor:default !important; " +
	"font-weight:bold;" +
"	color:#000066 !important;" +
"}" +

/* Different widths for tabs: use a class name: w1, w2, w3 or w2 */

/* width 1 */
"\nul.tabs a.s 			{ background-position: -553px 0; width:81px; }" +
"\nul.tabs a.s:hover 	{ background-position: -553px -31px; }" +
"\nul.tabs a.s.current  { background-position: -553px -62px; }" +

/* width 2 */
"\nul.tabs a.l 			{ background-position: -248px -0px; width:174px; }" +
"\nul.tabs a.l:hover 	{ background-position: -248px -31px; }" +
"\nul.tabs a.l.current  { background-position: -248px -62px; }" +

/* width 3 */
"\nul.tabs a.xl 			{ background-position: 0 -0px; width:248px; }" +
"\nul.tabs a.xl:hover 	{ background-position: 0 -31px; }" +
"\nul.tabs a.xl.current { background-position: 0 -62px; }" +

/* initially all panes are hidden */ 
".panes, .pane {" +
"	display:none;" +		
"}" +
"" +
"" +
"</style>\n";
	}

	public static <T, R> String generateGoogleData(
			Map<T, R> dataSet, String name, boolean isValueString) {
		return generateGoogleData(dataSet, name, isValueString, "Count", "value");
//		Map<Integer, Integer> dataSet, String name, boolean isValueString) {
		
//		StringBuilder sb = new StringBuilder("\nvar ");
//
//		sb.append(name)
//		.append(" = new google.visualization.DataTable(\n{cols: [{id: 'value', label: 'value', type: '"
//				+ (isValueString ? "string" : "number") +"'}, {id: 'Count', label: 'Count', type: 'number'}], ");
//		
//		// now for the data
//		sb.append("\nrows: [");
//
//		int i = 0;
//		for (Entry<T, R> entry : dataSet.entrySet()) {
//			if (i++ > 0)
//				sb.append(",\n");
//			
//			sb.append("{c:[{v: ");
//			
//			if (isValueString) {
//				sb.append("'").append(entry.getKey()).append("'}, ");
//			} else {
//				sb.append(entry.getKey()).append("}, ");
//			}
//
//			sb.append("{v: " + entry.getValue() + "}]}");
//		}
//		
//		// end of rows
//		sb.append("]}, 0.6);");
//		
//		return sb.toString();
	}
	
	public static <T, R> String generateGoogleData(
			Map<T, R> dataSet, String name, boolean isValueString, String xAxisLabel, String yAxisLabel) {
//		Map<Integer, Integer> dataSet, String name, boolean isValueString) {
		
		StringBuilder sb = new StringBuilder("\nvar ");
		
		sb.append(name)
		.append(" = new google.visualization.DataTable(\n{cols: [{id: '" + yAxisLabel + "', label: '" + yAxisLabel + "', type: '"
				+ (isValueString ? "string" : "number") +"'}, {id: '" + xAxisLabel + "', label: '" + xAxisLabel + "', type: 'number'}], ");
		
		// now for the data
		sb.append("\nrows: [");
		
		int i = 0;
		for (Entry<T, R> entry : dataSet.entrySet()) {
			if (i++ > 0)
				sb.append(",\n");
			
			sb.append("{c:[{v: ");
			
			if (isValueString) {
				sb.append("'").append(entry.getKey()).append("'}, ");
			} else {
				sb.append(entry.getKey()).append("}, ");
			}
			
			sb.append("{v: " + entry.getValue() + "}]}");
		}
		
		// end of rows
		sb.append("]}, 0.6);");
		
		return sb.toString();
	}
	public static <T> String generateGoogleDataMultiSeries(
			Map<T, AtomicLongArray> dataSet, String name, boolean isValueString, List<String> readGroupNames, boolean showCombined) {
//		Map<Integer, Integer> dataSet, String name, boolean isValueString) {
		int noOfReadGroups = readGroupNames.size();
		StringBuilder sb = new StringBuilder("\nvar ");
		
		sb.append(name)
		.append(" = new google.visualization.DataTable(\n{cols: [{id: 'value', label: 'value', type: '"
				+ (isValueString ? "string" : "number") +"'}");
		
		int count = 0;
		for (String s : readGroupNames) {
//			String label = s.substring(3);
			sb.append(", {id: 'RG" + count++ + "', label: '" + s + "', type: 'number'}");
		}
		if (showCombined && noOfReadGroups > 1) {
			// add in a combined series
			sb.append(", {id: 'combined', label: 'combined', type: 'number'}");
		}
		sb.append("], ");
		
		// now for the data
		sb.append("\nrows: [");
		
		int i = 0;
		for (Entry<T, AtomicLongArray> entry : dataSet.entrySet()) {
			if (i++ > 0)
				sb.append(",\n");
			
			sb.append("{c:[{v: ");
			
			if (isValueString) {
				sb.append("'").append(entry.getKey()).append("'}, ");
			} else {
				sb.append(entry.getKey()).append("}, ");
			}
			long combinedTally = 0;
			for (int j = 0 ; j < entry.getValue().length() ; j++) {
				if (j > 0) sb.append(",");
				sb.append("{v: " + entry.getValue().get(j) + "}");
				combinedTally += entry.getValue().get(j);
			}
			if (showCombined && noOfReadGroups > 1) {
				sb.append(",{v: " + combinedTally + "}");
			}
			sb.append("]}");
		}
		
		// end of rows
		sb.append("]}, 0.6);");
		
		return sb.toString();
	}
	
	public static String generateGoogleDataForTable(Map<String, List<String>> dataSet, String name) {
//		public static String generateGoogleDataForTable(Map<String, String> dataSet, String name) {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (Entry<String, List<String>> entry : dataSet.entrySet()) {
			sb .append("\nvar ");
			sb.append(name + i++)
			.append(" = new google.visualization.DataTable(\n{cols: [{id: 'value', label: '" + entry.getKey() + "', type: 'string'}], ");
			
			// now for the data
			sb.append("\nrows: [");
			
			int j = 0;
			for (String listEntry : entry.getValue()) {
				if (j++ > 0)
					sb.append(",\n");
				
				sb.append("{c:[");
				sb.append("{v: '").append(listEntry).append("'}]}");
			}
			// end of rows
			sb.append("]}, 0.6);");
		
		}
		return sb.toString();
	}
	
	public static String generateGoogleDataForTableStringMapPair(Map<String, Map<String, AtomicLong>> dataSet, String name) {
//		public static String generateGoogleDataForTable(Map<String, String> dataSet, String name) {
		StringBuilder sb = new StringBuilder();
			sb .append("\nvar ");
			sb.append(name)
			.append(" = new google.visualization.DataTable(\n{cols: [{id: 'header', label: 'Name', type: 'string'}, {id: 'value', label: 'Value', type: 'string'}, {id: 'count', label: 'Count', type: 'number'}], ");
			
			
			// now for the data
			sb.append("\nrows: [");
			
			int j = 0;
			for (Entry<String, Map<String, AtomicLong>> entry : dataSet.entrySet()) {
				if (j++ > 0)
					sb.append(",\n");
				
				int k = 0 ;
				for (Entry<String, AtomicLong> innerEntry : entry.getValue().entrySet()) {
					if (k++ > 0) {
						sb.append(",\n");
					}
					
					sb.append("{c:[");
					sb.append("{v: '").append(entry.getKey()).append("'},");
					sb.append("{v: '").append(innerEntry.getKey()).append("'},");
					sb.append("{v: '").append(innerEntry.getValue().longValue()).append("'}]}");
	//				sb.append("{v: '").append(entry.getValue()).append("', p:{style:'background-color: red;'}}]}");
				}
			}
			// end of rows
			sb.append("]}, 0.6);");
			
		return sb.toString();
	}
	
	public static String generateGoogleDataForTableStringMap(Map<String, String> dataSet, String name) {
//		public static String generateGoogleDataForTable(Map<String, String> dataSet, String name) {
		StringBuilder sb = new StringBuilder();
			sb .append("\nvar ");
			sb.append(name)
			.append(" = new google.visualization.DataTable(\n{cols: [{id: 'header', label: 'Summary', type: 'string'}, {id: 'value', label: 'Value', type: 'string'}], ");
			
			
			// now for the data
			sb.append("\nrows: [");
			
			int j = 0;
			for (Entry<String, String> entry : dataSet.entrySet()) {
				if (j++ > 0)
					sb.append(",\n");
				
				sb.append("{c:[");
				sb.append("{v: '").append(entry.getKey()).append("'},");
				sb.append(entry.getValue());
//				sb.append("{v: '").append(entry.getValue()).append("', p:{style:'background-color: red;'}}]}");
			}
			// end of rows
			sb.append("]}, 0.6);");
			
		return sb.toString();
	}
	
	public static String generateGoogleMatrixData(
			Map<MAPQMiniMatrix, AtomicLong> dataSet, String name, boolean isValueString) {
		
		int maxMapQ = 0;
		int maxValue = 0;
		for (MAPQMiniMatrix mmm : dataSet.keySet()) {
			if (mmm.getMapQ() > maxMapQ)
				maxMapQ = mmm.getMapQ();
			
			if (mmm.getValue() > maxValue)
				maxValue = mmm.getValue(); 
		}
		
		StringBuilder sb = new StringBuilder("var ");
		
		sb.append(name)
		.append(" = new google.visualization.DataTable(\n{cols: [{id: 'col_0', label: '0',type: 'string'}");

		// using MAPQ for the columns
		for (int i = 0 ; i <= maxMapQ ; i++) {
			sb.append(", {id: 'col_" + i + "', label: '" + i + "', type: 'number'}");
		}
		sb.append("], \nrows: [");
		
		for (int i = 0 ; i <= maxValue ; i++) {
			if (i > 0)
				sb.append(",\n");
			
			sb.append("{c:[{v: " + i + "}");
			
			for (int j = 0 ; j <= maxMapQ ; j++) {
				MAPQMiniMatrix mmm = new MAPQMiniMatrix(j,i);
				AtomicLong al = dataSet.get(mmm);
				sb.append(", {v: " + (null == al ? "null" : "" + al.get()) + "}");
			}
			
			sb.append("]}");
		}
		
		sb.append("]}, 0.6);");
		return sb.toString();
	}

	public static String generateGoogleChart(String dataName, String chartTitle, int width,
			int height, String chartType, boolean logScale, boolean isStacked) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, chartType);
		
		basicChartSetup(sb, chartName, dataName, chartTitle, width, height);
		
		if (isStacked) {
			sb.append(", isStacked: true");
		}
		
		if (COLUMN_CHART.equals(chartType) || AREA_CHART.equals(chartType)) {
			sb.append(", fontSize:12");
			sb.append(", hAxis: {title: 'Value', titleColor: 'blue'" + (width > 1000 ? ", textStyle:{fontSize:9}" : "") + "}");
			sb.append(", vAxis: {title: '" + (logScale ? "Log( " : "") + "Count " + (logScale ? ") " : "") +"', titleColor: 'blue',logScale: " + logScale + ", }");
			sb.append(", legend: 'none'");
		} else if (BAR_CHART.equals(chartType)) {
			sb.append(", fontSize:12");
			sb.append(", hAxis: {title: '" + (logScale ? "Log( " : "") + "Count " + (logScale ? ") " : "") +"', titleColor: 'blue',logScale: " + logScale + "}");
			sb.append(", vAxis: {title: 'Value', titleColor: 'blue'}");
			sb.append(", legend: 'none'");
		} else if (PIE_CHART.equals(chartType)) {
			sb.append(", is3D: 'true'");
		}
		sb.append("});");
		
		return sb.toString();
	}
	
	public static String generateGoogleTable(String dataName, int numberOfTables, String tableTitle) {
		String title = null != tableTitle ? "title: \"" + tableTitle + "\"": "";
		StringBuilder sb = new StringBuilder();
		if (numberOfTables > 1) {
		for (int i = 1 ; i <= numberOfTables ; i++) {
			String chartName = dataName + i + "Chart";
			
			initialTableSetup(sb, chartName);
			
			sb.append(chartName + ".draw(" + dataName + i +", {width:1000, height: " + dataName + i+ ".getNumberOfRows() > 50 ?400:0, showRowNumber: " + dataName + i + ".getNumberOfRows() > 1");
			sb.append("});");
		}
		} else {
			
			String chartName = dataName + "Chart";
			
			initialTableSetup(sb, chartName);
			
			sb.append(chartName + ".draw(" + dataName +", {allowHtml: 'true', width: 700, height: 500, " + title + ", showRowNumber: false});");
		}
		return sb.toString();
	}
	public static String generateGoogleTable(String dataName, int numberOfTables) {
		
		return generateGoogleTable(dataName, numberOfTables, null);
//		StringBuilder sb = new StringBuilder();
//		if (numberOfTables > 1) {
//			for (int i = 1 ; i <= numberOfTables ; i++) {
//				String chartName = dataName + i + "Chart";
//				
//				initialTableSetup(sb, chartName);
//				
//				sb.append(chartName + ".draw(" + dataName + i +", {width:1000, height: " + dataName + i+ ".getNumberOfRows() > 50 ?400:0, showRowNumber: " + dataName + i + ".getNumberOfRows() > 1");
//				sb.append("});");
//			}
//		} else {
//			
//			String chartName = dataName + "Chart";
//			
//			initialTableSetup(sb, chartName);
//			
//			sb.append(chartName + ".draw(" + dataName +", {width: 300, height: 300, showRowNumber: false});");
//		}
//		return sb.toString();
	}
	
	public static String generateRenderingTableInfoSummary(List<String> dataNames) {
		StringBuilder sb = new StringBuilder("\n<div class=\"pane\">\n<table>\n");
		
		int i = 0; 
		for (String dn : dataNames) {
			
			if (i % 2 == 0) {
				sb.append("<tr>");
			}
			
			String title = dn.contains("md") ? " title=\"cycles with mismatches over 1%\"" : "";
			
			sb.append("<td  id = \"" + dn + "ChartSummary_div\"" + title + "></td>");
//			String colSpan = dn.startsWith("is") ? "colspan=2" : "";
//			sb.append("<td " + colSpan + " id = \"" + dn + "ChartSummary_div\"></td>");
				
			if (i % 2 == 1) {
				sb.append("</tr>\n");
			}
			i++;
		}
		
		// add trailing tr
		if ( ! sb.toString().endsWith("</tr>\n")) {
			sb.append("</tr>\n");
		}
		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
	public static String generateRenderingTableInfo(String dataName, int numberOfTables) {
		StringBuilder sb = new StringBuilder("\n<div class=\"pane\">\n<table>");
		
		if (numberOfTables > 1) {
			for (int i = 1 ; i <= numberOfTables ; i++) {
				String chartName = dataName + i + "Chart_div";
				sb.append("<tr><td id = \"" + chartName + "\"></td></tr>\n");
			}
		} else {
			String chartName = dataName + "Chart_div";
			sb.append("<tr><td id = \"" + chartName + "\"></td></tr>\n");
		}
		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
	public static String generateRenderingTableInfo(List<String> data) {
		StringBuilder sb = new StringBuilder("\n<div class=\"pane\">\n<table>");
		
		int i = 0; 
		for (String s : data) {
			String chartName = "rnm" + s + "Chart_div";
			if (i % 2 == 0) {
				sb.append("<tr>");
			}
			
			sb.append("<td id = \"" + chartName + "\"></td>");
			
			if (i % 2 == 1) {
				sb.append("</tr>\n");
			}
			i++;
		}
		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
	
	public static String generateGoogleChartSlidingColors(String dataName, String chartTitle, int width,
			int height, String chartType, boolean logScale, boolean isStacked, int noOfColors, String[] colours) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, chartType);
		
		basicChartSetup(sb, chartName, dataName, chartTitle, width, height);
//		basicChartSetup(sb, chartName, dataName, chartTitle, 1200, 800);
		
		if (isStacked) {
			sb.append(", isStacked: true");
		}
		
		if (noOfColors == 0 && null == colours) {
			// use default
		} else {
			int legendFontSize = noOfColors;
			sb.append(", colors:[");
			if (null != colours) {
				for (String colour : colours) {
					sb.append("'").append(colour).append("',");
				}
				legendFontSize = colours.length;
			} else {
			
				int multiple = 255 / noOfColors;
				for (int i = 0; i < noOfColors; i++) {
					if (i > 0)
						sb.append(",");
					sb.append("'rgb(")
					.append((255 - (i * multiple)))
					.append(",0,")
					.append(i * multiple)
					.append(")'");
				}
			}
			sb.append("]");
			if (legendFontSize > 10)
				sb.append(", legendFontSize:10");
		}
		sb.append(", fontSize:12, titleFontSize:15, hAxis: {title: 'Count', titleColor: 'blue', logScale: " + logScale + "}");
		sb.append(
				", vAxis: {title: 'Cycle', titleColor: 'blue',logScale: " + logScale + "}});");
		
		return sb.toString();
	}
	
	public static String generateGoogleScatterChart(String dataName, String chartTitle,
			int width, int height, boolean logScale) {
		return generateGoogleScatterChart(dataName, chartTitle, width, height, logScale, " 'none' ");
	}
	
	public static String generateGoogleScatterChart(String dataName, String chartTitle,
			int width, int height, boolean logScale, String legendText) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, SCATTER_CHART);
		basicChartSetup(sb, chartName, dataName, chartTitle, width, height);
		
		sb.append(", hAxis: {title: 'Value', titleColor: 'blue'}");
		sb.append(", vAxis: {title: '" + (logScale ? "Log( Count )" : "Count") + "', titleColor: 'blue',logScale: " + logScale + ", format: '0'}");
		sb.append(", legend: ").append(legendText);
//		sb.append(", curveType: 'function'");
		sb.append(", pointSize: 2, lineWidth: 1});");
		
		return sb.toString();
	}
	
	public static String generateGoogleHeatMap(String dataName, String chartTitle,
			int width, int height) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialHeatMapChartSetup(sb, chartName);
		
		
		sb.append("options = {};options.tableTitle = \"Bar-fill\";options.enableFisheye = true;options.enableBarFill = true;" +
      "options.defaultRowHeight = 25;" +
      "options.columnWidths = [{column : 0, width : 130}];" +
      "options.defaultColumnWidth = 60;" +
      "options.rowHeaderCount = 0;" +
      "options.columnHeaderCount = 0;" +
      "options.tablePositionX = 0;" +
      "options.tablePositionY = 0;" +
      "options.tableHeight = 603;" +
      "options.tableWidth = 1015;" +
      "options.colourRamp = [{red:0, green:0, blue:255}, {red:0, green:255, blue:255}, {red:0, green:255, blue:0}, {red:255, green:255, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}, {red:255, green:0, blue:0}];");
		
		sb.append(chartName)
		.append(".draw(")
		.append(dataName).append(",options);");
		
		return sb.toString();
	}
	
	public static String generateGoogleBioHeatMap(String dataName, String chartTitle, int cellHeight) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialBioHeatMapChartSetup(sb, chartName);
		
		sb.append(chartName)
		.append(".draw(")
		.append(dataName)
		.append(", {fontSize:8,  drawBorder: false, cellWidth:11, cellHeight:" + cellHeight + ", numberOfColors: 256, " +
				"passThroughBlack: true, " +
				"emptyDataColor: {r: 255, g: 255, b: 255, a: 1}, startColor: {r: 0, g: 0, b: 255, a: 0.5}, " +
		"endColor: {r: 255, g: 255, b: 0, a: 1}});\n");
		
		return sb.toString();
	}
	
	public static <T> String generateGoogleDataCycles(
			SummaryByCycle<T> cycle, String name, List<String> stringList, boolean isQualData, Map<Integer, String> percentages) {
		
		boolean convertToAscii = false;
		boolean useValuesFromCycles = null == stringList || ! stringList.containsAll(cycle.getPossibleValues());
		
		if (useValuesFromCycles) {
			stringList = new ArrayList<String>();
			if (isQualData) {
				Set<Integer> possibleIntegerValues = new TreeSet<Integer>();
				// get values in sorted integer format
				for (T value : cycle.getPossibleValues()) {
					String valueString = value.toString();
					if ( ! Character.isDigit(valueString.charAt(0))) {
						convertToAscii = true;
						// convert from char to ascii value
						char c = value.toString().charAt(0);
						possibleIntegerValues.add(c - 64);
					} else {
						possibleIntegerValues.add(Integer.parseInt(valueString));
					}
				}
				
				for (Iterator<Integer> iter = ((TreeSet<Integer>) possibleIntegerValues).descendingIterator() ; iter.hasNext() ; ) {
					stringList.add(iter.next().toString());
				}
				
			} else {
				stringList.addAll((Collection<? extends String>) cycle.getPossibleValues());
			}
		}
		
		StringBuilder sb = new StringBuilder("\nvar ");
		sb.append(name);
		
		sb.append(" = new google.visualization.DataTable(\n{cols: [{id: 'Value', label: 'Value', type: 'string'}");
		for (String string : stringList) {
			if ("\\".equals(string)) {
				string = "\\\\";
			}
			sb.append(", {id: 'col_" + string + "', label: '" + string + "', type: 'number'}");
		}
		sb.append("], \nrows: [");
		
		// now for the data
		int i = 0;
		int k = -1;
		for (Integer cycleNumber : cycle.cycles()) {
			if (i++ > 0)
				sb.append(",\n");
			
			sb.append("{c:[{v: '");
			sb.append(cycleNumber.intValue());
			sb.append("'}");
			
			// now add the kids...
			for (String string : stringList) {
				sb.append(", {v: ");
				
				if (convertToAscii) {
					AtomicLong al = null;
					// need to loop through all the values as equals() on the T generic doesn't seem to work
					for (T value : cycle.values(cycleNumber)) {
						if (value.toString().equals( (Character.valueOf((char) (Integer.parseInt(string) + 64)).toString()))) {
							al = cycle.count(cycleNumber,  value);
//							sb.append(cycle.count(cycleNumber,  value).get());
							break;	// break out of for loop
						}
					}
					sb.append((null != al) ? al.get() : "null");
					
				} else
					sb.append(cycle.count(cycleNumber, (T) string).get());
				
				if (null != percentages) {
					String percent = percentages.get(cycleNumber);
					sb.append(",f: '").append(cycle.count(cycleNumber, (T) string).get() + "(" +percent).append(")'");
				}
				
				
				sb.append("}");
			}
			
			sb.append("]}");
		}

		// end of rows
		sb.append("]}, 0.6);");
		return sb.toString();
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Private Methods
	///////////////////////////////////////////////////////////////////////////
	
	private static void initialChartSetup(StringBuilder sb, String chartName, String chartType) {
		sb
		.append("\nvar ")
		.append(chartName)
		.append(" = new google.visualization.")
		.append(chartType)
		.append("(document.getElementById('")
		.append(chartName)
		.append("_div'));");
	}
	private static void initialTableSetup(StringBuilder sb, String chartName) {
		sb
		.append("\nvar ")
		.append(chartName)
		.append(" = new google.visualization.Table")
		.append("(document.getElementById('")
		.append(chartName)
		.append("_div'));");
	}
	
	private static void initialHeatMapChartSetup(StringBuilder sb, String chartName) {
		sb
		.append("\nvar ")
		.append(chartName)
		.append(" = new greg.ross.visualisation.MagicTable")
		.append("(document.getElementById('")
		.append(chartName)
		.append("_div'));");
	}
	
	private static void initialBioHeatMapChartSetup(StringBuilder sb, String chartName) {
		sb.append("\nvar ")
		.append(chartName)
		.append(" = new org.systemsbiology.visualization.BioHeatMap")
//		sb.append("heatmap = new org.systemsbiology.visualization.BioHeatMap")
		.append("(document.getElementById('")
		.append(chartName)
		.append("_div'));");
	}
	
	private static void basicChartSetup(StringBuilder sb, String chartName,
			String chartData, String chartTitle, int width, int height) {
		sb
		.append(chartName)
		.append(".draw(")
		.append(chartData)
		.append(", {width: ")
		.append(width)
		.append(", height: ")
		.append(height)
		.append(", title: '")
		.append(chartTitle)
		.append("'");
		sb.append(", chartArea:{left:150,top:40,width:\"75%\",height:\"75%\"}");
//		sb.append(", chartArea:{left:150,top:40,width:\"85%\",height:\"75%\"}");
	}
	
	public static String createStyleSheet() {
		StringBuilder sb = new StringBuilder("<style type=\"text/css\">");
		sb.append(".header {")
		.append("font-family: Verdana, Helvetica, Arial;")
		.append("color: rgb(0,66,174);")
		.append("background: rgb(234,242,255);")
		.append("font-size: 15px;}\n")
		.append(".desc{padding: 5px 10px;font-family: Verdana, Helvetica, Arial; font-size:12px}\n")
		.append(".butt{font-family: Verdana, Helvetica, Arial; font-size:12px}\n")
		.append("</style>\n");
		
		return sb.toString();
	}
}
