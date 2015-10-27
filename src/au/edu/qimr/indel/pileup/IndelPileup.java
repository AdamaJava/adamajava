package au.edu.qimr.indel.pileup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.picard.util.SAMUtils;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;


public class IndelPileup {
	private  final int indelStart, indelEnd, nearbySoftClipWindow ,nearyIndelWindow; 
	private final ChrPosition position; 
	private final List<String> motifs ; //same chrposition but differnt allel
 	private final SVTYPE type; 
 	
	private  int coverage ;  //total counts start-end
	private  int informativeCount; //crossover full indels start - end
	private  int nearbysoftClip ; //reads contain softclipping in either side window
	private  int nearbyIndel ;//= new ArrayList<Integer>(); //must be informative	
	
	List<Integer> support = new ArrayList<Integer>();
	List<Integer> forwardsupport = new ArrayList<Integer>();
	List<Integer> backwardsupport = new ArrayList<Integer>();
	
	List<Integer> partial = new ArrayList<Integer>();  
	List<Integer> novelStart = new ArrayList<Integer>(); 
	
	public IndelPileup( IndelPosition pos, int nearbySoftClipWindow, int nearyIndelWindow) throws Exception { 	
		this.position = pos.getChrPosition();
		this.indelStart = pos.getStart();
		this.indelEnd = pos.getEnd();		
		this.type = pos.getIndelType();
		this.nearbySoftClipWindow = nearbySoftClipWindow;
		this.nearyIndelWindow = nearyIndelWindow;
		this.motifs = pos.getMotifs();		
		for(int i = 0; i < motifs.size(); i ++){
			support.add(i,0); 
			forwardsupport.add(i,0); 
			backwardsupport.add(i,0); 
			partial.add(i,0);
			novelStart.add(i,0);
		}
		
	}
		
	public void pileup(List<SAMRecord> pool) throws Exception{
			
		if(pool.size() == 0 ) return; 
		this.coverage  = pool.size();		
		if(coverage >= 1000) return; 
		
		this.nearbysoftClip = getSoftCounts(pool);
		List<SAMRecord> infoPool = getInformative(pool);
		this.informativeCount = infoPool.size();	
		
		if(infoPool.size() == 0) return; 
		
		//remove non indel reads
		List<SAMRecord> tmpPool = new ArrayList<SAMRecord>();
		for(SAMRecord re: infoPool){
			Cigar cigar = re.getCigar();			
			if (null == cigar || cigar.isEmpty() ||
					(! cigar.toString().contains("I") && ! cigar.toString().contains("D")) ) 
				continue; //skip current record
			else
				tmpPool.add(re);
		}			
		infoPool = tmpPool; 	
		
		tmpPool = removeNearbyIndel(infoPool, nearyIndelWindow);
		
		//assign real counts to overwrite the list init
		for(int i = 0; i < motifs.size(); i ++){			 
			int[] counts = getCounts(tmpPool, motifs.get(i));
			support.add(i, counts[0]);			
			forwardsupport.add(i, counts[1]);  
			backwardsupport.add(i, counts[2]);
			
			//debug
			if(counts[1] + counts[2] != counts[0])
				System.out.println("forwardsupport + backwardsupport != TotalSupport: " +  counts[1] + ", " + counts[2] + " , " + counts[0]);			
			
			partial.add(i,counts[3]);
			novelStart.add(i, counts[4]);
		}		
	}
	
