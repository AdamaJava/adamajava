package au.edu.qimr.clinvar.util;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;

public class ClinVarUtil {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ClinVarUtil.class);
	
	public static int [] getDoubleEditDistance(String read1, String read2, String primer1, String primer2, int editDistanceCutoff) {
		
		int editDistance = getEditDistance(read1, primer1);
		int editDistance2 = Integer.MAX_VALUE;
		
		if (editDistance <= editDistanceCutoff) {
			// get read2 edit distance
			editDistance2 = getEditDistance(read2, primer2);
		}
		
		return new int [] {editDistance, editDistance2};
	}
	
	public static int  getEditDistance(String read, String primer) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(read)
				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(primer)) {
			throw new IllegalArgumentException("read or primer (or both) supplied to ClinVarUtil.getEditDistance were null. read: " + read + ", primer: " + primer);
		}
		
//		//TODO see if doing a basic edit distance here and only running levenshtein of bed > cutoff would save time
//		int bed = getBasicEditDistance(primer, read.substring(0, primer.length()));
//		if (bed <= 1) {
//			return bed;
//		}
		
		return StringUtils.getLevenshteinDistance(primer, read.substring(0, primer.length()));
	}
	
	public static String breakdownEditDistanceDistribution(List<Integer> editDistances) {
		TIntIntHashMap dist = new TIntIntHashMap();
		final StringBuilder sb = new StringBuilder();
		for (Integer ed : editDistances) {
			int existingValue = dist.get(ed);
			dist.put(ed, existingValue + 1);
		}
		dist.forEachEntry(new TIntIntProcedure() {

			@Override
			public boolean execute(int arg0, int arg1) {
				sb.append(arg0).append(":").append(arg1).append(",");
				return true;
			}

		});
		return sb.toString();
	}
	
	
	public static int getBasicEditDistance(CharSequence s, CharSequence t) {
		if (StringUtils.isEmpty(s) || StringUtils.isEmpty(t)) {
			throw new IllegalArgumentException("Null string passed to ClinVarUtil.getBasicEditDistance(). s: " + s + ", t: " + t);
		}
		
		// s and t need to be the same length
		if (s.length() != t.length()) {
			throw new IllegalArgumentException("Strings passed to ClinVarUtil.getBasicEditDistance() are not the same length s: " + s + ", t: " + t);
		}
		
		int ed = 0;
		for (int i = 0 , len = s.length() ; i < len ; i++) {
			if (s.charAt(i) != t.charAt(i)) {
				ed ++;
			}
		}
		
		return ed;
	}
	
	public static int getOutwardSlideCount(final String overlap1, final String overlap2) {
		// it is assumed that the 2 char sequences do not match as they are
		if (StringUtils.isEmpty(overlap1) || StringUtils.isEmpty(overlap2)) {
			throw new IllegalArgumentException("Null string passed to ClinVarUtil.getOutwardSlideCount(). s: " + overlap1 + ", t: " + overlap2);
		}
		
		// s and t need to be the same length
		if (overlap1.length() != overlap2.length()) {
			throw new IllegalArgumentException("Strings passed to ClinVarUtil.getOutwardSlideCount() are not the same length s: " + overlap1 + ", t: " + overlap2);
		}
		String s = overlap1;
		String t = overlap2;
		
		int initialLength = overlap1.length();
		int noOfSlides = 0;
		
		while (noOfSlides < initialLength &&  ! t.equals(s)) {
			noOfSlides++;
			s = s.substring(1);
			t = t.substring(0, t.length() -1);
		}
		// want a -ve number for outward slide
		return 0 - noOfSlides;
	}
	
//	public static String getFragmentUsingSlide(final String fullSeq1, final String fullSeq2, int expectedOverlap) {
//		// it is assumed that the 2 char sequences do not match as they are
//		if (StringUtils.isEmpty(fullSeq1) || StringUtils.isEmpty(fullSeq2)) {
//			throw new IllegalArgumentException("Null string passed to ClinVarUtil.getInwardSlideCount(). s: " + fullSeq1 + ", t: " + fullSeq2);
//		}
//		
//		if (fullSeq1.length() <= expectedOverlap || fullSeq2.length() <= expectedOverlap) {
//			throw new IllegalArgumentException("Strings passed to ClinVarUtil.getInwardSlideCount(). s: " + fullSeq1 + ", t: " + fullSeq2 +", are not longer than the expectedOverlap: " + expectedOverlap);
//		}
//		
////		String s = fullSeq1.substring(fullSeq1.length() - expectedOverlap);
//		String t = fullSeq2.substring(0, expectedOverlap);
//		
//		// get index of t occurring in fullSeq1
//		int index = fullSeq1.lastIndexOf(t);
//		int noOfShifts = 0;
//		while (noOfShifts < expectedOverlap &&  index == -1) {
//			noOfShifts++;
//			t = t.substring(0, t.length() -1);
//			index = fullSeq1.lastIndexOf(t);
//		}
//		if (index == -1) {
//			// can't create fragment
//			logger.info("unable to create fragment for fullSeq1: " + fullSeq1 + ", fullSeq2: " + fullSeq2 + ", expectedOverlap: " + expectedOverlap);
//			return null;
//		}
//		
//		index = fullSeq1.length() - expectedOverlap - index;
//		
//		return index;
		
