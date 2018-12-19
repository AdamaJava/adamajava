package org.qcmg.qvisualise2.report;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.ChartTab;
import org.qcmg.qvisualise2.report.ChartTabBuilder;
import org.qcmg.qvisualise2.report.ChartTabBuilderUtils;
import org.qcmg.qvisualise2.report.HTMLReportUtils;
import org.qcmg.qvisualise2.report.ReportBuilder;
import org.w3c.dom.Element;

public class ChartTabBuilderTest {
	private static final String INPUT_FILE = "input.sam";
	@Test
	public void createTabFromPossibleValueTest() throws ParserConfigurationException{
		Element seqE = createSeqElement();
		//create BaseByCycle
		String id = "sc";
		ChartTabBuilder para = new ChartTabBuilder( QprofilerXmlUtils.getChildElement(seqE, "BaseByCycle", 0),
				"CycleTally","SEQ Cycles", id).setStacked().legend(ChartTabBuilder.defaultLegend)
				.chartType(HTMLReportUtils.BAR_CHART).width(ReportBuilder.MAX_REPORT_WIDTH).height(ReportBuilder.MAX_REPORT_HEIGHT).setChartLeftRight().VAxisTitle("Cycle");				
		ChartTab tab = ChartTabBuilderUtils.createTabFromPossibleValue( para); 
		
	    String[] dataArray = tab.getData().split("\n");	    
	    int ii = 1;
	    assertEquals( dataArray[ii++],  String.format(  "var %s0 = google.visualization.arrayToDataTable([", id ) );
	    assertEquals( dataArray[ii ++], "['value','A','C','G','T','N'],");
	    for(int i : new int[]{1, 2, 3, 30})	    	
	    	assertEquals( dataArray[ii++], String.format( "[%d, 1, 0, 1, 0, 0]," ,  i) );	    
	    assertEquals( dataArray[ii++],   "[40, 1, 0, 1, 0, 0]"  );
	    assertEquals( dataArray[ii ],   "]);"  );
	    
	    dataArray = tab.getChartInfo().split(";");
	    dataArray[1] = dataArray[1].trim();
	    assertEquals( dataArray[0],  "\nvar sc0Chart = new google.visualization.BarChart(document.getElementById('sc0Chart_div'))" );
	    	    
	    String pattern = String.format("sc0Chart.draw(%s, {width: %s, height: %s, title: '%s'","sc0", ReportBuilder.MAX_REPORT_WIDTH, ReportBuilder.MAX_REPORT_HEIGHT, QprofilerXmlUtils.FirstOfPair+" SEQ Cycles");
	    assertTrue( dataArray[1].startsWith(pattern) );
	    assertTrue( dataArray[1].endsWith( "})")   );	    
	    String charInfo = dataArray[1].substring( pattern.length(), dataArray[1].length()-2 ).trim();	
	    
	    pattern = ", legend: " + ChartTabBuilder.defaultLegend ;
	    charInfo.contains(pattern);
	    charInfo = charInfo.replace(pattern, "").replaceAll(" ", "");	    

	    for(String pat : new String[]{",chartArea:{left:100,top:40,width:\"75%\",height:\"75%\"}",
	    		",isStacked:true",  ",fontSize:12",  ",hAxis:{title:'Count',titleColor:'blue',logScale:false}" 
	    		, ",vAxis:{title:'Cycle',titleColor:'blue',gridlines:{color:'transparent'}}"  }){
	    	assertTrue(charInfo.startsWith(pat));
	    	charInfo = charInfo.substring(pat.length(), charInfo.length());   	
	    }	
	   	    
 	    assertTrue( charInfo.length() == 0 );
	}
 	
