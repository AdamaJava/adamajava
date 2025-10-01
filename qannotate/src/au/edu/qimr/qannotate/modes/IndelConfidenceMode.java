/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.qcmg.qio.vcf.VcfFileReader;
import org.qcmg.qio.record.RecordWriter;

import au.edu.qimr.qannotate.Options;

/**
 * @author christix
 * annotate whether indel is high, low, or zero confidence
 *
 */
public class IndelConfidenceMode extends AbstractMode{
	private final QLogger logger = QLoggerFactory.getLogger(IndelConfidenceMode.class);
	
	private final String input;
	private final String output;
	private final String commandLine;
	private static final float DEFAULT_NIOC = 0.1f;
	private static final float DEFAULT_SSOI = 0.2f;
	public static final int DEFAULT_HOMN = 6;
	private final Map<String,BitSet> mask = new HashMap<>();

    private int homopolymerCutoff = IndelConfidenceMode.DEFAULT_HOMN;
		
 	//filters 
	private static final String FILTER_REPEAT = "REPEAT"; 
	private static final String DESCRIPTION_INFO_CONFIDENCE =  "set to HIGH if the variants passed all filter, "
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
        options.getHomoplymersCutoff().ifPresent(i -> homopolymerCutoff = i);
		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("mask File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        logger.tool("Homopolymer cutoff (will add to filter if value is greater than or equal to cutoff): " + homopolymerCutoff);
          
		addAnnotation(options.getDatabaseFileName() );				
	}

	/**
     * Loads repeat region data from the specified file and populates a BitSet for masking.
     * Each line in the file is processed to extract chromosome, start, and end positions,
     * which are then used to update the mask data structure.
     *
     * @param dbfile the path to the file containing repeat regions. The file should consist of lines
     *               where each line contains chromosome, start, and end positions of the repeat regions.
     * @throws IOException if an I/O error occurs while reading the file.
     */
	private void loadMask(String dbfile) throws IOException {		
        //load repeat region to bitset
        try(BufferedReader reader = new BufferedReader(new FileReader(dbfile))){
            String line;
            while (( line = reader.readLine()) != null) {
                if ( ! line.startsWith("geno")) {
                String[] array = line.split(" ");
                    //int no = Integer.parseInt(array[0]) - 1;
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
			
			//check homopolymers
			int lhomo = (info.getField(VcfHeaderUtils.INFO_HOM) == null)? 1 :
				StringUtils.string2Number(info.getField(VcfHeaderUtils.INFO_HOM).split(",")[0], Integer.class);
			
			if(nioc <= DEFAULT_NIOC && lhomo <= homopolymerCutoff && ssoi >= DEFAULT_SSOI) return MafConfidence.HIGH;
			
		} else if (filter.equals(IndelUtils.FILTER_HCOVN) || filter.equals(IndelUtils.FILTER_HCOVT) || 
				filter.equals(FILTER_REPEAT) || filter.contains(Constants.SEMI_COLON + FILTER_REPEAT + Constants.SEMI_COLON)  ||
				filter.startsWith(FILTER_REPEAT + Constants.SEMI_COLON) || filter.endsWith(Constants.SEMI_COLON + FILTER_REPEAT)) { 
			return MafConfidence.ZERO;
		} 
		
		//default is low
		return MafConfidence.LOW;
	}
	
 
	/**
     * Determines if the given VCF record falls within a repeat region.
     * It checks the specified chromosome and position range in the associated
     * BitSet mask for repeat regions.
     *
     * @param vcf a {@code VcfRecord} representing the variant call to check for repeat regions.
     *            This includes details such as chromosome, position, and other metadata.
     * @return {@code true} if the variant falls within a repeat region; {@code false} otherwise.
     */
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
	void addAnnotation(String dbfile) throws IOException {
		// TODO Auto-generated method stub
		loadMask( dbfile );	
		
		long count = 0;
		long repeatCount = 0; 
		HashSet<ChrPosition> posCheck = new HashSet<>();
		try (VcfFileReader reader = new VcfFileReader(input) ;
            RecordWriter<VcfRecord> writer = new RecordWriter<>(new File(output ))  ) {
			    
			//reheader
		    VcfHeader hd = 	reader.getVcfHeader();
		    hd.addFilter(FILTER_REPEAT, DESCRIPTION_FILTER_REPEAT );       	  
		    hd.addInfo(VcfHeaderUtils.INFO_CONFIDENCE, "1", "String", DESCRIPTION_INFO_CONFIDENCE);
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
		logger.info(String.format("outputted %d VCF records in %d locations.",  count , posCheck.size()));
		logger.info("number of variants in repeat regions: " + repeatCount);
					 
	}
 
}	
	
  
	
 
