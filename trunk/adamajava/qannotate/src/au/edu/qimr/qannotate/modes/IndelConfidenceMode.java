package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.SEMI_COLON_STRING;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_12_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_3_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
import static org.qcmg.common.util.SnpUtils.PASS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.modes.ConfidenceMode.Confidence;
import au.edu.qimr.qannotate.options.IndelConfidenceOptions;

/**
 * @author christix
 * annotate whether indel is high, low or zero confidence
 *
 */
public class IndelConfidenceMode extends AbstractMode{
	private final QLogger logger;
	private final IndelConfidenceOptions options;
	final int MAX_CONTIG_SIZE = 250000000;	
	static final float DEFAULT_NIOC = 0.1f;
	static final int DEFAULT_HOMCNTXTN = 6;
	final BitSet[] mask = new BitSet[24]; 
		
 	//filters 
	public static final String FILTER_REPEAT = "REPEAT"; 
	public static final String DESCRITPION_INFO_CONFIDENCE =  "set to HIGH if the variants passed all filter, "
			+ "nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if "
			+ "coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants";
 	
	public static final String DESCRITPION_FILTER_REPEAT = String.format( "this variants is fallen into the repeat region");
//	public enum Confidence{	HIGH , LOW, ZERO ; }	
	public IndelConfidenceMode(IndelConfidenceOptions options, QLogger logger) throws Exception{	
		this.logger = logger;	
		this.options = options;
		logger.tool("input: " + options.getInputFileName());
        logger.tool("mask File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel()); 
        
//        can't inputRecord, since same position may have multi entry for differnt insertion
//		inputRecord(new File( options.getInputFileName())   );
 
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
	private Confidence getConfidence(VcfRecord vcf){
		String filter = vcf.getFilter();
		if( filter.equals(VcfHeaderUtils.FILTER_PASS) ){
			//check nearby indel  
			String info = vcf.getInfoRecord().getField(IndelUtils.INFO_NIOC);
			float rate = 0;
			try{
				 rate = Float.parseFloat(info);
			}catch(NullPointerException | NumberFormatException  e){
				//do nothing
			}
			
			int lhomo = 0;
			info = vcf.getInfoRecord().getField("HOMCNTXTN");
			if(info != null){
				int pos = info.indexOf(Constants.COMMA_STRING);
				info = info.substring(0,pos);
				try{
					 lhomo = Integer.parseInt(info);
				}catch(NullPointerException | NumberFormatException  e){
					//do nothing
				}
			}
			
			if(rate <= DEFAULT_NIOC && lhomo <= DEFAULT_HOMCNTXTN )
				return Confidence.HIGH;			
			
		}else if(filter.equals(IndelUtils.FILTER_HCOVN) || filter.equals(IndelUtils.FILTER_HCOVT) || 
				filter.equals(FILTER_REPEAT) || filter.contains(Constants.SEMI_COLON + FILTER_REPEAT + Constants.SEMI_COLON)  ||
				filter.startsWith(FILTER_REPEAT + Constants.SEMI_COLON) || filter.endsWith(Constants.SEMI_COLON + FILTER_REPEAT)) { 
			return Confidence.ZERO;
		} 
		
		//default is low
		return Confidence.LOW;
	}
	
 
	private boolean isRepeat(VcfRecord vcf){
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
	
		try (VCFFileReader reader = new VCFFileReader(options.getInputFileName()  ) ;
                VCFFileWriter writer = new VCFFileWriter(new File( options.getOutputFileName() ))  ) {
			    
			//reheader
		    VcfHeader hd = 	reader.getHeader();
		    hd.addFilterLine(FILTER_REPEAT, DESCRITPION_FILTER_REPEAT );       	  
		    hd.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "1", "String", DESCRITPION_INFO_CONFIDENCE);		    
		    hd = reheader(hd, options.getCommandLine(),options.getInputFileName());			    	  
	
		    for(final VcfHeader.Record record: hd)  
		    	writer.addHeader(record.toString());
		
	        for (final VcfRecord vcf : reader) {               	
	        	if( isRepeat(vcf) ){
	        		VcfUtils.updateFilter(vcf, FILTER_REPEAT);
	        		repeatCount ++;
	        	}
	    		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENT, getConfidence(vcf).toString());		
	    		
	    		count++;
	    		writer.add(vcf);
	        }
		}        
		logger.info("outputed VCF record:  " + count + "; no of variants falled fallen into repeat region is " + repeatCount);
					 
	}
 
}	
	
  
	
 
