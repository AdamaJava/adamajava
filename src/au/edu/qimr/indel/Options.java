/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel;

import static java.util.Arrays.asList;


import java.io.File;


import joptsimple.OptionParser;
import joptsimple.OptionSet;

 
public class Options {

	private static final String HELP_OPTION = Messages.getMessage("OPTION_HELP");	
	private static final String VERSION_OPTION = Messages.getMessage("OPTION_VERSION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	
	private final String log;
	public int nearbyIndelWindow = 3;
	public int nearbyHomopolymer = 10;
	public int softClipWindow = 13;
	
	private File tumourBam;
	private File normalBam;
	private File reference;	
	private File tumourVcf;
	private File normalVcf;
	private File input;
	private File output;
	
	private String tumourSampleid;
	private String normalSampleid;
	private String dornorid; 
	
	private String commandLine;
	
	private String filterQuery;

	public Options(final String[] args) throws Exception {

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		parser.accepts("r", Messages.getMessage("OPTION_REFERENCE")).withRequiredArg().ofType(String.class).describedAs("reference.fa");
		parser.accepts("filter", Messages.getMessage("OPTION_FILTER")).withRequiredArg().ofType(String.class).describedAs("qbamfilter");
		parser.accepts("dup", Messages.getMessage("OPTION_DUPS"));

		parser.accepts("i", Messages.getMessage("OPTION_INPUT")).withRequiredArg().ofType(String.class).describedAs("input_bam");		
		parser.accepts("o", Messages.getMessage("OPTION_OUTPUT")).withRequiredArg().ofType(String.class).describedAs("output");	

		parser.accepts("tv", Messages.getMessage("OPTION_TUMOUR_INPUT")).withRequiredArg().ofType(String.class).describedAs("tumour_indel_vcf ");
		parser.accepts("nv", Messages.getMessage("OPTION_NORMAL_INPUT")).withRequiredArg().ofType(String.class).describedAs("normal_indle_vcf");
		parser.accepts("tb", Messages.getMessage("OPTION_INPUT_TUMOUR")).withRequiredArg().ofType(String.class).describedAs("input_tumour_bam");
		parser.accepts("nb", Messages.getMessage("OPTION_INPUT_NORMAL")).withRequiredArg().ofType(String.class).describedAs("input_normal_bam");
				
		parser.accepts("sc", Messages.getMessage("OPTION_SOFTCLIP")).withRequiredArg().ofType(Integer.class).describedAs("soft_clip_window");
		parser.accepts("hp", Messages.getMessage("OPTION_WINDOW")).withRequiredArg().ofType(Integer.class).describedAs("homopolymer_window");
		parser.accepts("n", Messages.getMessage("OPTION_NEAR_INDELS")).withRequiredArg().ofType(Integer.class).describedAs("nearby_indel_bases");

		parser.accepts("tumorSample", Messages.getMessage("OPTION_TUMOUR_SAMPLE")).withRequiredArg().ofType(Integer.class).describedAs("tumour sample id");
		parser.accepts("normalSample", Messages.getMessage("OPTION_NORMAL_SAMPLE")).withRequiredArg().ofType(Integer.class).describedAs("normal sample id");

		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	
		options = parser.parse(args);
		
		log = (String) options.valueOf("log");
		
		if(hasHelpOption())
			return; 
		
//		String loglevel = (String) options.valueOf("loglevel");
		
		commandLine = Messages.reconstructCommandLine(args) ;
//		if (loglevel == null) { loglevel = "INFO"; }		
		
		reference = new File( (String) options.valueOf("r")); 
		output =new File(  (String) options.valueOf("o"));
//		input = new File( (String) options.valueOf("i"));
		tumourBam = new File( (String) options.valueOf("tb")) ;
		normalBam = new File( (String) options.valueOf("nb")) ;			
		tumourVcf = new File( (String) options.valueOf("tv")); 
		normalVcf = new File( (String) options.valueOf("nv"));
 
		//indel options
		if (options.has("sc")) {
			softClipWindow = (Integer) options.valueOf("sc");
		}
		if (options.has("hp")) {
			nearbyHomopolymer = (Integer)options.valueOf("hp");
		}
		if (options.has("n")) {
			nearbyIndelWindow = (Integer)options.valueOf("n");
		}		
 		if (options.has("filter")) {
			filterQuery = (String) options.valueOf("filter");
		}
 		
 		

	//have bug inside	
//		detectBadOptions();		
	}	

	public boolean includeDuplicates() {
		if (options.has("dup")) {
			return true;
		}
		return false;
	}
	

	public String getFilterQuery() {
		return filterQuery;
	}


 
	public String getLog() {
		return log;
	}
	public String getLogLevel() {
		return (String) options.valueOf("loglevel");
	}

	public OptionSet getOptions() {
		return options;
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasLogOption() {		
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	public void detectBadOptions() throws Q3PileupException {
		if (!hasHelpOption() && !hasVersionOption()) { 

			if (output.exists()) {					
				throw new Q3PileupException("OUTPUT_EXISTS", output.getAbsolutePath());
			}
	 
			checkReference();
			if (!tumourBam.exists())  
			throw new Q3PileupException("FILE_EXISTS_ERROR", tumourBam.getAbsolutePath());
		 
			if (!normalBam.exists()) 
			throw new Q3PileupException("FILE_EXISTS_ERROR", normalBam.getAbsolutePath());				
			
			
			if(input != null && !input.exists()){
				throw new Q3PileupException("FILE_EXISTS_ERROR", input.getAbsolutePath());
				
			}else{			 
			 	if (normalVcf != null || !normalVcf.exists())  
			 		throw new Q3PileupException("FILE_EXISTS_ERROR","(normal indel vcf) " + normalVcf.getAbsolutePath());
			 	if (tumourVcf != null || !tumourVcf.exists())  
			 		throw new Q3PileupException("FILE_EXISTS_ERROR", "(tumour indel vcf) " + tumourVcf.getAbsolutePath());

			}
		}				
	}

	private void checkReference() throws Q3PileupException {
		if (!reference.exists()) {
			throw new Q3PileupException("NO_REF_FILE", reference.getAbsolutePath());
		}	
		File indexFile = new File(reference.getAbsolutePath() + ".fai");		

		if (!indexFile.exists()) {
			throw new Q3PileupException("FASTA_INDEX_ERROR", reference.getAbsolutePath());
		}
	}


	public File getTumourBam() {
		return tumourBam;
	}
	public File getNormalBam() {
		return normalBam;
	}	
	
	public File getNormalInputVcf() {		 
		return normalVcf; 
	}
	
	public File getTumourInputVcf() {		 
		return tumourVcf; 
	}
	
	public File getInputVcf() {
		return input;
	}
	public File getOutput() {
		return output;
	}
	public File getReference() {
		return reference;
	}

	public int getNearbyIndelWindow() {
		return nearbyIndelWindow;
	}

	public int getNearbySoftClipWindow(){
		return softClipWindow;
	}
	public int getNearbyHomopolymerWindow() {
		return nearbyHomopolymer;
	}

	public int getSoftClipWindow() {
		return softClipWindow;
	}

	public String getTumourSample(){		
		return (String) options.valueOf("tumorSample");
	}
	
	public String getNormalSample(){		
		return (String) options.valueOf("normalSample");
	}

	public String getCommandLine() {	return commandLine; }	

 
}

