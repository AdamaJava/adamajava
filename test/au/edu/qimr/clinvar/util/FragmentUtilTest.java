package au.edu.qimr.clinvar.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class FragmentUtilTest {

	@Test
	public void testFrag() {
		
		assertEquals("", FragmentUtil.getFragmentString("", "", ""));
		assertEquals("ABCDE", FragmentUtil.getFragmentString("ABC", "CDE", "C"));
		assertEquals("ABCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDCDE", "CDCD"));
		assertEquals("ABCDCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDCDE", "CD"));
		assertEquals("ABCDCDE", FragmentUtil.getFragmentString("ABCDCD", "CDE", "CD"));
		
		/*
		 * other way round now
		 */
		assertEquals("ABCDEF", FragmentUtil.getFragmentString("CDEF", "ABCD", "CD"));
		assertEquals("ABCDCDEF", FragmentUtil.getFragmentString("CDEF", "ABCDCD", "CD"));
	}
	


}
