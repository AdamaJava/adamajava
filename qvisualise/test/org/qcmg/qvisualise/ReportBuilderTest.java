package org.qcmg.qvisualise;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.qvisualise.report.Report;
import org.qcmg.qvisualise.report.ReportBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.Assert.assertEquals;

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
	public void createSEQ() {
		
		element.appendChild(doc.createElement("SEQ"));
		
		Report r = new Report(ProfileType.BAM, null, null, null);
		ReportBuilder.createSEQ(element, r);
		assertEquals(1, r.getTabs().size());
	}

}
