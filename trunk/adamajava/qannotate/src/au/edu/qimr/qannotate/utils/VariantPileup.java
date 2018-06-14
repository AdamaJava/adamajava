package au.edu.qimr.qannotate.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;
import htsjdk.samtools.SAMRecord;

import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.picard.util.SAMUtils;
import htsjdk.samtools.SAMTools;
import scala.Math;
import scala.actors.threadpool.Arrays;

public class VariantPileup {
	
	private final VcfRecord vcf; 
	private final SVTYPE variantType;
	private final int sampleColumnNo; //the first sample column marked as 1; 
	
	private final AtomicLong pairSame = new AtomicLong(); 
	private final AtomicLong pairDiff = new AtomicLong(); 
	private final AtomicLong refCount = new AtomicLong();
	private final AtomicLong altCount = new AtomicLong();
	private final AtomicLong otherCount = new AtomicLong();	
	private int depth = 0;
	
	//debug
	List<SAMRecord> reads = new ArrayList<>();
	public List<SAMRecord> getReads33(){ return reads; 	}
	/**
	 * 
	 * @param vcf: a vcf record of SNP, MNP, INDEL; this method only support one alternate bases, that is not allow common (,) appear on ALT column. 
	 * @param pool: a list of samrecord aligned cross over this muation region
	 */
	public VariantPileup(VcfRecord vcf, List<SAMRecord>  pool1, int columnNo){ 
		this.vcf = vcf;
		variantType = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		this.sampleColumnNo = columnNo;		
		
		//can't handle multi allels if they are different length;
		if(pool1 == null || pool1.size() == 0 || variantType.equals(SVTYPE.UNKNOWN)) return; 
		
		//copy pool to another array for further filtering
		List<SAMRecord>  pool = applyFilter(pool1);

		//debug
		if(vcf.getPosition() == 4511341 ){			 
			System.out.print("coverage: " + pool1.size() + ", informative: " + pool.size() + " => " + vcf.toSimpleString());
			int no = 0;
			for(SAMRecord re: pool){
				 
				reads.add(re);
				if(!SAMUtils.isSAMRecordValidForVariantCalling(re))System.out.println("not valid");				 
				int pos = re.getReadPositionAtReferencePosition(4511341);
				//debug check base
				byte[] baseQ = re.getBaseQualities(); 	
				if(baseQ[pos-1] < 10 )
				System.out.println(  re.getDuplicateReadFlag() + re.getReadName() + " ::: " +  re.getAlignmentStart() + " ~ " + re.getAlignmentEnd()  + "(base:" + pos + "th), cigar:" + re.getCigarString() );
				
//				for(int i = Math.max(0, pos-10); i< Math.min(pos+10, baseQ.length); i ++)
//					if(baseQ[i] < 10 ){System.out.println(no + " read contains low quality base on " + i  ); no++;break;				
						
			}
		
		}
		
		depth = pool.size();		//total number of reads
		Map<String,  Pair<SAMRecord,SAMRecord>> pairPool = new HashMap<>();		
		Map<String, SAMRecord> singlePool = new HashMap<>();
		
		for(SAMRecord re : pool){
//			System.out.println(re.getReadName() + " :: " + re.getAlignmentStart() + "~" +  re.getAlignmentEnd());
			
			String key = re.getReadName();
			if(pairPool.containsKey(key)){ pairPool.remove(key); }		
			if(singlePool.containsKey(key)){
				pairPool.put(key, new Pair<SAMRecord,SAMRecord>(singlePool.get(key), re));
				singlePool.remove(key);
			}else
				singlePool.put(key, re);
		}		
		//check pairs, if same base push first pair to singlePool		
		for(Pair<SAMRecord,SAMRecord> pair: pairPool.values()) 			 
			if(checkPair(pair)){
				pairSame.incrementAndGet();
				singlePool.put(pair.getLeft().getReadName(), pair.getLeft());
			}else
				pairDiff.incrementAndGet();
			 		
		//check singlePool
		for(SAMRecord re : singlePool.values()) 
			if(variantType.getOrder() < 5) checkMnp(re);
			else if(variantType.equals(SVTYPE.DEL)) checkDel(re);	
			else checkIns(re);	
	}

