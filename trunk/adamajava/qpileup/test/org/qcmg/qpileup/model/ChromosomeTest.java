package org.qcmg.qpileup.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.model.Chromosome;

public class ChromosomeTest {
	
//	@Test
//	public void testSortAList() {
//		Chromosome c1 = new Chromosome("chrMT", 123);
//		Chromosome c2 = new Chromosome("GL000199.1", 123);
//		Chromosome c3 = new Chromosome("chrX", 123);
//		Chromosome c4 = new Chromosome("chr10", 123);
//		Chromosome c5 = new Chromosome("chr1", 123);
//		Chromosome c6 = new Chromosome("chrY", 123);
//		
//		List<Chromosome> list = new ArrayList<Chromosome>();
//		list.add(c1);
//		list.add(c2);
//		list.add(c3);
//		list.add(c4);
//		list.add(c5);
//		list.add(c6);
//		
//		Collections.sort(list);
//		
//		assertEquals("chr1", list.get(0).getName());
//		assertEquals("chr10", list.get(1).getName());
//		assertEquals("chrX", list.get(2).getName());
//		assertEquals("chrY", list.get(3).getName());
//		assertEquals("chrMT", list.get(4).getName());
//		assertEquals("GL000199.1", list.get(5).getName());
//	}
	
	@Test
	public void testConstructorWith2Args() {
		Chromosome c = new Chromosome("chr1", 1000);
		assertEquals("chr1", c.getName());
		assertEquals(new Integer(1000), c.getTotalLength());
		assertEquals(new Integer(1000), c.getSectionLength());
		assertEquals(new Integer(1), c.getStartPos());
		assertEquals(new Integer(1000), c.getEndPos());
	}
	
	@Test
	public void testConstructorWith4Args() throws QPileupException {
		Chromosome c = new Chromosome("chr1", 1000, 1, 50);
		assertEquals("chr1", c.getName());
		assertEquals(new Integer(1000), c.getTotalLength());
		assertEquals(new Integer(50), c.getSectionLength());
		assertEquals(new Integer(1), c.getStartPos());
		assertEquals(new Integer(50), c.getEndPos());
	}
	
	@Test(expected=QPileupException.class)
	public void testConstructorWith4ArgsThrowsException() throws QPileupException {
		new Chromosome("chr1", 1000, 1, -2);
	}

}
