/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntShortHashMap;
import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.set.hash.THashSet;

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
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.model.SigMeta;
import org.qcmg.sig.util.SignatureUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class gets a list of all .qsig.vcf files from the supplied path.
 * It then performs a comparison between them all, regardless of whether they are bam or snp chip files
 * An xml output file is produced
 * If any comparison scores are less than the cutoff, they are added to a list, which is then emailed to interested parties informing them of the potential problem files
 *  
 * @author o.holmes
 *
 */
public class VariantAlleleFreqDist {
	
	private static QLogger logger;
	private int exitStatus;
	
	private float cutoff = 0.95f;
	private int minimumCoverage = 10;
	private int minimumRGCoverage = 10;
	
	private String outputXml;
	private String [] paths;
	private String [] additionalSearchStrings;
	
	private String excludeVcfsFile;
	private List<String> excludes;
	private String logFile;
	
	private final Map<String, int[]> fileIdsAndCounts = new THashMap<>();
	private final List<Comparison> allComparisons = new ArrayList<>();
	
	private final Map<File, Pair<SigMeta,TIntShortHashMap>> cache = new THashMap<>();
	
	List<String> suspiciousResults = new ArrayList<String>();
	
	private int engage() throws Exception {
		
		// get excludes
		logger.info("Retrieving excludes list from: " + excludeVcfsFile);
		excludes = SignatureUtil.getEntriesFromExcludesFile(excludeVcfsFile);
		
		// get qsig vcf files
		logger.info("Retrieving qsig vcf files from: " + Arrays.stream(paths).collect(Collectors.joining(",")));
		Set<File> uniqueFiles = new THashSet<>();
		for (String path : paths) {
			uniqueFiles.addAll(FileUtils.findFilesEndingWithFilterNIO(path, SignatureUtil.QSIG_VCF));
		}
		List<File> files = new ArrayList<>(uniqueFiles);
		
		if (files.isEmpty()) {
			logger.warn("Didn't find any files ending with " + SignatureUtil.QSIG_VCF + " in " + Arrays.toString(paths));
			return 0;
		}
		
		logger.info("Total number of files to be VAF'ed: " + files.size());
		// remove excluded files
		files = SignatureUtil.removeExcludedFilesFromList(files, excludes);
		logger.info("Total number of files to be VAF'ed (minus excluded files): " + files.size());
		
		if (files.isEmpty()) {
			logger.warn("No files left after removing exlcuded files");
			return 0;
		}
		
		/*
		 * Match files on additionalSearchStrings
		 */
		if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
			Predicate<File> p = (File f) -> {
				return Arrays.stream(additionalSearchStrings).anyMatch(s -> f.getAbsolutePath().contains(s));
			};
			files = files.stream().filter(f -> p.test(f)).collect(Collectors.toList());
		}
		
		
//		final int numberOfFiles = files.size();
//		final int numberOfComparisons = ((numberOfFiles * (numberOfFiles -1)) /2);
//		logger.info("Should have " +numberOfComparisons + " comparisons, based on " + numberOfFiles + " input files");
		
		files.sort(FileUtils.FILE_COMPARATOR);
		int size = files.size();
		for (int i = 0 ; i < size ; i++) {
			File f1 = files.get(i);
			Map<Short, int[]> vafDist = SignatureUtil.getVariantAlleleFractionDistribution(f1, minimumCoverage);
			writeXmlOutput(f1, vafDist,outputXml);
//			TShortIntHashMap vafDist = SignatureUtil.getVariantAlleleFractionDistribution(f1, minimumCoverage);
//			writeXmlOutput(f1, vafDist,outputXml);
		}
		
		return exitStatus;
	}
	
	public static void writeXmlOutput(File file, Map<Short, int[]> vafDist, String outputXml) throws ParserConfigurationException, TransformerException {
//		public static void writeXmlOutput(File file, TShortIntHashMap vafDist, String outputXml) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("qsignature");
		doc.appendChild(rootElement);
		
		// list files
		Element filesE = doc.createElement("files");
		rootElement.appendChild(filesE);
		
		// write output xml file
		// do it to console first...
			
			Element fileE = doc.createElement("file");
			fileE.setAttribute("id",  "1");
			fileE.setAttribute("name", file.getAbsolutePath());
//			fileE.setAttribute("coverage", value[1] + "");
//			fileE.setAttribute("average_coverage_at_positions", value[2] + "");
			filesE.appendChild(fileE);
		
		// list files
		Element vafsE = doc.createElement("variant_allele_frequencies");
		rootElement.appendChild(vafsE);
		
		/*
		 * list all possible values, regardless of if they have a non-zero value
		 */
		for (short s = 0 ; s <= 100 ; s++) {
			Element vafE = doc.createElement("variant_allele_frequency");
			vafE.setAttribute("value", s + "");
			vafE.setAttribute("count_passing_coverage", vafDist.get(s)[0] + "");
			vafE.setAttribute("count_not_passing_coverage", vafDist.get(s)[1] + "");
			vafsE.appendChild(vafE);
		}
		
		// write it out
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		
		
		/*
		 * check outputXml, if directory, add filename and extension, if file, don't
		 */
		File o = new File(outputXml);
		if (o.isDirectory()) {
			String name = file.getName();
			/*
			 * remove existing qsig if it exists
			 */
			int index = name.indexOf(".qsig");
			if (index > -1) {
				name = name.substring(0, index);
			}
			logger.info("will write output to: " + name);
			o = new File(o.getAbsolutePath() + System.getProperty("file.separator") + name + ".qsig.vaf.xml");
		}
		
		StreamResult result = new StreamResult(o);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(VariantAlleleFreqDist.class);
		
		VariantAlleleFreqDist sp = new VariantAlleleFreqDist();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running VariantAlleleFreqDist:", e);
			else {
				System.err.println("Exception caught whilst running VariantAlleleFreqDist: " + e.getMessage());
				System.err.println(Messages.USAGE);
			}
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(VariantAlleleFreqDist.class, logFile, options.getLogLevel());
			
			
			String [] cmdLineOutputFiles = options.getOutputFileNames();
			if (null != cmdLineOutputFiles && cmdLineOutputFiles.length > 0)
				outputXml = cmdLineOutputFiles[0];
			
			String[] paths = options.getDirNames(); 
			if (null != paths && paths.length > 0) {
				this.paths = paths;
			}
			if (null == paths || paths.length == 0) throw new QSignatureException("MISSING_DIRECTORY_OPTION");
			
			if (options.hasCutoff()) {
				cutoff = options.getCutoff();
			}
			
			options.getMinCoverage().ifPresent(i -> {minimumCoverage = i.intValue();});
			options.getMinRGCoverage().ifPresent(i -> {minimumRGCoverage = i.intValue();});
			logger.tool("Setting minumim coverage to: " + minimumCoverage);
			logger.tool("Setting minumim RG coverage to: " + minimumRGCoverage);
			
			additionalSearchStrings = options.getAdditionalSearchString();
			logger.tool("Setting additionalSearchStrings to: " + Arrays.deepToString(additionalSearchStrings));
			
			if (options.hasExcludeVcfsFileOption())
				excludeVcfsFile = options.getExcludeVcfsFile();
			
			logger.logInitialExecutionStats("VariantAlleleFreqDist", VariantAlleleFreqDist.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
