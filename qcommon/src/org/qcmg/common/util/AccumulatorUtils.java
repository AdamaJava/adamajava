package org.qcmg.common.util;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TCharObjectMap;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TLongCharHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;

public class AccumulatorUtils {
	
	private static final NumberFormat nf = new DecimalFormat("0.##");
	
	public static final int FORWARD_STRAND_COUNT = 0;
	public static final int FORWARD_STRAND_QUALITY = 1;
	public static final int FORWARD_STRAND_END_OF_READ_COUNT = 2;
	public static final int REVERSE_STRAND_COUNT = 3;
	public static final int REVERSE_STRAND_QUALITY = 4;
	public static final int REVERSE_STRAND_END_OF_READ_COUNT = 5;
	
	public static final int A_POSITION = 0;
	public static final int C_POSITION = 1;
	public static final int G_POSITION = 2;
	public static final int T_POSITION = 3;
	
	public static final long A_BASE_BIT = 0x2000000000000000l;
	public static final int A_BASE_BIT_POSITION = 61;
	public static final long C_BASE_BIT = 0x1000000000000000l;
	public static final int C_BASE_BIT_POSITION = 60;
	public static final long G_BASE_BIT = 0x800000000000000l;
	public static final int G_BASE_BIT_POSITION = 59;
	public static final long T_BASE_BIT = 0x400000000000000l;
	public static final int T_BASE_BIT_POSITION = 58;
	
	
	public static final long STRAND_BIT = 0x8000000000000000l;
	public static final int STRAND_BIT_POSITION = 63;
	public static final long END_OF_READ_BIT = 0x4000000000000000l;
	public static final int END_OF_READ_BIT_POSITION = 62;
	public static final int QUALITY_BIT_POSITION = 32;
	
	/**
	 * This removes reads that have the same read name hash from the accumulator.
	 * If the duplicates have the same base, then 1 is left, if they have different bases, they are both (all?) removed
	 * 
	 * This method updates the Accumulator object that is passed in, and is therefore not side-effect free
	 * 
	 * @param acc
	 */
	public static void removeOverlappingReads(Accumulator acc) {
		if (null != acc) {
			TLongList data = acc.getData();
			int len = data.size();
			TLongObjectMap<TLongList> readNameMap = new TLongObjectHashMap<>(len);
			
			
			for (int i = 0 ; i < len ; i += 2) {
				TLongList list = readNameMap.get(data.get(i));
				if (null == list) {
					list = new TLongArrayList(3);
					readNameMap.put(data.get(i), list);
				}
				list.add(data.get(i + 1));
			}
			
			if (readNameMap.size() == len / 2) {
				// woohoo!
			} else {
				
				TLongList toRemove = new TLongArrayList();
				readNameMap.forEachEntry((l, list) -> {
					
					if (list.size() > 1) {
						BitSet bs = getUniqueBasesUseAllList(list);
						if (doesBitSetContainMoreThan1True(bs)) {
							/*
							 * need to remove this whole batch of entries
							 */
							toRemove.add(l);
						} else {
							/*
							 * all the same base - remove all but one from the list
							 */
							long toKeep = list.get(0);
							list.clear();
							list.add(toKeep);
						}
					}
						
					return true;
				});

				/*
				 * remove any entries in the toRemove list
				 */
				toRemove.forEach(tr -> {
					readNameMap.remove(tr);
					return true;
				});
				
				/*
				 * rebuild a TLongList and set it on the Accumulator 
				 */
				
				TLongList newData = new TLongArrayList(readNameMap.size() * 2 + 1);
				
				readNameMap.forEachEntry((l, list) -> {
					
					newData.add(l);
					newData.add(list.get(0));
					
					return true;
				});
				acc.setData(newData);
			}
		}
	}
	
	/**
	 * Bit packs the following information into a long:
	 * strand (bit 63)
	 * end of read (bit 62)
	 * base (bits 58-61)
	 * quality (bits 32-40)
	 * position (bits 0-31)
	 * 
	 * @param forwardStrand
	 * @param endOfRead
	 * @param base
	 * @param quality
	 * @param position
	 * @return
	 */
	public static long convertStrandEORBaseQualAndPositionToLong(boolean forwardStrand, boolean endOfRead, byte base, byte quality, int position) {
		
		long l = 0;
		if (forwardStrand) {
			l |= STRAND_BIT;
		}
		
		if (endOfRead) {
			l |= END_OF_READ_BIT;
		}
		switch (base) {
		case Accumulator.A_BYTE:
			l |= A_BASE_BIT; break;
		case Accumulator.C_BYTE:
			l |= C_BASE_BIT; break;
		case Accumulator.G_BYTE:
			l |= G_BASE_BIT; break;
		case Accumulator.T_BYTE:
			l |= T_BASE_BIT; break;
			default: // don't set any of the positions
				break;
		}
		
		l += (long)quality << QUALITY_BIT_POSITION;
		l += (long)position;
		
		return l;
	}
	
