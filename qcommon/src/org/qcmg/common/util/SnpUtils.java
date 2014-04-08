/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfUtils;

public class SnpUtils {
	
	//NORMAL
	public static final String LESS_THAN_12_READS_NORMAL = "COVN12";
	public static final String LESS_THAN_8_READS_NORMAL = "COVN8";
	public static final String LESS_THAN_3_READS_NORMAL = "SAN3";
	
	//TUMOUR
	public static final String LESS_THAN_8_READS_TUMOUR = "COVT";
	public static final String LESS_THAN_3_READS_TUMOUR = "SAT3";
	
	//COMPOUND_SNP
	public static final String COMPOUND_SNP = "COMPOUNDMUTATION";
	
	
	//MUTATION
	public static final String MUTATION_IN_UNFILTERED_NORMAL = "MIUN";
	public static final String MUTATION_IN_NORMAL = "MIN";
	public static final String MUTATION_GERMLINE_IN_ANOTHER_PATIENT = "GERM";	//mutation is a germline variant in another patient
	
	public static final String STRAND_BIAS = "SBIAS";	// mutation only found on one strand
	public static final String ALLELIC_FRACTION = "AF";	// allelic fraction of mutation is less than appended number eg. AF20
	public static final String END_OF_READ = "5BP";	// allelic fraction of mutation is less than appended number eg. AF20
	
	// a couple of extra class B's
	public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED= LESS_THAN_12_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;
	public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED= LESS_THAN_3_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;
	
	//EXTRAs
	public static final String NOVEL_STARTS = "NNS";
	public static final String MUTANT_READS = "MR";
	public static final String PASS = VcfUtils.FILTER_PASS;
	public static final String MUTATION_EQUALS_REF = "MER";	// mutation same as reference
	
	
	public static final String INDEL_SATELLITE = "Satellite";	
//	public static final String INDEL_SATELLITE = "Satellite/telo::";	
	public static final String INDEL_SIMPLE_REPEAT = "Simple_repeat::";	
	public static final String INDEL_LOW_COMPLEXITY = "Low_complexity::";	
	public static final String INDEL_HOM_ADJ = "HOMADJ_";	
	public static final String INDEL_HOM_CON = "HOMCON_";	
	public static final String INDEL_HOM_EMB = "HOMEMB_";	
	public static final String INDEL_HCOVT = "HCOVT";	
	public static final String INDEL_HCOVN = "HCOVN";	
	public static final String INDEL_STRAND_BIAS = "TBIAS";	
	public static final String INDEL_NPART = "NPART";	
	
	/**
	 * Utility method to determine if an annotation contains text that classifies a snp as class A or B
	 * Class B is currently defined as one of the following:
	 * 
	 * <code>LESS_THAN_12_READS_NORMAL, LESS_THAN_3_READS_NORMAL, MUTATION_IN_UNFILTERED_NORMAL</code>
	 * 
	 * @param annotation
	 * @return
	 * @see #isClassA(String)
	 * @see #LESS_THAN_12_READS_NORMAL
	 * @see #LESS_THAN_3_READS_NORMAL
	 * @see #MUTATION_IN_UNFILTERED_NORMAL
	 */
	public static final boolean isClassAorB(String annotation) {
		annotation = removeCompoundSnpAnnotationFromString(annotation);
		return isClassA(annotation) 
			|| LESS_THAN_12_READS_NORMAL.equals(annotation)
			|| LESS_THAN_3_READS_NORMAL.equals(annotation)
			|| MUTATION_IN_UNFILTERED_NORMAL.equals(annotation)
			|| LESS_THAN_12_READS_NORMAL_AND_UNFILTERED.equals(annotation)
			|| LESS_THAN_3_READS_NORMAL_AND_UNFILTERED.equals(annotation);
	}
	
