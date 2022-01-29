/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import java.io.File;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.qcmg.common.log.QLogger;


/*
 * parse command line to options. 
 */
public class StatOptions extends Options {	
	      

    private final OptionParser parser1;
	private String controlInput = null;
	private String testInput = null;
    private String outputFileName = null;
    private String logFileName = null;
      
    private  String logLevel; 
    /**
     * check command line and store arguments and option information
     */
    public StatOptions(final String[] args) throws Exception{ 
    	super(args);			 
 		parser1 = getParser();
 		
        //stop here if ask help or version
 		if(  hasVersionOption())  return;       
        parser1.accepts("input-test", Messages.getMessage("TEST_METRIC_INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser1.accepts( "input-control", Messages.getMessage("CONTROL_METRIC_INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input");
        parser1.accepts("output", Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output"); 
        OptionSet options1 = parser1.parse(args);  
        
        //help can only display after all options are parsed
        if(hasHelpOption()) {
        	displayHelp();
        	return;
	     }
    
        //stop here if ask help or version
 		if(  hasVersionOption()) {
 			System.err.println( getVersion());  			
 			return;
 		}
                
        //check all parameters  	        
       checkCommand(options1, args);
       
     } 
    
    /**
     * 
     * @return true if no Exception happens
     * @throws Exception
     */
    private void checkCommand(OptionSet options, String[] args) throws Exception{     
    	
        if ( ! options.has("input-control") || ! options.has("input-test")  ||!options.has("output")  || !options.has("log")) {
	        System.out.println(Messages.getMessage("WRONG_OPTIONS",QLogger.reconstructCommandLine(Messages.getProgramName(), args))); 
	        System.out.println(Messages.getMessage("USAGE_STAT"));
	        return;
        }   	
    	
        controlInput = (String) options.valueOf("input-control");
		if (!new File(controlInput).exists()) 
			throw new Exception(Messages.getMessage("NO_FILE", controlInput));

        testInput = (String) options.valueOf("input-test");
		if (!new File(testInput).exists()) 
			throw new Exception(Messages.getMessage("NO_FILE", testInput));

        //check outputs
        outputFileName =  (String) options.valueOf("output");
        File output = new File(outputFileName);
        if( output.exists() && !output.canWrite())
        	throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName)); 
		if (!output.exists() && !new File(outputFileName).getParentFile().canWrite()) 
			throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName));       
  
    }    
    
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

	@Override
	void displayHelp() throws Exception {	
		System.err.println( Messages.getMessage("USAGE_STAT") );
		parser1.formatHelpWith(new BuiltinHelpFormatter(150, 2));
		parser1.printHelpOn(System.err);
	}
}
