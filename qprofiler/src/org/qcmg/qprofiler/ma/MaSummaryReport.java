/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.ma;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.ProfileType;
import org.qcmg.ma.MADefLine;
import org.qcmg.ma.MAMapping;
import org.qcmg.ma.MARecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.qvisualise.util.SummaryByCycle;
import org.qcmg.qvisualise.util.SummaryByCycleUtils;
import org.w3c.dom.Element;

public class MaSummaryReport extends SummaryReport {

	private SummaryByCycle<Character> colorByCycle = new SummaryByCycle<Character>(50, 5);
	private ConcurrentMap<Integer, AtomicLong> seqBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
	
	// definition stats
	private ConcurrentMap<String, AtomicLong> defChromosomeLineLengths = new ConcurrentHashMap<String, AtomicLong>(100);
	private ConcurrentMap<String, AtomicLong> sortedDefChromosomeLineLengths = new ConcurrentSkipListMap<String, AtomicLong>(new MAComparator());
	private ConcurrentMap<String, AtomicLong> defLocationLineLengths = new ConcurrentSkipListMap<String, AtomicLong>();
	private ConcurrentMap<String, AtomicLong> defQualityLineLengths = new ConcurrentHashMap<String, AtomicLong>(100);
	private ConcurrentMap<String, AtomicLong> sortedDefQualityLineLengths = new ConcurrentSkipListMap<String, AtomicLong>(new MAComparator(1));
	private ConcurrentMap<Integer, AtomicLong> defCountLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
//	private Map<Integer, Integer> defMismatchLineLengths = new TreeMap<Integer, Integer>();
	
	@Override
	public void toXml(Element parent) {
		Element element = init(parent, ProfileType.MA);
		
		// do some sorting 
		sortedDefChromosomeLineLengths.putAll(defChromosomeLineLengths);
		sortedDefQualityLineLengths.putAll(defQualityLineLengths);
		
		colorByCycle.toXml(element, "ColourByCycle");
		SummaryReportUtils.lengthMapToXml(element, "BadBasesInReads",
				seqBadReadLineLengths);
		SummaryReportUtils.lengthMapToXml(element, "Chromosome",
				sortedDefChromosomeLineLengths);
		SummaryReportUtils.lengthMapToXml(element, "Quality",
				sortedDefQualityLineLengths);
		SummaryReportUtils.lengthMapToXml(element, "Mappings",
				defCountLineLengths);
	}

	/**
	 * Parse a MARecord Collate various pieces of info from the MARecord ready
	 * for the summariser to retrieve
	 * 
	 * @param record
	 *            MARecord next row in file
	 */
	protected void parseRecord(MARecord record) {
		if (null != record) {
			updateRecordsParsed();
			
			SummaryByCycleUtils.parseCharacterSummary(colorByCycle, record.getReadSequence(), null, 1);
			
			// keep tally of the number of '.' and 'N' that occur in each read
			SummaryReportUtils.tallyBadReadsAsString(record.getReadSequence(), seqBadReadLineLengths);
//			SummaryReportUtils.tallyBadReads(record.getReadSequence(), seqBadReadLineLengths);
			
			// tally up some other details from the MaDefine
			MADefLine defLine = record.getDefLine();
			
			SummaryByCycleUtils.incrementCount(defCountLineLengths, Integer.valueOf(defLine.getNumberMappings()));
			
			for (Iterator<MAMapping> i = defLine.iterator() ; i.hasNext() ; ) {
				MAMapping map = i.next();
				SummaryByCycleUtils.incrementCount(defChromosomeLineLengths, map.getChromosome());
				SummaryByCycleUtils.incrementCount(defQualityLineLengths, map.getQuality());
			}
		}
	}

	// ///////////////////////////////////////////////////////////////////////
	// The following methods are used by the test class
	// ///////////////////////////////////////////////////////////////////////
	Map<String, AtomicLong> getChromosomeCount() {
		return defChromosomeLineLengths;
	}
	Map<String, AtomicLong> getLocationCount() {
		return defLocationLineLengths;
	}
	Map<String, AtomicLong> getQualityCount() {
		return defQualityLineLengths;
	}
//	Map<Integer, Integer> getMismatchCount() {
//		return defMismatchLineLengths;
//	}
	Map<Integer, AtomicLong> getBadReadsCount() {
		return seqBadReadLineLengths;
	}

	SummaryByCycle<Character> getColorByCycle() {
		return colorByCycle;
	}

}
