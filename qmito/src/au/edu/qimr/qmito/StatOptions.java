/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;



/*
 * parse command line to options. 
 */
public class StatOptions {
   private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
   private static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
   private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   
    private boolean commandCheck = false;

    private final OptionParser parser = new OptionParser();
    private  OptionSet options;
	private String controlInput = null;
	private String testInput = null;
    private String outputFileName = null;
    private String logFileName = null;
      
    private  String logLevel; 
    private String uuid;
    private QExec qexec;
    /**
     * check command line and store arguments and option information
     */
    public StatOptions(final String[] args) throws Exception{ 

    	parser.allowsUnrecognizedOptions(); 
        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("t", "test"), Messages.getMessage("TEST_METRIC_INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser.acceptsAll( asList("c", "contronal"), Messages.getMessage("CONTROL_METRIC_INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output"); 
        parser.accepts("log",  LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class); 
        options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
            System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));
            System.out.println(Messages.getMessage("USAGE_STAT"));         
            parser.printHelpOn(System.err);
            return;
        }     
        
        if ( ! options.has("c") || ! options.has("t")  ||!options.has("o")  || !options.has("log")) {
	        System.out.println(Messages.getMessage("WRONG_OPTIONS",QLogger.reconstructCommandLine(Messages.getProgramName(), args))); 
	        parser.printHelpOn(System.err);
            System.out.println(Messages.getMessage("USAGE_STAT"));
            return;
        } 
        
        this.logFileName =  (String) options.valueOf("log");        
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

        controlInput = (String) options.valueOf("c");
		if (!new File(controlInput).exists()) 
			throw new Exception(Messages.getMessage("NO_FILE", controlInput));

        testInput = (String) options.valueOf("t");
		if (!new File(testInput).exists()) 
			throw new Exception(Messages.getMessage("NO_FILE", testInput));

        //check outputs
        outputFileName =  (String) options.valueOf("o");
        File output = new File(outputFileName);
        if( output.exists() && !output.canWrite())
        	throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName)); 
		if (!output.exists() && !new File(outputFileName).getParentFile().canWrite()) 
			throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName));       
    	
    	return true;
    }
    
    public boolean hasCommandChecked(){
        return commandCheck;
    }

    public QExec getQExec(){return qexec;}
    
    
    public String getTestMetricFileName(){
    	return testInput;
    }
    
    public String getControlMetricFileName(){
    	return controlInput;
    } 
    
    public String getOutputFileName(){
        return outputFileName;
    }
  
	public String getLogFileName() {
		return logFileName;
	}

	public String getLogLevel(){
		return logLevel;
	}
}
