package org.qcmg.qprofiler2.bam;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.TagSummaryReport2;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;
import junit.framework.Assert;

public class TagSummaryReportTest {
	protected static final String INPUT_FILE = "input.sam";
	final  SAMTagUtil STU = SAMTagUtil.getSingleton();
	
	@Test
	public void simpleTest() throws Exception{
		
		TagSummaryReport2 report = new TagSummaryReport2();
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
		
		QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/unitTest.xml");
		
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
	
	private static void createMDerrFile() throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
	
	    data.add("HWI-ST1408:8	147	chrM	3085	255	28S96M2S	=	3021	-160	" + 
	    "CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN	" + 
	    "########################################################################F==#F==#EGGGGGGE<=#FE1GGGGGGGGGGGGGF=?#GGGE@=#E@?#<=##	" + 
	    "MD:Z:1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0N0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A1G0A3G3A10G19T6T3T2	NH:i:1	HI:i:1	NM:i:47	AS:i:169	RG:Z:1959T");
	
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT_FILE))){	    
			for (String line : data)  out.write(line + "\n");	               
		}	
	}
	
	@Test
	public void tempTest() throws Exception{
		createMDerrFile();	
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);	 
	}	
	
	
}
