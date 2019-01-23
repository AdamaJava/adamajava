package org.qcmg.common.vcf.header;



import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;


public class VcfHeaderTest {

	@Test
	public void sampleIdTest(){
		final VcfHeader header = new VcfHeader();
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Control	Test", true);
		String[] sample = header.getSampleId();
		assertTrue(sample[0].equals("Control"));
		assertTrue(sample[1].equals("Test"));
		
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tControl\tTest", true);
		assertNull(header.getSampleId()); 		
		
		VcfHeaderRecord re = new VcfHeaderRecord("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Control	Test");
		header.addOrReplace(re);
		assertTrue(header.getSampleId()[0].equals("Control"));		
		
	}
	
	@Test
	public void seeIfMultipleInputsAreAllowed() {
		VcfHeader header = new VcfHeader();
		int i = 1;
		String s = "inputFile";
		String s2 = "inputFile2";
		header.addOrReplace (VcfHeaderUtils.BLANK_HEADER_LINE + "INPUT=" + i++ + ",FILE=" + s, false);
		header.addOrReplace (VcfHeaderUtils.BLANK_HEADER_LINE + "INPUT=" + i++ + ",FILE=" + s2, false);
		assertEquals(2, header.getAllMetaRecords().size());
	}
	
	@Test
	public void examineOrderOfRecordsReturned() {
		VcfHeader header = new VcfHeader();
		header.addFormat(VcfHeaderUtils.FORMAT_MUTANT_READS, "1", "String", "teenage mutant ninja turtles");
		header.addFilter(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST, "NCIT");
		header.addInfo(VcfHeaderUtils.INFO_DONOR , "1", "String",  "donor details here");
		VcfHeaderUtils.addQPGLineToHeader(header, "junit", "4.10", "something like this");
		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT, true);
		header.addOrReplace("##QZ=qz1");
		header.addOrReplace("##QZ=qz2",false);
		
		VcfHeaderRecord qpgRec = VcfHeaderUtils.getqPGRecords(header).get(0);
		VcfHeaderRecord formatRec = header.getFormatRecord(VcfHeaderUtils.FORMAT_MUTANT_READS) ;
		VcfHeaderRecord filterRec = header.getFilterRecord(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST);
		VcfHeaderRecord infoRec = header.getInfoRecord(VcfHeaderUtils.INFO_DONOR);
		
		assertNotNull(qpgRec);
		assertNotNull(formatRec);
		assertNotNull(filterRec);
		assertNotNull(infoRec);
		
