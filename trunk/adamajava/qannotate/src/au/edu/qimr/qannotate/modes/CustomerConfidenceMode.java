package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.Iterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.options.CustomerConfidenceOptions;


/**
 * @author christix
 *
 */
public class CustomerConfidenceMode extends AbstractMode{
	
//	public static final String SOMATIC = "SOMATIC";
//	public static final String NOVEL_STARTS = "NNS";

	String description = null;
	int min_read_counts = 50;
	int variants_rate = 10 ;
	boolean passOnly = true;
	
	public CustomerConfidenceMode(CustomerConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
        
        min_read_counts = options.get_min_read_count();
        variants_rate = options.get_min_mutant_rate();
         passOnly = options.isPassOnly();
        
		inputRecord(new File( options.getInputFileName())   );		
		addAnnotation( options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}
	
	
	/**
	 * add dbsnp version
	 * @throws Exception
	 */
	@Override
	//inherited method from super
	void addAnnotation(String verificationFile) throws Exception{		
	    
		//add header line
		String description = "Set CONF to HIGH once the variants meet conditions: ";
		description += 	(passOnly) ? "passed filter," : "";
		description += "total read counts more than " +  Integer.toString(min_read_counts) + ", more than ";
		description += Integer.toString( variants_rate) + "% reads contains variants";		
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER,0, VcfInfoType.Flag, description, null,null) );
	      
		final Iterator<VcfRecord>  it =  positionRecordMap.values().iterator();
		while( it.hasNext() ){
			final VcfRecord re = it.next();
			
			//remove previous annotaion about CONF
			final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo());
			infoRecord.removeField(VcfHeaderUtils.INFO_CONFIDENT);
			 			
			//only annotate record passed filters
			if(passOnly && !re.getFilter().toUpperCase().contains(VcfHeaderUtils.FILTER_PASS))
				continue;
			  
			final String allel = (re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) ? re.getFormatFields().get(2) :  re.getFormatFields().get(1); 		 
			final int total =  VcfUtils.getAltFrequency(new VcfFormatFieldRecord(re.getFormatFields().get(0) ,  allel), null );
			if( total <  min_read_counts) continue;
			
			final int mutants = Integer.parseInt( infoRecord.getField(VcfHeaderUtils.FORMAT_MUTANT_READS));			  
			if( ((100 * mutants) / total) < variants_rate  ) continue;
							 
			infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, null);
			re.setInfo(infoRecord.toString());			
		}		
	}				
	
	
	 
	
	 
}	
	
  
	
 
