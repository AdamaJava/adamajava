package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;
import joptsimple.OptionSet;
import au.edu.qimr.qannotate.Messages;


public class FixOptions extends Options {
	 public FixOptions( ) {  super(Options.MODE.vcf2maf);	  }
 	 
	 
	 final String  Description_sequencer = "eg.  <Illumina GAIIx, Illumina HiSeq,SOLID,454, ABI 3730xl, Ion Torrent PGM,Ion Torrent Proton,PacBio RS, Illumina MiSeq,Illumina HiSeq 2500,454 GS FLX Titanium,AB SOLiD 4 System>";
	 String  sequencer ;
	 
	 @Override
	    public boolean parseArgs(final String[] args) throws Exception{  	
	    	 
	        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
	        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
	        parser.acceptsAll( asList("o", "output"),  "output maf file with full path").withRequiredArg().ofType(String.class).describedAs("output maf"); 
	        
 	        parser.accepts("sequencer", Description_sequencer).withRequiredArg().ofType(String.class).describedAs("Sequencer");	        
	        
	        parser.accepts("mode", "run f").withRequiredArg().ofType(String.class).describedAs("fix");
	       // "(compulsary) database location"
 	        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
	        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
	        final OptionSet options = parser.parse(args);   
	        
	        if(options.has("h") || options.has("help")){
	        	displayHelp(Messages.getMessage("VCF2MAF_USAGE"));
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
	        
 
	        sequencer = (options.has("sequencer"))? (String)options.valueOf("sequencer") : null;	 
 	        
  	        return true;
	     } 
	 
	 
	 public String getSequencer(){  return sequencer; }
	 
}
