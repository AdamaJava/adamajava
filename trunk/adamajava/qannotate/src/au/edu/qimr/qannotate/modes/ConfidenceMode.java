package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.common.util.*;

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
	
	//filters 
	public static final String PASS = "PASS";
	public static final String LESS_THAN_12_READS_NORMAL = "COVN12";
	public static final String LESS_THAN_3_READS_NORMAL = "SAN3";
	public static final String MUTATION_IN_UNFILTERED_NORMAL = "MIUN";
	public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED= LESS_THAN_12_READS_NORMAL + ";" + MUTATION_IN_UNFILTERED_NORMAL;
	public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED= LESS_THAN_3_READS_NORMAL + ";" + MUTATION_IN_UNFILTERED_NORMAL;

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
		
		//get control and test sample column; here use the header from inputRecord(...)
		retriveSampleColumn(options.getTestSample(), options.getControlSample(), null );


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
	        	 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        
	        else if (checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, vcf) 
					&& ( getAltFrequency(vcf) >=  HIGH_CONF_ALT_FREQ_PASSING_SCORE)
					&& PASS.equals(vcf.getFilter()))  
	        	 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        	 				 				
			else if (checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, vcf) 
					&& ( getAltFrequency(vcf) >= LOW_CONF_ALT_FREQ_PASSING_SCORE )
					&& isClassB(vcf.getFilter()) )
				 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString());					 
			 else
				 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString());		  
	        
	        vcf.setInfo(infoRecord.toString());	        
	    } 	
 
	    
		//add header line  set number to 1
		header.add(new VcfHeaderInfo(VcfHeaderUtils.INFO_CONFIDENT, 
				VcfInfoNumber.NUMBER, 1,  VcfInfoType.String, 
				VcfHeaderUtils.DESCRITPION_INFO_CONFIDENCE, null,null) );
	     
	}

	/**
	 * 
	 * @param filter
	 * @return true if the filter string match "PASS", "MIUN","SAN3" and "COVN12";
	 */
	private boolean isClassB(String filter){
		if (PASS.equals(filter))
				return true;
		
		//remove MUTATION_IN_UNFILTERED_NORMAL
		final String f = filter.replace(MUTATION_IN_UNFILTERED_NORMAL, "").replace(";","").trim();
		if(f.equals(Constants.EMPTY_STRING) ||  LESS_THAN_12_READS_NORMAL.equals(f)  ||   LESS_THAN_3_READS_NORMAL.equals(f))
			 return true;
		
		return false;
	}
	
	
	//require update
	/**
	 * ********error Here, default somatic is the last field, but now user can specify control and tumour, so look at vcf2maf 
	 * @param vcf
	 * @return
	 */
	private int getAltFrequency(VcfRecord vcf){
		 final String info =  vcf.getInfo();
//		 final String allel = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getFormatFields().get(2) :  vcf.getFormatFields().get(1); 		 
		 final VcfFormatFieldRecord re = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
				 
		//		 new VcfFormatFieldRecord(vcf.getFormatFields().get(0) ,  allel);		 
		 
		 return VcfUtils.getAltFrequency(re, vcf.getAlt());	 
	}
 
	 private boolean   checkNovelStarts(int score, VcfRecord vcf ) {
		 
		 //if marked "NNS" on filter, return false
		 String[] filters = vcf.getFilter().split(Constants.SEMI_COLON_STRING);
		 for(int i = 0; i < filters.length; i ++)
			 if(filters[i].equals(VcfHeaderUtils.FILTER_NOVEL_STARTS))
				 return false;
		 
		 
		 return true;
						 
		 	
/*		 //if no "NNS" maybe >=4; maybe not yet applied this filter, so check the counts
		 final VcfFormatFieldRecord re = (vcf.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
		 try{			 
			 if(   Integer.parseInt(  re.getField( VcfHeaderUtils.FORMAT_NOVEL_STARTS  )  ) >= score ) 
				 return true;
		 }catch(final Exception e){
			 //for compound SNP at moment only
			 if(vcf.getFormatFields().get(0).equals(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP)  )
				 return true;
			 return false;
		 } 
		 
		 return false;*/
	 }  
}	
	
  
	
 