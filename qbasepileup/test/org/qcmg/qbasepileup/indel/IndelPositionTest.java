package org.qcmg.qbasepileup.indel;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;




public class IndelPositionTest {
	
	IndelPosition position;
	int[] cols = {23, 34, 25, 10, 12}; 
	
	@After
	public void tearDown() {
		position = null;
	}
	
	@Test
	public void testInsertion() {
		String line = "date\ttumour\tind6\t2\tchr1\t3237948\t3237949\t1\t-999\t-999\t-\t-999\tTC\t-/TC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		position = new IndelPosition(line, false, "pindel", cols);
		assertEquals("chr1", position.getChromosome());
		assertEquals(3237948, position.getStart());
		assertEquals(3237949, position.getEnd());
		assertEquals(2, position.getLength());
		assertEquals(IndelPosition.INS, position.getMutationType());
		byte[] expected = {84, 67};		
		assertEquals(expected[0], position.getMotif()[0]);
		assertEquals(expected[1], position.getMotif()[1]);
	}
	
	@Test
	public void testDeletion() {
		String line = "date\ttumour\tind6\t3\tchr1\t3217897\t3217899\t1\t-999\t-999\tAAA\t-999\t---\tAAA/---\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		position = new IndelPosition(line, false, "pindel", cols);
		assertEquals("chr1", position.getChromosome());
		assertEquals(3217897, position.getStart());
		assertEquals(3217899, position.getEnd());
		assertEquals(3, position.getLength());
		assertEquals(IndelPosition.DEL, position.getMutationType());
		byte[] expected = {65, 65, 65};		
		assertEquals(expected[0], position.getMotif()[0]);
		assertEquals(expected[1], position.getMotif()[1]);
		assertEquals(expected[2], position.getMotif()[2]);
	}
	
	@Test
	public void testComplex() {
		String line = "date\ttumour\tind6\t4\tchr1\t3522121\t3522133\t1\t-999\t-999\tCTCATACACTCA\t-999\tCACGCT\tCTCATACACTCA>CACGCT\t\t-999\t-999\t-999	-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		position = new IndelPosition(line, false, "pindel", cols);
		assertEquals("chr1", position.getChromosome());
		assertEquals(3522121, position.getStart());
		assertEquals(3522133, position.getEnd());
		assertEquals(IndelPosition.CTX, position.getMutationType());
	}
	

}
