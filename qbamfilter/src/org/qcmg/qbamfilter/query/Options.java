/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.query;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;

/*
 * parse command line to options. 
 */
public class Options {
    private static final String VERSION_DESCRIPTION =
        Messages.getMessage("VERSION_OPTION_DESCRIPTION");
   private static final String HELP_DESCRIPTION =
        Messages.getMessage("HELP_OPTION_DESCRIPTION");  
   private static final String QUERY_DESCRIPTION =
        Messages.getMessage("QUERY_OPTION_DESCRIPTION");
   private static final String LOG_DESCRIPTION =
       Messages.getMessage("LOG_OPTION_DESCRIPTION");
   private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
	.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   
    private boolean commandCheck = false;
    private String query = "";

    private final OptionParser parser = new OptionParser();
    private  OptionSet options;

    private String outputFileName = null;
    private String inputFileName = null;
    private String filtedFileName = null;
    private String logFileName = null;
    private SAMFileHeader header = null;   
    private SAMFileHeader.SortOrder sort = null;
    
 
	private String tmpdir = null;
    
    private  String logLevel;
 
    
    private int noOfThreads;
    private int maxNoOfRecords;
    
    //default value
    private final int DefaultUnit = 1000;
    private final int DefaultThreads = 1;
    private final int DefaultRecords = 1000 * DefaultUnit;  
    /**
     * check command line and store arguments and option information
     */
    public Options(final String[] args) throws Exception{ 
    	
    	if (null == args || args.length == 0) {
               parser.printHelpOn(System.err);
               System.out.println();
               System.out.println(Messages.USAGE);
               return;
    	}

        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);
        parser.acceptsAll( asList("q", "query"), QUERY_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("\"query\"");
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output"); 
        parser.acceptsAll( asList("f", "filterOut"), Messages.getMessage("FILTEROUT_DESCRITPION")).withRequiredArg().ofType(String.class).describedAs("filterOut");
        parser.acceptsAll( asList("m", "maxRecordNumber"), Messages.getMessage("MAXRECORD_DESCRITPION")).withRequiredArg().ofType(String.class).describedAs("maxRecordNumber");       
        parser.acceptsAll( asList("t", "threadNumber"), Messages.getMessage("THREADNUMBER_DESCRITPION")).withRequiredArg().ofType(String.class).describedAs("threadNumber");
         
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("sort", Messages.getMessage("SORT_DESCRITPION") ).withRequiredArg().ofType(String.class);
        parser.accepts("tmpdir", Messages.getMessage("TMPDIR_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class); 
        parser.accepts("validation", Messages.getMessage("VALIDATION_STRINGENCY_DESCRIPTION")).withRequiredArg().ofType(String.class); 
        options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
            System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));
            Thread.sleep(1);
            parser.printHelpOn(System.err);
            System.out.println(Messages.USAGE);
            return;
        }
        if(options.has("v") || options.has("version")){
            System.out.println( "Current version is " + getVersion());
            return;
        }
        
        //check IO
        if ( ! options.has("i") || ! options.has("o") || ! options.has("q")) {
             System.out.println(Messages.USAGE);
             return;
        }
        inputFileName = options.valueOf("i").toString();      	 
        outputFileName = options.valueOf("o").toString();      
        if( !checkFiles(inputFileName, outputFileName) ) return;
        
        //check query string format
        query = options.valueOf("q").toString();                  
        if(! checkQuery(query)) return;

        if(options.has("f") || options.has("filterOut")){
             filtedFileName = options.valueOf("f").toString();
             if( (!checkFiles(inputFileName, filtedFileName)) ){
                return;
            }
        }
        
        if(options.has("t") ||  options.has("threadNumber"))
        	noOfThreads = Integer.parseInt(options.valueOf("t").toString());        	
        else
        	noOfThreads = DefaultThreads;
   
