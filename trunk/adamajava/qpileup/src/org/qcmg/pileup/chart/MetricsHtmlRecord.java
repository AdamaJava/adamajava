/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


public class MetricsHtmlRecord extends HtmlRecord {
	
	final static int REF_INDEX_F = 9;
	final static int NONREF_INDEX_F = 10;
	final static int MQ_INDEX_F = 8;
	final static int INS_INDEX_F = 13;
	final static int DEL_INDEX_F = 14;
	final static int DELSTART_INDEX_F = 15;
	final static int CLIP_S_INDEX_F = 16;
	final static int CLIP_SSTART_INDEX_F = 17;
	final static int CLIP_H_INDEX_F = 18;
	final static int CLIP_HSTART_INDEX_F = 19;
	
	final static int REF_INDEX_R = 26;
	final static int NONREF_INDEX_R = 27;
	final static int MQ_INDEX_R = 25;
	final static int INS_INDEX_R = 30;
	final static int DEL_INDEX_R = 31;
	final static int DELSTART_INDEX_R = 32;
	final static int CLIP_S_INDEX_R = 33;
	final static int CLIP_SSTART_INDEX_R = 34;
	final static int CLIP_H_INDEX_R = 35;
	final static int CLIP_HSTART_INDEX_R = 36;
	
	final static int POS_INDEX = 1;
	Map<Integer, Map<String, Integer>> clips = new TreeMap<Integer, Map<String, Integer>>(); //<Integer: position, Map<HDFFileName, Value>>
	Map<Integer, Map<String, Integer>> totals = new TreeMap<Integer, Map<String, Integer>>();
	Map<Integer, Map<String, Integer>> nonrefs = new TreeMap<Integer, Map<String, Integer>>();
	Map<Integer, Map<String, Integer>> indels = new TreeMap<Integer, Map<String, Integer>>();
	Map<Integer, Map<String, Integer>> mapqs = new TreeMap<Integer, Map<String, Integer>>();
	private String file;
	private List<String> hdfs = new ArrayList<String>();
	private String info; 
	
	public MetricsHtmlRecord(String title, String hAxis, String vAxis, String file, String info) throws Exception {
		super(title, hAxis, vAxis);	
		this.file = file;
		this.info = info;
		setUpMaps();		
	}


	private void setUpMaps() throws Exception {
		
		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		//header - ignore
		reader.readLine();
		String line;
		String hdf = null;
		while ((line = reader.readLine()) != null) {
			//name of file
			if (line.startsWith("#")) {
				hdf = line.replace("#", "");
				hdfs.add(hdf);
			} else {
				String[] values = line.split("\t");
				addToMaps(hdf, values);
			}
			
		}
		
		reader.close();
	}

	private void addToMaps(String hdf, String[] values) {
		Integer pos = new Integer(values[POS_INDEX]);
		//clips
		Integer clipCount = new Integer(values[CLIP_SSTART_INDEX_F]) + new Integer(values[CLIP_SSTART_INDEX_R]) + new Integer(values[CLIP_HSTART_INDEX_F]) + new Integer(values[CLIP_HSTART_INDEX_R]);
		addToMap(hdf, pos, clips, clipCount);
		//indels
		Integer indelCount = new Integer(values[DEL_INDEX_F]) + new Integer(values[DEL_INDEX_R]) + new Integer(values[INS_INDEX_F]) + new Integer(values[INS_INDEX_R]);
		addToMap(hdf, pos, indels, indelCount);
		//nonrefbases
		Integer nonrefCount = new Integer(values[NONREF_INDEX_F]) + new Integer(values[NONREF_INDEX_R]);
		addToMap(hdf, pos, nonrefs, nonrefCount);
		//totals
		Integer totalCount = new Integer(values[REF_INDEX_F]) + new Integer(values[REF_INDEX_R]) + new Integer(values[NONREF_INDEX_F]) + new Integer(values[NONREF_INDEX_R]) + clipCount + new Integer(values[DEL_INDEX_F]) + new Integer(values[DEL_INDEX_R]);
		addToMap(hdf, pos, totals, totalCount);
		Integer mapqCount = new Integer(values[MQ_INDEX_F]) + new Integer(values[MQ_INDEX_R]);
		addToMap(hdf, pos, mapqs, mapqCount);
	}

	private void addToMap(String hdf, Integer posKey, Map<Integer, Map<String, Integer>> map, Integer value) {
		if (map.containsKey(posKey)) {
			map.get(posKey).put(hdf, value);
		} else {
			Map<String, Integer> treemap = new TreeMap<String, Integer>();
			treemap.put(hdf, value);
			map.put(posKey, treemap);
		}		
	}

