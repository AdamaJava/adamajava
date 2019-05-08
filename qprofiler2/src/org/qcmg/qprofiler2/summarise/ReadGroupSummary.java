package org.qcmg.qprofiler2.summarise;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.Pair;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class ReadGroupSummary {

	public final static int errReadLimit  = 10;	
	//xml node name 	
	public final static String node_readgroup = "readGroup";		
	public final static String node_softClip = "softClippedBases";
	public final static String node_trim = "trimmedBases";	
	public final static String node_hardClip = "hardClippedBases";
	public final static String node_readLength = "readLength" ; 
	public final static String node_pairTlen = "tLen" ; 	
	public final static String node_overlap = "overlappedBases";	
	public final static String node_duplicate = "duplicateReads";
	public final static String node_secondary = "secondary";
	public final static String node_supplementary = "supplementary"; 	
	public final static String node_unmapped = "unmappedReads";
	public final static String node_nonCanonicalPair = "notProperPairs";
	public final static String node_failedVendorQuality = "failedVendorQuality";

		
	public final static String smin= "min";	
	public final static String smax = "max";
	public final static String smean = "mean"; 
	public final static String smode =  "mode"; 
	public final static String smedian = "median" ; 
	public final static String sreadCount = "readCount";
	public final static String spairCount = "pairCount";
	public final static String sbaseCount = "basesCount"; // 
	public final static String slostBase = "basesLostCount"; 
	public final static String sbasePercent = "basesLostPercent"; 	
	public final static String sunpaired ="unpairedReads";
	 			
	//softclips, hardclips, read length; 	
	QCMGAtomicLongArray softClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray hardClip = new QCMGAtomicLongArray(128);
	
	//record read length excluding the discard reads but includes duplicate,unmapped and nonCanonicalReads	
	QCMGAtomicLongArray readLength = new QCMGAtomicLongArray(128);	
	QCMGAtomicLongArray forTrimLength = new QCMGAtomicLongArray(128);	
	private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<String, AtomicLong>();
	//must be concurrent set for multi threads
	private final ConcurrentMap<Integer, PairSummary> pairCategory = new ConcurrentHashMap<>();

//	AtomicInteger max_isize = new AtomicInteger(); 	
	//bad reads inforamtion
	AtomicLong duplicate = new AtomicLong();
	AtomicLong secondary  = new AtomicLong();
	AtomicLong supplementary  = new AtomicLong();
	AtomicLong failedVendorQuality  = new AtomicLong();
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
	public ReadGroupSummary(String rgId){ this.readGroupId = rgId; }
	
	public String getReadGroupId(){ return readGroupId;  }	
		
	public int getMaxReadLength() { return (int) readlengthStats.getMax(); }
	
	public long getOverlappedBase() { return overlapStats.getBaseCounts(); }
	public long getSoftClippedBase() { return softclipStats.getBaseCounts(); }
	public long getHardClippedBase() { return hardclipStats.getBaseCounts(); }
	public long getTrimmedBase() { return trimBaseStats.getBaseCounts(); }	
	public long getReadCount() { return this.readlengthStats.getReadCounts(); }
	public long getDuplicateBase() { return this.duplicate.get() * getMaxReadLength(); }	
	public long getUnmappedBase() { return this.unmapped.get() * getMaxReadLength(); }	
	public long getnotPoperPairedBase() {  return notProperPairedReads.get() * getMaxReadLength(); }


		
	/**
	 * classify record belongs (duplicate...) and count the number. If record is parsed, then count the hard/soft clip bases, pair mapping overlap bases and pairs information
	 * @param record
	 * @return true if record parsed; otherwise return false since record duplicate, supplementary, secondary, failedVendorQuality, unmapped or nonCanonical. 
	 */
	public boolean parseRecord( final SAMRecord record ){
				
		//record input reads number
		inputReadCounts.incrementAndGet();  
				
		//find discard reads and return false
		if ( record.getSupplementaryAlignmentFlag()) {			
			supplementary.incrementAndGet();
			return false;
		}else if( record.isSecondaryAlignment() ) {
			secondary.incrementAndGet();
			return false;
		}else if(record.getReadFailsVendorQualityCheckFlag()) {
			failedVendorQuality.incrementAndGet();
			return false;
		} 
		
		
		//parseing cigar
		//cigar string from reads including duplicateReads, nonCanonicalPairs and unmappedReads but excluding discardedReads (failed, secondary and supplementary).
		int lHard = 0, lSoft = 0;
		if (null != record.getCigar()) {  			
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
		readLength.increment(record.getReadLength()+lHard);	
//		//record readlength includeing hardclip and duplicat, unmapped etc reads
//		int lenght_clip = record.getReadLength()+lHard;
//		if(this.maxReadLength < lenght_clip  )
//			this.maxReadLength = lenght_clip ;

			
		//find mapped badly reads and return false	
		if(record.getDuplicateReadFlag()){
			duplicate.incrementAndGet();
			return false;
		}

		if(record.getReadUnmappedFlag() ){
			unmapped.incrementAndGet();
			return false;
		} 
		
		//check pair orientaiton, tLen, mate
		if(record.getReadPairedFlag()) {
			PairSummary.Pair pairType = PairSummary.getPairType(record);
			boolean isProper = record.getProperPairFlag();
			int key = isProper? pairType.id * 2 :  pairType.id;			 
			pairCategory.computeIfAbsent(key, e-> new PairSummary( pairType, isProper)).parse(record);
			//we always parse pairs but stop here if not ProperPair
			if( !isProper ) { 
				notProperPairedReads.incrementAndGet();
				return false;
			}
		} else {
			unpaired.incrementAndGet();
		}
		
		hardClip.increment(lHard);
		softClip.increment(lSoft);
		//record read length excluding the discard reads duplicate.get() + unmapped.get() + getnonCanonicalReadsCount();	
		//due to it for trimmed base caculation as well
		forTrimLength.increment(record.getReadLength()+lHard);	
		
	 				
		return true;  
	}
	
	
	public ConcurrentMap<String, AtomicLong> getCigarCount() {		 
		return cigarValuesCount;
	}
				
	public long getDiscardreads() {
		return supplementary.get() + failedVendorQuality.get() + secondary.get();
	}
		

	/**
	 * check all globle value and assign the sumamry value
	 * eg.  
	 * 	private long trimedBase = 0; 	
	 */
	public void preSummary() {				
		//check overlap and tLen from pairSummary 
		QCMGAtomicLongArray overlapBase = new QCMGAtomicLongArray(PairSummary.segmentSize);	
		QCMGAtomicLongArray tLenOverall = new QCMGAtomicLongArray(PairSummary.middleTlenValue);	  
		for(PairSummary p : pairCategory.values()) {
			if( !p.isProperPair) continue; 
			for(int i = 0; i < PairSummary.segmentSize; i ++) {			 	
				overlapBase.increment(i, p.getoverlapCounts().get(i) );					 
			}
			
			for(int i = 0; i < PairSummary.middleTlenValue; i ++) {	
				tLenOverall.increment(i, p.getTLENCounts().get(i));					 
			}			
		}

		this.overlapStats = new SummaryReportUtils.TallyStats(overlapBase);
		this.pairtLenStats = new SummaryReportUtils.TallyStats(tLenOverall );
		this.softclipStats = new SummaryReportUtils.TallyStats( softClip);
		this.hardclipStats = new SummaryReportUtils.TallyStats( hardClip);
		this.readlengthStats = new SummaryReportUtils.TallyStats( readLength );	
		
		int maxLenght = (int)readlengthStats.getMax();
		QCMGAtomicLongArray trimedBase = new QCMGAtomicLongArray(maxLenght);	
		for (int i = 1 ; i < forTrimLength.length() ; i++)	{			
			if(forTrimLength.get(i) == 0 || maxLenght == i ) continue;
			//trimmedbase is maxLenght - i; readcounts is forTrimLength.get(i)
			trimedBase.increment( maxLenght - i, forTrimLength.get(i));
		}
		this.trimBaseStats = new SummaryReportUtils.TallyStats( trimedBase );		
		
	}
	@SuppressWarnings("unchecked")
	public void readSummary2Xml(Element parent ) throws Exception { 	
		
		preSummary();
	 		 
		//add to xml RG_Counts		
		Element rgElement = XmlUtils.createMetricsNode(parent,"basesLost", null);					
		//add discarded read Stats to readgroup summary	
		badReadStats( rgElement, node_duplicate, duplicate.get());
		badReadStats( rgElement, node_unmapped, unmapped.get() );		
		badReadStats( rgElement, node_nonCanonicalPair, notProperPairedReads.get());
		lostBaseStats( rgElement, node_trim, trimBaseStats );		
		lostBaseStats( rgElement, node_softClip, softclipStats );
		lostBaseStats( rgElement, node_hardClip, hardclipStats  );
		lostBaseStats( rgElement, node_overlap, overlapStats );
		
		
		//create node for overall	
		rgElement = XmlUtils.createMetricsNode(parent,"reads", new Pair(sreadCount, inputReadCounts.get()));		
		Element ele = XmlUtils.createGroupNode(rgElement, XmlUtils.discardReads );
		XmlUtils.outputValueNode(ele, "supplementaryAlignmentCount", supplementary.get());
		XmlUtils.outputValueNode(ele, "secondaryAlignmentCount", secondary.get());
		XmlUtils.outputValueNode(ele, "failedVendorQualityCount",failedVendorQuality.get()  );
		
		
		//readLength and tLen
		for(String name : new String[] {node_readLength, node_pairTlen}) {
			ele = XmlUtils.createGroupNode(rgElement, name );
			SummaryReportUtils.TallyStats stats = name.equals(node_readLength)? readlengthStats : pairtLenStats;
			String countName = name.equals(node_readLength)? sreadCount : spairCount;	
			String comment =  name.equals(node_readLength)? ": includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary)." 
					: ": only count properPaired reads which have a positive TLEN value or zero value but it is marked as firstOfPair";
			ele.appendChild( ele.getOwnerDocument().createComment( countName + comment ));	
			XmlUtils.outputValueNode(ele, countName, stats.getReadCounts());	
			XmlUtils.outputValueNode(ele, smax, stats.getMax());
			XmlUtils.outputValueNode(ele, smean, stats.getMean());
			XmlUtils.outputValueNode(ele, smode, stats.getMode());
			XmlUtils.outputValueNode(ele, smedian, stats.getMedium());	
			
		}		
		
		////add overall information to current readgroup element	
		long maxBases = getReadCount() * readlengthStats.getMax() ;
		long lostBase = (duplicate.get() + unmapped.get() + notProperPairedReads.get()  ) * getMaxReadLength() +
				trimBaseStats.getBaseCounts() + softclipStats.getBaseCounts() + hardclipStats.getBaseCounts()+ overlapStats.getBaseCounts();
		double lostPercent =  maxBases == 0? 0: 100 * (double) lostBase / maxBases ;	
		
		ele = XmlUtils.createGroupNode(rgElement, "countedReads" );
		XmlUtils.outputValueNode(ele, sunpaired,  unpaired.get());	
		ele.appendChild( ele.getOwnerDocument().createComment(sreadCount + ": includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).") );						
		XmlUtils.outputValueNode( ele, sreadCount,  getReadCount() );
		ele.appendChild( ele.getOwnerDocument().createComment(sbaseCount + ": " + sreadCount + " * readMaxLength") );
		XmlUtils.outputValueNode( ele, sbaseCount, maxBases);	
		ele.appendChild( ele.getOwnerDocument().createComment(slostBase + ": readMaxLength * (duplicateReads + nonCanonicalPairs + unmappedReads) + trimmedBases + softClippedBases + hardClippedBases + overlappedBases") );					
		XmlUtils.outputValueNode( ele, slostBase,  lostBase);	
		ele.appendChild( ele.getOwnerDocument().createComment(String.format("%s: %s / %s", sbasePercent, slostBase, sbaseCount)) );			
		XmlUtils.outputValueNode( ele, sbasePercent , lostPercent );			
	}
	 	 
	public void pairSummary2Xml( Element parent ) { 
		for(boolean isProper : new boolean[] {true, false}) {
			//add to xml RG_Counts
			String name = isProper? "properPairs" : "notProperPairs";
			Element ele =  XmlUtils.createMetricsNode( parent, name, null );
			long sum = 0;
			for(PairSummary p : pairCategory.values()) {
				if( p.isProperPair == isProper) {
					p.toSummaryXml(ele);	
					sum += p.getFirstOfPairCounts();
				}			
			}
			//can't really count he pair number due to RAM limits, just pickup number of firstOfPair
			ele.setAttribute( spairCount, sum+"");  			
		}
	}

	public void pairTlen2Xml( Element parent ) {
		Map<String, Element> metricEs = new HashMap<>();
		
		for(PairSummary p : pairCategory.values()) {
			String name = p.isProperPair? "tLenInProperPair" : "tLenInNotProperPair";
			Element ele = metricEs.computeIfAbsent(name,  k-> XmlUtils.createMetricsNode( parent, k, null )); 
			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getTLENCounts().toMap(), false );  
			
			name = p.isProperPair? "overlapBaseInProperPair" : "overlapBaseInNotProperPair";
			ele = metricEs.computeIfAbsent(name,  k-> XmlUtils.createMetricsNode( parent, k, null )); 			
			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getoverlapCounts().toMap(), false ); 							
		}
	}
	
	//for duplicate, unmapped and not proper paired reads
	private void badReadStats(Element parent, String nodeName, long reads ){
		Element ele = XmlUtils.createGroupNode(parent, nodeName);				
		XmlUtils.outputValueNode(ele, sreadCount, reads);	
		XmlUtils.outputValueNode(ele, slostBase, reads * getMaxReadLength());		
		double percentage = 100 * (double) reads / getReadCount() ;
		XmlUtils.outputValueNode(ele, sbasePercent,  percentage);		 	 
	}	
	
	private void lostBaseStats(Element parent, String nodeName, SummaryReportUtils.TallyStats stats ){
		long maxBases = getReadCount() * getMaxReadLength();		 
		Element ele = XmlUtils.createGroupNode(parent, nodeName);	
		XmlUtils.outputValueNode(ele, smin, stats.getMin());
		XmlUtils.outputValueNode(ele, smax, stats.getMax());
		XmlUtils.outputValueNode(ele, smean, stats.getMean());
		XmlUtils.outputValueNode(ele, smode, stats.getMode());
		XmlUtils.outputValueNode(ele, smedian, stats.getMedium());
		XmlUtils.outputValueNode(ele, sreadCount, stats.getReadCounts());
		XmlUtils.outputValueNode(ele, slostBase, stats.getBaseCounts());
		
		//deal with boundary value, missing reads
		double percentage = (maxBases == 0)? 0: 100 * (double) stats.getBaseCounts() /  maxBases ;				
		XmlUtils.outputValueNode(ele, sbasePercent,  percentage );		
	}

}