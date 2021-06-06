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

package org.qcmg.qprofiler2;

import java.util.List;
import static java.util.Arrays.asList;
import org.qcmg.common.messages.Messages;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


final class Options {
	private static final String msgResource = "org.qcmg.qprofiler2.messages";
	
	private static final String HELP_DESCRIPTION = Messages.getMessage(msgResource,"HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage(msgResource,"VERSION_OPTION_DESCRIPTION");
	private static final String CONSUMER_THREADS_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"CONSUMER_THREADS_OPTION_DESCRIPTION");
	private static final String PRODUCER_THREADS_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"PRODUCER_THREADS_OPTION_DESCRIPTION");

	private static final String INCLUDE_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"INCLUDE_OPTION_DESCRIPTION");
	private static final String TAGS_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"TAGS_OPTION_DESCRIPTION");
	private static final String TAGS_INT_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"TAGS_INT_OPTION_DESCRIPTION");
	private static final String TAGS_CHAR_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"TAGS_CHAR_OPTION_DESCRIPTION");
	private static final String RECORDS_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"RECORDS_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String OUTPUT_FILE_DESCRIPTION = Messages.getMessage(msgResource,"OUTPUT_FILE_DESCRIPTION");
	private static final String INPUT_FILE_DESCRIPTION = Messages.getMessage(msgResource,"INPUT_FILE_DESCRIPTION");
	private static final String INDEX_FILE_DESCRIPTION = Messages.getMessage(msgResource,"INDEX_FILE_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"VALIDATION_STRINGENCY_DESCRIPTION");	
	private static final String FULL_BAMHEADER_OPTION_DESCRIPTION = Messages.getMessage(msgResource, "FULL_BAMHEADER_OPTION_DESCRIPTION");
	
	// vcf mode
	private static final String FORMAT_OPTION_DESCRIPTION = Messages.getMessage(msgResource,"FORMAT_OPTION_DESCRIPTION");
	private final String[] formats;  // vcf mode		
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] fileNames;
	private final String[] indexFileNames;
	private final String outputFileName;
	private int noOfProducerThreads;
	private int noOfConsumerThreads;
	private int maxRecords;
	private final String log;
	private final String logLevel;
	
	
	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
		
		parser.accepts("help", HELP_DESCRIPTION);		
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);		
		parser.accepts("input", INPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);		
		parser.acceptsAll(asList("threads-producer", "tp"), PRODUCER_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.acceptsAll(asList("threads-consumer", "tc"), CONSUMER_THREADS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("index", INDEX_FILE_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("records", RECORDS_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		
		parser.accepts("include", INCLUDE_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("tags", TAGS_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("tagsInt", TAGS_INT_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("tagsChar", TAGS_CHAR_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.posixlyCorrect(true);		
		
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("fullBamHeader", FULL_BAMHEADER_OPTION_DESCRIPTION);
		parser.accepts("format", FORMAT_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("index", INDEX_FILE_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.posixlyCorrect(true);
		options = parser.parse(args);
		
		// no of threads - Consumer
		Object threadNumberConsumer = options.valueOf("threads-consumer"); 
		if (null != threadNumberConsumer) {
			noOfConsumerThreads =  (Integer) threadNumberConsumer;
		}
		
		// no of threads - Producer
		Object threadNumberProducer = options.valueOf("threads-producer"); 
		if (null != threadNumberProducer) {
			noOfProducerThreads =  (Integer) threadNumberProducer;
		}
		// maxRecords
		Object maxRecordsObject = options.valueOf("records"); 
		if (null != maxRecordsObject) {
			maxRecords =  (Integer) maxRecordsObject;	
		}	
		
		// log file
		log = (String) options.valueOf("log");
		// logLevel
		logLevel = (String) options.valueOf("loglevel");
		
		// vcf mode: format field name
		List<String> formatArgs = (List<String>) options.valuesOf("format");
		formats = new String[formatArgs.size()];
		formatArgs.toArray(formats);
				
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
		if ( ! options.nonOptionArguments().isEmpty()) {
			throw new IllegalArgumentException(Messages.getMessage(msgResource, "USAGE"));
		}
	}
	
	boolean hasFullBamHeaderOption() {
		return options.has("fullBamHeader") ; 
	}

	boolean hasVersionOption() {
		return options.has("version"); 
	}

	boolean hasHelpOption() {
		return options.has("help"); 
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
		
	// vcf mode
	String[] getFormats() {
		return formats; 
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
	
	String getVersion() {
		return QProfiler2.class.getPackage().getImplementationTitle()
				+ ", version " + QProfiler2.class.getPackage().getImplementationVersion();	
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
		} 	
		return null;
	}

}
