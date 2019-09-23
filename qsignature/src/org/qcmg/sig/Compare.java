/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.model.SigMeta;
import org.qcmg.sig.util.ComparisonUtil;
import org.qcmg.sig.util.SignatureUtil;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * This class gets a list of all .qsig.vcf files from the supplied path.
 * It then performs a comparison between them all, regardless of whether they are bam or snp chip files
 * 
 * If the SigMeta for the files being compared are eligible for comparison, then they will be compared.
 * If one (or both) of the SigMeta's is inValid, they will still be compared.
 * If both SigMetas are valid, but not eligible for comparison, then they will not be compared.
 * This is to get around the issue where we have some data sets sporting the 
 * new "bespoke" vcf format, and others with the "traditional" vcf format.
 * 
 * An xml output file is produced.
 *  
 * @author o.holmes
 *
 */
public class Compare {
	
	private static QLogger logger;
	private int exitStatus;
	
	private int nThreads = 1;
	
	private float cutoff = 0.95f;
	private int minimumCoverage = SignatureUtil.MINIMUM_COVERAGE;
	private int minimumRGCoverage = SignatureUtil.MINIMUM_RG_COVERAGE;
	
	private String outputXml;
	private String [] paths;
	private String [] additionalSearchStrings;
	
	private String excludeVcfsFile;
	private List<String> excludes;
	private String logFile;
	
	private final Map<String, int[]> fileIdsAndCounts = new THashMap<>();
	private final List<Comparison> allComparisons = new CopyOnWriteArrayList<>();
	
	private final ConcurrentMap<File, Pair<SigMeta,TIntByteHashMap>> cache = new ConcurrentHashMap<>();
	
	List<String> suspiciousResults = new ArrayList<>();
	
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
		
		logger.info("Total number of files to be compared: " + files.size());
		// remove excluded files
		files = SignatureUtil.removeExcludedFilesFromList(files, excludes);
		logger.info("Total number of files to be compared (minus excluded files): " + files.size());
		
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
		
		
		final int numberOfFiles = files.size();
		final int numberOfComparisons = ((numberOfFiles * (numberOfFiles - 1)) / 2);
		logger.info("Should have " +numberOfComparisons + " comparisons, based on " + numberOfFiles + " input files");
		
		files.sort(FileUtils.FILE_COMPARATOR);
		
		// add files to map
		addFilesToMap(files);
		
		populateCache(files);
		
		logger.info("number of entries in cache: " + cache.size());
		
		performComparisons(files);
		
		if (outputXml != null) {
			SignatureUtil.writeXmlOutput(fileIdsAndCounts, allComparisons, outputXml);
		}
		
