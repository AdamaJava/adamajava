/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

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
	private static final String MAX_CACHE_SIZE_OPTION_DESCRIPTION = Messages
			.getMessage("MAX_CACHE_SIZE_OPTION_DESCRIPTION");
	private static final String SEQUENTIAL_OPTION_DESCRIPTION = Messages
			.getMessage("SEQUENTIAL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String STREAM_OPTION_DESCRIPTION = Messages.getMessage("STREAM_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputFileNames;
	private final String[] outputFileNames;
	private final String[] dirNames;
	private final Integer minCoverage;
	private final Integer minRGCoverage;
	private final Integer minMappingQuality;
	private final Integer minBaseQuality;
	private final Integer noOfThreads;
	private final Integer maxCacheSize;
	private final Float homCutoff;
	private final Float hetUpperCutoff;
	private final Float hetLowerCutoff;
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
		parser.accepts("minRGCoverage", MIN_COV_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("minRGCoverage");
		parser.accepts("minMappingQuality", MIN_MAPPING_QUALITY_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("minMappingQuality");
		parser.accepts("minBaseQuality", MIN_BASE_QUALITY_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("minBaseQuality");
		parser.accepts("cutoff", CUT_OFF_OPTION_DESCRIPTION).withRequiredArg().ofType(Float.class)
			.describedAs("cutoff");
		parser.accepts("homCutoff", "Cutoff for homozygous positions (defaults to 0.9)").withRequiredArg().ofType(Float.class)
		.describedAs("homCutoff");
		parser.accepts("hetUpperCutoff", "Cutoff for upper heterozygous positions (defaults to 0.7). Position will be considered a heterozygous position if its VAF is between the upper and lower het cutoffs").withRequiredArg().ofType(Float.class)
		.describedAs("hetUpperCutoff");
		parser.accepts("hetLowerCutoff", "Cutoff for upper heterozygous positions (defaults to 0.3). Position will be considered a heterozygous position if its VAF is between the upper and lower het cutoffs").withRequiredArg().ofType(Float.class)
		.describedAs("hetLowerCutoff");
		parser.accepts("noOfThreads", NO_OF_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class)
			.describedAs("noOfThreads");
		parser.acceptsAll(asList("maxCacheSize", "max-cache-size"), MAX_CACHE_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("maxCacheSize");
		parser.accepts("snpPositions", SNP_POSITION_DESCRIPTION).withRequiredArg().ofType(String.class)
			.describedAs("snpPositions");
		parser.accepts("genePositions", "Gff3 file containing list of gene positions").withRequiredArg().ofType(String.class)
			.describedAs("genePositions");
		parser.accepts("geneModel", "GTF file containing gene model information").withRequiredArg().ofType(String.class)
			.describedAs("geneModel");
		parser.accepts("reference", "Reference fasta file").withRequiredArg().ofType(String.class)
			.describedAs("reference");
		parser.accepts("sequential", SEQUENTIAL_OPTION_DESCRIPTION);
		parser.accepts("searchSuffix", "If option is specified, files must end with the supplied suffix").withRequiredArg().ofType(String.class)
				.describedAs("Search suffix");
		parser.accepts("snpChipSearchSuffix", "If option is specified, snp chip files must end with the supplied suffix").withRequiredArg().ofType(String.class)
				.describedAs("Snp Chip Search Suffix");
		parser.accepts("additionalSearchString", "qsig vcf filenames must match any strings supplied by this option. If they don't match, they will not be included in the comparison").withRequiredArg().ofType(String.class)
				.describedAs("Additional Search string");
		parser.accepts("excludeString", "Qsig vcf filenames that contain the suppled excludes string will be excluded").withRequiredArg().ofType(String.class)
				.describedAs("Excluded strings");
		parser.accepts("illuminaArraysDesign", "Illumina arrays design document - contains list of snp ids and whether they should be complemented").withRequiredArg().ofType(String.class)
				.describedAs("Illumina Arrays Design file");
		parser.accepts("email", "email address to send output to").withRequiredArg().ofType(String.class)
				.describedAs("email");
		parser.accepts("emailSubject", "subject line for email").withRequiredArg().ofType(String.class)
				.describedAs("emailSubject");
		parser.accepts("excludeVcfsFile", "file containing a list of vcf files to ignore in the comparison").withRequiredArg().ofType(String.class)
				.describedAs("excludeVcfsFile");
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("stream", STREAM_OPTION_DESCRIPTION);
		parser.acceptsAll(asList("p", "position"), "File containing a list of positions that will be examined. Must be a subset of the positions in the snpPositions file").withRequiredArg().ofType(String.class).describedAs("position");
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
		
		minRGCoverage = ((Integer) options.valueOf("minRGCoverage"));
		minCoverage = ((Integer) options.valueOf("minCoverage"));
		minMappingQuality = ((Integer) options.valueOf("minMappingQuality"));
		minBaseQuality = ((Integer) options.valueOf("minBaseQuality"));
		noOfThreads = ((Integer) options.valueOf("noOfThreads"));
		maxCacheSize = ((Integer) options.valueOf("maxCacheSize"));
		homCutoff = ((Float) options.valueOf("homCutoff"));
		hetUpperCutoff = ((Float) options.valueOf("hetUpperCutoff"));
		hetLowerCutoff = ((Float) options.valueOf("hetLowerCutoff"));
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
	
	public Optional<String> getIlluminaArraysDesign() {
		return Optional.ofNullable((String) options.valueOf("illuminaArraysDesign"));
	}
	public Optional<String> getSnpPositions() {
		return Optional.ofNullable((String) options.valueOf("snpPositions"));
	}
	public Optional<String> getGenePositions() {
		return Optional.ofNullable((String) options.valueOf("genePositions"));
	}
	public Optional<String> getReference() {
		return  Optional.ofNullable((String) options.valueOf("reference"));
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
	
	public Optional<String []> getExcludeStrings() {
		List<?> list = options.valuesOf("excludeString");
		if (null != list) {
			String [] excludeStrings = new String[list.size()];
			list.toArray(excludeStrings);
			return Optional.ofNullable(excludeStrings);
		} else {
			return Optional.empty();
		}
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
	public Optional<Integer> getMinCoverage() {
		return Optional.ofNullable(minCoverage);
	}
	public Optional<Integer> getMinRGCoverage() {
		return Optional.ofNullable(minRGCoverage);
	}
	public Optional<Integer> getNoOfThreads() {
		return Optional.ofNullable(noOfThreads);
	}
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

	public Optional<Integer> getMinMappingQuality() {
		return Optional.ofNullable(minMappingQuality);
	}

	public Optional<Integer> getMinBaseQuality() {
		return Optional.ofNullable(minBaseQuality);
	}
	public Optional<Float> getHomCutoff() {
		return Optional.ofNullable(homCutoff);
	}
	public Optional<Float> getHetUpperCutoff() {
		return Optional.ofNullable(hetUpperCutoff);
	}
	public Optional<Float> getHetLowerCutoff() {
		return Optional.ofNullable(hetLowerCutoff);
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
	public Optional<String> getGeneModelFile() {
		return Optional.ofNullable((String) options.valueOf("geneModel"));
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


	public Optional<Integer> getMaxCacheSize() {
		return Optional.ofNullable(maxCacheSize);
  }
	public Optional<Boolean> getStream() {
		return Optional.ofNullable(options.has("stream"));
	}

}
