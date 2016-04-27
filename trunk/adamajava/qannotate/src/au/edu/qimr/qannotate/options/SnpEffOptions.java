package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;

import java.io.File;

import au.edu.qimr.qannotate.Messages;
import joptsimple.OptionSet;


/*
 * parse command line to options. 
 */
public class SnpEffOptions extends Options {
	public static final String DEFAULT_GENES_FILE_SUFFIX = ".snpEff_genes.txt";
	public static final String DEFAULT_SUMMARY_FILE_SUFFIX = ".snpEff_summary.html";
	public static final String DEFAULT_CONFIG_FILE = "snpEff.config";
	public static final String CONF_DESCRIPTION = "(optional) configure file with full path, by default will be " + 
			DEFAULT_CONFIG_FILE + " under database file directory";

	public static final String Summary_DESCRIPTION = "(optional) output stats file with full path, "
			+ "by default will be output file name plus " + DEFAULT_SUMMARY_FILE_SUFFIX ;
			 

	
    private String configFileName ;
    private String summaryFileName ;
    
 

    public SnpEffOptions( ) {  super(Options.MODE.snpEff);	  }

    /**
     * check command line and store arguments and option information
     */
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.accepts("mode", "run snpeff mode").withRequiredArg().ofType(String.class).describedAs("snpEff");
        parser.acceptsAll( asList("d", "database"), Messages.getMessage("SNPEFF_DATABSE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file"); 
        parser.accepts("config", CONF_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("config file");
        parser.accepts("summaryFile", Summary_DESCRIPTION).withRequiredArg().ofType(String.class).describedAs("stat output");
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        OptionSet options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
        	displayHelp(Mode);
            return false;
        }
                               
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
            return false;
        } else{  
        	logFileName = (String) options.valueOf("log");  	
        	logLevel = (String) options.valueOf("loglevel");
        }
    
        commandLine = Messages.reconstructCommandLine(args) ;
        //check IO
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
        databaseFileName = (String) options.valueOf("d") ;
        configFileName = (String) options.valueOf("config") ;
        summaryFileName = (String) options.valueOf("summaryFile") ;
        //,databaseFileName
        String[] inputs = new String[]{ inputFileName,getConfigFileName()} ;
        String[] outputs = new String[]{outputFileName, getSummaryFileName()};
        String [] ios = new String[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);

        return checkInputs(inputs )  && checkOutputs(outputs ) && checkUnique(ios);
        
     } 

  
	public String getConfigFileName() { 
		if(configFileName == null)
			configFileName = new File(databaseFileName).getParent() + "/" + DEFAULT_CONFIG_FILE; 
		return configFileName;
		
	}	  
	
	public String getSummaryFileName(){
		if(summaryFileName == null)
			summaryFileName = outputFileName + DEFAULT_SUMMARY_FILE_SUFFIX;
		
		return  summaryFileName;
	}
	
	public String getGenesFileName(){
		 
			return outputFileName + DEFAULT_GENES_FILE_SUFFIX;
		
		 
	}
}
