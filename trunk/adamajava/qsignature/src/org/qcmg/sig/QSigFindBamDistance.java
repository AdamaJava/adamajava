/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.sig.util.SignatureUtil;

public class QSigFindBamDistance {
	
	private static QLogger logger;
	private int exitStatus;
	
	private ConcurrentMap<String, Double> results = new ConcurrentHashMap<String, Double>();
	private ConcurrentMap<ChrPosition, double[]> lostFileRatios;
	
	private float cutoff = 0.01f;
	
	private String[] cmdLineInputFiles;
	private String path =  "/mnt/seq_results/icgc_pancreatic";
	private String searchSuffix = ".txt.qsig.vcf";
	private String [] additionalSearchStrings;
	
	private int engage() throws Exception {
		
		// get other signature files out there....
		final File[] files = FileUtils.findFilesEndingWithFilter(path, searchSuffix, true);
		final Set<File> uniqueFiles = new HashSet<File>(Arrays.asList(files));
		logger.info("No of unique files: " + uniqueFiles.size());
		
		final List<File> orderedUniqueFiles = new ArrayList<File>();
		for (File f : uniqueFiles) {
			
			if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
				boolean passesAdditionSearchString = true;
				for (String s : additionalSearchStrings) {
//					logger.info("looking for " + s + "  in  " + f.getPath());
					if ( ! f.getPath().contains(s)) {
						passesAdditionSearchString = false;
						break;
					}
				}
				
				if (passesAdditionSearchString) orderedUniqueFiles.add(f);
			} else {
				orderedUniqueFiles.add(f);
			}
		}
		
		Collections.sort(orderedUniqueFiles, FileUtils.FILE_COMPARATOR);
		
		logger.info("No of unique and filtered files: " + orderedUniqueFiles.size());
		// setup lost file ratios - in thread safe collection
		lostFileRatios = new ConcurrentHashMap<ChrPosition, double[]>(SignatureUtil.loadSignatureRatios(new File(cmdLineInputFiles[0])));
		
		
		// first of all, just check against same donor qsig files
		String donor = DonorUtils.getDonorFromString(cmdLineInputFiles[0]);
		Map<File, Map<ChrPosition, double[]>> donorSnpChipData = SignatureUtil.getDonorSnpChipData(donor, orderedUniqueFiles);
		
		if ( ! donorSnpChipData.isEmpty()) {
			logger.info("Will check against donor specific qsig files first - if no match is found, will widen the search");
		
			StringBuilder donorSB = new StringBuilder(); 
			donorSB.append("comparing against: \n");
			for (File file : donorSnpChipData.keySet()) {
				donorSB.append(file.getAbsolutePath() + "\n");
			}
			double [] donorSpecifResults = new double[donorSnpChipData.size()];
			int i = 0;
			for (Entry<File, Map<ChrPosition, double[]>> entry : donorSnpChipData.entrySet()) {
				
				double [] totalDiffs = QSigCompareDistance.compareRatios(lostFileRatios, entry.getValue());
				final double avg = totalDiffs[0] / totalDiffs[1];
				donorSpecifResults[i++] = avg;
			}
			donorSB.append(new File(cmdLineInputFiles[0]).getName() + " : ");
			boolean allMatches = true;
	
			StringBuilder sb = new StringBuilder();
			for (int j = 0 ; j < i ; j++) {
				if (donorSpecifResults[j] > cutoff || donorSpecifResults[j] > 0.0) allMatches = false;
				if (sb.length() > 0) sb.append(",");
				sb.append(SignatureUtil.nf.format(donorSpecifResults[j]));
			}
			donorSB.append(allMatches ? "" : "INVESTIGATE: ");
			donorSB.append(sb.toString());
			donorSB.append("\n");
			
			logger.info(donorSB.toString());
			if (allMatches) return exitStatus;
			logger.info("No match found - will search against all snp chip data files");
		}
		
		int noOfProcessedFiles = 0;
		for (File f : orderedUniqueFiles) {
			logger.debug("Will compare against: " + f.getAbsolutePath());
			
			Map<ChrPosition, double[]> ratios = SignatureUtil.loadSignatureRatios(f);
			double [] totalDiffs = QSigCompareDistance.compareRatios(lostFileRatios, ratios);
			final double avg = totalDiffs[0] / totalDiffs[1];
			results.put(f.getAbsolutePath(), avg);
			
			logger.info(f.getAbsolutePath() + " : " + avg + ", sum: " + totalDiffs[0] + ", count: " + (int)totalDiffs[1]);
		
			if (++noOfProcessedFiles % 100 == 0) {
				logger.info("hit " + noOfProcessedFiles + " files");
			}
		}
		
		Map<Double, String> sortedMap = new HashMap<Double, String>();
		
		
		// interrogate results
		double mean = 0.0;
		for (Map.Entry<String, Double> entry : results.entrySet()) {
			double result = entry.getValue();
			mean += result;
			if (sortedMap.containsKey(result)) {
				sortedMap.put(result, sortedMap.get(result) + ";" + entry.getKey());
			} else {
				sortedMap.put(result, entry.getKey());
			}
		}
		
		logger.info("SUMMARY:");
		List<Double> sortedList = new ArrayList<Double>(sortedMap.keySet());
		Collections.sort(sortedList);
		for (Double result : sortedList) {
			logger.info(DonorUtils.getDonorFromString(sortedMap.get(result)) + " : " +result);
		}
		
