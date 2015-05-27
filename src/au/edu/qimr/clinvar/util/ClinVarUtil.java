package au.edu.qimr.clinvar.util;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;

import au.edu.qimr.clinvar.model.Bin;
import au.edu.qimr.clinvar.model.Probe;

public class ClinVarUtil {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ClinVarUtil.class);
	
	public static int [] getDoubleEditDistance(String read1, String read2, String primer1, String primer2, int editDistanceCutoff) {
		
		int editDistance = getEditDistance(read1, primer1, editDistanceCutoff + 1);
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
	
	public static int  getEditDistance(String read, String primer, int editDistanceCutoff) {
//		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(read)
//				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(primer)) {
//			throw new IllegalArgumentException("read or primer (or both) supplied to ClinVarUtil.getEditDistance were null. read: " + read + ", primer: " + primer);
//		}
		int led = StringUtils.getLevenshteinDistance(primer, read.substring(0, primer.length()), editDistanceCutoff);
		
		return led >= 0 ? led : Integer.MAX_VALUE;
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
						
						int start = Math.max(0, indelStartPos - 1);
						String ref = refSeq.substring(start, indelStartPos + span);
						String alt = binSeq.substring(start, indelStartPos + span);
						mutations.add(new Pair<Integer, String>(indelStartPos - 1, ref.replaceAll("-","") +"/" +  alt.replaceAll("-","")));
						// reset span
						span = 0;
					}
					if (c == '.') {
						// snp
						char ref = refSeq.charAt(position);
						char alt = binSeq.charAt(position);
						mutations.add(new Pair<Integer, String>(position, ref + "/" + alt));
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
				
				int start = Math.max(0, indelStartPos - 1);
				String ref = refSeq.substring(start, indelStartPos + span);
				String alt = binSeq.substring(start, indelStartPos + span);
				mutations.add(new Pair<Integer, String>(indelStartPos - 1, ref.replaceAll("-","") +"/" +  alt.replaceAll("-","")));
			}
		}
		return mutations;
	}
	
	public static List<Probe> getAmpliconsOverlappingPosition(ChrPosition cp, Set<Probe> probes) {
		List<Probe> overlappingProbes = new ArrayList<>();
		for (Probe p : probes) {
			if (ChrPositionUtils.isChrPositionContained(p.getCp(), cp)) {
				overlappingProbes.add(p);
			}
		}
		return overlappingProbes;
	}
	
	/**
	 * Returns a representation of the supplied position as seen in the supplied amplicon and bins in the following format:
	 * base,count, ampliconId(total reads in amplicon),binId1(count),binId2(count).....
	 * @param cp
	 * @param overlappingProbes
	 * @param probeBinDist
	 * @return
	 */
	public static String getAmpliconDistribution(VcfRecord vcf, List<Probe> overlappingProbes, Map<Probe, List<Bin>> probeBinDist, int minBinSize) {
		StringBuilder sb = new StringBuilder();
		
		Map<String, List<Pair<Probe, Bin>>> baseDist = new HashMap<>();
		
		for (Probe amplicon : overlappingProbes) {
			
			List<Bin> bins = probeBinDist.get(amplicon);
			
			for (Bin b : bins) {
				/*
				 * only deal with bins that have >= minBinSize read in them
				 */
				if (b.getRecordCount() >= minBinSize) {
					String binSeq = getBaseAtPosition(vcf, amplicon, b);
					if (null != binSeq) {
						List<Pair<Probe, Bin>> probeBinPair = baseDist.get(binSeq);
						if (null == probeBinPair) {
							probeBinPair = new ArrayList<>();
							baseDist.put(binSeq, probeBinPair);
						}
						probeBinPair.add(new Pair<Probe, Bin>(amplicon, b));
					}
				}
			}
		}
		
		
		// convert map to sb
		for (Entry<String, List<Pair<Probe, Bin>>> entry : baseDist.entrySet()) {
			String bases = entry.getKey();
			List <Pair<Probe, Bin>> probeBinList = entry.getValue();
			int tally = 0;
			for (Pair<Probe, Bin> pair : probeBinList) {
				tally += pair.getRight().getRecordCount();
			}
			
			StringBuilder s = new StringBuilder(bases);
			s.append(Constants.COMMA);
			s.append(tally);
			for (Pair<Probe, Bin> pair : probeBinList) {
				s.append(Constants.COMMA);
				s.append(pair.getLeft().getId()).append("/");
				s.append(pair.getRight().getId()).append("(").append(pair.getRight().getRecordCount()).append(")");
			}
			if (sb.length() > 0) {
				sb.append(Constants.SEMI_COLON);
			}
			sb.append(s);
		}
		
		return sb.toString();
	}
	
	public static int getZeroBasedPositionInString(int mutationStartPosition, int binStartPosition) {
		return mutationStartPosition - binStartPosition;
	}
	
	public static String getBaseAtPosition(VcfRecord vcf, Probe amplicon, Bin bin) {
		
		String [] smithWatermanDiffs = bin.getSmithWatermanDiffs();
		if (null == smithWatermanDiffs) {
			logger.warn("bin does not contain sw diffs!!! bin: " + bin.getId() + ", probe: " + amplicon.getId() + ", vcf cp: " + vcf.getChrPosition().toIGVString());
		}
		String probeRef = amplicon.getReferenceSequence();
		// remove any indel characters - only checking
		String swRef = smithWatermanDiffs[0].replace("-", "");
		int offset = probeRef.indexOf(swRef);
		
		if ( offset == -1) {
			logger.warn("probeRef.indexOf(swRef) == -1!!! probe (id:bin.id: " + amplicon.getId() + ":" + bin.getId() + ") , probe ref: " + probeRef + ", swRef: " + swRef);
		}
		
		
//		int positionInAmplicon = amplicon.getCp().getPosition() + offset;
//		int positionInString = vcf.getChrPosition().getPosition() - positionInAmplicon;
		int length = vcf.getChrPosition().getLength();
		
		int positionInString = getZeroBasedPositionInString(vcf.getChrPosition().getPosition(), amplicon.getCp().getPosition() + offset);
		
		if (amplicon.getId() == 96) {
			logger.info("positionInString: " + positionInString +", from offset: " + offset + ", vcf.getChrPosition().getPosition(): " + vcf.getChrPosition().getPosition() +", amplicon.getCp().getPosition(): " + amplicon.getCp().getPosition());
		}
		
		return getMutationString(positionInString, length, smithWatermanDiffs);
		
//		int expectedStart = vcf.getChrPosition().getPosition() - positionInAmplicon;
//		int expectedEnd = vcf.getChrPosition().getEndPosition() - positionInAmplicon + 1;
//		
//		if (expectedStart < 0) {
//			/*
//			 * This happens when the bin is shorter than (starts after) the amplicon.
//			 * In this situation, we need to log and return null
//			 */
//			logger.info("bin " + bin.getId() + ", in amplicon " + amplicon.getId() + " has an offset of " + offset + ", which means that it starts after the position we are interested in " + vcf.getChrPosition().toIGVString());
//			return null;
//		}
//		
//		boolean insertion = vcf.getRef().length() < vcf.getAlt().length();
//		boolean deletion = vcf.getRef().length() > vcf.getAlt().length();
//		
//		
//		int safeExpectedEnd = Math.min(expectedEnd -1, smithWatermanDiffs[1].length());
//		int additionalOffset = 0;
//		String swDiffRegion = smithWatermanDiffs[1].substring(0, safeExpectedEnd); 
//		if (swDiffRegion.contains(" ")) {
//			
//			// keep track of number of insertions and deletions
//			
//			for (int i = 0 ; i < safeExpectedEnd ; i++) {
//				char c = swDiffRegion.charAt(i);
//				if (c == ' ') {
//					if ('-' == smithWatermanDiffs[0].charAt(i)) {
//						// insertion
//						additionalOffset++;
//					} else if ('-' == smithWatermanDiffs[2].charAt(i)) {
//						//deleteion
//						additionalOffset--;
//					} else {
//						//!@##$!!! something else-tion
//						logger.warn("Should have found either an indel at position " + i + " in smithWatermanDiffs[0] or smithWatermanDiffs[2]");
//					}
//				}
//			}
//			
//			
//		} else {
//			// no additional offset required 
//		}
//		
//		
//		/*
//		 *  next get the span of the event
//		 *  If we have an insertion after the position we are dealing with, get the type and length
//		 */
//		int span = 0;
//		int i = 0;
//		while (expectedStart + i < smithWatermanDiffs[1].length() && smithWatermanDiffs[1].charAt(expectedStart + i) == ' ') {
//			if (smithWatermanDiffs[0].charAt(expectedStart + i) == '-') {
//				span++;
//			} else if (smithWatermanDiffs[2].charAt(expectedStart + i) == '-') {
//				span--;
//			}
//			i++;
//		}
//		
//		
//		
//		if (amplicon.getId() == 96) {
//			logger.info("amplicon id: " + amplicon.getId() + ", bin id: " + bin.getId());
//			logger.info("expectedStart: " + expectedStart + ", expectedEnd: " + expectedEnd + ", positionInAmplicon: " + positionInAmplicon + ", offset: " + offset + ", additionalOffset: " + additionalOffset + ", span: " + span);
//		}
//		
//		int seqLength = bin.getLength();
//		int finalStart = expectedStart + additionalOffset;		
//		int finalEnd = expectedEnd + additionalOffset + span;		
//		if (finalEnd > seqLength || finalEnd < 0) {
//			logger.warn("end position is less than zero or greater than length of string! expectedEnd: " + expectedEnd + ", offset: " + offset + ", additionalOffset: " + additionalOffset + ", finalEnd: " +finalEnd + " seq length: " + seqLength + ", vcf cp: " + vcf.getChrPosition().toIGVString() + ", amplicon cp: " + amplicon.getCp().toIGVString());
//			for (String s : smithWatermanDiffs) {
//				logger.info(s);
//			}
//		}
//		if (finalStart < 0 || finalStart > seqLength) {
//			logger.warn("start position is less than zero or greater than length of string! expectedStart: " + expectedStart + ", offset: " + offset + ", additionalOffset: " + additionalOffset + ", finalStart: " +finalStart + " seq length: " + seqLength + ", vcf cp: " + vcf.getChrPosition().toIGVString() + ", amplicon cp: " + amplicon.getCp().toIGVString());
//			for (String s : smithWatermanDiffs) {
//				logger.info(s);
//			}
//		}
//		
//		String binSeq = bin.getSequence().substring(Math.max(0,finalStart), Math.min(seqLength, finalEnd));
//		
//		if (binSeq.length() != vcf.getChrPosition().getLength()) {
//			logger.warn("length of bin sequence does not equal cp. binseq: " + binSeq + ", vcf cp: " + vcf.getChrPosition().toIGVString());
//		}
//		
//		return binSeq; 
	}
	
	
	public static String getMutationString(final int positionInString, final int eventLength, String [] smithWatermanDiffs) {
		
		int expectedStart = positionInString;
		int expectedEnd = expectedStart + eventLength;
		int binSequenceLength = smithWatermanDiffs[2].length();
		
		if (expectedStart < 0) {
			/*
			 * This happens when the bin is shorter than (starts after) the amplicon.
			 * In this situation, we need to log and return null
			 */
//			logger.info("bin " + bin.getId() + ", in amplicon " + amplicon.getId() + " has an offset of " + offset + ", which means that it starts after the position we are interested in " + vcf.getChrPosition().toIGVString());
			return null;
		}
		if (expectedEnd > binSequenceLength) {
			logger.warn("Expected end: " + expectedEnd + ", is greater than the length of the bin sequence: " + binSequenceLength);
			return null;
		}
		
		
//		int safeExpectedEnd = Math.min(expectedEnd -1, smithWatermanDiffs[1].length());
		int additionalOffset = 0;
		String swDiffRegion = smithWatermanDiffs[1].substring(0, expectedStart); 
		if (swDiffRegion.contains(" ")) {
			
			// keep track of number of insertions and deletions
			
			for (int i = 0 ; i < expectedStart ; i++) {
				char c = swDiffRegion.charAt(i);
				if (c == ' ') {
					if ('-' == smithWatermanDiffs[0].charAt(i)) {
						// insertion
						additionalOffset++;
					} else if ('-' == smithWatermanDiffs[2].charAt(i)) {
						//deleteion
						additionalOffset--;
					} else {
						//!@##$!!! something else-tion
						logger.warn("Should have found either an indel at position " + i + " in smithWatermanDiffs[0] or smithWatermanDiffs[2]");
					}
				}
			}
		} else {
			// no additional offset required 
		}
		
		
		/*
		 *  next get the span of the event
		 *  If we have an insertion after the position we are dealing with, get the type and length
		 */
		int span = 0;
		int i = 1;
		while (expectedStart + i < binSequenceLength && smithWatermanDiffs[1].charAt(expectedStart + i) == ' ') {
			if (smithWatermanDiffs[0].charAt(expectedStart + i) == '-') {
				span++;
			} else if (smithWatermanDiffs[2].charAt(expectedStart + i) == '-') {
				/*
				 * only decrement span if we are dealing with a deletion, in which case the event length is > 1
				 */
//				if (eventLength > 1) {
//					span--;
//				}
			}
			i++;
		}
		
		int finalStart = expectedStart + additionalOffset;
		int finalEnd = expectedEnd + additionalOffset + span;
		
		if (finalStart >=finalEnd) {
			logger.warn("finalStart: " + finalStart + " is greater than finalEnd: " + finalEnd);
		} else if (finalStart < 0) {
			logger.warn("finalStart: " + finalStart + " is less than 0");
		} else if (finalEnd < 0) {
			logger.warn("finalEnd: " + finalEnd + " is less than 0");
		} else if (finalEnd > smithWatermanDiffs[1].length()) {
			logger.warn("finalEnd: " + finalEnd + " is > than smithWatermanDiffs[1].length(): " + smithWatermanDiffs[1].length());
		} else {
			return smithWatermanDiffs[2].substring(finalStart, finalEnd).replace("-", "");
		}
		
		return null;
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
