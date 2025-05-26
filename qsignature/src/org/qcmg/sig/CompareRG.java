/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.math3.util.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.model.SigMeta;
import org.qcmg.sig.util.ComparisonUtil;
import org.qcmg.sig.util.SignatureUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * This class gets a list of all bam.qsig.vcf files from the supplied path.
 * It then performs a comparison between the readgroups within each bam file.
 * It does not perform any cross-bam-readgroup comparisons.
 * eg. if the directory contains 2 bams that contain readgroup data, it will perform intra-readgroup comparisons for each of the files, but will not compare the readgroups of one bam with that of the other bam.
 * 
 * An xml output file is produced
 *  
 * @author o.holmes
 *
 */
public class CompareRG {
	
	private static QLogger logger;
	private int exitStatus;
	
	private float genotypeCutoff = 0.75f;
	private int minimumCoverage = 10;
	private int minimumRGCoverage = 10;
	
	private String outputXml;
	private String [] paths;
	private String [] additionalSearchStrings;
	
	private String excludeVcfsFile;

    private final List<Comparison> allComparisons = new ArrayList<>();
	
	private final Map<File, Pair<SigMeta, TMap<String, TIntByteHashMap>>> cache = new THashMap<>();
	
	List<String> suspiciousResults = new ArrayList<>();
	
	private int engage() throws Exception {
		
		// get excludes
		logger.info("Retrieving excludes list from: " + excludeVcfsFile);
        List<String> excludes = SignatureUtil.getEntriesFromExcludesFile(excludeVcfsFile);
		
		// get qsig vcf files for this donor
		logger.info("Retrieving qsig vcf files from: " + String.join(",", paths));
		Set<File> uniqueFiles = new THashSet<>();
		for (String path : paths) {
			uniqueFiles.addAll(FileUtils.findFilesEndingWithFilterNIO(path, SignatureUtil.BAM_QSIG_VCF));
		}
		List<File> files = new ArrayList<>(uniqueFiles);
		
		if (files.isEmpty()) {
			logger.warn("Didn't find any files ending with " + SignatureUtil.BAM_QSIG_VCF + " in " + Arrays.toString(paths));
			return 0;
		}
		
		logger.info("Total number of files to be compared: " + files.size());
		// remove excluded files
		files = SignatureUtil.removeExcludedFilesFromList(files, excludes);
		logger.info("Total number of files to be compared (minus excluded files): " + files.size());
		
		if (files.isEmpty()) {
			logger.warn("No files left after removing excluded files");
			return 0;
		}
		
		/*
		 * Match files on additionalSearchStrings
		 */
		if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
			Predicate<File> p = (File f) -> Arrays.stream(additionalSearchStrings).anyMatch(s -> f.getAbsolutePath().contains(s));
			files = files.stream().filter(p).collect(Collectors.toList());
		}
		
		final int numberOfFiles = files.size();
		files.sort(FileUtils.FILE_COMPARATOR);
		
		for (int i = 0 ; i < numberOfFiles ; i++) {
			File f = files.get(i);
			logger.info("getting data for " + f.getAbsolutePath());
			
			Pair<SigMeta, TMap<String, TIntByteHashMap>> p = getSignatureData(f);
			
			/*
			 * Just intra-file checks for now - keeping data in cache will allow for inter-bam-rg comparisons at a later date.....
			 */
			TMap<String, TIntByteHashMap> m = p.getSecond();
			
			/*
			 * Check to see if we have more than 1 entry left in the map - if we do, compare, otherwise move on
			 */
			int mapSize = m.size();
			if (mapSize > 2) {
				
				/*
				 * remove the "all" entry from the map, and compare the others
				 */
				m.remove("all");
				
				List<String> rgIds = new ArrayList<>(m.keySet());
				
				for (int j = 0 ; j < rgIds.size() ; j++) {
					
					String rg1 = rgIds.get(j);
					TIntByteHashMap r1 = m.get(rg1);
					for (int k = j + 1 ; k < rgIds.size() ; k++) {
						String rg2 = rgIds.get(k);
						TIntByteHashMap r2 = m.get(rg2);
						
						Comparison c = ComparisonUtil.compareRatiosUsingSnpsFloat(r1, r2, f.getAbsolutePath() + "-" + rg1, f.getAbsolutePath() + "-" + rg2);
						if (c.getScore() < genotypeCutoff) {
							logger.warn("rgs don't match!: " + c);
						}
						allComparisons.add(c);
					}
				}
			} else {
				logger.info("Will not include " + f.getAbsolutePath() + " in comparisons as it does not contain multiple readgroups");
			}
		}
		
		for (Comparison comp : allComparisons) {
			if (comp.getScore() < genotypeCutoff) {
				suspiciousResults.add(comp.toSummaryString());
			}
		}
		
		
		if (suspiciousResults.isEmpty()) {
			logger.info("No suspicious results found");
		} else {
			logger.info("");
			logger.info("Suspicious results SUMMARY:");
			for (String s : suspiciousResults) {
				logger.info(s);
			}
		}
		
