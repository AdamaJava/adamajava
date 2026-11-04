/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public final class Options {
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String INPUT_GFF3_OPTION_DESCRIPTION = Messages.getMessage("INPUT_GFF3_OPTION_DESCRIPTION");
	private static final String INPUT_BAM_OPTION_DESCRIPTION = Messages.getMessage("INPUT_BAM_OPTION_DESCRIPTION");
	private static final String INPUT_BAI_OPTION_DESCRIPTION = Messages.getMessage("INPUT_BAI_OPTION_DESCRIPTION");
	private static final String NUMBER_THREADS_DESCRIPTION = Messages.getMessage("NUMBER_THREADS_DESCRIPTION");
	private static final String TYPE_OPTION_DESCRIPTION = Messages.getMessage("TYPE_OPTION_DESCRIPTION");
	private static final String OUTPUT_FORMAT_DESCRIPTION = Messages.getMessage("OUTPUT_FORMAT_DESCRIPTION");
	
	private static final String PER_FEATURE_OPTION_DESCRIPTION = Messages.getMessage("PER_FEATURE_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String QUERY_OPTION_DESCRIPTION = Messages.getMessage("QUERY_OPTION_DESCRIPTION");
	private static final String SEQUENCE_QUERY_OPTION_DESCRIPTION = Messages.getMessage("SEQUENCE_QUERY_OPTION_DESCRIPTION");
	private static final String PHYSICAL_QUERY_OPTION_DESCRIPTION = Messages.getMessage("PHYSICAL_QUERY_OPTION_DESCRIPTION");
	private static final String LOW_READ_DEPTH_QUERY_OPTION_DESCRIPTION = Messages.getMessage("LOW_READ_DEPTH_QUERY_OPTION_DESCRIPTION");

	/*
	 * The options that supply a query for a single coverage type, which take precedence over the
	 * query option, keyed by the coverage type that they apply to.
	 */
	private static final String SEQUENCE_QUERY = "sequence-query";
	private static final String PHYSICAL_QUERY = "physical-query";
	private static final String LOW_READ_DEPTH_QUERY = "low-readdepth-query";
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String LOW_READ_DEPTH_OPTION_DESCRIPTION = Messages.getMessage("LOW_READ_DEPTH_OPTION_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String[] inputGFF3FileNames;
	private String[] inputBAMFileNames;
	private String[] outputFileNames;
	private String[] inputBAIFileNames;
	private String[] types;
	private String[] outputFormat;
	private Integer[] numberThreads;
	private final String logLevel;
	private final String log;
	private String[] queries;
	private String[] sequenceQueries;
	private String[] physicalQueries;
	private String[] lowReadDepthQueries;
	private String validation;
	private Integer[] lowReadDepthCutoffs;

	
	@Deprecated
	private boolean isSegmenterMode = false;
	@Deprecated
	private String inputSegmentFile;
	@Deprecated
	private String outputSegmentFile;
	@Deprecated
	private String bounds;
	@Deprecated
	private String[] features;


	@SuppressWarnings("unchecked")
	public Options(final String[] args) {
		parser.accepts("help", HELP_DESCRIPTION);	
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class);	
		parser.accepts("output-format", OUTPUT_FORMAT_DESCRIPTION).withRequiredArg().ofType(String.class);
				
		parser.accepts("input-bam", INPUT_BAM_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);				
		parser.accepts("input-bai", INPUT_BAI_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);		
		parser.accepts("input-gff3", INPUT_GFF3_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);				 
		parser.accepts("type", TYPE_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("query", QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts(SEQUENCE_QUERY, SEQUENCE_QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts(PHYSICAL_QUERY, PHYSICAL_QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts(LOW_READ_DEPTH_QUERY, LOW_READ_DEPTH_QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("thread", NUMBER_THREADS_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("per-feature", PER_FEATURE_OPTION_DESCRIPTION);
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("readdepth-cutoff", LOW_READ_DEPTH_OPTION_DESCRIPTION).withOptionalArg();



//		//segmenter options
//		parser.accepts("segmenter", Messages.getMessage("SEGMENTER_OPTION_DESCRIPTION"));
//		parser.accepts("infile",  Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class);
//		parser.accepts("outfile", Messages.getMessage("OUTFILE_DESCRIPTION")).withRequiredArg().ofType(String.class);
//		parser.accepts("feature", Messages.getMessage("FEATURE_DESCRIPTION")).withRequiredArg().ofType(String.class);
//		parser.accepts("merge", Messages.getMessage("MERGE_OPTION_DESCRIPTION"));
//		parser.accepts("fill", Messages.getMessage("FILL_OPTION_DESCRIPTION"));
//		parser.accepts("bounds", Messages.getMessage("BOUNDS_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
//		parser.accepts("reference", Messages.getMessage("REFERENCE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
		
		options = parser.parse(args);

		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		if (options.has("segmenter")) {
			isSegmenterMode = true;
			inputSegmentFile = (String) options.valueOf("infile");
			outputSegmentFile = (String) options.valueOf("outfile");			
			features = extractStringList("feature");			
			bounds = (String) options.valueOf("bounds");
			
		} else {
			List inputBAMFileNamesList = options.valuesOf("input-bam");
			inputBAMFileNames = new String[inputBAMFileNamesList.size()];
			inputBAMFileNamesList.toArray(inputBAMFileNames);

			List inputBAIFileNamesList = options.valuesOf("input-bai");
			inputBAIFileNames = new String[inputBAIFileNamesList.size()];
			inputBAIFileNamesList.toArray(inputBAIFileNames);

			inputGFF3FileNames = extractStringList("input-gff3");
			outputFileNames = extractStringList("output");						
			outputFormat = extractStringList("output-format");
						
			types = extractStringList("type");
			numberThreads = extractIntegerList();
			
			queries = extractStringList("query");
			sequenceQueries = extractStringList(SEQUENCE_QUERY);
			physicalQueries = extractStringList(PHYSICAL_QUERY);
			lowReadDepthQueries = extractStringList(LOW_READ_DEPTH_QUERY);
			validation = (String) options.valueOf("validation");

			List<?> lowReadDepthList = options.valuesOf("readdepth-cutoff");
			if (null == lowReadDepthList || lowReadDepthList.isEmpty()) {
				lowReadDepthCutoffs = null;
			} else {
				ArrayList<Integer> cutoffs = new ArrayList<>();
				for (Object o : lowReadDepthList) {
					if (null == o) {
						continue;
					}
					String s = o.toString();
					if (s.contains(",")) {
						String[] parts = s.split(",");
						for (String p : parts) {
							p = p.trim();
							if (!p.isEmpty()) {
								cutoffs.add(Integer.parseInt(p));
							}
						}
					} else {
						cutoffs.add(Integer.parseInt(s));
					}
				}
				lowReadDepthCutoffs = cutoffs.toArray(new Integer[0]);
			}
		}
	}

	private String[] extractStringList(final String id) {
		List<?> list = options.valuesOf(id);
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	private Integer[] extractIntegerList() {
		List<?> list = options.valuesOf("thread");
		Integer[] result = new Integer[list.size()];
		list.toArray(result);
		return result;
	}

	public boolean hasQueryOption() {
		return options.has("query");
	}

	public boolean hasPerFeatureOption() {
		return options.has("per-feature");
	}

	public boolean hasTypeOption() {
		return  options.has("type");
	}

	public String[] getTypes() {
		return types;
	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}
	
	boolean hasTxtFlag() {		 
		//default (empty) is TXT, or specify TXT
		return outputFormat.length == 0 || 
				! Arrays.stream(outputFormat).allMatch( f -> {  return ! f.equalsIgnoreCase("TXT"); });
	}

	boolean hasXmlFlag() {
		// at least one of the element match xml
		return ! Arrays.stream(outputFormat).allMatch( f -> {  return ! f.equalsIgnoreCase("XML"); });
	}
	
	boolean hasVcfFlag() {
		// at least one of the element match vcf
		return ! Arrays.stream(outputFormat).allMatch( f -> {  return ! f.equalsIgnoreCase("VCF"); });
	}

	String getLog() {
		return log;
	}

	/**
	 * @return the query supplied with the query option, or null if no query was supplied
	 */
	String getQuery() {
		return (null == queries || 0 == queries.length) ? null : queries[0];
	}

	/**
	 * Returns the query to be used for the supplied coverage type. A query supplied for the
	 * coverage type itself (eg. with the sequence-query option) takes precedence over the query
	 * supplied with the query option, which applies to all of the coverage types.
	 *
	 * @param typeName the coverage type, as supplied with the type option
	 * @return the query for the coverage type, or null if there is none. An empty query means
	 *         that no reads are filtered out for the coverage type.
	 */
	String getQuery(final String typeName) {
		final String[] typeQueries = getTypeQueries(typeName);
		if (null != typeQueries && typeQueries.length > 0) {
			return typeQueries[0];
		}
		return getQuery();
	}

	/**
	 * @return the queries supplied for the coverage type with its own query option, or null if
	 *         the coverage type does not have its own query option
	 */
	private String[] getTypeQueries(final String typeName) {
		return switch (typeName) {
			case "sequence", "seq" -> sequenceQueries;
			case "physical", "phys" -> physicalQueries;
			case "low_readdepth" -> lowReadDepthQueries;
			default -> null;
		};
	}

	String getLogLevel() {
		return logLevel;
	}

	public boolean hasInputGFF3Option() {
		return options.has("input-gff3");
	}

	public boolean hasInputBAMOption() {
		return options.has("input-bam");
	}

	public boolean hasOutputOption() {
		return options.has("output");
	}

	public boolean hasVersionOption() {
		return options.has("version");
	}

	public boolean hasHelpOption() {
		return   options.has("help");
	}

	public boolean hasNonOptions() {
		return !options.nonOptionArguments().isEmpty();
	}

	public boolean hasNumberThreadsOption() {
		return options.has("thread");
	}

	public String[] getBAMFileNames() {
		return inputBAMFileNames;
	}

	public String[] getInputGFF3FileNames() {
		return inputGFF3FileNames;
	}

	public String[] getOutputFileNames() {
		return outputFileNames;
	}

	public Integer[] getLowReadDepthCutoffs() {
		return lowReadDepthCutoffs;
	}

	public Integer getLowReadDepthCutoff() {
		if (null == lowReadDepthCutoffs || 0 == lowReadDepthCutoffs.length) {
			return null;
		}
		return lowReadDepthCutoffs[0];
	}

	public void displayHelp() throws Exception {
		parser.formatHelpWith(new BuiltinHelpFormatter(135, 2));
		parser.printHelpOn(System.err);
	}

	public Integer[] getNumberThreads() {
		return numberThreads;
	}

	public String[] getBAIFileNames() {
		return this.inputBAIFileNames;
	}
	
	public String getValidation() {	
		return validation;
	}

	public void detectBadOptions() throws Exception {
		if (null != options.nonOptionArguments()
				&& 0 < options.nonOptionArguments().size()) {
			throw new Exception("All arguments must be specified as options.");
		}
		if (!hasLogOption()) {
			throw new Exception("A log filename must be specified (using the --log option)");
		}
		if (isSegmenterMode) {
			if (!hasOption("infile")) {
				throw new Exception("Missing infile option");
			}
			
			checkFile("Input gff3", inputSegmentFile);			
			
			if (!hasOption("outfile")) {
				throw new Exception("Missing outfile option");
			}			
			
			if (!hasOption("feature")) {
				throw new Exception("Missing feature option");
			}
							
			checkFile("Bounds", bounds);				
 			
			return; 
		} 
		
		
		 		
		if (!hasTypeOption()) {
			throw new Exception("Missing type option");
		}
		if (0 == getTypes().length) {
			throw new Exception("Missing type option");
		}
		List<String> validTypes = Arrays.asList("seq", "low_readdepth", "sequence","physical","phys","segmenter");

		long lowReadDepthRequested = 0;
		for (final String type : getTypes()) {
			if (!validTypes.contains(type)) {
				throw new IllegalArgumentException("Invalid type: " + type + ". Valid types are: " + validTypes);
			}
			if (type.equals("low_readdepth")) {
				lowReadDepthRequested++;
			}
		}

		if (0 == lowReadDepthRequested && hasLowReadDepthOption()) {
			throw new Exception("Low read depth cutoff option should only be used for low_readdepth coverage type");
		}
		if (1 < lowReadDepthRequested) {
			throw new Exception("The low_readdepth coverage type can only be specified once. Supply several "
					+ "cutoffs with the readdepth-cutoff option to produce one report per cutoff");
		}
		if (0 < lowReadDepthRequested) {
			if (null == lowReadDepthCutoffs || 0 == lowReadDepthCutoffs.length) {
				throw new Exception("Missing low read depth cutoff option when running low_readdepth coverage type");
			}
			if (Arrays.stream(lowReadDepthCutoffs).distinct().count() != lowReadDepthCutoffs.length) {
				throw new Exception("Each low read depth cutoff should only be specified once");
			}
		}

		if (null != queries && queries.length > 1) {
			throw new Exception("The query option can only be specified once. Use the "
					+ SEQUENCE_QUERY + ", " + PHYSICAL_QUERY + " and " + LOW_READ_DEPTH_QUERY
					+ " options to supply a different query for a coverage type");
		}
		checkTypeQuery(SEQUENCE_QUERY, sequenceQueries, "sequence", "seq");
		checkTypeQuery(PHYSICAL_QUERY, physicalQueries, "physical", "phys");
		checkTypeQuery(LOW_READ_DEPTH_QUERY, lowReadDepthQueries, "low_readdepth");

		if (!hasInputBAMOption()) {
			throw new Exception("Missing BAM input file option");
		}
		if (!hasInputGFF3Option()) {
			throw new Exception("Missing GFF3 input file option");
		}
		if (!hasOutputOption()) {
			throw new Exception("Missing output option");
		}
		// the low_readdepth type results in one coverage per cutoff
		final int numberOfCoverageTypes = getTypes().length
				+ (0 < lowReadDepthRequested ? lowReadDepthCutoffs.length - 1 : 0);
		// either a single output file is provided, in which case the file names of the
		// individual coverage types are derived from it, or one output file is provided
		// for each of the coverage types
		final int numberOfOutputFiles = getOutputFileNames().length;
		if (numberOfOutputFiles > numberOfCoverageTypes) {
			throw new Exception("More output files have been specified than coverage types");
		}
		if (1 < numberOfOutputFiles && numberOfOutputFiles != numberOfCoverageTypes) {
			throw new Exception("Either a single output file, or one output file per coverage type, should be provided");
		}
		if (1 != getInputGFF3FileNames().length) {
			throw new Exception("Only one input GFF3 file should be provided");
		}
		if (hasNumberThreadsOption() && 1 < getNumberThreads().length) {
			throw new Exception("Thread count can be specified once");
		}
		//vcf output is only allowed under per-feature mode
		if( hasVcfFlag() && ! hasPerFeatureOption())  {
			throw new Exception("Only per-feature mode can produce VCF format output");
		}
		 
		
	}

	/**
	 * Checks that the query option for a coverage type is specified at most once, and only when
	 * that coverage type has been requested.
	 */
	private void checkTypeQuery(final String option, final String[] typeQueries, final String... typeNames) throws Exception {
		if (null == typeQueries || 0 == typeQueries.length) {
			return;
		}
		if (typeQueries.length > 1) {
			throw new Exception("The " + option + " option can only be specified once");
		}
		final List<String> names = Arrays.asList(typeNames);
		if (Arrays.stream(getTypes()).noneMatch(names::contains)) {
			throw new Exception("The " + option + " option has been specified, but the " + typeNames[0]
					+ " coverage type has not been requested");
		}
	}

	private boolean hasLowReadDepthOption() {
		return options.has("readdepth-cutoff");
	}

	private void checkFile(String type, String fileName) throws Exception {
		if (!new File(fileName).exists()) {
			throw new Exception(type + " file: " + fileName + " does not exist");
		}		
	}

	private boolean hasOption(String option) {
		return options.has(option);
	}
	
	//below method for deprecated Segementer.java
	@Deprecated
	public boolean hasSegementerOption() {
		return isSegmenterMode;
	}
	@Deprecated
	public File getInputSegmentFile() {
		return new File(inputSegmentFile);
	}
	@Deprecated
	public File getOutputSegmentFile() {
		return new File(outputSegmentFile);
	}
	@Deprecated
	public File getBoundsOption() {		
		return new File(bounds);
	}

	@Deprecated
	public String[] getFeatures() {
		return features;	
	}
	
	@Deprecated
	public boolean hasMergeOption() {
		return options.has("merge");		
	}
	
	@Deprecated
	public boolean hasFillOption() {
		return options.has("fill");		
	}
}
