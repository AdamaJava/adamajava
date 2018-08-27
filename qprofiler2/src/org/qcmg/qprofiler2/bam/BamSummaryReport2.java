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
package org.qcmg.qprofiler2.bam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import htsjdk.samtools.AlignmentBlock;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;

import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.MAPQMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.KmersSummary;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;
import org.qcmg.qprofiler2.util.CycleSummaryUtils;
import org.qcmg.qprofiler2.util.FlagUtil;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class BamSummaryReport2 extends SummaryReport {	

	private final static int mateRefNameMinusOne = 255;
	public static final Character cc = Character.MAX_VALUE;
	public static final Integer ii = Integer.MAX_VALUE;	
	public static final String[] sourceName = new String[]{ "UnPaired", QprofilerXmlUtils.FirstOfPair, QprofilerXmlUtils.SecondOfPair };	
	
	//SEQ
	//first of pair, secondof pair, unpaired
	@SuppressWarnings("unchecked")
	private final CycleSummary<Character>[] seqByCycle= new CycleSummary[]{new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512)};
	private final QCMGAtomicLongArray[] seqBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};

	//QUAL
	@SuppressWarnings("unchecked")
	private final  CycleSummary<Integer>[] qualByCycleInteger = new CycleSummary[]{new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512)};
	private final QCMGAtomicLongArray[] qualBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};
	
	@SuppressWarnings("unchecked")