        if(options.has("m") || options.has("maxRecord"))
        	maxNoOfRecords = Integer.parseInt(options.valueOf("m").toString()) * DefaultUnit;        	
        else
        	maxNoOfRecords = DefaultRecords;
        
        
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));
            Thread.sleep(1);
            parser.printHelpOn(System.err);
            System.out.println(Messages.USAGE);
            return;
        }
        else{       	
        	setLogFileName((String) options.valueOf("log"));        	
        }
          
        if( options.has("sort")){
        	String sortOrder = (String) options.valueOf("sort"); 
        	if(sortOrder.equalsIgnoreCase("coordinate"))
        		sort = SAMFileHeader.SortOrder.coordinate;
        	else if(sortOrder.equalsIgnoreCase("queryname"))
        		sort = SAMFileHeader.SortOrder.queryname;
        	else if(! sortOrder.equalsIgnoreCase("unsorted"))
        		throw new Exception( sortOrder +  " isn't valid SAMFileHeader sort order!");    	
        } 
        
        if(options.has("tmpdir"))
            tmpdir = options.valueOf("tmpdir").toString();
        
        logLevel = (String) options.valueOf("loglevel");
        
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(inputFileName));
        try {
        	header = reader.getFileHeader();
        	HeaderUtils.addProgramRecord(header,  getPGName(),  getVersion(), Arrays.toString(args) );
        } finally {
        	reader.close();
        }
        
       commandCheck = true;
    } 
    
    private boolean checkQuery(String query){
    	try{
    		new QueryExecutor(query);
    	}catch(Exception e){
    		String message = Messages.getMessage("QUERY_ERROR", query);
            System.err.println( message);
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean checkFiles(String input, String output ){
        File in = new File(input);
        File out = new File(output);

        if(!in.exists()){
            String message = Messages.getMessage("NONEXIST_INPUT_FILE", in.getName());
            System.err.println( message);
            return false;
        }

        if(!in.isFile()){       
            String message = Messages.getMessage("FILE_NOT_DIRECTORY", in.getName());
            System.err.println( message);
            return false;
        }
        
        if(!in.canRead()){
            System.err.println(Messages.getMessage("UNREAD_INPUT_FILE"));
            return false;
        }

        if(in.getName().equals(out.getName()) && in.getPath().equals(out.getPath())  ){
            String message = Messages.getMessage("INPUT_SAME_OUTPUT",in.getName(),out.getName());
            System.err.println( message);
            return false;          
        }

        return true;
    }


    public void displayHelp()
        throws Exception
    {
        parser.printHelpOn(System.err);
    }

    public boolean hasCommandChecked()
    {
        return commandCheck;
    }
    public String getOutputFileName()
    {
        return outputFileName;
    }
    public String getFiltedFileName(){
        return filtedFileName;
    }
    public String getInputFileName()
    {
        return inputFileName;
    }
    public String getQuery(){
        return query;
    }
    public String getVersion(){
    	return Options.class.getPackage().getImplementationVersion();
      //  return Messages.getMessage("VERSION_MESSAGE");
    }
    public String getPGName(){
        //return Messages.getMessage("PROGRAM_NAME");
        return Messages.getProgramName();
    }
	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	public String getLogFileName() {
		return logFileName;
	}
	public SAMFileHeader getHeader(){ 
		return header;
	}
  
	public int getThreadNumber(){
		return noOfThreads;
	}
	public int getMaxRecordNumber(){
		return maxNoOfRecords;
	}
	public String getLogLevel(){
		return logLevel;
	}
	public String getTmpDir()throws Exception{
    	if(tmpdir != null )  
    		if(! new File( tmpdir ).canWrite() )
	    			throw new Exception("the specified output directory for temporary file are not writable: " + tmpdir);

		return tmpdir;   	
    }	
	
	public SAMFileHeader.SortOrder getSortOrder(){
		if(sort == null)
			return header.getSortOrder();
		
		return sort;
	}
	/**
	 * 
	 * @return the queue size check point. The thread will check queue size when deal with certain number of records, eg, 1M, 2M ...
	 * If the checkPoint is too big, it means check the queue size very rare, cause queue size much bigger than user specified maxRecord. Then thread work inefficient since out of memory.
	 */
	public int getCheckPoint(){
		if(maxNoOfRecords > DefaultRecords )
			return DefaultRecords;
		else
			return maxNoOfRecords;
		
	}
	
	
	/**
	 * 
	 * @return ValidationStringency if specified on command line; otherwise return LENIENT as default
	 * @throws Exception if specified invalid ValidationStringency 
	 */
	public ValidationStringency getValidation() throws Exception{	
		if(options.has("validation")){
			if(options.valueOf("validation").toString().equalsIgnoreCase("LENIENT"))
				return ValidationStringency.LENIENT;
			else if(options.valueOf("validation").toString().equalsIgnoreCase("SILENT"))
				return  ValidationStringency.SILENT;
			if(options.valueOf("validation").toString().equalsIgnoreCase("STRICT"))
				return  ValidationStringency.STRICT;
			else
				throw new Exception("invalid validation option: " + options.valueOf("validation").toString()  );
		}

		return ValidationStringency.LENIENT;
	}
}
