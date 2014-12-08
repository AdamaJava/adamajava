package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.maf.util.MafUtils;

import au.edu.qimr.qannotate.options.ConfidenceOptions;

/**
 * @author christix
 *
 */
public class ConfidenceMode extends AbstractMode{
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	public static final String SOMATIC = "SOMATIC";
	public static final String NOVEL_STARTS = "NNS";

	public enum Confidence{	HIGH , LOW, ZERO ; }
	private final String patientId;
	
	//for unit testing
	ConfidenceMode(String patient){ patientId = patient;}
	
	
	public ConfidenceMode(ConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("verified File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
 		
		inputRecord(new File( options.getInputFileName())   );		
		//if(options.getpatientid == null)
		patientId = DonorUtils.getDonorFromFilename(options.getInputFileName());		
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

		
		//load verified file
		final Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedDataAll = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>();
		MafUtils.getVerifiedData(verificationFile, patientId,verifiedDataAll );
		final Map<ChrPosition, TorrentVerificationStatus> VerifiedData = verifiedDataAll.get(patientId);		
		
		//check high, low nns...
		final Iterator<  ChrPosition > it = positionRecordMap.keySet().iterator();
	    while (it.hasNext()) {
	    	final ChrPosition pos = it.next();
	    	final VcfRecord vcf = positionRecordMap.get(pos);
	    	final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(vcf.getInfo());	
	    		    	
	        if (VerifiedData != null && VerifiedData.get(pos) != null && VerifiedData.get(pos).equals( TorrentVerificationStatus.YES))  
	        	 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        
	        else if (checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, infoRecord) 
					&& ( VcfUtils.getAltFrequency(vcf) >=  HIGH_CONF_ALT_FREQ_PASSING_SCORE)
					&& SnpUtils.isClassA(vcf.getFilter()))  
	        	 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        	 				 				
			else if (checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, infoRecord) 
					&& ( VcfUtils.getAltFrequency(vcf) >= LOW_CONF_ALT_FREQ_PASSING_SCORE )
					&& SnpUtils.isClassAorB( vcf.getFilter() )) 
				 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString());					 
			 else
				 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString());		        
	        vcf.setInfo(infoRecord.toString());
	    } 	
 
	    
		//add header line
	//    VcfInfoNumber.NUMBER.setNumber(1); //set number to 1
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER, 1,  VcfInfoType.String, 
				VcfHeaderUtils.DESCRITPION_INFO_CONFIDENCE, null,null) );
 
	     
	}
	
 
	 private boolean   checkNovelStarts(int score, VcfInfoFieldRecord infoRecord ) {
		 try{			 
			 if(   Integer.parseInt(  infoRecord.getfield( VcfHeaderUtils.INFO_NOVEL_STARTS  )  ) >= score ) 
				 return true;
		 }catch(final Exception e){
			 return false;
		 }
		 
		 return false;
	 }   
}	
	
  
	
 