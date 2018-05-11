/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.fasta;

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

public class FastaSummaryReport extends SummaryReport {
	
	private SummaryByCycle<Character> colourByCycle = new SummaryByCycle<Character>(50,5);
	private ConcurrentMap<Integer, AtomicLong> seqBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();

	
	//it do nothing once specify exludes and it not make sense since coverting includes from option to excludes
	@Deprecated
	private String[] excludes;
	@Deprecated
	private boolean excludeAll;
	
	@Deprecated
	public FastaSummaryReport(String [] excludes) {
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
		logger.debug("running with excludeAll: " + excludeAll); 
	}
	
	
	public FastaSummaryReport() { super(); }	
	
	@Override
	public void toXml(Element parent) {
//		Element element = init(parent, ProfileType.FASTA, null, excludes, null);
		Element element = init(parent, ProfileType.FASTA, null, null);
		 
		colourByCycle.toXml( element, "ColourByCycle");
		SummaryReportUtils.lengthMapToXml(element, "BadColoursInReads", seqBadReadLineLengths);
		 
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(SimpleRecord record) {
		if (null == record) return;
			
		String data = record.getData();
		
		if (null != data) {
			updateRecordsParsed();
			
			SummaryByCycleUtils.parseCharacterSummary(colourByCycle, data, null, 1);
			// keep tally of the number of '.' and 'N' that occur in each read
			SummaryReportUtils.tallyBadReadsAsString(data, seqBadReadLineLengths);
		}
			
		 
	}
	
	SummaryByCycle<Character> getFastaByCycle() {
		return colourByCycle;
	}
}
