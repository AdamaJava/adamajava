package org.qcmg.snp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfUtils;

public class GenotypeUtil {
	
	private final static QLogger logger = QLoggerFactory.getLogger(GenotypeUtil.class);
	
	public static final int MUTATION_IN_NORMAL_MIN_COVERAGE = 3;
	public static final int MUTATION_IN_NORMAL_MIN_PERCENTAGE = 5;
	
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
		
		String oabs = null == acc ? Constants.MISSING_DATA_STRING : acc.getObservedAllelesByStrand();
		
		StringBuilder sb = new StringBuilder();
//		int accTotalCoverage = null == acc ? Constants.MISSING_DATA_STRING :acc.getCoverage();
		sb.append(null != gt ? gt : Constants.MISSING_GT).append(Constants.COLON);
		sb.append(VcfUtils.getAD(""+ref, alt, oabs)).append(Constants.COLON);
		sb.append(null == acc ? Constants.MISSING_DATA_STRING :acc.getCoverage()).append(Constants.COLON);
		/*
		 * adding EOR (end of reads -similar in format to FF)
		 */
		sb.append(null != acc ? acc.getEndOfReadsPileup() : Constants.MISSING_DATA_STRING).append(Constants.COLON);
		/*
		 * adding FF - failed filters
		 */
		sb.append(null != acc ? acc.getFailedFilterPileup() : Constants.MISSING_DATA_STRING).append(Constants.COLON);
		
		String [] altAlleleArray = alt.length() == 1 ? new String[]{alt} : TabTokenizer.tokenize(alt, Constants.COMMA);
		
	
		/*
		 * don't add filters here - move to qannotate
		 */
//		sb.append(getFormatFilter(acc, gt, altAlleleArray,  ref, runSBias,  sbiasPercentage, sbiasPercentageCoverage, cl, isControl)).append(Constants.COLON);
		sb.append(".:");
		
		/*
		 * only thing going into info filed is SOMATIC, and that only applies to test samples  and if the classification is somatic
		 */
		sb.append( ( ! isControl && cl == Classification.SOMATIC) ? Classification.SOMATIC : Constants.MISSING_DATA).append(Constants.COLON);
		
		List<String> mrNNS = getMRandNNS(gt, altAlleleArray, acc);
		
		/*
		 * removing MR - duplicated in AD
		 */
		sb.append(mrNNS.get(1).length() == 0 ? Constants.MISSING_DATA : mrNNS.get(1).toString()).append(Constants.COLON);
		sb.append(oabs);
		
		return sb.toString();
	}
	
	/**
	 * Gets the Number of Novel Starts (NNS) and Mutant Read (MR) counts for the supplied accumulator.
	 * It uses the supplied genotype and list of alt alleles to get the appropriate values from the accumulator object.
	 * 
	 * @param gt
	 * @param altAlleleArray
	 * @param acc
	 * @return
	 */
	public static List<String> getMRandNNS(String gt, String [] altAlleleArray, Accumulator acc) {
		if (null != gt) {
			StringBuilder mr = new StringBuilder();
			StringBuilder nns = new StringBuilder();
			int i = 0;
			int j = 1; 
			for (String s : altAlleleArray) {
				if (gt.contains((j) + "")) {
					char c = s.charAt(0);
					
					if (i++ > 0) {
						mr.append(Constants.COMMA);
						nns.append(Constants.COMMA);
					}
					PileupElementLite pel = acc != null ? acc.getPelForBase(c) : null;
					mr.append(null == pel ? Constants.MISSING_DATA_STRING : pel.getTotalCount());
					nns.append(null == pel ? Constants.MISSING_DATA_STRING : pel.getNovelStartCount());
					
				}
				j++;
			}
			return Arrays.asList(mr.length() > 0 ? mr.toString() : Constants.MISSING_DATA_STRING
					, nns.length() > 0 ? nns.toString() : Constants.MISSING_DATA_STRING);
		}
		return Arrays.asList(Constants.MISSING_DATA_STRING, Constants.MISSING_DATA_STRING);
	}

}
