package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;
//import au.edu.qimr.qannotate.options.Options.MODE;
import joptsimple.OptionSet;
import au.edu.qimr.qannotate.Messages;


/*
 * parse command line to options. 
 */
public class GermlineOptions extends Options {
  
		public GermlineOptions(){ super(Options.MODE.germline);   }

    /**
     * check command line and store arguments and option information
     */
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.acceptsAll( asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file"); 

        parser.accepts("mode", "run germline mode").withRequiredArg().ofType(String.class).describedAs("germline");
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        final OptionSet options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
        	displayHelp(Messages.getMessage("GERMLINE_USAGE"));
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
        final String[] inputs = new String[]{ inputFileName,databaseFileName} ;
        final String[] outputs = new String[]{outputFileName};
        final String [] ios = new String[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);
        
        return checkInputs(inputs )  && checkOutputs(outputs ) && checkUnique(ios);
         
    } 
 

 
   
}
