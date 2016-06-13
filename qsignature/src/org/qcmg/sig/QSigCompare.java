/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class QSigCompare {
	
	// make static
	static final double het_low = 0.35;
	static final double het_up  = 0.65;
	static final double hom_low = 0.05;
	static final double hom_up  = 0.95;
	static final double DOUBLE_NAN  = Double.NaN;
	
	private static QLogger logger = QLoggerFactory.getLogger(QSigCompare.class);
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private int minCoverage = 10;
	private float cutoff = 0.035f;
	
	private final  List<File> vcfFiles = new ArrayList<File>();
	
	private final Map<File, Map<ChrPosition, int[]>> fileRatios = new HashMap<File, Map<ChrPosition, int[]>>();
	private final List<String[]> displayResults = new ArrayList<String[]>();
	
	private int engage() throws Exception {
		
		// populate the random SNP Positions map
		for (String s : cmdLineInputFiles) loadVcfFiles(s);
		
		// lets go
		selectFilesForComparison();
		
		// log some stats:
		logger.info("SUMMARY STATS:");
		for (String [] stat : displayResults) {
			logger.info(Arrays.deepToString(stat));
		}
		
		// write xml output
		writeOutput();
		
		return exitStatus;
	}
	
	private void writeOutput() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Couldn't create new Document Builder", e);
		}
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("QSigCompare");
		doc.appendChild(rootElement);
		
		Element bamFilesElement = doc.createElement("VCFFiles");
		rootElement.appendChild(bamFilesElement);
		
		// bam file elements
		for (File f : vcfFiles) {
			Element bamElement = doc.createElement("VCF");
			bamFilesElement.appendChild(bamElement);
			
			String [] fileAttributes = null;
			TabbedFileReader reader = null;
			try {
				reader = new TabbedFileReader(f);
				fileAttributes = getDetailsFromVCFHeader(reader.getHeader());
			} catch (Exception e) {
				logger.error("Couldn't retrieve file attributes", e);
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error("Can't close file", e);
				}
			}
			
			// set some attributes on this bam element
			bamElement.setAttribute("id", "" + (vcfFiles.indexOf(f) + 1));
			bamElement.setAttribute("patient", fileAttributes[0]);
			bamElement.setAttribute("library", fileAttributes[1]);
			bamElement.setAttribute("inputType", fileAttributes[2]);
			bamElement.setAttribute("snpFile", fileAttributes[3]);
			bamElement.setAttribute("vcf", f.getAbsolutePath());
		}
		
		Element resultsElement = doc.createElement("Results");
		rootElement.appendChild(resultsElement);
		
		for (String [] s : displayResults) {
			Element resultElement = doc.createElement("result");
			resultsElement.appendChild(resultElement);
			
			resultElement.setAttribute("files", s[0] + " vs " + s[1]);
			resultElement.setAttribute("rating", s[2]);
			resultElement.setAttribute("score", s[3]);
			resultElement.setAttribute("snpsUsed", s[4]);
			resultElement.setAttribute("flag", s[5]);
		}
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			logger.error("Can't create new transformer factory", e);
		}
		DOMSource source = new DOMSource(doc);
		
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
		// if no output specified, output to standard out
		StreamResult result = null;
		if (cmdLineOutputFiles == null || cmdLineOutputFiles.length < 1)
			result = new StreamResult(System.out );
		else 
			result = new StreamResult(new File(cmdLineOutputFiles[0]));
		 
		 try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			logger.error("Can't transform file", e);
		}
	}
	
	private void loadVcfFiles(String directory) {
		// does not recurse - add boolean param to method if this is required
		vcfFiles.addAll(Arrays.asList(FileUtils.findFilesEndingWithFilter(directory, ".vcf")));
		// and any zipped files
		vcfFiles.addAll(Arrays.asList(FileUtils.findFilesEndingWithFilter(directory, ".vcf.gz")));
	}
	
	private void selectFilesForComparison() throws Exception {
		// loop through the vcfFiles list, and run comparisons if no entry exists in the results map
		
		for (int i = 0 , size = vcfFiles.size(); i < size ; i++) {
			File firstFile = vcfFiles.get(i);
			
			for (int j = i + 1; j < size ; j++) {
				File secondFile = vcfFiles.get(j);
				doComparison(firstFile, secondFile);
			}
			
			// can now remove firstFile data from the fileRatios map
			// this may be required to keep memory usage down
			 fileRatios.remove(firstFile);
		}
	}
	
	private void doComparison(File f1, File f2) throws Exception {
		TabbedFileReader vcf1 = new TabbedFileReader(f1);
		TabbedFileReader vcf2 = new TabbedFileReader(f2);
		String [] s1PatientAndType = null; 
		String [] s2PatientAndType = null;
		
		try {
			TabbedHeader vcfHeader1 = vcf1.getHeader();
			TabbedHeader vcfHeader2 = vcf2.getHeader();
			
			s1PatientAndType = getDetailsFromVCFHeader(vcfHeader1);
			s2PatientAndType = getDetailsFromVCFHeader(vcfHeader2);
			
			if ( ! fileRatios.containsKey(f1)) {
				fileRatios.put(f1, loadRatiosFromFile(vcf1));
			}
			if ( ! fileRatios.containsKey(f2)) {
				fileRatios.put(f2, loadRatiosFromFile(vcf2));
			}
		} finally {
			try {
				vcf1.close();
			} finally {
				vcf2.close();
			}
		}
		
		// now do the comparisons
		Map<ChrPosition, int[]> file1Ratios = fileRatios.get(f1);
		Map<ChrPosition, int[]> file2Ratios = fileRatios.get(f2);
		
		AtomicInteger noOfPositionsUsed = new AtomicInteger(0);
		
		float totalDiff = compareRatios(file1Ratios, file2Ratios, minCoverage, noOfPositionsUsed);
		
		String rating = getRating(s1PatientAndType, s2PatientAndType);
		float score = noOfPositionsUsed.get() == 0 ? Float.NaN : (totalDiff / noOfPositionsUsed.get());
		String flag = getFlag(rating, score, noOfPositionsUsed.get(), cutoff);
		
		displayResults.add(new String[] {"" + (vcfFiles.indexOf(f1) + 1) , "" +  (vcfFiles.indexOf(f2) + 1), 
				rating , ""+score , ""+noOfPositionsUsed.get() , flag}); 
		
	}
	
	/**
	 * Method that looks at a rating, score, and cutoff, and returns "OK" or "???"
	 * 
	 * @param rating String consisting of "AAA" through to "BBB"
	 * @param score float score
	 * @param snpCount int count of position used to generate the score
	 * @param cutoffValue float cutoff that is used to make the call
	 * @return String  
	 */
	public static final String getFlag(String rating, float score, int snpCount, float cutoffValue) {
		if (null == rating) throw new IllegalArgumentException("null rating passed to getFlag");
		
		// some hard-coded rules in here to start with
		if (snpCount == 0) return "???";
		if (rating.startsWith("B") && score < cutoffValue) return "???";
		if (rating.startsWith("A") && score > cutoffValue) return "???";
		
		return "OK";
	}
	
	/**
	 * Compares 2 string arrays, and returns a rating string representing their sameness
	 * <br>
	 * ie. if we have 2 arrays containing the same patient, library and input type, the rating returned is 'AAA'
	 * If any of the inputs differ, a 'B' is used instead.
	 * <br>
	 * ie. for 2 arrays containing different patients, libraries, but of the same input type, the corresponding rating would be 'BBA'
	 * 
	 * @param s1 1st String array containing patient, library, input type to be compared
	 * @param s2 2nd String array containing patient, library, input type to be compared
	 * @return String rating which is a scale from 'AAA' to 'BBB' depending on the sameness of the elements of the string arrays
	 */
	public static final String getRating(final String [] s1, final  String [] s2) {
		if (null == s1 || null == s2 || s1.length < 3 || s2.length < 3) 
			throw new IllegalArgumentException("invalid argument passed to getRating");
		
		if (Arrays.deepEquals(s1, s2)) return "AAA";
		
		if (s1[0].equals(s2[0]) && s1[1].equals(s2[1]))
			return "AAB";
		else if (s1[1].equals(s2[1]) && s1[2].equals(s2[2]))
			return "BAA";
		else if (s1[0].equals(s2[0]) && s1[2].equals(s2[2]))
			return "ABA";
		else if (s1[0].equals(s2[0]) )
			return "ABB";
		else if (s1[1].equals(s2[1]) )
			return "BAB";
		else if (s1[2].equals(s2[2]) )
			return "BBA";
		
		return "BBB";
	}

	public static float compareRatios(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int minCoverage, AtomicInteger noOfPositionsUsed) {
		float totalDifference = 0f;
		
		for (Entry<ChrPosition, int[]> file1RatiosEntry : file1Ratios.entrySet()) {
			final int[] file1Ratio = file1RatiosEntry.getValue();
			final int f1TotalCount = file1Ratio[1];
			// both total counts must be above the minCoverage value
			if (f1TotalCount < minCoverage) continue;
			
			final int[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
			if (file2Ratio == null) continue;
			
			// first entry in array is the non-ref count, the second is the total count
			final int f2TotalCount = file2Ratio[1];
			if (f2TotalCount < minCoverage) continue;
			
			final int f2NonRefCount = file2Ratio[0];
			final int f1NonRefCount = file1Ratio[0];
			
			noOfPositionsUsed.incrementAndGet();
			
			totalDifference += Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
		}
		
		return totalDifference;
	}
	
	private Map<ChrPosition, int[]> loadRatiosFromFile(TabbedFileReader reader) {
		logger.info("loading ratios from file: " + reader.getFile().getAbsolutePath());
		
		Map<ChrPosition, int[]> ratios = new HashMap<ChrPosition, int[]>();
		
		for (TabbedRecord vcfRecord : reader) {
			String[] params = TabTokenizer.tokenize(vcfRecord.getData());
//			String[] params = tabbedPattern.split(vcfRecord.getData(), -1);
			ChrPosition chrPos = ChrPointPosition.valueOf(params[0], Integer.parseInt(params[1]));
			char ref = params[3].charAt(0);
			if ('-' == ref || '.' == ref) {
//				logger.warn("skipping position with ref = -");
				continue;
			}
			String coverage = params[7];
			ratios.put(chrPos, getRatioFromCoverageString(coverage, ref));
		}
		
		return ratios;
	}
	
	public static int[] getRatioFromCoverageString(final String coverage, final char ref) {
		
		// strip out the pertinent bits
		// total
		int total = Integer.parseInt(coverage.substring(coverage.indexOf("TOTAL:") + 6, coverage.indexOf("NOVELCOV") - 1));
		
		int refPosition = coverage.indexOf(ref + ":") + 2;
//		logger.info("coverage: " + coverage + " , refPosition: " + refPosition + ", ref: " + ref);
		String refCountString = coverage.substring(refPosition, coverage.indexOf(",", refPosition));
		boolean isDigit = true;
		for (char c : refCountString.toCharArray()) {
			if (Character.isAlphabetic(c)) {
				logger.info("Invalid string in coverage: " + refCountString + " : " + coverage + " : " + ref);
				isDigit = false;
			}
		}
		int refCount = isDigit ? Integer.parseInt(refCountString) : 0;
		
		return new int[] {total-refCount, total};
	}
	
	public static double[] getDiscretisedValuesFromCoverageString(final String coverage) {
		
		int[] baseCoverages = SignatureUtil.decipherCoverageString(coverage);
		int total = baseCoverages[4];
		if (total < 10) return null;
		
		final double aFrac = (double) baseCoverages[0] / total;
		final double cFrac = (double) baseCoverages[1] / total;
		final double gFrac = (double) baseCoverages[2] / total;
		final double tFrac = (double) baseCoverages[3] / total;
		
		final double[] array = new double[] {getDiscretisedValue(aFrac), 
				getDiscretisedValue(cFrac),
				getDiscretisedValue(gFrac), 
				getDiscretisedValue(tFrac)};
		
		return array;
	}
	
	public static double getDiscretisedValue(double initialValue) {
		if (initialValue >= hom_up) return 1.0;
		if (initialValue <= hom_low) return 0.0;
		if (initialValue > het_low && initialValue < het_up) return 0.5;
		return DOUBLE_NAN;
	}
	
	public static String [] getDetailsFromVCFHeader(TabbedHeader header) {
		String patient = null;
		String library = null;
		String inputType = null;
		String snpFile = null;
		for (Iterator<String> iter = header.iterator() ; iter.hasNext() ; ) {
			String headerLine = iter.next();
			if (headerLine.contains("patient_id")) patient = headerLine.substring(headerLine.indexOf("=") + 1);  
			if (headerLine.contains("library")) library = headerLine.substring(headerLine.indexOf("=") + 1);  
			if (headerLine.contains("input_type")) inputType = headerLine.substring(headerLine.indexOf("=") + 1);
			if (headerLine.contains("snp_file")) snpFile = headerLine.substring(headerLine.indexOf("=") + 1);
		}
		return new String[] {patient, library, inputType, snpFile};
	}
	
	public static void main(String[] args) throws Exception {
		QSigCompare sp = new QSigCompare();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running QSigCompare:", e);
			else System.err.println("Exception caught whilst running QSigCompare");
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
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(QSigCompare.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("QSigCompare", QSigCompare.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QSignatureException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			if (options.getMinCoverage() > 0)
				minCoverage =  options.getMinCoverage();
			if (options.getCutoff() > 0.0f)
				cutoff =  options.getCutoff();
			
			logger.tool("Will use minCoverage of: " + minCoverage + ", and a cutoff of: " + cutoff);
			
			return engage();
		}
		return returnStatus;
	}
}
