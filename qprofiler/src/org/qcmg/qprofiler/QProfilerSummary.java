/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
/**
 * Top-level data container class for SolValidateGff application.
 * 
 * @author jpearson
 * @version $Id: SolSummary.java,v 1.3 2009/08/26 22:45:52 jpearson Exp $
 * 
 */

package org.qcmg.qprofiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.qcmg.qprofiler.report.SummaryReport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class QProfilerSummary {
	private final List<SummaryReport> reports = new ArrayList<SummaryReport>();
	private String startTime;
	private String finishTime;
	
	/**
	 * Sets startTime to the supplied String (I know...)
	 * @param startTime String containing the time the reports started processing
	 */
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	/**
	 * Gets startTime.
	 * @return execution start time for this report
	 */
	public String getStartTime() {
		return startTime;
	}

	/**
	 * Sets finishTime to the supplied String (I know...)
	 * @param finishTime String containing the time the reports finished processing
	 */
	public void setFinishTime(String finishTime) {
		this.finishTime = finishTime;
	}
	/**
	 * Gets the finishTime
	 * @return execution finish time for this report
	 */
	public String getFinishTime() {
		return finishTime;
	}

	/**
	 * Add SummaryReport objects to the reports collection
	 * 
	 * @param summaries List of SummaryReport objects to be added to the reports collection
	 */
	public void addSummaries( List<SummaryReport> summaries ) {
		if (null != summaries)
			reports.addAll(summaries);
	}
	
	/**
	 * Loops through all summaryReports in the reports collection and calls <code>SummaryReport.toXml</code> on them.
	 * 
	 * @param parent Element parent element that is passed to the subsequent <code>SummartReport.toXml</code> calls
	 * @see  org.qcmg.qprofiler.report.SummaryReport#toXml(Element)
	 */
	public void toXml( Element parent ) {
		for (SummaryReport report : reports) {
			if (null != report)
				report.toXml(parent);
		}
	}
	
	/**
	 * Writes the contents of the SummaryReports contained in the reports collection to a file
	 * 
	 * @param parent Element parent node that all SummaryReports are added to
	 * @param filename String representing the output file name 
	 * @throws Exception thrown if problems occur creating the output file and transforming the xml into the file
	 */
	public void asXmlText( Element parent, String filename ) throws Exception {		
		// Reading an XML file
		Document doc = parent.getOwnerDocument();		
		File xmlFile = new File( filename );

		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( OutputKeys.ENCODING, "ISO-8859-1" );
		
		DOMSource source = new DOMSource( doc );
		StreamResult sr = new StreamResult( xmlFile );
		transformer.transform( source, sr );
	}
	
}
