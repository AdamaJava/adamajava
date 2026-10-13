package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;

public class TagSummaryReportTest {
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();	

	@Test
	public void longReadsSkipPerCycleMdCountsAndKeepSubstitutionTotals() throws Exception {
		TagSummaryReport report = new TagSummaryReport(true);
		assertEquals(0, report.allReadsLineLengths.length);
		assertEquals(0, report.tagMDMismatchByCycle.length);
		byte[] bases = new byte[125000];
		Arrays.fill(bases, (byte) 'A');
		SAMRecord record = new SAMRecord(null);
		record.setReadName("LONG_READ");
		record.setReadBases(bases);
		record.setCigarString(bases.length + "M");
		record.setAttribute("MD", "0C" + (bases.length - 1));
		for (int order = 0; order < 3; order++) {
			record.setFlags(order == 0 ? 0 : order == 1 ? 65 : 129);
			report.parseTAGs(record, true);
			record.setReadNegativeStrandFlag(true);
			report.parseTAGs(record, true);
		}

		Element root = XmlElementUtils.createRootElement(XmlUtils.TAG, null);
		report.toXml(root);
		Element mdMetric = getChildNameIs(root, XmlUtils.SEQUENCE_METRICS, "tags:MD:Z").getFirst();
		assertEquals("6", mdMetric.getAttribute(ReadGroupSummary.READ_COUNT));
		assertEquals(0, XmlElementUtils.getOffspringElementByTagName(mdMetric, XmlUtils.BASE_CYCLE).size());
		for (int order = 0; order < 3; order++) {
			String name = BamSummaryReport.sourceName.get(order);
			assertEquals(0, getChildNameIs(mdMetric, XmlUtils.VARIABLE_GROUP, name).size());
			for (String strand : new String[] {"ForwardStrand", "ReverseStrand"}) {
				Element group = getChildNameIs(mdMetric, XmlUtils.VARIABLE_GROUP, name + strand).getFirst();
				List<Element> tallies = XmlElementUtils.getOffspringElementByTagName(group, XmlUtils.TALLY);
				assertEquals(1, tallies.size());
				assertEquals(strand.equals("ForwardStrand") ? "C>A" : "G>T", tallies.getFirst().getAttribute(XmlUtils.VALUE));
				assertEquals("1", tallies.getFirst().getAttribute(XmlUtils.COUNT));
			}
		}
	}

	@Test
	public void shortReadsKeepPerCycleMdDenominators() {
		TagSummaryReport report = new TagSummaryReport();
		SAMRecord record = new SAMRecord(null);
		record.setReadName("SHORT_READ");
		record.setReadString("AAAAA");
		record.setCigarString("5M");
		record.setAttribute("MD", "0C4");
		report.parseTAGs(record, false);
		report.getAllReadsLineLengths();
		for (int cycle = 1; cycle <= 5; cycle++) {
			assertEquals(1, report.allReadsLineLengths[0].get(cycle));
		}
		assertEquals(5, report.allReadsLineLengths[0].getSum());
		assertEquals(1, report.tagMDMismatchByCycle[0].count(1, 'A'));
	}

	@Test
	public void mdDenominatorsHandleMixedLengthsAndRepeatedOutput() throws Exception {
		TagSummaryReport report = new TagSummaryReport();
		SAMRecord record = new SAMRecord(null);
		record.setReadName("MIXED_LENGTHS");
		for (int order = 0; order < 3; order++) {
			record.setFlags(order == 0 ? 0 : order == 1 ? 65 : 129);
			for (int length : new int[] {0, 2, 5, 5}) {
				record.setReadString("A".repeat(length));
				record.setCigarString(length == 0 ? "*" : length + "M");
				record.setAttribute("MD", Integer.toString(length));
				report.parseTAGs(record, false);
			}
			// Reads without MD must not contribute to mismatch denominators.
			record.setReadString("A".repeat(10));
			record.setCigarString("10M");
			record.setAttribute("MD", null);
			report.parseTAGs(record, false);
		}
		for (int output = 0; output < 2; output++) {
			report.toXml(XmlElementUtils.createRootElement(XmlUtils.TAG, null));
			for (int order = 0; order < 3; order++) {
				assertEquals(0, report.allReadsLineLengths[order].get(0));
				for (int cycle = 1; cycle <= 10; cycle++) {
					assertEquals(cycle <= 2 ? 3 : cycle <= 5 ? 2 : 0, report.allReadsLineLengths[order].get(cycle));
				}
				assertEquals(12, report.allReadsLineLengths[order].getSum());
			}
		}
	}

