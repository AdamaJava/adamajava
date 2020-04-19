package au.edu.qimr.clinvar.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.SequenceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

import au.edu.qimr.clinvar.model.Contig;
import au.edu.qimr.clinvar.model.Bin;
import au.edu.qimr.clinvar.model.Fragment;
import au.edu.qimr.clinvar.model.Fragment2;
import au.edu.qimr.clinvar.model.Probe;

public class ClinVarUtilTest {
	
	
	@Test
	public void getSWScoreRealData() {
		String diff =  "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||   ||||||||||||.|||||| |||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		String seq = "AGATGAAGTAGCCCAGGTAAATGTATGTTTGAGATTACTAGATAACTGTTGTACAAATTGGTATGTCACTTAAATTGTTTTCTCTCAGAAAGTCCACATAAATAAATGAAATAGACTAATAATAGTAATATGGTGTAG-AAAAAACTCCCTTAACATTATTTCCATAGATAAAACTAATTAGAACTGTAAATTCTAAGGAGATTATTTATCTAAACTAATTTTAAAATCAGAAGTTAAGGCAGTGTTTTAGATGGCTCATTCACAACTATCTTTCCC";
		String [] diffs = new String[] {"AGATGAAGTAGCCCAGGTAAATGTATGTTTGAGATTACTAGATAACTGTTGTACAAATTGGTATGTCACTTAAATTGTTTTCTCTCAGAAAGTCCACATAAATAAATGAAATAGAC---TAATAGTAATATAGTGTAGAAAAAAACACCCTTAACATTATTTCCATAGATAAAACTAATTAGAACTGTAAATTCTAAGGAGATTATTTATCTAAACTAATTTTAAAATCAGAAGTTAAGGCAGTGTTTTAGATGGCTCATTCACAACTATCTTTCCC",
		        diff, seq};
		
		assertEquals(271, ClinVarUtil.getSmithWatermanScore(diffs));
		
		seq = seq.replace("-", "");
		assertEquals(276, seq.length());
		
		diff = diff.replace(" ", "");
		diff = diff.replace(".", "");
		System.out.println("diff: " + diff);
		assertEquals(271, diff.length());
	}
	
	
	@Test
	public void getSwScore() {
		assertEquals(4, ClinVarUtil.getSmithWatermanScore(new String [] {"ABCD", "||||", "ABCD"}));
		assertEquals(3, ClinVarUtil.getSmithWatermanScore( new String[] {"BBCD", ".|||", "ABCD"}));
		assertEquals(2, ClinVarUtil.getSmithWatermanScore( new String[] {"BBBD", ".|.|", "ABCD"}));
		assertEquals(1, ClinVarUtil.getSmithWatermanScore( new String[] {"BBBB", ".|..", "ABCD"}));
		assertEquals(0,  ClinVarUtil.getSmithWatermanScore( new String[] {"BABB", "....", "ABCD"}));
		assertEquals(-1,  ClinVarUtil.getSmithWatermanScore( new String[] {"BABB", "....", "ABCD",""}));
	}
	
	@Test
	public void getMDDetails() {
		String [] diffs= new String [] {"ABCD", "||||", "ABCD"};
		assertEquals("4=", ClinVarUtil.getSWDetails(diffs));
		
		
		diffs= new String [] {"ABCC", "|||.", "ABCD"};
		assertEquals("3=1X", ClinVarUtil.getSWDetails(diffs));
		diffs= new String [] {"ABCDEF", "||| ||", "ABC-EF"};
		assertEquals("3=1D2=", ClinVarUtil.getSWDetails(diffs));
		diffs= new String [] {"AB---F", "||   |", "ABCDEF"};
		assertEquals("2=3I1=", ClinVarUtil.getSWDetails(diffs));
	}
	
	@Test
	public void getIndelCigar() {
		Probe p = new Probe(720, "TGAAACCACGACATCCAAGAGCCCAAA", "TTTGGGCTCTTGGATGTCGTGGTTTCA", "AAATTCATCATTGGGCTCACTTCTAAT", "ATTAGAAGTGAGCCCAATGATGAATTT", 138270462, 138270488, 138270710, 138270736, "CAGTTCTGAGGCCTTTCTCACCTAGGGCAGGGGAAAGAAGAGGTTTGCTGAGTGGATTGGGGTCCACCGAGCCTCCATGGCTTCTCTTAATCCCACTGAATTTCCTTTAAAAGAAACACTTCTTATATTGAACATATTGAGACCATTTGCTCATCTGCTTTGCCATAAACATCCCTGTCTCGATTCCTGCTTCTCCTCCCCCACGTGCTTACAGCTCTGGGATCACTTACAGTGACTTCCAGTTGGTTTGAGTGAATAAACACTTGCTGAGGGGTGGGGGTGAGCAAGGCGAGGTTGCCTCATGTCTTCTCCAAAGGGTCTGAGGGCAGGCGCTTCCAGGCTATTTAGATAAGAGCAGCCGTTGAGGGTGTTCTTCAGGAACAGGCTGCTGCCCAATCCTCTGCGACCTGCCCAGGCCCTGGTCTTGCAGAGGAGTAGGGGGCTCCCCTTCAAGCACAGCCCACCTGTGTCCACGAGGCTGGAGGTCAGGCCGGTGCTTCTTACCACCCCTGTCTGCCTCGTAGGTGGACAGCGCCATGTCCCTGATCCAGGCAGCCAAGAACTTGATGAATGCTGTGGTGCAGACAGTGAAGGCATCCTACGTCGCCTCTACCAAATACCAAAAGTCACAGGGTATGGCTTCCCTCAACCTTCCTGCTGTGTCATGGAAGATGAAGGCACCAGAGAAAAAGCCATTGGTGAAGAGAGAGAAACAGGATGAGACACAGACCAAGATTAAACGGGCATCTCAGAAGAAGCACGTGAACCCGGTGCAGGCCCTCAGCGAGTTCAAAGCTATGGACAGCATCTAAGTCTGCCCAGGCCGGCCGCCCCCACCCCTCGGGGCTCCTGAATATCAGTCACTGTTCGTCACTCAAATGAATTTGCTAAATACAACACTGATACTAGATTCCACAGGGAAATGGGCAGACTGAACCAGTCCAGGTGGTGAATTTTCCAAGAACATAGTTTAAGTTGATTAAAAATGCTTTTAGAATGCAGGAGCCTACTTCTAGCTGTATTTTTTGTATGCTTAAATAAAAATAAAAATTCATAACCAAAGAGAATCCCACATTAGCTTGTTAGTAATGCTCTGACCAAGCCGAGATGCCCATTCTCTTAGTGATGGCGGCGTTAGGGTTTGAGAGAAGGGAATTTGGCTCAACTTCAGTTGAGAGGGTGCAGTCCAGACAGCTTGACTGCTTTTAAATGACCAAAGATGACCTGTGGTAAGCAACCTGGGCATCTTAGGAAGCAGTCCCTGGAGAAGGCATGTTCCCAGAAAGGTCTCTGGAGGGACAAACTCACTCAGTAAAACATAATGTATCATGAAGAAAACTGATTCTCTATGACATGAAATGAAAATTTTAATGCATTGTTATAATTACTAATGTACGCTGCTGCAGGACATTAATAAAGTTGCTTTTTTAGGCTACAGTGTCTCGATGCCATAATCAGAACACACTTTTTTTCCTCTTTCTCCCAGCTTCAAATGCAAATTCATCATTGGGCTCACTTCTAATAACTGCAGTGTTTCCCGCCTTGGGCTTGCAGCAGAAAAACCTGACAACATAGTGTTTGCTAAGGCAGTAATTTAGACTTTACCTTATTTGTGATTACTGTAGTGATTGATTGATTGATTACTATTAACTACAAGGTATAATTTACTATCACCTTATTTAAATTTTATGAATTAATTTGAATGTTTTTTACACTAACTAACTTTTCCCAATAAAGTCCACTATGAAACCACGACATCCAAGAGCCCAAAGTCGTCTTCTCTGCCTTCAAGTCATAGATTTGCCCGCAGTATCTGTGGTGCTCTGGGCCCTCCCGGTGTCCGTCTCTTCCAGGATGGGGATGCCCGGGAGGGAAACTGTCTGTGGCTCTAGGCTGCACGGCTCGTGCCAACCCATCAGGGAGGGCCATGCCCGTTGTCCTATTGAGTGCCCCACCCTGCACCCCCACCTTGGGAATTCACATGTCCATTCCTTGAGGTTCATGTCAACCTCGGAGGCATCCCTGTCTTCATTATAGCTGACCCCTCTCCTGCGTCCTTCTGTCAGCATATCCCTTCTGCATCCTTCCCGTCACACATACATACCAAGCTATGATGATTGATTGATAGTGGCCTTCGAGATGAAAACCATCCTTAACCCCATGATCCTTCCCAGCTGGCATCCCCACCCTAAGCAAGGTTCCCTAAAGAGAAGCTTGTTGACATTTTCTCCCCTTCCTCACTTACAGTCAGCTGTCACCTTGCTCCCTCCACCTCCCCTCGT", 138268966, 138271248, "chr5", true, "CTNNA1_Exon_(17103700)");
		Bin b = new Bin(111261, "AAATTCATCATTGGGCTCACTTCTAATAACTGCAGTGTTTCCCGCCTTGGGCTTGCAGCAGAAAAACCTGACAACATAGAGTTTGCTAAGGCAGTAATTTAGACTTTACCTTATTTGTGATTACTGTAGTGATTGATTGATTACTATTAACTACAAGGTATAATTTACTATCACCTTATTTAAATTTTATGAATTAATTTGAATGTTTTTTACACTAACTAACTTTTCCCAATAAAGTCCACTATGAAACCACGACATCCAAGAGCCCAAA", 0);
		String ref = p.getReferenceSequence();
		String binSeq = b.getSequence();
		String [] swDiffs = ClinVarUtil.getSwDiffs(ref, binSeq);
		Cigar ceegar = ClinVarUtil.getCigarForIndels(ref, binSeq, swDiffs, p, b);
		assertEquals("129M4D141M", ceegar.toString());
		
	}
	
	@Test
	public void getNMMDData() {
		String seq = "TTTCTCAGCATACTTAAATGTCAAGAAATACAGAATCATGTCTTGAAGTTATTTAGAATTTCATGTTAATATATTGTGTTCTTTTTAACAGGAAGTACTTAAACAACTACAAGGAAGTATAAGTACTTCCTG";
		String refSeq = "TTTCTCAGCATACTTAAATGTCAAGAAATACAGAATCATGTCTTGAAGTTATTTAGAATTTCATGTTAATATATTGTGTTCTTTTTAACAGGAAGTACTTAAACAACTACAAGGAAGTATAAGTACTTCCTG";
		SAMRecord rec = new SAMRecord(null);
		rec.setReferenceName("blah");
		rec.setReadString(seq);
		rec.setMappingQuality(60);
//		rec.setCigar(cigar);
		/*
		 * Set the alignment start to 1, which is a hack to get around picards calculateMdAndNmTags method which is expecting the entire ref for the chromosome in question
		 * and we only have the amplicon ref seq.
		 * Reset once MD and NM have been calculated and set
		 */
		rec.setAlignmentStart(1);
		SequenceUtil.calculateMdAndNmTags(rec, refSeq.substring(0).getBytes(), true, true);
	}
	
