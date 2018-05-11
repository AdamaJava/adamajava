package org.qcmg.qprofiler2.bam;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.TagSummaryReport;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;
import junit.framework.Assert;

public class TagSummaryReportTest {

	final  SAMTagUtil STU = SAMTagUtil.getSingleton();
	
	@Test
	public void simpleTest() throws Exception{
		
		TagSummaryReport report = new TagSummaryReport( null, null, null);
		SAMRecord record = new SAMRecord(null);
		record.setReadName("TESTDATA");
		
		//first read
		record.setReadBases("ACCCT AACCC CAACC CTAAC CNTAA CCCTA ACCCA AC".replace(" ","" ).getBytes());
		record.setFlags(67); 	//first of pair forward
		record.setAttribute("MD", "A25A");  //(ref) A>C (base) two
		record.setAttribute("RG", "first");
		record.setCigarString("10S27M");		
		report.parseTAGs(record);
		
		//second read
		record.setReadBases("ACCCT AACCC".replace(" ","" ).getBytes());
		record.setCigarString("10M");
		record.setAttribute("MD", "T9");  //(ref) T>A (Base)
		report.parseTAGs(record);
		
		//third read
		record.setReadNegativeStrandFlag(true);  // ref A>T (base)
		report.parseTAGs(record);
		
		//forth read
		record.setReadBases("ACCCT AACCC A".replace(" ","" ).getBytes());
		record.setAttribute("RG", "last");
		report.parseTAGs(record);
		
		Element root = createRootElement("ROOT");			
		report.toXml(root);		
		checkXml( root );
	}	
	
	private void checkXml(Element root){
		Element tagElement = QprofilerXmlUtils.getChildElement(  QprofilerXmlUtils.getChildElement( root, "TAG",0 ), "RG",0 );	
		//valueTally
		Element element = QprofilerXmlUtils.getChildElement(   tagElement,  QprofilerXmlUtils.valueTally,0 ); 
		Assert.assertEquals(element.getAttribute( QprofilerXmlUtils.totalCount), "4");
		//TallyItem first
		element = QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0 ); 
		Assert.assertEquals(element.getAttribute( QprofilerXmlUtils.count), "3");
		Assert.assertEquals(element.getAttribute( QprofilerXmlUtils.value), "first");
		Assert.assertEquals(element.getAttribute( QprofilerXmlUtils.percent), "75.0%");
				
		//MD->MismatchByCycle
		tagElement = QprofilerXmlUtils.getChildElement(  QprofilerXmlUtils.getChildElement( tagElement, "MD",0 ), "RG",0 );	
		tagElement = QprofilerXmlUtils.getChildElement(  QprofilerXmlUtils.getChildElement( root, "TAG",0 ), "MD",0 );	
		element = QprofilerXmlUtils.getChildElement( tagElement, "MismatchByCycle",0 );
		Assert.assertEquals(1, QprofilerXmlUtils.getChildElementByTagName( element,  QprofilerXmlUtils.cycleTally).size() );	
		//<CycleTally>
		element = QprofilerXmlUtils.getChildElement( element, "CycleTally",0 );
		Assert.assertEquals( element.getAttribute( "source"), QprofilerXmlUtils.FirstOfPair );
		Assert.assertEquals( element.getAttribute( QprofilerXmlUtils.possibles), "A,C,T" );
		Assert.assertEquals( 4, QprofilerXmlUtils.getChildElementByTagName( element, "Cycle").size() );		
		Assert.assertEquals("1,0,0", QprofilerXmlUtils.getChildElement( element, "Cycle",0 ).getAttribute(QprofilerXmlUtils.counts));
		Assert.assertEquals("25.0%", QprofilerXmlUtils.getChildElement( element, "Cycle",1 ).getAttribute(QprofilerXmlUtils.percent));
		Assert.assertEquals("11", QprofilerXmlUtils.getChildElement( element, "Cycle",2).getAttribute(QprofilerXmlUtils.value));
		Assert.assertEquals(QprofilerXmlUtils.getChildElement( element, "Cycle",3).getAttribute(QprofilerXmlUtils.counts), QprofilerXmlUtils.getChildElement( element, "Cycle",2).getAttribute(QprofilerXmlUtils.counts));
					
		//MD->MutationForward
		element = QprofilerXmlUtils.getChildElement(  tagElement,  "MutationForward",0 );		
		Assert.assertEquals( 1, QprofilerXmlUtils.getChildElementByTagName( element, QprofilerXmlUtils.valueTally).size() );	
		element = QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.valueTally, 0 );
		Assert.assertEquals( element.getAttribute( "source"), QprofilerXmlUtils.FirstOfPair );
		Assert.assertEquals("A>C", QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0).getAttribute(QprofilerXmlUtils.value));
		Assert.assertEquals("2", QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0).getAttribute(QprofilerXmlUtils.count));
		
		//MD->MutationReverse
		element = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(  tagElement,  "MutationReverse",0 ), QprofilerXmlUtils.valueTally, 0 );
		Assert.assertEquals("A>T", QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0).getAttribute(QprofilerXmlUtils.value));
		Assert.assertEquals("1", QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0).getAttribute(QprofilerXmlUtils.count));
		Assert.assertEquals("100.0%", QprofilerXmlUtils.getChildElement( element, QprofilerXmlUtils.tallyItem,0).getAttribute(QprofilerXmlUtils.percent));
	}
	
	/**
	 * create element for unit tests
	 * @param parentName
	 * @param childName
	 * @return
	 * @throws ParserConfigurationException
	 */
	private static Element createRootElement(String rootName) throws ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();		
		Document doc = domImpl.createDocument(null, rootName, null);
		Element root = doc.getDocumentElement();
		
		return root;
	}		
	
	@Ignore
	public void testTagPosition() {
		final  SAMTagUtil STU = SAMTagUtil.getSingleton();
		final short CS = STU.CS;
		final short CQ = STU.CQ;
		final short RG = STU.RG;
		final short CM = STU.CM;
		final short SM = STU.SM;
		final short NH = STU.NH;
		final short MD = STU.MD;
		final short IH = STU.IH;
		// custom tags
		final short ZM = STU.makeBinaryTag("ZM");
		final short ZP = STU.makeBinaryTag("ZP");
		final short ZF = STU.makeBinaryTag("ZF");
		
		short [] tags = {CS, CQ, RG, ZM, ZP, CM, ZF, SM, IH, NH, MD};
		
		System.out.println("current");
		for (short tag : tags) 
			System.out.println(STU.makeStringTag(tag) + " : " + tag);
		
		System.out.println("ordered");
		short [] orderedTags = {MD, ZF, RG, IH, NH,CM,SM,ZM,ZP, CQ, CS};
		
		for (short tag : orderedTags) 
			System.out.println(STU.makeStringTag(tag) + " : " + tag);
			
		
	}
}
