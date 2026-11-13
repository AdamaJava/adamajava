package au.edu.qimr.tiledaligner.util;

import static au.edu.qimr.tiledaligner.util.TiledAlignerUtil.POSITION_OF_TILE_IN_SEQUENCE_OFFSET;
import static au.edu.qimr.tiledaligner.util.TiledAlignerUtil.REVERSE_COMPLEMENT_BIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.qcmg.common.util.NumberUtils;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;

/**
 * Checks that TiledAlignerUtil.convertLongDoubleArrayToMap gives exactly the same results as the original implementation
 */
public class NonContinuousCountEquivalenceTest {

	/*
	 * verbatim copies of the original implementations
	 */
	static Map<Integer, TLongList> originalConvertLongDoubleArrayToMap(long [][] array, boolean rc) {
		Map<Integer, TLongList> map = new HashMap<>();
		
		/*
		 * get number of commonly occurring tiles at beginning of 2D array 
		 */
		TLongIntMap positionCountMap = new TLongIntHashMap();
		int currentMaxCount = 0;
		
		for (int i = 0 ; i < array.length - 1 ; i++) {
			
			long [] subArray = array[i];
			if (null != subArray) {
				for (long l : subArray) {
					
					/*
					 * if long is negative, then it is a commonly occurring tile - ignore these for now
					 */
					if (l < 0) {
						continue;
					}
					
					/*
					 * check the next array to see if we can find a +1 match
					 */
					int tally = originalNonContinuousCount(array, l, i + 1);
					if (tally > 65536) {
						int exactMatchComponent = NumberUtils.getPartOfPackedInt(tally, true);
						if (exactMatchComponent > currentMaxCount) {
							currentMaxCount = exactMatchComponent;
						}
						if (exactMatchComponent > 5) {
							
							/*
							 * check to see if we have already recorded this in the positionCountMap
							 */
							
							boolean alreadyRepresented = false;
							for (int z = 0 ; z <= i ; z++) {
								if ( positionCountMap.get(l - z) >= exactMatchComponent + z) {
									alreadyRepresented = true;
									break;
								}
							}
							if ( ! alreadyRepresented) {
								/*
								 * add
								 * we want to add the tile position into the long as this will aid split contig sequences
								 */
								long longToUse = NumberUtils.addShortToLong(l, (short) i, POSITION_OF_TILE_IN_SEQUENCE_OFFSET);
								if (rc) {
									longToUse = NumberUtils.setBit(longToUse, REVERSE_COMPLEMENT_BIT);
								}
								map.computeIfAbsent(tally, f -> new TLongArrayList()).add(longToUse);
								positionCountMap.put(l, exactMatchComponent);
							}
						}
					}
				}
			}
		}
		
		return map;
	}

