package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.SEMI_COLON;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_12_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_3_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
import static org.qcmg.common.util.SnpUtils.PASS;

import java.io.File;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.options.ConfidenceOptions;
import au.edu.qimr.qannotate.utils.SampleColumn;

/**
 * @author christix
 *
 */
public class ConfidenceMode extends AbstractMode{
	private final QLogger logger = QLoggerFactory.getLogger(ConfidenceMode.class);
	
	
	public static final String[] CLASS_B_FILTERS= new String[] {PASS, MUTATION_IN_UNFILTERED_NORMAL, LESS_THAN_12_READS_NORMAL, LESS_THAN_3_READS_NORMAL};
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	
	//filters 
//	public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED= LESS_THAN_12_READS_NORMAL + SEMI_COLON_STRING + MUTATION_IN_UNFILTERED_NORMAL;
//	public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED= LESS_THAN_3_READS_NORMAL + SEMI_COLON_STRING + MUTATION_IN_UNFILTERED_NORMAL;
	
	
	public static final String DESCRITPION_INFO_CONFIDENCE = String.format( "set to HIGH if the variants passed all filter, "
			+ "appeared on more than %d novel stars reads and more than %d reads contains variants; "
			+ "Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than %d novel stars reads and more than %d reads contains variants;"
			+ "Otherwise set to Zero if the variants didn't matched one of above conditions.",
			HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, HIGH_CONF_ALT_FREQ_PASSING_SCORE, LOW_CONF_NOVEL_STARTS_PASSING_SCORE, LOW_CONF_ALT_FREQ_PASSING_SCORE);


	public enum Confidence{	HIGH , LOW, ZERO ; }
//	private final String patientId;
	
	private int test_column = -2; //can't be -1 since will "+1"
	private int control_column  = -2;
	
	//for unit testing
	ConfidenceMode(String patient){ 
//		patientId = patient;
	}

	
	public ConfidenceMode(ConfidenceOptions options) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
 		
		inputRecord(new File( options.getInputFileName())   );	
		

		//get control and test sample column; here use the header from inputRecord(...)
		SampleColumn column =SampleColumn.getSampleColumn(options.getTestSample(), options.getControlSample(), this.header );
		test_column = column.getTestSampleColumn();
		control_column = column.getControlSampleColumn();


		//if(options.getpatientid == null)
//		patientId = DonorUtils.getDonorFromFilename(options.getInputFileName());
		addAnnotation();
//		addAnnotation( options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}

	public void setSampleColumn(int test, int control){
		test_column = test;
		control_column = control; 
	}


	//inherited method from super
	void addAnnotation() throws Exception{
		
		int high = 0;
		int low = 0;
		int zero = 0;
		int mergedHigh = 0;
		int mergedLow = 0;
		int mergedZero = 0;
		
		//check high, low nns...
		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
			for(VcfRecord vcf : vcfs){
				
				/*
				 * Check to see if this record is a merged record
				 * If it is, add multiple confidences
				 */
				boolean mergedRec = VcfUtils.isMergedRecord(vcf);
				
				
				
			 	VcfFormatFieldRecord formatField = (vcf.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
			 	
			 	if (mergedRec) {
			 		/*
			 		 * just catering for a 2 way merge for now
			 		 */
			 		String bases = formatField.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
			 		boolean compoundSnp = bases == null;
			 		if (compoundSnp) {
			 			bases = formatField.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP);
			 		}
			 		String [] basesArray = bases.split(Constants.VCF_MERGE_DELIM + "");
			 		if (basesArray.length < 2) {
			 			logger.warn("bases array is less than 2!! for " + vcf.toString());
			 			for (String s : basesArray) {
			 				logger.warn("s: " + s);
			 			}
			 		}
			 		
			 		for (int i = 1 ; i <= 2 ; i++) {
			 			
			 			String suffix = "_" + i;
			 			String thisFilter = VcfUtils.getFiltersEndingInSuffix(vcf, suffix).replace(suffix, "");
			 			int nns = getNNS(formatField, i);
			 			int altFreq =  SnpUtils.getCountFromNucleotideString(basesArray[i-1], vcf.getAlt(), compoundSnp);
			 			
			 			
			 			if ( nns >= HIGH_CONF_NOVEL_STARTS_PASSING_SCORE
								&& altFreq >=  HIGH_CONF_ALT_FREQ_PASSING_SCORE
								&& PASS.equals(thisFilter)) {
				        	
				        		vcf.getInfoRecord().addField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString() + suffix);    	 				 				
				        		mergedHigh++;
				        } else if ( nns >= LOW_CONF_NOVEL_STARTS_PASSING_SCORE
								&& altFreq >= LOW_CONF_ALT_FREQ_PASSING_SCORE 
								&& isClassB(thisFilter) ) {
				        	
				        		vcf.getInfoRecord().addField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString() + suffix);					 
				        		mergedLow++;
				        } else {
				        		vcf.getInfoRecord().addField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString() + suffix);
				        		mergedZero++;
				        }
			 		}
			 	} else {
				 	if ( checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, formatField)
							&& ( VcfUtils.getAltFrequency(formatField, vcf.getAlt()) >=  HIGH_CONF_ALT_FREQ_PASSING_SCORE)
							&& PASS.equals(vcf.getFilter())) {
			        	
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        	 				 				
			        		high++;
			        } else if ( checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, formatField)
							&& ( VcfUtils.getAltFrequency(formatField, vcf.getAlt()) >= LOW_CONF_ALT_FREQ_PASSING_SCORE )
							&& isClassB(vcf.getFilter()) ) {
			        	
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString());					 
			        		low++;
			        } else {
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString());
			        		zero++;
			        }
			 	}
		    }
		}
		
		logger.info("Confidence breakdown, high: " + high + ", low: " + low + ", zero: " + zero + ", mergedHigh: " + mergedHigh + ", mergedLow: " + mergedLow + ", mergedZero: " + mergedZero);
 
		//add header line  set number to 1
		header.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "1", "String", DESCRITPION_INFO_CONFIDENCE);
	}

	/**
	 * 
	 * @param filter
	 * @return true if the filter string match "PASS", "MIUN","SAN3" and "COVN12";
	 */
	public static final boolean isClassB(String filter){
		if (SnpUtils.PASS.equals(filter))
				return true;
		
		if (null == filter) {
			throw new IllegalArgumentException("Null filter string passed to ConfidenceMode.isClassB");
		}
		
		String f = filter;
		//remove MUTATION_IN_UNFILTERED_NORMAL
		int index = filter.indexOf(MUTATION_IN_UNFILTERED_NORMAL);
		if (index > -1) {
			
			if (filter.equals(MUTATION_IN_UNFILTERED_NORMAL)) {
				return true;
			} else if (index == 0) {
				f = filter.substring(MUTATION_IN_UNFILTERED_NORMAL.length() + 1);
			} else {
				f = filter.replace(SEMI_COLON + MUTATION_IN_UNFILTERED_NORMAL, "");
			}
		}
		
		return f.equals(Constants.EMPTY_STRING) ||  LESS_THAN_12_READS_NORMAL.equals(f)  ||   LESS_THAN_3_READS_NORMAL.equals(f);
	}
	
	
