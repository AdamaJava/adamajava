package org.qcmg.qbasepileup.snp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.qcmg.qbasepileup.QBasePileupException;

public class SnpPositionTest {
	
	String line;
	Integer[] columns;
	SnpPosition position;
	
	@After
	public void tearDown() {
		line = null;
		columns = null;
		position = null;
	}	
	
	@Test
	public void testConstructor() throws QBasePileupException {
		line = "test\tchr1:1234-1234\t";
		columns = new Integer[1];
		columns[0] = 1;
		SnpPosition p = new SnpPosition(line, columns);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1234, p.getEnd());
		assertEquals(1, p.getLength());
	}
	
	@Test
	public void testConstructorwithMultipleColumns() throws QBasePileupException {
		line = "test\tchr1\t1234\t1234\t";
		columns = new Integer[3];
		columns[0] = 1;
		columns[1] = 2;
		columns[2] = 3;
		SnpPosition p = new SnpPosition(line, columns);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1234, p.getEnd());
		assertEquals(1, p.getLength());
	}
	
	@Test
	public void testConstructorwithLongerPosition() throws QBasePileupException {
		line = "test\tchr1:1234-1235\t";
		columns = new Integer[1];
		columns[0] = 1;
		SnpPosition p = new SnpPosition(line, columns);
		assertEquals("chr1", p.getChromosome());
		assertEquals(1234, p.getStart());
		assertEquals(1235, p.getEnd());
		assertEquals(2, p.getLength());
	}
	
	@Test(expected=QBasePileupException.class)
	public void testConstructorthrowsException() throws QBasePileupException {
		line = "test\tchr11234-1235\t";
		columns = new Integer[1];
		columns[0] = 1;
		new SnpPosition(line, columns);
	}
	
	@Test
	public void testEquals() throws QBasePileupException {
		line = "test\tchr1:1234-1235\t";
		columns = new Integer[1];
		columns[0] = 1;
		SnpPosition p1 = new SnpPosition(line, columns);
		SnpPosition p2 = new SnpPosition(line, columns);
		
		assertTrue(p1.equals(p2));
		
		line = "test\tchr1:1234-1234\t";
		p2 = new SnpPosition(line, columns);
		assertFalse(p1.equals(p2));
	}


}
