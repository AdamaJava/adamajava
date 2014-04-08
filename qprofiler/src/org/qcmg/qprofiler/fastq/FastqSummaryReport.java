/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.fastq;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.picard.fastq.FastqRecord;
import net.sf.samtools.SAMUtils;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	
	private SummaryByCycle<Character> baseByCycle = new SummaryByCycle<Character>(200,8);
	private ConcurrentMap<Integer, AtomicLong> seqBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
	
	private SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>(200,60);
	private ConcurrentMap<Integer, AtomicLong> qualBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
	
	private Map<Integer, AtomicLong> sequenceLineLengths = null;
	
	
	private String[] excludes;
	private boolean excludeAll;
	
	public FastqSummaryReport() {
		super();
	}
	
	public FastqSummaryReport(String [] excludes) {
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
//		logger.debug("Running with [excludeAll: {}]" , excludeAll);
	}
	
	@Override
	public void toXml(Element parent) {
		
		
		sequenceLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(baseByCycle, getRecordsParsed());
		
		Element element = init(parent, ProfileType.FASTQ, null, excludes, null);
		if ( ! excludeAll) {
			baseByCycle.toXml( element, "BaseByCycle");
			SummaryReportUtils.lengthMapToXml(element, "BadBasesInReads", seqBadReadLineLengths);
			SummaryReportUtils.lengthMapToXml(element, "LengthTally", sequenceLineLengths);
			
			qualByCycle.toXml( element, "QualityByCycle");
			SummaryReportUtils.lengthMapToXml(element, "BadQualsInReads", qualBadReadLineLengths);
		}
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(FastqRecord record) {
		if (null != record) {
			
			updateRecordsParsed();
			
			if ( ! excludeAll) {
			
				SummaryByCycleUtils.parseCharacterSummary(baseByCycle, record.getReadString(), null);
				// keep tally of the number of '.' and 'N' that occur in each read
				SummaryReportUtils.tallyBadReadsAsString(record.getReadString(), seqBadReadLineLengths);
				
				// qual file is space delimited
				byte[] baseQualities = SAMUtils.fastqToPhred(record.getBaseQualityString());
				SummaryByCycleUtils.parseIntegerSummary(qualByCycle, baseQualities);
				
				// keep count of the no of read bases that have a qual score of less than 10 for each read
				SummaryReportUtils.tallyQualScores(baseQualities, qualBadReadLineLengths);
			}
		}
	}
	
	SummaryByCycle<Character> getFastqBaseByCycle() {
		return baseByCycle;
	}
}
