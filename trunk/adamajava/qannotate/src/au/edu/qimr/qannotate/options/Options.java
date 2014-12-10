package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;

import java.io.File;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.common.log.QLogger;

import au.edu.qimr.qannotate.Messages;
/*
 * parse command line to options. 
 */
public class Options {
	public enum MODE {dbSNP, germline, snpEff,confidence,customerConfidence,vcf2maf }
	
   protected static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	 
   protected static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
    
   protected static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
   protected static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   	
    
    private final boolean commandCheck = false;
    protected String commandLine;

    protected OptionParser parser;
    protected Options modeOptions = null;
    protected QLogger logger = null;
	
//	private Options option = null;
//	private Object modeOption = null;
    
//    public final Options.MODE Mode = null;
 	protected final Options.MODE Mode;   	
	protected String outputFileName = null;
	protected String inputFileName = null;
	protected String databaseFileName = null;
    
	protected String logFileName = null;
	protected  String logLevel;  
 
    /**
     * check command line and store arguments and option information
     */
    
    public Options(){  parser = new OptionParser(); this.Mode = null; }
    public Options(Options.MODE m){ parser = new OptionParser(); this.Mode = m;}
    
    public boolean parseArgs(final String[] args) throws Exception{ 

       	parser.allowsUnrecognizedOptions(); 
        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);
        parser.accepts("mode", Messages.getMessage("MODE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("mode");   
        final OptionSet options  = parser.parse(args);   
        
        if(options.has("v") || options.has("version")){
            System.out.println( "Current version is " + getVersion());
            return false;
        }
        
        commandLine = Messages.reconstructCommandLine(args) ;
        
        if(options.has("mode")){  
         	final String	m = ((String) options.valueOf("mode")).toLowerCase();
			if(m.equalsIgnoreCase(MODE.dbSNP.name())) 			 
				modeOptions = new DbsnpOptions();
			else if( m.equalsIgnoreCase(MODE.germline.name())) 			 
				modeOptions = new  GermlineOptions();
			else if(m.equalsIgnoreCase(MODE.snpEff.name()))
				modeOptions = new SnpEffOptions();
			else if(m.equalsIgnoreCase(MODE.confidence.name()))
				modeOptions = new ConfidenceOptions();
			else if(m.equalsIgnoreCase(MODE.customerConfidence.name()))
				modeOptions = new CustomerConfidenceOptions();
			else if(m.equalsIgnoreCase(MODE.vcf2maf.name()))
				modeOptions = new Vcf2mafOptions();
			
			else{ 
				System.err.println("err on command line : \n\t" + commandLine);
				System.err.println(Messages.getMessage("INVALID_MODE_OPTION", m + " " +  MODE.snpEff.name())); 
				}
        }else if(options.has("h") || options.has("help")) 
        	displayHelp();  
        
 //       System.out.println("cmd is " + commandLine);
        
      
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
    protected boolean checkIO( ){   	
    	 final File in = new File(inputFileName );
        final File out = new File(outputFileName  );
      
        String errMessage = null; 	
        if((out.exists() && !out.canWrite()) || !out.getParentFile().canWrite() )
        	 errMessage = Messages.getMessage("OUTPUT_ERR_DESCRIPTION",out.getName());
          else if(in.getName().equals(out.getName()) && in.getPath().equals(out.getPath()) )
            errMessage = Messages.getMessage("INPUT_SAME_OUTPUT",in.getName(),out.getName());
        
        if(errMessage == null)
        	return true;
        else
        	System.err.println(errMessage);
        
        return false;
      
    }
    protected boolean checkUnique(String[] ios ){   
    	
    	final File[] fios = new File[ios.length];
 //   	Path[] pios = new Path[ios.length];
    	
    	for (int i = 0; i < ios.length; i ++)
    		fios[i] = new File(ios[i]); 
    	   	
    	for(int  i = ios.length -1; i > 0; i --)
    		for (int j = i-1; j >= 0; j -- )
				try {
					//if( Files.isSameFile(pios[i], pios[j]))
					if(fios[i].getCanonicalFile().equals(fios[j].getCanonicalFile()))
						throw new Exception( "below command line values are point to same file: \n\t" + fios[i] + "\n\t" + fios[j]   ) ;
				} catch (final Exception e) {
					//e.printStackTrace();
					System.err.println(e.getMessage());
					return false;
				}

    	return true;    	
    }	 
    
    protected boolean checkOutputs( String[]  outputs){   
    	for(int i = 0; i < outputs.length; i ++){
	        final File out = new File(outputs[i] );
	        if((out.exists() && !out.canWrite()) || !out.getParentFile().canWrite() ){
	        	System.err.println( Messages.getMessage("OUTPUT_ERR_DESCRIPTION",out.getName()) );
	        	 return false;
	        }
	        
    	}
    	return true;
    }
    
    
    /**
     * check input and output files
     * @return true if input file readable and output file writable
     */
    protected boolean checkInputs( String[] inputs ){   
        String errMessage = null;
        
        for(int i = 0; i < inputs.length; i ++){
        	final File in = new File(inputs[i] );
	        if(!in.exists()) 
	            errMessage = Messages.getMessage("NONEXIST_INPUT_FILE", in.getPath());
	        else if(!in.isFile())       
	            errMessage = Messages.getMessage("FILE_NOT_DIRECTORY", in.getPath());
	         else if(!in.canRead())
	        	errMessage = Messages.getMessage("UNREAD_INPUT_FILE",in.getPath());     
	           
	        if(errMessage != null){
	        	System.err.println(errMessage);
	        	 return false;
	        }
	       }  	
	        	
	       return true;
      
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

    public MODE getMode(){	return  Mode; }

    
    public void displayHelp(String modeMessage) throws Exception {
    	System.out.println(modeMessage);          
		Thread.sleep(1);
		parser.printHelpOn(System.err);
		      
    }
 
}
