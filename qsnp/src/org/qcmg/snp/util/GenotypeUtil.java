/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.PileupElementLiteUtil;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfUtils;

import gnu.trove.map.TCharObjectMap;

import org.qcmg.common.model.Classification;
public class GenotypeUtil {
	
	
	private final static QLogger logger = QLoggerFactory.getLogger(GenotypeUtil.class);
	
	public static final int MUTATION_IN_NORMAL_MIN_COVERAGE = 3;
	public static final int MUTATION_IN_NORMAL_MIN_PERCENTAGE = 5;
	public static final int MIDDLE_OF_READ_CUTOFF = 5;
	
	
	public static boolean containsFilter(StringBuilder sb) {
		return sb.length() > 0 && ! sb.toString().equals(Constants.MISSING_DATA_STRING) && ! sb.toString().equals(Constants.MISSING_DATA_STRING + Constants.SEMI_COLON + Constants.MISSING_DATA); 
	}
	
	
	public static void updateStringBuilder(StringBuilder sb, CharSequence toAdd) {
		StringUtils.updateStringBuilder(sb, toAdd, Constants.SEMI_COLON);
	}
	
	
	/**
	 * Currently returning:
	 * GT
	 * AD
	 * DP
	 * EOR
	 * FF - failed filters
	 * FT - filter
	 * INF
	 * MR
	 * NNS
	 * OABS
	 * 
	 * @param acc
	 * @param gt
	 * @param alt
	 * @param ref
	 * @return
	 */
	public static String getFormatValues(Accumulator acc, String gt, String alt, char ref, boolean runSBias, int sbiasPercentage, int sbiasPercentageCoverage, Classification cl, boolean isControl) {
		/*
		 * need alt and ref to be able to proceed
		 */
		if (StringUtils.isNullOrEmpty(alt)) {
			throw new IllegalArgumentException("Null or empty alt passed to GenotypeUtil.getFormatValues()");
		}
			
		/*
		 * deal with null acc in place
		 */
		TCharObjectMap<int[]> accumulatorMap = AccumulatorUtils.getAccumulatorDataByBase(acc);
		
		String oabs = null == acc ? Constants.MISSING_DATA_STRING : AccumulatorUtils.getOABS(accumulatorMap);
		
		StringBuilder sb = new StringBuilder();
		sb.append(null != gt ? gt : Constants.MISSING_GT).append(Constants.COLON);
		sb.append(VcfUtils.getAD(""+ref, alt, oabs)).append(Constants.COLON);
		sb.append(null == acc ? Constants.MISSING_DATA_STRING :acc.getCoverage()).append(Constants.COLON);
		/*
		 * adding EOR (end of reads -similar in format to FF)
		 */
		sb.append(null != acc ? AccumulatorUtils.getEndOfReadsPileup(accumulatorMap) : Constants.MISSING_DATA_STRING).append(Constants.COLON);
		/*
		 * adding FF - failed filters
		 */
		sb.append(null != acc ? acc.getFailedFilterPileup() : Constants.MISSING_DATA_STRING).append(Constants.COLON);
		
		String [] altAlleleArray = alt.length() == 1 ? new String[]{alt} : TabTokenizer.tokenize(alt, Constants.COMMA);
		
	
		/*
		 * FT - don't add filters here - move to qannotate
		 */
		sb.append(".:");
		
		/*
		 * only thing going into info filed is SOMATIC, and that only applies to test samples  and if the classification is somatic
		 */
		sb.append( ( ! isControl && cl == Classification.SOMATIC) ? Classification.SOMATIC : Constants.MISSING_DATA).append(Constants.COLON);
		
		String nns = getNNS(gt, altAlleleArray, acc);
		
		sb.append(StringUtils.isNullOrEmptyOrMissingData(nns) ? Constants.MISSING_DATA : nns).append(Constants.COLON);
		sb.append(oabs);
		
		return sb.toString();
	}
	
