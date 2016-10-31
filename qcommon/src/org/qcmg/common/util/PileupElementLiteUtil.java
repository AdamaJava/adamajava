/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.Rule;

public class PileupElementLiteUtil {
	
	private static final NumberFormat nf = new DecimalFormat("0.##");
	
	public static final char OPEN_BRACKET = '[';
	public static final char CLOSE_BRACKET = ']';
	
	public static int getLargestVariantNovelStarts(final Accumulator accum, final char ref) {
		if (null == accum) return 0;
		PileupElementLite pel = accum.getLargestVariant(ref);
		if (null == pel) return 0;
		return pel.getNovelStartCount();
	}
	
	public static boolean[] isAccumulatorAKeeper(final Accumulator accum, final char ref, final Rule rule, final int percentage) {
		boolean [] results = new boolean[2];
		
		if (null == accum || null == rule) return results;
		PileupElementLite pel = accum.getLargestVariant(ref);
		if (null == pel) return results;
		
		
		int coverage = accum.getCoverage();
		int variantCount = pel.getTotalCount();
		int totalQualityScore = accum.getTotalQualityScore();
		int variantQualityScore = pel.getTotalQualityScore();
		
		if (passesCountCheck(variantCount, coverage, rule) 
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage)) {
			
			results[0] = true;	// first pass
			
		} else if (passesCountCheck(variantCount, coverage, rule, true) 
				&& pel.isFoundOnBothStrands()
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage, true)) {
			
			results[1] = true;	// second pass
		}
		return results;
	}
	
	public static boolean isAccumulatorAKeeperFirstPass(final Accumulator accum, final char ref, final Rule rule, final int percentage) {
		if (null == accum || null == rule) return false;
		PileupElementLite pel = accum.getLargestVariant(ref);
		if (null == pel) return false;
		
		int coverage = accum.getCoverage();
		int variantCount = pel.getTotalCount();
		int totalQualityScore = accum.getTotalQualityScore();
		int variantQualityScore = pel.getTotalQualityScore();
		
		return passesCountCheck(variantCount, coverage, rule) 
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage);
	}
	public static boolean isAccumulatorAKeeperSecondPass(final Accumulator accum, final char ref, final Rule rule, final int percentage) {
		if (null == accum || null == rule) return false;
		PileupElementLite pel = accum.getLargestVariant(ref);
		if (null == pel) return false;
		
		int coverage = accum.getCoverage();
		int variantCount = pel.getTotalCount();
		int totalQualityScore = accum.getTotalQualityScore();
		int variantQualityScore = pel.getTotalQualityScore();
		
		return passesCountCheck(variantCount, coverage, rule, true) 
				&& pel.isFoundOnBothStrands()
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage, true);
	}
	
	public static boolean passesCountCheck(int count, int coverage, Rule rule) {
		return passesCountCheck(count, coverage, rule, false);
	}
	
	public static boolean passesCountCheck(int count, int coverage, Rule rule, boolean secondPass) {
		if (null == rule) return false;
//			throw new IllegalArgumentException("null Rule passed to method");
		if (coverage < 0)
			throw new IllegalArgumentException("coverage cannot be less then zero");
		if (count > coverage)
			throw new IllegalArgumentException("count cannot be greater than the coverage");
		if (coverage > rule.getMaxCoverage())
			throw new IllegalArgumentException("coverage cannot be more than max coverage in rule");
		if (coverage < rule.getMinCoverage())
			throw new IllegalArgumentException("coverage cannot be less then min coverage in rule");
		
		boolean usePercentage = rule.getMaxCoverage() == Integer.MAX_VALUE;
		
		if (usePercentage) {
			double noOfVariants = rule.getNoOfVariants();
			
			if (secondPass) {
				return ((double)count / coverage * 100) >= (noOfVariants / 2);
			} else {
				return ((double)count / coverage * 100) >= noOfVariants;
			}
		} else {
			return count >= rule.getNoOfVariants();
		}
	}
	
	/**
	 * returns true if the PEL object has reads on both strands, 
	 * and that the lesser strand as a percentage of the total is greater than or equal to the supplied value
	 * @param pel
	 * @param percentage
	 * @return
	 */
	public static boolean areBothStrandsRepresented(PileupElementLite pel, int percentage) {
		
		if (null == pel) {
			throw new IllegalArgumentException("Null PileupElementLite obj passed to PileupElementLiteUtil.areBothStrandsRepresented");
		}
		if (percentage < 0) {
			throw new IllegalArgumentException("Negative percentage passed to PileupElementLiteUtil.areBothStrandsRepresented: " + percentage);
		}
		if ( ! pel.isFoundOnBothStrands()) {
			return false;
		}
		
		int total = pel.getTotalCount();
		int min = Math.min(pel.getForwardCount(), pel.getReverseCount());
		
		return ((double) min / total) * 100 >= percentage;
	}
	
	public static boolean passesWeightedVotingCheck(final int totalQualityScore, 
			final int variantQualityScore,  double percentage) {
		return passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage, false);
	}
	public static boolean passesWeightedVotingCheck(final int totalQualityScore, 
			final int variantQualityScore,  double percentage, boolean secondPass) {
		return (100 * (double)variantQualityScore / totalQualityScore) >= (secondPass ? percentage / 2 : percentage);
	}
	

	public static String toSummaryString(final PileupElementLite pel, final String base) {
		int forwardCount = pel.getForwardCount();
		int reverseCount = pel.getReverseCount();
		
		StringBuilder sb = new StringBuilder(base);
//		sb.append(":");
		sb.append(forwardCount).append(OPEN_BRACKET);
		double forwardQual = forwardCount == 0 ? 0.0 : (double)pel.getTotalForwardQualityScore() / forwardCount; 
		sb.append(nf.format(forwardQual));
		sb.append("],");
		sb.append(reverseCount).append(OPEN_BRACKET);
		double reverseQual = reverseCount == 0 ? 0.0 : (double)pel.getTotalReverseQualityScore() / reverseCount; 
		sb.append(nf.format(reverseQual));
		sb.append(CLOSE_BRACKET);
		return sb.toString();
	}
	
	public static String toObservedAlleleByStrand(final PileupElementLite pel, final String base) {
		int forwardCount = pel.getForwardCount();
		int reverseCount = pel.getReverseCount();
		
		StringBuilder sb = new StringBuilder(base);
		sb.append(forwardCount).append(OPEN_BRACKET);
		double forwardQual = forwardCount == 0 ? 0.0 : (double)pel.getTotalForwardQualityScore() / forwardCount; 
		sb.append(nf.format(forwardQual));
		sb.append(CLOSE_BRACKET);
		sb.append(reverseCount).append(OPEN_BRACKET);
		double reverseQual = reverseCount == 0 ? 0.0 : (double)pel.getTotalReverseQualityScore() / reverseCount; 
		sb.append(nf.format(reverseQual));
		sb.append(CLOSE_BRACKET);
		return sb.toString();
	}
	
	public static String getBaseAndReadIds(final PileupElementLite pel, final String base) {
		
		final StringBuilder sb = new StringBuilder(base);
		sb.append(Constants.COLON);
		
		TIntList forwardReadIds = null;
		TIntList reverseReadIds = null;
//		List<Long> forwardReadIds = null;
//		List<Long> reverseReadIds = null;
		
		TIntArrayList ids = pel.getForwardReadIds();
		if (null != ids) {
			forwardReadIds = new TIntArrayList(ids);
		}
		ids = pel.getReverseReadIds();
		if (null != ids) {
			reverseReadIds =new TIntArrayList(ids);
		}
		
		if (null != forwardReadIds) {
			forwardReadIds.forEach(new TIntProcedure() {
				@Override
				public boolean execute(int s) {
					sb.append(s).append(Constants.COMMA);
					return true;
				}});
		}
		
		sb.append('-');
		
		if (null != reverseReadIds) {
			reverseReadIds.forEach(new TIntProcedure() {
				@Override
				public boolean execute(int s) {
					sb.append(s).append(Constants.COMMA);
					return true;
				}});
		}
		
		return sb.toString();
	}
}
