package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VCFRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
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
	public static final Pattern pattern = Pattern.compile("[ACGT][0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");

	private final String patientId;
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
	//    VcfInfoNumber.NUMBER.setNumber(1); //set number to 1
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER, 1,  VcfInfoType.String, 
				VcfHeaderUtils.DESCRITPION_INFO_CONFIDENCE, null,null) );
 
	     
	}
	
	 private boolean checkAltFrequency(int score, VCFRecord vcf){
		 
		 String info =  vcf.getInfo();
		 //set to TD if somatic, otherwise set to normal
		 String allel = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getFormatFields().get(0) :  vcf.getFormatFields().get(1); 
		 allel = allel.substring(allel.lastIndexOf(":") + 1, allel.length());
		 
		List<PileupElement> result = new ArrayList<PileupElement>();
		Matcher m = pattern.matcher(allel);
		int count = 0;
		while (m.find()) {
			String pileup = m.group();
			// first char is the base
			char base = pileup.charAt(0);
			
			if(base == vcf.getAlt().charAt(0)){
				count = Integer.parseInt(pileup.substring(1, pileup.indexOf('['))) +
						Integer.parseInt(pileup.substring(pileup.indexOf(',')+1, pileup.indexOf('[', pileup.indexOf(','))));
			 //	 System.out.println("Confidence Mode, Allel: " + count);		
				return (count >= score) ? true:false;
			}
			 
		}
		 
		 
 /*		 List<PileupElement> pileups = PileupElementUtil.createPileupElementsFromString(allel);
		 for (PileupElement pe : pileups){ 
			 //debug
			
			if (pe.getBase() ==   vcf.getAlt().charAt(0) && pe.getTotalCount() >= score) 
				return true;
		 }
*/ 
		 return false;
	 }   
 
	 private boolean   checkNovelStarts(int score, VcfInfoFieldRecord infoRecord ) {
		 try{			 
			 if(   Integer.parseInt(  infoRecord.getfield( VcfHeaderUtils.INFO_NOVEL_STARTS  )  ) >= score ) 
				 return true;
		 }catch(Exception e){
			 return false;
		 }
		 
		 return false;
	 }   
}	
	
  
	
 