/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.qvisualise.ChartTab;
import org.qcmg.qvisualise.Messages;
import org.qcmg.qvisualise.QVisualiseException;
import org.qcmg.qvisualise.util.CycleDetailUtils;
import org.qcmg.qvisualise.util.QProfilerCollectionsUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReportBuilder {
	
	private static final String[] SEQ_COLOURS = new String[] { "green", "blue", "black", "red", "aqua" };
	private static final String[] CS_COLOURS = new String[] { "blue", "green", "orange", "red", "black" };
	private static int reportID;
	private static int MAX_REPORT_HEIGHT = 900;
	private static int MIN_REPORT_HEIGHT = 540;
	private static int MAX_REPORT_WIDTH = 1400;
	private static int MIN_REPORT_WIDTH = 800;
	
	private static final String BAD_READS_DESCRIPTION = Messages.getMessage("BAD_READS_DESCRIPTION");
	private static final String BAD_QUALS_DESCRIPTION = Messages.getMessage("BAD_QUALS_DESCRIPTION");
	private static final String LINE_LENGTH_DESCRIPTION = Messages.getMessage("LINE_LENGTH_DESCRIPTION");
	private static final String TAG_MD_DESCRIPTION = Messages.getMessage("TAG_MD_DESCRIPTION");

	public static Report buildReport(ProfileType type, Element reportElement, int reportNumberId) throws QVisualiseException {
		final String fileName = reportElement.getAttribute("file");
		final String recordCount = reportElement.getAttribute("records_parsed");
		final String duplicateRecordCount = reportElement.getAttribute("duplicate_records");
		Long dupsCount = (null != duplicateRecordCount && ! duplicateRecordCount.isEmpty())?  Long.parseLong(duplicateRecordCount) : 0;
		final Report report = new Report(type, fileName, Long.parseLong(recordCount), dupsCount);
		reportID = reportNumberId;
		
		switch (type) {
		case BAM:
			createBamHeader(reportElement, report);
			createSEQ(reportElement, report);
			createQUALS(reportElement, report);
			createTAGS(reportElement, report);
			createISIZE(reportElement, report);
			createRNM(reportElement, report);
			createMRNM(reportElement, report);
			createCIGAR(reportElement, report);
			createMAPQ(reportElement, report);
			createFLAGS(reportElement, report);
			createCoverage(reportElement, report);
			//
			createMatrix(reportElement, report);
			break;
		case QUAL:
			for (ChartTab ct : buildMultiTabCycles(false,"Qual", reportElement, "qual",
					"QualityByCycle", "BadQualsInReads", CycleDetailUtils.getQualFileCycle(), 
					null, true)) {
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
			for (ChartTab ct : buildMultiTabCycles(true,"Base", reportElement, "fastq",
					"BaseByCycle", "BadBasesInReads", CycleDetailUtils.getTagCSNumericCycle(), CS_COLOURS, false)) {
				report.addTab(ct);
			}
			for (ChartTab ct : buildMultiTabCycles(true,"Qual", reportElement, "qual",
					"QualityByCycle", "BadQualsInReads", null, 
					null, true)) {
				report.addTab(ct);
			}
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
		final NodeList flagNL = reportElement.getElementsByTagName("FLAG");
		final Element flagElement = (Element) flagNL.item(0);
		final ChartTab flagTab = new ChartTab("FLAG");
		Map<String, AtomicLong> flags = getMapFromElement(flagElement);
		
		Map<String, AtomicLong> flagsKeyChange = new LinkedHashMap<String, AtomicLong>();
		for (Entry<String, AtomicLong> entry : flags.entrySet()) {
			String[] flagStirngArray = entry.getKey().split(", ");
			flagsKeyChange.put((flagStirngArray.length > 1 ? flagStirngArray[1] + ", ": "")  + flagStirngArray[0], entry.getValue());
		}
		
		flagTab.addChild(getChartTabFromMap("Flag", "fl", HTMLReportUtils.BAR_CHART, true, flagsKeyChange));
		
		// duplicates
		final Map<String, String> dupMap = new HashMap<String, String>();
		dupMap.put("d", "Duplicates");
		final Map<String, AtomicLong> duplicateFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, dupMap, "Singletons");
		flagTab.addChild(getChartTabFromMap("Duplicate", "fld", HTMLReportUtils.PIE_CHART, true, duplicateFlags));
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
		mappedMap.put("u", "Unmapped");
		final Map<String, AtomicLong> mappedFlags = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flags, mappedMap, "Mapped");
		flagTab.addChild(getChartTabFromMap("Unmapped", "flm", HTMLReportUtils.PIE_CHART, true, mappedFlags));
		
		report.addTab(flagTab);
	}

	private static void createCIGAR(Element reportElement, Report report) {
		//CIGAR
		final NodeList cigarNL = reportElement.getElementsByTagName("CIGAR");
		final Element cigarElement = (Element) cigarNL.item(0);
		final ChartTab cigarCT = createCigarChartTab(cigarElement);
		report.addTab(cigarCT);
	}

	private static void createMRNM(Element reportElement, Report report) {
		//MRNM
		final NodeList mrnmNL = reportElement.getElementsByTagName("MRNM");
		final Element mrnmElement = (Element) mrnmNL.item(0);
		report.addTab(generateTallyChartTab(mrnmElement, "MRNM", "m", HTMLReportUtils.BAR_CHART,  true));
	}

	private static void createISIZE(Element reportElement, Report report) throws QVisualiseException {
		//ISIZE
		final NodeList iSizeNL = reportElement.getElementsByTagName("ISIZE");
		final Element iSizeElement = (Element) iSizeNL.item(0);
		final ChartTab iSizeCT = createISizeChartTab(iSizeElement);
		if (null != iSizeCT)
			report.addTab(iSizeCT);
	}
	
	private static void createRNM(Element reportElement, Report report) {
		//ISIZE
		final NodeList rnmNL = reportElement.getElementsByTagName("RNAME_POS");
		final Element rnmElement = (Element) rnmNL.item(0);
		final ChartTab rnmCT = createRNMChartTab(rnmElement);
		if (null != rnmCT)
			report.addTab(rnmCT);
	}

	private static void createTAGS(Element reportElement, Report report) {
		// TAG
		final NodeList tagNL = reportElement.getElementsByTagName("TAG");
		final Element tagElement = (Element) tagNL.item(0);
		
		//TAG-CS
		final NodeList tagCSNL = tagElement.getElementsByTagName("CS");
		if (tagCSNL.getLength() > 1) {
//			System.out.println("NO OF CS NODES: " + tagCSNL.getLength());
			final Element tagCSElement = (Element) tagCSNL.item(0);
			for (ChartTab ct : buildMultiTabCycles(true,"TAG CS", tagCSElement, "tcs",
					"ColourByCycle", "BadColoursInReads", CycleDetailUtils.getTagCSNumericCycle(), CS_COLOURS, false)) {
				report.addTab(ct);
			}
		}
		
		//TAG-CQ
		final NodeList tagCQNL = tagElement.getElementsByTagName("CQ");
		if (tagCQNL.getLength() > 1) {
//			System.out.println("NO OF CQ NODES: " + tagCQNL.getLength());
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
		for (ChartTab ct : buildMultiTabCycles(true,"QUAL", qualElement, "q",
				"QualityByCycle", "BadQualsInReads", null, null, true)) {
			report.addTab(ct);
		}
	}

	private static void createSEQ(Element reportElement, Report report) {
		//SEQ
		final NodeList seqNL = reportElement.getElementsByTagName("SEQ");
		final Element seqElement = (Element) seqNL.item(0);
		for (ChartTab ct : buildMultiTabCycles(true,"SEQ", seqElement, "s",
				"BaseByCycle", "BadBasesInReads", CycleDetailUtils
				.getSeqCycle(), SEQ_COLOURS, false)) {
			report.addTab(ct);
		}
	}
	
	private static void createBamHeader(Element reportElement, Report report) {
		final NodeList headerNL = reportElement.getElementsByTagName("HEADER");
		if (null != headerNL) {
			final Element headerElement = (Element) headerNL.item(0);
			if (null != headerElement) {
				Map<String, List<String>> headerList = QProfilerCollectionsUtils.convertHeaderTextToMap(headerElement.getTextContent());
				if (null != headerList && ! headerList.isEmpty()) {
					ChartTab ct = new ChartTab("BAMHeader", "head" + reportID);
					ct.setData(HTMLReportUtils.generateGoogleDataForTable(headerList, ct.getName()));
					ct.setChartInfo(HTMLReportUtils.generateGoogleTable(ct.getName(), headerList.size()));
					ct.setRenderingInfo(HTMLReportUtils.generateRenderingTableInfo(ct.getName(), headerList.size()));
					report.addTab(ct);
				}
			}
		}
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
//		int badReadsWidth = 1200 ;
		int badReadsWidth = badReads.size() > 25 ? 1200 : 950;
		// create bad reads
		final ChartTab badReadsCT = new ChartTab("Bad Reads", id + "br");
		badReadsCT.setData(HTMLReportUtils.generateGoogleData(badReads,
				badReadsCT.getName(), true));
		badReadsCT.setChartInfo(HTMLReportUtils.generateGoogleChart(badReadsCT
				.getName(), mainTabName + " Bad Reads", badReadsWidth, MIN_REPORT_HEIGHT,
				HTMLReportUtils.COLUMN_CHART, true, false));
		badReadsCT.setDescription(isQualData ? BAD_QUALS_DESCRIPTION : BAD_READS_DESCRIPTION);
//		badReadsCT.setDescription("This chart shows the number of reads that have 10 or more bad bases (. or N)");

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
	
	private static ChartTab createMDChartTab(Element tagElement) {
		ChartTab parentCT = null;
//		String title = "rName";
//		String id = "rnm" + reportID;
		
		ChartTab mismatchCT = null;
		ChartTab forwardCT = null;
		ChartTab reverseCT = null;
		
		//TAG-MD
		final NodeList tagMDNL = tagElement.getElementsByTagName("MD");
		final Element tagMDElement = (Element) tagMDNL.item(0);
		// don't always have tag MD info
		if (null != tagMDElement && tagMDElement.hasChildNodes()) {
			// get data
			final SummaryByCycle<Integer> cycle = QProfilerCollectionsUtils.generateSummaryByCycleFromElement(tagMDElement,
					"MismatchByCycle");
			final Map<Integer, String> cyclePercentages = QProfilerCollectionsUtils.generatePercentagesMapFromElement(tagMDElement,
			"MismatchByCycle");
			
			// create cycle tab
			mismatchCT = new ChartTab("TAG MD", "tmd");
			mismatchCT.setData(HTMLReportUtils.generateGoogleDataCycles(cycle, mismatchCT
					.getName(), CycleDetailUtils.getSeqCycle(), false, cyclePercentages));
			mismatchCT.setChartInfo(HTMLReportUtils.generateGoogleChartSlidingColors(
					mismatchCT.getName(), "TAG MD Cycles", 1400, MAX_REPORT_HEIGHT,
					HTMLReportUtils.BAR_CHART, false, true, cycle.getPossibleValues().size(),
					SEQ_COLOURS));
			mismatchCT.setDescription(TAG_MD_DESCRIPTION);
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
			parentCT.addChild(mismatchCT);
			parentCT.addChild(forwardCT);
			parentCT.addChild(reverseCT);
		} else if (null != mismatchCT) {
			parentCT = mismatchCT;
		}
		
		return parentCT;
	}
	
	private static ChartTab createRNMChartTab(Element element) {
		ChartTab parentCT = null;
		String title = "rName";
		String id = "rnm" + reportID;
		
		final NodeList nlTop = element.getElementsByTagName("RNAME");
		
		parentCT = new ChartTab(title);
		
		for (int i = 0 , length = nlTop.getLength() ; i < length ; i++) {
			
			final Element nameElementTop = (Element) nlTop.item(i);
			String chromosome =  nameElementTop.getAttribute("value");
			
			if (null != chromosome && chromosome.startsWith("chr")) {
		
				final NodeList nl = nameElementTop.getElementsByTagName("RangeTally");
				final Element nameElement = (Element) nl.item(0);
				final TreeMap<Integer, AtomicLong> map = (TreeMap<Integer, AtomicLong>) createRangeTallyMap(nameElement);
			
				if ( ! map.isEmpty()) {
		
			// decide if we need to create 2 tabs
//			if (map.lastKey() > 5000) {
//				// split into 2 tabs
//				// create parent
//				parentCT = new ChartTab(title);
//				
//				// first tab shows 0 - 5000
//				final ChartTab ct1 = new ChartTab("0 to 5000", id+"1");
//				ct1.setData(HTMLReportUtils.generateGoogleData(
//						(map).subMap(0, true, 5000, false),
//						ct1.getName(), false));
//				ct1.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct1.getName(),
//						title + ", 0 to 5000", 1200, MAX_REPORT_HEIGHT, true));
//				
//				parentCT.addChild(ct1);
//				
//				// second chart - bin first 50000 values by 100
//				
//				final Map<Integer, AtomicLong> subSetMap =  map.headMap(50000, true);
//				final Map<Integer, AtomicLong> binnedSubSetMap = new TreeMap<Integer, AtomicLong>();
//				
//				int tally = 0;
//				for (Entry<Integer, AtomicLong> entry : subSetMap.entrySet()) {
//					tally += entry.getValue().get();
//					if ((entry.getKey() + 5) % 100 == 0) {
//						binnedSubSetMap.put((entry.getKey() + 5) - 50, new AtomicLong(tally));
//						tally = 0;
//					} 
//				}
//				
//				final ChartTab ct2 = new ChartTab("0 to 50000, binned by 100", id+"2");
//				ct2.setData(HTMLReportUtils.generateGoogleData(
//						binnedSubSetMap, ct2.getName(), false));
//				ct2.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct2.getName(),
//						title + ", 0 to 50000, binned by 100", 1200, MAX_REPORT_HEIGHT, true));
//				
//				parentCT.addChild(ct2);
//				
//			} else {
				// no need for sub tabs
				final ChartTab ct1 = new ChartTab(chromosome, id+i);
				ct1.setData(HTMLReportUtils.generateGoogleData(
						map,
						ct1.getName(), false));
				ct1.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct1.getName(),
						title + " - " + chromosome, 1200, MIN_REPORT_HEIGHT, true));
				
				parentCT.addChild(ct1);
//				parentCT = new ChartTab(title, id);
//				parentCT.setData(HTMLReportUtils.generateGoogleData(
//						map,
//						parentCT.getName(), false));
//				parentCT.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(parentCT.getName(),
//						title, 1200, MIN_REPORT_HEIGHT, true));
//			}
		}
			}
		}
		return parentCT;
	}
	
	private static ChartTab createISizeChartTab(Element element) throws QVisualiseException {
		ChartTab parentCT = null;
		String title = "iSize";
		String id = "is" + reportID;
		int idCounter = 1;
		//get read group ids
		NodeList childNodes = element.getChildNodes();
		List<String> readGroups = new ArrayList<>();
		for (int k = 0 ; k < childNodes.getLength() ; k++) {
			Node n = childNodes.item(k);
			if (n.getNodeName().equals("RG")) {
				readGroups.add(n.getAttributes().getNamedItem("value").getNodeValue());
			}
		}
		int noOfReadGroups = readGroups.size();
		System.out.println("we have " + noOfReadGroups + " read groups to display");
//		for (String rg : readGroups) {
//			System.out.println("rg: " + rg);
//			
//		}
		
		if (noOfReadGroups == 0) {
			throw new QVisualiseException("ISIZE_ERROR");
		}
		
		TreeMap<Integer, AtomicLongArray> arrayMap = new TreeMap<>();
		
		
		int counter = 0;
		for (String rg : readGroups) {
			final NodeList nl = element.getElementsByTagName("RG");
			for (int k = 0 ; k < nl.getLength() ; k++) {
				Node n = childNodes.item(k);
				NamedNodeMap nnm =n.getAttributes(); 
				if (null != nnm && nnm.getNamedItem("value").getNodeValue().equals(rg)) {
					
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
		
//		final NodeList nl = element.getElementsByTagName("RangeTally");
//		final Element nameElement = (Element) nl.item(0);
//		
//		final TreeMap<Integer, AtomicLong> map = (TreeMap<Integer, AtomicLong>) createRangeTallyMap(nameElement);
		
		if ( ! arrayMap.isEmpty()) {
		
			// decide if we need to create 2 tabs
			if (arrayMap.lastKey() > 1500) {
				// split into 3 tabs
				// create parent
				parentCT = new ChartTab(title);
				
				// add in the summary tab
				String tabTitle = "0 to 1500" + (noOfReadGroups == 1 ? "" : " - Summary");
				String longTitle = title + ", 0 to 1500, summed across all read groups";
				final ChartTab ct1Sum = new ChartTab(tabTitle, (id + idCounter++));
				
				TreeMap<Integer, AtomicLong> summaryMap = new TreeMap<>();
				for (Entry<Integer, AtomicLongArray> entry : arrayMap.entrySet()) {
					summaryMap.put(entry.getKey(), QProfilerCollectionsUtils.tallyArrayValues(entry.getValue()));
				}
				ct1Sum.setData(HTMLReportUtils.generateGoogleData(
						summaryMap.subMap(0, true, 1500, false),
						ct1Sum.getName(), false));
				ct1Sum.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct1Sum.getName(),
						longTitle, 1400, MAX_REPORT_HEIGHT, true));
				
				parentCT.addChild(ct1Sum);
				
				if (noOfReadGroups > 1) {	
				
					// first tab shows 0 - 1500
					longTitle = title + ", 0 to 1500, split by read group";
					final ChartTab ct1All = new ChartTab("0 to 1500 - All", (id + idCounter++));
					ct1All.setData(HTMLReportUtils.generateGoogleDataMultiSeries(
							(arrayMap).subMap(0, true, 1500, false),
							ct1All.getName(), false, readGroups));
					ct1All.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct1All.getName(),
							longTitle, 1400, MAX_REPORT_HEIGHT, true, "{position: 'right', textStyle: {color: 'blue', fontSize: 14}}, crosshair: {trigger: 'both'}"));
					
					parentCT.addChild(ct1All);
				}
				
				// next tab shows 0 - 5000
				tabTitle = "0 to 5000" + (noOfReadGroups == 1 ? "" : " - Summary");
				longTitle = title + ", 0 to 5000, summed across all read groups";
				final ChartTab ct2sum = new ChartTab(tabTitle, (id + idCounter++));
				ct2sum.setData(HTMLReportUtils.generateGoogleData(
						summaryMap.subMap(0, true, 5000, false),
						ct2sum.getName(), false));
				ct2sum.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct2sum.getName(),
						longTitle, 1400, MAX_REPORT_HEIGHT, true));
				
				parentCT.addChild(ct2sum);
				
				if (noOfReadGroups > 1) {	
					
					// tab shows 0 - 5000
					longTitle = title + ", 0 to 5000, split by read group";
					final ChartTab ct2All = new ChartTab("0 to 5000 - All", (id + idCounter++));
					ct2All.setData(HTMLReportUtils.generateGoogleDataMultiSeries(
							(arrayMap).subMap(0, true, 5000, false),
							ct2All.getName(), false, readGroups));
					ct2All.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct2All.getName(),
							longTitle, 1400, MAX_REPORT_HEIGHT, true, "{position: 'right', textStyle: {color: 'blue', fontSize: 14}}, crosshair: {trigger: 'both'}"));
					
					parentCT.addChild(ct2All);
				}
				
				
				// third tab shows 0 - 50000
