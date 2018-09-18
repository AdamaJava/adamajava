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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
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
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.Qprofiler1XmlUtils;
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

//	private final static int mateRefNameMinusOne = 255;
	public static final Character cc = Character.MAX_VALUE;
	public static final Integer ii = Integer.MAX_VALUE;	
	public static final String[] sourceName = new String[]{ "UnPaired", XmlUtils.FirstOfPair, XmlUtils.SecondOfPair };	
	
	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
	//SEQ
	//first of pair, secondof pair, unpaired
	@SuppressWarnings("unchecked")
	private final CycleSummary<Character>[] seqByCycle= new CycleSummary[]{new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512)};
	private final QCMGAtomicLongArray[] seqBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};

	//QUAL
	@SuppressWarnings("unchecked")
	private final  CycleSummary<Integer>[] qualByCycleInteger = new CycleSummary[]{new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512)};
	private final QCMGAtomicLongArray[] qualBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};
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
		
	@SuppressWarnings("serial")
	private final ConcurrentMap<String, ReadGroupSummary> rgSummaries = new ConcurrentHashMap<String, ReadGroupSummary>(){{  
		put( XmlUtils.All_READGROUP, new ReadGroupSummary( XmlUtils.All_READGROUP) ); }}; 
			
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
 	TagSummaryReport2 tagReport; 
 	
	private int zeroCoverageCount;
	private boolean includeCoverage;
	private Long maxRecords;
	private SAMFileHeader bamHeader;
	private SAMSequenceDictionary samSeqDictionary;
	private List<String> readGroupIds = Arrays.asList( XmlUtils.UNKNOWN_READGROUP ); //init	
	
	public BamSummaryReport2(String [] includes, int maxRecs, String [] tags, String [] tagsInt, String [] tagsChar) {
		super();		
		tagReport = new TagSummaryReport2();

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
	/**
	 * BAM record field
	QNAME String [!-?A-~]{1,254} Query template NAME
	2 FLAG Int [0,216-1] bitwise FLAG
	3 RNAME String \*|[!-()+-<>-~][!-~]* Reference sequence NAME
	4 POS Int [0,231-1] 1-based leftmost mapping POSition
	5 MAPQ Int [0,28
	-1] MAPping Quality
	6 CIGAR String \*|([0-9]+[MIDNSHPX=])+ CIGAR string
	7 RNEXT String \*|=|[!-()+-<>-~][!-~]* Ref. name of the mate/next read
	8 PNEXT Int [0,231-1] Position of the mate/next read
	9 TLEN Int [-231+1,231-1] observed Template LENgth
	10 SEQ String \*|[A-Za-z=.]+ segment SEQuence
	11 QUAL 
	 */
	public void toXml( Element parent ) {	
		
		Element bamReportElement = init(parent, ProfileType.BAM, null,  maxRecords);		
		XmlUtils.bamHeaderToXml(bamReportElement, bamHeader);
		summaryToXml(bamReportElement ); 	//Summary for all read group
		
		//create bamMertrics
		bamReportElement = Qprofiler1XmlUtils.createSubElement( bamReportElement, "bamMetrics" );
		createQNAME(Qprofiler1XmlUtils.createSubElement(bamReportElement, "QNAME")  );					
		XmlUtils.outputMap( Qprofiler1XmlUtils.createSubElement(bamReportElement, "FLAG"), "read number", "read counts distribution based on flag string", "flag", "flag string" , flagBinaryCount);		 
		createRNAME(Qprofiler1XmlUtils.createSubElement(bamReportElement, "RNAME")  ); //it is same to RNEXT
		createPOS(Qprofiler1XmlUtils.createSubElement(bamReportElement, "POS")  );	
		createMAPQ(Qprofiler1XmlUtils.createSubElement(bamReportElement, "MAPQ"));	
		createCigar(Qprofiler1XmlUtils.createSubElement(bamReportElement, "CIGAR") );
		//PNEXT will be same to pos
		createTLen(Qprofiler1XmlUtils.createSubElement(bamReportElement, "TLEN") );
		createSeq(Qprofiler1XmlUtils.createSubElement(bamReportElement, "SEQ")  );
		createQual(Qprofiler1XmlUtils.createSubElement(bamReportElement, "QUAL")  );
		tagReport.toXml(Qprofiler1XmlUtils.createSubElement(bamReportElement, "TAG")); 

	}
	
	private void createQNAME(Element parent ){
		 //read name will be under RT Tab	
		for( Entry<String, ReadIDSummary> entry:  readIdSummary.entrySet()){
			Element readNameElement = Qprofiler1XmlUtils.createSubElement(parent, "ReadHeader");
			readNameElement.setAttribute(XmlUtils.readGroupid, entry.getKey());
			readNameElement.setAttribute(XmlUtils.count, entry.getValue().getInputReadNumber()+"");
			entry.getValue().toXml(readNameElement);			
		}	
	}
		
	//<SEQ>
	private void createSeq(Element seqElement){	
		
		//BaseByCycle
		for(int order = 0; order < 3; order++) 			
			seqByCycle[order].toXml(seqElement, "read Base distribution per base cycle", sourceName[order]+"BaseByCycle", "read base cycle", "counts for each base" );
		
		//seqLength
		for(int order = 0; order < 3; order++) 
			XmlUtils.outputMap( seqElement, "read counts", "read counts distribution based on read seq length",  sourceName[order]+"seqlength", "seq line length" , seqByCycle[order].getLengthMapFromCycle());
		//badBase
		for(int order = 0; order < 3; order++)  			
			XmlUtils.outputMap( seqElement, "base counts", "bad base(. or N) distribution based on read base cycle",  sourceName[order]+"BadBasesInReads", "read base cycle" , seqBadReadLineLengths[order]);
		
		//1mers is same to baseByCycle
		for( int i : new int[] { 2, 3, KmersSummary.maxKmers } )
			kmersSummary.toXml( seqElement,i );	
	}
	
	//<QUAL>
	private void createQual(Element parent){
		
		for(int order = 0; order < 3; order++) 			
			qualByCycleInteger[order].toXml(parent, "base Quality distribution per base cycle",sourceName[order]+"QualityByCycle", "Read Base cycle", "counts for each quality base value");
		for(int order = 0; order < 3; order++)			
			XmlUtils.outputMap(parent, 		"read counts", "read counts distribution based on read qual length","qualLength"+sourceName[order], "qual line length",qualByCycleInteger[order].getLengthMapFromCycle());
		for(int order = 0; order < 3; order++)  			
			XmlUtils.outputMap( parent, "bad base counts", "bad base(qual score < 10) distribution based on read base cycle",  sourceName[order]+"BadBasesInReads", "read base cycle" , qualBadReadLineLengths[order]);	 
	}	
	
	private void createCigar(Element parent) {
		Map<String, AtomicLong> map = new TreeMap<>(new CigarStringComparator());
		map.putAll(cigarValuesCount);
		
		XmlUtils.outputMap(parent, "base counts", "base counts distribution based on cigar element", "cigar", "cigar element",  map);
				
		//why require length
		XmlUtils.outputMap(parent, "length","not sure why output these data", "cigarLengths", "cigar string length", cigarLengths);
	}
	
	private void createMAPQ(Element parent) {				
		//,  sourceName, mapQualityLengths
		if(sourceName.length == 0 ||  sourceName.length != mapQualityLengths.length   ) return;				
		String poss = XmlUtils.joinByComma( Arrays.asList(sourceName) );	
		 				
		Map<Integer, String> values = new HashMap<>();
		//MAPQ is [0,255], there should be bug is out of this range
		for(int i = 0; i < 256; i ++) {
			List<String> Scount = new ArrayList<>();
			long total = 0;
			for(int j = 0; j < sourceName.length; j ++) {
				Scount.add(mapQualityLengths[j].get(i)+"");
				total += mapQualityLengths[j].get(i);
			}
			if(total > 0) //we don't output mapq if no read contain this value
				values.put(i, XmlUtils.joinByComma(Scount));
		} 
		XmlUtils.outputMatrix(parent, poss, "read number distribution based on mapping quality", "MAPQ", "mapping quality value [0,255]", "" ,values);
	}	
	private void createTLen(Element parent){
		// ISIZE	
		Set<String> readGroups =  rgSummaries.keySet();
		readGroups.remove(XmlUtils.All_READGROUP);
		String poss = XmlUtils.joinByComma(new ArrayList<String>(readGroups));		
		if(poss == null || poss.length() <= 0) return; 
		
		Map<Integer, String> values = new HashMap<>();
		//report isize < 5000
		for(int pos = 0; pos < ReadGroupSummary.middleTlenValue ; pos ++){
			List<Long> counts = new ArrayList<>();
			for(String rg : readGroups )
				counts.add(rgSummaries.get(rg).getISizeCount().get(pos));
			long sum = counts.stream().reduce((x,y) -> x+y).get();
			if(sum > 0)
				values.put(pos, XmlUtils.joinByComma(counts));			
		}
				
		XmlUtils.outputMatrix( parent, poss, "TLen distribution", "tLen", "TLEN value", "pair counts for each read group", values);
	}	
	
	private void createRNAME(Element parent){		
		if (null == samSeqDictionary) return;
				
		// convert to strings using SAMSequenceDictionary
		Map<String, AtomicLong> map = new HashMap<String, AtomicLong>();		
		for(String chr : rNamePosition.keySet()) {
 			long cov = rNamePosition.get(chr).getTotalCount();
			if(cov > 0)
			map.put(chr,new AtomicLong(cov));
		}		
		
		XmlUtils.outputMap( parent, "read counts", "read mapping distribution per reference", "RNAME", "reference name" , map);
	}	
	
	private void createPOS(Element parent){
		
		List<String> readGroups = rgSummaries.keySet().stream().filter(it -> !it.equals(XmlUtils.All_READGROUP)).sorted().collect(Collectors.toList());			
		rNamePosition.keySet().stream().sorted(new ReferenceNameComparator()).forEach(k->{
			Map<String, String> map = new HashMap<>();
			// iterator on may <binNumber, [counts for each read group at that binNo]> eg. <2, [500,600,600]>
			for (Entry<Integer, AtomicLongArray> entry : rNamePosition.get(k).getCoverageByRgs( readGroups).entrySet() ){  
				int start = entry.getKey() * PositionSummary.BUCKET_SIZE; 
				int end = start + PositionSummary.BUCKET_SIZE -1;
				//map.put(String.format("%d,%d", start, end), XmlUtils.joinByComma(Arrays.asList(entry.getValue())));	
				map.put(String.format("%d,%d", start, end),   entry.getValue().toString().replace("[", "").replace("]", "").replace(" ", "") );
				}
			XmlUtils.outputMatrix(parent, XmlUtils.joinByComma(readGroups), String.format("read depth on reference %s. Bin size is %d base pair", k,PositionSummary.BUCKET_SIZE ),					
					"depthOn"+k, "[start,end] position on referene for each bin", "read counts per bin based on readgroup id",map);
			
		});
	}

	/**
	 * Parse a SAMRecord Collate various pieces of info from the SAMRecord ready for the summariser to retrieve
	 * @param record SAMRecord next row in file
	 */
	public void parseRecord( final SAMRecord record ) {
 		updateRecordsParsed();
		
		String readGroup = XmlUtils.UNKNOWN_READGROUP;
		if(record.getReadGroup() != null && record.getReadGroup().getId() != null )
			readGroup = record.getReadGroup().getReadGroupId();				
		// check if record has its fail or duplicate flag set. if so, miss out some of the summaries
		//anyway, add to summary and then add to it's readgroup
		rgSummaries.get(XmlUtils.All_READGROUP).parseRecord(record); 
		ReadGroupSummary rgSumm = rgSummaries.computeIfAbsent(readGroup, k -> new ReadGroupSummary(k));
		//ReadIDSummary idSumm = readIdSummary.computeIfAbsent( readGroup, (k) -> new ReadIDSummary() );
		
		final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;			
		if( rgSumm.parseRecord(record) ) {	
			//QName			 		
			readIdSummary.computeIfAbsent( readGroup, (k) -> new ReadIDSummary() ).parseReadId( record.getReadName() );
 			
			// SEQ 
			byte[] data = (record.getReadNegativeStrandFlag())? SummaryReportUtils.getReversedSeq(record.getReadBases()) : record.getReadBases();
			seqByCycle[order].parseByteData(data );			
			SummaryReportUtils.tallyBadReadsAsString( record.getReadBases(), seqBadReadLineLengths[order] );	

			// QUAL 	
			data = (record.getReadNegativeStrandFlag())? SummaryReportUtils.getReversedQual(record.getBaseQualities()) : record.getBaseQualities();
			qualByCycleInteger[order].parseByteData(data);
			SummaryReportUtils.tallyQualScores( record.getBaseQualities(), qualBadReadLineLengths[order] );				
 			kmersSummary.parseKmers( record.getReadBases(), record.getReadNegativeStrandFlag(), order );
 						
			//TLen is done inside readGroupSummary.pParseRecord			
			// MRNM is same to RNAME
 			
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
				//zero coverage if there is no overlap btw two adjacent reads
				if (latestFromMap < referenceStart)  
					zeroCoverageCount += referenceStart - latestFromMap + 1;
				 				 
				for (int i = 0 ; i < ab.getLength( ); i++) 
					SummaryByCycleUtils.incrementCount(coverageQueue, Integer.valueOf(i + referenceStart));
				 				
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
		
	private void summaryToXml( Element parent){
		Element summaryElement = Qprofiler1XmlUtils.createSubElement(parent, XmlUtils.summary);
				
		String[][] properties = new String[5][2];
		//eg, <Property value="112" description="Number of cycles with >1% mismatches"/>
		properties[0][0] = CycleSummaryUtils.getBigMDCycleNo(tagReport.tagMDMismatchByCycle, (float) 0.01, tagReport.allReadsLineLengths) +"";
		properties[0][1] ="Number of cycles with greater than 1% mismatches";
		
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
		
		long badReadNum = rgSummaries.get(XmlUtils.All_READGROUP).getNumFailedVendorQuality() +
				rgSummaries.get(XmlUtils.All_READGROUP).getNumSecondary() + rgSummaries.get(XmlUtils.All_READGROUP).getNumSupplementary() ;				
		properties[3][0] = badReadNum + "";
		properties[3][1] = "Discarded reads (FailedVendorQuality, secondary, supplementary)";		
		properties[4][0] = getRecordsParsed() + "";
		properties[4][1] = "Total reads including discarded reads";
				
		for(String[] property : properties) {
			Element element = Qprofiler1XmlUtils.createSubElement(summaryElement,  "Property");
			element.setAttribute("value", property[0]);
			element.setAttribute("description", property[1]);			
		}
		
		//for each real read group first	
		long trimBases = 0;
		long maxBases = 0; 		
		for(ReadGroupSummary summary: rgSummaries.values()) 
			if(! summary.getReadGroupId().equals(XmlUtils.All_READGROUP)){
				summary.readSummary2Xml(summaryElement);
			//	summary.pairSummary2Xml(summaryElement);  
				trimBases += summary.getTrimedBases();
				maxBases += summary.getMaxReadLength() * summary.getCountedReads(); 
			}		
		
		if( maxBases == 0) return; //incase there is no readGroup  in bam header
		//overall group at last
		ReadGroupSummary summary = rgSummaries.get(XmlUtils.All_READGROUP);
		summary.setMaxBases(maxBases);
		summary.setTrimedBases(trimBases);
		summary.readSummary2Xml(summaryElement);
		//summary.pairSummary2Xml(summaryElement);
		
		//output pairs latter
		for(ReadGroupSummary summary1: rgSummaries.values()) 
			summary1.pairSummary2Xml(summaryElement);
		
		if (includeCoverage) 
			XmlUtils.outputMap(summaryElement, "reference base counts", "not sure", "coverage", "read depth", coverage);
	
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
}
