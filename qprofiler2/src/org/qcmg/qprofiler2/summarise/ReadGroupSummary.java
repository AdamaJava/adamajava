package org.qcmg.qprofiler2.summarise;


import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.BwaPair;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class ReadGroupSummary {

	public final static int ERR_READ_LIMIT  = 10;	
	//xml node name 	
	public final static String NODE_READGROUP = "readGroup";		
	public final static String NODE_SOFTCLIP = "softClippedBases";
	public final static String NODE_TRIM = "trimmedBases";	
	public final static String NODE_HARDCLIP = "hardClippedBases";
	public final static String NODE_READ_LENGTH = "readLength" ; 
	public final static String NODE_PAIR_TLEN = "tLen" ; 	
	public final static String NODE_OVERLAP = "overlappedBases";	
	public final static String NODE_DUPLICATE = "duplicateReads";
	public final static String NODE_SECONDARY = "secondary";
	public final static String NODE_SUPPLEMENTARY = "supplementary"; 	
	public final static String NODE_UNMAPPED = "unmappedReads";
	public final static String NODE_NOT_PROPER_PAIR = "notProperPairs";
	public final static String NODE_FAILED_VENDOR_QUALITY = "failedVendorQuality";

		
	public final static String MIN= "min";	
	public final static String MAX = "max";
	public final static String MEAN = "mean"; 
	public final static String MODE =  "mode"; 
	public final static String MEDIAN = "median" ; 
	public final static String READ_COUNT = "readCount";
	public final static String PAIR_COUNT = "pairCount";
	public final static String BASE_COUNT = "basesCount"; 
	public final static String BASE_LOST_COUNT = "basesLostCount"; 
	public final static String BASE_LOST_PERCENT = "basesLostPercent"; 	
	public final static String UNPAIRED_READ ="unpairedReads";
	
	 			
	//softclips, hardclips, read length; 	
	QCMGAtomicLongArray softClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray hardClip = new QCMGAtomicLongArray(128);
	
	//record read length excluding the discard reads but includes duplicate,unmapped and nonCanonicalReads	
	QCMGAtomicLongArray readLength = new QCMGAtomicLongArray(128);	
	QCMGAtomicLongArray forTrimLength = new QCMGAtomicLongArray(128);	
	private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<String, AtomicLong>();
	//must be concurrent set for multi threads
	private final ConcurrentMap<Integer, PairSummary> pairCategory = new ConcurrentHashMap<>();

	//bad reads inforamtion
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
	public ReadGroupSummary(String rgId){ this.readGroupId = rgId; }
	
	public String getReadGroupId(){ return readGroupId;  }	
		
	public int getMaxReadLength() { return (int) readlengthStats.getMax(); }
	
	public long getCigarReadCount() { return cigarRead.get(); }
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
		
		readLength.increment(record.getReadLength()+lHard);	
			
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
			BwaPair.Pair pairType = BwaPair.getPairType(record);
			boolean isProper = record.getProperPairFlag();
			int key = isProper? pairType.id   :  pairType.id * -1;	
			pairCategory.computeIfAbsent(key, e-> new PairSummary( pairType, isProper)).parse(record);
			//we always parse pairs but stop here if not ProperPair
			if( !isProper ) { 
				notProperPairedReads.incrementAndGet();
				return false;
			}
		} else {
			unpaired.incrementAndGet();
		}
		if(lHard > 0) hardClip.increment(lHard);
		if(lSoft > 0) softClip.increment(lSoft);
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
		QCMGAtomicLongArray trimedBase = new QCMGAtomicLongArray(maxLenght+1);	
		for (int i = 0 ; i < forTrimLength.length() ; i++)	{			
			if(forTrimLength.get(i) == 0 || maxLenght == i ) continue;
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
		badReadStats( rgElement, NODE_DUPLICATE, duplicate.get());
		badReadStats( rgElement, NODE_UNMAPPED, unmapped.get() );		
		badReadStats( rgElement, NODE_NOT_PROPER_PAIR, notProperPairedReads.get());
		lostBaseStats( rgElement, NODE_TRIM, trimBaseStats );		
		lostBaseStats( rgElement, NODE_SOFTCLIP, softclipStats );
		lostBaseStats( rgElement, NODE_HARDCLIP, hardclipStats  );
		lostBaseStats( rgElement, NODE_OVERLAP, overlapStats );
				
		//create node for overall	
		rgElement = XmlUtils.createMetricsNode(parent,"reads", new Pair<String, Number>(READ_COUNT, inputReadCounts.get()));		
		Element ele = XmlUtils.createGroupNode(rgElement, XmlUtils.DISCARD_READS );
		XmlUtils.outputValueNode(ele, "supplementaryAlignmentCount", supplementary.get());
		XmlUtils.outputValueNode(ele, "secondaryAlignmentCount", secondary.get());
		XmlUtils.outputValueNode(ele, "failedVendorQualityCount",failedVendorQuality.get()  );
				
		//readLength and tLen
		for(String name : new String[] {NODE_READ_LENGTH, NODE_PAIR_TLEN}) {
			ele = XmlUtils.createGroupNode(rgElement, name );
			SummaryReportUtils.TallyStats stats = name.equals(NODE_READ_LENGTH)? readlengthStats : pairtLenStats;
			String countName = name.equals(NODE_READ_LENGTH)? READ_COUNT : PAIR_COUNT;			
			//readCount: includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).
			//pairCount: only count properPaired reads which have a positive TLEN value or zero value but it is marked as firstOfPair						
			XmlUtils.outputValueNode(ele, countName, stats.getReadCounts());	
			XmlUtils.outputValueNode(ele, MAX, stats.getMax());
			XmlUtils.outputValueNode(ele, MEAN, stats.getMean());
			XmlUtils.outputValueNode(ele, MODE, stats.getMode());
			XmlUtils.outputValueNode(ele, MEDIAN, stats.getMedium());				
		}		
		
		//add overall information to current readgroup element	
		long maxBases = getReadCount() * readlengthStats.getMax() ;
		long lostBase = (duplicate.get() + unmapped.get() + notProperPairedReads.get()  ) * getMaxReadLength() +
				trimBaseStats.getBaseCounts() + softclipStats.getBaseCounts() + hardclipStats.getBaseCounts()+ overlapStats.getBaseCounts();
		double lostPercent =  maxBases == 0? 0: 100 * (double) lostBase / maxBases ;	
		
		ele = XmlUtils.createGroupNode(rgElement, "countedReads" );
		XmlUtils.outputValueNode(ele, UNPAIRED_READ,  unpaired.get());	
		//READ_COUNT : includes duplicateReads, nonCanonicalPairs and unmappedReads but excludes discardedReads (failed, secondary and supplementary).						
		XmlUtils.outputValueNode( ele, READ_COUNT,  getReadCount() );
		//BASE_COUNT :  READ_COUNT  * readMaxLength
		XmlUtils.outputValueNode( ele, BASE_COUNT, maxBases);	
		//BASE_LOST_COUNT : readMaxLength * (duplicateReads + nonCanonicalPairs + unmappedReads) + trimmedBases + softClippedBases + hardClippedBases + overlappedBases				
		XmlUtils.outputValueNode( ele, BASE_LOST_COUNT,  lostBase);	
		//basesLostPercent: basesLostCount / basesCount		
		XmlUtils.outputValueNode( ele, BASE_LOST_PERCENT , lostPercent );	
		
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
			ele.setAttribute( PAIR_COUNT, sum+"");  			
		}
	}

	public void pairTlen2Xml( Element parent ) {
		Map<String, Element> metricEs = new HashMap<>();
		Map<String, AtomicLong> metricCounts = new HashMap<>();
		for(PairSummary p : pairCategory.values()) {
			String name = p.isProperPair? "tLenInProperPair" : "tLenInNotProperPair";
			//sum all pairCounts belong to metrics section
			AtomicLong cPairs =  metricCounts.computeIfAbsent(name,  k->  new AtomicLong());
			cPairs.addAndGet(p.getTLENCounts().getSum());
			//output pair tLen to classified section
			Element ele = metricEs.computeIfAbsent(name,  k-> XmlUtils.createMetricsNode( parent, k, null)); 
			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getTLENCounts().toMap(), false, true );  
					
			name = p.isProperPair? "overlapBaseInProperPair" : "overlapBaseInNotProperPair";
			//sum all pairCounts belong to metrics section
			cPairs =  metricCounts.computeIfAbsent(name,  k->  new AtomicLong());
			cPairs.addAndGet(p.getoverlapCounts().getSum());
			//output pair overlap to classified section
			ele = metricEs.computeIfAbsent(name,  k-> XmlUtils.createMetricsNode( parent, k, null));
			XmlUtils.outputTallyGroup( ele, p.type.name(), p.getoverlapCounts().toMap(), false , true); 
		}
		
		//add pairCounts into metrics elements
		for(String name : metricEs.keySet()) {
			metricEs.get(name).setAttribute(ReadGroupSummary.PAIR_COUNT, metricCounts.get(name).get()+"" );
		}
		
	}
	
	//for duplicate, unmapped and not proper paired reads
	private void badReadStats(Element parent, String nodeName, long reads ){
		Element ele = XmlUtils.createGroupNode(parent, nodeName);				
		XmlUtils.outputValueNode(ele, READ_COUNT, reads);	
		XmlUtils.outputValueNode(ele, BASE_LOST_COUNT, reads * getMaxReadLength());		
		double percentage = 100 * (double) reads / getReadCount() ;
		XmlUtils.outputValueNode(ele, BASE_LOST_PERCENT,  percentage);		 	 
	}	
	
	private void lostBaseStats(Element parent, String nodeName, SummaryReportUtils.TallyStats stats ){
		long maxBases = getReadCount() * getMaxReadLength();		 
		Element ele = XmlUtils.createGroupNode(parent, nodeName);	
		XmlUtils.outputValueNode(ele, MIN, stats.getMin());
		XmlUtils.outputValueNode(ele, MAX, stats.getMax());
		XmlUtils.outputValueNode(ele, MEAN, stats.getMean());
		XmlUtils.outputValueNode(ele, MODE, stats.getMode());
		XmlUtils.outputValueNode(ele, MEDIAN, stats.getMedium());
		XmlUtils.outputValueNode(ele, READ_COUNT, stats.getReadCounts());
		XmlUtils.outputValueNode(ele, BASE_LOST_COUNT, stats.getBaseCounts());
		
		//deal with boundary value, missing reads
		double percentage = (maxBases == 0)? 0: 100 * (double) stats.getBaseCounts() /  maxBases ;				
		XmlUtils.outputValueNode(ele, BASE_LOST_PERCENT,  percentage );		
	}

}