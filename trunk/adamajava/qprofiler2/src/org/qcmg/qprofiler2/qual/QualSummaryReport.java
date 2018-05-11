/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.qual;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.record.SimpleRecord;
import org.w3c.dom.Element;

public class QualSummaryReport extends SummaryReport {
	@Deprecated
	private String [] excludes;
	private boolean excludeAll;
	private SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>(50,40);
	private ConcurrentMap<Integer, AtomicLong> qualBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();

	public QualSummaryReport() {
		super();
	}
	
	@Deprecated
	public QualSummaryReport(String [] excludes) {
		super();
		this.excludes = excludes;

	}
	
	@Override
	public void toXml(Element parent) {
		Element element = init(parent, ProfileType.QUAL, null, null);
		if ( ! excludeAll) {
			qualByCycle.toXml( element, "QualityByCycle");
			SummaryReportUtils.lengthMapToXml(element, "BadQualsInReads", qualBadReadLineLengths);
		}
	}
	
	/**
	 * Parse a SimpleRecord object into the required format for QUALS.<br>
	 * Calls <code>SummaryReport.updateRecordParsed</code>,<br>
	 * <code>SummaryByCycleUtils.parseIntegerSummary</code>, and<br>
	 * <code>SummaryReportUtils.tallyQualScores</code><br>
	 * 
	 * @param record SimpleRecord containing the data to be parsed. For QUAL files, this is space delimited.
	 * 
	 * @see org.qcmg.qprofiler2.report.SummaryReport#updateRecordsParsed()
	 * @see org.qcmg.common.util.SummaryByCycleUtils#parseIntegerSummary(SummaryByCycle, String, String)
	 * @see org.qcmg.qprofiler2.util.SummaryReportUtils#tallyQualScores(String, Map, String)
	 */
	public void parseRecord(SimpleRecord record) throws Exception{
		if (null == record) return;
		updateRecordsParsed();
		
		// qual file is space delimited
		SummaryByCycleUtils.parseIntegerSummary(qualByCycle, record.getData(), " ");
		
		// keep count of the no of read bases that have a qual score of less than 10 for each read
		SummaryReportUtils.tallyQualScores(record.getData(), qualBadReadLineLengths, " ");
		 
	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// Methods used by test classes
	//////////////////////////////////////////////////////////////////////////////////////////
	SummaryByCycle<Integer> getQualByCycle() {
		return qualByCycle;
	}
	
}