//	private final QCMGAtomicLongArray[] allReadsLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024)};
	private final QCMGAtomicLongArray MRNMLengths = new QCMGAtomicLongArray(mateRefNameMinusOne + 1);
	private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<String, AtomicLong>();
	private final QCMGAtomicLongArray[] mapQualityLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(256), new QCMGAtomicLongArray(256), new QCMGAtomicLongArray(256)};
	
	// FLAGS
	private final Map<String, AtomicLong> flagBinaryCount = new ConcurrentSkipListMap<String, AtomicLong>();
	private final QCMGAtomicLongArray flagIntegerCount = new QCMGAtomicLongArray( 2048 );

	// Coverage
	private final ConcurrentMap<Integer, AtomicLong> coverage = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentNavigableMap<Integer, AtomicLong> coverageQueue = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentMap<String, PositionSummary> rNamePosition = new ConcurrentHashMap<String, PositionSummary>(85);  //chr=>coveragesummary	
	
	private final QCMGAtomicLongArray cigarLengths = new QCMGAtomicLongArray(1024);	
	private final QCMGAtomicLongArray p1Lengths = new QCMGAtomicLongArray(1024);
	private final QCMGAtomicLongArray p2Lengths = new QCMGAtomicLongArray(1024);
	
	
	
	//Xu Code: each RG softclips, hardclips, read length; 	
	@SuppressWarnings("serial")
	private final ConcurrentMap<String, ReadGroupSummary> rgSummaries = new ConcurrentHashMap<String, ReadGroupSummary>(){{  
		put( QprofilerXmlUtils.All_READGROUP, new ReadGroupSummary( QprofilerXmlUtils.All_READGROUP) ); }}; 
			
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
 	TagSummaryReport2 tagReport; 
 	
	private int zeroCoverageCount;
	private boolean includeCoverage;
	private Long maxRecords;
	private SAMFileHeader bamHeader;
	private SAMSequenceDictionary samSeqDictionary;
	private List<String> readGroupIds = Arrays.asList( QprofilerXmlUtils.UNKNOWN_READGROUP ); //init	
	
	public BamSummaryReport2(String [] includes, int maxRecs, String [] tags, String [] tagsInt, String [] tagsChar) {
		super();		
		tagReport = new TagSummaryReport2(tags, tagsInt, tagsChar);

		if (maxRecs > 0) maxRecords = Long.valueOf(maxRecs);		
		
		if (null == includes) return;		
		for (String include : includes) {
			if ("matrices".equalsIgnoreCase(include) || "matricies".equalsIgnoreCase(include))  
				tagReport.setInclMatrices();
			 else if ("coverage".equalsIgnoreCase(include))  
				includeCoverage = true;
			 else  
				logger.warn("Unknown include type: " + include);				 
		}			
	}

	/**
	 * Called once all records have been parsed.
	 * Allows some cleanup to take place - eg. move remaining entries from coverageQueue and add to coverage map
	 */
	public void cleanUp() {
					
		if (includeCoverage ) {
			// add the zero coverage count to the collection
			if (zeroCoverageCount > 0)
				coverage.put(0, new AtomicLong(zeroCoverageCount));
			// if there are any entries left in the queue, add them to the map
			if ( ! coverageQueue.isEmpty()) {
				int lastEntry = ((ConcurrentSkipListMap<Integer, AtomicLong>)coverageQueue).lastKey().intValue();
				lastEntry++;	// increment as headMap returns values less than the passed in key
				removeCoverageFromQueueAndAddToMap(lastEntry, coverageQueue, coverage);
				assert coverageQueue.isEmpty() : "There are still entries in the coverageQueue!!"; 
			}
		}		
						
		long length = flagIntegerCount.length();
		for (int i = 0 ; i < length ; i++) 
			if (flagIntegerCount.get(i) > 0) {
				String flagString = FlagUtil.getFlagString(i);
				flagBinaryCount.put(flagString, new AtomicLong(flagIntegerCount.get(i)));
			}
	}
	
	@Override
	public void toXml( Element parent ) {	
		
		Element bamReportElement = init(parent, ProfileType.BAM, null,  maxRecords);		
		XmlUtils.bamHeaderToXml(bamReportElement, bamHeader);
		summaryToXml(bamReportElement ); //Summary for all read group
		
		
		//create bamMertrics
		bamReportElement = QprofilerXmlUtils.createSubElement( bamReportElement, "bamMetrics" );
		createSeq(bamReportElement );
		createQual(bamReportElement );
		tagReport.toXml(bamReportElement);   
//		createIsize(bamReportElement );	
//		createMRNM(bamReportElement );	
//		createRNAME(bamReportElement);
//		
//		// CIGAR
//		Element cigarElement = QprofilerXmlUtils.createSubElement(bamReportElement, "CIGAR");
//		SummaryReportUtils.lengthMapToXml(cigarElement, "ObservedOperations", null, cigarValuesCount, new CigarStringComparator());
//		SummaryReportUtils.lengthMapToXml(cigarElement, "Lengths", cigarLengths);		
//		SummaryReportUtils.MAPQtoXML(bamReportElement, "MAPQ", sourceName, mapQualityLengths) ;// MAPQ					
//		SummaryReportUtils.lengthMapToXml(bamReportElement, "FLAG", flagBinaryCount); // FLAG
//		if (includeCoverage) SummaryReportUtils.lengthMapToXml(bamReportElement, "Coverage", coverage);
	}
		
	//<SEQ>
	private void createSeq(Element bamReportElement){
		Element seqElement = QprofilerXmlUtils.createSubElement(bamReportElement, "report");	//SEQ
		seqElement.setAttribute("Category", "SEQ");
				
		for(int order = 0; order < 3; order++){
			//SEQ
			seqByCycle[order].toXml(seqElement, "read Base distribution per base cycle", sourceName[order]+"BaseByCycle" );
			SummaryReportUtils.lengthMapToXml(seqElement, "LengthTally",    sourceName[order], seqByCycle[order].getLengthMapFromCycle());
 			SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads" ,sourceName[order],  seqBadReadLineLengths[order]);
		}
		
		//add kmers  
		for( int i : new int[] { 1, 2, 3, kmersSummary.maxKmers } )
			kmersSummary.toXml(seqElement,i);	
		
		
		 //read name will be under RT Tab
		Element readNameElement = QprofilerXmlUtils.createSubElement(seqElement, "ReadNameAnalysis");
		for( Entry<String, ReadIDSummary> entry:  tagReport.getReadIDSummary().entrySet()){
			readNameElement.setAttribute("ReadGroupid", entry.getKey());
			entry.getValue().toXml(readNameElement);
			
		}
	}
	//<QUAL>
	private void createQual(Element bamReportElement){
		Element qualElement = QprofilerXmlUtils.createSubElement( bamReportElement, "QUAL" );	//QUAL		
		for(int order = 0; order < 3; order++){
			//QUAL
			qualByCycleInteger[order].toXml(qualElement, "base Quality distribution per base cycle",sourceName[order]+"QualityByCycle");
			SummaryReportUtils.lengthMapToXml(qualElement,"LengthTally",sourceName[order], qualByCycleInteger[order].getLengthMapFromCycle());
			SummaryReportUtils.lengthMapToXml(qualElement,"BadQualsInReads",sourceName[order], qualBadReadLineLengths[order]);
		}
	}	

	private void createIsize(Element bamReportElement){
		// ISIZE
		Element tagISizeElement = QprofilerXmlUtils.createSubElement(bamReportElement, "ISIZE");
		SummaryReportUtils.iSize2Xml(tagISizeElement, rgSummaries);
	}	
	
	private void createMRNM(Element bamReportElement){
		// MRNM
		if (null != samSeqDictionary) {
			// convert to strings using SAMSequenceDictionary
			Map<String, AtomicLong> MRNMLengthsString = new HashMap<String, AtomicLong>();

			for (int i = 0 ; i < MRNMLengths.length() ; i++) {
				long l = MRNMLengths.get(i);
				if (l > 0) {
					if (i == mateRefNameMinusOne) {
						MRNMLengthsString.put("*", new AtomicLong(l));
					} else {
						MRNMLengthsString.put(samSeqDictionary.getSequence(i).getSequenceName(), new AtomicLong(l));
					}
				}
			}
			SummaryReportUtils.lengthMapToXml(bamReportElement, "RNEXT",null, MRNMLengthsString, new ReferenceNameComparator());
		} else {
			SummaryReportUtils.lengthMapToXml(bamReportElement, "RNEXT", MRNMLengths);
		}
	}
	
	private void createRNAME(Element bamReportElement){
		// RNAME_POS		
		Element rnameElement = QprofilerXmlUtils.createSubElement(bamReportElement, "RNAME_POS");
		//SummaryReportUtils.coverageByReferenceToXml(rnameElement, "CoverageByChromosome", rNamePosition, Arrays.asList( QprofilerXmlUtils.All_READGROUP ) ); 
		List<String> readGroups = rgSummaries.keySet().stream().filter(it -> !it.equals(QprofilerXmlUtils.All_READGROUP)).sorted().collect(Collectors.toList());	
		SummaryReportUtils.coverageByReadGroupToXml(rnameElement, "CoverageByReadGroup", rNamePosition, readGroups); 

	}	

	/**
	 * Parse a SAMRecord Collate various pieces of info from the SAMRecord ready for the summariser to retrieve
	 * @param record SAMRecord next row in file
	 */
	public void parseRecord( final SAMRecord record ) {
 		updateRecordsParsed();
		
		String readGroup = QprofilerXmlUtils.UNKNOWN_READGROUP;
		if(record.getReadGroup() != null && record.getReadGroup().getId() != null )
			readGroup = record.getReadGroup().getReadGroupId();
				
		// Xu code: check if record has its fail or duplicate flag set. if so, miss out some of the summaries
		//anyway, add to summary and then add to it's readgroup
		rgSummaries.get(QprofilerXmlUtils.All_READGROUP).parseRecord(record); 

		ReadGroupSummary rgSumm = rgSummaries.computeIfAbsent(readGroup, k -> new ReadGroupSummary(k));
		boolean parsedRecord = rgSumm.parseRecord(record);
		
		final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;			
		if( parsedRecord  ) {	
			// SEQ 
			byte[] data = (record.getReadNegativeStrandFlag())? SummaryReportUtils.getReversedSeq(record.getReadBases()) : record.getReadBases();
			seqByCycle[order].parseByteData(data );			
			SummaryReportUtils.tallyBadReadsAsString( record.getReadBases(), seqBadReadLineLengths[order] );	

			// QUAL 	
			data = (record.getReadNegativeStrandFlag())? SummaryReportUtils.getReversedQual(record.getBaseQualities()) : record.getBaseQualities();
			qualByCycleInteger[order].parseByteData(data);
			SummaryReportUtils.tallyQualScores( record.getBaseQualities(), qualBadReadLineLengths[order] );				
 			kmersSummary.parseKmers( record.getReadBases(), record.getReadNegativeStrandFlag(), order );
 						
			//ISIZE is done inside readGroupSummary.pParseRecord
			
			// MRNM
			final int mateRefNameIndex = record.getMateReferenceIndex();
			if ( mateRefNameIndex == -1 )  MRNMLengths.increment( mateRefNameMinusOne );
			else  MRNMLengths.increment(mateRefNameIndex);			 

			// RNAME & POS			
			parseRNameAndPos(record.getReferenceName(), record.getAlignmentStart(), readGroup);	// Position value
			
			if (record.getReadPairedFlag()){  
				if (record.getFirstOfPairFlag()) p1Lengths.increment(record.getReadBases().length);
				else if (record.getSecondOfPairFlag())  p2Lengths.increment(record.getReadBases().length);
			} 
			// coverage 
			if (includeCoverage) { 	parseCoverage(record); }			
		}
		
		// MAPQ (Mapping Quality)
		final int mapQ = record.getMappingQuality();
		mapQualityLengths[order].increment(mapQ);


		// only TAGS, FLAGS, and CIGARS are always summarised
		tagReport.parseTAGs(record);
		parseCigar(record.getCigar());
		flagIntegerCount.increment(record.getFlags());
	}

	void parseCoverage(SAMRecord record) {
		int count = 0;
		for (AlignmentBlock ab : record.getAlignmentBlocks()) {
			// find out how many positions were skipped...
			int referenceStart = ab.getReferenceStart();
			synchronized (coverageQueue) {
				int latestFromMap = coverageQueue.isEmpty() ? 0 :  coverageQueue.lastKey();
				if (latestFromMap < referenceStart)  
					zeroCoverageCount += referenceStart - latestFromMap + 1;
				 
				SummaryReportUtils.addPositionAndLengthToMap(coverageQueue, referenceStart, ab.getLength());
				if (++count == 1)
					removeCoverageFromQueueAndAddToMap(referenceStart, coverageQueue, coverage);
			}
		}
	}

	void removeCoverageFromQueueAndAddToMap(int referenceStart, Map<Integer, AtomicLong> queue, 
			ConcurrentMap<Integer, AtomicLong> map) {
		for (Iterator<Integer> it = ((ConcurrentSkipListMap<Integer,AtomicLong>) queue)
				.headMap(referenceStart).keySet().iterator() ; it.hasNext() ; ) {

			SummaryByCycleUtils.incrementCount(map, Integer.valueOf((int)queue.get(it.next()).get()));
			synchronized (queue) {
				it.remove();
			}
		}
	}

	void parseCigar(Cigar cigar) {
		if (null != cigar) {
			int length = 0;

			for (CigarElement ce : cigar.getCigarElements()) {
				CigarOperator operator = ce.getOperator();
				if ( ! CigarOperator.M.equals(operator)) {
					SummaryByCycleUtils.incrementCount(cigarValuesCount, "" + ce.getLength() + operator);
				}
				length += getSizeFromInt(ce.getLength()) + 1;
			}
			cigarLengths.increment(length);
		}
	}

	private int getSizeFromInt(int value) {
		if (value == 0) return 0;
		return 1 + getSizeFromInt(value/10);
	}
		
	private void summaryToXml( Element bamReportElement){
		Element summaryElement = QprofilerXmlUtils.createSubElement(bamReportElement, QprofilerXmlUtils.summary);
				
		String[][] properties = new String[5][2];
		//eg, <Property value="112" description="Number of cycles with >1% mismatches"/>
		properties[0][0] = CycleSummaryUtils.getBigMDCycleNo(tagReport.tagMDMismatchByCycle, (float) 0.01, tagReport.allReadsLineLengths) +"";
		properties[0][1] ="Number of cycles with greater than 1% mismatches";
		
		//<Property value="151" description="Average length of first-of-pair reads" readCount="37804"/>
		//<Property value="151" description="Average length of second-of-pair reads" readCount="37259"/>
		int point = 1;
		for(QCMGAtomicLongArray length: new QCMGAtomicLongArray[] {p1Lengths, p2Lengths}) {
			long runningTally = 0, total = 0;
			for (int i = 0 ; i < length.length() ; i++) {
				long value = length.get(i);
				if (value > 0) {
					total += value;
					runningTally += (value * i);
				}
			}
			
			properties[point][0] = (total > 0 ? (runningTally / total) : 0 ) +"";
			properties[point][1] = String.format("Average length of %s-of-pair reads", point == 1? "first" : "second");			 
			point ++;
		}
		
		//<Property value="1122" description="Discarded reads (FailedVendorQuality, secondary, supplementary)"/>
		long badReadNum = rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumFailedVendorQuality() +
				rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumSecondary() + rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumSupplementary() ;				
		properties[3][0] = badReadNum + "";
		properties[3][1] = "Discarded reads (FailedVendorQuality, secondary, supplementary)";
		
		//<Property value="102789" description="Total reads including discarded reads"/>
		properties[4][0] = getRecordsParsed() + "";
		properties[4][1] = "Total reads including discarded reads";
		
		
		for(String[] property : properties) {
			Element element = QprofilerXmlUtils.createSubElement(summaryElement,  "Property");
			element.setAttribute("value", property[0]);
			element.setAttribute("description", property[1]);
			
		}
		
		//for each real read group first	
		long trimBases = 0;
		long maxBases = 0; 
		
		for(ReadGroupSummary summary: rgSummaries.values()) 
			if(! summary.getReadGroupId().equals(QprofilerXmlUtils.All_READGROUP)){
				summary.readSummary2Xml(summaryElement);
				summary.pairSummary2Xml(summaryElement);  
				trimBases += summary.getTrimedBases();
				maxBases += summary.getMaxReadLength() * summary.getCountedReads(); 
			}		
		
		if( maxBases == 0) return; //incase there is no readGroup  in bam header
		//overall group at last
		ReadGroupSummary summary = rgSummaries.get(QprofilerXmlUtils.All_READGROUP);
		summary.setMaxBases(maxBases);
		summary.setTrimedBases(trimBases);
		summary.readSummary2Xml(summaryElement);
		summary.pairSummary2Xml(summaryElement);
		
	}
		
	void parseRNameAndPos( final String rName,  final int position, String rgid ) {
		PositionSummary ps = (PositionSummary) rNamePosition.computeIfAbsent( rName, k->new PositionSummary( readGroupIds) );
		ps.addPosition( position, rgid );
	}
		
	//setting
	public void setBamHeader(SAMFileHeader header) {  this.bamHeader = header;	 }
	public void setTorrentBam() { if(tagReport != null) tagReport.setTorrentBam();	}
	public String getBamHeader() {	return bamHeader.getSAMString();	}
	public void setSamSequenceDictionary(SAMSequenceDictionary samSeqDictionary) {	this.samSeqDictionary = samSeqDictionary;	}
	public SAMSequenceDictionary getSamSequenceDictionary() { return samSeqDictionary;	}	
	public void setReadGroups(List<String> ids ){	readGroupIds = Stream.concat( ids.stream(), readGroupIds.stream()).collect(Collectors.toList()); }
	
	
	// ///////////////////////////////////////////////////////////////////////
	// The following methods are used by the test classes
	// ///////////////////////////////////////////////////////////////////////
	ConcurrentMap<String, AtomicLong> getCigarValuesCount() { return cigarValuesCount; }
	ConcurrentMap<String, PositionSummary> getRNamePosition() {	return rNamePosition; }
	CycleSummary<Character> getSeqByCycle(int flagFirstOfPair) { return   seqByCycle[flagFirstOfPair] ; 	}
	ConcurrentMap<Integer, AtomicLong> getCoverage() { return coverage;	}
	ConcurrentMap<Integer, AtomicLong> getCoverageQueue() {	return coverageQueue;	}	
	
	ConcurrentMap<Integer, MAPQMatrix> getMapQMatrix() {return tagReport.mapQMatrix ;}	
	ConcurrentMap<String, AtomicLong> getTagRGLineLengths() { return tagReport.tagRGLineLengths; }
	QCMGAtomicLongArray  getTagZMLineLengths() { return tagReport.tagZMLineLengths;	}
	Map<Integer, AtomicLong> getTagCSLineLengths() { return tagReport.tagCSByCycle.getLengthMapFromCycle();	}
	Map<Integer, AtomicLong> getTagCQLineLengths() { return tagReport.tagCQByCycle.getLengthMapFromCycle();	}			
	CycleSummary<Character> getTagCSByCycle() { return tagReport.tagCSByCycle; 	}
}
