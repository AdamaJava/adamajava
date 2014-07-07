/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.motif.util.RegionType;
import org.qcmg.motif.util.SummaryStats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class Motif {
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
		
		// summary element
		Element summaryE = doc.createElement("summary");
		rootElement.appendChild(summaryE);
		summaryE.setAttribute("bam", ss.getBamFileName());
		Element countsE = doc.createElement("counts");
		summaryE.appendChild(countsE);
		
		Element windowSizeE = doc.createElement("windowSize");
		windowSizeE.setAttribute("count", ss.getWindowSize() + "");
		Element cutoffE = doc.createElement("cutoff");
		cutoffE.setAttribute("count", ss.getCutoff() + "");
		Element totalReadCountE = doc.createElement("totalReadCount");
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
		
		countsE.appendChild(windowSizeE);
		countsE.appendChild(cutoffE);
		countsE.appendChild(totalReadCountE);
		countsE.appendChild(noOfMotifsE);
		countsE.appendChild(rawUnmappedE);
		countsE.appendChild(rawIncludesE);
		countsE.appendChild(rawGenomicE);
		
		countsE.appendChild(scaledUnmappedE);
		countsE.appendChild(scaledIncludesE);
		countsE.appendChild(scaledGenomicE);
		
		// add the list of user defined includes
		Element includesE = doc.createElement("includes");
		summaryE.appendChild(includesE);
		
		List<ChrPosition> includes = new ArrayList<>();
		for (Entry<ChrPosition, RegionCounter> entry : results.entrySet()) {
			if (RegionType.INCLUDES == entry.getValue().getType()) {
				includes.add(entry.getKey());
			}
		}
		Collections.sort(includes);
		
		for (ChrPosition cp : includes) {
			Element includeE = doc.createElement("region");
			includesE.appendChild(includeE);
			includeE.setAttribute("chrPos", cp.toIGVString());
		}
			
		// list motifs
		Element motifsE = doc.createElement("motifs");
		rootElement.appendChild(motifsE);
			
		// get the unique list of motifs from results map
		
		Map<String, AtomicInteger> motifs = new HashMap<>();
		Collection<RegionCounter> rcs = results.values();
		for (RegionCounter rc : rcs) {
			if (rc.hasMotifs()) {
				if (null != rc.getMotifsForwardStrand()) {
					Map<String, AtomicInteger> motifsFS = MotifUtils.convertStringArrayToMap(rc.getMotifsForwardStrand());
					for (Entry<String, AtomicInteger> entry : motifsFS.entrySet()) {
						// add to allMotifs if not already there
						addToMap(motifs, entry.getKey(), entry.getValue());
					}
				}
				if (null != rc.getMotifsReverseStrand()) {
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
		Collections.sort(orderedPositions);
		
		for (ChrPosition cp : orderedPositions) {
			// only output if we have a motif
			RegionCounter rc = results.get(cp);
			if (rc.hasMotifs()) {
				Element regionE = doc.createElement("region");
				regionE.setAttribute("chrPos", cp.toIGVString());
				regionE.setAttribute("type", rc.getType().toString().toLowerCase());
				regionE.setAttribute("stage1Cov", rc.getStage1Coverage() + "");
				regionE.setAttribute("stage2Cov", rc.getStage2Coverage() + "");
				regionsE.appendChild(regionE);
				
				// set motifs
				if (null != rc.getMotifsForwardStrand()) {
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
		String message = null;
		if (null == e.getMessage()) {
			message = "Unknown error";
		} else {
			message = e.getMessage();
		}
		return message;
	}

	private static void logErrorMessage(final String errorMessage,
			final Throwable throwable) {
		System.err.println(Messages.ERROR_PREFIX + errorMessage);
		System.err.println(Messages.USAGE);
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
