/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import org.qcmg.common.meta.QExec;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/*
 * parse command line to options. 
 */
abstract class Options {
	
	private static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	private static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
	private static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
	private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
	    
	private final OptionParser parser = new OptionParser();
	private  final OptionSet options;

    private String logFileName = null;
    private String logLevel; 
    private QExec qexec;    
    
    /**
     * check command line and store arguments and option information
     */
    public Options(final String[] args) throws Exception{ 

		parser.allowsUnrecognizedOptions(); 
		parser.accepts(  "version", VERSION_DESCRIPTION);
		parser.accepts(  "help" , HELP_DESCRIPTION);
		parser.accepts("log",  LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
		parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
		           
		options = parser.parse(args);  
		
        //stop here if ask help or version
		if(  hasVersionOption()) {
			System.err.println( getVersion());
			return;
		}		
		
		logFileName = (String) options.valueOf("log");
		logLevel = (String) options.valueOf("loglevel");
		qexec = new QExec( Messages.getProgramName(), Messages.getProgramVersion(), args );
		
    } 
   
    abstract void displayHelp() throws Exception;
		 	
	OptionParser getParser() { return parser; }
		   	   
    public boolean hasHelpOption(){
    	return  options.has("help");
    }
     
    public boolean hasVersionOption(){    	
    	return  options.has("version");
    }
  
    public String getVersion() { 
    	return this.getClass().getPackage().getImplementationTitle()
				+ ", version " + this.getClass().getPackage().getImplementationVersion();	   	 
    }
       
    public String getPGName() {  return Messages.getProgramName(); }
        
	public String getLogFileName() { return logFileName; }

	public String getLogLevel() { return logLevel; }
	
    public QExec getQExec() { return qexec; }

 
}