	@Test
	public void getCoverageString() {
		ChrPosition cp = ChrPointPosition.valueOf("1", 100);
		Map<Contig, List<Fragment>> map = new HashMap<>();
		TIntArrayList headers = new TIntArrayList();
		headers.add(1);
		TIntArrayList headers10 = new TIntArrayList();
		for (int i = 0 ; i < 10 ; i++) {
			headers10.add(i +2);
		}
//		List<StringBuilder> headers = Arrays.asList(new StringBuilder[]{new StringBuilder("hello")});
//		List<StringBuilder> headers10 = Arrays.asList(new StringBuilder[]{new StringBuilder("hello"), new StringBuilder("hello"), new StringBuilder("hello"), new StringBuilder("hello"), new StringBuilder("hello"),new StringBuilder("hello"),new StringBuilder("hello"),new StringBuilder("hello"),new StringBuilder("hello"),new StringBuilder("hello")});
		
		assertEquals("0,0,0", ClinVarUtil.getCoverageStringAtPosition(cp, map));
		Contig a = new Contig(1, new ChrRangePosition("1", 101,200));
		Fragment f1 = new Fragment(1, null, headers, headers10, null, null);
		f1.setActualPosition(new ChrRangePosition("1", 101,200));
		map.put(a, Arrays.asList(f1));
		assertEquals("0,0,0", ClinVarUtil.getCoverageStringAtPosition(cp, map));
		
		Contig a2 = new Contig(2, new ChrRangePosition("1", 100,200));
		Fragment f2 = new Fragment(2, null, headers, headers, null, null);
		f2.setActualPosition(new ChrRangePosition("1", 100,200));
		map.put(a2, Arrays.asList(f2));
		assertEquals("1,1,2", ClinVarUtil.getCoverageStringAtPosition(cp, map));
		
		cp = ChrPointPosition.valueOf("1", 102);
		assertEquals("2,2,13", ClinVarUtil.getCoverageStringAtPosition(cp, map));
	}
	
	@Test
	public void vcfRecordFormatField() {
		Map<String, Map<Contig, List<Fragment2>>> contigAmpliconMap = new HashMap<>();
		VcfRecord vcf = VcfUtils.createVcfRecord(new ChrPointPosition("chr1", 12345), ".", "A", "C");
		ClinVarUtil.getCoverageStatsForVcf(vcf, contigAmpliconMap, new StringBuilder(), new StringBuilder(), new int[] {10,99});
		assertEquals("GT:AD:DP:FB:MR:OABS	.:.:.:.:.:.", vcf.getFormatFieldStrings());
		
		
		Map<Contig, List<Fragment2>> contig = new HashMap<>();
		Fragment2 fragment = new Fragment2(1, "CCCCCCCCCCCCCCCCCCCC");
		contig.put(new Contig(1, new ChrRangePosition("chr1", 12300, 12700)), Arrays.asList(fragment));
		contigAmpliconMap.put("chr1", contig);
		
		/*
		 * reset vcf record
		 */
		vcf = VcfUtils.createVcfRecord(new ChrPointPosition("chr1", 12345), ".", "A", "C");
		ClinVarUtil.getCoverageStatsForVcf(vcf, contigAmpliconMap, new StringBuilder(), new StringBuilder(), new int[] {10,99});
		assertEquals("GT:AD:DP:FB:MR:OABS	.:.:.:.:.:.", vcf.getFormatFieldStrings());
		
		/*
		 * need to activate the fragment by setting position
		 */
		fragment.setPosition(new ChrPointPosition("chr1", 12345), true);
		vcf = VcfUtils.createVcfRecord(new ChrPointPosition("chr1", 12345), ".", "A", "C");
		ClinVarUtil.getCoverageStatsForVcf(vcf, contigAmpliconMap, new StringBuilder(), new StringBuilder(), new int[] {10,99});
		assertEquals("GT:AD:DP:FB:MR:OABS	.:.:.:.:.:.", vcf.getFormatFieldStrings());
		
		/*
		 * add some start positions
		 */
		fragment.addPosition(12345);
		fragment.addPosition(12345);
		fragment.addPosition(12345);
		fragment.addPosition(12345);
		fragment.addPosition(12345);
		fragment.addPosition(12345);
		vcf = VcfUtils.createVcfRecord(new ChrPointPosition("chr1", 12345), ".", "A", "C");
		ClinVarUtil.getCoverageStatsForVcf(vcf, contigAmpliconMap, new StringBuilder(), new StringBuilder(), new int[] {10,99});
//		assertEquals("GT:AD:DP:FB:MR:OABS	0/1:89,10:6/1,1,6:6:.", vcf.getFormatFieldStrings());
		
	}
	
	@Test
	public void getAmpliconFragments() {
//		List<StringBuilder> headers = Arrays.asList(new StringBuilder[]{new StringBuilder("hello")});
//		List<StringBuilder> headers2 = Arrays.asList(new StringBuilder[]{new StringBuilder("hello"), new StringBuilder("there")});
		
		TIntArrayList headers = new TIntArrayList();
		headers.add(1);
		TIntArrayList headers2 = new TIntArrayList();
		for (int i = 0 ; i < 2 ; i++) {
			headers2.add(i +2);
		}
		Fragment2 f1 = new Fragment2(1, "ABC");
		Fragment2 f2 = new Fragment2(2, "ABCD");
		Fragment2 f3 = new Fragment2(3, "ABCDE");
		ChrPosition cp = new ChrRangePosition("1", 100, 200);
		f1.setPosition(cp, true);
		f2.setPosition(cp, true);
		f3.setPosition(cp, true);
		
		List<Fragment2> list = Arrays.asList(f1, f2, f3);
		Map<Contig, List<Fragment2>> groupedFrags = ClinVarUtil.groupFragments(list, 10);
		
		assertEquals(1, groupedFrags.size());
		assertEquals(list.size(), groupedFrags.get(new Contig(1,cp)).size());
		assertEquals(true, groupedFrags.get(new Contig(1,cp)).containsAll(list));
		
		Fragment2 f10 = new Fragment2(10, "123");
		Fragment2 f11 = new Fragment2(11, "1234");
		Fragment2 f12 = new Fragment2(12, "12345");
		f10.setPosition(new ChrRangePosition("1", 102, 208), true);
		f11.setPosition(new ChrRangePosition("1", 102, 208), true);
		f12.setPosition(new ChrRangePosition("1", 102, 208), true);
		list = Arrays.asList(f1, f2, f3, f10, f11, f12);
		
		groupedFrags = ClinVarUtil.groupFragments(list, 10); 
		assertEquals(1, groupedFrags.size());
		assertEquals(list.size(), groupedFrags.get(new Contig(1,cp)).size());
		assertEquals(true, groupedFrags.get(new Contig(1,cp)).containsAll(list));
	}
	
	@Test
	public void arePositionsClose() {
		TLongArrayList list1 = new TLongArrayList();
		list1.add(1000);
		assertEquals(true, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 0, 2000));
		assertEquals(true, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 999, 1000));
		assertEquals(true, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 1000, 1000));
		assertEquals(true, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 1000, 1001));
		assertEquals(false, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 1001, 1001));
		assertEquals(false, ClinVarUtil.areAllListPositionsWithinBoundary(list1, 999, 999));
	}
	
	@Test
	public void createSAMRecord() {
		try {
			ClinVarUtil.createSAMRecord(null, null, 1, 1, 1, "", "", 1, 1, "", 0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.createSAMRecord(null, null, 1, 1, 1, "ABCD", "", 1, 1, "", 0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		Cigar cigar = new Cigar();
		SAMRecord rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "ABCD", "chr1", 1, 1, "AAAA", 0);
		assertEquals("0", rec.getAttribute("MD"));	// no cigar elements defined
		
		CigarElement ce = new CigarElement(4, CigarOperator.MATCH_OR_MISMATCH);
		cigar.add(ce);
		rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "AAAA", "chr1", 1, 0, "AAAA", 0);
		assertEquals("4", rec.getAttribute("MD"));
		rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "ABCA", "chr1", 1, 0, "AAAA", 0);
//		System.out.println("rec: " + rec.getSAMString());
		assertEquals("1B0C1", rec.getAttribute("MD"));
	}
	
	@Test
	public void getIndelCigarRealLife() {
		String ref = "AGTTTGCAATAACAACTGATGTAAGTATTGCTCTTCTGCAGTCTTTATTAGCATTGTTTAAACGTACCTTTTTTTAAAAAAAAAAAAATAGGTCATTGCTTCTTGCTGATCTTGACAAAGAAGAAAAGGAAAAAGACTGGTATTACGCTCAACT";
		String sequence = "AGTTTGCAATAACAACTGATGTAAGTATTGCTCTTCTGCAGTCTTTATTAGCATTGTTTAAACGTACCTTTTTTTAAAAAAAAAAAAAAATAGGTCATTGCTTCTTGCTGATCTTGACAAAGAAGAAAAGGAAAAAGACTGGTATTACGCTCAACT";
		String [] swDiffs = ClinVarUtil.getSwDiffs(ref, sequence);
		ChrPosition cp = ChrPointPosition.valueOf("chr5", 112111235);
		Cigar ceegar = ClinVarUtil.getCigarForIndels(ref, sequence, swDiffs, cp);
		assertEquals("75M2I79M", ceegar.toString());
	}
	
	@Test
	public void getIndelCigarRealLife2() {
		String ref = "TTTCTCAGCATACTTAAATGTCAAGAAATACAGAATCATGTCTTGAAGTTATTTAGAATTTCATGTTAATATATTGTGTTCTTTTTAACAGGAAGTACTTAAACAACTACAAGGAAGTATTGAAGATGAAGCTATGGCTTCTTCTG";
		String sequence = "TTTCTCAGCATACTTAAATGTCAAGAAATACAGAATCATGTCTTGAAGTTATTTAGAATTTCATGTTAATATATTGTGTTCTTTTTAACAGGAAGTACTTAAACAACTACAAGGAAGTATAAGTACTTCCTG";
		String [] swDiffs = ClinVarUtil.getSwDiffs(ref, sequence, true);
		for (String s : swDiffs) {
			System.out.println("s: " + s);
		}
		ChrPosition cp = ChrPointPosition.valueOf("chr5", 112111235);
		Cigar ceegar = ClinVarUtil.getCigarForIndels(ref, sequence, swDiffs, cp);
		assertEquals("120M8D3M1D2M3D4M2D3M", ceegar.toString());
	}
	
	@Test
	public void getIndelCigarRealLife3() {
		
		/*
		 * 11:40:44.563 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - cigar: 51M53I-52M7I159M
11:40:44.563 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - ref: GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTCTTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - f.getActualPosition(): chr5:112178500-112178657
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - f.getSequence(): GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTCCTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - cigar: 51M53I-52M7I159M
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - s: GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTC-----------------------------------------------------T-------TCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - s: |||||||||||||||||||||||||||||||||||||||||||||||||||                                                     |       ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
11:40:44.564 [main] INFO au.edu.qimr.clinvar.Q3ClinVar2 - s: GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTCCTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT
		 */
		String ref = "GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTCTTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT";
		String sequence = "GATGAATAATGGTAATGGAGCCAATAAAAAGGTAGAACTTTCTAGAATGTCCTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTAAATCAAGTGGAAGTGAATCTGATAGATCAGAAAGACCTGTATTAGTACGCCAGTCAACTTTCATCAAAGAAGCTCCAAGCCCAACCTTAAGAAGAAAAT";
		String [] swDiffs = ClinVarUtil.getSwDiffs(ref, sequence, true);
		for (String s : swDiffs) {
			System.out.println("s: " + s);
		}
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr5:112178500-112178657");
		Cigar ceegar = ClinVarUtil.getCigarForIndels(ref, sequence, swDiffs, cp);
		assertEquals("51M53I1M7I106M", ceegar.toString());
	}
	
	@Test
	public void createSAMRecordIndelRealLife() {
		Cigar cigar = new Cigar();
		CigarElement ce = new CigarElement(75, CigarOperator.MATCH_OR_MISMATCH);
		cigar.add(ce);
		ce = new CigarElement(2, CigarOperator.INSERTION);
		cigar.add(ce);
		ce = new CigarElement(79, CigarOperator.MATCH_OR_MISMATCH);
		cigar.add(ce);
		
		String ref = "AGTTTGCAATAACAACTGATGTAAGTATTGCTCTTCTGCAGTCTTTATTAGCATTGTTTAAACGTACCTTTTTTTAAAAAAAAAAAAATAGGTCATTGCTTCTTGCTGATCTTGACAAAGAAGAAAAGGAAAAAGACTGGTATTACGCTCAACT";
		String sequence = "AGTTTGCAATAACAACTGATGTAAGTATTGCTCTTCTGCAGTCTTTATTAGCATTGTTTAAACGTACCTTTTTTTAAAAAAAAAAAAAAATAGGTCATTGCTTCTTGCTGATCTTGACAAAGAAGAAAAGGAAAAAGACTGGTATTACGCTCAACT";
		System.out.println("ref length: " + ref.length());
		System.out.println("sequence length: " + sequence.length());
		
		SAMRecord rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, ref, "chr5", 112111235, 0, sequence, 0);
		assertEquals("154", rec.getAttribute("MD"));
		assertEquals(2, rec.getAttribute("NM"));
		assertEquals(sequence.length(), rec.getReadLength());
		assertEquals(112111235, rec.getAlignmentStart());
		assertEquals(112111388, rec.getAlignmentEnd());
	}
	
