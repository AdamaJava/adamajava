/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

import static java.util.Arrays.asList;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.common.log.QLogger;

final class Options {
	private static final String INPUT_DESCRIPTION = Messages
			.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String HELP_DESCRIPTION = Messages
			.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages
			.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String BAM_OUTPUT_DESCRIPTION = Messages
			.getMessage("BAM_OUTPUT_DESCRIPTION");
	private static final String SAM_OUTPUT_DESCRIPTION = Messages
			.getMessage("SAM_OUTPUT_DESCRIPTION");
	private static final String DIR_OUTPUT_DESCRIPTION = Messages
			.getMessage("DIR_OUTPUT_DESCRIPTION");
	private static final String NUMBERED_DESCRIPTION = Messages
			.getMessage("NUMBERED_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
			.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String INDEX_OPTION_DESCRIPTION = Messages
			.getMessage("INDEX_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages
			.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String SPLIT_1_DESCRIPTION = Messages
			.getMessage("SPLIT_1_DESCRIPTION");
	private static final String SPLIT_2_DESCRIPTION = Messages
			.getMessage("SPLIT_2_DESCRIPTION");
	private static final String SPLIT_1_ZCs_DESCRIPTION = Messages
			.getMessage("SPLIT_1_ZCs_DESCRIPTION");
	private static final String SPLIT_2_ZCs_DESCRIPTION = Messages
			.getMessage("SPLIT_2_ZCs_DESCRIPTION");

	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputFileNames;
	private final String[] dirNames;
	private final String log;
	private final String logLevel;
	private final String[] args;
	private final Integer[] split1zc;
	private final Integer[] split2zc;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
		this.args = args;
		parser.acceptsAll(asList("i", "input"), INPUT_DESCRIPTION)
				.withRequiredArg().ofType(String.class).describedAs("infile");
		parser.acceptsAll(asList("d", "dir", "directory"),
				DIR_OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
				.describedAs("outdir");
		parser.acceptsAll(asList("b", "bam"), BAM_OUTPUT_DESCRIPTION);
		parser.acceptsAll(asList("s", "sam"), SAM_OUTPUT_DESCRIPTION);
		parser.acceptsAll(asList("n", "num", "numbered"), NUMBERED_DESCRIPTION);
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(
				String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION)
				.withRequiredArg().ofType(String.class);
		parser.accepts("bai", INDEX_OPTION_DESCRIPTION);
		parser.accepts("singlelevel", INDEX_OPTION_DESCRIPTION);
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION)
		.withRequiredArg().ofType(String.class); 
		parser.accepts("split1", SPLIT_1_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("split1zc", SPLIT_1_ZCs_DESCRIPTION).withRequiredArg().ofType(Integer.class).withValuesSeparatedBy(',');;
		parser.accepts("split2", SPLIT_2_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("split2zc", SPLIT_2_ZCs_DESCRIPTION).withRequiredArg().ofType(Integer.class).withValuesSeparatedBy(',');;
		options = parser.parse(args);

		List inputList = options.valuesOf("i");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);

		List dirList = options.valuesOf("d");
		dirNames = new String[dirList.size()];
		dirList.toArray(dirNames);

		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		// split1zc
		List<Integer> split1zcArgs = (List<Integer>) options.valuesOf("split1zc");
		split1zc = new Integer[split1zcArgs.size()];
		split1zcArgs.toArray(split1zc);
		
		// split2zc
		List<Integer> split2zcArgs = (List<Integer>) options.valuesOf("split2zc");
		split2zc = new Integer[split2zcArgs.size()];
		split2zcArgs.toArray(split2zc);
	}

	String getLog() {
		return log;
	}

	String getLogLevel() {
		return logLevel;
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasNumberedOption() {
		return options.has("n") || options.has("num")
				|| options.has("numbered");
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	boolean hasSamOption() {
		return options.has("s") || options.has("sam");
	}

	boolean hasBamOption() {
		return options.has("b") || options.has("bam");
	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	String[] getInputFileNames() {
		return inputFileNames;
	}

	String[] getDirNames() {
		return this.dirNames;
	}
	
	String getValidation() {
		if ( options.has("validation")){
			return (String) options.valueOf("validation");
		} else return null;
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
//		if (hasBamOption() && hasSamOption()) {
//			throw new Exception("Only one output file type can be specified");
//		}
	}
	
	public boolean hasCreateIndexOption() {
		return options.has("bai");
	}
	public boolean hasSingleLevel() {
		return options.has("singlelevel");
	}
	
	String getSplit1String() {
		if ( options.has("split1")){
			return (String) options.valueOf("split1");
		} else return null;
	}
	String getSplit2String() {
		if ( options.has("split2")){
			return (String) options.valueOf("split2");
		} else return null;
	}
	Integer[] getSplit1ZCs() {
		return split1zc;
	}
	Integer[] getSplit2ZCs() {
		return split2zc;
	}
	public String getCommandLine() {
		return QLogger.reconstructCommandLine("qsplit", args);
	}
}
