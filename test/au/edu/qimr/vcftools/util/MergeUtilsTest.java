package au.edu.qimr.vcftools.util;

import static org.junit.Assert.*;

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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.*;

import au.edu.qimr.vcftools.Rule;

public class MergeUtilsTest {
	
	
	public static String controlId = "http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa"; //"ABC_123";
	public static String testId = "http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb";   //"DEF_456";
	
	@Test
	public void canHeadersBeMerged() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());		
		assertEquals(true, MergeUtils.canMergeBePerformed(qsnpHeader, qsnpHeader));
		assertEquals(true, MergeUtils.canMergeBePerformed(gatkHeader, gatkHeader));
		
		qsnpHeader.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "\tcontrol\ttest");
		
		assertEquals(false, MergeUtils.canMergeBePerformed(qsnpHeader, gatkHeader));
		
		VcfHeader h1 = new VcfHeader();
		VcfHeader h2 = new VcfHeader();
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456");
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456");
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456", true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "23456", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789", true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789", true);
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD", true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCDE", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD\tXZY", true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "123456\t789\tABCD\tXZY", true);
		assertEquals(true, MergeUtils.canMergeBePerformed(h1, h2));
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "12345\tXZY", true);
		h2.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "XZY\t12345", true);
		assertEquals(false, MergeUtils.canMergeBePerformed(h1, h2));
	}
	
	
	@Test
	public void getMergedHeadersAndRules() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpVcfHeader());
		Pair<VcfHeader, Rule> pair = MergeUtils.getMergedHeaderAndRules(qsnpHeader, gatkHeader);
		
		assertNotNull(pair);
		
		VcfHeader mergedHeader = pair.getLeft();
		assertEquals(true, null == mergedHeader.getInfoRecord(SnpUtils.SOMATIC));
		assertEquals(true, null != mergedHeader.getInfoRecord(Constants.VCF_MERGE_INFO));
		String columnHeaderLine = mergedHeader.getChrom().toString();
		String [] array = columnHeaderLine.split(Constants.TAB_STRING);
		assertEquals(13, array.length);
		assertEquals("FORMAT", array[8]);
		assertEquals(controlId + "_1", array[9]);
		assertEquals(testId + "_1", array[10]);
		assertEquals(controlId + "_2", array[11]);
		assertEquals(testId + "_2", array[12]);
		/*
		 * no longer adding the somatic info header lines - they should now appear in the format INF fields
		 */
		assertTrue(  mergedHeader.getInfoRecord(SnpUtils.SOMATIC) == null);
		assertTrue(  mergedHeader.getInfoRecord(SnpUtils.SOMATIC+ "_n") == null);
				
	}
	
	@Test
	public void mergeHeadersQLines() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		
 		List<VcfHeaderRecord> qsnpOtherRecords = qsnpHeader.getAllMetaRecords();  
 		List<VcfHeaderRecord> gatkOtherRecords = gatkHeader.getAllMetaRecords();
 		
 		Pair<VcfHeader, Rule> p =MergeUtils.getMergedHeaderAndRules(qsnpHeader, gatkHeader);
 		assertEquals(false, null == p);
 		VcfHeader mergedH = p.getLeft();
 		
 		/*
 		 * check to see if we have out QPG lines, and that they have the correct prefix applied
 		 */
 		List<String> qsnpQpgLines = mergedH.getAllMetaRecords().stream().filter(r -> r.toString().startsWith("##1:")).map(r -> r.toString()).collect(Collectors.toList());
 		List<String> gatkQpgLines = mergedH.getAllMetaRecords().stream().filter(r -> r.toString().startsWith("##2:")).map(r -> r.toString()).collect(Collectors.toList());
 		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = qsnpOtherRecords.stream()
			.map(r -> r.toString().replaceAll("##", "##1:"))
			.collect(Collectors.toList());
		List<String> gRecs = gatkOtherRecords.stream()
				.map(r -> r.toString().replaceAll("##", "##2:"))
				.collect(Collectors.toList());
		
		assertEquals(true, qRecs.containsAll(qsnpQpgLines));
		assertEquals(true, gRecs.containsAll(gatkQpgLines));
	}
	
	@Test
	public void mergeHeadersQLinesAgain() {
		VcfHeader h1 = new VcfHeader();
		VcfHeader h2 = new VcfHeader();
		List<VcfHeaderRecord> h1Recs = h1.getAllMetaRecords();   
		List<VcfHeaderRecord> h2Recs = h2.getAllMetaRecords();
		
		Pair<VcfHeader, Rule> p = MergeUtils.getMergedHeaderAndRules(h1, h2);
 		assertEquals(true, null == p);

		try{		
			h1.addOrReplace(VcfHeaderUtils.BLANK_HEADER_LINE + "=", true);	
			fail("Didn't throw an exception!");
		}catch(IllegalArgumentException e){}		
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE+"=", true);	
		h2.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE+"=", true);	
		
		p = MergeUtils.getMergedHeaderAndRules(h1, h2);
 		assertEquals(true, null == p);
 		
 		/*
 		 * need chrom line before merge will work
 		 */
 		h1.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
 		h2.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
 		
 		p = MergeUtils.getMergedHeaderAndRules(h1, h2);
 		assertEquals(false, null == p);
 		VcfHeader mergedH = p.getLeft();
 		assertEquals(2, mergedH.getAllMetaRecords().size());
		
		
		h2.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE+"=", true);
		p = MergeUtils.getMergedHeaderAndRules(h1, h2);
 		assertEquals(false, null == p);
 		mergedH = p.getLeft();
 		assertEquals(2, mergedH.getAllMetaRecords().size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = h1Recs.stream()
				.map(r -> r.toString().replaceAll("##", "##1:"))
				.collect(Collectors.toList());
		List<String> gRecs = h2Recs.stream()
				.map(r -> r.toString().replaceAll("##", "##2:"))
				.collect(Collectors.toList());
		
		
		List<String> qsnpQpgLines = mergedH.getAllMetaRecords().stream().filter(r -> r.toString().startsWith("##1:")).map(r -> r.toString()).collect(Collectors.toList());
 		List<String> gatkQpgLines = mergedH.getAllMetaRecords().stream().filter(r -> r.toString().startsWith("##2:")).map(r -> r.toString()).collect(Collectors.toList());
		
		assertEquals(true, qsnpQpgLines.containsAll(qRecs));
		assertEquals(true, gatkQpgLines.containsAll(gRecs));
	}
	
	@Test
	public void mergeHeadersQLines1File() {
		VcfHeader h1 = new VcfHeader();
		List<VcfHeaderRecord>h1Recs = h1.getAllMetaRecords();
		
		Pair<VcfHeader, Rule> p =MergeUtils.getMergedHeaderAndRules(h1);
 		assertEquals(true, null == p);
 		
 		/*
 		 * need chrom line before merge will work
 		 */
 		h1.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
 		
 		p =MergeUtils.getMergedHeaderAndRules(h1);
 		assertEquals(false, null == p);
 		VcfHeader mergedH = p.getLeft();
		
		assertEquals(0, mergedH.getAllMetaRecords().size());
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE+"=", true);
		p =MergeUtils.getMergedHeaderAndRules(h1);
 		assertEquals(false, null == p);
 		mergedH = p.getLeft();
		assertEquals(1, mergedH.getAllMetaRecords().size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		List<String> qRecs = h1Recs.stream()
				.map(r -> r.toString().replaceAll("##", "##1:"))
				.collect(Collectors.toList());
		assertEquals(true, mergedH.getAllMetaRecords().stream().map(r -> r.toString()).collect(Collectors.toList()).containsAll(qRecs));
	}
	
	@Test
	public void mergeHeadersQLines3File() {
		VcfHeader h1 = new VcfHeader();
		h1.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
		
		Pair<VcfHeader, Rule> p =MergeUtils.getMergedHeaderAndRules(h1, h1, h1);
 		assertEquals(false, null == p);
 		VcfHeader mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		
		
		try{
			h1.addOrReplace(VcfHeaderUtils.BLANK_HEADER_LINE, true);
			fail("Didn't throw an exception!");
		}catch(IllegalArgumentException e){}
		
		
		h1.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE+"=", true);
		p =MergeUtils.getMergedHeaderAndRules(h1, h1, h1);
 		assertEquals(false, null == p);
 		mergedH = p.getLeft();
 		assertEquals(3, mergedH.getAllMetaRecords().size());
		
		/*
		 * Check that first record has 1 appended to it, and that 2nd has 2....
		 */
		assertEquals(true, mergedH.getAllMetaRecords().contains(new VcfHeaderRecord("##1:qSource=")));
		assertEquals(true, mergedH.getAllMetaRecords().contains(new VcfHeaderRecord("##2:qSource=")));
		assertEquals(true, mergedH.getAllMetaRecords().contains(new VcfHeaderRecord("##3:qSource=")));
	}
	
	@Test
	public void mergeIDRecordLines() {
		VcfHeader h1 = new VcfHeader();
 		h1.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
 		
 		Pair<VcfHeader, Rule> p =MergeUtils.getMergedHeaderAndRules(h1);
 		assertEquals(false, null == p);
 		VcfHeader mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		
		h1.addOrReplace(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=ABC,Number=.,Type=String,Description=\"My info field\">");
		
		 p =MergeUtils.getMergedHeaderAndRules(h1);
 		assertEquals(false, null == p);
 		mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		assertEquals(2, mergedH.getInfoRecords().size());	// adds an IN line
		
		/*
		 * merge the same header, should have same number of entries
		 */
		p =MergeUtils.getMergedHeaderAndRules(h1, h1);
		assertEquals(false, null == p);
		mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		assertEquals(2, mergedH.getInfoRecords().size());
		
		
		VcfHeader h2 = new VcfHeader();
		h2.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");
		h2.addOrReplace(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=ABC,Number=.,Type=String,Description=\"My info field UPDATED\">");
		p =MergeUtils.getMergedHeaderAndRules(h1, h2);
		assertEquals(false, null == p);
		mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		assertEquals(3, mergedH.getInfoRecords().size());
		
		h2.addOrReplace(VcfHeaderUtils.HEADER_LINE_INFO + "=<ID=DEF,Number=.,Type=String,Description=\"My second info field\">");
		p =MergeUtils.getMergedHeaderAndRules(h1, h2);
		assertEquals(false, null == p);
		mergedH = p.getLeft();
		assertEquals(0, mergedH.getAllMetaRecords().size());
		assertEquals(4, mergedH.getInfoRecords().size());
	}
	
	@Test
	public void mergeHeadersFromRealVcf() {
		VcfHeader qsnpHeader = new VcfHeader(getQsnpVcfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		VcfHeader newHeader = MergeUtils.getMergedHeaderAndRules(qsnpHeader, gatkHeader).getLeft();
		
		assertEquals(21, newHeader.getInfoRecords().size());		
		assertEquals(15, newHeader.getFilterRecords().size());
		assertEquals(10,newHeader.getFormatRecords().size());
	}
	
	@Test
	public void mergeHeadersFromSoonToBeRealVcf() {
		VcfHeader qsnpHeader = new VcfHeader(getUpdatedQsnpVCfHeader());
		VcfHeader gatkHeader = new VcfHeader(getQsnpGATKVcfHeader());
		
		VcfHeader newHeader = MergeUtils.getMergedHeaderAndRules(qsnpHeader, gatkHeader).getLeft();		
		assertEquals(21, newHeader.getInfoRecords().size());		
		assertEquals(15, newHeader.getFilterRecords().size());
		assertEquals(10,newHeader.getFormatRecords().size());
		
		
//		List<VcfHeaderRecord> qsnpH = qsnpHeader.getInfoRecords();
//		List<VcfHeaderRecord> gatkH = gatkHeader.getInfoRecords();
//		List<VcfHeaderRecord> mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
//		assertEquals(20, mergedRecs.size());
//		
//		qsnpH = qsnpHeader.getFilterRecords();
//		gatkH = gatkHeader.getFilterRecords();
//		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
//		assertEquals(15, mergedRecs.size());
//		
//		qsnpH = qsnpHeader.getFormatRecords();
//		gatkH = gatkHeader.getFormatRecords();
//		mergedRecs = MergeUtils.mergeHeaderRecords(qsnpH, gatkH);
//		assertEquals(10, mergedRecs.size());
	}
	
	@Test
	public void getGT() {
		assertEquals("0/1", MergeUtils.getGT("A", "A", "0/1"));
		assertEquals("1/1", MergeUtils.getGT("A", "A", "1/1"));
		assertEquals("0/0", MergeUtils.getGT("A", "A", "0/0"));
		assertEquals("./.", MergeUtils.getGT("A", "A", "./."));
		
		assertEquals("0/1", MergeUtils.getGT("A,C", "A", "0/1"));
		assertEquals("0/2", MergeUtils.getGT("A,C", "C", "0/1"));
		assertEquals("0/2", MergeUtils.getGT("C,T", "T,C", "0/1"));
		assertEquals("0/1", MergeUtils.getGT("C,T", "T,C", "0/2"));
		
		
		assertEquals("0/1", MergeUtils.getGT("G,T", "T,G", "0/2"));
		assertEquals("0/2", MergeUtils.getGT("G,T", "T,G", "0/1"));
		assertEquals("2/1", MergeUtils.getGT("G,T", "T,G", "1/2"));
	}
	
	@Test
	public void mergeRecordInfo() {
		VcfRecord r1 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").build();
		VcfRecord r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").build();
		VcfRecord mergedR = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").build();
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
		
		VcfRecord r1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 0), ".", ".",".");			 
		VcfRecord r2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 0), ".", ".",".");				 
		VcfRecord mergedR = VcfUtils.createVcfRecord(new ChrPointPosition("1", 0), ".", ".",".");			 
				
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 = new VcfRecord.Builder("1", 100, ".").allele(".").build();
		r2 = new VcfRecord.Builder("1", 100, ".").allele(".").build();
		mergedR = new VcfRecord.Builder("1", 100, ".").allele(".").build();
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1 =new VcfRecord.Builder("1", 100, "ABC").allele(".").build();
		r2 = new VcfRecord.Builder("1", 100, "ABC").allele(".").build();
		mergedR = new VcfRecord.Builder("1", 100, "ABC").allele(".").build();
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

		r1 =  new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123").build();
		r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs456").build();
		mergedR =  new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123,rs456").build();
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
	}
	
	@Test
	public void mergeRecordInfoOnlyWithRules() {
		Map<Integer, Map<String, String>> idRules = new HashMap<>();
		Map<String, String> rulesForThisFile = new HashMap<>();
		idRules.put(1,  rulesForThisFile);
		rulesForThisFile.put("ID", "ID1");
		
		VcfRecord r1 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123").build();
		VcfRecord r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs456").build();
		VcfRecord mergedR = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123,rs456").build();
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
		
		VcfRecord r1 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123").build();
		VcfRecord r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs456").build();
		VcfRecord mergedR = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123,rs456").build();
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1.setFilter("F1");
		r2.setFilter("F1");
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		/*
		 * filters are not persisted
		 */
		assertEquals(null, mergedR.getFilter());
		
		r1.setFilter("F1");
		r2.setFilter("F2");
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals(null, mergedR.getFilter());
	}
	
	// TODO should we do anything special when dealing with FILTER? PASS value for example?
	
	@Test
	public void mergeRecordFormat() {
		VcfRecord r1 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123").build();
		VcfRecord r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs456").build();
		VcfRecord mergedR = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123,rs456").build();
		assertEquals(mergedR, MergeUtils.mergeRecords(null, r1, r2));
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF", "1:2:3"));
		r2.setFormatFields(Arrays.asList("GH:IJ:KL", "4:5:6"));
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals("AB:CD:EF:GH:IJ:KL\t1:2:3:.:.:.\t.:.:.:4:5:6", mergedR.getFormatFieldStrings());
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF", "1:2:3"));
		r2.setFormatFields(Arrays.asList("EF:GH:IJ:KL", "3:4:5:6"));
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals("AB:CD:EF:GH:IJ:KL\t1:2:3:.:.:.\t.:.:3:4:5:6", mergedR.getFormatFieldStrings());
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF:GH", "1:2:3:X"));
		r2.setFormatFields(Arrays.asList("EF:GH:IJ:KL", "3:4:5:6"));
		mergedR = MergeUtils.mergeRecords(null, r1, r2);
		assertEquals("AB:CD:EF:GH:IJ:KL\t1:2:3:X:.:.\t.:.:3:4:5:6", mergedR.getFormatFieldStrings());
	}
	
	@Test
	public void mergeRecordFormatWithRules() {
		Map<Integer, Map<String, String>> idRules = new HashMap<>();
		Map<String, String> rulesForThisFile = new HashMap<>();
		idRules.put(1,  rulesForThisFile);
		rulesForThisFile.put("EF", "EF1");
		
		VcfRecord r1 =new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123").build();
		VcfRecord r2 = new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs456").build();
		VcfRecord mergedR =new VcfRecord.Builder("1", 100, "ABC").allele("DEF").id("rs123,rs456").build();
		assertEquals(mergedR, MergeUtils.mergeRecords(idRules, r1, r2));
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF", "1:2:3"));
		r2.setFormatFields(Arrays.asList("GH:IJ:KL", "4:5:6"));
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("AB:CD:EF:GH:IJ:KL\t1:2:3:.:.:.\t.:.:.:4:5:6", mergedR.getFormatFieldStrings());
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF", "1:2:3"));
		r2.setFormatFields(Arrays.asList("EF:GH:IJ:KL", "3:4:5:6"));
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("AB:CD:EF:EF1:GH:IJ:KL\t1:2:3:.:.:.:.\t.:.:.:3:4:5:6", mergedR.getFormatFieldStrings());
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF", "1:2:3"));
		r2.setFormatFields(Arrays.asList("EF:GH:IJ:KL", "HEllo:4:5:6"));
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("AB:CD:EF:EF1:GH:IJ:KL\t1:2:3:.:.:.:.\t.:.:.:HEllo:4:5:6", mergedR.getFormatFieldStrings());
		
		r1.setFormatFields(Arrays.asList("AB:CD:EF:GH", "1:2:3:X"));
		r2.setFormatFields(Arrays.asList("EF:GH:IJ:KL", "3:4:5:6"));
		mergedR = MergeUtils.mergeRecords(idRules, r1, r2);
		assertEquals("AB:CD:EF:EF1:GH:IJ:KL\t1:2:3:.:X:.:.\t.:.:.:3:4:5:6", mergedR.getFormatFieldStrings());
	}
	
	@Test
	public void keepGQandQualFromGATK() {
		VcfRecord v1 = new VcfRecord(new String[] {"chr1","4985568",".",	"A","C",	".",	"PASS",	"FLANK=ACGTTCCTGCA","GT:GD:OABS:MR:NNS","0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]:8:8","1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]:42:38"});
		VcfRecord v2 = new VcfRecord(new String[] {"chr1","4985568","rs10753395","A","C","245.77","PASS",".","GT:AD:DP:GQ:PL:GD:OABS:MR:NNS:QL","0/1:18,8:26:99:274,0,686:A/C:A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:235.77","1/1:1,44:45:94:1826,94,0:C/C:A1[37],0[0],C24[38.88],23[40.26]:47:42:245.77"});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		List<String> ff = mr.getFormatFields();
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ff);
		
		assertEquals(11, ffMap.size());	// 10 in constituent records plus INF
		assertEquals(true, ffMap.containsKey("QL"));
		assertEquals(true, ffMap.containsKey("GQ"));
	}
	
	@Test
	public void updateAD() {
		VcfRecord v1 = new VcfRecord(new String[] {"chr1","100",".",	"A","C",	".",	"PASS",	"FLANK=ACGTTCCTGCA","GT:AD:OABS:MR:NNS","0/1:19,8:A8[33.75]11[38.82];C3[42]5[40]:8:8","1/1:1,42:A1[37]0[0];C23[38.96]19[41.21]:42:38"});
		VcfRecord v2 = new VcfRecord(new String[] {"chr1","100",".","A","C,T","245.77","PASS",".","GT:AD:DP:GQ:PL:OABS:MR:NNS:QL","0/1:18,8,0:26:99:274,0,686:A9[33.56]11[38.82];C3[42]5[40];G0[0]1[22];T1[11]0[0]:8:8:235.77","1/2:1,44,10:45:94:1826,94,0:A1[37]0[0];C24[38.88]23[40.26];T5[]5[]:47:42:245.77"});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		assertEquals("C,T", mr.getAlt());
		List<String> ff = mr.getFormatFields();
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ff);
		
		assertEquals(10, ffMap.size());	// 9 in constituent records plus INF
		assertEquals(true, ffMap.containsKey("QL"));
		assertEquals(true, ffMap.containsKey("GQ"));
		assertArrayEquals(new String[]{"0/1","1/1","0/1","1/2"}, ffMap.get("GT"));
		assertArrayEquals(new String[]{"19,8,0","1,42,0","18,8,0","1,44,10"}, ffMap.get("AD"));
		
		/*
		 * this time, update alt in first record so that GT and AD will need to be changed
		 */
		v1 = new VcfRecord(new String[] {"chr1","100",".",	"A","T",	".",	"PASS",	"FLANK=ACGTTCCTGCA","GT:AD:OABS:MR:NNS","0/1:19,8:A8[33.75]11[38.82];T3[42]5[40]:8:8","1/1:1,42:A1[37]0[0];T23[38.96]19[41.21]:42:38"});
		v2 = new VcfRecord(new String[] {"chr1","100",".","A","C,T","245.77","PASS",".","GT:AD:DP:GQ:PL:OABS:MR:NNS:QL","0/1:18,8:26:99:274,0,686:A9[33.56]11[38.82];C3[42]5[40];G0[0]1[22];T1[11]0[0]:8:8:235.77","1/2:1,44,10:45:94:1826,94,0:A1[37]0[0];C24[38.88]23[40.26];T5[]5[]:47:42:245.77"});
		mr = MergeUtils.mergeRecords(null,  v1, v2);
		assertEquals("C,T", mr.getAlt());
		ff = mr.getFormatFields();
		ffMap = VcfUtils.getFormatFieldsAsMap(ff);
			
		assertEquals(10, ffMap.size());	// 9 in constituent records plus INF
		assertEquals(true, ffMap.containsKey("QL"));
		assertEquals(true, ffMap.containsKey("GQ"));
		String [] gtArray = ffMap.get("GT");
		assertArrayEquals(new String[]{"0/2","2/2","0/1","1/2"}, gtArray);
		assertArrayEquals(new String[]{"19,0,8","1,0,42","18,8","1,44,10"}, ffMap.get("AD"));
	}
	
	@Test
	public void multipleSomatics() {
		VcfRecord v1 = new VcfRecord(new String[] {"chr1","4985568",".",	"A",	"C",	".",	"PASS",	"FLANK=ACGTTCCTGCA","GT:GD:AC:MR:NNS:INF","0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]:8:8:.","1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]:42:38:SOMATIC"});
		VcfRecord v2 = new VcfRecord(new String[] {"chr1","4985568","rs10753395","A","C","245.77","PASS","AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DB;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693","GT:AD:DP:GQ:PL:GD:AC:MR:NNS","0/1:18,8:26:99:274,0,686:A/C:A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8","1/1:1,44:45:94:1826,94,0:C/C:A1[37],0[0],C24[38.88],23[40.26]:47:42"});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		assertEquals("FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DB;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693", mr.getInfo());
		/*
		 * should have SOMATIC in format INF field
		 */
		List<String> ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		assertEquals(Constants.MISSING_DATA_STRING, new VcfFormatFieldRecord(ff.get(0), ff.get(1)).getField("INF"));
		assertEquals(SnpUtils.SOMATIC, new VcfFormatFieldRecord(ff.get(0), ff.get(2)).getField("INF"));
		assertEquals(Constants.MISSING_DATA_STRING, new VcfFormatFieldRecord(ff.get(0), ff.get(3)).getField("INF"));
		assertEquals(Constants.MISSING_DATA_STRING, new VcfFormatFieldRecord(ff.get(0), ff.get(4)).getField("INF"));
//		assertEquals("SOMATIC_1;FLANK=ACGTTCCTGCA;AC=1;AF=0.500;AN=2;BaseQRankSum=0.972;ClippingRankSum=1.139;DB;DP=26;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQ0=0;MQRankSum=-0.472;QD=9.45;ReadPosRankSum=-0.194;SOR=0.693", mr.getInfo());
		
		v1 = new VcfRecord(new String[] {"chr1","4985568",".",	"A",	",C",	".",	"PASS",	"FLANK=ACGTTCCTGCA","GT:GD:AC:MR:NNS:INF","0/1:A/C:A8[33.75],11[38.82],C3[42],5[40]:8:8:.","1/1:C/C:A1[37],0[0],C23[38.96],19[41.21]:42:38:SOMATIC"});
		v2 = new VcfRecord(new String[] {"chr1","4985568","rs10753395","A","C","245.77","PASS","FLANK=ACGTTCCTGCA","GT:AD:DP:GQ:PL:GD:AC:MR:NNS:INF","0/1:18,8:26:99:274,0,686:A/C:A9[33.56],11[38.82],C3[42],5[40],G0[0],1[22],T1[11],0[0]:8:8:.","1/1:1,44:45:94:1826,94,0:C/C:A1[37],0[0],C24[38.88],23[40.26]:47:42:SOMATIC"});
		mr = MergeUtils.mergeRecords(null,  v1, v2);
		assertEquals("FLANK=ACGTTCCTGCA", mr.getInfo());
		ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		assertEquals(Constants.MISSING_DATA_STRING, new VcfFormatFieldRecord(ff.get(0), ff.get(1)).getField("INF"));
		assertEquals(SnpUtils.SOMATIC, new VcfFormatFieldRecord(ff.get(0), ff.get(2)).getField("INF"));
		assertEquals(Constants.MISSING_DATA_STRING, new VcfFormatFieldRecord(ff.get(0), ff.get(3)).getField("INF"));
		assertEquals(SnpUtils.SOMATIC, new VcfFormatFieldRecord(ff.get(0), ff.get(4)).getField("INF"));
//		assertEquals("SOMATIC_1;FLANK=ACGTTCCTGCA;SOMATIC_2", mr.getInfo());
	}
	
	
	@Test
	public void mergeRealLifeData() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","10250",".","A","C",".","PASS","FLANK=CCTAACCCCTA;IN=1","GT:GD:AC:MR:NNS","0/1:A/C:A38[31.42],32[25],C11[27.64],5[36.6]:16:16","0/1:A/C:A75[31.96],57[29.32],C12[35.25],6[38]:18:16"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","10250",".","A","C","43.77","NCIT","C=1;AF=0.500;AN=2;BaseQRankSum=1.026;ClippingRankSum=0.000;DP=12;IN=2;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=29.55;MQ0=0;MQRankSum=-1.026;QD=3.65;ReadPosRankSum=1.026;SOR=0.693","GT:AD:DP:GQ:PL:GD:AC:MR:NNS","0/1:2,2:4:69:72,0,69:A/C:A101[29.56],51[27.63],C30[30.83],21[37.29],G1[12],0[0]:51:44","./.:.:.:.:.:.:A191[31.2],147[27.37],C70[30.29],92[37.47],T0[0],1[37]:162:101"});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		
		assertEquals("chr1", mr.getChromosome());
		assertEquals(10250, mr.getPosition());
		assertEquals("A", mr.getRef());
		assertEquals("C", mr.getAlt());
		assertEquals(null, mr.getFilter());
		Stream.of("FLANK=CCTAACCCCTA","IN=1,2","C=1","AF=0.500","AN=2","BaseQRankSum=1.026","ClippingRankSum=0.000","DP=12","FS=0.000","MLEAC=1","MLEAF=0.500","MQ=29.55","MQ0=0","MQRankSum=-1.026","QD=3.65","ReadPosRankSum=1.026","SOR=0.693")
			.forEach(s -> assertEquals(true, mr.getInfo().contains(s)));
		
		List<String> ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		/*
		 * alphabetisized
		 */
		assertEquals("GT:AC:AD:DP:FT:GD:GQ:MR:NNS:PL", ff.get(0));
		assertEquals("0/1:A38[31.42],32[25],C11[27.64],5[36.6]:.:.:PASS:A/C:.:16:16:.", ff.get(1));
		assertEquals("0/1:A75[31.96],57[29.32],C12[35.25],6[38]:.:.:PASS:A/C:.:18:16:.", ff.get(2));
		assertEquals("0/1:A101[29.56],51[27.63],C30[30.83],21[37.29],G1[12],0[0]:2,2:4:NCIT:A/C:69:51:44:72,0,69", ff.get(3));
		assertEquals("./.:A191[31.2],147[27.37],C70[30.29],92[37.47],T0[0],1[37]:.:.:NCIT:.:.:162:101:.", ff.get(4));
	}
	
	@Test
	public void mergeRealLifeDataAD() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","2587689",".","A","C,G",".",".","FLANK=AGCACCCACAC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","2/2:3,5,54:62:C1[]1[];G4[]1[]:A6;C26;G58:.:.:47:A2[26.5]1[41];C3[30]2[22];G37[32.59]17[33.06]","1/2:4,10,61:75:A1[]0[];C1[]0[];G0[]2[]:A13;C68;G87;T1:.:SOMATIC:10,50:A2[24.5]2[41];C4[28.25]6[24.33];G35[30.74]26[31.5]"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","2587689",".","A","G",".",".","DP=60;ExcessHet=3.0103;FS=0.000;MQ=53.08;QD=34.17;SOR=1.919","GT:AD:DP:GQ:QL:FT:INF","1/1:0,56:56:99:1913.77:.:.","1/1:0,91:91:99:2811.77:.:."});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		
		assertEquals("chr1", mr.getChromosome());
		assertEquals(2587689, mr.getPosition());
		assertEquals("A", mr.getRef());
		assertEquals("C,G", mr.getAlt());
		assertEquals(null, mr.getFilter());
		assertEquals("FLANK=AGCACCCACAC;DP=60;ExcessHet=3.0103;FS=0.000;MQ=53.08;QD=34.17;SOR=1.919", mr.getInfo());
		
		List<String> ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		
		Map<String, String[]> ffs = mr.getFormatFieldsAsMap();
		assertArrayEquals(new String[]{"3,5,54","4,10,61", "0,0,56","0,0,91"}, ffs.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS));
	}
	
	
	@Test
	public void combinedAlts() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr2","91629178",".","C","A",".",".","FLANK=TCTGTATGTTC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:62,4:66:A1[]0[];C1[]6[]:A17;C91;T29:.:.:4:A3[38]1[12];C32[38.06]30[38]","0/0:123,6:130:A2[]0[];C7[]6[]:A61;C181;G2;T76:.:.:.:A6[38]0[0];C80[40.11]43[37.51];T0[0]1[41]"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr2","91629178",".","C","T",".",".","BaseQRankSum=-0.281;ClippingRankSum=0.000;DP=92;ExcessHet=3.0103;FS=0.954;MQ=33.50;MQRankSum=-1.018;QD=7.31;ReadPosRankSum=1.395;SOR=0.846","GT:AD:DP:GQ:QL:FT:INF","0/1:69,22:91:99:664.77:.:.","0/1:147,47:194:99:1551.77:.:."});
		assertEquals("A,T", MergeUtils.getCombinedAlt(v1, v2).get());
		
		v1 = new VcfRecord(new String[]{"chr1","142671569",".","C","T",".",".","FLANK=GCAGATCTGAG","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:70,27:97:C0[]1[]:A1;C7;G46;T6:.:.:23:C30[37.8]40[37.85];T9[32.67]18[33.17]","0/1:137,39:177:C6[]5[];T1[]0[]:C15;G117;T10:.:.:37:C69[38.42]68[39.04];G1[41]0[0];T16[38.38]23[34.26]"});
		v2 = new VcfRecord(new String[]{"chr1","142671569",".","C","T,G",".",".",".","GT:AD:DP:GQ:QL:FT:INF","0/1:71,28:99:99:757.77:.:.","0/2:141,0,59:200:99:1491.77:.:SOMATIC"});
		assertEquals("G,T", MergeUtils.getCombinedAlt(v1, v2).get());
		
	}
	
	@Test
	public void mergeRealLifeDataAD2() {
		
		/*
		 * Actual output
		 * chr2    91629178        .       C       A,T     .       .       FLANK=TCTGTATGTTC;BaseQRankSum=-0.281;ClippingRankSum=0.000;DP=92;ExcessHet=3.0103;FS=0.954;MQ=33.50;MQRankSum=-1.018;QD=7.31;ReadPosRankSum=1.395;SOR=0.846;IN=1,2     GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL   0/1:62,4,0:66:A1[]0[];C1[]6[]:A17;C91;T29:.:.:.:4:A3[38]1[12];C32[38.06]30[38]:.        0/0:123,6,1:130:A2[]0[];C7[]6[]:A61;C181;G2;T76:.:.:.:.:A6[38]0[0];C80[40.11]43[37.51];T0[0]1[41]:.     0/2:69,22:91:.:.:.:99:.:.:.:664.77      0/2:147,47:194:.:.:.:99:.:.:.:1551.77
		 */
		
		
		VcfRecord v1 = new VcfRecord(new String[]{"chr2","91629178",".","C","A",".",".","FLANK=TCTGTATGTTC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:62,4:66:A1[]0[];C1[]6[]:A17;C91;T29:.:.:4:A3[38]1[12];C32[38.06]30[38]","0/0:123,6:130:A2[]0[];C7[]6[]:A61;C181;G2;T76:.:.:.:A6[38]0[0];C80[40.11]43[37.51];T0[0]1[41]"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr2","91629178",".","C","T",".",".","BaseQRankSum=-0.281;ClippingRankSum=0.000;DP=92;ExcessHet=3.0103;FS=0.954;MQ=33.50;MQRankSum=-1.018;QD=7.31;ReadPosRankSum=1.395;SOR=0.846","GT:AD:DP:GQ:QL:FT:INF","0/1:69,22:91:99:664.77:.:.","0/1:147,47:194:99:1551.77:.:."});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		
		assertEquals("chr2", mr.getChromosome());
		assertEquals(91629178, mr.getPosition());
		assertEquals("C", mr.getRef());
		assertEquals("A,T", mr.getAlt());
		assertEquals(null, mr.getFilter());
		assertEquals("FLANK=TCTGTATGTTC;BaseQRankSum=-0.281;ClippingRankSum=0.000;DP=92;ExcessHet=3.0103;FS=0.954;MQ=33.50;MQRankSum=-1.018;QD=7.31;ReadPosRankSum=1.395;SOR=0.846", mr.getInfo());
		
		List<String> ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		
		Map<String, String[]> ffs = mr.getFormatFieldsAsMap();
		assertArrayEquals(new String[]{"62,4,0","123,6,1", "69,0,22","147,0,47"}, ffs.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS));
	}
	
	@Test
	public void mergeRealLifeDataAD3() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","142671569",".","C","T",".",".","FLANK=GCAGATCTGAG","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:70,27:97:C0[]1[]:A1;C7;G46;T6:.:.:23:C30[37.8]40[37.85];T9[32.67]18[33.17]","0/1:137,39:177:C6[]5[];T1[]0[]:C15;G117;T10:.:.:37:C69[38.42]68[39.04];G1[41]0[0];T16[38.38]23[34.26]"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","142671569",".","C","T,G",".",".",".","GT:AD:DP:GQ:QL:FT:INF","0/1:71,28:99:99:757.77:.:.","0/2:141,0,59:200:99:1491.77:.:SOMATIC"});
		VcfRecord mr = MergeUtils.mergeRecords(null,  v1, v2);
		
		assertEquals("chr1", mr.getChromosome());
		assertEquals(142671569, mr.getPosition());
		assertEquals("C", mr.getRef());
		assertEquals("G,T", mr.getAlt());
		assertEquals(null, mr.getFilter());
		
		List<String> ff = mr.getFormatFields();
		assertEquals(5, ff.size());
		
		Map<String, String[]> ffs = mr.getFormatFieldsAsMap();
		assertArrayEquals(new String[]{"0/2","0/2", "0/2","0/1"}, ffs.get(VcfHeaderUtils.FORMAT_GENOTYPE));
		assertArrayEquals(new String[]{"70,0,27","137,1,39", "71,0,28","141,59,0"}, ffs.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS));
	}
	@Test
	public void moveDataToFormatRealLife2() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","142671569",".","C","T",".",".","FLANK=GCAGATCTGAG","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","0/1:70,27:97:C0[]1[]:A1;C7;G46;T6:.:.:23:C30[37.8]40[37.85];T9[32.67]18[33.17]","0/1:137,39:177:C6[]5[];T1[]0[]:C15;G117;T10:.:.:37:C69[38.42]68[39.04];G1[41]0[0];T16[38.38]23[34.26]"});
		MergeUtils.moveDataToFormat(v1, "G,T", true);
		Map<String, String[]> ffMap = v1.getFormatFieldsAsMap();
		String [] gtArray = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		assertEquals("0/2", gtArray[0]);
		assertEquals("0/2", gtArray[1]);
		String [] adArray = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		assertEquals("70,0,27", adArray[0]);
		assertEquals("137,1,39", adArray[1]);
		