	public static String getNNS(String gt, String [] altAlleleArray, Accumulator acc) {
		if (null != gt) {
			StringBuilder nns = new StringBuilder();
			int i = 0;
			int j = 1; 
			for (String s : altAlleleArray) {
				if (gt.contains((j) + "")) {
					char c = s.charAt(0);
					
					if (i++ > 0) {
						nns.append(Constants.COMMA);
					}
					int ns = AccumulatorUtils.getNovelStartsForBase(acc, c);
					nns.append(ns == 0 ? Constants.MISSING_DATA_STRING : ns);
					
				}
				j++;
			}
			return nns.length() > 0 ? nns.toString() : Constants.MISSING_DATA_STRING;
		}
		return Constants.MISSING_DATA_STRING;
	}
	
	public static String convertListOfListOfFiltersToString(List<List<String>> loloFilters) {
		int size = loloFilters.size();
		StringBuilder sb = new StringBuilder();
		if (size == 0) {
			return Constants.MISSING_DATA_STRING;
		} else if (size == 1) {
			
			/*
			 * filter out any missing data strings, and return with semi colon used to join
			 */
			sb.append( loloFilters.get(0).stream().filter(s -> ! s.equals(Constants.MISSING_DATA_STRING)).collect(Collectors.joining(Constants.SEMI_COLON_STRING)));
			
		} else {
			int i = 0;
			for (String s : loloFilters.get(0)) {
				StringUtils.updateStringBuilder(sb, s, Constants.SEMI_COLON);
				for (int j = 1 ; j < size ; j++) {
					String sj = loloFilters.get(j).get(i);
					sb.append(Constants.SEMI_COLON).append(sj);
				}
				i++;
			}
		}
		if (sb.length() == 0) {
			sb.append(Constants.MISSING_DATA);
		}
		return sb.toString();
	}
	
	public static int endsOfReads(PileupElementLite pel) {
		return endsOfReads(pel, MIDDLE_OF_READ_CUTOFF);
	}
	
	public static int endsOfReads(PileupElementLite pel, int middleOfReadCutoff) {
		if (null == pel) {
			return 0;
		}
		
		int [] countAndEndOfReadByStrand = PileupElementLiteUtil.getCountAndEndOfReadByStrand(pel); 
		int totaCount = countAndEndOfReadByStrand[0] + countAndEndOfReadByStrand[1];
		int endOfReadCount = countAndEndOfReadByStrand[2] + countAndEndOfReadByStrand[3];
		
		if ((totaCount - endOfReadCount) >= middleOfReadCutoff 
				&& countAndEndOfReadByStrand[0] - countAndEndOfReadByStrand[2] > 0	// forward strand Count - forward strand end of read count
				 && countAndEndOfReadByStrand[1] - countAndEndOfReadByStrand[3] > 0) {	// reverse strand Count - reverse strand end of read count
			// all good
			return 0;
		} else {
			return  endOfReadCount;
		}
	}
	
	/**
	 */
	public static boolean strandBias(PileupElementLite pel, int sbiasPercentage) {
		
		return ! PileupElementLiteUtil.areBothStrandsRepresented(pel, sbiasPercentage);
	}
	
	public static Classification getClassification(String controlPileup, String controlGT, String testGT, String alts) {
		/*
		 * If we have both the control and test genotypes, use the other method
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(controlGT) &&  ! StringUtils.isNullOrEmptyOrMissingData(testGT)) {
			return getClassification(controlGT, testGT);
		}
		
		if ( ! StringUtils.isNullOrEmptyOrMissingData(controlGT) ) {
			if (controlGT.startsWith("0")) {
				if (controlGT.charAt(2) != '0') {
					return Classification.GERMLINE;
				} else {
					logger.info("Ignoring (ND) due to homozygous and equal to ref: " + controlGT);
				}
			} else {
				return Classification.GERMLINE;
			}
		}
		if (! StringUtils.isNullOrEmptyOrMissingData(testGT)) {
			
			int t1 = Integer.parseInt(testGT.substring(0,1));
			int t2 = Integer.parseInt(testGT.substring(2));
			boolean isHom = t1 == t2;
			String [] aAlts = alts.split(Constants.COMMA_STRING);
			
			String alt1 = t1 > 0 ?   aAlts[t1 - 1] : null;
			String alt2 = t2 > 0 ?   aAlts[t2 - 1] : null;
			
			boolean controlContainsAlt1 = null != controlPileup && null != alt1 && controlPileup.contains(alt1);
			boolean controlContainsAlt2 = null != controlPileup && null != alt2 && controlPileup.contains(alt2);
			
			if (t1 == 0) {
				
				if (isHom) {
					// Ignore (not expecting this to happen...)
				} else {
					// if there is evidence of the variant in the normal - > GERMLINE
					return controlContainsAlt2 ? Classification.GERMLINE : Classification.SOMATIC;
				}
			} else {	// reference not present in tumour genotype
				if (isHom) {
					return controlContainsAlt2 ? Classification.GERMLINE : Classification.SOMATIC;
				} else {
					return controlContainsAlt1 && controlContainsAlt2 ? Classification.GERMLINE : Classification.SOMATIC;
				}
			}
		}
		return Classification.UNKNOWN;
	}
	
	
	public static Classification getClassification(String control, String test) {
		
		if (null == control || null == test) {
			logger.error("Error predicting genotype. Control: " + control + ", test: " + test);
			throw new IllegalArgumentException("Error in GenotypeUtil.getClassification. Error predicting genotype. Control: " + control + ", test: " + test);
		}
		
		if (control.equals(test)) {
			/*
			 * If genotypes are the same - Germline
			 */
			return Classification.GERMLINE;
		}
		