	public static final boolean isClassAorBIndel(String annotation) throws IllegalArgumentException {
		if (StringUtils.isNullOrEmpty(annotation)) throw new IllegalArgumentException("null or empty annotation passed to SnpUtils.isClassAorBIndel");
		
		return 
				isAnnotationAlone(annotation, INDEL_SATELLITE)
				|| isAnnotationAlone(annotation, INDEL_SIMPLE_REPEAT)
				|| isAnnotationAlone(annotation, INDEL_LOW_COMPLEXITY)
				|| isAnnotationAlone(annotation, MUTATION_IN_NORMAL)
				|| isAnnotationAlone(annotation, INDEL_HCOVN)
				|| isAnnotationAlone(annotation, INDEL_HCOVT)
				|| isAnnotationAlone(annotation, LESS_THAN_12_READS_NORMAL)
				|| isAnnotationAlone(annotation, LESS_THAN_8_READS_NORMAL)
				|| isAnnotationAlone(annotation, INDEL_STRAND_BIAS)
				|| isAnnotationAlone(annotation, INDEL_NPART);
//				|| isAnnotationAlone(annotation, LESS_THAN_3_READS_NORMAL);
	}
//	public static final boolean isClassAorBIndel(String annotation) throws IllegalArgumentException {
//		if (StringUtils.isNullOrEmpty(annotation)) throw new IllegalArgumentException("null or empty annotation passed to SnpUtils.isClassAorBIndel");
//		
//		return isClassAIndel(annotation) 
//				|| (annotation.contains(INDEL_SATELLITE) && isAnnotationAlone(annotation, INDEL_SATELLITE))
//				|| (annotation.contains(INDEL_SIMPLE_REPEAT) && isAnnotationAlone(annotation, INDEL_SIMPLE_REPEAT))
//				|| (annotation.contains(INDEL_LOW_COMPLEXITY) && isAnnotationAlone(annotation, INDEL_LOW_COMPLEXITY))
//				|| LESS_THAN_3_READS_NORMAL.equals(annotation)
//				|| MUTATION_IN_UNFILTERED_NORMAL.equals(annotation)
//				|| LESS_THAN_12_READS_NORMAL_AND_UNFILTERED.equals(annotation)
//				|| LESS_THAN_3_READS_NORMAL_AND_UNFILTERED.equals(annotation);
//	}
	
	public static String removeCompoundSnpAnnotationFromString(String annotation) {
		if ( ! StringUtils.isNullOrEmpty(annotation) && annotation.contains(COMPOUND_SNP)) {
			return annotation.replace(";" + COMPOUND_SNP, "");
		}
		return annotation;
	}
	
	/**
	 * Utility method to determine if an annotation contains text that classifies a snp as class A
	 * Class A is currently defined as "PASS"
	 * 
	 * @param annotation String
	 * @return true if string matches "--", false otherwise
	 */
	public static final boolean isClassA(String annotation) {
//		return PASS.equals(annotation) || "--".equals(annotation) || NOVEL_STARTS.equals(annotation) || "---".equals(annotation) ;	// my god...
		return PASS.equals(removeCompoundSnpAnnotationFromString(annotation));
	}
	
	public static final boolean isClassAIndel(String annotation, int homopolymerCutoff) throws IllegalArgumentException {
		if (StringUtils.isNullOrEmpty(annotation)) throw new IllegalArgumentException("null or empty annotation passed to SnpUtils.isClassAIndel");
		
		return 
				isClassA(annotation) 
				|| ( ! containsIndelClassBAnnotation(annotation)
				&&
				((annotation.contains(INDEL_HOM_ADJ) 
						&& getNNumberFromAnnotation(annotation, INDEL_HOM_ADJ) <= homopolymerCutoff)
				||
				(annotation.contains(INDEL_HOM_CON) 
						&& getNNumberFromAnnotation(annotation, INDEL_HOM_CON) <= homopolymerCutoff)
				||
				(annotation.contains(INDEL_HOM_EMB) 
						&& getNNumberFromAnnotation(annotation, INDEL_HOM_EMB) <= homopolymerCutoff)));
	}
	
	public static boolean containsIndelClassBAnnotation(String annotation) {
		return 
				annotation.contains(INDEL_SATELLITE)
				|| annotation.contains(INDEL_SIMPLE_REPEAT)
				|| annotation.contains(INDEL_LOW_COMPLEXITY)
				|| annotation.contains(MUTATION_IN_NORMAL)
				|| annotation.contains(INDEL_HCOVN)
				|| annotation.contains(INDEL_HCOVT)
				|| annotation.contains(LESS_THAN_12_READS_NORMAL)
				|| annotation.contains(MUTATION_GERMLINE_IN_ANOTHER_PATIENT)
				|| annotation.contains(LESS_THAN_8_READS_NORMAL)
				|| annotation.contains(INDEL_STRAND_BIAS)
				|| annotation.contains(INDEL_NPART);
				
	}
	
