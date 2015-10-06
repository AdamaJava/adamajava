package au.edu.qimr.indel.pileup;


import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.indel.Options;
import au.edu.qimr.indel.pileup.Homopolymer;
import au.edu.qimr.indel.pileup.IndelPileup;
import au.edu.qimr.indel.pileup.IndelPosition;
import au.edu.qimr.indel.pileup.ReadIndels;


public class IndelMT {
		
	class contigPileup implements Runnable {

		private final AbstractQueue<IndelPosition> qIn;
		private final AbstractQueue<IndelPileup> qOut;
		private final Thread mainThread;
		final CountDownLatch pLatch;
//		final CountDownLatch wLatch;
		private int countOutputSleep;
		private SAMSequenceRecord contig;
		private File bam; 
		private QueryExecutor exec;
		private SAMRecordIterator ite;

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
		contigPileup(SAMSequenceRecord contig,  AbstractQueue<IndelPosition> qIn, File bam, QueryExecutor exec,
				AbstractQueue<IndelPileup> qOut, Thread mainThread, CountDownLatch latch) throws Exception {
			this.qIn = qIn;
			this.qOut = qOut;
			this.mainThread = mainThread;
			this.pLatch = latch;
//			this.wLatch = wLatch;
			this.bam = bam;
			this.contig = contig;
			this.exec = exec; 
 		}

		@Override
		public void run() {
		 	List<SAMRecord> current_pool = new ArrayList<SAMRecord>();
		 	List<SAMRecord> next_pool = new ArrayList<SAMRecord>(); 		 	
		 	IndelPosition topPos= qIn.poll();
			if (topPos == null) {
		 		logger.info("There is no indel fallen in contig: " + contig.getSequenceName() );
		 		
		 		return;
		 	}
		 	
			try (SAMFileReader Breader = new SAMFileReader(bam )){	
				
				SAMRecordIterator ite = Breader.query(contig.getSequenceName(), 0, contig.getSequenceLength(),false);		
			 	while (ite.hasNext()) {	
			 		SAMRecord re = ite.next(); 
			 		//bam file already sorted, skip non-indel region record
			 		if(re.getAlignmentEnd() < topPos.getStart()) continue; 
			 		
			 		//only interested pass filter record
			 		boolean passFilter; 	
			 		if(exec != null )
						passFilter = exec.Execute(re);
					else
						passFilter = !re.getReadUnmappedFlag() && (!re.getDuplicateReadFlag() || options.includeDuplicates());
			 		if(! passFilter ) continue; 
			 			 		
			 		//whether in current indel region
			 		if(re.getAlignmentStart() <= topPos.getEnd())
			 			current_pool.add(re);
			 		else{
			 			next_pool.add(re); 
			 			//pileup
			 			IndelPileup pileup= new IndelPileup(topPos, options.getSoftClipWindow(), options.getNearbyIndelWindow());
			 			pileup.pileup(current_pool);
			 			qOut.add(pileup);
			 			
			 			//prepare for next indel position
			 		//	topPos = qIn.poll();
			 			if( (topPos = qIn.poll()) == null) break; 
			 			resetPool(topPos,  current_pool, next_pool); 	 				 			 
			 		}
			 	}	 	
			
			 	//after loop check all pool
			 	do{			
			 		//check whether previous loop also used up all indel position
			 		if(topPos == null) break; 
			 			
		 			IndelPileup pileup= new IndelPileup(topPos, options.getSoftClipWindow(), options.getNearbyIndelWindow());
		 			pileup.pileup(current_pool);
		 			qOut.add(pileup);
					
					if( (topPos = qIn.poll()) == null) break; 
					resetPool(topPos,  current_pool, next_pool); 							
			 	}while( true ) ;					 					 
				 
				logger.info( contig.getSequenceName() + " completed pileup indels: " + qOut.size()  + " on " + bam.getName());
			} catch (Exception e) {
				logger.error("Exception caught in pileup thread", e);
				mainThread.interrupt();
			} finally {
				pLatch.countDown();
//				logger.debug(String.format(" total slept %d times since input queue is " +
//						"empty and %d time since either output queue is full. " +
//						"each sleep take %d mill-second. queue size for qIn, qOutqOutBad are %d,%d",
//						sleepcount, countOutputSleep, sleepUnit, qIn.size(), qOut.size()));
			}			
		}
	
		
		/**
		 * it swap SAMRecord between currentPool and nextPool. After then, the currentPool will contain all SAMRecord overlapping topPos position, 
		 * the nextPool will contain all SAMRecord start after topPos position.  All SAMRecord end before topPos position will be remvoved from both pool. 
		 * @param topPos:   pileup position
		 * @param currentPool: a list of SAMRecord overlapped previous pileup Position
		 * @param nextPool: a list of SAMRecord behind previous pileup Position
		 */
		private void resetPool( IndelPosition topPos, List<SAMRecord> currentPool, List<SAMRecord> nextPool){
				List<SAMRecord> tmp_current_pool = new ArrayList<SAMRecord>();			
				
				List<SAMRecord> tmp_pool = new ArrayList<SAMRecord>();	
				tmp_pool.addAll(nextPool);
				
				//check read record behind on current position			
				for( SAMRecord  re : tmp_pool ){
					//aligned position before indel
					if(re.getAlignmentEnd() < topPos.getStart())
						nextPool.remove(re);
					//aligned position cross indel
					else if(re.getAlignmentStart() <= topPos.getEnd()) 	 					 
						tmp_current_pool.add(re);	 				 
				}	 

				
				tmp_pool.clear();
				tmp_pool.addAll(currentPool);
				//check already read record  for previous pileup
				for( SAMRecord  re1 : tmp_pool ){
					//aligned position before indel
					if(re1.getAlignmentEnd() < topPos.getStart())
						currentPool.remove(re1);
					//aligned position after indel
					else if(re1.getAlignmentStart() > topPos.getEnd()){
						nextPool.add(re1);
						currentPool.remove(re1);
					}
				}
				
				//merge samrecord
				currentPool.addAll(tmp_current_pool);
		}
	}
	
	
	
