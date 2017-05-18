/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Options;

/**
 * @author christix
 * annotate whether indel is high, low or zero confidence
 *
 */
public class IndelConfidenceMode extends AbstractMode{
	private final QLogger logger = QLoggerFactory.getLogger(IndelConfidenceMode.class);
	
	private final String input;
	private final String output;
	private final String commandLine;
	private static final float DEFAULT_NIOC = 0.1f;
	private static final float DEFAULT_SSOI = 0.2f;
	static final int DEFAULT_HOMN = 6;
	private final Map<String,BitSet> mask = new HashMap<>();
		
 	//filters 
	private static final String FILTER_REPEAT = "REPEAT"; 
	private static final String DESCRITPION_INFO_CONFIDENCE =  "set to HIGH if the variants passed all filter, "
			+ "nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if "
			+ "coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants";
 	
	public static final String DESCRIPTION_FILTER_REPEAT = "this variants is fallen into the repeat region";
	
	@Deprecated
	//unit test only
	IndelConfidenceMode(){
 		this.input = null;
		this.output = null;
		commandLine = null;
	}
	
	public IndelConfidenceMode(Options options) throws Exception{
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
	 * load repeat region into RAM 
	 * @param dbfile
	 * @throws Exception
	 */
	private void loadMask(String dbfile) throws Exception {		
        //load repeat region to bitset
        try(BufferedReader reader = new BufferedReader(new FileReader(dbfile))){
            String line;
            while (( line = reader.readLine()) != null) {
            		if ( ! line.startsWith("geno")) { //head line
            			String[] array = line.split(" ");
	                	String chr = IndelUtils.getFullChromosome(array[0]);
	                	
	                	int start = Integer.parseInt(array[1]);
	                	int end = Integer.parseInt(array[2]);
	                	
	                	mask.computeIfAbsent(chr, (v) -> new BitSet()).set(start,end);
            		}
            }
		}        
	}
		
	/*
	 *check the confidence level
	 */
	MafConfidence getConfidence(VcfRecord vcf){
		String filter = vcf.getFilter();
		VcfInfoFieldRecord info = vcf.getInfoRecord();
		if ( filter.equals(VcfHeaderUtils.FILTER_PASS)  && info!= null) {//|| filter.contains(IndelUtils.FILTER_HOM)){
			
			//check nearby indel  
			float nioc = StringUtils.string2Number(info.getField(IndelUtils.INFO_NIOC), Float.class);			
			
			//check strong supporting for germline
			float ssoi = (info.getField(VcfHeaderUtils.INFO_SOMATIC) != null) ?  
					1 : StringUtils.string2Number(vcf.getInfoRecord().getField(IndelUtils.INFO_SSOI), Float.class);
			
			//check homoplymers			
			int lhomo = (info.getField(VcfHeaderUtils.INFO_HOM) == null)? 1 :
				StringUtils.string2Number(info.getField(VcfHeaderUtils.INFO_HOM).split(",")[0], Integer.class);
			
			if(nioc <= DEFAULT_NIOC && lhomo <= DEFAULT_HOMN && ssoi >= DEFAULT_SSOI) return MafConfidence.HIGH;		
			
		} else if (filter.equals(IndelUtils.FILTER_HCOVN) || filter.equals(IndelUtils.FILTER_HCOVT) || 
				filter.equals(FILTER_REPEAT) || filter.contains(Constants.SEMI_COLON + FILTER_REPEAT + Constants.SEMI_COLON)  ||
				filter.startsWith(FILTER_REPEAT + Constants.SEMI_COLON) || filter.endsWith(Constants.SEMI_COLON + FILTER_REPEAT)) { 
			return MafConfidence.ZERO;
		} 
		
		//default is low
		return MafConfidence.LOW;
	}
	
 
	private boolean isRepeat(VcfRecord vcf){
        
		String chr = IndelUtils.getFullChromosome(vcf.getChromosome()); 
		BitSet chrMask = mask.get(chr);
		if (null == chrMask) {
			return false;
		}
		
		for (int i = vcf.getPosition(); i <= vcf.getChrPosition().getEndPosition(); i ++) {
			if (chrMask.get(i)) {
				return true; 
			}
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
		    hd.addFilter(FILTER_REPEAT, DESCRIPTION_FILTER_REPEAT );       	  
		    hd.addInfo(VcfHeaderUtils.INFO_CONFIDENCE, "1", "String", DESCRITPION_INFO_CONFIDENCE);		    
		    hd = reheader(hd, commandLine ,input);			    	  
	
		    for(final VcfHeaderRecord record: hd)  
		    	writer.addHeader(record.toString());
		
	        for (final VcfRecord vcf : reader) {
		        	if( isRepeat(vcf) ){
		        		VcfUtils.updateFilter(vcf, FILTER_REPEAT);
		        		repeatCount ++;
		        	}
		    		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, getConfidence(vcf).toString());		
		    		
		    		count++;
		    		posCheck.add(vcf.getChrPosition());
		    		writer.add(vcf);
	        }
		}  
		logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  count , posCheck.size()));
		logger.info("number of variants fallen into repeat region is " + repeatCount);
					 
	}
 
}	
	
  
	
 
