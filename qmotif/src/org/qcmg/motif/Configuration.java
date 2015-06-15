/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.ini4j.Ini;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.motif.util.IniUtils;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class Configuration {
	private final int numberThreads;
	private final File outputFile;
	private final HashSet<Pair<File, File>> filePairs = new HashSet<Pair<File, File>>();
	private final QueryExecutor filter;
	private final Algorithm algorithm;
	private final LoggerInfo loggerInfo;
	private final String validation;
	private final AtomicLong countReadFromInput;
	private final AtomicLong countReadToCoverage;
	private final List<ChrPosition> excludes;
	private final List<ChrPosition> includes;
	private final Integer windowSize;
	private final String outputBamFileName;
	private final boolean includesOnlyMode;
	private final  Ini iniFile;
	private final MotifsAndRegexes mAndR;
	
	public List<ChrPosition> getExcludes() {
		return excludes;
	}
	public List<ChrPosition> getIncludes() {
		return includes;
	}

	public Configuration(final Options options) throws Exception {
		options.detectBadOptions();
		
		iniFile = new Ini(new File(options.getIniFile()));
			
		windowSize = Integer.parseInt(IniUtils.getEntry(iniFile, "PARAMS", "window_size"));
		
		String stageOneRegex = IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_1_MOTIF_REGEX);
		String stageTwoRegex = IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_2_MOTIF_REGEX);
		Motifs stageOneMotifs = null;
		Motifs stageTwoMotifs = null;
		
		String s1Motifs = IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_1_MOTIF_STRING);
		if (null != s1Motifs) {
			stageOneMotifs = new Motifs(Boolean.parseBoolean(IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_1_STRING_REV_COMP)), s1Motifs.split(Constants.COMMA_STRING));
		}
		String s2Motifs = IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_2_MOTIF_STRING);
		if (null != s2Motifs) {
			stageTwoMotifs = new Motifs(Boolean.parseBoolean(IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.STAGE_2_STRING_REV_COMP)), s2Motifs.split(Constants.COMMA_STRING));
		}
		
		mAndR = new MotifsAndRegexes(stageOneMotifs, stageOneRegex, stageTwoMotifs, stageTwoRegex, windowSize.intValue());
		algorithm = new MotifCoverageAlgorithm(mAndR);
		
		// get excludes and includes
		includes = IniUtils.getPositions(iniFile, "INCLUDES");
		excludes = IniUtils.getPositions(iniFile, "EXCLUDES");
			
		loggerInfo = new LoggerInfo(options);
		validation = options.getValidation();

		outputBamFileName = options.getOutputFileNames().length > 1 ? options.getOutputFileNames()[1] : null;
		String [] bamFileNames = options.getBAMFileNames();
		String [] baiFileNames = options.getBAIFileNames();
		
		inferFilePairings(bamFileNames, baiFileNames);

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
		
		outputFile = new File(options.getOutputFileNames()[0]);
		checkFileExistence();
		countReadFromInput = new AtomicLong();
		countReadToCoverage = new AtomicLong();
		
		includesOnlyMode = Boolean.getBoolean(IniUtils.getEntry(iniFile, MotifConstants.PARAMS, MotifConstants.INCLUDES_ONLY_MODE));
		
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

	private void inferFilePairings(String [] inputBAMFiles, String [] inputBAIFiles) {
		for (int i = 0; i < inputBAMFiles.length; i++) {
			String bamFile = inputBAMFiles[i];
			String baiFile = null;
			if (null != inputBAIFiles && inputBAIFiles.length > 0) {
				baiFile = inputBAIFiles[i];
			}
			Pair<File, File> filePair = new Pair<>(new File(bamFile), null == baiFile ? null : new File(baiFile));
			filePairs.add(filePair);
		}
	}

	private void checkFileExistence() throws Exception {
		// loop through filePairs
		
		for (Pair<File, File> entry : filePairs) {
			if (null != entry.getLeft()) {
				if ( ! entry.getLeft().exists()) {
					throw new Exception("Input BAM file '" + entry.getLeft().getAbsolutePath() + "' does not exist");
				}
			}
			if (null != entry.getRight()) {
				if ( ! entry.getRight().exists()) {
					throw new Exception("Input BAI file '" + entry.getRight().getAbsolutePath() + "' does not exist");
				}
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

	public String getOutputBam() {
		return outputBamFileName;
	}

	public MotifsAndRegexes getRegex() {
		return mAndR;
	}
	public boolean isIncludesOnlyMode() {
		return includesOnlyMode;
	}
}
