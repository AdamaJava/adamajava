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

/*
 * parse command line to options. 
 */
public class GermlineOptions extends Options {
  
//    private boolean commandCheck = false;
//    private String commandLine;

//    private final OptionParser parser = new OptionParser();
//    private final OptionSet options;

//    private String outputFileName = null;
//    private String inputFileName = null;
//    private String databaseFileName = null;
//    private String logFileName = null;

//    private  String logLevel;  
	
//	public static final String DEFAULT_SUMMARY_CSV_FILE = "snpEff_summary.csv";
	
 	public static final Options.MODE Mode = Options.MODE.germline;
    

    public GermlineOptions(final String[] args) throws Exception{  	
    	super();
    	parseArgs( args);
    }

    /**
     * check command line and store arguments and option information
     */
    @Override
    public boolean parseArgs(final String[] args) throws Exception{  	
    	 
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.accepts("mode", "specify [germline] here").withRequiredArg().ofType(String.class).describedAs("germline mode");
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
        	displayHelp();
            return false;
        }
        
        //check IO
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
         if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
            return false;
        } else{  
        	logFileName = (String) options.valueOf("log");  	
        	logLevel = (String) options.valueOf("loglevel");
        }
    
                
        return checkIO();
    } 

    public void displayHelp() throws Exception {
    	 System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));
		    Thread.sleep(1);
		    parser.printHelpOn(System.err);
		    System.out.println(Messages.getMessage("SNPEFF_USAGE"));       
    }


   
}
