package org.qcmg.motif;

import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReferenceMotifFinderTest {
	
	ReferenceMotifFinder rmf ;
	
	@Before
	public void setup() {
		rmf = new ReferenceMotifFinder(null, null, null);
	}
	
	@After
	public void tearDown() {
		rmf = null;
	}
	
	@Test
	public void testRegex() {
		
		String [] stringsToTest = new String[]{"TTAGGG",
			"TCAGGG",
			"TTCGGG",
			"GTAGGG",
			"TGAGGG",
			"TTGGGG",
			"TAAGGG",
			"ATAGGG",
			"CTAGGG",
			"TTTGGG",
			"TTAAGGG",
			// now the complement
			"CCCTAA",
			"CCCTGA",
			"CCCGAA",
			"CCCTAC",
			"CCCCAA",
			"CCCTTA",
			"CCCTAT",
			"CCCTAG",
			"CCCAAA",
			"CCCTTAA",
		};
		
		int count = 0;
		for (String s :  stringsToTest) {
			Matcher m = rmf.TELOMERE_PATTERN.matcher(s+s);
			while (m.find()) count++;
		}			
		assertEquals(stringsToTest.length, count);
	}
	
	@Test
	public void testRegexFail() {
		
		String [] stringsToTest = new String[]{"AATGGG",
				"AGTGGG",
				"CCTGGG",
				"GGAGGG",
				"CGAGGG",
				"AACGGG",
				"ATTGGG",
				"TATGGG",
				"GATGGG",
				"CCCGGG",
				"AATTGGG",
				// now the complement
				"CCCATT",
				"CCCACT",
				"CCCCTT",
				"CCCATG",
				"CCCGTT",
				"CCCAAT",
				"CCCATA",
				"CCCATC",
				"CCCTTT",
				"CCCAATT",
		};
		
		int count = 0;
		for (String s :  stringsToTest) {
			Matcher m = rmf.TELOMERE_PATTERN.matcher(s+s);
			while (m.find()) {
				System.out.println("found match for " + s);
				count++;
			}
		}			
		assertEquals(0, count);
	}
	
	@Test
	public void checkPositionOfMatch() {
		String reference = "NNNNACGTACGTACGTACGTCCCTAACCCTAAACGTGTAGGGGTAGGG";
		Matcher m = rmf.TELOMERE_PATTERN.matcher(reference);
		assertEquals(true, m.find());
		assertEquals(20, m.start());
		assertEquals(true, m.find());
		assertEquals(36, m.start());
		assertEquals(false, m.find());
	}

}
