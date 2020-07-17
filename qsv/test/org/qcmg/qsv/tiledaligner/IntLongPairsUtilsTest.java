package org.qcmg.qsv.tiledaligner;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtilsTest {
	
	@Test
	public void getCoveredBases() {
		assertEquals(0, IntLongPairsUtils.getBasesCoveredByIntLongPairs(null, 100, 0));
		assertEquals(0, IntLongPairsUtils.getBasesCoveredByIntLongPairs(null, 100, 13));
		
		long l1 = NumberUtils.addShortToLong(1000000, (short)0, 40);
		long l2 = NumberUtils.addShortToLong(1000000, (short)50, 40);
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), l1);
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), l2);
		
		assertEquals(10 + 12 + 10 + 12, IntLongPairsUtils.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), l1);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(17, 0), l2);
		assertEquals(15 + 12 + 17 + 12, IntLongPairsUtils.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(50, 0), l1);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(30, 0), l2);
		assertEquals(50 + 30 + 12, IntLongPairsUtils.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
	}
	
	@Test
	public void getCoveredBases2() {
		/*
		 * {8=[IntLongPairs [pairs=[IntLongPair [i=786432, l=4611722303067418037], IntLongPair [i=1572864, l=4611691516741840313]]]]}
		 * IntLongPairs [pairs=[IntLongPair [i=786432, l=4611722303067418037], IntLongPair [i=1572864, l=4611691516741840313]]]
		 */
		IntLongPair p1 = new IntLongPair(786432, 4611722303067418037l);
		IntLongPair p2 = new IntLongPair(1572864, 4611691516741840313l);
		
		assertEquals(24 + 36 - 8, IntLongPairsUtils.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 57, 13));
		
	}
	
	@Test
	public void sortingRS() {
		/*
		 * [IntLongPair [i=1572864, l=4611691516741840313], IntLongPair [i=786432, l=4611722303067418037]]
		 */
		IntLongPair p1 = new IntLongPair(1572864, 4611691516741840313l);
		IntLongPair p2 = new IntLongPair(786432, 4611722303067418037l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtils.sortIntLongPairs(pairs, 57);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	@Test
	public void sorting() {
		IntLongPair p1 = new IntLongPair(589824, 315561373177018l);
		IntLongPair p2 = new IntLongPair(17956864, 1536005032);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtils.sortIntLongPairs(pairs, 308);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	@Test
	public void sorting2() {
		IntLongPair p1 = new IntLongPair(7012352, 75868643634102l);
		IntLongPair p2 = new IntLongPair(3145728, 4611816862119100198l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtils.sortIntLongPairs(pairs, 188);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	
	@Test
	public void sorting3() {
//IntLongPairs [pairs=[IntLongPair [i=2752512, l=4611705811171869861], IntLongPair [i=786432, l=4611775080404419812], IntLongPair [i=720896, l=87962594069495]]
		IntLongPair p1 = new IntLongPair(2752512, 4611705811171869861l);
		IntLongPair p2 = new IntLongPair(786432, 4611775080404419812l);
		IntLongPair p3 = new IntLongPair(720896, 87962594069495l);
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		long long3 = NumberUtils.getLongPositionValueFromPackedLong(p3.getLong());
		
		/*
		 * p1: fs start position is 33 (total length - (rs start + tileCount - 1)) => (105 - (18 + 54))
		 * p2: fs start position is 33 (total length - (rs start + tileCount - 1)) => (105 - (81 + 24))
		 * p3: fs start position is 80 (its hasn't been reverse complemented)
		 */
		
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		System.out.println("p3: tile count: " + NumberUtils.getPartOfPackedInt(p3.getInt(), true) + ", long position: " + long3 + ", start position in sequence: " + NumberUtils.getShortFromLong(p3.getLong(), 40));
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2, p3});
		IntLongPair[] sortedPairs = IntLongPairsUtils.sortIntLongPairs(pairs, 105);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
		assertEquals(p3, sortedPairs[2]);
	}
	
	@Test
	public void getStartPos() {
		IntLongPair p1 = new IntLongPair(2752512, 4611705811171869861l);
		assertEquals(33, IntLongPairsUtils.getStartPositionInSequence(p1, 105));
	}
	
	@Test
	public void doHashCodeAndEqualsWork() {
	//	IntLongPairs [pairs=[IntLongPair [i=2818048, l=4611725601128380012], IntLongPair [i=720896, l=4611699212824942509]
		IntLongPair p1 = new IntLongPair(2818048, 4611725601128380012l);
		IntLongPair p2 = new IntLongPair(720896, 4611699212824942509l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		
		IntLongPair p21 = new IntLongPair(2818048, 4611725601128380012l);
		IntLongPair p22 = new IntLongPair(720896, 4611699212824942509l);
		IntLongPairs pairs2 = new IntLongPairs(new IntLongPair[]{p21, p22});
		
		/*
		 * these 2 object are the same as far as we are concerned.
		 */
		assertEquals(pairs.hashCode(), pairs2.hashCode());
		assertEquals(true, pairs.equals(pairs2));
		Set<IntLongPairs> set = new HashSet<>();
		set.add(pairs);
		set.add(pairs2);
		assertEquals(1, set.size());
		
	}

}
