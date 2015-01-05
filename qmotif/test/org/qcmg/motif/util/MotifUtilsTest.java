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
import org.qcmg.common.util.ChrPositionUtils;

public class MotifUtilsTest {
	
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
		String existingMotif = null;
		existingMotif = MotifUtils.addMotifToString(existingMotif, null);
		assertEquals(null,  existingMotif);
		String newMotif = "ABCDEFG";
		existingMotif = MotifUtils.addMotifToString(existingMotif, newMotif);
		assertEquals(newMotif, existingMotif);
		existingMotif = MotifUtils.addMotifToString(existingMotif, newMotif);
		assertEquals(newMotif + MotifUtils.M_D + newMotif, existingMotif);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullChromosome() {
		MotifUtils.getPositionsForChromosome(null, null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void emptyChromosome() {
		MotifUtils.getPositionsForChromosome("", null);
	}
	
	@Test
	public void getChr1Positions() {
		List<ChrPosition> positions = new ArrayList<>();
		for (int i = 1 ; i < 25 ; i++) positions.add(new ChrPosition(i + "" , i));
		
		assertEquals(1, MotifUtils.getPositionsForChromosome("1", positions).size());
		assertEquals("1", MotifUtils.getPositionsForChromosome("1", positions).get(0).getChromosome());
		
		positions.add(new ChrPosition("chr1", 100));
		assertEquals(1, MotifUtils.getPositionsForChromosome("1", positions).size());
		positions.add(new ChrPosition("1", 100));
		assertEquals(2, MotifUtils.getPositionsForChromosome("1", positions).size());
	}
	
	@Test
	public void noOverlaps() {
		List<ChrPosition> existingPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrPosition cp = new ChrPosition("1", i * 1000, (i + 1)*1000 -1);
			existingPositions.add(cp);
		}
		
		List<ChrPosition> newPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrPosition cp = new ChrPosition("1", i * 1000000, (i + 1) * 1000000 -1);
			newPositions.add(cp);
		}
		List<ChrPosition> overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(0, overlap.size());
	}
	
	@Test
	public void oneOverlap() {
		List<ChrPosition> existingPositions = new ArrayList<>();
		for (int i = 1 ; i < 100 ; i++) {
			ChrPosition cp = new ChrPosition("1", i * 1000, (i + 1)*1000 -1);
			existingPositions.add(cp);
		}
		
		List<ChrPosition> newPositions = new ArrayList<>();
		ChrPosition cp = new ChrPosition("1", 1, 1000000);
		newPositions.add(cp);
		
		List<ChrPosition> overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(99, overlap.size());
		
		// change so that only 1overlap is in place
		cp = new ChrPosition("1", 6009, 6100);
		newPositions.clear();
		newPositions.add(cp);
		overlap = MotifUtils.getExistingOverlappingPositions(existingPositions, newPositions);
		assertEquals(1, overlap.size());
	}
	
	@Test
	public void getRegionWithNoIncludesOrExcludes() {
		int size = 1000000;
		int windowSize = 10000;
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, null, null);
		assertEquals((size / windowSize) + 1, regions.size());
		
		size = 10;
		windowSize = 1;
		regions = MotifUtils.getRegionMap("1", size, windowSize, new ArrayList<ChrPosition>(), null);
		assertEquals((size / windowSize) + 1, regions.size());
		
		size = 1;
		windowSize = 1;
		regions = MotifUtils.getRegionMap("1", size, windowSize, null, new ArrayList<ChrPosition>());
		assertEquals((size / windowSize) + 1, regions.size());
		
		size = 1000000;
		windowSize = 1;
		regions = MotifUtils.getRegionMap("1", size, windowSize, null, null);
		assertEquals((size / windowSize) + 1, regions.size());
		
		size = 1000000;
		windowSize = 1000000;
		regions = MotifUtils.getRegionMap("1", size, windowSize, new ArrayList<ChrPosition>(), new ArrayList<ChrPosition>());
		assertEquals((size / windowSize) + 1, regions.size());
	}
	
	@Test
	public void includesCoversWholeContig() {
		List<ChrPosition> includes = new ArrayList<>();
		ChrPosition includedCP = new ChrPosition("chr6_ssto_hap7", 1, 4928567);
		includes.add(includedCP);
		
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("chr6_ssto_hap7", 4928567, 100000, includes, null);
		
		//TODO this should be 1...
		assertEquals(2, regions.size());
	}
	
	@Test
	public void getRegionWithIncludesDirectSwap() {
		int size = 100;
		int windowSize = 10;
		List<ChrPosition> includes = new ArrayList<>();
		ChrPosition includedCP = new ChrPosition("1", 11, 20);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, includes, null);
		assertEquals((size / windowSize) + 1, regions.size());
		
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
		ChrPosition includedCP = new ChrPosition("1", 12, 15);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, includes, null);
		assertEquals((size / windowSize) + 3, regions.size());
		
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
		ChrPosition includedCP = new ChrPosition("1", 10, 50);
		includes.add(includedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, includes, null);
		assertEquals(8, regions.size());
		
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
		ChrPosition excludedCP = new ChrPosition("1", 11, 20);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, null, excludes);
		assertEquals((size / windowSize) + 1, regions.size());
		
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
		ChrPosition excludedCP = new ChrPosition("1", 10, 15);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, null, excludes);
		assertEquals((size / windowSize) + 2, regions.size());
		
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
		ChrPosition excludedCP = new ChrPosition("1", 10, 50);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, null, excludes);
		assertEquals(8, regions.size());
		
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
		ChrPosition includedCP = new ChrPosition("1", 5, 18);
		includes.add(includedCP);
		
		List<ChrPosition> excludes = new ArrayList<>();
		ChrPosition excludedCP = new ChrPosition("1", 27, 31);
		excludes.add(excludedCP);
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("1", size, windowSize, includes, excludes);
		assertEquals(13, regions.size());
		
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
	public void realLifeDataChrYGetRegions() {
		int size = 59373566;
		int windowSize = 10000;
		
		List<ChrPosition> includes = new ArrayList<>();
		includes.add(ChrPositionUtils.getChrPositionFromString("chrY:10001-12033"));
		includes.add(ChrPositionUtils.getChrPositionFromString("chrY:59360739-59363565"));
		
		Map<ChrPosition, RegionCounter> regions = MotifUtils.getRegionMap("chrY", size, windowSize, includes, null);
		List<ChrPosition> positions = new ArrayList<>(regions.keySet());
		Collections.sort(positions);
		
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
			for (int i = cp.getPosition() ; i <= cp.getEndPosition() ; i++) {
				if (i < size) coverage[i]++;
			}
		}
		
		// each position in the array should be equal to 1
		for (int i : coverage) assertEquals(1, i);
		
	}
	
}
