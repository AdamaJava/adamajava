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
package org.qcmg.qprofiler2;

import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.FileUtils;
import org.w3c.dom.Element;

public abstract class SummaryReport {
	
	private final AtomicLong recordsInputed = new AtomicLong();
	
	private String startTime;
	private String finishTime;
	private String fileName;
	private String fileMd5;
	
	protected QLogger logger;
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public SummaryReport() {
		logger = QLoggerFactory.getLogger(getClass());
	}

	public long getRecordsInputed() {
		return recordsInputed.get();
	}
 	public void updateRecordsInputed() {
		recordsInputed.incrementAndGet();
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public void setFinishTime(String finishTime) {
		this.finishTime = finishTime;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void setFileMd5() {	
		if(this.fileMd5 != null) {
			logger.warn("file md5 value is already caculated, please check why check again!!! ");
			return; 
		}
		
		logger.info("waiting for md5 checksum for input file to finish (max wait will be 20 hours)");
		this.fileMd5 =  FileUtils.getFileCheckSum(this.fileName);
		logger.info("md5 checksum is done!");		
	}	
	
	protected Element init(Element parent, ProfileType reportType, Long noOfDuplicates,  Long maxRecords) {
		Element element = parent.getOwnerDocument().createElement(reportType.getReportName() + "Report");
		parent.appendChild(element);
				
		element.setAttribute( "uuid", QExec.createUUid() ); //add uuid 
		element.setAttribute("file", this.fileName);

		//xml reorganise
		element.setAttribute("startTime", this.startTime );
		element.setAttribute("finishTime", this.finishTime );
		
		if(this.fileMd5 == null) { setFileMd5(); }
		element.setAttribute("md5sum",this.fileMd5);
				
		//don't list records_parsed on xml for BAM type
		if(!reportType.equals(ProfileType.BAM)) {
			element.setAttribute("records_parsed", String.format("%,d", getRecordsInputed()) );	
		}
				
		if (null != maxRecords) {
			element.setAttribute("max_no_of_records", String.format("%,d",maxRecords) );
		}
		if (null != noOfDuplicates) {
			element.setAttribute("duplicate_records", String.format("%,d", noOfDuplicates));
		}
		
		return element;
	}
		
	protected Element init(Element parent, ProfileType reportType) {
		return init(parent, reportType, null, null);
	}
		
	public abstract void toXml( Element parent );

}
