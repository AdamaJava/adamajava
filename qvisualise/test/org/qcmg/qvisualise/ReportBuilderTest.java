package org.qcmg.qvisualise;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.qvisualise.report.Report;
import org.qcmg.qvisualise.report.ReportBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class ReportBuilderTest {
	Document doc;
	Element element;
	
	@Before
	public void setup() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		doc = builder.newDocument();
		 // create the root element node
		element = doc.createElement("root");
        doc.appendChild(element);
	}
	
	@After
	public void teatDown() {
		element = null;
		doc = null;
	}
	
	@Test
	public void createSEQNoKmer() throws ParserConfigurationException, SAXException, IOException {
		
		String xml = "<qProfiler><SEQ></SEQ></qProfiler>";
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
//	    NodeList nodes = doc.getElementsByTagName("SEQ");
		
		
//		element.appendChild(doc.createElement("SEQ"));
		Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		
		
	}
	@Test
	public void createSEQ1Kmer() throws ParserConfigurationException, SAXException, IOException {
		String xml = "<qProfiler>"
				+ "<SEQ>"
				+ "<mers1>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers1>"
				+ "</SEQ>"
				+ "</qProfiler>";
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
	    Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		assertEquals(3, r.getTabs().get(0).getChildren().size());
		assertEquals("kmer_1", r.getTabs().get(0).getChildren().get(2).getName());
	}
	
	@Test
	public void createSEQ2Kmer() throws ParserConfigurationException, SAXException, IOException {
		
		String xml = "<qProfiler>"
				+ "<SEQ>"
				+ "<mers1>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers1>"
				+ "<mers2>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers2>"
				+ "</SEQ>"
				+ "</qProfiler>";
		
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
	    Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		assertEquals(4, r.getTabs().get(0).getChildren().size());
		assertEquals("kmer_1", r.getTabs().get(0).getChildren().get(2).getName());
		assertEquals("kmer_2", r.getTabs().get(0).getChildren().get(3).getName());
	}
	
	@Test
	public void createSEQ3Kmer() throws ParserConfigurationException, SAXException, IOException {
		
		String xml = "<qProfiler>"
				+ "<SEQ>"
				+ "<mers1>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers1>"
				+ "<mers2>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers2>"
				+ "<mers3>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers3>"
				+ "</SEQ>"
				+ "</qProfiler>";
		
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
	    Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		assertEquals(5, r.getTabs().get(0).getChildren().size());
		assertEquals("kmer_1", r.getTabs().get(0).getChildren().get(2).getName());
		assertEquals("kmer_2", r.getTabs().get(0).getChildren().get(3).getName());
		assertEquals("kmer_3", r.getTabs().get(0).getChildren().get(4).getName());
	}
	
	@Test
	public void createSEQ4Kmer() throws ParserConfigurationException, SAXException, IOException {
		
		String xml = "<qProfiler>"
				+ "<SEQ>"
				+ "<mers1>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers1>"
				+ "<mers2>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers2>"
				+ "<mers3>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers3>"
				+ "<mers4>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers4>"
				+ "</SEQ>"
				+ "</qProfiler>";
		
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
	    Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		assertEquals(5, r.getTabs().get(0).getChildren().size());
		assertEquals("kmer_1", r.getTabs().get(0).getChildren().get(2).getName());
		assertEquals("kmer_2", r.getTabs().get(0).getChildren().get(3).getName());
		assertEquals("kmer_3", r.getTabs().get(0).getChildren().get(4).getName());
	}
	
	@Test
	public void createSEQ6Kmer() throws ParserConfigurationException, SAXException, IOException {
		
		String xml = "<qProfiler>"
				+ "<SEQ>"
				+ "<mers1>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers1>"
				+ "<mers2>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers2>"
				+ "<mers3>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers3>"
				+ "<mers6>"
				+ "<CycleTally possibleValues=\"A,T,G,C\">" 
				+ "<Cycle counts=\"8719895,7992205,4253150,5140426\" value=\"0\"/>"
				+ "</CycleTally>"
				+ "</mers6>"
				+ "</SEQ>"
				+ "</qProfiler>";
		
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xml));
	    Document doc = db.parse(is);
	    Element e = doc.getDocumentElement();
	    Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(e, r);
		assertEquals(1, r.getTabs().size());
		assertEquals(6, r.getTabs().get(0).getChildren().size());
		assertEquals("kmer_1", r.getTabs().get(0).getChildren().get(2).getName());
		assertEquals("kmer_2", r.getTabs().get(0).getChildren().get(3).getName());
		assertEquals("kmer_3", r.getTabs().get(0).getChildren().get(4).getName());
		assertEquals("kmer_6", r.getTabs().get(0).getChildren().get(5).getName());
	}

}
