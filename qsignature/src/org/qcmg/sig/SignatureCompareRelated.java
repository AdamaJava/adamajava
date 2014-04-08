/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.util.ComparisonUtil;
import org.qcmg.sig.util.SignatureUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class gets a list of snp chip signature files, and bam file signature files from the supplied path.
 * It then performs a comparison between related bam signature files and snp chip signature files.
 * If any comparison scores are less than the cutoff, they are added to a list, which is then emailed to interested parties informing them of the potential problem files
 *  
 * @author o.holmes
 *
 */
public class SignatureCompareRelated {
	
	private static QLogger logger;
	private int exitStatus;
	
	private List<File> orderedSnpChipFiles;
	
	private float cutoff = 0.2f;
	
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private String outputXml;
	private String path;
	private String searchSuffix = ".qsig.vcf";
	private String snpChipSearchSuffix = ".txt.qsig.vcf";
	private String [] additionalSearchStrings;
	
	private String excludeVcfsFile;
	private List<String> excludes;
	
	private final Map<File, int[]> fileIdsAndCounts = new HashMap<>();
	private final List<Comparison> allComparisons = new ArrayList<>();
	
//	private String email;
//	private String emailSubject = "Qsignature Comparison: all bams vs donor snp chip";
	private String logFile;
	
	List<String> suspiciousResults = new ArrayList<String>();
	
	private int engage() throws Exception {
		
		// get excludes
		excludes = SignatureUtil.getEntriesFromExcludesFile(excludeVcfsFile);
		
		
		// load snp chip data in map
		orderedSnpChipFiles = SignatureUtil.populateSnpChipFilesList(path, snpChipSearchSuffix, excludes, SignatureUtil.SNP_ARRAY);
		
		if (orderedSnpChipFiles.isEmpty()) {
			logger.warn("No snp chip signature files found to use in the comparison based on path: " + path + ", and snpChipSearchSuffix: " + snpChipSearchSuffix + ", and " + SignatureUtil.SNP_ARRAY +". Will try without " + SignatureUtil.SNP_ARRAY);
			orderedSnpChipFiles = SignatureUtil.populateSnpChipFilesList(path, snpChipSearchSuffix, excludes, null);
			if (orderedSnpChipFiles.isEmpty()) {
				logger.warn("No snp chip signature files found to use in the comparison based on path: " + path + ", and snpChipSearchSuffix: " + snpChipSearchSuffix + " - giving up");
				exitStatus = 1;
				return exitStatus;
			}
		}
		
		// get other signature files out there....
		List<File> files = FileUtils.findFilesEndingWithFilterNIO(path, searchSuffix);
		logger.info("Total number of files to be compared: " + files.size());
		// remove excluded files
		files = SignatureUtil.removeExcludedFilesFromList(files, excludes);
		logger.info("Total number of files to be compared (minus excluded files): " + files.size());
		
		final Set<File> uniqueFiles = new HashSet<File>(files);
		logger.info("No of unique files: " + uniqueFiles.size());
		
		final List<File> orderedUniqueFiles = new ArrayList<File>();
		for (File f : uniqueFiles) {
			
			if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
				boolean passesAdditionSearchString = true;
				for (String s : additionalSearchStrings) {
					if ( ! f.getPath().contains(s)) {
						passesAdditionSearchString = false;
						break;
					}
				}
				if (passesAdditionSearchString) 
					orderedUniqueFiles.add(f);
			} else {
				orderedUniqueFiles.add(f);
			}
		}
		
		if (orderedUniqueFiles.isEmpty()) {
			logger.warn("No signature files found based on path: " + path + ", and searchSuffix: " + searchSuffix);
			exitStatus = 1;
			return exitStatus;
		}
		
		// remove GEMMS
//		int countOfGemms = 0;
//		Iterator<File> iter = orderedUniqueFiles.iterator();
//		while (iter.hasNext()) {
//			File f = iter.next();
//			if (f.getAbsolutePath().contains("GEMM")) {
//				iter.remove();
//				countOfGemms++;
//			}
//		}
//		logger.info("removed: " + countOfGemms + " GEMMS bams from list");
		
		Collections.sort(orderedUniqueFiles, FileUtils.FILE_COMPARATOR);
		
		logger.info("No of unique and filtered files: " + orderedUniqueFiles.size());
		// bail if we have no files
		if (orderedUniqueFiles.isEmpty()) return exitStatus;
		
