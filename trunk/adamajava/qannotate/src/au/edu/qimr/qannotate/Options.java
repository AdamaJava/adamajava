package au.edu.qimr.qannotate;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
/*
 * parse command line to options. 
 */
public class Options {
	protected enum MODE {dbSNP, germline, snpEff }
	
   protected static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	 
   protected static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
    
   protected static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
   protected static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   	
    
 	public final Options.MODE Mode = null ;   
    private boolean commandCheck = false;
    private String commandLine;

    protected OptionParser parser;
    protected Options modeOptions = null;
	
//	private Options option = null;
//	private Object modeOption = null;
	
	protected String outputFileName = null;
	protected String inputFileName = null;
	protected String databaseFileName = null;
    
	protected String logFileName = null;
	protected  String logLevel;  
 
    /**
     * check command line and store arguments and option information
     */
    
    public Options(){  parser = new OptionParser();  }
    
    public boolean parseArgs(final String[] args) throws Exception{ 

       	parser.allowsUnrecognizedOptions(); 
        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);
        parser.accepts("mode", Messages.getMessage("MODE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("mode");   
        OptionSet options  = parser.parse(args);   
        
        if(options.has("v") || options.has("version")){
            System.out.println( "Current version is " + getVersion());
            return false;
        }
        
        commandLine = Messages.reconstructCommandLine(args) ;
        
        if(options.has("mode")){  
        	
	 System.out.println(     options.valueOf("mode"));
       	
        	String	m = ((String) options.valueOf("mode")).toLowerCase();
			if(m.equalsIgnoreCase(MODE.dbSNP.name())) 			 
				modeOptions = new DbsnpOptions();
			else if( m.equalsIgnoreCase(MODE.germline.name())) 			 
				modeOptions = new  GermlineOptions();
			else if(m.equalsIgnoreCase(MODE.snpEff.name()))
				modeOptions = new SnpEffOptions();
			else{ 
				System.err.println("err on command line : \n\t" + commandLine);
				System.err.println(Messages.getMessage("INVALID_MODE_OPTION", m + " " +  MODE.snpEff.name())); 
				}
        }else if(options.has("h") || options.has("help")) 
        	displayHelp();  
      
        if(modeOptions != null)   
        	return modeOptions.parseArgs( args  );    	
        else
        	return false;
        
	} 
    
    public void displayHelp() throws Exception {
		    System.out.println(Messages.getMessage("USAGE"));  
		    Thread.sleep(1);
		    parser.printHelpOn(System.err);	        
    }
    
    public String getVersion(){
    	return Options.class.getPackage().getImplementationVersion();
    }
    
    public String getPGName(){
        return Messages.getProgramName();
    }
    
    /**
     * check input and output files
     * @return true if input file readable and output file writable
     */
    protected boolean checkIO(  ){   	
        File in = new File(inputFileName);
        File out = new File(outputFileName);
      
        String errMessage = null;
        
        if(!in.exists()) 
            errMessage = Messages.getMessage("NONEXIST_INPUT_FILE", inputFileName);
        else if(!in.isFile())       
            errMessage = Messages.getMessage("FILE_NOT_DIRECTORY", inputFileName); 
         else if(!in.canRead())
        	errMessage = Messages.getMessage("UNREAD_INPUT_FILE",inputFileName);           
        else if(in.getName().equals(out.getName()) && in.getPath().equals(out.getPath()) )
            errMessage = Messages.getMessage("INPUT_SAME_OUTPUT",in.getName(),out.getName());
        else if((out.exists() && !out.canWrite()) || !out.getParentFile().canWrite() )
        	 errMessage = Messages.getMessage("OUTPUT_ERR_DESCRIPTION",in.getName(),out.getName());
         
        if(errMessage == null)
        	return true;
        else
        	System.err.println(errMessage);
        
        return false;
      
    }

	public String getLogFileName() { return logFileName;}	
	public String getLogLevel(){ return logLevel; }	 
	public String getCommandLine() {	return commandLine; }	
	public String getInputFileName(){return inputFileName;}
	public String getOutputFileName(){return outputFileName;}
	public String getDatabaseFileName(){return databaseFileName;}
	
	public Options getOption(){		
		return modeOptions;
	}
	
	public MODE getMode(){
		if(modeOptions != null)
			return modeOptions.Mode;
		else
			return null;
	}

	
}
