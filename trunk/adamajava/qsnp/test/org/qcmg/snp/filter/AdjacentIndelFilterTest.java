package org.qcmg.snp.filter;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.snp.filters.AdjacentIndelFilter;

public class AdjacentIndelFilterTest {
	
	@Test
	public void testFilterNoIndel() {
		SAMRecord sam = new SAMRecord(null);
		// setup Filter
		AdjacentIndelFilter filter = new AdjacentIndelFilter(105);
		
		sam.setAlignmentStart(100);
		Cigar cigar = new Cigar();
		CigarElement ce = new CigarElement(10, CigarOperator.M);
		cigar.add(ce);
		sam.setCigar(cigar);
		
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(99);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(100);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(101);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(109);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(110);
		Assert.assertEquals(false, filter.filterOut(sam));
		filter = new AdjacentIndelFilter(111);
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	@Test
	public void testFilterInsertion() {
		SAMRecord sam = new SAMRecord(null);
		
		sam.setAlignmentStart(100);
		sam.setCigarString("10M1I10M");
		
		for (int i = 99 ; i < 121 ; i++) {
			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
			if (i == 110 ) Assert.assertEquals(true, filter.filterOut(sam));
			else  Assert.assertEquals(false, filter.filterOut(sam));
		}
		
		sam.setCigarString("10M2I10M");
		for (int i = 99 ; i < 125 ; i++) {
			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
			if (i == 110 ) Assert.assertEquals(true, filter.filterOut(sam));
			else Assert.assertEquals(false, filter.filterOut(sam));
		}
		
		sam.setCigarString("10M12I10M");
		for (int i = 99 ; i < 130 ; i++) {
			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
			if (i == 110 ) Assert.assertEquals(true, filter.filterOut(sam));
			else Assert.assertEquals(false, filter.filterOut(sam));
		}
	}
	
	@Test
	public void testFilterDeletion() {
		SAMRecord sam = new SAMRecord(null);
		
		sam.setAlignmentStart(100);
		sam.setCigarString("10M2D10M");
		
		for (int i = 99 ; i < 125 ; i++) {
			AdjacentIndelFilter filter = new AdjacentIndelFilter(i);
			if (i == 109 || i == 112 )
				Assert.assertEquals(true, filter.filterOut(sam));
//			if (i == 109 || i == 110 || i == 113 || i == 114)
//				Assert.assertEquals(true, filter.filterOut(sam));
			else 
				Assert.assertEquals(false, filter.filterOut(sam));
		}
	}
	
	@Test
	public void testRealLifeRecord() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(32911136);
		sam.setCigarString("23M1I11M1I80M1D19M4S23H");
		AdjacentIndelFilter filter = new AdjacentIndelFilter(32911254);
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeRecordFilterOut() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(32911149);
		//103M2D15M29H
		sam.setCigarString("103M2D15M29H");
		
		AdjacentIndelFilter filter = new AdjacentIndelFilter(32911254);
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911162);
		sam.setCigarString("90M2D16M5S42H");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911189);
		sam.setCigarString("63M2D15M1D37M29H");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911158);
		sam.setCigarString("94M2D20M32H");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911232);
		sam.setCigarString("25H11M1D8M2D15M1D4M2D32M1D52M");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911170);
		sam.setCigarString("82M2D15M9S44H");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAlignmentStart(32911137);
		sam.setCigarString("29M1I86M2D16M5S23H");
		Assert.assertEquals(true, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeRecordFilterIn() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(32911144);
		//103M2D15M29H
		sam.setCigarString("106M1D11M1I12M1S29H");
		
		AdjacentIndelFilter filter = new AdjacentIndelFilter(32911254);
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setCigarString("43H77M1I10M1I32M");
		sam.setAlignmentStart(32911166);
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	
	@Test
	public void testRealLifeRecordLongestCigarEver() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(32911153);
		//103M2D15M29H
		//                                                                                                         v - this is where the snp is adjacent to
		sam.setCigarString("6M3I5M1I4M2I14M1I8M1I4M1I4M1I15M1I30M1I11M1I19M1S35H");
		
		AdjacentIndelFilter filter = new AdjacentIndelFilter(32911254);
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("56H11S12M1I78M1I5M1I22M");
		Assert.assertEquals(false, filter.filterOut(sam));
		
	}
	
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
