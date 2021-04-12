/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public final class Options {
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String INPUT_BAM_OPTION_DESCRIPTION = Messages.getMessage("INPUT_BAM_OPTION_DESCRIPTION");
	private static final String INPUT_BAI_OPTION_DESCRIPTION = Messages.getMessage("INPUT_BAI_OPTION_DESCRIPTION");
	private static final String NUMBER_THREADS_DESCRIPTION = Messages.getMessage("NUMBER_THREADS_DESCRIPTION");
	private static final String OUTPUT_XML_DESCRIPTION = Messages.getMessage("OUTPUT_XML_OPTION_DESCRIPTION");
	private static final String OUTPUT_BAM_DESCRIPTION = Messages.getMessage("OUTPUT_BAM_OPTION_DESCRIPTION");
	private static final String QUERY_OPTION_DESCRIPTION = Messages.getMessage("QUERY_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String INI_OPTION_DESCRIPTION = Messages.getMessage("INI_OPTION_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputBAMFileNames;
	private final String[] inputBAIFileNames;
	private final Integer numberThreads;
	private final String logLevel;
	private final String log;
	private final String query;
	private final String validation;

	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("output-xml"), OUTPUT_XML_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList( "output-bam"), OUTPUT_BAM_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList("input-bam"), INPUT_BAM_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList("input-bai"), INPUT_BAI_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList("query"), QUERY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.acceptsAll(asList("threads"), NUMBER_THREADS_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.acceptsAll(asList( "version"), VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class); 
		parser.accepts("ini", INI_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		options = parser.parse(args);

		List<String> inputBAMFileNamesList = (List<String>) options.valuesOf("input-bam");
		inputBAMFileNames = new String[inputBAMFileNamesList.size()];
		inputBAMFileNamesList.toArray(inputBAMFileNames);

		List<String> inputBAIFileNamesList = (List<String>) options.valuesOf("output-bai");
		inputBAIFileNames = new String[inputBAIFileNamesList.size()];
		inputBAIFileNamesList.toArray(inputBAIFileNames);

		numberThreads = (Integer) options.valueOf("threads");
		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		query = (String) options.valueOf("query");
		validation = (String) options.valueOf("validation");
	}

	public String getIniFile() {
		return (String) (options.has("ini") ? options.valueOf("ini") : null);
	}

	public boolean hasQueryOption() {
		return  options.has("query");
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

	public boolean hasInputBaiOption() {
		return options.has("input-bai");
	}


	public boolean hasInputBamOption() {
		return options.has("input-bam");
	}

	public boolean hasOutputOption() {
		return  options.has("output-xml");
	}

	public boolean hasVersionOption() {
		return  options.has("version");
	}

	public boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	public boolean hasNonOptions() {
		return 0 != options.nonOptionArguments().size();
	}

	public boolean hasNumberThreadsOption() {
		return options.has("threads");
	}

	public String[] getBAMFileNames() {
		return inputBAMFileNames;
	}
	
	public String getOutputBamFileName() {
		return options.has("output-bam")? (String) options.valueOf("output-bam") : null;
	}

	public String getOutputXmlFileName() {
		return options.has("output-xml")? (String) options.valueOf("output-xml") : null;
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
		if ( ! hasInputBamOption()) {
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
