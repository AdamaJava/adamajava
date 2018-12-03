/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2.report;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.qvisualise2.ChartTab;

public class HTMLReportUtils {
	public static final String BAR_CHART = "BarChart";
	public static final String COLUMN_CHART = "ColumnChart";
	public static final String PIE_CHART = "PieChart";
	public static final String SCATTER_CHART = "ScatterChart";
	public static final String AREA_CHART = "AreaChart";
	public static final String HEAT_MAP_CHART = "HeatMap";
	public static final String TABLE_CHART = "Table";
	public static final String LINE_CHART = "LineChart";
	public static final String COMBO_CHART = "ComboChart";
		
	public static void generateChartScriptHeader( StringBuilder sb ) {		
		//here we use   "current" rather than version 45
		sb.append("\n<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
				"<script type=\"text/javascript\">google.charts.load('current', {packages: ['corechart', 'table', 'line']});\n" + 
				 "google.charts.setOnLoadCallback(drawChart);"	)		
		.append("\nfunction drawChart() \n{");
	}
	
	public static StringBuilder getJavaScript(List<Report> reports){
		StringBuilder sb  = new StringBuilder();
		//2018 below jquery works		
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>\n");
		sb.append("<script src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js\"></script>\n");		
		sb.append("<script type=\"text/javascript\">\n $(document).ready(function () {\n");	
		sb.append(" $( \"#tabs\" ).tabs();\n"); //active tabs
		
		// loop through all tabs, if we have descriptions set, disable them initially so they are not 'on' by default
		for (Report report : reports) {
			for (ChartTab tab: report.getTabs()) {	
				if(tab == null) continue;				
				//active children tab by java script
				if(tab.getChildren() != null && !tab.getChildren().isEmpty())
					sb.append( "$(\"#" + tab.getId() + "\").tabs();\n"  );				
				sb.append(tab.getToggleFlaseDescritpion());
				for (ChartTab child: tab.getChildren()) {	
					sb.append(child.getToggleFlaseDescritpion());
				}
			}
		}
		
		sb.append("});");
		for (Report report : reports) 
			for (ChartTab tab: report.getTabs()){  				
				sb.append( tab.getPopDescritpionJavaScript() );
				for (ChartTab child: tab.getChildren())
					sb.append( child.getPopDescritpionJavaScript() );
			}
				
		//popWindowJavascriptFunction
		sb.append("\n function toggleWindow( des, title ) { ");
		sb.append("\nvar win = window.open(\"\", \"_blank\", 'left=0,top=0,toolbar=no,location=no,status=1,menubar=yes,scrollbars=yes,resizable=yes,width=350,height=250');"   );		
		sb.append("\nwin.document.write( des ); \nwin.document.title = title;}");
		sb.append("\n function toggleDiv(divId) { $(\"#\"+divId).toggle('fast'); } ");
		sb.append("\n</script>");	
		
		return sb;
	}
	
	public static String getStyle() {
		
		return "<style> " +
"\n.header {font-family: Verdana, Helvetica, Arial;color: rgb(0,66,174);background: rgb(234,242,255);font-size: 15px;}" +
"\n.desc{padding: 5px 10px;font-family: Verdana, Helvetica, Arial; font-size:12px}" +
"\n.butt{font-family: Verdana, Helvetica; border: none;  margin: 5px; background-color: rgb(234,242,255); float: left;   color:rgb(0,66,174);  font-size:12px}" +
"\n.left {float: left; width: 50%;}" +
"\n.right {float: right; width: 50%;}" +
"\ntable { font-family: Verdana, Helvetica, Arial; font-size:12px }" +

//tabs
"\nul.tabs { margin:0 !important; padding:0; height:30px; border-bottom:1px solid #666; }" +

/* single tab */
"\nul.tabs li { float:left; text-indent:0; padding:0; margin:0 !important; 	list-style-image:none !important; }" +

/* link inside the tab. uses a background image */
"\nul.tabs a { float:left; 	font-family: Verdana, Helvetica, Arial;" +
"	font-size:12px; display:block; padding:5px 30px; text-decoration:none; border:1px solid #666; border-bottom:0px;" +
"	height:18px; background-color:rgb(234,242,255); color:rgb(0,66,174); margin-right:2px; position:relative; top:1px;	" +
"	outline:0; border-radius:4px 4px 0 0; -moz-border-radius:4px 4px 0 0; }" +

"\nul.tabs a:active { 	background-color:#ddd; 	border-bottom:1px solid #ddd; color:#CCCCCC; cursor:default; }" +

/* when mouse enters the tab move the background image */
"\nul.tabs a:hover { 	background-position: -420px -31px; background-color:#CCFFFF; color:#333; }" +

/* active tab uses a class name "current". its highlight is also done by moving the background image. */
"\nul.tabs a.current, ul.tabs a.current:hover, ul.tabs li.current a { background-position: -420px -62px;	cursor:default !important; font-weight:bold; color:#000066 !important; }" +

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
"\nul.tabs a.xl 		{ background-position: 0 -0px; width:248px; }" +
"\nul.tabs a.xl:hover 	{ background-position: 0 -31px; }" +
"\nul.tabs a.xl.current { background-position: 0 -62px; }" +

/* initially all panes are hidden */ 
"\n.panes, .pane { 	display:none; }" +

/* tooltip style for summary page */
//"\n.uitooltip { font-size: 14px !important; background: rgba(234,242,255, 1); color: green; border-radius: 4px; text-align: center; position: absolute; max-width: 300px; z-index: 9999; 	padding: 10px 5px 10px 5px; # top right bottom left }" +
"\n</style>\n"; //end of style
}
	
