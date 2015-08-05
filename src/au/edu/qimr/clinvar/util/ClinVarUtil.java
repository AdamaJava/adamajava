package au.edu.qimr.clinvar.util;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TLongIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

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
	
	public static long[] getBestStartPosition(long [][] tilePositions, int tileLength, int indelOffset) {
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
				return reduceStartPositionsAndTileCount(positionAndTiles);
			}
		}
		return reduceStartPositionsAndTileCount(positionAndTiles);
	}
	
	/**
	 * Calculates the sw score for each entry in the map.
	 * If there is a winner, return the position to which it corresponds.
	 * If there is no winner (ie. empty map, or more than 1 position share the top score), return null 
	 * 
	 * @param scores
	 * @return
	 */
	public static ChrPosition getPositionWithBestScore(Map<ChrPosition, String[]> scores) {
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
		List<ChrPosition> maxScoringPositions = scoresToPositionMap.lastEntry().getValue();
		return maxScoringPositions.size() == 1 ? maxScoringPositions.get(0) : null;
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
	public static long[] reduceStartPositionsAndTileCount(TLongIntMap positionAndTiles) {
		int [] tileCounts = positionAndTiles.values();
		int tileCountsLength = tileCounts.length;
		if (tileCountsLength == 0) {
			return new long[]{0,0};
		}
		Arrays.sort(tileCounts);
		final int bestTileCount = tileCounts[tileCountsLength -1];
		
		final TLongList results = new TLongArrayList();
		positionAndTiles.forEachEntry(new TLongIntProcedure() {
			@Override
			public boolean execute(long l, int i) {
				if (i == bestTileCount) {
					results.add(l);
					results.add(i);
				}
				return true;
			}
		});
		return results.toArray();
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
		String swBinSeq = diffs[2].replaceAll("-", "");
		int lengthDiff = binSeq.length() - swBinSeq.length();
		if (lengthDiff > 0) {
//			logger.warn("Missing data in sw diffs. lengthDiff:  " + lengthDiff);
			
			String swRef = diffs[0].replaceAll("-", "");
			
			if (binSeq.startsWith(swBinSeq)) {
				
				// need to get the last few bases
				String missingBinSeqBases = binSeq.substring(binSeq.length()  - lengthDiff);
				
				int refIndex = ref.indexOf(swRef); 
				if (refIndex > -1) {
					int positionInRef = refIndex + swRef.length();
					if (ref.length() < positionInRef + lengthDiff) {
						logger.warn("ref.length() < positionInRef + lengthDiff");
					} else {
						
						String missingRefBases = ref.substring(positionInRef, positionInRef + lengthDiff);
						
						
						if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
							logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
							// oh dear
						} else {
							logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
							diffs[0] += missingRefBases;
							for (int i = 0 ; i < lengthDiff ; i++) {
								diffs[1] += ".";
							}
							diffs[2] += missingBinSeqBases;
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
					String missingRefBases = ref.substring(refIndex - lengthDiff, refIndex);
					
					if (missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length()) {
						logger.info("missingBinSeqBases.equals(missingRefBases) || missingBinSeqBases.length() != missingRefBases.length(), missingBinSeqBases: " + missingBinSeqBases + ", missingRefBases: " + missingRefBases);
						// oh dear
					} else {
						logger.info("adding " + missingRefBases + ">" + missingBinSeqBases + " to sw diffs");
						diffs[0] = missingRefBases + diffs[0];
						for (int i = 0 ; i < lengthDiff ; i++) {
							diffs[1] = "." + diffs[1];
						}
						diffs[2] = missingBinSeqBases + diffs[2];
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
	public static String getAmpliconDistribution(VcfRecord vcf, List<Probe> overlappingProbes, 
			Map<Probe, List<Bin>> probeBinDist, int minBinSize) {
		return getAmpliconDistribution(vcf, overlappingProbes, probeBinDist, minBinSize, false);
	}
	
	public static String getAmpliconDistribution(VcfRecord vcf, List<Probe> overlappingProbes, 
			Map<Probe, List<Bin>> probeBinDist, int minBinSize, boolean diagnosticMode) {
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
		String probeRef = amplicon.getReferenceSequence();
		// remove any indel characters - only checking
		String swRef = smithWatermanDiffs[0].replace("-", "");
		int offset = probeRef.indexOf(swRef);
		
		if ( offset == -1) {
			logger.warn("probeRef.indexOf(swRef) == -1!!! probe (id:bin.id: " + amplicon.getId() + ":" + bin.getId() + ") , probe ref: " + probeRef + ", swRef: " + swRef);
		}
		
		int length = vcf.getChrPosition().getLength();
		int positionInString = getZeroBasedPositionInString(vcf.getChrPosition().getPosition(), amplicon.getCp().getPosition() + offset);
		
//		if (amplicon.getId() == 241) {
//			logger.info("positionInString: " + positionInString +", from offset: " + offset + ", vcf.getChrPosition().getPosition(): " + vcf.getChrPosition().getPosition() +", amplicon.getCp().getPosition(): " + amplicon.getCp().getPosition());
//		}
		
		return getMutationString(positionInString, length, smithWatermanDiffs);
		
	}
	
	public static boolean doChrPosOverlap(ChrPosition cp1, ChrPosition cp2) {
		/*
		 * Use a consistent buffer value
		 */
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
			logger.warn("Expected end: " + expectedEnd + ", is greater than the length of the bin sequence: " + binSequenceLength);
			for (String s : smithWatermanDiffs) {
				logger.warn("s: " + s);
			}
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
}
