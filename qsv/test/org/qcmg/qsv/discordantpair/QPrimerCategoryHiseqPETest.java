package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;



public class QPrimerCategoryHiseqPETest {
	
	QPrimerCategory category;
	
	@After
	public void tearDown() {
		category = null;
	}
	
	@Test
	public void testCountCategoriesWithCat1() {
		category = new QPrimerCategory("AAC", "chr1", "chr1", "id", "pe");
		
		category.countCategories(getClusterMatePair("AAC", "F2R1", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("1"));
		
		category = new QPrimerCategory("Cxx", "chr1", "chr2", "id", "pe");
		
		category.countCategories(getClusterMatePair("Cxx", "F1R2", "chr1", "chr2"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("1"));
	}
	
	@Test
	public void testCountCategoriesWithCat2() {
		category = new QPrimerCategory("ABA", "chr1", "chr1", "id", "pe");
		
		category.countCategories(getClusterMatePair("ABA", "R1F2", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("2"));
		
		
		category = new QPrimerCategory("Cxx", "chr1", "chr2", "id", "pe");		
		category.countCategories(getClusterMatePair("Cxx", "R2F1", "chr1", "chr2"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("2"));
	}
	
	@Test
	public void testCountCategoriesWithCat3() {
		category = new QPrimerCategory("BAA", "chr1", "chr1", "id", "pe");
		
		category.countCategories(getClusterMatePair("BAA", "F1F2", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("3"));
		
		
		category = new QPrimerCategory("Cxx", "chr1", "chr2", "id", "pe");		
		category.countCategories(getClusterMatePair("Cxx", "F2F1", "chr1", "chr2"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("3"));
	}
	
	@Test
	public void testCountCategoriesWithCat4() {
		category = new QPrimerCategory("BAA", "chr1", "chr1", "id", "pe");
		
		category.countCategories(getClusterMatePair("BAA", "R2R1", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("4"));
		
		
		category = new QPrimerCategory("Cxx", "chr1", "chr2", "id", "pe");		
		category.countCategories(getClusterMatePair("Cxx", "R1R2", "chr1", "chr2"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("4"));
	}
	
	@Test
	public void testCountCategoriesWithAABandABB() {
		category = new QPrimerCategory("AAB", "chr1", "chr1", "id", "pe");
		
		category.countCategories(getClusterMatePair("AAB", "R1F2", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("2"));
		
		
		category = new QPrimerCategory("ABB", "chr1", "chr1", "id", "pe");		
		category.countCategories(getClusterMatePair("ABB", "R2F1", "chr1", "chr1"));
		assertEquals(1, category.getMap().size());
		assertTrue(category.getMap().containsKey("2"));
	}
	
	@Test
	public void testFindCategoryNoOneCategory() throws Exception {
		category = new QPrimerCategory("AAB", "chr1", "chr1", "id", "pe");
		Map<String, AtomicInteger> map = new HashMap<>();
		map.put("1", new AtomicInteger(2));
		category.setMap(map);
		
		category.findCategoryNo();
		
		assertEquals("1", category.getPrimaryCategoryNo());
		assertEquals("", category.getMixedCategories());
	}
	
	@Test
	public void testFindCategoryNoTwoCategories() throws Exception {
		category = new QPrimerCategory("AAB", "chr1", "chr1", "id", "pe");
		Map<String, AtomicInteger> map = new HashMap<>();
		map.put("1", new AtomicInteger(2));
		map.put("2", new AtomicInteger(1));
		category.setMap(map);
		
		category.findCategoryNo();
		
		assertEquals("1", category.getPrimaryCategoryNo());
		assertEquals("Cat2(1),Cat1(2),", category.getMixedCategories());
	}

	@Test
	public void testCategory1() {
		category = new QPrimerCategory("AAC", "chr1", "chr1", "id", "pe");
		category.setPrimaryCategoryNo("1");
		category.findQPrimerSites(1000, 3000, 5000, 7000);
		
		assertEquals("id\tchr1:1000-2950\tchr1:5050-7000\tfalse\t1\t", category.toString("id"));
	}
	
	@Test
	public void testCategory2() {
		category = new QPrimerCategory("Cxx", "chr1", "chr2", "id", "pe");
		category.setPrimaryCategoryNo("2");
		category.findQPrimerSites(1000, 3000, 5000, 7000);
		
		assertEquals("id\tchr2:5000-6950\tchr1:1050-3000\tfalse\t2\t",  category.toString("id"));
	}
	
	@Test
	public void testCategory3() {
		category = new QPrimerCategory("Cxx", "chr2", "chr1", "id", "pe");
		category.setPrimaryCategoryNo("3");
		category.findQPrimerSites(1000, 3000, 5000, 7000);
		
		assertEquals("id\tchr1:1000-2950\tchr2:5000-6950\ttrue\t3\t",  category.toString("id"));
	}
	
	@Test
	public void testCategory4() {
		category = new QPrimerCategory("BAB", "chr1", "chr1", "id", "pe");
		category.setPrimaryCategoryNo("4");
		category.findQPrimerSites(1000, 3000, 5000, 7000);
		
		assertEquals("id\tchr1:1050-3000\tchr1:5050-7000\tlefttrue\t4\t",  category.toString("id"));
	}
	
	private List<MatePair> getClusterMatePair(String zp, String pairOrder, String leftChr, String rightChr) {
		String line = "test,"+leftChr+",1000,2000,"+zp+",167,false,test,"+rightChr+",5000,6000,zp,167,false,"+pairOrder;
		MatePair m = new MatePair(line);
		
		List<MatePair> list = new ArrayList<>();
		list.add(m);
		return list;
	}

	
	

}
