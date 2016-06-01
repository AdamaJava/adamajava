package org.qcmg.motif.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.ChrPositionUtils;

public class MotifUtilsTest {
	
//	public static final int buffer = 1000;
	
	@Test(expected=IllegalArgumentException.class)
	public void nullString() {
		MotifUtils.convertStringArrayToMap(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void emptyString() {
		MotifUtils.convertStringArrayToMap("");
	}
	
	@Test
	public void singleEntry() {
		Map<String, AtomicInteger> map = MotifUtils.convertStringArrayToMap("hello");
		assertEquals(1, map.size());
		assertEquals(1, map.get("hello").get());
	}
	
	@Test
	public void singleEntryx2() {
		Map<String, AtomicInteger> map = MotifUtils.convertStringArrayToMap("hello:hello");
		assertEquals(1, map.size());
		assertEquals(2, map.get("hello").get());
	}
	
	@Test
	public void singleEntryx10() {
		Map<String, AtomicInteger> map = MotifUtils.convertStringArrayToMap("hello:hello:hello:hello:hello:hello:hello:hello:hello:hello");
		assertEquals(1, map.size());
		assertEquals(10, map.get("hello").get());
	}
	
	@Test
	public void multipleDifferentEntries() {
		Map<String, AtomicInteger> map = MotifUtils.convertStringArrayToMap("hello:hello:world:I:like:crisps");
		assertEquals(5, map.size());
		assertEquals(2, map.get("hello").get());
		assertEquals(1, map.get("world").get());
		assertEquals(1, map.get("I").get());
		assertEquals(1, map.get("like").get());
		assertEquals(1, map.get("crisps").get());
	}
	
	@Test
	public void addMotifToString() {
		
		StringBuilder existingMotif =new StringBuilder();
		MotifUtils.addMotifToString(existingMotif, null);
		
		assertEquals(0, existingMotif.length() );
		String newMotif = "ABCDEFG";
		MotifUtils.addMotifToString(existingMotif, newMotif);
		assertEquals(newMotif,  existingMotif.toString());
		MotifUtils.addMotifToString(existingMotif, newMotif);
		assertEquals(newMotif + ":" + newMotif, existingMotif.toString());
		MotifUtils.addMotifToString(existingMotif, newMotif);
		assertEquals("ABCDEFG:ABCDEFG:ABCDEFG", existingMotif.toString());
//		assertEquals(newMotif + MotifUtils.M_D + newMotif, existingMotif.toString());
	}
//	@Test
//	public void addMotifToString() {
//		Map<String, AtomicInteger> existingMotif = null;
//		MotifUtils.addMotifToString(existingMotif, null);
//		assertEquals(null,  existingMotif);
//		String newMotif = "ABCDEFG";
//		MotifUtils.addMotifToString(existingMotif, newMotif);
//		assertEquals(null,  existingMotif);
//		existingMotif =new HashMap<>();
//		MotifUtils.addMotifToString(existingMotif, newMotif);
//		assertEquals(true, existingMotif.containsKey(newMotif));
//		MotifUtils.addMotifToString(existingMotif, newMotif);
//		assertEquals(2, existingMotif.get(newMotif).intValue());
////		assertEquals(newMotif + MotifUtils.M_D + newMotif, existingMotif.toString());
//	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullChromosome() {
		MotifUtils.getPositionsForChromosome(null, null);
	}
	
	@Test
	public void getChr1Positions() {
		List<ChrPosition> positions = new ArrayList<>();
		for (int i = 1 ; i < 25 ; i++) {
			positions.add(new ChrRangePosition(i + "" , i, i));
		}
		
		assertEquals(1, MotifUtils.getPositionsForChromosome(new ChrRangePosition("1",1,1), positions).size());
		assertEquals("1", MotifUtils.getPositionsForChromosome(new ChrRangePosition("1",1,1), positions).get(0).getChromosome());
		
		positions.add(new ChrRangePosition("chr1", 100,100));
		assertEquals(1, MotifUtils.getPositionsForChromosome(new ChrRangePosition("1",1,1), positions).size());
		positions.add(new ChrRangePosition("1", 1,1));
		assertEquals(2, MotifUtils.getPositionsForChromosome(new ChrRangePosition("1",1,1), positions).size());
	}
	
	@Test
	public void noOverlaps() {
		List<ChrPosition> existingPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrRangePosition cp = new ChrRangePosition("1", i * 1000, (i + 1)*1000 -1);
			existingPositions.add(cp);
		}
		
		List<ChrPosition> newPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrRangePosition cp = new ChrRangePosition("1", i * 1000000, (i + 1) * 1000000 -1);
			newPositions.add(cp);
		}
		List<ChrPosition> overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(0, overlap.size());
	}
	
	@Test
	public void oneOverlap() {
		List<ChrPosition> existingPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrRangePosition cp = new ChrRangePosition("1", i * 1000, (i + 1)*1000 -1);
			existingPositions.add(cp);
		}
		
		List<ChrPosition> newPositions = new ArrayList<>();
		ChrRangePosition cp = new ChrRangePosition("1", 1, 1000000);
		newPositions.add(cp);
		
		List<ChrPosition> overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(99, overlap.size());
		
		// change so that only 1overlap is in place
		cp = new ChrRangePosition("1", 6009, 6100);
		newPositions.clear();
		newPositions.add(cp);
		overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(1, overlap.size());
	}
	
	@Test
	public void windowSizeLargerThanContigSize() {
		int size = 1000;
		int windowSize = 10000;
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, null);
		assertEquals(1, regions.size());
		
		// get ChrPosition
		ChrRangePosition [] array = new ChrRangePosition[]{};
		array = regions.keySet().toArray(array);
		
		assertEquals(size, array[0].getEndPosition());
		assertEquals(1, array[0].getStartPosition());
		assertEquals("1", array[0].getChromosome());
	}
	
	@Test
	public void getRegionWithNoIncludesOrExcludes() {
		int size = 1000000;
		int windowSize = 10000;
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, null);
		assertEquals(((size) / windowSize), regions.size());
		
		size = 10;
		windowSize = 1;
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, new ArrayList<ChrPosition>(), null);
		assertEquals(((size) / windowSize), regions.size());
		
		size = 1;
		windowSize = 1;
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, new ArrayList<ChrPosition>());
		assertEquals((size ) / windowSize, regions.size());
		
		size = 1000000;
		windowSize = 1;
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, null);
		assertEquals((size ) / windowSize, regions.size());
		
		size = 1000000;
		windowSize = 1000000;
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals((size ) / windowSize, regions.size());
		
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, 11), 5, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals(3, regions.size());
		
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1,  14), 5, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals(3, regions.size());
		
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1,  15), 5, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals(3, regions.size());
		
		regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1,  16), 5, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals(4, regions.size());
	}
	
	@Test
	public void includesCoversWholeContig() {
		List<ChrPosition> includes = new ArrayList<>();
		ChrRangePosition includedCP = new ChrRangePosition("chr6_ssto_hap7", 1, 4928567);
		includes.add(includedCP);
		
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("chr6_ssto_hap7",1,  4928567), 100000, includes, null);
		
		//TODO this should be 1...
		assertEquals(1, regions.size());
		
		// increase contig length by 1
		regions = MotifUtils.getRegionMap(new ChrRangePosition("chr6_ssto_hap7", 1, 4928568), 100000, includes, null);
		assertEquals(2, regions.size());
		// decrease contig length by 2
		regions = MotifUtils.getRegionMap(new ChrRangePosition("chr6_ssto_hap7", 1, 4928566), 100000, includes, null);
		assertEquals(1, regions.size());
	}
	
	@Test
	public void getRegionWithIncludesDirectSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> includes = new ArrayList<>();
		ChrRangePosition includedCP = new ChrRangePosition("1", 11, 20);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1",1, size), windowSize, includes, null);
		assertEquals(((size ) / windowSize), regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(includedCP)) assertEquals(RegionType.INCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	@Test
	public void getRegionWithIncludesPartialSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> includes = new ArrayList<>();
		ChrRangePosition includedCP = new ChrRangePosition("1", 12, 15);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, includes, null);
		assertEquals(((size ) / windowSize) + 2, regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(includedCP)) assertEquals(RegionType.INCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	@Test
	public void getRegionWithIncludesMultipleSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> includes = new ArrayList<>();
		ChrRangePosition includedCP = new ChrRangePosition("1", 10, 50);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, includes, null);
		assertEquals(7, regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(includedCP)) assertEquals(RegionType.INCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	@Test
	public void getRegionWithExcludesDirectSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> excludes = new ArrayList<>();
		ChrRangePosition excludedCP = new ChrRangePosition("1", 11, 20);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, excludes);
		assertEquals(((size ) / windowSize), regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(excludedCP)) assertEquals(RegionType.EXCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	@Test
	public void getRegionWithExcludesPartialSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> excludes = new ArrayList<>();
		ChrRangePosition excludedCP = new ChrRangePosition("1", 10, 15);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1",1, size), windowSize, null, excludes);
		assertEquals(11, regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(excludedCP)) assertEquals(RegionType.EXCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	@Test
	public void getRegionWithExcludesMultipleSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> excludes = new ArrayList<>();
		ChrRangePosition excludedCP = new ChrRangePosition("1", 10, 50);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, null, excludes);
		assertEquals(7, regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(excludedCP)) assertEquals(RegionType.EXCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
	}
	
	
	@Test
	public void getRegionWithIncludesAndExcludes() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> includes = new ArrayList<>();
		ChrRangePosition includedCP = new ChrRangePosition("1", 5, 18);
		includes.add(includedCP);
		
		List<ChrPosition> excludes = new ArrayList<>();
		ChrRangePosition excludedCP = new ChrRangePosition("1", 27, 31);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("1", 1, size), windowSize, includes, excludes);
		assertEquals(12, regions.size());
		
		for (Entry<ChrPosition, RegionCounter> entry : regions.entrySet()) {
			if (entry.getKey().equals(includedCP)) assertEquals(RegionType.INCLUDES, entry.getValue().getType());
			else if (entry.getKey().equals(excludedCP)) assertEquals(RegionType.EXCLUDES, entry.getValue().getType());
			else assertEquals(RegionType.GENOMIC, entry.getValue().getType());
		}
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size);
		
	}
	
	
	@Test
	public void realLifeChr1() {
		int windowSize = 10000;
		ChrRangePosition contig = new ChrRangePosition("chr1", 1, 249250621);
		
		ChrRangePosition include = new ChrRangePosition("chr1", 10001,12464);
		List<ChrPosition> includes = new ArrayList<>();
		includes.add(include);
		
		int expectedRegions = contig.getLength() / windowSize;
		assertEquals(expectedRegions + 2, MotifUtils.getRegionMap(contig, windowSize, includes, null).size());
		
	}
	
	@Test
	public void realLifeChr16() {
		int windowSize = 10000;
		ChrRangePosition contig = new ChrRangePosition("chr16", 1, 90354753);
		
		//chr16p  chr16:60001-62033
//		chr16q  chr16:90292753-90294752
		ChrRangePosition include1 = new ChrRangePosition("chr16", 60001,62033);
		ChrRangePosition include2 = new ChrRangePosition("chr16", 90292753,90294752);
		
		List<ChrPosition> includes = new ArrayList<>();
		includes.add(include1);
		includes.add(include2);
		Map<ChrPosition, RegionCounter> map = MotifUtils.getRegionMap(contig, windowSize, includes, null);
		List<ChrPosition> list = new ArrayList<>(map.keySet());
		Collections.sort(list, new ChrPositionComparator());
		
		// check to see if chr16:59999-59999 is covered by map
		ChrRangePosition chr16CP = new ChrRangePosition("chr16", 59999, 59999);
		boolean foundIt = false;
		for (ChrPosition cp : list) {
			if (ChrPositionUtils.doChrPositionsOverlapPositionOnly(cp, chr16CP)) {
				foundIt = true;
			}
		}
		assertEquals(true, foundIt);
		
		int expectedRegions = contig.getLength() / windowSize;
		Collections.reverse(list);
		
		assertEquals(expectedRegions + 4, map.size());
		
	}
	
	@Test
	public void realLifeDataChrYGetRegions() {
		int size = 59373566;
		int windowSize = 10000;
		
		List<ChrPosition> includes = new ArrayList<>();
		includes.add(ChrPositionUtils.getChrPositionFromString("chrY:10001-12033"));
		includes.add(ChrPositionUtils.getChrPositionFromString("chrY:59360739-59363565"));
		
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap(new ChrRangePosition("chrY",1, size), windowSize, includes, null);
		List<ChrPosition> positions = new ArrayList<>(regions.keySet());
		Collections.sort(positions, new ChrPositionComparator());
		
		// want to check that I have uniform coverage over my region
		Set<ChrPosition> regionChrPos = regions.keySet();
		testUniformCoverage(regionChrPos, size, "chrY");
	}

	
	private void testUniformCoverage(Set<ChrPosition> regionChrPos, int size) {
		testUniformCoverage(regionChrPos, size, "1");
	}
	
	private void testUniformCoverage(Set<ChrPosition> regionChrPos, int size, String chr) {
		int[] coverage = new int[size];
		// 1-based so populate the first entry
		coverage[0] =1;
		
		for (ChrPosition cp : regionChrPos) {
			for (int i = cp.getStartPosition() ; i <= cp.getEndPosition() ; i++) {
				if (i < size) coverage[i]++;
			}
		}
		
		// each position in the array should be equal to 1
		for (int i : coverage) assertEquals(1, i);
		
	}
	
}
