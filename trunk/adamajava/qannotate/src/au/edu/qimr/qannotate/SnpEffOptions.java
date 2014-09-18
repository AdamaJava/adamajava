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
public class SnpEffOptions extends Options {
	public final Options.MODE Mode = Options.MODE.snpEff;
	
	public static final String DEFAULT_SUMMARY_FILE_SUFFIX = ".snpEff_summary.html";
	public static final String DEFAULT_CONFIG_FILE = "snpEff.config";
	 
    private String configFileName ;
    private String statOutputFileName ;
    
    public SnpEffOptions( ) throws Exception{  	super(); }

    public SnpEffOptions(final String[] args) throws Exception{  	
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
        parser.accepts("mode", "run snpeff mode").withRequiredArg().ofType(String.class).describedAs("snpEff");
       // "(compulsary) database location"
        parser.acceptsAll( asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file"); 
        parser.accepts("config", "(optional) configure file with full path").withRequiredArg().ofType(String.class).describedAs("config file");
        parser.accepts("statFile", "(optional) output stats file with full path").withRequiredArg().ofType(String.class).describedAs("stat output");
//        parser.acceptsAll( asList("m", "mode"), "snpEff").withRequiredArg().ofType(String.class);
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
        configFileName = (String) options.valueOf("config") ;
        statOutputFileName = (String) options.valueOf("statFile") ;
        
        String[] inputs = new String[]{ inputFileName,databaseFileName,getConfigFileName()} ;
        String[] outputs = new String[]{outputFileName, getStatOutputFileName()};
        String [] ios = new String[inputs.length + outputs.length];
        System.arraycopy(inputs, 0, ios, 0, inputs.length);
        System.arraycopy(outputs, 0, ios, inputs.length, outputs.length);

        return checkInputs(inputs )  && checkOutputs(outputs ) && checkUnique(ios);
        
     } 

    public void displayHelp() throws Exception {
    	    System.out.println(Messages.getMessage("SNPEFF_USAGE"));       
		   
		    parser.printHelpOn(System.err);
		  
    }

  
	public String getConfigFileName() { 
		if(configFileName == null)
			configFileName = new File(databaseFileName).getParent() + "/" + DEFAULT_CONFIG_FILE; 
		return configFileName;
		
	}	  
	
	public String getStatOutputFileName(){
		if(statOutputFileName == null)
			statOutputFileName = outputFileName + DEFAULT_SUMMARY_FILE_SUFFIX;
		
		return  statOutputFileName;
	}
}
