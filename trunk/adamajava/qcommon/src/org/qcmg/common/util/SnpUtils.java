/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;

public class SnpUtils {
	
	//NORMAL
	public static final String LESS_THAN_12_READS_NORMAL = "COVN12";
	public static final String LESS_THAN_8_READS_NORMAL = "COVN8";
	public static final String LESS_THAN_3_READS_NORMAL = "SAN3";
	
	//TUMOUR
	public static final String LESS_THAN_8_READS_TUMOUR = "COVT";
	public static final String LESS_THAN_3_READS_TUMOUR = "SAT3";
	public static final String NO_CALL_IN_TEST = "NCIT";
	
	//COMPOUND_SNP
	public static final String COMPOUND_SNP = "COMPOUNDMUTATION";
	
	public static final String SOMATIC = "SOMATIC";
	
	
	//MUTATION
	public static final String MUTATION_IN_UNFILTERED_NORMAL = "MIUN";
	public static final String MUTATION_IN_NORMAL = "MIN";
	public static final String MUTATION_GERMLINE_IN_ANOTHER_PATIENT = "GERM";	//mutation is a germline variant in another patient
	
//	public static final String STRAND_BIAS = "SBIAS";	// mutation only found on one strand
	public static final String STRAND_BIAS_ALT = "SBIASALT";	// mutation only found on one strand
	public static final String STRAND_BIAS_COVERAGE = "SBIASCOV";	// mutation only found on 1 strand, and no (or v. little) coverage at all found on other strand
	public static final String ALLELIC_FRACTION = "AF";	// allelic fraction of mutation is less than appended number eg. AF20
	public static final String END_OF_READ = "5BP";	// allelic fraction of mutation is less than appended number eg. AF20
	
	// a couple of extra class B's
	public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED= LESS_THAN_12_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;
	public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED= LESS_THAN_3_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;
	
	//EXTRAs
	public static final String NOVEL_STARTS = "NNS";
	public static final String MUTANT_READS = "MR";
	public static final String PASS = "PASS";
	public static final String MERGE_PASS = "PASS_1;PASS_2";
	public static final String MUTATION_EQUALS_REF = "MER";	// mutation same as reference
	
	
	public static final String INDEL_SATELLITE = "Satellite";	
	public static final String INDEL_SIMPLE_REPEAT = "Simple_repeat::";	
	public static final String INDEL_LOW_COMPLEXITY = "Low_complexity::";	
	public static final String INDEL_HOM_ADJ = "HOMADJ_";	
	public static final String INDEL_HOM_CON = "HOMCON_";	
	public static final String INDEL_HOM_EMB = "HOMEMB_";	
	public static final String INDEL_HCOVT = "HCOVT";	
	public static final String INDEL_HCOVN = "HCOVN";	
	public static final String INDEL_STRAND_BIAS = "TBIAS";	
	public static final String INDEL_NPART = "NPART";
	