	class homopoPileup implements Runnable {
		private final AbstractQueue<IndelPosition> qIn;
		private final AbstractQueue<Homopolymer> qOut;
		private final Thread mainThread;
		private File referenceFile; 
		ReferenceSequence referenceSeq;
		private final byte[] referenceBase;
		private int window; 
		final CountDownLatch pLatch;
		
		homopoPileup(String contig,   AbstractQueue<IndelPosition> qIn, File reference,  
				AbstractQueue<Homopolymer> qOut, int window, Thread mainThread, CountDownLatch latch) throws Exception {
			this.qIn = qIn;
			this.qOut = qOut;
			this.mainThread = mainThread;
			this.referenceFile = reference; 
			this.window = window;
			this.pLatch = latch; 
			
			IndexedFastaSequenceFile indexedFasta = Homopolymer.getIndexedFastaFile(referenceFile);
			this.referenceBase = indexedFasta.getSequence(contig).getBases();
 	
 		}
		 
 
		@Override
		public void run() {
			try {
  				logger.info("seeking homopolymer for indel :" + qIn.size());
				try {
					IndelPosition pos;					
					while ((pos = qIn.poll()) != null)  
						qOut.add(new Homopolymer(pos, referenceBase, window));
				} finally {					 
					pLatch.countDown();
					logger.info("Completed homopolymer threads with indel size :" + qOut.size()   );
				}
			}catch(Exception e){
				logger.error("Exception caught in homopolymer thread", e);
				mainThread.interrupt();

			} finally {
				pLatch.countDown();
//				wBadLatch.countDown();
			}
		}
		
	}
	
	Options options; 
	QLogger logger; 
	ReadIndels indelload;
	
	
	private final int sleepUnit = 10;
	
	private final List<SAMSequenceRecord> sortedContigs = new ArrayList<SAMSequenceRecord>();
	private Map<ChrPosition, IndelPosition> positionRecordMap ;
	
	public IndelMT(File inputVcf, Options options, QLogger logger) throws Exception {		
		this.options = options;	
		this.logger = logger; 

		SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
		
		SAMFileReader TBreader = new SAMFileReader(options.getTumourBam());
		for (final SAMSequenceRecord contig : TBreader.getFileHeader().getSequenceDictionary().getSequences())  
			sortedContigs.add(contig);
		
		this.indelload = new ReadIndels(logger);
		indelload.LoadSingleIndels(inputVcf);	
	}

	public IndelMT(File inputTumourVcf, File inputNormalVcf, Options options, QLogger logger) throws Exception {
		this(inputTumourVcf, options, logger); 
		indelload.appendIndels(inputNormalVcf); 		
	}
	