	/**
	 * Looks at the supplied long, and returns the following:
	 * if bit 61 is set, return 'A'
	 * if bit 60 is set, return 'C'
	 * if bit 59 is set, return 'G'
	 * if bit 58 is set, return 'T'
	 * otherwise return '\u0000'
	 * 
	 * This method assumes that the long will only have one of these bits set, and will return only a single char.
	 * 
	 * @param l
	 * @return corresponding char if any of bits 58,59,60, or 61 are set, null char otherwise
	 */
	public static char getBaseAsCharFromLong(long l) {
		if ((l & A_BASE_BIT) != 0) {
			return Accumulator.A_CHAR;
		} else if ((l & C_BASE_BIT) != 0) {
			return Accumulator.C_CHAR;
		} else if ((l & G_BASE_BIT) != 0) {
			return Accumulator.G_CHAR;
		} else if ((l & T_BASE_BIT) != 0) {
			return Accumulator.T_CHAR;
		} else {
			return '\u0000';
		}
	}

	/**
	 * Returns a BitSet containing the bases seen in this Accumulator 
	 * 
	 * @see getUniqueBases(TLongList data)
	 * @param acc
	 * @return
	 */
	public static BitSet getUniqueBases(Accumulator acc) {
		if (null != acc) {
			return getUniqueBases(acc.getData());
		} else {
			return new BitSet(4);
		}
	}
	
	/**
	 * 
	 * Create and return a BitSet of length 4.
	 * If bit zero is set, 'A' is present
	 * If bit one is set, 'C' is present
	 * If bit two is set, 'G' is present
	 * If bit three is set, 'T' is present
	 * 
	 * This method indicated which bases are present, but does not give any information regarding frequency, quality etc.
	 * 
	 * 
	 * This method will look at every other long entry in the supplied list, starting at position 1 (0-based).
	 * This is because the Accumulator object inserts 2 entries into the list for each base, the first entry 
	 * is the hash or the read name, and the second, is the long, which has been bit-packed to contain strand, end of read, base, quality and start position
	 * 
	 * @param data
	 * @return
	 */
	public static BitSet getUniqueBases(TLongList data) {
		BitSet bs = new BitSet(4);
		if (null != data) {
			for (int i = 1, len = data.size() ; i < len ; i += 2) {
				long l = data.get(i);
				
				if ((l & A_BASE_BIT) != 0) {
					bs.set(A_POSITION);
				} else if ((l & C_BASE_BIT) != 0) {
					bs.set(C_POSITION);
				} else if ((l & G_BASE_BIT) != 0) {
					bs.set(G_POSITION);
				} else if ((l & T_BASE_BIT) != 0) {
					bs.set(T_POSITION);
				}
			}
		}
		return bs;
	}
	
	/**
	 * 
	 * Create and return a BitSet of length 4.
	 * If bit zero is set, 'A' is present
	 * If bit one is set, 'C' is present
	 * If bit two is set, 'G' is present
	 * If bit three is set, 'T' is present
	 * 
	 * This method indicated which bases are present, but does not give any information regarding frequency, quality etc.
	 * 
	 * 
	 * This method will look at all long entries in the list.
	 * The long values have been bit-packed to contain strand, end of read, base, quality and start position
	 * 
	 * @param data
	 * @return
	 */
	public static BitSet getUniqueBasesUseAllList(TLongList data) {
		BitSet bs = new BitSet(4);
		if (null != data) {
			for (int i = 0, len = data.size() ; i < len ; i ++) {
				long l = data.get(i);
				
				if ((l & A_BASE_BIT) != 0) {
					bs.set(A_POSITION);
				} else if ((l & C_BASE_BIT) != 0) {
					bs.set(C_POSITION);
				} else if ((l & G_BASE_BIT) != 0) {
					bs.set(G_POSITION);
				} else if ((l & T_BASE_BIT) != 0) {
					bs.set(T_POSITION);
				}
			}
		}
		return bs;
	}
	
	public static String getUniqueBasesAsString(Accumulator acc) {
		BitSet bs = getUniqueBases(acc);
		return getUniqueBasesAsString(bs);
	}
	