		return exitStatus;
	}
	
	Pair<SigMeta, TIntByteHashMap> getSignatureData(File f) throws IOException {
		// check map to see if this data has already been loaded
		// if not - load
		Pair<SigMeta, TIntByteHashMap> result = cache.get(f);
		if (result == null) {
			Pair<SigMeta, TMap<String, TIntByteHashMap>> rgResults = SignatureUtil.loadSignatureGenotype(f, minimumCoverage, minimumRGCoverage);
			
			/*
			 * if we have multiple rgs (more than 2 entries in map) - perform comparison on them before adding overall ratios to cache
			 * 
			 */
			if ( rgResults.getSecond().size() == 2) {
				result = new Pair<>(rgResults.getKey(), rgResults.getSecond().get("all"));
			} else {
				/*
				 * remove all from map
				 */
				result =new Pair<>(rgResults.getKey(), rgResults.getSecond().remove("all"));
				
				List<String> rgs = new ArrayList<>(rgResults.getSecond().keySet());
				for (int i = 0 ; i < rgs.size() ; i++) {
					String rg1 = rgs.get(i);
					TIntByteHashMap r1 = rgResults.getSecond().get(rg1);
					for (int j = i + 1 ; j < rgs.size() ; j++) {
						String rg2 = rgs.get(j);
						TIntByteHashMap r2 = rgResults.getSecond().get(rg2);
						
						Comparison c = ComparisonUtil.compareRatiosUsingSnpsFloat(r1, r2, new File(rg1), new File(rg2));
						if (c.getScore() < cutoff) {
							logger.warn("rgs don't match!: " + c.toString());
						}
					}
				}
			}
			
			if (result.getValue().size() < 1000) {
				logger.warn("low coverage (" + result.getValue().size() + ") for file " + f.getAbsolutePath());
			}
			
			cache.put(f, result);
			fileIdsAndCounts.get(f.getAbsolutePath())[1] = result.getValue().size();
		}
		return result;
	}
	
	private void addFilesToMap(List<File> orderedFiles) {
		int id = 1;
		for (File f : orderedFiles) {
			fileIdsAndCounts.put(f.getAbsolutePath(), new int[]{id++, -1, -1});
		}
	}
	
	private void performComparisons(List<File> files) {
		int size = files.size();
		AbstractQueue<Integer> queue =  new ConcurrentLinkedQueue<>();
		for (int i = 0 ; i < size - 1 ; i++) {
			queue.add(i);
		}
		
		ExecutorService service = Executors.newFixedThreadPool(nThreads);		
		for (int i = 0 ; i < nThreads; i++) {
			service.execute(() -> {
					List<Comparison> myComps = new ArrayList<>();
					Integer in;
					while ((in = queue.poll()) != null) {
				
						logger.info("performing comparison for : " + in.intValue());
						
						File f1 = files.get(in);
						Pair<SigMeta, TIntByteHashMap> r1 = cache.get(f1);
						
						for (int j = in.intValue() + 1 ; j < size ; j ++ ) {
							File f2 = files.get(j);
							Pair<SigMeta, TIntByteHashMap> r2 =  cache.get(f2);
							
							
							/*
							 * If both sig metas are valid, check to see if they are suitable for comparison (same snp positions file and have had the same filters applied).
							 * If one is invalid, perform comparison anyway, as we will be dealing with the traditional format here
							 */
							if ( ! r1.getKey().isValid() || ! r2.getKey().isValid() || SigMeta.suitableForComparison(r1.getKey(), r2.getKey())) {
//								logger.info("SigMeta matches: " + ratios1.getKey() + " and " + ratios2.getKey());
								Comparison comp = ComparisonUtil.compareRatiosUsingSnpsFloat(r1.getValue(), r2.getValue(), f1, f2);
								myComps.add(comp);
							} else {
								logger.warn("Could not compare " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + " as their SigMeta information was not equal or not valid: " + r1.getKey() + " and " + r2.getKey());
							}
						}
					}
					/*
					 * add myComps to allComps
					 */
					allComparisons.addAll(myComps);
				});
		}
		service.shutdown();
		try {
			if ( ! service.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
				logger.info("Timed out getting data from threads");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void populateCache(List<File> files) {
		AbstractQueue<File> queue =  new ConcurrentLinkedQueue<>(files);
		ExecutorService service = Executors.newFixedThreadPool(nThreads);		
		for (int i = 0 ; i < nThreads; i++) {
			service.execute(() -> {
					File f;
					while ((f = queue.poll()) != null) {
				
						logger.info("loading data from: " + f.getAbsolutePath());
						
						Pair<SigMeta, TIntByteHashMap> genotypes = null;
						try {
							genotypes = getSignatureData(f);
						} catch (Exception e) {
							/*
							 * set exit status, log exception and re-throw
							 */
							exitStatus = 1;
							e.printStackTrace();
							try {
								throw e;
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
						Pair<SigMeta, TIntByteHashMap> prevGenotypes = cache.putIfAbsent(f, genotypes);
						if (null != prevGenotypes) {
							logger.warn("already genotypes associated with file: " + f.getAbsolutePath());
						}
					}
				});
		}
		service.shutdown();
		try {
			if ( ! service.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS)) {
				logger.info("Timed out getting data from threads");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Compare.class);
		
		Compare sp = new Compare();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running Compare:", e);
			else {
				System.err.println("Exception caught whilst running Compare: " + e.getMessage());
				System.err.println(Messages.COMPARE_USAGE);
			}
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
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
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(Compare.class, logFile, options.getLogLevel());
			
			
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
			options.getNoOfThreads().ifPresent(i -> {nThreads = i.intValue();});
			options.getMinRGCoverage().ifPresent(i -> {minimumRGCoverage = i.intValue();});
			logger.tool("Number of threads to use: " + nThreads);
			logger.tool("Setting minumim coverage to: " + minimumCoverage);
			logger.tool("Setting minumim RG coverage to: " + minimumRGCoverage);
			
			additionalSearchStrings = options.getAdditionalSearchString();
			logger.tool("Setting additionalSearchStrings to: " + Arrays.deepToString(additionalSearchStrings));
			
			if (options.hasExcludeVcfsFileOption())
				excludeVcfsFile = options.getExcludeVcfsFile();
			
			logger.logInitialExecutionStats("Compare", Compare.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
