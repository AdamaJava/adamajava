package au.edu.qimr.clinvar.util;

import htsjdk.samtools.util.SequenceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

import au.edu.qimr.clinvar.model.FastqProbeMatch;
import au.edu.qimr.clinvar.model.IntPair;
import au.edu.qimr.clinvar.model.Probe;

public class FastqProbeMatchUtil {
	private static QLogger logger =  QLoggerFactory.getLogger(FastqProbeMatchUtil.class);
	
	public static boolean isMultiMatched(FastqProbeMatch fpm) {
		
		return bothReadsHaveAMatch(fpm)
				&& ! fpm.getRead1Probe().equals(fpm.getRead2Probe());
	}
	
	/**
	 * both reads match to the same probe
	 * @param fpm
	 * @return
	 */
	public static boolean isProperlyMatched(FastqProbeMatch fpm) {
		
		return bothReadsHaveAMatch(fpm)
				&& fpm.getRead1Probe().equals(fpm.getRead2Probe());
	}
	
	/**
	 * Both reads have a match, although they may not be the same...
	 */
	public static boolean bothReadsHaveAMatch(FastqProbeMatch fpm) {
		
		return null != fpm.getRead1Probe() 
				&& null != fpm.getRead2Probe();
	}
	
	/**
	 * Both reads have a match, although they may not be the same...
	 */
	public static boolean neitherReadsHaveAMatch(FastqProbeMatch fpm) {
		
		return null == fpm.getRead1Probe() 
				&& null == fpm.getRead2Probe();
	}
	/**
	 * Both reads have a match, although they may not be the same...
	 */
	public static boolean onlyOneReadHasAMatch(FastqProbeMatch fpm) {
		
		return (null != fpm.getRead1Probe() && null == fpm.getRead2Probe()
				|| null == fpm.getRead1Probe() && null != fpm.getRead2Probe());
	}
	
	
	public static boolean doesFPMMatchProbe(FastqProbeMatch fpm, Probe p) {
		return ! neitherReadsHaveAMatch(fpm)
				&& (null != fpm.getRead1Probe() && p.equals(fpm.getRead1Probe())
						|| null != fpm.getRead2Probe() && p.equals(fpm.getRead2Probe()));
	}
	
	public static <T> void incrementCount(Map<T, AtomicInteger> map, T data) {

		if (null == map || null == data)
			throw new AssertionError("Null map or data found in FastqProbeMatchUtil.incrementCount");
		
		AtomicInteger currentCount = map.get(data);
		if (null == currentCount) {
			currentCount = map.put(data, new AtomicInteger(1));
			if (null == currentCount)
				return;
		}
		currentCount.incrementAndGet();
	}
	
	
	