	public static String getUniqueBasesAsString(BitSet bs) {
		String s = "";
		if (bs.get(A_POSITION)) {
			s += Accumulator.A_STRING;
		}
		if (bs.get(C_POSITION)) {
			s += Accumulator.C_STRING;
		}
		if (bs.get(G_POSITION)) {
			s += Accumulator.G_STRING;
		}
		if (bs.get(T_POSITION)) {
			s += Accumulator.T_STRING;
		}
		return s;
	}

	
	/**
	 * If the supplied array is not null and has 6 elements, this method will sum elements 0 and 3, and move to the top 32 bits of the returned long
	 * with the lower 32 bits of the returned long containing the sum of elements 1 and 4.
	 * 
	 * If the array is null, or has a length not equal to 6, 0 is returned
	 * 
	 * @param array
	 * @return
	 */
	public static long getCountAndQualCombo(int[] array) {
		if (null != array && array.length == 6) {
			long count = array[FORWARD_STRAND_COUNT] + array[REVERSE_STRAND_COUNT];
			int quality = array[FORWARD_STRAND_QUALITY] + array[REVERSE_STRAND_QUALITY];
			
			return (count << 32) + quality;
		}
		return 0l;
	}
	
	/**
	 * Static class that contains the base, count, total quality and boolean indicating whether the base is the same as the reference
	 * Creates an immutable object, containing a getter for the base.
	 * 
	 * This is used when we have bases competing for a place in the genotype. It implements the Comparable interface, and sorts according to count, isRef, and then quality.
	 * 
	 * @author oliverh
	 *
	 */
	static class BaseDetails implements Comparable<BaseDetails> {
		private final char base;
		private final int count;
		private final int quality;
		private final boolean isRef;
		public BaseDetails(char base, int count, int quality, boolean isRef) {
			this.base = base;
			this.count = count;
			this.quality = quality;
			this.isRef = isRef;
		}
		public char getBase() {
			return base;
		}
		@Override
		public int compareTo(BaseDetails o) {
			int diff = o.count - count;
			if (diff != 0) return diff;
			if (o.isRef) 
				return 1;
			else if (isRef) 
				return -1;
			return o.quality - quality;
		}
	}
	
	/**
	 * 
	 * @param lArray
	 * @param ref
	 * @return
	 */
	public static char[] getBasesForGenotype(long[] lArray, char ref) {
		int countOfBases = 0;
		for (long l : lArray) {
			if (l > 0) {
				countOfBases++;
			}
		}
		
		if (countOfBases == 1) {
			
			int position = 0;
			for (int i = 0 ; i < lArray.length ; i++) {
				if (lArray[i] > 0) {
					position = i;
					break;
				}
			}
			char c = getCharFromIntPosition(position);
			return new char[] {c,c};
			
		} else if (countOfBases == 2) {
			
			char [] cArray = new char[2];
			int j = 0;
			for (int i = 0 ; i < lArray.length ; i++) {
				if (lArray[i] > 0) {
					cArray[j++] = getCharFromIntPosition(i);
				}
			}
			return cArray;
			
		} else if (countOfBases > 2) {
			
			List<BaseDetails> bd = new ArrayList<>(countOfBases);
			for (int i = 0 ; i < lArray.length ; i++) {
				if (lArray[i] > 0) {
					char c = getCharFromIntPosition(i);
					bd.add(new BaseDetails(c, (int)(lArray[i] >> 32), (int) lArray[i], c == ref));
				}
			}
			bd.sort(null);
			
			return new char[] {bd.get(0).getBase(), bd.get(1).getBase()};
			
		} else {
			return null;
		}
		
	}
	
	/**
	 * Returns a char depending on the values of the supplied int.
	 * if i is 0, return 'A'
	 * if i is 1, return 'C'
	 * if i is 2, return 'G'
	 * if i is 3, return 'T'
	 * else return '\u0000'
	 * 
	 * @param i int
	 * @return char corresponding to value in suppled int
	 */
	public static char getCharFromIntPosition(int i) {
		switch (i) {
		case A_POSITION: return Accumulator.A_CHAR;
		case C_POSITION: return Accumulator.C_CHAR;
		case G_POSITION: return Accumulator.G_CHAR;
		case T_POSITION: return Accumulator.T_CHAR;
		default: return '\u0000';
		}
	}
	
