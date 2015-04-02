package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;
import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.options.Options.MODE;
import joptsimple.OptionSet;

/*
 * parse command line to options. 
 */
public class CustomerConfidenceOptions extends Options {
 	
//    private boolean passOnly = false;
    private int min_read_count = 50;
    private int min_mutant_rate = 10;

    /**
     * check command line and store arguments and option information
     */
 	
 	// public Options(){  parser = new OptionParser(); this.Mode = null; } 
 	public CustomerConfidenceOptions(){ super(Options.MODE.customerConfidence);   }
 	
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.accepts("mode", "run customer confident mode").withRequiredArg().ofType(String.class).describedAs("customerConfidence");
//        parser.accepts("passOnly", Messages.getMessage("PASSONLY_DESCRIPTION"));
        parser.accepts("minReadCount", Messages.getMessage("MIN_READ_COUNT_DESCRIPTION")).withRequiredArg().ofType(Integer.class);
        parser.accepts("minMutantRate", Messages.getMessage("MIN_MUTANT_RATE_DESCRIPTION")).withRequiredArg().ofType(Integer.class) ;
        
        parser.accepts(test,  Messages.getMessage("TUMOUR_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("testSample");
        parser.accepts(control, Messages.getMessage("NORMAL_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("controlSample");	        

        
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse(args);    
        if(options.has("h") || options.has("help")){
        	displayHelp(Messages.getMessage("CUSTOMERCONFIDENCE_USAGE"));
            return false;
        }
               
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
            return false;
        } else{  
        	logFileName = (String) options.valueOf("log");  	
        	logLevel = (String) options.valueOf("loglevel");
        }   
        
//        if(options.has("passOnly")) passOnly = true; 
        if(options.has("min_read_count"))
        	min_read_count = (Integer) options.valueOf("min_read_count");
        if(options.has("min_mutant_rate"))
        	min_mutant_rate = (Integer) options.valueOf("min_mutant_rate");
       
        testSample = (options.has(test))? (String)options.valueOf(test) : null;
        controlSample = (options.has(control))? (String)options.valueOf(control) : null;
 
        
        commandLine = Messages.reconstructCommandLine(args) ;
        
        //check IO
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
//        databaseFileName = (String) options.valueOf("d") ; 

        String[] inputs = new String[]{ inputFileName} ;
        String[] outputs = new String[]{outputFileName};
        String [] ios = new String[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);
        return checkInputs(inputs )  && checkOutputs(outputs ) && checkUnique(ios);

    } 

   
//     public boolean isPassOnly(){  	 return passOnly; }
     
     public int get_min_read_count(){ return min_read_count; }
     
     public int get_min_mutant_rate(){ return min_mutant_rate;}
   
}