	public int process(final int threadNo) throws Exception{
		positionRecordMap = indelload.getIndelMap();
		if(positionRecordMap == null || positionRecordMap.size() == 0){
			logger.info("Exit program since there is no indels loaded from inputs");
			return 0; 
		}			
		
        final CountDownLatch pileupLatch = new CountDownLatch(sortedContigs.size() * 2); // filtering thread               
//        final CountDownLatch homopoLatch = new CountDownLatch(threadNo); // homopolymer thread for satisfied records
        
        final AbstractQueue<IndelPileup> tumourQueue = new ConcurrentLinkedQueue<IndelPileup>();
        final AbstractQueue<Homopolymer> homopoQueue = new ConcurrentLinkedQueue<Homopolymer>();
        final AbstractQueue<IndelPileup> normalQueue = new ConcurrentLinkedQueue<IndelPileup>();
        // set up executor services
        ExecutorService pileupThreads = Executors.newFixedThreadPool(threadNo);
//        ExecutorService homopoThreads = Executors.newFixedThreadPool(threadNo);
        
        //homopolymer thread should be before pileupThread
//     	final AbstractQueue<IndelPosition> qIn = getIndelList(null) ; 
//        final AbstractQueue<IndelPosition> qIn = new ConcurrentLinkedQueue<IndelPosition>(); //debug
//    	for (int i = 0; i < threadNo; i++)  
//    		homopoThreads.execute(new homopoPileup(qIn, options.getReference(),homopoQueue, options.nearbyHomopolymer, Thread.currentThread(),homopoLatch));
//    	homopoThreads.shutdown();
    	
    	
    	//each time only throw threadNo thread, the loop finish untill the last threadNo                    	
    	for(SAMSequenceRecord contig : sortedContigs ){       		        	 
    		pileupThreads.execute(new contigPileup(contig, getIndelList(contig), options.getTumourBam(),null ,
    				 tumourQueue, Thread.currentThread() ,pileupLatch));
    		       		
    		pileupThreads.execute(new contigPileup(contig, getIndelList(contig), options.getNormalBam(),null ,
    				normalQueue, Thread.currentThread(),pileupLatch ));
    		
    		pileupThreads.execute(new homopoPileup(contig.getSequenceName(), getIndelList(contig), options.getReference(),
    				homopoQueue, options.nearbyHomopolymer, Thread.currentThread(),pileupLatch));
    		
    	}
    	pileupThreads.shutdown();
    	
		// wait for threads to complete
		try {
			logger.info("waiting for  threads to finish (max wait will be 20 hours)");
			pileupThreads.awaitTermination(20, TimeUnit.HOURS);
	//		homopoThreads.awaitTermination(20, TimeUnit.HOURS);	 
			logger.info("All threads finished");
			
			 writeVCF( tumourQueue, normalQueue, homopoQueue,options.getOutput());			
			
		} catch (Exception e) {
			logger.error("Exception caught whilst waiting for threads to finish: " + e.getMessage(), e);
			throw e;
		} finally {
			// kill off any remaining threads
//        	homopoThreads.shutdownNow();
            pileupThreads.shutdownNow();
		}
        
		return 0; 
	}
	

	private void writeVCF(AbstractQueue<IndelPileup> tumourQueue, AbstractQueue<IndelPileup> normalQueue, AbstractQueue<Homopolymer> homopoQueue, File output ) throws IOException{
		IndelPileup pileup;
		if(positionRecordMap == null ){
			logger.warn("the indel map: positionRecordMap point to nothing");
			return; 		
		}
			
		while((pileup = tumourQueue.poll()) != null ){
			ChrPosition pos = pileup.getChrPosition();
			IndelPosition indel = positionRecordMap.get(pos);
			//debug
			if(indel == null)
				System.out.println("tumour indel is null for " + pos.toIGVString());
			indel.setPileup(true, pileup);			
		}
		while((pileup = normalQueue.poll()) != null ){
			ChrPosition pos = pileup.getChrPosition();
			IndelPosition indel = positionRecordMap.get(pos);
			indel.setPileup(false, pileup);			
		}
		
		Homopolymer homopo;
		while((homopo = homopoQueue.poll()) != null ){
			ChrPosition pos = homopo.getChrPosition();
			IndelPosition indel = positionRecordMap.get(pos);
			indel.setHomopolymer(homopo);
		}
		
		final AbstractQueue<IndelPosition> orderedList = getIndelList(null);
		logger.info("reading indel position:  " + orderedList.size() );
		try(VCFFileWriter writer = new VCFFileWriter( output)) {						 
			long count = 0;
			IndelPosition indel; 
			while( (indel = orderedList.poll()) != null)
				for(int i = 0; i < indel.getMotifs().size(); i++){					
					writer.add( indel.getPileupedVcf(i) );	
					count ++;
				}
						
			logger.info("outputed VCF record:  " + count);	
		}
		
	}
	 
	/**
	 * 
	 * @param contig: contig name or null for whole reference
	 * @return a sorted list of IndelPotion on this contig; return whole reference indels if contig is null
	 */
	private  AbstractQueue<IndelPosition>  getIndelList( SAMSequenceRecord contig ){	  
		if (positionRecordMap == null || positionRecordMap.size() == 0)
			return new ConcurrentLinkedQueue<IndelPosition>(); 			  
		  
		List<IndelPosition> list = new ArrayList<IndelPosition> ();	
		if(contig == null){ //get whole reference
			list.addAll(positionRecordMap.values());	
		}else{	  //get all chrPosition on specified contig	
		for(ChrPosition pos : positionRecordMap.keySet())
			if(pos.getChromosome().equals(contig.getSequenceName()))
				list.add(positionRecordMap.get(pos));	 
		}
  
		final Comparator<String> chrComparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return sortedContigs.indexOf(o1) - sortedContigs.indexOf(o2);
			}
		};
		Collections.sort(list, new Comparator<IndelPosition>() {
			@Override
			public int compare(IndelPosition o1, IndelPosition o2) {
				final int diff = chrComparator.compare(o1.getChrPosition().getChromosome(), o2.getChrPosition().getChromosome());
				if (diff != 0) return diff;
				return o1.getChrPosition().getPosition() - o2.getChrPosition().getPosition();
			}
		});
		
		return new ConcurrentLinkedQueue<IndelPosition>(list);
  }
	 
		
}
