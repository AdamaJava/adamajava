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
	
	public static final String INI_SEC_IOS = "IOs";
	public static final String INI_SEC_IDS = "ids";
	public static final String INI_SEC_PARAM = "parameters";
	public static final String INI_SEC_RULE = "rules";
	
	public static final String RUNMODE_GATK = "gatk";
	public static final String RUNMODE_DEFAULT = "pindel";
	public static final String RUNMODE_GATKTEST = "test";
	
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
	private File output;
	
	private File testVcf;
	private File controlVcf;
	private final List<File> pindelVcfs = new ArrayList<File>(); 
	private String runMode; 
	
	private String testSampleid ;
	private String controlSampleid ;
	private String donorid; 
	private String analysisid; 
	private int gematicNNS;
	private float gematicSOI; 
	private int maxEvents;
				
	private String filterQuery;

	public Options(final String[] args) throws IOException, Q3IndelException  {
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		parser.accepts("i", Messages.getMessage("OPTION_INI_FILE")).withRequiredArg().ofType(String.class).describedAs("in file ");

		options = parser.parse(args);
		
		if (hasHelpOption() || hasVersionOption()) {			
			return; 
		}
			
		log = (String) options.valueOf("log");
		loglevel = (String) options.valueOf("loglevel");	
		logger = QLoggerFactory.getLogger(Main.class, log, loglevel);
		
		String	version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();		
		if (version == null) {
			version = Constants.NULL_STRING_UPPER_CASE; 
		}
		if (pg == null) {
			pg = Constants.NULL_STRING_UPPER_CASE; 
		}
				
		qexec = logger.logInitialExecutionStats(pg , version, args, QExec.createUUid());	
		 		
		if (!options.has("i")) {
			throw new IOException("missing ini file option \'-i \'");
		}		
		
		Ini	iniFile =  new Ini( new File(  (String) options.valueOf("i")));
		output = getIOFromIni(iniFile, INI_SEC_IOS, "output");	
		testBam = getIOFromIni(iniFile, INI_SEC_IOS, "testBam");	
		controlBam = getIOFromIni(iniFile, INI_SEC_IOS, "controlBam");
		
		runMode =  iniFile.fetch(INI_SEC_PARAM, "runMode");		
		if (runMode.equalsIgnoreCase(RUNMODE_GATK) || runMode.equalsIgnoreCase(RUNMODE_GATKTEST)) {
			testVcf = getIOFromIni(iniFile, INI_SEC_IOS, "testVcf") ;		
			controlVcf = getIOFromIni(iniFile, INI_SEC_IOS, "controlVcf");					 			
		} else if (runMode.equalsIgnoreCase(RUNMODE_DEFAULT)) {
			String[] inputs = iniFile.get(INI_SEC_IOS).getAll("inputVcf",String[].class);
            for (String input : inputs) {
                pindelVcfs.add(new File(input));
            }
		}
		
		nearbyIndelWindow = Integer.parseInt( iniFile.fetch(INI_SEC_PARAM, "window.nearbyIndel"));
		maxEvents = Integer.parseInt( iniFile.fetch(INI_SEC_PARAM, "strong.event"));
		softClipWindow = Integer.parseInt( iniFile.fetch(INI_SEC_PARAM, "window.softClip"));
		threadNo = Integer.parseInt( iniFile.fetch(INI_SEC_PARAM, "threadNo"));
		filterQuery =  iniFile.fetch(INI_SEC_PARAM, "filter");
		if (StringUtils.isNullOrEmpty(filterQuery) || filterQuery.equalsIgnoreCase("null")) {
			filterQuery = null;
		}			
		
		testSampleid = iniFile.fetch(INI_SEC_IDS, "testSample");
		controlSampleid = iniFile.fetch(INI_SEC_IDS, "controlSample");
		donorid = iniFile.fetch(INI_SEC_IDS, "donorId");
		analysisid = iniFile.fetch(INI_SEC_IDS, "analysisId");
		
		gematicNNS = Integer.parseInt( iniFile.fetch(INI_SEC_RULE, "gematic.nns"));
		gematicSOI = Float.parseFloat( iniFile.fetch(INI_SEC_RULE, "gematic.soi"));
		
  		detectBadOptions();	  		  		
	}
	
	private File getIOFromIni(Ini ini, String parent, String child) throws Q3IndelException {
		if (ini == null) {
			throw new Q3IndelException("INI_FILE_FORMAT_ERROR", (String) options.valueOf("i") );
		}
		
		String f = ini.fetch(parent, child);
		if ( StringUtils.isNullOrEmpty(f) || f.equalsIgnoreCase("null")) {
			return null; 
		}
					
		return  new File(f ) ;		 
	}
	
	public QLogger getLogger() { 
		return logger; 
	}
	
	public QExec getQExec() {
		return qexec; 
	}	
	
	public String getAnalysisId() {
		return analysisid; 
	}
	
	public int getMinGematicNovelStart() {
		return gematicNNS;
	}
	
	public float getMinGematicSupportOfInformative() {
		return gematicSOI; 
	}
	
	public int getMaxEventofStrongSupport() {
		return  maxEvents; 
	}
	
	public String getFilterQuery() {
		return filterQuery;
	}

	public String getRunMode() {
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
		if (hasHelpOption() || hasVersionOption()) {
			return; 
		}
			
		if (output != null && output.exists()) {	
			throw new Q3IndelException("OUTPUT_EXISTS", output.getAbsolutePath());
		}
		
		if (testBam != null && !testBam.exists()) {  
			throw new Q3IndelException("FILE_EXISTS_ERROR", testBam.getAbsolutePath());
		}
		
		if (controlBam != null && !controlBam.exists()) {
			throw new Q3IndelException("FILE_EXISTS_ERROR", controlBam.getAbsolutePath());			
		}
		
		if (RUNMODE_GATK.equalsIgnoreCase(runMode) || RUNMODE_GATKTEST.equalsIgnoreCase(runMode)) {
			if (testVcf != null && !testVcf.exists()) {
				throw new Q3IndelException("FILE_EXISTS_ERROR","(test gatk vcf) " + testVcf.getAbsolutePath());
			}
			if (controlVcf != null && !controlVcf.exists()) {
				throw new Q3IndelException("FILE_EXISTS_ERROR","(control gatk vcf) " + controlVcf.getAbsolutePath());
			}
		} else if (RUNMODE_DEFAULT.equalsIgnoreCase(runMode)) { 
			if (pindelVcfs.isEmpty()) {
				throw new Q3IndelException("INPUT_OPTION_ERROR","(pindel input vcf) not specified" );
			}
            for (File pindelVcf : pindelVcfs) {
                if (pindelVcf != null && !pindelVcf.exists()) {
                    throw new Q3IndelException("FILE_EXISTS_ERROR", "(control indel vcf) " + pindelVcf.getAbsolutePath());
                }
            }
		} else {
			throw new Q3IndelException("UNKNOWN_RUNMODE_ERROR", runMode);			
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

	public int getNearbyIndelWindow() {
		return nearbyIndelWindow;
	}

	public int getSoftClipWindow() {
		return softClipWindow;
	}
	
	public int getThreadNo() {
		return threadNo;
	}

	public String getDonorId() { 
		return donorid; 
	}
	
	public String getControlSample() { 
		return  this.controlSampleid; 
	}
	
	public String getTestSample() { 
		return this.testSampleid; 
	}
}

