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
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Options {

	private static final String HELP_OPTION = Messages.getMessage("OPTION_HELP");	
	private static final String VERSION_OPTION = Messages.getMessage("OPTION_VERSION");
	
	public static String ini_secIOs = "IOs";
	public static String ini_secIDs = "ids";
	public static String ini_secParameter = "parameters";
	public static String ini_secRule = "rules";
	
	public static String RUNMODE_GATK = "gatk";
	public static String RUNMODE_DEFAULT = "pindel";
	public static String RUNMODE_GATKTEST = "test";
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private QLogger logger ;
	private QExec qexec;
	
	private String log;
	private String loglevel;
	public int nearbyIndelWindow = 3;
	public int softClipWindow = 13;
	public int threadNo = 5;
	
	private File testBam;
	private File controlBam;
	private File reference;	
	private File output;
	
	private File testVcf;
	private File controlVcf;
	private final List<File> pindelVcfs = new ArrayList<File>(); 
	private String runMode; 
	
	private String testSampleid ;
	private String controlSampleid ;
	private String donorid; 
	private String analysisid; 
	private int gematic_nns;
	private float gematic_soi; 
	private int max_events;
//	private boolean exdup;
				
	private String filterQuery;

	public Options(final String[] args) throws IOException, Q3IndelException  {
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		parser.accepts("i", Messages.getMessage("OPTION_INI_FILE")).withRequiredArg().ofType(String.class).describedAs("in file ");

		options = parser.parse(args);
		
		if(hasHelpOption() || hasVersionOption()){			
			return; 
		}
			
		log = (String) options.valueOf("log");
		loglevel = (String) options.valueOf("loglevel");	
		logger = QLoggerFactory.getLogger(Main.class, log, loglevel);
		
		String	version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();		
		if (version == null) version = Constants.NULL_STRING_UPPER_CASE; 
		if (pg == null) pg = Constants.NULL_STRING_UPPER_CASE; 
				
		qexec = logger.logInitialExecutionStats(pg , version, args, QExec.createUUid());	
		 		
		if(!options.has("i"))
			throw new IOException("missing ini file option \'-i \'");
		
		Ini	iniFile =  new Ini( new File(  (String) options.valueOf("i")));
		reference = getIOFromIni(iniFile, ini_secIOs, "ref");
		output = getIOFromIni(iniFile, ini_secIOs, "output");	
		testBam = getIOFromIni(iniFile, ini_secIOs, "testBam");	
		controlBam = getIOFromIni(iniFile, ini_secIOs, "controlBam");
		
		runMode =  iniFile.fetch(ini_secParameter, "runMode");		
		if(runMode.equalsIgnoreCase(RUNMODE_GATK) || runMode.equalsIgnoreCase(RUNMODE_GATKTEST)){
			testVcf = getIOFromIni(iniFile, ini_secIOs, "testVcf") ;		
			controlVcf = getIOFromIni(iniFile, ini_secIOs, "controlVcf");					 			
		}else if(runMode.equalsIgnoreCase(RUNMODE_DEFAULT)){
			String[] inputs = iniFile.get(ini_secIOs).getAll("inputVcf",String[].class);
			for(int i = 0; i < inputs.length; i ++)
				pindelVcfs.add(new File(inputs[i]));
		}
		
		nearbyIndelWindow = Integer.parseInt( iniFile.fetch(ini_secParameter, "window.nearbyIndel"));
//		nearbyHomopolymer = Integer.parseInt( iniFile.fetch(ini_secParameter, "window.homopolymer"));
//		String[] windows = iniFile.fetch(ini_secParameter, "window.homopolymer").split(",");
//		nearbyHomopolymer = Integer.parseInt(windows[0]);
//		nearbyHomopolymerReport = Integer.parseInt(windows[1]);
		max_events = Integer.parseInt( iniFile.fetch(ini_secParameter, "strong.event"));
		softClipWindow = Integer.parseInt( iniFile.fetch(ini_secParameter, "window.softClip"));
		threadNo = Integer.parseInt( iniFile.fetch(ini_secParameter, "threadNo"));
		filterQuery =  iniFile.fetch(ini_secParameter, "filter");
		if(StringUtils.isNullOrEmpty(filterQuery) || filterQuery.equalsIgnoreCase("null"))
			filterQuery = null;
		
		testSampleid = iniFile.fetch(ini_secIDs, "testSample");
		controlSampleid = iniFile.fetch(ini_secIDs, "controlSample");
		donorid = iniFile.fetch(ini_secIDs, "donorId");
		analysisid = iniFile.fetch(ini_secIDs, "analysisId");
		
		gematic_nns = Integer.parseInt( iniFile.fetch(ini_secRule, "gematic.nns"));
		gematic_soi = Float.parseFloat( iniFile.fetch(ini_secRule, "gematic.soi"));
		
//		exdup  = Boolean.parseBoolean( iniFile.fetch(ini_secRule, "exclude.Duplicates"));
				 		
  		detectBadOptions();	  		  		
	}
	
	private File getIOFromIni(Ini ini, String parent, String child) throws Q3IndelException {
		if(ini == null)
			throw new Q3IndelException("INI_FILE_FORMAT_ERROR", (String) options.valueOf("i") );
		
		String f = ini.fetch(parent, child);
		if( StringUtils.isNullOrEmpty(f) || f.toLowerCase().equals("null"))
			return null; 
//		 if( StringUtils.isNullOrEmpty(f))			 
//			 throw new Q3IndelException("MISSING_PARAMETER", child);
		
		 return  new File(f ) ;		 
	}
	
	public  QLogger getLogger(){ return logger; }
	public QExec getQExec(){return qexec; }	
	
	public String getAnalysisId(){return analysisid; }
	public int getMinGematicNovelStart(){return gematic_nns ;}
	public float getMinGematicSupportOfInformative(){return gematic_soi; }
	public int getMaxEventofStrongSupport(){return  max_events; }
	

//	public boolean excludeDuplicates() {
//		return exdup;
//	}	

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
		if (hasHelpOption() || hasVersionOption())
			return; 
		
		checkReference();
			
		if (output != null && output.exists()) 			
			throw new Q3IndelException("OUTPUT_EXISTS", output.getAbsolutePath());
	 		
		if (testBam != null && !testBam.exists())  
			throw new Q3IndelException("FILE_EXISTS_ERROR", testBam.getAbsolutePath());
	 
		if (controlBam != null && !controlBam.exists()) 
			throw new Q3IndelException("FILE_EXISTS_ERROR", controlBam.getAbsolutePath());			
		
		if (RUNMODE_GATK.equalsIgnoreCase(runMode) || RUNMODE_GATKTEST.equalsIgnoreCase(runMode)){ 
			if(testVcf != null && !testVcf.exists())
				throw new Q3IndelException("FILE_EXISTS_ERROR","(test gatk vcf) " + testVcf.getAbsolutePath());
			if(controlVcf != null && !controlVcf.exists())
				throw new Q3IndelException("FILE_EXISTS_ERROR","(control gatk vcf) " + controlVcf.getAbsolutePath());
			 
		}else if (RUNMODE_DEFAULT.equalsIgnoreCase(runMode)){ 
			if(pindelVcfs.size() == 0)
				throw new Q3IndelException("INPUT_OPTION_ERROR","(pindel input vcf) not specified" );
			
			for(int i = 0; i < pindelVcfs.size(); i ++)
			if ( pindelVcfs.get(i) != null && ! pindelVcfs.get(i).exists())  
				throw new Q3IndelException("FILE_EXISTS_ERROR","(control indel vcf) " + pindelVcfs.get(i).getAbsolutePath());				
		}else
			throw new Q3IndelException("UNKNOWN_RUNMODE_ERROR", runMode);			
		 				
	}

	/**
	 * check reference file and index if it is not null
	 * @throws Q3IndelException
	 */
	private void checkReference() throws Q3IndelException {
		if(reference == null)
			return; 
			
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
//	public int getNearbyHomopolymerWindow() {
//		return nearbyHomopolymer;
//	}
//	public int getNearbyHomopolymerReportWindow() {
//		return nearbyHomopolymerReport;
//	}

	public int getSoftClipWindow() {
		return softClipWindow;
	}
	
	public int getThreadNo(){
		return threadNo;
	}

//	public String getCommandLine() {	return commandLine; }		
	public String getDonorId(){ return donorid; }
	public String getControlSample(){ return  this.controlSampleid; }
	public String getTestSample(){ return this.testSampleid; }

 
}

