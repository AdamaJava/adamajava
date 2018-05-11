/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2;

import java.util.ResourceBundle;

import org.qcmg.common.messages.QMessage;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

final class Options {
	private final static QMessage messages = new QMessage(QVisualise.class, ResourceBundle.getBundle("org.qcmg.qvisualise2.messages") );
	
	private static final String HELP_DESCRIPTION = messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String INPUT_DESCRIPTION = messages.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String inputFile;
	private final String outputFile;
	private String log;
	private String logLevel;

	Options(final String[] args) throws Exception {

		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("input", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.posixlyCorrect(true);

		options = parser.parse(args);
		
		inputFile = (String) options.valueOf("input");
		outputFile = (String) options.valueOf("output");
		
		// log file
		log = (String) options.valueOf("log");
		// logLevel
		logLevel = (String) options.valueOf("loglevel");

	}

	boolean hasVersionOption() {
		return options.has("version");
	}

	boolean hasHelpOption() {
		return options.has("help");
	}
	boolean hasInputOption() {
		return options.has("input");
	}
	boolean hasOutputOption() {
		return options.has("output");
	}
	boolean hasLogFileOption() {
		return options.has("log");
	}
	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	String getInputFile() {
		return inputFile;
	}
	String getOutputFile() {
		return outputFile;
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}
	public QMessage getMessage(){ return messages; }

	public String getLog() {
		return log;
	}

	public String getLogLevel() {
		return logLevel;
	}
}
