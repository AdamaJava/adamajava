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
}