//	/**
//	 * @param vcf
//	 * @return
//	 */
//	public static final int getAltFrequency(VcfFormatFieldRecord re, String alt){
////		 final String info =  vcf.getInfo();
////		 final VcfFormatFieldRecord re = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
//		 
//		 return VcfUtils.getAltFrequency(re, alt);	 
//	}
 
	/*
	 * Get the relevant format field, and check the NNS value 
	 */
	/**
	 * 
	 * @param score : integer
	 * @param formatField : vcf formate field string
	 * @return true if novel start value higher than score or not exists
	 */
	 public static final boolean checkNovelStarts(int score, VcfFormatFieldRecord formatField , int input) {
		 int nns = getNNS(formatField, input);
		 if (nns == 0) {
			 /*
			  * Special case whereby if novel starts count is zero, we return true (eg. compound snps)
			  */
			 return true;
		 }
		 return nns >= score;
		 
//		 return true;
//		 String nnsString = formatField.getField(VcfHeaderUtils.FILTER_NOVEL_STARTS);
//		 if (StringUtils.isNullOrEmpty(nnsString) || nnsString.equals(Constants.MISSING_DATA_STRING)) {
//			 /*
//			  * If we don't have a value for NNS (I'm looking at you compound snps), then return true...
//			  */
//			 return true;
//		 } else if (nnsString.contains(Constants.VCF_MERGE_DELIM + "")) {
//			 /*
//			  * in a merged vcf record, there may be 2 values in the NNS field separated  by a delimiter. Pick the first
//			  */
//			 int commaIndex = nnsString.indexOf(Constants.VCF_MERGE_DELIM);
//			 switch (input) {
//			 case 1:  return Integer.parseInt(nnsString.substring(0, commaIndex)) >= score; 
//			 case 2:  return Integer.parseInt(nnsString.substring(commaIndex + 1)) >= score;
//			 default: return false;
//			 }
//			
//		 }
//		 return Integer.parseInt(nnsString) >= score;
	 }
	 public static final boolean checkNovelStarts(int score, VcfFormatFieldRecord formatField ) {
		 return checkNovelStarts(score, formatField, 1);
	 }
	 
	 public static int getNNS(VcfFormatFieldRecord formatField , int input) {
		 String nnsString = formatField.getField(VcfHeaderUtils.FILTER_NOVEL_STARTS);
		 if (StringUtils.isNullOrEmpty(nnsString) || nnsString.equals(Constants.MISSING_DATA_STRING)) {
			 return 0;
		 }
		 if (nnsString.contains(Constants.VCF_MERGE_DELIM + "")) {
			 /*
			  * in a merged vcf record, there may be 2 values in the NNS field separated  by a delimiter. Pick the input value
			  */
			 int commaIndex = nnsString.indexOf(Constants.VCF_MERGE_DELIM);
			 switch (input) {
			 case 1:  return Integer.parseInt(nnsString.substring(0, commaIndex)); 
			 case 2:  return Integer.parseInt(nnsString.substring(commaIndex + 1));
			 default: return 0;
			 }
			
		 }
		 return Integer.parseInt(nnsString);
	 }
	 
	 public static int getNNS(VcfFormatFieldRecord formatField) {
		 return getNNS(formatField, 1);
	 }


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub		
	}  
}	
	
  
	
 