	private List<SAMRecord> removeNearbyIndel(List<SAMRecord> pool, int window){
		int count = 0;
		int windowStart = indelStart - window;
		int windowEnd =  indelEnd + window;
		
		List<SAMRecord> nonearbyPool = new ArrayList<SAMRecord>();
								
		for(SAMRecord re: pool){
			boolean nearby = false; 
			int refPos = re.getAlignmentStart();
			Cigar cigar = re.getCigar();
			for (CigarElement ce : cigar.getCigarElements()){
				//insertion only one base, eg, start = 100; end = 101
				if(CigarOperator.I == ce.getOperator()){
					//check whether it is supporting or partical indel
					if(refPos == indelStart + 1){
						if(type.equals(SVTYPE.DEL) )
							nearby = true; 		
						//else is supporting or partial, go to next cigar block
					}else if(refPos > windowStart && refPos < windowEnd)
						nearby = true; 
				}else if( CigarOperator.D == ce.getOperator()){
					 
					//at least some overlap
					if((refPos <= indelStart && refPos + ce.getLength() >= indelStart) ||
							(refPos <= indelEnd && refPos + ce.getLength() >= indelEnd)){
						if(type.equals(SVTYPE.INS) ) 
							nearby = true; 
						//else is supporting or partial, go to next cigar block
					}else if((refPos > indelEnd &&  refPos < indelEnd + window ) ||							
							(refPos + ce.getLength() < indelStart &&  refPos + ce.getLength() > windowStart ))
						nearby = true; 
				}
								
				if(nearby) break; //quit current samrecord
				//go to next block
				else if (ce.getOperator().consumesReferenceBases()) 
					refPos += ce.getLength();
			}
			
			if(! nearby) nonearbyPool.add(re);				
			 else count ++;
		}
		
		this.nearbyIndel = count; 
		
		return nonearbyPool; //all nearby Reads are removed;
		
	}
	
	private Set<Integer> addToNovelStarts(SAMRecord record, Set<Integer> novelStarts) {
		
		if (record.getReadNegativeStrandFlag()) {
			novelStarts.add(record.getAlignmentEnd());
		} else {
			novelStarts.add(record.getAlignmentStart());
		}
		return novelStarts; 
		
	}	
	
	//	public void pileup(IndelPosition indel, List<SAMRecord> pool) throws Exception{
	private int[] getCounts(List<SAMRecord> pool, String motif) throws Exception{
		int support = 0;
		int partsupport = 0;
		int forwardSupport = 0;
		int backwardSupport = 0;
//		int nns = 0;
		Set<Integer> novelStarts = new HashSet<Integer>();				
		//check this indel is nearby, partial or full match
		
		for(SAMRecord re : pool){
			boolean flag = false; 			
			int refPos = re.getAlignmentStart();
			Cigar cigar = re.getCigar();
			for (CigarElement ce : cigar.getCigarElements()){					
				if(CigarOperator.I == ce.getOperator()){ 
					//if insert rePos go next cigar block after cigar.I, which is indel end position
				 	if(refPos == indelEnd && type.equals(SVTYPE.INS)){				 		 	
				 		if(ce.getLength() != motif.length()) 				 			
				 	 		partsupport ++;	
				 		 else{
						 	int startIndex = SAMUtils.getIndexInReadFromPosition(re, indelStart);
					 		int endIndex =  SAMUtils.getIndexInReadFromPosition(re, indelEnd  );	
				 			String base = re.getReadString().substring(startIndex+1,endIndex);
				 			if(motif.toLowerCase().matches(base.toLowerCase())){
				 				flag = true;
				 				support ++;
				 				novelStarts = addToNovelStarts(re, novelStarts);
				 			}else{
				 				partsupport ++; 
//				 				//debug
//								System.out.println("support reads: " + re.getSAMString());
				 			}
				 		 }	 
				 	}
				}else if( CigarOperator.D == ce.getOperator()){					 
					//at least some overlap
					if((refPos <= indelStart && refPos + ce.getLength() >= indelStart) ||
							(refPos <= indelEnd && refPos + ce.getLength() >= indelEnd)) 
						if(type.equals(SVTYPE.DEL) ){							
							if(refPos == indelStart && ce.getLength() == motif.length()){
				 				flag = true; 
				 				support ++;
				 				novelStarts = addToNovelStarts(re, novelStarts);
								
							}else{
								partsupport ++;
//								//debug
//								System.out.println("support reads: " + re.getReadString());
							}
						}
						//else is supporting or partial, go to next cigar block					 				 
				}
				
				// match indel
				if(flag){ 	
//					//debug
//					System.out.println("support reads: " + re.getReadString());
					
					if (re.getReadNegativeStrandFlag())   backwardSupport ++;
					else   forwardSupport ++;
					break;
				}
				//go to next block
				else if (ce.getOperator().consumesReferenceBases()) 
					refPos += ce.getLength();
				
			}
		}
				 
		int[] counts = {support, forwardSupport,backwardSupport, partsupport, novelStarts.size() };
		return counts; 
		
	}
		