	public static GenotypeEnum getGenotype(final Accumulator acc, final char ref, final Rule rule, final boolean secondPass, final double percentage) {
		
		TCharObjectMap<int[]> accMap = getAccumulatorDataByBase(acc);
		
		AtomicInteger totalCoverage = new AtomicInteger();
		AtomicInteger totalQuality = new AtomicInteger();
		accMap.forEachValue(a -> {
				totalCoverage.addAndGet(a[FORWARD_STRAND_COUNT] + a[REVERSE_STRAND_COUNT]);
				totalQuality.addAndGet(a[FORWARD_STRAND_QUALITY] + a[REVERSE_STRAND_QUALITY]);
				return true;
		});
		
		
		long [] passingBases = new long[4];
		
		accMap.forEachEntry((c, a) -> {
			
			if (canContributeToGenotype(a, totalCoverage.get(), totalQuality.get(), rule, secondPass, percentage) ) {
				long l = getCountAndQualCombo(a);
				switch (c) {
				case Accumulator.A_CHAR: passingBases[A_POSITION] = l; break;
				case Accumulator.C_CHAR: passingBases[C_POSITION] = l; break;
				case Accumulator.G_CHAR: passingBases[G_POSITION] = l; break;
				case Accumulator.T_CHAR: passingBases[T_POSITION] = l; break;
				default :  break; // do nothing
				}
			}
			return true;
		});
		
		char [] cArray = getBasesForGenotype(passingBases, ref);
		return null == cArray ? null : GenotypeEnum.getGenotypeEnum(cArray[0], cArray[1]);
	}
	
	public static boolean canContributeToGenotype(int[] baseData, int totalCoverage, final int totalQuality, final Rule rule, final boolean secondPass, final double percentage) {
		return 
				null != baseData && baseData.length > REVERSE_STRAND_END_OF_READ_COUNT 
				&& ( ! secondPass || (baseData[FORWARD_STRAND_COUNT] > 0 && baseData[REVERSE_STRAND_COUNT] > 0))
				&& PileupElementLiteUtil.passesCountCheck(baseData[FORWARD_STRAND_COUNT] + baseData[REVERSE_STRAND_COUNT], totalCoverage, rule, secondPass) 
				&& PileupElementLiteUtil.passesWeightedVotingCheck(totalQuality, baseData[FORWARD_STRAND_QUALITY] + baseData[REVERSE_STRAND_QUALITY], percentage, secondPass);
	}
	
	
	public static boolean passesInitialCheck(Accumulator acc1, Accumulator acc2, char ref) {
		
		BitSet bs1 = getUniqueBases(acc1);
		if (doesBitSetContainMoreThan1True(bs1)) {
			return true;
		}
		
		BitSet bs2 = getUniqueBases(acc2);
		if (doesBitSetContainMoreThan1True(bs2)) {
			return true;
		}
		
		if ( ! doBitSetsHaveSameBitsSet(bs1, bs2)) {
			return true;
		}
		
		/*
		 * we are here if both accumulators represent the same single base
		 * if this base is the same as the reference, its dead to us
		 */
		if (Character.isLowerCase(ref)) {
			ref = Character.toUpperCase(ref);
		}
		
		switch (ref) {
		case Accumulator.A_CHAR: return bs1.get(A_POSITION) == false;
		case Accumulator.C_CHAR: return bs1.get(C_POSITION) == false;
		case Accumulator.G_CHAR: return bs1.get(G_POSITION) == false;
		case Accumulator.T_CHAR: return bs1.get(T_POSITION) == false;
		default: return false;
		}
	}
	
	public static boolean passesInitialCheck(Accumulator acc, char ref) {
		BitSet bs = getUniqueBases(acc);
		if (doesBitSetContainMoreThan1True(bs)) {
			return true;
		}
		
		/*
		 * we are here if both accumulators represent the same single base
		 * if this base is the same as the reference, its dead to us
		 */
		if (Character.isLowerCase(ref)) {
			ref = Character.toUpperCase(ref);
		}
		
		switch (ref) {
		case Accumulator.A_CHAR: return bs.get(A_POSITION) == false;
		case Accumulator.C_CHAR: return bs.get(C_POSITION) == false;
		case Accumulator.G_CHAR: return bs.get(G_POSITION) == false;
		case Accumulator.T_CHAR: return bs.get(T_POSITION) == false;
		default: return false;
		}
	}
	
	public static boolean doesBitSetContainMoreThan1True(BitSet bs) {
		return bs.cardinality() > 1;
	}
	