		// add files to map
		addFilesToMap(orderedUniqueFiles, orderedSnpChipFiles);
		
		
		int noOfProcessedFiles = 0;
		String currentDonor = DonorUtils.getDonorFromFilename(orderedUniqueFiles.get(0).getAbsolutePath());
		if (null == currentDonor) {
			logger.warn("Could not get donor information from file: " + orderedUniqueFiles.get(0).getAbsolutePath());
		}
		StringBuilder donorSB = new StringBuilder(currentDonor + "\n");
		Map<File, Map<ChrPosition, double[]>> donorSnpChipData = SignatureUtil.getDonorSnpChipData(currentDonor, orderedSnpChipFiles);
		if (donorSnpChipData.isEmpty()) {
			logger.warn("No snp chip qsignature files for donor: " + currentDonor);
		} else {
			donorSB.append("comparing against: \n");
			for (File file : donorSnpChipData.keySet()) {
				donorSB.append(file.getAbsolutePath() + "\n");
				
				// populate map with coverage details
				fileIdsAndCounts.get(file)[1] = donorSnpChipData.get(file).size();
			}
		}
		
		for (File f : orderedUniqueFiles) {
			
			String donor = DonorUtils.getDonorFromFilename(f.getAbsolutePath());
			if (null == donor) {
				logger.warn("Could not get donor information from file: " + f.getAbsolutePath());
				logger.warn("skipping....");
				continue;
			}
			
			if ( ! currentDonor.equals(donor)) {
				//print out previous donor info
				logger.info(donorSB.toString());
				
				// setup new currentDonor
				currentDonor = donor;
				donorSB = new StringBuilder(donor + "\n");
				// load new donors snp chip data files in memory
				donorSnpChipData = SignatureUtil.getDonorSnpChipData(currentDonor, orderedSnpChipFiles);
				if (donorSnpChipData.isEmpty()) {
					logger.warn("No snp chip qsignature files for donor: " + currentDonor);
//					suspiciousResults.add(currentDonor + "\t" + f.getName() + "\tNo snp chip qsignature files for donor: " + currentDonor);
				} else {
					donorSB.append("comparing against: \n");
					for (File file : donorSnpChipData.keySet()) {
						donorSB.append(file.getAbsolutePath() + "\n");
					}
				}
			}
			
			if ( ! donorSnpChipData.isEmpty()) {
				Map<ChrPosition, double[]> ratios = null;
				if (donorSnpChipData.containsKey(f)) {
					ratios = donorSnpChipData.get(f);
				} else {
					 ratios = SignatureUtil.loadSignatureRatios(f);
					 if (null == ratios) {
						 logger.info("file no longer exists: " + (null == f ? "" : f.getAbsoluteFile()));
						 continue;
					 }
					 // populate map with coverage details
					 fileIdsAndCounts.get(f)[1] = ratios.size();
					 
					if (ratios.size() < 1000) logger.warn("low coverage (" + ratios.size() + ") for file " + f.getAbsolutePath());
				}
				
				List<Comparison> comparisons = new ArrayList<>();
				for (Entry<File, Map<ChrPosition, double[]>> entry : donorSnpChipData.entrySet()) {
					comparisons.add(QSigCompareDistance.compareRatios(ratios, entry.getValue(), f, entry.getKey()));
				}
				if ( ! comparisons.isEmpty()) {
					allComparisons.addAll(comparisons);
					String comparisonResults = ComparisonUtil.getComparisonsBody(comparisons);
					donorSB.append(comparisonResults);
					donorSB.append("\n");
					
					if (ComparisonUtil.containsDodgyComparisons(comparisons, cutoff)) {
						suspiciousResults.add(currentDonor + "\t" + comparisonResults);
					}
				}
			}
		
			if (++noOfProcessedFiles % 100 == 0) {
				logger.info("hit " + noOfProcessedFiles + " files");
			}
		}
		
		// flush out last donor details
		logger.info(donorSB.toString());
		
		logger.info("");
		if (suspiciousResults.isEmpty()) {
			logger.info("No suspicious results found");
		} else {
			logger.info("SUMMARY:");
			for (String s : suspiciousResults) logger.info(s);
			//email();
		}
		
		if (outputXml != null)
			writeXmlOutput();
		