	private boolean nBasePresentInInsertion(SAMRecord record ) {
		byte[] maskedReadBases = record.getReadBases();
	 
		int startIndex = SAMUtils.getIndexInReadFromPosition(record, indelStart);
		int endIndex =  SAMUtils.getIndexInReadFromPosition(record, indelEnd);		
		if (startIndex > -1 && endIndex > -1) {
			for (int i = startIndex; i<=endIndex; i++) {							
				char base = (char)maskedReadBases[i];										
				if (base == 'N') {					
					return true;
				}			
			}			
		}
		return false;
	}
	
	private List<SAMRecord> getInformative(List<SAMRecord> pool){
		
		List<SAMRecord> informativePool = new ArrayList<SAMRecord>();		
		int start = indelStart;
		int end = indelEnd; 
		
		//add one to make sure read extends PAST the indel, unless the deletion is one base only
		if (type.equals(SVTYPE.DEL)) {
			start -= 1;
			end += 1;
		}
		
		for( SAMRecord  record : pool ){					 
			//make sure that the read bases span the indel, 
			//the soft clip always on both edge of alignment, it is outside of [record.getAlignmentStart(),record.getAlignmentEnd() ]
			//at least one base before and after indel positon
			//if (record.getAlignmentStart() <= start && record.getAlignmentEnd() >= end) {	
			if (record.getAlignmentStart() < start && record.getAlignmentEnd() > end) {	
			    //skip reads with "N" insertion
				if(type.equals(SVTYPE.INS) && nBasePresentInInsertion(record)) 					
					continue; 			 
				informativePool.add(record);
			}	
//			//debug
//			else
//				System.out.println("non informativ reads: " + record.getSAMString());
		}
		
		return informativePool; 
	}
	
	private int getSoftCounts(List<SAMRecord> pool){ 
		int count = 0; 
		int windowStart = indelStart-nearbySoftClipWindow+1;
		int windowEnd = indelEnd+nearbySoftClipWindow-1;		

		for( SAMRecord  record : pool ){
			//check left hand clipping	
			if (record.getAlignmentStart() != record.getUnclippedStart()) {
				int clipStartPosition = record.getAlignmentStart()-1;
				if (clipStartPosition >= windowStart && clipStartPosition <= windowEnd){  			
					count ++;
//					//debug
//					System.out.println("softClip left: " + record.getSAMString());
				}
			}
			//check right hand clipping
			if (record.getAlignmentEnd() != record.getUnclippedEnd()) {
				int clipEndPosition = record.getAlignmentEnd()+1;			
				//clip start position is in the window to the left of the indel			
				if (clipEndPosition >= windowStart && clipEndPosition <= windowEnd){  			
					count ++;	
//					//debug
//					System.out.println("softClip right: " + record.getSAMString());
				}
			}
		}		
		return count;
	}
	//refer to qsnp speed up the process

	public ChrPosition getChrPosition() { return position; }
	
	public int getTotalCount(){ return coverage; }
	public int getInformativeCount(){ return informativeCount; }
	public int getNearybySoftclipCount(){ return nearbysoftClip; }
	public int getNearbyIndelCount(){ return nearbyIndel; }
	public int getsuportReadCount(int index){return support.get(index);}
	public int getforwardsuportReadCount(int index){return forwardsupport.get(index);}
	public int getbackwardsuportReadCount(int index){return backwardsupport.get(index);}
	
	public int getparticalReadCount(int index) { return partial.get(index); }
	public int getnovelStartReadCount(int index ){return novelStart.get(index); }
	
	
	public boolean hasStrandBias(int index, double d, double e) {
		//supporting reads are greater than 3
		if (support.get(index) >= 3) {
			//0 reads on one side
			if (forwardsupport.get(index) == 0 || backwardsupport.get(index) == 0) {
				return true;
			}
			
			double minP = d * 100;
			double maxP = e * 100;
			double forwardPercent = ((double) forwardsupport.get(index) / (double)support.get(index)) * 100;
			double reversePercent = ((double) backwardsupport.get(index) / (double)support.get(index)) * 100;
			if (forwardPercent < minP    || forwardPercent > maxP 
					|| reversePercent < minP  || reversePercent > maxP ) {
				return true;
			}					
		}

		return false;
	}
	
	//for unit test
	String getmotif(int index ){return motifs.get(index); }
	
}