	/**
	 * returns false if one of the BitSets is null, or if they are not equal
	 * true otherwise
	 * @param bs1
	 * @param bs2
	 * @return
	 */
	public static boolean doBitSetsHaveSameBitsSet(BitSet bs1, BitSet bs2) {
		return null != bs1 ? bs1.equals(bs2) : false;
	}
	
	
	public static int[] getTotalQualityByStrand(Accumulator acc) {
		if (null != acc) {
			TLongList list = acc.getData();
			int qualTallyFS = 0;
			int qualTallyRS = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					if (((list.get(i)  >>> STRAND_BIT_POSITION) & 1) == 1) {
						qualTallyFS += getQualityFromLong(list.get(i));
					} else {
						qualTallyRS += getQualityFromLong(list.get(i));
					}
				}
			}
			return new int[] {qualTallyFS, qualTallyRS};
		}
		return new int[] {0,0};
	}
	
	/**
	 * Returns the part of the long that contains the quality information
	 * This is bits 32-40
	 * 
	 * @param l
	 * @return
	 */
	public static byte getQualityFromLong(long l) {
		return (byte)(l >>> QUALITY_BIT_POSITION);
	}
	
	public static int[] getCountAndEndOfReadByStrand(Accumulator acc) {
		if (null != acc) {
			TLongList list = acc.getData();
			int eorTallyFS = 0;
			int eorTallyRS = 0;
			int fsCount = 0, rsCount = 0;
			if (null != list) {
				for (int i = 1, len = list.size(); i < len ; i += 2) {
					if (((list.get(i)  >>> STRAND_BIT_POSITION) & 1) == 1) {
						fsCount++;
						if ((list.get(i) & END_OF_READ_BIT) != 0) {
							eorTallyFS ++;
						}
					} else {
						rsCount++;
						if ((list.get(i) & END_OF_READ_BIT) != 0) {
							eorTallyRS ++;
						}
					}
				}
			}
			return new int[] {fsCount, rsCount, eorTallyFS, eorTallyRS};
		}
		return new int[] {0,0,0,0};
	}
	
	public static String getOABS(Accumulator acc) {
		 TCharObjectMap<int[]> map = getAccumulatorDataByBase(acc);
		 return getOABS(map); 
	}
	public static String getOABS(TCharObjectMap<int[]> map) {
		if (null != map && ! map.isEmpty()) {
			
			StringBuilder pileup = new StringBuilder();
			
			int [] baseData = map.get('A');
			if (null != baseData) {
				StringUtils.updateStringBuilder(pileup, toObservedAlleleByStrand("A", baseData), Constants.SEMI_COLON);
			}
			baseData = map.get('C');
			if (null != baseData) {
				StringUtils.updateStringBuilder(pileup, toObservedAlleleByStrand("C", baseData), Constants.SEMI_COLON);
			}
			baseData = map.get('G');
			if (null != baseData) {
				StringUtils.updateStringBuilder(pileup, toObservedAlleleByStrand("G", baseData), Constants.SEMI_COLON);
			}
			baseData = map.get('T');
			if (null != baseData) {
				StringUtils.updateStringBuilder(pileup, toObservedAlleleByStrand("T", baseData), Constants.SEMI_COLON);
			}
			
			return pileup.length() == 0 ? Constants.MISSING_DATA_STRING : pileup.toString();
		}
		return Constants.MISSING_DATA_STRING;
	}
	
	
	/**
	 * Returns a string representation of bases in the supplied Accumulator that are a the ends of the reads
	 * First gets the Accucmulator data as a map, and passes to the overloaded method
	 * 
	 * @see getAccumulatorDataByBase
	 * @see getEndOfReadsPileup
	 * @param acc
	 * @return
	 */
	public static String getEndOfReadsPileup(Accumulator acc) {
		TCharObjectMap<int[]> map = getAccumulatorDataByBase(acc);
		return getEndOfReadsPileup(map);
	}
	
	/**
	 * Returns a string representation of bases in the supplied Accumulator that are a the ends of the reads 
	 * 
	 * 
	 * @param map
	 * @return
	 */
	public static String getEndOfReadsPileup(TCharObjectMap<int[]> map) {
		if (null != map && ! map.isEmpty()) {
			
			StringBuilder pileup = new StringBuilder();
			
			int [] baseData = map.get(Accumulator.A_CHAR);
			if (null != baseData && (baseData[FORWARD_STRAND_END_OF_READ_COUNT] > 0 || baseData[REVERSE_STRAND_END_OF_READ_COUNT] > 0)) {
				StringUtils.updateStringBuilder(pileup, Accumulator.A_STRING + baseData[FORWARD_STRAND_END_OF_READ_COUNT] + "[]" + baseData[REVERSE_STRAND_END_OF_READ_COUNT] + "[]", Constants.SEMI_COLON);
			}
			baseData = map.get(Accumulator.C_CHAR);
			if (null != baseData && (baseData[FORWARD_STRAND_END_OF_READ_COUNT] > 0 || baseData[REVERSE_STRAND_END_OF_READ_COUNT] > 0)) {
				StringUtils.updateStringBuilder(pileup, Accumulator.C_STRING + baseData[FORWARD_STRAND_END_OF_READ_COUNT] + "[]" + baseData[REVERSE_STRAND_END_OF_READ_COUNT] + "[]", Constants.SEMI_COLON);
			}
			baseData = map.get(Accumulator.G_CHAR);
			if (null != baseData && (baseData[FORWARD_STRAND_END_OF_READ_COUNT] > 0 || baseData[REVERSE_STRAND_END_OF_READ_COUNT] > 0)) {
				StringUtils.updateStringBuilder(pileup, Accumulator.G_STRING + baseData[FORWARD_STRAND_END_OF_READ_COUNT] + "[]" + baseData[REVERSE_STRAND_END_OF_READ_COUNT] + "[]", Constants.SEMI_COLON);
			}
			baseData = map.get(Accumulator.T_CHAR);
			if (null != baseData && (baseData[FORWARD_STRAND_END_OF_READ_COUNT] > 0 || baseData[REVERSE_STRAND_END_OF_READ_COUNT] > 0)) {
				StringUtils.updateStringBuilder(pileup, Accumulator.T_STRING + baseData[FORWARD_STRAND_END_OF_READ_COUNT] + "[]" + baseData[REVERSE_STRAND_END_OF_READ_COUNT] + "[]", Constants.SEMI_COLON);
			}
			return pileup.length() == 0 ? Constants.MISSING_DATA_STRING : pileup.toString();
		}
		return Constants.MISSING_DATA_STRING;
	}
	
	
	/**
	 * Returns a string representing the supplied base and counts (data array)
	 * It is expected that the data array contains the following fields:
	 * data[0] = forward strand count 
	 * data[1] = forward strand total quality 
	 * data[2] = forward strand end of read count 
	 * data[3] = reverse strand count 
	 * data[4] = reverse strand total quality
	 * data[5] = reverse strand end of read count
	 * 
	 * 
	 * @param base
	 * @param data
	 * @return
	 */
	static String toObservedAlleleByStrand(String base, int[] data) {
		if (null != base && null != data && data.length == 6) {
			int forwardCount = data[FORWARD_STRAND_COUNT];
			int reverseCount = data[REVERSE_STRAND_COUNT];
			
			StringBuilder sb = new StringBuilder(base);
			sb.append(forwardCount).append(Constants.OPEN_SQUARE_BRACKET);
			float qual = forwardCount == 0 ? 0.0f : (float)data[FORWARD_STRAND_QUALITY] / forwardCount; 
			sb.append(nf.format(qual));
			sb.append(Constants.CLOSE_SQUARE_BRACKET);
			sb.append(reverseCount).append(Constants.OPEN_SQUARE_BRACKET);
			qual = reverseCount == 0 ? 0.0f : (float)data[REVERSE_STRAND_QUALITY] / reverseCount; 
			sb.append(nf.format(qual));
			sb.append(Constants.CLOSE_SQUARE_BRACKET);
			return sb.toString();
		}
		return null;
	}
	
	/**
	 * 
	 * This method breaks the Accumulator object down by base.
	 * For each base present, an int array containing the following is populated:
	 * baseData[0] = forward strand count 
	 * baseData[1] = forward strand total quality 
	 * baseData[2] = forward strand end of read count 
	 * baseData[3] = reverse strand count 
	 * baseData[4] = reverse strand total quality
	 * baseData[5] = reverse strand end of read count
	 * 
	 * @param acc
	 * @return
	 */
	public static TCharObjectMap<int[]> getAccumulatorDataByBase(Accumulator acc) {
		TCharObjectMap<int[]> map = new TCharObjectHashMap<>(8);
		if (null != acc) {
			TLongList data = acc.getData();
			if (null != data) {
				for (int i = 1, len = data.size() ; i < len ; i += 2) {
					long l = data.get(i);
					char base = getBaseAsCharFromLong(l);
					int [] baseData = map.get(base);
					if (null == baseData) {
						baseData = new int[6];
						map.put(base, baseData);
					}
					
					byte qual = getQualityFromLong(l);
					boolean endOfRead = (l & END_OF_READ_BIT) != 0;
					
					if ((l & STRAND_BIT) != 0) {
						baseData[FORWARD_STRAND_COUNT]++;
						baseData[FORWARD_STRAND_QUALITY] +=  qual;
						if (endOfRead) baseData[FORWARD_STRAND_END_OF_READ_COUNT] ++;
					} else {
						baseData[REVERSE_STRAND_COUNT]++;
						baseData[REVERSE_STRAND_QUALITY] +=  qual;
						if (endOfRead) baseData[REVERSE_STRAND_END_OF_READ_COUNT] ++;
					}
				}
			}
		}
		return map;
	}
	
	/**
	 * Returns a count of the novel start positions in the Accumulator for the specified base.
	 * If the base is not represented in the Accumulator, it will return 0.
	 * 
	 * @param acc
	 * @param novelStartsBase
	 * @return
	 */
	public static int getNovelStartsForBase(Accumulator acc, char novelStartsBase) {
		if (null != acc) {
			TLongList data = acc.getData();
			TIntSet fsStartPositions = new TIntHashSet();
			TIntSet rsStartPositions = new TIntHashSet();
			for (int i = 1, len = data.size() ; i < len ; i += 2) {
				long l = data.get(i);
				char base = getBaseAsCharFromLong(l);
				if (base == novelStartsBase) {
					if ((l & STRAND_BIT) != 0) {
						fsStartPositions.add((int) l);
					} else {
						rsStartPositions.add((int) l);
					}
				}
			}
			return fsStartPositions.size() + rsStartPositions.size();
		}
		return 0;
	}

	/**
	 * Return an array of ints for the largest base (based on count) in the Accumulator that is NOT equal to the supplied reference char
	 * The int array contains the following information:
	 * array[0] = forward strand count 
	 * array[1] = forward strand total quality 
	 * array[2] = forward strand end of read count 
	 * array[3] = reverse strand count 
	 * array[4] = reverse strand total quality
	 * array[5] = reverse strand end of read count
	 * 
	 * @see getAccumulatorDataByBase(Accumulator acc)
	 * @param acc
	 * @param ref
	 * @return
	 */
	public static int[] getLargestVariant(Accumulator acc, char ref) {
		TCharObjectMap<int[]> map = getAccumulatorDataByBase(acc);
		map.remove(ref);
		int mapSize = map.size();
		
		if (mapSize == 1) {
			return (int[])map.values()[0];
		} else if (mapSize > 1) {
			AtomicInteger maxValue = new AtomicInteger();
			map.forEachValue(array -> {
				if (array[FORWARD_STRAND_COUNT] + array[REVERSE_STRAND_COUNT] > maxValue.intValue()) {
					maxValue.set(array[FORWARD_STRAND_COUNT] + array[REVERSE_STRAND_COUNT]);
				}
				return true;
			});
			
			/*
			 * loop through again to find entry with largest value
			 */
			AtomicReference<Character> ar = new AtomicReference<>();
			map.forEachEntry((c, array) -> {
				if (array[FORWARD_STRAND_COUNT] + array[REVERSE_STRAND_COUNT] == maxValue.intValue()) {
					ar.set(c);
				}
				return true;
			});
			return map.get((char) ar.get());
		} else {
			return new int[] {0,0,0,0,0,0};
		}
	}
	
	/**
	 * Here we are returning true if there is coverage on both strands, 
	 * AND that the number of reads on the least well represented strand 
	 * accounts for more than the supplied percentage of the total number of reads
	 * 
	 * @param acc
	 * @param percentage
	 */
