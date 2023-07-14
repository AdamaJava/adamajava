package org.qcmg.qprofiler.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FastqSummaryReportTest {
	
	@Test
	public void parseRecordHeader() {
		FastqRecord rec = new FastqRecord("ERR091788.1 HSQ955_155:2:1101:1473:2037/1",
				"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
				"",
				"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("ERR091788").intValue());
		assertEquals(1, report.flowCellIds.get("HSQ955").intValue());
		assertEquals(1, report.flowCellLanes.get("2").intValue());
		assertEquals(1, report.tileNumbers.get(1101).intValue());
		assertEquals(1, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}
	
	@Test	// may need to cater for this in the future...
	public void parseHeaderDoubleSpace2() {
		FastqRecord rec = new FastqRecord("SRR001666.1 071112_SLXA-EAS1_s_7:5:1:817:345 length=36",
				"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
				"",
				"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("071112").intValue());
		assertEquals(1, report.runIds.get("SLXA-EAS1_s_7").intValue());
		assertEquals(1, report.flowCellLanes.get("5").intValue());
		assertEquals(1, report.tileNumbers.get(1).intValue());
		assertEquals(0, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}
	@Test	// may need to cater for this in the future...
	public void parseHeaderDoubleSpace() {

		/*
		@SRR14585604.19092 A00805:41:HMJJWDRXX:1:1101:15329:20181 length=101
TGCATTGTGTCAAAAGAAATTTCCTTATTTTCTACTGCCATTCCCATAAAAGTAAGTAGTCTCATTTTTGACATATTCTGTTCATGTAACAGGCCAAGTTA
+SRR14585604.19092 A00805:41:HMJJWDRXX:1:1101:15329:20181 length=101
:::FF:F,:F,FF:F:FFF:FFF,FFFFF,FF:FFF,F:F:,F,:FFF:FF:FF:F,F,::F::FF,FF,,:F,F,FFF,FFF:,,FFFFF,F:FF,FF,F
		 */
		FastqRecord rec = new FastqRecord("SRR14585604.19092 A00805:41:HMJJWDRXX:1:1101:15329:20181 length=101",
				"TGCATTGTGTCAAAAGAAATTTCCTTATTTTCTACTGCCATTCCCATAAAAGTAAGTAGTCTCATTTTTGACATATTCTGTTCATGTAACAGGCCAAGTTA",
				"SRR14585604.19092 A00805:41:HMJJWDRXX:1:1101:15329:20181 length=101",
				":::FF:F,:F,FF:F:FFF:FFF,FFFFF,FF:FFF,F:F:,F,:FFF:FF:FF:F,F,::F::FF,FF,,:F,F,FFF,FFF:,,FFFFF,F:FF,FF,F");

		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("A00805").intValue());
		assertEquals(1, report.flowCellIds.get("HMJJWDRXX").intValue());
		assertEquals(1, report.flowCellLanes.get("1").intValue());
		assertEquals(1, report.tileNumbers.get(1101).intValue());
		assertEquals(0, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}

	@Test
	public void parseHeaderSingleSpace() {
		/*
		@VH01336:23:AAC37HWHV:1:1101:18459:1000 1:N:0:GGGGGGGG+AGATCTCG
GTCCAGTTGCATTTTAGTAAGCTCTTTTTGATTCTCAAATCCGGCGTCAACCATACCAGCAGAGGAAGCATCAGCACCAGCACGCTCCCAAGCATTAAGCT
+
CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC;CCCCCCCCC;;CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC-CCCCCCCC
		 */
		FastqRecord rec = new FastqRecord("VH01336:23:AAC37HWHV:1:1101:18459:1000 1:N:0:GGGGGGGG+AGATCTCG",
				"GTCCAGTTGCATTTTAGTAAGCTCTTTTTGATTCTCAAATCCGGCGTCAACCATACCAGCAGAGGAAGCATCAGCACCAGCACGCTCCCAAGCATTAAGCT",
				"",
				"CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC;CCCCCCCCC;;CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC-CCCCCCCC");

		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("VH01336").intValue());
		assertEquals(1, report.flowCellIds.get("AAC37HWHV").intValue());
		assertEquals(1, report.flowCellLanes.get("1").intValue());
		assertEquals(1, report.tileNumbers.get(1101).intValue());
		assertEquals(1, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}

	@Test
	public void parseHeaderNoSpace() {
		/*
		@HWI-ST590:2:1201:12570:134058#0
AATAGTCCTAACGTTCTACATAACTTCAAGTAGTAAAATTCACCATCCTCT
+
:BC8?ABCEBEB9CEBFB@BC;>BFD=DE?B;@DBDED?DCD?BDDDBBBB
		 */
		FastqRecord rec = new FastqRecord("HWI-ST590:2:1201:12570:134058#0",
				"AATAGTCCTAACGTTCTACATAACTTCAAGTAGTAAAATTCACCATCCTCT",
				"",
				":BC8?ABCEBEB9CEBFB@BC;>BFD=DE?B;@DBDED?DCD?BDDDBBBB");

		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertEquals(1, report.instruments.get("HWI-ST590").intValue());
		assertTrue(report.flowCellIds.isEmpty());
		assertEquals(1, report.flowCellLanes.get("2").intValue());
		assertEquals(1, report.tileNumbers.get(1201).intValue());
		assertEquals(0, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}
	@Test
	public void parseHeaderNoSpace2() {
		/*
		@V350046278L1C001R00100004433/2
CGCTGAAAATTGAAAGCCCGCTTGGGATAAGTGACATTAAGAACTGGCACCGACTGCAGAACCGCAATTTCCAGTTGACGCTAAGTGGGGGCTTATTTAGCACCCAGCTCTGTTTGCCAACACCCCCTGGGCATGAGAGCTCCCCAAGGG
+
HGGCGEE<DDDH<EAHBFGGGDBHHBB;C:HCGBEEBA@8HEDGAFGECFGG,1BH?@G)-C@EB?D/C6>GDHBBF(EH:7>>G@GH?G?F@?6<CB?B=DEC:>>CBE?G???BBG<.F:E?CFD?@?A:#5E>5BE/>BFFD+$E,>
		 */
		FastqRecord rec = new FastqRecord("V350046278L1C001R00100004433/2",
				"CGCTGAAAATTGAAAGCCCGCTTGGGATAAGTGACATTAAGAACTGGCACCGACTGCAGAACCGCAATTTCCAGTTGACGCTAAGTGGGGGCTTATTTAGCACCCAGCTCTGTTTGCCAACACCCCCTGGGCATGAGAGCTCCCCAAGGG",
				"",
				"HGGCGEE<DDDH<EAHBFGGGDBHHBB;C:HCGBEEBA@8HEDGAFGECFGG,1BH?@G)-C@EB?D/C6>GDHBBF(EH:7>>G@GH?G?F@?6<CB?B=DEC:>>CBE?G???BBG<.F:E?CFD?@?A:#5E>5BE/>BFFD+$E,>");

		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec);
		assertEquals(1, report.getRecordsParsed());
		assertTrue(report.instruments.isEmpty());
		assertTrue(report.flowCellIds.isEmpty());
		assertTrue(report.flowCellLanes.isEmpty());
		assertTrue(report.tileNumbers.isEmpty());
		assertEquals(0, report.firstInPair.intValue());
		assertEquals(0, report.secondInPair.intValue());
	}
}
