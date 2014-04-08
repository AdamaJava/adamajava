/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


public class StrandedMetricsHtmlRecord extends HtmlRecord {
	
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
	TreeMap<Integer, Map<String, Integer>> fclips = new TreeMap<Integer, Map<String, Integer>>(); //<Integer: position, Map<HDFFileName, Value>>
	TreeMap<Integer, Map<String, Integer>> ftotals = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> fnonrefs = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> findels = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> fmapqs = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> rclips = new TreeMap<Integer, Map<String, Integer>>(); //<Integer: position, Map<HDFFileName, Value>>
	TreeMap<Integer, Map<String, Integer>> rtotals = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> rnonrefs = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> rindels = new TreeMap<Integer, Map<String, Integer>>();
	TreeMap<Integer, Map<String, Integer>> rmapqs = new TreeMap<Integer, Map<String, Integer>>();
	private String file;
	private List<String> hdfs = new ArrayList<String>();
	private String info;
	private String summaryFile; 
	
	public StrandedMetricsHtmlRecord(String title, String hAxis, String vAxis, String file, String info, String summaryFile) throws Exception {
		super(title, hAxis, vAxis);	
		this.file = file;
		this.info = info;
		this.summaryFile = summaryFile;
		
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
			 if (line.startsWith("##")) {
				 continue;
			 }
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
		Integer fclipCount = new Integer(values[CLIP_SSTART_INDEX_F]) + new Integer(values[CLIP_HSTART_INDEX_F]);
		addToMap(hdf, pos, fclips, fclipCount);
		Integer rclipCount = new Integer(values[CLIP_SSTART_INDEX_R]) + new Integer(values[CLIP_HSTART_INDEX_R]);
		addToMap(hdf, pos, rclips, rclipCount);
		//indels
		Integer indelCount = new Integer(values[DEL_INDEX_F]) +  new Integer(values[INS_INDEX_F]);
		addToMap(hdf, pos, findels, indelCount);
		indelCount = new Integer(values[DEL_INDEX_R]) + new Integer(values[INS_INDEX_R]);
		addToMap(hdf, pos, rindels, indelCount);
		//nonrefbases
		Integer nonrefCount = new Integer(values[NONREF_INDEX_F]);
		addToMap(hdf, pos, fnonrefs, nonrefCount);
		nonrefCount = new Integer(values[NONREF_INDEX_R]);
		addToMap(hdf, pos, rnonrefs, nonrefCount);
		//totals
		Integer totalCount = new Integer(values[REF_INDEX_F]) + new Integer(values[NONREF_INDEX_F]) + fclipCount + new Integer(values[DEL_INDEX_F]);
		addToMap(hdf, pos, ftotals, totalCount);
		totalCount = new Integer(values[REF_INDEX_R]) + new Integer(values[NONREF_INDEX_R]) + rclipCount + new Integer(values[DEL_INDEX_R]);
		addToMap(hdf, pos, rtotals, totalCount);
		
		Integer mapqCount = new Integer(values[MQ_INDEX_F]);
		addToMap(hdf, pos, fmapqs, mapqCount);
		mapqCount = new Integer(values[MQ_INDEX_R]);
		addToMap(hdf, pos, rmapqs, mapqCount);
		
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
	public String drawChart() throws IOException {
		
		writeSummaryFile();
		
		StringBuilder sb = new StringBuilder();
		sb.append(constructDataTable());
		
		//Options
        
        //Dataview all elements
        
        for (int i=0; i< hdfs.size(); i++) {
        	sb.append(getOptions("options" + i, hdfs.get(i)  + "", hAxis, vAxis));
        	sb.append("var dataView"+i+"= new google.visualization.DataView(data"+i+");").append(NEWLINE);       
            
            sb.append("dataView"+i+".setColumns([0,2,3,4,5,7,8,9,10]);").append(NEWLINE);           
            sb.append("var chart"+i+" = new google.visualization.LineChart(document.getElementById('chart_div"+i+"'));").append(NEWLINE);
            sb.append("chart"+i+".draw(dataView"+i+", options"+i+");").append(NEWLINE);
            sb.append("var table"+i+" = new google.visualization.Table(document.getElementById('table_div"+i+"'));").append(NEWLINE);
            sb.append("table"+i+".draw(data"+i+", null);").append(NEWLINE);
        }
        
       
		return sb.toString();
	}	
	
	private void writeSummaryFile() throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(summaryFile), true));
		String[] vals = info.split(";");
		Integer hpStart = null;
		Integer hpEnd = null;
		String base = null;
		for (String s: vals) {
			if (s.startsWith("HpStart")) {
				hpStart = new Integer(s.replace("HpStart=", ""));
			}
			if (s.startsWith("HpEnd")) {
				hpEnd = new Integer(s.replace("HpEnd=", ""));
			}
			if (s.startsWith("HPBase")) {
				base = s.replace("HPBase=", "");
			}
		}
		
		for (int i = 0; i< hdfs.size(); i++) {			
			String hdf = hdfs.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(getCounts(hdf, fclips.firstKey(), hpStart-1));
			sb.append(getCounts(hdf, hpStart, hpEnd));
			sb.append(getCounts(hdf, hpEnd+1, fclips.lastKey()));
			w.write(hdf + "\t" + base + "\t" + hpStart + "\t" + hpEnd + "\t" + sb.toString() + "\n");			
		}
		w.close();
	}
	
	private String getCounts(String hdf, int start, int end) {
		int count = 0;
		
		int totalCount = 0;
		int clipCount = 0;
		int indelCount =  0;
		int nonrefCount =  0;
		//int mapQCount =0;
		int rtotalCount = 0;
		int rclipCount = 0;
		int rindelCount =  0;
		int rnonrefCount =  0;
		//int rmapQCount = 0;
		
		for (Entry<Integer, Map<String, Integer>> entry: fclips.entrySet()) {
		
		Integer key = entry.getKey();
		
		if (key>=start && key <=end) {
			count++;
			totalCount = ftotals.get(key).get(hdf);
			clipCount += getDoubleCount(fclips.get(key).get(hdf), totalCount);
			indelCount += getDoubleCount(findels.get(key).get(hdf), totalCount);
			nonrefCount += getDoubleCount(fnonrefs.get(key).get(hdf), totalCount);
			//mapQCount += getDoubleAvgMapQ(fmapqs.get(key).get(hdf), totalCount);
			rtotalCount = rtotals.get(key).get(hdf);
			rclipCount += getDoubleCount(rclips.get(key).get(hdf), rtotalCount);
			rindelCount += getDoubleCount(rindels.get(key).get(hdf), rtotalCount);
			rnonrefCount += getDoubleCount(rnonrefs.get(key).get(hdf), rtotalCount);
			//rmapQCount += getDoubleAvgMapQ(rmapqs.get(key).get(hdf), rtotalCount);	
		}			
	}
//	System.out.println("ref " + start + " " + end + " " + count + " " + " " + nonrefCount + " " + rnonrefCount);
//	System.out.println("ind " + start + " " + end + " " + count + " " + " " + indelCount + " " + rindelCount);
//	System.out.println("clip " + start + " " + end + " " + count + " " + " " + clipCount + " " + rclipCount);
//	System.out.println("amp " + start + " " + end + " " + count + " " + " " + mapQCount + " " + rmapQCount);
	DecimalFormat df = new DecimalFormat("##.###");
	return  df.format((double)clipCount/(double)count) + "\t" +
			df.format((double)indelCount/(double)count) + "\t" + 
			df.format((double)nonrefCount/(double)count) + "\t" + 
			//df.format((double)mapQCount/(double)count) + "\t" + 
			df.format((double)rclipCount/(double)count) + "\t" + 
			df.format((double)rindelCount/(double)count) + "\t" + 
			df.format((double) rnonrefCount/(double)count) + "\t"; 
			//df.format((double)rmapQCount/(double)count) + "\t";
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
			sb.append("'Position', 'F Totals', 'F %Clips', 'F %Indels', 'F %NonRefBases', 'R Avg MapQ', 'R Totals', 'R %Clips', 'R %Indels', 'R %NonRefBases', 'R Avg MapQ'");
			sb.append("],").append(NEWLINE);			
			String hdf = hdfs.get(i);
			
			for (Entry<Integer, Map<String, Integer>> entry: fclips.entrySet()) {

				Integer key = entry.getKey();
				int totalCount = ftotals.get(key).get(hdf);
				String clipCount = getCount(fclips.get(key).get(hdf), totalCount);
				String indelCount = getCount(findels.get(key).get(hdf), totalCount);
				String nonrefCount = getCount(fnonrefs.get(key).get(hdf), totalCount);
				String mapQCount = getAvgMapQ(fmapqs.get(key).get(hdf), totalCount);
				int rtotalCount = rtotals.get(key).get(hdf);
				String rclipCount = getCount(rclips.get(key).get(hdf), totalCount);
				String rindelCount = getCount(rindels.get(key).get(hdf), totalCount);
				String rnonrefCount = getCount(rnonrefs.get(key).get(hdf), totalCount);
				String rmapQCount = getAvgMapQ(rmapqs.get(key).get(hdf), totalCount);				
				
				sb.append("[" + entry.getKey() + "," + totalCount + "," + clipCount + "," + indelCount + "," + nonrefCount + "," + mapQCount + ","
						+ rtotalCount + "," + rclipCount + "," + rindelCount + "," + rnonrefCount + "," + rmapQCount + 
						"],");	
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
	
//	private double getDoubleAvgMapQ(Integer count, int totalCount) {
//		DecimalFormat df = new DecimalFormat("#.00");
//		if (count > 0 && totalCount > 0) {
//			return (double) count / (double)totalCount;
//		} else {
//			return 0;
//		}
//	}

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
	
	private double getDoubleCount(Integer count, int totalCount) {
		
		if (count > totalCount) {
			return 100;
		} else if (count > 0 && totalCount > 0) {
			return (double) count / (double)totalCount * 100;
		} else {
			return 0;
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
