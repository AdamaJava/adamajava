/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * © Copyright QIMR Berghofer Medical Research Institute 2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.fastq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.SAMUtils;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.KmersSummary;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	
	private static final int ReportErrNumber = 100;
	AtomicLong errNumber = new AtomicLong();	
	
	//SEQ
	private final CycleSummary<Character> seqByCycle = new CycleSummary<Character>(c, 512);
	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
	
	//QUAL
	private final CycleSummary<Integer> qualByCycleInteger = new CycleSummary<Integer>(i, 512);
	private Map<Integer, AtomicLong> qualLineLengths = null;
	private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);
		
	AtomicLong qualHeaderNotEqualToPlus = new AtomicLong();		
	private ReadIDSummary readHeaderSummary = new ReadIDSummary(); 
	
	public FastqSummaryReport() { super(); }
	public ReadIDSummary getReadIDSummary(){	return readHeaderSummary;	}
	@Override
	public void toXml(Element parent) {
		
		Element element = init( parent, ProfileType.FASTQ, null, null );					
		Element readNameElement = QprofilerXmlUtils.createSubElement(element, "ReadNameAnalysis");
		readHeaderSummary.toXml(readNameElement);
				
		Map<String, AtomicLong> qualHeaders = new HashMap<>();
		qualHeaders.put("non +", qualHeaderNotEqualToPlus);
		qualHeaders.put("+", new AtomicLong(getRecordsParsed() - qualHeaderNotEqualToPlus.longValue()));
		SummaryReportUtils.lengthMapToXml(readNameElement, "QUAL_HEADERS", qualHeaders);
					
		// create the length maps here from the cycles objects
		seqLineLengths = seqByCycle.getLengthMapFromCycle();
		qualLineLengths = qualByCycleInteger.getLengthMapFromCycle();
		
		// SEQ
		Element seqElement = QprofilerXmlUtils.createSubElement(element, "SEQ");
		seqByCycle.toXml(seqElement, "BaseByCycle", null);
		SummaryReportUtils.lengthMapToXml(seqElement, "LengthTally", seqLineLengths);
		SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads", seqBadReadLineLengths);
		
		kmersSummary.toXml(seqElement,kmersSummary.maxKmers); //debug
		kmersSummary.toXml(seqElement,1); //add 1-mers
		kmersSummary.toXml(seqElement,2); //add 2-mers
		kmersSummary.toXml(seqElement,3); //add 3-mers
		
		// QUAL
		Element qualElement = QprofilerXmlUtils.createSubElement(element, "QUAL");
		qualByCycleInteger.toXml(qualElement, "QualityByCycle", null);
		SummaryReportUtils.lengthMapToXml(qualElement, "LengthTally", qualLineLengths);
		SummaryReportUtils.lengthMapToXml(qualElement, "BadQualsInReads", qualBadReadLineLengths);			
			 		
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(FastqRecord record) {
		if( null == record ) return;
		 			
		updateRecordsParsed();
					 
		// QUAL   it also throw exception if fastq reads is invalid
		byte[] baseQualities = SAMUtils.fastqToPhred( record.getBaseQualityString() );
		
		//read are raw sequence ignore the strand
		qualByCycleInteger.parseByteData( baseQualities);
		SummaryReportUtils.tallyQualScores(baseQualities, qualBadReadLineLengths);
						
		// SEQ
		byte[] readBases = record.getReadString().getBytes();
		seqByCycle.parseByteData(readBases);
		SummaryReportUtils.tallyBadReadsAsString(readBases, seqBadReadLineLengths);
		//fastq base are all orignal forward, treat all as first of pair 
		kmersSummary.parseKmers( readBases, false, 0 ); 
		
		String qualHeader = record.getBaseQualityHeader();			
		// If header just contains "+" then FastqRecord has null for qual header
		if ( ! StringUtils.isNullOrEmpty(qualHeader))  
			qualHeaderNotEqualToPlus.incrementAndGet();		
		
		String id = record.getReadHeader();
		try {
			readHeaderSummary.parseReadId( id );
		} catch (Exception e) {
			if ( errNumber.incrementAndGet() < ReportErrNumber)
				logger.error( "Invalid read id: " + id );
		}		 						 			 	 
	}

//	private <T> void updateMap(ConcurrentMap<T, AtomicLong> map , T key) {
//		AtomicLong al = map.get(key);
//		if (null == al) {
//			al = new AtomicLong();
//			AtomicLong existing = map.putIfAbsent(key, al);
//			if (null != existing) {
//				al = existing;
//			}
//		}
//		al.incrementAndGet();
//	}
//	
//	private <T> void updateMapAndPosition(ConcurrentMap<T, AtomicLongArray> map , T key, int position) {
//		
//		
//		AtomicLongArray ala = map.get(key);
//		if (null == ala) {
//			ala = new AtomicLongArray(256);
//			AtomicLongArray existing = map.putIfAbsent(key, ala);
//			if (null != existing) {
//				ala = existing;
//			}
//		}
//		ala.incrementAndGet(position);
//	}
	
//	CycleSummary<Character> getFastqBaseByCycle() {
//		return seqByCycle;
//	}
	
}
