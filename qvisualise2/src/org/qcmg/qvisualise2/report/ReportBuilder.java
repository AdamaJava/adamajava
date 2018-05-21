/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.messages.QMessage;
import org.qcmg.common.model.ProfileType;

import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.ChartTab;
import org.qcmg.qvisualise2.util.QProfilerCollectionsUtils;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javafx.util.Pair;

public class ReportBuilder {
	private static final String[] SEQ_COLOURS = new String[] { "green", "blue", "black", "red", "aqua" };
	private static final String[] CS_COLOURS = new String[] { "blue", "green", "orange", "red", "black" };
	public static final int MAX_REPORT_HEIGHT = 900;
	public static final int MIN_REPORT_HEIGHT = 540;
	public static final int MAX_REPORT_WIDTH = 1400;
	public static final int MIN_REPORT_WIDTH = 800;
	
	private static final QLogger logger = QLoggerFactory.getLogger(ReportBuilder.class);	
	private static final QMessage messages = new QMessage(ReportBuilder.class, ResourceBundle.getBundle("org.qcmg.qvisualise2.messages"));
	private static final String ISIZE = "TLEN";
	private static final String UNMAPPED = "Unmapped";
	private static final String DUPLICATE = "Duplicate";
	private static final String MD = "TAG MD";
	private static final String FLAG = "FLAG";
	private static int reportID;
	
	static final String BAD_READS_DESCRIPTION = messages.getMessage( "BAD_READS_DESCRIPTION" );
	static final String BAD_QUALS_DESCRIPTION = messages.getMessage( "BAD_QUALS_DESCRIPTION" );
	static final String LINE_LENGTH_DESCRIPTION = messages.getMessage( "LINE_LENGTH_DESCRIPTION" );
	static final String TAG_MD_DESCRIPTION = messages.getMessage( "TAG_MD_DESCRIPTION" );
	static final String SUMMARY_NOTES = messages.getMessage( "SUMMARY_NOTES" );
		
	public static Report buildReport( ProfileType type, Element reportElement, int reportNumberId, Element qProfilerElement ) throws Exception {
		final Report report = buildReport(  type,  reportElement,  reportNumberId );	
		report.setRunBy( qProfilerElement.getAttribute( "run_by_user" ) );
		report.setRunOn( qProfilerElement.getAttribute( "start_time" ) );
		report.setVersion( qProfilerElement.getAttribute( "version" ) );		
		return report;
	}

	public static Report buildReport(ProfileType type, Element reportElement, int reportNumberId) throws Exception {
		final String fileName = reportElement.getAttribute("file");
		
		final String recordParsed = reportElement.getAttribute( "records_parsed" );
		final String duplicateRecordCount = reportElement.getAttribute( "duplicate_records" );
		final Report report = new Report( type, fileName, recordParsed, duplicateRecordCount );		
		reportID = reportNumberId;
		
		switch (type) { 
			case bam:		
				createBamHeader( reportElement, report );
				createSummary( reportElement, report );
				createRNEXT( reportElement, report );
				createSEQ( reportElement, report );
				createQUALS( reportElement, report );
				createTAGS( reportElement, report );			
				createISIZE( reportElement, report );
				createRNM( reportElement, report );			
				createCIGAR( reportElement, report );
				createMAPQ( reportElement, report );
				createFLAGS( reportElement, report );
				createCoverage( reportElement, report );
				createMatrix( reportElement, report );			
				break;
			case FASTQ:
				createFastqSummary( reportElement, report);
				createSEQ( reportElement, report);
				createQUALS( reportElement, report);
				break;				
			default:
				break;
		}
		return report;
	}
	

	
	private static void createFaSummary(Element reportElement, Report report) {
		
		ChartTabBuilder  para = new ChartTabBuilder ( reportElement, "KMERS",
				"kmer", "kmer").description("Top 100 6-mers seen in reference genome").chartType(HTMLReportUtils.COLUMN_CHART).setlogScale();			 
		report.addTab( ChartTabBuilderUtils.createTabFromPossibleValue(para) );
	}
	
