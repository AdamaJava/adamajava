package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qcmg.maf.util.MafUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.common.vcf.header.VcfHeaderRecord;

import au.edu.qimr.qannotate.options.ConfidenceOptions;


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
	
	public CustomerConfidenceMode(ConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
        
       // min_read_counts = options.getMinReadCount();
       // variants_rate = options.getVariantsRate();
        // passOnly = options.getPassOnly();
        
		inputRecord(new File( options.getInputFileName())   );		
		addAnnotation( options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}
	/**
	 * add dbsnp version
	 * @throws Exception
	 */
	//inherited method from super
	void addAnnotation(String verificationFile) throws Exception{		
	    
		//add header line
		String description = (passOnly) ? "": "passed filter,";
		description += "total read counts more than " +  Integer.toString(min_read_counts) + ", more than ";
		description += Integer.toString( variants_rate) + "% reads contains variants";		
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER,0, VcfInfoType.Flag,
				description, null,null) );
	     
 
		Iterator<VCFRecord>  it =  positionRecordMap.values().iterator();
		while( it.hasNext() ){
			VCFRecord re = it.next();
			
			//remove previous annotaion about CONF
			VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo());
			infoRecord.removefield(VcfHeaderUtils.INFO_CONFIDENT);
 			
			//only annotate record passed filters
			if(passOnly && !re.getFilter().toLowerCase().contains(VcfHeaderUtils.FILTER_PASS))
				continue;
			
			int total = getReadCount(re);			
			if( total <  min_read_counts) continue;
			
			int mutants = Integer.parseInt( infoRecord.getfield(VcfHeaderUtils.INFO_MUTANT_READS));			  
			if( (100 * mutants) / total < variants_rate  ) continue;
							 
			infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, null);
			
		}
		
	}				

 
	
	
	private int getReadCount(VCFRecord vcf){
		
		 String info =  vcf.getInfo();
		 
		 //set to TD if somatic, otherwise set to normal
		 String allel = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getFormatFields().get(0) :  vcf.getFormatFields().get(1);
			 
		 List<PileupElement> pileups = PileupElementUtil.createPileupElementsFromString(allel);
		 int total = 0;
		 for (PileupElement pe : pileups) 
			total += pe.getTotalCount(); 
	 	
		return total;
	}
	
	 
}	
	
  
	
 