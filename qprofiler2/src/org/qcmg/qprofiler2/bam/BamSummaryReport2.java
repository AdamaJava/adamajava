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
import java.util.Set;
import java.util.TreeMap;
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
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
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

	public static final Character cc = Character.MAX_VALUE;
	public static final Integer ii = Integer.MAX_VALUE;	
	public static final String[] sourceName = new String[]{ "unPaired", QprofilerXmlUtils.FirstOfPair, QprofilerXmlUtils.SecondOfPair };	
	
	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
	//SEQ first of pair, secondof pair, unpaired
	@SuppressWarnings("unchecked")
	private final CycleSummary<Character>[] seqByCycle= new CycleSummary[]{new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512)};
	private final QCMGAtomicLongArray[] seqBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};

	//QUAL
	@SuppressWarnings("unchecked")
	private final CycleSummary<Integer>[] qualByCycleInteger = new CycleSummary[]{new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512)};
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
		put( QprofilerXmlUtils.All_READGROUP, new ReadGroupSummary( QprofilerXmlUtils.All_READGROUP) ); }}; 
			
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
 	private final TagSummaryReport2 tagReport = new TagSummaryReport2();
 	
	private int zeroCoverageCount;
	private boolean includeCoverage;
	private Long maxRecords;
	private SAMFileHeader bamHeader;
	private SAMSequenceDictionary samSeqDictionary;
	private List<String> readGroupIds = Arrays.asList( QprofilerXmlUtils.UNKNOWN_READGROUP ); //init	
		
	public BamSummaryReport2(String [] includes, int maxRecs, String [] tags, String [] tagsInt, String [] tagsChar) {
		super();		

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
		bamReportElement = QprofilerXmlUtils.createSubElement( bamReportElement, "bamMetrics" );
		createQNAME( QprofilerXmlUtils.createSubElement(bamReportElement, "QNAME")  );	
		createFLAG( QprofilerXmlUtils.createSubElement(bamReportElement, "FLAG")  );
		
		createRNAME( QprofilerXmlUtils.createSubElement(bamReportElement, "RNAME")  ); //it is same to RNEXT
		createPOS( QprofilerXmlUtils.createSubElement(bamReportElement, "POS")  );	
		createMAPQ( QprofilerXmlUtils.createSubElement(bamReportElement, "MAPQ"));	
		createCigar( QprofilerXmlUtils.createSubElement(bamReportElement, "CIGAR") );
		//PNEXT will be same to pos
		createTLen( QprofilerXmlUtils.createSubElement(bamReportElement, "TLEN") );
		createSeq( QprofilerXmlUtils.createSubElement(bamReportElement, "SEQ")  );
		createQual( QprofilerXmlUtils.createSubElement(bamReportElement, "QUAL")  );
		tagReport.toXml( QprofilerXmlUtils.createSubElement(bamReportElement, "TAG")); 

	}
	
	private void createQNAME(Element parent ){
		 //read name will be under RT Tab	
		Element rgsElement = QprofilerXmlUtils.createSubElement(parent, XmlUtils.readGroupsEle);
		for( Entry<String, ReadIDSummary> entry:  readIdSummary.entrySet()){
			//"analysis read name pattern for read group
			Element readNameElement = XmlUtils.createMetricsNode(rgsElement, entry.getKey(),null, entry.getValue().getInputReadNumber());					
			entry.getValue().toXml(readNameElement);			
		}	
	}
	
	private void createFLAG(Element parent ){
		 if ( null == flagBinaryCount || flagBinaryCount.isEmpty()) return;
		 
	//	 Map<String, Long> map = flagBinaryCount.entrySet().stream() .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
	     XmlUtils.outputTallyGroup( XmlUtils.createMetricsNode(parent, null, null, null) , "FLAG", null, flagBinaryCount, true);
		
	}
 		
	//<SEQ>
	private void createSeq(Element parent){			
		//BaseByCycle
		
		//Element ele = XmlUtils.createMetricsNode(parent, "readBaseByCycle", null, "read Base distribution per base cycle" );		
		for(int order = 0; order < 3; order++) 	
			seqByCycle[order].toXml( parent, "sequenceBase", sourceName[order], "seqBaseCycle" );
		
		//seqLength
		//"read counts distribution based on read seq length";
		for(int order = 0; order < 3; order++) { 
			if(seqByCycle[order].getLengthMapFromCycle().isEmpty()) continue;
			Element ele = XmlUtils.createMetricsNode( parent, "sequenceLength", sourceName[order], null); 
			XmlUtils.outputTallyGroup( ele, "seqLength",null, seqByCycle[order].getLengthMapFromCycle(), true );		
		}
		
		//badBase: "bad base(. or N) distribution based on read base cycle");
		for(int order = 0; order < 3; order++) {
			if( seqBadReadLineLengths[order].toMap().isEmpty() )continue;
			Element ele = XmlUtils.createMetricsNode( parent, "badBasesInReads", sourceName[order], null);
			XmlUtils.outputTallyGroup( ele, "seqBaseCycle",null,  seqBadReadLineLengths[order].toMap(), true );
		}
		
		//1mers is same to baseByCycle
		for( int i : new int[] { 2, 3, KmersSummary.maxKmers } )
			kmersSummary.toXml( parent,i );	
	}
	
	//<QUAL>
	private void createQual(Element parent){
		//"count on quality base",
		for(int order = 0; order < 3; order++) 			
			qualByCycleInteger[order].toXml(parent,"qualBase",sourceName[order],  "qualBaseCycle");
		
		// "quality string length distribution" );	
		for(int order = 0; order < 3; order++) {
			if(qualByCycleInteger[order].getLengthMapFromCycle().isEmpty()) continue;
			Element ele = XmlUtils.createMetricsNode( parent, "qualLength", sourceName[order],null);
			XmlUtils.outputTallyGroup( ele, "qualSeqLength", null, qualByCycleInteger[order].getLengthMapFromCycle(), true );	
		}
		// "bad base(qual score < 10) distribution based on read base cycle");
		for(int order = 0; order < 3; order++) { 
			if( qualBadReadLineLengths[order].toMap().isEmpty() )continue;
			Element ele = XmlUtils.createMetricsNode( parent, "BadBasesInReads",sourceName[order], null);
			XmlUtils.outputTallyGroup( ele, "qualBaseCycle",null, qualBadReadLineLengths[order].toMap(), true );	
		}
	}	
	
	private void createTLen(Element parent){
        // ISIZE
        Set<String> readGroups =  rgSummaries.keySet();
        readGroups.remove(QprofilerXmlUtils.All_READGROUP);       
        parent = QprofilerXmlUtils.createSubElement(parent, XmlUtils.readGroupsEle );        
        for(String rg : readGroups ) {
        	QCMGAtomicLongArray iarray= rgSummaries.get(rg).getISizeCount();
        	
        	Map<String, AtomicLong> tallys =  iarray.toMap().entrySet().stream().collect(Collectors.toMap(e -> String.valueOf( e.getKey()  ), Map.Entry::getValue));
        	
        	XmlUtils.outputTallyGroup( XmlUtils.createMetricsNode(parent, rg, null, null), "tLen", null, tallys, true );   
        }	
	}			
	private void createCigar(Element parent) {
		Map<String, AtomicLong> tallys = new TreeMap<>(new CigarStringComparator());
		tallys.putAll(	cigarValuesCount);
		XmlUtils.outputTallyGroup( XmlUtils.createMetricsNode(parent, null, null, null), "CIGAR",null, tallys, true );		
	}	
	private void createRNAME(Element parent){		
		if (null == samSeqDictionary) return;
				
		Map<String, AtomicLong> tallys = new TreeMap<>(new ReferenceNameComparator());
		
		for(String chr : rNamePosition.keySet()) {
			PositionSummary posSum = rNamePosition.get(chr);
			if( posSum == null ) continue;
 			long cov = posSum.getTotalCount();
			if(cov > 0)
				tallys.put(chr, new AtomicLong(cov));
		}
		XmlUtils.outputTallyGroup( XmlUtils.createMetricsNode(parent, null, null, null), "RNAME",null, tallys, true );	
	}	
	private void createMAPQ(Element parent) {				
		//,  sourceName, mapQualityLengths
		if( sourceName.length == 0 ||  sourceName.length != mapQualityLengths.length   ) return;	
		
		Element ele = XmlUtils.createMetricsNode( parent, null, null, null);
		
		for(int j = 0; j < sourceName.length; j ++) {
			
			Map<Integer, AtomicLong> tallys = new HashMap<>();
			for(int i = 0; i < 256; i ++) 
				if(mapQualityLengths[j].get(i) > 0)
					tallys.put( i, new AtomicLong(mapQualityLengths[j].get(i)));
			
			XmlUtils.outputTallyGroup(ele, "MAPQ", sourceName[j], tallys, true);			
		}	 
	}	
	private void createPOS(Element parent){
		parent = QprofilerXmlUtils.createSubElement( parent, XmlUtils.readGroupsEle );
		
		for( String rg :  rgSummaries.keySet()) {
			if( rg.equals(QprofilerXmlUtils.All_READGROUP)) continue;
			ReadGroupSummary summary = rgSummaries.get(rg);
			Element ele = XmlUtils.createMetricsNode(parent,  rg, null, summary.getCountedReads());			
			rNamePosition.keySet().stream().sorted(new ReferenceNameComparator()).forEach( ref->{
				Element cateEle = XmlUtils.createGroupNode(ele, ref);
				for (Entry<Integer, AtomicLong> entry :rNamePosition.get(ref).getCoverageByRg(rg).entrySet()) {
					//middle position of the bin
					int start = entry.getKey() * PositionSummary.BUCKET_SIZE;
					XmlUtils.outputBinNode( cateEle, start, start + PositionSummary.BUCKET_SIZE-1, entry.getValue().get());
				}});			
		}
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
		// check if record has its fail or duplicate flag set. if so, miss out some of the summaries
		//anyway, add to summary and then add to it's readgroup
		rgSummaries.get(QprofilerXmlUtils.All_READGROUP).parseRecord(record); 
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
	
	private void summaryToXml( Element parent ){
		Element summaryElement = QprofilerXmlUtils.createSubElement(parent, QprofilerXmlUtils.summary);
				
		//QprofilerXmlUtils.createSubElement(summaryElement,  XmlUtils.metricsEle);		
		String[][] properties = new String[5][2];
		//eg, <Property value="112" description="Number of cycles with >1% mismatches"/>
		properties[0][0] = CycleSummaryUtils.getBigMDCycleNo(tagReport.tagMDMismatchByCycle, (float) 0.01, tagReport.allReadsLineLengths) +"";
		properties[0][1] = "Number of cycles with greater than 1% mismatches";
		
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
		
		long badReadNum = rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumFailedVendorQuality() +
				rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumSecondary() + rgSummaries.get(QprofilerXmlUtils.All_READGROUP).getNumSupplementary();				
		properties[3][0] = badReadNum + "";
		properties[3][1] = "Discarded reads (FailedVendorQuality, secondary, supplementary)";		
		properties[4][0] = getRecordsParsed() + "";
		properties[4][1] = "Total reads including discarded reads";

		//overall readgroup 
		Element metricsE = XmlUtils.createMetricsNode(summaryElement, "summary",  null, getRecordsParsed());						
		for(String[] property : properties) 
			XmlUtils.outputValueNode(metricsE, property[1], property[0]);
				
		//base	
		long trimBases = 0;
		long maxBases = 0; 	
		Element rgsElement = QprofilerXmlUtils.createSubElement(summaryElement, XmlUtils.readGroupsEle);
		
		for(ReadGroupSummary summary: rgSummaries.values()) 
			if(! summary.getReadGroupId().equals(QprofilerXmlUtils.All_READGROUP)){
				summary.readSummary2Xml(rgsElement);
				summary.pairSummary2Xml(rgsElement); 
				trimBases += summary.getTrimedBases();
				maxBases += summary.getMaxReadLength() * summary.getCountedReads(); 
			}		
		
		if( maxBases == 0) return; //incase there is no readGroup  in bam header
		//overall group at last
		ReadGroupSummary summary = rgSummaries.get(QprofilerXmlUtils.All_READGROUP);
		summary.setMaxBases(maxBases);
		summary.setTrimedBases(trimBases);
		summary.readSummary2Xml(metricsE);	
		summary.pairSummary2Xml(metricsE);	
	}
		
	void parseRNameAndPos( final String rName,  final int position, String rgid ){
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
