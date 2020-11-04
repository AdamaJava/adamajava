/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.qcnv;


import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.qmule.Messages;
import org.qcmg.common.log.*;	

public class Options {
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");	
	private static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");	
	private static final String LOGLEVEL_DESCRIPTION = Messages.getMessage("LOGLEVEL_OPTION_DESCRIPTION");	
	
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("OUTPUT_OPTION_DESCRIPTION");
	private static final String TEST_DESCRIPTION = Messages.getMessage("TEST_OPTION_DESCRIPTION");
	private static final String REF_DESCRIPTION = Messages.getMessage("REF_OPTION_DESCRIPTION");	
	private static final String THREAD_DESCRIPTION = Messages.getMessage("THREAD_OPTION_DESCRIPTION");	
	private static final String WINDOW_DESCRIPTION = Messages.getMessage("WINDOW_SIZE_DESCRIPTION");	
	private static final String TMPDIR_DESCRIPTION = Messages.getMessage("TMPDIR_DESCRIPTION");
	private final OptionParser parser = new OptionParser();  
	private final OptionSet options;	
	
	final static int DEFAULT_THREAD = 2;
	final String commandLine;	
	final String USAGE = Messages.getMessage("USAGE_QCNV");
	final String version = org.qcmg.qmule.Main.class.getPackage().getImplementationVersion();	
	
	public Options( final String[] args) throws Exception {		
		parser.accepts("output", OUTPUT_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("outputfile");
		parser.accepts("ref", REF_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("Normal BAM");
		parser.accepts("test", TEST_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("Tumor BAM");
		parser.accepts("thread", THREAD_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("thread number");
		parser.accepts("window", WINDOW_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("window size");
		parser.accepts("tmpdir", TMPDIR_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("window size");
		
		
		parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("logfile");
		parser.accepts("loglevel", LOGLEVEL_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("loglevel");
		parser.accepts("version", VERSION_DESCRIPTION);
		parser.accepts("help", HELP_DESCRIPTION);

		options = parser.parse(args);	
		commandLine = Messages.reconstructCommandLine(args);
	}
	
	//IO parameters
	String getIO(String io) throws Exception{		

		int size = options.valuesOf(io).size();
		if( size > 1){
			throw new Exception("multiple "+ io + " files specified" );
		}
		else if( size < 1 ){
			throw new Exception(" missing or invalid IO option specified: " + io );
		}
		
		return options.valueOf(io).toString();		 
	}
	
	File getTmpDir() throws Exception{
		if(options.has("tmpdir"))
			return new File (options.valueOf("tmpdir").toString());
			
		 
		
		return null;

	}
	
	int getThreadNumber(){
		
		if(options.has("thread")){
			return  Integer.parseInt((String) options.valueOf("thread"));
		}
			
		return DEFAULT_THREAD;
	}
	
	int getWindowSize(){
		
		if(options.has("window")){
			return  Integer.parseInt((String) options.valueOf("window"));
		}
			
		return  0;
	}
	
	QLogger getLogger(String[] args) throws Exception{
				
		// configure logging
		QLogger logger;
		String logLevel = (String) options.valueOf("loglevel");
		String logFile;
		if(options.has("log")){
			logFile = options.valueOf("log").toString();			 
		}
		else{
			logFile = options.valueOf("output") + ".log";			 
		}		
		
		logger = QLoggerFactory.getLogger( Main.class, logFile,logLevel);
		logger.logInitialExecutionStats(Main.class.toString(), version, args);	
		return logger;
	}
	
	 boolean hasHelp() throws Exception{
		 if(options.has("h") || options.has("help")){    
        	System.out.println(USAGE);			 
        	System.out.println(HELP_DESCRIPTION);
            parser.printHelpOn(System.err);
            return true;       
        }
		return false;
	 }
	 
	 boolean hasVersion()throws Exception{
		 if(options.has("v") || options.has("version")){
			System.out.println(VERSION_DESCRIPTION);
			System.err.println(version);    	
			return true;       
	    }
		return false;
	 }
	 
	 boolean commandCheck() throws Exception{
			//quit system after provide help or version info
			if( hasHelp() || hasVersion() ){ 
				 System.exit(0); 
			} 		 
	
		
			if (options.nonOptionArguments().size() > 0) {	
				List<String> nonoptions = (List<String>) options.nonOptionArguments();

				for(String str : nonoptions){
					System.err.println("INVALID OPTION: " + str);
				} 				
				return false;
			}

			if(getIO("ref") == null || getIO("test") == null){
				System.err.println("Missing ref or test option");		 
				return false;
			}
			if( getIO("ref").equals(getIO("output"))){
				System.err.println(Messages.getMessage("SAME_FILES", "ref", "output"));		
						return false;
			}	
			if(options.has("thread")){
				int thread = Integer.parseInt((String) options.valueOf("thread"));
				if(thread < 1){
					System.err.println("THREAD NUMBER MUST GREATER THAN ONE: " + options.valueOf("thread") );
				}
			}
			
	 	return true;	
		}
}
