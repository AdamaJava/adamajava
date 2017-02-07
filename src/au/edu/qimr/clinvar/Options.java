/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package au.edu.qimr.clinvar;

import java.util.List;
import java.util.Optional;

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
	private static final String MIN_BIN_SIZE_OPTION_DESCRIPTION = Messages.getMessage("MIN_BIN_SIZE_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String OUTPUT_FILE_DESCRIPTION = Messages.getMessage("OUTPUT_FILE_DESCRIPTION");
	private static final String INPUT_FILE_DESCRIPTION = Messages.getMessage("INPUT_FILE_DESCRIPTION");
	private static final String NO_HTML_DESCRIPTION = Messages.getMessage("NO_HTML_OPTION_DESCRIPTION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
//	private final List<String> fileNames;
	private final List<String> fastqsR1;
	private final List<String> fastqsR2;
	private boolean extendedFB = false;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		
		parser.accepts("xml", INPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("tiledRef", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("ref", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("bedFile", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("transcriptsFile", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("cosmic", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("dbsnp", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("minBinSize", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("minFragmentSize", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("minReadPercentage", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("tiledDiffThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("swDiffThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("tileMatchThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("multiMutationThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("maxIndelLength", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("ampliconBoundary", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("uuid", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("fastqsR1", TAGS_CHAR_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("fastqsR2", TAGS_CHAR_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("extendedFB", LOG_LEVEL_OPTION_DESCRIPTION);
		parser.accepts("bamFilterDepth", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.posixlyCorrect(true);

		options = parser.parse(args);
		
		// inputs
		fastqsR1 = (List<String>) options.valuesOf("fastqsR1");
		fastqsR2 = (List<String>) options.valuesOf("fastqsR2");
		
		
		extendedFB = options.has("extendedFB");
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
	
	Optional<Integer> getMinBinSize() {
		return  Optional.ofNullable((Integer) options.valueOf("minBinSize"));
	}
	Optional<Integer> getMinFragmentSize() {
		return  Optional.ofNullable((Integer) options.valueOf("minFragmentSize"));
	}
	Optional<Integer> getMinReadPercentageSize() {
		return  Optional.ofNullable( (Integer) options.valueOf("minReadPercentage"));
	}
	Optional<Integer> getTiledDiffThreshold() {
		return  Optional.ofNullable( (Integer) options.valueOf("tiledDiffThreshold"));
	}
	Optional<Integer> getSwDiffThreshold() {
		return  Optional.ofNullable( (Integer) options.valueOf("swDiffThreshold"));
	}
	Optional<Integer> getTileMatchThreshold() {
		return  Optional.ofNullable((Integer) options.valueOf("tileMatchThreshold"));
	}
	Optional<Integer> getMaxIndelLength() {
		return  Optional.ofNullable((Integer) options.valueOf("maxIndelLength"));
	}
	Optional<Integer> getAmpliconBoundary() {
		return  Optional.ofNullable( (Integer) options.valueOf("ampliconBoundary"));
	}
	Optional<Integer> getBamFilterDepth() {
		return  Optional.ofNullable( (Integer) options.valueOf("bamFilterDepth"));
	}
	
	Optional<String> getXml() {
		return Optional.ofNullable((String) options.valueOf("xml"));
	}
	Optional<String> getCosmicFile() {
		return Optional.ofNullable((String) options.valueOf("cosmic"));
	}
	Optional<String> getDbSnpFile() {
		return Optional.ofNullable( (String) options.valueOf("dbsnp"));
	}
	
	Optional<String> getBedFile() {
		return  Optional.ofNullable((String) options.valueOf("bedFile"));
	}
	
	Optional<String> getUUID() {
		return Optional.ofNullable( (String) options.valueOf("uuid"));
	}

	List<String> getFastqsR1() {
		return fastqsR1;
	}
	List<String> getFastqsR2() {
		return fastqsR2;
	}
	
	Optional<String> getLog() {
		return  Optional.ofNullable((String) options.valueOf("log"));
	}
	Optional<String> getLogLevel() {
		return Optional.ofNullable((String) options.valueOf("loglevel"));
	}
	public Optional<String> getOutputFileName() {
		return Optional.ofNullable((String) options.valueOf("output"));
	}
	public Optional<String> getTiledRefFileName() {
		return Optional.ofNullable((String) options.valueOf("tiledRef"));
	}
	public Optional<String> getRefFileName() {
		return Optional.ofNullable((String) options.valueOf("ref"));
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}
	
	Optional<String> getValidation() {
		return Optional.ofNullable((String) options.valueOf("validation"));
	}

	public Optional<String> getGeneTranscriptsFile() {
		return  Optional.ofNullable( (String) options.valueOf("transcriptsFile"));
	}

	public Optional<Integer> getMultiMutationThreshold() {
		return Optional.ofNullable((Integer) options.valueOf("multiMutationThreshold"));
	}
	
	public boolean runExtendedFB() {
		return extendedFB;
	}

}