	public static void getStats(Set<FastqProbeMatch> set) {
		
		int bothReadsHaveAMatch = 0;
		int sameMatch = 0;
		Map<IntPair, AtomicInteger> sameMatchScores = new HashMap<>();
		int differentMatches = 0;
		Map<IntPair, AtomicInteger> differentMatchesScores = new HashMap<>();
		int neitherHaveAMatch = 0;
		int justOneMatch = 0;
		Map<IntPair, AtomicInteger> justOneMatchScores = new HashMap<>();
		int count = set.size();
		
		for (FastqProbeMatch fpm : set) {
			
			if (neitherReadsHaveAMatch(fpm)) {
				neitherHaveAMatch++;
			} else {
				// at least 1 match
				if (bothReadsHaveAMatch(fpm)) {
					bothReadsHaveAMatch++;
					if (isProperlyMatched(fpm)) {
						sameMatch++;
						incrementCount(sameMatchScores, fpm.getScore());
					}
					if (isMultiMatched(fpm)) {
						differentMatches++;
						incrementCount(differentMatchesScores, fpm.getScore());
					}
					
				} else {
					justOneMatch++;
					incrementCount(justOneMatchScores, fpm.getScore());
				}
			}
		}
		
		logger.info("Total count: " + count);
		logger.info("Neither read matches: " + neitherHaveAMatch + " (" + ((100 * neitherHaveAMatch) / count) + "%)");
		logger.info("One read has a match: " + justOneMatch + " (" + ((100 * justOneMatch) / count) + "%)");
		
		List<IntPair> scores = new ArrayList<>(justOneMatchScores.keySet());
		Collections.sort(scores);
		for (IntPair score : scores) {
			logger.info("just one match score: " + score + ", count: " + justOneMatchScores.get(score).get());
		}
		
		logger.info("Both reads have a match: " + bothReadsHaveAMatch + " (" + ((100 * bothReadsHaveAMatch) / count) + "%)");
		logger.info("Both reads have the same match: " + sameMatch + " (" + ((100 * sameMatch) / count) + "%)");
		// some stats on edit distances used
		scores = new ArrayList<>(sameMatchScores.keySet());
		Collections.sort(scores);
////		for (MatchScore score : scores) {
////			logger.info("same match score: " + score + ", count: " + sameMatchScores.get(score).get());
////		}
//		
////		logger.info("Reads have a different match: " + differentMatches + " (" + ((100 * differentMatches) / count) + "%)");
		
		scores = new ArrayList<>(differentMatchesScores.keySet());
		Collections.sort(scores);
		for (IntPair score : scores) {
			logger.info("different match score: " + score + ", count: " + differentMatchesScores.get(score).get());
		}
	}
	
	
	public static void createFragment(FastqProbeMatch fpm) {
		// only do this if we have same probe for r1 and r2 for now...
		if (isProperlyMatched(fpm)) {
			Probe p = fpm.getRead1Probe();
			int expectedFragLen = p.getExpectedFragmentLength();
			int combinedReadLen = fpm.getCombnedReadLength();
			int expectedOverlap = combinedReadLen - expectedFragLen;
			String r1 = fpm.getRead1().getReadString();
			String r2 = fpm.getRead2().getReadString();
			String r1Overlap = r1.substring(r1.length() - expectedOverlap);
			String r2Overlap = r2.substring(r2.length() - expectedOverlap);
			
			String r2OverlapRC = SequenceUtil.reverseComplement(r2Overlap);
			
			int [] distances = ClinVarUtil.getBasicAndLevenshteinEditDistances(r1Overlap, r2OverlapRC);
			fpm.setOverlapBasicEditDistance(distances[0]);
			fpm.setOverlapLevenshteinEditDistance(distances[1]);
			
			
			if (distances[0] == 0 && distances[1] == 0) {
				// lets create a fragment!!!
				String frag = r1 + SequenceUtil.reverseComplement(r2.substring(0,r2.length() - expectedOverlap));
				fpm.setFragment(frag);
//				logger.info("frag: " + frag);
//				logger.info("r1: " + r1);
//				logger.info("r2: " + r2);
//				logger.info("r1Overlap: " + r1Overlap);
//				logger.info("r2Overlap: " + r2Overlap);
//				logger.info("r1OverlapRC: " + r1OverlapRC);
//				logger.info("frag length: " + frag.length() + ", expectedFragLen: " + expectedFragLen);
			}
			
			
			if (distances[0] > 10) {
				// calculate sliding value
				int slideValue = ClinVarUtil.noOfSlidesToGetPerfectMatch(r1Overlap, r2OverlapRC);
				fpm.setSlideValue(slideValue);
				
//				if (Math.abs(slideValue) > 10 && Math.abs(slideValue) < (expectedOverlap / 2)) {
//					logger.info("slide value: "+ slideValue + " expected value: " + expectedOverlap + ", r1: " + r1 + " r2: " + r2);
//				}
				
//				if (slideValue == expectedOverlap || Math.abs(slideValue) == 1) {
//					logger.info("slide value: "+ slideValue + " expected value: " + expectedOverlap + ", r1: " + r1 + " r2: " + r2);
//				}
				
				// if sliding value is less that the overlap, then try and create a fragment
				if (Math.abs(slideValue) < (expectedOverlap / 2)) {
					
					
					
					
//					logger.info("attempting to create a fragment with slideValue: " + slideValue);
					String frag = r1.substring(0,  r1.length() - (expectedOverlap + slideValue)) + SequenceUtil.reverseComplement(r2);
					fpm.setFragment(frag);
//					if (fpm.getRead1Probe().getId() == 542) {
//						logger.info("r1: " + r1 + ", r2: " + r2 + ", frag: " + frag + ", r1Overlap: " + r1Overlap + ", r2OverlapRC: " + r2OverlapRC );
//					}
//					if (slideValue > 0) {
//						frag =  r1.substring(0,  r1.length() - expectedOverlap - slideValue) + SequenceUtil.reverseComplement(r2);
//					} else {
//						frag =  r1 + SequenceUtil.reverseComplement(r2.substring(0,r2.length() - (expectedOverlap - Math.abs(slideValue))));
//
////						frag =  "---" + r1.substring(0, r1.length() - Math.abs(slideValue)) + SequenceUtil.reverseComplement(r2.substring(0,r2.length() - expectedOverlap - Math.abs(slideValue)));
//					}
				}
			}
			
			fpm.setExpectedReadOverlapLength(expectedOverlap);
		}
	}

}
