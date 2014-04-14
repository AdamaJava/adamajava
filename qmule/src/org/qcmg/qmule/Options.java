/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.qcmg.qmule.Messages;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * The Class Options.
 */
public final class Options {
	
	public enum Ids{
		PATIENT,
		SOMATIC_ANALYSIS,
		GEMLINE_ANALYSIS,
		TUMOUR_SAMPLE,
		NORMAL_SAMPLE;
	}
	
	/** The Constant HELP_DESCRIPTION. */
	private static final String HELP_DESCRIPTION = Messages
			.getMessage("HELP_OPTION_DESCRIPTION");

	/** The Constant VERSION_DESCRIPTION. */
	private static final String VERSION_DESCRIPTION = Messages
			.getMessage("VERSION_OPTION_DESCRIPTION");
	
	/** The Constant INPUT_DESCRIPTION. */
	private static final String INPUT_DESCRIPTION = Messages
			.getMessage("INPUT_OPTION_DESCRIPTION");
	
	/** The Constant OUTPUT_DESCRIPTION. */
	private static final String OUTPUT_DESCRIPTION = Messages
			.getMessage("OUTPUT_OPTION_DESCRIPTION");
	
	/** The parser. */
	private final OptionParser parser = new OptionParser();
	
	/** The options. */
	private final OptionSet options;
	
	/** The command line. */
	private final String commandLine;
	
	/** The input file names. */
	private String[] inputFileNames;
	
	/** The output file names. */
	private  String[] outputFileNames;
	
	/** The log file  */
	private  String logFile;
	
	/** The log level */
	private  String logLevel;
	
	private  String patientId;
	private  String somaticAnalysisId;
	private  String germlineAnalysisId;
	private   String normalSampleId;
	private  String tumourSampleId;
	private   String position;
	private   String pileupFormat;
	private int normalCoverage;
	private int tumourCoverage;
	private int minCoverage;
	private  String mafMode;
	private  String gff;
	private  String fasta;
	private   String[] gffRegions;
	private int noOfBases;
	private   String mode;


	private String column;

	private String annotation;

	private String features;

	private String tumour;

	private String normal;

	private String analysis;

