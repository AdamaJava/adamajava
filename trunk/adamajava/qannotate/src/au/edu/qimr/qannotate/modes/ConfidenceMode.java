/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.SEMI_COLON;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_12_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_3_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
import static org.qcmg.common.util.SnpUtils.PASS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
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
	
	public static final String DESCRITPION_INFO_CONFIDENCE = String.format( "set to HIGH if the variants passed all filter, "
			+ "appeared on more than %d novel stars reads and more than %d reads contains variants, is adjacent to reference sequence with less than %d homopolymer base; "
			+ "Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than %d novel stars reads and more than %d reads contains variants;"
			+ "Otherwise set to Zero if the variants didn't matched one of above conditions.",
			HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, HIGH_CONF_ALT_FREQ_PASSING_SCORE,IndelConfidenceMode.DEFAULT_HOMN,  LOW_CONF_NOVEL_STARTS_PASSING_SCORE, LOW_CONF_ALT_FREQ_PASSING_SCORE);


	private int test_column = -2; //can't be -1 since will "+1"
	private int control_column  = -2;
	
	private int nnsCount = HIGH_CONF_NOVEL_STARTS_PASSING_SCORE;
	private int mrCount = HIGH_CONF_ALT_FREQ_PASSING_SCORE;
	private List<String> filtersToIgnore = new ArrayList<>();
	private double mrPercentage = 0.0f;
	
	//for unit testing
	ConfidenceMode(String patient){}

	
	public ConfidenceMode( Options options) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
 		
		inputRecord(new File( options.getInputFileName())   );
		
		options.getNNSCount().ifPresent(i -> nnsCount = i.intValue());
		options.getMRCount().ifPresent(i -> mrCount = i.intValue());
		options.getMRPercentage().ifPresent(i -> mrPercentage = i.floatValue());
		filtersToIgnore = options.getFiltersToIgnore();
		logger.tool("Number of Novel Starts filter value: " + nnsCount);
		logger.tool("Number of Mutant Reads filter value: " + mrCount);
		logger.tool("Percentage of Mutant Reads filter value: " + mrPercentage);
		logger.tool("Filters to ignore: " + filtersToIgnore.stream().collect(Collectors.joining(", ")));

		//get control and test sample column; here use the header from inputRecord(...)
		SampleColumn column =SampleColumn.getSampleColumn(options.getTestSample(), options.getControlSample(), this.header );
		test_column = column.getTestSampleColumn();
		control_column = column.getControlSampleColumn();

		//if(options.getpatientid == null)
		addAnnotation();
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}

	public void setSampleColumn(int test, int control){
		test_column = test;
		control_column = control; 
	}


	//inherited method from super
	void addAnnotation() {
		
		int high = 0;
		int low = 0;
		int zero = 0;
		int mergedHigh = 0;
		int mergedLow = 0;
		int mergedZero = 0;
		
		final boolean percentageMode = mrPercentage > 0.0f;
		
		//check high, low nns...
		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
			for(VcfRecord vcf : vcfs){
				
				/*
				 * Check to see if this record is a merged record
				 * If it is, add multiple confidences
				 */
				boolean mergedRec = VcfUtils.isMergedRecord(vcf);
				
				boolean isSomatic = VcfUtils.isRecordSomatic(vcf);
				
			 	VcfFormatFieldRecord formatField =isSomatic ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
		 		VcfInfoFieldRecord info = vcf.getInfoRecord();
		 		int lhomo = (info.getField(VcfHeaderUtils.INFO_HOM) == null)? 1 :
						StringUtils.string2Number(info.getField(VcfHeaderUtils.INFO_HOM).split(",")[0], Integer.class);
		 		
		 		/*
		 		 * update the mrCount value of we are in percentage mode based on the totacl coverage of this position.
		 		 */
		 		if (percentageMode) {
		 			int totalCoverage = Integer.parseInt(formatField.getField(VcfHeaderUtils.FORMAT_READ_DEPTH));
		 			mrCount =  (int)(totalCoverage * mrPercentage);
		 		}

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
			 			
		 			
			 			if ((nns == 0 && compoundSnp ||  nns >= nnsCount)
								&& altFreq >=  mrCount
								&& lhomo < IndelConfidenceMode.DEFAULT_HOMN
								&& (PASS.equals(thisFilter) || filtersToIgnore.contains(thisFilter))) {
				        	
				        		vcf.getInfoRecord().appendField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.HIGH.toString() + suffix);    	 				 				
				        		mergedHigh++;
				        } else if ( (nns == 0 && compoundSnp ||  nns >= LOW_CONF_NOVEL_STARTS_PASSING_SCORE)
								&& altFreq >= LOW_CONF_ALT_FREQ_PASSING_SCORE 
								&& isClassB(thisFilter) ) {
				        	
				        		vcf.getInfoRecord().appendField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.LOW.toString() + suffix);					 
				        		mergedLow++;
				        } else {
				        		vcf.getInfoRecord().appendField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.ZERO.toString() + suffix);
				        		mergedZero++;
				        }
		 			}
			 	} else {
			 		
				 	if ( checkNovelStarts(nnsCount, formatField)
							&& ( VcfUtils.getAltFrequency(formatField, vcf.getAlt()) >=  mrCount)
							&& lhomo < IndelConfidenceMode.DEFAULT_HOMN
							&& PASS.equals(vcf.getFilter())) {
			        	
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.HIGH.toString());		        	 				 				
			        		high++;
			        } else if ( checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, formatField)
							&& ( VcfUtils.getAltFrequency(formatField, vcf.getAlt()) >= LOW_CONF_ALT_FREQ_PASSING_SCORE )
							&& isClassB(vcf.getFilter()) ) {
			        	
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.LOW.toString());					 
			        		low++;
			        } else {
			        		vcf.getInfoRecord().setField(VcfHeaderUtils.INFO_CONFIDENCE, MafConfidence.ZERO.toString());
			        		zero++;
			        }
			 	}
		    }
		}
		
		logger.info("Confidence breakdown, high: " + high + ", low: " + low + ", zero: " + zero + ", mergedHigh: " + mergedHigh + ", mergedLow: " + mergedLow + ", mergedZero: " + mergedZero);
 
		//add header line  set number to 1
		if (null != header ) {
			header.addInfoLine(VcfHeaderUtils.INFO_CONFIDENCE, "1", "String", DESCRITPION_INFO_CONFIDENCE);
		}
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
	
