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
import java.util.concurrent.atomic.AtomicLong;

import net.sf.picard.fastq.FastqRecord;
import net.sf.samtools.SAMUtils;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SummaryByCycleNew2;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	
//	private SummaryByCycle<Character> baseByCycle = new SummaryByCycle<Character>(200,8);
//	private ConcurrentMap<Integer, AtomicLong> seqBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
//	
//	private SummaryByCycle<Integer> qualByCycle = new SummaryByCycle<Integer>(200,60);
//	private ConcurrentMap<Integer, AtomicLong> qualBadReadLineLengths = new ConcurrentSkipListMap<Integer, AtomicLong>();
//	
//	private Map<Integer, AtomicLong> sequenceLineLengths = null;
	
	//SEQ
	private final SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);

	//QUAL
	private final SummaryByCycleNew2<Integer> qualByCycleInteger = new SummaryByCycleNew2<Integer>(i, 512);
	private Map<Integer, AtomicLong> qualLineLengths = null;
	private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);
	
	
	private  String[] excludes;
	private boolean excludeAll;
	private boolean reverseStrand;
	
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
	}
	
	@Override
	public void toXml(Element parent) {
		
		Element element = init(parent, ProfileType.FASTQ, null, excludes, null);
		if ( ! excludeAll) {
			
			// create the length maps here from the cycles objects
			seqLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, getRecordsParsed());
			qualLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(qualByCycleInteger, getRecordsParsed());
			
			// SEQ
			Element seqElement = createSubElement(element, "SEQ");
			seqByCycle.toXml(seqElement, "BaseByCycle");
			SummaryReportUtils.lengthMapToXmlTallyItem(seqElement, "LengthTally", seqLineLengths);
			SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads", seqBadReadLineLengths);
			
			// QUAL
			Element qualElement = createSubElement(element, "QUAL");
			qualByCycleInteger.toXml(qualElement, "QualityByCycle");
			SummaryReportUtils.lengthMapToXmlTallyItem(qualElement, "LengthTally", qualLineLengths);
			SummaryReportUtils.lengthMapToXml(qualElement, "BadQualsInReads", qualBadReadLineLengths);
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
				
				// SEQ
				SummaryByCycleUtils.parseCharacterSummary(seqByCycle, record.getReadString().getBytes(), reverseStrand);
				SummaryReportUtils.tallyBadReadsAsString(record.getReadString(), seqBadReadLineLengths);

				// QUAL
				byte[] baseQualities = SAMUtils.fastqToPhred(record.getBaseQualityString());
				SummaryByCycleUtils.parseIntegerSummary(qualByCycleInteger, baseQualities, reverseStrand);
				SummaryReportUtils.tallyQualScores(baseQualities, qualBadReadLineLengths);
				
			}
		}
	}
	
	SummaryByCycleNew2<Character> getFastqBaseByCycle() {
		return seqByCycle;
	}
}