	public static <T, R> String generateGoogleData(
			Map<T, R> dataSet, String name, boolean isValueString, String xAxisLabel, String yAxisLabel)  {
		
		StringBuilder sb = new StringBuilder("\nvar " + name + " = google.visualization.arrayToDataTable([\n" );		
		sb.append(  "['" + xAxisLabel + "','" + yAxisLabel + "'],\n" ); //column name X and Y
		
		int i = 0;
		for (Entry<T, R> entry : dataSet.entrySet()) {
			if (i++ > 0) sb.append(",\n");
			sb.append("[" + (isValueString ? "'"+entry.getKey() +"'" : entry.getKey()) + ", " + entry.getValue() + "]"); 			
		}
		sb.append("\n]);"); 
		
		return sb.toString();
	}
	
	public static <T, R> String generateGoogleData(	Map<T, R> dataSet, String name, boolean isValueString) {
		return generateGoogleData( dataSet, name, isValueString, "value", "Count" );
	}	
	
	public static <T,R> String generateGoogleaArraywithDuplicatedKey(
			Map<T, AtomicLongArray> dataSet,  Map<T, R> lastColumnData, String name, boolean isValueString, String labelName, String label2ndvaxis,  boolean showCombined, boolean zero2null) {
		StringBuilder sb = new StringBuilder("\nvar " + name + " = google.visualization.arrayToDataTable([\n['value'");
		//add column name						 
		  sb.append(  ", '" + labelName + "'" );
		 if( lastColumnData != null) sb.append(  ", '" + label2ndvaxis + "'" ) ;
		 sb.append( "],");
		 		 
		 //add data
		 boolean firstRow = true;
		 for( Entry<T, AtomicLongArray> row : dataSet.entrySet() ) {			  
			 //set data column type to number for continue axis			 
			 
			 String key = row.getKey() instanceof Double? String.format("\n[%.2f", row.getKey()) : "\n["+row.getKey();
			 AtomicLongArray array = row.getValue();			 
			 for(int i = 0; i < array.length(); i++) {	
				 String count = (array.get(i) == 0 && zero2null && !firstRow)? null : array.get(i)+"";
				 sb.append( key ).append(  ", ").append(count).append( "]," );						 
			 } 
			 			 
			 //after read first row set the switch to false
			 if( firstRow == true ) firstRow = false;
		 }
		 
		 int pos = sb.lastIndexOf("],");
		 sb.replace(pos+1, pos+2, "\n]);");
				
		 return sb.toString();		
	}
	
