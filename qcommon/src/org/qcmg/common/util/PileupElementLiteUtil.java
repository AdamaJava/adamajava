/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.Rule;

public class PileupElementLiteUtil {
	
	private static final NumberFormat nf = new DecimalFormat("0.##");
	
	public static final char OPEN_BRACKET = '[';
	public static final char CLOSE_BRACKET = ']';
	
	public static int getNovelStarts(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			TIntSet forwardStrandPositions = new TIntHashSet();
			TIntSet reverseStrandPositions = new TIntHashSet();
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					if (((list.get(i)  >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 1) {
						forwardStrandPositions.add((int)list.get(i));
					} else {
						reverseStrandPositions.add((int)list.get(i));
					}
				}
			}
			
			return forwardStrandPositions.size() + reverseStrandPositions.size();
		}
		return 0;
	}
	
	public static TLongIntMap getReadNameHashStartPositionMap(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			if (null != list) {
				TLongIntMap map = new TLongIntHashMap(list.size() * 2);
				for (int i = 0, len = list.size(); i < len ; i += 2) {
					
					int startPosition = (int) list.get(i + 1);
					if (((list.get(i + 1)  >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 0) {
						startPosition = - startPosition;
					}
					map.put(list.get(i), startPosition);
				}
				return map;
			}
		}
		return null;
	}
	
	public static TLongList getReadNameHashes(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			if (null != list) {
				TLongList hashes = new TLongArrayList(list.size());
				for (int i = 0, len = list.size(); i < len ; i += 2) {
					hashes.add(list.get(i));
				}
				return list;
			}
		}
		return null;
	}
	
	public static int getTotalQuality(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			int qualTally = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					qualTally += (byte)(list.get(i) >>> PileupElementLite.QUALITY_BIT_POSITION);
				}
			}
			return qualTally;
		}
		return 0;
	}
	public static int[] getTotalQualityByStrand(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			int qualTallyFS = 0;
			int qualTallyRS = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					if (((list.get(i)  >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 1) {
						qualTallyFS += (byte)(list.get(i) >>> PileupElementLite.QUALITY_BIT_POSITION);
					} else {
						qualTallyRS += (byte)(list.get(i) >>> PileupElementLite.QUALITY_BIT_POSITION);
					}
				}
			}
			return new int[] {qualTallyFS, qualTallyRS};
		}
		return new int[] {0,0};
	}
	
	public static int[] getEndOfReadByStrand(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			int eorTallyFS = 0;
			int eorTallyRS = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					
					if ((list.get(i) & (1L << PileupElementLite.END_OF_READ_BIT_POSITION)) != 0) {
						if (((list.get(i)  >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 1) {
							eorTallyFS++;
						} else {
							eorTallyRS++;
						}
					}
				}
			}
			return new int[] {eorTallyFS, eorTallyRS};
		}
		return new int[] {0,0};
	}
	
	public static int[] getCountAndEndOfReadByStrand(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			int eorTallyFS = 0;
			int eorTallyRS = 0;
			int fsCount = 0, rsCount = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					if (((list.get(i)  >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 1) {
						fsCount++;
						if ((list.get(i) & (1L << PileupElementLite.END_OF_READ_BIT_POSITION)) != 0) {
							eorTallyFS ++;
						}
//						eorTallyFS += (list.get(i) >>> PileupElementLite.END_OF_READ_BIT_POSITION);
					} else {
						rsCount++;
						if ((list.get(i) & (1L << PileupElementLite.END_OF_READ_BIT_POSITION)) != 0) {
							eorTallyRS ++;
						}
//						eorTallyRS += (list.get(i) >>> PileupElementLite.END_OF_READ_BIT_POSITION);
					}
				}
			}
			return new int[] {fsCount, rsCount, eorTallyFS, eorTallyRS};
		}
		return new int[] {0,0};
	}
	
	public static boolean onBothStrands(PileupElementLite pel) {
		if (null != pel) {
			TLongList list = pel.getData();
			boolean fs = false;
			boolean rs = false;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					
					if (((list.get(i) >>> PileupElementLite.STRAND_BIT_POSITION) & 1) == 1l) {
						fs = true;
					} else {
						rs = true;
					}
					
					if (fs && rs) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static TIntList getDetailsFromCombinedList(TIntList combinedList, int divider, int remainder) {
		if (null == combinedList || combinedList.isEmpty()) {
			return new TIntArrayList(0);
		}
		/*
		 * read ids are the even numbered elements in this list (0 based)
		 */
		TIntList l = new TIntArrayList();
		for (int i = 0, len = combinedList.size() ; i < len ; i++) {
			if (i % divider == remainder) {
				l.add(combinedList.get(i));
			}
		}
		return l;
	}
	public static TIntList getDetailsFromCombinedList(TIntList combinedList, int divider, int remainder, boolean forwardStrand) {
		if (null == combinedList || combinedList.isEmpty()) {
			return new TIntArrayList(0);
		}
		/*
		 * read ids are the even numbered elements in this list (0 based)
		 */
		TIntList l = new TIntArrayList();
		for (int i = 0, len = combinedList.size() ; i < len ; i++) {
			if (i % divider == remainder) {
				l.add(combinedList.get(i));
			}
		}
		return l;
	}
	
	public static TIntIntMap getDetailsFromCombinedListInMap(TIntList combinedList, int divider, int keyRemainder, int valueRemainder) {
		if (null == combinedList || combinedList.isEmpty()) {
			return new TIntIntHashMap(0);
		}
		/*
		 * read ids are the even numbered elements in this list (0 based)
		 */
		TIntIntMap l = new TIntIntHashMap();
		int key = 0, value = 0;
		for (int i = 0, len = combinedList.size() ; i < len ; i++) {
			if (i % divider == keyRemainder) {
				key =combinedList.get(i);
			}
			if (i % divider == valueRemainder) {
				value =combinedList.get(i); 
			}
			if (key != 0 && value != 0) {
				l.put(key, value);
				key = 0;
				value =0;
			}
		}
		return l;
	}
	
	public static boolean[] isAccumulatorAKeeper(final Accumulator acc, final char ref, final Rule rule, final int percentage) {
		boolean [] results = new boolean[2];
		
		if (null == acc || null == rule) return results;
		
		/*
		 * get largest base based on total count
		 */
		int [] largestAltStats = AccumulatorUtils.getLargestVariant(acc, ref); 
		
		if (null == largestAltStats) return results;
		
		
		int coverage = acc.getCoverage();
		int variantCount = largestAltStats[AccumulatorUtils.FORWARD_STRAND_COUNT] + largestAltStats[AccumulatorUtils.REVERSE_STRAND_COUNT];
		int [] totalQualityScoreArray = AccumulatorUtils.getTotalQualityByStrand(acc);
		int totalQualityScore = totalQualityScoreArray[0] + totalQualityScoreArray[1];
		int variantQualityScore = largestAltStats[AccumulatorUtils.FORWARD_STRAND_QUALITY] + largestAltStats[AccumulatorUtils.REVERSE_STRAND_QUALITY];
		
		if (passesCountCheck(variantCount, coverage, rule) 
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage)) {
			
			results[0] = true;	// first pass
			
		} else if (passesCountCheck(variantCount, coverage, rule, true) 
				&& (largestAltStats[AccumulatorUtils.FORWARD_STRAND_COUNT] > 0 && largestAltStats[AccumulatorUtils.REVERSE_STRAND_COUNT] > 0) 
				&& passesWeightedVotingCheck(totalQualityScore, variantQualityScore, percentage, true)) {
			
			results[1] = true;	// second pass
		}
		return results;
	}
	
	public static boolean passesCountCheck(int count, int coverage, Rule rule) {
		return passesCountCheck(count, coverage, rule, false);
	}
	
	public static boolean passesCountCheck(int count, int coverage, Rule rule, boolean secondPass) {
		if (null == rule) return false;
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
		if ( ! onBothStrands(pel)) {
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
	
	public static String toObservedAlleleByStrand(final PileupElementLite pel, final String base) {
		int forwardCount = pel.getForwardCount();
		int reverseCount = pel.getReverseCount();
		
		StringBuilder sb = new StringBuilder(base);
		sb.append(forwardCount).append(OPEN_BRACKET);
		int [] qualsByStrand = getTotalQualityByStrand(pel);
		float qual = forwardCount == 0 ? 0.0f : (float)qualsByStrand[0] / forwardCount; 
		sb.append(nf.format(qual));
		sb.append(CLOSE_BRACKET);
		sb.append(reverseCount).append(OPEN_BRACKET);
		qual = reverseCount == 0 ? 0.0f : (float)qualsByStrand[1] / reverseCount; 
		sb.append(nf.format(qual));
		sb.append(CLOSE_BRACKET);
		return sb.toString();
	}
}
