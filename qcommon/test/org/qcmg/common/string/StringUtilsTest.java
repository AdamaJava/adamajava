package org.qcmg.common.string;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;


public class StringUtilsTest {
	
	@Test
	public void testIsNumeric() {
		assertEquals(false, StringUtils.isNumeric(null));
		assertEquals(false, StringUtils.isNumeric(""));
		assertEquals(false, StringUtils.isNumeric(" "));
		assertEquals(false, StringUtils.isNumeric("  "));
		assertEquals(false, StringUtils.isNumeric("a"));
		assertEquals(false, StringUtils.isNumeric("abc"));
		assertEquals(false, StringUtils.isNumeric("123abc"));
		assertEquals(false, StringUtils.isNumeric("abc123"));
		assertEquals(false, StringUtils.isNumeric("123-456"));
		assertEquals(false, StringUtils.isNumeric("123456e"));
		assertEquals(true, StringUtils.isNumeric("0"));
		assertEquals(true, StringUtils.isNumeric("1"));
		assertEquals(true, StringUtils.isNumeric("123456"));
		assertEquals(true, StringUtils.isNumeric("-123456"));
		assertEquals(false, StringUtils.isNumeric("-"));
		assertEquals(true, StringUtils.isNumeric("-0"));
		assertEquals(false, StringUtils.isNumeric("-abc"));
	}
	
	@Test
	public void testAddASCIIValueToChar() {
		String s = "ABC";
		
		String returnedString = StringUtils.addASCIIValueToChar(s, 0);
		assertEquals(s, returnedString);
		
		// shift by 1
		returnedString = StringUtils.addASCIIValueToChar(s, 1);
		assertEquals("BCD", returnedString);
		
		// shift by -1
		returnedString = StringUtils.addASCIIValueToChar(s, -1);
		assertEquals("@AB", returnedString);
		
		// shift by 32
		returnedString = StringUtils.addASCIIValueToChar(s, 32);
		assertEquals("abc", returnedString);
	}
	
	@Test
	public void testAddASCIIValueToCharWithQualString() {
		String qualString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
		String expectedConvertedString = "BHHKIIIIKKKLJJFFFLLJIFFFFJORKKKNLKHHJJKKVVddg______dddddddWV";
		int offset = 33;
		
		String returnedString = StringUtils.addASCIIValueToChar(qualString, offset);
		assertEquals(expectedConvertedString, returnedString);
		
		// and now convert it back
		returnedString = StringUtils.addASCIIValueToChar(expectedConvertedString, -offset);
		assertEquals(qualString, returnedString);
	}
	
	@Test
	public void testPadString() {
		String s = "";
		
		String returnedString = StringUtils.padString(s, 10, ' ', true);
		assertEquals("          ", returnedString);
		returnedString = StringUtils.padString(s, 10, ' ', false);
		assertEquals("          ", returnedString);
		
		s = "123";
		returnedString = StringUtils.padString(s, 7, ' ', true);
		assertEquals("    123", returnedString);
		returnedString = StringUtils.padString(s, 7, ' ', false);
		assertEquals("123    ", returnedString);
		
		returnedString = StringUtils.padString(s, 6, '9', true);
		assertEquals("999123", returnedString);
		returnedString = StringUtils.padString(s, 6, '9', false);
		assertEquals("123999", returnedString);
		
		s = "11111";
		returnedString = StringUtils.padString(s, 8, '0', true);
		assertEquals("00011111", returnedString);
		returnedString = StringUtils.padString(s, 8, '0', false);
		assertEquals("11111000", returnedString);
	}
	
	@Test
	public void testPadStringInvalidLengths() {
		String s = "abc";
		
		// zero padding - should return orig string
		String returnedString = StringUtils.padString(s, 0, ' ', true);
		assertEquals("abc", returnedString);
		returnedString = StringUtils.padString(s, 0, ' ', false);
		assertEquals("abc", returnedString);
		
		// negative padding - should return orig string
		returnedString = StringUtils.padString(s, -1, '0', true);
		assertEquals("abc", returnedString);
		returnedString = StringUtils.padString(s, -10, '3', false);
		assertEquals("abc", returnedString);
		
		// padding that might result in orig string being cut - should return orig string
		returnedString = StringUtils.padString(s, 1, '0', true);
		Assert.assertEquals("abc", returnedString);
		returnedString = StringUtils.padString(s, 2, '3', false);
		Assert.assertEquals("abc", returnedString);
	}
	
