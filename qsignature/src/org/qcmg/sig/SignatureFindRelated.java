/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.util.FastMath;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.util.Quartile;
import org.qcmg.sig.util.SignatureUtil;

/**
 * Attempts to find a home for a lost signature file.
 * Takes an orphaned signature file as input, along with a path to search for other signature files.
 * Compares the signature files and lists those that match the closest.
 * 
 * 
 * @author o.holmes
 *
 */
public class SignatureFindRelated {
	
	private static QLogger logger;
	private int exitStatus;
	
	private ConcurrentMap<ChrPosition, double[]> lostPatientRatio = new ConcurrentHashMap<>();
	
	private List<File> orderedSnpChipFiles;
	
	private float cutoff = 0.2f;
	
	private String[] cmdLineInputFiles;
	private String path;
	private String searchSuffix = ".qsig.vcf";
	private String snpChipSearchSuffix = ".txt.qsig.vcf";
	private String [] additionalSearchStrings;
	
	private String excludeVcfsFile;
	private List<String> excludes;
	
	private Map<ChrPosition, ChrPosition>  positionsOfInterest;
	
	private String email;
	private String logFile;
	
	private final AtomicInteger counter = new AtomicInteger();
	
	private int nThreads = 2;	// defaults to 2 
	
	private final AbstractQueue<File> inputQueue = new ConcurrentLinkedQueue<>();
	private final AbstractQueue<Comparison> outputQueue = new ConcurrentLinkedQueue<>();
	
