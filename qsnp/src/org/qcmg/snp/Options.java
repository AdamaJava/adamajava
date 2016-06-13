/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * The Class Options.
 */
public final class Options {
	
	/** The Constant HELP_DESCRIPTION. */
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");

	/** The Constant VERSION_DESCRIPTION. */
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	
	/** The Constant INPUT_DESCRIPTION. */
	private static final String INPUT_DESCRIPTION = Messages.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String SEARCH_SUFFIX_DESCRIPTION = Messages.getMessage("SEARCH_SUFFIX_OPTION_DESCRIPTION");
	private static final String DB_SNP_DESCRIPTION = Messages.getMessage("DBSNP_OPTION_DESCRIPTION");
	
	/** The Constant LOG_DESCRIPTION. */
	private static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	
	/** The Constant LOG_LEVEL_DESCRIPTION. */
	private static final String LOG_LEVEL_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	
	/** The parser. */
	private final OptionParser parser = new OptionParser();
	
	/** The options. */
	private final OptionSet options;
	
	/** The command line. */
//	private final String commandLine;
	
	/** The input file names. */
	private final String[] inputFileNames;
	private final String[] searchSuffixNames;
	
	private final String logFile;
	private final String logLevel;
	
	/**
	 * Instantiates a new options.
	 *
	 * @param args the args
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
//		commandLine = Messages.reconstructCommandLine(args);

		parser.accepts("help", HELP_DESCRIPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.accepts("input", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("inputfile");
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("outputfile");
		parser.accepts("searchSuffix", SEARCH_SUFFIX_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Search suffix");
		parser.accepts("additionalSearchString", SEARCH_SUFFIX_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("Additional Search string");
		parser.accepts("dbSnp", DB_SNP_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("dbSnp file");
		parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("logfile");
		parser.accepts("loglevel", LOG_LEVEL_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("loglevel");

		options = parser.parse(args);

		List inputList = options.valuesOf("input");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);
		
		List searchSuffixList = options.valuesOf("searchSuffix");
		searchSuffixNames = new String[searchSuffixList.size()];
		searchSuffixList.toArray(searchSuffixNames);

		logFile = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
	}

	/**
	 * Checks for input option.
	 *
	 * @return true, if successful
	 */
	public boolean hasInputOption() {
		return options.has("input");
	}

	/**
	 * Checks for output option.
	 *
	 * @return true, if successful
	 */
	public boolean hasOutputOption() {
		return options.has("output");
	}
	
	public String getOutput() {
		return (String) options.valueOf("output");
	}
	
	public boolean hasSearchSuffixOption() {
		return options.has("searchSuffix");
	}
	public String [] getSearchSuffix() {
		return searchSuffixNames;
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
	public boolean hasDbSnpOption() {
		return options.has("dbSnp");
	}
	public String getDbSnp() {
		return (String) options.valueOf("dbSnp");
	}

	/**
	 * Checks for version option.
	 *
	 * @return true, if successful
	 */
	public boolean hasVersionOption() {
		return options.has("version");
	}

	/**
	 * Checks for help option.
	 *
	 * @return true, if successful
	 */
	public boolean hasHelpOption() {
		return options.has("help");
	}

	/**
	 * Checks for non options.
	 *
	 * @return true, if successful
	 */
	public boolean hasNonOptions() {
		return 0 != options.nonOptionArguments().size();
	}

	/**
	 * Gets the input file names.
	 *
	 * @return the input file names
	 */
	public String[] getInputFileNames() {
		return inputFileNames;
	}

	/**
	 * Gets the command line.
	 *
	 * @return the command line
	 */
//	public String getCommandLine() {
//		return commandLine;
//	}

	/**
	 * Display help.
	 *
	 * @throws Exception the exception
	 */
	public void displayHelp() throws Exception {
		parser.printHelpOn(System.out);
	}

	/**
	 * Detect bad options.
	 *
	 * @throws Exception the exception
	 */
	public void detectBadOptions() throws Exception {
		if (hasNonOptions()) {
			System.err.println(Messages.USAGE);
			throw new SnpException("ALL_ARGUMENTS_MUST_BE_OPTIONS");
		}
		if (!hasInputOption()) {
			System.err.println(Messages.USAGE);
			throw new SnpException("MISSING_INPUT_OPTIONS");
		}
	}

	public String getLogFile() {
		return logFile;
	}

	public String getLogLevel() {
		return logLevel;
	}

}