	/**
	 *  to draw chart with double Y-axis
	 * @param dataSet
	 * @param lastColumnData
	 * @param name
	 * @param isValueString
	 * @param labelNames
	 * @param label2ndvaxis
	 * @param showCombined
	 * @param zero2null
	 * @return
	 */
	public static <T,R> String generateGoogleaArraywith2ndVAxis(
			Map<T, AtomicLongArray> dataSet,  Map<T, R> lastColumnData, String name, boolean isValueString, List<String> labelNames, String label2ndvaxis,  boolean showCombined, boolean zero2null) {
		StringBuilder sb = new StringBuilder("\nvar " + name + " = google.visualization.arrayToDataTable([\n['value'");
		//add column name						 
		 for(String label : labelNames) sb.append(  ", '" + label + "'" );
		 if( showCombined) sb.append(  ",'combined'" );
		 if( lastColumnData != null) sb.append(  ", '" + label2ndvaxis + "'" ) ;
		 sb.append( "],");
		 		 
		 //add data
		 boolean firstRow = true;
		 for( Entry<T, AtomicLongArray> row : dataSet.entrySet() ) {			  
			 //set data column type to number for continue axis			 
			 sb.append( row.getKey() instanceof Double? String.format("\n[%.2f", row.getKey()) : "\n["+row.getKey() );
			 AtomicLongArray array = row.getValue();
			 
			 for(int i = 0; i < array.length(); i++) {	
				 String count = (array.get(i) == 0 && zero2null && !firstRow)? null : array.get(i)+"";
				 sb.append(  ", ").append(count);						 
			 } 
			 
			 if(showCombined){
				 long combined = 0;
				 for(int i = 0; i < array.length(); i++)   combined += array.get(i) ;	
				 sb.append( ", " ).append(combined);	
			 }
			 
			 if( lastColumnData != null ) 			    
				 sb.append( ", " ).append( lastColumnData.get( row.getKey()) == null ? "0.00" : lastColumnData.get(row.getKey()) );
			 
			 sb .append( "]," );	
			 //after read first row set the switch to false
			 if( firstRow == true ) firstRow = false;
		 }
		 
		 int pos = sb.lastIndexOf("],");
		 sb.replace(pos+1, pos+2, "\n]);");
				
		 return sb.toString();		
	}
	
	
	public static <T> String generateGoogleArray(
			Map<T, AtomicLongArray> dataSet,  String name, boolean isValueString, List<String> labelNames,  boolean showCombined, boolean zero2null) {
		StringBuilder sb = new StringBuilder("\nvar " + name + " = google.visualization.arrayToDataTable([\n['value'");
		//add column name						 
		 for(String label : labelNames) sb.append(  ", '" + label + "'" );
		 if( showCombined) sb.append(  ",'combined'" );
		 sb.append( "],");
		 		 
		 //add data
		 boolean firstRow = true;
		 for( Entry<T, AtomicLongArray> row : dataSet.entrySet() ) {			  
			 //set data column type to number for continue axis			 
			 sb.append( row.getKey() instanceof Double? String.format("\n[%.2f", row.getKey()) : "\n["+row.getKey() );
			 AtomicLongArray array = row.getValue();
			 
			 for(int i = 0; i < array.length(); i++) {	
				 String count = (array.get(i) == 0 && zero2null && !firstRow)? null : array.get(i)+"";
				 sb.append(  ", ").append(count);						 
			 } 
			 
			 if(showCombined){
				 long combined = 0;
				 for(int i = 0; i < array.length(); i++)   combined += array.get(i) ;	
				 sb.append( ", " ).append(combined);	
			 }
			 
			 sb .append( "]," );	
			 //after read first row set the switch to false
			 if( firstRow == true ) firstRow = false;
		 }
		 
		 int pos = sb.lastIndexOf("],");
		 sb.replace(pos+1, pos+2, "\n]);");
				
		 return sb.toString();		
	}

	//debug
	@Deprecated
	public static void generateGoogleaArraywithGCPercent4R(
			Map<Integer, AtomicLongArray> dataSet,  Map<Integer, String> lastColumnData, String name, boolean isValueString, List<String> labelNames, boolean showCombined) {
		StringBuilder sb = new StringBuilder("");	
		 for(String label : labelNames) sb.append(  "'" + label + "', " );
		 if(showCombined) sb.append(  "'combined'" );
		 if(lastColumnData != null) sb.append(", 'GC%'").append("\n");

		 //add data
		 for( Entry<Integer, AtomicLongArray> row : dataSet.entrySet() ) {
			 AtomicLongArray array = row.getValue();		 
			 for(int i = 0; i < array.length(); i++) 				 
				 sb.append(array.get(i)).append(  ", ");		 
			 
			 if(showCombined){
				 long combined = 0;
				 for(int i = 0; i < array.length(); i++)  combined += array.get(i);
				 sb.append(combined);	
			 }
			 
			 if(lastColumnData != null) 			    
				 sb.append( ", " ).append( lastColumnData.get(row.getKey()) == null ? "0.00" : lastColumnData.get(row.getKey()) );			 
			sb .append( "\n");			
		 }
		 
		 //remove last "\n"
		System.out.println(sb.substring(0, sb.length()-2));		
		 
	}
	