		if (test.length() < 3) {
			logger.warn("test length is less than 3!: " + test);
		}
		if (control.length() < 3) {
			logger.warn("control length is less than 3!: " + control);
		}
		
		char c1 = control.charAt(0);
		char c2 = control.charAt(2);
		char t1 = test.charAt(0);
		char t2 = test.charAt(2);
		boolean cHom = c1 == c2;
		boolean tHom = t1 == t2;
		boolean cHet = ! cHom;
		boolean tHet = ! tHom;
		
		if ((cHom && tHom) || (cHet && tHet)) {
			/*
			 *  not equal but both are homozygous or both are hets
			 */
			return Classification.SOMATIC;
			
		} else if (cHom && tHet) {
			
			if (t1 == '0' && t2 == c1) {
				return Classification.GERMLINE;
			} else {
				return Classification.SOMATIC;
			}
		} else if (cHet && tHom){
			if (c1 == t1 || c2 == t1) {
				return Classification.GERMLINE;
			} else {
				return Classification.SOMATIC;
			}
		} else {
			return Classification.UNKNOWN;
		}
	}
	
	/**
	 * Returns the alt alleles as determined by the supplied genotypes, classification and reference.
	 * 
	 * eg. If control is A/A, test is C/C, reference is A, then "C" will be returned.
	 * 
	 * @param control
	 * @param test
	 * @param reference
	 * @param c
	 * @return
	 */
	public static String getAltAlleles(GenotypeEnum control, GenotypeEnum test, char reference) {
		/*
		 * Add all alleles that are not the ref
		 */
		String controlAltAlleles = null != control ? control.getQualifyingAltAlleles(reference) : null;
		String testAltAlleles = null != test ?  test.getQualifyingAltAlleles(reference) : null;
		if (null != controlAltAlleles && null != testAltAlleles) {
			
			if (controlAltAlleles.equals(testAltAlleles)) {
				return controlAltAlleles;
			}
			/*
			 * need to make sure we don't have duplication
			 */
			if (controlAltAlleles.length() == 1) {
				if (testAltAlleles.contains(controlAltAlleles)) {
					return testAltAlleles;
				} else {
					return controlAltAlleles + Constants.COMMA + testAltAlleles;
				}
			}
			if (testAltAlleles.length() == 1) {
				if (controlAltAlleles.contains(testAltAlleles)) {
					return controlAltAlleles;
				} else {
					return controlAltAlleles + Constants.COMMA + testAltAlleles;
				}
			}
			
			/*
			 * both contain more than 1 alleles and they are not the same - what are the odds....
			 */
			List<String> alleles = new ArrayList<>(5);
			for (String s : controlAltAlleles.split(Constants.COMMA_STRING)) {
				alleles.add(s);
			}
			for (String s : testAltAlleles.split(Constants.COMMA_STRING)) {
				alleles.add(s);
			}
			return alleles.stream().distinct().collect(Collectors.joining(Constants.COMMA_STRING));
			
		} else if (null != controlAltAlleles) {
			return controlAltAlleles;
		} else if (null != testAltAlleles) {
			return testAltAlleles;
		} else {
			/*
			 * both null - hmmmmmm
			 */
			return null;
		}
	}

}