		return exitStatus;
	}
	
	private void writeXmlOutput() throws ParserConfigurationException, TransformerException {
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
		List<File> keys = new ArrayList<>( fileIdsAndCounts.keySet());
		Collections.sort(keys, FileUtils.FILE_COMPARATOR);
		for (File f  : keys) {
			int[] value = fileIdsAndCounts.get(f);
			
//			logger.info(value[0] + " : " +  f.getAbsolutePath() + " : " + value[1]);
			Element fileE = doc.createElement("file");
			fileE.setAttribute("id", value[0] + "");
			fileE.setAttribute("name", f.getAbsolutePath());
			fileE.setAttribute("coverage", value[1] + "");
			filesE.appendChild(fileE);
		}
		// and now the comparisons
//		logger.info("COMPARISONS: ");
		
		// list files
		Element compsE = doc.createElement("comparisons");
		rootElement.appendChild(compsE);
		for (Comparison comp : allComparisons) {
			int id1 = fileIdsAndCounts.get(comp.getMain())[0];
			int id2 = fileIdsAndCounts.get(comp.getTest())[0];
//			logger.info(id1 + " vs " + id2 + " - score: " + comp.getScore() + " overlaping coverage: " + comp.getOverlapCoverage() + " no of calcs: " + comp.getNumberOfCalculations());
			
			String id = "id_" + id1 + "_vs_" + id2;
			Element compE = doc.createElement(id);
			compE.setAttribute("score", comp.getScore() + "");
			compE.setAttribute("overlap", comp.getOverlapCoverage() + "");
			compE.setAttribute("calcs", comp.getNumberOfCalculations() + "");
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
	
	private void addFilesToMap(List<File> orderedUniqueFiles, List<File> orderedSnpChipFiles2) {
		int id = 1;
		for (File f : orderedUniqueFiles) {
			fileIdsAndCounts.put(f, new int[]{id++, -1});
		}
		for (File f : orderedSnpChipFiles2) {
			fileIdsAndCounts.put(f, new int[]{id++, -1});
		}
	}

//	private void email() throws IOException, InterruptedException {
//		if (suspiciousResults.isEmpty()) return;
//		
//		StringBuilder sb = new StringBuilder("Suspicious qsignature comparisons:\n");
//		for (String s : suspiciousResults) sb.append(s + "\n");
//		// add in log file path
//		sb.append('\n').append("Log file path:\n").append(logFile);
//		
//		String[] cmd = { "/bin/bash", "-c", "echo \"" + sb.toString() + "\" | mail -s \"" + emailSubject + "\" " + email};
////		String[] cmd = { "/bin/bash", "-c", "echo \"" + sb.toString() + "\" | mail -s \"Qsignature Comparison: all bams vs donor snp chip\" " + email};
//		logger.debug(Arrays.deepToString(cmd));
//		
//		Process p = new ProcessBuilder(cmd).start();
//		int emalExitStatus = p.waitFor();
//		
//		byte[] errorStream = new byte[1024];
//		java.io.InputStream isError = p.getErrorStream();
//		isError.read(errorStream);
//		
//		String errorMessage = new String(errorStream);
//		logger.info("Email sending exit status: " + emalExitStatus + ", msg: " + errorMessage);
//	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(SignatureCompareRelated.class);
		
		SignatureCompareRelated sp = new SignatureCompareRelated();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running SignatureCompareRelated:", e);
			else {
				System.err.println("Exception caught whilst running SignatureCompareRelated: " + e.getMessage());
				System.err.println(Messages.USAGE);
			}
		}
		
		// email results
		
		if (exitStatus == 0) {
//			sp.email();
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
//		} else if (options.getInputFileNames().length < 1) {
//			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(SignatureCompareRelated.class, logFile, options.getLogLevel());
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
//			if (cmdLineInputFiles.length < 1) {
//				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
//			} else {
//				// loop through supplied files - check they can be read
//				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
//					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
//						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
//					}
//				}
//			}
			
			cmdLineOutputFiles = options.getOutputFileNames();
			if (null != cmdLineOutputFiles && cmdLineOutputFiles.length > 0)
				outputXml = cmdLineOutputFiles[0];
			
			//TODO implement ability to search across multiple directories
			String[] paths = options.getDirNames(); 
			if (null != paths && paths.length > 0) {
				path = paths[0];
			}
			if (null == path) throw new QSignatureException("MISSING_DIRECTORY_OPTION");
			
			if (options.hasCutoff())
				cutoff = options.getCutoff();
			
			if (options.hasSearchSuffixOption())
				searchSuffix = options.getSearchSuffix();
			
			if (options.hasSnpChipSearchSuffixOption())
				snpChipSearchSuffix = options.getSnpChipSearchSuffix();
			
			if (options.hasAdditionalSearchStringOption())
				additionalSearchStrings = options.getAdditionalSearchString();
			
//			if (options.hasEmailOption())
//				email = options.getEmail();
//			if (options.hasEmailSubjectOption())
//				emailSubject = options.getEmaiSubjectl();
			
			if (options.hasExcludeVcfsFileOption())
				excludeVcfsFile = options.getExcludeVcfsFile();
			
			logger.logInitialExecutionStats("SignatureCompareRelated", SignatureCompareRelated.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
