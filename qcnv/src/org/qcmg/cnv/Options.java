/**
 * Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;



import java.io.File;
import java.util.HashSet;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;

import org.apache.commons.cli.*;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.messages.Messages;
import org.qcmg.picard.SAMFileReaderFactory;

public class Options {
	private final String HELP_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","HELP_OPTION_DESCRIPTION");
	private static final String VERSION_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","VERSION_OPTION_DESCRIPTION");	
	private static final String LOG_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","LOG_OPTION_DESCRIPTION");	
	private static final String LOGLEVEL_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","LOGLEVEL_OPTION_DESCRIPTION");		
	private static final String OUTPUT_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","OUTPUT_OPTION_DESCRIPTION");
	private static final String INPUT_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","INPUT_OPTION_DESCRIPTION");	
	private static final String ID_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","SAMPLEID_OPTION_DESCRIPTION");	
	private static final String THREAD_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","THREAD_OPTION_DESCRIPTION");	
	private static final String WINDOW_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","WINDOW_SIZE_DESCRIPTION");
	private static final String QUERY_DESCRIPTION = Messages.getMessage("org.qcmg.cnv.messages","QUERY_OPTION_DESCRIPTION");
	 
	private final QOptions options;
//	private String tmpdir = null;
	private String refFileName = null;
	private boolean checkCMD = false;

	final static int DEFAULT_THREAD = 2;
	final static int DEFAULT_WindowSize = 10000;
	final String USAGE = Messages.getMessage("org.qcmg.cnv.messages","USAGE");
	final String version = org.qcmg.cnv.Main.class.getPackage().getImplementationVersion();	
	
	public Options(String[] args) throws Exception {	
		options = new QOptions(args);
		
    	options.add( OptionBuilder.hasArg(false).withLongOpt("help").withDescription(HELP_DESCRIPTION).create('h'));    
    	options.add( OptionBuilder.hasArg(false).withLongOpt("version").withDescription(VERSION_DESCRIPTION).create('v'));  
     	options.add( OptionBuilder.hasArgs(1).withArgName("input BAM").withLongOpt("input").withDescription(INPUT_DESCRIPTION).create('i'));     	
       	options.add( OptionBuilder.hasArgs(1).withArgName("sample id").withLongOpt("id").withDescription( ID_DESCRIPTION).create());  
       	options.add( OptionBuilder.hasArgs(1).withArgName("count file").withLongOpt("output").withDescription( OUTPUT_DESCRIPTION).create('o'));
       	options.add( OptionBuilder.hasArgs(1).withArgName("log file").withLongOpt("log").withDescription( LOG_DESCRIPTION).create());
       	options.add( OptionBuilder.hasArgs(1).withArgName("thread number").withLongOpt("thread").withDescription( THREAD_DESCRIPTION).create());
       	options.add( OptionBuilder.hasArgs(1).withArgName("window size").withLongOpt("window_size").withDescription( WINDOW_DESCRIPTION).create('w'));
       	options.add( OptionBuilder.hasArgs(1).withArgName("query string").withLongOpt("query").withDescription(QUERY_DESCRIPTION).create('q'));
       	 
       	if(options.has("help") || options.has("version"))
       		return;
       	
       	//check inputs and sample ids
       	if(options.valuesOf("input") == null || options.valuesOf("id") == null)
       		throw new Exception("missing input or id parameters in command line");
       	
       	String[] inputList = getInputNames();
      // 			options.valuesOf("input");
	    String[] ids = getSampleIds();
//	    		options.valuesOf("id");
	    if(ids.length != inputList.length)
	    	throw new Exception("the number of inputs and ids is not matched, please specify a uniqe sample id for each input file!");

       	
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
	 
	 
	 public int getWindowSize() throws Exception{
		 if(options.has("window_size")){
			 return Integer.parseInt(options.valueOf("window_size"));
		 }
		 		 
		 return DEFAULT_WindowSize;   
	 }
	 
	 public String getProgramName(){
		 return Options.class.getPackage().getImplementationTitle();	 
	 }
	 
	 public String[] getInputNames() throws Exception{ 
		String[] inputList = options.valuesOf("input");	
		HashSet<File> uniqe = new HashSet<File>();
	    for (int i = 0; i < inputList.length; i ++){
	    	if(! new File(inputList[i]).canRead())
	    		throw new Exception("input file is unreadable: " + inputList[i]);
	    	//make sure each single input file are physically unique
	    	uniqe.add(new File(inputList[i]).getCanonicalFile());
	    }
		
	    if(uniqe.size() != inputList.length)
	    	throw new Exception("same input file typed multi times in commandline!");
	    
		 return inputList;		 
	 }
	 public String[] getSampleIds() throws Exception{ 
		 String[] ids = options.valuesOf("id");
		 HashSet<String> uniqe = new HashSet<String>();
		 for(String id : ids)
			 uniqe.add(id.toLowerCase());
		 
	    if(ids.length != uniqe.size())
	    	throw new Exception("same sample id typed multi times in command line ");

		 return options.valuesOf("id");		 
	 }
	 
	 
 	 public String getOutputName() throws Exception{ return options.valueOf("output");}
 	 public String getQuery() throws ParseException{return options.valueOf("query"); }
	  
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
