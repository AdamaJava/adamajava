package org.qcmg.qvisualise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.qcmg.qvisualise.report.XmlReportReader;
import org.w3c.dom.Document;

public class XmlReportReaderTest {
	
	@Ignore
	public void testCreateDocumentFromFile() throws Exception {
		
		// null file
		try {
			XmlReportReader.createDocumentFromFile(null);
			Assert.fail("Should not have got this far");
		} catch (Exception e) {}
		
		// empty file
		Document doc = XmlReportReader.createDocumentFromFile(new File("/test"));
		Assert.assertNull(doc);
		
		// valid file
		File file = createTempTestFile();
		doc = XmlReportReader.createDocumentFromFile(file);
		Assert.assertNotNull(doc);
		
		// valid file containing DOCTYPE
		file = createTempTestFileContainingDoctype();
		doc = XmlReportReader.createDocumentFromFile(file);
		Assert.assertNotNull(doc);
	}
	
	private File createTempTestFile() throws IOException{
		File temp = File.createTempFile("XmlReportReaderTest_", null, new File("/tmp"));
		temp.deleteOnExit();
		
		BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		out.write("<xml></xml>");
		out.close();
		
		return temp;
	}
	
	private File createTempTestFileContainingDoctype() throws IOException{
		File temp = File.createTempFile("XmlReportReaderTest2_", null, new File("/tmp"));
		temp.deleteOnExit();
		
		BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>" +
			"<!DOCTYPE qProfiler SYSTEM \"http://abc.123.net/DTD/qProfiler_1_0.dtd\">");
		out.write("<qProfiler></qProfiler>");
		out.close();
		
		return temp;
	}

}
