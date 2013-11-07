package org.qcmg.gff;

import org.junit.Assert;
import org.junit.Test;


public class GFFSerializerTest {

	@Test
	public void testParseDataInvalid() throws Exception {
		// test empty string
		try {
			GFFSerializer.parseData("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
		try {
			GFFSerializer.parseData("								");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
		
		// test null
		try {
			GFFSerializer.parseData(null);
			Assert.fail("Should have thrown an Exception");
		} catch (AssertionError e) {
			Assert.assertEquals("Record was null", e.getMessage());
		}
		
		// string with fewer than 8 entries
		try {
			GFFSerializer.parseData("1	2	3	4	5	6	");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
	}
	
	@Test
	public void testParseData() throws Exception {
		String[] returnedArray;
		
		// test with 8 entries
		returnedArray = GFFSerializer.parseData("a	b	c	d	e	f	g	h");
		Assert.assertEquals(8, returnedArray.length);
		Assert.assertEquals("a", returnedArray[0]);
		Assert.assertEquals("h", returnedArray[7]);
		
		// test with 9 entries
		returnedArray = GFFSerializer.parseData("a	b	c	d	e	f	g	h	i");
		Assert.assertEquals(9, returnedArray.length);
		Assert.assertEquals("a", returnedArray[0]);
		Assert.assertEquals("h", returnedArray[7]);
		Assert.assertEquals("i", returnedArray[8]);
		
		// test with 10 entries
		returnedArray = GFFSerializer.parseData("a	b	c	d	e	f	g	h	i	j");
		Assert.assertEquals(10, returnedArray.length);
		Assert.assertEquals("a", returnedArray[0]);
		Assert.assertEquals("h", returnedArray[7]);
		Assert.assertEquals("j", returnedArray[9]);
		
		//test with realistic data string
		returnedArray = GFFSerializer.parseData("1	solid	read	10148	10190	14.4	-	.	" +
				"aID=1212_1636_246;at=F3;b=GGTTAGGGTTAGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG;" +
				"g=G0103200103201032001033001032001032001032001032001;mq=43;o=0;" +
				"q=31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18," +
				"18,13,14,18,19,16,14,5,16,23,18,21,16,16,14,20,13,17,20,11;r=23_2;s=a23;u=0,4,1,1");
		Assert.assertEquals(9, returnedArray.length);
		Assert.assertEquals("solid", returnedArray[1]);
		Assert.assertEquals("read", returnedArray[2]);
	}
	
	@Test
	public void testParseRecordInvalid() throws Exception {
		// test null
		try {
			GFFSerializer.parseRecord(null);
			Assert.fail("Should have thrown an exception");
		} catch (AssertionError e) {
			Assert.assertEquals("Record was null", e.getMessage());
		}
		// test empty string
		try {
			GFFSerializer.parseRecord("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
		try {
			GFFSerializer.parseRecord("								");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
	}
	
	@Test
	public void testParseRecord() throws Exception {
		GFFRecord record;
		
		// 8 values
		record = GFFSerializer.parseRecord("this	is	a	0	1	0.0	works	OK");
		Assert.assertNotNull(record);
		Assert.assertEquals("this", record.getSeqname());
		Assert.assertEquals("OK", record.getFrame());
		
		
	}
	
	@Test
	public void testParseRecordWithAttributes() throws Exception {
		GFFRecord record;
		
		// real record containing attributes
		record = GFFSerializer.parseRecord("1	solid	read	10148	10190	14.4	-	.	" +
				"aID=1212_1636_246;at=F3;b=GGTTAGGGTTAGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG;" +
				"g=G0103200103201032001033001032001032001032001032001;mq=43;o=0;" +
				"q=31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18," +
				"18,13,14,18,19,16,14,5,16,23,18,21,16,16,14,20,13,17,20,11;r=23_2;s=a23;u=0,4,1,1");
		Assert.assertNotNull(record);
		Assert.assertEquals("1", record.getSeqname());
		Assert.assertEquals("solid", record.getSource());
		Assert.assertEquals("read", record.getFeature());
		Assert.assertEquals(10148, record.getStart());
		Assert.assertEquals(10190, record.getEnd());
		Assert.assertTrue(14.4 == record.getScore());
		Assert.assertEquals("-", record.getStrand());
		Assert.assertEquals(".", record.getFrame());
		
	}
	
	@Test
	public void testParseRecordWithInvalidAttributes() throws Exception {
		try {
			GFFSerializer.parseRecord("sequence	source	feature	0	1	99.99	strand	frame	attributes");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertEquals("Attribute [attributes] is badly formed", e.getMessage());
		}
	}
}
