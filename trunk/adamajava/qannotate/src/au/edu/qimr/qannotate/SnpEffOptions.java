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
        
        //check IO
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
        databaseFileName = (String) options.valueOf("database") ;
        configFileName = (String) options.valueOf("config") ;
        statOutputFileName = (String) options.valueOf("statFile") ;
                       
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
            return false;
        } else{  
        	logFileName = (String) options.valueOf("log");  	
        	logLevel = (String) options.valueOf("loglevel");
        }
    
                
        return hasCommandChecked();
    } 

    public void displayHelp() throws Exception {
    	    System.out.println(Messages.getMessage("SNPEFF_USAGE"));       
		   
		    parser.printHelpOn(System.err);
		  
    }


	public boolean hasCommandChecked(){  	
    	String message = null;
         File database = new File(databaseFileName); 
   	
    	if(databaseFileName == null)
    		message = Messages.getMessage("MISSING_OPTION", "--database");       	
        else if(!database.isDirectory())
        	message = Messages.getMessage("DATABASE_FILE_ERR_DESCRIPTION",MODE.snpEff.name(), database.getAbsolutePath()); 
       // 	|| !database.canRead()) 
        	 
        else if(configFileName != null){
        	File conf = new File(configFileName);
        	if(!conf.exists() || !conf.isFile() || !conf.canRead()) 
        		message = Messages.getMessage("CONF_FILE_ERR_DESCRIPTION",MODE.snpEff.name(), conf.getName());
        }else if( statOutputFileName != null ){
        	File stat = new File(statOutputFileName);     
            if(( stat.exists() && ! stat.canWrite()) || !stat.getParentFile().canWrite() )
           	 message = Messages.getMessage("OUTPUT_ERR_DESCRIPTION",stat.getName());
        }
        
        if(message != null){
        	System.err.println( message);
            return false;
        }
        
       //finally check input and output 
        return checkIO();
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
