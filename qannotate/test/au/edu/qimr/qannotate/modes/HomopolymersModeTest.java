package au.edu.qimr.qannotate.modes;

import org.junit.Test;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import static org.junit.Assert.*;

public class HomopolymersModeTest {
	
	@Test
	public void startOfContig() {
		//SNP
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "1", null, "T", "A" });		
		HomopolymersMode homo = new HomopolymersMode(3,3);
		re = homo.annotate(re, getReference());
		assertEquals("0,tATG", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "0", null, "T", "A" });		
		homo = new HomopolymersMode(3,3);
		try {
			re = homo.annotate(re, getReference());
			fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae){}
    }

	@Test
	public void testGetReferenceBase() {
		// Setup
		byte[] reference = "GATTACA".getBytes();
		ChrRangePosition pos = new ChrRangePosition("chr1", 2, 2);
		SVTYPE type = SVTYPE.SNP;
		int homopolymerWindow = 2;

		// Call method under test
		byte[][] actualResult = HomopolymersMode.getReferenceBase(reference, pos, type, homopolymerWindow);

		// Create our expected result
		byte[][] expectedResult = new byte[][] {"G".getBytes(), "TT".getBytes()};

		// Assertion
		assertArrayEquals(expectedResult, actualResult);
	}
	
	@Test
	public void endOfContig() {
		//SNP
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "40", null, "T", "A" });		
		HomopolymersMode homo = new HomopolymersMode(3,3);
		re = homo.annotate(re, getReference());
		assertEquals("3,CCCt", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "41", null, "T", "A" });		
		homo = new HomopolymersMode(3,3);
		try {
            homo.annotate(re, getReference());
            fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored){}
    }
 	

	@Test
	public void testInsert(){				
		VcfRecord re = new VcfRecord( new String[] {"chr1", "21",null, "T", "TTAA" });
		
		//small insertion inside reference region
		HomopolymersMode homo = new HomopolymersMode(3,3);
		re = homo.annotate(re, getReference());
		assertEquals("3,CCTtaaCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));

		//check on edge of contig but report window still inside
		homo = new HomopolymersMode( 100, 10);
		re = homo.annotate(re, getReference());
        assertEquals("19,ATCGGACCCTtaaCCCCCCCCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
 		
		// report window also over edge
		homo = new HomopolymersMode( 100, 25);
		re = homo.annotate(re, getReference());
        assertEquals("19,AATGCAATTGGATCGGACCCTtaaCCCCCCCCCCCCCCCCCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
		
		//embed case
		re = new VcfRecord(new String[] {"chr1", "23", null, "C", "CCAC" });
		homo = new HomopolymersMode(3,3);
		re = homo.annotate(re, getReference());
        assertEquals("5,TCCcacCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
	}

	
	@Test
	public void testDel(){
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "T" });		
		HomopolymersMode homo = new HomopolymersMode(3,4);
		re = homo.annotate(re, getReference());
        assertEquals("5,CCT__CCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
 		
		//no repeats
		re = new VcfRecord(new String[] {  "chr1", "12", null, "AT", "A" });
		homo = new HomopolymersMode(100,10);
		re = homo.annotate(re, getReference());
        assertEquals("0,TGCAATTGGA_CGGACCCTCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));

		//motif of DEL contains repeat base same to adjacant homopolymers 
		re = new VcfRecord(new String[] {  "chr1", "18", null, "CCCTCC", "C" });
		homo = new HomopolymersMode(100,10);
		re = homo.annotate(re, getReference());
        assertEquals("19,TGGATCGGAC_____CCCCCCCCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
	}
	
	@Test
	public void testSNP(){
		//MNP
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "AGG" });		
		HomopolymersMode homo = new HomopolymersMode(3,4);
		re = homo.annotate(re, getReference());
		assertEquals("5,CCCtccCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertEquals("0,CCCaggCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP
		re = new VcfRecord(new String[] {  "chr1", "16", null, "G", "A" });		
		homo = new HomopolymersMode(10,5);
		re = homo.annotate(re, getReference());
//		assertEquals("2,GATCGaACCCT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertEquals("2,GATCGgACCCT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP in non homoplyers region
		re = new VcfRecord(new String[] {  "chr1", "13", null, "T", "A" });	
		re = homo.annotate(re, getReference());	 
		assertEquals("0,TTGGAtCGGAC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertEquals("2,TTGGAaCGGAC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "T", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTGCTGCTAGCTA".getBytes());	 
		assertEquals("10,TTTTTtGCTGC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "G", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTGCTGCTAGCTA".getBytes());	 
		assertEquals("9,TTTTTgGCTGC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertEquals("2,TTTTTgGCTGC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "G", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("10,TTTTTgTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertEquals("0,TTTTTgTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "A", "C" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("10,TTTTTaTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
//		assertEquals("0,TTTTTaTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "T", "C" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("20,TTTTTtTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
	}
	
	@Test
	public void findHomScore() {
		byte[][] b = new byte[2][];
		b[0] = "AAAAA".getBytes();
		b[1] = "AAAAA".getBytes();
		
		assertEquals(5, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(11, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(5, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(5, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(11, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife() {
		byte[][] b = new byte[2][];
		b[0] = "CTTTTCCC".getBytes();
		b[1] = "CCTTGGTT".getBytes();
		
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife2() {
		byte[][] b = new byte[2][];
		b[0] = "TTGACTCC".getBytes();
		b[1] = "TTTTTTAT".getBytes();
		
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(3, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(7, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife3() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(6, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife4() {
		byte[][] b = new byte[2][];
		b[0] = "CATAAATTTT".getBytes();
		b[1] = "TTTTTTTAAA".getBytes();
		
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(12, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(12, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife5() {
		byte[][] b = new byte[2][];
		b[0] = "TTGTTTTTTT".getBytes();
		b[1] = "AATTTCCAAA".getBytes();
		
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(8, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
//		assertEquals(3, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
//		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
//		assertEquals(8, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreCS() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "AA", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "CC", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "GG", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "TT", SVTYPE.SNP));
		assertEquals(4, HomopolymersMode.findHomopolymer(b, "TA", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "ATA", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "AT", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreMultipleAlts() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		try {
			HomopolymersMode.findHomopolymer(b, "A,T", SVTYPE.SNP);
			fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		assertEquals("6,GGTTTtTTGTT", HomopolymersMode.getHomopolymerData("A,T", b, SVTYPE.SNP, 10));
		assertEquals("3,GGTTTaTTGTT", HomopolymersMode.getHomopolymerData("A,C", b, SVTYPE.SNP, 10));
		assertEquals("6,GGTTTtTTGTT", HomopolymersMode.getHomopolymerData("A,C,T", b, SVTYPE.SNP, 10));
		assertEquals("3,GGTTTcTTGTT", HomopolymersMode.getHomopolymerData("C,G", b, SVTYPE.SNP, 10));
	}
	
	
	@Test
	public void findHomopolymer() {
		byte[][] b = new byte[2][];
		b[0] = "CTTGACCACATCCTATTTTATCAGCAGGGTCTTTATGACCTGTATCTCATGATATCAATCCTGCAGACCTCATCTATCTTTTTTTTTTTTTTTTTTTTTT".getBytes();
		b[1] = "AGACAAAGTCTCACTTTGTCACCCAGGCTGGAGTGCAATGACACCATCTCAGCTCACTGCAACTTCTGCCTCCCAGGTTCAAGCAATTCTCCTGCCTTAG".getBytes();
		
		assertEquals(22, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(22, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(22, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(23, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomopolymer2() {
		byte[][] b = new byte[2][];
		b[0] = "AAGAAAATAAAGCATATACAATCCTGGACTCCATAGATATAAAACTGTGATGTAATATCTGTATTGGTATCAAGTGATAAAACAACATTAAACTTTTCCC".getBytes();
		b[1] = "CCTTGGTTTTGGCTCTAAGATAGCAACTCTATCATTGACTTAGTTTTCAAACTATAGGTCACACACTCATTTTTTCACTCATGGGTCTTCATGAAAAGAT".getBytes();
		
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	@Test
	public void findHomopolymer3() {
		byte[][] b = new byte[2][];
		b[0] = "AGGATGACAGCCTCCAGCTGCATCTCTGTTGGAGAGTCAAATTACCTACAGTACCATCTAAATACTTGGAATCTGTCATTCCTGGAGCTGTGTTGACTCC".getBytes();
		b[1] = "TTTTTTATTCATTTTGTAATTCGGAACATTTTCTTTTATTTTTAAGGGAGTTTGGGTTAGCTTTCTATCAATCGTAAACAAAGGAAACCAGTCGAAAGTA".getBytes();
		
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomopolymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomopolymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHom() {
		byte[][] refs = new byte[2][];
		refs[0] = "TCAAGAGTTT".getBytes();
		refs[1] = "CTTTATTTTT".getBytes();
		
		assertEquals(3, HomopolymersMode.findHomopolymer(refs, "C", SVTYPE.SNP));
		assertEquals(3, HomopolymersMode.findHomopolymer(refs, "C", SVTYPE.SNP));
		
	}
	
	static int findHomopolymer(byte[][] updownReference, String motif, SVTYPE indelType){
		
		int upBaseCount = 1;
		int downBaseCount = 1;
 		//upstream - start from end since this is the side adjacent to the indel
		//decide if it is contiguous		
		int finalUpIndex = updownReference[0].length-1;	
		
		//count upstream homopolymer bases
		char nearBase = (char) updownReference[0][finalUpIndex];
		for (int i=finalUpIndex-1; i>=0; i--) {
			if (nearBase == updownReference[0][i]) {
				upBaseCount++;
			} else {
				break;
			}
		}
		
		//count downstream homopolymer
		nearBase = (char) updownReference[1][0];
		for (int i=1; i< updownReference[1].length; i++) {
			if (nearBase == updownReference[1][i]) {
				downBaseCount++;
			} else {
				break;
			}
		}
		
		int max;
		//reset up or down stream for deletion and SNPs reference base
		if(indelType.equals(SVTYPE.DEL) || indelType.equals(SVTYPE.SNP) || indelType.equals(SVTYPE.DNP)  
				|| indelType.equals(SVTYPE.ONP) || indelType.equals(SVTYPE.TNP) ){
			byte[] mByte = motif.getBytes();
			
			int left = 0;
			nearBase = (char) updownReference[0][finalUpIndex];
			for (byte b : mByte) {
				if (nearBase == b) {
					left ++;
				} else {
					break;				 
				}
			}
			upBaseCount += left;
						
			int right = 0;
			nearBase = (char) updownReference[1][0];
			for(int i = mByte.length -1; i >=0; i--) { 
				if (nearBase == mByte[i]) {
					right++;
				} else  {
					break;
				}
			}
			downBaseCount += right; 
			
			max = (left == right && left == mByte.length)? 
					(downBaseCount + upBaseCount - mByte.length) : Math.max(downBaseCount, upBaseCount);
						 			
		} else {
		    //INS don't have reference base
			max = (updownReference[0][finalUpIndex] == updownReference[1][0] )? 
					(downBaseCount + upBaseCount) : Math.max(downBaseCount, upBaseCount);
		}
					
		return (max == 1)? 0 : max;
	}
	

	private byte[] getReference(){				
		
	/**
		1  AATGC
		6  AATTG
		11 GATCG
		16 GACCC
		21 TCCCC
		26 CCCCC
		31 CCCCC
		36 CCCCC
	*/			
		return "AATGCAATTGGATCGGACCCTCCCCCCCCCCCCCCCCCCC".getBytes(); 	
	}		
}
