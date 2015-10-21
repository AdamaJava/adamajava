package org.qcmg.snp.filter;

import net.sf.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.snp.filters.MultipleAdjacentSnpsFilter;

public class MultipleAdjacentSnpsFilterTest {
	
	// setup Filter
	private static final MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(1);
	
	@Test
	public void testFilter() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(1);
		sam.setCigarString("10M");
		sam.setAttribute("MD", null);
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setAttribute("MD", "");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setAttribute("MD", "ABCD");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setAttribute("MD",  "A0C0G");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setAttribute("MD",  "3A0C0G4");
		Assert.assertEquals(false, filter.filterOut(sam));
		Assert.assertEquals(false, new MultipleAdjacentSnpsFilter(2).filterOut(sam));
		Assert.assertEquals(false, new MultipleAdjacentSnpsFilter(3).filterOut(sam));
		Assert.assertEquals(true, new MultipleAdjacentSnpsFilter(4).filterOut(sam));
		Assert.assertEquals(true, new MultipleAdjacentSnpsFilter(5).filterOut(sam));
		Assert.assertEquals(true, new MultipleAdjacentSnpsFilter(6).filterOut(sam));
		Assert.assertEquals(false, new MultipleAdjacentSnpsFilter(7).filterOut(sam));
	}
	
	@Test
	public void testRealLifeExamples() {
		MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(55598227);
		SAMRecord sam = new SAMRecord(null);
		
		sam.setCigarString("53H1S89M10I10M");
		sam.setAttribute("MD", "90T2T0T2T1");
		sam.setAlignmentStart(55598134);
		sam.setReadString("TAAAGGGCATGGCTTTCCTCGCCTCCAAGAATGTAAGTGGGAGTGATTCTCTAAAGAGTTTTGTGTTTTGTTTTTTTGATTTTTTTTTTTGAGAACAGAGTCTTGCTTGT");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setCigarString("52H1S86M10I8M");
		sam.setAttribute("MD", "87T1T0T0T1T0");
		sam.setAlignmentStart(55598137);
		sam.setReadString("TGGGCATGGCTTTCCTCGCCTCCAAGAATGTAAGTGGGAGTGATTCTCTAAAGAGTTTTGTGTTTTGTTTTTTTGATTTTTTTTTTTGAGAACAGAATCTCGCTC");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("42H1S14M1I74M3I17M3I7M2I2M");
		sam.setAttribute("MD", "88T1T1T3T0T0T2T5C1G4");
		sam.setAlignmentStart(55598135);
		sam.setReadString("TAAGGGCATGGCTTTCCCTCGCCTCCAAGAATGTAAGTGGGAGTGATTCTCTAAAGAGTTTTGTGTTTTGTTTTTTTGATTTTTTTTTTTACAGTGTGTTTAGATTGGAGCCAAATAAAGTCCA");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setCigarString("46M1I12M2D25M3D21M2I15M51H");
		sam.setAttribute("MD", "58^GA2T0T9T0T0T9^GAG36");
		sam.setAlignmentStart(55598152);
		sam.setReadString("CGCCTCCAAGAATGTAAGTGGGAGTGATTCTCTAAAGAGTTTTGTGTTTTTGTTTTTTTTTGATTTTTTTTTGAGTTTTTTTTTAACAGAGCATTTTAGAGCCATTAAGTTAAAATGCAGAA");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("17M1I10M1I10M1I6M2I82M58S15H");
		sam.setAttribute("MD", "1C0T40G12G0A2T0T11T0T0T7G0A0G22G0T15");
		sam.setAlignmentStart(55598154);
		sam.setReadString("CTCCCAAGAATGTAAGTGGGGAGTGATTCCTCTAAAGAGTTTTTGTTGTTTTTGTTTTTTTTTTTGATTTTTTTTTTTGAGTTTTTTTTTTAACAGAGCATTTTAGAGCCATAAGTAAAATGCAGAATGTTACATTTTTGGAAAGCAAGCCGACCATTTTGGAACCGACACGAGGCTGCACTGCGAAG");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("52H1S61M1I13M1D31M");
		sam.setAttribute("MD", "74^T13T10T0T0T0T1T0T0");
		sam.setAlignmentStart(55598129);
		sam.setReadString("TGTGGCAAAGGGCATGGCTTTCCTCGCCTCCAAGAATGTAAGTGGGAGTGATTCTCTAAAGATGTTTTGTGTTTTGTTTTTTGATTTTTATTTTTTTTTTGAGATGG");
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeExamples2() {
		MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(41281419);
		SAMRecord sam = new SAMRecord(null);
		
		sam.setCigarString("42H5S55M1I38M5D21M");
		sam.setAttribute("MD", "68T21A2^TTTTT0T0T0T18");
		sam.setAlignmentStart(41281320);
		sam.setReadString("TAATGACAAATGGATTTTGGGAGTGACTCAAGAAGTGAAGAATGCACAAGAATGGATCACAAAGATGGAATTTAGCAAACCCTAGCCTTGCTTGTTTAAAAATTTTTTTTTTTAAGAATA");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("54M1I14M6D16M2I33M1S39H");
		sam.setAttribute("MD", "43T24^TTTTTT0T0T47");
		sam.setAlignmentStart(41281345);
		sam.setReadString("AGAAGTGAAGAATGCACAAGAATGGATCACAAGATGGAATTTAGCAAACCCTAGCCCTTGCTTGTTAAAAATTTTTTTTTTTAAGGAAATATCTGTAATGGTACTGACTTTGCTTGCTTTA");
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeExamples3() {
		MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(11288633);
		SAMRecord sam = new SAMRecord(null);
		
		sam.setCigarString("12M2I113M15H");
		sam.setAttribute("MD", "0G0A2A0A0G0G4G0G0G12G97");
		sam.setAlignmentStart(11288606);
		sam.setReadString("CCGGCTTTCATTTACCTCTCAGGGGCACACAGAATGCACAATTAAGAAGCTGAAGCACAGATGGATCTGTGCATGTGTGGTGCAGAGGAGAAAGAGAAGGATTGGGGTTTGAGGTACTTACTTCCCG");
		Assert.assertEquals(false, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeExamples4() {
		MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(41256085);
		SAMRecord sam = new SAMRecord(null);
		
		sam.setCigarString("29M1I29M1D60M1S41H");
		sam.setAttribute("MD", "4A2A0A0A0A0A0G1A0A0A0A5G34^T60");
		sam.setAlignmentStart(41256075);
		sam.setReadString("AAAATAACTCTCCATCTCAAAAAAAAAAGAAAGAAGAAGAAGAAGAAGAAAACAAATGGTTTACCAAGGAAGGATTTTCGGGTTCACTCTGTAGAAGTCTTTTGGCACGGTTTCTGTAGA");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("25M1I5M2D51M2I29M6S51H");
		sam.setAttribute("MD", "0A0A0A1A0A0G0A22^AG70G1T7");
		sam.setAlignmentStart(41256081);
		sam.setReadString("TCCATCTCAAAAAAAAAGAAAAGAACGAAGAAAGAAGAAGAAAACAAATGGTTTTACCAAGGAAGGATTTTCGGGTTCACTCGTTGTAGAAGTCTTTTGGCACTGGTTCTGTATGCCCA");
		Assert.assertEquals(true, filter.filterOut(sam));
	}
	
	@Test
	public void testRealLifeExamples5() {
		MultipleAdjacentSnpsFilter filter = new MultipleAdjacentSnpsFilter(27101507);
		SAMRecord sam = new SAMRecord(null);
		sam.setReadName("OHREF:1702:876");
		sam.setCigarString("13H9M2D56M1D26M1D9M15S18H");
		sam.setAttribute("MD", "9^CC56^G8A0G0A15^G9");
		sam.setAlignmentStart(27101429);
		sam.setReadString("AACTACCAGCCCACCAAGCATGCAGAATCACATTCCTCAGGTATCCAGCCCTGCTCCCCTGCCCCGCCAATGGCACACCGCACCTCTCCTACAAGTCTCCTCTGCACTCTGGATA");
		Assert.assertEquals(true, filter.filterOut(sam));
		
		sam.setCigarString("13H9M2D12M1D26M1D17M2D5M2D15M1D5M1I21M8S15H");
		sam.setAttribute("MD", "9^CC12^G7A18^C17^GC1A3^GA0G0A13^T26");
		sam.setAlignmentStart(27101429);
		sam.setReadString("AACTACCAGCCCACCAAGCATCAGAATCTCATTCCTCAGGTATCCAGCCTGCTCCCCTGCCCCGCCATGACACCGCACCTCTCCAGCAACGTCTCCATTCCTGCACTCTGGATGAAATG");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		sam.setCigarString("13H9M2D6M1D9M1D22M1D6M1D8M3D8M1D18M1D22M4S20H");
		sam.setAttribute("MD", "9^CC6^A9^A22^C6^C8^CGG0C2A2G1^G0A17^A22");
		sam.setAlignmentStart(27101429);
		sam.setReadString("AACTACCAGCCCACCAGCATGCAGATCACATTCCTCAGGTATCCAGCCTGCTCCCTGCCCGCACTGCACACCGCACCTCTCCTAGCAGTCTCCATTCCTGCACTCTGGATGA");
		Assert.assertEquals(false, filter.filterOut(sam));
		
		
	}
	
}