	//test static ChartTab createTabFromValueTally( Parameters para )  	
	@Test
	public void createTabFromValueTallyTest() throws ParserConfigurationException{
		Element seqE = createSeqElement();
		//create LengthTally	
		String id = "sll";
		ChartTabBuilder para = new ChartTabBuilder( QprofilerXmlUtils.getChildElement( seqE, "LengthTally",0), "ValueTally",
				 "SEQ Line Lengths", id).description(ReportBuilder.LINE_LENGTH_DESCRIPTION).chartType(HTMLReportUtils.COLUMN_CHART)
				.setlogScale().setChartLeftRight().VAxisMinValue(1).VAxisMaxValue(100).setValueInt();  
		ChartTab tab =  ChartTabBuilderUtils.createTabFromValueTally( para)  ;
		
		//test data
	    String[] dataArray = tab.getData().split("\n");	    
	    int ii = 1;
	    assertEquals( dataArray[ii++],  String.format(  "var %s0 = google.visualization.arrayToDataTable([", id ) );
	    assertEquals( dataArray[ii ++], "['value','Count'],");
	    for(int i = 19; i < 24; i ++ )	    	
	    	assertEquals( dataArray[ii++], String.format( "[%d, 100]," ,  i) );	  
	    assertEquals( dataArray[ii++],  "[24, 100]" );	
	    assertEquals( dataArray[ii ],   "]);"  );
			
	    //char info
		 dataArray = tab.getChartInfo().split(";");
		 assertEquals( dataArray[0],  "\nvar sll0Chart = new google.visualization.ColumnChart(document.getElementById('sll0Chart_div'))" );   
		 
		 String charInfo = dataArray[1].trim();
	 
		 String pattern = String.format("%sChart.draw(%s, {width: %s, height: %s, title: '%s'","sll0","sll0", ReportBuilder.MIN_REPORT_WIDTH, ReportBuilder.MIN_REPORT_HEIGHT, QprofilerXmlUtils.FirstOfPair+" SEQ Line Lengths");
		 assertTrue( charInfo.startsWith(pattern) );
		 assertTrue( charInfo.endsWith( "})")   );	 
	 
		 charInfo = charInfo.substring( pattern.length(), dataArray[1].length()-2 ).replaceAll(" ","");	

	    for(String pat : new String[]{",chartArea:{left:100,top:40,width:\"75%\",height:\"75%\"}",
	    		",fontSize:12", ",hAxis:{title:'Value',titleColor:'blue',gridlines:{color:'transparent'}}" 
	    		, ",vAxis:{viewWindow:{min:1,max:100},title:'Log(Count)',titleColor:'blue',logScale:true}", ",legend:'none'" }){
	    	assertTrue(charInfo.startsWith(pat));
	    	charInfo = charInfo.substring(pat.length(), charInfo.length());   			   
	    }		    
		assertTrue( charInfo.length() == 0 );
		
		//render infor		
		dataArray = tab.getRenderingInfo().split("<br>");
		
		assertTrue(dataArray[0].equals("\n<div id=\"seqlinelengths\" class=\"pane\"><button onclick=\"toggleDiv('des_sll1_div')\" class=\"butt\">Description</button><div id=\"des_sll1_div\" class=\"desc\">") );
		assertTrue(dataArray[1].equals("<p>This chart shows the distribution of read lengths for the run</p></div><BR><p><p class=\"left\" id=\"sll0Chart_div\"></p></p></div>"));
										 
	}
	
	public static Element createSeqElement() throws ParserConfigurationException{
		Element root = QprofilerXmlUtils.createRootElement( "SEQ", null);
		
		//BaseByCycle
		Element ele = QprofilerXmlUtils.createSubElement(root, "BaseByCycle");	
		ele = QprofilerXmlUtils.createSubElement(ele, QprofilerXmlUtils.cycleTally);
		ele.setAttribute(QprofilerXmlUtils.possibles , "A,C,G,T,N" );
		ele.setAttribute(QprofilerXmlUtils.source , QprofilerXmlUtils.FirstOfPair );
		
		for(int i : new int[]{1,2,3, 30,40}){
			Element cycleE = QprofilerXmlUtils.createSubElement(ele, QprofilerXmlUtils.cycle );
			cycleE.setAttribute(QprofilerXmlUtils.value, i+"");
			cycleE.setAttribute(QprofilerXmlUtils.counts, "1,0,1,0,0");			
		}
		
		//LengthTally
		ele = QprofilerXmlUtils.createSubElement(root, "LengthTally");	
		ele = QprofilerXmlUtils.createSubElement(ele, QprofilerXmlUtils.valueTally);
		ele.setAttribute(QprofilerXmlUtils.source , QprofilerXmlUtils.FirstOfPair );
		
		for(int i : new int[]{19, 20,21,22, 23, 24} ) {
			Element cycleE = QprofilerXmlUtils.createSubElement(ele, QprofilerXmlUtils.tallyItem );
			cycleE.setAttribute(QprofilerXmlUtils.value, i+"");
			cycleE.setAttribute(QprofilerXmlUtils.count, "100");	
			cycleE.setAttribute(QprofilerXmlUtils.percent, "0.01%");	
		}		
		
		return root; 		
	}

}
