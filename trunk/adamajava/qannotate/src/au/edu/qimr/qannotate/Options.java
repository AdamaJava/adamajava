/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.common.log.QLogger;

import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.modes.*;
/*
 * parse command line to options. 
 */
public class Options {
	public enum MODE {dbsnp, germline, snpeff,confidence, vcf2maf, cadd,indelconfidence,trf, hom}
	
   protected static final String VERSION_DESCRIPTION = Messages.getMessage("VERSION_OPTION_DESCRIPTION");
	 
   protected static final String HELP_DESCRIPTION = Messages.getMessage("HELP_OPTION_DESCRIPTION");  
    
   protected static final String LOG_DESCRIPTION = Messages.getMessage("LOG_OPTION_DESCRIPTION");
   protected static final String LOG_LEVEL_OPTION_DESCRIPTION = Messages.getMessage("LOG_LEVEL_OPTION_DESCRIPTION");
   
   protected static final String test = "test";
   protected static final String control = "control";

//    private final boolean commandCheck = false;
    protected final String commandLine;
    
 	protected final Options.MODE Mode;  
    protected final  OptionParser parser;
    protected QLogger logger = null;
	
 	
	protected final String outputFileName ;
	protected final String inputFileName ;
	protected final String[] databaseFiles;
	protected final String logFileName;
	protected final String logLevel;  
	
	//vcf2maf 
	protected  final String testSample;
	protected  final String controlSample ;
	
	protected  final int bufferSize; //trf
	protected  final int gap ; //cadd
	
	//Vcf2maf
	protected final String center; 
	protected final String sequencer; 
	protected final String outputDir;
	protected final String donorId; 
	
	//snpeff
	protected  final String configFileName ;
	protected  final String summaryFileName ;
	
	//hom 
	protected final int homWindow  ;
	protected final int homReportSize;
	
    /**
     * check command line and store arguments and option information
     */   
    public Options(final String[] args) throws Exception{      	
    	parser = new OptionParser();    	       
    	OptionSet options =  parseArgs(args);
    	
    	if(options.has("mode")){
    		String m = ((String) options.valueOf("mode")).toLowerCase();
    		this.Mode = MODE.valueOf(m); //already checked the validation of mode
    	}else
    		this.Mode = null; 
    		   	
        if(options.has("h") || options.has("help")){ 
        	displayHelp(Mode);  
        	System.exit(0);
        }
        
       //parse parameters        
        commandLine = Messages.reconstructCommandLine(args) ;
        
        //log, input and output are compulsory
        inputFileName = (String) options.valueOf("i") ;      	 
        outputFileName = (String) options.valueOf("o") ; 
    	logFileName = (String) options.valueOf("log");  	
    	logLevel = (String) options.valueOf("loglevel");   
    	
    	//
		List<String> dbList = (List<String>) options.valuesOf("d");		 
		databaseFiles = dbList.toArray(new String[dbList.size()]);
        
        gap = (options.has("gap"))? (int)options.valueOf("gap") : 1000;  //CADD default is 1000
        bufferSize = (options.has("buffer"))? (Integer) options.valueOf("buffer") : 0; //TRF default is 0
        
        
        //vcf2maf
        outputDir = (options.has("outdir"))? (String)options.valueOf("outdir") : null;
        center = (options.has("center"))? (String)options.valueOf("center") : null;
        sequencer = (options.has("sequencer"))? (String)options.valueOf("sequencer") : null;	         
        donorId = (options.has("donor"))? (String)options.valueOf("donor") : null;
        
        //vcf2maf confidence
        testSample = (options.has(test))? (String)options.valueOf(test) : null;
        controlSample = (options.has(control))? (String)options.valueOf(control) : null;
          
        //snpeff
        configFileName = (options.has("config"))? (String) options.valueOf("config") : 
        	   ( Mode.equals(MODE.snpeff)?  new File(databaseFiles[0]).getParent() + "/snpEff.config" : null);
        summaryFileName = (options.has("summaryFile"))?(String) options.valueOf("summaryFile"):
        	( Mode.equals(MODE.snpeff)?  outputFileName +  ".snpEff_summary.html" : null);
        	
        //homoplymers
        homWindow  = (options.has("window"))? (int) options.valueOf("window") : 100;  //default is 100
        homReportSize = (options.has("report"))? (int) options.valueOf("report") : 10; //default is 10
        		
        checkIO();    //not yet complete         	
    }
    
