package org.qcmg.qio.fasta;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.qio.fasta.FastaRecord;

public class FastaRecordTest {

	@Test
	public void testParseIDInvalid() throws Exception {
		// test empty string
		try {
			(new FastaRecord()).setId("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: ", e.getMessage());
		}
				
		// string containing anything other than '>'
		String testString = "this is an id $%^&*()";
		try {
			(new FastaRecord()).setId(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
		
		// string containing '>' but not at the start..
		testString = "this is an > id";
		try {
			
			(new FastaRecord()).setId(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseID() throws Exception {
		FastaRecord frecord = new FastaRecord();		
		String returnedID;
				
		frecord.setId(">123");
		returnedID = frecord.getId(); 
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123", returnedID);
		
		
		frecord.setId(">");
		returnedID = frecord.getId(); 
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">", returnedID);
		
		frecord.setId(">123_456_789");
		returnedID = frecord.getId();
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123_456_789", returnedID);
		
		frecord.setId(">>>");
		returnedID = frecord.getId();
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">>>", returnedID);
		
	}
	
	@Test
	public void testParseSequenceInvalid() throws Exception {
		// test null
		// test id string
		String testString = ">123";
		try {
			(new FastaRecord()).setData(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad sequence format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseSequence() throws Exception {
		FastaRecord frecord = new FastaRecord();	
		
		frecord.setData("G0103200103201032001033001032001032001032001032001");
		String returnedSequence = frecord.getData(); 
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", returnedSequence);	
		
		frecord.setData("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18");
		returnedSequence = frecord.getData(); 
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18", returnedSequence);
	}
	
	@Test
	public void testParseRecords() throws Exception {
		FastaRecord record = new FastaRecord(">123", "G0103200103201032001033001032001032001032001032001");
		
		// real record
		Assert.assertNotNull(record);
		Assert.assertEquals(">123", record.getId());
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", record.getData());
	}
}
