/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntCharMap;
import gnu.trove.map.hash.TIntCharHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.PileupElementLiteUtil;

import static org.qcmg.common.util.Constants.COMMA;


/**
 * This class aims to track all the necessary information at a loci required by qSNP to make a call on its eligibility as being a position of interest
 * with regard to snp calling
 * 
 * NOT THREAD SAFE
 * 
 * @author oholmes
 *
 */
public class Accumulator {
	
	private static final char DOT = '.';
	public static final int END_OF_READ_DISTANCE = 5;
	
	private PileupElementLite A;
	private PileupElementLite C;
	private PileupElementLite G;
	private PileupElementLite T;
	
	public static final char A_CHAR = 'A';
	public static final char C_CHAR = 'C';
	public static final char G_CHAR = 'G';
	public static final char T_CHAR = 'T';
	
	public static final byte A_BYTE = 'A';
	public static final byte C_BYTE = 'C';
	public static final byte G_BYTE = 'G';
	public static final byte T_BYTE = 'T';
	
	public static final char A_CHAR_LC = 'a';
	public static final char C_CHAR_LC = 'c';
	public static final char G_CHAR_LC = 'g';
	public static final char T_CHAR_LC = 't';
	
	public static final String A_STRING = "A";
	public static final String C_STRING = "C";
	public static final String G_STRING = "G";
	public static final String T_STRING = "T";
	
	
	private int nCount;
	private final int position;
	
	private boolean unfilteredA;
	private boolean unfilteredC;
	private boolean unfilteredG;
	private boolean unfilteredT;
	// now need 2 unfiltered to trigger the MIUN flag - any more and move to int...
	private boolean unfilteredA2;
	private boolean unfilteredC2;
	private boolean unfilteredG2;
	private boolean unfilteredT2;
	
	public Accumulator(int position) {
		this.position = position;
	}
	
	public int getPosition() {
		return position;
	}

	public void addUnfilteredBase(final byte base) {
		switch (base) {
		case A_BYTE: if (unfilteredA) {
			unfilteredA2 = true;
		} else {
			unfilteredA = true;
		}
		break;
		case C_BYTE:  if (unfilteredC) {
			unfilteredC2 = true;
		} else {
			unfilteredC = true;
		}
		break;
		case G_BYTE: if (unfilteredG) {
			unfilteredG2 = true;
		} else {
			unfilteredG = true;
		}
		break;
		case T_BYTE: if (unfilteredT) {
			unfilteredT2 = true;
		} else {
			unfilteredT = true;
		}
		break;
		}
	}
	
