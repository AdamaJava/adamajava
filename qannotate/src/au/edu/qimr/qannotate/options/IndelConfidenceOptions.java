package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;

import java.awt.List;

import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.options.Options.MODE;
import joptsimple.OptionSet;

/*
 * parse command line to options. 
 */
public class IndelConfidenceOptions extends Options {
 	
  boolean div = false;
  boolean mnv = false;
  boolean snv = false;  
    
    enum Variants{ 	MNV  , SNV  , DIV  ;  }
    
    Variants Div, Mnv, Snv; 

    /**
     * check command line and store arguments and option information
     */
 	
 	// public Options(){  parser = new OptionParser(); this.Mode = null; } 
 	public IndelConfidenceOptions(){ super(Options.MODE.indelConfidence);   }
 	
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.acceptsAll( asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("repeat file"); 
        parser.accepts("mode", "run indelConfidence mode").withRequiredArg().ofType(String.class).describedAs("indelConfidence");
//        parser.accepts("VC", "Variation Class").withRequiredArg().ofType(String.class).describedAs("must be DIV, SNV or MNV. By default is SNV and MNV");        
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        OptionSet options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
        	displayHelp(Messages.getMessage("INDELCONFIDENCE_USAGE"));
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
          
        String[] inputs = new String[]{ inputFileName,databaseFileName} ;
        String[] outputs = new String[]{outputFileName};
        String [] ios = new String[]{ inputFileName,databaseFileName,outputFileName} ;
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);
        
        
        return checkInputs(inputs) && checkOutputs(outputs) && checkUnique(ios);
     
    } 


   
}
