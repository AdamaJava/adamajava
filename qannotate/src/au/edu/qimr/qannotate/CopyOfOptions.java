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
public class CopyOfOptions {
    private static final String VERSION_DESCRIPTION =
        Messages.getMessage("VERSION_OPTION_DESCRIPTION");
   private static final String HELP_DESCRIPTION =
        Messages.getMessage("HELP_OPTION_DESCRIPTION");  
    
   private static final String LOG_DESCRIPTION =
       Messages.getMessage("LOG_OPTION_DESCRIPTION");
   private static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages
	.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   
    private boolean commandCheck = false;
    private String commandLine;

    private final OptionParser parser = new OptionParser();
    private final OptionSet options;

    private String outputFileName = null;
    private String inputFileName = null;
    private String dbSNPFileName = null;
    private String logFileName = null;

    private  String logLevel;  
 

    /**
     * check command line and store arguments and option information
     */
    public CopyOfOptions(final String[] args) throws Exception{ 

        parser.acceptsAll( asList("h", "help"), HELP_DESCRIPTION);
        parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.acceptsAll( asList("m", "mode"), Messages.getMessage("MODE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("mode");   
//        parser.acceptsAll( asList( "dbSNP"), Messages.getMessage("DBSNP_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("dbSNP vcf");              
        parser.accepts("dbSNP", Messages.getMessage("DBSNP_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("dbSNP vcf");
        parser.accepts("dbSNP", Messages.getMessage("DBSNP_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("dbSNP vcf");
        
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
        options = parser.parse(args);   
        
        if(options.has("h") || options.has("help")){
            System.out.println(Messages.getMessage("HELP_OPTION_DESCRIPTION"));
            Thread.sleep(1);
            parser.printHelpOn(System.err);
            System.out.println(Messages.USAGE);
            return;
        }
        if(options.has("v") || options.has("version")){
            System.out.println( "Current version is " + getVersion());
            return;
        }
        
        commandLine = Messages.reconstructCommandLine(args);
        
        //check IO
        inputFileName = options.valueOf("i").toString();      	 
        outputFileName = options.valueOf("o").toString();      
        dbSNPFileName = options.valueOf("dbSNP").toString();
                      
        if( !options.has("log")){
            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));
            Thread.sleep(1);
            parser.printHelpOn(System.err);
            System.out.println(Messages.USAGE);
            return;
        }
        else{       	
        	setLogFileName((String) options.valueOf("log"));        	
        }
                  
        logLevel = (String) options.valueOf("loglevel");
                
        commandCheck = checkFiles();
    } 
        
    private boolean checkFiles(  ){   	
        File in = new File(inputFileName);
        File out = new File(outputFileName);
        File fsnp= new File(dbSNPFileName);
        
        if(!fsnp.exists() || !fsnp.isFile() || !fsnp.canRead()){
        	String message = Messages.getMessage("DBSNPFILE_ERR_DESCRIPTION",fsnp.getName());
        	System.err.println( message);
            return false;
        }
        
        if(!in.exists()){
            String message = Messages.getMessage("NONEXIST_INPUT_FILE", inputFileName);
            System.err.println( message);
            return false;
        }

        if(!in.isFile()){       
            String message = Messages.getMessage("FILE_NOT_DIRECTORY", inputFileName);
            System.err.println( message);
            return false;
        }
        
        if(!in.canRead()){
            System.err.println(Messages.getMessage("UNREAD_INPUT_FILE"));
            return false;
        }

        if(in.getName().equals(out.getName()) && in.getPath().equals(out.getPath())  ){
            String message = Messages.getMessage("INPUT_SAME_OUTPUT",in.getName(),out.getName());
            System.err.println( message);
            return false;          
        }

        return true;
    }


    public void displayHelp() throws Exception {
        parser.printHelpOn(System.err);
    }

    public boolean hasCommandChecked(){
        return commandCheck;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
     
    public String getInputFileName(){
        return inputFileName;
    }
    
    public String getDbSNPFileName(){
    	
    	return dbSNPFileName;
    }
    
     
    
    public String getVersion(){
    	return CopyOfOptions.class.getPackage().getImplementationVersion();
    }
    
    public String getPGName(){
        return Messages.getProgramName();
    }
    
	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	
	public String getLogFileName() {
		return logFileName;
	}
	
	public String getLogLevel(){
		return logLevel;
	}
	
	/**
	 * Gets the command line.
	 *
	 * @return the command line
	 */
	public String getCommandLine() {
		return commandLine;
	}
	
	
}
