package org.qcmg.qprofiler2.summarise;

import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;

public class PairSummary {		
	public enum Pair{ 
		F3F5(1), F5F3(2), Inward(3), Outward(4), Others(5); 
		int id;
		Pair(int id){this.id = id;}
	}

	/**
	 *  
	 * @param record: a samRrecord
	 * @return 0 if record with negative tLen value or secondOfPair with zero tLen value ; otherwise return the overlapped base counts
	 */
    public static int getOverlapBase(SAMRecord  record) {
	   	 
    	//to avoid double counts, we only select one of Pair: tLen > 0 
        int iSize = record.getInferredInsertSize();
       	if(iSize < 0) return 0;  
       	//pick up first of pair if same strand read and tlen=0
       	if(iSize == 0 && !record.getFirstOfPairFlag() ) return 0;
        	       	
       	int result  = 0;  	
   		//|--> <--| mate end = tLen -1 + read start
   
   		if( record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag()  ) {  			
   			// outward pair without overlap
   			if( iSize == 0) {  return 0;   } 
   			
   			//We don't know forward mate end but know reverse mate end = tLen + read start
   			int mateEnd = iSize + record.getAlignmentStart()-1 ;  //reverse mate end
   			int readEnd = record.getAlignmentEnd();   //forward read start
   			result = Math.min( readEnd, mateEnd ) - Math.max(record.getAlignmentStart(), record.getMateAlignmentStart() ) + 1;   
   		}
   		
   		//|--->    or     |-----> (read end - mate start>0) or       |----->
   		//|----->              |----->                      |---> (read end - mate start < 0)  		
		//if tLen >= 0, then mate_start > read_start; so min_end will be read end if we assue pair with same length. 
   		else if( !record.getReadNegativeStrandFlag()) {  		
   			result = record.getAlignmentEnd() - record.getMateAlignmentStart() + 1; 			
   		}else {  		
	  		//<---|    or     <-----| (read end - mate start>0) or       <-----|
	   		//<-----|             <-----|                      <---| (read end - mate start < 0)
	   		result = record.getAlignmentEnd() - Math.max( record.getAlignmentStart(), record.getMateAlignmentStart()) + 1;			
   		}
   		
   		return result; 	
    }
	public static Pair getPairType( SAMRecord  record ) {
	 
		if(record.getReadUnmappedFlag() ||  record.getMateUnmappedFlag() || ! record.getReferenceName().equals(record.getMateReferenceName()) )
			return Pair.Others;
		
		// pair with different orientation 
		if( record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag()  ) {	
			// |----->  <-------|(read is reverse)
			if( record.getReadNegativeStrandFlag() && record.getAlignmentStart() >= record.getMateAlignmentStart())
				return Pair.Inward;
				
			// (read is forward)|----->  <-------| 
			if( !record.getReadNegativeStrandFlag() && record.getAlignmentStart() <= record.getMateAlignmentStart())
				return Pair.Inward;
	 					
			return Pair.Outward;
		}
				
		//non-canonical: pair with same orientation  		
		//first of pair start
		int s1 = ( record.getFirstOfPairFlag())? record.getAlignmentStart() : record.getMateAlignmentStart();		
		//second of pair start
		int s2 = ( !record.getFirstOfPairFlag())? record.getAlignmentStart() : record.getMateAlignmentStart();
						    	 		 
		//F3(s1)|---->  F5(s2)|------>
		if( !record.getReadNegativeStrandFlag() &&  s1 <= s2)  return Pair.F3F5;
		//F5(s2)<----|  F3(s1)<------|
		if( record.getReadNegativeStrandFlag() &&  s1 >= s2)  return Pair.F3F5;		
		//F5(s2)|---->  F3(s1)|------>
		if( !record.getReadNegativeStrandFlag() &&  s1 > s2)  return Pair.F5F3;
		//F3(s1)<----|  F5(s2)<----|
		if( record.getReadNegativeStrandFlag() &&  s1 < s2)  return Pair.F5F3;
				
		return Pair.Others;
	}	
	
	public final Pair type;
	public final Boolean isProperPair; 
	public PairSummary( Pair pair, boolean isProper){this.type = pair; this.isProperPair = isProper;}
	
	/*
	 * I decide not to override equals and hashCode method. 
	 * The original plan is to create unique PairSummary based on pair orientation and ProperPair flag. 
	 * However, two objects may have the same Pair and flag but still possible have different counts. 
	 * They should be considered non-equal.
	 */
//	@Override
//	public boolean equals(Object obj) {
//		
//		if (obj == null ) return false;	
//		if (!(obj instanceof PairSummary)) return false;
//		
//		if(this.type == null || ((PairSummary) obj).type == null ) { 
//			return false;		
//		}
//		
//		if( ((PairSummary) obj).type.equals(this.type)  &&
//				((PairSummary) obj).isProperPair == this.isProperPair ) {
//			return true;
//		}
//		
//		return false;
//	}
//	
//	@Override
//	public int hashCode() {	
//		if(type == null ) return 0; 
//		
//	    return isProperPair? this.type.id  : this.type.id * -1;	    
//	}
	