//	public static boolean bothStrandsByPercentage(Accumulator acc, int percentage) {
//		int [] data = getCountAndEndOfReadByStrand(acc);
//		return areBothStrandsRepresented(data[0], data[1], percentage);
//	}
	
	/**
	 * Sums the zero-th and 2nd entries in the values of the supplied map, and if the 
	 * percentage is higher than the supplied percentage, return true. False otherwise
	 * 
	 * @param basesCountsNNS map containing a String key, and int[] value, which contains 
	 * counts and quals for both strands so that position 0 is count on forward strand, and position 2 is count on reverse strand
	 * @param percentage
	 * @return
	 */
	public static boolean bothStrandsByPercentageCS(Map<String, short[]> basesCountsNNS, int percentage) {
		AtomicInteger fs = new AtomicInteger();
		AtomicInteger rs = new AtomicInteger();
		basesCountsNNS.values().stream().forEach(sa -> {fs.addAndGet(sa[0]);	rs.addAndGet(sa[2]);});
		
		return  areBothStrandsRepresented(fs, rs, percentage);
	}
	
	public static boolean areBothStrandsRepresented(AtomicInteger fs, AtomicInteger rs, int percentage) {
		return areBothStrandsRepresented(fs.get(), rs.get(), percentage);
	}
	
	
	/**
	 * returns true if Math.min(fs, rs) / (fs + rs) as a percentage is larger than the supplied percentage 
	 * 
	 * @param fs
	 * @param rs
	 * @param percentage
	 * @return
	 */
	public static boolean areBothStrandsRepresented(int fs, int rs, int percentage) {
		if (fs == 0 || rs == 0) {
			return false;
		}
		int min = Math.min(fs, rs);
		return ((double) min / (fs + rs)) * 100 > percentage;
	}
	
	/**
	 * Returns a {@link TLongIntMap} containing the read names hash as the key, and the corresponding start position of that read as the value
	 * If the read is on the reverse strand, the start position is represented as a negative number
	 * This will need to be converted back to a positive number when the value of the start position is required
	 * 
	 * @param acc
	 * @return
	 */
	public static TLongIntMap getReadNameHashStartPositionMap(Accumulator acc) {
		if (null != acc) {
			TLongList list = acc.getData();
			if (null != list) {
				TLongIntMap map = new TLongIntHashMap(list.size() * 2);
				for (int i = 0, len = list.size(); i < len ; i += 2) {
					
					int startPosition = (int) list.get(i + 1);
					if (((list.get(i + 1)  >>> STRAND_BIT_POSITION) & 1) == 0) {
						startPosition = - startPosition;
					}
					map.put(list.get(i), startPosition);
				}
				return map;
			}
		}
		return null;
	}
	
	/**
	 * Returns a map of readName hash to base
	 * If the base is lower case, its on the reverse strand, if upper case, forward strand
	 * @param acc
	 * @return
	 */
	public static TLongCharMap getReadNameHashBaseMap(Accumulator acc) {
		if (null != acc) {
			TLongList list = acc.getData();
			if (null != list) {
				TLongCharMap map = new TLongCharHashMap(list.size() * 2);
				for (int i = 0, len = list.size(); i < len ; i += 2) {
					
					char base = getBaseAsCharFromLong(list.get(i + 1));
					if (((list.get(i + 1)  >>> STRAND_BIT_POSITION) & 1) == 0) {
						base = Character.toLowerCase(base);
					}
					map.put(list.get(i), base);
				}
				return map;
			}
		}
		return null;
	}
	
	
	/**
	 * Returns a {@link TLongIntMap} containing the read name hash as key, and start position as value for the list of accumulators provided
	 * This is used by compound snp logic in {@link org.qcmg.snp.utilPipelineUtil.getBasesFromAccumulators}
	 * @param accs
	 * @return
	 */
	public static TLongIntMap getReadIdStartPosMap(List<Accumulator> accs) {
		if (null != accs) {
			
			TLongIntMap combinedMap = new TLongIntHashMap();
			for (Accumulator acc : accs) {
				combinedMap.putAll(getReadNameHashStartPositionMap(acc));
			}
			return combinedMap;
		}
		return null;
	}
	
	
	
	/**
	 * Attempts to create an Accumulator object in all its glory from a mere Observed Allele By Strand string (no doubt from the format field of a vcf record)
	 * format is:
	 * A10[40]0[0]
	 * 
	 * NOTE that this Accumulator object will not contain unfiltered records, nor will it have meaningful information for ends of reads or read ids or novel starts.
	 * 
	 * SHOULD BE USED FOR TESTING PURPOSES ONLY!!!
	 * 
	 * @param oabs
	 * @return
	 */
	public static Accumulator createFromOABS(String oabs, int position) {
		
		if (null != oabs) {
			Accumulator acc = new Accumulator(position);
			String [] alleles = oabs.split(Constants.SEMI_COLON_STRING);
			for (String a : alleles) {
				char base = a.charAt(0);
				int openBracketIndex = a.indexOf(Constants.OPEN_SQUARE_BRACKET);
				int closeBracketIndex = a.indexOf(Constants.CLOSE_SQUARE_BRACKET);
				int fsCount = Integer.parseInt(a.substring(1, openBracketIndex));
				float fsQual = Float.parseFloat(a.substring(openBracketIndex + 1, closeBracketIndex));
				/*
				 * reverse strand
				 */
				int openBracketIndexRS = a.indexOf(Constants.OPEN_SQUARE_BRACKET, openBracketIndex + 1);
				int closeBracketIndexRS = a.indexOf(Constants.CLOSE_SQUARE_BRACKET, closeBracketIndex + 1);
				int rsCount = Integer.parseInt(a.substring(closeBracketIndex + 1, openBracketIndexRS));
				float rsQual = Float.parseFloat(a.substring(openBracketIndexRS + 1, closeBracketIndexRS));
				
				for (int i = 0 ; i < fsCount ; i++) {
					acc.addBase((byte)base, (byte)fsQual, true, position - 10, position, position + 10, i);
				}
				for (int i = 0 ; i < rsCount ; i++) {
					acc.addBase((byte)base, (byte)rsQual, false, position - 10, position, position + 10, i);
				}
			}
			return acc;
		}
		return null;
	}

}