//				final ChartTab ct3 = new ChartTab("0 to 50000", (id + idCounter++));
//				ct3.setData(HTMLReportUtils.generateGoogleDataMultiSeries(
//						(arrayMap).subMap(0, true, 50000, false),
//						ct3.getName(), false, readGroups));
//				ct3.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct3.getName(),
//						title + ", 0 to 50000", 1400, MAX_REPORT_HEIGHT, true, "{position: 'right', textStyle: {color: 'blue', fontSize: 14}}, crosshair: {trigger: 'both'}"));
//				
//				parentCT.addChild(ct3);
				
				// second chart - bin first 50000 values by 100
				
//				final Map<Integer, AtomicLongArray> subSetMap =  arrayMap.headMap(50000, true);
//				final Map<Integer, AtomicLongArray> binnedSubSetMap = new TreeMap<Integer, AtomicLongArray>();
//				
//				int tally = 0;
//				for (Entry<Integer, AtomicLongArray> entry : subSetMap.entrySet()) {
//					tally += entry.getValue().get(i)get();
//					if ((entry.getKey() + 5) % 100 == 0) {
//						binnedSubSetMap.put((entry.getKey() + 5) - 50, new AtomicLong(tally));
//						tally = 0;
//					} 
//				}
//				
//				final ChartTab ct2 = new ChartTab("0 to 50000, binned by 100", id+"2");
//				ct2.setData(HTMLReportUtils.generateGoogleDataMultiSeries(
//						binnedSubSetMap, ct2.getName(), false));
//				ct2.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(ct2.getName(),
//						title + ", 0 to 50000, binned by 100", 1200, MAX_REPORT_HEIGHT, true));
//				
//				parentCT.addChild(ct2);
				
			} else {
				// no need for sub tabs
				parentCT = new ChartTab(title, id);
				parentCT.setData(HTMLReportUtils.generateGoogleDataMultiSeries(
						arrayMap,
						parentCT.getName(), false, readGroups));
				parentCT.setChartInfo(HTMLReportUtils.generateGoogleScatterChart(parentCT.getName(),
						title, 1200, 940, true));
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
		
		// Fish-eye
//		ChartTab ct = new ChartTab("MAPQ Matrix", id);
//		ct.setData(HTMLReportUtils.generateGoogleMatrixData(tallys, ct.getName(), true));
//		ct.setChartInfo(HTMLReportUtils.generateGoogleHeatMap(ct.getName(),
//				title + " FishEye", 1600, 1000));
		
		ChartTab ct = null; 
		if ( ! tallys.isEmpty()) {
			ct = new ChartTab(title, id+idSuffix);
			ct.setData(HTMLReportUtils.generateGoogleMatrixData(tallys, ct.getName(), true));
			ct.setChartInfo(HTMLReportUtils.generateGoogleBioHeatMap(ct.getName(),
					title + " BioHeatMap", tallys.size() > 1000 ? 12 : 18));
//			ct.setChartInfo(HTMLReportUtils.generateGoogleBioHeatMap(ct.getName(),
//					title + " BioHeatMap", MAX_REPORT_WIDTH, MAX_REPORT_HEIGHT));
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

}
