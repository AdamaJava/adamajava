package org.qcmg.common.util;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;



public class IndelUtils {
	public enum SVTYPE {SNP,DNP,TNP, ONP,INS,DEL,CTX,UNKOWN }		
	
	//qbasepileup indel vcf header info column ID
	public static final String INFO_END = "END"; 
	public static final String DESCRITPION_INFO_END = "End position of the variant described in this record"; 
	
	public static final String INFO_SVTYPE = "SVTYPE";
	public static final String DESCRITPION_INFO_SVTYPE = "Type of structural variant";

	public static final String INFO_SOMATIC = "SOMATIC";
	public static final String DESCRITPION_INFO_SOMATIC = "set to somatic unless there are more than three novel starts on normal BAM;"
			+ " or more than 10% imformative reads are supporting reads; or homopolymeric sequence exists on either side with nearby indels.";
		
	public static final String FILTER_COVN12 ="COVN12";
	public static final String DESCRITPION_FILTER_COVN12 = "For somatic calls: less than 12 reads coverage in normal BAM";
	
	public static final String FILTER_COVN8 = "COVN8";
	public static final String DESCRITPION_FILTER_COVN8 = "For germline calls: less than 8 reads coverage in normal";	
	
	public static final String FILTER_COVT = "COVT";
	public static final String DESCRITPION_FILTER_COVT = "For germline calls: less than 8 reads coverage in tumour";
	
	public static final String FILTER_HCOVN = "HCOVN";
	public static final String DESCRITPION_FILTER_HCOVN = "more than 1000 reads in normal BAM";
	
	public static final String FILTER_HCOVT = "HCOVT";
	public static final String DESCRITPION_FILTER_HCOVT = "more than 1000 reads in tumour BAM";
	
	public static final String FILTER_MIN = "MIN";
	public static final String DESCRITPION_FILTER_MIN = "For somatic calls: mutation also found in pileup of normal BAM";
	
	public static final String FILTER_NNS = "NNS";
	public static final String DESCRITPION_FILTER_NNS = "For somatic calls: less than 4 novel starts not considering read pair in tumour BAM";
	
	public static final String FILTER_TPART = "TPART";
	public static final String DESCRITPION_FILTER_TPART = "The number in the tumour partials column is >=3 and is >10% of the total reads at that position";

	public static final String FILTER_NPART = "NPART";
	public static final String DESCRITPION_FILTER_NPART = "The number in the normal partials column is >=3 and is >5% of the total reads at that position";

	public static final String FILTER_TBIAS = "TBIAS";
	public static final String DESCRITPION_FILTER_TBIAS = "For somatic calls: the supporting tumour reads value is >=3 and the count on one strand is =0 or >0 "
			+ "and is either <10% of supporting reads or >90% of supporting reads";

	public static final String FILTER_NBIAS = "NBIAS";
	public static final String DESCRITPION_FILTER_NBIAS = "For germline calls: the supporting normal reads value is >=3 and the count on one strand is =0 or >0 "
			+ "and is either <5% of supporting reads or >95% of supporting reads";
	
	public static final String INFO_HOMTXT = "HOMTXT";
	public static final String DESCRITPION_INFO_HOMTXT = "If A indel is adjacent to a homopolymeric sequence,  the nearby reference sequence within a window is reported";
	public static final String FILTER_HOM = "HOM";
	public static final String DESCRITPION_FILTER_HOM = "a digit number is attached on this FILTER id, eg. HOM24 means the nearby homopolymers sequence is 24 base long";
	
	public static final String INFO_NIOC = "NIOC";
	public static final String DESCRITPION_INFO_NIOC = " counts of nearby indels compare with total coverage";	
	
	public static final String INFO_HOMCNTXT = "HOMCNTXT";
	public static final String DESCRITPION_INFO_HOMCNTXT = "nearby refernce sequence. if it is homopolymeric, the maximum repeated based counts will be added infront of sequence ";
	
