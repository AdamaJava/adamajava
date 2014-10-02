package org.qcmg.qbasepileup.snp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.qcmg.qbasepileup.QBasePileupException;

public class SnpPositionTest {
	
	
	@Test
	public void testConstructor() throws QBasePileupException {
		SnpPosition p = new SnpPosition("test", "chr1", 1234, 1234);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1234, p.getEnd());
		assertEquals(1, p.getLength());
	}
	
	@Test
	public void testConstructorwithMultipleColumns() throws QBasePileupException {
		SnpPosition p = new SnpPosition("test", "chr1", 1234, 1234);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1234, p.getEnd());
		assertEquals(1, p.getLength());
	}
	
	@Test
	public void testConstructorwithLongerPosition() throws QBasePileupException {
		SnpPosition p = new SnpPosition("test", "chr1", 1234, 1235);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1235, p.getEnd());
		assertEquals(2, p.getLength());
	}
	
	@Test
	public void testEquals() throws QBasePileupException {
		SnpPosition p1 = new SnpPosition("test", "chr1", 1234, 1235);
		SnpPosition p2 = new SnpPosition("test", "chr1", 1234, 1235);
		
		assertTrue(p1.equals(p2));
		
		p2 = new SnpPosition("test", "chr1", 1234, 1234);
		assertFalse(p1.equals(p2));
	}
	
	@Test
	public void doesMurnIntochrMT() {
		SnpPosition p = new SnpPosition("test", "M", 1234, 1235);
		assertEquals("chrMT", p.getFullChromosome());
	}
	@Test
	public void doesMTurnIntochrMT() {
		SnpPosition p = new SnpPosition("test", "MT", 1234, 1235);
		assertEquals("chrMT", p.getFullChromosome());
	}

}