		int i = 0 ;
		for (VcfHeaderRecord rec : header) {
			i++;			
			if (i == 1)  assertEquals(VcfHeaderUtils.CURRENT_FILE_FORMAT, rec.toString());		
			if( i == 2) assertEquals("##QZ=qz1", rec.toString());
			if( i == 3) assertEquals("##QZ=qz2", rec.toString());
			if (i == 4)  assertEquals(filterRec, rec);			 
			if (i == 6)   assertEquals(formatRec, rec);			 
			if (i == 5)   assertEquals(infoRec, rec);			 
			if (i == 7)   assertEquals(qpgRec, rec);
		}
		
	}
	
	@Test
	public void doWeHaveQIMRDataInHeader() {
		VcfHeader header = new VcfHeader();
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT, true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=today", true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		String infoLine = VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=" +  VcfHeaderUtils.INFO_DB + ",Number=1,Type=String,Description=\"anything will do\">";
		header.addOrReplace(infoLine, true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		String formatLine = VcfHeaderUtils.HEADER_LINE_FORMAT + "=<ID=" +  VcfHeaderUtils.FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"anything will do\">";
		header.addOrReplace(formatLine, true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.addOrReplace(filterLine, true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		
		// lets add some qcmg data
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_!23_123", true);
		assertEquals(false,  VcfHeaderUtils.containsQIMRDetails(header));
		// need some qpg lines too
		VcfHeaderUtils.addQPGLine(header, 111 , "test", "1.1", "who knows", "right now - yesterday even");
		assertEquals(true,  VcfHeaderUtils.containsQIMRDetails(header));
	}
	
	@Test
	public void replace() {
		VcfHeader header = new VcfHeader();
		String inputUuid = "123_123_123_!23";
		String inputVcfName = "testVcf.vcf";
		header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName, true);
		assertEquals(1, header.getAllMetaRecords().size());
		assertEquals(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName, header.getRecords(VcfHeaderUtils.STANDARD_INPUT_LINE).get(0).toString());
		
		String inputUuid2 = "456_456_456_456";
		header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid2 + ":"+ inputVcfName, true);
		assertEquals(1, header.getAllMetaRecords().size());
		assertEquals(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid2 + ":"+ inputVcfName, header.getRecords(VcfHeaderUtils.STANDARD_INPUT_LINE).get(0).toString());
		header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "123=" + inputUuid2 + ":"+ inputVcfName, false);
		assertEquals(2, header.getAllMetaRecords().size());
		assertEquals(1, header.getRecords(VcfHeaderUtils.STANDARD_INPUT_LINE).size());
	}
	
	@Test
	public void parseDataNullAndEmpty() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace((String) null, true);
		assertTrue(header.getAllMetaRecords().size() == 0);
		header.addOrReplace("", true);
		assertTrue(header.getAllMetaRecords().size() == 0);
		
		
		int i = 0; //empty header only contain format and Chrom line
		for(VcfHeaderRecord re: header){
			i++;
			assertTrue( re.toString().startsWith(VcfHeaderUtils.CURRENT_FILE_FORMAT) 
					|| re.toString().startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE) );			
		}		
		assertTrue(i == 2);
		
	}
	@Test
	public void parseDataFileFormat() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=1.1", true);
		assertEquals(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=1.1", header.getFileFormat().toString());
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=1.2", true);
		assertEquals(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=1.2", header.getFileFormat().toString());
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=XYZ", true);
		assertEquals(VcfHeaderUtils.STANDARD_FILE_FORMAT + "=XYZ", header.getFileFormat().toString());
	}
	@Test
	public void parseDataFileDate() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=today", true);
		assertEquals(VcfHeaderUtils.STANDARD_FILE_DATE + "=today", header.getFileDate().toString());
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=yesterday", true);
		assertEquals("yesterday", header.getFileDate().getMetaValue());
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=tomorrow", true);
		assertEquals("tomorrow", header.getFileDate().getMetaValue());
	}
	@Test
	public void parseDataUUID() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_123_123", true);
		assertEquals(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_123_123", header.getUUID().toString());
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=this_could_be_anything", true);
		assertEquals(VcfHeaderUtils.STANDARD_UUID_LINE + "=this_could_be_anything", header.getUUID().toString());
	}
	@Test
	public void parseDataSource() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=qsnp");
		
		assertEquals("qsnp", header.getSource().getMetaValue());
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=gatk");
		assertEquals(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=gatk", header.getRecords(VcfHeaderUtils.STANDARD_SOURCE_LINE).get(0).toString());
	}
	@Test
	public void parseDataInfo() {
		VcfHeader header = new VcfHeader();
		String infoLine = VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=" +  VcfHeaderUtils.INFO_DB + ",Number=1,Type=String,Description=\"anything will do\">";
		header.addOrReplace(infoLine, true);
		assertEquals(infoLine, header.getInfoRecord(VcfHeaderUtils.INFO_DB).toString());
		header.addOrReplace(infoLine, true);
		assertEquals(1, header.getInfoRecords().size());
		header.addOrReplace(infoLine, true);
		assertEquals(1, header.getInfoRecords().size());
		VcfHeaderRecord infoRec1 = header.getInfoRecord(VcfHeaderUtils.INFO_DB);
		
		header.addInfo(VcfHeaderUtils.INFO_DB, "2", "String", "anything will do");
		assertEquals(1, header.getInfoRecords().size());
		VcfHeaderRecord infoRec2 = header.getInfoRecord(VcfHeaderUtils.INFO_DB);
		
		//debug
		System.out.println("hasCode: " + infoRec1.hashCode() + " : " + infoRec2.hashCode());
		
		// these should not be equal - different numbers
		assertEquals(false, infoRec1.equals(infoRec2));
		
		header.addInfo(VcfHeaderUtils.INFO_DB, "1", "String", "anything will do");
		assertEquals(1, header.getInfoRecords().size());
		VcfHeaderRecord infoRec3 = header.getInfoRecord(VcfHeaderUtils.INFO_DB);
		
		assertEquals(true, infoRec1.equals(infoRec3));
	}
	@Test
	public void parseDataFormat() {
		VcfHeader header = new VcfHeader();
		String formatLine = VcfHeaderUtils.HEADER_LINE_FORMAT + "=<ID=" +  VcfHeaderUtils.FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"anything will do\">";
		header.addOrReplace(formatLine,true);
		assertEquals(formatLine, header.getFormatRecord(VcfHeaderUtils.FORMAT_ALLELE_COUNT).toString());
		header.addOrReplace(formatLine,true);
		assertEquals(1, header.getFormatRecords().size());
		assertEquals(0, header.getInfoRecords().size());
		assertEquals(0, header.getFilterRecords().size());
		header.addOrReplace(formatLine,true);
		assertEquals(1, header.getFormatRecords().size());
		VcfHeaderRecord formatRec1 = header.getFormatRecord(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "2", "String", "anything will do");
		assertEquals(1, header.getFormatRecords().size());
		VcfHeaderRecord formatRec2 = header.getFormatRecord(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		// these should not be equal - different numbers
		assertEquals(false, formatRec1.equals(formatRec2));
		
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "1", "String", "anything will do");
		assertEquals(1, header.getFormatRecords().size());
		VcfHeaderRecord formatRec3 = header.getFormatRecord(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		// 3 should be equal to 1
		assertEquals( true, formatRec1.equals(formatRec3) );
	}
	
	@Test
	public void parseDataFilter() {
		VcfHeader header = new VcfHeader();
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.addOrReplace( filterLine, true );
		assertEquals( filterLine, header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE).toString() );
		header.addOrReplace( filterLine, true );
		assertEquals( 0, header.getFormatRecords().size());
		assertEquals( 0, header.getInfoRecords().size());
		assertEquals( 1, header.getFilterRecords().size());
		header.addOrReplace( filterLine, true );
		assertEquals( 1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec1 = header.getFilterRecord( VcfHeaderUtils.FILTER_GERMLINE );
		
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE, "anything will do yoo hoo");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec2 = header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE);
		
		// these should not be equal - different numbers
		assertEquals(false, filterRec1.equals(filterRec2));
		
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE, "anything will do");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec3 = header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE);
		
		// 3 should be equal to 1		
		assertEquals(true, filterRec1.equals(filterRec3));
	}
	
	@Test
	public void parseDataQPG() {
		VcfHeader header = new VcfHeader();
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.addOrReplace(filterLine, true);
		assertEquals(filterLine, header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE).toString());
		header.addOrReplace(filterLine, true);
		assertEquals(0, header.getFormatRecords().size());
		assertEquals(0, header.getInfoRecords().size());
		assertEquals(1, header.getFilterRecords().size());
		header.addOrReplace(filterLine, true);
		assertEquals(1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec1 = header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE);
		
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE, "anything will do yoo hoo");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec2 = header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE);
		
		// these should not be equal - different numbers
		assertEquals(false, filterRec1.equals(filterRec2));
		
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE, "anything will do");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeaderRecord filterRec3 = header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE);
		
		// 3 should be equal to 1
		assertEquals(true, filterRec1.equals(filterRec3));
	}
	
	@Test
	public void doesEqualsWork() {
 		
		VcfHeader header = new VcfHeader();
		header.addInfo(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		header.addInfo(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		assertEquals(1, header.getInfoRecords().size());
		
		header.addFormat(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		VcfHeaderRecord infoRec = header.getInfoRecord(VcfHeaderUtils.INFO_FLANKING_SEQUENCE);
		VcfHeaderRecord formatRec = header.getFormatRecord(VcfHeaderUtils.INFO_FLANKING_SEQUENCE);
		assertEquals(false, infoRec.equals(formatRec));
	}
	
	 
	@Test //(expected=IllegalArgumentException.class)
	public void invalidHeaderLine() {
		VcfHeader h2 = new VcfHeader();
		try{			
			h2.addOrReplace("##hello");	
			fail("should throw exception for invalid vcf header line!");
		}catch(Exception e){}		
		
		try{
			h2.addOrReplace("hello=au");		
			fail("should throw exception for invalid vcf header line!");
		}catch(Exception e){}		
		
		
		//allow empty value
		h2.addOrReplace("##hello=");
		assertTrue(h2.getAllMetaRecords().size() == 1);
		assertTrue(  h2.getRecords("##hello").get(0).getMetaValue().equals( ""));
	}

	
	
	@Test
	public void testIteratorOrder(){
		VcfHeader h = new VcfHeader();
		Arrays.asList("##fileformat=VCFv4.2",
				"##qUUID=627d12bf-93c7-4e1f-b156-fc397def35a6",
				"##FORMAT=<ID=id1,Number=1,type=.,Description=ok>",				
				"##qSource=null-null",
				"##qINPUT=627d12bf-93c7-4e1f-b156-fc397def35a6:input.vcf",
				"##INFO=<ID=id1,Number=1,type=.,Description=ok>",
				"##qPG=<ID=1,Tool=NULL,Version=NULL,Date=2017-01-29 20:28:23,CL=\"testing run\">",
				"#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO",
				"##FORMAT=<ID=id,Number=1,type=.,Description=ok>",
				"##fileformat=VCFv4.2", "##fileDate=20170129").stream().forEach( h::addOrReplace );
	
		int i = 0;
		for(VcfHeaderRecord re : h){
			if(i == 0)
				assertTrue(re.getMetaKey().equals("##fileformat"));
			else if(i == 1)
				assertTrue(re.getMetaKey().equals("##fileDate"));
			else if(i == 6)
				assertTrue(re.getMetaKey().equals("##FORMAT") && re.getId().equals("id1"));
			else if(i == 7)
				assertTrue(re.getId().equals("id") ); 
			else if(i == 5)
				assertTrue(re.getMetaKey().equals("##INFO"));
			//	assertTrue(re.getMetaKey().equals("##FORMAT")); || re.getId().equals("id1")
			else if(i == 8)				
				assertTrue(re.getId().equals("1")); //qpg line
			else if(i == 9)
				assertTrue(re.getMetaKey().startsWith("#CHROM"));
			i++;
		}
	}
	
	@Test //(expected = IllegalArgumentException.class)  
	public void invalidRecord(){					
		try{
			new VcfHeaderRecord(null, "");
			fail("should throw exception for instance of VcfHeaderRecord with key = null!");
		}catch(IllegalArgumentException e){ }
		
		try{
			 new VcfHeaderRecord("", null);
			fail("should throw exception: missing # ");
		}catch(IllegalArgumentException e){ }
		 
		
		try{
			 new VcfHeaderRecord("##key");
			fail("should throw exception not follow: ##key=value patten");
		}catch(IllegalArgumentException e){ }
		
				 
		 VcfHeader header = new VcfHeader();
		 header.addOrReplace(new VcfHeaderRecord("##key", null));		
		 assertTrue(header.firstMatchedRecord("##key").toString().equals("##key=") );
		 
		 header.addOrReplace(new VcfHeaderRecord("##key= " ));	
		 for(VcfHeaderRecord re : header.getRecords("key") )			 
			 assertTrue(re.toString().equals("##key=") );
		 
				
	}
	
	
	@Test
	public void subFieldTest(){
		String line = " ##FORMAT = <  Number=1,ID=GD, Source = \" \", no= \"1\" > "; 
		VcfHeaderRecord record = new VcfHeaderRecord(line);
		
		assertTrue(record.toString().equals("##FORMAT=<ID=GD,Number=1,Source=\"\",no=\"1\">"));
		assertTrue(record.getSubFieldValue("source").equals("\"\""));
		
		line = " ##FORMAT = < Number=1, ID=GD,  Source = \" not sure \" ,  Type  = String,  Description = \" Genotype details: specific alleles = (A,G,T or C) \" , version=\"no.1\"  > ";
		record = new VcfHeaderRecord(line);
		assertTrue(record.getSubFieldValue("Description").equals("\"Genotype details: specific alleles = (A,G,T or C)\""));
		assertTrue(record.getId().equals(record.getSubFieldValue("id") ));
		assertTrue(record.toString().equals("##FORMAT=<ID=GD,Number=1,Source=\"not sure\",Type=String,Description=\"Genotype details: specific alleles = (A,G,T or C)\",version=\"no.1\">"));
		VcfHeader header = new VcfHeader();
		header.addOrReplace(record);
		line=line.replace("ID=GD", "ID=GD1");
		header.addOrReplace(line);
		assertTrue(header.getFormatRecord("GD").getSubFieldValue("description").equals( header.getFormatRecord("GD1").getSubFieldValue("description") ) );
		assertTrue(header.getFormatRecord("GD").getSubFieldValue("number").equals( header.getFormatRecord("GD1").getSubFieldValue("number") ) );
		assertTrue(header.getFormatRecord("GD").getSubFieldValue("source").equals( header.getFormatRecord("GD1").getSubFieldValue("source") ) );
		assertTrue(header.getFormatRecord("GD").getSubFieldValue("type").equals( header.getFormatRecord("GD1").getSubFieldValue("type") ) );
		assertTrue(record.getSubFieldValue("description").equals( header.getFormatRecord("GD1").getSubFieldValue("description") ) );
		assertTrue(record.getSubFieldValue("number").equals( header.getFormatRecord("GD1").getSubFieldValue("number") ) );
		assertTrue(record.getSubFieldValue("source").equals( header.getFormatRecord("GD1").getSubFieldValue("source") ) );
		assertTrue(record.getSubFieldValue("type").equals( header.getFormatRecord("GD1").getSubFieldValue("type") ) );
		
				
		line = "##FILTER = <ID=GD, Descriptin = \" grep \"hello \", url> "; 
		record = new VcfHeaderRecord(line);
		assertTrue( record.toString().equals("##FILTER=<ID=GD,Descriptin=\"grep \"hello\",url>")  );
		
		line = "##FILTER = <ID=GD, Descriptin = \" grep \" hello=1,2 \" \", =url> "; 
		record = new VcfHeaderRecord(line);
		assertTrue( record.toString().equals("##FILTER=<ID=GD,Descriptin=\"grep \" hello=1,2 \"\",=url>")  );		
		
		
		//parser get confused by mutli double quot and common join together
		line = "##TEST = <ID=GD, Descriptin = \" grep \" hello=1,2 \", \", =url> "; 
		record = new VcfHeaderRecord(line);
 		assertTrue( record.toString().equals("##TEST=<ID=GD,Descriptin=\"grep \" hello=1,2\",\",=url>")  );		
		
	}
	
	
	@Test
	public void xuTest(){
		VcfHeaderRecord re = new VcfHeaderRecord("##test=< order = 100,  ID=1,description=\"not sure\">");	
		System.out.println(re.getId() + re.toString()); 		
		VcfHeaderRecord re1 = new VcfHeaderRecord("##test=< order = 100,  ID=1,description=\"not sure\">");		
		System.out.println(re1.getId() + re1.toString()); 
		
		System.out.println( re.hashCode() + " : " + re1.hashCode() );
		
		System.out.println(  re1.getSubFieldValue("order") );
		
		assertTrue(re.equals(re1));
		
	}
	
}
