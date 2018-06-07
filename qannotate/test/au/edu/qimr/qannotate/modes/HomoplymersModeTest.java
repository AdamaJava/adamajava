package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;


import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class HomoplymersModeTest {
 	

	@Test
	public void testInsert(){				
		VcfRecord re = new VcfRecord( new String[] {"chr1", "21",null, "T", "TTAA" });
		
		//small insertion inside reference region
		HomoplymersMode  homo = new HomoplymersMode(3,3);		
		re = homo.annotate(re, getReference());
		assertEquals("3,CCTtaaCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));

		//check on edge of contig but report window still inside
		homo = new HomoplymersMode( 100, 10);	
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,ATCGGACCCTtaaCCCCCCCCCC"));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
 		
		// report window also over edge
		homo = new HomoplymersMode( 100, 25);
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,AATGCAATTGGATCGGACCCTtaaCCCCCCCCCCCCCCCCCCC"));
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
		
		//embed case
		re = new VcfRecord(new String[] {"chr1", "23", null, "C", "CCAC" });
		homo = new HomoplymersMode(3,3);		
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,TCCcacCCC"));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
	}

	
	@Test
	public void testDel(){
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "T" });		
		HomoplymersMode  homo = new HomoplymersMode(3,4);	
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,CCT__CCC"));
//		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
 		
		//no repeats
		re = new VcfRecord(new String[] {  "chr1", "12", null, "AT", "A" });
		homo = new HomoplymersMode(100,10);
		re = homo.annotate(re, getReference());
		assertTrue(  re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("0,TGCAATTGGA_CGGACCCTCC") );

		//motif of DEL contains repeat base same to adjacant homopolymers 
		re = new VcfRecord(new String[] {  "chr1", "18", null, "CCCTCC", "C" });
		homo = new HomoplymersMode(100,10);
		re = homo.annotate(re, getReference());
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,TGGATCGGAC_____CCCCCCCCCC") );	
//		assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_HOM));
	}
	
	@Test
	public void testSNP(){
		//MNP
		VcfRecord re = new VcfRecord(new String[] {  "chr1", "21", null, "TCC", "AGG" });		
		HomoplymersMode  homo = new HomoplymersMode(3,4);	
		re = homo.annotate(re, getReference());
		assertEquals("0,CCCaggCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP
		re = new VcfRecord(new String[] {  "chr1", "16", null, "G", "A" });		
		homo = new HomoplymersMode(10,5);	
		re = homo.annotate(re, getReference());
		assertEquals("2,GATCGaACCCT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP in non homoplyers region
		re = new VcfRecord(new String[] {  "chr1", "13", null, "T", "A" });	
		re = homo.annotate(re, getReference());	 
		assertEquals("2,TTGGAaCGGAC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
	}
	
	@Test
	public void findHomScore() {
		byte[][] b = new byte[2][];
		b[0] = "AAAAA".getBytes();
		b[1] = "AAAAA".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(11, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife() {
		byte[][] b = new byte[2][];
		b[0] = "CTTTTCCC".getBytes();
		b[1] = "CCTTGGTT".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife2() {
		byte[][] b = new byte[2][];
		b[0] = "TTGACTCC".getBytes();
		b[1] = "TTTTTTAT".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife3() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife4() {
		byte[][] b = new byte[2][];
		b[0] = "CATAAATTTT".getBytes();
		b[1] = "TTTTTTTAAA".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(12, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife5() {
		byte[][] b = new byte[2][];
		b[0] = "TTGTTTTTTT".getBytes();
		b[1] = "AATTTCCAAA".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(8, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreCS() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "AA", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "CC", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "GG", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "TT", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "TA", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "ATA", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "AT", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreMultipleAlts() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		try {
			HomoplymersMode.findHomopolymer(b, "A,T", SVTYPE.SNP);
			fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		assertEquals("6,GGTTTtTTGTT", HomoplymersMode.getHomopolymerData("A,T", b, SVTYPE.SNP));
		assertEquals("0,GGTTTaTTGTT", HomoplymersMode.getHomopolymerData("A,C", b, SVTYPE.SNP));
		assertEquals("6,GGTTTtTTGTT", HomoplymersMode.getHomopolymerData("A,C,T", b, SVTYPE.SNP));
		assertEquals("0,GGTTTcTTGTT", HomoplymersMode.getHomopolymerData("C,G", b, SVTYPE.SNP));
	}
	
	
	@Test
	public void findHomopolymer() {
		byte[][] b = new byte[2][];
		b[0] = "CTTGACCACATCCTATTTTATCAGCAGGGTCTTTATGACCTGTATCTCATGATATCAATCCTGCAGACCTCATCTATCTTTTTTTTTTTTTTTTTTTTTT".getBytes();
		b[1] = "AGACAAAGTCTCACTTTGTCACCCAGGCTGGAGTGCAATGACACCATCTCAGCTCACTGCAACTTCTGCCTCCCAGGTTCAAGCAATTCTCCTGCCTTAG".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(2, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(23, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomopolymer2() {
		byte[][] b = new byte[2][];
		b[0] = "AAGAAAATAAAGCATATACAATCCTGGACTCCATAGATATAAAACTGTGATGTAATATCTGTATTGGTATCAAGTGATAAAACAACATTAAACTTTTCCC".getBytes();
		b[1] = "CCTTGGTTTTGGCTCTAAGATAGCAACTCTATCATTGACTTAGTTTTCAAACTATAGGTCACACACTCATTTTTTCACTCATGGGTCTTCATGAAAAGAT".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	@Test
	public void findHomopolymer3() {
		byte[][] b = new byte[2][];
		b[0] = "AGGATGACAGCCTCCAGCTGCATCTCTGTTGGAGAGTCAAATTACCTACAGTACCATCTAAATACTTGGAATCTGTCATTCCTGGAGCTGTGTTGACTCC".getBytes();
		b[1] = "TTTTTTATTCATTTTGTAATTCGGAACATTTTCTTTTATTTTTAAGGGAGTTTGGGTTAGCTTTCTATCAATCGTAAACAAAGGAAACCAGTCGAAAGTA".getBytes();
		
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(0, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
//	public byte[] getRef() throws IOException {
//		
//		   Map<String, byte[]> referenceBase = new HashMap<>();
//		   File indexFile = new File("/Users/oliverh/development/data/GRCh37_ICGC_standard_v2.fa.fai");
//		   FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
//		   byte[] b;
//		   try (IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(new File("/Users/oliverh/development/data/GRCh37_ICGC_standard_v2.fa"), index);) {
//			   ReferenceSequence chrBases = indexedFasta.getSequence("chr2");
//			   b = chrBases.getBases();
//			   referenceBase.put("chr2", b);
//		   }
//		
//		assertEquals(false, referenceBase.isEmpty());
//		return b;
//	}
//	
//	@Test
//	public void get2Darray() throws IOException {
//		byte[][] bb = HomoplymersMode.getReferenceBase(getRef(), new ChrRangePosition(new ChrPointPosition("chr2", 209421502)), SVTYPE.SNP, 100);
//		assertEquals(2, bb.length);
//		assertEquals(100, bb[0].length);
//		assertEquals(100, bb[1].length);
//		System.out.println(" bb[0]: " +  new String(bb[0]));
//		System.out.println(" bb[0]: " +  new String(bb[1]));
//	}
//	
	
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
	
		String ref = "AATGCAATTGGATCGGACCCTCCCCCCCCCCCCCCCCCCC"	;				  
		byte[] bytes = ref.getBytes();
		return bytes; 	
	}		
}
