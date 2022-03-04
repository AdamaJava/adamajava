/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */

package au.edu.qimr.tiledaligner;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

final class GenerateTiledAlignerFileOptions {

	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String POSITIONS_CUTOFF_OPTION_DESCRIPTION = Messages.getMessage("POSITIONS_CUTOFF_OPTION_DESCRIPTION");
	private static final String OUTPUT_FILE_DESCRIPTION = Messages.getMessage("OUTPUT_FILE_DESCRIPTION");
	private static final String REF_FILE_DESCRIPTION = Messages.getMessage("REF_FILE_DESCRIPTION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String outputFileName;
	private final String log;
	private final Integer positionsCutoff;
	private final String reference;
	private final String logLevel;

	@SuppressWarnings("unchecked")
	GenerateTiledAlignerFileOptions(final String[] args) throws Exception {
		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("reference", REF_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("positionsCutoff", POSITIONS_CUTOFF_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.posixlyCorrect(true);

		options = parser.parse(args);
		
		// log file
		log = (String) options.valueOf("log");
		// logLevel
		logLevel = (String) options.valueOf("loglevel");
		
		positionsCutoff =  (Integer) options.valueOf("positionsCutoff");
		reference = (String) options.valueOf("reference");
		
		// output
		outputFileName = (String) options.valueOf("output");
		
		if ( ! options.nonOptionArguments().isEmpty()) {
			throw new IllegalArgumentException(Messages.getMessage("USAGE"));
		}
	}

	public void displayHelp() throws IOException {
		parser.printHelpOn(System.out);
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
	
	OptionalInt getPositionsCutoff() {
		return null != positionsCutoff ? OptionalInt.of(positionsCutoff.intValue()) : OptionalInt.empty();
	}
	
	Optional<String> getReference() {
		return Optional.ofNullable(reference);
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

}
