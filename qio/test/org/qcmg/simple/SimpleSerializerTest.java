package org.qcmg.simple;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.record.SimpleRecord;


public class SimpleSerializerTest {

	@Test
	public void testParseIDInvalid() throws Exception {
		// test empty string
		try {
			SimpleSerializer.parseID("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: ", e.getMessage());
		}
		
		// test null
//		try {
//			SimpleSerializer.parseID(null);
//			Assert.fail("Should have thrown an Exception");
//		} catch (Exception e) {
//			Assert.assertEquals("Bad id format", e.getMessage());
//		}
		
		// string containing anything other than '>'
		String testString = "this is an id $%^&*()";
		try {
			SimpleSerializer.parseID(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
		
		// string containing '>' but not at the start..
		testString = "this is an > id";
		try {
			SimpleSerializer.parseID(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad id format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseID() throws Exception {
		String returnedID;
		
		returnedID = SimpleSerializer.parseID(">123");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123", returnedID);
		
		returnedID = SimpleSerializer.parseID(">");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">", returnedID);
		
		returnedID = SimpleSerializer.parseID(">123_456_789");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">123_456_789", returnedID);
		
		returnedID = SimpleSerializer.parseID(">>>");
		Assert.assertNotNull(returnedID);
		Assert.assertEquals(">>>", returnedID);
		
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
			SimpleSerializer.parseSequence(testString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Bad sequence format: " + testString, e.getMessage());
		}
	}
	
	@Test
	public void testParseSequence() throws Exception {
		
		String returnedSequence = SimpleSerializer.parseSequence("G0103200103201032001033001032001032001032001032001");
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", returnedSequence);
		
		returnedSequence = SimpleSerializer.parseSequence("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18");
		Assert.assertNotNull(returnedSequence);
		Assert.assertEquals("31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18", returnedSequence);
	}
	
	@Test
	public void testParseRecords() throws Exception {
		SimpleRecord record;
		
		// real record
		record = SimpleSerializer.parseRecord(">123", "G0103200103201032001033001032001032001032001032001");
		Assert.assertNotNull(record);
		Assert.assertEquals(">123", record.getId());
		Assert.assertEquals("G0103200103201032001033001032001032001032001032001", record.getData());
	}
}
