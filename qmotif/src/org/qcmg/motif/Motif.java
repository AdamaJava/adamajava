/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.motif.util.MotifMode;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.motif.util.RegionType;
import org.qcmg.motif.util.SummaryStats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class Motif {
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	
	private final Options options;
	private final Configuration invariants;
	private final JobQueue jobQueue;

	public Motif(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;
		invariants = new Configuration(options);
		jobQueue = new JobQueue(invariants);
		saveCoverageReport();
	}
	
	private void addToMap(Map<String, AtomicInteger> map, String key, AtomicInteger value) {
		if (map.containsKey(key)) {
			AtomicInteger existingValue = map.get(key);
			existingValue.addAndGet(value.get());
		} else {
			map.put(key, value);
		}
	}

	private void writeXMLCoverageReport(SummaryStats ss)
			throws ParserConfigurationException, TransformerException {
		
		Map<ChrPosition, RegionCounter> results = ss.getResults();
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("qmotif");
		rootElement.setAttribute("version", getProgramVersion());
		doc.appendChild(rootElement);
		
		// ini file
		Element iniE = doc.createElement("ini");
		rootElement.appendChild(iniE);
		iniE.setAttribute("file", options.getIniFile());
		
		MotifsAndRegexes mAndR = invariants.getRegex();
		MotifMode mm = mAndR.getMotifMode();
		
		// stage1
		Element stage1E = doc.createElement("stage1_motif");
		
		if (mm.stageOneString()) {
			for (String s : mAndR.getStageOneMotifs().getMotifs()) {
				Element e = doc.createElement("string");
				e.setAttribute("value", s);
				stage1E.appendChild(e);
			}
		} else {
			// add regex
			Element e = doc.createElement("regex");
			e.setAttribute("value", mAndR.getStageOneRegexPattern().pattern());
			stage1E.appendChild(e);
		}
		// stage2
		Element stage2E = doc.createElement("stage2_motif");
		
		
		if (mm.stageTwoString()) {
			for (String s : mAndR.getStageTwoMotifs().getMotifs()) {
				Element e = doc.createElement("string");
				e.setAttribute("value", s);
				stage2E.appendChild(e);
			}
		} else {
			// add regex
			Element e = doc.createElement("regex");
			e.setAttribute("value", mAndR.getStageTwoRegexPattern().pattern());
			stage2E.appendChild(e);
		}
		
		Element windowSizeE = doc.createElement("window_size");
		windowSizeE.setAttribute("value", mAndR.getWindowSize() + "");
		Element includesOnlyE = doc.createElement(MotifConstants.INCLUDES_ONLY_MODE);
		includesOnlyE.setAttribute("value", ss.getIncludesOnly() + "");
		
		// add to ini element
		iniE.appendChild(stage1E);
		iniE.appendChild(stage2E);
		iniE.appendChild(windowSizeE);
		iniE.appendChild(includesOnlyE);
		
		// need includes and excludes
		// add the list of user defined includes
		Element includesE = doc.createElement("includes");
		iniE.appendChild(includesE);
		
		List<ChrPosition> includes = new ArrayList<>();
		for (Entry<ChrPosition, RegionCounter> entry : results.entrySet()) {
			if (RegionType.INCLUDES == entry.getValue().getType()) {
				includes.add(entry.getKey());
			}
		}
		Collections.sort(includes, COMPARATOR);
		
		for (ChrPosition cp : includes) {
			Element e = doc.createElement("region");
			includesE.appendChild(e);
			e.setAttribute("chrPos", cp.toIGVString());
			e.setAttribute("name", cp.getName());
		}
		// add the list of user defined excludes
		List<ChrPosition> excludes = new ArrayList<>();
		for (Entry<ChrPosition, RegionCounter> entry : results.entrySet()) {
			if (RegionType.EXCLUDES == entry.getValue().getType()) {
				excludes.add(entry.getKey());
			}
		}
		if ( ! excludes.isEmpty()) {
			
			Element excludesE = doc.createElement("excludes");
			iniE.appendChild(excludesE);
			
			Collections.sort(excludes, COMPARATOR);
			
			for (ChrPosition cp : excludes) {
				Element e = doc.createElement("region");
				includesE.appendChild(e);
				e.setAttribute("chrPos", cp.toIGVString());
				e.setAttribute("name", cp.getName());
			}
		}
		
		// summary element
		Element summaryE = doc.createElement("summary");
		rootElement.appendChild(summaryE);
		summaryE.setAttribute("bam", ss.getBamFileName());
		Element countsE = doc.createElement("counts");
		summaryE.appendChild(countsE);
		
		Element totalReadCountE = doc.createElement("totalReadsInThisAnalysis");
		totalReadCountE.setAttribute("count", ss.getTotalReadCount() + "");
		Element noOfMotifsE = doc.createElement("noOfMotifs");
		noOfMotifsE.setAttribute("count", ss.getUniqueMotifCount() + "");
		Element rawUnmappedE = doc.createElement("rawUnmapped");
		rawUnmappedE.setAttribute("count", ss.getRawUnmapped() + "");
		Element rawIncludesE = doc.createElement("rawIncludes");
		rawIncludesE.setAttribute("count", ss.getRawIncludes() + "");
		Element rawGenomicE = doc.createElement("rawGenomic");
		rawGenomicE.setAttribute("count", ss.getRawGenomic() + "");
		
		Element scaledUnmappedE = doc.createElement("scaledUnmapped");
		scaledUnmappedE.setAttribute("count", ss.getScaledUnmapped() + "");
		Element scaledIncludesE = doc.createElement("scaledIncludes");
		scaledIncludesE.setAttribute("count", ss.getScaledIncludes() + "");
		Element scaledGenomicE = doc.createElement("scaledGenomic");
		scaledGenomicE.setAttribute("count", ss.getScaledGenomic() + "");
		Element coveredBasesE = doc.createElement(MotifConstants.BASES_CONTAINING_MOTIFS);
		coveredBasesE.setAttribute("count", ss.getCoveredBases() + "");
		
		countsE.appendChild(totalReadCountE);
		countsE.appendChild(noOfMotifsE);
		countsE.appendChild(rawUnmappedE);
		countsE.appendChild(rawIncludesE);
		countsE.appendChild(rawGenomicE);
		
		countsE.appendChild(scaledUnmappedE);
		countsE.appendChild(scaledIncludesE);
		countsE.appendChild(scaledGenomicE);
		countsE.appendChild(coveredBasesE);
		
		// add the list of user defined includes
//		Element includesE = doc.createElement("includes");
//		summaryE.appendChild(includesE);
//		
//		List<ChrPosition> includes = new ArrayList<>();
//		for (Entry<ChrPosition, RegionCounter> entry : results.entrySet()) {
//			if (RegionType.INCLUDES == entry.getValue().getType()) {
//				includes.add(entry.getKey());
//			}
//		}
//		Collections.sort(includes);
//		
//		for (ChrPosition cp : includes) {
//			Element includeE = doc.createElement("region");
//			includesE.appendChild(includeE);
//			includeE.setAttribute("chrPos", cp.toIGVString());
//		}
			
		// list motifs
		Element motifsE = doc.createElement("motifs");
		rootElement.appendChild(motifsE);
			
		// get the unique list of motifs from results map
		
		Map<String, AtomicInteger> motifs = new HashMap<>();
		Collection<RegionCounter> rcs = results.values();
		for (RegionCounter rc : rcs) {
			if (rc.hasMotifs()) {
				if (null != rc.getMotifsForwardStrand()) {
//					Map<String, AtomicInteger> motifsFS = rc.getMotifsForwardStrand();
					Map<String, AtomicInteger> motifsFS = MotifUtils.convertStringArrayToMap(rc.getMotifsForwardStrand());
					for (Entry<String, AtomicInteger> entry : motifsFS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(motifs, entry.getKey(), entry.getValue());
					}
				}
				if (null != rc.getMotifsReverseStrand()) {
//					Map<String, AtomicInteger> motifsRS = rc.getMotifsReverseStrand();
					Map<String, AtomicInteger> motifsRS = MotifUtils.convertStringArrayToMap(rc.getMotifsReverseStrand());
					for (Entry<String, AtomicInteger> entry : motifsRS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(motifs, entry.getKey(), entry.getValue());
					}
				}
			}
		}
		
		int i = 0;
		List<String> orderedMotifs = new ArrayList<>(motifs.keySet());
		Collections.sort(orderedMotifs);
		for (String s : orderedMotifs) {
			Element motifE = doc.createElement("motif");
			motifE.setAttribute("id", ++i + "");
			motifE.setAttribute("motif", s);
			motifE.setAttribute("noOfHits", motifs.get(s).get() + "");
			motifsE.appendChild(motifE);
		}
		
		// set up regions element
		Element regionsE = doc.createElement("regions");
		rootElement.appendChild(regionsE);
		
		// get ordered positions
		List<ChrPosition> orderedPositions = new ArrayList<>(results.keySet());
		Collections.sort(orderedPositions, COMPARATOR);
		
		for (ChrPosition cp : orderedPositions) {
			// only output if we have a motif
			RegionCounter rc = results.get(cp);
			if (rc.hasMotifs()) {
				Element regionE = doc.createElement("region");
				regionE.setAttribute("chrPos", cp.toIGVString());
				regionE.setAttribute("name", cp.getName());
				regionE.setAttribute("type", rc.getType().toString().toLowerCase());
				regionE.setAttribute("stage1Cov", rc.getStage1Coverage() + "");
				regionE.setAttribute("stage2Cov", rc.getStage2Coverage() + "");
				regionsE.appendChild(regionE);
				
				// set motifs
				if (null != rc.getMotifsForwardStrand()) {
//					Map<String, AtomicInteger> motifsFS = rc.getMotifsForwardStrand();
					Map<String, AtomicInteger> motifsFS = MotifUtils.convertStringArrayToMap(rc.getMotifsForwardStrand());
					for (Entry<String, AtomicInteger> entry : motifsFS.entrySet()) {
						Element motifFSE = doc.createElement("motif");
						motifFSE.setAttribute("motifRef", (orderedMotifs.indexOf(entry.getKey()) + 1) + "");
						motifFSE.setAttribute("number", entry.getValue().get() + "");
						motifFSE.setAttribute("strand", "F");
						regionE.appendChild(motifFSE);
					}
				}
				if (null != rc.getMotifsReverseStrand()) {
//					Map<String, AtomicInteger> motifsRS = rc.getMotifsReverseStrand();
					Map<String, AtomicInteger> motifsRS = MotifUtils.convertStringArrayToMap(rc.getMotifsReverseStrand());
					for (Entry<String, AtomicInteger> entry : motifsRS.entrySet()) {
						Element motifFSE = doc.createElement("motif");
						motifFSE.setAttribute("motifRef", (orderedMotifs.indexOf(entry.getKey()) +1) + "");
						motifFSE.setAttribute("number", entry.getValue().get() + "");
						motifFSE.setAttribute("strand", "R");
						regionE.appendChild(motifFSE);
					}
				}
			}
		}
			
		// write it out
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(options.getOutputFileNames()[0]));
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}
	
	private void saveCoverageReport() throws Exception {
		writeXMLCoverageReport(jobQueue.getResults());
	}

	private static Options moptions = null;
	private static int exitStatus = 1; // Defaults to FAILURE
	private static boolean performLogging = false; // Defaults to false
	private static QLogger mlogger = null;

	public static void main(final String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Motif.class);
		try {
			moptions = new Options(args);
			if (moptions.hasHelpOption()) {
				displayHelpMessage();
			} else if (moptions.hasVersionOption()) {
				displayVersionMessage();
			} else {
				moptions.detectBadOptions();
				performLogging = true;
				mlogger = QLoggerFactory.getLogger(Motif.class, moptions
						.getLog(), moptions.getLogLevel());
				mlogger.logInitialExecutionStats(getProgramName(),
						getProgramVersion(), args);
				Motif operation = new Motif(moptions);
				exitStatus = 0; // SUCCESS
			}
		} catch (Throwable e) {
			String errorMessage = chooseErrorMessage(e);
			logErrorMessage(errorMessage, e);
		}
		if (performLogging && null != mlogger) {
			mlogger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}

	private static String chooseErrorMessage(Throwable e) {
		String message = e.toString();
		if (null == message) {
			message = "Unknown error";
		}
		return message;
	}

	private static void logErrorMessage(final String errorMessage,
			final Throwable throwable) {
		System.err.println(Messages.ERROR_PREFIX + errorMessage);
		throwable.printStackTrace();
		if (performLogging && null != mlogger) {
			mlogger.error(errorMessage, throwable);
			for (StackTraceElement elem : throwable.getStackTrace()) {
				mlogger.error(elem.toString());
			}
		}
	}

	private static void displayHelpMessage() throws Exception {
		System.out.println(Messages.USAGE);
		moptions.displayHelp();
	}

	private static void displayVersionMessage() throws Exception {
		System.err.println(Messages.getVersionMessage());
	}

	static String getProgramName() {
		return Motif.class.getPackage().getImplementationTitle();
	}

	static String getProgramVersion() {
		return Motif.class.getPackage().getImplementationVersion();
	}

	static String getVersionMessage() throws Exception {
		return getProgramName() + ", version " + getProgramVersion();
	}

}
