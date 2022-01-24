/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/*
 * parse command line to options. 
 */
public class Options {
   public static final String MetricMode = "metric";
   public static final String StatMode = "stat";
	
   private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
   private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
   
	private String mode = null;
	private MetricOptions  metricOpt = null; 
	private StatOptions statOpt = null;
    
    private final OptionParser parser = new OptionParser();
    private  OptionSet options;
 
    /**
     * check command line and store arguments and option information
     */
    public Options(final String[] args) throws Exception{ 

    	parser.allowsUnrecognizedOptions(); 
        parser.accepts(  "help", HELP_DESCRIPTION);
        parser.accepts(  "version", VERSION_DESCRIPTION);
        parser.accepts("mode", Messages.getMessage("OPTION_MODE")).withRequiredArg().ofType(String.class);	
        options = parser.parse(args);   
             
//        if( !options.has("v") && !options.has("version") && !options.has("m") && !options.has("v") & !options.has("version")){
//	        System.out.println(Messages.getMessage("WRONG_OPTIONS",QLogger.reconstructCommandLine(Messages.getProgramName(), args))); 
//	        parser.printHelpOn(System.err);
//	        System.out.println(Messages.USAGE);   	
//        }
        
        if(options.has("v") || options.has("version")){ 
        	System.out.println( "Current version is " + getVersion());
        	return;
        }
        
        if(options.has("mode")) {
			mode = ((String) options.valueOf("mode")).toLowerCase();
			if(mode.equals(MetricMode)) 			 
				metricOpt = new MetricOptions( args);			
			else if ( mode.equals( StatMode))
				statOpt =new StatOptions(args);
			else
				throw new Exception("invalide mode: "+(String) options.valueOf("mode"));  		
        } else {
        	System.out.println( "Missing mode option!");
        	System.out.println(Messages.USAGE); 
        	
        }
        
        
        
        
//        if( (options.has("h") || options.has("help") ) && mode == null){
//	        System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));	   
//	        parser.printHelpOn(System.err);
//	        System.out.println(Messages.USAGE);   	
//        }
        	
     
    } 
    
    public boolean hasHelpOption(){
    	if(options.has("h") || options.has("help"))
    		return true;
    	
    	return false;
    }
  
    public MetricOptions getMetricOption(){ return metricOpt;}
    
    public StatOptions getStatOption(){ return statOpt;}
    
    public boolean hasVersionOption(){    	
    	if(options.has("v") || options.has("version")) 
    		return true;    	
    	return false;
    }
  
    public String getVersion(){
    	return Options.class.getPackage().getImplementationVersion();
      //  return Messages.getMessage("VERSION_MESSAGE");
    }
    public String getPGName(){
        //return Messages.getMessage("PROGRAM_NAME");
        return Messages.getProgramName();
    }
    
	public String getMode() {
		return mode;
	}
    
    public String getLogFileName() {
	    if(mode.equals(MetricMode)) 			 
			return metricOpt.getLogFileName();	
		else if ( mode.equals( StatMode))
			return statOpt.getLogFileName();
		else
			return null;
    
    }
    
    public String getLogLevel() {
	    if(mode.equals(MetricMode)) 			 
			return metricOpt.getLogLevel();	
		else if ( mode.equals( StatMode))
			return statOpt.getLogLevel();
		else
			return null;   
    }
 
}
