/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * Attempts to find a home for some lost signature files.
 * Takes an orphaned signature file(s) as input, along with a path to search for other signature files.
 * Compares the signature files and lists those that match the closest.
 * 
 * 
 * @author o.holmes
 *
 */
public class SignatureFindRelatedMulti {
	
	private static QLogger logger;
	private int exitStatus;
	
//	private ConcurrentMap<ChrPosition, double[]> lostPatientRatio = new ConcurrentHashMap<>();
	private final ConcurrentMap<File, ConcurrentMap<ChrPosition, double[]>> lostPatientRatios = new ConcurrentHashMap<>();
	
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
	
//	private String email;
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
			// try again this time using just the bams rather than snp chips
			logger.info("no files found using " + snpChipSearchSuffix + ", will try again using: " +  searchSuffix);
			orderedSnpChipFiles = SignatureUtil.populateSnpChipFilesList(path, searchSuffix, excludes, additionalSearchStrings);
			
			if (orderedSnpChipFiles.isEmpty()) {
				logger.warn("No snp chip signature files found to use in the comparison based on path: " + path + ", and searchSuffix: " + searchSuffix + ", and additional search string: " + Arrays.deepToString(additionalSearchStrings));
				exitStatus = 1;
				return exitStatus;
			}
		}
		
		
		logger.info("Running comparison for the following file(s): "); 
		for (String s : cmdLineInputFiles) {
			logger.info(s);
			// check that it is not in the excludes
			for (String exclude : excludes) {
				if (s.startsWith(exclude)) {
					logger.warn("file being examined is in the excludes file list!!! " + s);
				}
			}
			File f = new File(s);
			ConcurrentMap<ChrPosition, double[]> ratios = new ConcurrentHashMap<>(SignatureUtil.loadSignatureRatios(f));
			
			logger.info( (ratios.size() < 1000 ? "low" : "") +  "coverage (" + ratios.size() + ") for file " + s);
			
			lostPatientRatios.put(f, ratios);
			
		}
		logger.info(""); 
		
		for (File f : orderedSnpChipFiles) {
			inputQueue.add(f);
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
					
					Map<ChrPosition, double[]> ratios = lostPatientRatios.get(f);
					if (null == ratios) {
						try {
							ratios = SignatureUtil.loadSignatureRatios(f);
						} catch (Exception e) {
							e.printStackTrace();
							break;
						}
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
					
					List<Comparison> comps = QSigCompareDistance.compareRatios(ratios, f, lostPatientRatios, positionsOfInterest);
					outputQueue.addAll(comps);
					
				}
			}
		});
		service.shutdown();
		if ( ! service.awaitTermination(100, TimeUnit.HOURS)) {
			
			logger.info("Timed out getting data from threads");
			return -1;
		}
		
		
		List<Comparison> comparisons = new ArrayList<>(outputQueue);
		writeResults(comparisons);
		
		return exitStatus;
	}
	
	
	private void writeResults(List<Comparison> comps) throws IOException {
		// for each file in the map, emit an output file with the corresponding comparisons
		
		for (File f : lostPatientRatios.keySet()) {
			List<Comparison> fileSpecificComparisons = new ArrayList<>();
			
			// get all comparisons where this file is the main one
			for (Comparison comp : comps) {
				if (comp.getMain().equals(f)) {
					fileSpecificComparisons.add(comp);
				}
			}
			
			if (fileSpecificComparisons.isEmpty()) {
				continue;
			}
			
			Collections.sort(fileSpecificComparisons);
			
			Path outputFile = Paths.get(f.getAbsolutePath().substring(0, f.getAbsolutePath().indexOf(".vcf")) + ".report.txt");
			if ( ! Files.exists(outputFile)) {
				Files.createFile(outputFile);
			}
			
			logger.info("will write to " + outputFile.toString());
			
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
			
				writer.write("found " + fileSpecificComparisons.size() + " comparisons for file: " + f.getName());
				writer.newLine();
				
				double mean = 0.0;
				writer.write("SUMMARY:");
				writer.newLine();
				for (Comparison comp : fileSpecificComparisons) {
					double result = comp.getScore();
					mean += result;
					writer.write(comp.toSummaryString());
					writer.newLine();
				}
				
				mean /= fileSpecificComparisons.size();
				writer.write("mean: " + mean);
				writer.newLine();
				
				writer.write("SUMMARY - POTENTIAL MATCHES:");
				writer.newLine();
				writer.write("CUTOFF RESULTS (" + cutoff + "): ");
				writer.newLine();
				// interrogate results
				String mainDonor = DonorUtils.getDonorFromFilename(f.getAbsolutePath());
				boolean potentialMatchFound = false;
				for (Comparison comp : fileSpecificComparisons) {
					double result = comp.getScore();
					
					if (result < cutoff) {
						potentialMatchFound = true;
						String fileName = comp.getTest();
						String fileDonor = DonorUtils.getDonorFromFilename(fileName);
						String output = fileDonor + " : " + fileName + " : " +result;
						if (mainDonor.equals(fileDonor)) {
							writer.write("CORRECT MATCH : " + output);
							writer.newLine();
						} else {
							writer.write("INCORRECT MATCH : " + output);
							writer.newLine();
						}
					}
				}
				writer.write("CUTOFF RESULTS (" + cutoff + ") - END");
				writer.newLine();
				
				double irqMultiplier = 2.0;
				List<Number> quartileMatches = Quartile.getPassingValuesString(fileSpecificComparisons, irqMultiplier);
				if ( ! quartileMatches.isEmpty()) {
					potentialMatchFound = true;
					writer.write("QUARTILE RESULTS (IRQ multiplier: " + irqMultiplier + "): ");
					writer.newLine();
					for (Number n : quartileMatches) {
						
						// get entry in sortedList
						for (Comparison comp : fileSpecificComparisons) {
							if (n.doubleValue() == comp.getScore()) {
								writer.write(comp.toSummaryString());
								writer.newLine();
								break;
							}
						}
					}
					writer.write("QUARTILE RESULTS (IRQ multiplier: " + irqMultiplier + ") - END");
					writer.newLine();
				}
				
				if ( ! potentialMatchFound) {
					writer.write("WARNING: No potential matches were found!");
					writer.newLine();
				}
				
				
				double totalSumOfSquares = 0.0;
				for (Comparison comp : fileSpecificComparisons) {
					double result = comp.getScore();
					
					totalSumOfSquares += FastMath.pow(result - mean, 2.0);
				}
				writer.write("totalSumOfSquares : " + totalSumOfSquares);
				writer.newLine();
				
				double subSumOfSquares = 0.0;
				for (Comparison comp : fileSpecificComparisons) {
					double result = comp.getScore();
					
					if (result > cutoff) subSumOfSquares += FastMath.pow(result - mean, 2.0);
				}
				writer.write("subSumOfSquares : " + subSumOfSquares);
				writer.newLine();
				double subSumOfSquaresTimes2 = (subSumOfSquares * 2.5);
				writer.write("subSumOfSquares * 2.5 : " + subSumOfSquaresTimes2);
				writer.newLine();
				
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(SignatureFindRelatedMulti.class);
		
		SignatureFindRelatedMulti sp = new SignatureFindRelatedMulti();
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
			logger = QLoggerFactory.getLogger(SignatureFindRelatedMulti.class, logFile, options.getLogLevel());
			
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
			
			if (options.hasExcludeVcfsFileOption())
				excludeVcfsFile = options.getExcludeVcfsFile();
			
			if (options.getNoOfThreads() > 0) nThreads = options.getNoOfThreads(); 
			
			String [] positions = options.getPositions();
			if (null != positions) {
				positionsOfInterest = ChrPositionUtils.getChrPointPositionsFromStrings(positions);
			}
			
			logger.logInitialExecutionStats("SignatureFindRelated", SignatureFindRelatedMulti.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
