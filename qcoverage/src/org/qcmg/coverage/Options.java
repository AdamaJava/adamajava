/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

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
	private static final String XML_OPTION_DESCRIPTION = Messages.getMessage("XML_OPTION_DESCRIPTION");
	private static final String VCF_OPTION_DESCRIPTION = Messages.getMessage("VCF_OPTION_DESCRIPTION");
	private static final String PER_FEATURE_OPTION_DESCRIPTION = Messages.getMessage("PER_FEATURE_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String QUERY_OPTION_DESCRIPTION = Messages.getMessage("QUERY_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String SEGMENTER_OPTION_DESCRIPTION = Messages.getMessage("SEGMENTER_OPTION_DESCRIPTION");
	private static final String INPUT_DESCRIPTION = Messages.getMessage("INPUT_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String[] inputGFF3FileNames;
	private String[] inputBAMFileNames;
	private String[] outputFileNames;
	private String[] inputBAIFileNames;
	private String[] types;
	private Integer[] numberThreads;
	private final String logLevel;
	private final String log;
	private String query;
	private String validation;
	
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
	public Options(final String[] args) throws Exception {
		parser.accepts("help", HELP_DESCRIPTION);	
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class);			
		parser.accepts("output-xml", XML_OPTION_DESCRIPTION);		
		parser.accepts("output-vcf", VCF_OPTION_DESCRIPTION);
				 
		
		//??.describedAs("BAM file");
		parser.accepts("input-bam", INPUT_BAM_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);				
		parser.accepts("input-bai", INPUT_BAI_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);		
		parser.accepts("input-gff3", INPUT_GFF3_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);				 
		parser.accepts("type", TYPE_OPTION_DESCRIPTION)	.withRequiredArg().ofType(String.class);
		parser.accepts("query", QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("thread", NUMBER_THREADS_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("per-feature", PER_FEATURE_OPTION_DESCRIPTION);						
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class); 
		
		
		//segmenter options
		parser.accepts("segmenter", SEGMENTER_OPTION_DESCRIPTION);
		parser.accepts("infile", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("outfile", Messages.getMessage("OUTFILE_DESCRIPTION")).withRequiredArg().ofType(String.class);
		parser.accepts("feature", Messages.getMessage("FEATURE_DESCRIPTION")).withRequiredArg().ofType(String.class);
		parser.accepts("merge", Messages.getMessage("MERGE_OPTION_DESCRIPTION"));
		parser.accepts("fill", Messages.getMessage("FILL_OPTION_DESCRIPTION"));
		parser.accepts("bounds", Messages.getMessage("BOUNDS_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
		parser.accepts("reference", Messages.getMessage("REFERENCE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
		
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
			types = extractStringList("type");

			numberThreads = extractIntegerList("thread");
			
			query = (String) options.valueOf("query");
			validation = (String) options.valueOf("validation");
		}
	}

	private String[] extractStringList(final String id) {
		List<?> list = options.valuesOf(id);
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	private Integer[] extractIntegerList(final String id) {
		List<?> list = options.valuesOf(id);
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

	boolean hasXmlFlag() {
		return options.has("output-xml");
	}
	
	boolean hasVcfFlag() {
		return options.has("output-vcf");
	}

	String getLog() {
		return log;
	}

	String getQuery() {
		return query;
	}

	String getLogLevel() {
		return logLevel;
	}

//	boolean hasJsonFlag() {
//		return options.has("json");
//	}

	public boolean hasInputBAIOption() {
		return options.has("input-bai");
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
		return 0 != options.nonOptionArguments().size();
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

	public void displayHelp() throws Exception {
		parser.printHelpOn(System.out);
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
 			
			
		} else {			
			if (!hasTypeOption()) {
				throw new Exception("Missing type option");
			}
			if (1 != getTypes().length) {
				throw new Exception("Only one type option can be provided");
			}
			if (!hasInputBAMOption()) {
				throw new Exception("Missing BAM input file option");
			}
			if (!hasInputGFF3Option()) {
				throw new Exception("Missing GFF3 input file option");
			}
			if (!hasOutputOption()) {
				throw new Exception("Missing output option");
			}
			if (1 != getInputGFF3FileNames().length) {
				throw new Exception("Only one input GFF3 file should be provided");
			}
			if (hasNumberThreadsOption() && 1 < getNumberThreads().length) {
				throw new Exception("Thread count can be specified once");
			}
			
		}
		
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
