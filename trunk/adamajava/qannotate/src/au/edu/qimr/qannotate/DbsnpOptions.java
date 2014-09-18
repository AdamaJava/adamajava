package au.edu.qimr.qannotate;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;

import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;

import au.edu.qimr.qannotate.Options.MODE;

/*
 * parse command line to options. 
 */
public class DbsnpOptions extends Options {
 	
 //	 public final Options.MODE Mode = Options.MODE.dbSNP;

    /**
     * check command line and store arguments and option information
     */
 	
 	// public Options(){  parser = new OptionParser(); this.Mode = null; } 
 	public DbsnpOptions(){ super(Options.MODE.dbSNP);   }
 	
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.acceptsAll( asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file"); 
        parser.accepts("mode", "run dbSNP mode").withRequiredArg().ofType(String.class).describedAs("dbsnp");
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        OptionSet options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
        	displayHelp();
            return false;
        }
        
        
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
            return false;
        } else{  
        	logFileName = (String) options.valueOf("log");  	
        	logLevel = (String) options.valueOf("loglevel");
        }   
        
        //check IO
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
        databaseFileName = (String) options.valueOf("d") ; 

        String[] inputs = new String[]{ inputFileName,databaseFileName} ;
        String[] outputs = new String[]{outputFileName};
        String [] ios = new String[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);
        return checkInputs(inputs )  && checkOutputs(outputs ) && checkUnique(ios);
        
  
    } 

    public void displayHelp() throws Exception {
   
    	 System.out.println(Messages.getMessage("DBSNP_USAGE"));     
		    Thread.sleep(1);
		    parser.printHelpOn(System.err);
		      
    }
    


   
}