		if (outputXml != null) {
			writeXmlOutput();
		}
		
		return exitStatus;
	}
	
	Pair<SigMeta, TMap<String, TIntByteHashMap>> getSignatureData(File f) throws Exception {
		// check map to see if this data has already been loaded
		// if not - load
		Pair<SigMeta, TMap<String, TIntByteHashMap>> result = cache.get(f);
		if (result == null) {
			result = SignatureUtil.loadSignatureGenotype(f, minimumCoverage, minimumRGCoverage);
			
			if (result.getValue().get("all").size() < 1000) {
				logger.warn("low coverage (" + result.getValue().size() + ") for file " + f.getAbsolutePath());
			}
			
			cache.put(f, result);
		}
		return result;
	}
	
	private void writeXmlOutput() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("qsignature");
		doc.appendChild(rootElement);
		
		/*
		 * Create map of inputs,ids from from comparisons
		 */
		Map<String, int[]> inputIds = new THashMap<>();
		int i = 1;
		for (Comparison comp : allComparisons) {
			String i1 = comp.getMain();
			String i2 = comp.getTest();
			
			if ( ! inputIds.containsKey(i1)) {
				inputIds.put(i1, new int[] {i++, comp.getMainCoverage()});
			}
			if ( ! inputIds.containsKey(i2)) {
				inputIds.put(i2, new int[] {i++, comp.getTestCoverage()});
			}
		}
		
		// list files
		Element filesE = doc.createElement("files");
		rootElement.appendChild(filesE);
		
		// write output xml file
		// do it to console first...
		List<String> keys = new ArrayList<>( inputIds.keySet());
		keys.sort(null);
		for (String s  : keys) {
			int[] value = inputIds.get(s);
			
			Element fileE = doc.createElement("file");
			fileE.setAttribute("id", value[0] + "");
			fileE.setAttribute("name", s);
			fileE.setAttribute("coverage", value[1] + "");
			filesE.appendChild(fileE);
		}
		
		// list files
		Element compsE = doc.createElement("comparisons");
		rootElement.appendChild(compsE);
		for (Comparison comp : allComparisons) {
			int id1 = inputIds.get(comp.getMain())[0];
			int id2 = inputIds.get(comp.getTest())[0];
			
			Element compE = doc.createElement("comparison");
			compE.setAttribute("file1", id1 + "");
			compE.setAttribute("file2", id2 + "");
			compE.setAttribute("score", comp.getScore() + "");
			compE.setAttribute("overlap", comp.getOverlapCoverage() + "");
			compsE.appendChild(compE);
		}
		
		// write it out
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(outputXml));
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}
	
	public static void main(String[] args) throws Exception {
//		LoadReferencedClasses.loadClasses(CompareRG.class);
		
		CompareRG sp = new CompareRG();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running CompareRG:", e);
			else {
				System.err.println("Exception caught whilst running CompareRG: " + e.getMessage());
				System.err.println(Messages.COMPARE_USAGE);
			}
		}
		
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.COMPARE_USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.COMPARE_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.COMPARE_USAGE);
			options.displayHelp();
		} else {
			// configure logging
            String logFile = options.getLog();
			logger = QLoggerFactory.getLogger(CompareRG.class, logFile, options.getLogLevel());
			
			
			String [] cmdLineOutputFiles = options.getOutputFileNames();
			if (null != cmdLineOutputFiles && cmdLineOutputFiles.length > 0) {
				outputXml = cmdLineOutputFiles[0];
			}
			
			String[] paths = options.getDirNames(); 
			if (null != paths && paths.length > 0) {
				this.paths = paths;
			}
			if (null == paths || paths.length == 0) throw new QSignatureException("MISSING_DIRECTORY_OPTION");
			
			if (options.hasCutoff()) {
				genotypeCutoff = options.getCutoff();
			}
			logger.tool("Setting cutoff to: " + genotypeCutoff);
			
			options.getMinCoverage().ifPresent(i -> minimumCoverage = i);
			options.getMinRGCoverage().ifPresent(i -> minimumRGCoverage = i);
			logger.tool("Setting minimum coverage to: " + minimumCoverage);
			logger.tool("Setting minimum RG coverage to: " + minimumRGCoverage);
			
			additionalSearchStrings = options.getAdditionalSearchString();
			logger.tool("Setting additionalSearchStrings to: " + Arrays.deepToString(additionalSearchStrings));
			
			if (options.hasExcludeVcfsFileOption()) {
				excludeVcfsFile = options.getExcludeVcfsFile();
			}
			
			logger.logInitialExecutionStats("CompareRGGenotype", CompareRG.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