	static int originalNonContinuousCount(long [][] array, long l, int arrayStartPosition) {
		int tally = 1;	// start at 1
		int commonTileTally = 0;
		int mismatchTally = 0;
		int arrayLength = array.length;
		
		for (int i = 0 ; i < arrayLength - arrayStartPosition ; i++) {
			long [] nextArray = array[arrayStartPosition + i];
			if (null != nextArray) {
				if (nextArray.length == 1 && nextArray[0] < 0) {
					commonTileTally++;
					
				} else {
					
					/*
					 * try and reduce the occurrences of binary searching
					 */
					int position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 1);
					
					if (position > -1) {
						tally++;
						/*
						 * if we have commonly occurring tiles, add to tally, and reset common tile count
						 */
						if (commonTileTally > 0) {
							tally += commonTileTally;
							commonTileTally = 0;
						}
					} else {
						
						
						/*
						 * check to see if this position was a mismatch
						 * by jumping ahead 13
						 */
						if (arrayStartPosition + i + 13 < arrayLength) {
							nextArray = array[arrayStartPosition + i + 13];
							if (null != nextArray && nextArray.length > 0) {
								if (nextArray[0] != -1) {
							
									position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 14);
									if (position > -1) {
										i += 13;
										tally += 14;
										mismatchTally++;
										continue;
									}
								} else if (nextArray.length == 1){
									/*
									 * allow a common occurring tile after the snp
									 */
									if (arrayStartPosition + i + 14 < arrayLength) {
										nextArray = array[arrayStartPosition + i + 14];
										if (null != nextArray && nextArray.length > 0 && nextArray[0] != -1) {
											position = NumberUtils.getPositionOfLongInArray(nextArray, l + i + 15);
											if (position > -1) {
												commonTileTally++;
												i += 14;	// plus one
												tally += 14;	// kept the same, as we will add the common tile count at the end
												mismatchTally++;
												continue;
											}
										}
									}
								}
							}
						}
						
						
						
						/*
						 * no longer allow gaps - we want exact matches only, and then use splits mechanism to stitch together constituent chunks if necessary 
						 * 
						 * and so, if we don't have a match, or a commonly occurring tile, break out
						 */
						break;
					}
				}
			}
		}
		
		/*
		 * If the commonTallyCount is less than the tally, incorporate it into the tally
		 */
		if (commonTileTally > 0 && tally > commonTileTally) {
			tally += commonTileTally;
		}
		
		return NumberUtils.getTileCount(tally, mismatchTally);
	}

	/*
	 * Tile start position arrays: some diagonals (well matched stretches of sequence, with mismatches, split into pieces, repeated),
	 * plus random hits nearby (so that positions collide with the diagonals) and far away, commonly occurring tiles and nulls
	 */
	static long[][] randomArray(Random r) {
		int n = 2 + r.nextInt(r.nextInt(10) == 0 ? 400 : 160);
		TLongHashSet[] sets = new TLongHashSet[n];
		for (int i = 0; i < n; i++) {
			sets[i] = new TLongHashSet();
		}
		int range = 200 + r.nextInt(r.nextBoolean() ? 1000 : 100000);
		int diagonals = r.nextInt(6);
		for (int d = 0; d < diagonals; d++) {
			long base = r.nextInt(range);
			int from = r.nextInt(n);
			int to = from + 1 + r.nextInt(n - from);
			for (int i = from; i < to; i++) {
				if (r.nextInt(40) == 0) {
					i += r.nextInt(r.nextBoolean() ? 13 : 30);	// mismatch / gap
					continue;
				}
				if (r.nextInt(15) == 0) {
					base += r.nextInt(5) - 2;	// indel
				}
				if (base + i >= 0) {
					sets[i].add(base + i);
				}
			}
		}
		int hits = r.nextInt(r.nextBoolean() ? 3 : 30);
		for (int i = 0; i < n; i++) {
			int h = r.nextInt(hits + 1);
			for (int k = 0; k < h; k++) {
				sets[i].add(r.nextInt(10) == 0 ? r.nextInt(Integer.MAX_VALUE) : r.nextInt(range));
			}
		}
		long[][] array = new long[n][];
		for (int i = 0; i < n; i++) {
			int type = r.nextInt(r.nextBoolean() ? 25 : 8);
			if (type == 0) {
				array[i] = null;
			} else if (type == 1) {
				array[i] = new long[] {r.nextInt(5) == 0 ? - 2 - r.nextInt(1000) : -1};		// commonly occurring tile (-1 is what getStartPositionsArray uses)
			} else if (type == 2) {
				array[i] = new long[0];
			} else if (type == 3 && sets[i].size() > 0) {
				// commonly occurring tile marker along with positions (not produced by getStartPositionsArray, but handled by nonContinuousCount)
				long[] a = sets[i].toArray();
				Arrays.sort(a);
				long[] b = new long[a.length + 1];
				b[0] = -1;
				System.arraycopy(a, 0, b, 1, a.length);
				array[i] = b;
			} else {
				long[] a = sets[i].toArray();
				Arrays.sort(a);
				if (r.nextInt(200) == 0 && a.length > 2) {
					// out of order, just in case
					long t = a[0]; a[0] = a[a.length - 1]; a[a.length - 1] = t;
				}
				array[i] = a;
			}
		}
		return array;
	}

	@Test
	public void matchesOriginal() {
		Random r = new Random(11);
		int nonEmpty = 0;
		int total = 20000;
		for (int t = 0; t < total; t++) {
			long[][] array = randomArray(r);
			boolean rc = r.nextBoolean();
			Map<Integer, TLongList> expected = originalConvertLongDoubleArrayToMap(array, rc);
			Map<Integer, TLongList> actual = TiledAlignerUtil.convertLongDoubleArrayToMap(array, rc);
			assertEquals(Arrays.deepToString(array), expected.toString(), actual.toString());
			if (expected.size() > 1) {
				nonEmpty++;
			}
			for (int s = 0; s < 3 && array.length > 1; s++) {
				int i = 1 + r.nextInt(array.length - 1);
				long l = array[i - 1] != null && array[i - 1].length > 0 ? array[i - 1][0] : r.nextInt(1000);
				assertEquals(originalNonContinuousCount(array, l, i), TiledAlignerUtil.nonContinuousCount(array, l, i));
			}
		}
		// make sure that the arrays are producing results
		assertTrue(nonEmpty + " of " + total, nonEmpty > total / 4);
	}

	@Test
	public void cleanDiagonals() {
		// a perfect match, and 2 overlapping split pieces
		long[][] array = new long[140][];
		for (int i = 0; i < array.length; i++) {
			array[i] = i < 80 ? new long[] {5, 1000 + i, 70000 + i} : new long[] {1000 + i, 500000 + i};
		}
		assertEquals(originalConvertLongDoubleArrayToMap(array, false).toString(), TiledAlignerUtil.convertLongDoubleArrayToMap(array, false).toString());
	}
}