	/**
	 * 
	 * @param poolIn: a list of read aligned cross variant region, this pool can't be modified since it may use for next loop
	 * @return a new list of record which satisfied by qsnp or q3indle rule
	 */
	List<SAMRecord> applyFilter( List<SAMRecord>  poolIn){
		List<SAMRecord>  pool = new ArrayList<>( );
		//start will be first base of MNP; one base before leading base which is two base before actually indel
		int start = variantType.order <= 4? vcf.getPosition() : vcf.getPosition() - 1;
		//end will be last base of MNP; two base after indel
		int end =  variantType.order <= 4? vcf.getPosition() + vcf.getRef().length()-1 : vcf.getPosition() + vcf.getRef().length()+1;
				
		for(SAMRecord re: poolIn){
			if( re.getAlignmentStart() > start || re.getAlignmentEnd() < end ) continue;
			int baseStart = re.getReadPositionAtReferencePosition( start ); //return 1 if first base; 0 if deletion in read
			int baseEnd = re.getReadPositionAtReferencePosition( end );
			boolean add2Pool = true;
			if(baseStart == 0 || baseEnd == 0 ) ;
			else if( variantType.order < 5 ){  //check snp mnp etc
				byte[]  baseQ = re.getBaseQualities();
				for(int i = baseStart-1; i < baseEnd; i ++)
					if(baseQ[i] < 10 ){ add2Pool = false; break;}						
			}else if( variantType.equals(SVTYPE.INS) ){
				//deletion occurs, belong to nearby indel, add to pool; otherwise check N			 				
				byte[] maskedReadBases = re.getReadBases();
				for ( int i = baseStart; i < baseEnd; i++ )
					if( (char)maskedReadBases[i] == 'N'){ add2Pool = false; break;}									 
			}//end INS
			if( add2Pool )  pool.add( re ) ;			 			
		}		
			
		return pool; 
	 
//		System.err.println(vcf.toSimpleString() + ": 0 is impossible vairant postion: " + re.getReadName() + " :: " +re.getAlignmentStart() + " ~ " +re.getAlignmentEnd());		
//		System.err.println(insStart + "-" + insEnd  + " shift to " + re.getReadPositionAtReferencePosition(start, true) + "-" + re.getReadPositionAtReferencePosition(end , true));
//		if(!vcf.getChromosome().equals("chr2")) continue;
//		System.err.print("\n(" + start + "~" + end +")" + vcf.toSimpleString() );
//		System.err.println( re.getReadBases().length + "bases mapped to ref " + re.getAlignmentStart() + "-" + re.getAlignmentEnd() );
//		System.err.println(  " (ref=>offset) " + re.getReadPositionAtReferencePosition(re.getAlignmentStart()) + "-" + re.getReadPositionAtReferencePosition(re.getAlignmentEnd()));
		
	}
	
