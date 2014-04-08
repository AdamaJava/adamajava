/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import org.apache.commons.cli.*;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.messages.Messages;

public class Options {
	private final String HELP_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","VERSION_OPTION_DESCRIPTION");	
	private static final String LOG_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","LOG_OPTION_DESCRIPTION");	
	private static final String LOGLEVEL_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","LOGLEVEL_OPTION_DESCRIPTION");		
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","OUTPUT_OPTION_DESCRIPTION");
	private static final String TEST_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","TEST_OPTION_DESCRIPTION");
	private static final String REF_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","REF_OPTION_DESCRIPTION");	
	private static final String THREAD_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","THREAD_OPTION_DESCRIPTION");	
	private static final String WINDOW_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","WINDOW_SIZE_DESCRIPTION");	
	private static final String TMPDIR_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","TMPDIR_DESCRIPTION");
//	private static final String LINECOUNT_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","LINECOUNT_OPTION_DESCRIPTION");
//	private static final String OUTPUT_COLUMN_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","FIVECOLUMN_OPTION_DESCRIPTION");
	
	private final QOptions options;
	private String tmpdir = null;
	private String refFileName = null;
	private boolean checkCMD = false;

	final static int DEFAULT_THREAD = 2;
	final String USAGE = Messages.getMessage("org.qcmg.cnv.messages","USAGE");
	final String version = org.qcmg.cnv.Main.class.getPackage().getImplementationVersion();	
	
	public Options(String[] args) throws Exception {	
		options = new QOptions(args);
		
    	options.add( OptionBuilder.hasArg(false).withLongOpt("help").withDescription(HELP_DESCRIPTION).create('h'));    
    	options.add( OptionBuilder.hasArg(false).withLongOpt("version").withDescription(VERSION_DESCRIPTION).create('v'));  
     	options.add( OptionBuilder.hasArgs(1).withArgName("tumor BAM").withLongOpt("test").withDescription(TEST_DESCRIPTION).create('t'));
       	options.add( OptionBuilder.hasArgs(1).withArgName("normal BAM").withLongOpt("ref").withDescription( REF_DESCRIPTION).create('r'));  
       	options.add( OptionBuilder.hasArgs(1).withArgName("count file").withLongOpt("output").withDescription( OUTPUT_DESCRIPTION).create('o'));
       	options.add( OptionBuilder.hasArgs(1).withArgName("log file").withLongOpt("log").withDescription( LOG_DESCRIPTION).create());
       	options.add( OptionBuilder.hasArgs(1).withArgName("thread number").withLongOpt("thread").withDescription( THREAD_DESCRIPTION).create());
//       	options.add( OptionBuilder.hasArg(false).withLongOpt("linecount").withDescription(LINECOUNT_DESCRIPTION).create());
       	options.add( OptionBuilder.hasArgs(1).withArgName("temporary file directory").withLongOpt("tmpdir").withDescription( TMPDIR_DESCRIPTION).create());
 //      	options.add( OptionBuilder.hasArg(false).withLongOpt("output_columns_5").withDescription(OUTPUT_COLUMN_DESCRIPTION).create());
       	options.add( OptionBuilder.hasArgs(1).withArgName("window size").withLongOpt("window_size").withDescription( WINDOW_DESCRIPTION).create('w'));
       	
	}	 
	/**
	 * 
	 * @return String of output directory for temporary file
	 * @throws Exception if the directory
	 */
	public String getTmpDir()throws Exception{
    	if(tmpdir == null )  
    		if( options.has("tmpdir")){   			
    			tmpdir = options.valueOf("tmpdir"); 		 
	    		if(! new File( tmpdir ).canWrite() )
	    			throw new Exception("the specified output directory for temporary file are not writable: " + tmpdir);				 
    		}else
    			tmpdir = new File(getOutputName()).getParent();
    	
		return tmpdir;   	
    }
	
	public int getThreadNumber() throws NumberFormatException, ParseException{
		if(options.has("thread")) 
			return Integer.parseInt(options.valueOf("thread"));
		
		return DEFAULT_THREAD;
	}
	
	 public boolean hasHelp() throws Exception{
		 if( options.has("help")){
				options.printHelp(USAGE);	        	
            return true;       
        }
		return false;
	 }
	 
	 public boolean hasVersion()throws Exception{
		 if(options.has("version")){
			System.out.println(VERSION_DESCRIPTION);
			System.err.println(version);    	
			return true;       
	    }
		return false;
	 }
	 
	 public boolean hasLineCounts(QLogger logger) throws Exception{
		 if(options.has("linecount")){
			 BufferedReader br = new BufferedReader(new FileReader(getOutputName() ));
			 int header = 0;
			 int body = 0;
			 String line = br.readLine();
			 while (line != null) {
				 if(line.startsWith("#")) header ++;
				 else body ++;				            
		         line = br.readLine();
		       }
			 br.close();
			 logger.info("##total number of count file lines are "+ body);
			 
			 return true;
		    }
		 
		 return false;
	 }
	 
	 public int getWindowSize() throws Exception{
		 if(options.has("window_size")){
			 return Integer.parseInt(options.valueOf("window_size"));
		 }
		 
		 //<=0 means no fixed window size by default
		 return 0;   
	 }
	 
	 public String getProgramName(){
		 return Options.class.getPackage().getImplementationTitle();	 
	 }
	 
	 public String getTumorInputName() throws Exception{ return options.valueOf("test");}
	 public String getNormalInputName() throws Exception{ return options.valueOf("ref");}
	 public String getOutputName() throws Exception{ return options.valueOf("output");}
	  
	 public QLogger getLogger(String[] args) throws Exception{
			
			// configure logging
			QLogger logger;
			String logLevel =  options.valueOf("loglevel");
			String logFile;
			if(options.has("log")){
				logFile = options.valueOf("log");			 
			}
			else{
				logFile = options.valueOf("output") + ".log";			 
			}		
			
			logger = QLoggerFactory.getLogger( Main.class, logFile,logLevel);
			logger.logInitialExecutionStats(Main.class.toString(), version, args);	
			return logger;
	}

}
