package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.qprofiler2.fastq.FastqSummaryReport;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;

public class ReadIDSummaryTest {
	
	
	@Test
	public void parseBGIRecordHeader() throws Exception {
		
		SAMRecord record = new SAMRecord(null);
		record.setReadName("CL100013884L2C004R071_323304");				
	 
		ReadIDSummary summary =  new ReadIDSummary();
		summary.parseReadId( record.getReadName() );
		assertEquals(0, summary.getInstrumentsMap().size() );
		assertEquals(0, summary.getRunIdsMap().size());
		assertEquals(1, summary.getFlowCellIdsMap().size() );
		assertEquals(1, summary.getFlowCellLanesMap().size() );
		assertEquals(1, summary.getTileNumbersMap().size() );		
		assertEquals(1, summary.getFlowCellIdsMap().get("CL100013884").get() );
		assertEquals(1, summary.getFlowCellLanesMap().get("L2").get() );
		assertEquals(1, summary.getTileNumbersMap().get("C004R071").get() ); 
 	
	}	

	
	@Test
	public void parseRecordHeader() {
		FastqRecord rec = new FastqRecord("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1", 
				"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
				"+", 
				"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		
		ReadIDSummary header = report.getReadIDSummary();		
		assertEquals(1, header.getInstrumentsMap().get("@ERR091788").intValue());
		assertEquals(1, header.getFlowCellIdsMap().get("HSQ955").intValue());
		assertEquals(1, header.getFlowCellLanesMap().get("2").intValue());		
		assertEquals(1, header.getTileNumbersMap().get("1101").intValue());
		assertEquals(1, header.getPairs().get("1") .intValue());
		assertEquals(0, header.getPairs().get("2").intValue());
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
		
		ReadIDSummary header = report.getReadIDSummary();		
		assertEquals(1, header.getInstrumentsMap().get("@ERR091788").intValue());
		assertEquals(1, header.getFlowCellIdsMap().get("HSQ955").intValue());
		assertEquals(1, header.getFlowCellLanesMap().get("2").intValue());
		assertEquals(1, header.getTileNumbersMap().get(1101).intValue());
		assertEquals(1, header.getPairs().get("1") .intValue());
		assertEquals(0, header.getPairs().get("2").intValue());
					
	}	
	
}