	@Test
	public void concurrentTagTalliesKeepTypesAndDenominatorsSeparate() throws Exception {
		TagSummaryReport report = new TagSummaryReport();
		List<Callable<Void>> workers = new ArrayList<>();
		for (int worker = 0; worker < 4; worker++) {
			final boolean integerTag = worker % 2 == 0;
			workers.add(() -> {
				SAMRecord record = new SAMRecord(null);
				record.setReadString("AAAAA");
				record.setCigarString("5M");
				record.setAttribute("MD", "5");
				record.setAttribute("RG", "shared");
				record.setAttribute("NM", integerTag ? (Object) 1 : "1");
				for (int i = 0; i < 1000; i++) {
					report.parseTAGs(record, false);
				}
				return null;
			});
		}
		try (var executor = Executors.newFixedThreadPool(4)) {
			for (var result : executor.invokeAll(workers)) {
				result.get();
			}
		}
		Element root = XmlElementUtils.createRootElement(XmlUtils.TAG, null);
		report.toXml(root);
		for (String tag : new String[] {"RG:Z", "NM:i", "NM:Z"}) {
			Element metric = getChildNameIs(root, XmlUtils.SEQUENCE_METRICS, "tags:" + tag).getFirst();
			String expected = tag.startsWith("RG") ? "4000" : "2000";
			assertEquals(expected, metric.getAttribute(ReadGroupSummary.READ_COUNT));
			List<Element> tallies = XmlElementUtils.getOffspringElementByTagName(metric, XmlUtils.TALLY);
			assertEquals(1, tallies.size());
			assertEquals(expected, tallies.getFirst().getAttribute(XmlUtils.COUNT));
		}
		for (int cycle = 1; cycle <= 5; cycle++) {
			assertEquals(4000, report.allReadsLineLengths[0].get(cycle));
		}
	}
	
	@Test
	public void simpleTest() throws Exception {
		
		TagSummaryReport report = new TagSummaryReport();
		SAMRecord record = new SAMRecord(null);
		record.setReadName("TESTDATA");
		
		// first read
		record.setReadBases("ACCCT AACCC CAACC CTAAC CNTAA CCCTA ACCCA AC".replace(" ","" ).getBytes());
		record.setFlags(67); 	// first of pair forward
		record.setAttribute("MD", "A25A"); // (ref) A>C (base) in cycle 11 and 37
		record.setAttribute("RG", "first");
		record.setCigarString("10S27M");		
		report.parseTAGs(record, false);
		
		// second read
		record.setReadBases("ACCCT AACCC".replace(" ","" ).getBytes());
		record.setCigarString( "10M" );
		record.setAttribute( "MD", "T9" ); // ref T>A (Base) in cycle 1
		report.parseTAGs(record, false);
		
		// third read
		record.setReadNegativeStrandFlag(true); // ref A>T (base) in cycle 10 according to reverse
		report.parseTAGs(record, false);
		
		// forth read invalid cigar so mutation and MD are ignored
		record.setReadBases("ACCCT AACCC A".replace(" ","" ).getBytes());
		record.setAttribute("RG", "last");
		report.parseTAGs(record, false);
		
		// check md counts
		Element root = XmlElementUtils.createRootElement( XmlUtils.TAG, null );
		report.toXml( root );		
		checkXml( root );				
	}	
		
	private List<Element> getChildNameIs(Element parent, String eleName, String nameValue) {		
		return XmlElementUtils.getChildElementByTagName(parent,eleName).stream().
			filter( e -> e.getAttribute( XmlUtils.NAME ).equals( nameValue ) ).collect(Collectors.toList());		
	}
	
	private void checkXml(Element root) {
		 		
		assertEquals( 2, XmlElementUtils.getChildElementByTagName( root, XmlUtils.SEQUENCE_METRICS ).size()  );				
		
		// <sequenceMetrics name="tags:MDM:Z">
		Element metricE = getChildNameIs( root, XmlUtils.SEQUENCE_METRICS, "tags:MD:Z" ).getFirst();
		assertEquals( metricE.getChildNodes().getLength() , 3 );
		
		// check mutation on each base cycle
		Element ele = getChildNameIs( metricE, XmlUtils.VARIABLE_GROUP, XmlUtils.FIRST_PAIR ).getFirst();
		assertEquals(ele.getAttribute(ReadGroupSummary.READ_COUNT), "4");
		// three of firstOfPair have four mutation base
		String[] values = new String[] {"A", "T", "C", "C"};
		String[] counts =  new String[] {"1", "10", "11", "37"};
		for (int i = 0; i < counts.length; i++ ) {			
			String count = counts[i];
			Element vE = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.BASE_CYCLE).stream().
				filter( e -> e.getAttribute( XmlUtils.CYCLE ).equals(count)  ).findFirst().get();
			assertEquals( vE.getChildNodes().getLength() , 1 );
			vE = (Element) vE.getChildNodes().item(0);
			assertEquals( vE.getAttribute(XmlUtils.VALUE), values[i]);
			assertEquals( vE.getAttribute(XmlUtils.COUNT), "1");
		}
		
