package au.edu.qimr.tiledaligner.util;

import static org.junit.Assert.assertEquals;

import au.edu.qimr.tiledaligner.model.IntLongPair;
import au.edu.qimr.tiledaligner.model.IntLongPairs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.qcmg.common.util.NumberUtils;

public class IntLongPairsUtilTest {
	
	@Test
	public void getCoveredBases() {
		assertEquals(0, IntLongPairsUtil.getBasesCoveredByIntLongPairs(null, 100, 0));
		assertEquals(0, IntLongPairsUtil.getBasesCoveredByIntLongPairs(null, 100, 13));
		
		long l1 = NumberUtils.addShortToLong(1000000, (short)0, 40);
		long l2 = NumberUtils.addShortToLong(1000000, (short)50, 40);
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), l1);
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), l2);
		
		assertEquals(10 + 12 + 10 + 12, IntLongPairsUtil.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), l1);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(17, 0), l2);
		assertEquals(15 + 12 + 17 + 12, IntLongPairsUtil.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(50, 0), l1);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(30, 0), l2);
		assertEquals(50 + 30 + 12, IntLongPairsUtil.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 100, 13));
	}
	
	@Test
	public void getCoveredBases2() {
		/*
		 * {8=[IntLongPairs [pairs=[IntLongPair [i=786432, l=4611722303067418037], IntLongPair [i=1572864, l=4611691516741840313]]]]}
		 * IntLongPairs [pairs=[IntLongPair [i=786432, l=4611722303067418037], IntLongPair [i=1572864, l=4611691516741840313]]]
		 */
		IntLongPair p1 = new IntLongPair(786432, 4611722303067418037l);
		IntLongPair p2 = new IntLongPair(1572864, 4611691516741840313l);
		
		assertEquals(24 + 36 - 8, IntLongPairsUtil.getBasesCoveredByIntLongPairs(new IntLongPairs(p1, p2), 57, 13));
		
	}
	
	@Test
	public void sortingRS() {
		/*
		 * [IntLongPair [i=1572864, l=4611691516741840313], IntLongPair [i=786432, l=4611722303067418037]]
		 */
		IntLongPair p1 = new IntLongPair(1572864, 4611691516741840313l);
		IntLongPair p2 = new IntLongPair(786432, 4611722303067418037l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtil.sortIntLongPairs(pairs, 57);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	@Test
	public void sorting() {
		IntLongPair p1 = new IntLongPair(589824, 315561373177018l);
		IntLongPair p2 = new IntLongPair(17956864, 1536005032);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtil.sortIntLongPairs(pairs, 308);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	@Test
	public void sorting2() {
		IntLongPair p1 = new IntLongPair(7012352, 75868643634102l);
		IntLongPair p2 = new IntLongPair(3145728, 4611816862119100198l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		IntLongPair[] sortedPairs = IntLongPairsUtil.sortIntLongPairs(pairs, 188);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
	}
	
	@Test
	public void getMismatches() {
		IntLongPair p1 = new IntLongPair(7012352, 75868643634102l);
		IntLongPair p2 = new IntLongPair(3145728, 4611816862119100198l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(0, IntLongPairsUtil.getMismatches(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), 100000000l);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), 200000000l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(0, IntLongPairsUtil.getMismatches(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 1), 100000000l);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), 200000000l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(1, IntLongPairsUtil.getMismatches(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), 100000000l);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 1), 200000000l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(1, IntLongPairsUtil.getMismatches(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 20), 100000000l);
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 1), 200000000l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(21, IntLongPairsUtil.getMismatches(pairs));
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
		IntLongPair[] sortedPairs = IntLongPairsUtil.sortIntLongPairs(pairs, 105);
		assertEquals(p2, sortedPairs[0]);
		assertEquals(p1, sortedPairs[1]);
		assertEquals(p3, sortedPairs[2]);
	}
	
	@Test
	public void validForSingleRec() {
		IntLongPair p1 = new IntLongPair(2752512, 4611705811171869861l);
		IntLongPair p2 = new IntLongPair(786432, 4611775080404419812l);
		IntLongPair p3 = new IntLongPair(720896, 87962594069495l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2, p3});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(7012352, 75868643634102l);
		p2 = new IntLongPair(3145728, 4611816862119100198l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(786432, 4611722303067418037l);
		p2 = new IntLongPair(1572864, 4611691516741840313l);
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		/*
		 * correctly formed ILPs
		 */
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(1000l, (short)0, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(2000l, (short)40, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(true, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		/*
		 * stretch out the genomic distance a bit...
		 */
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(100000l, (short)0, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(599999l, (short)40, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(true, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		/*
		 * stretch out the genomic distance a bit too far...
		 */
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(100000l, (short)0, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(600001l, (short)40, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
	}
	
	@Test
	public void validForSingleRecWrongSeqOrder() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(2000l, (short)0, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(1000l, (short)40, 40));
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(1000l, (short)40, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(2000l, (short)0, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(3000l, (short)40, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(2000l, (short)0, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(true, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
	}
	
	@Test
	public void getBestMatching() {
		/*
		 * list
		 * [IntLongPair [i=2555905, l=160529393802964], IntLongPair [i=2228225, l=103354788666256], IntLongPair [i=786433, l=120946983753377], IntLongPair [i=720897, l=120946996001436], IntLongPair [i=720897, l=120947046443537], 
		 * IntLongPair [i=655361, l=117648551146731], IntLongPair [i=655361, l=122046505277795], IntLongPair [i=655361, l=122046603764683], IntLongPair [i=655360, l=4611864140062071056], IntLongPair [i=589825, l=83563602760479], 
		 * IntLongPair [i=589825, l=117648490513400], IntLongPair [i=589825, l=120947011893536], IntLongPair [i=589825, l=123145998326992]]
		 * pair
		 * IntLongPairs [pairs=[IntLongPair [i=4390912, l=695655218], IntLongPair [i=5570561, l=217703998447365]]]
		 * seqLength: 295
		 */
		IntLongPairs pairs = new IntLongPairs(new IntLongPair(4390912,695655218), new IntLongPair(5570561, 217703998447365l));
		List<IntLongPair> potentials = Arrays.asList(
				new IntLongPair(2555905, 160529393802964l),
				new IntLongPair(2228225, 103354788666256l),
				new IntLongPair(786433, 120946983753377l),
				new IntLongPair(720897, 120946996001436l),
				new IntLongPair(720897, 120947046443537l),
				new IntLongPair(655361, 117648551146731l),
				new IntLongPair(655361, 122046505277795l),
				new IntLongPair(655361, 122046603764683l),
				new IntLongPair(655361, 4611864140062071056l),
				new IntLongPair(589825, 83563602760479l),
				new IntLongPair(589825, 117648490513400l),
				new IntLongPair(589825, 120947011893536l),
				new IntLongPair(589825, 123145998326992l));
		
		System.out.println("pairs: " + pairs.toDetailedString());
		for (IntLongPair ilp : potentials) {
			System.out.println("potential partners: " + ilp.toDetailedString());
		}
		
		IntLongPairsUtil.addBestILPtoPairs(pairs, potentials);
		
		System.out.println("pairs: " + pairs.toDetailedString());
		assertEquals(4, pairs.getPairs().length);
		Arrays.sort(pairs.getPairs());
		assertEquals(true, Arrays.binarySearch(pairs.getPairs(), new IntLongPair(2555905, 160529393802964l)) >= 0);
		assertEquals(true, Arrays.binarySearch(pairs.getPairs(), new IntLongPair(2228225, 103354788666256l)) >= 0);
	}
	
	
	@Test
	public void isSubset() {
		
		List<IntLongPairs> existingPairs = Arrays.asList(new IntLongPairs(new IntLongPair[]{new IntLongPair(10,1000), new IntLongPair(20, 2000), new IntLongPair(30, 3000)}));
		assertEquals(true, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(10,1000), new IntLongPair(20, 2000))));
		assertEquals(true, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(10,1000), new IntLongPair(30, 3000))));
		assertEquals(true, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(20,2000), new IntLongPair(30, 3000))));
		assertEquals(false, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(40,4000), new IntLongPair(30, 3000))));
		assertEquals(false, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(20,2000), new IntLongPair(40, 4000))));
		assertEquals(false, IntLongPairsUtil.isPairsASubSetOfExistingPairs(existingPairs, new IntLongPairs(new IntLongPair(40,4000), new IntLongPair(10, 1000))));
	}
	
	@Test
	public void validForSingleRecWrongStrand() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(NumberUtils.setBit(100l, 62), (short)0, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(1000l, (short)40, 40));
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(NumberUtils.setBit(100l, 62), (short)0, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(NumberUtils.setBit(200l, 62), (short)0, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(true, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
		
		p1 = new IntLongPair(NumberUtils.pack2IntsInto1(20, 0), NumberUtils.addShortToLong(100l, (short)0, 40));
		p2 = new IntLongPair(NumberUtils.pack2IntsInto1(15, 0), NumberUtils.addShortToLong(NumberUtils.setBit(200l, 62), (short)0, 40));
		pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(pairs));
	}
	
	@Test
	public void addToPairs() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=3801095, l=20892605523929], IntLongPair [i=8192022, l=157232047215073]]]
		 * [IntLongPair [i=2097164, l=90161838073880], IntLongPair [i=851982, l=127545233418298]]
		 */
		
		IntLongPairs p = new IntLongPairs(new IntLongPair(3801095, 20892605523929l), new IntLongPair(8192022, 157232047215073l));
		List<IntLongPair> listOfILPs = Arrays.asList(new IntLongPair(2097164, 90161838073880l), new IntLongPair(851982, 127545233418298l));
		
		System.out.println("pairs: " + p.toDetailedString());
		for (IntLongPair ilp : listOfILPs) {
			System.out.println("potential partners: " + ilp.toDetailedString());
		}
		
		IntLongPairsUtil.addBestILPtoPairs(p, listOfILPs);
		
		assertEquals(3, p.getPairs().length);
		assertEquals(-1, Arrays.binarySearch(p.getPairs(), new IntLongPair(2097164, 90161838073880l)));
		
	}
	
	@Test
	public void isValidRealLife1() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=1048576, l=4611712409752715004], IntLongPair [i=851968, l=4611688220496904249]]]
		 * IntLongPairs [pairs=[IntLongPair [i=917504, l=4611714608830778186], IntLongPair [i=851968, l=4611688220550996199]]]
		 * IntLongPairs [pairs=[IntLongPair [i=1048576, l=4611712409807521324], IntLongPair [i=851968, l=4611688220550996199]]]
		 */
		IntLongPairs p = new IntLongPairs(new IntLongPair(1048576, 4611712409752715004l), new IntLongPair(851968, 4611688220496904249l));
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p));
		p = new IntLongPairs(new IntLongPair(917504, 4611714608830778186l), new IntLongPair(851968, 4611688220550996199l));
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p));
		p = new IntLongPairs(new IntLongPair(1048576, 4611712409807521324l), new IntLongPair(851968, 4611688220550996199l));
		assertEquals(false, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p));
		
		IntLongPair p11 = new IntLongPair(NumberUtils.getTileCount(12,0), 4611716807799226451l);
		IntLongPair p6 = new IntLongPair(NumberUtils.getTileCount(13,0), 4611688220496904249l);
		p = new IntLongPairs(p11, p6);
		assertEquals(true, IntLongPairsUtil.isIntLongPairsAValidSingleRecord(p));
	}
	
	@Test
	public void getStartPos() {
		IntLongPair p1 = new IntLongPair(2752512, 4611705811171869861l);
		assertEquals(33, IntLongPairsUtil.getStartPositionInSequence(p1, 105));
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
