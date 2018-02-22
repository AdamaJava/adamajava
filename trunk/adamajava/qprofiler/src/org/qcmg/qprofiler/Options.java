/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

final class Options {

	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String NO_OF_THREADS_OPTION_DESCRIPTION = Messages.getMessage("NO_OF_THREADS_OPTION_DESCRIPTION");
	private static final String INCLUDE_OPTION_DESCRIPTION = Messages.getMessage("INCLUDE_OPTION_DESCRIPTION");
	private static final String TAGS_OPTION_DESCRIPTION = Messages.getMessage("TAGS_OPTION_DESCRIPTION");
	private static final String TAGS_INT_OPTION_DESCRIPTION = Messages.getMessage("TAGS_INT_OPTION_DESCRIPTION");
	private static final String TAGS_CHAR_OPTION_DESCRIPTION = Messages.getMessage("TAGS_CHAR_OPTION_DESCRIPTION");
	private static final String MAX_RECORDS_OPTION_DESCRIPTION = Messages.getMessage("MAX_RECORDS_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String OUTPUT_FILE_DESCRIPTION = Messages.getMessage("OUTPUT_FILE_DESCRIPTION");
	private static final String INPUT_FILE_DESCRIPTION = Messages.getMessage("INPUT_FILE_DESCRIPTION");
	private static final String INDEX_FILE_DESCRIPTION = Messages.getMessage("INDEX_FILE_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String NO_HTML_DESCRIPTION = Messages.getMessage("NO_HTML_OPTION_DESCRIPTION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
//	private final List<String> fileNames;
	private final String[] fileNames;
	private final String[] indexFileNames;
	private final String outputFileName;
	private final String[] includes;
	private final String[] tags;
	private final String[] tagsInt;
	private final String[] tagsChar;
	private int noOfProducerThreads;
	private int noOfConsumerThreads;
	private int maxRecords;
	private final String log;
	private final String logLevel;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws QProfilerException {
//		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
//		parser.acceptsAll(asList("v", "version"), VERSION_DESCRIPTION);
		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		
		parser.accepts("input", INPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("ntProducer", NO_OF_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("ntConsumer", NO_OF_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("maxRecords", MAX_RECORDS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("include", INCLUDE_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("index", INPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
//		parser.accepts("exclude", EXCLUDES_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("tags", TAGS_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("tagsInt", TAGS_INT_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("tagsChar", TAGS_CHAR_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("nohtml", NO_HTML_DESCRIPTION);
		parser.posixlyCorrect(true);

		options = parser.parse(args);
		
		// no of threads - Consumer
		Object threadNumberConsumer = options.valueOf("ntConsumer"); 
		if (null != threadNumberConsumer)
			noOfConsumerThreads =  (Integer) threadNumberConsumer;
		// no of threads - Producer
		Object threadNumberProducer = options.valueOf("ntProducer"); 
		if (null != threadNumberProducer)
			noOfProducerThreads =  (Integer) threadNumberProducer;
		
		// maxRecords
		Object maxRecordsObject = options.valueOf("maxRecords"); 
		if (null != maxRecordsObject)
			maxRecords =  (Integer) maxRecordsObject;
		
		// include
		List<String> includeArgs = (List<String>) options.valuesOf("include");
		includes = new String[includeArgs.size()];
		includeArgs.toArray(includes);

		// tags
		List<String> tagsArgs = (List<String>) options.valuesOf("tags");
		tags = new String[tagsArgs.size()];
		tagsArgs.toArray(tags);
		
		// tagsInt
		List<String> tagsIntArgs = (List<String>) options.valuesOf("tagsInt");
		tagsInt = new String[tagsIntArgs.size()];
		tagsIntArgs.toArray(tagsInt);
		
		// tagsChar
		List<String> tagsCharArgs = (List<String>) options.valuesOf("tagsChar");
		tagsChar = new String[tagsCharArgs.size()];
		tagsCharArgs.toArray(tagsChar);
		
		// log file
		log = (String) options.valueOf("log");
		// logLevel
		logLevel = (String) options.valueOf("loglevel");
		
		
		// inputs
		List<String> inputs = (List<String>) options.valuesOf("input");
		fileNames = new String[inputs.size()];
		inputs.toArray(fileNames);
		
		// indexes
		List<String> indexes = (List<String>) options.valuesOf("index");
		indexFileNames = new String[indexes.size()];
		indexes.toArray(indexFileNames);
		
		
		// output
		outputFileName = (String) options.valueOf("output");
		
		if ( ! options.nonOptionArguments().isEmpty())
			throw new IllegalArgumentException(Messages.getMessage("USAGE"));
		
//		List<String> nonoptions = options.nonOptionArguments();
//		fileNames = new String[nonoptions.size()];
//		nonoptions.toArray(fileNames);
	}

	boolean hasVersionOption() {
		return options.has("version");
	}

	boolean hasHelpOption() {
		return options.has("help");
	}
	
	boolean hasNoHtmlOption() {
		return options.has("nohtml");
	}
	
	boolean hasLogOption() {
		return options.has("log");
	}
	
	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	String[] getFileNames() {
		return fileNames;
	}
	String[] getIndexFileNames() {
		return indexFileNames;
	}
//	List<String> getFileNames() {
//		return fileNames;
//	}
	
	String[] getBamIncludes() {
		return includes;
	}
	
	String[] getTags() {
		return tags;
	}
	
	String[] getTagsInt() {
		return tagsInt;
	}
	String[] getTagsChar() {
		return tagsChar;
	}
	
	int getNoOfConsumerThreads() {
		return noOfConsumerThreads;
	}
	int getNoOfProducerThreads() {
		return noOfProducerThreads;
	}
	
	int getMaxRecords() {
		return maxRecords;
	}
	
	String getLog() {
		return log;
	}
	String getLogLevel() {
		return logLevel;
	}
	public String getOutputFileName() {
		return outputFileName;
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}
	
	String getValidation() {
		if (options.has("validation")) {
			return (String) options.valueOf("validation");
		} else return null;
	}

}