	public static String generateGoogleaArraywithAnnotation(
			Map<Integer, AtomicLongArray> dataSet,  Map<Integer, String> annotation, String name, boolean isValueString, List<String> labelNames, boolean showCombined) {
		return generateGoogleaArraywithAnnotation( dataSet,  annotation,   name,   isValueString,   labelNames,   showCombined , false );
	}
	//MD mismatch shows percentage; kmers, isize, rname coverage without percentage
	public static String generateGoogleaArraywithAnnotation(
		Map<Integer, AtomicLongArray> dataSet,  Map<Integer, String> annotation, String name, boolean isValueString, List<String> labelNames, 
		boolean showCombined, boolean zero2null) {
		StringBuilder sb = new StringBuilder("\nvar " + name + " = google.visualization.arrayToDataTable([\n['value'");
		//add column name						 
		 for(String label : labelNames) sb.append(  ",'" + label + "'" );
		 if(showCombined) sb.append(  ",'combined'" );
		 sb.append( "],");
		 		 
		 //add data
		 boolean firstRow = true;
		 for( Entry<Integer, AtomicLongArray> row : dataSet.entrySet() ) {	
			 //discard empty row, they are not null
			 AtomicLongArray array = row.getValue();
			 if(array == null || array.length() == 0 ) continue; 
			 sb.append(  "\n[" + row.getKey() ); //set data column type to number for continue axis			 
			 String f = null; 
			 if(annotation != null  && annotation.get(row.getKey()) != null  )
			 	f = annotation.get(row.getKey());
			 
			 for(int i = 0; i < array.length(); i++){  
				 if(f == null){
					 //convert the zero value to null string except first row,
					 //otherwise google.visualization.arrayToDataTable will confuse the data type and throw 
					 // exception: All series on a given axis must be of the same data type
					 String count = (array.get(i) == 0 && zero2null && !firstRow)? null : array.get(i)+"";
					 sb.append(  ", ").append(count);						 
				 }else
					 sb.append(", {v: ").append(array.get(i)).append(",f: '").append(array.get(i)).append(" ("+f + ")'}");
			 }
			 if(showCombined){
				 long combined = 0;
				 for(int i = 0; i < array.length(); i++)   combined += array.get(i) ;	
				 if(f == null)
					 sb.append(  ", ").append(combined);	
				 else
					 sb.append(", {v: ").append(combined).append(",f: '").append(combined).append("("+f + ")'}");				 
				 
			 }			 
			 sb.append( "],");
			 //after read first row set the switch to false
			 if(firstRow == true) firstRow = false;
		 }
		 
		 int pos = sb.lastIndexOf("],");
		 sb.replace(pos+1, pos+2, "\n]);");
				
		 return sb.toString();				 				
	}
	
	//convert data formate to current google version
	public static String generateGoogleDataForTableStringMap(Map<String, String> dataSet, String ...names) {
		StringBuilder sb = new StringBuilder();
		 		
		sb .append("\nvar ").append(names[0]).append(" = new google.visualization.DataTable();\n" );
		for(int i = 1; i < names.length; i++)
			sb.append(names[0])  .append( ".addColumn('string', '" + names[i] + "');\n");
		
		sb.append(names[0])  .append( ".addRows([");
		for (Entry<String, String> entry : dataSet.entrySet()) {
			String v = entry.getValue().trim();
			if( (v.startsWith("{") && v.endsWith("}")) || (v.startsWith("'") && v.endsWith("'")) )
				sb.append("['").append(entry.getKey()).append("', ").append(v).append("],\n");
			else
				sb.append("['").append(entry.getKey()).append("', '").append(v).append("'],\n");
		}
		if(sb.length() > 0)
			sb.delete(sb.length()-2, sb.length()-1);//delete , and \n
		// end of rows
		sb.append("]);\n");
		 			
		return sb.toString();
	}	
	