	@Override
	public String drawChart() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(constructDataTable());
		
		//Options
        
        //Dataview all elements
        
        for (int i=0; i< hdfs.size(); i++) {
        	sb.append(getOptions("options" + i, hdfs.get(i)  + "", hAxis, vAxis));
        	sb.append("var dataView"+i+"= new google.visualization.DataView(data"+i+");").append(NEWLINE);       
            
            sb.append("dataView"+i+".setColumns([0,2,3,4,5]);").append(NEWLINE);           
            sb.append("var chart"+i+" = new google.visualization.LineChart(document.getElementById('chart_div"+i+"'));").append(NEWLINE);
            sb.append("chart"+i+".draw(dataView"+i+", options"+i+");").append(NEWLINE);
            sb.append("var table"+i+" = new google.visualization.Table(document.getElementById('table_div"+i+"'));").append(NEWLINE);
            sb.append("table"+i+".draw(data"+i+", null);").append(NEWLINE);
        }
        
       
		return sb.toString();
	}	
	
	private String getOptions(String name, String title, String hAxis, String vAxis) {
		StringBuilder sb = new StringBuilder();
		sb.append("var "+name+" = {");
        sb.append("title: '"+title+"',").append(NEWLINE);
        sb.append("hAxis: {title: '"+hAxis+"', titleTextStyle: {color: 'red'}},").append(NEWLINE);
        sb.append("vAxis: {title: '"+vAxis+"', maxValue: 100}").append(NEWLINE);
        sb.append("};").append(NEWLINE);
        return sb.toString();
	}
	
	private String constructDataTable() {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i< hdfs.size(); i++) {
			sb.append("var data"+i+" = google.visualization.arrayToDataTable([").append(NEWLINE);
			sb.append("[");
			sb.append("'Position', 'Totals', '%Clips', '%Indels', '%NonRefBases', 'Avg MapQ'");
			sb.append("],").append(NEWLINE);			
			String hdf = hdfs.get(i);
			for (Entry<Integer, Map<String, Integer>> entry: clips.entrySet()) {

				Integer key = entry.getKey();
				int totalCount = totals.get(key).get(hdf);
				String clipCount = getCount(clips.get(key).get(hdf), totalCount);
				String indelCount = getCount(indels.get(key).get(hdf), totalCount);
				String nonrefCount = getCount(nonrefs.get(key).get(hdf), totalCount);
				String mapQCount = getAvgMapQ(mapqs.get(key).get(hdf), totalCount);
				
				sb.append("[" + entry.getKey() + "," + totalCount + "," + clipCount + "," + indelCount + "," + nonrefCount + "," + mapQCount +  "],");	
				sb.append(NEWLINE);
			}	    
			sb.append("]);").append(NEWLINE);			
		}
		
		return sb.toString();
	}

	private String getAvgMapQ(Integer count, int totalCount) {
		DecimalFormat df = new DecimalFormat("#.00");
		if (count > 0 && totalCount > 0) {
			return df.format(((double) count / (double)totalCount));
		} else {
			return "0.00";
		}
	}

	private String getCount(Integer count, int totalCount) {
		DecimalFormat df = new DecimalFormat("#.00");
		if (count > totalCount) {
			return "100.00";
		} else if (count > 0 && totalCount > 0) {
			return df.format(((double) count / (double)totalCount) * 100);
		} else {
			return "0.00";
		}
	}
	
	@Override 
	public String drawDisplayDivs() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(writeInfoDiv());
		for (int i=0; i<hdfs.size(); i++) {			
			sb.append(writeChartDiv(i));			
		}
		for (int i=0; i<hdfs.size(); i++) {
			sb.append(writeHeadingDiv(hdfs.get(i), i));
			sb.append(writeTableDiv(i));
		}
		return sb.toString();	
	}

	public String writeChartDiv(int i) {
		return "<div id=\"chart_div"+i+"\" style=\"width: 1350px; height: 400px;\"></div></br>\n";		
	}

	public String writeTableDiv(int i) {
		return "<div id=\"table_div"+i+"\" style=\"width: 1350px; height: 500px;\"></div></br>\n";		
	}

	public String writeHeadingDiv(String hdf, int i) {
		return "<div id=\"title_div"+i+"\" style=\"width: 1350px; height: 40px;\">"+ hdf+ "</div></br>\n";		
	}
	
	@Override
	public String getPageTitle() {	
		return "<div>"+title+ "</div></br>" + NEWLINE;		
	}
	
	private String writeInfoDiv() {
		return "<div id=\"info_div\" style=\"width: 1350px; height: 40px;\">"+info+"</div></br>";	
	}


}