	/**
	 * Instantiates a new options.
	 *
	 * @param args the args
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		commandLine = Messages.reconstructCommandLine(args);
		
//		parser.accepts("qmule", "Tool").withRequiredArg().ofType(String.class).describedAs("tool name");

		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.accepts("input", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("inputfile");
		parser.accepts("log", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("logfile");
		parser.accepts("loglevel", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("loglevel");
		parser.accepts("help", HELP_DESCRIPTION);
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("patientId", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("patientId");
		parser.accepts("somaticAnalysisId", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("somaticAnalysisId");
		parser.accepts("germlineAnalysisId", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("germlineAnalysisId");
		parser.accepts("normalSampleId", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("normalSampleId");
		parser.accepts("tumourSampleId", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("tumourSampleId");
		parser.accepts("position", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("position");
		parser.accepts("pileupFormat", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("pileupFormat");
		parser.accepts("normalCoverage", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("normalCoverage");
		parser.accepts("tumourCoverage", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("tumourCoverage");
		parser.accepts("minCoverage", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class)
		.describedAs("minCoverage");
		parser.accepts("mafMode", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("mafMode");
		parser.accepts("mode", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("mode");
		parser.accepts("column", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("column");
		parser.accepts("annotation", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("annotation");
		parser.accepts("gffFile", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("gffFile");
		parser.accepts("fasta", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("fasta");
		parser.accepts("feature", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("feature");
		parser.accepts("tumour", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("tumour");
		parser.accepts("normal", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("normal");
		parser.accepts("analysis", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class)
		.describedAs("analysis");
		parser.accepts("verifiedInvalid", INPUT_DESCRIPTION);
		parser.accepts("gffRegions", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',').describedAs("gffRegions");
		parser.accepts("noOfBases", INPUT_DESCRIPTION).withRequiredArg().ofType(Integer.class).describedAs("noOfBases");
		parser.accepts("proportion", Messages
				.getMessage("PROPORTION_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
		parser.accepts("stranded", Messages
				.getMessage("STRANDED_OPTION_DESCRIPTION"));		
		parser.accepts("compareAll",Messages.getMessage("COMPAREALL_OPTION"));
		
		parser.posixlyCorrect(true);
		options = parser.parse(args);
		
		// throw exception if tool name has not been supplied
//		tool = (String) options.valueOf("qmule");
//		if (null == tool) throw new Exception("Tool name must be specified");

		List inputList = options.valuesOf("input");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);

		List outputList = options.valuesOf("output");
		outputFileNames = new String[outputList.size()];
		outputList.toArray(outputFileNames);
		
		logFile = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		patientId = (String) options.valueOf("patientId");
		somaticAnalysisId = (String) options.valueOf("somaticAnalysisId");
		germlineAnalysisId = (String) options.valueOf("germlineAnalysisId");
		normalSampleId = (String) options.valueOf("normalSampleId");
		tumourSampleId = (String) options.valueOf("tumourSampleId");
		
		// WiggleFromPileup specific options
		pileupFormat = (String) options.valueOf("pileupFormat");
		if (null != options.valueOf("normalCoverage"))
			normalCoverage = (Integer) options.valueOf("normalCoverage");
		if (null != options.valueOf("tumourCoverage"))
			tumourCoverage = (Integer) options.valueOf("tumourCoverage");
		// end of WiggleFromPileup specific options
		
		//compareReferenceRegions
		mode = (String) options.valueOf("mode");
		column = (String) options.valueOf("column");
		annotation = (String) options.valueOf("annotation");
		features = (String) options.valueOf("feature");
		position = (String) options.valueOf("position");
		mafMode = (String) options.valueOf("mafMode");
		
		gff = (String) options.valueOf("gffFile");
		fasta = (String) options.valueOf("fasta");
		
		tumour = (String) options.valueOf("tumour");
		normal = (String) options.valueOf("normal");
		analysis = (String) options.valueOf("analysis");
		
		// gffRegions
		List<String> gffRegionsArgs = (List<String>) options.valuesOf("gffRegions");
		gffRegions = new String[gffRegionsArgs.size()];
		gffRegionsArgs.toArray(gffRegions);

		// MafAddCPG specific
		if (null != options.valueOf("noOfBases"))
			noOfBases = (Integer) options.valueOf("noOfBases");
		
		// qsignature
		if (null != options.valueOf("minCoverage"))
			minCoverage = (Integer) options.valueOf("minCoverage");
		
		//subSample
 		 		 
		 
		
	}

	/**
	 * 
	 * @param className
	 * @param args
	 * @throws Exception
	 */
	public Options( final Class myclass,  final String[] args) throws Exception {
		commandLine = Messages.reconstructCommandLine(args);
		
		parser.acceptsAll( asList("h", "help"),   HELP_DESCRIPTION );	 
		parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);				
		parser.acceptsAll( asList("i", "input"), INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("input");
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("outputfile");
//		parser.accepts("log", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("logfile");
//		parser.accepts("loglevel", INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("loglevel");
		
		if( myclass.equals(AlignerCompare.class) ){ 
			parser.accepts("compareAll",Messages.getMessage("COMPAREALL_OPTION"));
 			parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_AlignerCompare")).withRequiredArg().ofType(String.class).describedAs("output");
			
		}
		options = parser.parse(args);
		
		List inputList = options.valuesOf("input");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);

