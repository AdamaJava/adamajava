/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public final class Options {
	private static final String HELP_DESCRIPTION = Messages
			.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages
			.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String INPUT_GFF3_OPTION_DESCRIPTION = Messages
			.getMessage("INPUT_GFF3_OPTION_DESCRIPTION");
	private static final String INPUT_BAM_OPTION_DESCRIPTION = Messages
			.getMessage("INPUT_BAM_OPTION_DESCRIPTION");
	private static final String INPUT_BAI_OPTION_DESCRIPTION = Messages
			.getMessage("INPUT_BAI_OPTION_DESCRIPTION");
	private static final String NUMBER_THREADS_DESCRIPTION = Messages
			.getMessage("NUMBER_THREADS_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages
			.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String QUERY_OPTION_DESCRIPTION = Messages
			.getMessage("QUERY_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages
			.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputGFF3FileNames;
	private final String[] inputBAMFileNames;
	private final String[] outputFileNames;
	private final String[] inputBAIFileNames;
	private final Integer numberThreads;
	private final String logLevel;
	private final String log;
	private final String query;
	private final String validation;

	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("o", "output"), OUTPUT_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.acceptsAll(asList("bam"), INPUT_BAM_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("BAM file");
		parser.acceptsAll(asList("bai"), INPUT_BAI_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("BAI file");
		parser.acceptsAll(asList("gff3"), INPUT_GFF3_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("GFF3 file");
		parser.acceptsAll(asList("q", "query"), QUERY_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("expression");
		parser.acceptsAll(asList("n"), NUMBER_THREADS_DESCRIPTION)
				.withRequiredArg().ofType(Integer.class).describedAs(
						"number of worker threads");
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(
				String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class);
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class); 
		parser.accepts("reference", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(
				String.class);
		parser.accepts("ini", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(
				String.class);
		parser.accepts("windowSize", NUMBER_THREADS_DESCRIPTION)
			.withRequiredArg().ofType(Integer.class).describedAs("size of window bins");
		parser.accepts("cutoff", NUMBER_THREADS_DESCRIPTION)
			.withRequiredArg().ofType(Integer.class).describedAs("cutoff");
		options = parser.parse(args);

		List inputBAMFileNamesList = options.valuesOf("bam");
		inputBAMFileNames = new String[inputBAMFileNamesList.size()];
		inputBAMFileNamesList.toArray(inputBAMFileNames);

		List inputBAIFileNamesList = options.valuesOf("bai");
		inputBAIFileNames = new String[inputBAIFileNamesList.size()];
		inputBAIFileNamesList.toArray(inputBAIFileNames);

		inputGFF3FileNames = extractStringList("gff3");
		outputFileNames = extractStringList("o");

		numberThreads = (Integer) options.valueOf("n");
		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		query = (String) options.valueOf("query");
		validation = (String) options.valueOf("validation");
	}

	private String[] extractStringList(final String id) {
		List<?> list = options.valuesOf(id);
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	public String getIniFile() {
		return (String) (options.has("ini") ? options.valueOf("ini") : null);
	}

	public boolean hasQueryOption() {
		return options.has("q") || options.has("query");
	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
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

	boolean hasJsonFlag() {
		return options.has("json");
	}

	public boolean hasInputBAIOption() {
		return options.has("bai");
	}

	public boolean hasInputGFF3Option() {
		return options.has("gff3");
	}

	public boolean hasInputBAMOption() {
		return options.has("bam");
	}

	public boolean hasOutputOption() {
		return options.has("o") || options.has("output");
	}

	public boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	public boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}
	public String getReference() {
		return options.has("reference") ? (String)options.valueOf("reference") : null;
	}
//	public String getRegex() {
//		return options.has("regex") ? (String)options.valueOf("regex") : null;
//	}

	public boolean hasNonOptions() {
		return 0 != options.nonOptionArguments().size();
	}

	public boolean hasNumberThreadsOption() {
		return options.has("n");
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

	public Integer getNumberThreads() {
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
		if ( ! hasInputBAMOption()) {
			throw new Exception("Missing BAM input file option");
		}
		if ( ! hasOutputOption()) {
			throw new Exception("Missing output option");
		}
		if ( ! hasLogOption()) {
			throw new Exception("A log filename must be specified (using the --log option)");
		}
	}
}
