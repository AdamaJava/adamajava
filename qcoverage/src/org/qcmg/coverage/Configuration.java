/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;
import org.qcmg.qbamfilter.query.QueryExecutor;

public final class Configuration {
	private final int numberThreads;
	private final String type;
	private final String inputGFF3FileName;
	private final String[] bamFileNames;
	private String[] baiFileNames;
	private final File inputGFF3File;
	private File[] inputBAMFiles;
	private File[] inputBAIFiles;
	private final HashSet<Pair<File, File>> filePairs = new HashSet<>();
	private final Options options;
	private final LoggerInfo loggerInfo;
	private final String validation;
	private final ReadsNumberCounter countReadFromInput;
	private final ReadsNumberCounter countReadToCoverage;

	/**
	 * The algorithm to be used for each of the requested coverage types, in the order that
	 * the types were requested on the command line.
	 */
	private final Map<Object, Algorithm> algorithms = new LinkedHashMap<>();
	/**
	 * Whether the coverage should be reported per feature, for each of the coverage types.
	 */
	private final Map<Object, Boolean> perFeatureFlags = new LinkedHashMap<>();
	/**
	 * The filter used to select the reads for each of the coverage types. A coverage type
	 * without a filter is not present in the map.
	 */
	private final Map<Object, QueryExecutor> filters = new LinkedHashMap<>();
	/**
	 * Whether the coverage should be reported per feature, as requested on the command line.
	 */
	private final boolean perFeatureFlag;
	/**
	 * The name of the output file for each of the coverage types.
	 */
	private final Map<Object, String> outputFileNames = new LinkedHashMap<>();

	private final QLogger logger;

	public Configuration(final Options options) throws Exception {
		options.detectBadOptions();
		this.options = options;

		type = options.getTypes()[0];
		final Integer[] lowReadDepthCutoffs = options.getLowReadDepthCutoffs();
		// the query to be used for each of the coverage types, if any
		final Map<Object, String> queriesByType = new LinkedHashMap<>();
		for (final String typeName : options.getTypes()) {
			// all of the low_readdepth coverages share the same query
			final String query = options.getQuery(typeName);
			if ("low_readdepth".equals(typeName)) {
				/*
				 * Each cutoff results in a separate low read depth coverage, so the coverages are
				 * keyed by the position of their cutoff.
				 */
				for (int instance = 0; instance < lowReadDepthCutoffs.length; instance++) {
					final Integer cutoff = lowReadDepthCutoffs[instance];
					final Object key = new CoverageTypeKey(CoverageType.LOW_READDEPTH, instance, cutoff);
					// the low read depth coverage is always reported per feature
					algorithms.put(key, new LowReadDepthAlgorithm(cutoff));
					perFeatureFlags.put(key, true);
					if (null != query && !query.isBlank()) {
						queriesByType.put(key, query);
					}
				}
				continue;
			}
			final CoverageType coverageType;
			final Algorithm algorithm;
			switch (typeName) {
				case "sequence", "seq" -> {
					coverageType = CoverageType.SEQUENCE;
					algorithm = new SequenceCoverageAlgorithm();
				}
				case "physical", "phys" -> {
					coverageType = CoverageType.PHYSICAL;
					algorithm = new PhysicalCoverageAlgorithm();
				}
				default -> throw new Exception("Unknown coverage type: '" + typeName + "'");
			}
			if (algorithms.containsKey(coverageType)) {
				throw new Exception("Duplicate coverage type: '" + typeName + "'");
			}
			// the low read depth coverage is always reported per feature
			final boolean perFeature = options.hasPerFeatureOption();
			algorithms.put(coverageType, algorithm);
			perFeatureFlags.put(coverageType, perFeature);
			if (null != query && !query.isBlank()) {
				queriesByType.put(coverageType, query);
			}
		}

		loggerInfo = new LoggerInfo(options);
		logger = QLoggerFactory.getLogger(Configuration.class);
		
		validation = options.getValidation();

		/*
		 * Each distinct query is only parsed once, so that coverage types sharing a query also
		 * share a filter, and the filter is only executed once for each read.
		 */
		final Map<String, QueryExecutor> queryExecutors = new LinkedHashMap<>();
		for (final Map.Entry<Object, String> e : queriesByType.entrySet()) {
			QueryExecutor filter = queryExecutors.get(e.getValue());
			if (null == filter) {
				filter = new QueryExecutor(e.getValue());
				queryExecutors.put(e.getValue(), filter);
			}
			filters.put(e.getKey(), filter);
		}

		inputGFF3FileName = options.getInputGFF3FileNames()[0];
		bamFileNames = options.getBAMFileNames();
		baiFileNames = options.getBAIFileNames();
		inferMissingBaiFileNames();

		perFeatureFlag = options.hasPerFeatureOption();
		setOutputFileNames(options.getOutputFileNames());

		if (options.hasNumberThreadsOption()) {
			numberThreads = options.getNumberThreads()[0];
		} else {
			numberThreads = 1;
		}

		if (options.hasVcfFlag() && ! perFeatureFlag) {
			logger.warn("VCF mode has been selected, however, the per-feature flag has not been set" +
					" - will not be able to create the vcf file");
		}

		inputGFF3File = new File(inputGFF3FileName);
		instantiateBamFiles();
		instantiateBaiFiles();
		checkFileExistence();
		inferFilePairings();
		countReadFromInput = new ReadsNumberCounter();
		countReadToCoverage = new ReadsNumberCounter();
	}