	@Test
	public void testPadStringInvalidString() {
		String s = null;
		
		String returnedString = StringUtils.padString(s, 10, ' ', true);
		Assert.assertNull(returnedString);
		returnedString = StringUtils.padString(s, 10, ' ', false);
		Assert.assertNull(returnedString);
	}
	
	@Test
	public void testGetPositionOfStringInArray() {
		try {
			StringUtils.getPositionOfStringInArray(null, null, false);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		assertEquals(-1, StringUtils.getPositionOfStringInArray(new String[] {}, "", false));
		assertEquals(0, StringUtils.getPositionOfStringInArray(new String[] {""}, "", false));
		assertEquals(0, StringUtils.getPositionOfStringInArray(new String[] {"","",""}, "", false));
		assertEquals(1, StringUtils.getPositionOfStringInArray(new String[] {"1","2","3"}, "2", false));
		assertEquals(-1, StringUtils.getPositionOfStringInArray(new String[] {"one","two","Three"}, "three", false));
		assertEquals(2, StringUtils.getPositionOfStringInArray(new String[] {"one","two","Three"}, "three", true));
		assertEquals(2, StringUtils.getPositionOfStringInArray(new String[] {"one","two","three","four","Three"}, "three", true));
	}
	
	@Test
	public void testIsNullOrEmpty() {
		assertEquals(true,  StringUtils.isNullOrEmpty(null));
		assertEquals(true,  StringUtils.isNullOrEmpty(""));
		assertEquals(true,  StringUtils.isNullOrEmpty("  "));
		assertEquals(true,  StringUtils.isNullOrEmpty("	"));
		assertEquals(true,  StringUtils.isNullOrEmpty("\t"));
		assertEquals(true,  StringUtils.isNullOrEmpty("\n"));
		assertEquals(false,  StringUtils.isNullOrEmpty("\n", false));
		assertEquals(false,  StringUtils.isNullOrEmpty("  ", false));
		assertEquals(false,  StringUtils.isNullOrEmpty("1"));
		assertEquals(false,  StringUtils.isNullOrEmpty("   empty   "));
		assertEquals(false,  StringUtils.isNullOrEmpty("  1 ", false));
	}
	
	@Test
	public void testIsStringInStringArray() {
		try {
			StringUtils.isStringInStringArray(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		try {
			StringUtils.isStringInStringArray(null, new String []{""});
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		try {
			StringUtils.isStringInStringArray("", null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		assertEquals(false,  StringUtils.isStringInStringArray("", new String []{}));
		assertEquals(true,  StringUtils.isStringInStringArray("", new String []{""}));
		assertEquals(true,  StringUtils.isStringInStringArray("1", new String []{"3","2","1"}));
		assertEquals(true,  StringUtils.isStringInStringArray("XYZ", new String []{"3","XYZ","1"}));
		assertEquals(false,  StringUtils.isStringInStringArray("xyz", new String []{"3","XYZ","1"}));
		
	}
	
	@Test
	public void testGetChrPositionFromString() {
		//Returns a ChrPosition object based on a string of the following format: chr1:123456-123456
		assertEquals(new ChrPosition("1", 1), StringUtils.getChrPositionFromString("1:1-1"));
		assertEquals(new ChrPosition("chr1", 1), StringUtils.getChrPositionFromString("chr1:1-1"));
		assertEquals(new ChrPosition("chr1", 1, 2), StringUtils.getChrPositionFromString("chr1:1-2"));
		try {
			assertEquals(new ChrPosition("XYZ", -10, -2), StringUtils.getChrPositionFromString("XYZ:-10--2"));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(new ChrPosition("XYZ", -10, -2), StringUtils.getChrPositionFromString("XYZ123"));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(new ChrPosition("XYZ", -10, -2), StringUtils.getChrPositionFromString("XYZ:1--23"));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testDoesStringContainSubString() {
		try {
			StringUtils.doesStringContainSubString(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			StringUtils.doesStringContainSubString(null, null, true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(false, StringUtils.doesStringContainSubString(null, null, false));
		
		try {
			StringUtils.doesStringContainSubString("", "");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			StringUtils.doesStringContainSubString("", "", true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(false, StringUtils.doesStringContainSubString("", "", false));
		
		assertEquals(false, StringUtils.doesStringContainSubString("abc", "def"));
		assertEquals(false, StringUtils.doesStringContainSubString("abc", "abcd"));
		assertEquals(true, StringUtils.doesStringContainSubString("abc", "abc"));
		assertEquals(false, StringUtils.doesStringContainSubString("abc", "ABC"));
		assertEquals(true, StringUtils.doesStringContainSubString("a b c", "a b c"));
		assertEquals(false, StringUtils.doesStringContainSubString("abc", " abc "));
		assertEquals(true, StringUtils.doesStringContainSubString(" abc ", "abc"));
	}
	
	@Test
	public void testGetChildArrayFromParentArray() {
		try {
			StringUtils.getChildArrayFromParentArray(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		String[] parentArray = new String[] {};
		try {
			StringUtils.getChildArrayFromParentArray(parentArray, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		Integer[] positionsArray = new Integer[] {};
		try {
			StringUtils.getChildArrayFromParentArray(null, positionsArray);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(0, StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).size());
		
		parentArray = new String[] {"gene1", "gene2"};
		positionsArray = new Integer[] {0};
		assertEquals(1, StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).size());
		assertEquals("gene1", StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).get(0));
		positionsArray = new Integer[] {0,1};
		assertEquals(2, StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).size());
		assertEquals("gene1", StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).get(0));
		assertEquals("gene2", StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).get(1));
		
		parentArray = new String[] {"gene1", "gene2", "gene3"};
		positionsArray = new Integer[] {2};
		assertEquals(1, StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).size());
		assertEquals("gene3", StringUtils.getChildArrayFromParentArray(parentArray, positionsArray).get(0));
		
		try {
			positionsArray = new Integer[] {3};
			StringUtils.getChildArrayFromParentArray(parentArray, positionsArray);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testGetValueFromKey() {
		String commentLine = "@CO	CN:QCMG	PN:qlimsmeta	Project=test_project	Donor=TestDonor	Material=1:DNA	Sample Code=Tumour	Sample=Sample-1	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=HiSeq";
		
		assertEquals("test_project", StringUtils.getValueFromKey(commentLine, "Project"));
		assertEquals("TestDonor", StringUtils.getValueFromKey(commentLine, "Donor"));
		assertEquals("1:DNA", StringUtils.getValueFromKey(commentLine, "Material"));
		assertEquals("Tumour", StringUtils.getValueFromKey(commentLine, "Sample Code"));
		assertEquals("Sample-1", StringUtils.getValueFromKey(commentLine, "Sample"));
		assertEquals("bwa", StringUtils.getValueFromKey(commentLine, "Aligner"));
		assertEquals("Human Rapid Exome (Nextera)", StringUtils.getValueFromKey(commentLine, "Capture Kit"));
		assertEquals("HiSeq", StringUtils.getValueFromKey(commentLine, "Sequencing Platform"));
	}
	
	
	@Test
	public void testGetJoinedString() {
		assertEquals(null, StringUtils.getJoinedString(null, null));
		assertEquals("", StringUtils.getJoinedString("", null));
		assertEquals("", StringUtils.getJoinedString(null, ""));
		assertEquals(" ", StringUtils.getJoinedString(" ", null));
		assertEquals(" ", StringUtils.getJoinedString(null, " "));
		assertEquals("", StringUtils.getJoinedString("", ""));
		assertEquals(" ", StringUtils.getJoinedString(" ", ""));
		assertEquals(" ", StringUtils.getJoinedString("", " "));
		assertEquals("   ", StringUtils.getJoinedString(" ", " "));
	}
	
	@Test
	public void testPassesOccurenceCountCheck() {
		String dodgyBamHeader = "@HD     VN:1.0  GO:none SO:coordinate" + 
"@SQ     SN:chr1 LN:249250621" + 
"@SQ     SN:chr2 LN:243199373" + 
"@SQ     SN:chr3 LN:198022430" + 
"@SQ     SN:chr4 LN:191154276" + 
"@SQ     SN:chr5 LN:180915260" + 
"@SQ     SN:chr6 LN:171115067" + 
"@SQ     SN:chr7 LN:159138663" + 
"@SQ     SN:chr8 LN:146364022" + 
"@SQ     SN:chr9 LN:141213431" + 
"@SQ     SN:chr10        LN:135534747" + 
"@SQ     SN:chr11        LN:135006516" + 
"@SQ     SN:chr12        LN:133851895" + 
"@SQ     SN:chr13        LN:115169878" + 
"@SQ     SN:chr14        LN:107349540" + 
"@SQ     SN:chr15        LN:102531392" + 
"@SQ     SN:chr16        LN:90354753" + 
"@SQ     SN:chr17        LN:81195210" + 
"@SQ     SN:chr18        LN:78077248" + 
"@SQ     SN:chr19        LN:59128983" + 
"@SQ     SN:chr20        LN:63025520" + 
"@SQ     SN:chr21        LN:48129895" + 
"@SQ     SN:chr22        LN:51304566" + 
"@SQ     SN:chrX LN:155270560" +
"@SQ     SN:chrY LN:59373566" + 
"@SQ     SN:GL000191.1   LN:106433" + 
"@SQ     SN:GL000192.1   LN:547496" + 
"@SQ     SN:GL000193.1   LN:189789" + 
"@SQ     SN:GL000194.1   LN:191469" + 
"@SQ     SN:GL000195.1   LN:182896" + 
"@SQ     SN:GL000196.1   LN:38914" + 
"@SQ     SN:GL000197.1   LN:37175" + 
"@SQ     SN:GL000198.1   LN:90085" + 
"@SQ     SN:GL000199.1   LN:169874" + 
"@SQ     SN:GL000200.1   LN:187035" + 
"@SQ     SN:GL000201.1   LN:36148" + 
"@SQ     SN:GL000202.1   LN:40103" + 
"@SQ     SN:GL000203.1   LN:37498" + 
"@SQ     SN:GL000204.1   LN:81310" + 
"@SQ     SN:GL000205.1   LN:174588" + 
"@SQ     SN:GL000206.1   LN:41001" + 
"@SQ     SN:GL000207.1   LN:4262" + 
"@SQ     SN:GL000208.1   LN:92689" + 
"@SQ     SN:GL000209.1   LN:159169" + 
"@SQ     SN:GL000210.1   LN:27682" + 
"@SQ     SN:GL000211.1   LN:166566" + 
"@SQ     SN:GL000212.1   LN:186858" + 
"@SQ     SN:GL000213.1   LN:164239" + 
"@SQ     SN:GL000214.1   LN:137718" + 
"@SQ     SN:GL000215.1   LN:172545" + 
"@SQ     SN:GL000216.1   LN:172294" + 
"@SQ     SN:GL000217.1   LN:172149" + 
"@SQ     SN:GL000218.1   LN:161147" + 
"@SQ     SN:GL000219.1   LN:179198" + 
"@SQ     SN:GL000220.1   LN:161802" + 
"@SQ     SN:GL000221.1   LN:155397" + 
"@SQ     SN:GL000222.1   LN:186861" + 
"@SQ     SN:GL000223.1   LN:180455" + 
"@SQ     SN:GL000224.1   LN:179693" + 
"@SQ     SN:GL000225.1   LN:211173" + 
"@SQ     SN:GL000226.1   LN:15008" + 
"@SQ     SN:GL000227.1   LN:128374" + 
"@SQ     SN:GL000228.1   LN:129120" + 
"@SQ     SN:GL000229.1   LN:19913" + 
"@SQ     SN:GL000230.1   LN:43691" + 
"@SQ     SN:GL000231.1   LN:27386" + 
"@SQ     SN:GL000232.1   LN:40652" + 
"@SQ     SN:GL000233.1   LN:45941" + 
"@SQ     SN:GL000234.1   LN:40531" + 
"@SQ     SN:GL000235.1   LN:34474" + 
"@SQ     SN:GL000236.1   LN:41934" + 
"@SQ     SN:GL000237.1   LN:45867" + 
"@SQ     SN:GL000238.1   LN:39939" + 
"@SQ     SN:GL000239.1   LN:33824" + 
"@SQ     SN:GL000240.1   LN:41933" + 
"@SQ     SN:GL000241.1   LN:42152" + 
"@SQ     SN:GL000242.1   LN:43523" + 
"@SQ     SN:GL000243.1   LN:43341" + 
"@SQ     SN:GL000244.1   LN:39929" + 
"@SQ     SN:GL000245.1   LN:36651" + 
"@SQ     SN:GL000246.1   LN:38154" + 
"@SQ     SN:GL000247.1   LN:36422" + 
"@SQ     SN:GL000248.1   LN:39786" + 
"@SQ     SN:GL000249.1   LN:38502" + 
"@SQ     SN:chrMT        LN:16569"; 
		
		
		assertEquals(true, StringUtils.passesOccurenceCountCheck(dodgyBamHeader, "@SQ", 100000));
	}
	
}
