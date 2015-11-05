/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

 
public class CopyOfOptions {

	private static final String HELP_OPTION = Messages.getMessage("OPTION_HELP");	
	private static final String VERSION_OPTION = Messages.getMessage("OPTION_VERSION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	
	private final String log;
	private final String loglevel;
	public int nearbyIndelWindow = 3;
	public int nearbyHomopolymer = 10;
	public int softClipWindow = 13;
	public int threadNo = 5;
	
	private final File testBam;
	private final File controlBam;
	private final File reference;	
	private final File output;
	
	private final File inputVcf1;
	private final File inputVcf2;
	
	private String testSampleid = "TEST";
	private String controlSampleid = "CONTROL";
	private String donorid; 
	
	private String commandLine;
	
	private String filterQuery;

	public CopyOfOptions(final String[] args) throws Q3IndelException {

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		parser.accepts("r", Messages.getMessage("OPTION_REFERENCE")).withRequiredArg().ofType(String.class).describedAs("reference.fa");
		parser.accepts("filter", Messages.getMessage("OPTION_FILTER")).withRequiredArg().ofType(String.class).describedAs("qbamfilter");
		parser.accepts("dup", Messages.getMessage("OPTION_DUPS"));

		parser.accepts("o", Messages.getMessage("OPTION_OUTPUT")).withRequiredArg().ofType(String.class).describedAs("output");	
		parser.accepts("i", Messages.getMessage("OPTION_VCF_INPUT")).withRequiredArg().ofType(String.class).describedAs("test_indel_vcf ");
		parser.accepts("tb", Messages.getMessage("OPTION_INPUT_TEST")).withRequiredArg().ofType(String.class).describedAs("input_test_bam");
		parser.accepts("cb", Messages.getMessage("OPTION_INPUT_CONTROL")).withRequiredArg().ofType(String.class).describedAs("input_control_bam");
		
		
		//indel options
		parser.accepts("softwindow", Messages.getMessage("OPTION_SOFTCLIP")).withRequiredArg().ofType(Integer.class).describedAs("soft_clip_window");
		parser.accepts("homowindow", Messages.getMessage("OPTION_WINDOW")).withRequiredArg().ofType(Integer.class).describedAs("homopolymer_window");
		parser.accepts("nearbywindow", Messages.getMessage("OPTION_NEAR_INDELS")).withRequiredArg().ofType(Integer.class).describedAs("nearby_indel_bases");
		parser.accepts("n", Messages.getMessage("OPTION_THREAD_NO")).withRequiredArg().ofType(Integer.class).describedAs("thread number");

		parser.accepts("testSample", Messages.getMessage("OPTION_TEST_SAMPLE")).withRequiredArg().ofType(String.class).describedAs("test sample id");
		parser.accepts("controlSample", Messages.getMessage("OPTION_CONTROL_SAMPLE")).withRequiredArg().ofType(String.class).describedAs("control sample id");
		parser.accepts("donor", "DONOR ID").withRequiredArg().ofType(String.class).describedAs("donor id");
		
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	
		options = parser.parse(args);
		
		log = (String) options.valueOf("log");
		

		
		loglevel = (String) options.valueOf("loglevel");		
		commandLine = Messages.reconstructCommandLine(args) ;
		
		if(options.has("r"))
			reference = new File( (String) options.valueOf("r"));
		else
			reference = null; 
				
		if(options.has("o"))			
			output =new File(  (String) options.valueOf("o"));
		else
			output = null; 
		
		if(options.has("tb"))
			testBam = new File( (String) options.valueOf("tb")) ;
		else
			testBam = null; 
		
		if(options.has("cb"))
			controlBam = new File( (String) options.valueOf("cb")) ;	
		else
			controlBam = null; 
		
		if(options.has("i")){
			 List<String> vcfs =  (List<String>) options.valuesOf("i");
			 if(vcfs.size() == 1){
				 inputVcf1  = new File(vcfs.get(0));
			 	 inputVcf2 = null; 
			 }else if(vcfs.size() == 2){
				 inputVcf1  = new File(vcfs.get(0));
				 inputVcf2 = new File(vcfs.get(1));
			 }else{
				 throw new Q3IndelException("INPUT_OPTION_ERROR", "please specify one or two input vcf files!");
			 }
		}else{
			 inputVcf1  = null; 
		 	 inputVcf2 = null; 			
		}
		

		if (options.has("n")) {
			this.threadNo = (Integer) options.valueOf("n");
		}
		
		if (options.has("softwindow")) {
			this.softClipWindow = (Integer) options.valueOf("softwindow");
		}
		if (options.has("homowindow")) {
			this.nearbyHomopolymer = (Integer)options.valueOf("homowindow");
		}
		if (options.has("nearbywindow")) {
			this.nearbyIndelWindow = (Integer)options.valueOf("nearbywindow");
		}		
		
		
		if(options.has("testSample"))
			this.testSampleid = (String) options.valueOf("testSample");
		
		if(options.has("controlSample"))
			this.controlSampleid = (String) options.valueOf("controlSample");
		
		
		System.out.println(options.valueOf("donor"));
 		if(options.has("donor"))
// 			this.donorid = "donor";
			this.donorid = (String) options.valueOf("donor");
		
 		if (options.has("filter")) {
			this.filterQuery = (String) options.valueOf("filter");
		}
 		
 		detectBadOptions();		

	 
		
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

	public void detectBadOptions() throws Q3IndelException {
		if (!hasHelpOption() && !hasVersionOption()) { 

			if (output.exists()) {					
				throw new Q3IndelException("OUTPUT_EXISTS", output.getAbsolutePath());
			}
	 
			checkReference();
			if (!testBam.exists())  
			throw new Q3IndelException("FILE_EXISTS_ERROR", testBam.getAbsolutePath());
		 
			if (!controlBam.exists()) 
			throw new Q3IndelException("FILE_EXISTS_ERROR", controlBam.getAbsolutePath());							
 			 
		 	if (inputVcf2 != null && !inputVcf2.exists())  
		 		throw new Q3IndelException("FILE_EXISTS_ERROR","(control indel vcf) " + inputVcf2.getAbsolutePath());
		 	if (inputVcf1 != null && !inputVcf1.exists())  
		 		throw new Q3IndelException("FILE_EXISTS_ERROR", "(test indel vcf) " + inputVcf1.getAbsolutePath());			 
		}				
	}

	private void checkReference() throws Q3IndelException {
		if (!reference.exists()) {
			throw new Q3IndelException("NO_REF_FILE", reference.getAbsolutePath());
		}	
		File indexFile = new File(reference.getAbsolutePath() + ".fai");		

		if (!indexFile.exists()) {
			throw new Q3IndelException("FASTA_INDEX_ERROR", reference.getAbsolutePath());
		}
	}


	public File getTestBam() {
		return testBam;
	}
	public File getControlBam() {
		return controlBam;
	}	
	
	public File getFirstInputVcf() {		 
		return inputVcf1; 
	}
	
	public File getSecondInputVcf() {		 
		return inputVcf2; 
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
	
	public int getThreadNo(){
		return threadNo;
	}

	public String getCommandLine() {	return commandLine; }		
	public String getDonorId(){ return donorid; }
	public String getControlSample(){ return  this.controlSampleid; }
	public String getTestSample(){ return this.testSampleid; }

 
}

