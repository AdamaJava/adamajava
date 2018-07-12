/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package au.edu.qimr.vcftools;

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
	private static final String GOLD_STANDARD_DESCRIPTION = Messages.getMessage("GOLD_STANDARD_DESCRIPTION");
	private static final String VCF_DESCRIPTION = Messages.getMessage("VCF_DESCRIPTION");
	private static final String SOMATIC_DESCRIPTION = Messages.getMessage("SOMATIC_DESCRIPTION");
	private static final String GERMLINE_DESCRIPTION = Messages.getMessage("GERMLINE_DESCRIPTION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
//	private final List<String> fileNames;
	private final String[] vcfs;
	private final String outputFileName;
	private final String tiledRefFileName;
	private final String refFileName;
	private final String cosmicFileName;
	private final String dbSnpFileName;
	private final String log;
	private final Integer minBinSize;
	private final Integer minFragSize;
	private final Integer minReadPercentage;
	private final Integer tiledDiffThreshold;
	private final Integer swDiffThreshold;
	private final Integer tileMatchThreshold;
	private final Integer maxIndelLength;
	private final String xml;
	private final String logLevel;
	private final String uuid;

	@SuppressWarnings("unchecked")
	Options(final String[] args) throws Exception {
//		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
//		parser.acceptsAll(asList("v", "version"), VERSION_DESCRIPTION);
		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("somatic", SOMATIC_DESCRIPTION);
		parser.accepts("germline", GERMLINE_DESCRIPTION);
		
		parser.accepts("xml", INPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("output", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("tiledRef", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("ref", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("bedFile", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("cosmic", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("dbsnp", OUTPUT_FILE_DESCRIPTION).withRequiredArg().ofType(String.class);
//		parser.accepts("exclude", EXCLUDES_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("minBinSize", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("minFragmentSize", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("minReadPercentage", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("tiledDiffThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("swDiffThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("tileMatchThreshold", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("maxIndelLength", MIN_BIN_SIZE_OPTION_DESCRIPTION).withRequiredArg().ofType(Integer.class);
		parser.accepts("goldStandard", GOLD_STANDARD_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("uuid", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("vcf", VCF_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.posixlyCorrect(true);

		options = parser.parse(args);
		
		// log file
		log = (String) options.valueOf("log");
		// logLevel
		logLevel = (String) options.valueOf("loglevel");
		
		minBinSize =  (Integer) options.valueOf("minBinSize");
		minFragSize =  (Integer) options.valueOf("minFragmentSize");
		minReadPercentage =  (Integer) options.valueOf("minReadPercentage");
		tiledDiffThreshold =  (Integer) options.valueOf("tiledDiffThreshold");
		swDiffThreshold =  (Integer) options.valueOf("swDiffThreshold");
		tileMatchThreshold =  (Integer) options.valueOf("tileMatchThreshold");
		maxIndelLength =  (Integer) options.valueOf("maxIndelLength");
		xml = (String) options.valueOf("xml");
		
		// inputs
		List<String> inputs = (List<String>) options.valuesOf("vcf");
		vcfs = new String[inputs.size()];
		inputs.toArray(vcfs);
		
		
		// output
		outputFileName = (String) options.valueOf("output");
		tiledRefFileName = (String) options.valueOf("tiledRef");
		refFileName = (String) options.valueOf("ref");
		cosmicFileName = (String) options.valueOf("cosmic");
		dbSnpFileName = (String) options.valueOf("dbsnp");
		uuid = (String) options.valueOf("uuid");
		
	}

	boolean hasVersionOption() {
		return options.has("version");
	}

	boolean hasHelpOption() {
		return options.has("help");
	}
	boolean hasSomaticOption() {
		return options.has("somatic");
	}
	boolean hasGermlineOption() {
		return options.has("germline");
	}
	boolean hasMinBinSizeOption() {
		return options.has("minBinSize");
	}
	boolean hasMinFragmentSizeOption() {
		return options.has("minFragmentSize");
	}
	boolean hasMinReadPercentageSizeOption() {
		return options.has("minReadPercentage");
	}
	boolean hasTiledDiffThresholdOption() {
		return options.has("tiledDiffThreshold");
	}
	boolean hasSwDiffThresholdOption() {
		return options.has("swDiffThreshold");
	}
	boolean hasTileMatchThresholdOption() {
		return options.has("tileMatchThreshold");
	}
	boolean hasMaxIndelLengthOption() {
		return options.has("maxIndelLength");
	}
	boolean hasAmpliconBoundaryOption() {
		return options.has("ampliconBoundary");
	}
	
	boolean hasNoHtmlOption() {
		return options.has("nohtml");
	}
	
	boolean hasLogOption() {
		return options.has("log");
	}
	boolean hasUUIDOption() {
		return options.has("uuid");
	}
	
	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}
	
	Integer getMinBinSize() {
		return minBinSize;
	}
	Integer getMinFragmentSize() {
		return minFragSize;
	}
	Integer getMinReadPercentageSize() {
		return minReadPercentage;
	}
	Integer getTiledDiffThreshold() {
		return tiledDiffThreshold;
	}
	Integer getSwDiffThreshold() {
		return swDiffThreshold;
	}
	Integer getTileMatchThreshold() {
		return tileMatchThreshold;
	}
	Integer getMaxIndelLength() {
		return maxIndelLength;
	}
	String getXml() {
		return xml;
	}
	Optional<String> getGoldStandard() {
		if (options.has("goldStandard")) {
			return Optional.of((String) options.valueOf("goldStandard"));
		}
		return Optional.empty();
	}
	String getCosmicFile() {
		return cosmicFileName;
	}
	String getDbSnpFile() {
		return dbSnpFileName;
	}
	
	String getBedFile() {
		return  (String) options.valueOf("bedFile");
	}
	
	String getUUID() {
		return uuid;
	}

	String[] getVcfs() {
		return vcfs;
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
	public String getTiledRefFileName() {
		return tiledRefFileName;
	}
	public String getRefFileName() {
		return refFileName;
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