	/**
	 * check whether this read contain same deletion(supporting read), partial deletion (others), the other case are belong to reference counts
	 * @param re: a selected read aligned over mutaion region
	 */
	void checkMnp(SAMRecord re){
		//leading base is refernce base in front of MNP
		int start = re.getReadPositionAtReferencePosition(vcf.getPosition());
		//tail base should be last base of MNP
		int end = re.getReadPositionAtReferencePosition(vcf.getChrPosition().getEndPosition());	
		
		//deletion in MNP region
		if(start == 0 || end  == 0){  otherCount.incrementAndGet(); return;  }
		
		byte[] bases = Arrays.copyOfRange(re.getReadBases(),  start-1, end ); 	
		
		if( Arrays.equals(bases, vcf.getRef().getBytes()) ) refCount.incrementAndGet();
		else if( Arrays.equals(bases, vcf.getAlt().getBytes()) ) altCount.incrementAndGet();
		else otherCount.incrementAndGet();			
	}
	/**
	 * check whether this read contain same deletion(supporting read), partial deletion (others), the other case are belong to reference counts
	 * @param re: a selected read aligned over deletion region
	 */
	void checkDel(SAMRecord re){
		//leading base is refernce base in front of indel
		int start = re.getReadPositionAtReferencePosition(vcf.getPosition()); 
		//tail base should be one base after indel
		int end = re.getReadPositionAtReferencePosition(vcf.getPosition() + vcf.getRef().length() ); 
		
		//if deletion happen in tail of del (adjacant), it is partical deletion, belong to others
		if( start == 0 || end  == 0  ){	otherCount.incrementAndGet(); return;  }
		
		//match del: supporting read
		if(end - start == 1) altCount.incrementAndGet();
		//not deletion, maybe MNP/insertion, go to maf ref_count
		else if(end -start >= vcf.getRef().length()) refCount.incrementAndGet();
		//smaller deltion than vcf, partial supporting reads
		else otherCount.incrementAndGet();
	}
	/**
	 * check whether this read contain same insertion(supporting read), partial insertion (others), the other case are belong to reference counts
	 * @param re: a selected read aligned over insertion region
	 */
	void checkIns(SAMRecord re){
		//leading base is refernce base in front of indel
		int start = re.getReadPositionAtReferencePosition(vcf.getPosition()); 
		//tail base should be one base after indel
		int end = re.getReadPositionAtReferencePosition(vcf.getPosition() + vcf.getRef().length() ); 
		
		//deletion happen at the edge of ins, it is nearby indel, belong to maf ref_count
		if( start == 0 || end  == 0  )   refCount.incrementAndGet();  	
		//no insertion
		else if(end - start == 1)  refCount.incrementAndGet();  
		else{ 
			//check insertions, one leading base + inserted base == vcf.alt
			byte[] bases = Arrays.copyOfRange( re.getReadBases(),  start-1, end-1 ); 			
			//do not compare reference: eg. bases.equals(vcf.getAlt().getBytes())
			if(Arrays.equals(bases, vcf.getAlt().getBytes()))altCount.incrementAndGet();
			else otherCount.incrementAndGet(); 			
		}		
	}

	/**
	 * 
	 * @param pair: a pair of read aligned over same region
	 * @return true if pair with same base on mutation region
	 */
	boolean checkPair( Pair<SAMRecord,SAMRecord> pair ){
		
		if(pair.getLeft() == null || pair.getRight() == null){ System.err.println("dealing with pair of null"); return false;  }
		
		//check start base
		int start1 = pair.getLeft().getReadPositionAtReferencePosition(vcf.getPosition(), true );
		int start2 = pair.getRight().getReadPositionAtReferencePosition(vcf.getPosition(), true );
		//it shift to one base before mutation if read contains deletion. so we have to check both read ref positon again
		if( pair.getLeft().getReferencePositionAtReadPosition(start1) != pair.getRight().getReferencePositionAtReadPosition(start2) )
			return false;
		
		//check last base
		int end1 = pair.getLeft().getReadPositionAtReferencePosition(vcf.getChrPosition().getEndPosition() + 1 );
		int end2 = pair.getRight().getReadPositionAtReferencePosition(vcf.getChrPosition().getEndPosition() + 1 );
		// in case of deletion
		if(end1 == 0)  end1 = start1 + 1;
		if(end2 == 0)  end2 = start2 + 1;
		if( pair.getLeft().getReferencePositionAtReadPosition(end1) != pair.getRight().getReferencePositionAtReadPosition(end2) )
			return false;
 		
		 //check pair base
		try{
			byte[] bases1 = Arrays.copyOfRange(pair.getLeft().getReadBases(),  start1-1, end1 ); 
			byte[] bases2 = Arrays.copyOfRange(pair.getRight().getReadBases(),  start2-1, end2 );			
			//compare array not reference
			return  Arrays.equals(bases1, bases2);  			 
		}catch( ArrayIndexOutOfBoundsException e){
			System.err.println(vcf.toSimpleString() + " (pileup on): " + pair.getLeft().getReadName());
			System.out.println(pair.getLeft().getReadBases() + " : substring of "+ (start1-1) + " ~ " + end1 );
			System.out.println(pair.getRight().getReadBases() + " : substring of "+ (start2-1) + " ~ " + end2 );
		}
		
		return false; 
	}
	
	public String getAnnotation(){
		return String.format("%d[%d,%d,%d,%d,%d]", depth, pairSame.get(), pairDiff.get(), refCount.get(), altCount.get(),otherCount.get() );
	}
	
	public VcfRecord getVcf(){return vcf; }
	
	public int getSampleColumnNo(){ return sampleColumnNo; }
}