	private static void createFastqSummary(Element reportElement, Report report) {
		final Element summaryElement = QprofilerXmlUtils.getChildElement( reportElement, "ReadNameAnalysis", 0);
		
		// setup parent tab
		ChartTab parentCT = new ChartTab("Summary", "summ" + reportID);
		
		// table with instrument, run ids, flow cell ids, tile numbers, etc
		Map<String, Map<String, AtomicLong>> summaryMap = ReportBuilderUtils.createFastqSummaryMap(summaryElement);		
		
		ChartTab ct = new ChartTab("Summary", "summ" + reportID);
		ct.setData(HTMLReportUtils.generateGoogleDataForTableStringMapPair(summaryMap, ct.getName()));
		ct.setChartInfo(HTMLReportUtils.generateGoogleSingleTable(ct.getName(), 0, 1300));		
		ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getId(), ct.getName(), 1, false));	
		parentCT.addChild(ct);
		
		ChartTabBuilder  para = new ChartTabBuilder ( summaryElement, "INDEXES", "Index", "index")
			.description("Top 50 indexes seen in fastq sequencing reads").chartType(HTMLReportUtils.COLUMN_CHART).setlogScale();					
		ChartTab indexesTab = ChartTabBuilderUtils.createTabFromValueTally(para);
		if (null != indexesTab)  parentCT.addChild(indexesTab);		
		report.addTab(parentCT);
	}
	
	private static void createSummary( Element reportElement, Report report ) {
		
		final NodeList summaryNL = reportElement.getElementsByTagName(QprofilerXmlUtils.summary );		
		if (null == summaryNL) return;
		 
		final Element summaryElement = (Element) summaryNL.item(0);
		if (null == summaryElement)	return;	
		 				
		Map<String, String> summaryMap = new LinkedHashMap<>();
		NodeList summaryNodes = summaryElement.getChildNodes();
		
		if (null == summaryNodes) { logger.info("summaryNodes was null"); return; }
			 
		for (int i = 0 ; i < summaryNodes.getLength() ; i++) {			
			Node n = summaryNodes.item(i);
			String nodeName = n.getNodeName();
			
			final String startVBlock = "{v: '";
            final String endVBlock = "', p: {style: 'text-align: right'}}" ;
            switch (nodeName) {
                case    QprofilerXmlUtils.FirstOfPair :
                         summaryMap.put("Average length of first-of-pair reads", startVBlock + n.getAttributes().getNamedItem("averageLength").getNodeValue()+ endVBlock);
                        break;
                case QprofilerXmlUtils.SecondOfPair:
                        summaryMap.put("Average length of second-of-pair reads", startVBlock + n.getAttributes().getNamedItem("averageLength").getNodeValue()+ endVBlock);
                        break;
                case QprofilerXmlUtils.mdCycle:
                        int noOfCylces =  Integer.parseInt(n.getAttributes().getNamedItem("value").getNodeValue());                        
                        String rag = "', p:{ style: 'text-align: right; background-color: ";
                        rag += (noOfCylces > 20) ? "tomato;'}}" : (noOfCylces > 10) ? "yellow;'}}" : "palegreen;'}}" ;
                        
                        summaryMap.put("Number of cycles with >1% mismatches", startVBlock + noOfCylces+ rag);
                        break;
                case QprofilerXmlUtils.filteredReads:{
                	    String value = String.format("%,.0f", Double.parseDouble( n.getAttributes().getNamedItem(QprofilerXmlUtils.count).getNodeValue().trim()));
                        summaryMap.put("Discarded reads (FailedVendorQuality, secondary, supplementary)", startVBlock + value + endVBlock);
                        break;
                }case QprofilerXmlUtils.fileReads:{
                		String value = String.format("%,.0f", Double.parseDouble( n.getAttributes().getNamedItem(QprofilerXmlUtils.count).getNodeValue().trim()));
                        summaryMap.put("Total reads including discarded reads", startVBlock + value + endVBlock);
                        break;  
                }						
           }
		}					
								 								
		ChartTab ct = new ChartTab("Summary", "summ" + reportID);;
		StringBuilder sb = new StringBuilder(HTMLReportUtils.generateGoogleDataForTableStringMap(summaryMap, ct.getName()+1, "Property", "Value" ) );
	
		//add header line
		String[] arr = ReportBuilderUtils.createBamSummaryColumnName(ct.getName()+2);
				
		sb.append(  HTMLReportUtils.generateGoogleDataForTableStringMap( ReportBuilderUtils.createBamRgMap( summaryElement ), arr  ) );
		ct.setData(sb.toString());	
		
		sb = new StringBuilder( HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 1, 0, 600) );
		sb.append(HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 2, 0, null));					 
		ct.setChartInfo(sb.toString()); 					
		
		ct.setDescritionButton( "Summary Notes", SUMMARY_NOTES, true); //add first button