	public void addBase(final byte base, final byte qual, final boolean forwardStrand, final int startPosition, final int position, final int endPosition, int readId) {
		
		if (this.position != position) throw new IllegalArgumentException("Attempt to add data for wrong position. " +
				"This position: " + this.position + ", position: " + position);
		
		boolean endOfRead = startPosition > (position - END_OF_READ_DISTANCE) 
				|| endPosition < (position + END_OF_READ_DISTANCE);
		
		// if on the reverse strand, start position is actually endPosition
		int startPositionToUse = forwardStrand ? startPosition : endPosition;
		
		switch (base) {
		case A_BYTE: 
			if (null == A) A = new PileupElementLite();
			update(A, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case C_BYTE: 
			if (null == C) C = new PileupElementLite();
			update(C, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case G_BYTE: 
			if (null == G) G = new PileupElementLite();
			update(G, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case T_BYTE: 
			if (null == T) T = new PileupElementLite();
			update(T, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case 'N':
			nCount++;
			break;
		}
	}
	
	private void update(final PileupElementLite peLite, final byte qual, final boolean forwardStrand, final int startPosition, boolean endOfRead, int readId) {
		if (forwardStrand)
			peLite.addForwardQuality(qual, startPosition, readId, endOfRead);
		else
			peLite.addReverseQuality(qual, startPosition, readId, endOfRead);
	}
	
	public String getPileup() {
		StringBuilder pileup = new StringBuilder();
		if (null != A) {
			for (int i = 0 , count = A.getForwardCount() ; i < count ; i++)
				pileup.append(A_CHAR);
			for (int i = 0 , count = A.getReverseCount() ; i < count ; i++)
				pileup.append(A_CHAR_LC);
		}
		if (null != C) {
			for (int i = 0 , count = C.getForwardCount() ; i < count ; i++)
				pileup.append(C_CHAR);
			for (int i = 0 , count = C.getReverseCount() ; i < count ; i++)
				pileup.append(C_CHAR_LC);
		}
		if (null != G) {
			for (int i = 0 , count = G.getForwardCount() ; i < count ; i++)
				pileup.append(G_CHAR);
			for (int i = 0 , count = G.getReverseCount() ; i < count ; i++)
				pileup.append(G_CHAR_LC);
		}
		if (null != T) {
			for (int i = 0 , count = T.getForwardCount() ; i < count ; i++)
				pileup.append(T_CHAR);
			for (int i = 0 , count = T.getReverseCount() ; i < count ; i++)
				pileup.append(T_CHAR);
		}
		
		return pileup.toString();
	}
	
	public String getPileupQualities() {
		StringBuilder qualities = new StringBuilder();
		if (null != A) {
			qualities.append(A.getForwardQualitiesAsString());
			qualities.append(A.getReverseQualitiesAsString());
		}
		if (null != C) {
			qualities.append(C.getForwardQualitiesAsString());
			qualities.append(C.getReverseQualitiesAsString());
		}
		if (null != G) {
			qualities.append(G.getForwardQualitiesAsString());
			qualities.append(G.getReverseQualitiesAsString());
		}
		if (null != T) {
			qualities.append(T.getForwardQualitiesAsString());
			qualities.append(T.getReverseQualitiesAsString());
		}
		
		return qualities.toString();
	}
	
	private int getNS(PileupElementLite peLite) {
		if (null != peLite) return peLite.getNovelStartCount();
		return 0;
	}
	
	public int getNovelStartsCountForBase(final char base) {
		switch (base) {
		case A_CHAR: 
			return  getNS(A);
		case C_CHAR: 
			return  getNS(C);
		case G_CHAR: 
			return  getNS(G);
		case T_CHAR: 
			return  getNS(T);
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return position + ":" + getPileup();
	}
	
	public String toPileupString(String pileup) {
		if (null == pileup)
			pileup = getPileup();
		return pileup.length() + Constants.TAB + pileup + Constants.TAB + getPileupQualities();
	}
	
	/**
	 * Use '.' and ',' rather than the reference in the pileup string
	 * @param ref
	 * @return
	 */
	public String toSamtoolsPileupString(char ref) {
		ref = Character.toUpperCase(ref);
		StringBuilder pileup = new StringBuilder();
		boolean equalsToRef = false;
		if (null != A) {
			equalsToRef = ref == A_CHAR;
			for (int i = 0 , count = A.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : A_CHAR);
			for (int i = 0 , count = A.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : A_CHAR_LC);
		}
		if (null != C) {
			equalsToRef = ref == C_CHAR;
			for (int i = 0 , count = C.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : C_CHAR);
			for (int i = 0 , count = C.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : C_CHAR_LC);
		}
		if (null != G) {
			equalsToRef = ref == G_CHAR;
			for (int i = 0 , count = G.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : G_CHAR);
			for (int i = 0 , count = G.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : G_CHAR_LC);
		}
		if (null != T) {
			equalsToRef = ref == T_CHAR;
			for (int i = 0 , count = T.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : T_CHAR);
			for (int i = 0 , count = T.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : T_CHAR_LC);
		}
		
		return pileup.toString();
	}
	
	public boolean containsMultipleAlleles() {
		int differentBases = 0;
//		if (null != DOT) differentBases++;
		if (null != A) differentBases++;
		if (null != C) differentBases++;
		if (null != G) differentBases++;
		if (null != T) differentBases++;
		return differentBases > 1;
	}
	
	/**
	 * This method is only called if containsMultipleAlleles returns false.
	 * Meaning that there should only be a single base being represented at this position.
	 * @return
	 */
	public char getBase() {
		if (containsMultipleAlleles()) 
			throw new UnsupportedOperationException(
					"Accumulator.getBase() called when there is more than 1 base at this position");
		
		if (null != A) return A_CHAR;
		if (null != C) return C_CHAR;
		if (null != G) return G_CHAR;
		if (null != T) return T_CHAR;
		return '\u0000';
	}
	
	public String getUnfilteredPileup() {
		StringBuilder pileup = new StringBuilder();
		if (unfilteredA2) pileup.append(A_CHAR);
		if (unfilteredC2) pileup.append(C_CHAR);
		if (unfilteredG2) pileup.append(G_CHAR);
		if (unfilteredT2) pileup.append(T_CHAR);
		return pileup.toString();
	}

	public int getCoverage() {
		int coverage = nCount;
		if (null != A) coverage += A.getTotalCount();
		if (null != C) coverage += C.getTotalCount();
		if (null != G) coverage += G.getTotalCount();
		if (null != T) coverage += T.getTotalCount();
		return coverage; 
	}
	
	public PileupElementLite getLargestVariant(char ref) {
		List<PileupElementLite> pel = new ArrayList<PileupElementLite>(6);
		if (null != A && ref != A_CHAR) pel.add(A);
		if (null != C && ref != C_CHAR) pel.add(C);
		if (null != G && ref != G_CHAR) pel.add(G);
		if (null != T && ref != T_CHAR) pel.add(T);
		
		if (pel.size() > 1) {
			pel.sort(null);
		}
		else if (pel.isEmpty()) return null;
		
		return pel.get(0);
	}
	
	public int getBaseCountForBase(char base) {
		if (null != A && base == A_CHAR) return A.getTotalCount();
		if (null != C && base == C_CHAR) return C.getTotalCount();
		if (null != G && base == G_CHAR) return G.getTotalCount();
		if (null != T && base == T_CHAR) return T.getTotalCount();
		return 0;
	}
	
	public int getTotalQualityScore() {
		int qualityScore = 0;
		if (null != A) qualityScore += A.getTotalQualityScore();
		if (null != C) qualityScore += C.getTotalQualityScore();
		if (null != G) qualityScore += G.getTotalQualityScore();
		if (null != T) qualityScore += T.getTotalQualityScore();
		return qualityScore; 
	}
	
	/**
	 * Just return a single instance of each base seen at this position
	 * @return
	 */
	public String getCompressedPileup() {
		StringBuilder pileup = new StringBuilder();
		if (null != A) pileup.append(A_STRING);
		if (null != C) pileup.append(C_STRING);
		if (null != G) pileup.append(G_STRING);
		if (null != T) pileup.append(T_STRING);
		return pileup.toString();
	}
	
	private char getCharFromPel(PileupElementLite pel) {
		if (A == pel) return A_CHAR;
		if (C == pel) return C_CHAR;
		if (G == pel) return G_CHAR;
		if (T == pel) return T_CHAR;
		return '\u0000';
	}
	
	public static boolean canContributeToGenotype(final PileupElementLite pel, final int coverage, final int totalQuality, final Rule rule, final boolean secondPass, final double percentage) {
		return 
				null != pel
				&& ( ! secondPass || pel.isFoundOnBothStrands())
				&& PileupElementLiteUtil.passesCountCheck(pel.getTotalCount(), coverage, rule, secondPass) 
				&& PileupElementLiteUtil.passesWeightedVotingCheck(totalQuality, pel.getTotalQualityScore(), percentage, secondPass);
	}
	
	public GenotypeEnum getGenotype(final char ref, final Rule rule, final boolean secondPass, final double percentage) {
		final int coverage = getCoverage();
		final int totalQuality = getTotalQualityScore();
		
		List<PileupElementLite> pels = new ArrayList<PileupElementLite>(6);
		
		if (canContributeToGenotype(A, coverage, totalQuality, rule, secondPass, percentage))
			pels.add(A);
		if (canContributeToGenotype(C, coverage, totalQuality, rule, secondPass, percentage))
			pels.add(C);
		if (canContributeToGenotype(G, coverage, totalQuality, rule, secondPass, percentage))
			pels.add(G);
		if (canContributeToGenotype(T, coverage, totalQuality, rule, secondPass, percentage))
			pels.add(T);
		
		if (pels.isEmpty()) return null;
		
		if (pels.size() == 1) {
			char c = getCharFromPel(pels.get(0));
			return GenotypeEnum.getGenotypeEnum(c,c);
		} else if (pels.size() == 2) {
			char c1 = getCharFromPel(pels.get(0));
			char c2 = getCharFromPel(pels.get(1));
			return GenotypeEnum.getGenotypeEnum(c1,c2);
		} else  {
			//3 or more in collection
			// re-sort according to count, reference, and base qualities
			pels.sort(new Comparator<PileupElementLite>(){
				@Override
				public int compare(PileupElementLite o1, PileupElementLite o2) {
					int diff = o1.compareTo(o2);
					if (diff != 0) return diff;
					if (ref == getCharFromPel(o2)) 
						return 1;
					else if (ref == getCharFromPel(o1)) 
						return -1;
					return o2.getTotalQualityScore() - o1.getTotalQualityScore();
				}
			});
			char c1 = getCharFromPel(pels.get(0));
			char c2 = getCharFromPel(pels.get(1));
			return GenotypeEnum.getGenotypeEnum(c1,c2);
		}
	}
	
	public String getPileupElementString() {
		StringBuilder pileup = new StringBuilder();
		if (null != A) {
			pileup.append(PileupElementLiteUtil.toSummaryString(A, A_STRING));
		}
		if (null != C) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(C, C_STRING));
		}
		if (null != G) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(G, G_STRING));
		}
		if (null != T) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(T, T_STRING));
		}
		