	/**
	 * Checks to see if the supplied subAnnotation is the only annotation in the supplied string
	 * With the exclusion of certain allowed annotations. eg. PASS
	 *  
	 * @param annotation
	 * @param subAnnotation
	 * @return
	 */
	public static boolean isAnnotationAlone(String annotation, String subAnnotation) {
		if (StringUtils.isNullOrEmpty(annotation)) throw new IllegalArgumentException("null or empty annotation passed to isAnnotationAlone");
		
		if (annotation.equals(subAnnotation)) return true;
		
		// check to see if there are more than 1 annotation - they are delimited by ';'
		int colonIndex = annotation.indexOf(';');
		if (colonIndex > -1) {
		
			String [] params = TabTokenizer.tokenize(annotation, ';');
			if (params.length > 2) return false;
			
			for (String s : params) {
				if ( ! SnpUtils.PASS.equals(s) && ! s.contains(subAnnotation)) {
					return false;
				}
			}
			return true;
		}
		
		// only 1 annotation and it doesn't match the subAnnotation
		return false;
		
	}
	
	public static int getNNumberFromAnnotation(String annotation, String prefix) {
		
		int startIndex = annotation.indexOf(prefix) + prefix.length();
		String numberString = "";
		for (int i = startIndex, len = annotation.length() ; i < len ; i++) {
			char c = annotation.charAt(i);
			if (Character.isDigit(c))
				numberString += c;
			else break;
		}
		
		// deal with empty string case
		if (numberString.length() == 0) throw new IllegalArgumentException("Invalid annotation passed to getNNumberFromAnnotation");
		
		return Integer.parseInt(numberString);
	}
	
	/**
	 * returns the alt allele from the mutation string
	 * assuming that the following format is adhered to:
	 * A/C
	 * will return C in this instance
	 * 
	 * @param mutation
	 * @return char representing the alt allele
	 * @TODO must update to deal with X>X/Y and X/Y>X/Y
	 */
	public static char getAltFromMutationString(String mutation) {
		if (StringUtils.isNullOrEmpty(mutation))
			throw new IllegalArgumentException("invalid mutation string supplied to getAltFromMutationString (null or empty)");
		return mutation.charAt(mutation.length() - 1);
	}
	
	/**
	 * Get the count of the supplied base from the supplied Nucleotide string.
	 * 
	 * eg. for the Nucleotide string A:1[41],3[19.33],G:8[39.12],29[26.48]
	 * this method will return 4 if 'A' is the supplied base, 37 if 'G' is the supplied base, and 0 for all other supplied bases
	 * 
	 * 
	 * @param bases
	 * @param base
	 * @return
	 */
	public static int getCountFromNucleotideString(final String bases, final char base) {
		if (StringUtils.isNullOrEmpty(bases)) return 0;
		
		final int basePosition = bases.indexOf(base);  
		if (basePosition == -1) return 0;
		
		final int bracketPosition = bases.indexOf('[', basePosition);
		
		final int forwardCount = Integer.parseInt(bases.substring(basePosition + 2, bracketPosition));
		
		final int commaPosition = bases.indexOf(',', bracketPosition);
		final int reverseCount = Integer.parseInt(bases.substring(commaPosition + 1, bases.indexOf('[', commaPosition)));
		
		return forwardCount + reverseCount;
	}
	
	/**
	 * Get the count of the supplied base from the supplied indel Nucleotide string.
	 * 
	 * eg. for the Nucleotide string 4;39;24;4;4;0;5
	 * this method will return 4 if 0 is the supplied position , 39 if 1 is the supplied position, etc.
	 * 
	 * 
	 * @param countsString
	 * @param position 0 based position of count of interest in string
	 * @return
	 */
	public static int getCountFromIndelNucleotideString(final String countsString, int position) {
		if (StringUtils.isNullOrEmpty(countsString)) return 0;
		
		String [] counts = TabTokenizer.tokenize(countsString, ';');
		if (position >= counts.length) {
			// hmmmm
			return 0;
		}
		String count = counts[position];
		// check that count is a number
		if (StringUtils.isNumeric(count)) {
			return Integer.parseInt(count);
		} else {
			throw new IllegalArgumentException
			("invalid parameters supplied to getCountFromIndelNucleotideString: " + countsString + ", and position: " + position);
		}
	}
	
}
