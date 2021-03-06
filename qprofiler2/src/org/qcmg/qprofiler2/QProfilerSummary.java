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

package org.qcmg.qprofiler2;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

public class QProfilerSummary {
	private final List<SummaryReport> reports = new ArrayList<SummaryReport>();
	private String startTime;
	private String finishTime;
	
	/**
	 * Sets startTime to the supplied String (I know...)
	 * 
	 * @param startTime String containing the time the reports started processing
	 */
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Gets startTime.
	 * 
	 * @return execution start time for this report
	 */
	public String getStartTime() {
		return startTime;
	}

	/**
	 * Sets finishTime to the supplied String (I know...)
	 * 
	 * @param finishTime String containing the time the reports finished processing
	 */
	public void setFinishTime(String finishTime) {
		this.finishTime = finishTime;
	}
	
	/**
	 * Gets the finishTime.
	 * 
	 * @return execution finish time for this report
	 */
	public String getFinishTime() {
		return finishTime;
	}

	/**
	 * Add SummaryReport objects to the reports collection.
	 * 
	 * @param summaries List of SummaryReport objects to be added to the reports collection
	 */
	public void addSummaries( List<SummaryReport> summaries ) {
		if (null != summaries) {
			reports.addAll(summaries);
		}
	}
	
	/**
	 * Loops through all summaryReports in the reports collection and calls <code>SummaryReport.toXml</code> on them.
	 * 
	 * @param parent Element parent element that is passed to the subsequent <code>SummartReport.toXml</code> calls
	 * @see  org.qcmg.qprofiler2.SummaryReport#toXml(Element)
	 */
	public void toXml( Element parent ) {
		for (SummaryReport report : reports) {
			if ( null != report ) {
				report.toXml( parent );
			}
		}
	}
}