		// check mutation type on forward reads
		ele = getChildNameIs(metricE, XmlUtils.VARIABLE_GROUP, XmlUtils.FIRST_PAIR+"ForwardStrand" ).getFirst();
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).stream()
			.filter(e -> e.getAttribute(XmlUtils.VALUE).equals("A>C") && e.getAttribute(XmlUtils.COUNT).equals("2") ).count() );
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).stream()
			.filter(e -> e.getAttribute(XmlUtils.VALUE).equals("T>A") && e.getAttribute(XmlUtils.COUNT).equals("1") ).count() );		
		
		// check mutation type on reverse reads
		ele = getChildNameIs( metricE, XmlUtils.VARIABLE_GROUP, XmlUtils.FIRST_PAIR+"ReverseStrand" ).getFirst();
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).size());
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).stream()
				.filter(e -> e.getAttribute(XmlUtils.VALUE).equals("A>T") && e.getAttribute(XmlUtils.COUNT).equals("1") ).count() );
		
		// check tag RG
		ele = getChildNameIs( root, XmlUtils.SEQUENCE_METRICS, "tags:RG:Z" ).getFirst();
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).stream()
				.filter(e -> e.getAttribute(XmlUtils.VALUE).equals("first") && e.getAttribute(XmlUtils.COUNT).equals("3") ).count() );
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY).stream()
				.filter(e -> e.getAttribute(XmlUtils.VALUE).equals("last") && e.getAttribute(XmlUtils.COUNT).equals("1") ).count() );
	}

	private void createMDerrFile(String input) throws IOException {
		List<String> data = new ArrayList<>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
	
	    data.add("HWI-ST1408:8	147	chrM	3085	255	28S96M2S	=	3021	-160	" + 
	    "CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN	" + 
	    "########################################################################F==#F==#EGGGGGGE<=#FE1GGGGGGGGGGGGGF=?#GGGE@=#E@?#<=##	" + 
	    "MD:Z:1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0N0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A1G0A3G3A10G19T6T3T2	NH:i:1	HI:i:1	NM:i:47	AS:i:169	RG:Z:1959T");
	
		try(BufferedWriter out = new BufferedWriter(new FileWriter(input))) {  
			for (String line : data) {
				out.write(line + "\n");             
			}
		}	
	}
	
	@Test
	public void tempTest() throws Exception {
		String INPUT_FILE = testFolder.newFile("input.sam").getAbsolutePath();
		createMDerrFile(INPUT_FILE);	
		Element root = XmlElementUtils.createRootElement("root",null);
		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(INPUT_FILE); 
		sr.toXml(root);	 
	}	
	
	@Test
	public void toXmlTest() throws ParserConfigurationException {
		final  SAMTagUtil STU = SAMTagUtil.getSingleton();
		TagSummaryReport report = new TagSummaryReport();
		SAMRecord record = new SAMRecord(null);
		record.setReadName("TESTDATA");
		record.setAttribute(STU.makeStringTag(STU.NM), Integer.MAX_VALUE);
		report.parseTAGs(record, false);
		
		for (int i = 0; i < 200; i++) {	
			for (int j = 0; j < 2; j ++) {
				record.setAttribute(STU.makeStringTag(STU.NM), i + j);
				report.parseTAGs(record, false);
			}			
			record.setAttribute(STU.makeStringTag(STU.NM), i + "");
			report.parseTAGs(record, false);
		}		
		
		Element root = XmlElementUtils.createRootElement( XmlUtils.TAG, null );
		report.toXml( root );	
		
		Element ele = getChildNameIs( getChildNameIs( root, XmlUtils.SEQUENCE_METRICS, "tags:NM:Z" ).getFirst(), XmlUtils.VARIABLE_GROUP, "NM" ).getFirst();
		assertEquals( ele.getAttribute(XmlUtils.TALLY_COUNT) , TagSummaryReport.ADDI_TAG_MAP_LIMIT+"+" ); 
		assertEquals( ((Element)ele.getParentNode()).getAttribute(ReadGroupSummary.READ_COUNT) , "200" );		
		assertEquals( XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY).size(), 101);
		long findNo = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY).stream()
			.filter(e -> e.getAttribute( XmlUtils.VALUE ).equals(XmlUtils.OTHER ) && e.getAttribute( XmlUtils.COUNT ).equals("100" )).count() ;
		assertEquals(findNo, 1);
		
		
		ele = getChildNameIs( getChildNameIs( root, XmlUtils.SEQUENCE_METRICS, "tags:NM:i" ).getFirst(), XmlUtils.VARIABLE_GROUP, "NM" ).getFirst();
		assertEquals( ele.getAttribute(XmlUtils.TALLY_COUNT) , TagSummaryReport.ADDI_TAG_MAP_LIMIT+"+" ); 
		assertEquals( ((Element)ele.getParentNode()).getAttribute(ReadGroupSummary.READ_COUNT) , "401" );
		assertEquals( XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY).size(), 101);
		findNo = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY).stream()
				.filter(e -> e.getAttribute( XmlUtils.VALUE ).equals(XmlUtils.OTHER ) && e.getAttribute( XmlUtils.COUNT ).equals("203" )).count() ;
			assertEquals(findNo, 1);
		
	}
	
}
