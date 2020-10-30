package org.qcmg.fasta;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.fasta.FastaRecord;


public class FastaRecordTest {

	@Test
	public void testParseIDInvalid() throws Exception {
		// test empty string
		try {
			new FastaRecord().setId("");;
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: ", e.getMessage());
		}
				
		// string containing anything other than '>'
		String testString = "this is an id $%^&*()";
		try {
			new FastaRecord().setId(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
		
		// string containing '>' but not at the start..
		testString = "this is an > id";
		try {
			new FastaRecord().setId(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseID() throws Exception {
		String returnedID;
				 
		returnedID = getId(">123");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123", returnedID);
		
		returnedID = getId(">");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">", returnedID);
		
		returnedID = getId(">123_456_789");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123_456_789", returnedID);
		
		returnedID = getId(">>>");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">>>", returnedID);
		
	}
	private String getId(String str) throws Exception {
		FastaRecord rec = new FastaRecord();
		rec.setId(str);
		return rec.getId();
	}
	
	@Test
	public void testParseSequenceInvalid() throws Exception {
		// test null
//		try {
//			SimpleSerializer.parseSequence(null);
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {
//			Assert.assertEquals("Bad sequence format", e.getMessage());
//		}
//		// test empty string
//		try {
//			SimpleSerializer.parseSequence("");
//			Assert.fail("Should have thrown an Exception");
//		} catch (Exception e) {
//			Assert.assertEquals("Bad sequence format", e.getMessage());
//		}
//		try {
//			SimpleSerializer.parseSequence("								");
//			Assert.fail("Should have thrown an Exception");
//		} catch (Exception e) {
//			Assert.assertEquals("Bad sequence format", e.getMessage());
//		}
		// test id string
		String testString = ">123";
		try {
			new FastaRecord().setData(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad sequence format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseSequence() throws Exception {
		
		String returnedSequence = getData("G0103200103201032001033001032001032001032001032001");
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", returnedSequence);
		
		returnedSequence = getData("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18");
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18", returnedSequence);
	}
	
	private String getData(String str) throws Exception {
		FastaRecord rec = new FastaRecord();
		rec.setData(str);
		return rec.getData();
	}
	
	@Test
	public void testParseRecords() throws Exception {
	 
		
		// real record
		FastaRecord record = new FastaRecord(">123", "G0103200103201032001033001032001032001032001032001");
		Assert.assertNotNull(record);
		Assert.assertEquals(">123", record.getId());
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", record.getData());
	}
}
