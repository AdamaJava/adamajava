/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;
import java.util.Vector;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

/**
 * The Class Options.
 */
public final class Options {
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String FORCE_DESCRIPTION = Messages.getMessage("FORCE_OPTION_DESCRIPTION");
	private static final String REPLACE_DESCRIPTION = Messages.getMessage("REPLACE_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String INPUT_DESCRIPTION = Messages.getMessage("INPUT_OPTION_DESCRIPTION");
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String MERGE_DESCRIPTION = Messages.getMessage("MERGE_OPTION_DESCRIPTION");
	private static final String NUMBER_RECORDS_DESCRIPTION = Messages.getMessage("NUMBER_RECORDS_DESCRIPTION");
	private static final String LOG_OPTION_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	private static final String INDEX_OPTION_DESCRIPTION = Messages.getMessage("INDEX_OPTION_DESCRIPTION");
	private static final String VALIDATION_STRINGENCY_OPTION_DESCRIPTION = Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION");
	private static final String COMMENT_OPTION_DESCRIPTION = Messages.getMessage("COMMENT_OPTION_DESCRIPTION");	 
	private static final String TMPDIR_OPTION_DESCRIPTION = Messages.getMessage("TMPDIR_OPTION_DESCRIPTION");
	private static final String UUID_OPTION_DESCRIPTION = Messages.getMessage("UUID_OPTION_DESCRIPTION");
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String[] inputFileNames;
	private final String[] mergeFileNames;
	private final String[] outputFileNames;
	private final Integer[] numberRecords;
	private final List<GroupReplacement> groupReplacements = new Vector<GroupReplacement>();
	private final String[] groupReplacementStrings;
	private final String[] types;
	private final String log;
	private final String logLevel;
	private final String validation;	
	private final String[] comments;
	private final String tmpdir;

	/**
	 * Instantiates a new options.
	 * 
	 * @param args
	 *            the args
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		parser.acceptsAll(asList("h", "help"), HELP_DESCRIPTION);
		parser.acceptsAll(asList("r", "replace"), REPLACE_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("inputfile:oldgroup:newgroup");
		parser.acceptsAll(asList("f", "force"), FORCE_DESCRIPTION);
		parser.acceptsAll(asList("o", "output"), OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.acceptsAll(asList("i", "input"), INPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("inputfile");
		parser.acceptsAll(asList("m", "merge"), MERGE_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("mergefile");
		parser.acceptsAll(asList("v", "V", "version"), VERSION_DESCRIPTION);
		parser.acceptsAll(asList("n", "number"), NUMBER_RECORDS_DESCRIPTION).withRequiredArg().ofType(Integer.class).describedAs("numberRecords");
		parser.accepts("log", LOG_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel", LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);		
		parser.accepts("validation", VALIDATION_STRINGENCY_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class); 
		parser.accepts("tmpdir", TMPDIR_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class); 
		parser.accepts("co", COMMENT_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("uuid", UUID_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("bai", INDEX_OPTION_DESCRIPTION);
		
		options = parser.parse(args);

		List replaceList = options.valuesOf("r");
		for (Object value : replaceList) {
			GroupReplacement details = new GroupReplacement((String) value);
			groupReplacements.add(details);
		}

		List stringReplacementList = options.valuesOf("r");
		groupReplacementStrings = new String[stringReplacementList.size()];
		stringReplacementList.toArray(groupReplacementStrings);

		List inputList = options.valuesOf("i");
		inputFileNames = new String[inputList.size()];
		inputList.toArray(inputFileNames);

		List outputList = options.valuesOf("o");
		outputFileNames = new String[outputList.size()];
		outputList.toArray(outputFileNames);

		List mergeList = options.valuesOf("m");
		mergeFileNames = new String[mergeList.size()];
		mergeList.toArray(mergeFileNames);

		List numberRecordsList = options.valuesOf("n");
		numberRecords = new Integer[numberRecordsList.size()];
		numberRecordsList.toArray(numberRecords);

		List typesList = options.valuesOf("t");
		types = new String[typesList.size()];
		typesList.toArray(types);

		log = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		
		validation = (String) options.valueOf("validation");
		tmpdir = (String) options.valueOf("tmpdir");
		
		List coList = options.valuesOf("co");
		comments = new String[coList.size()];
		coList.toArray(comments);
	}

	/**
	 * Checks for input option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasCommentOption(){
		return options.has("co");
	}
	public boolean hasUUIDOption(){
		return options.has("uuid");
	}
	public boolean hasInputOption() {
		return options.has("i") || options.has("input");
	}

	public boolean hasRunTypeOption() {
		return options.has("t") || options.has("type");
	}

	public String[] getRiuTypes() {
		return types;
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
	 * Checks for merge option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasMergeOption() {
		return options.has("m") || options.has("merge");
	}

	/**
	 * Checks for version option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasVersionOption() {
		return options.has("v") || options.has("version");
	}

	/**
	 * Checks for help option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	/**
	 * Checks for force option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasForceOption() {
		return options.has("f") || options.has("force");
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
	 * Checks for number records option.
	 * 
	 * @return true, if successful
	 */
	public boolean hasNumberRecordsOption() {
		return options.has("n") || options.has("number");
	}

	String getLog() {
		return log;
	}

	String getLogLevel() {
		return logLevel;
	}
	String[] getComment(){
		if(comments == null)
			return null;
		
		return comments;
	}
	
	String getUUID() {
		return hasUUIDOption() ? (String) options.valueOf("uuid") : null;
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
		if (this.hasMergeOption()) {
			return mergeFileNames;
		}
		return outputFileNames;
	}

	/**
	 * Gets the number records.
	 * 
	 * @return the number records
	 */
	public Integer[] getNumberRecords() {
		return numberRecords;
	}

	/**
	 * Gets the group replacements.
	 * 
	 * @return the group replacements
	 */
	public List<GroupReplacement> getGroupReplacements() {
		return groupReplacements;
	}

	/**
	 * Gets the group replacement strings.
	 * 
	 * @return the group replacements
	 */
	public String[] getGroupReplacementStrings() {
		return groupReplacementStrings;
	}

	/**
	 * Display help.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void displayHelp() throws Exception {
		parser.printHelpOn(System.out);
	}

	/**
	 * Detect bad options.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void detectBadOptions() throws Exception {
		if (hasOutputOption() && hasMergeOption()) {
			throw new BamMergeException("MERGE_AND_OUTPUT_ERROR");
		}
		if (!hasOutputOption() && !hasMergeOption()) {
			throw new BamMergeException("MISSING_OUTPUT_MERGE_OPTIONS");
		}
		if (hasNonOptions()) {
			throw new BamMergeException("ALL_ARGUMENTS_MUST_BE_OPTIONS");
		}
		if (hasOutputOption() && 1 != getOutputFileNames().length) {
			throw new BamMergeException("MULTIPLE_OUTPUT_FILES_SPECIFIED");
		}
		if (hasNumberRecordsOption() && 1 != getNumberRecords().length) {
			throw new BamMergeException("MULTIPLE_NUMBER_RECORDS_SPECIFIED");
		}
		if (hasMergeOption() && 1 != getOutputFileNames().length) {
			throw new BamMergeException("MULTIPLE_MERGE_FILES_SPECIFIED");
		}
		if (!hasInputOption()) {
			throw new BamMergeException("MISSING_INPUT_OPTIONS");
		}
		if (!hasLogOption()) {
			throw new Exception("A log filename must be specified (using the --log option)");
		}
	}

	public boolean hasLogOption() {
		return options.has("log");
	}

	public boolean hasLogLevelOption() {
		return options.has("loglevel");
	}
	public boolean hasCreateIndexOption() {
		return options.has("bai");
	}
	
	public ValidationStringency getValidation() throws Exception{	
		
		if( options.has("validation")){
			if( options.valueOf("validation").toString().equalsIgnoreCase("LENIENT"))
				return ValidationStringency.LENIENT;
			else if( options.valueOf("validation").toString().equalsIgnoreCase("SILENT"))
				return ValidationStringency.SILENT;
			else if( options.valueOf("validation").toString().equalsIgnoreCase("STRICT"))
				return  ValidationStringency.STRICT;
			else
				throw new Exception("invalid validation option: " + options.valueOf("validation").toString() + " Possible values: {STRICT, LENIENT, SILENT}" );
		}

		return null;
	}
	
	public String getTmpDir()throws Exception{
    	if(tmpdir != null )  
    		if(! new File( tmpdir ).canWrite() )
	    			throw new Exception("the specified output directory for temporary file are not writable: " + tmpdir);
    		
    	
    	
		return tmpdir;   	
    }	
	
	
		
}
