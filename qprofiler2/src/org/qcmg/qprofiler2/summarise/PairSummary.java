package org.qcmg.qprofiler2.summarise;

import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.picard.BwaPair;
import org.qcmg.picard.BwaPair.Pair;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;

public class PairSummary {		
	public final Pair type;
	public final Boolean isProperPair; 
	public PairSummary( Pair pair, boolean isProper){this.type = pair; this.isProperPair = isProper;}

	
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
		int	overlap = BwaPair.getOverlapBase( record);
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
