package au.edu.qimr.qannotate.utils;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

public class ContigPileup implements Runnable {
	private final static QLogger logger = QLoggerFactory.getLogger(ContigPileup.class);
	public static final int  MAXRAMREADS = 500; //maximum number of total reads in RAM	
	 
	private final AbstractQueue<ChrPosition> qIn;
	private final AbstractQueue<SnpPileup> qOut;
	private final Thread mainThread;
	final CountDownLatch pLatch;
	private SAMSequenceRecord contig;
	private File bam; 	
	private QueryExecutor query;
	
	//unit Test only
	ContigPileup(){
		this.qIn = null;
		this.qOut = null;
		this.mainThread = null;
		this.pLatch = new CountDownLatch(2); // testing only
	}

	/**
	 * 
	 * @param qIn : store SAM record from input file
	 * @param qOutGood : store unmatched record based on query
	 * @param qOutBad: store unmatched record based on query (null is allowed)
	 * @param query : query string
	 * @param maxRecords : queue size
	 * @param mainThread : parent thread
	 * @param rLatch : the counter for reading thread
	 * @param fLatch : the counter for filtering thread (current type)
	 */
	public ContigPileup(SAMSequenceRecord contig,  AbstractQueue<ChrPosition> qIn, File bam, QueryExecutor query,
			AbstractQueue<SnpPileup> qOut, Thread mainThread, CountDownLatch latch)  {
		this.qIn = qIn;
		this.qOut = qOut;
		this.mainThread = mainThread;
		this.pLatch = latch;
		this.bam = bam;
		this.contig = contig;	
		this.query = query;
	}

	@Override
	public void run() {						
	   	
		if (qIn.size() <= 0) {
	 		logger.debug("There is no snp fallen in contig: " + contig.getSequenceName() );		 		
	 		return;
	 	}
		 			
		ChrPosition topPos= qIn.poll();
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
		 		if( re.getAlignmentEnd() < topPos.getStartPosition() || (query != null && query.Execute(re) != true) ) continue; 

		 		//whether in current indel region
		 		if (re.getAlignmentStart() <= topPos.getEndPosition() && current_pool.size() < MAXRAMREADS ) {
		 			current_pool.add(re) ;			 			
		 		} else {			 			
		 			next_pool.add(re); 
		 			//pileup			 					 			 
		 			qOut.add( new SnpPileup( topPos,  current_pool) );	
		 			
		 			//prepare for next indel position 
		 			if( ( topPos = qIn.poll() ) == null ) break; 			 			
		 			resetPool( topPos,  current_pool, next_pool ); 
		 		}			 		
		 	}

		 	//after loop check all pool
		 	do{			
		 		//check whether previous loop also used up all indel position
		 		if( topPos == null ) break; 		
		 		qOut.add( new SnpPileup( topPos,  current_pool ) );
 	 			
				if( ( topPos = qIn.poll()) == null ) break; 
				resetPool( topPos, current_pool, next_pool ); 							
		 	}while( true );			 					 
		} catch (Exception e) {
			logger.error( "Exception caught in pileup thread", e );
			mainThread.interrupt();
		} finally {			
			pLatch.countDown();
			logger.info( qOut.size() + " snps is completed pileup from " + contig.getSequenceName() + " on " + bam.getName());
		}			
	}		
	/**
	 * it swap SAMRecord between currentPool and nextPool. After then, the currentPool will contain all SAMRecord overlapping topPos position, 
	 * the nextPool will contain all SAMRecord start after topPos position.  All SAMRecord end before topPos position will be remvoved from both pool. 
	 * @param topPos:   pileup position
	 * @param currentPool: a list of SAMRecord overlapped previous pileup Position
	 * @param nextPool: a list of SAMRecord behind previous pileup Position
	 */
	
	public void resetPool( ChrPosition topPos,   List<SAMRecord> currentPool, List<SAMRecord> nextPool){	

		 //check current pool, remove reads aligned before snps
		 List<SAMRecord> toRemove = new ArrayList<SAMRecord>();	
		 for( SAMRecord  re : currentPool ){
			 if (re.getAlignmentEnd() < topPos.getStartPosition()) 
				 toRemove.add(re);
			 else if ( re.getAlignmentStart() > topPos.getEndPosition() ) //shouldn't happen
				 System.err.println("input vcf is not sorted read start earlier than snp position: " + topPos.toIGVString()  );	
			 //the remaining keep in current pool for snp pileup
		 }
		 
		 currentPool.removeAll(toRemove);
		 
		 //check next pool
		 toRemove.clear();
		 for( SAMRecord  re : nextPool ){
			 if (re.getAlignmentEnd() < topPos.getStartPosition()) //discard reads never cover snp region
				 toRemove.add(re);
			 else if ( re.getAlignmentStart() <= topPos.getEndPosition() ){ //move to current pool if cover current snp region				
				 toRemove.add(re); //remove from nextPool
				 currentPool.add(re); //move to current pool
			 }
			 //the remaining keep in next pool maybe for next snp
		 }
		 nextPool.removeAll(toRemove);
		 
	}		
		 
}
