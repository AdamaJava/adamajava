package org.qcmg.qprofiler.fastq;

import static org.junit.Assert.assertEquals;
import htsjdk.samtools.fastq.FastqRecord;

import org.junit.Ignore;
import org.junit.Test;

public class FastqSummaryReportTest {
	
	@Test
	public void parseRecordHeader() {
		FastqRecord rec = new FastqRecord("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1", 
				"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
				"+", 
				"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("@ERR091788").intValue());
		assertEquals(1, report.flowCellIds.get("HSQ955").intValue());
		assertEquals(1, report.flowCellLanes.get("2").intValue());
		assertEquals(1, report.tileNumbers.get(1101).intValue());
		assertEquals(1, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
		
	}
	
	@Ignore	// may need to cater for this in the future...
	public void parseAnotherRecordHeader() {
		FastqRecord rec = new FastqRecord("@SRR001666.1 071112_SLXA-EAS1_s_7:5:1:817:345 length=36", 
				"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
				"+", 
				"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("@ERR091788").intValue());
		assertEquals(1, report.flowCellIds.get("HSQ955").intValue());
		assertEquals(1, report.flowCellLanes.get("2").intValue());
		assertEquals(1, report.tileNumbers.get(1101).intValue());
		assertEquals(1, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
		
	}
	
	

}
