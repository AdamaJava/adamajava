/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.File;
import java.util.HashSet;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class Configuration {
	private final boolean perFeatureFlag;
	private final int numberThreads;
	private final String type;
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
	private final CoverageType coverageType;
	private final QueryExecutor filter;
	private final Algorithm algorithm;
	private final LoggerInfo loggerInfo;
	private final String validation;
	private final ReadsNumberCounter countReadFromInput;
	private final ReadsNumberCounter countReadToCoverage;
	
	private final QLogger logger;

	public Configuration(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;

		type = options.getTypes()[0];
		if (type.equals("sequence") || type.equals("seq")) {
			coverageType = CoverageType.SEQUENCE;
			algorithm = new SequenceCoverageAlgorithm();
		} else if (type.equals("physical") || type.equals("phys")) {
			coverageType = CoverageType.PHYSICAL;
			algorithm = new PhysicalCoverageAlgorithm();
		} else {
			throw new Exception("Unknown coverage type: '" + type + "'");
		}

		loggerInfo = new LoggerInfo(options);
		logger = QLoggerFactory.getLogger(Configuration.class);
		
		validation = options.getValidation();

		outputFileName = options.getOutputFileNames()[0];
		inputGFF3FileName = options.getInputGFF3FileNames()[0];
		bamFileNames = options.getBAMFileNames();
		baiFileNames = options.getBAIFileNames();
		inferMissingBaiFileNames();

		perFeatureFlag = options.hasPerFeatureOption();
		if (options.hasNumberThreadsOption()) {
			numberThreads = options.getNumberThreads()[0];
		} else {
			numberThreads = 1;
		}

		if (options.hasQueryOption()) {
			filter = new QueryExecutor(options.getQuery());
		} else {
			filter = null;
		}
		
		if (options.hasVcfFlag() && ! perFeatureFlag) {
			logger.warn("VCF mode has been selected, however, the per-feature flag has not been set" +
					" - will not be able to create the vcf file");
		}

		inputGFF3File = new File(inputGFF3FileName);
		instantiateBamFiles();
		instantiateBaiFiles();
		outputFile = new File(outputFileName);
		checkFileExistence();
		inferFilePairings();
		countReadFromInput = new ReadsNumberCounter();
		countReadToCoverage = new ReadsNumberCounter();
	}

	public Options getOptions() {
		return options;
	}
	
	public boolean isPerFeatureFlag() {
		return perFeatureFlag;
	}
	
	public ReadsNumberCounter getInputReadsCount(){
		return countReadFromInput;
	}
	
	public ReadsNumberCounter getCoverageReadsCount(){
		return countReadToCoverage;
	}
	
	public int getNumberThreads() {
		return numberThreads;
	}

	public String getType() {
		return type;
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

	public CoverageType getCoverageType() {
		return coverageType;
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
			Pair<File, File> filePair = new Pair<File, File>(bamFile, baiFile);
			filePairs.add(filePair);
		}
	}

	private void checkFileExistence() throws Exception {
		if (!inputGFF3File.exists()) {
			throw new Exception("Input GFF3 feature file does not exist");
		}
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
		if (outputFile.exists()) {
			throw new Exception(
					"An output file of the same name already exists.");
		}
	}

	public LoggerInfo getLoggerInfo() {
		return loggerInfo;
	}

	public String getValidation() {
		return validation;
	}
}
