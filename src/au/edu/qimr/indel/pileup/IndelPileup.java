package au.edu.qimr.indel.pileup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.picard.util.SAMUtils;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;


public class IndelPileup {
	private  final int indelStart, indelEnd, nearbySoftClipWindow ,nearyIndelWindow; 
	private final ChrRangePosition position; 
	private final List<String> motifs ; //same ChrRangePosition but differnt allel
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
		this.position = pos.getChrRangePosition();
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
				
		List<SAMRecord> tmpPool = getRegionIndels(infoPool, nearyIndelWindow);		
		//assign real counts to overwrite the list init
		for(int i = 0; i < motifs.size(); i ++){			 
			int[] counts = getCounts(tmpPool, motifs.get(i));
			support.add(i, counts[0]);			
			forwardsupport.add(i, counts[1]);  
			backwardsupport.add(i, counts[2]);
			
			partial.add(i,counts[3]);
			novelStart.add(i, counts[4]);			
		}
	}
	
	/**
	 * 
	 * @param pool: a pool of input reads
	 * @param window: window size on both side of indel
	 * @return: a pool of reads which are potential supporting reads or partial supporting reads
	 */
	private List<SAMRecord> getRegionIndels(List<SAMRecord> pool, int window){
		int count = 0;
		int windowStart = indelStart - window;
		int windowEnd =  indelEnd + window;
		
 		List<SAMRecord> regionPool = new ArrayList<SAMRecord>();
 		
		for(SAMRecord re: pool){
			Cigar cigar = re.getCigar();		 		
			if (null == cigar || cigar.isEmpty() ||
					(! cigar.toString().contains("I") && ! cigar.toString().contains("D")) ) 
					continue; //skip current record	}		
			
			boolean nearby = false; 
			boolean support = false; 
			int refPos = re.getAlignmentStart();
					
			for (CigarElement ce : cigar.getCigarElements()){				
				//insertion only one base, eg, start = 100; end = 101
				if(CigarOperator.I == ce.getOperator()){
					//check whether it is supporting or partical indel
					//if(refPos == indelStart ){
					if (refPos >= indelStart && refPos <= indelEnd)  {	
						if(type.equals(SVTYPE.DEL) ) 
							nearby = true; 	//insertion inside deletion region							
						else	
							support = true; // refPos==indelStart=indelEnd
					}else if(refPos > windowStart && refPos < windowEnd)						
						nearby = true;  //nearby insertion overlap the window
				}else if( CigarOperator.D == ce.getOperator()){
					// deletion overlaps variants, full/part supporting reads
						//rePos inside indle region
					if( (refPos >= indelStart && refPos <= indelEnd)  || 
						//indel chock end inside indel region
						(refPos + ce.getLength() -1 >= indelStart && refPos + ce.getLength() -1 <= indelEnd) || 
						//indel chock have base on both side of indel region
						(refPos <= indelStart && refPos + ce.getLength() -1 >= indelEnd)){						
						if(type.equals(SVTYPE.INS))
							nearby = true;		//nearyby deletion
						else							
							support = true; // supporting or partial 						 	
					}else if((refPos >= windowStart &&  refPos <= windowEnd) ||
							(refPos + ce.getLength() -1 >= windowStart && refPos + ce.getLength() -1 <= windowEnd) ||
							(refPos <= windowStart && refPos + ce.getLength() -1 >= windowEnd)){
											
						nearby = true;			//nearby deletion 	
					}
				}
												
				if(nearby)
					break; //quit current samrecord				
					
				//go to next block
				if (ce.getOperator().consumesReferenceBases()) 
					refPos += ce.getLength();
			}
			
			if(  nearby)  count ++;
			else if(support)
				regionPool.add(re); //if support but nearby won't be added 
		}
		
		this.nearbyIndel = count; 
				
		return regionPool; //all nearby/faraway indle reads are removed;
		
	}
	
	private Set<Integer> addToNovelStarts(SAMRecord record, Set<Integer> novelStarts) {
		
		if (record.getReadNegativeStrandFlag()) {			
			novelStarts.add(record.getAlignmentEnd());
		} else {
			novelStarts.add(record.getAlignmentStart());
		}
		return novelStarts; 
		
	}	
	
	/**
	 * 
	 * @param pool: potential support or partial support reads
	 * @param motif
	 * @return the counts of support, forwardSupport,backwardSupport, partsupport, novelStarts 
	 * @throws Exception
	 */
	private int[] getCounts(List<SAMRecord> pool, String motif) throws Exception{
		int support = 0;
		int partsupport = 0;
		int forwardSupport = 0;
		int backwardSupport = 0;
		Set<Integer> novelStarts = new HashSet<Integer>();				
		//check this indel is nearby, partial or full match
		
		for(SAMRecord re : pool){
			boolean supportflag = false; 
			boolean partialflag = false; 
			int refPos = re.getAlignmentStart();
			Cigar cigar = re.getCigar();
			for (CigarElement ce : cigar.getCigarElements()){					
				if(CigarOperator.I == ce.getOperator() && (refPos == indelEnd && type.equals(SVTYPE.INS))){ 
					//if insert rePos go next cigar block after cigar.I, which is indel end position			 	 			 		 	
			 		if(ce.getLength() != motif.length())
			 			partialflag = true; 				 	 
			 		 else{
					 	int startIndex = SAMUtils.getIndexInReadFromPosition(re, indelStart);
				 		int endIndex =  SAMUtils.getIndexInReadFromPosition(re, indelEnd  );	
			 			String base = re.getReadString().substring(startIndex+1,endIndex);
			 			if(motif.toLowerCase().matches(base.toLowerCase())){
			 				supportflag = true;				 				 
			 				novelStarts = addToNovelStarts(re, novelStarts);
			 				//System.out.println("supportI: " + re.getSAMString());
			 			}else 
			 				partialflag = true; 
			 		 }	 
				 	 
				}else if( CigarOperator.D == ce.getOperator())				 			
//					if((refPos <= indelStart && refPos + ce.getLength() >= indelStart) || (refPos <= indelEnd && refPos + ce.getLength() >= indelEnd)) 
					//part or more overlap with del region	
					if(refPos <= indelEnd && refPos + ce.getLength() >= indelStart && type.equals(SVTYPE.DEL)){ 												 						
							if(refPos == indelStart && ce.getLength() == motif.length()){
				 				supportflag = true; 				 				
				 				novelStarts = addToNovelStarts(re, novelStarts);					 				 
							}else
								partialflag = true; 
					}						
				
				// match indel
				if(supportflag){ 					
					if (re.getReadNegativeStrandFlag())   backwardSupport ++;
					else   forwardSupport ++;
					support ++;
					break;
				}else if(partialflag){
					partsupport ++;
					break;					
				//go to next block 		
				}else if (ce.getOperator().consumesReferenceBases()) 
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
		
		for( SAMRecord  record : pool )					 
			//make sure that the read bases span the indel, 
			//the soft clip always on both edge of alignment, it is outside of [record.getAlignmentStart(),record.getAlignmentEnd() ]
			//at least one base before and after indel positon
			if (record.getAlignmentStart() < start && record.getAlignmentEnd() > end) {	
			    //skip reads with "N" insertion
				if(type.equals(SVTYPE.INS) && nBasePresentInInsertion(record)) 					
					continue; 			 
				informativePool.add(record);
			}	
		
		
		return informativePool; 
	}
	
//	private int getSoftCounts(List<SAMRecord> pool){ 
//		int count = 0; 
//		int windowStart = indelStart-nearbySoftClipWindow+1;
//		int windowEnd = indelEnd+nearbySoftClipWindow-1;	
//
//		for( SAMRecord  record : pool ){						
//			//check left hand clipping	
//			if (record.getAlignmentStart() != record.getUnclippedStart()) {				
//				int clipStartPosition = record.getAlignmentStart()-1;
//				if (clipStartPosition >= windowStart && clipStartPosition <= windowEnd){  
//					//debug
//					System.out.println(windowStart + "~" + windowEnd + ", left soft: " + record.getSAMString());
//					count ++;
//				}
//			}
//			//check right hand clipping
//			if (record.getAlignmentEnd() != record.getUnclippedEnd()) {
//				int clipEndPosition = record.getAlignmentEnd()+1;			
//				//clip start position is in the window to the left of the indel			
//				if (clipEndPosition >= windowStart && clipEndPosition <= windowEnd){  
//					//debug
//					System.out.println(windowStart + "~" + windowEnd + ", right soft: " + record.getSAMString());
//
//					count ++;	
//				}
//			}
//		}		
//		return count;
//	}
	
	
	private int getSoftCounts(List<SAMRecord> pool){ 
		int count = 0; 
		int windowStart = indelStart-nearbySoftClipWindow+1;
		int windowEnd = indelEnd+nearbySoftClipWindow-1;	

		for( SAMRecord  record : pool ){								 	
			int clipStartPosition = record.getAlignmentStart()-1;				 
			Cigar cigar = record.getCigar();
			for (CigarElement ce : cigar.getCigarElements()){ 				
				if(CigarOperator.S == ce.getOperator()){ 											
					if (clipStartPosition <= windowEnd && clipStartPosition >= windowStart){  						
						count ++;	break;
					}					
				}
				clipStartPosition += (ce.getOperator().consumesReferenceBases()) ? ce.getLength() : 0;
			}
		}		
		return count;
	}	
	
	
	
	//refer to qsnp speed up the process

	public ChrRangePosition getChrRangePosition() { return position; }
	
	public int getTotalCount(){ return coverage; }
	public int getInformativeCount(){ return informativeCount; }
	public int getNearybySoftclipCount(){ return nearbysoftClip; }
	public int getNearbyIndelCount(){ return nearbyIndel; }
	public int getsuportReadCount(int index){return support.get(index);}
	public int getforwardsuportReadCount(int index){return forwardsupport.get(index);}
	public int getbackwardsuportReadCount(int index){return backwardsupport.get(index);}
	
	public int getparticalReadCount(int index) { return partial.get(index); }
	public int getnovelStartReadCount(int index ){return novelStart.get(index); }
	
	
	public boolean hasStrandBias(int index, double d) {
		//supporting reads are greater than 3
		if(support.get(index) <= 0)
			return false; 
		
		double minP = d * 100;
		double fPercent = ((double) forwardsupport.get(index) / (double)support.get(index)) * 100;
		double rPercent = ((double) backwardsupport.get(index) / (double)support.get(index)) * 100;
		if( Math.min(fPercent, rPercent) < minP)
			return true; 		 

		return false;
	}
	
	//for unit test
	String getmotif(int index ){return motifs.get(index); }
	
}
