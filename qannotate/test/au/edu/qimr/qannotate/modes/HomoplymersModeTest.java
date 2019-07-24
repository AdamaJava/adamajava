package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.Faidx;

public class HomoplymersModeTest {
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	private File refFile;
 
	
	@Before
	public void createSequenceFile() throws IOException {
		refFile = testFolder.newFile("test.fa");
		createFaFile(refFile.getAbsolutePath());
	}
	
	@Test
	public void refNameTest() throws IOException {
		String chr = "chrMT";
		//SNP  in strict mode  chrM != chrMT
		VcfRecord re = new VcfRecord(new String[] { chr, "1", null, "T", "A" });		
		HomoplymersMode  homo = new HomoplymersMode(null, 3,3, true);	
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
	    assertFalse(referenceBase.containsKey(chr));
		 
	  //SNP  in Not strict mode  chrM == chrMT
		homo = new HomoplymersMode(null, 3,3, false);	
		referenceBase = homo.getReferenceBase(refFile);
		assertTrue(referenceBase.containsKey(chr));
		
		
		//notStrict mode MT == chrM
		chr = "MT";
		String fvcf = testFolder.newFile("test.vcf").getAbsolutePath();
        try(BufferedWriter out = new BufferedWriter(new FileWriter(fvcf));) {    
        	 out.write("##fileformat=VCFv4.2\n"+VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "\n");    
        	 out.write(new VcfRecord(new String[] { chr, "1", null, "T", "A" }).toString());                          
         } 
        
      //not Strict mode MT != chrM no exception
		homo = new HomoplymersMode(fvcf, 3,3, false);
		homo.addAnnotation(refFile.getAbsolutePath());
		
		//Strict mode MT != chrM throw excption
		homo = new HomoplymersMode(fvcf, 3,3, true);
		try {
			homo.addAnnotation(refFile.getAbsolutePath());
			fail("Should have thrown an NullPointerException");
		} catch (NullPointerException iae){};
		
	}
	