		return pileup.toString();
	}
	
	public String getAlleleicFrequencies(char ref, String alts) {
		int refCount = getBaseCountForBase(ref);
		
		if (null == alts) {
			/*
			 * return ref count and other count
			 */
			return refCount + Constants.COMMA_STRING +(getCoverage() - refCount);
		} else if (alts.length() == 1) {
			char a = alts.charAt(0);
			int altCount = ref != a ? getBaseCountForBase(alts.charAt(0)) : 0;
			return refCount + Constants.COMMA_STRING + altCount + Constants.COMMA_STRING + (getCoverage() - refCount - altCount);
			
		} else {
			/*
			 * more than 1 alt
			 */
			String [] altsArray = alts.split(Constants.COMMA_STRING);
			String [] altCounts = new String[altsArray.length];
			int i = 0;
			int tally = 0;
			for (String s : altsArray) {
				if (s.length() != 1 || s.charAt(0) == ref) {
					/*
					 * hmmm
					 */
				} else {
					int count = getBaseCountForBase(s.charAt(0));
					tally += count;
					altCounts[i++] = count + "";
				}
			}
			return refCount + Constants.COMMA_STRING + Arrays.stream(altCounts).collect(Collectors.joining(Constants.COMMA_STRING)) + Constants.COMMA_STRING + (getCoverage() - refCount - tally);
		}
	}
	
	public String getObservedAllelesByStrand() {
		StringBuilder pileup = new StringBuilder();
		if (null != A) {
			pileup.append(PileupElementLiteUtil.toObservedAlleleByStrand(A, A_STRING));
		}
		if (null != C) {
			if (pileup.length() > 0) pileup.append(Constants.SEMI_COLON);
			pileup.append(PileupElementLiteUtil.toObservedAlleleByStrand(C, C_STRING));
		}
		if (null != G) {
			if (pileup.length() > 0) pileup.append(Constants.SEMI_COLON);
			pileup.append(PileupElementLiteUtil.toObservedAlleleByStrand(G, G_STRING));
		}
		if (null != T) {
			if (pileup.length() > 0) pileup.append(Constants.SEMI_COLON);
			pileup.append(PileupElementLiteUtil.toObservedAlleleByStrand(T, T_STRING));
		}
		
		return pileup.toString();
	}
	
	public String getReadIdsPerAllele() {
		StringBuilder sb = new StringBuilder();
		if (null != A) {
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(A, A_STRING));
		}
		if (null != C) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(C, C_STRING));
		}
		if (null != G) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(G, G_STRING));
		}
		if (null != T) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(T, T_STRING));
		}
		
		return sb.toString();
		
	}
	
	private void updateMap(TIntCharMap map, TIntIterator iter, char c) {
		while(iter.hasNext()) {
			map.put(iter.next(), c);
		}
	}
	
	public TIntCharMap getReadIdBaseMap() {
		final TIntCharMap map = new TIntCharHashMap();
		
		if (null != A) {
			TIntArrayList ids = A.getForwardReadIds();
			if (ids != null) {
				updateMap(map, ids.iterator(), A_CHAR);
			}
			
			ids = A.getReverseReadIds();
			
			if (ids != null) {
				updateMap(map, ids.iterator(), A_CHAR_LC);
			}
		}
		
		if (null != C) {
			TIntArrayList ids = C.getForwardReadIds();
			if (ids != null) {
				updateMap(map, ids.iterator(), C_CHAR);
			}
			
			 ids = C.getReverseReadIds();
			 
			if (ids != null) {
				updateMap(map, ids.iterator(), C_CHAR_LC);
			}
		}
		
		if (null != G) {
			TIntArrayList ids = G.getForwardReadIds();
			if (ids != null) {
				updateMap(map, ids.iterator(), G_CHAR);
			}
			 ids = G.getReverseReadIds();
			 
			if (ids != null) {
				updateMap(map, ids.iterator(), G_CHAR_LC);
			}
		}
		
		if (null != T) {
			TIntArrayList ids = T.getForwardReadIds();
			if (ids != null) {
				updateMap(map, ids.iterator(), T_CHAR);
			}
			 ids = T.getReverseReadIds();
			if (ids != null) {
				updateMap(map, ids.iterator(), T_CHAR_LC);
			}
		}
		return map;
	}
	
}
