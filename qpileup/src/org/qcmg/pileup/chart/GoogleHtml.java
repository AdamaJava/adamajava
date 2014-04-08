/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.chart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.qcmg.pileup.PileupConstants;


public class GoogleHtml {
	
	final static String HTML = "html";
	final static String HEAD = "head";
	final static String BODY = "body";
	final static String GOOGLEJS = "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>";
	final static String SCRIPTOPEN = "<script type=\"text/javascript\">";
	final static String SCRIPT = "script";
	final static String NEWLINE = "\n";
	private File file;
	private HtmlRecord record;
	
	public GoogleHtml(String htmlDir, String type, HtmlRecord record, String fileEnding) throws IOException {
		this.record = record;
		this.file = new File(htmlDir + PileupConstants.FILE_SEPARATOR +  type + fileEnding);
	}
	
	public File write() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(draw());
		writer.close();
		return file;
	}

	private String draw() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(open(HTML));
		sb.append(open(HEAD));
		sb.append(GOOGLEJS).append(NEWLINE);
		sb.append(SCRIPTOPEN).append(NEWLINE);
		sb.append("google.load(\"visualization\", \"1\", {packages: ['table', 'corechart']});").append(NEWLINE);
	    sb.append("google.setOnLoadCallback(drawChart);").append(NEWLINE);
	    sb.append("function drawChart() {").append(NEWLINE);
	    
	    
    	sb.append(record.drawChart());
	    
    	sb.append(NEWLINE);
	    
	    
        sb.append("}").append(NEWLINE);
        sb.append("function drawSort() {").append(NEWLINE);

    	sb.append(NEWLINE);
	    
	    
        sb.append("}").append(NEWLINE);
		sb.append(close(SCRIPT));
		sb.append(close(HEAD));
		sb.append(open(BODY));
		sb.append("<body style=\"font-family: Arial;border: 0 none;\">").append(NEWLINE);
		sb.append(record.getPageTitle());
		
		 if (record != null) {
			sb.append(record.drawDisplayDivs());
		 }
    	
    	sb.append(NEWLINE);
	    
		sb.append(close(BODY));
		sb.append(close(HTML));
		return sb.toString();
	}


	private String open(String value) {
		return "<" + value + ">\n";
	}
	
	private String close(String value) {
		return "</" + value + ">\n";
	}



}