	//for FastqSummary table
	public static String generateGoogleDataForTableStringMapPair(Map<String, Map<String, AtomicLong>> dataSet, String name) {
		StringBuilder sb = new StringBuilder("\nvar "); 		
		sb.append(name).append(" = new google.visualization.DataTable();\n" );
		
		//add column name. eg. name.addColumn('String', 'value')
		sb.append(name).append( ".addColumn('string', 'Name');\n");  
		sb.append(name).append( ".addColumn('string', 'Value');\n");   
		sb.append(name).append( ".addColumn('number', 'Number');\n");   
		
		//add rows
		sb.append(name) .append( ".addRows([\n");
 		int j = 0;
		for (Entry<String, Map<String, AtomicLong>> entry : dataSet.entrySet()) {
 			if (j++ > 0) sb.append(",\n");			
			int k = 0 ;
			for (Entry<String, AtomicLong> innerEntry : entry.getValue().entrySet()) {
				if (k++ > 0)  sb.append(",\n");			 				
				sb.append("['").append(entry.getKey()).append("',");
				sb.append(" '").append(innerEntry.getKey()).append("',");
				sb.append(" ").append(innerEntry.getValue().longValue()).append("]");
			}
		}		
		sb.append("\n]);");	// end of rows
		
		return sb.toString();
	}
	

	
	public static String generateGoogleChart(String dataName, String chartTitle, int width,
			int height, String chartType, boolean logScale, boolean isStacked) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, chartType);
		
		sb.append(chartName).append(String.format(".draw(%s, {width: %d, height: %d, title: ' %s '",dataName, width, height, chartTitle))
			.append(", chartArea:{left:150,top:40,width:\"75%\",height:\"75%\"} ");
		
		if (isStacked) 	sb.append(", isStacked: true");
		 		
		if (COLUMN_CHART.equals(chartType) || AREA_CHART.equals(chartType)) {
			sb.append(", fontSize:12");
			sb.append(", hAxis: {title: 'Value', titleColor: 'blue'" + (width > 1000 ? ", textStyle:{fontSize:9}" : "") + "}");
			sb.append(", vAxis: {title: '" + (logScale ? "Log( " : "") + "Count " + (logScale ? ") " : "") +"', titleColor: 'blue',logScale: " + logScale + ", }");
			sb.append(", legend: 'none'");
		} else if (BAR_CHART.equals(chartType)) {
			sb.append(", fontSize:12");
			sb.append(", hAxis: {minValue: 0,  title: '" + (logScale ? "Log( " : "") + "Count " + (logScale ? ") " : "") +"', titleColor: 'blue',logScale: " + logScale + "}");
			sb.append(", vAxis: {title: 'Value', titleColor: 'blue'}");
			sb.append(", legend: 'none'"); //for flag
		} else if (PIE_CHART.equals(chartType)) {
			sb.append(", is3D: 'true'");
		}
		sb.append("});");
		
		return sb.toString();
	}	
	
