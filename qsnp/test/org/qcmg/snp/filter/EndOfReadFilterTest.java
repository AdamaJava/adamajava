package org.qcmg.snp.filter;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.snp.filters.EndOfReadFilter;

public class EndOfReadFilterTest {
	
	@Test
	public void testFilter() {
		SAMRecord sam = new SAMRecord(null);
		// setup Filter
		EndOfReadFilter filter = new EndOfReadFilter(5, 105);
		
		sam.setAlignmentStart(100);
		Cigar cigar = new Cigar();
		CigarElement ce = new CigarElement(10, CigarOperator.M);
		cigar.add(ce);
		sam.setCigar(cigar);
		
		Assert.assertEquals(true, filter.filterOut(sam));
		filter = new EndOfReadFilter(4, 105);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new EndOfReadFilter(3, 105);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new EndOfReadFilter(2, 105);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new EndOfReadFilter(1, 105);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new EndOfReadFilter(0, 105);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new EndOfReadFilter(0, 110);
		Assert.assertEquals(true, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeData() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(46334072);
		sam.setCigarString("13H6M1I67M4S15H");
		Assert.assertEquals(true, new EndOfReadFilter(5, 46334140).filterOut(sam));
	}
	
//	@Test
//	public void testFilterInsertion() {
//		SAMRecord sam = new SAMRecord(null);
//		
//		sam.setAlignmentStart(100);
//		Cigar cigar = new Cigar();
//		CigarElement ce = new CigarElement(10, CigarOperator.M);
//		CigarElement ce2 = new CigarElement(1, CigarOperator.I);
//		CigarElement ce3 = new CigarElement(10, CigarOperator.M);
//		cigar.add(ce);
//		cigar.add(ce2);
//		cigar.add(ce3);
//		sam.setCigar(cigar);
//		
//		for (int i = 99 ; i < 121 ; i++) {
//			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
//			if (i == 110 || i == 111)
//				Assert.assertEquals(true, filter.filterOut(sam));
//			else 
//				Assert.assertEquals(false, filter.filterOut(sam));
//		}
//	}
//	
//	@Test
//	public void testFilterDeletion() {
//		SAMRecord sam = new SAMRecord(null);
//		
//		sam.setAlignmentStart(100);
//		Cigar cigar = new Cigar();
//		CigarElement ce = new CigarElement(10, CigarOperator.M);
//		CigarElement ce2 = new CigarElement(3, CigarOperator.D);
//		CigarElement ce3 = new CigarElement(10, CigarOperator.M);
//		cigar.add(ce);
//		cigar.add(ce2);
//		cigar.add(ce3);
//		sam.setCigar(cigar);
//		
//		for (int i = 99 ; i < 125 ; i++) {
//			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
//			if (i == 109 || i == 113 )
//				Assert.assertEquals(true, filter.filterOut(sam));
////			if (i == 109 || i == 110 || i == 113 || i == 114)
////				Assert.assertEquals(true, filter.filterOut(sam));
//			else 
//				Assert.assertEquals(false, filter.filterOut(sam));
//		}
//	}
	
//	@Test
//	public void testFilter() {
//		SAMRecord sam = new SAMRecord(null);
//		// setup Filter
//		AdjacentIndelFilter filter = new AdjacentIndelFilter(105);
//		
//		sam.setAlignmentStart(100);
//		Cigar cigar = new Cigar();
//		CigarElement ce = new CigarElement(10, CigarOperator.M);
//		cigar.add(ce);
//		sam.setCigar(cigar);
//		
//		Assert.assertEquals(false, filter.filterOut(sam));
//		
//		// add a cigar element
//		CigarElement ce2 = new CigarElement(1, CigarOperator.I);
//		cigar.add(ce2);
//		filter = new AdjacentIndelFilter(110);
//		Assert.assertEquals(true, filter.filterOut(sam));
//		
//		// add a cigar element
//		CigarElement ce3 = new CigarElement(10, CigarOperator.M);
//		cigar.add(ce3);
//		filter = new AdjacentIndelFilter(120);
//		Assert.assertEquals(false, filter.filterOut(sam));
//		
//		// add a cigar element
//		CigarElement ce4 = new CigarElement(2, CigarOperator.D);
//		cigar.add(ce4);
//		filter = new AdjacentIndelFilter(121);
//		Assert.assertEquals(true, filter.filterOut(sam));
//		
//	}
//	

}
