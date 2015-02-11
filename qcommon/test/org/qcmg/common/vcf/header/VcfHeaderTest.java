package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class VcfHeaderTest {

	@Test
	public void sampleIdTest() throws Exception{
		final VcfHeader header = new VcfHeader();
		header.parseHeaderLine("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Control	Test");
		
		final String[] sample = header.getSampleId();
		assertTrue(sample[0].equals("Control"));
		assertTrue(sample[1].equals("Test"));
	}
	
	@Test
	public void examineOrderOfRecordsReturned() {
		VcfHeader header = new VcfHeader();
		header.addFormatLine(VcfHeaderUtils.FORMAT_MUTANT_READS, "1", "String", "teenage mutant ninja turtles");
		header.addFilterLine(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST, "NCIT");
		header.addInfoLine(VcfHeaderUtils.INFO_DONOR , "1", "String",  "donor details here");
		VcfHeaderUtils.addQPGLineToHeader(header, "junit", "4.10", "something like this");
		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);
		
		VcfHeader.Record qpgRec = header.getqPGLines().get(0);
		VcfHeader.Record formatRec = header.getFormatRecords().get(VcfHeaderUtils.FORMAT_MUTANT_READS);
		VcfHeader.Record filterRec = header.getFilterRecords().get(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST);
		VcfHeader.Record infoRec = header.getInfoRecords().get(VcfHeaderUtils.INFO_DONOR);
		
		assertNotNull(qpgRec);
		assertNotNull(formatRec);
		assertNotNull(filterRec);
		assertNotNull(infoRec);
		
		int i = 0 ;
		for (VcfHeader.Record rec : header) {
			i++;
			if (i == 1) {
				assertEquals(VcfHeaderUtils.CURRENT_FILE_VERSION, rec.getData());
			}
			if (i == 2) {
				assertEquals(qpgRec, rec);
			}
			if (i == 3) {
				assertEquals(infoRec, rec);
			}
			if (i == 4) {
				assertEquals(filterRec, rec);
			}
			if (i == 5) {
				assertEquals(formatRec, rec);
			}
		}
		
	}
	
	@Test
	public void doWeHaveQIMRDataInHeader() {
		VcfHeader header = new VcfHeader();
		assertEquals(false,  header.containsQIMRDetails());
		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);
		assertEquals(false,  header.containsQIMRDetails());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=today");
		assertEquals(false,  header.containsQIMRDetails());
		String infoLine = VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=" +  VcfHeaderUtils.INFO_DB + ",Number=1,Type=String,Description=\"anything will do\">";
		header.parseHeaderLine(infoLine);
		assertEquals(false,  header.containsQIMRDetails());
		String formatLine = VcfHeaderUtils.HEADER_LINE_FORMAT + "=<ID=" +  VcfHeaderUtils.FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"anything will do\">";
		header.parseHeaderLine(formatLine);
		assertEquals(false,  header.containsQIMRDetails());
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.parseHeaderLine(filterLine);
		assertEquals(false,  header.containsQIMRDetails());
		
		// lets add some qcmg data
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_!23_123");
		assertEquals(false,  header.containsQIMRDetails());
		// need some qpg lines too
		header.addQPGLine(111 , "test", "1.1", "who knows", "right now - yesterday even");
		assertEquals(true,  header.containsQIMRDetails());
	}
	
	@Test
	public void replace() {
		VcfHeader header = new VcfHeader();
		String inputUuid = "123_123_123_!23";
		String inputVcfName = "testVcf.vcf";
		header.replace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		assertEquals(1, header.getMetaRecords().size());
		assertEquals(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName, header.getMetaRecords().get(0).getData());
		String inputUuid2 = "456_456_456_456";
		header.replace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid2 + ":"+ inputVcfName);
		assertEquals(1, header.getMetaRecords().size());
		assertEquals(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid2 + ":"+ inputVcfName, header.getMetaRecords().get(0).getData());
		header.replace(VcfHeaderUtils.STANDARD_INPUT_LINE + "123=" + inputUuid2 + ":"+ inputVcfName);
		assertEquals(2, header.getMetaRecords().size());
	}
	
	@Test
	public void parseDataNullAndEmpty() {
		VcfHeader header = new VcfHeader();
		try {
			header.parseHeaderLine(null);
			Assert.fail("sHould have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			header.parseHeaderLine("");
			Assert.fail("sHould have thrown an exception");
		} catch (IllegalArgumentException iae) {}
	}
	@Test
	public void parseDataFileFormat() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_VERSION + "=1.1");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_VERSION + "=1.1", header.getFileVersion().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_VERSION + "=1.2");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_VERSION + "=1.2", header.getFileVersion().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_VERSION + "=XYZ");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_VERSION + "=XYZ", header.getFileVersion().getData());
	}
	@Test
	public void parseDataFileDate() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=today");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_DATE + "=today", header.getFileDate().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=yesterday");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_DATE + "=yesterday", header.getFileDate().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=tomorrow");
		assertEquals(VcfHeaderUtils.STANDARD_FILE_DATE + "=tomorrow", header.getFileDate().getData());
	}
	@Test
	public void parseDataUUID() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_123_123");
		assertEquals(VcfHeaderUtils.STANDARD_UUID_LINE + "=123_123_123_123", header.getUUID().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=this_could_be_anything");
		assertEquals(VcfHeaderUtils.STANDARD_UUID_LINE + "=this_could_be_anything", header.getUUID().getData());
	}
	@Test
	public void parseDataSource() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=qsnp");
		assertEquals(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=qsnp", header.getSource().getData());
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=gatk");
		assertEquals(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=gatk", header.getSource().getData());
	}
	@Test
	public void parseDataInfo() {
		VcfHeader header = new VcfHeader();
		String infoLine = VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=" +  VcfHeaderUtils.INFO_DB + ",Number=1,Type=String,Description=\"anything will do\">";
		header.parseHeaderLine(infoLine);
		assertEquals(infoLine, header.getInfoRecords().get(VcfHeaderUtils.INFO_DB).getData());
		header.parseHeaderLine(infoLine);
		assertEquals(1, header.getInfoRecords().size());
		header.parseHeaderLine(infoLine);
		assertEquals(1, header.getInfoRecords().size());
		VcfHeader.Record infoRec1 = header.getInfoRecords().get(VcfHeaderUtils.INFO_DB);
		
		header.addInfoLine(VcfHeaderUtils.INFO_DB, "2", "String", "anything will do");
		assertEquals(1, header.getInfoRecords().size());
		VcfHeader.Record infoRec2 = header.getInfoRecords().get(VcfHeaderUtils.INFO_DB);
		
		// these should not be equal - different numbers
		assertEquals(false, infoRec1.equals(infoRec2));
		
		header.addInfoLine(VcfHeaderUtils.INFO_DB, "1", "String", "anything will do");
		assertEquals(1, header.getInfoRecords().size());
		VcfHeader.Record infoRec3 = header.getInfoRecords().get(VcfHeaderUtils.INFO_DB);
		
		// 3 should be equal to 1
		assertEquals(true, infoRec1.equals(infoRec3));
	}
	@Test
	public void parseDataFormat() {
		VcfHeader header = new VcfHeader();
		String formatLine = VcfHeaderUtils.HEADER_LINE_FORMAT + "=<ID=" +  VcfHeaderUtils.FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"anything will do\">";
		header.parseHeaderLine(formatLine);
		assertEquals(formatLine, header.getFormatRecords().get(VcfHeaderUtils.FORMAT_ALLELE_COUNT).getData());
		header.parseHeaderLine(formatLine);
		assertEquals(1, header.getFormatRecords().size());
		assertEquals(0, header.getInfoRecords().size());
		assertEquals(0, header.getFilterRecords().size());
		header.parseHeaderLine(formatLine);
		assertEquals(1, header.getFormatRecords().size());
		VcfHeader.Record formatRec1 = header.getFormatRecords().get(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "2", "String", "anything will do");
		assertEquals(1, header.getFormatRecords().size());
		VcfHeader.Record formatRec2 = header.getFormatRecords().get(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		// these should not be equal - different numbers
		assertEquals(false, formatRec1.equals(formatRec2));
		
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "1", "String", "anything will do");
		assertEquals(1, header.getFormatRecords().size());
		VcfHeader.Record formatRec3 = header.getFormatRecords().get(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		// 3 should be equal to 1
		assertEquals(true, formatRec1.equals(formatRec3));
	}
	@Test
	public void parseDataFilter() {
		VcfHeader header = new VcfHeader();
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.parseHeaderLine(filterLine);
		assertEquals(filterLine, header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE).getData());
		header.parseHeaderLine(filterLine);
		assertEquals(0, header.getFormatRecords().size());
		assertEquals(0, header.getInfoRecords().size());
		assertEquals(1, header.getFilterRecords().size());
		header.parseHeaderLine(filterLine);
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec1 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE, "anything will do yoo hoo");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec2 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		// these should not be equal - different numbers
		assertEquals(false, filterRec1.equals(filterRec2));
		
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE, "anything will do");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec3 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		// 3 should be equal to 1
		assertEquals(true, filterRec1.equals(filterRec3));
	}
	@Test
	public void parseDataQPG() {
		VcfHeader header = new VcfHeader();
		String filterLine = VcfHeaderUtils.HEADER_LINE_FILTER + "=<ID=" +  VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"anything will do\">";
		header.parseHeaderLine(filterLine);
		assertEquals(filterLine, header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE).getData());
		header.parseHeaderLine(filterLine);
		assertEquals(0, header.getFormatRecords().size());
		assertEquals(0, header.getInfoRecords().size());
		assertEquals(1, header.getFilterRecords().size());
		header.parseHeaderLine(filterLine);
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec1 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE, "anything will do yoo hoo");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec2 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		// these should not be equal - different numbers
		assertEquals(false, filterRec1.equals(filterRec2));
		
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE, "anything will do");
		assertEquals(1, header.getFilterRecords().size());
		VcfHeader.Record filterRec3 = header.getFilterRecords().get(VcfHeaderUtils.FILTER_GERMLINE);
		
		// 3 should be equal to 1
		assertEquals(true, filterRec1.equals(filterRec3));
	}
	
	@Test
	public void doesEqualsWork() {
		VcfHeader header = new VcfHeader();
		header.addInfoLine(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		header.addInfoLine(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		assertEquals(1, header.getInfoRecords().size());
		
		header.addFormatLine(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1",  "blah", VcfHeaderUtils.INFO_FS);
		VcfHeader.Record infoRec = header.getInfoRecords().get(VcfHeaderUtils.INFO_FLANKING_SEQUENCE);
		VcfHeader.Record formatRec = header.getFormatRecords().get(VcfHeaderUtils.INFO_FLANKING_SEQUENCE);
		assertEquals(false, infoRec.equals(formatRec));
	}
}
