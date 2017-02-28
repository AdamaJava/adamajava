package au.edu.qimr.clinvar.util;

import static org.junit.Assert.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.Collections;

import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;

import au.edu.qimr.clinvar.model.Fragment;
import au.edu.qimr.clinvar.model.Fragment2;

public class FragmentUtilTest {

	@Test
	public void testFrag() {
		
		assertEquals("", FragmentUtil.getFragmentString("", "", "").get());
		assertEquals("ABCDE", FragmentUtil.getFragmentString("ABC", "CDE", "C").get());
		assertEquals("ABCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDCDE", "CDCD").get());
		assertEquals("ABCDCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDCDE", "CD").get());
		assertEquals("ABCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDE", "CD").get());
		
		/*
		 * other way round now
		 */
		assertEquals("ABCDEF", FragmentUtil.getFragmentString("CDEF", "ABCD", "CD").get());
		assertEquals("ABCDCDEF", FragmentUtil.getFragmentString("CDEF", "ABCDCD", "CD").get());
	}
	
	@Test
	public void testGetBase() {
		/*
		 * cp : chr13:28610098-28610098, fCP: chr13:28610098-28610267, fragment length: 170, len: 1,offset: -1, fragment: CCAGGTCCAAGATGGTAATGGGTATCCATCCGAGAAACAGGACGCCTGACTTGCCGATGCTTCAGCGAGCACTTGAGGTTTCCCTGTAGAGAAGAACGTGTGAAATAAGCTCACTGGCTGGGCATAGTGGTTCACTCCTATAATACCAATACTTTGTGAAGCCAAGGTGG
		 */
		ChrPosition cp = new ChrPointPosition("chr13", 28610098);
		ChrPosition fCp = new ChrRangePosition("chr13", 28610098, 28610267);
		Fragment2 f = new Fragment2(1, "CCAGGTCCAAGATGGTAATGGGTATCCATCCGAGAAACAGGACGCCTGACTTGCCGATGCTTCAGCGAGCACTTGAGGTTTCCCTGTAGAGAAGAACGTGTGAAATAAGCTCACTGGCTGGGCATAGTGGTTCACTCCTATAATACCAATACTTTGTGAAGCCAAGGTGG");
		f.setPosition(fCp, true);;
		
		assertEquals("C", FragmentUtil.getBasesAtPosition(cp, f, 1).get());
	}
	
	@Test
	public void testGetBaseEndOfRead() {
		/*
		 * chr22:24176495-24176495, fCP: chr22:24176324-24176495, fragment length: 171, len: 1, fragment: CCAGGCGGATGAGGCGTCTTGCCAGCACGGCCCCGGCCTGGTAACAGCCTATCAGCACACGGCTCCCACGGAGCATCTCAGAAGATTGGGCCGCCTCTCCTCCATCTTCTGGCAAGGACAGAGGCGAGGGGACAGCCCAGCGCCATCCTGAGGATCGGGTGGGGGTGGAGC
		 */
		ChrPosition cp = new ChrPointPosition("chr22", 24176495);
		ChrPosition fCp = new ChrRangePosition("chr22", 24176324, 24176495);
		Fragment2 f = new Fragment2(1, "CCAGGCGGATGAGGCGTCTTGCCAGCACGGCCCCGGCCTGGTAACAGCCTATCAGCACACGGCTCCCACGGAGCATCTCAGAAGATTGGGCCGCCTCTCCTCCATCTTCTGGCAAGGACAGAGGCGAGGGGACAGCCCAGCGCCATCCTGAGGATCGGGTGGGGGTGGAGC");
		f.setPosition(fCp, true);;
		
//		assertEquals("C", FragmentUtil.getBasesAtPosition(cp, f, 1).get());
	}
	


}