	/**
	 * Determines the name of the output file to be used for each of the coverage types.
	 * <p>
	 * When a single coverage type is requested, the output file name is used as is, so that
	 * the behaviour is unchanged. When more than one coverage type is requested, either one
	 * output file name is provided for each of the types, or a single name is provided and
	 * the name of the coverage type is added to it.
	 */
	private void setOutputFileNames(final String[] outputNames) {
		if (1 == algorithms.size()) {
			outputFileNames.put(algorithms.keySet().iterator().next(), outputNames[0]);
			return;
		}
		int i = 0;
		long lowReadDepthInstances = algorithms.keySet().stream()
				.filter(key -> key instanceof CoverageTypeKey coverageTypeKey && coverageTypeKey.type == CoverageType.LOW_READDEPTH)
				.count();
		for (final Object key : algorithms.keySet()) {
			if (outputNames.length == algorithms.size()) {
				// one output file name has been provided for each coverage type
				outputFileNames.put(key, outputNames[i++]);
			} else if (key instanceof CoverageTypeKey coverageTypeKey) {
				/*
				 * A single output file name has been provided, so derive the name for each type.
				 * The cutoff is used to distinguish the low read depth coverages, as that is what
				 * differs between them.
				 */
				final String suffix = lowReadDepthInstances > 1 ? coverageTypeKey.cutoff.toString() : "";
				outputFileNames.put(key, deriveOutputFileName(outputNames[0], coverageTypeKey.type, suffix));
			} else {
				outputFileNames.put(key, deriveOutputFileName(outputNames[0], (CoverageType) key, ""));
			}
		}
	}

	/**
	 * Adds the name of the coverage type, and an optional suffix, to the supplied output file name.
	 */
	private static String deriveOutputFileName(final String outputFileName, final CoverageType coverageType,
			final String suffix) {
		String baseName = outputFileName;
		for (final String extension : new String[] { ".xml", ".txt", ".vcf", ".bed" }) {
			if (baseName.toLowerCase().endsWith(extension)) {
				baseName = baseName.substring(0, baseName.length() - extension.length());
				break;
			}
		}
		return baseName + "." + coverageType.value() + suffix;
	}

	public Options getOptions() {
		return options;
	}
	
	public boolean isPerFeatureFlag() {
		return perFeatureFlag;
	}

	/**
	 * @return whether the coverage should be reported per feature, for each of the coverage types
	 */
	public Map<Object, Boolean> getPerFeatureFlags() {
		return perFeatureFlags;
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

	/**
	 * @return the name of the output file to be used for the given coverage type
	 */
	public String getOutputFileName(final CoverageType coverageType) {
		if (outputFileNames.containsKey(coverageType)) {
			return outputFileNames.get(coverageType);
		}
		for (Map.Entry<Object, String> e : outputFileNames.entrySet()) {
			Object k = e.getKey();
			if (k instanceof CoverageTypeKey) {
				CoverageTypeKey key = (CoverageTypeKey) k;
				if (key.type == coverageType) {
					return e.getValue();
				}
			}
		}
		return null;
	}

	public String getOutputFileName(Object key) {
		return outputFileNames.get(key);
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
		if (outputFileNames.isEmpty()) {
			return null;
		}
		return new File(outputFileNames.values().iterator().next());
	}

	public HashSet<Pair<File, File>> getFilePairs() {
		return filePairs;
	}

	/**
	 * @return the coverage types that are to be calculated, in the order that they were
	 *         requested on the command line
	 */
	public List<CoverageType> getCoverageTypes() {
		List<CoverageType> result = new ArrayList<>();
		for (Object k : algorithms.keySet()) {
			result.add(extractType(k));
		}
		return result;
	}

	static CoverageType extractType(final Object key) {
		if (key instanceof CoverageTypeKey coverageTypeKey) {
			return coverageTypeKey.type;
		}
		return (CoverageType) key;
	}

	/**
	 * @return the filter used to select the reads for each of the coverage types. A coverage
	 *         type that has no filter is not present in the map.
	 */
	public Map<Object, QueryExecutor> getFilters() {
		return filters;
	}
	
	/**
	 * @return the algorithm to be used for each of the coverage types
	 */
	public Map<Object, Algorithm> getAlgorithms() {
		return algorithms;
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
		Vector<String> baiFileNameList = new Vector<>();
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
		for (final String outputFileName : outputFileNames.values()) {
			if (new File(outputFileName).exists()) {
				throw new Exception(
						"An output file of the same name already exists.");
			}
		}
	}

	public LoggerInfo getLoggerInfo() {
		return loggerInfo;
	}

	public String getValidation() {
		return validation;
	}

	static class CoverageTypeKey {
		final CoverageType type;
		final int index;
		final Integer cutoff;
		CoverageTypeKey(CoverageType type, int index) {
			this(type, index, null);
		}
		CoverageTypeKey(CoverageType type, int index, Integer cutoff) {
			this.type = type;
			this.index = index;
			this.cutoff = cutoff;
		}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof CoverageTypeKey other)) return false;
			return type == other.type && index == other.index;
		}
		@Override
		public int hashCode() {
			return Objects.hash(type, index);
		}
		@Override
		public String toString() {
			return type.name() + "_" + index;
		}
	}
}
