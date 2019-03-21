package org.qcmg.picard.util;

import static org.junit.Assert.assertEquals;
import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;

public class SAMUtilsTest {

	@Test
	public void testGetIndexInReadFromPosition() {
		SAMRecord sam = new SAMRecord(null);
		
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 0));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 1));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 100000));
		assertEquals(0, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		
		sam.setAlignmentStart(1);
		sam.setReadString("A");
		sam.setCigarString("1M");
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 1));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 2));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 100000));
		assertEquals(1, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(2));	// 1-based offset for picard
		
		sam.setAlignmentStart(10000);
		sam.setReadString("AAACCCGGGTTT");
		sam.setCigarString("12M");
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 0));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(11, SAMUtils.getIndexInReadFromPosition(sam, 10011));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10012));
		
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10011, sam.getReferencePositionAtReadPosition(12));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(0));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(13));	// 1-based offset for picard
		
		// now put in a deletion
		sam.setCigarString("6M4D6M");
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 0));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(5, SAMUtils.getIndexInReadFromPosition(sam, 10005));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10006));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10009));
		assertEquals(6, SAMUtils.getIndexInReadFromPosition(sam, 10010));
		assertEquals(11, SAMUtils.getIndexInReadFromPosition(sam, 10015));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10016));
		
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10005, sam.getReferencePositionAtReadPosition(6));	// 1-based offset for picard
		assertEquals(10010, sam.getReferencePositionAtReadPosition(7));	// 1-based offset for picard
		assertEquals(10013, sam.getReferencePositionAtReadPosition(10));	// 1-based offset for picard
		assertEquals(10015, sam.getReferencePositionAtReadPosition(12));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(13));	// 1-based offset for picard
		
		// insertion time
		sam.setReadString("AAACCCGGGTTTACGT");
		sam.setCigarString("6M4I6M");
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(5, SAMUtils.getIndexInReadFromPosition(sam, 10005));
		assertEquals(10, SAMUtils.getIndexInReadFromPosition(sam, 10006));
		assertEquals(13, SAMUtils.getIndexInReadFromPosition(sam, 10009));
		assertEquals(15, SAMUtils.getIndexInReadFromPosition(sam, 10011));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10012));
		
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10005, sam.getReferencePositionAtReadPosition(6));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(7));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(10));	// 1-based offset for picard
		assertEquals(10007, sam.getReferencePositionAtReadPosition(12));	// 1-based offset for picard
		assertEquals(10008, sam.getReferencePositionAtReadPosition(13));	// 1-based offset for picard
		assertEquals(10011, sam.getReferencePositionAtReadPosition(16));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(17));	// 1-based offset for picard
	}
	
	@Test
	public void testGetIndexInReadFromPositionRealData() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(10000);
		// deletion
		sam.setReadString("CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC");
		sam.setCigarString("30M8D20M");
		
		assertEquals(0, sam.getReferencePositionAtReadPosition(0));	// 1-based offset for picard
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(1, SAMUtils.getIndexInReadFromPosition(sam, 10001));
		assertEquals(29, SAMUtils.getIndexInReadFromPosition(sam, 10029));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10030));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10037));
		assertEquals(30, SAMUtils.getIndexInReadFromPosition(sam, 10038));
		assertEquals(49, SAMUtils.getIndexInReadFromPosition(sam, 10057));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10058));
		
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10029, sam.getReferencePositionAtReadPosition(30));	// 1-based offset for picard
		assertEquals(10038, sam.getReferencePositionAtReadPosition(31));	// 1-based offset for picard
		assertEquals(10057, sam.getReferencePositionAtReadPosition(50));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(51));	// 1-based offset for picard
		
		// insertion