	public static final String FORMAT_ACINDEL = "ACINDEL";
	public static final String DESCRITPION_FORMAT_ACINDEL = "counts of indels, follow formart:novelStarts,TotalCoverage,InformativeReadCount," 
			+"suportReadCount[forwardsuportReadCount,backwardsuportReadCount],particalReadCount,NearbyIndelCount,NearybySoftclipCount";

	public static final int MAX_INDEL_LENGTH = 200;

	/**
	 * 
	 * @param ref: reference base from vcf record 4th column;
	 * @param alt: single alleles base from vcf record 5th column
	 * @return variant type, whether it is SNP, MNP, INSERTION, DELETION or TRANSLOCATION
	 */
	public static SVTYPE getVariantType(String ref, String alt){
		if (StringUtils.isNullOrEmpty(alt) || StringUtils.isNullOrEmpty(ref)) {
			throw new IllegalArgumentException("Null or empty alt and/or ref passed to getVariantType. alt: " + alt + ", ref: " + ref);
		}
		 if(alt.contains(",")) {
			 return SVTYPE.UNKOWN;	
		 } else if(ref.length() == alt.length() ){
			 switch (ref.length()) {
			 case 1: return SVTYPE.SNP;	
			 case 2: return SVTYPE.DNP;	
			 case 3: return SVTYPE.TNP;
			 default: return SVTYPE.ONP;
			 }
		 } else if ( alt.length() <  MAX_INDEL_LENGTH &&  alt.length() > ref.length() && ref.length() == 1) {
			 return  SVTYPE.INS;		 
		 } else if (ref.length() <  MAX_INDEL_LENGTH && alt.length() < ref.length() && alt.length() == 1) {
			 return  SVTYPE.DEL;
		 }
		return SVTYPE.UNKOWN;	
	}	
	
	public static String getFullChromosome(String ref) {
		
		/*
		 * Deal with MT special case first
		 */
		if (ref.equals("chrM") || ref.equals("M") || ref.equals("MT")) {
			return "chrMT";
		}
		
		/*
		 *  if ref starts with chr or GL, just return it
		 */
		if (ref.startsWith("chr") || ref.startsWith("GL")) {
			return ref;
		}
		
		if (ref.equals("X") || ref.equals("Y")) {
			return "chr" + ref;
		}
		
		/*
		 * If ref is an integer less than 23, slap "chr" in front of it
		 */
		try {
			if (Integer.parseInt(ref) < 23) {
				return "chr" + ref;
			}
		} catch (NumberFormatException nfe) {
			// don't do anything here - will return the original reference
		}
		
		return ref;
	}

	/**
	 * 
	 * @param re: VcfFormatFieldRecord
	 * @param ref: reference base from vcf record column 5
	 * @param alt
	 * @return genotyp string. eg. A/T; return null if GT field is not exsit or empty
	 */
	public static String getGenotypeDetails(VcfFormatFieldRecord re, String ref, String alt){
		//if "GT" field is not exist, do nothing
		String sgt = re.getField(VcfHeaderUtils.FORMAT_GENOTYPE) ;
		String gd = null;
		if( ! StringUtils.isNullOrEmpty(sgt) && !sgt.equals(Constants.MISSING_DATA_STRING)){		
			boolean isbar = sgt.contains("\\|");
			String[] gts = isbar? sgt.split("\\|") : sgt.split("\\/"); 
			String[] sgd = new String[gts.length];
	
			String[] alts = alt.split(",");
			//only allow three multi-allele, otherwise too confuse
			for(int j = 0; j < gts.length; j ++) 
				switch (gts[j].trim()){
					case "0": sgd[j] = ref; 
					break;
					case "1": sgd[j] = (alts.length >= 1) ? alts[0] : Constants.MISSING_DATA_STRING; 
					break;
					case "2": sgd[j] = (alts.length >= 2) ? alts[1] : Constants.MISSING_DATA_STRING; 
					break;
					case "3": sgd[j] = (alts.length >= 3) ? alts[2] : Constants.MISSING_DATA_STRING; 
					break;
					default:
						sgd[j] = Constants.MISSING_DATA_STRING; 
				}				
			gd = isbar? String.join("|", sgd) : String.join("/", sgd);
		}		
		return gd;
	}

	
}
