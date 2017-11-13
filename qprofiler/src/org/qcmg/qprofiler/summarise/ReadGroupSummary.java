package org.qcmg.qprofiler.summarise;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qprofiler.bam.BamSummaryReport;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.picard.util.PairedRecordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class ReadGroupSummary {
	//xml node name 	
	public final static String node_readgroup = "ReadGroup";
	public final static String node_f5f3 = "F5F3";
	public final static String node_f3f5 = "F3F5";
	public final static String node_inward = "Inward";
	public final static String node_outward = "Outward";		
	public final static String node_softClip = "softClip";
	public final static String node_hardClip = "hardClip";
	public final static String node_readLength = "readLength" ; 
	public final static String node_overlap = "overlap";	
	public final static String node_duplicate = "duplicate";
	public final static String node_secondary = "secondary";
	public final static String node_supplementary = "supplementary"; 	
	public final static String node_unmapped = "unmapped";
	public final static String node_nonCanonicalPair = "nonCanonicalPair";
	public final static String node_failedVendorQuality = "failedVendorQuality";
	
	//fixed value
	public final static int bigTlenValue = 10000;
	public final static int farTlenValue = 1500;
 			
	//softclips, hardclips, read length; 	
	QCMGAtomicLongArray softClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray hardClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray readLength = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray overlapBase = new QCMGAtomicLongArray(128);	
		 
	//QCMGAtomicLongArray.get(arrayTlenLimit) for tlen=[bigTlenValue, ~)
	QCMGAtomicLongArray isize = new QCMGAtomicLongArray(bigTlenValue + 1);		
	AtomicInteger max_isize = new AtomicInteger(); 
	
	//bad reads inforamtion
	AtomicLong duplicate = new AtomicLong();
	AtomicLong secondary  = new AtomicLong();
	AtomicLong supplementary  = new AtomicLong();
	AtomicLong unmapped  = new AtomicLong();
	AtomicLong nonCanonical  = new AtomicLong();
	AtomicLong failedVendorQuality  = new AtomicLong();
	
	//pairing information	
	AtomicLong pairNum  = new AtomicLong();
	Pair f3f5 = new Pair( node_f3f5 );
	Pair f5f3 = new Pair( node_f5f3 );
	Pair inward = new Pair( node_inward );
	Pair outward = new Pair( node_outward );	
	
	//pair in different reference
	AtomicLong diffRef  = new AtomicLong();	
	AtomicLong mateUnmapped  = new AtomicLong();	
	AtomicLong diffRef_flag_p  = new AtomicLong();	
	AtomicLong mateUnmapped_flag_p  = new AtomicLong();
	
	//for combined readgroups, since max read length maybe different
	private long trimedBase = 0; 
	private Long maxBases = null; 
	
	private final String readGroupId; 		
	public ReadGroupSummary(String rgId){	this.readGroupId = rgId; }
	public String getReadGroupId(){return readGroupId; }
		
 	private class Pair {
 		private final String name;		
 		Pair(String name){this.name = name;}
 		
		AtomicLong overlap = new AtomicLong();
		AtomicLong near = new AtomicLong();
		AtomicLong far = new AtomicLong();
		AtomicLong bigTlen  = new AtomicLong();	
				
		//test flag proper mapped pairing  		
		AtomicLong bigTlen_flag_p  = new AtomicLong();	
		AtomicLong overlap_flag_p  = new AtomicLong();
		AtomicLong near_flag_p  = new AtomicLong();
		AtomicLong far_flag_p  = new AtomicLong();
		
		AtomicLong recordSum  = new AtomicLong();
		AtomicLong recordSum_flag_p = new AtomicLong();
		
		void parse(SAMRecord record, Integer overlapBase ){		
			recordSum.getAndIncrement();
			
			if(record.getProperPairFlag())	recordSum_flag_p.getAndIncrement();
			
			if( overlapBase > 0 ){
				overlap.incrementAndGet();	
				if(record.getProperPairFlag()) overlap_flag_p.incrementAndGet();
			}
			if(record.getInferredInsertSize() >= bigTlenValue){
				bigTlen.incrementAndGet();
				if(record.getProperPairFlag()) bigTlen_flag_p.incrementAndGet();
			}							
			if( record.getInferredInsertSize() >= farTlenValue && record.getInferredInsertSize() < bigTlenValue ){
				far.incrementAndGet();		
				if( record.getProperPairFlag()) far_flag_p.incrementAndGet();
			}
			if( record.getInferredInsertSize() < farTlenValue && overlapBase <= 0 ){
				near.incrementAndGet();		
				if( record.getProperPairFlag()) near_flag_p.incrementAndGet();
			}			
		}
		
		void toXml(Element parent  ){
			Element stats = BamSummaryReport.createSubElement(parent, name);			
			stats.setAttribute("overlappedPairs", overlap.get()+"");		 
			stats.setAttribute("TlenLess1500", near.get()+"");		 
			stats.setAttribute("TlenOver1500", far.get()+"");
			stats.setAttribute("TlenOver10000", bigTlen.get()+"");
			stats.setAttribute("pairNumber", recordSum.get() + "");
			
			//debug
			stats = BamSummaryReport.createSubElement(parent, name+"_flag_p");
			stats.setAttribute("overlappedPairs_flag_p", overlap_flag_p.get()+"");		 
			stats.setAttribute("TlenLess1500_flag_p", near_flag_p.get()+"");		 
			stats.setAttribute("TlenOver1500_flag_p", far_flag_p.get()+"");
			stats.setAttribute("TlenOver10000_flag_p", bigTlen_flag_p.get()+"");
			stats.setAttribute("pairNumber_flag_p", recordSum_flag_p.get() + "");
		}
		
	}
		
	/**
	 * 
	 * @param record
	 * @return false if record is duplicate, ....
	 */
	public boolean parseRecord( final SAMRecord record ){
				
		if ( record.getSupplementaryAlignmentFlag())
			supplementary.incrementAndGet();
		else if( record.getNotPrimaryAlignmentFlag()  ) 
			secondary.incrementAndGet();
		else if(record.getReadFailsVendorQualityCheckFlag())
			failedVendorQuality.incrementAndGet();
		else if(record.getReadUnmappedFlag() )
			unmapped.incrementAndGet();
		else if(record.getDuplicateReadFlag())
			duplicate.incrementAndGet();
		else if(! PairedRecordUtils.isCanonical(  record) ){
			nonCanonical.incrementAndGet();			 
			parsePairing( record , PairedRecordUtils.getOverlapBase( record)); 			
		}else{
			//parse clips
			 int lHard = 0;
			 int lSoft = 0;
			 for (CigarElement ce : record.getCigar().getCigarElements()) {
				 if (ce.getOperator().equals(CigarOperator.HARD_CLIP)) {  
					 lHard += ce.getLength();
				 } else if (ce.getOperator().equals(CigarOperator.SOFT_CLIP)) {  
					 lSoft += ce.getLength();
				 }
			 }
			hardClip.increment(lHard);
			softClip.increment(lSoft);
			readLength.increment(record.getReadLength()+lHard);
	 		
			//parse overlap
			int overlap = PairedRecordUtils.getOverlapBase(record);
			if(overlap > 0) overlapBase.increment(overlap);
			parsePairing( record , overlap); 	
			
			return true; 			
		}
		
		//return false for bad reads
		return false; 
	}
	
	
	/**
	 * count the first of pair number and record iSize(Tlen);
 	 * if( mate unmapped) record it and then return; 
 	 * if(mate map to different ref && first of pair) record it and then return; 
 	 * counts pair direction and distance; Here we only select reads with Tlen>0 from pair to avoiding double counts
	 * @param record: a mapped and paired read and not duplicate, not supplementary, secondary or failed
	 * @param overlapBase
	 */
	public void parsePairing( SAMRecord record, int overlapBase ){
		//skip non-paired reads
		if( !record.getReadPairedFlag() )  return;  						
				
		//record iSize, first pair only to avoid double iSize		
		if(record.getFirstOfPairFlag()){
			int tLen = Math.abs(record.getInferredInsertSize());
			if( tLen > max_isize.get() ){ 
				max_isize.getAndSet(record.getInferredInsertSize() );
			}
			if(tLen > bigTlenValue ) {
				tLen = bigTlenValue;			
			}
	 			isize.increment(tLen); 	 
		}
		
		
		//if first pair missing, it won't be count here but it still go to direction if tlen>0	
	 	//so sometime the pairNum will be different to sum of f5f3, f3f5, inward and outwards
		//to avoid double counts, we only count first of pair or the pair is first time appear(mate is unmapped), since all input record are mapped. 
		if(record.getFirstOfPairFlag() || record.getMateUnmappedFlag()) 
	 		pairNum.incrementAndGet();	
				
		//pair from different reference, only look at first pair to avoid double counts
		if( record.getMateUnmappedFlag() ){
			mateUnmapped.incrementAndGet();
			if (record.getProperPairFlag()) {
				mateUnmapped_flag_p.incrementAndGet();	
			}
			return; 
		}
		
		if( !record.getReferenceName().equals( record.getMateReferenceName()) && record.getFirstOfPairFlag()){
			diffRef.incrementAndGet();
			if (record.getProperPairFlag()) {
				diffRef_flag_p.incrementAndGet();					
			}
			return; 
		}
		
		//only count reads with tlen > 0 to avoid double counts
		if( record.getInferredInsertSize() <=  0) return; 
		
		//detailed pair inforamtion
//		if(overlapBase == null)	overlapBase = PairedRecordUtils.getOverlapBase( record);				 
		if( PairedRecordUtils.isF5toF3(record)) f5f3.parse( record, overlapBase ); 		
		if( PairedRecordUtils.isF3toF5(record)) f3f5.parse( record, overlapBase );	 
		if( PairedRecordUtils.isOutward(record)) outward.parse( record, overlapBase );		
		if( PairedRecordUtils.isInward(record)) inward.parse( record, overlapBase );
 	}
		
	public long getNumDuplicate(){ return duplicate.get(); }
	public long getNumSecondary(){ return secondary.get(); }
	public long getNumSupplementary(){ return supplementary.get(); }
	public long getNumUnMapped(){ return unmapped.get(); }
	public long getNumNonCanonical(){ return nonCanonical.get(); }
	public long getNumFailedVendorQuality(){ return failedVendorQuality.get(); }

	public int getMaxReadLength(){
		int  maxReadLength  = 0 ;		 
		for (int i = 1 ; i < readLength.length() ; i++) {
			if(readLength.get(i) > maxReadLength) {
				maxReadLength = i;
			}
		}
		return maxReadLength; 
	}
	
	/**
	 * total base of mapped reads excludes duplicate and noncanonial reads; including all clipped bases 
	 * @return average of read length which is total base divided by read number
	 */
	public int getAveReadLength(){
		long totalgoodRead = 0 , totalgoodBase = 0;
		for (int i = 1 ; i < readLength.length() ; i++){ 			 
			totalgoodBase += i * readLength.get(i);
			totalgoodRead += readLength.get(i);
		}		
		return ( totalgoodRead == 0 )? 0 :(int) (totalgoodBase / totalgoodRead);		
	}
	
	public long getTrimedBases(){
		if( readGroupId.equals(SummaryReportUtils.All_READGROUP ))			 
			return trimedBase ;
		
		//suppose original reads with same length for same read group
		long totalgoodRead = 0 , totalgoodBase = 0;
		for (int i = 1 ; i < readLength.length() ; i++){ 			 
			totalgoodBase += i * readLength.get(i);
			totalgoodRead += readLength.get(i);
		}		
		return totalgoodRead * getMaxReadLength() - totalgoodBase;
	}
	
	public void setTrimedBases(long bases){ 
		if( readGroupId.equals( SummaryReportUtils.All_READGROUP ) )			 
			trimedBase = bases;
	}
	
	public void setMaxBases( Long bases){
		if(this.maxBases != null ) 
			throw new IllegalStateException("Illegal attempt to set a Once value (this.maxBases) after it's value has already been set.");
		
		if( bases == null )
			throw new IllegalArgumentException("Illegal attempt to pass null value to Once setter (this.maxBases).");
		
		//if( readGroupId.equals(SummaryReportUtils.All_READGROUP ))			 	
		this.maxBases = bases;
		
//		System.out.println("debug, maxBase: " + maxBases);
	}	
	
	public long getMaxBases(){
		
		if(null == this.maxBases || this.maxBases <= 0) 
			throw new IllegalStateException("Illegal attempt to access unitialized or minus value (this.maxBases).");
		
        return this.maxBases;
	}
	
	public long getCountedReads() {
		long totalgoodRead = 0  ;
		for (int i = 1 ; i < readLength.length() ; i++)			 
			totalgoodRead += readLength.get(i);
		
		return totalgoodRead + duplicate.get() + unmapped.get() + nonCanonical.get() ;		
	}
	
	//public void toXml(Element rgClipElement, Element summaryElement) {
	public void readSummary2Xml(Element parent ) { 	 		
						
			//add to xml RG_Counts
			Element rgElement = BamSummaryReport.createSubElement( parent, node_readgroup );
			rgElement.setAttribute("id", readGroupId);			
						
			//add discarded read Stats to readgroup summary			
			Element stats = BamSummaryReport.createSubElement( rgElement, "nonCountedReads" );	
			stats.setAttribute( "supplementaryReads", supplementary.get()+"" );
			stats.setAttribute( "secondaryReads", secondary.get()+"" );
			stats.setAttribute( "failedVendorQualityReads", secondary.get()+"" );			
			
			long noOfRecords = getCountedReads( );
			double lostPercentage = 0; 
			//add discarded reads
			lostPercentage += addDiscardReadStats( rgElement, node_duplicate, duplicate, noOfRecords );
			lostPercentage += addDiscardReadStats( rgElement, node_unmapped, unmapped, noOfRecords );		
			lostPercentage += addDiscardReadStats( rgElement, node_nonCanonicalPair, nonCanonical, noOfRecords );			
						
			//add counted read stats to readgroup summary	
			int  maxReadLength = getMaxReadLength();
			if(! readGroupId.equals(SummaryReportUtils.All_READGROUP )){
				setMaxBases( noOfRecords * maxReadLength );
				lostPercentage += addCountedReadStats(rgElement, "trimmedBase", parseTrim(readLength, maxReadLength), maxBases);				
			}else{
				double percentage = 100 * (double) getTrimedBases() / getMaxBases() ;
				BamSummaryReport.createSubElement(rgElement, "trimmedBase").setAttribute("basePercentage", String.format("%2.2f%%", percentage ));
				lostPercentage += percentage;	
			}
						
			lostPercentage += addCountedReadStats( rgElement, node_softClip, softClip, maxBases );
			lostPercentage += addCountedReadStats( rgElement, node_hardClip, hardClip, maxBases );
			lostPercentage += addCountedReadStats( rgElement, node_overlap, overlapBase, maxBases );	
			
			//addCountedReadStats( rgElement, node_readLength, readLength, maxBases);	
			//add overall
			stats = BamSummaryReport.createSubElement(rgElement, "overall");
			stats.setAttribute("maxLength", maxReadLength + "" );
			stats.setAttribute("aveLength", getAveReadLength() + "" );
			stats.setAttribute("countedReads", noOfRecords + "" );
			
			//by default String.format will round the tail of digits
			stats.setAttribute("lostBases", String.format("%2.2f%%", lostPercentage) );
//			stats.setAttribute("lostBases", ( readGroupId.equals(SummaryReportUtils.All_READGROUP ) )? "-" : String.format("%2.2f%%", lostPercentage) );					
	}	
	
	public void pairSummary2Xml(Element parent) { 	 
		//add paring information
		Element pairElement = BamSummaryReport.createSubElement(parent, node_readgroup);
		pairElement.setAttribute("id", readGroupId);	
		pairElement.setAttribute("TotalPairs", pairNum.get()+"");	
			 	
		Element stats = BamSummaryReport.createSubElement(pairElement, "MateUnmapped");
		stats.setAttribute("pairNumber", mateUnmapped.get() + "");
		stats.setAttribute("pairNumber_flag_p", mateUnmapped_flag_p.get() + "");
		
		stats = BamSummaryReport.createSubElement(pairElement, "MateDiffRef");
		stats.setAttribute("pairNumber", diffRef.get() + "");
		stats.setAttribute("pairNumber_flag_p", diffRef_flag_p.get() + "");

		f5f3.toXml( pairElement );
		f3f5.toXml( pairElement );
		inward.toXml( pairElement );
		outward.toXml( pairElement );
		
	}	
	
	public void iSize2Xml(Element parent) { 
		Element rgElement = SummaryReport.createSubElement(parent, node_readgroup);
		rgElement.setAttribute("id", readGroupId );
		final int boundary = Math.min(farTlenValue *2, bigTlenValue/2);
		
		
		// standard deviation per RG as an attribute
		long modal = 0; 
		StandardDeviation stdDev = new StandardDeviation();
		for(int i = 1; i < boundary; i ++ ){
			if(isize.get(i) > modal) modal = isize.get(i);
			for(int j = 0; j < isize.get(i); j ++)
				stdDev.increment(i);			
		}
		rgElement.setAttribute("stdDev", stdDev.getResult() + "");
		
		String modalIsize = "";
		for(int i = 1; i < boundary; i ++ )
			if(isize.get(i) == modal )
				modalIsize += (StringUtils.isNullOrEmpty(modalIsize) ) ? i : "and" + i; 
		rgElement.setAttribute( "ModalISize", modalIsize );
		
		
		//counts for each Tlen value
		int start = 0;
		for(int i = 0; i < boundary ; i ++ ) 
			if(isize.get(i) != 0) 			
				setISizeElement(rgElement,isize.get(i),   i, i);				
		
		
		start = boundary;
		long count = isize.get(boundary); 
		for(int i = boundary ; i < bigTlenValue;  i ++ ){
			count += isize.get(i);
			if( i % 1000 == 0 ){
				if(count != 0)
					setISizeElement(rgElement, count,   start, i);
				count = 0; 
				start = i+1; 				
			}			
		}
		
		//region for last bin before bigTlenValue
		if(count > 0)
			setISizeElement(rgElement, count,   start, (bigTlenValue - 1));
				
		//region for oversize Tlen 
		setISizeElement(rgElement, isize.get(bigTlenValue),   bigTlenValue, max_isize.get() );
	}
	
	/**
	 * Create element node for each isize region with same counts
	 * @param parent
	 * @param count
	 * @param start
	 * @param end
	 */
	private void setISizeElement(Element parent, long count, int start, int end ){
		Document doc = parent.getOwnerDocument(); 
		Element cycleE = doc.createElement("RangeTallyItem");
		cycleE.setAttribute("count", count +"");
		cycleE.setAttribute("start", start + "");
		cycleE.setAttribute("end", end + "");	
		parent.appendChild(cycleE);			
	}

	private double addDiscardReadStats(Element parent, String nodeName, AtomicLong counts, long totalReads ){
			//add to RG_count section
			Element stats = BamSummaryReport.createSubElement(parent, nodeName);	
			stats.setAttribute("readsNumber", String.format("%,d",counts.get()));
						 	
			//duplicats, non canonical and unmapped will be counted, others reads just discards
			float percentage = 100 * (float) counts.get() / totalReads ;						
			stats.setAttribute("percentage",  String.format("%2.2f%%", percentage   ));		
			
			return percentage; 
			 	 
		}		
			
	private double addCountedReadStats(Element rgElement, String nodeName, QCMGAtomicLongArray array, long maxBases ){
		ConcurrentHashMap<String, AtomicLong> map = new ConcurrentHashMap<String, AtomicLong>();
		
		//get the position of median
			long bases = 0,counts = 0;
			long arrayLength = null != array ? array.length() : 0;
			for (int i = 1 ; i < arrayLength ; i++){
				counts += array.get(i);
				bases += i * array.get(i);
			}
			
			int mean = (counts == 0) ? 0: (int) (bases / counts);
			long medium = arrayLength > 0 ? array.get( (int) (arrayLength / 2)) : 0;
			
			medium = 0;
			for (int i = 0 ; i < arrayLength ; i++)
				if((medium += array.get(i)) >= counts/2  ){
					medium = i; 
					break; 
				}
			int min = 0, max = 0, mode = 0; 
			long highest = 0;
			for (int i = 1 ; i < arrayLength ; i++) 
				if(array.get(i) > 0){
					//last non-zero position
					max = i;					
					//first non-zero position
					min = ( min == 0 ) ? i : min; 					
					//mode is the number of read which length is most popular
					if(array.get(i) > highest){
						highest = array.get(i);
						mode = i; 
					}  
				}
								
	 		map.put( "min", new AtomicLong(min));
			map.put( "max", new AtomicLong(max) );
			map.put("mean",new AtomicLong(mean));
			map.put( "mode", new AtomicLong(mode)) ;
			map.put( "median", new AtomicLong(medium) );	
			map.put("readsNumber",new AtomicLong(counts));				

			Element stats = BamSummaryReport.createSubElement(rgElement, nodeName);		
			for(String key : map.keySet()) 
				stats.setAttribute(key,  map.get(key)+"");
				

			//deal with boundary value, missing reads
			double percentage = (maxBases == 0)? 0: 100 * (double) bases /  maxBases ;		
			stats.setAttribute("basePercentage",  String.format("%2.2f%%", percentage    ));		
			
			return percentage; 
	}
	
	private QCMGAtomicLongArray parseTrim( QCMGAtomicLongArray readLengthArray, int maxReadLength ) {
		QCMGAtomicLongArray array = new QCMGAtomicLongArray(  maxReadLength+1 ); 
		for(int i = 1; i < maxReadLength; i ++) {
			array.increment( maxReadLength - i, readLengthArray.get(i) );
		}
		return array;
	}

}
