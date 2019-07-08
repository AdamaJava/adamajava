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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.SequenceUtil;

import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.SummaryReport;
import org.qcmg.qprofiler2.fastq.FastqSummaryReport;
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
	public static final String[] sourceName = new String[]{ "unPaired", XmlUtils.FIRST_PAIR, XmlUtils.SECOND_PAIR };	
	
	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
	//SEQ first of pair, secondof pair, unpaired
	@SuppressWarnings("unchecked")
	private final CycleSummary<Character>[] seqByCycle= new CycleSummary[]{new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512), new CycleSummary<Character>(cc, 512)};
	private final QCMGAtomicLongArray[] seqBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};

	//QUAL
	@SuppressWarnings("unchecked")
	private final CycleSummary<Integer>[] qualByCycleInteger = new CycleSummary[]{new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512), new CycleSummary<Integer>(ii, 512)};
	private final QCMGAtomicLongArray[] qualBadReadLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128), new QCMGAtomicLongArray(128)};
	private final QCMGAtomicLongArray[] mapQualityLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(256), new QCMGAtomicLongArray(256), new QCMGAtomicLongArray(256)};
	
	// FLAGS
	private final Map<String, AtomicLong> flagBinaryCount = new ConcurrentSkipListMap<String, AtomicLong>(); //summary all read flag
	private final QCMGAtomicLongArray flagIntegerCount = new QCMGAtomicLongArray( 2048 );  //count on each read

	// Coverage chr=>coveragesummary
	private final ConcurrentMap<String, PositionSummary> rNamePosition = new ConcurrentHashMap<String, PositionSummary>(85);  	
	private final QCMGAtomicLongArray p1Lengths = new QCMGAtomicLongArray(1024);
	private final QCMGAtomicLongArray p2Lengths = new QCMGAtomicLongArray(1024);
	private final ConcurrentMap<String, ReadGroupSummary> rgSummaries = new ConcurrentHashMap<>(); 
			
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
 	private final TagSummaryReport2 tagReport = new TagSummaryReport2();
 	
	private Long maxRecords;
	private boolean isFullBamHeader;
	private SAMFileHeader bamHeader;
	private SAMSequenceDictionary samSeqDictionary;
	private List<String> readGroupIds = Arrays.asList( XmlUtils.UNKNOWN_READGROUP ); //init	
			
	public BamSummaryReport2( int maxRecs, boolean isFullBamHeader ) {
		super();		
		if (maxRecs > 0) maxRecords = Long.valueOf(maxRecs);	
		this.isFullBamHeader =  isFullBamHeader;
	}	

	/**
	 * Called once all records have been parsed.
	 * Allows some cleanup to take place - eg. move remaining entries from coverageQueue and add to coverage map
	 */
	public void cleanUp() {	
						
		long length = flagIntegerCount.length();
		for (int i = 0 ; i < length ; i++) 
			if (flagIntegerCount.get(i) > 0) {
				String flagString = FlagUtil.getFlagString(i);
				flagBinaryCount.put( flagString, new AtomicLong(flagIntegerCount.get(i)) );
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
		XmlUtils.bamHeaderToXml(bamReportElement, bamHeader,  isFullBamHeader);
		summaryToXml(bamReportElement ); 	//Summary for all read group
		
		//create bamMertrics
		bamReportElement = XmlElementUtils.createSubElement( bamReportElement, ProfileType.BAM.getReportName()+ XmlUtils.METRICS );
		createQNAME( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.QNAME ));	
		createFLAG( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.FLAG )  );		
		createRNAME( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.RNAME  )); //it is same to RNEXT
		createPOS( XmlElementUtils.createSubElement( bamReportElement,XmlUtils.POS ));	
		createMAPQ( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.MAPQ ));	
		createCigar( XmlElementUtils.createSubElement( bamReportElement,XmlUtils.CIGAR ));
		createTLen( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.TLEN ));
		createSeq( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.SEQ)  );
		createQual( XmlElementUtils.createSubElement( bamReportElement,XmlUtils.QUAL ));
		tagReport.toXml( XmlElementUtils.createSubElement( bamReportElement, XmlUtils.TAG )); 

	}
	
	private void createQNAME(Element parent ){
		 //read name will be under RT Tab	
		Element rgsElement = XmlElementUtils.createSubElement(parent, XmlUtils.READGROUPS);
		for( Entry<String, ReadIDSummary> entry:  readIdSummary.entrySet()){
			//"analysis read name pattern for read group
			Element ele = XmlUtils.createReadGroupNode(rgsElement, entry.getKey());
			entry.getValue().toXml(ele);			
		}
	}
	
	private void createFLAG(Element parent ){
		 if ( null == flagBinaryCount || flagBinaryCount.isEmpty() ) return;		 
		 long counts = flagBinaryCount.values().stream().mapToLong( x -> (long) x.get() ).sum() ;			   	 
		 Element ele = XmlUtils.createMetricsNode(parent,  null,  new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts) )	;		 
	     XmlUtils.outputTallyGroup( ele , "FLAG", flagBinaryCount, true, false);		
	}
 		
	//<SEQ>
	private void createSeq(Element parent){
		
		long counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += seqByCycle[order].getInputCounts();
		}
		Pair rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);
		
		//sequenceBase	
		Element ele = XmlUtils.createMetricsNode( parent,  XmlUtils.SEQ_BASE,  rcPair );	
		for(int order = 0; order < 3; order++) { 	
			seqByCycle[order].toXml( ele,  sourceName[order], seqByCycle[order].getInputCounts() );
		}
				
		//seqLength
		ele = XmlUtils.createMetricsNode( parent,  XmlUtils.SEQ_LENGTH,  rcPair); 
		for(int order = 0; order < 3; order++) { 
			if(seqByCycle[order].getLengthMapFromCycle().isEmpty()) continue;
			XmlUtils.outputTallyGroup( ele, sourceName[order] ,  seqByCycle[order].getLengthMapFromCycle(), true, true );		
		}
		
		//badBase: 
		counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += seqBadReadLineLengths[order].getSum();	
		}
		rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);
		ele = XmlUtils.createMetricsNode( parent,  XmlUtils.BAD_READ,  rcPair); 
		XmlUtils.addCommentChild(ele, FastqSummaryReport.badBaseComment );
		for(int order = 0; order < 3; order++) {
			if( seqBadReadLineLengths[order].toMap().isEmpty() )continue;
			XmlUtils.outputTallyGroup( ele, sourceName[order],  seqBadReadLineLengths[order].toMap(), true , true);			
		}
				
		//1mers is same to baseByCycle
		for( int i : new int[] { 2, 3, KmersSummary.maxKmers } )
			kmersSummary.toXml( parent,i );
	}
	
	//<QUAL>
	private void createQual(Element parent){
		
		long counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += qualByCycleInteger[order].getInputCounts();
		}
		Pair rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);
		
		//"count on quality base",
		Element ele = XmlUtils.createMetricsNode( parent,  XmlUtils.QUAL_BASE,  rcPair); 
		for(int order = 0; order < 3; order++) 			
			qualByCycleInteger[order].toXml(ele, sourceName[order], qualByCycleInteger[order].getInputCounts());
		
		//qual length
		ele = XmlUtils.createMetricsNode( parent,  XmlUtils.QUAL_LENGTH,  rcPair); 
		for(int order = 0; order < 3; order++) {
			if(qualByCycleInteger[order].getLengthMapFromCycle().isEmpty()) continue;		
			XmlUtils.outputTallyGroup( ele, sourceName[order],  qualByCycleInteger[order].getLengthMapFromCycle(), true , true);	
		}
		
		//badBase:  
		counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += seqBadReadLineLengths[order].getSum();	
		}
		rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);

		
		ele = XmlUtils.createMetricsNode( parent,  XmlUtils.BAD_READ,  rcPair); 
		XmlUtils.addCommentChild(ele, FastqSummaryReport.badQualComment);
		for(int order = 0; order < 3; order++) { 
			if( qualBadReadLineLengths[order].toMap().isEmpty() )continue;
			XmlUtils.outputTallyGroup( ele, sourceName[order], qualBadReadLineLengths[order].toMap(), true , true);
		}		
	}	
	
	private void createTLen(Element parent){
        // ISIZE
        Set<String> readGroups =  rgSummaries.keySet();     
        parent = XmlElementUtils.createSubElement(parent, XmlUtils.READGROUPS );  
        
        for(String rg : readGroups ) {
        	String rgName = (readGroups.size() == 1 && rg.equals(XmlUtils.UNKNOWN_READGROUP))? null : rg;
        	//output tLen inside pairSummary, eg. inward, f3f5
        	rgSummaries.get(rg).pairTlen2Xml(XmlUtils.createReadGroupNode(parent, rgName));       	
        }	
	}			
	private void createCigar(Element parent) {
				
        Set<String> readGroups =  rgSummaries.keySet();    
        parent = XmlElementUtils.createSubElement(parent, XmlUtils.READGROUPS );  
        
        for(String rg : readGroups ) {
        	String rgName = (readGroups.size() == 1 && rg.equals(XmlUtils.UNKNOWN_READGROUP))? null : rg;
        	Element ele = XmlUtils.createMetricsNode( XmlUtils.createReadGroupNode(parent, rgName), null, 
        			new Pair<String, Number>(ReadGroupSummary.READ_COUNT,rgSummaries.get(rg).getCigarReadCount()) )	;
        	
        	//cigar string from reads including duplicateReads, notProperPairs and unmappedReads but excluding discardedReads (failed, secondary and supplementary).
        	Map<String, AtomicLong> tallys = new TreeMap<>(new CigarStringComparator());
        	tallys.putAll(	rgSummaries.get(rg).getCigarCount() );
        	XmlUtils.outputTallyGroup( ele ,XmlUtils.CIGAR , tallys, true, false );	
        }       
	}	
	
	private void createRNAME(Element parent){
		if (null == samSeqDictionary) return;				
		Map<String, AtomicLong> tallys = new TreeMap<>(new ReferenceNameComparator());	
		long readCount = 0;
		for(String chr : rNamePosition.keySet()) {
			PositionSummary posSum = rNamePosition.get(chr);
			if( posSum == null ) continue;
 			long cov = posSum.getTotalCount();			
			if(cov > 0) {
				readCount += cov; 
				tallys.put(chr, new AtomicLong(cov));
			}
		}
		Element ele = XmlUtils.createMetricsNode(parent,  null,  new Pair<String, Number>(ReadGroupSummary.READ_COUNT,readCount) )	;		
		XmlUtils.outputTallyGroup( ele, XmlUtils.RNAME, tallys, true, false );
	}
	
	private void createMAPQ(Element parent) {
		//  sourceName, mapQualityLengths
		if( sourceName.length == 0 ||  sourceName.length != mapQualityLengths.length ) return;	
				
		Element ele = XmlUtils.createMetricsNode( parent, null, null);	
		long sum = 0;
		for(int j = 0; j < sourceName.length; j ++) {			
			Map<Integer, AtomicLong> tallys = new HashMap<>();
			for(int i = 0; i < 256; i ++) {
				if(mapQualityLengths[j].get(i) > 0) {
					sum += mapQualityLengths[j].get(i);
					tallys.put( i, new AtomicLong(mapQualityLengths[j].get(i)));
					}
				}	
			XmlUtils.outputTallyGroup(ele, sourceName[j] , tallys, true, true);		
		}
		ele.setAttribute(ReadGroupSummary.READ_COUNT, sum+"");		
	}
	
	private void createPOS(Element parent){
		parent = XmlElementUtils.createSubElement( parent, XmlUtils.READGROUPS );
		
		for( String rg :  rgSummaries.keySet()) {
			long readCount = rNamePosition.values().stream().mapToLong( x -> x.getTotalCountByRg(rg) ).sum();
			Element ele = XmlUtils.createMetricsNode(XmlUtils.createReadGroupNode(parent, rg)  , null, new Pair<String, Number>(ReadGroupSummary.READ_COUNT, readCount));			
			rNamePosition.keySet().stream().sorted(new ReferenceNameComparator()).forEach( ref-> 
				XmlUtils.outputBins(ele, ref, rNamePosition.get(ref).getCoverageByRg(rg), PositionSummary.BUCKET_SIZE));		
		}
	}

	/**
	 * Parse a SAMRecord Collate various pieces of info from the SAMRecord ready for the summariser to retrieve
	 * @param record SAMRecord next row in file
	 */
	public void parseRecord( final SAMRecord record ) {
 		updateRecordsInputed();
 				
		String readGroup = XmlUtils.UNKNOWN_READGROUP;
		if(record.getReadGroup() != null && record.getReadGroup().getId() != null )
			readGroup = record.getReadGroup().getReadGroupId();								
			
		final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
		
		// MAPQ (Mapping Quality)
		final int mapQ = record.getMappingQuality();
		mapQualityLengths[order].increment(mapQ);

		// only FLAGS are always summarised		
		flagIntegerCount.increment(record.getFlags());	
			
		//excludes repeated reads for tags
		if ( !record.getSupplementaryAlignmentFlag() &&	!record.isSecondaryAlignment() && !record.getReadFailsVendorQualityCheckFlag() ) {
			tagReport.parseTAGs(record);			
		} 
						
		// check if record has its fail or duplicate flag set. if so, miss out some of the summaries
		ReadGroupSummary rgSumm = rgSummaries.computeIfAbsent(readGroup, k -> new ReadGroupSummary(k));	
		if( rgSumm.parseRecord(record) ) {	
			
			//QName	
			readIdSummary.computeIfAbsent( readGroup, (k) -> new ReadIDSummary() ).parseReadId( record.getReadName() );
 			
			// SEQ 
			byte[] data = record.getReadBases();			
			if (record.getReadNegativeStrandFlag()) {
				SequenceUtil.reverseComplement(data );
			}		
			seqByCycle[order].parseByteData(data );			
			SummaryReportUtils.tallyBadReadsAsString( record.getReadBases(), seqBadReadLineLengths[order] );	
			kmersSummary.parseKmers( record.getReadBases(), record.getReadNegativeStrandFlag(), order );

			// QUAL 
			data = record.getBaseQualities();
			if (record.getReadNegativeStrandFlag()) {
				SequenceUtil.reverseQualities(data);
			}
			//data = (record.getReadNegativeStrandFlag())? SummaryReportUtils.getReversedQual(record.getBaseQualities()) : record.getBaseQualities();
			qualByCycleInteger[order].parseByteData(data);
			SummaryReportUtils.tallyQualScores( record.getBaseQualities(), qualBadReadLineLengths[order] );				
 			
 						
			//TLen is done inside readGroupSummary.ParseRecord			
			// MRNM is same to RNAME
 			
			// RNAME & POS			
			parseRNameAndPos( record.getReferenceName(), record.getAlignmentStart(), readGroup );	// Position value
			
			//it is not include hard clip
			if (record.getReadPairedFlag()){  
				if (record.getFirstOfPairFlag()) p1Lengths.increment(record.getReadBases().length);
				else if (record.getSecondOfPairFlag())  p2Lengths.increment(record.getReadBases().length);
			} 
		}
	}
		
	private void summaryToXml( Element parent ){
		Element summaryElement = XmlElementUtils.createSubElement(parent, XmlUtils.BAM_SUMMARY);
		
		long  discardReads = 0,maxBases = 0,duplicateBase = 0, unmappedBase = 0, noncanonicalBase = 0;
		long  trimBases = 0,overlappedBase = 0, softClippedBase = 0, hardClippedBase = 0;
		long  readCount = 0, lostBase=0; //baseCount = 0,
		Element rgsElement = XmlElementUtils.createSubElement(summaryElement, XmlUtils.READGROUPS);		
		for(ReadGroupSummary summary: rgSummaries.values()) { 					
			try {	
				
				Element rgEle = XmlUtils.createReadGroupNode(rgsElement, summary.getReadGroupId());
				summary.readSummary2Xml(rgEle);
				summary.pairSummary2Xml(rgEle); 
				//presummary
				lostBase += summary.getDuplicateBase() + summary.getUnmappedBase() + summary.getnotPoperPairedBase() + 
						summary.getTrimmedBase() + summary.getOverlappedBase() + summary.getSoftClippedBase() + summary.getHardClippedBase();
				maxBases += summary.getReadCount() * summary.getMaxReadLength();						
				duplicateBase += summary.getDuplicateBase();
				unmappedBase += summary.getUnmappedBase();
				noncanonicalBase += summary.getnotPoperPairedBase();
				trimBases += summary.getTrimmedBase();
				overlappedBase += summary.getOverlappedBase();
				softClippedBase += summary.getSoftClippedBase();
				hardClippedBase += summary.getHardClippedBase();
				
				discardReads += summary.getDiscardreads();
				readCount += summary.getReadCount();
			} catch (Exception e) {	logger.warn(e.getMessage()); }
		}
	
		//overall readgroup 
		//Element metricsE = XmlUtils.createMetricsNode(summaryElement, "summary1", new Pair( ReadGroupSummary.READ_COUNT, getRecordsInputed()));	
		Element metricsE = XmlUtils.createMetricsNode(summaryElement, XmlUtils.OVERALL, null);	
		XmlUtils.outputValueNode(metricsE, "Number of cycles with greater than 1% mismatches",
				CycleSummaryUtils.getBigMDCycleNo(tagReport.tagMDMismatchByCycle, (float) 0.01, tagReport.allReadsLineLengths));		
				
		int point = 1;
		for(QCMGAtomicLongArray length: new QCMGAtomicLongArray[] {p1Lengths, p2Lengths}) {
			long runningTally = 0, total = 0;
			for (int i = 1 ; i < length.length() ; i++) {
				if (length.get(i) > 0) {
					total += length.get(i);
					runningTally += (length.get(i) * i);
				}
			}			
			String str = String.format("Average length of %s-of-pair reads", point == 1? "first" : "second");		
			XmlUtils.outputValueNode(metricsE, str, total > 0 ? (int)(runningTally / total) : 0 );			
			point ++;
		}
		
		XmlUtils.outputValueNode(metricsE, "Discarded reads (FailedVendorQuality, secondary, supplementary)", discardReads);	
		XmlUtils.outputValueNode(metricsE, "Total reads including discarded reads", getRecordsInputed());	
				
		metricsE = XmlUtils.createMetricsNode(summaryElement, XmlUtils.ALL_BASE_LOST, null);		
		
		//readCount: includes duplicateReads, notProperPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.READ_COUNT, readCount);	
		//baseCount: the sum of baseCount from all read group
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.BASE_COUNT, maxBases);	
		
		double percentage = (maxBases == 0)? 0: 100 * (double) lostBase /  maxBases ;
		//basePercent: basesLost / baseCount, basesLost: the sum of basesLost from all read group
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.BASE_LOST_COUNT + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage);	

		percentage = (maxBases == 0)? 0: 100 * (double) duplicateBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_DUPLICATE + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) unmappedBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_UNMAPPED + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) noncanonicalBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_NOT_PROPER_PAIR + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) trimBases /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_TRIM + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) softClippedBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_SOFTCLIP + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) hardClippedBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_HARDCLIP + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
		
		percentage = (maxBases == 0)? 0: 100 * (double) overlappedBase /  maxBases ;				
		XmlUtils.outputValueNode(metricsE, ReadGroupSummary.NODE_OVERLAP + "_" +ReadGroupSummary.BASE_LOST_PERCENT,  percentage );	
				
	}
		
	void parseRNameAndPos( final String rName,  final int position, String rgid ){
		PositionSummary ps = (PositionSummary) rNamePosition.computeIfAbsent( rName, k->new PositionSummary( readGroupIds) );
		ps.addPosition( position, rgid );
	}
		
	//setting
	public void setBamHeader(SAMFileHeader header, boolean isFullBamHeader ) { 
		this.bamHeader = header;	
		this.isFullBamHeader = isFullBamHeader;
	}
//	public String getBamHeader() {	
//		return bamHeader.getSAMString();	
//	}
	public void setSamSequenceDictionary(SAMSequenceDictionary samSeqDictionary) {	
		this.samSeqDictionary = samSeqDictionary;	
	}
//	public SAMSequenceDictionary getSamSequenceDictionary() { 
//		return samSeqDictionary;	
//	}	

	
	
	// ///////////////////////////////////////////////////////////////////////
	// The following methods are used by the test classes
	// ///////////////////////////////////////////////////////////////////////
	public void setReadGroups(List<String> ids ){	
		readGroupIds = Stream.concat( ids.stream(), readGroupIds.stream()).collect(Collectors.toList()); 		
	}
	ConcurrentMap<String, PositionSummary> getRNamePosition() {	
		return rNamePosition; 
	}
	CycleSummary<Character> getSeqByCycle(int flagFirstOfPair) { 
		return   seqByCycle[flagFirstOfPair] ; 	
	}
}
