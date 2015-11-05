/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.qcmg.common.util.FileUtils;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

 
public class Options {

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
	
	private final File testVcf;
	private final File controlVcf;
	private final List<File> pindelVcfs = new ArrayList<File>(); 
	private final String runMode; 
	
	private String testSampleid ;
	private String controlSampleid ;
	private String donorid; 
 
	
			
	private String commandLine;
	
	private String filterQuery;

	public Options(final String[] args) throws IOException, IOException, Q3IndelException  {

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		
		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);
		parser.accepts("i", Messages.getMessage("OPTION_INI_FILE")).withRequiredArg().ofType(String.class).describedAs("in file ");
 		
		options = parser.parse(args);
		log = (String) options.valueOf("log");
		loglevel = (String) options.valueOf("loglevel");		
		commandLine = Messages.reconstructCommandLine(args) ;
		
		
 	
		Ini iniFile = null; 
		if(options.has("i") &&
				FileUtils.isFileTypeValid((String) options.valueOf("i"), "ini")
				)			
		iniFile =  new Ini( new File(  (String) options.valueOf("i")));
		 
		reference = new File( IniFileUtil.getInputFile(iniFile, "ref") );
		output =new File(  IniFileUtil.getOutputFile(iniFile, "vcf"));		
		testBam = new File(IniFileUtil.getInputFile(iniFile, "testBam")) ;	
		controlBam = new File( IniFileUtil.getInputFile(iniFile, "controlBam")) ;	
		testVcf = new File( IniFileUtil.getInputFile(iniFile, "testvcf") );
		controlVcf = new File(IniFileUtil.getInputFile(iniFile, "controlvcf"));
		String[] inputs = IniFileUtil.getInputFiles(iniFile, "inputVcf");
		for(int i = 0; i < inputs.length; i ++)
			pindelVcfs.add(new File(inputs[i]));
		
		runMode =  IniFileUtil.getEntry(iniFile, "parameters", "runMode");
		nearbyIndelWindow = Integer.parseInt( IniFileUtil.getEntry(iniFile, "parameters", "window.nearbyIndel"));
		nearbyHomopolymer = Integer.parseInt( IniFileUtil.getEntry(iniFile, "parameters", "window.homopolymer"));
		softClipWindow = Integer.parseInt( IniFileUtil.getEntry(iniFile, "parameters", "window.softClip"));
		threadNo = Integer.parseInt( IniFileUtil.getEntry(iniFile, "parameters", "threadNo"));
		filterQuery =  IniFileUtil.getEntry(iniFile, "parameters", "filter");
		
		testSampleid = IniFileUtil.getEntry(iniFile, "id", "testSample");
		controlSampleid = IniFileUtil.getEntry(iniFile, "id", "controlSample");
		donorid = IniFileUtil.getEntry(iniFile, "id", "donorid");
 
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

	public String getRunMode(){
		return runMode; 
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
			if (testBam == null || !testBam.exists())  
			throw new Q3IndelException("FILE_EXISTS_ERROR", testBam.getAbsolutePath());
		 
			if (controlBam == null || !controlBam.exists()) 
			throw new Q3IndelException("FILE_EXISTS_ERROR", controlBam.getAbsolutePath());	
			
			
			if ("gatk".equalsIgnoreCase(runMode)){ 
				if(testVcf == null || !testVcf.exists())
					throw new Q3IndelException("FILE_EXISTS_ERROR","(test gatk vcf) " + testVcf.getAbsolutePath());
				if(controlVcf == null || !controlVcf.exists())
					throw new Q3IndelException("FILE_EXISTS_ERROR","(control gatk vcf) " + controlVcf.getAbsolutePath());
				 
			}else if ("gatk".equalsIgnoreCase(runMode)){ 
				if(pindelVcfs.size() == 0)
					throw new Q3IndelException("INPUT_OPTION_ERROR","(pindel input vcf) not specified" );
				
				for(int i = 0; i < pindelVcfs.size(); i ++)
				if ( pindelVcfs.get(i) != null && ! pindelVcfs.get(i).exists())  
					throw new Q3IndelException("FILE_EXISTS_ERROR","(control indel vcf) " + pindelVcfs.get(i).getAbsolutePath());				
			}else
				throw new Q3IndelException("UNKNOWN_RUNMODE_ERROR", runMode);			
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
	
	public List<File> getInputVcfs() {		 
		return pindelVcfs; 
	}
	
	public File getTestInputVcf() {		 
		return testVcf; 
	}
	public File getControlInputVcf() {		 
		return controlVcf; 
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

