/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.qual;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.ProfileType;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.SummaryByCycle;
import org.qcmg.qprofiler.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.record.SimpleRecord;
import org.w3c.dom.Element;

public class QualSummaryReport extends SummaryReport {
	
	private String [] excludes;
	private boolean excludeAll;
	private SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>(50,40);
	private ConcurrentMap<Integer, AtomicLong> qualBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
//	private Map<Integer, AtomicLong> qualBadReadLineLengths = new TreeMap<Integer, AtomicLong>();

	public QualSummaryReport() {
		super();
	}
	
	public QualSummaryReport(String [] excludes) {
		super();
		this.excludes = excludes;
		if (null != excludes) {
			for (String exclude : excludes) {
				if ("all".equalsIgnoreCase(exclude)) {
					excludeAll = true;
					break;
				}
			}
		}
		logger.debug("Running with excludeAll: " + excludeAll);
//		logger.debug("Running with [excludeAll: {}]", excludeAll);
	}
	
	@Override
	public void toXml(Element parent) {
		Element element = init(parent, ProfileType.QUAL, null, excludes, null);
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
	 * @see org.qcmg.qprofiler.report.SummaryReport#updateRecordsParsed()
	 * @see org.qcmg.qprofiler.util.SummaryByCycleUtils#parseIntegerSummary(SummaryByCycle, String, String)
	 * @see org.qcmg.qprofiler.util.SummaryReportUtils#tallyQualScores(String, Map, String)
	 */
	public void parseRecord(SimpleRecord record) throws Exception{
		if (null != record) {
			updateRecordsParsed();
			
			if (excludeAll) {
				// test that file is OK
//				StringTokenizer quals = new StringTokenizer(record.getData());
//				// remove first token - should be a letter
//				quals.nextToken();
//				for (int i = 1, size = quals.countTokens() ; i <= size ; i++) {
//					Integer.parseInt( quals.nextToken() );
//				}
//				String[] quals = record.getData().split(" ");
//				for (int i = 1, size = quals.length ; i <= size ; i++) {
//					Integer.parseInt( quals[i-1] );
//				}
				
			} else {
				// qual file is space delimited
				SummaryByCycleUtils.parseIntegerSummary(qualByCycle, record.getData(), " ");
				
				// keep count of the no of read bases that have a qual score of less than 10 for each read
				SummaryReportUtils.tallyQualScores(record.getData(), qualBadReadLineLengths, " ");
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// Methods used by test classes
	//////////////////////////////////////////////////////////////////////////////////////////
	SummaryByCycle<Integer> getQualByCycle() {
		return qualByCycle;
	}
	
}
