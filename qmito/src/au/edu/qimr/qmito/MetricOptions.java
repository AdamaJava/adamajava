/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;



/*
 * parse command line to options. 
 */
public class MetricOptions {
    private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
   private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
   private static final String QUERY_DESCRIPTION = Messages.getMessage("QUERY_OPTION_DESCRIPTION");
   private static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
   private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   
    private boolean commandCheck = false;
    private String query = "and (flag_NotprimaryAlignment == false, flag_ReadUnmapped == false)";
    private String defaultRefName = "chrMT";
    private String referenceFile;
    private SAMSequenceRecord referenceRecord;
    private int lowreadcount = 10;
    private int nonrefthreshold = 20;
    private int referenceSize = -1;

    private final OptionParser parser = new OptionParser();
    private  OptionSet options;
	private String[] inputBamNames = null;
    private String outputFileName = null;
    private String logFileName = null;
    private SAMFileHeader header = null;   
    private SAMFileHeader.SortOrder sort = null;
     
	private String tmpdir = null;    
    private  String logLevel; 
    private int maxNoOfRecords;
    private String uuid;
    private QExec qexec;
    /**
     * check command line and store arguments and option information
     */
    public MetricOptions(final String[] args) throws Exception{ 

    	parser.allowsUnrecognizedOptions(); 
        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("q", "query"), QUERY_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("\"query\"");
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output"); 
        parser.acceptsAll(asList("r", "referenceFile"),Messages.getMessage("REFERENCE_DESCRIPTION")).withRequiredArg().ofType(String.class);
        parser.accepts("log",  LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("lowreadcount",Messages.getMessage("LOW_READ_COUNT_DESCRIPTION")).withRequiredArg().ofType(Integer.class);
        parser.accepts("nonrefthreshold",Messages.getMessage("NONREFERENCE_THRESHOLD_DESCRIPTION")).withRequiredArg().ofType(Integer.class);
 
        options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
            System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));
            Thread.sleep(1);
            parser.printHelpOn(System.err);
            System.out.println(Messages.getMessage("USAGE_METRIC"));
            return;
        }        
        
        if ( !options.has("i") || ! options.has("o")  || !options.has("r") || !options.has("log")) {
	        System.out.println(Messages.getMessage("WRONG_OPTIONS",QLogger.reconstructCommandLine(Messages.getProgramName(), args))); 
	        parser.printHelpOn(System.err);
            System.out.println(Messages.getMessage("USAGE_METRIC"));
            return;
        }

        setLogFileName((String) options.valueOf("log"));        	
        logLevel = (String) options.valueOf("loglevel");
        
        //check all parameters  	        
        commandCheck = checkCommand();       
	    qexec = new QExec(Messages.getProgramName(), Messages.getProgramVersion(), args);
    } 
    
    /**
     * 
     * @return true if no Exception happens
     * @throws Exception
     */
    private boolean checkCommand() throws Exception{     
        //check query string format
        if(options.has("q") || options.has("query")){
	        query = options.valueOf("q").toString();                  
	        if(! checkQuery(query)) return false;	        	 
        }
        
 
        List inputs = options.valuesOf("i");
        inputBamNames = new String[inputs.size()];
        inputs.toArray(inputBamNames);   
        checkInputBams();
        
        //check Reference
        referenceFile = (String) options.valueOf("r");
        if( ! (new File(referenceFile).exists()))
        	throw new Exception("reference is not exist: " + referenceFile);      
        
         
        
        String ref = defaultRefName; 
        if(options.has("refname"))
        	ref = (String) options.valueOf("refname");
      	
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(inputBamNames[0]));  
		SAMFileHeader header = reader.getFileHeader().clone();
		reader.close();
		
//		int index = header.getSequenceIndex(ref);
		referenceRecord = header.getSequence(header.getSequenceIndex(ref));
		if(referenceRecord == null)
			throw new Exception("invalide reference sequence name: " + ref);

         //check outputs
        outputFileName =  (String) options.valueOf("o");
        File output = new File(outputFileName);
        if( output.exists() && !output.canWrite())
        	throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName)); 
		if (!output.exists() && !new File(outputFileName).getParentFile().canWrite()) 
			throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName));       
    	
    	return true;
    }
    

    private void checkInputBams() throws Exception{
		for (String bam : inputBamNames) {
			File bamFile = new File(bam);
			if (!bamFile.exists()) 
				throw new Exception(Messages.getMessage("NO_FILE", bam));
								
			String bamCanPath = bamFile.getCanonicalPath();
			File bamLock = new File(bamCanPath + ".lck");
			File bamIndex = new File(bamCanPath + ".bai");					
			
			if (bamLock.exists() )					
				throw new Exception(Messages.getMessage("BAM_LOCK", bam));
				
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");			
			File indexLock = new File(bam + ".bai.lck");
			//does bam have index
			if (!reader.hasIndex()) {
				reader.close();
				throw new Exception(Messages.getMessage("NO_INDEX", bam));
			} 
			
			if (bamIndex.lastModified() < bamFile.lastModified()) {
				reader.close();
				throw new Exception(Messages.getMessage("INDEX_OLD", bam));
			}
								
			if (indexLock.exists()) {
				reader.close();
				throw new Exception(Messages.getMessage("INDEX_LOCK", bam));
			}
			
			reader.close();								
		}
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

 
    public void displayHelp()  throws Exception{
        parser.printHelpOn(System.err);
    }

    public boolean hasCommandChecked(){
        return commandCheck;
    }
    
//    public String getUuid(){return uuid;}
    
    public QExec getQExec(){return qexec;}
    
    public String getOutputFileName(){
        return outputFileName;
    }
    public String[] getInputFileNames(){
        return inputBamNames;
    }
    public String getReferenceFile(){
    	return referenceFile;
    }
    public String getQuery(){
        return query;
    }
    public SAMSequenceRecord  getReferenceRecord(){    	   	
    	return referenceRecord;
    }

    public int getLowReadCount(){
    	if(options.has("lowreadcount"))
    		lowreadcount = (Integer) options.valueOf("lowreadcount");
    	
    	return lowreadcount;
    		 
    }
    
    public int getNonRefThreshold(){
    	if(options.has("nonrefthreshold"))
    		nonrefthreshold = (Integer) options.valueOf("nonrefthreshold");

    	return nonrefthreshold;    		 
    }
  

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	public String getLogFileName() {
		return logFileName;
	}

	public String getLogLevel(){
		return logLevel;
	}
}