	@Test
	public void startOfContig() throws IOException {
		String chr = "chr1";
		
		//SNP  in strict mode but both ref and vcf have chr1
		VcfRecord re = new VcfRecord(new String[] { chr, "1", null, "T", "A" });		
		HomoplymersMode  homo = new HomoplymersMode(null, 3,3, true);	
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
		
		re = homo.annotate(re,referenceBase.get(chr));
		assertEquals("0,tATG", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] { chr, "0", null, "T", "A" });		
		//homo = new HomoplymersMode(3,3);
		try {
			re = homo.annotate(re, referenceBase.get(chr));
			fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae){};
		

	}
	
	@Test
	public void endOfContig() throws IOException {
		String chr = "chr1";
		//SNP
		VcfRecord re = new VcfRecord(new String[] {  chr, "40", null, "T", "A" });		
		HomoplymersMode  homo = new HomoplymersMode(null, 3,3, true);
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
		
		re = homo.annotate(re, referenceBase.get(chr));
		assertEquals("3,CCCt", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {"chr1", "41", null, "T", "A" });			
		try {
			re = homo.annotate(re, referenceBase.get(chr));
			fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae){};
	}
	
	
 	

	@Test
	public void testInsert() throws IOException{	
		String chr = "chr1";
		VcfRecord re = new VcfRecord( new String[] {chr, "21",null, "T", "TTAA" });
		
		//small insertion inside reference region
		HomoplymersMode  homo = new HomoplymersMode(null, 3,3, true);	
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertEquals("3,CCTtaaCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));

		//check on edge of contig but report window still inside
		homo = new HomoplymersMode(null,  100, 10, true);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,ATCGGACCCTtaaCCCCCCCCCC"));
		assertTrue(StringUtils.isMissingDtaString(re.getFilter()));
 		
		// report window also over edge
		homo = new HomoplymersMode(null,  100, 25, true);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,AATGCAATTGGATCGGACCCTtaaCCCCCCCCCCCCCCCCCCC"));
		
		//embed case
		re = new VcfRecord(new String[] {"chr1", "23", null, "C", "CCAC" });
		homo = new HomoplymersMode(null, 3,3, true);	
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,TCCcacCCC"));
	}

	
	@Test
	public void testDel() throws IOException{
		String chr = "chr1";
		VcfRecord re = new VcfRecord(new String[] {  chr, "21", null, "TCC", "T" });		
		HomoplymersMode  homo = new HomoplymersMode(null, 3,4, true);	
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
		
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("5,CCT__CCC"));
 		
		//no repeats
		re = new VcfRecord(new String[] {  "chr1", "12", null, "AT", "A" });
		homo = new HomoplymersMode(null, 100,10, true);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(  re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("0,TGCAATTGGA_CGGACCCTCC") );

		//motif of DEL contains repeat base same to adjacant homopolymers 
		re = new VcfRecord(new String[] {  "chr1", "18", null, "CCCTCC", "C" });
		homo = new HomoplymersMode(null, 100,10, true);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM).equals("19,TGGATCGGAC_____CCCCCCCCCC") );	
	}
	
	@Test
	public void testSNP() throws IOException{
		String chr = "chr1";
		//MNP
		VcfRecord re = new VcfRecord(new String[] {  chr, "21", null, "TCC", "AGG" });		
		HomoplymersMode  homo = new HomoplymersMode(null, 3,4, true);
		Map<String, byte[]> referenceBase = homo.getReferenceBase(refFile);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertEquals("5,CCCtccCCC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP
		re = new VcfRecord(new String[] {  "chr1", "16", null, "G", "A" });		
		homo = new HomoplymersMode(null, 10,5, true);
		re = homo.annotate(re,  referenceBase.get(chr));
		assertEquals("2,GATCGgACCCT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		//SNP in non homoplyers region
		re = new VcfRecord(new String[] {  "chr1", "13", null, "T", "A" });	
		re = homo.annotate(re,  referenceBase.get(chr));	 
		assertEquals("0,TTGGAtCGGAC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "T", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTGCTGCTAGCTA".getBytes());	 
		assertEquals("10,TTTTTtGCTGC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "G", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTGCTGCTAGCTA".getBytes());	 
		assertEquals("9,TTTTTgGCTGC", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "G", "A" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("10,TTTTTgTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "A", "C" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("10,TTTTTaTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
		
		re = new VcfRecord(new String[] {  "chr1", "10", null, "T", "C" });	
		re = homo.annotate(re, "TTTTTTTTTTTTTTTTTTTT".getBytes());	 
		assertEquals("20,TTTTTtTTTTT", re.getInfoRecord().getField(VcfHeaderUtils.INFO_HOM));
	}
	
	@Test
	public void findHomScore() {
		byte[][] b = new byte[2][];
		b[0] = "AAAAA".getBytes();
		b[1] = "AAAAA".getBytes();
		
		assertEquals(5, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(11, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(5, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(5, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife() {
		byte[][] b = new byte[2][];
		b[0] = "CTTTTCCC".getBytes();
		b[1] = "CCTTGGTT".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife2() {
		byte[][] b = new byte[2][];
		b[0] = "TTGACTCC".getBytes();
		b[1] = "TTTTTTAT".getBytes();
		
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife3() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife4() {
		byte[][] b = new byte[2][];
		b[0] = "CATAAATTTT".getBytes();
		b[1] = "TTTTTTTAAA".getBytes();
		
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(12, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreRealLife5() {
		byte[][] b = new byte[2][];
		b[0] = "TTGTTTTTTT".getBytes();
		b[1] = "AATTTCCAAA".getBytes();
		
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(8, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomScoreCS() {
		byte[][] b = new byte[2][];
		b[0] = "GGTTT".getBytes();
		b[1] = "TTGTT".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "AA", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "CC", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "GG", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "TT", SVTYPE.SNP));
		assertEquals(4, HomoplymersMode.findHomopolymer(b, "TA", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "ATA", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "AT", SVTYPE.SNP));
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
		assertEquals("3,GGTTTaTTGTT", HomoplymersMode.getHomopolymerData("A,C", b, SVTYPE.SNP));
		assertEquals("6,GGTTTtTTGTT", HomoplymersMode.getHomopolymerData("A,C,T", b, SVTYPE.SNP));
		assertEquals("3,GGTTTcTTGTT", HomoplymersMode.getHomopolymerData("C,G", b, SVTYPE.SNP));
	}
	
	
	@Test
	public void findHomopolymer() {
		byte[][] b = new byte[2][];
		b[0] = "CTTGACCACATCCTATTTTATCAGCAGGGTCTTTATGACCTGTATCTCATGATATCAATCCTGCAGACCTCATCTATCTTTTTTTTTTTTTTTTTTTTTT".getBytes();
		b[1] = "AGACAAAGTCTCACTTTGTCACCCAGGCTGGAGTGCAATGACACCATCTCAGCTCACTGCAACTTCTGCCTCCCAGGTTCAAGCAATTCTCCTGCCTTAG".getBytes();
		
		assertEquals(22, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(22, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(22, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(23, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHomopolymer2() {
		byte[][] b = new byte[2][];
		b[0] = "AAGAAAATAAAGCATATACAATCCTGGACTCCATAGATATAAAACTGTGATGTAATATCTGTATTGGTATCAAGTGATAAAACAACATTAAACTTTTCCC".getBytes();
		b[1] = "CCTTGGTTTTGGCTCTAAGATAGCAACTCTATCATTGACTTAGTTTTCAAACTATAGGTCACACACTCATTTTTTCACTCATGGGTCTTCATGAAAAGAT".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	@Test
	public void findHomopolymer3() {
		byte[][] b = new byte[2][];
		b[0] = "AGGATGACAGCCTCCAGCTGCATCTCTGTTGGAGAGTCAAATTACCTACAGTACCATCTAAATACTTGGAATCTGTCATTCCTGGAGCTGTGTTGACTCC".getBytes();
		b[1] = "TTTTTTATTCATTTTGTAATTCGGAACATTTTCTTTTATTTTTAAGGGAGTTTGGGTTAGCTTTCTATCAATCGTAAACAAAGGAAACCAGTCGAAAGTA".getBytes();
		
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "A", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "G", SVTYPE.SNP));
		assertEquals(6, HomoplymersMode.findHomopolymer(b, "C", SVTYPE.SNP));
		assertEquals(7, HomoplymersMode.findHomopolymer(b, "T", SVTYPE.SNP));
	}
	
	@Test
	public void findHom() {
		byte[][] refs = new byte[2][];
		refs[0] = "TCAAGAGTTT".getBytes();
		refs[1] = "CTTTATTTTT".getBytes();
		
		assertEquals(3, HomoplymersMode.findHomopolymer(refs, "C", SVTYPE.SNP));
		assertEquals(3, HomoplymersMode.findHomopolymer(refs, "C", SVTYPE.SNP));
		
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
	
	public void  createFaFile(String file) throws IOException {    
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
    	final List<String> data = new ArrayList<>();
        data.add(">chr1  AC:CM000663.2  gi:568336023  LN:248956422  rl:Chromosome  M5:6aef897c3d6ff0c78aff06ac189178dd  AS:GRCh38");      
        data.add("AATGCAATTGGATCGGACCCTCCCCCCCCCCCCCCCCCCC");          	
        data.add(">chrM  AC:J01415.2  gi:113200490  LN:16569  rl:Mitochondrion  M5:c68f52674c9fb33aef52dcf399755519  AS:GRCh38  tp:circular");      
        data.add("AATGCAATTGGATCGGACCCTCCCCCCCCCCCCCCCCCCC");
 
        try(BufferedWriter out = new BufferedWriter(new FileWriter(file));) {          
           for (final String line : data)  out.write(line + "\n");
        } 
    	//creat index file
        Faidx idx = new Faidx(new File(file));
        idx.outputIdx(file + ".fai");
        idx.outputDict(file + ".dict");
        
	}
}
