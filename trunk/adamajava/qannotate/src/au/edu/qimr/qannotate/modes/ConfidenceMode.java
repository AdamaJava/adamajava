package au.edu.qimr.qannotate.modes;

import java.io.File;
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
import org.qcmg.common.vcf.VcfHeaderUtils;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
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
public class ConfidenceMode extends AbstractMode{
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	public static final String SOMATIC = "SOMATIC";
	public static final String NOVEL_STARTS = "NNS";

	public enum Confidence{	HIGH , LOW, ZERO ; }

	private final String patientId;
	
	public ConfidenceMode(ConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("verified File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
 		
		inputRecord(new File( options.getInputFileName())   );		
		patientId = options.getpatientid();
		
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
		
		//get patient id from command line or vcf file header
		String id = patientId;
		if(id == null){ 
			VcfHeaderRecord re = header.get(MetaType.META, VcfHeaderUtils.PATIENT_ID);
			 id = (re == null)?  null : re.getDescription();
		}
 		 
		
		//load verified file	 
		Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedDataAll = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>();
		MafUtils.getVerifiedData(verificationFile, patientId,verifiedDataAll );
		Map<ChrPosition, TorrentVerificationStatus> VerifiedData = verifiedDataAll.get(patientId);		
		

		//check high, low nns...
		Iterator<  ChrPosition > it = positionRecordMap.keySet().iterator();
	    while (it.hasNext()) {
	    	ChrPosition pos = it.next();
	    	VCFRecord vcf = positionRecordMap.get(pos);
	    	VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(vcf.getInfo());	    	
	    	
	        if (VerifiedData != null && VerifiedData.get(pos).equals( TorrentVerificationStatus.YES))  	             	
	        	 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        
	        else if (checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, infoRecord) 
					&& checkAltFrequency(HIGH_CONF_ALT_FREQ_PASSING_SCORE, vcf)
					&& SnpUtils.isClassA(vcf.getFilter()))  
	        	 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        	 				 				
			else if (checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, infoRecord) 
					&& checkAltFrequency(LOW_CONF_ALT_FREQ_PASSING_SCORE, vcf)
					&& SnpUtils.isClassAorB( vcf.getFilter() )) 
				 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString());					 
			 else
				 infoRecord.setfield(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString());		        
	        vcf.setInfo(infoRecord.toString());
	    } 	
	    
		//add header line
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER, VcfInfoType.String, 
				VcfHeaderUtils.DESCRITPION_INFO_CONFIDENCE, null,null) );
	     
	}
	
	 private boolean   checkAltFrequency(int score, VCFRecord vcf){
		 
		 String info =  vcf.getInfo();
		 String allel = vcf.getExtraFields().get(0); //init set to normal
		 if(info.contains("SOMATIC")){
			 //get td
			 allel = vcf.getExtraFields().get(1);
		 }
		 
		 List<PileupElement> pileups = PileupElementUtil.createPileupElementsFromString(allel);
			for (PileupElement pe : pileups) {
				if (pe.getBase() ==   vcf.getAlt().charAt(0) && pe.getTotalCount() >= score) return true;
			}
 
		 return false;
	 }   
 
	 private boolean   checkNovelStarts(int score, VcfInfoFieldRecord infoRecord ) {
		 try{
			 if(   Integer.parseInt(  infoRecord.getfield( SnpUtils.NOVEL_STARTS )  ) >= score ) 
				 return true;
		 }catch(Exception e){
			 return false;
		 }		 
		 return false;
	 }   
}	
	
  
	
 