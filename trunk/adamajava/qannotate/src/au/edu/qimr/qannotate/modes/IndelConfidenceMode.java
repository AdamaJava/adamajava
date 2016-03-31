package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.BitSet;
import java.util.HashSet;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.options.IndelConfidenceOptions;

/**
 * @author christix
 * annotate whether indel is high, low or zero confidence
 *
 */
public class IndelConfidenceMode extends AbstractMode{
	private final QLogger logger = QLoggerFactory.getLogger(IndelConfidenceMode.class);
//	private final IndelConfidenceOptions options;
	
	final String input;
	final String output;
	final String commandLine;
	final int MAX_CONTIG_SIZE = 250000000;	
	static final float DEFAULT_NIOC = 0.1f;
	static final int DEFAULT_HOMN = 6;
	final BitSet[] mask = new BitSet[24]; 
		
 	//filters 
	public static final String FILTER_REPEAT = "REPEAT"; 
	public static final String DESCRITPION_INFO_CONFIDENCE =  "set to HIGH if the variants passed all filter, "
			+ "nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if "
			+ "coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants";
 	
	public static final String DESCRITPION_FILTER_REPEAT = String.format( "this variants is fallen into the repeat region");
//	public enum Confidence{	HIGH , LOW, ZERO ; }
	
	@Deprecated
	//unit test only
	public IndelConfidenceMode(){
 		this.input = null;
		this.output = null;
		commandLine = null;
	}
	
	public IndelConfidenceMode(IndelConfidenceOptions options) throws Exception{	
		input = options.getInputFileName();
		output = options.getOutputFileName();
		commandLine = options.getCommandLine();
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("mask File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
          
		addAnnotation(options.getDatabaseFileName() );				
	}

	/**
	 * load repeart region into RAM 
	 * @param dbfile
	 * @throws Exception
	 */
	private void loadMask(String dbfile) throws Exception {		
        //load repeat region to bitset
        try(BufferedReader reader = new BufferedReader(new FileReader(dbfile))){
                String line;
                while (( line = reader.readLine()) != null) {
                        String[] array = line.split(" ");
               	
                        try{
                        	int no = Integer.parseInt(array[0]) - 1;
                        	int start = Integer.parseInt(array[1]);
                        	int end = Integer.parseInt(array[2]);
                        	if(no >= 24 || no < 0 || start < 1 || end < 1)
                        		throw new NumberFormatException();
                         	if(mask[no] == null)
                        		mask[no] = new BitSet();
                        	mask[no].set(start, end);                       	
                        }catch(NumberFormatException e){
                        	 //logger.warn("can't convert mask file string into integer: " + line);
                        	 continue;
                        }
                }
		}        
	}
		
	/*
	 *check the confidence level
	 */
	MafConfidence getConfidence(VcfRecord vcf){
		String filter = vcf.getFilter();
		
		
		if( filter.equals(VcfHeaderUtils.FILTER_PASS) ||  filter.matches("HOM\\d+")) {//|| filter.contains(IndelUtils.FILTER_HOM)){
			//check nearby indel  
			String info = vcf.getInfoRecord().getField(IndelUtils.INFO_NIOC);
			float rate = 0;
			try{
				 rate = Float.parseFloat(info);
			}catch(NullPointerException | NumberFormatException  e){
				//do nothing
			}
			
			int lhomo = 0;
			try{	
				lhomo = Integer.parseInt(filter.replace(IndelUtils.FILTER_HOM, ""));
			}catch(NullPointerException | NumberFormatException  e){
				// do nothing				
			}
		
			if(rate <= DEFAULT_NIOC && lhomo <= DEFAULT_HOMN )
				return MafConfidence.HIGH;			
			
		}else if(filter.equals(IndelUtils.FILTER_HCOVN) || filter.equals(IndelUtils.FILTER_HCOVT) || 
				filter.equals(FILTER_REPEAT) || filter.contains(Constants.SEMI_COLON + FILTER_REPEAT + Constants.SEMI_COLON)  ||
				filter.startsWith(FILTER_REPEAT + Constants.SEMI_COLON) || filter.endsWith(Constants.SEMI_COLON + FILTER_REPEAT)) { 
			return MafConfidence.ZERO;
		} 
		
		//default is low
		return MafConfidence.LOW;
	}
	
 
	 boolean isRepeat(VcfRecord vcf){
       	try{
    		String chr = vcf.getChromosome().toLowerCase(); 
    		if(chr.startsWith("chr"))
    			chr = chr.substring(3);
    		int no = Integer.parseInt(chr) - 1;
    		BitSet chrMask = mask[no];
    		//do nothing when contig isn't on mask lists
    		if(chrMask == null)
    			throw new NumberFormatException();
    		
    		for (int i = vcf.getPosition(); i <= vcf.getChrPosition().getEndPosition(); i ++) 
    			if(chrMask.get(i)) 				
    				return true; 
    			              		               		
    	}catch(NumberFormatException e){
       	  return false;
       }
    	
       	return false; 
       	
	}


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		loadMask( dbfile );	
		
		long count = 0;
		long repeatCount = 0; 
		HashSet<ChrPosition> posCheck = new HashSet<ChrPosition>();	
		try (VCFFileReader reader = new VCFFileReader(input) ;
                VCFFileWriter writer = new VCFFileWriter(new File(output ))  ) {
			    
			//reheader
		    VcfHeader hd = 	reader.getHeader();
		    hd.addFilterLine(FILTER_REPEAT, DESCRITPION_FILTER_REPEAT );       	  
		    hd.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "1", "String", DESCRITPION_INFO_CONFIDENCE);		    
		    hd = reheader(hd, commandLine ,input);			    	  
	
		    for(final VcfHeader.Record record: hd)  
		    	writer.addHeader(record.toString());
		
	        for (final VcfRecord vcf : reader) {               	
	        	if( isRepeat(vcf) ){
	        		VcfUtils.updateFilter(vcf, FILTER_REPEAT);
	        		repeatCount ++;
	        	}
	    		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENT, getConfidence(vcf).toString());		
	    		
	    		count++;
	    		posCheck.add(vcf.getChrPosition());
	    		writer.add(vcf);
	        }
		}  
		logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  count , posCheck.size()));
		logger.info("number of variants fallen into repeat region is " + repeatCount);
					 
	}
 
}	
	
  
	
 