//		ct.setDescritionButton("Column Description", SUMMARY_DESCRIPTION, true); //add second button
		
		sb = new StringBuilder( "\n<div id=\"" + ct.getId() + "\" class=\"pane\">") ; 									  
		sb.append(ct.getDescritionButtonHtml())  //eg. <button onclick="toggleDiv('summ12Desc_div');" class="butt">Column Description</button>
			.append( " <p  id=\"" + ct.getName() + 1 + "Chart_div\"></p>" )
			.append( " <p><BR></p>")				
			.append( " <p  id=\"" + ct.getName() + 2 + "Chart_div\"></p>")					
			.append( " <h5 style=\"color:orange;\"> Hover over mouse on above table column name for detailed descritpion </h5>" )			
			.append(  "</div>" );
		ct.setRenderingInfo( sb.toString() );
	 							
		// add summary report to the front
		List<ChartTab> chartTabs = report.getTabs();
		chartTabs.add(0, ct);				
		report.setTabs(chartTabs);
					 	
	}
	//it is optional from qprofiler bam mode
	private static void createMatrix(Element reportElement, Report report) {
		
		final ChartTab matrixParent = new ChartTab("MAPQ Matricies");		
		for(String str : new String[] { "CM", "SM", "Length" }){
			Element element = QprofilerXmlUtils.getChildElement(reportElement, "MAPQMatrix"+str, 0);
			if(element != null){
				ChartTabBuilder  para = new ChartTabBuilder (element,"ValueTally", str+" Matrix",  "mx" + reportID + str.toLowerCase());
				ChartTab ct = ChartTabBuilderUtils.createMatrixChartTab(para);
				if (null != ct) matrixParent.addChild(ct);
			}
		}
		
		if ( ! matrixParent.getChildren().isEmpty()) report.addTab(matrixParent);
	}
	
	private static void createMAPQ(Element reportElement, Report report) {
		 
		ChartTabBuilder  para = new ChartTabBuilder ( QprofilerXmlUtils.getChildElement( reportElement, "MAPQ", 0), "ValueTally", "MAPQ", "mq")
		.setlogScale().chartType(HTMLReportUtils.LINE_CHART).chartTitle("MAPQ Tally").legend(ChartTabBuilder.defaultLegend)
		.setShowCombinedLine().width(ChartTabBuilder.windowWidth);

		report.addTab( ChartTabBuilderUtils.createTabFromPossibleValue(para));
		
	}

	private static void createFLAGS(Element reportElement, Report report) {
		final ChartTab flagTab = new ChartTab( FLAG );
		
		//FLAG
		final Map<String, AtomicLong> flags = new LinkedHashMap<String, AtomicLong>();
		final Element nameElement = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(reportElement, FLAG, 0),"ValueTally", 0);
		QProfilerCollectionsUtils.populateTallyItemMap(nameElement, flags, false, null);
				
		Map<String, AtomicLong> flagsKeyChange = new LinkedHashMap<String, AtomicLong>();
		for (Entry<String, AtomicLong> entry : flags.entrySet()) {
			String[] flagStirngArray = entry.getKey().split(", ");
			flagsKeyChange.put((flagStirngArray.length > 1 ? flagStirngArray[1] + ", ": "")  + flagStirngArray[0], entry.getValue());
		}
			
		ChartTabBuilder  para = new ChartTabBuilder ("Flag", "fl").chartType(HTMLReportUtils.BAR_CHART).setlogScale();		
		flagTab.addChild(ChartTabBuilderUtils.getChartTabFromMap(para, flagsKeyChange) );
		
		// duplicates
		final Map<String, String> dupMap = new HashMap<String, String>();
		dupMap.put("d", "Duplicates");
		final Map<String, AtomicLong> duplicateFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, dupMap, "Singletons");
		para = new ChartTabBuilder ( DUPLICATE, "fld").chartType(HTMLReportUtils.PIE_CHART).setlogScale();
		flagTab.addChild( ChartTabBuilderUtils.getChartTabFromMap(para, duplicateFlags) );
		
		// vendor check
		final Map<String, String> vendorMap = new HashMap<String, String>();
		vendorMap.put("f", "Failed Vendor Check");
		final Map<String, AtomicLong> failedFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, vendorMap, "Passed Vendor Check");
		para = new ChartTabBuilder ( "Vendor Check", "flf").chartType(HTMLReportUtils.PIE_CHART).setlogScale();
		flagTab.addChild( ChartTabBuilderUtils.getChartTabFromMap(para, failedFlags) );		
		
		// first and second
		final Map<String, String> firstSecondMap = new HashMap<String, String>();
		firstSecondMap.put("2", "Second");
		firstSecondMap.put("1", "First");
		final Map<String, AtomicLong> firstSecondFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, firstSecondMap, null);
		para = new ChartTabBuilder ( "Read in Pair", "flfs").chartType(HTMLReportUtils.PIE_CHART).setlogScale();
		flagTab.addChild( ChartTabBuilderUtils.getChartTabFromMap(para, firstSecondFlags) );				

		
		final Map<String, String> mappedMap = new HashMap<String, String>();
		mappedMap.put("u", UNMAPPED);
		final Map<String, AtomicLong> mappedFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, mappedMap, "Mapped");
		para = new ChartTabBuilder ( UNMAPPED, "flm").chartType(HTMLReportUtils.PIE_CHART).setlogScale();
		flagTab.addChild( ChartTabBuilderUtils.getChartTabFromMap(para, mappedFlags) );						
		
		final Map< String, String > primaryMap = new HashMap< String, String >();
		primaryMap.put("s", "secondary");
		primaryMap.put("S", "Supplementary");
		final Map< String, AtomicLong > primaryFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher( flags, primaryMap, "Primary" );

		para = new ChartTabBuilder ("Primary", "fls").chartType(HTMLReportUtils.PIE_CHART).setlogScale();
		flagTab.addChild( ChartTabBuilderUtils.getChartTabFromMap(para, primaryFlags) );						

		report.addTab(flagTab);
	}



	private static void createRNEXT(Element reportElement, Report report) {
		//MRNM
		final NodeList rnextNL = reportElement.getElementsByTagName("RNEXT");
		final Element rnextElement = (Element) rnextNL.item(0);
		if (null != rnextElement) {
			ChartTabBuilder para = new ChartTabBuilder(rnextElement, "ValueTally", "RNEXT", "m" )
						.chartType( HTMLReportUtils.BAR_CHART).chartTitle("RNEXT Tally");//value is string with min size
			//set filter for chr
			para = para.setDataFilter(  new ChartTabBuilder.Filter() {
			 
				@Override
				public boolean result(Element element ){
					
					//only check <TallyItem>; return true if not, that means skip the filter process
					if( !element.getNodeName().equalsIgnoreCase("TallyItem")) return true;
					
					//report all reference start with chr
					String value = element.getAttribute("value");
					if(value == null ) return false;					
					if( element.getAttribute("value").toLowerCase().startsWith("chr"))
						 return true;					
					//otherwise report reference with big percent
					try{ 
						String percent = element.getAttribute("percent").replace("%", "");
						float p = Float.parseFloat(percent);
						if(p > 1) return true;
					}catch(NullPointerException | NumberFormatException e) {
						return false; 
					}						 					
					return false;					
				}				
			});			
			report.addTab( ChartTabBuilderUtils.createTabFromValueTally(para) );
		}
	}
	
	//coverge for each chromosome and readGroup
	private static void createRNM(Element reportElement, Report report) {
		ChartTab parentCT = new ChartTab( "RNAME" );		
		report.addTab(parentCT);
		
		final NodeList rnmNL = reportElement.getElementsByTagName("RNAME_POS");
		final Element rnmElement = (Element) rnmNL.item(0);
		final Element rnameE = QprofilerXmlUtils.getChildElement( rnmElement, "CoverageByReadGroup" , 0 );
		final NodeList nlTop = rnameE.getElementsByTagName("RangeTally");;
		final Element nameElementTop = (Element) nlTop.item(0);
		int rgNo = nameElementTop.getAttribute("possibleValues").split(",").length;
		int cellingValue = Integer.parseInt(nameElementTop.getAttribute("visuallCellingValue"));	
				
		//create coverage by read group
		ChartTabBuilder  para = new ChartTabBuilder (rnameE, "RangeTally", "Coverage with GC percentage", "rgccov")
			.setDataContinue().legend(ChartTabBuilder.defaultLegend).setValueRange().VAxisMaxValue(cellingValue).setShowCombinedLine()
			.chartType(HTMLReportUtils.LINE_CHART).width(ChartTabBuilder.windowWidth).HAxisTitle("position").height((MIN_REPORT_HEIGHT/4) *(rgNo/4) )
			.setParentTagFilter(ChartTabBuilder.filterOfGCRef);		
		parentCT.addChild( ChartTabBuilderUtils.createTabWithGCPercent(para) );
					
		//create coverage by chromosome
		para = new ChartTabBuilder (rnmElement,"CoverageByReadGroup", "Coverage by chromosome", "ccov")
				.legend(ChartTabBuilder.defaultLegend).setValueRange().setDataContinue().explorer(ChartTabBuilder.defaultZoomInExplorer)
				.HAxisFormat(ChartTabBuilder.AXIS_FORMAT_SHORT) 
				.chartType(HTMLReportUtils.LINE_CHART ).height(ChartTabBuilder.windowHeight).width(ChartTabBuilder.windowWidth).HAxisTitle("position");			
		parentCT.addChild( ChartTabBuilderUtils.createTabCoverageByChr(para) );

		//create coverage by chromosome smoothy replace peak with null
		para = new ChartTabBuilder (rnmElement,"CoverageByReadGroup", "Coverage by chromosome (smooth)", "scov")
				.legend(ChartTabBuilder.defaultLegend).setValueRange().setDataContinue().explorer(ChartTabBuilder.defaultZoomInExplorer)
				.LineWidth(0.2).PointSize(1).DataOpacity(0.2).HAxisFormat(ChartTabBuilder.AXIS_FORMAT_SHORT)
				.chartType(HTMLReportUtils.LINE_CHART).height(ChartTabBuilder.windowHeight).width(ChartTabBuilder.windowWidth).HAxisTitle("position");					 		
		parentCT.addChild( ChartTabBuilderUtils.createTabSmoothy(para) );				
	}

	private static ChartTab createMDChartTab(Element tagElement) {
		// don't always have tag MD, MismatchByCycle info
		final Element element =  QprofilerXmlUtils.getChildElement ( tagElement, "MD",0);	
		if (null ==element ||  ! element.hasChildNodes()) return null; 
				
		ChartTabBuilder  para = new ChartTabBuilder (
				QprofilerXmlUtils.getChildElement( element, "MismatchByCycle",0), "CycleTally","MD mismatch","tmd")
				.description(TAG_MD_DESCRIPTION,true).setStacked().setPercentage().legend(ChartTabBuilder.defaultLegend)
				.colours(SEQ_COLOURS).chartType(HTMLReportUtils.BAR_CHART).height(MAX_REPORT_HEIGHT).width(MAX_REPORT_WIDTH).setChartLeftRight().VAxisTitle("Cycle");	
		
		ChartTab mismatchCT = ChartTabBuilderUtils.createTabFromPossibleValue(para);
				
		//stop here if no details of MD information 
		if( mismatchCT == null) return null; 
		
		para = new ChartTabBuilder (
				QprofilerXmlUtils.getChildElement( element,"MutationForward",0), 	"ValueTally","Mutation Forward Strand","mdf")
				.chartTitle("Mutation Forward Strand Tally").chartType(HTMLReportUtils.BAR_CHART).setChartLeftRight().width(MAX_REPORT_WIDTH);	
		ChartTab forwardCT = ChartTabBuilderUtils.createTabFromValueTally(para);
		
		para = new ChartTabBuilder (
				QprofilerXmlUtils.getChildElement( element,"MutationReverse",0), 	"ValueTally","Mutation Reverse Strand","mdr")
				.chartTitle("Mutation Reverse Strand Tally").chartType(HTMLReportUtils.BAR_CHART).setChartLeftRight().width(MAX_REPORT_WIDTH);	
		ChartTab reverseCT = ChartTabBuilderUtils.createTabFromValueTally(para);
		if (  null == forwardCT || null == reverseCT){
			mismatchCT.setTitle(MD);
			return mismatchCT; 
		}  		
				
		para = new ChartTabBuilder (
				QprofilerXmlUtils.getChildElement( element,"MismatchByCycle",0), 	"CycleTally","Cycles with 1% error or more","tmdpc")
				.chartType(HTMLReportUtils.BAR_CHART).setChartLeftRight();	
		ChartTab onePercentCT = ChartTabBuilderUtils.createMDPercentageTab(para);
				
		ChartTab parentCT = new ChartTab(MD);
		mismatchCT.setTitle("MD mismatch");
		parentCT.addChild(mismatchCT);
		parentCT.addChild(onePercentCT);
		parentCT.addChild(forwardCT);
		parentCT.addChild(reverseCT);
 
		return parentCT;
	}
	/**
	 * 		ChartTab ct = new ChartTab("Summary", "summ" + reportID);;
		StringBuilder sb = new StringBuilder(HTMLReportUtils.generateGoogleDataForTableStringMap(summaryMap, ct.getName()+1, "Property", "Value" ) );
	
		//add header line
		String[] arr = new String[] {ct.getName()+2, "Read Group","Read Count","Average<BR>Read<BR>Length","Max<BR>Read<BR>Length","Mode<BR>TLEN","Unmapped<BR>Reads",
				"Non-canonical<BR>ReadPair","Duplicate<BR>Reads","Within<BR>ReadPair<BR>Overlap","Soft<BR>Clipping<BR>(CIGAR)","Hard<BR>Clipping<BR>(CIGAR)",
				"Adaptor<BR>Trimming","Total<BR>Bases<BR>Lost"};
		sb.append(  HTMLReportUtils.generateGoogleDataForTableStringMap( ReportBuilderUtils.createBamRgMap( summaryElement ), arr  ) );
		ct.setData(sb.toString());	
		
		sb = new StringBuilder( HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 1, 0, 600) );
		sb.append(HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 2, 0, null));					 
		ct.setChartInfo(sb.toString());
	 */
	
	private static ChartTab createRGChartTab(Element tagElement) {
		ChartTab parentCT = new ChartTab("TAG RG");
		
		Element element = QprofilerXmlUtils.getChildElement( tagElement,"RG", 0); 
		if ( element != null && element.hasChildNodes())  {  	
 			ChartTabBuilder  para = new ChartTabBuilder ( element,"ValueTally", "RG Tally", "trg" )
				.chartTitle("TAG RG Tally").chartType(HTMLReportUtils.BAR_CHART).HAxisMinValue(0); //min size valueInt=false
			parentCT.addChild( ChartTabBuilderUtils.createTabFromValueTally(para) );
		}
		
		ChartTab ct = new ChartTab("Read Name Analysis", "trg1" );
		Map<String, String> summaryMap = ReportBuilderUtils.createReadNameSummaryMap( QprofilerXmlUtils.getChildElement( tagElement,"ReadNameAnalysis", 0)  );
		ct.setData( HTMLReportUtils.generateGoogleDataForTableStringMap(summaryMap, ct.getName(), "Read Group","Instrument", "Run Id","Flow Cell Id",  "Flow Cell Lane", "Total Tile Number") );	 
		ct.setChartInfo( HTMLReportUtils.generateGoogleSingleTable(ct.getName() , 0, null)  ); 
		parentCT.addChild( ct );
			
		return parentCT;
	}
	
	private static void createTAGS(Element reportElement, Report report) {
		// TAG
		final Element tagElement =  QprofilerXmlUtils.getChildElement( reportElement, "TAG", 0); // (Element) tagNL.item(0);
		
		//TAG-CS	 
		Element element = QprofilerXmlUtils.getChildElement( tagElement, "CS", 0);   	
		if ( element != null && element.hasChildNodes())  
			for (ChartTab ct : buildMultiTabCycles("TAG CS", element, "tcs", "ColourByCycle", "BadColoursInReads", CS_COLOURS, false))  
				report.addTab(ct);
		
		//TAG-CQ
		element = QprofilerXmlUtils.getChildElement( tagElement,"CQ", 0); 
		if ( element != null && element.hasChildNodes())   		
 			for (ChartTab ct : buildMultiTabCycles("TAG CQ", element, "tcq", "QualityByCycle", "BadQualsInReads", null, true))  
				report.addTab(ct);
			 	 		
		//TAG-RG
		ChartTab rgCT = createRGChartTab(tagElement);
		if (null != rgCT) report.addTab(rgCT);  	
		
		//TAG-ZM
		element = QprofilerXmlUtils.getChildElement( tagElement,"ZM", 0); 
		if ( element != null && element.hasChildNodes())  {   
			//min size valueInt=false
			ChartTabBuilder  para = new ChartTabBuilder ( element,"ValueTally", "TAG ZM", "tzm" ).chartTitle("TAG ZM Tally").chartType(HTMLReportUtils.BAR_CHART); 
			report.addTab( ChartTabBuilderUtils.createTabFromValueTally(para) );
		}			
		
		//TAG-MD
		ChartTab mdCT = createMDChartTab(tagElement);
		if (null != mdCT) report.addTab(mdCT);  
	}

	private static void createQUALS(Element reportElement, Report report) {
				
		// QUALS
		final Element qualElement = QprofilerXmlUtils.getChildElement( reportElement, "QUAL", 0);
		final ChartTab parentCT  = new ChartTab("QUAL");	
		
		//setting color default to 60 colors
		int ColorNo = 0;
		List<Element> tallyElements = QprofilerXmlUtils.getChildElementByTagName( QprofilerXmlUtils.getChildElement( qualElement, "QualityByCycle", 0 ), "CycleTally" );
		for(Element ele : tallyElements){
			String possibles = ele.getAttribute(QprofilerXmlUtils.possibles);
			if(possibles != null ){ 
				int ss = possibles.split(QprofilerXmlUtils.COMMA).length ;
				if(ss > ColorNo ) ColorNo = ss;				
			}				
		}
		
		String[] colors = null;
		if(ColorNo > 0){
			int multiple = 255 / ColorNo;
			colors = new String[ColorNo];
			for(int i = 0; i < ColorNo; i ++)
				colors[i] = "rgb(" + (255 - (i * multiple)) + ",0," + (i * multiple) + ")";
		}

		for( ChartTab  tab : buildMultiTabCycles("QUAL", qualElement, "q", "QualityByCycle", "BadQualsInReads", colors, true) )
			parentCT.addChild(tab);
		report.addTab(parentCT);
	}

	private static void createSEQ(Element reportElement, Report report) {
		final Element seqElement = QprofilerXmlUtils.getChildElement( reportElement, "SEQ", 0);
		final ChartTab parentCT  = new ChartTab("SEQ");	
				
		for( ChartTab  tab : buildMultiTabCycles("SEQ", seqElement, "s", "BaseByCycle", "BadBasesInReads",  SEQ_COLOURS, false) )
			parentCT.addChild(tab);
								
		//add kmers tab		Parameters(Element baseElement, String tagName, String tabTitle , String id){
		for(int i : new Integer[]{1,2,3,6}){
			ChartTabBuilder  para = new ChartTabBuilder (QprofilerXmlUtils.getChildElement( seqElement, "mers" + i ,0),  "CycleTally", "kmer_" + i, "kmer_" + i)
				.chartType( ChartTabBuilder.LINE_CHART).legend(ChartTabBuilder.defaultLegend).width(ChartTabBuilder.windowWidth).height(500).chartTitle("kmers Distribution");
			parentCT.addChild( ChartTabBuilderUtils.createTabFromPossibleValue(para));		
		}
		report.addTab(parentCT);		
	}

	//for SEQ, QUAL, MA 
	static List<ChartTab> buildMultiTabCycles( String mainTabName, Element tabElement, String id, String cycleName, String badReadName, String[] colour, boolean isQualData) {		
		List<ChartTab> tabs = new ArrayList<ChartTab>();		
		//create BaseByCycle
		ChartTabBuilder  para = new ChartTabBuilder ( QprofilerXmlUtils.getChildElement( tabElement, cycleName ,0),
				"CycleTally",mainTabName+" Cycles", id+"c").colours(colour).setStacked().legend(ChartTabBuilder.defaultLegend)
				.chartType(HTMLReportUtils.BAR_CHART).width(MAX_REPORT_WIDTH).height(MAX_REPORT_HEIGHT).setChartLeftRight().VAxisTitle("Cycle");		
		tabs.add(  ChartTabBuilderUtils.createTabFromPossibleValue( para) );		
		
		//create LengthTally	
		para = new ChartTabBuilder ( QprofilerXmlUtils.getChildElement( tabElement, "LengthTally",0), "ValueTally",
				mainTabName+" Line Lengths", id+"ll").description(LINE_LENGTH_DESCRIPTION, false).chartType(HTMLReportUtils.COLUMN_CHART)
				.setlogScale().setChartLeftRight().VAxisMinValue(1).width(MAX_REPORT_WIDTH).setValueInt(); 
		tabs.add( ChartTabBuilderUtils.createTabFromValueTally( para)  );
		
		//create badRead
		String descript = isQualData ? BAD_QUALS_DESCRIPTION : BAD_READS_DESCRIPTION;
		para = new ChartTabBuilder ( QprofilerXmlUtils.getChildElement(tabElement, badReadName, 0), "ValueTally",
				mainTabName+" Bad Reads", id+"br").description(descript, false).chartType(HTMLReportUtils.COLUMN_CHART).setValueInt()
				.setlogScale().setChartLeftRight().VAxisTitle("Cycle").VAxisMinValue(1).width(MAX_REPORT_WIDTH).HAxisTitle("Cycle");		
		tabs.add( ChartTabBuilderUtils.createTabFromValueTally(para) );
				
		return tabs;
	}
	 
	private static void createBamHeader( Element reportElement, Report report ) {		
		final Element headerElement = QprofilerXmlUtils.getChildElement( reportElement, "bamHeader" , 0);
		if (null == headerElement)  return; 
		Map<String, List<String>> headerList = QProfilerCollectionsUtils.convertHeaderTextToMap(headerElement.getTextContent());
		if (null == headerList || headerList.isEmpty())  return; 
		
		ChartTab ct = new ChartTab("BAMHeader", "head" + reportID);
		report.addTab(ct);
		
		int i = 0;
		StringBuilder dataStr = new StringBuilder();
		StringBuilder chartStr = new StringBuilder();
		for(Entry<String, List<String>> entry : headerList.entrySet()){
			String name = ct.getName() + (++i);
			
			//create data for each header session
			StringBuilder sb = new StringBuilder("\nvar ").append(name).append(" = new google.visualization.DataTable();");
			sb.append("\n").append(name).append(".addColumn('string', '").append(entry.getKey()).append("');\n");
			sb.append(name+".addRows([");			
			for(String str : entry.getValue()) sb.append("\n['"+ str + "'],");
			//remove , at the end of string
			if(sb.charAt(sb.length()-1) == ',' )  sb.deleteCharAt( sb.length()-1 );			
			sb.append("]);");
			dataStr.append(sb);
			
			//create chart info 
			Integer height = entry.getValue().size() > 50 ? 400 : null; 
			chartStr.append( HTMLReportUtils.generateGoogleSingleTable(name, height, null) );
			
		}
			
		ct.setData(dataStr.toString());
		ct.setChartInfo(chartStr.toString()+"\n");
		ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getId(), ct.getName(), headerList.size(),false));
		//ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getName(), headerList.size(),false));		
	}
	

	private static void createISIZE(Element reportElement, Report report) throws Exception {
		
	   //ISIZE		
		Element  isizeE = QprofilerXmlUtils.getChildElement(reportElement, "ISIZE",  0);
		ChartTab iSizeCT = new ChartTab(ISIZE);
		report.addTab(iSizeCT);

		for(int tLen : new int[]{1500, 5000}){
			ChartTabBuilder  para = new ChartTabBuilder ( isizeE, "ValueTally", "TLEN 0 to " + tLen, "is" + tLen)
				.setlogScale().chartType(HTMLReportUtils.LINE_CHART).chartTitle("iSize Tally").legend(ChartTabBuilder.defaultLegend)
				.setShowCombinedLine().width(ChartTabBuilder.windowWidth).VAxisMinValue(1).height(1000).setDataContinue()
				.explorer(ChartTabBuilder.defaultZoomInExplorer);

			//only check <TallyItem>; return true if not, that means skip the filter process						
			para = para.setDataFilter( ( Element element ) -> {
				if( !element.getNodeName().equalsIgnoreCase("TallyItem")) 
					return false;
				try{ 					 
					int v = Integer.parseInt(element.getAttribute("value")) ;
					if(v < tLen) return true;
				}catch(NullPointerException | NumberFormatException e) {
					return false; 
				}					 					
				return false;						
			});
			
			iSizeCT.addChild( ChartTabBuilderUtils.createTabFromPossibleValue(para));			
		}
	}
		
	private static void createCIGAR( Element reportElement, Report report ) {
		//CIGAR
		final String title = "CIGAR";	
		
		@SuppressWarnings("unchecked")
		final Pair<String,String>[] cigars = (Pair<String,String>[]) new Pair[4];
		cigars[0] = new Pair<String, String>( "D",  "Deletions" );
		cigars[1] = new Pair<String, String>( "I", "Insertions" );
		cigars[2] = new Pair<String, String>( "H", "Hard clips" );
		cigars[3] = new Pair<String, String>( "S", "Soft clips" );
				
		final Element cigarE = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(reportElement, title, 0), "ObservedOperations",  0 );
		final ChartTab cigarCT = new ChartTab(title);
		report.addTab(cigarCT);
		
		for(Pair<String,String> cigar : cigars){
			//create LengthTally	
			ChartTabBuilder  para = new ChartTabBuilder ( cigarE,  "ValueTally", (String)cigar.getValue(),  "cig" + cigar.getKey() )
					.chartType( HTMLReportUtils.COLUMN_CHART).chartTitle(title + ", " + cigar.getValue() )
					.setlogScale().setChartLeftRight().VAxisMinValue(1).height(800).width( MAX_REPORT_WIDTH );
						
			para = para.setDataFilter(  ( Element element ) -> {
				if( !element.getNodeName().equalsIgnoreCase("TallyItem")) return false;
				if( !element.getAttribute(QprofilerXmlUtils.value).contains((String) cigar.getKey()) ) return false;
				return true;						
			});
						
			cigarCT.addChild( ChartTabBuilderUtils.createTabFromValueTally( para)  );			
		}		
	}	
	
	//it is optional from qprofiler bam mode
	private static void createCoverage(Element reportElement, Report report) {
		ChartTab parentCT = new ChartTab( "Coverage" );		
		report.addTab(parentCT);
		
		final NodeList rnmNL = reportElement.getElementsByTagName("RNAME_POS");
		final Element rnmElement = (Element) rnmNL.item(0);
		
		//last week one
		ChartTabBuilder  para = new ChartTabBuilder (rnmElement, "CoverageByReadGroup", "Coverage By GC Content", "gcbin1")
			.setDataContinue().legend(ChartTabBuilder.defaultLegend).setValueRange()
			.explorer(ChartTabBuilder.defaultZoomInExplorer).legend(ChartTabBuilder.defaultLegend).width(ChartTabBuilder.windowWidth).VAxisMinValue(1).height( 1000 ) 
			.chartType(HTMLReportUtils.SCATTER_CHART).width(ChartTabBuilder.windowWidth).HAxisTitle( "GC content %" );				
		parentCT.addChild( ChartTabBuilderUtils.createTabGCBinCoverage( para) );
		
		para = new ChartTabBuilder (rnmElement, "CoverageByReadGroup", "Coverage By GC Content2", "gcbin11")
		.setDataContinue().legend(ChartTabBuilder.defaultLegend).setValueRange()
		.explorer(ChartTabBuilder.defaultZoomInExplorer).legend(ChartTabBuilder.defaultLegend).width(ChartTabBuilder.windowWidth).VAxisMinValue(1).height( 1000 ) 
		.chartType(HTMLReportUtils.SCATTER_CHART).width(ChartTabBuilder.windowWidth).HAxisTitle( "GC content %" );	
		parentCT.addChild( ChartTabBuilderUtils.createTabGCBinCoverage1( para) );
		
		para = new ChartTabBuilder (rnmElement, "CoverageByReadGroup", "Coverage By GC Content Accumulate", "gcbinacc")
			.setDataContinue().legend(ChartTabBuilder.defaultLegend).setValueRange()
			.explorer(ChartTabBuilder.defaultZoomInExplorer).legend(ChartTabBuilder.defaultLegend).width(ChartTabBuilder.windowWidth).VAxisMinValue(1).height( 1000 ) 
			.chartType(HTMLReportUtils.COMBO_CHART).width(ChartTabBuilder.windowWidth).HAxisTitle("GC content %");				
//		parentCT.addChild( ChartTabBuilderUtils.createTabGCAccumulateCoverage( para) );						
	}

}
