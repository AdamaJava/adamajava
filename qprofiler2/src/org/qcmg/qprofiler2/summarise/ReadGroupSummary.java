package org.qcmg.qprofiler2.summarise;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.BwaPair;
import org.qcmg.common.model.QCMGAtomicLongArray;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class ReadGroupSummary {

	public static final String NODE_SOFTCLIP = "softClippedBases";
	public static final String NODE_TRIM = "trimmedBases";	
	public static final String NODE_HARDCLIP = "hardClippedBases";
	public static final String NODE_READ_LENGTH = "readLength" ; 
	public static final String NODE_PAIR_TLEN = "tLen" ; 	
	public static final String NODE_OVERLAP = "overlappedBases";	
	public static final String NODE_DUPLICATE = "duplicateReads";
	public static final String NODE_UNMAPPED = "unmappedReads";
	public static final String NODE_NOT_PROPER_PAIR = "notProperPairs";

	public static final String MIN = "min";	
	public static final String MAX = "max";
	public static final String MEAN = "mean"; 
	public static final String MODE =  "mode"; 
	public static final String MEDIAN = "median" ; 
	public static final String READ_COUNT = "readCount";
	public static final String PAIR_COUNT = "pairCount";
	public static final String BASE_COUNT = "basesCount"; 
	public static final String BASE_LOST_COUNT = "basesLostCount"; 
	public static final String BASE_LOST_PERCENT = "basesLostPercent"; 	
	public static final String UNPAIRED_READ = "unpairedReads";
	
	 			
	// softclips, hardclips, read length; 	
	QCMGAtomicLongArray softClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray hardClip = new QCMGAtomicLongArray(128);
	
	// record read length excluding the discard reads but includes duplicate,unmapped and nonCanonicalReads	
	QCMGAtomicLongArray readLength = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray readLengthBins = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray forTrimLength = new QCMGAtomicLongArray(128);	
	private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<>();
	// must be concurrent set for multi threads
	private final ConcurrentMap<Integer, PairSummary> pairCategory = new ConcurrentHashMap<>();

	//Isize
	public final static int middleTlenValue = 5000;
	QCMGAtomicLongArray tLenOverall = new QCMGAtomicLongArray(middleTlenValue);

	// bad reads information
	AtomicLong duplicate = new AtomicLong();
	AtomicLong secondary  = new AtomicLong();
	AtomicLong supplementary  = new AtomicLong();
	AtomicLong failedVendorQuality  = new AtomicLong();
	AtomicLong cigarRead = new AtomicLong();
	AtomicLong unmapped  = new AtomicLong();
	AtomicLong unpaired  = new AtomicLong();
	AtomicLong notProperPairedReads  = new AtomicLong();
	
	AtomicLong inputReadCounts  = new AtomicLong();	
	private SummaryReportUtils.TallyStats overlapStats ;
	private SummaryReportUtils.TallyStats softclipStats ;
	private SummaryReportUtils.TallyStats hardclipStats ;
	private SummaryReportUtils.TallyStats readlengthStats ;
	private SummaryReportUtils.TallyStats pairtLenStats;
	private SummaryReportUtils.TallyStats trimBaseStats;
		
	private final String readGroupId;
	private final boolean isLongReadBam;
	
	public ReadGroupSummary(String rgId, boolean isLongReadBam) {
		this.readGroupId = rgId;
        this.isLongReadBam = isLongReadBam;
    }
	
	public String getReadGroupId() {
		return readGroupId; 
	}	
		
	public int getMaxReadLength() {
		return (int) readlengthStats.getMax(); 
	}
	
	public long getCigarReadCount() {
		return cigarRead.get();
	}
	
	public long getOverlappedBase() {
		return overlapStats.getBaseCounts();
	}
	
	public long getSoftClippedBase() {
		return softclipStats.getBaseCounts(); 
	}
	
	public long getHardClippedBase() {
		return hardclipStats.getBaseCounts();
	}
	
	public long getTrimmedBase() {
		return trimBaseStats.getBaseCounts(); 
	}	

	public long getReadCount() {
		return this.readlengthStats.getReadCounts();
	}
	
	public long getDuplicateBase() {
		return this.duplicate.get() * getMaxReadLength(); 
	}	
	
	public long getUnmappedBase() {
		return this.unmapped.get() * getMaxReadLength(); 
	}	
	
	public long getNotProperPairedBase() {
		return notProperPairedReads.get() * getMaxReadLength(); 
	}
		
	/**
	 * classify record belongs (duplicate...) and count the number. If record is parsed, then count the hard/soft clip bases, pair mapping overlap bases and pairs information
	 * 
	 * @param record is a SAM record
	 * @return true if record parsed; otherwise return false since record duplicate, supplementary, secondary, failedVendorQuality, unmapped or nonCanonical. 
	 */
	public boolean parseRecord( final SAMRecord record ) {
				
		// record input reads number
		inputReadCounts.incrementAndGet();  
				
		// find discard reads and return false
		if ( record.getSupplementaryAlignmentFlag()) {			
			supplementary.incrementAndGet();
			return false;
		} else if ( record.isSecondaryAlignment() ) {
			secondary.incrementAndGet();
			return false;
		} else if (record.getReadFailsVendorQualityCheckFlag()) {
			failedVendorQuality.incrementAndGet();
			return false;
		} 		
		
		// parsing cigar
		// cigar string from reads including duplicateReads, nonCanonicalPairs and unmappedReads but excluding discardedReads (failed, secondary and supplementary).
		int lHard = 0;
		int lSoft = 0;
		if (null != record.getCigar()) {
			cigarRead.incrementAndGet();
			for (CigarElement ce : record.getCigar().getCigarElements()) {			 
				if ( ! CigarOperator.M.equals(ce.getOperator())) {
					String key = "" + ce.getLength() + ce.getOperator();
					cigarValuesCount.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
					if (ce.getOperator().equals(CigarOperator.HARD_CLIP)) {
						lHard += ce.getLength();
					 } else if (ce.getOperator().equals(CigarOperator.SOFT_CLIP)) {
						lSoft += ce.getLength();
					 }
				}			 			 				 
			 }	 
		}
		
		readLength.increment(record.getReadLength() + lHard);

		// find mapped badly reads and return false	
		if (record.getDuplicateReadFlag()) {
			duplicate.incrementAndGet();
			return false;
		}

		if (record.getReadUnmappedFlag() ) {
			unmapped.incrementAndGet();
			return false;
		} 
		
		// check pair orientation, tLen, mate
		if (record.getReadPairedFlag()) {

			//overall tLen for isize calculation
			int tLen = record.getInferredInsertSize();
			// to avoid double counts, we only select one of Pair: tLen > 0 or firstOfPair with tLen==0;
			if (tLen > 0 || (tLen ==0 && record.getFirstOfPairFlag())) {
				if (tLen < middleTlenValue) {
					tLenOverall.increment(tLen);
				}
			}

			BwaPair.Pair pairType = BwaPair.getPairType(record);
			boolean isProper = record.getProperPairFlag();
			int key = isProper ? pairType.id : pairType.id * -1;	
			pairCategory.computeIfAbsent(key, e -> new PairSummary( pairType, isProper)).parse(record);
			// we always parse pairs but stop here if not ProperPair
			if ( !isProper ) {
				notProperPairedReads.incrementAndGet();
				return false;
			}
		} else {
			unpaired.incrementAndGet();
		}
		
		if (lHard > 0) {
			hardClip.increment(lHard);
		}
		
		if (lSoft > 0) {
			softClip.increment(lSoft);
		}

		// record read length excluding the discard reads duplicate.get() + unmapped.get() + getnonCanonicalReadsCount();	
		// due to it for trimmed base calculation as well
		// Adapter trimming not relevant for long read - the adapter trimming is calculated based on the length of the current read vs
		// the length of the maximum read and long read has variable length so it is not possible to calculate the adapter trimming for long read
		if (! isLongReadBam) {
			forTrimLength.increment(record.getReadLength() + lHard);
		}

		return true;  
	}
	
	
	public ConcurrentMap<String, AtomicLong> getCigarCount() {		 
		return cigarValuesCount;
	}
				
	public long getDiscardreads() {
		return supplementary.get() + failedVendorQuality.get() + secondary.get();
	}
		

	/**
	 * check all global value and assign the summary value
	 * eg. private long trimmedBase = 0;
	 */
	public void preSummary() {				
		// check overlap and tLen from pairSummary 
		QCMGAtomicLongArray overlapBase = new QCMGAtomicLongArray(PairSummary.segmentSize);	
		QCMGAtomicLongArray tLenOverall = new QCMGAtomicLongArray(PairSummary.middleTlenValue);	  
		for (PairSummary p : pairCategory.values()) {
			if ( !p.isProperPair) {
				continue;
			}
			
			for (int i = 0; i < PairSummary.segmentSize; i ++) {			 	
				overlapBase.increment(i, p.getoverlapCounts().get(i) );					 
			}
			
			for (int i = 0; i < PairSummary.middleTlenValue; i ++) {	
				tLenOverall.increment(i, p.getTLENCounts().get(i));					 
			}			
		}
		this.overlapStats = new SummaryReportUtils.TallyStats(overlapBase);
		this.pairtLenStats = new SummaryReportUtils.TallyStats(tLenOverall );
		this.softclipStats = new SummaryReportUtils.TallyStats( softClip);
		this.hardclipStats = new SummaryReportUtils.TallyStats( hardClip);
		this.readlengthStats = new SummaryReportUtils.TallyStats( readLength );	
		
		int maxLength = (int)readlengthStats.getMax();
		QCMGAtomicLongArray trimmedBase = new QCMGAtomicLongArray(maxLength + 1);
		for (int i = 0 ; i < forTrimLength.length() ; i ++)	 {			
			if (forTrimLength.get(i) == 0 || maxLength == i ) {
				continue;
			}
			trimmedBase.increment( maxLength - i, forTrimLength.get(i));
		}
		this.trimBaseStats = new SummaryReportUtils.TallyStats( trimmedBase );

	}
	
	public void readSummary2Xml(Element parent ) {
		
		preSummary();
	 		 
		// add to xml RG_Counts		
		Element rgElement = XmlUtils.createMetricsNode(parent,"basesLost", null);					
		// add discarded read Stats to readgroup summary	
		badReadStats( rgElement, NODE_DUPLICATE, duplicate.get());
		badReadStats( rgElement, NODE_UNMAPPED, unmapped.get() );		
		badReadStats( rgElement, NODE_NOT_PROPER_PAIR, notProperPairedReads.get());
		lostBaseStats( rgElement, NODE_TRIM, trimBaseStats );		
		lostBaseStats( rgElement, NODE_SOFTCLIP, softclipStats );
		lostBaseStats( rgElement, NODE_HARDCLIP, hardclipStats  );
		lostBaseStats( rgElement, NODE_OVERLAP, overlapStats );
				
		// create node for overall	
		rgElement = XmlUtils.createMetricsNode(parent,"reads", new Pair<>(READ_COUNT, inputReadCounts.get()));
		Element ele = XmlUtils.createGroupNode(rgElement, XmlUtils.DISCARD_READS );
		XmlUtils.outputValueNode(ele, "supplementaryAlignmentCount", supplementary.get());
		XmlUtils.outputValueNode(ele, "secondaryAlignmentCount", secondary.get());
		XmlUtils.outputValueNode(ele, "failedVendorQualityCount",failedVendorQuality.get());
				
		// readLength and tLen
		for (String name : new String[] {NODE_READ_LENGTH, NODE_PAIR_TLEN}) {
			ele = XmlUtils.createGroupNode(rgElement, name );
			SummaryReportUtils.TallyStats stats = name.equals(NODE_READ_LENGTH) ? readlengthStats : pairtLenStats;
			String countName = name.equals(NODE_READ_LENGTH) ? READ_COUNT : PAIR_COUNT;			
			// readCount: includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).
			// pairCount: only count properPaired reads which have a positive TLEN value or zero value but it is marked as firstOfPair						
			XmlUtils.outputValueNode(ele, countName, stats.getReadCounts());	
			XmlUtils.outputValueNode(ele, MAX, stats.getMax());
			XmlUtils.outputValueNode(ele, MEAN, stats.getMean());
			XmlUtils.outputValueNode(ele, MODE, stats.getMode());
			XmlUtils.outputValueNode(ele, MEDIAN, stats.getMedium());				
		}		
		
		// add overall information to current readgroup element	
		long maxBases = getReadCount() * readlengthStats.getMax() ;
		long lostBase = (duplicate.get() + unmapped.get() + notProperPairedReads.get()  ) * getMaxReadLength()
				+ trimBaseStats.getBaseCounts() + softclipStats.getBaseCounts() + hardclipStats.getBaseCounts() + overlapStats.getBaseCounts();
		final double lostPercent =  maxBases == 0 ? 0 : 100 * (double) lostBase / maxBases;

		ele = XmlUtils.createGroupNode(rgElement, "countedReads" );
		XmlUtils.outputValueNode(ele, UNPAIRED_READ,  unpaired.get());	
		// READ_COUNT : includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).						
		XmlUtils.outputValueNode( ele, READ_COUNT,  getReadCount() );
		// BASE_COUNT :  READ_COUNT  * readMaxLength
		XmlUtils.outputValueNode( ele, BASE_COUNT, maxBases);	
		// BASE_LOST_COUNT : readMaxLength * (duplicateReads + nonCanonicalPairs + unmappedReads) + trimmedBases + softClippedBases + hardClippedBases + overlappedBases				
		XmlUtils.outputValueNode( ele, BASE_LOST_COUNT,  lostBase);	
		// basesLostPercent: basesLostCount / basesCount		
		XmlUtils.outputValueNode( ele, BASE_LOST_PERCENT , lostPercent );	
		
	}
	 	 
	public void pairSummary2Xml( Element parent ) {
		for (boolean isProper : new boolean[] {true, false}) {
			// add to xml RG_Counts
			String name = isProper ? "properPairs" : "notProperPairs";
			Element ele =  XmlUtils.createMetricsNode( parent, name, null );
			long sum = 0;
			for (PairSummary p : pairCategory.values()) {
				if ( p.isProperPair == isProper) {
					p.toSummaryXml(ele);	
					sum += p.getFirstOfPairCounts();
				}			
			}
			// can't really count the pair number due to RAM limits, just pickup number of firstOfPair
			ele.setAttribute( PAIR_COUNT, sum + "");  			
		}
	}

	public void readLength2Xml( Element parent ) {

		Map<String, Element> metricEs = new HashMap<>();
		Map<String, AtomicLong> metricCounts = new HashMap<>();

		String name = "readLength";
		// sum all pairCounts belong to metrics section
		AtomicLong cPairs =  metricCounts.computeIfAbsent(name,  k ->  new AtomicLong());
		cPairs.addAndGet(readLength.getSum());
		// output pair tLen to classified section
		if (isLongReadBam) {
			Element ele = metricEs.computeIfAbsent(name,  k -> XmlUtils.createMetricsNode( parent, k, null));
			for(int i = 0; i < readLength.length(); i ++) {
				if(readLength.get(i) > 0) {
					if (i <=1000) {
						readLengthBins.increment(i, readLength.get(i));
					} else {
						int roundedLength = (int) (Math.ceil((double)i/1000)*1000);
						readLengthBins.increment(roundedLength, readLength.get(i));
					}
				}
			}

			XmlUtils.outputTallyGroup( ele, "readLength", readLengthBins.toMap(), false, true );

			// add pairCounts into metrics elements
			for (Entry<String, Element> entry : metricEs.entrySet()) {
				entry.getValue().setAttribute(ReadGroupSummary.READ_COUNT,
						metricCounts.get(entry.getKey()).get() + "" );
			}
		} else{
			Element ele = metricEs.computeIfAbsent(name,  k -> XmlUtils.createMetricsNode( parent, k, null));
			XmlUtils.outputTallyGroup( ele, "readLength", readLength.toMap(), false, true );

			// add pairCounts into metrics elements
			for (Entry<String, Element> entry : metricEs.entrySet()) {
				entry.getValue().setAttribute(ReadGroupSummary.READ_COUNT,
						metricCounts.get(entry.getKey()).get() + "" );
			}
		}

	}

	public void pairTlen2Xml( Element parent ) {
		Map<String, Element> metricEs = new HashMap<>();
		Map<String, AtomicLong> metricCounts = new HashMap<>();

		// For isize, write overall counts
		AtomicLong cPairs =  metricCounts.computeIfAbsent("Overall",  k ->  new AtomicLong());
		cPairs.addAndGet(tLenOverall.getSum());
		Element ele = metricEs.computeIfAbsent("Overall",  k -> XmlUtils.createMetricsNode( parent, k, null));
		XmlUtils.outputTallyGroup( ele, "Overall", tLenOverall.toMap(), false, true );

		for (PairSummary p : pairCategory.values()) {
			String name = p.isProperPair ? "tLenInProperPair" : "tLenInNotProperPair";
			// sum all pairCounts belong to metrics section
			cPairs =  metricCounts.computeIfAbsent(name,  k ->  new AtomicLong());
			cPairs.addAndGet(p.getTLENCounts().getSum());
			// output pair tLen to classified section
			ele = metricEs.computeIfAbsent(name,  k -> XmlUtils.createMetricsNode( parent, k, null));
			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getTLENCounts().toMap(), false, true );  
					
			name = p.isProperPair ? "overlapBaseInProperPair" : "overlapBaseInNotProperPair";
			// sum all pairCounts belong to metrics section
			cPairs =  metricCounts.computeIfAbsent(name,  k  ->  new AtomicLong());
			cPairs.addAndGet(p.getoverlapCounts().getSum());
			// output pair overlap to classified section
			ele = metricEs.computeIfAbsent(name, k -> XmlUtils.createMetricsNode( parent, k, null));

			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getoverlapCounts().toMap(), false , true); 
		}
		
		// add pairCounts into metrics elements
		for (Entry<String, Element> entry : metricEs.entrySet()) {
			entry.getValue().setAttribute(ReadGroupSummary.PAIR_COUNT, 
					metricCounts.get(entry.getKey()).get() + "" );
		}		
	}
	
	// for duplicate, unmapped and not proper paired reads
	private void badReadStats(Element parent, String nodeName, long reads ) {
		Element ele = XmlUtils.createGroupNode(parent, nodeName);				
		XmlUtils.outputValueNode(ele, READ_COUNT, reads);	
		XmlUtils.outputValueNode(ele, BASE_LOST_COUNT, reads * getMaxReadLength());		
		double percentage = 100 * (double) reads / getReadCount() ;
		XmlUtils.outputValueNode(ele, BASE_LOST_PERCENT,  percentage);		 	 
	}	
	
	private void lostBaseStats(Element parent, String nodeName, SummaryReportUtils.TallyStats stats ) {
		long maxBases = getReadCount() * getMaxReadLength();		 
		Element ele = XmlUtils.createGroupNode(parent, nodeName);	
		XmlUtils.outputValueNode(ele, MIN, stats.getMin());
		XmlUtils.outputValueNode(ele, MAX, stats.getMax());
		XmlUtils.outputValueNode(ele, MEAN, stats.getMean());
		XmlUtils.outputValueNode(ele, MODE, stats.getMode());
		XmlUtils.outputValueNode(ele, MEDIAN, stats.getMedium());
		XmlUtils.outputValueNode(ele, READ_COUNT, stats.getReadCounts());
		XmlUtils.outputValueNode(ele, BASE_LOST_COUNT, stats.getBaseCounts());
		
		// deal with boundary value, missing reads
		double percentage = (maxBases == 0) ? 0 : 100 * (double) stats.getBaseCounts() /  maxBases ;				
		XmlUtils.outputValueNode(ele, BASE_LOST_PERCENT,  percentage );		
	}

}