		mean /= results.size();
		logger.info("mean: " + mean);
		
		logger.info("SUMMARY - POTENTIAL MATCHES:");
		// interrogate results
		String mainDonor = DonorUtils.getDonorFromString(cmdLineInputFiles[0]);
		boolean potentialMatchFound = false;
		StringBuilder emailContent = new StringBuilder();
		for (Map.Entry<String, Double> entry : results.entrySet()) {
			double result = entry.getValue();
			
			if (result < cutoff) {
				potentialMatchFound = true;
				String fileDonor = DonorUtils.getDonorFromString(entry.getKey());
				if (mainDonor.equals(fileDonor)) {
					logger.info("CORRECT MATCH : " + fileDonor + " : " +result);
				}else {
					logger.info("INCORRECT MATCH : " + fileDonor + " : " +result);
					emailContent.append("INCORRECT MATCH : " + fileDonor + " : " +result);
				}
				
//				// check donors
//				if ( ! mainDonor.equals(fileDonor)) {
//					logger.warn("Donors don't match! Suspicious donor: " + mainDonor + ", file donor: " + fileDonor);
//				}
			}
		}
		
		if ( ! potentialMatchFound) {
			logger.warn("No potential matches were found!");
			emailContent.append("No potential matches were found!");
		}
		
		if (emailContent.length() > 0) {
			// send email
			email(emailContent.toString());
		}
		
		double totalSumOfSquares = 0.0;
		for (Map.Entry<String, Double> entry : results.entrySet()) {
			double result = entry.getValue();
			
			totalSumOfSquares += Math.pow(result - mean, 2.0);
		}
		logger.info("totalSumOfSquares : " + totalSumOfSquares);
		
		double subSumOfSquares = 0.0;
		for (Map.Entry<String, Double> entry : results.entrySet()) {
			double result = entry.getValue();
			
			if (result > cutoff) subSumOfSquares += Math.pow(result - mean, 2.0);
		}
		logger.info("subSumOfSquares : " + subSumOfSquares);
		double subSumOfSquaresTimes2 = (subSumOfSquares * 2.5);
		logger.info("subSumOfSquares * 2.5 : " + subSumOfSquaresTimes2);
		
//		if (subSumOfSquaresTimes2 < totalSumOfSquares) {
//			logger.info("CORRECT MATCH FOUND");
//		} else {
//			logger.info("INCORRECT MATCH FOUND - INVESTIGATE");
//		}
		
		return exitStatus;
	}
	
	private void email(String message) throws IOException {
		Process p = new ProcessBuilder("echo"  , "\"" + message + "\" | mail -s " + cmdLineInputFiles[0] + " o.holmes@imb.uq.edu.au").start();
	}
	
//	private Map<ChrPosition, double[]> loadSignatureRatios(File file) throws Exception {
//		
//		TabbedFileReader reader = new TabbedFileReader(file);
//		Map<ChrPosition, double[]> ratios = null;
//		int zeroCov = 0, invalidRefCount = 0;
//		try {
//			logger.debug("loading ratios from file: " + file.getAbsolutePath());
//			
//			ratios = new HashMap<ChrPosition, double[]>();
//			
//			for (TabbedRecord vcfRecord : reader) {
//				String[] params = TabTokenizer.tokenize(vcfRecord.getData());
//				ChrPosition chrPos = new ChrPosition(params[0], Integer.parseInt(params[1]));
//				
//				String coverage = params[7];
//				
//				// only populate ratios with non-zero values
//				// attempt to keep memory usage down...
//				if (SignatureUtil.EMPTY_COVERAGE.equals(coverage))  {
//					zeroCov++;
//					continue;
//				}
//				
//				char ref = params[3].charAt(0);
//				if ( ! BaseUtils.isACGTN(ref)) {
////					if ('-' != ref && '.' != ref)
////						logger.info("invalid reference base for record: " + Arrays.deepToString(params));
//					invalidRefCount++;
//					continue;
//				}
//				
//				double[] array = QSigCompare.getDiscretisedValuesFromCoverageString(coverage);
//				if (null != array)
//					ratios.put(chrPos, array);
//			}
//		} finally {
//			reader.close();
//		}
////		logger.info("zero cov count: " + zeroCov + ", ratios count: " + ratios.size() + ", invalid ref count: " + invalidRefCount);
//		return ratios;
//	}
	
	public static void main(String[] args) throws Exception {
		QSigFindBamDistance sp = new QSigFindBamDistance();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running QSigFindPatientDistance:", e);
			else {
				System.err.println("Exception caught whilst running QSigFindPatientDistance: " + e.getMessage());
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
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logger = QLoggerFactory.getLogger(QSigFindBamDistance.class, options.getLog(), options.getLogLevel());
			
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
			//TODO implement ability to search across multiple directories
			String[] paths = options.getDirNames(); 
			if (null != paths && paths.length > 0) {
				path = paths[0];
			}
			
			if (options.hasCutoff())
				cutoff = options.getCutoff();
			
			if (options.hasSearchSuffixOption())
				searchSuffix = options.getSearchSuffix();
			
			if (options.hasAdditionalSearchStringOption())
				additionalSearchStrings = options.getAdditionalSearchString();
			
			logger.logInitialExecutionStats("QSigFindPatientDistance", QSigFindBamDistance.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