//		assertEquals("G,T", v1.getAlt());
		v1 = new VcfRecord(new String[]{"chr1","142671569",".","C","T,G",".",".",".","GT:AD:DP:GQ:QL:FT:INF","0/1:71,28:99:99:757.77:.:.","0/2:141,0,59:200:99:1491.77:.:SOMATIC"});
		MergeUtils.moveDataToFormat(v1, "G,T", true);
		ffMap = v1.getFormatFieldsAsMap();
		gtArray = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		assertEquals("0/2", gtArray[0]);
		assertEquals("0/1", gtArray[1]);
		adArray = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		assertEquals("71,0,28", adArray[0]);
		assertEquals("141,59,0", adArray[1]);
		
	}
	
	@Test
	public void moveDataToFormat() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","2587689",".","A","C,G",".",".","FLANK=AGCACCCACAC","GT:AD:DP:EOR:FF:FT:INF:NNS:OABS","2/2:3,5,54:62:C1[]1[];G4[]1[]:A6;C26;G58:.:.:47:A2[26.5]1[41];C3[30]2[22];G37[32.59]17[33.06]","1/2:4,10,61:75:A1[]0[];C1[]0[];G0[]2[]:A13;C68;G87;T1:.:SOMATIC:10,50:A2[24.5]2[41];C4[28.25]6[24.33];G35[30.74]26[31.5]"});
		MergeUtils.moveDataToFormat(v1, "C,G", true);
		assertEquals("FLANK=AGCACCCACAC", v1.getInfo());
		assertEquals("GT:AD:DP:EOR:FF:FT:INF:NNS:OABS\t2/2:3,5,54:62:C1[]1[];G4[]1[]:A6;C26;G58:.:.:47:A2[26.5]1[41];C3[30]2[22];G37[32.59]17[33.06]\t1/2:4,10,61:75:A1[]0[];C1[]0[];G0[]2[]:A13;C68;G87;T1:.:SOMATIC:10,50:A2[24.5]2[41];C4[28.25]6[24.33];G35[30.74]26[31.5]", v1.getFormatFieldStrings());
		
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","2587689",".","A","G",".",".","DP=60;ExcessHet=3.0103;FS=0.000;MQ=53.08;QD=34.17;SOR=1.919","GT:AD:DP:GQ:QL:FT:INF","1/1:0,56:56:99:1913.77:.:.","1/1:0,91:91:99:2811.77:.:."});
		MergeUtils.moveDataToFormat(v2, "C,G", true);
		assertEquals("DP=60;ExcessHet=3.0103;FS=0.000;MQ=53.08;QD=34.17;SOR=1.919", v2.getInfo());
		assertEquals("GT:AD:DP:FT:GQ:INF:QL\t2/2:0,0,56:56:.:99:.:1913.77\t2/2:0,0,91:91:.:99:.:2811.77", v2.getFormatFieldStrings());
	}
	
	@Test
	public void fillAD() {
		assertEquals("0,1", MergeUtils.fillAD("0,1", "1/1", "1/1"));
		assertEquals("0,0,1", MergeUtils.fillAD("0,1", "2/2", "1/1"));
		assertEquals("0,0,1", MergeUtils.fillAD("0,1", "0/2", "0/1"));
		assertEquals("0,100,50", MergeUtils.fillAD("0,100,50", "1/2", "1/2"));
		assertEquals("0,0,100", MergeUtils.fillAD("0,100", "2/2", "1/1"));
		assertEquals("0,0,100,50", MergeUtils.fillAD("0,100,50", "2/3", "1/2"));
		assertEquals("0,100,0,50", MergeUtils.fillAD("0,100,50", "3/3", "2/2"));
		assertEquals("0,100,0,50", MergeUtils.fillAD("0,100,50", "3/3", "2/2"));
		
		assertEquals("141,59,0", MergeUtils.fillAD("141,0,59", "0/1", "0/2"));
		assertEquals("10,30,2", MergeUtils.fillAD("10,2,30", "0/1", "0/2"));
	}
	
	// TODO should we do anything special when dealing with FILTER? PASS value for example? 
	
	public List<String> getQsnpGATKVcfHeader() {
		
		return Arrays.asList("##fileformat=VCFv4.2",
"##fileDate=20151211",
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
"#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa	http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb");

//"#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	http://purl.org/net/grafli/collectedsample#e734bdbc-2e43-44e4-ad32-11719865f9d6	http://purl.org/net/grafli/collectedsample#49c59f9e-fe9d-4813-a4c6-3198ec003859");
	}
	
	public List<String> getUpdatedQsnpVCfHeader() {
		String controlId = "http://purl.org/net/grafli/collectedsample#f124a2ca-5e24-419a-96f5-dd849ccc50aa"; //"ABC_123";
		String testId = "http://purl.org/net/grafli/collectedsample#64d9c65d-d0af-43e7-a835-8fe3c36b93bb";   //"DEF_456";
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		
		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=qSNP v2.0 (882)");		
		
		header.addOrReplace(VcfHeaderUtils.STANDARD_DONOR_ID + "=");
		header.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=" + controlId);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=" + testId);		
		
		header.addOrReplace( "##qAnalysisId=12345");
		
		header.addInfo(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1", "String","Flanking sequence either side of variant");																	
		
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12, "Less than 12 reads coverage in normal");
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8,"Less than 8 reads coverage in normal");  
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR,"Less than 8 reads coverage in tumour"); 
		header.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL,"Less than 3 reads of same allele in normal");  
		header.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR,"Less than 3 reads of same allele in tumour");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL,"Mutation also found in pileup of normal");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL,"Mutation also found in pileup of (unfiltered) normal");  
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE,"Mutation is a germline variant in another patient");  
		header.addFilter(VcfHeaderUtils.FILTER_NOVEL_STARTS,"Less than 4 novel starts not considering read pair");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTANT_READS,"Less than 5 mutant reads"); 
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF,"Mutation equals reference"); 
		header.addFilter(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST,"No call in test"); 
		header.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_ALT,"Alternate allele on only one strand (or percentage alternate allele on other strand is less than " + 5 + "%)"); 
		header.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_COV,"Sequence coverage on only one strand (or percentage coverage on other strand is less than " + 5 + "%)"); 
		
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE, "1", "String" ,"Genotype");
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, "1", "String","Genotype details: specific alleles (A,G,T or C)");
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "1", "String","Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]");
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP, "1", "String","Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)");
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, ".", "Integer","Allelic depths for the ref and alt alleles in the order listed");
		header.addFormat(VcfHeaderUtils.FORMAT_READ_DEPTH, "1", "Integer","Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY, "1", "Integer","Genotype Quality");
		header.addFormat(VcfHeaderUtils.FORMAT_MUTANT_READS,  "1", "Integer","Number of mutant/variant reads");
		header.addFormat(VcfHeaderUtils.FORMAT_NOVEL_STARTS, "1", "Integer","Number of novel starts not considering read pair");		
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + (controlId != null ? controlId + "\t" : "") + testId);
		
		List<String> list = new ArrayList<>();
		Iterator<VcfHeaderRecord> iter = header.iterator();
		
		while(iter.hasNext()) {
			list.add(iter.next().toString());
		}
		return list;
	}
}