	private int engage() throws Exception {
		
		// get excludes
		excludes = SignatureUtil.getEntriesFromExcludesFile(excludeVcfsFile);
		
		orderedSnpChipFiles = SignatureUtil.populateSnpChipFilesList(path, snpChipSearchSuffix, excludes, additionalSearchStrings);
		
		if (orderedSnpChipFiles.isEmpty()) {
			logger.warn("No snp chip signature files found to use in the comparison based on path: " + path + ", and snpChipSearchSuffix: " + snpChipSearchSuffix + ", and additional search string: " + Arrays.deepToString(additionalSearchStrings));
			exitStatus = 1;
			return exitStatus;
		}
		
		final File patientQsigVcfFile = new File(cmdLineInputFiles[0]);
		// check that bam file is not in failedQCRuns list
		for (String exclude : excludes) {
			if (patientQsigVcfFile.getName().startsWith(exclude)) {
				logger.warn("file being examined is in the excludes file list!!!");
			}
		}
		
		// load ratios for bam in question
		lostPatientRatio = new ConcurrentHashMap<>(SignatureUtil.loadSignatureRatios(patientQsigVcfFile));
		if (lostPatientRatio.size() < 1000) {
			logger.warn("low coverage (" + lostPatientRatio.size() + ") for file " + cmdLineInputFiles[0]);
		} else {
			logger.info("coverage (" + lostPatientRatio.size() + ") for file " + cmdLineInputFiles[0]);
		}
		
		logger.info("Running comparison for file: " + patientQsigVcfFile.getAbsolutePath() + " against all snp chip qsig vcf files!");
		
		for (File f : orderedSnpChipFiles) {
			inputQueue.add(f);
//			logger.debug("Will compare against: " + f.getAbsolutePath());
//			
//			Map<ChrPosition, double[]> ratios = SignatureUtil.loadSignatureRatios(f);
//			if (ratios.size() < 1000) logger.warn("low coverage (" + ratios.size() + ") for file " + f.getAbsolutePath());
//			
//			Comparison comp = QSigCompareDistance.compareRatios(lostPatientRatio, ratios, patientQsigVcfFile, f, positionsOfInterest);
//			comparisons.add(comp);
//			
//			logger.info(comp.toString());
//		
//			if (++noOfProcessedFiles % 100 == 0) {
//				logger.info("hit " + noOfProcessedFiles + " files");
//			}
		}
		
		ExecutorService service = Executors.newFixedThreadPool(nThreads);		
		for (int i = 0 ; i < nThreads; i++)
		service.execute(new Runnable() {

			@Override
			public void run() {
				while (true) {
					File f = inputQueue.poll();
					if (null == f) break;
					
					if (counter.incrementAndGet() % 100 == 0) {
						logger.info("hit " + counter.get() + " files");
					}
					logger.debug("Will compare against: " + f.getAbsolutePath());
					
					Map<ChrPosition, double[]> ratios = null;
					try {
						ratios = SignatureUtil.loadSignatureRatios(f);
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
					if (null == ratios)
						try {
							throw new Exception("Got null ratios");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					if (ratios.size() < 1000) logger.warn("low coverage (" + ratios.size() + ") for file " + f.getAbsolutePath());
					
					Comparison comp = QSigCompareDistance.compareRatios(lostPatientRatio, ratios, patientQsigVcfFile, f, positionsOfInterest);
					outputQueue.add(comp);
					
				}
			}
		});
		service.shutdown();
		if ( ! service.awaitTermination(100, TimeUnit.HOURS)) {
			
			logger.info("Timed out getting data from threads");
			return -1;
		}
		
		
//		List<String> sortedList = new ArrayList<String>();
//		for (Comparison comp : comparisons) {
////			sortedList.add(result + " : " + comp.getTest().getAbsolutePath());
//		}
		
		// interrogate results
		List<Comparison> comparisons = new ArrayList<>(outputQueue);
		Collections.sort(comparisons);
		
		double mean = 0.0;
		logger.info("SUMMARY:");
		for (Comparison comp : comparisons) {
			double result = comp.getScore();
			mean += result;
			logger.info(comp.toSummaryString());
		}
		
		mean /= comparisons.size();
		logger.info("mean: " + mean);
		
		logger.info("SUMMARY - POTENTIAL MATCHES:");
		logger.info("CUTOFF RESULTS (" + cutoff + "): ");
		// interrogate results
		String mainDonor = DonorUtils.getDonorFromFilename(cmdLineInputFiles[0]);
		boolean potentialMatchFound = false;
		StringBuilder emailContent = new StringBuilder();
		for (Comparison comp : comparisons) {
			double result = comp.getScore();
			
			if (result < cutoff) {
				potentialMatchFound = true;
				String fileName = comp.getTest();
				String fileDonor = DonorUtils.getDonorFromFilename(fileName);
				String output = fileDonor + " : " + fileName + " : " +result;
				if (mainDonor.equals(fileDonor)) {
					logger.info("CORRECT MATCH : " + output);
				} else {
					logger.info("INCORRECT MATCH : " + output);
					emailContent.append("INCORRECT MATCH : " + output);
				}
			}
		}
		logger.info("CUTOFF RESULTS (" + cutoff + ") - END");
		
		double irqMultiplier = 2.0;
		List<Number> quartileMatches = Quartile.getPassingValuesString(comparisons, irqMultiplier);
		if ( ! quartileMatches.isEmpty()) {
			potentialMatchFound = true;
			logger.info("QUARTILE RESULTS (IRQ multiplier: " + irqMultiplier + "): ");
			for (Number n : quartileMatches) {
				
				// get entry in sortedList
				for (Comparison comp : comparisons) {
					if (n.doubleValue() == comp.getScore()) {
						logger.info(comp.toSummaryString());
						break;
					}
				}
			}
			logger.info("QUARTILE RESULTS (IRQ multiplier: " + irqMultiplier + ") - END");
		}
		
		if ( ! potentialMatchFound) {
			logger.warn("No potential matches were found!");
			emailContent.append("No potential matches were found!");
		}
		
		if (emailContent.length() > 0) {
			// send email
//			SignatureUtil.sendEmail("Qsignature Comparison: " + cmdLineInputFiles[0] + " vs the world", emailContent.toString(), email, logger);
//			email(emailContent.toString());
		}
		
		double totalSumOfSquares = 0.0;
		for (Comparison comp : comparisons) {
			double result = comp.getScore();
			
			totalSumOfSquares += FastMath.pow(result - mean, 2.0);
		}
		logger.info("totalSumOfSquares : " + totalSumOfSquares);
		
		double subSumOfSquares = 0.0;
		for (Comparison comp : comparisons) {
			double result = comp.getScore();
			
			if (result > cutoff) subSumOfSquares += FastMath.pow(result - mean, 2.0);
		}
		logger.info("subSumOfSquares : " + subSumOfSquares);
		double subSumOfSquaresTimes2 = (subSumOfSquares * 2.5);
		logger.info("subSumOfSquares * 2.5 : " + subSumOfSquaresTimes2);
		
		return exitStatus;
	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(SignatureFindRelated.class);
		
		SignatureFindRelated sp = new SignatureFindRelated();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running SignatureFindRelated:", e);
			else {
				System.err.println("Exception caught whilst running SignatureFindRelated: " + e.getMessage());
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
			logFile = options.getLog();
			// configure logging
			logger = QLoggerFactory.getLogger(SignatureFindRelated.class, logFile, options.getLogLevel());
			
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
			
			if (options.hasEmailOption())
				email = options.getEmail();
			
			if (options.hasExcludeVcfsFileOption())
				excludeVcfsFile = options.getExcludeVcfsFile();
			
			if (options.getNoOfThreads() > 0) nThreads = options.getNoOfThreads(); 
			
			String [] positions = options.getPositions();
			if (null != positions) {
				positionsOfInterest = ChrPositionUtils.getChrPointPositionsFromStrings(positions);
			}
			
			logger.logInitialExecutionStats("SignatureFindRelated", SignatureFindRelated.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
