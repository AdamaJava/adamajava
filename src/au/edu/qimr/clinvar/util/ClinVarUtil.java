package au.edu.qimr.clinvar.util;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TLongIntProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.SequenceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qmule.SmithWatermanGotoh;

import au.edu.qimr.clinvar.model.Bin;
import au.edu.qimr.clinvar.model.Fragment;
import au.edu.qimr.clinvar.model.Probe;

public class ClinVarUtil {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ClinVarUtil.class);
	
	public static boolean isSequenceExactMatch(String [] diffs, String sequecne) {
		return ! doesSWContainSnpOrIndel(diffs) && diffs[2].equals(sequecne);
	}
	
	public static boolean doesSWContainSnpOrIndel(String [] diffs) {
		return doesSWContainSnp(diffs) || doesSWContainIndel(diffs);
	}
	public static boolean doesSWContainSnp(String [] diffs) {
		return diffs[1].contains(".");
	}
	public static boolean doesSWContainIndel(String [] diffs) {
		return diffs[1].contains(" ");
	}
	public static String[] getSwDiffs(String ref, String sequence) {
		return getSwDiffs(ref, sequence, false);
	}
	
	public static String[] getSwDiffs(String ref, String sequence, boolean rescueSequence) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(ref)
				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(sequence)) {
			throw new IllegalArgumentException("ref or sequence (or both) supplied to ClinVarUtil.getSwDiffs were null. ref: " + ref + ", sequence: " + sequence);
		}
		
		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 16, 4);
		String [] diffs = nm.traceback();
		if (rescueSequence) {
			
			int swSeqLength = diffs[2].replace("-", "").length();
			if (swSeqLength == sequence.length()) {
				/*
				 * Perfect Match!
				 */
			} else {
				/*
				 * try a more lenient sw calculation to see if we can get a better score
				 */
				nm = new SmithWatermanGotoh(ref, sequence, 5, -4, 8, 2);
				String [] lenientDiffs = nm.traceback();
				int swLenientScore =  lenientDiffs[2].replace("-", "").length();
				if (swLenientScore > swSeqLength) {
					logger.debug("got a better score when using a more lenient sw setup!!!");
					for (String s : diffs) {
						logger.debug("old s: " + s);
					}
					for (String s : lenientDiffs) {
						logger.debug("new s: " + s);
					}
				}
				
				if (swLenientScore == sequence.length()) {
					/*
					 * Lenient sw puts all our bases down
					 */
					diffs = lenientDiffs;
				} else {
					if (swLenientScore > swSeqLength) {
						diffs = lenientDiffs;
					}
					rescueSWData(diffs, ref, sequence);
				}
			}
		}
		return diffs;
	}
	
	
	public static TLongArrayList getSingleArray(TIntObjectHashMap<TLongArrayList> map) {
		final TLongArrayList results = new TLongArrayList();
		map.forEachEntry(new TIntObjectProcedure<TLongArrayList>(){
			@Override
			public boolean execute(int i, TLongArrayList longList) {
				results.addAll(longList);
				return true;
			}});
		return results;
	}
	
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
		
		return StringUtils.getLevenshteinDistance(primer, read.substring(0, primer.length()));
	}
	
	public static int  getEditDistance(String read, String primer, int editDistanceCutoff) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(read)
				|| org.qcmg.common.string.StringUtils.isNullOrEmpty(primer)) {
			throw new IllegalArgumentException("read or primer (or both) supplied to ClinVarUtil.getEditDistance were null. read: " + read + ", primer: " + primer);
		}
		int led = StringUtils.getLevenshteinDistance(primer, read.substring(0, primer.length()), editDistanceCutoff);
		
		return led >= 0 ? led : Integer.MAX_VALUE;
	}
	
	public static String breakdownEditDistanceDistribution(TIntArrayList editDistances) {
		if (null == editDistances) {
			throw new IllegalArgumentException("Null TIntArrayList passed to ClinVarUtil.breakdownEditDistanceDistribution");
		}
		editDistances.reverse();
		final TIntIntHashMap dist = new TIntIntHashMap();
		final StringBuilder sb = new StringBuilder();
		
		editDistances.forEach(new TIntProcedure(){
			@Override
			public boolean execute(int ed) {
				int existingValue = dist.get(ed);
				dist.put(ed, existingValue + 1);
				return true;
		}});
		dist.forEachEntry(new TIntIntProcedure() {
			@Override
			public boolean execute(int arg0, int arg1) {
				if (sb.length() > 0) {
					sb.append(Constants.COMMA);
				}
				sb.append(arg0).append(Constants.COLON).append(arg1);
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
		int len = s.length();
		if (len != t.length()) {
			throw new IllegalArgumentException("Strings passed to ClinVarUtil.getBasicEditDistance() are not the same length s: " + s + ", t: " + t);
		}
		
		int ed = 0;
		for (int i = 0 ; i < len ; i++) {
			if (s.charAt(i) != t.charAt(i)) {
				ed ++;
			}
		}
		return ed;
	}
	
	public static TIntObjectHashMap<TLongArrayList> getBestStartPosition(long [][] tilePositions, int tileLength, int indelOffset, int tiledDiffThreshold, int minNumberOfTiles) {
//		public static long[] getBestStartPosition(long [][] tilePositions, int tileLength, int indelOffset, int tiledDiffThreshold) {
		TLongIntMap positionAndTiles = new TLongIntHashMap();
		int noOfTiles = tilePositions.length;
		long startPos = -1;
		for (int i = 0 ; i < noOfTiles ; i++) {
			/*
			 * get first entry, search subsequent arrays
			 */
			long [] positions = tilePositions[i];
			int positionsLength = positions.length;
			
			for (int j = 0 ; j < positionsLength ; j++) {
				startPos = positions[j];
				int tileDepth = 1;
				if (startPos == Long.MAX_VALUE || startPos == Long.MIN_VALUE) {
					// this either does not occur or occurs very frequently - in either case - ignore for now...
					continue;
				}
				positionAndTiles.put(startPos, 1);
				
				for (int k = i +1; k < noOfTiles ; k++) {
					/*
					 * Exact matching only for the moment - need to traverse array and use indelOffset
					 */
					long from = startPos + (tileDepth * tileLength) - indelOffset;
					long to = startPos + (tileDepth * tileLength) + indelOffset;
					tileDepth++;
					for (long l : tilePositions[k]) {
						if (l > to) {
							// array i sorted, if we are past to, we are done
							break;
						}
						if (l >= from) {
							// match!
							positionAndTiles.increment(startPos);
							break;
						}
					}
				}
			}
			
			/*
			 * If we have a match for all tiles, exit!
			 * Only do this at the first iteration
			 */
			if (positionAndTiles.containsValue(noOfTiles - i)) {
				return reduceStartPositionsAndTileCount(positionAndTiles, tiledDiffThreshold, minNumberOfTiles, tileLength);
			}
		}
		return reduceStartPositionsAndTileCount(positionAndTiles, tiledDiffThreshold, minNumberOfTiles, tileLength);
	}
	
	/**
	 * Calculates the sw score for each entry in the map.
	 * If there is a winner, return the position to which it corresponds.
	 * If there is no winner (ie. empty map, or more than 1 position share the top score), return null 
	 * 
	 * @param scores
	 * @return
	 */
	public static ChrPosition getPositionWithBestScore(Map<ChrPosition, String[]> scores, int swDiffThreshold) {
		if (null == scores || scores.isEmpty()) {
			return null;
		}
		
		/*
		 * Calculate the sw diffs score for each entry
		 */
		TreeMap<Integer, List<ChrPosition>> scoresToPositionMap = new TreeMap<>();
		for (Entry<ChrPosition, String[]> entry : scores.entrySet()) {
			int cpScore = getSmithWatermanScore(entry.getValue());
			List<ChrPosition> positionsWithThisScore = scoresToPositionMap.get(cpScore);
			if (null == positionsWithThisScore) {
				positionsWithThisScore = new ArrayList<>();
				scoresToPositionMap.put(cpScore, positionsWithThisScore);
			}
			positionsWithThisScore.add(entry.getKey());
		}
		
		/*
		 * Get maximum score, and see how many matching ChrPositions we have
		 */
		Integer maxKey = scoresToPositionMap.lastKey();
		SortedMap<Integer, List<ChrPosition>> map = scoresToPositionMap.tailMap(maxKey  - swDiffThreshold);
		if (map.size() == 1 & map.get(maxKey).size() == 1) {
			return map.get(maxKey).get(0);
		}
		return null;
//		List<ChrPosition> maxScoringPositions = scoresToPositionMap.lastEntry().getValue();
//		return maxScoringPositions.size() == 1 ? maxScoringPositions.get(0) : null;
	}
	
	
	public static int getSmithWatermanScore(String [] diffs) {
		int score = -1;
		if (null != diffs && diffs.length == 3) {
			score = StringUtils.countMatches(diffs[1], '|');
		}
		return score;
	}
	
	/**
	 * Returns a long[] containing a position, and count for each result.
	 * eg. long[]{12345,10,23456,10}
	 * This tells us that at position 12345 we had a count of 10, and that at position 23456 we also had a count of ten
	 * 
	 * @param positionAndTiles
	 * @return
	 */
	public static TIntObjectHashMap<TLongArrayList> reduceStartPositionsAndTileCount(final TLongIntMap positionAndTiles, int tiledDiffThreshold, int minNoOfTiles, int tileLength) {
//		public static long[] reduceStartPositionsAndTileCount(TLongIntMap positionAndTiles, int tiledDiffThreshold) {

		/*
		 * remove from positionAndTiles values that are a tiles length from each other with n-1 tile counts
		 */
		TLongHashSet toRemove = new TLongHashSet();
		long [] sortedPositions = positionAndTiles.keys();
		int sortedPositionsLength = sortedPositions.length;
		if (sortedPositionsLength > 1) {
			Arrays.sort(sortedPositions);
			for (int i = 0 ; i < sortedPositionsLength ; i++) {
				long thisPosition = sortedPositions[i];
				int thisPositionTileCount = positionAndTiles.get(thisPosition) ;
				int j = 1;
				while ((i + j) < sortedPositionsLength) {
					long nextPosition =  sortedPositions[i + j];
					long diff = nextPosition - thisPosition;
					if (diff > 1000) {
						break;
					}
					if ((diff / tileLength) < 5 
							&& thisPositionTileCount == positionAndTiles.get(nextPosition) + j ) {
//						if (Math.abs(diff % tileLength) < 5 // indel Offset
//								&& thisPositionTileCount == positionAndTiles.get(nextPosition) + j ) {
//						if (diff == (tileLength * j)
//								&& thisPositionTileCount == positionAndTiles.get(nextPosition) + j ) {
						// remove
						toRemove.add(nextPosition);
					}
					j++;
				}
			}
			// do the removal
			toRemove.forEach(new TLongProcedure(){
				@Override
				public boolean execute(long l) {
					positionAndTiles.remove(l);
					return true;
				}});
		}
		
		int [] tileCounts = positionAndTiles.values();
		int tileCountsLength = tileCounts.length;
		if (tileCountsLength == 0) {
			return new TIntObjectHashMap<TLongArrayList>(0);
		}
		Arrays.sort(tileCounts);
		final int bestTileCount = tileCounts[tileCountsLength -1];
		final int tileCountCutoff = Math.max(bestTileCount - tiledDiffThreshold, minNoOfTiles);
		
		final TIntObjectHashMap<TLongArrayList> resultsMap = new TIntObjectHashMap<>(tileCountsLength);
		
//		final TLongList results = new TLongArrayList();
		positionAndTiles.forEachEntry(new TLongIntProcedure() {
			@Override
			public boolean execute(long l, int i) {
				if (i >= tileCountCutoff) {
					TLongArrayList list = resultsMap.get(i);
					if (null == list) {
						list = new TLongArrayList();
						resultsMap.put(i, list);
					}
					list.add(l);
//					results.add(l);
//					results.add(i);
				}
				return true;
			}
		});
//		return results.toArray();
		return resultsMap;
	}
	
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
		
			if (diffs.charAt(0) == ' ') {
				logger.warn("First char in diffs string is empty string!!!");
			}
			int position = 0;
			int span = 0;
			int indelStartPosDiff = 0;
			int indelStartPosRef = 0;
			int insertionBaseCount = 0;
			for (char c : diffs.toCharArray()) {
				if (c != ' ') {
					if (span >0) {
						// create indel
						
						int start = Math.max(0, indelStartPosDiff - 1);
						String ref = refSeq.substring(start, indelStartPosDiff + span);
						String alt = binSeq.substring(start, indelStartPosDiff + span);
						mutations.add(new Pair<Integer, String>(indelStartPosRef - 1, ref.replaceAll("-","") +"/" +  alt.replaceAll("-","")));
						// reset span
						span = 0;
					}
					if (c == '.') {
						// snp
						char ref = refSeq.charAt(position);
						char alt = binSeq.charAt(position);
						mutations.add(new Pair<Integer, String>(position - insertionBaseCount, ref + "/" + alt));
					}
					
				} else {
					if (span == 0) {
						indelStartPosRef = position - insertionBaseCount;
						indelStartPosDiff = position;
					}
					span++;
					// indel
					// if this is an insertion, update insertionBaseCount
					if (refSeq.charAt(position) == '-') {
						insertionBaseCount++;
					}
				}
				position++;
			}
			if (span >0) {
				// create indel
				
				int start = Math.max(0, indelStartPosDiff - 1);
				String ref = refSeq.substring(start, indelStartPosDiff + span);
				String alt = binSeq.substring(start, indelStartPosDiff + span);
				mutations.add(new Pair<Integer, String>(indelStartPosRef - 1, ref.replaceAll("-","") +"/" +  alt.replaceAll("-","")));
			}
		}
		return mutations;
	}
	
	public static List<Probe> getAmpliconsOverlappingPosition(ChrPosition cp, Set<Probe> probes) {
		if (null == cp) {
			throw new IllegalArgumentException("Null ChrPosition object passed to CLinVarUtil.getAmpliconsOverlappingPosition");
		}
		if (null == probes) {
			throw new IllegalArgumentException("Null Set<Probe> object passed to CLinVarUtil.getAmpliconsOverlappingPosition");
		}
		List<Probe> overlappingProbes = new ArrayList<>();
		for (Probe p : probes) {
			if (ChrPositionUtils.isChrPositionContained(p.getCp(), cp)) {
				overlappingProbes.add(p);
			}
		}
		
		if (overlappingProbes.isEmpty()) {
			for (Probe p : probes) {
				if (ChrPositionUtils.doChrPositionsOverlap(p.getCp(), cp, 20)) {
					overlappingProbes.add(p);
				}
			}
			if ( overlappingProbes.isEmpty()) {
				logger.warn("no containing or overlapping (20) probes exist");
			} else {
				logger.warn("no containing probes exist but some overlapping (20) probes do exist");
			}
		}
		
		return overlappingProbes;
	}
	
	public static SAMSequenceDictionary getSequenceDictionaryFromProbes(List<Probe> probes) {
		if (null == probes) {
			throw new IllegalArgumentException("Null List<Probe> passed to ClinVarUtil.getSequenceDictionaryFromProbes");
		}
		
		SAMSequenceDictionary dict = new SAMSequenceDictionary();
		
		for (Probe p : probes) {
			String chr = p.getCp().getChromosome();
			int len = p.getCp().getEndPosition();
			
			SAMSequenceRecord ssr = dict.getSequence(chr);
			if (null == ssr) {
				ssr = new SAMSequenceRecord(chr, len);
				dict.addSequence(ssr);
			} else {
				// check to see if length is already greater than this length
				if (ssr.getSequenceLength() < len) {
					ssr.setSequenceLength(len);
				}
			}
		}
		
		return dict;
	}
	public static SAMSequenceDictionary getSequenceDictionaryFromFragments(Collection<Fragment> fragments) {
		if (null == fragments) {
			throw new IllegalArgumentException("Null List<Probe> passed to ClinVarUtil.getSequenceDictionaryFromProbes");
		}
		
		SAMSequenceDictionary dict = new SAMSequenceDictionary();
		
		fragments.stream()
			.filter(f -> f.getActualPosition() != null)
			.forEach(f -> {
				String chr = f.getActualPosition().getChromosome();
				int len = f.getActualPosition().getEndPosition();
				
				SAMSequenceRecord ssr = dict.getSequence(chr);
				if (null == ssr) {
					ssr = new SAMSequenceRecord(chr, len);
					dict.addSequence(ssr);
				} else {
					// check to see if length is already greater than this length
					if (ssr.getSequenceLength() < len) {
						ssr.setSequenceLength(len);
					}
				}
			});
		
		
		return dict;
	}
	
	
	public static void calculateMdAndNmTags(final SAMRecord record, final byte[] ref, final boolean calcMD, final boolean calcNM) {
		if (!calcMD && !calcNM)
		return;
		
		final Cigar cigar = record.getCigar();
		final List<CigarElement> cigarElements = cigar.getCigarElements();
		final byte[] seq = record.getReadBases();
		final int start = record.getAlignmentStart() - 1;
		int i, x, y, u = 0;
		int nm = 0;
		final StringBuilder str = new StringBuilder();
		
		final int size = cigarElements.size();
		for (i = y = 0, x = start; i < size; ++i) {
			final CigarElement ce = cigarElements.get(i);
			int j;
			final int length = ce.getLength();
			final CigarOperator op = ce.getOperator();
			if (op == CigarOperator.MATCH_OR_MISMATCH || op == CigarOperator.EQ || op == CigarOperator.X) {
				for (j = 0; j < length; ++j) {
					final int z = y + j;
					
					if (ref.length <= x + j) break; // out of boundary
					
					int c1 = 0;
					int c2 = 0;
					// try {
					c1 = seq[z];
					c2 = ref[x + j];
					
					if ((c1 == c2) || c1 == 0) {
						// a match
						++u;
					} else {
						str.append(u);
						str.appendCodePoint(ref[x + j]);
						u = 0;
						++nm;
					}
				}
				if (j < length) break;
				x += length;
				y += length;
			} else if (op == CigarOperator.DELETION) {
				str.append(u);
				str.append('^');
				for (j = 0; j < length; ++j) {
				if (ref[x + j] == 0) break;
				str.appendCodePoint(ref[x + j]);
				}
				u = 0;
				if (j < length) break;
				x += length;
				nm += length;
			} else if (op == CigarOperator.INSERTION || op == CigarOperator.SOFT_CLIP) {
				y += length;
				if (op == CigarOperator.INSERTION) nm += length;
			} else if (op == CigarOperator.SKIPPED_REGION) {
				x += length;
			}
		}
		str.append(u);
		
		if (calcMD) record.setAttribute(SAMTag.MD.name(), str.toString());
		if (calcNM) record.setAttribute(SAMTag.NM.name(), nm);
}
	
	
	public static String[] rescueSWData(String[] diffs, String ref, String binSeq) {
		if (null == diffs) {
			throw new IllegalArgumentException("Null String []  passed to CLinVarUtil.rescueSWData");
		}
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(ref)) {
			throw new IllegalArgumentException("Null or empty ref passed to CLinVarUtil.rescueSWData: " + ref);
		}
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(binSeq)) {
			throw new IllegalArgumentException("Null or empty binSeq passed to CLinVarUtil.rescueSWData: " + binSeq);
		}
		
		/*
		 * Check length of binSequence returned from SW calc, as leading/trailing mutations will have been dropped
		 */
		String swBinSeq = diffs[2].replace("-", "");
		int lengthDiff = binSeq.length() - swBinSeq.length();
		if (lengthDiff > 0) {
//			logger.warn("Missing data in sw diffs. lengthDiff:  " + lengthDiff);
			
			String swRef = diffs[0].replace("-", "");
			
			if (binSeq.startsWith(swBinSeq)) {
				
				// need to get the last few bases
				String missingBinSeqBases = binSeq.substring(binSeq.length()  - lengthDiff);
				
				int refIndex = ref.indexOf(swRef); 
				if (refIndex > -1) {
					int positionInRef = refIndex + swRef.length();
					if (ref.length() < positionInRef + lengthDiff) {
						logger.warn("ref.length() < positionInRef + lengthDiff, ref.length(): " + ref.length() + ", positionInRef: " + positionInRef + ", lengthDiff: " + lengthDiff + ", ref: " + ref + ", binSeq: " + binSeq);
					} else {
						
						String missingRefBases = ref.substring(positionInRef, positionInRef + lengthDiff);
						
						
						if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
							logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
							// oh dear
						} else {
							if (lengthDiff > 1) {
								logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
							}
							diffs[0] += missingRefBases;
							for (int i = 0 ; i < lengthDiff ; i++) {
								diffs[1] += ".";
							}
							diffs[2] += missingBinSeqBases;
//							for (String s : diffs) {
//								logger.info("s: " + s);
//							}
						}
						
					}
				} else {
					logger.warn(" refIndex = ref.indexOf(swRef) == -1!!!");
				}
				
				
			} else if (binSeq.endsWith(swBinSeq)) {
				// need to get the first few bases
				String missingBinSeqBases = binSeq.substring(0, lengthDiff);
				
				int refIndex = ref.indexOf(swRef); 
				if (refIndex > -1) {
					if (refIndex - lengthDiff < 0) {
						logger.warn("refIndex - lengthDiff is lt 0, refIndex:  " + refIndex + ", lengthDiff: " + lengthDiff + ", ref: " + ref + ", binSeq: " + binSeq);
						for (String s : diffs) {
							logger.warn("s: " + s);
						}
					}
					String missingRefBases = ref.substring(Math.max(0, refIndex - lengthDiff), refIndex);
					
					if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
						logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
						// oh dear
					} else {
						if (lengthDiff > 1) {
							logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
						}
						diffs[0] = missingRefBases + diffs[0];
						for (int i = 0 ; i < lengthDiff ; i++) {
							diffs[1] = "." + diffs[1];
						}
						diffs[2] = missingBinSeqBases + diffs[2];
//						for (String s : diffs) {
//							logger.info("s: " + s);
//						}
					}
						
				} else {
					logger.warn(" refIndex = ref.indexOf(swRef) == -1!!!");
				}
				
			} else {
				logger.warn("binSeq neither startsWith norEndsWith swBinSeq. binSeq: " + binSeq + ", swBinSeq: " + swBinSeq);
			}
		}
		
		return diffs;
	}
	
	
	public static String getSortedBBString(String bb, String ref) {
		String [] params = bb.split(";");
		int length = params.length;
		if (length == 1) {
			return bb;
		}
		
		List<String> sizeSortedList = new ArrayList<>();
		for (String s : params) {
			String sRef = s.substring(0, s.indexOf(","));
			if ( ! sRef.equals(ref)) {
				sizeSortedList.add(s);
			}
		}
		
		Collections.sort(sizeSortedList, new Comparator<String>(){

			@Override
			public int compare(String arg0, String arg1) {
				int arg0Tally = Integer.valueOf(arg0.split(",")[1]);
				int arg1Tally = Integer.valueOf(arg1.split(",")[1]);
				return arg1Tally - arg0Tally;
			}
			
		});
		
		// now add in the ref
		for (String s : params) {
			String sRef = s.substring(0, s.indexOf(","));
			if (sRef.equals(ref)) {
				sizeSortedList.add(s);
			}
		}
		
		//stringify
		StringBuilder sb = new StringBuilder();
		for (String s : sizeSortedList) {
			if (sb.length() > 0) {
				sb.append(";");
			}
			sb.append(s);
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns a representation of the supplied position as seen in the supplied amplicon and bins in the following format:
	 * base,count, ampliconId(total reads in amplicon),binId1(count),binId2(count).....
	 * @param cp
	 * @param overlappingProbes
	 * @param probeBinDist
	 * @return
	 */
//	public static String getAmpliconDistribution(VcfRecord vcf, List<Probe> overlappingProbes, 
//			Map<Probe, List<Bin>> probeBinDist, int minBinSize) {
//		return getAmpliconDistribution(vcf, overlappingProbes, probeBinDist, minBinSize, false);
//	}
	
	public static String getAmpliconDistribution(VcfRecord vcf, List<Probe> overlappingProbes, 
			Map<Probe, List<Bin>> probeBinDist, int minBinSize, boolean diagnosticMode, boolean useBinsCloseToAmplicon) {
		StringBuilder sb = new StringBuilder();
		
		Map<String, List<Pair<Probe, Bin>>> baseDist = new HashMap<>();
		
		for (Probe amplicon : overlappingProbes) {
			
			List<Bin> bins = probeBinDist.get(amplicon);
			if (null != bins) {
				for (Bin b : bins) {
					/*
					 * only deal with bins that have >= minBinSize read in them
					 * and if useBinsCloseToAmplicon then probe and bin ChrPositions must overlap
					 */
					if (b.getRecordCount() >= minBinSize 
							&& ( ! useBinsCloseToAmplicon 
									|| (b.getBestTiledLocation() != null && doChrPosOverlap(amplicon.getCp(), b.getBestTiledLocation())))) {
						
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
			if (diagnosticMode) {
				for (Pair<Probe, Bin> pair : probeBinList) {
					s.append(Constants.COMMA);
					s.append(pair.getLeft().getId()).append("/");
					s.append(pair.getRight().getId()).append("(").append(pair.getRight().getRecordCount()).append(")");
				}
			} else {
				TIntSet probeSet = new TIntHashSet();
				TIntSet binSet = new TIntHashSet();
				
				for (Pair<Probe, Bin> pair : probeBinList) {
					probeSet.add(pair.getLeft().getId());
					binSet.add(pair.getRight().getId());
				}
				s.append(Constants.COMMA);
				s.append(probeSet.size()).append("/").append(binSet.size());
				
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
//		String probeRef = amplicon.getReferenceSequence();
		// remove any indel characters - only checking
		String swRef = smithWatermanDiffs[0].replace("-", "");
		int subRefPosition = amplicon.getSubReferencePosition(swRef);
//		int offset = probeRef.indexOf(swRef);
		
		if ( subRefPosition == -1) {
			logger.warn("amplicon.getSubReferencePosition(swRef) == -1!!! probe (id:bin.id: " + amplicon.getId() + ":" + bin.getId() + "), swRef: " + swRef);
		}
//		if ( offset == -1) {
//			logger.warn("probeRef.indexOf(swRef) == -1!!! probe (id:bin.id: " + amplicon.getId() + ":" + bin.getId() + ") , probe ref: " + probeRef + ", swRef: " + swRef);
//		}
		
		int length = vcf.getChrPosition().getLength();
		int positionInString = getZeroBasedPositionInString(vcf.getChrPosition().getPosition(), subRefPosition);
		
//		if (amplicon.getId() == 241) {
//			logger.info("positionInString: " + positionInString +", from offset: " + offset + ", vcf.getChrPosition().getPosition(): " + vcf.getChrPosition().getPosition() +", amplicon.getCp().getPosition(): " + amplicon.getCp().getPosition());
//		}
		
		return getMutationString(positionInString, length, smithWatermanDiffs);
		
	}
	
	public static Cigar getCigarForMatchMisMatchOnly(int length) {
		CigarElement ce = new CigarElement(length, CigarOperator.MATCH_OR_MISMATCH);
		List<CigarElement> ces = new ArrayList<>(2);
		ces.add(ce);
		return new Cigar(ces);
	}
	
	
	public static void addSAMRecordToWriter(SAMFileHeader header, SAMFileWriter writer, Cigar cigar, int probeId, int binId, int binSize, String referenceSeq, String chr, int position, int offset, String binSeq) {
		addSAMRecordToWriter( header, writer, cigar, probeId, binId, binSize, referenceSeq, chr, position, offset, binSeq, 60);
	}
	public static void addSAMRecordToWriter(SAMFileHeader header, SAMFileWriter writer, Cigar cigar, int probeId, int binId, int binSize, String referenceSeq, String chr, int position, int offset, String binSeq, int mappingQuality) {
		/*
		 * Setup some common properties on the sam record
		 */
		for (int i = 0 ; i < binSize ; i++) {
			SAMRecord rec = createSAMRecord(header, cigar,probeId, binId, binSize, referenceSeq, chr, position, offset, binSeq, i, mappingQuality);
			writer.addAlignment(rec);
		}
	}
	
	public static SAMRecord createSAMRecord(SAMFileHeader header, Cigar cigar, int probeId, int binId, int binSize, String referenceSeq, String chr, int position, int offset, String binSeq, int i) {
		return createSAMRecord(header, cigar, probeId, binId, binSize, referenceSeq, chr, position, offset, binSeq, i, 60);
	}
	public static SAMRecord createSAMRecord(SAMFileHeader header, Cigar cigar, int probeId, int binId, int binSize, String referenceSeq, String chr, int position, int offset, String binSeq, int i, int mappingQuality) {
		if (org.qcmg.common.string.StringUtils.isNullOrEmpty(referenceSeq)) {
			throw new IllegalArgumentException("Null or empty reference passed to ClinVarUtil.createSAMRecord: " + referenceSeq);
		}
		if (null == cigar) {
			throw new IllegalArgumentException("Null cigar passed to ClinVarUtil.createSAMRecord");
		}
		
//		if (probeId == 720 && binId == 111261) {
//			logger.info("pid: " + probeId + ", bid: " + binId + ", cigar: " + cigar.toString());
//		}
		SAMRecord rec = new SAMRecord(header);
		rec.setReferenceName(chr);
		rec.setReadString(binSeq);
		rec.setAttribute("ai", probeId);
		rec.setAttribute("bi", binId);
		rec.setMappingQuality(mappingQuality);
		rec.setCigar(cigar);
		/*
		 * Set the alignment start to 1, which is a hack to get around picards calculateMdAndNmTags method which is expecting the entire ref for the chromosome in question
		 * and we only have the amplicon ref seq.
		 * Reset once MD and NM have been calculated and set
		 */
		rec.setAlignmentStart(1);
		
		SequenceUtil.calculateMdAndNmTags(rec, referenceSeq.substring(offset).getBytes(), true, true);
		rec.setAlignmentStart(position + offset);
		
		rec.setReadName(probeId + "_" + binId + "_" + (i + 1) + "_of_" + binSize);
		return rec;
	}
	
	/**
	 * Calls ChrPositionUtils.doChrPositionsOverlap with 100 as the overlap
	 * @param cp1
	 * @param cp2
	 * @return
	 */
	public static boolean doChrPosOverlap(ChrPosition cp1, ChrPosition cp2) {
		return ChrPositionUtils.doChrPositionsOverlap(cp1, cp2, 100);
	}
	
	public static String getMutationString(final int positionInString, final int eventLength, String [] smithWatermanDiffs) {
		
		int expectedStart = positionInString;
		int expectedEnd = expectedStart + eventLength;
		int binSequenceLength = smithWatermanDiffs[2].length();
		
		if (expectedStart < 0) {
			/*
			 * This happens when the bin is shorter than (starts after) the amplicon.
			 * In this situation, we need to return null
			 */
//			logger.info("bin " + bin.getId() + ", in amplicon " + amplicon.getId() + " has an offset of " + offset + ", which means that it starts after the position we are interested in " + vcf.getChrPosition().toIGVString());
			return null;
		}
		if (expectedEnd > binSequenceLength) {
			/*
			 * This happens when the bin is shorter (ends before) the amplicon.
			 * Return null
			 */
//			logger.warn("Expected end: " + expectedEnd + ", is greater than the length of the bin sequence: " + binSequenceLength);
//			for (String s : smithWatermanDiffs) {
//				logger.warn("s: " + s);
//			}
			return null;
		}
		
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
//					} else if ('-' == smithWatermanDiffs[2].charAt(i)) {
//						//deletion
//						additionalOffset--;
//					} else {
//						//!@##$!!! something else-tion
//						logger.warn("Should have found either an indel at position " + i + " in smithWatermanDiffs[0] or smithWatermanDiffs[2]");
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
		}
		
		return noOfSlides;
	}
	
	
	public static int[] getBasicAndLevenshteinEditDistances(CharSequence s, CharSequence t) {
		if (StringUtils.isEmpty(s) || StringUtils.isEmpty(t)) {
			throw new IllegalArgumentException("Null or empty CharSequence passed to ClinVarUtil.getBasicAndLevenshteinEditDistances");
		}
		// do exact match first
		if (s.equals(t)) {
			return new int[]{0,0};			// we have a match - set both distances to 0
		} else {
			// do basic distancing next
			int ed = getBasicEditDistance(s, t);
			return ed > 1 ? new int[]{ ed, StringUtils.getLevenshteinDistance(s,t) } :  new int[]{ ed, ed };
			
		}
	}

	public static Cigar getCigarForIndels(String referenceSequence, String binSeq, String [] swDiffs, Probe p, Bin b) {
		int offset = 0;
		if (swDiffs[0].replaceAll("-","").length() == referenceSequence.length()) {
			offset = 0;
		} else {
			int posOfFistIndel = swDiffs[1].indexOf(" ");
			if (posOfFistIndel < 10) {
				logger.warn("posOfFistIndel is < 10 : " + posOfFistIndel);
			}
			offset = binSeq.indexOf(swDiffs[2].substring(0, posOfFistIndel - 1));
			logger.info("diff length indel at: " + p.getCp().toIGVString() + ", offset: " + offset);
			logger.info("binSeq: " + binSeq);
			for (String s : swDiffs) {
				logger.info(s);
			}
		}
//			indelSameLength++;
			
			List<CigarElement> ces = new ArrayList<>();
//										// get mutations
			List<Pair<Integer, String>> mutations = getPositionRefAndAltFromSW(swDiffs);
			
			int lastPosition = 0;
			for (Pair<Integer, String> mutation : mutations) {
				/*
				 * only care about indels
				 */
				String mutationString = mutation.getRight();
				String [] mutArray = mutationString.split("/");
				
				if (mutArray[0].length() != mutArray[1].length()) {
					int indelPosition = mutation.getLeft().intValue() + 1 + offset;
					
					if (mutArray[0].length() == 1) {
//													// insertion
						if (indelPosition > 0) {
							// create cigar element up to this position
							CigarElement match = new CigarElement(indelPosition - lastPosition, CigarOperator.MATCH_OR_MISMATCH);
							CigarElement insertion = new CigarElement(mutArray[1].length() - 1, CigarOperator.INSERTION);
							ces.add(match);
							ces.add(insertion);
							lastPosition = indelPosition;
						}
					} else {
						// deletion
						if (indelPosition > 0) {
							// create cigar element up to this position
							if (indelPosition - lastPosition > 0) {
								CigarElement match = new CigarElement(indelPosition - lastPosition, CigarOperator.MATCH_OR_MISMATCH);
								ces.add(match);
							}
							CigarElement deletion = new CigarElement(mutArray[0].length() - 1, CigarOperator.DELETION);
							ces.add(deletion);
							lastPosition = indelPosition;
						}
					}
				}
			}
			if (lastPosition + 1 < b.getLength()) {
				CigarElement match = new CigarElement(b.getLength() - (lastPosition + 1), CigarOperator.MATCH_OR_MISMATCH);
				ces.add(match);
			}
			Cigar cigar = new Cigar(ces);
			return cigar;
	}
	
	public static Cigar getCigarForIndels(String referenceSequence, String binSeq, String [] swDiffs, ChrPosition cp, int length) {
		int offset = 0;
		if (swDiffs[0].replaceAll("-","").length() != referenceSequence.length()) {
			int posOfFistIndel = swDiffs[1].indexOf(" ");
			if (posOfFistIndel < 10) {
				logger.warn("posOfFistIndel is < 10 : " + posOfFistIndel);
			}
			offset = binSeq.indexOf(swDiffs[2].substring(0, posOfFistIndel - 1));
			logger.info("diff length indel at: " + cp.toIGVString() + ", offset: " + offset);
			logger.info("binSeq: " + binSeq);
			for (String s : swDiffs) {
				logger.info(s);
			}
		}
		
		List<CigarElement> ces = new ArrayList<>(4);
		// get mutations
		List<Pair<Integer, String>> mutations = getPositionRefAndAltFromSW(swDiffs);
		
		int lastPosition = 0;
		for (Pair<Integer, String> mutation : mutations) {
			/*
			 * only care about indels
			 */
			String [] mutArray = mutation.getRight().split("/");
			
			if (mutArray[0].length() != mutArray[1].length()) {
				int indelPosition = mutation.getLeft().intValue() + 1 + offset;
				
				if (mutArray[0].length() == 1) {
					// insertion
					if (indelPosition > 0) {
						// create cigar element up to this position
						int insertionLength = mutArray[1].length() - 1;
						CigarElement match = new CigarElement(indelPosition - lastPosition, CigarOperator.MATCH_OR_MISMATCH);
						CigarElement insertion = new CigarElement(insertionLength, CigarOperator.INSERTION);
						ces.add(match);
						ces.add(insertion);
						lastPosition = indelPosition + insertionLength;
					}
				} else {
					// deletion
					if (indelPosition > 0) {
						// create cigar element up to this position
						if (indelPosition - lastPosition > 0) {
							CigarElement match = new CigarElement(indelPosition - lastPosition, CigarOperator.MATCH_OR_MISMATCH);
							ces.add(match);
						}
						CigarElement deletion = new CigarElement(mutArray[0].length() - 1, CigarOperator.DELETION);
						ces.add(deletion);
						lastPosition = indelPosition;
					}
				}
			}
		}
		if (lastPosition + 1 < length) {
			CigarElement match = new CigarElement(length - lastPosition, CigarOperator.MATCH_OR_MISMATCH);
//			CigarElement match = new CigarElement(length - (lastPosition + 1), CigarOperator.MATCH_OR_MISMATCH);
			ces.add(match);
		}
		Cigar cigar = new Cigar(ces);
		return cigar;
	}
	
	public static boolean areAllListPositionsWithinBoundary(TLongArrayList list, long start, long end) {
		TLongIterator iter = list.iterator();
		while (iter.hasNext()) {
			long l = iter.next();
			if (l > end || l < start) {
				return false;
			}
		}
		return true;
	}

	public static boolean areAllPositionsClose(Collection<TLongArrayList> collection, Collection<TLongArrayList> collection2,
			final long ampliconStartLongPosition, int buffer) {
		/*
		 * If both collections are null or empty - false
		 */
		if ((null == collection || collection.isEmpty()) && (null == collection2 || collection2.isEmpty())) {
			return false;
		}
		final long start = ampliconStartLongPosition - buffer;
		final long end = ampliconStartLongPosition + buffer;
		
		if (null != collection) {
			for (TLongArrayList list :  collection) {
				if ( ! areAllListPositionsWithinBoundary(list, start, end)) {
					return false;
				}
			}
		}
		if (null != collection2) {
			for (TLongArrayList list :  collection2) {
				if ( ! areAllListPositionsWithinBoundary(list, start, end)) {
					return false;
				}
			}
		}
		return true;
	}
}
