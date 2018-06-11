package au.edu.qimr.qannotate.utils;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

public class ContigPileup implements Runnable {
	private final static QLogger logger = QLoggerFactory.getLogger(ContigPileup.class);
	public static final int  MAXRAMREADS = 5000; //maximum number of total reads in RAM	
	 
	private final AbstractQueue<VcfRecord> qIn;
	private final AbstractQueue<VariantPileup> qOut;
	private final int sampleColumnNo; 
	private final Thread mainThread;
	final CountDownLatch pLatch;
	private SAMSequenceRecord contig;
	private File bam; 	
	private QueryExecutor query;
	
	//unit Test only
	ContigPileup(){
		this.qIn = null;
		this.qOut = null;
		this.sampleColumnNo = -1;
		this.mainThread = null;
		this.pLatch = new CountDownLatch(2); // testing only
	}
	
	/**
	 * 
	 * @param contig: all selected vcf should happened on this contig
	 * @param qIn: store selected vcf records
	 * @param bam: alignment file which match the sample column on vcf
	 * @param query : query string
	 * @param qOut: vcfs  
	 * @param sampleColumn: number of sample column related to input bam file
	 * @param mainThread
	 * @param latch
	 */
	public ContigPileup(SAMSequenceRecord contig,  AbstractQueue<VcfRecord> qIn, File bam, QueryExecutor query,
			AbstractQueue<VariantPileup> qOut, int sampleColumn,  Thread mainThread, CountDownLatch latch)  {
		this.qIn = qIn;
		this.qOut = qOut;
		this.sampleColumnNo = sampleColumn;
		this.mainThread = mainThread;
		this.pLatch = latch;
		this.bam = bam;
		this.contig = contig;	
		this.query = query;
	}

	@Override
	public void run() {						
	   	AtomicLong outSize = new AtomicLong();
	   	
		if (qIn.size() <= 0) {
	 		logger.debug("There is no variant fallen in contig: " + contig.getSequenceName() );		 		
	 		return;
	 	}
		 			
		VcfRecord topPos= qIn.poll();
		File index = new File(bam.getAbsolutePath() + ".bai");
		if( !index.exists() && bam.getAbsolutePath().endsWith(".bam") )
			index = new File(bam.getAbsolutePath().replace(".bam", ".bai"));
		
		try (SamReader Breader =  SAMFileReaderFactory.createSAMFileReader(bam, index); ){		
			SAMRecordIterator ite = Breader.query(contig.getSequenceName(), 0, contig.getSequenceLength(),false);	
							
		 	List<SAMRecord> current_pool = new ArrayList<SAMRecord>();
		 	List<SAMRecord> next_pool = new ArrayList<SAMRecord>(); 
							
		 	while (ite.hasNext()) {	
		 		SAMRecord re = ite.next(); 		 		
		 		//bam file already sorted, skip non-indel region record
		 		//query take longer time so put to last condition
		 		if( re.getAlignmentEnd() < topPos.getPosition() || (query != null && query.Execute(re) != true) ) continue; 
		 		
		 		//whether in current indel region
		 		//if (re.getAlignmentStart() <= topPos.getChrPosition().getEndPosition() && current_pool.size() < MAXRAMREADS ) {
		 		if (re.getAlignmentStart() <= topPos.getPosition() && current_pool.size() < MAXRAMREADS ) {
		 			current_pool.add(re) ;			 			
		 		} else {			 			
		 			next_pool.add(re); 
		 			//pileup			 					 			 
		 			qOut.add( new VariantPileup( topPos,  current_pool, sampleColumnNo) );	
		 			outSize.incrementAndGet();
//		 			//debug
//		 			if(bam.getName().contains("6fecb447"))
//		 				System.out.println( bam.getName() + ">>> "+topPos.toSimpleString() );

		 			//prepare for next indel position 
		 			if( ( topPos = qIn.poll() ) == null ) break; 			 			
		 			resetPool( topPos,  current_pool, next_pool ); 
		 		}			 		
		 	}

		 	//after loop check all pool
		 	do{			
		 		//check whether previous loop also used up all indel position
		 		if( topPos == null ) break; 		
		 		qOut.add( new VariantPileup( topPos,  current_pool, sampleColumnNo ) ); 	
		 		outSize.incrementAndGet();
				if( ( topPos = qIn.poll()) == null ) break; 
				resetPool( topPos, current_pool, next_pool ); 							
		 	}while( true );			 					 
		} catch (Exception e) {
			logger.error( "Exception caught in pileup thread", e );
			mainThread.interrupt();
		} finally {			
			pLatch.countDown();
			logger.info( outSize.get() + " variants are completed pileup from " + contig.getSequenceName() + " on " + bam.getName());
//			if(bam.getName().contains("6fecb447")){
//				System.out.println(bam.getName() + "----" + contig.getSequenceName());
//				for(VariantPileup vp: qOut){
//					System.out.println(  vp.getAnnotation() + " :: "+ vp.getVcf().toSimpleString());
//				}
//			}
		}			
	}
	
	/**
	 * it swap SAMRecord between currentPool and nextPool. After then, the currentPool will contain all SAMRecord overlapping topPos position, 
	 * the nextPool will contain all SAMRecord start after topPos position.  All SAMRecord end before topPos position will be remvoved from both pool. 
	 * @param topPos:   pileup position
	 * @param currentPool: a list of SAMRecord overlapped previous pileup Position
	 * @param nextPool: a list of SAMRecord behind previous pileup Position
	 */
	public void resetPool( VcfRecord topPos,   List<SAMRecord> currentPool, List<SAMRecord> nextPool){	

		 //check current pool, remove reads aligned before snps
		 List<SAMRecord> toRemove = new ArrayList<SAMRecord>();	
		 for( SAMRecord  re : currentPool ){
			 if (re.getAlignmentEnd() < topPos.getPosition())
				 toRemove.add(re);
			 else if ( re.getAlignmentStart() > topPos.getChrPosition().getEndPosition() ) //shouldn't happen
				 System.err.println("input vcf is not sorted read start earlier than snp position: " + topPos.toSimpleString()  );	
			 //the remaining keep in current pool for snp pileup
		 }
		 
		 currentPool.removeAll(toRemove);		 
		 //check next pool
		 toRemove.clear();
		 for( SAMRecord  re : nextPool ){
			 if (re.getAlignmentEnd() < topPos.getPosition()) //discard reads never cover snp region
				 toRemove.add(re);
			 else if ( re.getAlignmentStart() <= topPos.getPosition()  ){ //move to current pool if cover current snp region				
				 toRemove.add(re); //remove from nextPool
				 currentPool.add(re); //move to current pool
			 }
			 //the remaining keep in next pool maybe for next snp
		 }
		 nextPool.removeAll(toRemove);
		 
	}		
		 
}
