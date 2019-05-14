/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.ReferenceNameComparator;
//import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.qvisualise.ChartTab;
import org.qcmg.qvisualise.Messages;
import org.qcmg.qvisualise.QVisualiseException;
import org.qcmg.qvisualise.util.CycleDetailUtils;
import org.qcmg.qvisualise.util.QProfilerCollectionsUtils;
import org.qcmg.qvisualise.util.SummaryByCycle;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReportBuilder {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ReportBuilder.class);	
	private static final String ISIZE = "TLEN";
	private static final String UNMAPPED = "Unmapped";
	private static final String DUPLICATE = "Duplicate";
	private static final String MD = "TAG MD";
	private static final String FLAG = "FLAG";
	private static final String[] SEQ_COLOURS = new String[] { "green", "blue", "black", "red", "aqua" };
	private static final String[] CS_COLOURS = new String[] { "blue", "green", "orange", "red", "black" };
	private static int reportID;
	private static final int MAX_REPORT_HEIGHT = 900;
	private static final int MIN_REPORT_HEIGHT = 540;
	private static final int MAX_REPORT_WIDTH = 1400;
	private static final int MIN_REPORT_WIDTH = 800;
	
	private static final String BAD_READS_DESCRIPTION = Messages.getMessage("BAD_READS_DESCRIPTION");
	private static final String BAD_QUALS_DESCRIPTION = Messages.getMessage("BAD_QUALS_DESCRIPTION");
	private static final String LINE_LENGTH_DESCRIPTION = Messages.getMessage("LINE_LENGTH_DESCRIPTION");
	private static final String TAG_MD_DESCRIPTION = Messages.getMessage("TAG_MD_DESCRIPTION");
	private static final String SUMMARY_DESCRIPTION = Messages.getMessage("SUMMARY_DESCRIPTION");
	private static final String SUMMARY_NOTES = Messages.getMessage("SUMMARY_NOTES");
		
	public static Report buildReport(ProfileType type, Element reportElement, int reportNumberId, Element qProfilerElement) throws QVisualiseException {
		final Report report = buildReport(  type,  reportElement,  reportNumberId);
	
		report.setRunBy( qProfilerElement.getAttribute("run_by_user") );
		report.setRunOn( qProfilerElement.getAttribute("start_time") );
		report.setVersion( qProfilerElement.getAttribute("version") );
		
		return report;
	}

	public static Report buildReport(ProfileType type, Element reportElement, int reportNumberId) throws QVisualiseException {
		final String fileName = reportElement.getAttribute("file");
		
		final String recordParsed = reportElement.getAttribute("records_parsed");
		final String duplicateRecordCount = reportElement.getAttribute("duplicate_records");
		final Report report = new Report(type, fileName, recordParsed, duplicateRecordCount);		
		reportID = reportNumberId;
		
		switch (type) {
		case BAM:
 			createBamHeader(reportElement, report);
			createSEQ(reportElement, report);
			createQUALS(reportElement, report);
			createTAGS(reportElement, report);
 			createISIZE(reportElement, report);
			createRNM(reportElement, report);
			createRNEXT(reportElement, report);
			createCIGAR(reportElement, report);
			createMAPQ(reportElement, report);
			createFLAGS(reportElement, report);
			createCoverage(reportElement, report);
			createMatrix(reportElement, report);
			createSummary(reportElement, report);
			
			break;
		case QUAL:
			for (ChartTab ct : buildMultiTabCycles(false,"Qual", reportElement, "qual",
					"QualityByCycle", "BadQualsInReads", CycleDetailUtils.getQualFileCycle(), null, true)) {
				report.addTab(ct);
			}
			break;
		case FASTA:
			for (ChartTab ct : buildMultiTabCycles(false,"Fasta", reportElement, "fasta",
					"ColourByCycle", "BadColoursInReads", CycleDetailUtils.getTagCSNumericCycle(), CS_COLOURS, false)) {
				report.addTab(ct);
			}
			break;
		case FASTQ:
			createFastqSummary(reportElement, report);
			createSEQ(reportElement, report);
			createQUALS(reportElement, report);
			break;
		case FA:
			createFaSummary(reportElement, report);
			break;
		case MA:
			for (ChartTab ct : buildMultiTabCycles(false,"Ma", reportElement, "ma",
					"ColourByCycle", "BadBasesInReads", CycleDetailUtils.getTagCSNumericCycle(), CS_COLOURS, false)) {
				report.addTab(ct);
			}
			break;
		}
		return report;
	}
	
	private static void addEntryToSummaryMap(Element reportElement, String elementName, String mapEntryName, Map<String, Map<String, AtomicLong>> summaryMap) {
		final NodeList nodeList = reportElement.getElementsByTagName(elementName);
		if (null != nodeList) {
			final Element element = (Element) nodeList.item(0);
			if (null != element) {
				Map<String, AtomicLong> sourceMap = new HashMap<>();
				QProfilerCollectionsUtils.populateTallyItemMap(element, sourceMap, false);
				
				for (Entry<String, AtomicLong> entry : sourceMap.entrySet()) {
					// get map from summaryMap
					Map<String, AtomicLong> map = summaryMap.get(mapEntryName);
					if (null == map) {
						map = new HashMap<String, AtomicLong>();
						summaryMap.put(mapEntryName, map);
					}
					map.put(entry.getKey(), entry.getValue());
				}
			} else {
				System.out.println("null " + elementName  + " element");
			}
		} else {
			System.out.println("null  " + elementName  + " NL");
		}
	}
	
	private static void createFaSummary(Element reportElement, Report report) {
		
		// setup parent tab
		report.addTab(addTop100Chart(reportElement, "KMERS", "Kmer", "kmer", "Top 100 6-mers seen in reference genome", 100, true));
	}
	private static void createFastqSummary(Element reportElement, Report report) {
		
		// setup parent tab
		ChartTab parentCT = new ChartTab("Summary", "summ" + reportID);
		
		// table with instrument, run ids, flow cell ids, tile numbers, etc
		Map<String, Map<String, AtomicLong>> summaryMap = new LinkedHashMap<>();
		
		// instruments first
		addEntryToSummaryMap(reportElement, "INSTRUMENTS", "Instrument", summaryMap);
		addEntryToSummaryMap(reportElement, "RUN_IDS", "Run Id", summaryMap);
		addEntryToSummaryMap(reportElement, "FLOW_CELL_IDS", "Flow Cell Id", summaryMap);
		addEntryToSummaryMap(reportElement, "FLOW_CELL_LANES", "Flow Cell Lane", summaryMap);
		addEntryToSummaryMap(reportElement, "PAIR_INFO", "Pair", summaryMap);
		addEntryToSummaryMap(reportElement, "FILTER_INFO", "Filter", summaryMap);
		addEntryToSummaryMap(reportElement, "TILE_NUMBERS", "Tile Number", summaryMap);
		
		
		ChartTab ct = new ChartTab("Summary", "summ" + reportID);
		ct.setData(HTMLReportUtils.generateGoogleDataForTableStringMapPair(summaryMap, ct.getName()));
		ct.setChartInfo(HTMLReportUtils.generateGoogleSingleTable(ct.getName(), 0, 1300));
		
		ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getName(), 1, false));
		
		parentCT.addChild(ct);

		ChartTab indexesTab = addTop100Chart(reportElement, "INDEXES", "Index", "index", "Top 50 indexes seen in fastq sequencing reads", 50, true);
		if (null != indexesTab) {
			parentCT.addChild(indexesTab);
		}
		
		report.addTab(parentCT);
	}
	
	private static ChartTab addTop100Chart(Element reportElement, String nodeName, String charTitle, String chartId, String description, int number, boolean logScale) {
		final NodeList nodeList = reportElement.getElementsByTagName(nodeName);
		if (null != nodeList) {
			final Element element = (Element) nodeList.item(0);
			if (null != element) {
				
				Map<String, AtomicLong> map = new HashMap<>();
				QProfilerCollectionsUtils.populateTallyItemMap(element, map, false);
				List <String> list = new ArrayList<>();
				
				for (Entry<String, AtomicLong> entry : map.entrySet()) {
					list.add(entry.getValue() + "-" + entry.getKey());
				}
				Collections.sort(list, new Comparator<String>() {
					@Override
					public int compare(String arg0, String arg1) {
						//strip the number part out of the string
						int arg0Tally = Integer.parseInt(arg0.substring(0, arg0.indexOf("-")));
						int arg1Tally = Integer.parseInt(arg1.substring(0, arg1.indexOf("-")));
						return arg1Tally - arg0Tally;
					}
				});
				
				int magicNumber = Math.min(number,  list.size());
				
				Map<String, AtomicLong> top100Entries = new LinkedHashMap<>();
				for (int i = 0 ; i < magicNumber ; i++) {
					String entry =  list.get(i);
					
					int dashIndex = entry.indexOf("-");
					String key = entry.substring(dashIndex + 1);
					AtomicLong al = new AtomicLong(Long.parseLong(entry.substring(0, dashIndex)));
					
					top100Entries.put(key, al);
				}
				
				final ChartTab charTab = new ChartTab(charTitle, chartId);
				charTab.setData(HTMLReportUtils.generateGoogleData(top100Entries, charTab.getName(), true));
				charTab.setChartInfo(HTMLReportUtils.generateGoogleChart(charTab.getName(), 
						charTitle, 1200, MIN_REPORT_HEIGHT,
						HTMLReportUtils.COLUMN_CHART, logScale, false));
				charTab.setDescription(description + " (total number: " + map.size() + ")");
				
				return charTab;
			} else {
				System.out.println("null " + nodeName + " Element");
			}
		} else {
			System.out.println("null " + nodeName + "NL");
		}
		return null;
	}
	
	
	private static void createSummary(Element reportElement, Report report) {
		
		final NodeList summaryNL = reportElement.getElementsByTagName("SUMMARY");
				
		if (null != summaryNL) {
			final Element summaryElement = (Element) summaryNL.item(0);
			if (null != summaryElement) {
				
				Map<String, String> summaryMap = new LinkedHashMap<>();
				NodeList summaryNodes = summaryElement.getChildNodes();
				
				if (null != summaryNodes) {
					for (int i = 0 ; i < summaryNodes.getLength() ; i++) {
						
						Node n = summaryNodes.item(i);
						String nodeName = n.getNodeName();
						
						final String startVBlock = "{v: '";
						final String endVBlock = "', p: {style: 'text-align: right'}}]}" ;
						switch (nodeName) {
						case "FirstInPairAveLength":
							summaryMap.put("Average read length of first in pair reads", startVBlock + n.getAttributes().getNamedItem("value").getNodeValue()+ endVBlock);
							break;
						case "SecondInPairAveLength":
							summaryMap.put("Average read length of second in pair reads", startVBlock + n.getAttributes().getNamedItem("value").getNodeValue()+ endVBlock);
							break;
						case "MDMismatchCycles":
							int noOfCylces =  Integer.parseInt(n.getAttributes().getNamedItem("value").getNodeValue());
							
							String rag = "', p:{ style: 'text-align: right; background-color: ";
							rag += (noOfCylces > 20) ? "tomato;'}}]}" : (noOfCylces > 10) ? "yellow;'}}]}" : "palegreen;'}}]}" ;
							
							summaryMap.put("Number of cycles with >1% mismatches", startVBlock + noOfCylces+ rag);
							break;
						case "Failed_Secondary_Supplementary":
							summaryMap.put("Discarded reads (FailedVendorQuality, secondary, supplementary)", startVBlock + n.getAttributes().getNamedItem("value").getNodeValue()+ endVBlock);
							break;
						case "inputedReads":
							summaryMap.put("Total inputed reads including counted and discarded reads", startVBlock + n.getAttributes().getNamedItem("value").getNodeValue()+ endVBlock);
							break;	
						}
					}					
					
					//*****start xu										
					ChartTab ct = new ChartTab("Summary", "summ" + reportID);
					String str = HTMLReportUtils.generateGoogleDataForTableStringMap(summaryMap, ct.getName()+1, "Property", "Value"  );	
					
					//add RG table
					final NodeList isizeNL = reportElement.getElementsByTagName("ISIZE");
					final Element isizeElement = (isizeNL == null)? null : (Element) isizeNL.item(0);
				 
					summaryMap = createRgMap( summaryElement, isizeElement ); //coding
					String[] arr = summaryMap.remove("Read Group").split(",");  //table header
					arr[0] = ct.getName()+2; 					
					str += HTMLReportUtils.generateGoogleDataForTableStringMap(summaryMap,  arr  );
					ct.setData(str);	
					
					str = HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 1, 0, 600);
					str += HTMLReportUtils.generateGoogleSingleTable(ct.getName() + 2, 0, null);
					ct.setChartInfo(str); 					
					
					str = "\n<div class=\"pane\">" 
							+" <p id=\"" + ct.getName() + 1 + "Chart_div\"></p>"
							+" <p id=\"" + ct.getName() + 2 + "Chart_div\"></p>"
							+ HTMLReportUtils.generateDescriptionButton(ct.getName()+1, SUMMARY_NOTES, "Summary Notes")
							+ HTMLReportUtils.generateDescriptionButton(ct.getName()+2, SUMMARY_DESCRIPTION, "Column Description")							
							+ "</div>";
 					ct.setRenderingInfo( str );
										
					// add summary report to the front
					List<ChartTab> chartTabs = report.getTabs();
					chartTabs.add(0, ct);				
					report.setTabs(chartTabs);
					//*****end xu		
				} else {
					System.out.println("summaryNodes was null");
				}
			}
		}
	}
	
	private static void createMatrix(Element reportElement, Report report) {
		
		final ChartTab matrixParent = new ChartTab("MAPQ Matricies");
		
		//Matricies
		final NodeList matrixCMNL = reportElement.getElementsByTagName("MAPQMatrixCM");
		final Element matrixCMElement = (Element) matrixCMNL.item(0);
		ChartTab ct = createMatrixChartTab(matrixCMElement, "CM Matrix", "cm");
		if (null != ct)
			matrixParent.addChild(ct);
		
		NodeList matrixSMNL = reportElement.getElementsByTagName("MAPQMatrixSM");
		Element matrixSMElement = (Element) matrixSMNL.item(0);
		ct = createMatrixChartTab(matrixSMElement, "SM Matrix", "sm");
		if (null != ct)
			matrixParent.addChild(ct);
		
		NodeList matrixLengthNL = reportElement.getElementsByTagName("MAPQMatrixLength");
		Element matrixLengthElement = (Element) matrixLengthNL.item(0);
		ct = createMatrixChartTab(matrixLengthElement, "Length Matix", "len");
		if (null != ct)
			matrixParent.addChild(ct);
		
		if ( ! matrixParent.getChildren().isEmpty())
			report.addTab(matrixParent);
	}

	private static void createMAPQ(Element reportElement, Report report) {
		final NodeList mapQNL = reportElement.getElementsByTagName("MAPQ");
		final Element mapQElement = (Element) mapQNL.item(0);
		report.addTab(generateTallyChartTab(mapQElement, "MAPQ", "mq", HTMLReportUtils.SCATTER_CHART,  false));
	}

	private static void createCoverage(Element reportElement, Report report) {
		//Coverage
		final NodeList coverageNL = reportElement.getElementsByTagName("Coverage");
		final Element coverageElement = (Element) coverageNL.item(0);
		final ChartTab coverageCT = createCoverageChartTab(coverageElement);
		if (null != coverageCT)
			report.addTab(coverageCT);
	}

	private static void createFLAGS(Element reportElement, Report report) {
		//FLAG
		final NodeList flagNL = reportElement.getElementsByTagName( FLAG );
		final Element flagElement = (Element) flagNL.item(0);
		final ChartTab flagTab = new ChartTab( FLAG );
		Map<String, AtomicLong> flags = getMapFromElement(flagElement);
		
		Map<String, AtomicLong> flagsKeyChange = new LinkedHashMap<String, AtomicLong>();
		for (Entry<String, AtomicLong> entry : flags.entrySet()) {
			String[] flagStirngArray = entry.getKey().split(", ");
			flagsKeyChange.put((flagStirngArray.length > 1 ? flagStirngArray[1] + ", ": "")  + flagStirngArray[0], entry.getValue());
		}
		
		flagTab.addChild(getChartTabFromMap( "Flag", "fl", HTMLReportUtils.BAR_CHART, true, flagsKeyChange) );
		
		// duplicates
		final Map<String, String> dupMap = new HashMap<String, String>();
		dupMap.put("d", "Duplicates");
		final Map<String, AtomicLong> duplicateFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, dupMap, "Singletons");
		flagTab.addChild(getChartTabFromMap(DUPLICATE, "fld", HTMLReportUtils.PIE_CHART, true, duplicateFlags));
		// vendor check
		final Map<String, String> vendorMap = new HashMap<String, String>();
		vendorMap.put("f", "Failed Vendor Check");
		final Map<String, AtomicLong> failedFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, vendorMap, "Passed Vendor Check");
		flagTab.addChild(getChartTabFromMap("Vendor Check", "flf", HTMLReportUtils.PIE_CHART, true, failedFlags));
		// first and second
		final Map<String, String> firstSecondMap = new HashMap<String, String>();
		firstSecondMap.put("2", "Second");
		firstSecondMap.put("1", "First");
		final Map<String, AtomicLong> firstSecondFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, firstSecondMap, null);
		flagTab.addChild(getChartTabFromMap("Read in Pair", "flfs", HTMLReportUtils.PIE_CHART, true, firstSecondFlags));
		
		final Map<String, String> mappedMap = new HashMap<String, String>();
		mappedMap.put("u", UNMAPPED);
		final Map<String, AtomicLong> mappedFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, mappedMap, "Mapped");
		flagTab.addChild(getChartTabFromMap(UNMAPPED, "flm", HTMLReportUtils.PIE_CHART, true, mappedFlags));
		
		final Map<String, String> primaryMap = new HashMap<String, String>();
		primaryMap.put("s", "secondary");
		primaryMap.put("S", "Supplementary");
		final Map<String, AtomicLong> primaryFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, primaryMap, "Primary");
		flagTab.addChild(getChartTabFromMap("Primary", "fls", HTMLReportUtils.PIE_CHART, true, primaryFlags));

		report.addTab(flagTab);
	}

	private static void createCIGAR(Element reportElement, Report report) {
		//CIGAR
		final NodeList cigarNL = reportElement.getElementsByTagName("CIGAR");
		final Element cigarElement = (Element) cigarNL.item(0);
		final ChartTab cigarCT = createCigarChartTab(cigarElement);
		report.addTab(cigarCT);
	}

	private static void createRNEXT(Element reportElement, Report report) {
		//MRNM
		final NodeList rnextNL = reportElement.getElementsByTagName("RNEXT");
		final Element rnextElement = (Element) rnextNL.item(0);
		if (null != rnextElement) {
			report.addTab(generateTallyChartTab(rnextElement, "RNEXT", "m", HTMLReportUtils.BAR_CHART,  true));
		}
	}

	private static void createISIZE(Element reportElement, Report report) throws QVisualiseException {
		//ISIZE
		final NodeList iSizeNL = reportElement.getElementsByTagName("ISIZE");
		final Element iSizeElement = (Element) iSizeNL.item(0);
		final ChartTab iSizeCT = createISizeChartTab(iSizeElement);
		if (null != iSizeCT)
			report.addTab(iSizeCT);
	}
	
	//coverge for each chromosome and readGroup
	private static void createRNM(Element reportElement, Report report) {
		final NodeList rnmNL = reportElement.getElementsByTagName("RNAME_POS");
		final Element rnmElement = (Element) rnmNL.item(0);
		final NodeList nlTop = rnmElement.getElementsByTagName("RNAME");
		
		/*
		 * if we don't have the data in the xml, don't try and create a chart
		 */
		if (nlTop.getLength()<= 0) {
			return;
		}
		
		//get data from xml
		Map<String, TreeMap<Integer, AtomicLong>> contigMaps = new HashMap<>();
		Map<String, TreeMap<Integer, AtomicLongArray>> rgCountsMaps = new HashMap<>();
		
		List<String> chromos = new ArrayList<>();
		List<String> readGroups = null; 
		
		int cellingValue = 0;
		for (int i = 0 , length = nlTop.getLength() ; i < length ; i++) {			
			final Element nameElementTop = (Element) nlTop.item(i);
			String chromosome =  nameElementTop.getAttribute("value");
			int contigLength = Integer.parseInt(nameElementTop.getAttribute("maxPosition"));
			
			if(readGroups == null){
				readGroups = new LinkedList ( Arrays.asList(nameElementTop.getAttribute("readGroups").split(",")) );
				readGroups.remove("unkown_readgroup_id");
				readGroups.add(0, "Total");
			}
			
			//viral have 6000 contig's lots of them big then 1M, it cause html can't show well on browers
			//chrMT is small but special 
			if (null != chromosome && (contigLength > 50 * 1000 * 1000 || chromosome.toUpperCase().startsWith("CHR")) ){
				chromos.add(chromosome);
		
				final NodeList nl = nameElementTop.getElementsByTagName("RangeTally");
				final Element nameElement = (Element) nl.item(0);
				final TreeMap<Integer, AtomicLong> map = (TreeMap<Integer, AtomicLong>) createRangeTallyMap(nameElement);				
				if ( ! map.isEmpty())  contigMaps.put(chromosome, map);
				
				final TreeMap<Integer, AtomicLongArray> map1 = ( TreeMap<Integer, AtomicLongArray> ) createRgCountMap(nameElement);
				rgCountsMaps.put(chromosome, map1);	
				if (cellingValue == 0) {
					cellingValue = Integer.parseInt(nameElementTop.getAttribute("visuallCellingValue"));
				}
			}
		}	
				
		//tab 1
		StringBuilder dataSB = new StringBuilder();
		StringBuilder chartSB = new StringBuilder();	
		String tabName = "rnmref";
		for (Entry<String, TreeMap<Integer, AtomicLong>> map : contigMaps.entrySet()) {
			String keyWithOutPeriods = map.getKey().replace(".","");			
			dataSB.append(HTMLReportUtils.generateGoogleData(map.getValue(), tabName + keyWithOutPeriods, false));
			chartSB.append(HTMLReportUtils.generateGoogleScatterChart(tabName + keyWithOutPeriods, keyWithOutPeriods, 600, MIN_REPORT_HEIGHT, true));
		}

		ChartTab child1 = new ChartTab(  "Coverage overall" , tabName);				
		child1.setData(dataSB.toString());
		child1.setChartInfo(chartSB.toString());
		Collections.sort(chromos, new ReferenceNameComparator());
		child1.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(tabName , chromos, 2));	
				
		//tab 2  
		dataSB = new StringBuilder();
		chartSB = new StringBuilder();
		tabName = "rnmrg";
		
		readGroups.remove(0);	
		
		for (Entry<String, TreeMap<Integer, AtomicLongArray>> map : rgCountsMaps.entrySet()) {
			String keyWithOutPeriods = map.getKey().replace(".","");	
			TreeMap<Integer, AtomicLongArray> notTotalMap = new TreeMap<Integer, AtomicLongArray>();
			for(Entry<Integer, AtomicLongArray> entry: map.getValue().entrySet()){
				AtomicLongArray rgArray = new AtomicLongArray(readGroups.size());
				for(int j = 1; j <= readGroups.size(); j ++) {
					rgArray.addAndGet(j-1, entry.getValue().get(j));
				}
				notTotalMap.putIfAbsent(entry.getKey(), rgArray);
			}	
			
			dataSB.append( HTMLReportUtils.generateGoogleaArrayToDataTable(notTotalMap,  tabName + keyWithOutPeriods, false, readGroups, false ) );			
			int max = cellingValue; 
			String extraOption = String.format(", vAxis: { viewWindowMode:'explicit', viewWindow:{  max: %d }}, fontSize:12, legend: {position: 'right', textStyle: {color: 'blue'}}, crosshair: {trigger: 'both'}, lineWidth: 2", max);
			chartSB.append(HTMLReportUtils.generateGoogleChart(tabName + keyWithOutPeriods, keyWithOutPeriods, "$(window).width()", MIN_REPORT_HEIGHT/4, false, HTMLReportUtils.LINE_CHART, 
					null, extraOption ) );
			
		}
		
		ChartTab child2 = new ChartTab(  "Coverage by readGroup" , tabName);				
		child2.setData(dataSB.toString());
		child2.setChartInfo(chartSB.toString());
		child2.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(tabName, chromos, 1));	
								
		ChartTab parentCT = new ChartTab("RNAME");		
		parentCT.addChild(child1);
		parentCT.addChild(child2);	 
		report.addTab(parentCT);			 
	}


	private static void createTAGS(Element reportElement, Report report) {
		// TAG
		final NodeList tagNL = reportElement.getElementsByTagName("TAG");
		final Element tagElement = (Element) tagNL.item(0);
		
		//TAG-CS
		final NodeList tagCSNL = tagElement.getElementsByTagName("CS");
		if (tagCSNL.getLength() > 1) {
			final Element tagCSElement = (Element) tagCSNL.item(0);
			for (ChartTab ct : buildMultiTabCycles(true,"TAG CS", tagCSElement, "tcs", "ColourByCycle", "BadColoursInReads", CycleDetailUtils.getTagCSNumericCycle(), CS_COLOURS, false)) {
				report.addTab(ct);
			}
		}
		
		//TAG-CQ
		final NodeList tagCQNL = tagElement.getElementsByTagName("CQ");
		if (tagCQNL.getLength() > 1) {
			final Element tagCQElement = (Element) tagCQNL.item(0);
			for (ChartTab ct : buildMultiTabCycles(true,"TAG CQ", tagCQElement, "tcq",
					"QualityByCycle", "BadQualsInReads", null, null, true)) {
				report.addTab(ct);
			}
		}
		
		//TAG-RG
		final NodeList tagRGNL = tagElement.getElementsByTagName("RG");
		final Element tagRGElement = (Element) tagRGNL.item(0);
		if (tagRGElement.hasChildNodes())
			report.addTab(generateTallyChartTab(tagRGElement, "TAG RG", "trg", HTMLReportUtils.BAR_CHART,  true));
		
		//TAG-ZM
		final NodeList tagZMNL = tagElement.getElementsByTagName("ZM");
		final Element tagZMElement = (Element) tagZMNL.item(0);
		if (tagZMElement.hasChildNodes())
			report.addTab(generateTallyChartTab(tagZMElement, "TAG ZM", "tzm", HTMLReportUtils.SCATTER_CHART,  false));
		
		//TAG-MD
		ChartTab mdCT = createMDChartTab(tagElement);
		if (null != mdCT) report.addTab(mdCT);  
	}

	private static void createQUALS(Element reportElement, Report report) {
		// QUALS
		final NodeList qualNL = reportElement.getElementsByTagName("QUAL");
		final Element qualElement = (Element) qualNL.item(0);
		for (ChartTab ct : buildMultiTabCycles(true,"QUAL", qualElement, "q", "QualityByCycle", "BadQualsInReads", null, null, true)) {
			report.addTab(ct);
		}
	}

	public static void createSEQ(Element reportElement, Report report) {
		//SEQ
		final NodeList seqNL = reportElement.getElementsByTagName("SEQ");
		final Element seqElement = (Element) seqNL.item(0);
		
		//SEQ show mainMainTab is true, so only one element on the list<ChartTab>
		List<ChartTab> tabs = buildMultiTabCycles(true,"SEQ", seqElement, "s", "BaseByCycle", "BadBasesInReads", 
				CycleDetailUtils.getSeqCycle(), SEQ_COLOURS, false);
		 	
		//add kmers tab
		ChartTab parentCT = tabs.get(0);
		/*
		 * check to see if we have kmer data before heading down this path
		 */
		NodeList nl = seqElement.getElementsByTagName("mers1");
		if (null != nl && nl.getLength() > 0) {
			parentCT.addChild( createKmersTab((Element) nl.item(0), "kmer_1" ) );
		}
		nl = seqElement.getElementsByTagName("mers2");
		if (null != nl && nl.getLength() > 0) {
			parentCT.addChild( createKmersTab((Element) nl.item(0), "kmer_2" ) );
		}
		nl = seqElement.getElementsByTagName("mers3");
		if (null != nl && nl.getLength() > 0) {
			parentCT.addChild( createKmersTab((Element) nl.item(0), "kmer_3" ) );
		}
		nl = seqElement.getElementsByTagName("mers6");
		if (null != nl && nl.getLength() > 0) {
			parentCT.addChild( createKmersTab((Element) nl.item(0), "kmer_6" ) );
		}
		report.addTab(parentCT);
		
		
	}
	
	private static ChartTab createKmersTab(Element mersElement,  String tabTitle ){
 		 		 
		Map<Integer, AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();	
		NodeList nl = mersElement.getElementsByTagName("CycleTally");
		Element tallyElement = (Element) nl.item(0);
		
		List<String> kmers = Arrays.asList(  tallyElement.getAttribute("possibleValues").split(","));
	 	
		nl = tallyElement.getElementsByTagName("Cycle");		 
		for (int i = 0, size = nl.getLength() ; i < size ; i++) {
			Element e = (Element) nl.item(i);	
			String[] sValues = e.getAttribute("counts").split(",");
			long[] nValues = new long[sValues.length];
			for(int j = 0; j < sValues.length; j ++)
				nValues[j] = Long.parseLong(sValues[j]);
			map.put(Integer.parseInt(e.getAttribute("value")), new AtomicLongArray( nValues));
				
		}
				
		String dataSB = HTMLReportUtils.generateGoogleaArrayToDataTable(map,  tabTitle, false, kmers, false);
		String chartSB = HTMLReportUtils.generateGoogleChart(tabTitle, "Kmers Distribution", "$(window).width()", MAX_REPORT_HEIGHT, false, HTMLReportUtils.LINE_CHART, "Cycle", 
				", vAxis: { viewWindowMode:'explicit' }, fontSize:12, legend: { position: 'right', textStyle: { color: 'blue' } }, crosshair: { trigger: 'both' },  lineWidth: 2");

		ChartTab ct = new ChartTab(tabTitle, tabTitle);
		ct.setData( dataSB);
		ct.setChartInfo( chartSB);
				
		return ct;		
	}
	
	private static void createBamHeader(Element reportElement, Report report) {
		final NodeList headerNL = reportElement.getElementsByTagName("HEADER");
		if (null != headerNL) {
			final Element headerElement = (Element) headerNL.item(0);
			if (null != headerElement) {
				Map<String, List<String>> headerList = QProfilerCollectionsUtils.convertHeaderTextToMap(headerElement.getTextContent());
				if ( ! headerList.isEmpty()) {
					ChartTab ct = new ChartTab("BAMHeader", "head" + reportID);
					ct.setData(HTMLReportUtils.generateGoogleDataForTable(headerList, ct.getName()));
					String str = "";
					for(int i = 1; i<= headerList.size(); i ++)
						str += HTMLReportUtils.generateGoogleSingleTable(ct.getName() + i,null, null);
					ct.setChartInfo(str);
					ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getName(), headerList.size(),false));
					report.addTab(ct);
				}
			}
		}
	}


	private static ChartTab createMDChartTab(Element tagElement) {
		ChartTab parentCT = null;
		ChartTab mismatchCT = null;
		ChartTab onePercentCT = null;
		ChartTab forwardCT = null;
		ChartTab reverseCT = null;
		
		//TAG-MD
		final NodeList tagMDNL = tagElement.getElementsByTagName("MD");
		final Element tagMDElement = (Element) tagMDNL.item(0);
		// don't always have tag MD info
		if (null != tagMDElement && tagMDElement.hasChildNodes()) {
			// get data
			final SummaryByCycle<Integer> cycle = QProfilerCollectionsUtils.generateSummaryByCycleFromElement(tagMDElement, "MismatchByCycle");
			final Map<Integer, String> cyclePercentages = QProfilerCollectionsUtils.generatePercentagesMapFromElement(tagMDElement, "MismatchByCycle");
			
			// create cycle tab
			mismatchCT = new ChartTab(MD, "tmd");
			mismatchCT.setData(HTMLReportUtils.generateGoogleDataCycles(cycle, mismatchCT
					.getName(), CycleDetailUtils.getSeqCycle(), false, cyclePercentages));
			mismatchCT.setChartInfo(HTMLReportUtils.generateGoogleChartSlidingColors(
					mismatchCT.getName(), "TAG MD Cycles", 1400, MAX_REPORT_HEIGHT,
					HTMLReportUtils.BAR_CHART, false, true, cycle.getPossibleValues().size(),
					SEQ_COLOURS));
			mismatchCT.setDescription(TAG_MD_DESCRIPTION);
			
			
			Map<Integer, Double> sortedPercentageMap = new TreeMap<>();
			
			for (Entry<Integer, String> entry : cyclePercentages.entrySet()) {
				String value = entry.getValue();
				double percentage = Double.parseDouble(value.substring(0, value.length() - 1));
				if (percentage > 1.0) {
					sortedPercentageMap.put(entry.getKey(), percentage);
				}
			}
			
			onePercentCT = new ChartTab(MD + " 1 PERC", "tmd1pc");
			onePercentCT.setData(HTMLReportUtils.generateGoogleData(sortedPercentageMap, onePercentCT.getName(),false, "Error Percentage", "Cycle"));
			onePercentCT.setChartInfo(HTMLReportUtils.generateGoogleSingleTable(onePercentCT.getName(), 0 , 400));
		}
		
		//TAG-MD_forward
		final NodeList tagMDForward = tagElement.getElementsByTagName("MD_mutation_forward");
		final Element tagMDFElement = (Element) tagMDForward.item(0);
		if (null != tagMDFElement && tagMDFElement.hasChildNodes())
			forwardCT = generateTallyChartTab(tagMDFElement, "Mutation Forward Strand", "mdf", HTMLReportUtils.BAR_CHART,  true, true);
		//TAG-MD_reverse
		final NodeList tagMDReverse = tagElement.getElementsByTagName("MD_mutation_reverse");
		final Element tagMDRElement = (Element) tagMDReverse.item(0);
		if (null != tagMDRElement && tagMDRElement.hasChildNodes())
			reverseCT = generateTallyChartTab(tagMDRElement, "Mutation Reverse Strand", "mdr", HTMLReportUtils.BAR_CHART,  true, true);
		
		
		// deal with all charts being null, or just the forward and reverse charts being null, in which case no need for parent ct
		
		if (null != mismatchCT && null != forwardCT && null != reverseCT) {
			parentCT = new ChartTab("TAG MD");
			mismatchCT.setTitle("MD mismatch");
			onePercentCT.setTitle("Cycles with 1% error or more");
			parentCT.addChild(mismatchCT);
			parentCT.addChild(onePercentCT);
			parentCT.addChild(forwardCT);
			parentCT.addChild(reverseCT);
		} else if (null != mismatchCT) {
			parentCT = mismatchCT;
		}
		return parentCT;
	}
		
	private static ChartTab createISizeChartTab(Element element) throws QVisualiseException {
		ChartTab parentCT = null;
		String id = "is" + reportID;
		int idCounter = 1;
		//get read group ids
		NodeList childNodes = element.getChildNodes();
		List<String> readGroups = new ArrayList<>();
		for (int k = 0 ; k < childNodes.getLength() ; k++) {
			Node n = childNodes.item(k);
			if (n.getNodeName().equals("ReadGroup")) {
				String rg = n.getAttributes().getNamedItem("id").getNodeValue();
				if(!rg.equals("overall")) readGroups.add(rg);
			}
		}
		int noOfReadGroups = readGroups.size();
		
		if (noOfReadGroups == 0) {
			throw new QVisualiseException("ISIZE_ERROR");
		}
		
		TreeMap<Integer, AtomicLongArray> arrayMap = new TreeMap<>();
		
		int counter = 0;
		for (String rg : readGroups) {
			final NodeList nl = element.getChildNodes();
			for (int k = 0 ; k < nl.getLength() ; k++) {
				Node n = childNodes.item(k);
				if (n.getNodeName().equals("ReadGroup") && n.getAttributes().getNamedItem("id").getNodeValue().equals(rg)) {					
					final Element nameElement = (Element) nl.item(k);
					final TreeMap<Integer, AtomicLong> map = (TreeMap<Integer, AtomicLong>) createRangeTallyMap(nameElement);
			
					// add map data to arrayMap
					for (Entry<Integer, AtomicLong> entry : map.entrySet()) {
						// get entry from arrayMap
						AtomicLongArray ala = arrayMap.get(entry.getKey());
						if (ala == null) {
							ala = new AtomicLongArray(noOfReadGroups);
							arrayMap.put(entry.getKey(), ala);
						}
						ala.addAndGet(counter, entry.getValue().get());
					}
				}
			}
			
			counter++;
		}
		
		if ( ! arrayMap.isEmpty()) {
		
			// decide if we need to create 2 tabs
			if (arrayMap.lastKey() > 1500) {
				// split into 3 tabs
				// create parent
				parentCT = new ChartTab(ISIZE);
				
				// add in the summary tab
				String tabTitle = "0 to 1500" + (noOfReadGroups == 1 ? "" : " - Summary");
				String longTitle = ISIZE + ", 0 to 1500, summed across all read groups";
				final ChartTab ct1Sum = new ChartTab(tabTitle, (id + idCounter++));
				
				TreeMap<Integer, AtomicLong> summaryMap = new TreeMap<>();
				for (Entry<Integer, AtomicLongArray> entry : arrayMap.entrySet()) {
					summaryMap.put(entry.getKey(), QProfilerCollectionsUtils.tallyArrayValues(entry.getValue()));
				}
				ct1Sum.setData(HTMLReportUtils.generateGoogleData( 	summaryMap.subMap(0, true, 1500, false), ct1Sum.getName(), false));
				ct1Sum.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct1Sum.getName(), longTitle, 1400, MAX_REPORT_HEIGHT, true));				
				parentCT.addChild(ct1Sum);
				
				if (noOfReadGroups > 1) {
				
					// first tab shows 0 - 1500
					longTitle = ISIZE + ", 0 to 1500, split by read group";
					final ChartTab ct1All = new ChartTab("0 to 1500 - All", (id + idCounter++));
					
					ct1All.setData( HTMLReportUtils.generateGoogleaArrayToDataTable((arrayMap).subMap(0, true, 1500, false), ct1All.getName(), false, readGroups, true ));
					ct1All.setChartInfo(HTMLReportUtils.generateGoogleChart(ct1All.getName(), longTitle, 1400+"", MAX_REPORT_HEIGHT, true, HTMLReportUtils.SCATTER_CHART, 
							null, ", legend: {position: 'right', textStyle: {color: 'blue', fontSize: 14}}, crosshair: {trigger: 'both'}, pointSize: 2, lineWidth: 1"));
					
					parentCT.addChild(ct1All);
				}
				
				// next tab shows 0 - 5000
				tabTitle = "0 to 5000" + (noOfReadGroups == 1 ? "" : " - Summary");
				longTitle = ISIZE + ", 0 to 5000, summed across all read groups";
				final ChartTab ct2sum = new ChartTab(tabTitle, (id + idCounter++));
				ct2sum.setData(HTMLReportUtils.generateGoogleData(
						summaryMap.subMap(0, true, 5000, false),
						ct2sum.getName(), false));
				ct2sum.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct2sum.getName(),
						longTitle, 1400, MAX_REPORT_HEIGHT, true));
				
				parentCT.addChild(ct2sum);
				
				if (noOfReadGroups > 1) {	
					
					// tab shows 0 - 5000
					longTitle = ISIZE + ", 0 to 5000, split by read group";
					final ChartTab ct2All = new ChartTab("0 to 5000 - All", (id + idCounter++));
					ct2All.setData(HTMLReportUtils.generateGoogleaArrayToDataTable((arrayMap).subMap(0, true, 5000, false), ct2All.getName(), false, readGroups, true) );					
					ct2All.setChartInfo(HTMLReportUtils.generateGoogleChart(ct2All.getName(),longTitle, 1400+"", MAX_REPORT_HEIGHT, true,  HTMLReportUtils.SCATTER_CHART,
							null, ", legend: {position: 'right', textStyle: {color: 'blue', fontSize: 14}}, crosshair: {trigger: 'both'}, pointSize: 2, lineWidth: 1"));
					
					parentCT.addChild(ct2All);
				}
			} else {
				// no need for sub tabs
				parentCT = new ChartTab(ISIZE, id);
				parentCT.setData(HTMLReportUtils.generateGoogleaArrayToDataTable(arrayMap, parentCT.getName(), false, readGroups, false) );
				parentCT.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(parentCT.getName(),
						ISIZE, 1200, 940, true));
			}
		}
		return parentCT;
	}
	
	private static ChartTab createCigarChartTab(Element element) {
		final String title = "CIGAR";
		final String id = "cig" + reportID;
		final ChartTab parentCT = new ChartTab(title);
		final int height = 800;
		
		final Map<String, AtomicLong> cigars = getMapFromElement(element);
		
		final Map<String, AtomicLong> cigarsD = new TreeMap<String, AtomicLong>(new CigarStringComparator());
		final Map<String, AtomicLong> cigarsI = new TreeMap<String, AtomicLong>(new CigarStringComparator());
		final Map<String, AtomicLong> cigarsH = new TreeMap<String, AtomicLong>(new CigarStringComparator());
		final Map<String, AtomicLong> cigarsS = new TreeMap<String, AtomicLong>(new CigarStringComparator());
		
		for (String key : cigars.keySet()) {
			if (key.endsWith("D"))
				cigarsD.put(key, cigars.get(key));
			else if (key.endsWith("I"))
				cigarsI.put(key, cigars.get(key));
			else if (key.endsWith("H"))
				cigarsH.put(key, cigars.get(key));
			else if (key.endsWith("S"))
				cigarsS.put(key, cigars.get(key));
			
		}
		
		if ( ! cigarsD.isEmpty()) {
			final ChartTab ctd = new ChartTab("Deletions", id+"d");
			ctd.setData(HTMLReportUtils.generateGoogleData(
					cigarsD,
					ctd.getName(), true));
			int width = cigarsD.size() > 100 ? 1600 : 1000;
			ctd.setChartInfo(HTMLReportUtils.generateGoogleChart(ctd.getName(),
					title + ", Deletions", width, height, HTMLReportUtils.COLUMN_CHART, true, false));
			
			parentCT.addChild(ctd);
		}
		
		if ( ! cigarsI.isEmpty()) {
			final ChartTab cti = new ChartTab("Insertions", id+"i");
			cti.setData(HTMLReportUtils.generateGoogleData(
					cigarsI,
					cti.getName(), true));
			int width = cigarsI.size() > 100 ? 1600 : 1000;
			cti.setChartInfo(HTMLReportUtils.generateGoogleChart(cti.getName(),
					title + ", Insertions", width, height, HTMLReportUtils.COLUMN_CHART, true, false));
			
			parentCT.addChild(cti);
		}
		
		if ( ! cigarsH.isEmpty()) {
			final ChartTab cth = new ChartTab("Hard clips", id+"h");
			cth.setData(HTMLReportUtils.generateGoogleData(
					cigarsH,
					cth.getName(), true));
			int width = cigarsH.size() > 100 ? 1600 : 1000;
			cth.setChartInfo(HTMLReportUtils.generateGoogleChart(cth.getName(),
					title + ", Hard clips", width, height, HTMLReportUtils.COLUMN_CHART, true, false));
			
			parentCT.addChild(cth);
		}
		
		if ( ! cigarsS.isEmpty()) {
			final ChartTab cts = new ChartTab("Soft clips", id+"s");
			cts.setData(HTMLReportUtils.generateGoogleData(
					cigarsS,
					cts.getName(), true));
			int width = cigarsS.size() > 100 ? 1600 : 1000;
			cts.setChartInfo(HTMLReportUtils.generateGoogleChart(cts.getName(),
					title + ", Soft clips", width, height, HTMLReportUtils.COLUMN_CHART, true, false));
			
			parentCT.addChild(cts);
		}
		
		return parentCT;
	}
	
	private static ChartTab createCoverageChartTab(Element element) {
		final String title = "Coverage";
		final String id = "cov" + reportID;
		
		ChartTab parentCT = null;
		
		int width = MAX_REPORT_WIDTH + 200;
		
		final Map<Integer, AtomicLong> tallys = generateLengthsTally(element, "ValueTally");
		if ( ! tallys.isEmpty()) {
			
			parentCT = new ChartTab(title);
			
			TreeMap<Integer, AtomicLong> tallysTM = new TreeMap<Integer, AtomicLong>(tallys);
			final int maxCoverageValue = tallysTM.lastKey().intValue();
			
			// want 2 tabs - standard and cumulative
			
			// standard
			// 0 - 100 no binning
			final ChartTab ct = new ChartTab("0-100", id+"s");
			ct.setData(HTMLReportUtils.generateGoogleData(tallysTM.headMap(100, true), ct.getName(), true));
			ct.setChartInfo(HTMLReportUtils.generateGoogleChart(ct.getName(),
					title + " - Standard, 0-100", width, MAX_REPORT_HEIGHT, HTMLReportUtils.COLUMN_CHART, true, false));
			
			parentCT.addChild(ct);
			
			// do 0-1000, 0-10000, etc..
			int multiplier = 100;
			while (true) {
			
				if (multiplier < maxCoverageValue) {
					// set multplier to the next level
					multiplier *= 10;
					
					final Map<String, AtomicLong> binMap = QProfilerCollectionsUtils.convertMapIntoBinnedMap(
							tallysTM.headMap(multiplier, true) , multiplier / 100, true);
					
					final ChartTab ctBin10 = new ChartTab("0-" + multiplier + " (binned by " + multiplier/100 + ")", id+"s"+multiplier/100);
					
					ctBin10.setData(HTMLReportUtils.generateGoogleData(binMap, ctBin10.getName(), true));
					ctBin10.setChartInfo(HTMLReportUtils.generateGoogleChart(ctBin10.getName(),
							title + " - Standard, 0-" + multiplier + " (binned by " + multiplier/100 + ")", width, MAX_REPORT_HEIGHT, HTMLReportUtils.COLUMN_CHART, true, false));
					
					parentCT.addChild(ctBin10);
				} else {
					break;
				}
			}
			
			// cumulative
			Map<Integer, AtomicLong> cumulativeTallys = new TreeMap<Integer, AtomicLong>();
			long count = 0;
			int i = 0;
			for (Entry<Integer, AtomicLong> entry : tallys.entrySet()) {
				count += entry.getValue().get();
				if (i++ % 10 == 0)
					cumulativeTallys.put(entry.getKey(), new AtomicLong(count));
			}
			
			ChartTab ctCumulative = new ChartTab("Cumulative", id+"c");
			ctCumulative.setData(HTMLReportUtils.generateGoogleData(cumulativeTallys, ctCumulative.getName(), false));
			ctCumulative.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ctCumulative.getName(),
					title + " - Cumulative", width, MAX_REPORT_HEIGHT, false));
			
			parentCT.addChild(ctCumulative);
		}
		return parentCT;
	}
	
	private static ChartTab createMatrixChartTab(Element element, String title, String idSuffix) {
		String id = "mx" + reportID;
		Map<MAPQMiniMatrix, AtomicLong> tallys = generateMatrixCollection(element, "ValueTally");
		
		
		ChartTab ct = null; 
		if ( ! tallys.isEmpty()) {
			ct = new ChartTab(title, id+idSuffix);
			ct.setData(HTMLReportUtils.generateGoogleMatrixData(tallys, ct.getName(), true));
			ct.setChartInfo(HTMLReportUtils.generateGoogleBioHeatMap(ct.getName(),
					title + " BioHeatMap", tallys.size() > 1000 ? 12 : 18));
		}
		return ct;
	}
	
	private static Map<Integer, AtomicLong> createRangeTallyMap(Element element) {
		Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();

		final NodeList nl = element.getElementsByTagName("RangeTallyItem");
		for (int i = 0, size = nl.getLength() ; i < size ; i++) {
			Element e = (Element) nl.item(i);
			
			int start = Integer.parseInt(e.getAttribute("start"));
			int end = Integer.parseInt(e.getAttribute("end"));
			Integer key = Math.round((float)(start + end) / 2);
			
			map.put(key, new AtomicLong(Integer.parseInt(e.getAttribute("count"))));
		}
		
		return map;
	}
		
	private static Map< Integer, AtomicLongArray > createRgCountMap(Element element) {	
//	private static Map<Integer, int[]> createRgCountMap(Element element) {
		//Map<Integer, int[]> map = new TreeMap<Integer, int[]>();
		Map<Integer,AtomicLongArray> map = new TreeMap<Integer, AtomicLongArray>();
		final NodeList nl = element.getElementsByTagName("RangeTallyItem");
		for (int i = 0, size = nl.getLength() ; i < size ; i++) {
			Element e = (Element) nl.item(i);
			
			int start = Integer.parseInt(e.getAttribute("start"));
			int end = Integer.parseInt(e.getAttribute("end"));
			Integer key = Math.round((float)(start + end) / 2);
			
			String[] sValues = e.getAttribute("rgCount").split(",");
			long[] nValues = new long[sValues.length+1];
			for(int j = 0; j < sValues.length-1; j ++)
				nValues[j+1] = Long.parseLong(sValues[j].replace(" ", ""));
			nValues[0] = Long.parseLong(e.getAttribute("count")); 			//total coverage
				
			map.put(key, new AtomicLongArray( nValues));
		}
		
		return map;
	}
	
	private static <T> ChartTab generateTallyChartTab(Element element, String name, 
			String id, String chartType, boolean isValueString) {
		return generateTallyChartTab(element, name, id, chartType, isValueString, false);
	}
	
	private static <T> ChartTab generateTallyChartTab(Element element, String name, 
			String id, String chartType, boolean isValueString, boolean turnOffLogScale) {
		Map<T, AtomicLong> cycleCount = getMapFromElement(element);
		return getChartTabFromMap(name, id, chartType, isValueString, cycleCount, turnOffLogScale);
	}
	
	private static <T> ChartTab getChartTabFromMap(String name, String id,
			String chartType, boolean isValueString,
			Map<T, AtomicLong> cycleCount) {
		return getChartTabFromMap(name, id, chartType, isValueString, cycleCount, false);
	}

	private static <T> ChartTab getChartTabFromMap(String name, String id,
			String chartType, boolean isValueString,
			Map<T, AtomicLong> cycleCount, boolean turnOffLogScale) {
		
		ChartTab ct = new ChartTab(name, id + reportID);
		int width = MIN_REPORT_WIDTH;
		int height = MIN_REPORT_HEIGHT;
		// set width and height depending on dataset size
		if (cycleCount.size() > 25) {
			width = MAX_REPORT_WIDTH;
			height = MAX_REPORT_HEIGHT;
			if (HTMLReportUtils.BAR_CHART.equals(chartType)) {
				width = MAX_REPORT_WIDTH - 100;
				height = MAX_REPORT_HEIGHT + 300;
			}
		}		
		
		ct.setData(HTMLReportUtils.generateGoogleData(cycleCount,
				ct.getName(), isValueString));
		
		if (HTMLReportUtils.SCATTER_CHART.equals(chartType)) {
			
			ct.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(
				ct.getName(), name + " Tally", width, height,  ! turnOffLogScale));
			
		} else {
			
			ct.setChartInfo(HTMLReportUtils.generateGoogleChart(
					ct.getName(), name + " Tally", width,
					height, chartType, ! turnOffLogScale, false));
		}
		
		return ct;
	}

	private static <T> Map<T, AtomicLong> getMapFromElement(Element element) {
		final Map<T, AtomicLong> cycleCount = new LinkedHashMap<T, AtomicLong>();
		
		// get ValueTally
		final NodeList nl = element.getElementsByTagName("ValueTally");
		final Element nameElement = (Element) nl.item(0);
		
		QProfilerCollectionsUtils.populateTallyItemMap(nameElement, cycleCount, false);
		return cycleCount;
	}

	private static Map<Integer, AtomicLong> generateLengthsTally(Element tabElement,
			String name) {
		
		final Map<Integer, AtomicLong> lengths = new LinkedHashMap<Integer, AtomicLong>();
		if (null != tabElement) {
			NodeList nl = tabElement.getElementsByTagName(name);
			//FIXME hack to get around the "NEW" tag added to lengths
			if (nl.getLength() == 0) {
				nl = tabElement.getElementsByTagName(name + "NEW");
			}
			Element nameElement = (Element) nl.item(0);
	
			QProfilerCollectionsUtils.populateTallyItemMap(nameElement, lengths, true);
		}
		return lengths;
	}
	
	private static Map<MAPQMiniMatrix, AtomicLong> generateMatrixCollection(Element tabElement,
			String name) {
		
		final Map<MAPQMiniMatrix, AtomicLong> lengths = new LinkedHashMap<MAPQMiniMatrix, AtomicLong>();
		
		if (null != tabElement) {
			NodeList nl = tabElement.getElementsByTagName(name);
			//FIXME hack to get around the "NEW" tag added to lengths
			if (nl.getLength() == 0) {
				nl = tabElement.getElementsByTagName(name + "NEW");
			}
			Element nameElement = (Element) nl.item(0);
			
			QProfilerCollectionsUtils.populateMatrixMap(nameElement, lengths);
		}
		return lengths;
	}

	private static Map<Integer, AtomicLong> generateValueTally(Element tabElement,
			String name) {
		final NodeList nl = tabElement.getElementsByTagName(name);
		final Element nameElement = (Element) nl.item(0);
		return generateLengthsTally(nameElement, "ValueTally");
	}
	
	private static List<ChartTab> buildMultiTabCycles(boolean showMainTab, String mainTabName,
			Element tabElement, String id, String cycleName,
			String badReadName, List<String> columns, String[] colours, boolean isQualData) {
		
		final SummaryByCycle<Integer> cycle = QProfilerCollectionsUtils.generateSummaryByCycleFromElement(tabElement,
				cycleName);
		final Map<Integer, AtomicLong> lengths = generateLengthsTally(tabElement,
				"LengthTally");
		final Map<Integer, AtomicLong> badReads = generateValueTally(tabElement,
				badReadName);

		return createCyclesTabFromCollections(showMainTab, mainTabName, id + reportID, columns,
				colours, cycle, lengths, badReads, isQualData);
	}

	private static <T> List<ChartTab> createCyclesTabFromCollections(boolean showMainTab, String mainTabName,
			String id, List<String> columns, String[] colours, 
			SummaryByCycle<T> cycle, Map<Integer, AtomicLong> lengths,
			Map<Integer, AtomicLong> badReads, boolean isQualData) {
		
		// create cycle tab
		final ChartTab cycleCT = new ChartTab("Cycles", id + "c");
		cycleCT.setData(HTMLReportUtils.generateGoogleDataCycles(cycle, cycleCT
				.getName(), columns, isQualData, null));
		cycleCT.setChartInfo(HTMLReportUtils.generateGoogleChartSlidingColors(
				cycleCT.getName(), mainTabName + " Cycles", 1400, MAX_REPORT_HEIGHT,
				HTMLReportUtils.BAR_CHART, false, true, cycle.getPossibleValues().size(),
				colours));

		// create line lengths - don't always have these so check that collection has data..
		ChartTab lineLengthCT = null;
		if ( ! lengths.isEmpty()) {
			int lengthsWidth = lengths.size() > 26 ? 1200 : 950;
			lineLengthCT = new ChartTab("Line Lengths", id + "ll");
			lineLengthCT.setData(HTMLReportUtils.generateGoogleData(lengths,
					lineLengthCT.getName(), true));
			lineLengthCT.setChartInfo(HTMLReportUtils.generateGoogleChart(
					lineLengthCT.getName(), mainTabName + " Line Lengths", lengthsWidth,
					MIN_REPORT_HEIGHT, HTMLReportUtils.COLUMN_CHART, true, false));
			lineLengthCT.setDescription(LINE_LENGTH_DESCRIPTION);
		}
		//		
		int badReadsWidth = badReads.size() > 25 ? 1200 : 950;
		// create bad reads
		final ChartTab badReadsCT = new ChartTab("Bad Reads", id + "br");
		badReadsCT.setData(HTMLReportUtils.generateGoogleData(badReads,
				badReadsCT.getName(), true));
		badReadsCT.setChartInfo(HTMLReportUtils.generateGoogleChart(badReadsCT
				.getName(), mainTabName + " Bad Reads", badReadsWidth, MIN_REPORT_HEIGHT,
				HTMLReportUtils.COLUMN_CHART, true, false));
		badReadsCT.setDescription(isQualData ? BAD_QUALS_DESCRIPTION : BAD_READS_DESCRIPTION);

		List<ChartTab> tabs = new ArrayList<ChartTab>();
		
		if (showMainTab) {
			final ChartTab main = new ChartTab(mainTabName);
			main.addChild(cycleCT);
			if (null != lineLengthCT)
				main.addChild(lineLengthCT);
			main.addChild(badReadsCT);
			tabs.add(main);
		} else {
			tabs.add(cycleCT);
			if (null != lineLengthCT)
				tabs.add(lineLengthCT);
			tabs.add(badReadsCT);
		}
		
		return tabs;
	}
	//xu code
	private static Map<String, String>  createRgMap( Element summaryElement, Element isizeElement ) {		
		
 		Map<String, String> duplicateMap = new HashMap<>();
		Map<String, String> maxLengthMap = new HashMap<>();
		Map<String, String> aveLengthMap = new HashMap<>();
		Map<String, String> totalReadsMap = new LinkedHashMap<>();
		Map<String, String> unmappedMap = new HashMap<>(); 	
		Map<String, String> nonCanonicalMap = new HashMap<>(); 	
		Map<String, String> isizeMap = new HashMap<>();
		Map<String, String> hardClipMap = new HashMap<>();
		Map<String, String> softClipMap = new HashMap<>();
		Map<String, String> overlapMap = new HashMap<>();
		Map<String, String> lostMap = new HashMap<>();
		Map<String, String> trimmedMap = new HashMap<>();
		
		//isize
		NodeList isizeNodes =  isizeElement.getElementsByTagName("ReadGroup"); 			
		if (null != isizeNodes) 
			for (int i = 0 ; i < isizeNodes.getLength() ; i++){
				String rg = isizeNodes.item(i).getAttributes().getNamedItem("id").getNodeValue();
				String modal = isizeNodes.item(i).getAttributes().getNamedItem("ModalISize").getNodeValue();
				isizeMap.put( rg, modal);			   
			}	 
		
		//reads information
		NodeList readsChildren = ( (Element) summaryElement.getElementsByTagName("Reads").item(0) ).getElementsByTagName("ReadGroup");  
		
		int rgNum = (null != readsChildren)?  readsChildren.getLength() : 0; 		
		for (int i = 0 ; i < rgNum ; i++){  			
			String rg = readsChildren.item(i).getAttributes().getNamedItem("id").getNodeValue();
			//a NodeList of all descendant Elements 
			NodeList rgNodes =  ((Element) readsChildren.item(i)).getElementsByTagName("*"); 
			
			for(int j = 0; j < rgNodes.getLength(); j ++){
				String nodeName =  rgNodes.item(j).getNodeName();
				NamedNodeMap nodeMap = rgNodes.item(j).getAttributes();
				String percentage  = (nodeMap.getNamedItem("percentage") != null )? nodeMap.getNamedItem("percentage").getNodeValue() :
						(nodeMap.getNamedItem("basePercentage")  != null )? nodeMap.getNamedItem("basePercentage").getNodeValue() : null; 	
				switch (nodeName) {
					case "duplicate":  duplicateMap.put(rg, percentage); break;
					case "unmapped" : unmappedMap.put(rg,percentage); break; 
					case "nonCanonicalPair" : nonCanonicalMap.put(rg, percentage); ; break; 					
					case "softClip" : softClipMap.put(rg, percentage); break; 
					case "hardClip" : hardClipMap.put(rg, percentage); break; 
					case "overlap" : overlapMap.put(rg, percentage) ; break; 
					case "trimmedBase" : trimmedMap.put(rg, percentage) ; break; 						
					case "overall" : {
						maxLengthMap.put(rg, nodeMap.getNamedItem("maxLength").getNodeValue());				
						aveLengthMap.put(rg, nodeMap.getNamedItem("aveLength").getNodeValue());
						totalReadsMap.put(rg, nodeMap.getNamedItem("countedReads").getNodeValue());
						lostMap.put(rg, nodeMap.getNamedItem("lostBases").getNodeValue());							
					}; break; 													
				}					
			}	
		 }
				
		Map<String, String> summaryMap = new LinkedHashMap<>();
		final String startVBlock = "{v: '";
		final String endVBlock = "', p: {style: 'text-align: right'}}" ;
		final String finalVBlock = "]}";	
		
		//add header line
		summaryMap.put("Read Group", "TableName,Read Group,Read Count,Average<BR>Read<BR>Length,Max<BR>Read<BR>Length,Mode<BR>TLEN,Unmapped<BR>Reads,Non-canonical<BR>ReadPair,Duplicate<BR>Reads,"
				+ "Within<BR>ReadPair<BR>Overlap,Soft<BR>Clipping<BR>(CIGAR),Hard<BR>Clipping<BR>(CIGAR),Adaptor<BR>Trimming,Total<BR>Bases<BR>Lost");
		
		String overallEle = null; 
		for (  String rg : totalReadsMap.keySet()) {
			String lostColor = endVBlock; 
			try{ 
				float lost =  Float.valueOf(lostMap.get(rg).replace("%", "").trim());
				String color = (lost > 40)? "tomato":"yellow"; 
				if (lost < 20) {
					color = "palegreen";
				}
				lostColor = "', p: {style: 'text-align: right; background-color:" + color +";'}}" ;
			}catch(NumberFormatException e){ }	//do nothing			
			
			StringBuilder ele  = new StringBuilder(startVBlock).append(totalReadsMap.get(rg)).append(endVBlock)  
					.append(",").append(startVBlock).append(aveLengthMap.get(rg)).append(endVBlock )
					.append(",").append(startVBlock).append(maxLengthMap.get(rg)).append(endVBlock  						 	)
					.append(",").append(startVBlock).append((isizeMap.get(rg) == null ? "-" : isizeMap.get(rg) )).append(endVBlock)  					
					.append(",").append(startVBlock).append( unmappedMap.get(rg)).append( endVBlock  )
					.append(",").append(startVBlock).append(nonCanonicalMap.get(rg)).append(endVBlock) 
					.append(",").append(startVBlock).append(duplicateMap.get(rg)).append(endVBlock)  
					.append(",").append(startVBlock).append(overlapMap.get(rg)).append( endVBlock  )
					.append(",").append(startVBlock).append(softClipMap.get(rg)).append(endVBlock  )
					.append(",").append(startVBlock).append(hardClipMap.get(rg)).append( endVBlock) 
					.append(",").append(startVBlock).append(trimmedMap.get(rg)).append(endVBlock)
					.append(",").append(startVBlock).append(lostMap.get(rg)).append(lostColor).append(finalVBlock);

			if( ! rg.equals("overall")) {
				summaryMap.put(rg, ele.toString());	
			} else {
				overallEle = ele.toString(); 
			}
		}
		summaryMap.put("overall", overallEle);

		return summaryMap;
	}	
}
