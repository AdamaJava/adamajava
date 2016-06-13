/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

final class Options {
	private static final String INPUT_DESCRIPTION = Messages
			.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String SNP_POSITION_DESCRIPTION = Messages
			.getMessage("SNP_POSITION_DESCRIPTION");
	private static final String MIN_MAPPING_QUALITY_DESCRIPTION = Messages
			.getMessage("MIN_MAPPING_QUALITY_DESCRIPTION");
	private static final String MIN_BASE_QUALITY_DESCRIPTION = Messages
			.getMessage("MIN_BASE_QUALITY_DESCRIPTION");
//	private static final String SNP_CHIP_COVERAGE_DESCRIPTION = Messages
//			.getMessage("SNP_CHIP_COVERAGE_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages
			.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String DIR_OUTPUT_DESCRIPTION = Messages
			.getMessage("DIR_OUTPUT_DESCRIPTION");
	private static final String HELP_DESCRIPTION = Messages
			.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages
			.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String MIN_COV_OPTION_DESCRIPTION = Messages
			.getMessage("MIN_COVERAGE_OPTION_DESCRIPTION");
	private static final String CUT_OFF_OPTION_DESCRIPTION = Messages
			.getMessage("CUTOFF_OPTION_DESCRIPTION");
	private static final String NO_OF_THREADS_OPTION_DESCRIPTION = Messages
			.getMessage("NO_OF_THREADS_OPTION_DESCRIPTION");
	private static final String SEQUENTIAL_OPTION_DESCRIPTION = Messages
			.getMessage("SEQUENTIAL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputFileNames;
	private final String[] outputFileNames;
	private final String[] dirNames;
	private int minCoverage;
	private Integer minMappingQuality;
	private Integer minBaseQuality;
	private int noOfThreads;
//	private int snpChipCoverage;
//	private float cutoff;
	private final String log;
	private final String logLevel;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
		parser.acceptsAll(asList("i", "input"), INPUT_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("infile");
		parser.accepts("output", OUTPUT_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.acceptsAll(asList("d", "dir", "directory"),
				DIR_OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("outdir");
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("minCoverage", MIN_COV_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("minCoverage");
		parser.accepts("minMappingQuality", MIN_MAPPING_QUALITY_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("minMappingQuality");
		parser.accepts("minBaseQuality", MIN_BASE_QUALITY_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("minBaseQuality");
		parser.accepts("cutoff", CUT_OFF_OPTION_DESCRIPTION).withRequiredArg().ofType(Float.class)
			.describedAs("cutoff");
		parser.accepts("noOfThreads", NO_OF_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("noOfThreads");
//		parser.accepts("snpChipCoverage", SNP_CHIP_COVERAGE_DESCRIPTION).withRequiredArg().ofType(Integer.class)
//			.describedAs("snpChipCoverage");
		parser.accepts("snpPositions", SNP_POSITION_DESCRIPTION).withRequiredArg().ofType(String.class)
			.describedAs("snpPositions");
		parser.accepts("sequential", SEQUENTIAL_OPTION_DESCRIPTION);
		parser.accepts("searchSuffix", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Search suffix");
		parser.accepts("snpChipSearchSuffix", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Snp Chip Search Suffix");
		parser.accepts("additionalSearchString", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Additional Search string");
		parser.accepts("illuminaArraysDesign", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Illumina Arrays Design file");
		parser.accepts("email", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("email");
		parser.accepts("emailSubject", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("emailSubject");
		parser.accepts("excludeVcfsFile", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("excludeVcfsFile");
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList("p", "position"), INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("position");
		options = parser.parse(args);

		List inputList = options.valuesOf("i");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);
		
		List outputList = options.valuesOf("output");
		outputFileNames = new String[outputList.size()];
		outputList.toArray(outputFileNames);

		List dirList = options.valuesOf("d");
		dirNames = new String[dirList.size()];
		dirList.toArray(dirNames);

		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		if (null != options.valueOf("minCoverage"))
			minCoverage = (Integer) options.valueOf("minCoverage");
		if (null != options.valueOf("minMappingQuality"))
			minMappingQuality = (Integer) options.valueOf("minMappingQuality");
		if (null != options.valueOf("minBaseQuality"))
			minBaseQuality = (Integer) options.valueOf("minBaseQuality");
		if (null != options.valueOf("noOfThreads"))
			noOfThreads = (Integer) options.valueOf("noOfThreads");
//		if (null != options.valueOf("snpChipCoverage"))
//			snpChipCoverage = (Integer) options.valueOf("snpChipCoverage");
	}

	String getLog() {
		return log;
	}

	String getLogLevel() {
		return logLevel;
	}
	
	public boolean hasSearchSuffixOption() {
		return options.has("searchSuffix");
	}
	public String getSearchSuffix() {
		return (String) options.valueOf("searchSuffix");
	}
	
	public boolean hasSnpChipSearchSuffixOption() {
		return options.has("snpChipSearchSuffix");
	}
	public String getSnpChipSearchSuffix() {
		return (String) options.valueOf("snpChipSearchSuffix");
	}
	
	public boolean hasIlluminaArraysDesignOption() {
		return options.has("illuminaArraysDesign");
	}
	public String getIlluminaArraysDesign() {
		return (String) options.valueOf("illuminaArraysDesign");
	}
	
	public boolean hasEmailOption() {
		return options.has("email");
	}
	public String getEmail() {
		return (String) options.valueOf("email");
	}
	
	public boolean hasAdditionalSearchStringOption() {
		return options.has("additionalSearchString");
	}
	public String [] getAdditionalSearchString() {
		List<?> inputList = options.valuesOf("additionalSearchString");
		String [] additionalSearchStrings = new String[inputList.size()];
		inputList.toArray(additionalSearchStrings);
		return additionalSearchStrings;
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}
	boolean hasLogOption() {
		return options.has("log");
	}
	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}
	boolean runInSequentialMode() {
		return options.has("sequential");
	}

	String[] getInputFileNames() {
		return inputFileNames;
	}
	String[] getOutputFileNames() {
		return outputFileNames;
	}

	String[] getDirNames() {
		return this.dirNames;
	}
	public int getMinCoverage() {
		return minCoverage;
	}
	public int getNoOfThreads() {
		return noOfThreads;
	}
//	public int getSnpChipCoverage() {
//		return snpChipCoverage;
//	}
	public float getCutoff() {
		return null != options.valueOf("cutoff") ? (Float) options.valueOf("cutoff") : 0.0f;
	}
	public boolean hasCutoff() {
		return options.has("cutoff");
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.out);
	}

	void detectBadOptions() throws Exception {
		if (1 > inputFileNames.length) {
			throw new Exception("One input file must be specified");
		} else if (1 < inputFileNames.length) {
			throw new Exception("Only one input file permitted");
		} else if (1 > dirNames.length) {
			throw new Exception("One output directory must be specified");
		} else if (1 < dirNames.length) {
			throw new Exception("Only one output directory permitted");
		}
		if (!hasLogOption()) {
			throw new Exception("A log filename must be specified (using the --log option)");
		}
	}

	public Integer getMinMappingQuality() {
		return minMappingQuality;
	}

	public Integer getMinBaseQuality() {
		return minBaseQuality;
	}

	public boolean hasEmailSubjectOption() {
		return options.has("emailSubject");
	}

	public String getEmaiSubjectl() {
		return (String) options.valueOf("emailSubject");
	}

	public boolean hasExcludeVcfsFileOption() {
		return options.has("excludeVcfsFile");
	}

	public String getExcludeVcfsFile() {
		return (String) options.valueOf("excludeVcfsFile");
	}
	
	public String getValidation() {
		return options.has("validation") ?  (String) options.valueOf("validation") : null;
	}
	
	public String[] getPositions() {
		List inputList = options.valuesOf("p");
		
		if (inputList.isEmpty()) return null;
		
		String[] inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);
		return inputFileNames;
	}

	public boolean hasMinCoverage() {
		return options.has("minCoverage");
	}
	
}
