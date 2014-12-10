package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;
import joptsimple.OptionSet;
import au.edu.qimr.qannotate.Messages;

public class Vcf2mafOptions extends Options {
	 public Vcf2mafOptions( ) {  super(Options.MODE.vcf2maf);	  }
	 
	 @Override
	    public boolean parseArgs(final String[] args) throws Exception{  	
	    	 
	        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
	        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
	        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
	        parser.accepts("mode", "run vcf2maf").withRequiredArg().ofType(String.class).describedAs("vcf2maf");
	       // "(compulsary) database location"
 	        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
	        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
	        final OptionSet options = parser.parse(args);   
	        
	        if(options.has("h") || options.has("help")){
	        	displayHelp(Messages.getMessage("SNPEFF_USAGE"));
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
	        
	        
  	        	return true;
	     } 
	 
}