//		int initialLength = fullSeq1.length();
//		int noOfSlides = 0;
//		
//		while (noOfSlides < initialLength &&  ! t.equals(s)) {
//			noOfSlides++;
//			s = s.substring(1);
//			t = t.substring(0, t.length() -1);
//		}
//		// want a -ve number for outward slide
//		return 0 - noOfSlides;
//	}
	
	
	/**
	 * 
	 * @param smithWatermanDiffs
	 * @return
	 */
	public static List<Pair<Integer, String>> getPositionRefAndAltFromSW(String [] smithWatermanDiffs) {
		List<Pair<Integer, String>> mutations = new ArrayList<>();
		
		String refSeq = smithWatermanDiffs[0];
		String diffs = smithWatermanDiffs[1];
		String binSeq = smithWatermanDiffs[2];
		
		if (null != diffs && ! diffs.isEmpty()) {
			
			int position = 0;
			int span = 0;
			int indelStartPos = 0;
			for (char c : diffs.toCharArray()) {
				if (c != ' ') {
					if (span >0) {
						// create indel
						
						String ref = refSeq.substring(indelStartPos,indelStartPos + span);
						String alt = binSeq.substring(indelStartPos,indelStartPos + span);
						mutations.add(new Pair<Integer, String>(indelStartPos, ref +"/" +  alt));
						// reset span
						span = 0;
					}
					if (c == '.') {
						// snp
						char ref = refSeq.charAt(position);
						char alt = binSeq.charAt(position);
						mutations.add(new Pair<Integer, String>(position, ref + "/" + alt));
//						createMutation(p, b, position, ref + "", alt + "");
					}
					
				} else {
					if (span == 0) {
						indelStartPos = position;
					}
					span++;
					// indel
				}
				position++;
			}
			if (span >0) {
				// create indel
				
				String ref = refSeq.substring(indelStartPos,indelStartPos + span);
				String alt = binSeq.substring(indelStartPos,indelStartPos + span);
				mutations.add(new Pair<Integer, String>(indelStartPos, ref +"/" +  alt));
			}
		}
		return mutations;
	}
	
	
	
	public static int noOfSlidesToGetPerfectMatch(final String s1, final String t1) {
		// it is assumed that the 2 char sequences do not match as they are
		if (StringUtils.isEmpty(s1) || StringUtils.isEmpty(t1)) {
			throw new IllegalArgumentException("Null string passed to ClinVarUtil.noOfSlidesToGetPerfectMatch(). s: " + s1 + ", t: " + t1);
		}
		
		// s and t need to be the same length
		if (s1.length() != t1.length()) {
			throw new IllegalArgumentException("Strings passed to ClinVarUtil.noOfSlidesToGetPerfectMatch() are not the same length s: " + s1 + ", t: " + t1);
		}
		String s = s1;
		String t = t1;
		
		int initialLength = s1.length();
		int noOfSlides = 0;
		// left slide first
		while (noOfSlides < initialLength &&  ! t.equals(s)) {
			noOfSlides++;
			t = t.substring(1);
			s = s.substring(0, s.length() -1);
		}
		
		
		// need a reliable check to see if noOfSlides is sufficiently large to trigger a RHS slide
		if (noOfSlides >= initialLength -1) {
			//perform a RHS slide
			s = s1;
			t = t1;
			noOfSlides = 0;
			// left slide first
			while (noOfSlides < initialLength &&  ! t.equals(s)) {
				noOfSlides--;
				s = s.substring(1);
				t = t.substring(0, t.length() -1);
			}
			
//			if (Math.abs(noOfSlides) >= initialLength -1) {
//				logger.info("RHS and LHS slide gave us nothing....");
//			}
		}
		
		return noOfSlides;
	}
	
	
	public static int[] getBasicAndLevenshteinEditDistances(CharSequence s, CharSequence t) {
		// do exact match first
		if (s.equals(t)) {
			return new int[]{0,0};			// we have a match - set both distances to 0
		} else {
			// do basic distancing next
			int ed = ClinVarUtil.getBasicEditDistance(s, t);
			return ed > 1 ? new int[]{ ed, StringUtils.getLevenshteinDistance(s,t) } :  new int[]{ ed, ed };
			
		}
	}
}
