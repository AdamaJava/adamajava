package org.qcmg.qprofiler2.fastq;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.fastq.FastqSummaryReport;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.fastq.FastqRecord;

public class FastqSummaryReportTest {
	
	private final FastqRecord rec1 = new FastqRecord("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1", 
			"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA", 
			"+", 
			"<@@((#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
	
	private FastqRecord rec2 = new FastqRecord("@SRR001666.1 071112_SLXA-EAS1_s_7:5:1:817:345 length=36", 
			"GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGT", 
			"+", 
			"<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:");
	/**
	 * FastqRecord.getBaseQualities() return a arry of base quaity but the digt already -33
	 *  eg it return below for rec1
	 *  ## %%'''%'%%%$&#%!%%%&#(!"!%'($%'&&&"&'(('#'%'%$"#$$$$"""""" """"""""""!!!! ""
	 *  for(byte c : rec1.getBaseQualities() )System.out.print((char) (c+33) );
	 *  "<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C"
	 *  
	 */

	
	@Test
	@DisplayName("A special test case")
	public void qnameTest() throws Exception {
			
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec1);
		report.parseRecord(rec1);
		report.parseRecord(rec2);
		report.toXml(root);		
		assertEquals( 3, report.getRecordsParsed() );
		
		Element qnameE = QprofilerXmlUtils.getOffspringElementByTagName(root, QprofilerXmlUtils.qname ).get(0);		
		String[] names = new String[] { "InValidReadName", "INSTRUMENTS", "FLOW_CELL_IDS", "RUN_IDS", "FLOW_CELL_LANES", "TILE_NUMBERS", "PAIR_INFO" };
		String[] values = new String[] { "ReadNameInValid", "@ERR091788", "HSQ955", "155", "2", "1101","1" };
		for(int i = 0; i < names.length; i ++) {
			String name = names[i];
			Element ele = QprofilerXmlUtils.getOffspringElementByTagName(qnameE, XmlUtils.variableGroupEle).stream()
				.filter( e -> e.getAttribute(XmlUtils.Sname).equals( name )).findFirst().get();			
			assertEquals( 1, ele.getChildNodes().getLength());			
			if(i == 0)
				assertEquals( "1", ((Element) ele.getFirstChild()).getAttribute(XmlUtils.Scount)) ;				
			else 
				assertEquals( "2", ((Element) ele.getFirstChild()).getAttribute(XmlUtils.Scount)) ;
			assertEquals( values[i], ((Element) ele.getFirstChild()).getAttribute(XmlUtils.Svalue));
		}
	
	}
	

	@Test
	public void badQualTest() throws ParserConfigurationException {
				
		Element root = QprofilerXmlUtils.createRootElement( "root", null );	
		//below data from SummaryReportUtilsTest::testTallyQualScoresValid()
		//three records with 5badbase and one read with 0badbase which is the last read
		QCMGAtomicLongArray badQualCount = new QCMGAtomicLongArray(15);	 
		SummaryReportUtils.tallyQualScores( new byte[] { 1,1,1,1,1 }, badQualCount ); 						 
		SummaryReportUtils.tallyQualScores( new byte[] { 1,2,3,4,5 }, badQualCount ); 		
		SummaryReportUtils.tallyQualScores( new byte[] { 1,2,3,9,9,10,11,12,13,14,15 }, badQualCount);  	
		SummaryReportUtils.tallyQualScores( new byte[] { 10,11,12,13,14,15 }, badQualCount );
				
		XmlUtils.outputTallyGroup( root, FastqSummaryReport.badBaseNum, badQualCount.toMap(), true );			
		List<Element> tallys =  QprofilerXmlUtils.getOffspringElementByTagName( root, XmlUtils.Stally);
		assertEquals(tallys.size(), 2);
		for(Element tally: tallys)
			if(tally.getAttribute(XmlUtils.Svalue).equals( "0" ))
				assertEquals( tally.getAttribute(XmlUtils.Scount), "1" );
			else {
				assertEquals( tally.getAttribute(XmlUtils.Scount), "3" );
				assertEquals( tally.getAttribute(XmlUtils.Svalue), "5" );
			}
		
		//test on original data
		root = QprofilerXmlUtils.createRootElement( "root", null );
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord( rec1 );
		report.toXml( root );

		root = QprofilerXmlUtils.getOffspringElementByTagName( root, QprofilerXmlUtils.qual ).get(0);
		Element ele = QprofilerXmlUtils.getOffspringElementByTagName( root, XmlUtils.variableGroupEle ).stream()
			.filter( e ->e.getAttribute( XmlUtils.Sname ).equals( FastqSummaryReport.badBaseNum) ).findFirst().get();		 
		ele = QprofilerXmlUtils.getChildElement( ele, XmlUtils.Stally, 0 );		  
		assertEquals( ele.getAttribute( XmlUtils.Scount), "1" );
		assertEquals( ele.getAttribute( XmlUtils.Svalue), "3" );		
	}
	

	@Test
	public void checkReadLength() throws ParserConfigurationException {
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		FastqSummaryReport report = new FastqSummaryReport();
		report.parseRecord(rec1);
		report.parseRecord(rec1);
		report.parseRecord(rec2);
		report.toXml(root);		
		assertEquals( 3, report.getRecordsParsed() );
		
		String[] names = new String[] {QprofilerXmlUtils.seqLength, QprofilerXmlUtils.qualLength };
		for(String name : names) {			
			Element ele = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.variableGroupEle).stream()
					.filter( e ->e.getAttribute(XmlUtils.Sname).equals(  name ) ).findFirst().get();				 
			assertEquals(2, ele.getChildNodes().getLength());			
			for(int i = 0; i < 2; i ++) {
				Element tallyE = (Element) ele.getChildNodes().item(i);
				if(tallyE.getAttribute(XmlUtils.Svalue).equals("97"))
					assertEquals("1", tallyE.getAttribute(XmlUtils.Scount));
				else {
					assertEquals("2", tallyE.getAttribute(XmlUtils.Scount));
					assertEquals("100", tallyE.getAttribute(XmlUtils.Svalue));
				}			
			}			
		}
	}
	
	
}
















