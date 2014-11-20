/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.qcmg.common.util.PileupElementLiteUtil;


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
	private static final char COMMA = ',';
	
	public static final int END_OF_READ_DISTANCE = 5;
	
	private PileupElementLite A;
	private PileupElementLite C;
	private PileupElementLite G;
	private PileupElementLite T;
	
	private int nCount;
	
	private final int position;
	private final int positionOfInterestANDEndOfReadLength;
	private final int positionOfInterestMINUSEndOfReadLength;
	
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
		this.positionOfInterestANDEndOfReadLength = position + END_OF_READ_DISTANCE;
		this.positionOfInterestMINUSEndOfReadLength = position - END_OF_READ_DISTANCE;
	}
	
	public int getPosition() {
		return position;
	}

	public void addUnfilteredBase(final byte base) {
		switch (base) {
		case 'A': if (unfilteredA) {
			unfilteredA2 = true;
		} else {
			unfilteredA = true;
		}
		break;
		case 'C':  if (unfilteredC) {
			unfilteredC2 = true;
		} else {
			unfilteredC = true;
		}
		break;
		case 'G': if (unfilteredG) {
			unfilteredG2 = true;
		} else {
			unfilteredG = true;
		}
		break;
		case 'T': if (unfilteredT) {
			unfilteredT2 = true;
		} else {
			unfilteredT = true;
		}
		break;
		}
	}
	
	public void addBase(final byte base, final byte qual, final boolean forwardStrand, final int startPosition, final int position, final int endPosition, long readId) {
		
		if (this.position != position) throw new IllegalArgumentException("Attempt to add data for wrong position. " +
				"This position: " + this.position + ", position: " + position);
		
		boolean endOfRead = startPosition > positionOfInterestMINUSEndOfReadLength 
				|| endPosition < positionOfInterestANDEndOfReadLength;
		
		// if on the reverse strand, start position is actiually endPosition
		int startPositionToUse = forwardStrand ? startPosition : endPosition;
		
		switch (base) {
		case 'A': 
			if (null == A) A = new PileupElementLite();
			update(A, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case 'C': 
			if (null == C) C = new PileupElementLite();
			update(C, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case 'G': 
			if (null == G) G = new PileupElementLite();
			update(G, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case 'T': 
			if (null == T) T = new PileupElementLite();
			update(T, qual, forwardStrand, startPositionToUse, endOfRead, readId);
			break;
		case 'N':
			nCount++;
			break;
		}
	}
	
	private void update(final PileupElementLite peLite, final byte qual, final boolean forwardStrand, final int startPosition, boolean endOfRead, long readId) {
		if (forwardStrand)
			peLite.addForwardQuality(qual, startPosition, readId, endOfRead);
		else
			peLite.addReverseQuality(qual, startPosition, readId, endOfRead);
	}
	
	public String getPileup() {
		StringBuilder pileup = new StringBuilder();
		if (null != A) {
			for (int i = 0 , count = A.getForwardCount() ; i < count ; i++)
				pileup.append('A');
			for (int i = 0 , count = A.getReverseCount() ; i < count ; i++)
				pileup.append('a');
		}
		if (null != C) {
			for (int i = 0 , count = C.getForwardCount() ; i < count ; i++)
				pileup.append('C');
			for (int i = 0 , count = C.getReverseCount() ; i < count ; i++)
				pileup.append('c');
		}
		if (null != G) {
			for (int i = 0 , count = G.getForwardCount() ; i < count ; i++)
				pileup.append('G');
			for (int i = 0 , count = G.getReverseCount() ; i < count ; i++)
				pileup.append('g');
		}
		if (null != T) {
			for (int i = 0 , count = T.getForwardCount() ; i < count ; i++)
				pileup.append('T');
			for (int i = 0 , count = T.getReverseCount() ; i < count ; i++)
				pileup.append('t');
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
		case 'A': 
			return  getNS(A);
		case 'C': 
			return  getNS(C);
		case 'G': 
			return  getNS(G);
		case 'T': 
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
		return pileup.length() + "\t" + pileup + "\t" + getPileupQualities();
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
			equalsToRef = ref == 'A';
			for (int i = 0 , count = A.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : 'A');
			for (int i = 0 , count = A.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : 'a');
		}
		if (null != C) {
			equalsToRef = ref == 'C';
			for (int i = 0 , count = C.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : 'C');
			for (int i = 0 , count = C.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : 'c');
		}
		if (null != G) {
			equalsToRef = ref == 'G';
			for (int i = 0 , count = G.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : 'G');
			for (int i = 0 , count = G.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : 'g');
		}
		if (null != T) {
			equalsToRef = ref == 'T';
			for (int i = 0 , count = T.getForwardCount() ; i < count ; i++)
				pileup.append(equalsToRef ? DOT : 'T');
			for (int i = 0 , count = T.getReverseCount() ; i < count ; i++)
				pileup.append(equalsToRef ? COMMA : 't');
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
		
		if (null != A) return 'A';
		if (null != C) return 'C';
		if (null != G) return 'G';
		if (null != T) return 'T';
		return '\u0000';
	}
	
	public String getUnfilteredPileup() {
		StringBuilder pileup = new StringBuilder();
		if (unfilteredA2) pileup.append('A');
		if (unfilteredC2) pileup.append('C');
		if (unfilteredG2) pileup.append('G');
		if (unfilteredT2) pileup.append('T');
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
		List<PileupElementLite> pel = new ArrayList<PileupElementLite>();
		if (null != A && ref != 'A') pel.add(A);
		if (null != C && ref != 'C') pel.add(C);
		if (null != G && ref != 'G') pel.add(G);
		if (null != T && ref != 'T') pel.add(T);
		
		if (pel.size() > 1) Collections.sort(pel);
		else if (pel.isEmpty()) return null;
		
		return pel.get(0);
	}
	
	public int getBaseCountForBase(char base) {
		if (null != A && base == 'A') return A.getTotalCount();
		if (null != C && base == 'C') return C.getTotalCount();
		if (null != G && base == 'G') return G.getTotalCount();
		if (null != T && base == 'T') return T.getTotalCount();
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
		if (null != A) pileup.append("A");
		if (null != C) pileup.append("C");
		if (null != G) pileup.append("G");
		if (null != T) pileup.append("T");
		return pileup.toString();
	}
	
	private char getCharFromPel(PileupElementLite pel) {
		if (A == pel) return 'A';
		if (C == pel) return 'C';
		if (G == pel) return 'G';
		if (T == pel) return 'T';
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
		
		List<PileupElementLite> pels = new ArrayList<PileupElementLite>();
		
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
			Collections.sort(pels, new Comparator<PileupElementLite>(){
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
			pileup.append(PileupElementLiteUtil.toSummaryString(A, "A"));
		}
		if (null != C) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(C, "C"));
		}
		if (null != G) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(G, "G"));
		}
		if (null != T) {
			if (pileup.length() > 0) pileup.append(COMMA);
			pileup.append(PileupElementLiteUtil.toSummaryString(T, "T"));
		}
		
		return pileup.toString();
	}
	
	public String getReadIdsPerAllele() {
		StringBuilder sb = new StringBuilder();
		if (null != A) {
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(A, "A"));
		}
		if (null != C) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(C, "C"));
		}
		if (null != G) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(G, "G"));
		}
		if (null != T) {
			if (sb.length() > 0) sb.append(COMMA);
			sb.append(PileupElementLiteUtil.getBaseAndReadIds(T, "T"));
		}
		
		return sb.toString();
		
	}
	
	public Map<Long, Character> getReadIdBaseMap() {
		Map<Long, Character> map = new HashMap<>();
		
		if (null != A) {
			Queue<Long> ids = A.getForwardReadIds();
			if (ids != null) {
				for (Long s : ids) {
					map.put(s, 'A');
				}
			}
			 ids = A.getReverseReadIds();
				if (ids != null) {
					for (Long s : ids) {
						map.put(s, 'a');
					}
				}
		}
		if (null != C) {
			Queue<Long> ids = C.getForwardReadIds();
			if (ids != null) {
				for (Long s : ids) {
					map.put(s, 'C');
				}
			}
			 ids = C.getReverseReadIds();
				if (ids != null) {
					for (Long s : ids) {
						map.put(s, 'c');
					}
				}
		}
		if (null != G) {
			Queue<Long> ids = G.getForwardReadIds();
			if (ids != null) {
				for (Long s : ids) {
					map.put(s, 'G');
				}
			}
			 ids = G.getReverseReadIds();
				if (ids != null) {
					for (Long s : ids) {
						map.put(s, 'g');
					}
				}
		}
		if (null != T) {
			Queue<Long> ids = T.getForwardReadIds();
			if (ids != null) {
				for (Long s : ids) {
					map.put(s, 'T');
				}
			}
			 ids = T.getReverseReadIds();
				if (ids != null) {
					for (Long s : ids) {
						map.put(s, 't');
					}
				}
		}
		
		return map;
	}
	
}