//update to suit google current
	/**
	 * 
	 * @param dataName
	 * @param height: if null set to 98%; if zero set nothing; else set: " + height + ", "
	 * @param width: if null set to 98%; if zero set nothing; else set: " + width + ", "
	 * @return
	 */
	public static String generateGoogleSingleTable(String dataName, Integer height, Integer width) {
		StringBuilder sb = new StringBuilder();		
		String chartName = dataName + "Chart";
		initialTableSetup(sb, chartName); 
		String heightStr = ( height == null)? " height: '98%', ":  ( height <= 0 )?  "" : " height: " + height + ", "; 							
		String widthStr = (width == null ) ? " width: '98%', " :  ( width <= 0) ?  "" : " width: " + width+ ", "; 
		sb.append(chartName + ".draw(" + dataName +", {" + widthStr  +  heightStr + " allowHtml: true,  showRowNumber: true");
		
		sb.append("});");
		
		return sb.toString();
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
				
			if (i % 2 == 1) { 	sb.append("</tr>\n"); }
			i++;
		}
		
		// add trailing tr
		if ( ! sb.toString().endsWith("</tr>\n")) {
			sb.append("</tr>\n");
		}
		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
		
	public static String generateRenderingTableInfo(String tabid, String dataName, int numberOfTables, boolean addBlank  ) {
		
		StringBuilder sb = new StringBuilder( );
		
		if(tabid == null)
			sb.append("\n<div class=\"pane\">\n<table>" );
		else
			sb.append("\n<div id=\"" + tabid + "\" class=\"pane\">\n<table>");
		
		
		for (int i = 1 ; i <= numberOfTables ; i++) {
			String chartName = (numberOfTables == 1) ? dataName + "Chart_div": dataName + i + "Chart_div";
			sb.append("<tr><td id = \"" + chartName + "\"></td></tr>\n");
			//add blank line between each table
			if(addBlank && i < numberOfTables)
				sb.append("<td bgcolor=\"#FFFFFF\" style=\"line-height:30px;\" colspan=3>&nbsp;</td>\n"); 
		}
			 		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
	
	public static String generateRenderingTableInfo(String prefix, List<String> tabIds, int columns) {
		StringBuilder sb = new StringBuilder("\n<div class=\"pane\">\n<table>");  
				
		for (int i =0; i < tabIds.size(); i ++ ) {
			String chartName =  prefix + tabIds.get(i) + "Chart_div";
			if (i % columns == 0)  sb.append("<tr>");			 			
			sb.append("<td id = \"" + chartName + "\"></td>");			
			if (i % columns == (columns - 1))  sb.append("</tr>\n");			 			
		}
		
		sb.append("</table>\n</div>");
		return sb.toString();
	}
	
	public static String generateGoogleChartSlidingColors(String dataName, String chartTitle, int width,
			int height, String chartType, boolean logScale, boolean isStacked, int noOfColors, String[] colours) {
		
		String chartName = dataName + "Chart";
		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, chartType);
		
		 sb.append(chartName)
		.append(String.format(".draw(%s, { width: %d, height: %d, title: ' %s '", dataName, width, height, chartTitle ))
		.append(", chartArea:{left:100,top:40,width:\"75%\",height:\"75%\"} ");
		
		if (isStacked) { sb.append(", isStacked: true"); }
		
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
			sb.append(", legendFontSize: 10" );
		
		}
		sb.append(", fontSize:12, titleFontSize:15, hAxis: {title: 'Count', titleColor: 'blue', logScale: " + logScale + "}");
		sb.append(", vAxis: {title: 'Cycle', titleColor: 'blue',logScale: " + logScale + "}});");
		return sb.toString();
	}
	
	public static String generateGoogleScatterChart( String dataName, String chartTitle,
			int width, int height, boolean logScale) {
		return generateGoogleChart(dataName, chartTitle, width+"", height, logScale, SCATTER_CHART, null,  
				null, " pointSize: 2, lineWidth: 1" );
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
	/*
	@Deprecated
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

	*/
	
	///////////////////////////////////////////////////////////////////////////
	// Private Methods
	///////////////////////////////////////////////////////////////////////////
	
	
	private static void initialChartSetup(StringBuilder sb, String chartName, String chartType) {
		sb.append( 
		String.format("\nvar %s = new google.visualization.%s(document.getElementById('%s'));", chartName, chartType, chartName+"_div"));
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
		.append("(document.getElementById('")
		.append(chartName)
		.append("_div'));");
	}
	
//	private static void basicChartSetup(StringBuilder sb, String chartName,
//			String chartData, String chartTitle, int width, int height) {
//		
//		sb.append(chartName)
//		.append(String.format(".draw(%s, {width: %d, height: %d, title: ' %s '",chartData, width, height, chartTitle))
//		.append(", chartArea:{left:150,top:40,width:\"75%\",height:\"75%\"} ");
//		
//	}
	

	@Deprecated	
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
	@Deprecated
	public static String generateGoogleChart( String dataName, String chartTitle,
			String width, Integer height, boolean logScale,  String chartType , String hTitle, String legend, String otherOptions ) {
		
		String chartName = dataName + "Chart";		
		StringBuilder sb = new StringBuilder();
		initialChartSetup(sb, chartName, chartType);
		
		sb.append(chartName + String.format(".draw(%s, { width: %s, height: %d, title: ' %s '",dataName, width, height, chartTitle))	;
		sb.append(", chartArea:{left:150,top:40,width:\"75%\",height:\"75%\"} ");
		sb.append(", hAxis: {title: '" +  (hTitle == null? "Value":hTitle )+ "', titleColor: 'blue'}");
		sb.append(", vAxis: {title: '" + (logScale ? "Log( Count )" : "Count") + "', titleColor: 'blue',logScale: " + logScale + ", format: '0'}");
		sb.append(", legend: " + ((legend != null && legend.trim().length() > 0) ?  legend : "'none'") ); 
		if(otherOptions != null && otherOptions.trim().length() > 0){
			String str = otherOptions.trim();
			if(!str.startsWith(",")) sb.append(",");
			sb.append( otherOptions );			
		}
		sb.append(" });");
 
		return sb.toString();
	}
}