		List outputList = options.valuesOf("output");
		outputFileNames = new String[outputList.size()];
		outputList.toArray(outputFileNames);		
		
	}
	
	public String getTumour() {
		return tumour;
	}

	public void setTumour(String tumour) {
		this.tumour = tumour;
	}

	public String getNormal() {
		return normal;
	}

	public void setNormal(String normal) {
		this.normal = normal;
	}

	public String getAnalysis() {
		return analysis;
	}

	public void setAnalysis(String analysis) {
		this.analysis = analysis;
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
		return options.has("o") || options.has("output");
	}

	/**
	 * Checks for version option.
	 *
	 * @return true, if successful
	 */
	public boolean hasVersionOption() {
		return options.has("version");
	}
	
	public boolean getIncludeInvalid() {
		return options.has("verifiedInvalid");
	}

	/**
	 * Checks for help option.
	 *
	 * @return true, if successful
	 */
	public boolean hasHelpOption() {
		return options.has("help");
	}
	
	public boolean hasCompareAllOption() {
		return options.has("compareAll");
	}
	
	/**
	 * Checks for log option.
	 *
	 * @return true, if successful
	 */
	public boolean hasLogOption() {
		return options.has("log");
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
	 * Gets the output file names.
	 *
	 * @return the output file names
	 */
	public String[] getOutputFileNames() {
		return outputFileNames;
	}

	/**
	 * Gets the command line.
	 *
	 * @return the command line
	 */
	public String getCommandLine() {
		return commandLine;
	}
	
	public boolean hasStrandedOption() {
		return options.has("stranded");
	}
	
	public String getPosition() {
		return position;
	}
	public String getPileupFormat() {
		return pileupFormat;
	}
	public int getNormalCoverage() {
		return normalCoverage;
	}
	public int getTumourCoverage() {
		return tumourCoverage;
	}
	public int getMinCoverage() {
		return minCoverage;
	}
	public String getMafMode() {
		return mafMode;
	}
	public String getGffFile() {
		return gff;
	}
	public String getFastaFile() {
		return fasta;
	}
	
	public String getMode() {
		return mode;
	}

	//subSample
	public double getPROPORTION() throws Exception{
		if(options.has("proportion")){			
			double prop = Double.parseDouble(  (String) options.valueOf("proportion")  );
			if(prop > 0 && prop <= 1){
				return  prop;
				
			} 
		}		
		throw new Exception("no proportion are specified"); 
	}
	
//	public String getToolName() {
//		return tool;
//	}

	/**
	 * Display help.
	 *
	 * @throws Exception the exception
	 */
	public void displayHelp() throws IOException {
		parser.printHelpOn(System.out);
	}

	/**
	 * Detect bad options.
	 *
	 * @throws Exception the exception
	 */
	public void detectBadOptions() throws Exception {
		if (hasNonOptions()) {
			throw new Exception("ALL_ARGUMENTS_MUST_BE_OPTIONS");
		}
		if (hasOutputOption() && 1 != getOutputFileNames().length) {
			throw new Exception("MULTIPLE_OUTPUT_FILES_SPECIFIED");
		}
		if (!hasInputOption()) {
			throw new Exception("MISSING_INPUT_OPTIONS");
		}
	}

	public String getLogFile(){	 
		
		return logFile;
	}

	public String getLogLevel(){
 
		return logLevel;
	}
	
	public Properties getIds() {
		Properties props = new Properties();
		props.put(Ids.PATIENT, patientId);
		props.put(Ids.SOMATIC_ANALYSIS, somaticAnalysisId);
		props.put(Ids.GEMLINE_ANALYSIS, germlineAnalysisId);
		props.put(Ids.NORMAL_SAMPLE, normalSampleId);
		props.put(Ids.TUMOUR_SAMPLE, tumourSampleId);
		return props;
	}

	public String[] getGffRegions() {
		
		return gffRegions;
	}

	public int getNoOfBases() {
		
		return noOfBases;
	}

	public String getColumn() {
		return column;
	}

	public String getAnnotation() {
		return annotation;
	}

	public String[] getFeature() {
		if (features != null) {
		return features.split(",");
		} 
		return null;		
	}

}
