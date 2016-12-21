/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlReportReader {
	
	private static final QLogger log = QLoggerFactory.getLogger(XmlReportReader.class);
	private static int ioExceptionCount;
	
	public static Document createDocumentFromFile(File absoluteFile)  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(absoluteFile);
			doc.getDocumentElement().normalize();
			log.info("qprofiler xml file encoding: " + doc.getXmlEncoding());
		} catch (ParserConfigurationException e1) {
			log.error("ParserConfigurationException caught in XmlReportReader.createDocumentFromFile", e1);
		} catch (SAXException se) {
			log.error("SAXException caught in XmlReportReader.createDocumentFromFile", se);
		} catch (IOException ioe) {
			log.error("IOException caught in XmlReportReader.createDocumentFromFile", ioe);
			
			// hack coming up - can't hit grimmond.imb.uq.edu from our clusters, so create a temp file that doesn't contain the reference...
			// try creating a temp file, without the <!DOCTYPE> line
			if (ioExceptionCount ++ < 1) {	// only want to do this once...
				try {
					return createDocumentFromFile(createTempFile(absoluteFile));
				} catch (IOException e) {
					log.error("IOException caught in XmlReportReader whilst creating temp file and running createDocumentFromFile", e);
				}
			}
		}
		
		return doc;
	}
	
	public static File createTempFile(File originalFile) throws IOException{
		
		log.info("creating temp file from contents of: "+ originalFile.getAbsoluteFile());
		File tempFile = File.createTempFile("qVisualise", null);	// will use .tmp extension
		tempFile.deleteOnExit();
		log.info("created temp file: "+ tempFile.getAbsoluteFile());
		
		BufferedReader in = new BufferedReader(new FileReader(originalFile));
		BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
		
		String originalLine = null;
		try {
			while ((originalLine = in.readLine()) != null) {
				if (originalLine.startsWith("<!DOCTYPE")) continue;
				out.write(originalLine);
			}
		} finally {
			try {
				in.close();
			} finally {
				out.close();
			}
		}
		
		return  tempFile;
	}
	
}