	//fixed value
	public final static int bigTlenValue = 10000;
	public final static int smallTlenValue = 1500;
	public final static int middleTlenValue = 5000;
	public final static int rangeGap = 100;
	public final static int segmentSize = 500; //assume 
	
	//AtomicLong overlapPair = new AtomicLong();
	AtomicLong near = new AtomicLong();
	AtomicLong far = new AtomicLong();
	AtomicLong bigTlen  = new AtomicLong();			
	
	QCMGAtomicLongArray tLenOverall = new QCMGAtomicLongArray(middleTlenValue);	 //store count bwt [0, 5000]
	QCMGAtomicLongArray tLenOverlap = new QCMGAtomicLongArray(segmentSize);	 //store count bwt [0, 1500]
	QCMGAtomicLongArray overlapBase = new QCMGAtomicLongArray(segmentSize);	
	
	//pair in different reference
	AtomicLong diffRef  = new AtomicLong();	
	AtomicLong mateUnmapped  = new AtomicLong();	

	AtomicLong firstOfPairNum  = new AtomicLong();
	AtomicLong secondOfPairNum  = new AtomicLong();
	
	protected QLogger logger = QLoggerFactory.getLogger(getClass());			
	public long getFirstOfPairCounts() { return firstOfPairNum.get(); }
	public long getSecondOfPairCounts(){ return secondOfPairNum.get(); }
	public QCMGAtomicLongArray getoverlapCounts() {return overlapBase;}
	public QCMGAtomicLongArray getTLENCounts() {return tLenOverall;}
				
	/**
	 * only select one read from a pair for summary. here, read with positive tLen or firOfPair if tLen == 0 are selected. 
	 * it's tLen, overlap information will be collected. 
	 * @param record
	 */
	public void parse(SAMRecord record ){	
			
		if(record.getFirstOfPairFlag()) firstOfPairNum.incrementAndGet();
		else secondOfPairNum.incrementAndGet();
	
		//normally bam reads are mapped, if the mate is missing, we still count it to pair but no detailed pair` information
		if( record.getMateUnmappedFlag() ){ mateUnmapped.incrementAndGet();  return;  }
		//pair from different reference, only look at first pair to avoid double counts
		else if( !record.getReferenceName().equals( record.getMateReferenceName()) && record.getFirstOfPairFlag()){
			diffRef.incrementAndGet();	
			return; 
		}	
				
		int tLen =  record.getInferredInsertSize();	
 		//to avoid double counts, we only select one of Pair: tLen > 0 or firstOfPair with tLen==0;
 		if( tLen < 0 || (tLen == 0 && !record.getFirstOfPairFlag())   ) {
 			return;
  		}
		
		//only record popular tLen, since RAM too expensive
		if(tLen < middleTlenValue) tLenOverall.increment(tLen);
		
		//classify tlen groups
		int	overlap = getOverlapBase( record);
		if( overlap > 0 ){
			overlapBase.increment(overlap);
			tLenOverlap.increment(tLen);			
		} else if( tLen < smallTlenValue  ){
			near.incrementAndGet();	
		} else if( tLen < bigTlenValue ){
			far.incrementAndGet();	
		}else { // must be record.getInferredInsertSize() >= bigTlenValue
			bigTlen.incrementAndGet();
		} 		
	}		
	
	/**
	 * output example: 	 
	 *  <variableGroup name="outwardPair">
			<value name="overlappedPairs">898775</value>
			<value name="tlenUnder1500Pairs">219588</value>
			<value name="tlenOver10000Pairs">572986</value>
			<value name="tlenBetween1500And10000Pairs">237035</value>
			<value name="pairCount">1928384</value>
		</variableGroup>
	 * @param parent element
	 */
	
	void toSummaryXml(Element parent  ){	
		long overlapPair = overlapBase.toMap().values().stream().mapToLong(e->e.get()).reduce(0, (x,y) -> x+y);
		long pairCoutns = tLenOverall.toMap().values().stream().mapToLong(e->e.get()).reduce(0, (x,y) -> x+y);
	
		Element stats = XmlUtils.createGroupNode(parent, type.name());
		XmlUtils.outputValueNode(stats, "firstOfPairs", firstOfPairNum.get());
		XmlUtils.outputValueNode(stats, "secondOfPairs", secondOfPairNum.get());
		XmlUtils.outputValueNode( stats, "mateUnmappedPair", mateUnmapped.get() );
		XmlUtils.outputValueNode( stats, "mateDifferentReferencePair", diffRef.get() );
		XmlUtils.outputValueNode(stats, "overlappedPairs", overlapPair);
		XmlUtils.outputValueNode(stats, "tlenUnder1500Pairs", near.get() );		 
		XmlUtils.outputValueNode(stats, "tlenOver10000Pairs", bigTlen.get()  );
		XmlUtils.outputValueNode(stats, "tlenBetween1500And10000Pairs",far.get() );			
		XmlUtils.outputValueNode(stats, "pairCountUnderTlen5000", pairCoutns );		
	}	
	

	
}
