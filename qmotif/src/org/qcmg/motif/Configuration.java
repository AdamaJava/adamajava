/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Pair;
import org.qcmg.motif.util.IniUtils;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class Configuration {
	private final int numberThreads;
	private final String outputFileName;
	private final String inputGFF3FileName;
	private final String[] bamFileNames;
	private String[] baiFileNames;
	private final File inputGFF3File;
	private final File outputFile;
	private File[] inputBAMFiles;
	private File[] inputBAIFiles;
	private final HashSet<Pair<File, File>> filePairs = new HashSet<Pair<File, File>>();
	private final Options options;
	private final QueryExecutor filter;
	private final Algorithm algorithm;
	private final LoggerInfo loggerInfo;
	private final String validation;
	private final AtomicLong countReadFromInput;
	private final AtomicLong countReadToCoverage;
	private Motifs stageOneMotifs;
	private final String stageOneRegex;
	private Motifs stageTwoMotifs;
	private final String stageTwoRegex;
	private final List<ChrPosition> excludes;
	private final List<ChrPosition> includes;
	private final Integer windowSize;
	private final Integer cutoff;
	private final String outputGffFileName;
	private final String outputBamFileName;
	private final  Ini iniFile;
	private final MotifsAndRegexes mAndR;
	
	public List<ChrPosition> getExcludes() {
		return excludes;
	}
	public List<ChrPosition> getIncludes() {
		return includes;
	}

	private final QLogger logger;

	public Configuration(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;
		
		iniFile = new Ini(new File(options.getIniFile()));
			
		windowSize = Integer.parseInt(IniUtils.getEntry(iniFile, "PARAMS", "window_size"));
		
		stageOneRegex = IniUtils.getEntry(iniFile, "PARAMS", "stage1_motif_regex");
		stageTwoRegex = IniUtils.getEntry(iniFile, "PARAMS", "stage2_motif_regex");
		
		String s1Motifs = IniUtils.getEntry(iniFile, "PARAMS", "stage1_motif_string");
		if (null != s1Motifs) {
			stageOneMotifs = new Motifs(Boolean.parseBoolean(IniUtils.getEntry(iniFile, "PARAMS", "revcomp")), s1Motifs.split(","));
		}
		String s2Motifs = IniUtils.getEntry(iniFile, "PARAMS", "stage2_motif_string");
		if (null != s2Motifs) {
			stageTwoMotifs = new Motifs(Boolean.parseBoolean(IniUtils.getEntry(iniFile, "PARAMS", "revcomp")), s2Motifs.split(","));
		}
		
		mAndR = new MotifsAndRegexes(stageOneMotifs, stageOneRegex, stageTwoMotifs, stageTwoRegex, windowSize);
		algorithm = new MotifCoverageAlgorithm(mAndR);
		
		// get excludes and includes
		includes = IniUtils.getPositions(iniFile, "INCLUDES");
		excludes = IniUtils.getPositions(iniFile, "EXCLUDES");
			
		loggerInfo = new LoggerInfo(options);
		logger = QLoggerFactory.getLogger(Configuration.class);
		
		validation = options.getValidation();

		outputFileName = options.getOutputFileNames()[0];
		outputGffFileName = options.getOutputFileNames().length > 1 ? options.getOutputFileNames()[1] : null;
		outputBamFileName = options.getOutputFileNames().length > 2 ? options.getOutputFileNames()[2] : null;
		inputGFF3FileName = options.getInputGFF3FileNames()[0];
		bamFileNames = options.getBAMFileNames();
		baiFileNames = options.getBAIFileNames();
		inferMissingBaiFileNames();

		if (options.hasNumberThreadsOption()) {
			numberThreads = options.getNumberThreads().intValue();
		} else {
			numberThreads = 1;
		}

		if (options.hasQueryOption()) {
			filter = new QueryExecutor(options.getQuery());
		} else {
			filter = null;
		}
		
		inputGFF3File = new File(inputGFF3FileName);
		instantiateBamFiles();
		instantiateBaiFiles();
		outputFile = new File(outputFileName);
		checkFileExistence();
		inferFilePairings();
		countReadFromInput = new AtomicLong();
		countReadToCoverage = new AtomicLong();
		
		cutoff = Integer.parseInt(IniUtils.getEntry(iniFile, "PARAMS", "cutoff_size"));
	}
	
	Map<ChrPosition, String> getMapFromList(List<ChrPosition> cps) {
		Map<ChrPosition, String> map = new HashMap<>();
		for (ChrPosition cp : cps) {
			map.put(cp, null);
		}
		return map;
	}
	
	public Options getOptions() {
		return options;
	}
	
	public AtomicLong getInputReadsCount(){
		return countReadFromInput;
	}
	
	public AtomicLong getCoverageReadsCount(){
		return countReadToCoverage;
	}
	
	public int getNumberThreads() {
		return numberThreads;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public String getInputGFF3FileName() {
		return inputGFF3FileName;
	}

	public File getInputGFF3File() {
		return inputGFF3File;
	}

	public String[] getBamFileNames() {
		return bamFileNames;
	}

	public String[] getBaiFileNames() {
		return baiFileNames;
	}

	public File[] getInputBAMFiles() {
		return inputBAMFiles;
	}

	public File[] getInputBAIFiles() {
		return inputBAIFiles;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public HashSet<Pair<File, File>> getFilePairs() {
		return filePairs;
	}

	public QueryExecutor getFilter() {
		return filter;
	}
	
	public Algorithm getAlgorithm() {
		return algorithm;
	}

	private void inferMissingBaiFileNames() throws Exception {
		if (null == baiFileNames) {
			inferAllBaiFileNames();
		} else if (0 == baiFileNames.length) {
			inferAllBaiFileNames();
		} else if (baiFileNames.length < bamFileNames.length) {
			throw new Exception(
					"Partial BAI file name lists not yet supported. Provide either none or all BAI file names.");
		} else if (baiFileNames.length > bamFileNames.length) {
			throw new Exception(
					"Too many BAI files specified. Provide either none or a number of BAI file names equal to the number of BAM files.");
		} else {
			// No inferencing necessary: equal number of BAI and BAM file names.
		}
	}

	private void inferAllBaiFileNames() {
		Vector<String> baiFileNameList = new Vector<String>();
		for (String bamFileName : bamFileNames) {
			baiFileNameList.add(bamFileName + ".bai");
		}
		baiFileNames = new String[baiFileNameList.size()];
		baiFileNameList.toArray(baiFileNames);
	}

	private void instantiateBaiFiles() {
		inputBAIFiles = new File[baiFileNames.length];
		int i = 0;
		for (String baiFileName : baiFileNames) {
			inputBAIFiles[i] = new File(baiFileName);
			i++;
		}
	}

	private void instantiateBamFiles() {
		inputBAMFiles = new File[bamFileNames.length];
		int i = 0;
		for (String bamFileName : bamFileNames) {
			inputBAMFiles[i] = new File(bamFileName);
			i++;
		}
	}

	private void inferFilePairings() {
		assert (inputBAMFiles.length == inputBAIFiles.length);
		for (int i = 0; i < inputBAMFiles.length; i++) {
			File bamFile = inputBAMFiles[i];
			File baiFile = inputBAIFiles[i];
			Pair<File, File> filePair = new Pair<>(bamFile, baiFile);
			filePairs.add(filePair);
		}
	}

	private void checkFileExistence() throws Exception {
		for (final File file : inputBAMFiles) {
			if (!file.exists()) {
				throw new Exception("Input BAM file '" + file.getAbsolutePath()
						+ "' does not exist");
			}
		}
		for (final File file : inputBAIFiles) {
			if (!file.exists()) {
				throw new Exception("Input BAI file '" + file.getAbsolutePath()
						+ "' does not exist");
			}
		}
	}

	public LoggerInfo getLoggerInfo() {
		return loggerInfo;
	}

	public String getValidation() {
		return validation;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	public Integer getCutoff() {
		return cutoff;
	}

	public String getOutputGff() {
		return outputGffFileName;
	}
	public String getOutputBam() {
		return outputBamFileName;
	}

	public MotifsAndRegexes getRegex() {
		return mAndR;
	}
}
