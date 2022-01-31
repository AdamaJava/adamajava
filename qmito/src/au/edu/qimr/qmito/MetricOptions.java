/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import java.io.File;
import java.util.List;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;


/*
 * parse command line to options. 
 */
public class MetricOptions  extends Options {
   
    private String query = "and (flag_NotprimaryAlignment == false, flag_ReadUnmapped == false)";
    private String defaultRefName = "chrMT";
    private SAMSequenceRecord referenceRecord;
    private String referenceFile;
//    private SAMSequenceRecord referenceRecord;
    private int lowreadcount = 10;
    private int nonrefthreshold = 20;

	private String[] inputBamNames = null;
    private String outputFileName = null;
    private final OptionParser parser1;
    
    
    public MetricOptions(String[] args) throws Exception {
		super(args);			 
		parser1 = getParser();
		
        //stop here if ask help or version
		if(  hasVersionOption())  return;
		 			
        parser1.accepts( "query", Messages.getMessage("QUERY_OPTION_DESCRIPTION")).withRequiredArg().ofType(String.class);
        parser1.accepts(  "input", Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class);
        parser1.accepts("output", Messages.getMessage("OUTPUT_DESCRIPTION")).withRequiredArg().ofType(String.class); 
        parser1.accepts("reference", Messages.getMessage("REFERENCE_DESCRIPTION")).withRequiredArg().ofType(String.class);
        parser1.accepts("mito-name", Messages.getMessage("MITO_SEQUENCE_DESCRIPTION")).withRequiredArg().ofType(String.class);
        parser1.accepts("lowread-count",Messages.getMessage("LOW_READ_COUNT_DESCRIPTION")).withRequiredArg().ofType(Integer.class);
        parser1.accepts("nonref-threshold",Messages.getMessage("NONREFERENCE_THRESHOLD_DESCRIPTION")).withRequiredArg().ofType(Integer.class);
        
        //help can only display after all options are parsed
        if(hasHelpOption()) {
        	displayHelp();
        	return;
	     }
         
        //stop here if ask help or version
		if(  hasVersionOption()) {
			System.err.println( getVersion());  			
			return;
		}

	    OptionSet options1 = parser1.parse(args);  
      
       	if(options1.has("nonref-threshold"))
    		nonrefthreshold = (Integer) options1.valueOf("nonref-threshold");
       	
       	if(options1.has("lowread-count"))
    		lowreadcount = (Integer) options1.valueOf("lowread-count");       	
       	
        if ( !options1.has("input") || ! options1.has("output")  || !options1.has("reference") || !options1.has("log")) {
	        System.out.println(Messages.getMessage("WRONG_OPTIONS",QLogger.reconstructCommandLine(Messages.getProgramName(), args))); 
            System.out.println(Messages.getMessage("USAGE_METRIC"));
            return;            
        }

        //check query string format
        if(  options1.has("query")){
	        query = options1.valueOf("q").toString();                  
	        checkQuery(query);       	 
        }
        
        
       	    
        checkCommand( options1 ); 		
	}
    
      
    /**
     * 
     * @return true if no Exception happens
     * @throws Exception
     */
    private void checkCommand(OptionSet options) throws Exception{     
    	    
        List inputs = options.valuesOf("input");
        inputBamNames = new String[inputs.size()];
        inputs.toArray(inputBamNames);   
        checkInputBams();
        
        //check Reference
        referenceFile = (String) options.valueOf("reference");
        if( ! (new File(referenceFile).exists()))
        	throw new Exception("reference is not exist: " + referenceFile);  
        
        String ref = options.has("mito-name")? (String) options.valueOf("refname") : defaultRefName;
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(inputBamNames[0])); ) {
        	SAMFileHeader header = reader.getFileHeader().clone();          
            referenceRecord = header.getSequence(header.getSequenceIndex(ref));
    		if(referenceRecord == null)
    			throw new Exception("invalide reference sequence name: " + ref);
        }
        		
         //check outputs
        outputFileName =  (String) options.valueOf("output");
        File output = new File(outputFileName);
        if( output.exists() && !output.canWrite())
        	throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName)); 
		if (!output.exists() && !new File(outputFileName).getParentFile().canWrite()) 
			throw new Exception(Messages.getMessage("UNWRITE_OUTPUT_FILE", outputFileName));          	
    	 
    }
    

    private void checkInputBams() throws Exception {
		for (String bam : inputBamNames) {
			File bamFile = new File(bam);
			if (!bamFile.exists()) 
				throw new Exception(Messages.getMessage("NO_FILE", bam));
								
			String bamCanPath = bamFile.getCanonicalPath();
			File bamLock = new File(bamCanPath + ".lck");
			File bamIndex = new File(bamCanPath + ".bai");					
			
			if (bamLock.exists() )					
				throw new Exception(Messages.getMessage("BAM_LOCK", bam));
				
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");			
			File indexLock = new File(bam + ".bai.lck");
			//does bam have index
			if (!reader.hasIndex()) {
				reader.close();
				throw new Exception(Messages.getMessage("NO_INDEX", bam));
			} 
			
			if (bamIndex.lastModified() < bamFile.lastModified()) {
				reader.close();
				throw new Exception(Messages.getMessage("INDEX_OLD", bam));
			}
								
			if (indexLock.exists()) {
				reader.close();
				throw new Exception(Messages.getMessage("INDEX_LOCK", bam));
			}
			
			reader.close();								
		}
	}
    	   	
    private void checkQuery(String query) throws Exception {
    	try{
    		new QueryExecutor(query);
    	}catch(Exception e){    		
    		throw new Exception(Messages.getMessage("QUERY_ERROR", query));           
    	}    	
    }
 
    
    public String getOutputFileName(){
        return outputFileName;
    }
    
    public String[] getInputFileNames(){
        return inputBamNames;
    }
    
    public String getReferenceFile(){
    	return referenceFile;
    }
    
    public String getQuery(){
        return query;
    }
    
    public SAMSequenceRecord  getReferenceRecord(){    	   	
    	return referenceRecord;
    }

    public int getLowReadCount(){
       	return lowreadcount;
    		 
    }
    
    public int getNonRefThreshold(){ 
    	return nonrefthreshold;    		 
    }
    
	@Override
	void displayHelp() throws Exception {	
		System.err.println( Messages.getMessage("USAGE_METRIC") );
		parser1.formatHelpWith(new BuiltinHelpFormatter(150, 2));
		parser1.printHelpOn(System.err);
	}

}