	private static final QLogger logger = QLoggerFactory.getLogger(SnpUtils.class);
	
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
				isClassA(annotation)  || ( ! containsIndelClassBAnnotation(annotation)
				&& ((annotation.contains(INDEL_HOM_ADJ) 
						&& getNNumberFromAnnotation(annotation, INDEL_HOM_ADJ) <= homopolymerCutoff)
				   || (annotation.contains(INDEL_HOM_CON) 
						&& getNNumberFromAnnotation(annotation, INDEL_HOM_CON) <= homopolymerCutoff)
				   || (annotation.contains(INDEL_HOM_EMB) 
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
	public static String getAltFromMutationString(String mutation) {
		if (StringUtils.isNullOrEmpty(mutation)) {
			throw new IllegalArgumentException("invalid mutation string supplied to getAltFromMutationString (null or empty)");
		}
		int index = mutation.indexOf(Constants.MUT_DELIM);
		return mutation.substring(index + 1);
	}
	
	
	
	
	/**
	 * Get the count of the supplied base from the supplied Nucleotide string.
	 * 
	 * eg. for the Nucleotide string A1[41],3[19.33],G8[39.12],29[26.48]
	 * this method will return 4 if 'A' is the supplied base, 37 if 'G' is the supplied base, and 0 for all other supplied bases
	 * 
	 * If the bases have come from a merged vcf record, then the first occurrence of the desired base will be returned.
	 * 
	 * @param bases
	 * @param base
	 * @return
	 */
	public static int getCountFromNucleotideString(final String bases, final String base) {
		return getCountFromNucleotideString(bases, base, false);

//		if (StringUtils.isNullOrEmpty(bases) || StringUtils.isNullOrEmpty(base)) {
//			return 0;
//		}
//		
//		final int basePosition = bases.indexOf(base);  
//		if (basePosition == -1) return 0;
//		
//		final int bracketPosition = bases.indexOf('[', basePosition);
//		
//		final int forwardCount = Integer.parseInt(bases.substring(basePosition + base.length(), bracketPosition));
//		
//		final int commaPosition = bases.indexOf(',', bracketPosition);
//		final int reverseCount = Integer.parseInt(bases.substring(commaPosition + 1, bases.indexOf('[', commaPosition)));
//		
//		return forwardCount + reverseCount;
	}
	
	public static int getCountFromNucleotideString(final String bases, final String base, boolean compoundSnp) {
		if (StringUtils.isNullOrEmpty(bases) || StringUtils.isNullOrEmpty(base)) {
			return 0;
		}
		
		final int basePosition = bases.indexOf(base);  
		if (basePosition == -1) return 0;
		
		if (compoundSnp) {
			//AC,2,2,AT,2,4,A_,3,3,CA,2,1,CC,16,17,CG,2,1,CT,1,2,C_,16,16,GC,1,0,_C,0,1,G_,0,1
			// need next 2 commas locations
			int comma1 = bases.indexOf(Constants.COMMA, basePosition);
			int comma2 = bases.indexOf(Constants.COMMA, comma1 + 1);
			int comma3 = bases.indexOf(Constants.COMMA, comma2 + 1);
			if (comma1 == -1 || comma2 == -1) {
				return 0;
			}
			if (comma3 == -1) {
				// end of string
				comma3 = bases.length();
			}
			final int forwardCount = Integer.parseInt(bases.substring(comma1 + 1, comma2));
			final int reverseCount = Integer.parseInt(bases.substring(comma2 + 1, comma3));
			
			return forwardCount + reverseCount;	
		} else {
			
			final int bracketPosition = bases.indexOf('[', basePosition);
			
			final int forwardCount = Integer.parseInt(bases.substring(basePosition + base.length(), bracketPosition));
			
			final int commaPosition = bases.indexOf(',', bracketPosition);
			final int reverseCount = Integer.parseInt(bases.substring(commaPosition + 1, bases.indexOf('[', commaPosition)));
			
			return forwardCount + reverseCount;
		}
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
	
	public static boolean doesNucleotideStringContainReadsOnBothStrands(String bases) {
		
		if (StringUtils.isNullOrEmpty(bases)) return false;
		
//		logger.info("in doesNucleotideStringContainReadsOnBothStrands with : " + bases);
		
		String [] basesArray = TabTokenizer.tokenize(bases, Constants.COMMA);
		// should always have an even number of bases (1 for each strand)
		if (basesArray.length % 2 != 0) {
			logger.warn("Incorrect number of basesArray elements: " + basesArray.length);
		}
		
		int zeroOnFS = 0, zeroOnRS = 0;
		for (int i = 0 ; i < basesArray.length ; i++) {
			// strip leading character if it is a letter
			String arrayValue = basesArray[i];
			
			if ( ! Character.isDigit(arrayValue.charAt(0))) {
				
				arrayValue = arrayValue.substring(1);
				// check to see if first char is now a colon
				if (Constants.COLON == arrayValue.charAt(0)) {
					arrayValue = arrayValue.substring(1);
				}
			}
			if ("0[0]".equals(arrayValue)) {
				
				// if remainder when divided by 2 is 0 - fs, otherwise rs
				int rem = i % 2;
				if (rem == 0) {
					zeroOnFS++;
				} else {
					zeroOnRS++;
				}
			}
		}
//		logger.info("zeroOnFS : " + zeroOnFS + ", zeroOnRS: " + zeroOnRS + ", basesArray.length / 2: " + (basesArray.length / 2));
		
		return (zeroOnFS != (basesArray.length / 2)) &&  (zeroOnRS != (basesArray.length / 2));
	}
	
	public static boolean doesNucleotideStringContainReadsOnBothStrands(String bases, int sBiasCovPercentage) {
		
		if (StringUtils.isNullOrEmpty(bases)) return false;
		
//		logger.info("in doesNucleotideStringContainReadsOnBothStrands with : " + bases);
		
//		A0[0],848[37.74],C0[0],3[23],G0[0],393[38.01],T0[0],4[21.5]
		
		String [] basesArray = TabTokenizer.tokenize(bases, Constants.COMMA);
		// should always have an even number of bases (1 for each strand)
		if (basesArray.length % 2 != 0) {
			logger.warn("Incorrect number of basesArray elements: " + basesArray.length);
		}
		
		int fsCount = 0, rsCount = 0;
		for (int i = 0 ; i < basesArray.length ; i++) {
			// strip leading character if it is a letter
			String arrayValue = basesArray[i];
			
			if (Character.isAlphabetic(arrayValue.charAt(0))) {
				// forward strand
				// strip colon if it exists.
				int startPos = 1;
				if (arrayValue.charAt(1) == Constants.COLON) {
					startPos++;
				}
				int endPos = arrayValue.indexOf('[');
				fsCount += Integer.parseInt(arrayValue.substring(startPos, endPos));
					
			} else {
				// reverse strand
				int startPos = 0;
				if (arrayValue.charAt(1) == Constants.COLON) {
					startPos++;
				}
				int endPos = arrayValue.indexOf('[');
				rsCount += Integer.parseInt(arrayValue.substring(startPos, endPos));
			}
		}
//		logger.info("fsCount : " + fsCount + ", rsCount: " + rsCount + ", basesArray.length / 2: " + (basesArray.length / 2));
		
		int total = fsCount + rsCount;
		int min = Math.min(rsCount, fsCount);
		
		return ((double) min / total) * 100 > sBiasCovPercentage;
		
//		return (zeroOnFS != (basesArray.length / 2)) &&  (zeroOnRS != (basesArray.length / 2));
	}
	
}