//	@Test
//	public void fixSWResultsBothEndsDiff() {
//		assertArrayEquals(new String[] {"AABCDE",".||||.","XABCDF"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AABCDE", "XABCDF"));
//	}
	
	@Test
	public void fixSWResultsOneEndDiff() {
		assertArrayEquals(new String[] {"ABCD","||||","ABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCD", "ABCD"));
		assertArrayEquals(new String[] {"ABCDE","||||.","ABCDF"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDE", "ABCDF"));
		assertArrayEquals(new String[] {"ABCDEF","||||..","ABCDFG"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDEF", "ABCDFG"));
		assertArrayEquals(new String[] {"AABCD",".||||","XABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AABCD", "XABCD"));
		assertArrayEquals(new String[] {"AAABCD","..||||","ZXABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AAABCD", "ZXABCD"));
		assertArrayEquals(new String[] {"AAAAABCD","....||||","YYZXABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AAAAABCD", "YYZXABCD"));
		assertArrayEquals(new String[] {"AAAAABCD",".|..||||","YAZXABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AAAAABCD", "YAZXABCD"));
		assertArrayEquals(new String[] {"ABCDEFGH","||||..||","ABCDFGGH"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDEFGH", "ABCDFGGH"));
		assertArrayEquals(new String[] {"ABCDEFGHI","||||..||.","ABCDFGGHX"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDEFGHI", "ABCDFGGHX"));
	}
	
	@Test
	public void fixSWResultsNullOrEmpty() {
		try {
			ClinVarUtil.rescueSWData(null, null, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, "", null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, null, "");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, "", "");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void getSequenceDictonaryFromProbes() {
		List<Probe> list = new ArrayList<>();
		try {
			ClinVarUtil.getSequenceDictionaryFromProbes(null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(0, ClinVarUtil.getSequenceDictionaryFromProbes(list).getReferenceLength());
		
		Probe p1 = new Probe(1, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr1", false, null);
		list.add(p1);
		assertEquals(200, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		Probe p2 = new Probe(2, null, null, null, null, 100, 0, 0, 250, null, 0, 0, "chr1", false, null);
		list.add(p2);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		Probe p3 = new Probe(3, null, null, null, null, 100, 0, 0, 300, null, 0, 0, "chr2", false, null);
		list.add(p3);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		assertEquals(300, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr2").getSequenceLength());
		Probe p4 = new Probe(4, null, null, null, null, 100, 0, 0, 250, null, 0, 0, "chr2", false, null);
		list.add(p4);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		assertEquals(300, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr2").getSequenceLength());
	}
	
	@Test
	public void getOverlappingProbes() {
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr1:100-200");
		try {
			ClinVarUtil.getAmpliconsOverlappingPosition(null, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.getAmpliconsOverlappingPosition(cp, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		Set<Probe> set = new HashSet<>();
		assertEquals(0, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		
		// setup some probes
		Probe p1 = new Probe(1, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr1", false, null);
		assertEquals(p1.getCp(), cp);
		
		set.add(p1);
		assertEquals(1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(p1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).get(0));
		Probe p2 = new Probe(2, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr2", false, null);
		set.add(p2);
		assertEquals(1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(p1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).get(0));
		Probe p3 = new Probe(3, null, null, null, null, 99, 0, 0, 201, null, 0, 0, "chr1", false, null);
		set.add(p3);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p4 = new Probe(4, null, null, null, null, 201, 0, 0, 202, null, 0, 0, "chr1", false, null);
		set.add(p4);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p5 = new Probe(5, null, null, null, null, 200, 0, 0, 202, null, 0, 0, "chr1", false, null);
		set.add(p5);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p6 = new Probe(6, null, null, null, null, 10, 0, 0, 2000, null, 0, 0, "chr1", false, null);
		set.add(p6);
		assertEquals(3, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p6));
		
	}
	
	@Test
	public void trimResults() {
		TLongIntMap positionAndTiles = new TLongIntHashMap();
		positionAndTiles.put(1000,10);
		positionAndTiles.put(1013,9);
		positionAndTiles.put(1026,8);
		
		TIntObjectHashMap<TLongArrayList> results = ClinVarUtil.reduceStartPositionsAndTileCount(positionAndTiles, 1, 2, 13);
		assertEquals(1, results.size());
		assertEquals(1, results.get(10).size());
		assertEquals(1000, results.get(10).get(0));
		
		positionAndTiles = new TLongIntHashMap();
		positionAndTiles.put(1000,10);
		positionAndTiles.put(1013,9);
		positionAndTiles.put(1026,8);
		positionAndTiles.put(2000,10);
		positionAndTiles.put(2013,9);
		positionAndTiles.put(2026,8);
		
		results = ClinVarUtil.reduceStartPositionsAndTileCount(positionAndTiles, 1, 2, 13);
		assertEquals(1, results.size());
		assertEquals(2, results.get(10).size());
		assertEquals(true, results.get(10).contains(1000));
		assertEquals(true, results.get(10).contains(2000));
		
		positionAndTiles = new TLongIntHashMap();
		positionAndTiles.put(1000,10);
		positionAndTiles.put(1013,9);
		positionAndTiles.put(1026,8);
		positionAndTiles.put(2000,10);
		positionAndTiles.put(2013,9);
		positionAndTiles.put(2026,8);
		positionAndTiles.put(3000,9);
		positionAndTiles.put(3013,8);
		positionAndTiles.put(3026,7);
		
		results = ClinVarUtil.reduceStartPositionsAndTileCount(positionAndTiles, 1, 2, 13);
		assertEquals(2, results.size());
		assertEquals(2, results.get(10).size());
		assertEquals(true, results.get(10).contains(1000));
		assertEquals(true, results.get(10).contains(2000));
	}
	
	@Test
	public void getMapEntryWithHighestSWScore() {
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(null, 0));
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(new HashMap<ChrPosition, String[]>(), 0));
		
		HashMap<ChrPosition, String[]> map = new HashMap<>();
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr1:12345-12345");
		map.put(cp, new String[3]);
		
		assertEquals(cp, ClinVarUtil.getPositionWithBestScore(map, 0));
		ChrPosition cp2 = ChrPositionUtils.getChrPositionFromString("chr1:12346-12346");
		map.put(cp2, new String[3]);
		
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(map, 0));
		
		String[] swDiffs = new String[]{"ABCDEFG","|||||||","ABCDEFG"};
		map.put(cp2, swDiffs);
		assertEquals(cp2, ClinVarUtil.getPositionWithBestScore(map, 0));
		map.put(cp, swDiffs);
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(map, 0));
		
		String[] swDiffs2 = new String[]{"ABCDEFGH","||||||||","ABCDEFGH"};
		ChrPosition cp3 = ChrPositionUtils.getChrPositionFromString("chr1:12347-12347");
		map.put(cp3, swDiffs2);
		assertEquals(cp3, ClinVarUtil.getPositionWithBestScore(map, 0));
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(map, 1));
	}
	
	
	@Test
	public void editDistanceDist() {
		try {
			assertEquals("", ClinVarUtil.breakdownEditDistanceDistribution(null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		assertEquals("", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList()));
		assertEquals("1:1", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1})));
		assertEquals("1:2", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1})));
		assertEquals("1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,1})));
		assertEquals("2:1,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,1,2})));
		assertEquals("2:2,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,2,1,2})));
		assertEquals("12:1,2:2,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,2,12,2,1})));
	}
	
	
	@Test
	public void basicEditDistanceDouble() {
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getDoubleEditDistance(null, null, null, null, 0));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getDoubleEditDistance("hello", "ello", "hello", null, 0));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "XXX", "YYY", 2));
		assertArrayEquals(new int[] {3,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "XXX", "YYY", 3));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "YYY", 2));
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "DEF", "NOP", 2));
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "EFG", "NOP", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "NOP", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "KLM", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "KLM", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "JKL", 2));
		assertArrayEquals(new int[] {0,2}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "IJK", 2));
		assertArrayEquals(new int[] {0,1}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "XIJ", 2));
		assertArrayEquals(new int[] {0,0}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "HIJ", 2));
	}
	
	
	@Test
	public void getBestPosition() {
		
		long[][] tilePositions = new long[1][];
		tilePositions[0] = new long[]{100};
		TIntObjectHashMap<TLongArrayList> results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0, 0, 1);
		assertEquals(1, results.size());
		assertEquals(100, results.get(1).get(0));
		
		tilePositions[0] = new long[]{100, 1000};
		results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0, 0, 1);
		assertEquals(1, results.size());
		assertEquals(true, results.get(1).contains(100));
		assertEquals(true, results.get(1).contains(1000));
		tilePositions[0] = new long[]{100, 1000,10000};
		results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0, 0, 1);
		assertEquals(1, results.size());
		assertEquals(true, results.get(1).contains(100));
		assertEquals(true, results.get(1).contains(1000));
		assertEquals(true, results.get(1).contains(10000));
	}
	
	@Test
	public void bestPositionsRealLifeData() {
		/*
		 * QIMR13579:data oliverh$ grep -w TCAAAAAAAGGAA q3tiledaligner.out.gz.condensed
TCAAAAAAAGGAA	C643
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$ grep -w TTCCATAACTTCT q3tiledaligner.out.gz.condensed
TTCCATAACTTCT	54441301,81004583,91993417,119383169,158841860,171177226,187449800,195842600,238696762,246645164,249907876,263512805,290664374,413525506,442042580,516502511,532313353,543900614,545914178,557033451,570539881,619567319,660817481,692183056,720334805,737404231,763939540,780329395,825243023,840024227,844739893,894152354,904823369,906179614,910993866,916934338,938768668,949235902,970632708,983875719,1001889192,1027416205,1043834398,1079585857,1135078366,1212848363,1241051369,1241743198,1250009141,1261914836,1266039985,1353060209,1385926859,1398339451,1422701441,1493785729,1499634944,1632342192,1634265466,1660879831,1683113904,1688546475,1696070843,1724658502,1730534307,1736825538,1744955547,1763612917,1846691662,1882841257,1884705941,1900518458,1938961645,1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2010369283,2028165352,2055169346,2057690489,2059613402,2113415446,2125704662,2131365251,2142635021,2151129028,2158614647,2175299682,2245537376,2272456933,2330078621,2332649455,2351038537,2364825490,2387773275,2404946193,2432058312,2463593115,2539267700,2539268013,2543194015,2545546255,2547462156,2569183788,2587608326,2621061713,2646899912,2654728923,2716688667,2758309228,2763598036,2766568503,2773002306,2898978353,2901924904,2918511117,2935092487,2944787668,2952404849,2953877262,2976620086,2992612126,3016130929,3031260578
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$ grep -w TGCTAAGTCCTGA q3tiledaligner.out.gz.condensed
TGCTAAGTCCTGA	68236928,181877274,289174219,379871042,407464590,433734841,531132793,617626140,635267381,715900883,760044870,800473049,848398617,917477066,1009939623,1125070610,1169151520,1175631072,1185537588,1360190696,1414582806,1427478754,1427478845,1427478936,1427479027,1519792815,1611882893,1643538072,1793223481,1838300648,1861258204,1880846658,1976293002,2017227347,2173524984,2186432022,2282138469,2337043497,2463890593,2486972151,2531042628,2549985692,2611145621,2627424189,2636419256,2849128607,2897877268,2898412131,2910339381,2972513655,3020093124
QIMR13579:data oliverh$
		 */
		long[][] tilePositions = new long[3][];
		tilePositions[0] = new long[]{Long.MAX_VALUE};
		tilePositions[1] = new long[] {1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2131365251,2142635021,2151129028l,2330078621l};
		tilePositions[2] = new long[]{1861258204,1880846658,1976293002,2017227347,2173524984l,2186432022l};
//		tilePositions[1] = new long[] {54441301l,81004583l,91993417l,119383169l,158841860l,171177226l,187449800l,195842600l,238696762l,246645164l,249907876l,263512805l,290664374,413525506,442042580,516502511,532313353,543900614,545914178,557033451,570539881,619567319,660817481,692183056,720334805,737404231,763939540,780329395,825243023,840024227,844739893,894152354,904823369,906179614,910993866,916934338,938768668,949235902,970632708,983875719,1001889192,1027416205,1043834398,1079585857,1135078366,1212848363,1241051369,1241743198,1250009141,1261914836,1266039985,1353060209,1385926859,1398339451,1422701441,1493785729,1499634944,1632342192,1634265466,1660879831,1683113904,1688546475,1696070843,1724658502,1730534307,1736825538,1744955547,1763612917,1846691662,1882841257,1884705941,1900518458,1938961645,1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2010369283,2028165352,2055169346,2057690489,2059613402,2113415446,2125704662,2131365251,2142635021,2151129028l,2158614647l,2175299682l,2245537376l,2272456933l,2330078621l,2332649455l,2351038537l,2364825490l,2387773275l,2404946193l,2432058312l,2463593115l,2539267700l,2539268013l,2543194015l,2545546255l,2547462156l,2569183788l,2587608326l,2621061713l,2646899912l,2654728923l,2716688667l,2758309228l,2763598036l,2766568503l,2773002306l,2898978353l,2901924904l,2918511117l,2935092487l,2944787668l,2952404849l,2953877262l,2976620086l,2992612126l,3016130929l,3031260578l};
//		tilePositions[2] = new long[]{68236928,181877274,289174219,379871042,407464590,433734841,531132793,617626140,635267381,715900883,760044870,800473049,848398617,917477066,1009939623,1125070610,1169151520,1175631072,1185537588,1360190696,1414582806,1427478754,1427478845,1427478936,1427479027,1519792815,1611882893,1643538072,1793223481,1838300648,1861258204,1880846658,1976293002,2017227347,2173524984l,2186432022l,2282138469l,2337043497l,2463890593l,2486972151l,2531042628l,2549985692l,2611145621l,2627424189l,2636419256l,2849128607l,2897877268l,2898412131l,2910339381l,2972513655l,3020093124l};
		TIntObjectHashMap<TLongArrayList> results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0, 0, 2);
		assertEquals(1, results.size());
		assertEquals(1, results.get(2).size());
		assertEquals(1976292989, results.get(2).get(0));
		
	}
	@Test
	public void bestPositionsRealLifeAgain() {
		/*
		 * QIMR13579:data oliverh$ grep -w TTGTTCGTGCACA q3tiledaligner.out.gz.condensed
TTGTTCGTGCACA	408127821,426351096,533725718,677645341,2113730620,2273656032,2959256072
TCAGGATACTCAG	80222241,152579258,237960695,333224362,483918644,487262066,572915476,587263901,859268689,874692502,884150076,962305441,1014577454,1055468406,1104642437,1226814192,1373833484,1387771206,1422039318,1635453719,1639459814,1740484384,1744788857,1779246940,1955729943,1991350272,2111592335,2330179873,2350048124,2496268860,2531895101,2579136534,2593088219,2602326753,2646245428,2702391395,2727220912,2778067804,2798603941,2967444679,2992700504,2996126587
CGCCGTACGTCCA	533725744,2665235085
TGGGTGGGACACA	7076123,14016715,18110880,37356146,64396232,65184027,83264460,85221226,86949834,93747200,114602931,118053916,153412567,153433240,155295519,170372292,180155544,194424290,195386606,250798516,301465931,315095107,346699810,348036776,360686037,360871851,365765816,376842039,395885256,397207618,400971632,408067853,421076040,422720627,435001236,451623372,467505622,482655131,484733285,498248752,510631617,524626833,533725757,546737628,547069852,547945509,575756886,600270812,604005749,604740700,610858070,618380238,619156639,619524932,631152213,649730517,654228619,662823927,666820475,670302744,672697335,682603270,690782279,723125352,725293503,743722675,747968261,766545689,780840362,780952105,786693417,789700083,791444927,791614504,825273312,842724823,847218279,849474935,855332550,875769601,877890971,886283937,887071777,903697303,907867714,921950357,959759613,967301503,980203470,991803189,991939888,994099738,1006368728,1011944883,1024551410,1035956569,1037807580,1064948215,1092021906,1112050015,1125366065,1144449594,1150047448,1154036491,1157942030,1173325381,1180093447,1183146642,1200496005,1202657701,1207513383,1227480239,1231295222,1235061435,1250548822,1261410538,1272385474,1304356111,1308482659,1317565214,1318281721,1342095068,1344026606,1348728192,1353192526,1361216380,1363752085,1364688829,1365636493,1383415207,1390911759,1390911915,1393667688,1413629208,1432077850,1464044709,1486811423,1490600996,1491826764,1502149426,1516073271,1517829166,1526797173,1535810679,1536461769,1556057740,1565178509,1618106755,1618106817,1624654535,1625238255,1631845196,1637154315,1649694445,1656188829,1661986827,1664362875,1673308526,1677053931,1700003463,1710724935,1747892674,1775747756,1781482207,1781495074,1789330858,1790552846,1798120171,1802822958,1804025131,1838235170,1855192239,1872758956,1885845740,1921128860,1928112314,1929236551,1974106668,1982693890,1991889390,1994548427,2018655839,2024555503,2038768285,2043112348,2056977388,2057877960,2070710402,2075301644,2083984715,2107713903,2114780119,2153627340,2159743127,2167431738,2244242297,2248813942,2255441174,2267236344,2268409045,2275888574,2283900847,2285411814,2291162812,2344100320,2345322770,2354147146,2389827732,2393716644,2394703324,2402477151,2411491082,2419465929,2456849082,2462224392,2467211656,2471021875,2503154412,2507405415,2519906524,2534637668,2537573347,2568078986,2575021051,2578102776,2590361667,2594678278,2595414656,2625098786,2634479948,2645049986,2647041359,2657740835,2657741199,2657741227,2657741255,2657741283,2657741311,2657741339,2657741367,2657741535,2657741563,2657741647,2660478456,2660479088,2688931553,2698423931,2720345985,2753647385,2764048093,2781241281,2808646959,2810730643,2813734119,2825058418,2852194432,2873327278,2921860953,2930954755,2943325036,2959640022,2971310664,2979280723,3000576337,3028733364,3040769036,3060570772
GCAGCAATTTGTG	4202125,58935285,61538896,65060164,96180871,115546485,217080826,333289756,413314072,437829685,533725770,563230177,806621889,852780137,1075831800,1115963955,1153407487,1314780862,1330721620,1494005611,1502455270,1541003610,1542843044,1654325043,1664179856,1675222588,1741143092,1766525927,1780219918,1885833611,1960809044,1993414162,1997452658,2026748231,2138742546,2139115670,2184133636,2228794559,2235507986,2249095624,2270596623,2290508244,2348974952,2376367792,2434079635,2628812116,2676500777,2716299086,2763438186,2781488811,2799994652,2854662819
GTAGGTAAATTCT	57118454,99701152,198819364,258130360,390150836,405155253,412464175,424744808,435987809,533725783,579746809,654423854,666022904,671952829,675704153,693675294,716350809,734340618,796543746,851498765,853504597,860315802,866068321,911211682,941074431,941570760,977929321,1006043098,1025986662,1086486080,1111885129,1144693402,1145838624,1338155445,1370303450,1473071283,1503528707,1505772227,1513157584,1613885880,1668263305,1828903944,1852666958,1910092443,1963590291,1993731081,2018156681,2079748758,2114960561,2141326106,2182227258,2191640943,2298586375,2489902752,2514063804,2607376510,2633842063,2651107932,2690784723,2726504312,2804623471,2811760728,2853009309,2909119614,2918531866,3008794333
TACAGTGATACCT	31873262,192366178,431348391,478304860,494168100,513982294,533725796,608056860,660158329,703915569,805308650,816558658,841588397,857971704,867073798,1075795791,1113096584,1149138635,1366616327,1452842993,1542368501,1546675062,1567101993,1714440903,1726392286,1740574074,1774894310,1790194305,1794356429,1844785681,1914281198,1933869197,1940016891,2026995512,2060263855,2149969493,2178529188,2234977566,2279207434,2391798654,2431251665,2431683943,2436968969,2438189385,2439230365,2439332170,2440072139,2466920590,2479845351,2491207260,2555863671,2593004663,2641795122,2642007918,2925914695,2930181796,3053095265
GGCTATCTAAAAG	83160273,256578346,317404685,451811376,498479255,533725809,556920555,649480029,761488348,802143308,805144283,807040669,938015882,1038894172,1086864163,1333093953,1406261543,1509331623,1532448001,1550676872,1681344589,1688213171,1741791840,1831473316,1872410988,1933937842,2171371271,2231512750,2284732439,2654972704,2668678490,2713141147,3007786933
GAATGCATAAATC	16143285,106824542,195413409,211733672,268430107,308395574,312419317,329413421,414227344,438897031,470861193,479492537,493506071,511651636,533725822,588030560,597341140,644253085,662490876,712022782,748379706,814172988,834208671,835027298,844771688,893520275,927873534,950789437,951664585,985621867,1164860172,1169285874,1182972125,1198550159,1225592212,1295306891,1295885450,1295885620,1316719237,1318126205,1353503624,1376303774,1386404649,1418668547,1500224650,1557976368,1566546239,1606163254,1623011718,1658581350,1668361723,1687142992,1711221949,1712325916,1730528557,1735175850,1774712517,1792164764,1836057107,1866584731,1948848428,1956436237,1984411607,1985516329,1988948313,2017791890,2035545552,2195029633,2227104773,2236158677,2280255780,2334033560,2346456237,2352358788,2389456707,2464124874,2471992247,2612538625,2655154346,2687341838,2769201254,2792313319,2867478789,2887640335,2907181275,2938428231,2965646853,2966772764
CAAAGGATCCTGA	31457202,73732421,99022123,167498762,181685600,261522255,296554953,314523986,357330208,369018319,379798713,379799812,394372736,416919864,423895393,505841439,533725835,557997155,635384642,655724337,706386711,731309059,798406788,880500006,897814639,941597070,976715569,978366234,1004935187,1050006867,1062636822,1199232029,1344219454,1376367592,1392900721,1428498743,1432500646,1447214221,1451548646,1462607417,1528270724,1622231378,1655644005,1658640116,1685873834,1733073416,1741548567,1777126274,1790270237,1801366291,1815983484,1834793330,1834885838,2147869961,2427072353,2553698322,2568019232,2587200657,2594436902,2630759351,2654316155,2659589546,2662499215,2748880577,2754721598,2765999388,2767171260,2850517405,2862579108,2897249148,2916811972,2926093762,3100736858
ACTTCTTTCTTTG	2563877,14444954,25630260,37206052,38234489,45049372,48684853,59327760,66357075,66689943,70449493,72232704,76510007,88564693,91708348,101641438,103610379,104778019,106953355,115845216,116152737,119177029,145730348,147195704,154397668,169903081,174078810,183884546,186892204,198149085,200633618,200913576,208618670,215425144,232986348,233218539,237181917,239475322,246909782,247332377,262858538,266138064,272442160,280472383,281699448,284121788,288450377,292870292,302060742,315452671,351053597,352421501,354902613,358714847,365752807,382107150,389740539,458899453,460718298,463657814,466189405,466887875,513353505,522063719,528863571,533725848,536463904,566141537,572162958,572857314,576684587,579105717,603126085,616980771,648695478,649755728,657655785,664355420,671846731,685080647,690585069,706540279,715095928,745240998,747515608,754623925,755814041,764162028,772163665,808929220,814690752,820077807,825535506,832172188,834626177,838865270,863227583,865957532,870556449,872932414,895022655,897577864,904831940,907322017,909236596,919405259,919412898,936304315,943904817,950010734,966350875,968831767,969732382,979182567,979714226,985524995,988029725,989622706,1015003640,1036839218,1064860238,1078875509,1081135170,1087987357,1091077081,1092646033,1097484830,1098610566,1112020346,1130214826,1150504308,1156912040,1159323061,1161394826,1176945343,1180986657,1185394012,1185605015,1185612306,1204826333,1241528553,1241607539,1255069992,1255490482,1258309943,1263420816,1273967777,1282050517,1298233123,1304601936,1323293224,1334875286,1355926765,1368415688,1379062813,1390506213,1393591548,1394782055,1395455389,1402010902,1417720895,1434776742,1435869028,1447004864,1449484944,1450167162,1461425050,1468216575,1468750206,1492490291,1515935112,1525001764,1534656556,1537565497,1544005564,1566969142,1609002168,1613410493,1624225069,1664373545,1691511389,1692915458,1699362572,1709120190,1735003360,1740030790,1743212954,1745339526,1756443816,1761629898,1770080402,1804419449,1805307269,1831066052,1845867062,1852464378,1858291623,1859891114,1871097874,1903298221,1923707605,1937860508,1943716902,1947535014,1954446354,1955083105,2001224195,2031136281,2033287706,2036776624,2051940435,2051956751,2070927869,2075042566,2077784354,2110729155,2121557770,2134398870,2137459815,2151222872,2151317409,2165544508,2182121559,2185631110,2189320931,2230671901,2232112864,2251409179,2260610029,2271078083,2278080022,2280070166,2285477167,2300257876,2303880996,2331359662,2348081233,2363089014,2364928377,2369873500,2370364364,2400433790,2411483784,2416018898,2421757661,2424574130,2441113742,2462117978,2480863472,2482140833,2492943330,2503163985,2527335700,2533903075,2534137318,2595044725,2596633907,2634963559,2642299807,2646189910,2653906604,2662320358,2695028031,2696100541,2722129571,2722613711,2735005424,2756392617,2760505921,2766019319,2776823965,2800754060,2810642735,2816556800,2818116789,2822451273,2826007984,2864211579,2870480419,2885236070,2891057201,2896461026,2910173884,2914403953,2926528977,2931724664,2936456007,2967104405,2972619695,2976961968,2996073780,3041617443,3053418209,3056200295,3096040042
GTCATTGGTTCCC	143478235,170795491,205848716,206768656,264728709,293308494,428813967,493106446,533725861,602898424,823761679,991196599,1059574006,1205410016,1427561192,1513343471,1660987677,1669779620,1685043971,1688671889,1780423400,1786818712,1835207286,1907885273,2017707217,2240379611,2423825396,2791577985,2909082403,3002156833,3049685112,3096646790,3099211401,3099393339
CCCATCCGTCTTC	118734030,309918991,533725874,665383698,913325681,1168831010,1213579583,1362353854,1666401464,1922183849,2005665330,2064741789,2080467817,2385467099,2477512244,2497916019,2742800514,2744543175,2818943601,2862012387
CTGAAGAGCTAAT	63392630,99501195,99709566,106665165,113194375,146562301,220748483,229876153,264379384,296484760,304631509,323325708,386722647,394251337,425985394,431711780,507863697,533725887,533935019,605198970,637688260,663714574,666808160,706071455,721504701,723895408,757514359,765606957,793264264,797739304,846081039,855549006,859559758,898366852,912119604,945265969,971432134,1020381440,1065244162,1084967902,1140974736,1195900229,1224687205,1302334616,1312741290,1344217543,1366194024,1422487152,1621262129,1659570844,1736462089,1790736089,1840554687,1855420921,1912868727,2023377480,2048626615,2053087710,2110244827,2127358413,2148392058,2158948496,2226766929,2258187031,2268386565,2304325668,2355690347,2384777777,2432884958,2440421029,2472040567,2494721866,2586444290,2594613265,2620539602,2667499053,2701639498,2733051411,2912255265,2914428351,2981593984,3000737156,3051612956,3097528522
GACAAAGTGAATA	12427520,34610772,115221244,149586969,182536366,218870840,243716833,289675749,384695756,389920124,432476008,462656628,518754715,591845427,598036201,600862716,650369686,683141785,766736055,818563759,842098859,874050557,1003487912,1010054835,1052207845,1102575790,1130730827,1138303939,1144553770,1151471087,1159221035,1258903206,1296065741,1341221616,1353128840,1506910419,1522618546,1623699315,1623714387,1623758646,1658215132,1795227497,1863511332,1876007517,1914722395,1917531547,1944478637,1968072122,1989288185,2016014929,2034317283,2141065647,2156282010,2157831994,2168950170,2255006137,2256601793,2288889913,2294569078,2332802414,2367955575,2471465324,2483036723,2528705291,2559180447,2600546453,2672847434,2677013239,2741409734,2797914523,2906493276,2958409164,2968181823,2970312342,3022853962,3029300709
AATAAATAATTAC	23237666,36318292,41695477,44913017,46295068,48793184,56371188,67550751,71884566,80020061,107522368,118405734,145047348,145055457,147162036,155488764,155512349,185839897,187928974,189215659,194178021,195286685,207402812,210987625,219047306,219335728,227298770,231103545,236244600,239713815,241970747,256645521,259123422,261169453,273153192,290453008,297370703,300495562,316763678,328195378,328535235,329523272,332641986,364824944,375331329,375426734,379620172,390608294,393492751,394125730,397520392,406385121,407000895,408638344,421516536,425230778,429604929,434841935,438054550,440528323,464324230,479260088,500744348,509095533,529203460,531345092,533725913,550119008,557371635,567739283,571668516,579090911,581035729,587181519,588622806,588998195,594370194,595069736,596776095,596790679,603018717,606814143
ACATTTCTATGGC	88793688,230131955,278981127,289687136,330071289,335353241,348686112,385923140,395468660,402767734,412264999,428984917,446221520,533725926,549447985,627440240,651588597,675846013,682680574,729841274,743297734,744227186,744534398
		 */
	
		
		long[][] tilePositions = new long[17][];
		tilePositions[0] = new long[]{408127821,426351096,533725718,677645341,2113730620,2273656032l,2959256072l};
		tilePositions[1] = new long[]{80222241,152579258,237960695,333224362,483918644,487262066,572915476,587263901,859268689,874692502,884150076,962305441,1014577454,1055468406,1104642437,1226814192,1373833484,1387771206,1422039318,1635453719,1639459814,1740484384,1744788857,1779246940,1955729943,1991350272,2111592335,2330179873l,2350048124l,2496268860l,2531895101l,2579136534l,2593088219l,2602326753l,2646245428l,2702391395l,2727220912l,2778067804l,2798603941l,2967444679l,2992700504l,2996126587l};
		tilePositions[2] = new long[]{533725744,2665235085l};
		tilePositions[3] = new long[]{7076123,14016715,18110880,37356146,64396232,65184027,83264460,85221226,86949834,93747200,114602931,118053916,153412567,153433240,155295519,170372292,180155544,194424290,195386606,250798516,301465931,315095107,346699810,348036776,360686037,360871851,365765816,376842039,395885256,397207618,400971632,408067853,421076040,422720627,435001236,451623372,467505622,482655131,484733285,498248752,510631617,524626833,533725757,546737628,547069852,547945509,575756886,600270812,604005749,604740700,610858070,618380238,619156639,619524932,631152213,649730517,654228619,662823927,666820475,670302744,672697335,682603270,690782279,723125352,725293503,743722675,747968261,766545689,780840362,780952105,786693417,789700083,791444927,791614504,825273312,842724823,847218279,849474935,855332550,875769601,877890971,886283937,887071777,903697303,907867714,921950357,959759613,967301503,980203470,991803189,991939888,994099738,1006368728,1011944883,1024551410,1035956569,1037807580,1064948215,1092021906,1112050015,1125366065,1144449594,1150047448,1154036491,1157942030,1173325381,1180093447,1183146642,1200496005,1202657701,1207513383,1227480239,1231295222,1235061435,1250548822,1261410538,1272385474,1304356111,1308482659,1317565214,1318281721,1342095068,1344026606,1348728192,1353192526,1361216380,1363752085,1364688829,1365636493,1383415207,1390911759,1390911915,1393667688,1413629208,1432077850,1464044709,1486811423,1490600996,1491826764,1502149426,1516073271,1517829166,1526797173,1535810679,1536461769,1556057740,1565178509,1618106755,1618106817,1624654535,1625238255,1631845196,1637154315,1649694445,1656188829,1661986827,1664362875,1673308526,1677053931,1700003463,1710724935,1747892674,1775747756,1781482207,1781495074,1789330858,1790552846,1798120171,1802822958,1804025131,1838235170,1855192239,1872758956,1885845740,1921128860,1928112314,1929236551,1974106668,1982693890,1991889390,1994548427,2018655839,2024555503,2038768285,2043112348,2056977388,2057877960,2070710402,2075301644,2083984715,2107713903,2114780119,2153627340l,2159743127l,2167431738l,2244242297l,2248813942l,2255441174l,2267236344l,2268409045l,2275888574l,2283900847l,2285411814l,2291162812l,2344100320l,2345322770l,2354147146l,2389827732l,2393716644l,2394703324l,2402477151l,2411491082l,2419465929l,2456849082l,2462224392l,2467211656l,2471021875l,2503154412l,2507405415l,2519906524l,2534637668l,2537573347l,2568078986l,2575021051l,2578102776l,2590361667l,2594678278l,2595414656l,2625098786l,2634479948l,2645049986l,2647041359l,2657740835l,2657741199l,2657741227l,2657741255l,2657741283l,2657741311l,2657741339l,2657741367l,2657741535l,2657741563l,2657741647l,2660478456l,2660479088l,2688931553l,2698423931l,2720345985l,2753647385l,2764048093l,2781241281l,2808646959l,2810730643l,2813734119l,2825058418l,2852194432l,2873327278l,2921860953l,2930954755l,2943325036l,2959640022l,2971310664l,2979280723l,3000576337l,3028733364l,3040769036l,3060570772l};
		tilePositions[4] = new long[]{4202125,58935285,61538896,65060164,96180871,115546485,217080826,333289756,413314072,437829685,533725770,563230177,806621889,852780137,1075831800,1115963955,1153407487,1314780862,1330721620,1494005611,1502455270,1541003610,1542843044,1654325043,1664179856,1675222588,1741143092,1766525927,1780219918,1885833611,1960809044,1993414162,1997452658,2026748231,2138742546,2139115670,2184133636l,2228794559l,2235507986l,2249095624l,2270596623l,2290508244l,2348974952l,2376367792l,2434079635l,2628812116l,2676500777l,2716299086l,2763438186l,2781488811l,2799994652l,2854662819l};
		tilePositions[5] = new long[]{57118454,99701152,198819364,258130360,390150836,405155253,412464175,424744808,435987809,533725783,579746809,654423854,666022904,671952829,675704153,693675294,716350809,734340618,796543746,851498765,853504597,860315802,866068321,911211682,941074431,941570760,977929321,1006043098,1025986662,1086486080,1111885129,1144693402,1145838624,1338155445,1370303450,1473071283,1503528707,1505772227,1513157584,1613885880,1668263305,1828903944,1852666958,1910092443,1963590291,1993731081,2018156681,2079748758,2114960561,2141326106,2182227258l,2191640943l,2298586375l,2489902752l,2514063804l,2607376510l,2633842063l,2651107932l,2690784723l,2726504312l,2804623471l,2811760728l,2853009309l,2909119614l,2918531866l,3008794333l};
		tilePositions[6] = new long[]{31873262,192366178,431348391,478304860,494168100,513982294,533725796,608056860,660158329,703915569,805308650,816558658,841588397,857971704,867073798,1075795791,1113096584,1149138635,1366616327,1452842993,1542368501,1546675062,1567101993,1714440903,1726392286,1740574074,1774894310,1790194305,1794356429,1844785681,1914281198,1933869197,1940016891,2026995512,2060263855,2149969493l,2178529188l,2234977566l,2279207434l,2391798654l,2431251665l,2431683943l,2436968969l,2438189385l,2439230365l,2439332170l,2440072139l,2466920590l,2479845351l,2491207260l,2555863671l,2593004663l,2641795122l,2642007918l,2925914695l,2930181796l,3053095265l};
		tilePositions[7] = new long[]{83160273,256578346,317404685,451811376,498479255,533725809,556920555,649480029,761488348,802143308,805144283,807040669,938015882,1038894172,1086864163,1333093953,1406261543,1509331623,1532448001,1550676872,1681344589,1688213171,1741791840,1831473316,1872410988,1933937842,2171371271l,2231512750l,2284732439l,2654972704l,2668678490l,2713141147l,3007786933l};
		tilePositions[8] = new long[]{16143285,106824542,195413409,211733672,268430107,308395574,312419317,329413421,414227344,438897031,470861193,479492537,493506071,511651636,533725822,588030560,597341140,644253085,662490876,712022782,748379706,814172988,834208671,835027298,844771688,893520275,927873534,950789437,951664585,985621867,1164860172,1169285874,1182972125,1198550159,1225592212,1295306891,1295885450,1295885620,1316719237,1318126205,1353503624,1376303774,1386404649,1418668547,1500224650,1557976368,1566546239,1606163254,1623011718,1658581350,1668361723,1687142992,1711221949,1712325916,1730528557,1735175850,1774712517,1792164764,1836057107,1866584731,1948848428,1956436237,1984411607,1985516329,1988948313,2017791890,2035545552,2195029633l,2227104773l,2236158677l,2280255780l,2334033560l,2346456237l,2352358788l,2389456707l,2464124874l,2471992247l,2612538625l,2655154346l,2687341838l,2769201254l,2792313319l,2867478789l,2887640335l,2907181275l,2938428231l,2965646853l,2966772764l};
		tilePositions[9] = new long[]{31457202,73732421,99022123,167498762,181685600,261522255,296554953,314523986,357330208,369018319,379798713,379799812,394372736,416919864,423895393,505841439,533725835,557997155,635384642,655724337,706386711,731309059,798406788,880500006,897814639,941597070,976715569,978366234,1004935187,1050006867,1062636822,1199232029,1344219454,1376367592,1392900721,1428498743,1432500646,1447214221,1451548646,1462607417,1528270724,1622231378,1655644005,1658640116,1685873834,1733073416,1741548567,1777126274,1790270237,1801366291,1815983484,1834793330,1834885838,2147869961l,2427072353l,2553698322l,2568019232l,2587200657l,2594436902l,2630759351l,2654316155l,2659589546l,2662499215l,2748880577l,2754721598l,2765999388l,2767171260l,2850517405l,2862579108l,2897249148l,2916811972l,2926093762l,3100736858l};
		tilePositions[10] = new long[]{2563877,14444954,25630260,37206052,38234489,45049372,48684853,59327760,66357075,66689943,70449493,72232704,76510007,88564693,91708348,101641438,103610379,104778019,106953355,115845216,116152737,119177029,145730348,147195704,154397668,169903081,174078810,183884546,186892204,198149085,200633618,200913576,208618670,215425144,232986348,233218539,237181917,239475322,246909782,247332377,262858538,266138064,272442160,280472383,281699448,284121788,288450377,292870292,302060742,315452671,351053597,352421501,354902613,358714847,365752807,382107150,389740539,458899453,460718298,463657814,466189405,466887875,513353505,522063719,528863571,533725848,536463904,566141537,572162958,572857314,576684587,579105717,603126085,616980771,648695478,649755728,657655785,664355420,671846731,685080647,690585069,706540279,715095928,745240998,747515608,754623925,755814041,764162028,772163665,808929220,814690752,820077807,825535506,832172188,834626177,838865270,863227583,865957532,870556449,872932414,895022655,897577864,904831940,907322017,909236596,919405259,919412898,936304315,943904817,950010734,966350875,968831767,969732382,979182567,979714226,985524995,988029725,989622706,1015003640,1036839218,1064860238,1078875509,1081135170,1087987357,1091077081,1092646033,1097484830,1098610566,1112020346,1130214826,1150504308,1156912040,1159323061,1161394826,1176945343,1180986657,1185394012,1185605015,1185612306,1204826333,1241528553,1241607539,1255069992,1255490482,1258309943,1263420816,1273967777,1282050517,1298233123,1304601936,1323293224,1334875286,1355926765,1368415688,1379062813,1390506213,1393591548,1394782055,1395455389,1402010902,1417720895,1434776742,1435869028,1447004864,1449484944,1450167162,1461425050,1468216575,1468750206,1492490291,1515935112,1525001764,1534656556,1537565497,1544005564,1566969142,1609002168,1613410493,1624225069,1664373545,1691511389,1692915458,1699362572,1709120190,1735003360,1740030790,1743212954,1745339526,1756443816,1761629898,1770080402,1804419449,1805307269,1831066052,1845867062,1852464378,1858291623,1859891114,1871097874,1903298221,1923707605,1937860508,1943716902,1947535014,1954446354,1955083105,2001224195,2031136281,2033287706,2036776624,2051940435,2051956751,2070927869,2075042566,2077784354,2110729155,2121557770,2134398870,2137459815,2151222872l,2151317409l,2165544508l,2182121559l,2185631110l,2189320931l,2230671901l,2232112864l,2251409179l,2260610029l,2271078083l,2278080022l,2280070166l,2285477167l,2300257876l,2303880996l,2331359662l,2348081233l,2363089014l,2364928377l,2369873500l,2370364364l,2400433790l,2411483784l,2416018898l,2421757661l,2424574130l,2441113742l,2462117978l,2480863472l,2482140833l,2492943330l,2503163985l,2527335700l,2533903075l,2534137318l,2595044725l,2596633907l,2634963559l,2642299807l,2646189910l,2653906604l,2662320358l,2695028031l,2696100541l,2722129571l,2722613711l,2735005424l,2756392617l,2760505921l,2766019319l,2776823965l,2800754060l,2810642735l,2816556800l,2818116789l,2822451273l,2826007984l,2864211579l,2870480419l,2885236070l,2891057201l,2896461026l,2910173884l,2914403953l,2926528977l,2931724664l,2936456007l,2967104405l,2972619695l,2976961968l,2996073780l,3041617443l,3053418209l,3056200295l,3096040042l};
		tilePositions[11] = new long[]{143478235,170795491,205848716,206768656,264728709,293308494,428813967,493106446,533725861,602898424,823761679,991196599,1059574006,1205410016,1427561192,1513343471,1660987677,1669779620,1685043971,1688671889,1780423400,1786818712,1835207286,1907885273,2017707217,2240379611l,2423825396l,2791577985l,2909082403l,3002156833l,3049685112l,3096646790l,3099211401l,3099393339l};
		tilePositions[12] = new long[]{118734030,309918991,533725874,665383698,913325681,1168831010,1213579583,1362353854,1666401464,1922183849,2005665330,2064741789,2080467817,2385467099l,2477512244l,2497916019l,2742800514l,2744543175l,2818943601l,2862012387l};
		tilePositions[13] = new long[]{63392630,99501195,99709566,106665165,113194375,146562301,220748483,229876153,264379384,296484760,304631509,323325708,386722647,394251337,425985394,431711780,507863697,533725887,533935019,605198970,637688260,663714574,666808160,706071455,721504701,723895408,757514359,765606957,793264264,797739304,846081039,855549006,859559758,898366852,912119604,945265969,971432134,1020381440,1065244162,1084967902,1140974736,1195900229,1224687205,1302334616,1312741290,1344217543,1366194024,1422487152,1621262129,1659570844,1736462089,1790736089,1840554687,1855420921,1912868727,2023377480,2048626615,2053087710,2110244827,2127358413,2148392058l,2158948496l,2226766929l,2258187031l,2268386565l,2304325668l,2355690347l,2384777777l,2432884958l,2440421029l,2472040567l,2494721866l,2586444290l,2594613265l,2620539602l,2667499053l,2701639498l,2733051411l,2912255265l,2914428351l,2981593984l,3000737156l,3051612956l,3097528522l};
		tilePositions[14] = new long[]{12427520,34610772,115221244,149586969,182536366,218870840,243716833,289675749,384695756,389920124,432476008,462656628,518754715,591845427,598036201,600862716,650369686,683141785,766736055,818563759,842098859,874050557,1003487912,1010054835,1052207845,1102575790,1130730827,1138303939,1144553770,1151471087,1159221035,1258903206,1296065741,1341221616,1353128840,1506910419,1522618546,1623699315,1623714387,1623758646,1658215132,1795227497,1863511332,1876007517,1914722395,1917531547,1944478637,1968072122,1989288185,2016014929,2034317283,2141065647,2156282010l,2157831994l,2168950170l,2255006137l,2256601793l,2288889913l,2294569078l,2332802414l,2367955575l,2471465324l,2483036723l,2528705291l,2559180447l,2600546453l,2672847434l,2677013239l,2741409734l,2797914523l,2906493276l,2958409164l,2968181823l,2970312342l,3022853962l,3029300709l};
		tilePositions[15] = new long[]{23237666,36318292,41695477,44913017,46295068,48793184,56371188,67550751,71884566,80020061,107522368,118405734,145047348,145055457,147162036,155488764,155512349,185839897,187928974,189215659,194178021,195286685,207402812,210987625,219047306,219335728,227298770,231103545,236244600,239713815,241970747,256645521,259123422,261169453,273153192,290453008,297370703,300495562,316763678,328195378,328535235,329523272,332641986,364824944,375331329,375426734,379620172,390608294,393492751,394125730,397520392,406385121,407000895,408638344,421516536,425230778,429604929,434841935,438054550,440528323,464324230,479260088,500744348,509095533,529203460,531345092,533725913,550119008,557371635,567739283,571668516,579090911,581035729,587181519,588622806,588998195,594370194,595069736,596776095,596790679,603018717,606814143};
		tilePositions[16] = new long[]{88793688,230131955,278981127,289687136,330071289,335353241,348686112,385923140,395468660,402767734,412264999,428984917,446221520,533725926,549447985,627440240,651588597,675846013,682680574,729841274,743297734,744227186,744534398l};
		
		TIntObjectHashMap<TLongArrayList> results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0, 1, 2);
		assertEquals(1, results.size());
		
	}
	
	
	@Test
	public void addMDAndNMTags() {
		SAMRecord rec = new SAMRecord(null);
		rec.setAlignmentStart(1);
		rec.setReferenceName("chr1");
		CigarElement ce = new CigarElement(225, CigarOperator.MATCH_OR_MISMATCH);
		List<CigarElement> ces = new ArrayList<>();
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("TGGGGGTCTGAGTGATGGGGTCCAGGAATACATTTAGGTCCAATGGCAAGCTGGCTGAAATTCTTGTATAATAAAATAGGTTGGTAATATGGCTCTTCTCAGACATGTGATCAAGATTCCTTGACTAACAAGATATATATATATATCTTTCTAGCTCATCATACTGGCTAGTGGTGGACCCCAAGCTTTAGTAAATATAATGAGGACCTATACTTACGAAAAACT");
		rec.setReadName("1_1_1");
		ClinVarUtil.calculateMdAndNmTags(rec, "TGGGGGTCTGAGTGATGGGGTCCAGGAATACATTTAGGTCCAATGGCAAGCTGGCTGAAATTCTTGTATAATAAAATAGGTTGGTAATATGGCTCTTCTCAGACATGTGATCAAGATTCCTTGACTAACAAGATATATATATATATCTTTCTAGCTCATCATACTGGCTAGTGGTGGACCCCAAGCTTTAGTAAATATAATGAGGACCTATACTTACGAAAAACT".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("225", rec.getAttribute("MD"));
		
		ce = new CigarElement(230, CigarOperator.MATCH_OR_MISMATCH);
		ces = new ArrayList<>();
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("CCAAGCACCTCAGGGGAACAGGCTCCTCCCGCCGCGGGAGTCCGACCGTCCTCGACCTGCGGTGGCGGCTCGGCGGGGACTGAAGCTGCTCCTCAGACCTTCCTCCGTCTCCGCCTCCCCTCGCTCTCCGCTCCCGGGGCCGGGCCAACGCTGCTGCCACAGACCGAGAGGCTTAAAATGGCGCCGCACAAGGAGCTCTTATAAGTCGCGCAGAAGCCGCTGTATCCTGC");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "CCAAGCACCTCAGGGGAACAGGCTCCTCCCGCCGCGGGAGTCCGACCGTCCTCGACCTGCGGTGGCGGCTCGGCGGGGACTGAAGCTGCTCCTCAGACCTTCCTCCGTCTCCGCCTCCCCTCGCTCTCCGCTCCCGGGGCCGGGCCAACGCTGCTGCCACAGACCGAGAGGCTTAAAATGGCGCCGCACAAGGAGCTCTTATAAGTCGCGCAGAAGCCGCTGTATCCTGC".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("230", rec.getAttribute("MD"));
		
		ces = new ArrayList<>();
		CigarElement indel = new CigarElement(1, CigarOperator.SOFT_CLIP);
		ce = new CigarElement(226, CigarOperator.MATCH_OR_MISMATCH);
		ces.add(indel);
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("GAGAATCATCTGGATTATAGACCAGTGGCACTGTTGTTTCACAAGATGATGTTTGAAACTATTCCAATGTTCAGTGGCGGAACTTGCAGTAAGTGCTTGAAATTCTCATCCTTCCATGTATTGGAACAGTTTTCTTAACCATATCTAGAAGTTTACATAAAAATTTAGAAAGAAATTTACCACATTTGAAATTTATGCAGGAGACTATATTTCTGAAGCATTTGAAC");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "AGAATCATCTGGATTATAGACCAGTGGCACTGTTGTTTCACAAGATGATGTTTGAAACTATTCCAATGTTCAGTGGCGGAACTTGCAGTAAGTGCTTGAAATTCTCATCCTTCCATGTATTGGAACAGTTTTCTTAACCATATCTAGAAGTTTACATAAAAATTTAGAAAGAAATTTACCACATTTGAAATTTATGCAGGAGACTATATTTCTGAAGCATTTGAAC".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("226", rec.getAttribute("MD"));
		
		/*
		 * 56M1I171M
		 */
		ces = new ArrayList<>();
		ce = new CigarElement(56, CigarOperator.MATCH_OR_MISMATCH);
		indel = new CigarElement(1, CigarOperator.INSERTION);
		CigarElement match = new CigarElement(170, CigarOperator.MATCH_OR_MISMATCH);
		ces.add(ce);
		ces.add(indel);
		ces.add(match);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("TTATCAAGAGGGATAAAACACCATGAAAATAAACTTGAATAAACTGAAAATGGACCTTTTTTTTTTTTAATGGCAATAGGACATTGTGTCAGATTACCAGTTATAGGAACAATTCTCTTTTCCTGACCAATCTTGTTTTACCCTATACATCCACAGGGTTTTGACACTTGTTGTCCAGTTGAAAAAAGGTTGTGTAGCTGTGTCATGTATATACCTTTTTGTGTCAA");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "TTATCAAGAGGGATAAAACACCATGAAAATAAACTTGAATAAACTGAAAATGGACCTTTTTTTTTTTAATGGCAATAGGACATTGTGTCAGATTACCAGTTATAGGAACAATTCTCTTTTCCTGACCAATCTTGTTTTACCCTATACATCCACAGGGTTTTGACACTTGTTGTCCAGTTGAAAAAAGGTTGTGTAGCTGTGTCATGTATATACCTTTTTGTGTCAA".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("226", rec.getAttribute("MD"));
	}
	
	@Test
	public void doesComparatorWork() {
		String origBB = "A,471,1/4;C,17,1/1";
		assertEquals("C,17,1/1;A,471,1/4", ClinVarUtil.getSortedBBString(origBB, "A"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "C"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "G"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "T"));
		
	}
	@Test
	public void doesComparatorWorkINdel() {
		//CT	C,CTT	.	.	END=41265954	BB	C,29,96/11579(29);CT,431,96/11790(396),96/11834(18),96/11722(17);CTT,28,96/11736(28)
		String origBB = "C,29,96/11579(29);CT,431,96/11790(396),96/11834(18),96/11722(17);CTT,28,96/11736(28)";
		assertEquals("C,29,96/11579(29);CTT,28,96/11736(28);CT,431,96/11790(396),96/11834(18),96/11722(17)", ClinVarUtil.getSortedBBString(origBB, "CT"));
		
	}
	
	@Test
	public void basicEditDistance() {
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance(null,null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance("",""));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance("hello","oh my goodness"));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		assertEquals(0, ClinVarUtil.getBasicEditDistance("hello", "hello"));
		assertEquals(1, ClinVarUtil.getBasicEditDistance("hello", "hallo"));
		assertEquals(4, ClinVarUtil.getBasicEditDistance("hello", " hell"));
		assertEquals(2, ClinVarUtil.getBasicEditDistance("crap", "carp"));
	}
	
	
	@Test
	public void getEditDistances() {
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances(null, null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("", ""));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "A"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AC", "AC"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACG", "ACG"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACGT", "ACGT"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "C"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("C", "A"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACC", "AAC"));
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AACCGGTT", "ACCGGTTT"));
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("frog", "fog "));
		assertArrayEquals(new int[]{13,12}, ClinVarUtil.getBasicAndLevenshteinEditDistances("GCCCCGTGCCCCAGCCCTGCGCCCCTTCCTC", "GCCCTGCGCCCCTTCCTCTCCCGTCGTCACC"));
	}
	
	@Test
	public void getEditDistancesRealLife() {
		//CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, r1OverlapRC: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, basicED: 51, led: 2
		String s = "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA";
		String t = "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG";
		assertArrayEquals(new int[]{51,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances(s, t));
		
		assertEquals(1, ClinVarUtil.noOfSlidesToGetPerfectMatch(s, t));
		
		t = t.substring(1);
		s = s.substring(0, s.length() -1);
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances(s, t));
	}
	
	@Test
	public void getMutationFromSWDataNoMut() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "||||";
		swData[2] = "ACGT";
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(0, mutations.size());
	}
	
	@Test
	public void getMutationFromSWDataMut() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "|.||";
		swData[2] = "ATGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = ".|||";
		swData[2] = "TCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/T", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||.|";
		swData[2] = "ACAT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|||.";
		swData[2] = "ACGC";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleMut() {
		String [] swData = new String[3];
		swData[0] = "ACGTACGT";
		swData[1] = "|..||..|";
		swData[2] = "ATTTAGTG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(4, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("C/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(6), p.getLeft());
		assertEquals("G/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleMutNew() {
		String [] swData = new String[3];
		swData[0] = "ACGTACGT";
		swData[1] = "|..||..|";
		swData[2] = "ATTTAGTG";
		
		List<String> mutations = ClinVarUtil.getPositionRefAndAltFromSWNew(swData);
		assertEquals(4, mutations.size());
		String p = mutations.get(0);
		assertEquals("1", p.substring(0, p.indexOf(Constants.COLON_STRING)));
		assertEquals("C/T", p.substring(p.indexOf(Constants.COLON_STRING) + 1));
		
		p = mutations.get(1);
		assertEquals("2", p.substring(0, p.indexOf(Constants.COLON_STRING)));
		assertEquals("G/T", p.substring(p.indexOf(Constants.COLON_STRING) + 1));
		
		p = mutations.get(2);
		assertEquals("5", p.substring(0, p.indexOf(Constants.COLON_STRING)));
		assertEquals("C/G",  p.substring(p.indexOf(Constants.COLON_STRING) + 1));
		
		p = mutations.get(3);
		assertEquals("6", p.substring(0, p.indexOf(Constants.COLON_STRING)));
		assertEquals("G/T", p.substring(p.indexOf(Constants.COLON_STRING) + 1));
	}
	
	@Test
	public void getMutationFromSWDataDel() {
		String [] swData = new String[3];
		swData[0] = "AACGT";
		swData[1] = "| |||";
		swData[2] = "A-CGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("AA/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "| ||";
		swData[2] = "A-GT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("AC/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|| |";
		swData[2] = "AC-T";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("CG/C", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||| ";
		swData[2] = "ACG-";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("GT/G", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseDel() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "  ||";
		swData[2] = "--GT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("AC/", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|  |";
		swData[2] = "A--T";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("ACG/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||  ";
		swData[2] = "AC--";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("CGT/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseIns() {
		String [] swData = new String[3];
		swData[0] = "A--GT";
		swData[1] = "|  ||";
		swData[2] = "AACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		swData[0] = "A--TAC--A";
		swData[1] = "|  |||  |";
		swData[2] = "ACGTACGTA";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/ACG", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("C/CGT", p.getRight());
		
		swData[0] = "AC--";
		swData[1] = "||  ";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CGT", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseInsAndDel() {
		String [] swData = new String[3];
		swData[0] = "A--GTACGT";
		swData[1] = "|  ||  ||";
		swData[2] = "AACGT--GT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("TAC/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseIndelAndSnp() {
		String [] swData = new String[3];
		swData[0] = "A--GTACGT";
		swData[1] = "|  ||||.|";
		swData[2] = "AACGTACAT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/A", p.getRight());
		
		swData[0] = "ACGTACGT";
		swData[1] = "||.| |||";
		swData[2] = "ACTT-CGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("TA/T", p.getRight());
		
		swData[0] = "AC-TACGT";
		swData[1] = "|| |.|||";
		swData[2] = "ACGTTCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CG", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("A/T", p.getRight());
		
		swData[0] = "ACGTACGT";
		swData[1] = "||||.|||";
		swData[2] = "ACGTTCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(4), p.getLeft());
		assertEquals("A/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataIns() {
		String [] swData = new String[3];
		swData[0] = "-CGT";
		swData[1] = " |||";
		swData[2] = "ACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("/A", p.getRight());
		
		swData[0] = "A-GT";
		swData[1] = "| ||";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AC", p.getRight());
		
		swData[0] = "AC-T";
		swData[1] = "|| |";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CG", p.getRight());
		
		swData[0] = "ACG-";
		swData[1] = "||| ";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/GT", p.getRight());
	}
	
	@Test
	public void areDeletionPositionsAccurate() {
		/*
		 * TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

		* expecting a single deletion at position 89
		 */
		String [] swData = new String[3];
		swData[0] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(88), p.getLeft());
		assertEquals("CT/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataSingleSmallInsertion() {
		String [] swData = new String[3];
		swData[0] = "AAGGTGAGTTCTGGAATGTAGAAGTAGGAGGCTGCTGGGGAGTCTGCGAGGAAACTTGATTTCTAGCAAAATCTTGTGTGATAATTTGCTGTGAATGAGAAATGAAGGAAGTGGTAAAATTCATTGAGTACTTGC-AAAAAAAAAATAGTATTAAGAAATCTAGATATCTTTATTATAAATTTCTTTTTCTATATGAAATCTGCTTTCCCCATGATCAAAAAAGAAAAATTAACTAATAAGAATAATGAAAAACTTACACAGATGTGA";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "AAGGTGAGTTCTGGAATGTAGAAGTAGGAGGCTGCTGGGGAGTCTGCGAGGAAACTTGATTTCTAGCAAAATCTTGTGTGATAATTTGCTGTGAATGAGAAATGAAGGAAGTGGTAAAATTCATTGAGTACTTGCAAAAAAAAAAATAGTATTAAGAAATCTAGATATCTTTATTATAAATTTCTTTTTCTATATGAAATCTGCTTTCCCCATGATCAAAAAAGAAAAATTAACTAATAAGAATAATGAAAAACTTACACAGATGTGA";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(134), p.getLeft());
		assertEquals("C/CA", p.getRight());
	}
	@Test
	public void getMutationFromSWDataSingleSmallDeletion() {
		/*
		 * CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAATTTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG
20:34:46.042 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
20:34:46.042 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAA-TTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG
2
		 */
		String [] swData = new String[3];
		swData[0] = "CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAATTTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAA-TTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(92), p.getLeft());
		assertEquals("AT/A", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataSnpAndDeletion() {
	/*
	 * GAAACCACAGAGAACAGTTCCCCTGAGTGCACAGTCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTTCAACAACAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT
21:14:57.649 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||      |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
21:14:57.649 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - GAAACCACAGAGAACAGTTCCCCTGAGTGCACAATCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTT------CAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT
*/
		String [] swData = new String[3];
		swData[0] = "GAAACCACAGAGAACAGTTCCCCTGAGTGCACAGTCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTTCAACAACAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT";
		swData[1] = "|||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||      |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "GAAACCACAGAGAACAGTTCCCCTGAGTGCACAATCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTT------CAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(33), p.getLeft());
		assertEquals("G/A", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(138), p.getLeft());
		assertEquals("TCAACAA/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleSnpAndDeletion() {
		/*
		 *  TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTATTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGCTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACAAACTGCATGCATCCATCTAAAAAGACCATATTTGTACATTTTTTTTAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC
21:36:50.089 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||| |||||..|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
21:36:50.089 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTACTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGTTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACGAACTGCATGCATCCATCTAAAAAGACCATATTTGTACA-TTTTTAAAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC
21:36:50.093 [main] INFO au.
		 */
		String [] swData = new String[3];
		swData[0] = "TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTATTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGCTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACAAACTGCATGCATCCATCTAAAAAGACCATATTTGTACATTTTTTTTAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC";
		swData[1] = "|||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||| |||||..|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTACTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGTTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACGAACTGCATGCATCCATCTAAAAAGACCATATTTGTACA-TTTTTAAAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(6, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(43), p.getLeft());
		assertEquals("T/C", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(84), p.getLeft());
		assertEquals("C/T", p.getRight());
		p = mutations.get(2);
		assertEquals(Integer.valueOf(145), p.getLeft());
		assertEquals("A/G", p.getRight());
		p = mutations.get(3);
		assertEquals(Integer.valueOf(183), p.getLeft());
		assertEquals("AT/A", p.getRight());
		p = mutations.get(4);
		assertEquals(Integer.valueOf(190), p.getLeft());
		assertEquals("T/A", p.getRight());
		p = mutations.get(5);
		assertEquals(Integer.valueOf(191), p.getLeft());
		assertEquals("T/A", p.getRight());
	}
	@Test
	public void getMutationFromSWDataMultipleDeletionsAndSnp() {
		/*
		 *   TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGATAGAGGAGTTTCTTAAAATACCACATATGGTGCTCTTTCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAGTATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTAATAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG
08:57:51.079 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||  ||||||||||||||||||||||||||||||    ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
08:57:51.079 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG

		 */
		String [] swData = new String[3];
		swData[0] = "TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGATAGAGGAGTTTCTTAAAATACCACATATGGTGCTCTTTCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAGTATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTAATAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||  ||||||||||||||||||||||||||||||    ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(4, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
//		assertEquals(Integer.valueOf(43), p.getLeft());
//		assertEquals("AG/--", p.getRight());
//		p = mutations.get(1);
//		assertEquals(Integer.valueOf(84), p.getLeft());
//		assertEquals("C/T", p.getRight());
		p = mutations.get(2);
		assertEquals(Integer.valueOf(154), p.getLeft());
		assertEquals("G/A", p.getRight());
//		p = mutations.get(3);
//		assertEquals(Integer.valueOf(145), p.getLeft());
//		assertEquals("A/G", p.getRight());
	}
	
	
	
	@Test
	public void getMutationFromSWDataMultipleSnp() {
		/*
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AAATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AAATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
n] INFO au.edu.qimr.clinvar.Q3ClinVar - TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG

		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		swData[1] = "|||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(8, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(6), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(19), p.getLeft());
		assertEquals("A/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(20), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(4);
		assertEquals(Integer.valueOf(21), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleSnpDiffBin() {
		/*
11																						AATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
11:51:37.621 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
11:51:37.621 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		swData[1] = "||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(8, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(4), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(18), p.getLeft());
		assertEquals("A/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(19), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(4);
		assertEquals(Integer.valueOf(20), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	
	@Test
	public void getMutationFromSWDataMultipleSnpAndIndel() {
		/*
		 * swData
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTAT-AAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG
||||||||||||||||||||||||||||||||||||| |||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG

xxData - missing the initial insertion
 ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG
||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG

		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTAT-AAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		swData[1] = "||||||||||||||||||||||||||||||||||||| |||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		String [] xxData = new String[3];
		xxData[0] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		xxData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||";
		xxData[2] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(7, mutations.size());
		List<Pair<Integer, String>> mutations2 = ClinVarUtil.getPositionRefAndAltFromSW(xxData);
		assertEquals(6, mutations2.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(36), p.getLeft());
		assertEquals("T/TA", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(54), p.getLeft());
		assertEquals("G/A", p.getRight());
		Pair<Integer, String> p2 = mutations2.get(0);
		assertEquals(Integer.valueOf(54), p2.getLeft());
		assertEquals("G/A", p2.getRight());
//		
//		p = mutations.get(2);
//		assertEquals(Integer.valueOf(18), p.getLeft());
//		assertEquals("A/G", p.getRight());
//		
//		p = mutations.get(3);
//		assertEquals(Integer.valueOf(19), p.getLeft());
//		assertEquals("C/T", p.getRight());
//		
//		p = mutations.get(4);
//		assertEquals(Integer.valueOf(20), p.getLeft());
//		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationString() {
		/*
		 * AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
 ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
 AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

		 */
		
		String [] swDiffs = new String[3];
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		assertEquals(null, ClinVarUtil.getMutationString(-1, 1, swDiffs));
		assertEquals(null, ClinVarUtil.getMutationString(0, 0, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(0, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(1, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(2, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(3, 1, swDiffs));
		
		swDiffs[0] = "ACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		assertEquals("C", ClinVarUtil.getMutationString(7, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(7, 2, swDiffs));
		assertEquals("CT", ClinVarUtil.getMutationString(7, 3, swDiffs));
		 
		 // this data has no mutations - should just get CT back
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("TTTTCT", ClinVarUtil.getMutationString(20, 6, swDiffs));
		
		
		// insertion
		swDiffs[0] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("CT", ClinVarUtil.getMutationString(37, 1, swDiffs));
	}
	
	@Test
	public void getMutationStringRealLife() {
		/*
		 * 	AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
			||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
			AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
		 */
		String [] swDiffs = new String[3];
		swDiffs[0] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		assertEquals("C", ClinVarUtil.getMutationString(146, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(169, 2, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(171, 1, swDiffs));
	}
	
	@Test
	public void getMutationStringRealLife2() {
		/*
		 * 	AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
			||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
			AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
		 */
		String [] swDiffs = new String[3];
		swDiffs[0] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		assertEquals("C", ClinVarUtil.getMutationString(146, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(169, 2, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(171, 1, swDiffs));
	}
	
	@Test
	public void getPositionInString() {
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(0, 0));
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(1, 1));
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(2, 2));
		assertEquals(1, ClinVarUtil.getZeroBasedPositionInString(3, 2));
		
		assertEquals(2, ClinVarUtil.getZeroBasedPositionInString(41265867, 41265865));
	}
	
	@Test
	public void getMutationFromSWRealLifeData() {
		/*
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
	}
	
}
