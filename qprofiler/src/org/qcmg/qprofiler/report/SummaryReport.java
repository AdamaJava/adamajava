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
package org.qcmg.qprofiler.report;

import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ProfileType;
import org.w3c.dom.Element;

public abstract class SummaryReport {
	
	private final AtomicLong recordsParsed = new AtomicLong();
	
	private String startTime;
	private String finishTime;
	private String generatedBy;
	private String generatedByVersion;
	private String fileName;
	
	protected QLogger logger;
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public SummaryReport() {
		logger = QLoggerFactory.getLogger(getClass());
	}

	public long getRecordsParsed() {
		return recordsParsed.get();
	}
 	public void updateRecordsParsed() {
		recordsParsed.incrementAndGet();
	}

	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(String finishTime) {
		this.finishTime = finishTime;
	}

	public String getGeneratedBy() {
		return generatedBy;
	}
	public void setGeneratedBy(String generatedBy) {
		this.generatedBy = generatedBy;
	}

	public String getGeneratedByVersion() {
		return generatedByVersion;
	}
	public void setGeneratedByVersion(String generatedByVersion) {
		this.generatedByVersion = generatedByVersion;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
	}
	
	protected Element init(Element parent, ProfileType reportType, Long noOfDuplicates, String [] excludes, Long maxRecords) {
		Element element = parent.getOwnerDocument().createElement(reportType + "Report");
		parent.appendChild(element);
		
		element.setAttribute("file", getFileName());
		element.setAttribute("execution_started", getStartTime());
		element.setAttribute("execution_finished", getFinishTime());
		
		//don't list records_parsed on xml for BAM type
		if(!reportType.equals(ProfileType.BAM))
			element.setAttribute("records_parsed", String.format("%,d", getRecordsParsed()) );				 
		if (null != maxRecords)
			element.setAttribute("max_no_of_records", String.format("%,d",maxRecords) );
		if (null != noOfDuplicates)
			element.setAttribute("duplicate_records", String.format("%,d", noOfDuplicates));
		if (null != excludes && excludes.length > 0) {
			StringBuilder excludesString = new StringBuilder();
			for (String ex : excludes) {
				if (excludesString.length() > 0)
					excludesString.append(", ");
				excludesString.append(ex);
			}
			element.setAttribute("excludes", excludesString.toString());
		}			
		return element;
	}
	
	
	protected Element init(Element parent, ProfileType reportType) {
		return init(parent, reportType, null, null, null);
	}
	
	public static Element createSubElement(Element parent, String name) {
		Element element = parent.getOwnerDocument().createElement(name);
		parent.appendChild(element);
		return element;
	}
	
	public abstract void toXml( Element parent );
	public String toHTML() {return null;};
}
