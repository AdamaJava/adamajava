package au.edu.qimr.vcftools.util;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.FormattedRecord;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class MergeUtilsTest {
	
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void canHeadersBeMerged() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		assertEquals(false, MergeUtils.canMergeBePerformed(qsnpHeader, gatkHeader));
		assertEquals(true, MergeUtils.canMergeBePerformed(qsnpHeader, qsnpHeader));
		assertEquals(true, MergeUtils.canMergeBePerformed(gatkHeader, gatkHeader));
		
		VcfHeader h1 = new VcfHeader();
		VcfHeader h2 = new VcfHeader();
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456");
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456");
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456", true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "23456", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789", true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789", true);
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD", true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCDE", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD\tXZY", true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD\tXZY", true);
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "12345\tXZY", true);
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "XZY\t12345", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
	}
	
	@Test
	public void mergeHeadersQLines() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		List<Record>qsnpOtherRecords = qsnpHeader.getNonStandardRecords();
		List<Record>gatkOtherRecords = gatkHeader.getNonStandardRecords();
		List<String> mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(qsnpOtherRecords, gatkOtherRecords);
		
		assertEquals(qsnpOtherRecords.size() + gatkOtherRecords.size(), mergedOtherRecords.size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = qsnpOtherRecords.stream()
			.map(r -> r.getData().replaceAll("##", "##1:"))
			.collect(Collectors.toList());
		List<String> gRecs = gatkOtherRecords.stream()
				.map(r -> r.getData().replaceAll("##", "##2:"))
				.collect(Collectors.toList());
		
		assertEquals(true, mergedOtherRecords.containsAll(qRecs));
		assertEquals(true, mergedOtherRecords.containsAll(gRecs));
	}
	
	@Test
	public void mergeHeadersQLinesAgain() {
		VcfHeader h1 = new VcfHeader();
		VcfHeader h2 = new VcfHeader();
		List<Record>h1Recs = h1.getNonStandardRecords();
		List<Record>h2Recs = h2.getNonStandardRecords();
		
		List<String> mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h2Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		
		h1.parseHeaderLine(VcfHeaderUtils.BLANK_HEADER_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h2Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h2Recs);
		assertEquals(1, mergedOtherRecords.size());
		
		h2.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE, true);
		h2Recs = h2.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h2Recs);
		assertEquals(2, mergedOtherRecords.size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = h1Recs.stream()
				.map(r -> r.getData().replaceAll("##", "##1:"))
				.collect(Collectors.toList());
		List<String> gRecs = h2Recs.stream()
				.map(r -> r.getData().replaceAll("##", "##2:"))
				.collect(Collectors.toList());
		
		assertEquals(true, mergedOtherRecords.containsAll(qRecs));
		assertEquals(true, mergedOtherRecords.containsAll(gRecs));
	}
	
	@Test
	public void mergeHeadersQLines1File() {
		VcfHeader h1 = new VcfHeader();
		List<Record>h1Recs = h1.getNonStandardRecords();
		
		List<String> mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		
		h1.parseHeaderLine(VcfHeaderUtils.BLANK_HEADER_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs);
		assertEquals(1, mergedOtherRecords.size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = h1Recs.stream()
				.map(r -> r.getData().replaceAll("##", "##1:"))
				.collect(Collectors.toList());
		assertEquals(true, mergedOtherRecords.containsAll(qRecs));
	}
	
	@Test
	public void mergeHeadersQLines3File() {
		VcfHeader h1 = new VcfHeader();
		List<Record>h1Recs = h1.getNonStandardRecords();
		
		List<String> mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h1Recs, h1Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		
		h1.parseHeaderLine(VcfHeaderUtils.BLANK_HEADER_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h1Recs, h1Recs);
		assertEquals(0, mergedOtherRecords.size());
		
		h1.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE, true);
		h1Recs = h1.getNonStandardRecords();
		mergedOtherRecords = MergeUtils.mergeOtherHeaderRecords(h1Recs, h1Recs, h1Recs);
		assertEquals(3, mergedOtherRecords.size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		assertEquals(true, mergedOtherRecords.containsAll( h1Recs.stream().map(r -> r.getData().replaceAll("##", "##1:")).collect(Collectors.toList())));
		assertEquals(true, mergedOtherRecords.containsAll( h1Recs.stream().map(r -> r.getData().replaceAll("##", "##2:")).collect(Collectors.toList())));
		assertEquals(true, mergedOtherRecords.containsAll( h1Recs.stream().map(r -> r.getData().replaceAll("##", "##3:")).collect(Collectors.toList())));
	}
	
	@Test
	public void mergeFormattedRecordLines() {
		VcfHeader h1 = new VcfHeader();
		Map<String, FormattedRecord> h1Info = h1.getInfoRecords();
		
		List<FormattedRecord> mergedRecs = MergeUtils.mergeHeaderRecords(h1Info);
		assertEquals(0, mergedRecs.size());
		
		h1.parseHeaderLine(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=ABC,Number=.,Type=String,Description=\"My info field\">");
		h1Info = h1.getInfoRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(h1Info);
		assertEquals(1, mergedRecs.size());
		mergedRecs = MergeUtils.mergeHeaderRecords(h1Info, h1Info);
		assertEquals(1, mergedRecs.size());
		
		VcfHeader h2 = new VcfHeader();
		h2.parseHeaderLine(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=ABC,Number=.,Type=String,Description=\"My info field UPDATED\">");
		Map<String, FormattedRecord> h2Info = h2.getInfoRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(h1Info, h2Info);
		assertEquals(2, mergedRecs.size());
		
		h2.parseHeaderLine(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=DEF,Number=.,Type=String,Description=\"My second info field\">");
		h2Info = h2.getInfoRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(h1Info, h2Info);
		assertEquals(3, mergedRecs.size());
	}
	
	@Test
	public void mergeHeadersFromRealVcf() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		Map<String, FormattedRecord> qsnpH = qsnpHeader.getInfoRecords();
		Map<String, FormattedRecord> gatkH = gatkHeader.getInfoRecords();
		List<FormattedRecord> mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(20, mergedRecs.size());
		
		qsnpH = qsnpHeader.getFilterRecords();
		gatkH = gatkHeader.getFilterRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(15, mergedRecs.size());
		
		qsnpH = qsnpHeader.getFormatRecords();
		gatkH = gatkHeader.getFormatRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(10, mergedRecs.size());
	}
	
	@Test
	public void mergeHeadersFromSoonToBeRealVcf() {
		VcfHeader qsnpHeader = new VcfHeader(getUpdatedQsnpVCfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		Map<String, FormattedRecord> qsnpH = qsnpHeader.getInfoRecords();
		Map<String, FormattedRecord> gatkH = gatkHeader.getInfoRecords();
		List<FormattedRecord> mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(20, mergedRecs.size());
		
		qsnpH = qsnpHeader.getFilterRecords();
		gatkH = gatkHeader.getFilterRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(15, mergedRecs.size());
		
		qsnpH = qsnpHeader.getFormatRecords();
		gatkH = gatkHeader.getFormatRecords();
		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
		assertEquals(10, mergedRecs.size());
	}
	
	@Test
	public void mergeRecordInfo() {
		VcfRecord r1 = new VcfRecord( "1", 100, null, "ABC", "DEF");
		VcfRecord r2 = new VcfRecord( "1", 100, null, "ABC", "DEF");
		VcfRecord mergedR = new VcfRecord("1", 100, null, "ABC", "DEF");
		VcfRecord actualMergerRecord = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals(mergedR, actualMergerRecord);
		
		r1.setInfo("Hello");
		r2.setInfo("Hello");
		assertEquals("Hello", MergeUtils.mergeRecords(null, r1, r2).getInfo());
		
		r1.setInfo("Hello");
		r2.setInfo("There");
		assertEquals("Hello;There", MergeUtils.mergeRecords(null, r1, r2).getInfo());
		
		r1.setInfo("Hello;There");
		r2.setInfo("There");
		assertEquals("Hello;There", MergeUtils.mergeRecords(null, r1, r2).getInfo());
		
		r1.setInfo("Hello;There");
		r2.setInfo("Hello;There");
		assertEquals("Hello;There", MergeUtils.mergeRecords(null, r1, r2).getInfo());
		
		r1.setInfo("Hello;There");
		r2.setInfo("Hello;World;There");
		assertEquals(true, Stream.of("Hello","There","World").allMatch(s -> MergeUtils.mergeRecords(null, r1, r2).getInfo().contains(s)));
		
		r1.setInfo("ID=123");
		r2.setInfo("ID=123");
		assertEquals("ID=123", MergeUtils.mergeRecords(null, r1, r2).getInfo());
		
		r1.setInfo("ID=123");
		r2.setInfo("ID=234");
		assertEquals(true, Stream.of("ID=123,234").allMatch(s -> MergeUtils.mergeRecords(null, r1, r2).getInfo().contains(s)));
	}
	
	@Test
	public void mergeRecordIdOnly() {
		VcfRecord r1 = VcfUtils.createVcfRecord("1", 0);			 
		VcfRecord r2 = VcfUtils.createVcfRecord("1", 0);				 
		VcfRecord mergedR = VcfUtils.createVcfRecord("1", 0);			 
				
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( "1", 100, null, ".", null);
		r2 = new VcfRecord( "1", 100, null, ".", null);
		mergedR = new VcfRecord( "1", 100, null, ".",  null);
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( "1", 100, null, "ABC", null);
		r2 = new VcfRecord( "1", 100, null, "ABC", null);
		mergedR = new VcfRecord("1", 100, null, "ABC", null);
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( new String[] {"1", "100", null, "ABC", "DEF"});
		r2 = new VcfRecord( new String[] {"1", "100", null, "ABC", "DEF"});
		mergedR = new VcfRecord( new String[] {"1", "100", null, "ABC", "DEF"});
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		r2 = new VcfRecord( new String[] {"1", "100", null, "ABC", "DEF"});
		mergedR = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( new String[] {"1", "100", null, "ABC", "DEF"});
		r2 = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		mergedR = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		r2 = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		mergedR = new VcfRecord( new String[] {"1", "100", "rs123", "ABC", "DEF"});
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord( "1", 100, "rs123", "ABC", "DEF");
		r2 = new VcfRecord( "1", 100, "rs456", "ABC", "DEF");
		mergedR = new VcfRecord("1", 100, "rs123,rs456", "ABC", "DEF");
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
	}
	
	@Test
	public void mergeRecordInfoOnlyWithRules() {
		Map<Integer, Map<String, String>> idRules = new HashMap<>();
		Map<String, String> rulesForThisFile = new HashMap<>();
		idRules.put(1,  rulesForThisFile);
		rulesForThisFile.put("ID", "ID1");
		
		VcfRecord r1 = new VcfRecord("1", 100, "rs123", "ABC", "DEF");
		VcfRecord r2 = new VcfRecord("1", 100, "rs456", "ABC", "DEF");
		VcfRecord mergedR = new VcfRecord("1", 100, "rs123,rs456", "ABC", "DEF");
		assertEquals(mergedR, MergeUtils.mergeRecords(idRules, r1, r2));
		
		r1.setInfo("ID=XXX");
		r2.setInfo("ID=YYY");
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("ID=XXX;ID1=YYY", mergedR.getInfo());
		
		r1.setInfo("ID=XXX");
		r2.setInfo("ID=XXX");
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("ID=XXX;ID1=XXX", mergedR.getInfo());
		
		r1.setInfo("ID=I dont know");
		r2.setInfo("ID=XXX");
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("ID=I dont know;ID1=XXX", mergedR.getInfo());
		
		r1.setInfo("XY=Z");
		r2.setInfo("XY=ABC");
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("XY=Z,ABC", mergedR.getInfo());
	}
	
	@Test
	public void mergeRecordFilter() {
		
		VcfRecord r1 = new VcfRecord("1", 100, "rs123", "ABC", "DEF");
		VcfRecord r2 = new VcfRecord("1", 100, "rs456", "ABC", "DEF");
		VcfRecord mergedR = new VcfRecord("1", 100, "rs123,rs456", "ABC", "DEF");
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1.setFilter("F1");
		r2.setFilter("F1");
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals("F1", mergedR.getFilter());
		
		r1.setFilter("F1");
		r2.setFilter("F2");
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals("F1;F2", mergedR.getFilter());
	}
	
	// TODO should we do anything special when dealing with FILTER? PASS value for example? 
	
//	@Test
//	public void recordsEligibleForMergingCPOnly() {
//		VcfRecord r1 = new VcfRecord("1", 0);
//		VcfRecord r2 = new VcfRecord("1", 0);
//		assertEquals(true, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("1", 0);
//		r2 = new VcfRecord("2", 0);
//		assertEquals(false, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("2", 1);
//		r2 = new VcfRecord("2", 0);
//		assertEquals(false, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("2", 1);
//		r2 = new VcfRecord("2", 1);
//		assertEquals(true, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//	}
//	@Test
//	public void recordsEligibleForMerging() {
//		VcfRecord r1 = new VcfRecord("1", 0);
//		VcfRecord r2 = new VcfRecord("1", 0);
//		assertEquals(true, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("1", 0);
//		r2 = new VcfRecord("2", 0);
//		assertEquals(false, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("2", 1);
//		r2 = new VcfRecord("2", 0);
//		assertEquals(false, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//		
//		r1 = new VcfRecord("2", 1);
//		r2 = new VcfRecord("2", 1);
//		assertEquals(true, MergeUtils.areRecordsEligibleForMerge(r1, r2));
//	}
	

	public List<String> getQsnpGATKVcfHeader() {
		
		return Arrays.asList("##fileformat=VCFv4.2",
"##fileDate=20151211",
"##",
"##qUUID=fe8cd25c-2ef2-45c3-aec3-d7b6f08c73c9",
"##qSource=qSNP v2.0 (882)",
"##qDonorId=OESO-0138",
"##qControlSample=http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa",
"##qTestSample=http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb",
"##qControlBam=/mnt/genomeinfo_projects/sample/f/1/f124a2ca-5e24-419a-96f5-dd849ccc50aa/aligned_read_group_set/6f5cf6e7-3dbd-4e73-b675-575a0d5d5c04.bam",
"##qControlBamUUID=null",
"##qTestBam=/mnt/genomeinfo_projects/sample/6/4/64d9c65d-d0af-43e7-a835-8fe3c36b93bb/aligned_read_group_set/9553985e-db17-4960-bcdb-e8c53eab4aa4.bam",
"##qTestBamUUID=null",
"##qAnalysisId=80bd9224-abf8-4265-858e-007b67bb2c42",
"##qControlVcf=/mnt/genomeinfo_projects/analysis/e/3/e3cc52e9-9817-4ebc-a279-06ae22cf44d6/e3cc52e9-9817-4ebc-a279-06ae22cf44d6.vcf",
"##qControlVcfUUID=null",
"##qControlVcfGATKVersion=3.3-0-g37228af",
"##qTestVcf=/mnt/genomeinfo_projects/analysis/3/f/3ff8046e-8465-4a43-9da5-38cc6ab726cd/3ff8046e-8465-4a43-9da5-38cc6ab726cd.vcf",
"##qTestVcfUUID=null",
"##qTestVcfGATKVersion=3.3-0-g37228af",
"##qPG=<ID=1,Tool=qsnp,Version=2.0 (882),Date=2015-12-11 03:16:18,CL=\"qsnp -i /mnt/genomeinfo_projects/analysis/8/0/80bd9224-abf8-4265-858e-007b67bb2c42/80bd9224-abf8-4265-858e-007b67bb2c42.ini -log /mnt/genomeinfo_projects/analysis/8/0/80bd9224-abf8-4265-858e-007b67bb2c42/80bd9224-abf8-4265-858e-007b67bb2c42.log [runMode: vcf]\">",
"##",
"##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">",
"##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">",
"##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">",
"##INFO=<ID=FLANK,Number=1,Type=String,Description=\"Flanking sequence either side of variant\">",
"##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">",
"##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">",
"##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">",
"##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">",
"##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">",
"##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">",
"##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">",
"##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">",
"##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">",
"##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">",
"##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">",
"##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">",
"##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">",
"##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">",
"##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">",
"##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\">",
"##FILTER=<ID=COVN8,Description=\"Less than 8 reads coverage in normal\">",
"##FILTER=<ID=NCIT,Description=\"No call in test\">",
"##FILTER=<ID=MR,Description=\"Less than 5 mutant reads\">",
"##FILTER=<ID=SAT3,Description=\"Less than 3 reads of same allele in tumour\">",
"##FILTER=<ID=MIUN,Description=\"Mutation also found in pileup of (unfiltered) normal\">",
"##FILTER=<ID=NNS,Description=\"Less than 4 novel starts not considering read pair\">",
"##FILTER=<ID=COVN12,Description=\"Less than 12 reads coverage in normal\">",
"##FILTER=<ID=MIN,Description=\"Mutation also found in pileup of normal\">",
"##FILTER=<ID=SBIASALT,Description=\"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)\">",
"##FILTER=<ID=LowQual,Description=\"Low quality\">",
"##FILTER=<ID=COVT,Description=\"Less than 8 reads coverage in tumour\">",
"##FILTER=<ID=SAN3,Description=\"Less than 3 reads of same allele in normal\">",
"##FILTER=<ID=SBIASCOV,Description=\"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)\">",
"##FILTER=<ID=GERM,Description=\"Mutation is a germline variant in another patient\">",
"##FILTER=<ID=MER,Description=\"Mutation equals reference\">",
"##FORMAT=<ID=AC,Number=1,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">",
"##FORMAT=<ID=NNS,Number=1,Type=String,Description=\"Number of novel starts not considering read pair\">",
"##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">",
"##FORMAT=<ID=MR,Number=1,Type=String,Description=\"Number of mutant/variant reads\">",
"##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">",
"##FORMAT=<ID=ACCS,Number=1,Type=String,Description=\"Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)\">",
"##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">",
"##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">",
"##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">",
"##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">",
"#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
		
	}
	
	public List<String> getQsnpVcfHeader() {
		return Arrays.asList("##fileformat=VCFv4.2",
"##fileDate=20151209",
"##",
"##qUUID=48867781-d540-467c-9cd9-45049488b90b",
"##qSource=qSNP v2.0 (882)",
"##qDonorId=OESO-0132",
"##qControlSample=http://purl.org/net/grafli/collectedsample#e734bdbc-2e43-44e4-ad32-11719865f9d6",
"##qTestSample=http://purl.org/net/grafli/collectedsample#49c59f9e-fe9d-4813-a4c6-3198ec003859",
"##qControlBam=/mnt/genomeinfo_projects/sample/e/7/e734bdbc-2e43-44e4-ad32-11719865f9d6/aligned_read_group_set/46e77aa7-8e01-41dd-87ed-0c910e9268c5.bam",
"##qControlBamUUID=null",
"##qTestBam=/mnt/genomeinfo_projects/sample/4/9/49c59f9e-fe9d-4813-a4c6-3198ec003859/aligned_read_group_set/45fa3ea6-70ae-4743-b432-2dc9fe59e7c1.bam",
"##qTestBamUUID=null",
"##qAnalysisId=cebce2b6-dc2d-4be6-9bcb-ae097ddc221f",
"##qPG=<ID=1,Tool=qsnp,Version=2.0 (882),Date=2015-12-09 11:36:25,CL=\"qsnp -i /mnt/genomeinfo_projects/analysis/c/e/cebce2b6-dc2d-4be6-9bcb-ae097ddc221f/cebce2b6-dc2d-4be6-9bcb-ae097ddc221f.ini -log /mnt/genomeinfo_projects/analysis/c/e/cebce2b6-dc2d-4be6-9bcb-ae097ddc221f/cebce2b6-dc2d-4be6-9bcb-ae097ddc221f.log [runMode: standard]\">",
"##",
"##INFO=<ID=FLANK,Number=1,Type=String,Description=\"Flanking sequence either side of variant\">",
"##FILTER=<ID=COVN8,Description=\"Less than 8 reads coverage in normal\">",
"##FILTER=<ID=NCIT,Description=\"No call in test\">",
"##FILTER=<ID=MR,Description=\"Less than 5 mutant reads\">",
"##FILTER=<ID=SAT3,Description=\"Less than 3 reads of same allele in tumour\">",
"##FILTER=<ID=MIUN,Description=\"Mutation also found in pileup of (unfiltered) normal\">",
"##FILTER=<ID=NNS,Description=\"Less than 4 novel starts not considering read pair\">",
"##FILTER=<ID=COVN12,Description=\"Less than 12 reads coverage in normal\">",
"##FILTER=<ID=MIN,Description=\"Mutation also found in pileup of normal\">",
"##FILTER=<ID=SBIASALT,Description=\"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)\">",
"##FILTER=<ID=COVT,Description=\"Less than 8 reads coverage in tumour\">",
"##FILTER=<ID=SAN3,Description=\"Less than 3 reads of same allele in normal\">",
"##FILTER=<ID=SBIASCOV,Description=\"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)\">",
"##FILTER=<ID=GERM,Description=\"Mutation is a germline variant in another patient\">",
"##FILTER=<ID=MER,Description=\"Mutation equals reference\">",
"##FORMAT=<ID=AC,Number=1,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">",
"##FORMAT=<ID=NNS,Number=1,Type=String,Description=\"Number of novel starts not considering read pair\">",
"##FORMAT=<ID=AD,Number=1,Type=String,Description=\"Allelic depths for the ref and alt alleles in the order listed\">",
"##FORMAT=<ID=MR,Number=1,Type=String,Description=\"Number of mutant/variant reads\">",
"##FORMAT=<ID=GQ,Number=1,Type=String,Description=\"Genotype Quality\">",
"##FORMAT=<ID=ACCS,Number=1,Type=String,Description=\"Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)\">",
"##FORMAT=<ID=DP,Number=1,Type=String,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">",
"##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype: 0/0 homozygous reference; 0/1 heterozygous for alternate allele; 1/1 homozygous for alternate allele\">",
"##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">",
"#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#e734bdbc-2e43-44e4-ad32-11719865f9d6	http://purl.org/net/grafli/collectedsample#49c59f9e-fe9d-4813-a4c6-3198ec003859");
	}
	
	public List<String> getUpdatedQsnpVCfHeader() {
		String controlId = "ABC_123";
		String testId = "DEF_456";
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		
		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=qSNP v2.0 (882)");		
		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_DONOR_ID + "=");
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=" + controlId);		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=" + testId);		
		
		header.parseHeaderLine( "##qAnalysisId=12345");
		
		header.addInfoLine(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1", "String","Flanking sequence either side of variant");																	
		
		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12, "Less than 12 reads coverage in normal");
		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8,"Less than 8 reads coverage in normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR,"Less than 8 reads coverage in tumour"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL,"Less than 3 reads of same allele in normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR,"Less than 3 reads of same allele in tumour");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL,"Mutation also found in pileup of normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL,"Mutation also found in pileup of (unfiltered) normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE,"Mutation is a germline variant in another patient");  
		header.addFilterLine(VcfHeaderUtils.FILTER_NOVEL_STARTS,"Less than 4 novel starts not considering read pair");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTANT_READS,"Less than 5 mutant reads"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF,"Mutation equals reference"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST,"No call in test"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_STRAND_BIAS_ALT,"Alternate allele on only one strand (or percentage alternate allele on other strand is less than " + 5 + "%)"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_STRAND_BIAS_COV,"Sequence coverage on only one strand (or percentage coverage on other strand is less than " + 5 + "%)"); 
		
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE, "1", "String" ,"Genotype");
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, "1", "String","Genotype details: specific alleles (A,G,T or C)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "1", "String","Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP, "1", "String","Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, ".", "Integer","Allelic depths for the ref and alt alleles in the order listed");
		header.addFormatLine(VcfHeaderUtils.FORMAT_READ_DEPTH, "1", "Integer","Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY, "1", "Integer","Genotype Quality");
		header.addFormatLine(VcfHeaderUtils.FORMAT_MUTANT_READS,  "1", "Integer","Number of mutant/variant reads");
		header.addFormatLine(VcfHeaderUtils.FORMAT_NOVEL_STARTS, "1", "Integer","Number of novel starts not considering read pair");		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + (controlId != null ? controlId + "\t" : "") + testId);
		
		List<String> list = new ArrayList<>();
		Iterator<Record> iter = header.iterator();
		
		while(iter.hasNext()) {
			list.add(iter.next().toString());
		}
		return list;
	}
}