//		683_862_952	1105	chr1	6610753	4	7M3I40M	=	6610559	-240	TTTGTTTTGATGAGATGGAGTTTTGCTCTTGTTGCCCAGGCTGGAGTACA	%))))))),,?B0BII@.@?44,AIB7*@@:=/GIII=@IH9%%III<EI	ZC:i:6	MD:Z:3T43	RG:Z:201011090922324
//				43	NH:i:4	CM:i:3	NM:i:4	CQ:Z::@&7:9=%'36;&8:2@((6%<%&21;'&/&:'(91:)(;%(%%%%%%%%	CS:Z:T01131223123021003101102223100012201322213210001100	XW:Z:36_39
		sam.setCigarString("7M3I40M");
		sam.setReadString("TTTGTTTTGATGAGATGGAGTTTTGCTCTTGTTGCCCAGGCTGGAGTACA");
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(1, SAMUtils.getIndexInReadFromPosition(sam, 10001));
		assertEquals(6, SAMUtils.getIndexInReadFromPosition(sam, 10006));
		assertEquals(10, SAMUtils.getIndexInReadFromPosition(sam, 10007));
		assertEquals(12, SAMUtils.getIndexInReadFromPosition(sam, 10009));
		assertEquals(13, SAMUtils.getIndexInReadFromPosition(sam, 10010));
		assertEquals(49, SAMUtils.getIndexInReadFromPosition(sam, 10046));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10047));
		
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10006, sam.getReferencePositionAtReadPosition(7));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(8));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(9));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(10));	// 1-based offset for picard
		assertEquals(10007, sam.getReferencePositionAtReadPosition(11));	// 1-based offset for picard
		assertEquals(10046, sam.getReferencePositionAtReadPosition(50));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(51));	// 1-based offset for picard
		
		// regular
		sam.setCigarString("35M");
		sam.setReadString("CCTCTGAATGGAGGCCGAGGGCATTGAGGTTGCAT");
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 9999));
		assertEquals(0, SAMUtils.getIndexInReadFromPosition(sam, 10000));
		assertEquals(34, SAMUtils.getIndexInReadFromPosition(sam, 10034));
		assertEquals(-1, SAMUtils.getIndexInReadFromPosition(sam, 10035));
		
		assertEquals(0, sam.getReferencePositionAtReadPosition(0));	// 1-based offset for picard
		assertEquals(10000, sam.getReferencePositionAtReadPosition(1));	// 1-based offset for picard
		assertEquals(10034, sam.getReferencePositionAtReadPosition(35));	// 1-based offset for picard
		assertEquals(0, sam.getReferencePositionAtReadPosition(36));	// 1-based offset for picard
	}
	
	@Test
	public void testGetIndexInReadFromPositionRealData2() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(178954866);
		sam.setCigarString("6M1I29M1I6M1I18M1I11M1I9M1I13M2I15M2I7M1I22M3S28H");
		sam.setReadString("GGTAAGAAAATGACTGTTGGAAAATTATGCTTTCACTTTTCTACCCATATTCTCAGCTATACAAAAACCATTTATTTTTGAAGATTTTTTAGACTACTTGTTTAATTTGAAATCTTGTTTACTCTTTATTGTGAATTTTGTTTTTTTTTA");
		int index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954867);
		sam.setCigarString("136M26H");
		sam.setReadString("GTAAGAAATGACTGTTGGAAAATTATGCTTTCACTTTCTACCATATTCTCAGCTATACAAAACCATTTATTTTGAAGATTTTTAGACTACTGTTAATTTGAAATCTGTTACTCTTATTGTGGAATTTGTTTTTTTA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954874);
		sam.setCigarString("128M5S29H");
		sam.setReadString("ATGACTGTTGGAAAATTATGCTTTCACTTTCTACCATATTCTCAGCTATACAAAACCATTTATTTTGAAGATTTTTAGACTACTGTTAATTTGAAATCTGTTACTCTTATTGTGGAATTTGTTTTTTTTCATA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954882);
		sam.setCigarString("42H37M1I6M1D63M2I13M8I14M");
		sam.setReadString("TGGAAAATTATGCTTTCACTTTCTACCATATTCTCAGACTATACAAACCATTTATTTTGAAGATTTTTAGACTACTGTTAATTTGAAATCTGTTACTCTTATTGTGGGAATTTTGTTTTTTTTTTTAAAAAAAAAAGATGTTTT");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954902);
		sam.setCigarString("34H23M1I20M1I46M1I11M3I10M1I11M");
		sam.setReadString("TTCTACCATATCCTCAGCTATACTAAAACCATTTATTTTGAAGATTTTTTAGACTACTGTTAATTTGAAATCTGTTACTCTTATTGTGGAATTTTGTTTTTTTTTAAAAAAAGATGTTTTCTAATTGG");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954925);
		sam.setCigarString("42H3S11M1I38M1I17M1I11M2I10M1I12M2I20M1I9M");
		sam.setReadString("TCAAAAACCATTTATTTTTGAAGATTTTTAGACTACTGTTAATTTGAAATCTGTTTACTCTTATTGTGGAATTTTGTTTTTTTTAAAAAAAGATGTTTTCTAATTGGATTTTTTTAAAAGAAGAATGGAATTTTGGTTGC");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954954);
		sam.setCigarString("5M1I7M2I8M1I9M1I8M1I11M7I15M1I12M3I5M1I15M1I6M1I25M4S42H");
		sam.setReadString("ACCTGTTTAATTTTGAAAATCTGTTTACTCTTATTTGTGGAATTTTGTTTTTTTTTTTAAAAAAAAAGATGTTTCTAAATTGGATTTTTTTAAAAAGAAAGAATGGAATTTGGTTTGCTATTTTTACAATAGAACCTAAGCTTTTTTGTA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954984);
		sam.setCigarString("43H18M1D98M1D10M");
		sam.setReadString("TGTGGAATTTGTTTTTTTAAAAAGATGTTTCTAATTGGATTTTTAAAAGAAGAATGGAATTTGGTTGCTATTTTACAATAGAACCTAAGCTTTTTGTGGTTCTTAGTGTCCTATGTAAACTTAGTG");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('T', (char)sam.getReadBases()[index]);
		
		// deletion
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954990);
		sam.setCigarString("10M3D9M1D15M2D45M1D40M27H");
		sam.setReadString("AATTGTTTTTAAAAAGATGTTCTAATTGGATTTTAAAGAAGAATGGAATTTGGTTGCTATTTTACAATAGAACCTAAGCTTTTGTGGTTCTTAGTGTCCTATGTAAAACTTAGTGTCAA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals(-1, index);
		
		// and now for the reads with the mutation
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954997);
		sam.setCigarString("32M2I26M1I20M1I30M10S42H");
		sam.setReadString("TTTAAAAAAAAGATGTTTCTAATTGGATTTTTTAAAAAGAAGAATGGAATTTGGTTGCTATTTTTACAATAGAACCTAAGCTTTTTTGTGGTTCTTAGTGTCCTATGTAAAAACTTGGTGTA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('A', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954998);
		sam.setCigarString("26M1I89M1S42H");
		sam.setReadString("TTTAAAAAAAGATGTTTCTAATTGGATTTTTTAAAAGAAGAATGGAATTTGGTTGCTATTTTACAATAGAACCTAAGCTTTTTGTGGTTCTTAGTGTCCTATGTAAAACTTAGTGTA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('A', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954999);
		sam.setCigarString("114M1S42H");
		sam.setReadString("TTAAAAAAAGATGTTTCTAATTGGATTTTTAAAAGAAGAATGGAATTTGGTTGCTATTTTACAATAGAACCTAAGCTTTTTGTGGTTCTTAGTGTCCTATGTAAAACTTAGTGTA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('A', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(178954989);
		sam.setCigarString("127M27H");
		sam.setReadString("AATTTGTTTTTTAAAAAAGAATGTTTCTAATTGGATTTTTAAAAGAAGAATGGAATTTGGTTGCTATTTTACAATAGAACCTAAGCTTTTGGTGGTTCTTAGTGTCCTATGTAAAACTTAGTGTCAA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 178955001);
		assertEquals('A', (char)sam.getReadBases()[index]);
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(157985036);
		sam.setCigarString("101M");
		sam.setReadString("CCTGCGCCGTATGGGTCCTGGCGAGCACGTCTGAGGCTGGGGCCTGGGACAGGGCCTCCAGGAAGGGCAGGTGGCGTCGGAGGGCGTTGGCCAGGGCAGCA");
		index = SAMUtils.getIndexInReadFromPosition(sam, 157985136);
		assertEquals('A', (char)sam.getReadBases()[index]);
		
	}
	
	@Test
	public void testGetSAMRecordAsSting() {
		try {
			SAMUtils.getSAMRecordAsSting(null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		
		SAMRecord rec = new SAMRecord(null);
		assertEquals('\n', rec.getSAMString().charAt(rec.getSAMString().length() -1));
		Assert.assertTrue(rec.getSAMString().endsWith("\n"));
		
		String newSAMString = SAMUtils.getSAMRecordAsSting(rec);
		Assert.assertFalse(newSAMString.endsWith("\n"));
	}
	
	@Test
	public void testIsSAMRecordValid() {
		assertEquals(false, SAMUtils.isSAMRecordValid(null));
		SAMRecord sam = new SAMRecord(null);
		assertEquals(true, SAMUtils.isSAMRecordValid(sam));
		sam.setReadFailsVendorQualityCheckFlag(false);
		assertEquals(true, SAMUtils.isSAMRecordValid(sam));
		sam.setReadFailsVendorQualityCheckFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValid(sam));
 	}
	
	@Test
	public void testIsSAMRecordValidForVariantCalling() {
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(null));
		SAMRecord sam = new SAMRecord(null);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setReadFailsVendorQualityCheckFlag(false);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setReadFailsVendorQualityCheckFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		
		// dups
		sam.setReadFailsVendorQualityCheckFlag(false);
		sam.setDuplicateReadFlag(false);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setDuplicateReadFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		
		// unmapped
		sam.setDuplicateReadFlag(false);
		sam.setReadUnmappedFlag(false);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setReadUnmappedFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		
		// primary alignmant
		sam.setReadUnmappedFlag(false);
		sam.setNotPrimaryAlignmentFlag(false);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setNotPrimaryAlignmentFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
	}
	
	@Test
	public void supplementaryReads() {
		SAMRecord sam = new SAMRecord(null);
		sam.setSecondaryAlignment(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setSecondaryAlignment(false);
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
		sam.setSupplementaryAlignmentFlag(true);
		assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
	}
	
	@Test
	public void testIsSAMRecordValidForVariantCallingAllOptions() {
		
		/*
		 * flag values are 1 less than actual position as a right-shift operation is being performed
		 */
		int failsVendorCheck = 9;
		int duplicate = 10;
		int notPrimaryAlignment = 8;
		int unmapped = 2;
		int supplementary = 11;
		int secondary = 8;
		SAMRecord sam = new SAMRecord(null);
		
		for (int i = 0 ; i < 5000; i++) {
			sam.setFlags(i);
			
			if (((i >>> duplicate) & 1) != 0) {
				if (((i >>> unmapped) & 1) != 0 && ((i >>> notPrimaryAlignment) & 1) != 0 && ((i >>> failsVendorCheck) & 1) != 0) {
//					System.out.println("all flags set for " + i);
				} else if (((i >>> unmapped) & 1) != 0 && ((i >>> notPrimaryAlignment) & 1) != 0) {
//					System.out.println("duplicate, unmapped, npa flags set for " + i);
				} else if (((i >>> notPrimaryAlignment) & 1) != 0 && ((i >>> failsVendorCheck) & 1) != 0) {
//					System.out.println("duplicate, fvc, npa flags set for " + i);
				} else if (((i >>> unmapped) & 1) != 0 && ((i >>> failsVendorCheck) & 1) != 0) {
//					System.out.println("duplicate, fvc, unmapped flags set for " + i);
				} else if (((i >>> unmapped) & 1) != 0 ) {
//					System.out.println("duplicate, unmapped flags set for " + i);
				} else if (((i >>> notPrimaryAlignment) & 1) != 0 ) {
//					System.out.println("duplicate, npa flags set for " + i);
				} else if (((i >>> failsVendorCheck) & 1) != 0 ) {
//					System.out.println("duplicate, fvc flags set for " + i);
				} else {
//					System.out.println("duplicate flag set for " + i);
				}
				
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
				
			} else if (((i >>> failsVendorCheck) & 1) != 0) {
				if (((i >>> unmapped) & 1) != 0 && ((i >>> notPrimaryAlignment) & 1) != 0) {
//					System.out.println("fvc, unmapped, npa flags set for " + i);
				} else if (((i >>> unmapped) & 1) != 0 ) {
//					System.out.println("fvc, unmapped flags set for " + i);
				} else if (((i >>> notPrimaryAlignment) & 1) != 0 ) {
//					System.out.println("fvc, npa flags set for " + i);
				} else {
//					System.out.println("fvc flag set for " + i);
				}
				
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
				
			} else if (((i >>> notPrimaryAlignment) & 1) != 0) {
				if (((i >>> unmapped) & 1) != 0 ) {
//					System.out.println("npa, unmapped flags set for " + i);
				} else {
//					System.out.println("npa flag set for " + i);
				}
				
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
			} else if (((i >>> unmapped) & 1) != 0) {
//				System.out.println("unmapped flag set for " + i);
				
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
				
			} else if (((i >>> supplementary) & 1) != 0) {
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
			} else if (((i >>> secondary) & 1) != 0) {
				assertEquals(false, SAMUtils.isSAMRecordValidForVariantCalling(sam));
			} else {
				assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam));
			}
		}
	}
}
