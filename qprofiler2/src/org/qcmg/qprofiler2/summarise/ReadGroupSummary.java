package org.qcmg.qprofiler2.summarise;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.Qprofiler1XmlUtils;
import org.qcmg.picard.util.PairedRecordUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
//import javafx.util.Pair;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class ReadGroupSummary {
	//xml node name 	
	public final static String node_readgroup = "readGroup";
	public final static String node_f5f3 = "f5f3Pair";
	public final static String node_f3f5 = "f3f5Pair";
	public final static String node_inward = "inwardPair";
	public final static String node_outward = "outwardPair";		
	public final static String node_softClip = "softClippedBases";
	public final static String node_hardClip = "hardClippedBases";
	public final static String node_readLength = "readLength" ; 
	public final static String node_overlap = "overlapBases";	
	public final static String node_duplicate = "duplicateReads";
	public final static String node_secondary = "secondary";
	public final static String node_supplementary = "supplementary"; 	
	public final static String node_unmapped = "unmappedReads";
	public final static String node_nonCanonicalPair = "nonCanonicalPair";
	public final static String node_failedVendorQuality = "failedVendorQuality";
	public final static String modal_isize = "modalSize";
	
	//fixed value
	public final static int bigTlenValue = 10000;
	public final static int farTlenValue = 1500;
	public final static int middleTlenValue = 5000;
	public final static int rangeGap = 100;
 			
	//softclips, hardclips, read length; 	
	QCMGAtomicLongArray softClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray hardClip = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray readLength = new QCMGAtomicLongArray(128);
	QCMGAtomicLongArray overlapBase = new QCMGAtomicLongArray(128);	
		 
	//QCMGAtomicLongArray.get(arrayTlenLimit) for tlen=[bigTlenValue, ~)
	QCMGAtomicLongArray isize1 = new QCMGAtomicLongArray(bigTlenValue + 1);	//previous method
	QCMGAtomicLongArray isize = new QCMGAtomicLongArray(middleTlenValue);	 //store count bwt [0, 1499]
	QCMGAtomicLongArray isizeRange = new QCMGAtomicLongArray( (bigTlenValue/rangeGap) + 1);	//store count bwt [0/100, 10000/100]
	AtomicInteger max_isize = new AtomicInteger(); 
	
	//bad reads inforamtion
	AtomicLong duplicate = new AtomicLong();
	AtomicLong secondary  = new AtomicLong();
	AtomicLong supplementary  = new AtomicLong();
	AtomicLong unmapped  = new AtomicLong();
	AtomicLong nonCanonical  = new AtomicLong();
	AtomicLong failedVendorQuality  = new AtomicLong();
	AtomicLong inputReadCounts  = new AtomicLong();
	
	//pairing information	
	AtomicLong pairNum  = new AtomicLong();
	PairedRead f3f5 = new PairedRead( node_f3f5 );
	PairedRead f5f3 = new PairedRead( node_f5f3 );
	PairedRead inward = new PairedRead( node_inward );
	PairedRead outward = new PairedRead( node_outward );	
	
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
		
 	private class PairedRead {
 		private final String name;		
 		PairedRead(String name){this.name = name;}
 		
		AtomicLong overlap = new AtomicLong();
		AtomicLong near = new AtomicLong();
		AtomicLong far = new AtomicLong();
		AtomicLong bigTlen  = new AtomicLong();	
		
		AtomicLong recordSum  = new AtomicLong();
		AtomicLong recordSum_flag_p = new AtomicLong();
		
		void parse(SAMRecord record, Integer overlapBase ){		
			recordSum.getAndIncrement();
			
			if(record.getProperPairFlag())	recordSum_flag_p.getAndIncrement();
			
			if( overlapBase > 0 ){
				overlap.incrementAndGet();	
//				if(record.getProperPairFlag()) overlap_flag_p.incrementAndGet();
			}
			if(record.getInferredInsertSize() >= bigTlenValue){
				bigTlen.incrementAndGet();
//				if(record.getProperPairFlag()) bigTlen_flag_p.incrementAndGet();
			}							
			if( record.getInferredInsertSize() >= farTlenValue && record.getInferredInsertSize() < bigTlenValue ){
				far.incrementAndGet();		
//				if( record.getProperPairFlag()) far_flag_p.incrementAndGet();
			}
			if( record.getInferredInsertSize() < farTlenValue && overlapBase <= 0 ){
				near.incrementAndGet();		
//				if( record.getProperPairFlag()) near_flag_p.incrementAndGet();
			}			
		}
		
		
		
		/**
		 * 	  <f5f3Pair tlenUnder1500="3528" TlenOver10000="102075" tlenBetween1500And10000="22027" overlapping="241" count="127871"/>
	  <f3f5Pair tlenUnder1500="12784" TlenOver10000="105049" tlenBetween1500And10000="21797" overlapping="738" count="140368"/>
	  <inwardPair tlenUnder1500="134706192" TlenOver10000="198419" tlenBetween1500And10000="148179" overlapping="6988807" count="142041597"/>
	  <outwardPair tlenUnder1500="100769" TlenOver10000="205890" tlenBetween1500And10000="111947" overlapping="322649" count="741255"/>

		 * @param parent
		 */
		
		void toXml(Element parent  ){
			Element stats = Qprofiler1XmlUtils.createSubElement(parent, name);			
			stats.setAttribute("overlapping", overlap.get()+"");		 
			stats.setAttribute("tlenUnder1500", near.get()+"");		 
			stats.setAttribute("tlenOver10000", bigTlen.get() +"");
			stats.setAttribute("tlenBetween1500And10000",far.get()+"");
			stats.setAttribute("count", recordSum.get() + "");
		}
		
	}
		
	/**
	 * classify record belongs (duplicate...) and count the number. If record is parsed, then count the hard/soft clip bases, pair mapping overlap bases and pairs information
	 * @param record
	 * @return true if record parsed; otherwise return false since record duplicate, supplementary, secondary, failedVendorQuality, unmapped or nonCanonical. 
	 */
	public boolean parseRecord( final SAMRecord record ){
				
		//record counts disregard good or not
		inputReadCounts.incrementAndGet();  
				
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
			parsePairing( record , null); 			
		}else{
			//parse clips
			 int lHard = 0, lSoft = 0;
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
	
	public QCMGAtomicLongArray getISizeCount(){return isize;}
	public QCMGAtomicLongArray getISizeRangeCount(){return isizeRange;}
	
	/**
	 * count the first of pair number and record iSize(Tlen);
 	 * if( mate unmapped) record it and then return; 
 	 * if(mate map to different ref && first of pair) record it and then return; 
 	 * counts pair direction and distance; Here we only select reads with Tlen>0 from pair to avoiding double counts
	 * @param record: a mapped and paired read and not duplicate, not supplementary, secondary or failed
	 * @param overlapBase
	 */
	public void parsePairing( SAMRecord record, Integer overlapBase ){
		//skip non-paired reads
		if( !record.getReadPairedFlag() )  return;  						
				
		//record iSize, first pair only to avoid double iSize		
		if(record.getFirstOfPairFlag()){
			int tLen = Math.abs(record.getInferredInsertSize());
				
			if( tLen > max_isize.get() ) 
				max_isize.getAndSet( tLen );
				//max_isize.getAndSet(record.getInferredInsertSize() );	
			
			//only record popular TLEN
			if(tLen < middleTlenValue) isize.increment(tLen);

			//record region
			if(tLen > bigTlenValue )  tLen = bigTlenValue;		
			
			isizeRange.increment( (tLen/rangeGap));
		}
		
		
		//if first pair missing, it won't be count here but it still go to direction if tlen>0	
	 	//so sometime the pairNum will be different to sum of f5f3, f3f5, inward and outwards
		//to avoid double counts, we only count first of pair or the pair is first time appear(mate is unmapped), since all input record are mapped. 
		if(record.getFirstOfPairFlag() || record.getMateUnmappedFlag()) 
	 		pairNum.incrementAndGet();	
				
		//pair from different reference, only look at first pair to avoid double counts
		if( record.getMateUnmappedFlag() ){
			mateUnmapped.incrementAndGet();
			if(record.getProperPairFlag()) mateUnmapped_flag_p.incrementAndGet();	
			return; 
		}
		
		if( !record.getReferenceName().equals( record.getMateReferenceName()) && record.getFirstOfPairFlag()){
			diffRef.incrementAndGet();
			if(record.getProperPairFlag()) diffRef_flag_p.incrementAndGet();					
			return; 
		}
		
		//only count reads with tlen > 0 to avoid double counts
		if( record.getInferredInsertSize() <=  0) return; 
		
		//detailed pair inforamtion
		if(overlapBase == null)	overlapBase = PairedRecordUtils.getOverlapBase( record);				 
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
		for (int i = 1 ; i < readLength.length() ; i++) 
			if(readLength.get(i) > 0) maxReadLength = i;			
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
		if( readGroupId.equals(XmlUtils.All_READGROUP ))			 
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
		if( readGroupId.equals( XmlUtils.All_READGROUP ) )			 
			trimedBase = bases;
	}
	
	public void setMaxBases( Long bases){
		if(this.maxBases != null ) 
			throw new IllegalStateException("Illegal attempt to set a Once value (this.maxBases) after it's value has already been set.");
		
		if( bases == null )
			throw new IllegalArgumentException("Illegal attempt to pass null value to Once setter (this.maxBases).");
		
		//if( readGroupId.equals(XmlUtils.All_READGROUP ))			 	
		this.maxBases = bases;
	}	
	
	public long getMaxBases(){		
		if(null == this.maxBases || this.maxBases <= 0) 			 
			throw new IllegalStateException("Illegal attempt to access unitialized or minus value (this.maxBases).");
		
        return this.maxBases;
	}
	
	/**
	 * 
	 * @return number of reads excluds discarded one. 
	 */
	public long getCountedReads() {
		long totalgoodRead = 0  ;
		for (int i = 1 ; i < readLength.length() ; i++)			 
			totalgoodRead += readLength.get(i);
		
		return totalgoodRead + duplicate.get() + unmapped.get() + nonCanonical.get() ;		
	}
	
	//public void toXml(Element rgClipElement, Element summaryElement) {
	public void readSummary2Xml(Element parent ) { 	 		
						
			//add to xml RG_Counts
			Element rgElement = Qprofiler1XmlUtils.createSubElement( parent, "reads" );
			rgElement.setAttribute(XmlUtils.readGroupid, readGroupId);	
			rgElement.setAttribute(XmlUtils.count, inputReadCounts.get() + "");
						
			//add discarded read Stats to readgroup summary			
			Element stats = Qprofiler1XmlUtils.createSubElement( rgElement, XmlUtils.filteredReads );	
			stats.setAttribute( "supplementaryAlignmentCount", supplementary.get()+"" );
			stats.setAttribute( "secondaryAlignmentCount", secondary.get()+"" );
			stats.setAttribute( "failedVendorQualityCount", secondary.get()+"" );			
			
			long noOfRecords = getCountedReads( );
			double lostPercentage = 0; 
			//add discarded reads
			lostPercentage += addDiscardReadStats( rgElement, node_duplicate, duplicate, noOfRecords );
			lostPercentage += addDiscardReadStats( rgElement, node_unmapped, unmapped, noOfRecords );		
			lostPercentage += addDiscardReadStats( rgElement, node_nonCanonicalPair, nonCanonical, noOfRecords );			
						
			//add counted read stats to readgroup summary	
			int  maxReadLength = getMaxReadLength();
			if(! readGroupId.equals(XmlUtils.All_READGROUP )){
				setMaxBases( noOfRecords * maxReadLength );
				lostPercentage += addCountedReadStats(rgElement, "trimmedBase", parseTrim(readLength, maxReadLength), maxBases);				
			}else{
				double percentage = 100 * (double) getTrimedBases() / getMaxBases() ;
				Qprofiler1XmlUtils.createSubElement(rgElement, "trimmedBase").setAttribute(XmlUtils.basePercent, String.format("%2.2f", percentage ));
				lostPercentage += percentage;	
			}
						
			lostPercentage += addCountedReadStats( rgElement, node_softClip, softClip, maxBases );
			lostPercentage += addCountedReadStats( rgElement, node_hardClip, hardClip, maxBases );
			lostPercentage += addCountedReadStats( rgElement, node_overlap, overlapBase, maxBases );
			
			//add overall information to current readgroup element
			rgElement.setAttribute("maxLength", maxReadLength + "" );
			rgElement.setAttribute("averageLength", getAveReadLength() + "" );
			rgElement.setAttribute("lostBasesPercent", String.format("%2.2f", lostPercentage) );
								
	}	
	
	 	 
	public void pairSummary2Xml(Element parent) { 	 
		//add paring information
		//<readGroup pairCount="148242192" id="1bf380d4-8b0f-483c-bc84-fad5f8f2bf1c">
		Element pairElement = Qprofiler1XmlUtils.createSubElement(parent, "pairs");
		pairElement.setAttribute("readGroupId", readGroupId);	
		pairElement.setAttribute("pairCount", pairNum.get()+"");	
		
		// <mateUnmappedPair count="1743093" pairNumber_flag_p="0"/>	 	
		Element stats = Qprofiler1XmlUtils.createSubElement(pairElement, "mateUnmappedPair");
		stats.setAttribute(XmlUtils.count, mateUnmapped.get() + "");
		
		// <mateDifferentReferencePair count="3318739" pairNumber_flag_p="0"/>
		stats = Qprofiler1XmlUtils.createSubElement(pairElement, "mateDifferentReferencePair");
		stats.setAttribute(XmlUtils.count, diffRef.get() + "");

		f5f3.toXml( pairElement );
		f3f5.toXml( pairElement );
		inward.toXml( pairElement );
		outward.toXml( pairElement );
		
		Pair<Integer, String> isize = getIsizeStats();
		stats = Qprofiler1XmlUtils.createSubElement(pairElement, "tlen");
		stats.setAttribute( modal_isize, isize.getLeft() + "" );
		stats.setAttribute("stdDev", isize.getRight());
		
	}
	
	/**
	 * 
	 * @return a pair of isize modal value and std
	 */
	private Pair<Integer, String> getIsizeStats(){
		
		
		//move isize summary here	 
		Pair<Integer, Long> modal = new Pair<Integer, Long>(0, (long) 0);		
		StandardDeviation stdDev = new StandardDeviation();
		for(int i = 1; i < farTlenValue; i ++ ){
			if(isize.get(i) > modal.getRight() ) 
				modal = new Pair<Integer, Long>( i, isize.get(i) ); //= isize.get(i);
			for(int j = 0; j < isize.get(i); j ++){
				stdDev.increment(i);
			}	
		}

		return new Pair<Integer, String>(modal.getLeft() , String.format( "%,.2f",  stdDev.getResult()));
		
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
			Element stats = Qprofiler1XmlUtils.createSubElement(parent, nodeName);	
			stats.setAttribute("readCount", String.format("%d",counts.get()));
						 	
			//duplicats, non canonical and unmapped will be counted, others reads just discards
			float percentage = 100 * (float) counts.get() / totalReads ;						
			stats.setAttribute("basePercent",  String.format("%2.2f", percentage   ));		
			
			return percentage; 
			 	 
		}		
			
	private double addCountedReadStats(Element rgElement, String nodeName, QCMGAtomicLongArray array, long maxBases ){
		
		//get the position of median
		long bases = 0,counts = 0;
		long arrayLength = null != array ? array.length() : 0;
		for (int i = 1 ; i < arrayLength ; i++){
			counts += array.get(i);
			bases += i * array.get(i);
		}
		
		int mean = (counts == 0) ? 0: (int) (bases / counts);
		
		long medium = 0;
		for (int i = 0 ; i < arrayLength ; i++) {
			if((medium += array.get(i)) >= counts/2  ){
				medium = i; 
				break; 
			}
		}
		int min = 0, max = 0, mode = 0; 
		long highest = 0;
		for (int i = 1 ; i < arrayLength ; i++) {
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
		}
							
		Element stats = Qprofiler1XmlUtils.createSubElement(rgElement, nodeName);		
		stats.setAttribute( "min", min+"");
		stats.setAttribute( "max", max +"");
		stats.setAttribute("mean",mean+"");
		stats.setAttribute( "mode", mode+"") ;
		stats.setAttribute( "median", medium+"" );	
		stats.setAttribute("readCount",counts+"");				

		//deal with boundary value, missing reads
		double percentage = (maxBases == 0)? 0: 100 * (double) bases /  maxBases ;		
		stats.setAttribute(XmlUtils.basePercent,  String.format("%2.2f", percentage    ));		
		
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
