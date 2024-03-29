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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;

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
 * It will also perform comparisons for old and new styled qsig xml files.
 * In doing so, it will forgo the sigmeta check where there is no sigmeta data for one of the comparisons.
 * Where both files have valid sigmeta data, the check will still be performed. Make sense? 
 * 
 * An xml output file is produced
 * If any comparison scores are less than the cutoff, they are added to a list, which is then emailed to interested parties informing them of the potential problem files
 *  
 * @author o.holmes
 * 
 *  @Deprecated use Compare instead
 *
 */
@Deprecated
public class CompareGenotypeBespoke {
	
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
	
	private final Map<File, Pair<SigMeta,TIntByteHashMap>> cache = new THashMap<>();
	
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
			uniqueFiles.addAll(FileUtils.findFilesEndingWithFilterNIO(path, SignatureUtil.QSIG_VCF_GZ));
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
		final int numberOfComparisons = ((numberOfFiles * (numberOfFiles -1)) /2);
		logger.info("Should have " +numberOfComparisons + " comparisons, based on " + numberOfFiles + " input files");
		
		files.sort(FileUtils.FILE_COMPARATOR);
		
		// add files to map
		addFilesToMap(files);
		
		StringBuilder sb = new StringBuilder();
		
		int size = files.size();
		
		for (int i = 0 ; i < size -1 ; i++) {
			
			File f1 = files.get(i);
			Pair<SigMeta, TIntByteHashMap> ratios1 = getSignatureData(f1);
			
			for (int j = i + 1 ; j < size ; j++) {
				File f2 = files.get(j);
				Pair<SigMeta, TIntByteHashMap> ratios2 = getSignatureData(f2);
				
				/*
				 * Check that the 2 files were born of the same snp positions file and have had the same filters applied
				 */
				if (SigMeta.suitableForComparison(ratios1.getKey(), ratios2.getKey())) {
//					logger.info("SigMeta matches: " + ratios1.getKey() + " and " + ratios2.getKey());
					Comparison comp = ComparisonUtil.compareRatiosUsingSnpsFloat(ratios1.getValue(), ratios2.getValue(), f1, f2);
					sb.append(comp.toString()).append(Constants.NEW_LINE);
					allComparisons.add(comp);
				} else {
					logger.warn("Could not compare " + f1.getAbsolutePath() + " and " + f2.getAbsolutePath() + " as their SigMeta information was not equal or not valid: " + ratios1.getKey() + " and " + ratios2.getKey());
				}
			}
			
			// can now remove f1 from the cache as it is no longer required
			Pair<SigMeta,TIntByteHashMap> m = cache.remove(f1);
			m.getSecond().clear();
			m = null;
		}
		
		for (Comparison comp : allComparisons) {
			if (comp.getScore() < cutoff) {
				suspiciousResults.add(comp.toSummaryString());
			}
		}
		
		
		// flush out last donor details
		logger.info(sb.toString());
		
		logger.info("");
		if (suspiciousResults.isEmpty()) {
			logger.info("No suspicious results found");
		} else {
			logger.info("Suspicious results SUMMARY:");
			for (String s : suspiciousResults) {
				logger.info(s);
			}
		}
		
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

	
	public static void main(String[] args) throws Exception {
		CompareGenotypeBespoke sp = new CompareGenotypeBespoke();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running CompareGenotypeBespoke:", e);
			else {
				System.err.println("Exception caught whilst running CompareGenotypeBespoke: " + e.getMessage());
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
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(CompareGenotypeBespoke.class, logFile, options.getLogLevel());
			
			
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
			
			if (options.hasExcludeVcfsFileOption()) {
				excludeVcfsFile = options.getExcludeVcfsFile();
			}
			
			logger.logInitialExecutionStats("CompareGenotypeBespoke", CompareGenotypeBespoke.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
