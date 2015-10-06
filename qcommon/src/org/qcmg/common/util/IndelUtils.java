package org.qcmg.common.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IndelUtils {
	public enum SVTYPE {SNP,MNP,INS,DEL,CTX,UNKOWN }		
	
	//qbasepileup indel vcf header info column ID
	public static final String INFO_END = "END"; 
	public static final String DESCRITPION_INFO_END = "End position of the variant described in this record"; 
	
	public static final String INFO_SVTYPE = "SVTYPE";
	public static final String DESCRITPION_INFO_SVTYPE = "Type of structural variant";

	public static final String INFO_SOMATIC = "SOMATIC";
	public static final String DESCRITPION_INFO_SOMATIC = "set to somatic unless there are more than three novel starts on normal BAM;"
			+ " or more than 10% imformative reads are supporting reads; or homopolymeric sequence exists on either side with nearby indels.";
		
	public static final String INFO_COVN12 ="COVN12";
	public static final String DESCRITPION_INFO_COVN12 = "For somatic calls: less than 12 reads coverage in normal BAM";
	
	public static final String INFO_COVN8 = "COVN8";
	public static final String DESCRITPION_INFO_COVN8 = "For germline calls: less than 8 reads coverage in normal";	
	
	public static final String INFO_COVT = "COVT";
	public static final String DESCRITPION_INFO_COVT = "For germline calls: less than 8 reads coverage in tumour";
	
	public static final String INFO_HCOVN = "HCOVN";
	public static final String DESCRITPION_INFO_HCOVN = "more than 1000 reads in normal BAM";
	
	public static final String INFO_HCOVT = "HCOVT";
	public static final String DESCRITPION_INFO_HCOVT = "more than 1000 reads in tumour BAM";
	
	public static final String INFO_MIN = "MIN";
	public static final String DESCRITPION_INFO_MIN = "For somatic calls: mutation also found in pileup of normal BAM";
	
	public static final String INFO_NNS = "NNS";
	public static final String DESCRITPION_INFO_NNS = "For somatic calls: less than 4 novel starts not considering read pair in tumour BAM";
	
	public static final String INFO_TPART = "TPART";
	public static final String DESCRITPION_INFO_TPART = "The number in the tumour partials column is >=3 and is >10% of the total reads at that position";

	public static final String INFO_NPART = "NPART";
	public static final String DESCRITPION_INFO_NPART = "The number in the normal partials column is >=3 and is >5% of the total reads at that position";

	public static final String INFO_TBIAS = "TBIAS";
	public static final String DESCRITPION_INFO_TBIAS = "For somatic calls: the supporting tumour reads value is >=3 and the count on one strand is =0 or >0 "
			+ "and is either <10% of supporting reads or >90% of supporting reads";

	public static final String INFO_NBIAS = "NBIAS";
	public static final String DESCRITPION_INFO_NBIAS = "For germline calls: the supporting normal reads value is >=3 and the count on one strand is =0 or >0 "
			+ "and is either <5% of supporting reads or >95% of supporting reads";

	public static final String INFO_HOMADJ = "HOMADJ";
	public static final String DESCRITPION_INFO_HOMADJ = "In tumour BAM, indel is adjacent to a homopolymeric sequence, but is not contiguous with it and the nearest,"
			+ " longest sequence is n bases long. The value format is <longest proximal homopolymer length>,< sequence bracketing indel>";

	public static final String INFO_HOMCON = "HOMCON";
	public static final String DESCRITPION_INFO_HOMCON = "indel is contiguous with a homopolymeric sequence and the nearest, longest sequence is n bases long."
			+ "The value format is <longest proximal homopolymer length>,< sequence bracketing indel>";

 	public static final String INFO_HOMEMB = "HOMEMB";
	public static final String DESCRITPION_INFO_HOMEMB = "indel is embedded in a homopolymeric sequence and the nearest, longest sequence is n bases long."
			+ "The value format is <longest proximal homopolymer length>,< sequence bracketing indel>";

	public static final Pattern DOUBLE_DIGIT_PATTERN = Pattern.compile("\\d{1,2}");
	public static final int MAX_INDEL_LENGTH = 200;

	/**
	 * 
	 * @param ref: reference base from vcf record 4th column;
	 * @param alt: single alleles base from vcf record 5th column
	 * @return variant type, wether it is SNP, MNP, INSERTION, DELETION or TRUNSLOCATION
	 */
	public static SVTYPE getVariantType(String ref, String alt){
		 if(alt.contains(","))
			 return SVTYPE.UNKOWN;		
		 else if(ref.length() == 1 &&  alt.length() == 1)
			 return SVTYPE.SNP;	
		 else if(alt.length() > MAX_INDEL_LENGTH)
			 return SVTYPE.CTX;
		 else if(ref.length() == alt.length()  )
			 return SVTYPE.MNP;		 
		 else if(alt.length() > ref.length() && ref.length() == 1)
			 return  SVTYPE.INS;		 
		 else if(alt.length() < ref.length() && alt.length() == 1)
			 return  SVTYPE.DEL;
		 		
		return SVTYPE.CTX;
	}	
	
	public static String getFullChromosome(String ref) {
		
		// if ref starts with chr or GL, just return it
		if (ref.startsWith("chr") || ref.startsWith("GL")) {
			if (ref.equals("chrM")) {
				return "chrMT";
			}
			return ref;
		}
		
		
		if (ref.equals("M")) {
			return "chrMT";
		}
		
		if (addChromosomeReference(ref)) {
			return "chr" + ref;
		} else {
			return ref;
		}
	}
	
	public static boolean addChromosomeReference(String ref) {
		
		if (ref.startsWith("chr") || ref.startsWith("GL")) {
			return false;
		}
		
		if (ref.equals("X") || ref.equals("Y") || ref.equals("M") || ref.equals("MT")) {
			return true;
		}
		
		Matcher matcher = DOUBLE_DIGIT_PATTERN.matcher(ref);
		if (matcher.matches()) {		    	
			if (Integer.parseInt(ref) < 23) {
				return true;
			}
		}
		return false;
	}
	

	

}