    public OptionSet parseArgs(final String[] args) throws Exception{  
       	parser.allowsUnrecognizedOptions(); 
        parser.acceptsAll( asList("v", "version"), VERSION_DESCRIPTION);
        parser.accepts("mode", Messages.getMessage("MODE_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("mode");   
        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
        parser.acceptsAll( asList("o", "output"), Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("output vcf"); 
        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);

        OptionSet options  = parser.parse(args);  
        if(options.has("v") || options.has("version")){
    		System.err.println( "qannotate: Current version is " + getVersion());
    		System.exit(0);
        }  
        
        Options.MODE mm = null;   	
        if(options.has("mode")){
        	final String	m = ((String) options.valueOf("mode")).toLowerCase();
        	try{
        		mm = MODE.valueOf(m);
        	}catch(IllegalArgumentException | NullPointerException e){
        		System.err.println("invalid mode specified: "  + m);
        		System.exit(0);
        	}
        }
        
        if(mm == null) return options;
        
        if(  mm.equals(MODE.confidence) || mm.equals(MODE.vcf2maf) ){
	        parser.accepts(test,  Messages.getMessage("TUMOUR_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("testSample");
	        parser.accepts(control, Messages.getMessage("NORMAL_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("controlSample");	                	
        }else
            parser.acceptsAll( asList("d", "database"), Messages.getMessage("DATABASE_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("database file"); 

        if(mm.equals(MODE.snpeff)){
	        parser.accepts("config", Messages.getMessage("CONF_FILE_ERR_DESCRIPTION") ).withRequiredArg().ofType(String.class).describedAs("config file");
	        parser.accepts("summaryFile", Messages.getMessage("SUMMARY_FILE_DESCRIPTION")  ).withRequiredArg().ofType(String.class).describedAs("stat output");
        }
                 		
        if(mm.equals(MODE.trf))
            parser.accepts("buffer", "check TRF region on both side of indel within this nominated size" ).withRequiredArg().ofType(Integer.class);//.describedAs("integer");
 
         if(mm.equals(MODE.cadd))
             parser.accepts("gap", "adjacant variants size").withRequiredArg().ofType(String.class).describedAs("gap size");

         if(mm.equals(MODE.hom)){
 	        parser.accepts("window", "check homoplymers inside window size on both side of variants. Default value is " + HomoplymersMode.defaultWindow).withRequiredArg().ofType(String.class).describedAs("window size");
 	        parser.accepts("report", "report specified number of homoplymers base fallen in the swindow. Default value is " + HomoplymersMode.defaultreport  ).withRequiredArg().ofType(String.class).describedAs("report base number");
         }
        
         if( mm.equals(MODE.vcf2maf) ){
 	        parser.accepts("outdir", Messages.getMessage("MAF_OUTPUT_DIRECTORY_OPTION_DESCRIPTION") ).withRequiredArg().ofType(String.class).describedAs("output file location");
 	        parser.accepts("donor",  Messages.getMessage("DONOR_ID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("donor id");	        
 	        parser.accepts("center", "Genome sequencing center").withRequiredArg().ofType(String.class).describedAs("center");
 	        parser.accepts("sequencer", Messages.getMessage("SEQUENCER_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("Sequencer");	 
         }
         
         return parser.parse(args);
        
	} 
       
    public String getVersion(){
    	return Options.class.getPackage().getImplementationVersion();
    }
    
    public String getPGName(){
        return Messages.getProgramName();
    }
    
    /**
     * check input and output files
     * @return true if input file readable and output file writable
     * @throws Exception 
     */
    protected void checkIO( ) throws Exception{   	
    	List<File> inputs = new ArrayList<File>();
    	List<File> outputs = new ArrayList<File>();
    	
    	inputs.add(new File(inputFileName));
    	if( getConfigFileName() != null ) inputs.add(new File(getConfigFileName()));
    	if( databaseFiles != null ) 
    		for(String name : databaseFiles)
    		inputs.add (new File(name));
    	
    	if(outputFileName != null) outputs.add(new File(outputFileName));
    	if( getSummaryFileName() != null) outputs.add(new File(getSummaryFileName()));
    	
    	//check output 	
    	for(File out : outputs)  {   
    		//out.getParentFile() maybe null if file name string exclude path eg. out = "ok.txt"
    		File parent = out.getAbsoluteFile().getParentFile();    		    		
    		if( (out.exists() && ! out.canWrite()) || ( !out.exists() && !parent.canWrite()))    				 
    			throw new Exception( Messages.getMessage("OUTPUT_ERR_DESCRIPTION", out.getName()));
    	}	
    	//check inputs
    	for(File fin : inputs)       	 
	        if(!fin.exists()) 
	        	throw new Exception( Messages.getMessage("NONEXIST_INPUT_FILE", fin.getPath()));
	         else if(!fin.canRead())
	        	 throw new Exception( Messages.getMessage("UNREAD_INPUT_FILE",fin.getPath()));     
	     	
    	//check whether file unique
 	    inputs.addAll(outputs);
       	for(int  i = inputs.size() -1; i > 0; i --)
    		for (int j = i-1; j >= 0; j -- ){
    			if(inputs.get(i).getCanonicalFile().equals(inputs.get(j).getCanonicalFile()))
    			throw new Exception( "below command line values are point to same file: \n\t" + inputs.get(i) + "\n\t" + inputs.get(j) );
    		}    
       	//here we don't check outdir for vcf2maf mode, the Vcf2Maf.java will do the job
       	
    }
    
    
  

	public String getLogFileName() { return logFileName;}	
	public String getLogLevel(){ return logLevel; }	 
	public String getCommandLine() {	return commandLine; }	
	public String getInputFileName(){return inputFileName;}
	public String getOutputFileName(){return outputFileName;}
	public String getDatabaseFileName(){return databaseFiles[0];}	
	public String[] getDatabaseFiles(){ return  (Mode.equals(MODE.cadd))? databaseFiles : null;}
    public MODE getMode(){	return  Mode; }
   
    public void displayHelp(MODE mode) throws Exception {   
        String mess = Messages.getMessage("USAGE");  
        if(mode != null){
        	switch (mode){        		
                case dbsnp: mess = Messages.getMessage("DBSNP_USAGE"); break;
                case germline: mess = Messages.getMessage("GERMLINE_USAGE"); break;
                case trf: mess = Messages.getMessage("TRF_USAGE"); break;
                case snpeff: mess = Messages.getMessage("SNPEFF_USAGE"); break;
                case confidence: mess = Messages.getMessage("CONFIDENCE_USAGE"); break;
                case cadd: mess = Messages.getMessage("CADD_USAGE"); break;
                case indelconfidence: mess = Messages.getMessage("INDELCONFIDENCE_USAGE"); break;
                case vcf2maf: mess = Messages.getMessage("VCF2MAF_USAGE"); break;
                case hom: mess = Messages.getMessage("HOM_USAGE"); break;
            } 
        }   
        
        System.out.println(mess);          
        Thread.sleep(1);
        parser.printHelpOn(System.err);    
    }   
        
    //vcf2maf confidence
	public String getTestSample(){  return testSample; }
	public String getControlSample(){  return controlSample; }
	
	//vcf2maf
	public String getCenter(){  return ( Mode.equals(MODE.vcf2maf))? center: null; }
	public String getSequencer(){  return ( Mode.equals(MODE.vcf2maf))? sequencer: null; }
	public String getOutputDir(){return ( Mode.equals(MODE.vcf2maf))? outputDir:null;}
	public String getDonorId(){return ( Mode.equals(MODE.vcf2maf))? donorId :null; }
	 
	 //snpEff
	public String getSummaryFileName(){ return  (Mode.equals(MODE.snpeff))? summaryFileName : null; }		
	public String getGenesFileName(){ return  (Mode.equals(MODE.snpeff))? outputFileName + ".snpEff_genes.txt" : null; 	}		
	public String getConfigFileName() { return (Mode.equals(MODE.snpeff))? configFileName : null; }
	
	public int getBufferSize(){ return (Mode.equals(MODE.trf))? bufferSize : -1; } //trf
	public int getGapSize(){ return (Mode.equals(MODE.trf))? gap : -1; } //cadd
	
	//hom
	public int getHomoplymersWindow(){ return (Mode.equals(MODE.hom))? homWindow : -1; } //trf
	public int getHomoplymersReportSize(){ return (Mode.equals(MODE.hom))? homReportSize : -1; } //cadd


}
