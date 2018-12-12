package au.edu.qimr.indel.pileup;


import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.*;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QBamIdFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.indel.Options;
import au.edu.qimr.indel.pileup.IndelPileup;
import au.edu.qimr.indel.pileup.IndelPosition;
import au.edu.qimr.indel.pileup.ReadIndels;


public class IndelMT {
	public static final int  MAXRAMREADS = 1500; //maximum number of total reads in RAM
	
	class ContigPileup implements Runnable {

		private final AbstractQueue<IndelPosition> qIn;
		private final AbstractQueue<IndelPileup> qOut;
		private final Thread mainThread;
		final CountDownLatch pLatch;
		private SAMSequenceRecord contig;
		private File bam;
		private File index;
		private QueryExecutor exec;
		
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
		ContigPileup(SAMSequenceRecord contig,  AbstractQueue<IndelPosition> qIn, File bam, File index, QueryExecutor exec,
				AbstractQueue<IndelPileup> qOut, Thread mainThread, CountDownLatch latch)  {
			this.qIn = qIn;
			this.qOut = qOut;
			this.mainThread = mainThread;
			this.pLatch = latch;
			this.bam = bam;
			this.index = index;
			this.contig = contig;
			this.exec = exec; 
 		}

		@Override
		public void run() {
		 	List<SAMRecord> current_pool = new ArrayList<>();
		 	List<SAMRecord> next_pool = new ArrayList<>(); 
		 	int size = qIn.size();	
			if (size <= 0) {
		 		logger.debug("There is no indel in contig: " + contig.getSequenceName() );		 		
		 		return;
		 	}
			
			IndelPosition topPos= qIn.poll();
			try (SamReader bReader =  SAMFileReaderFactory.createSAMFileReader(bam, index); ){
				SAMRecordIterator ite = bReader.query(contig.getSequenceName(), 0, contig.getSequenceLength(),false);		
			 	while (ite.hasNext()) {	
			 		SAMRecord re = ite.next(); 
			 		//bam file already sorted, skip non-indel region record
			 		if(re.getAlignmentEnd() < topPos.getStart()) continue; 
			 		
			 		//only interested pass filter record
			 		boolean passFilter = exec != null ? exec.Execute(re) : ( ! re.getReadUnmappedFlag() && ! re.getDuplicateReadFlag());
			 		
			 	 	if ( ! passFilter ) continue;
			 			 		
			 		//whether in current indel region
			 		if (re.getAlignmentStart() <= topPos.getEnd() && current_pool.size() < MAXRAMREADS ) {
			 			current_pool.add(re);
			 		} else {
			 			next_pool.add(re); 
			 			//pileup
			 			IndelPileup pileup= new IndelPileup(topPos, options.getSoftClipWindow(), options.getNearbyIndelWindow(), options.getMaxEventofStrongSupport());
			 			pileup.pileup(current_pool);
			 			qOut.add(pileup);
			 			
			 			//prepare for next indel position
			 			if( (topPos = qIn.poll()) == null) break; 
			 			
			 			resetPool(topPos,  current_pool, next_pool); 	 
			 		}
			 	}	 	
			
			 	//after loop check all pool
			 	do{			
			 		//check whether previous loop also used up all indel position
			 		if(topPos == null) break; 			 			
		 			IndelPileup pileup= new IndelPileup(topPos, options.getSoftClipWindow(), options.getNearbyIndelWindow(),options.getMaxEventofStrongSupport());
		 			pileup.pileup(current_pool);
		 			qOut.add(pileup);					
					if( (topPos = qIn.poll()) == null) break; 
					resetPool(topPos,  current_pool, next_pool); 							
			 	}while( true ) ;					 					 
			} catch (Exception e) {
				e.printStackTrace();
//				logger.error("Exception caught in pileup thread", e);
				mainThread.interrupt();
			} finally {
				pLatch.countDown();
				logger.info( size + " indels have completed pileup from " + contig.getSequenceName() + " on " + bam.getName());

 			}			
		}		
		/**
		 * it swap SAMRecord between currentPool and nextPool. After then, the currentPool will contain all SAMRecord overlapping topPos position, 
		 * the nextPool will contain all SAMRecord start after topPos position.  All SAMRecord end before topPos position will be remvoved from both pool. 
		 * @param topPos:   pileup position
		 * @param currentPool: a list of SAMRecord overlapped previous pileup Position
		 * @param nextPool: a list of SAMRecord behind previous pileup Position
		 */
		 void resetPool( IndelPosition topPos, List<SAMRecord> currentPool, List<SAMRecord> nextPool){
			
			
				List<SAMRecord> tmp_current_pool = new ArrayList<>();							
				List<SAMRecord> tmp_pool = new ArrayList<>();	
				tmp_pool.addAll(nextPool);
				
				//check read record behind on current position			
				for( SAMRecord  re : tmp_pool ){
					//aligned position before indel
					if (re.getAlignmentEnd() < topPos.getStart()) {
						nextPool.remove(re);
					//aligned position cross indel
					} else if (re.getAlignmentStart() <= topPos.getEnd()){		 
						tmp_current_pool.add(re);	
						nextPool.remove(re);
					}			 
				}	
				
				tmp_pool.clear();
				tmp_pool.addAll(currentPool);
				//check already read record  for previous pileup
				for( SAMRecord  re1 : tmp_pool ){
					//aligned position before indel
					if (re1.getAlignmentEnd() < topPos.getStart()) {
						currentPool.remove(re1);
					//aligned position after indel
					} else if (re1.getAlignmentStart() > topPos.getEnd()){
						nextPool.add(re1);
						currentPool.remove(re1);
					}
				}
				
				//merge samrecord
				currentPool.addAll(tmp_current_pool);
		}				
	}
			
	Options options; 
	QLogger logger; 
	ReadIndels indelload;
		
	private final List<SAMSequenceRecord> sortedContigs = new ArrayList<SAMSequenceRecord>();
	private Map<ChrRangePosition, IndelPosition> positionRecordMap ;
	
	//unit test purpose
	@Deprecated
	IndelMT(){}
		
	public IndelMT(Options options, QLogger logger) throws IOException  {		
		this.options = options;	
		this.logger = logger; 
		
		//get sequence from bam header
		File bam = (options.getTestBam() != null)? options.getTestBam(): options.getControlBam();
		try (SamReader reader =  SAMFileReaderFactory.createSAMFileReader(bam)) {				 
			for (final SAMSequenceRecord contig : reader.getFileHeader().getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig);
			}
		}
		
		//loading indels 
		this.indelload = new ReadIndels(logger);		
		if(options.getRunMode().equalsIgnoreCase(Options.RUNMODE_GATK) ){
			//first load control
			if(options.getControlInputVcf() != null){
				indelload.loadIndels(options.getControlInputVcf(),options.getRunMode());	
				if(indelload.getCounts_newIndel() != indelload.getCounts_totalIndel())
					logger.warn("ERROR: Found " + indelload.getCounts_newIndel() + 
							" indels from control input, but it is not the same as the number of indel inside MAP, which is " + indelload.getCounts_totalIndel());
				logger.info(indelload.getCounts_newIndel() + " indels are found from control vcf input.");
				logger.info(indelload.getCounts_multiIndel() + " indels are split from multi alleles in control vcf.");
				logger.info(indelload.getCounts_inputLine() + " variant records exsit inside control vcf input.");
				logger.info(indelload.getCounts_inputMultiAlt() + " variant records with multi alleles exsits inside control vcf input.");									
			}	
			//then test second column
			if(options.getTestInputVcf() != null){
				indelload.appendTestIndels(options.getTestInputVcf());				
				logger.info(indelload.getCounts_inputLine() + " variant records exsit inside test vcf input.");
				logger.info(indelload.getCounts_inputMultiAlt() + " variants record with multi alleles exsits inside test vcf input.");	
				logger.info(indelload.getCounts_multiIndel() + " indels are split from multi alleles inside test vcf");					
				logger.info(indelload.getCounts_newIndel() + " new indels are found in test vcf input only.");
				logger.info(indelload.getCounts_overlapIndel() + " indels are found in both control and test vcf inputs.");
				logger.info((indelload.getCounts_totalIndel() - indelload.getCounts_newIndel() - indelload.getCounts_overlapIndel()) +  
						" indels are found in control vcf input only." );				
			}				
		}else if(options.getRunMode().equalsIgnoreCase("pindel")){	
			for(int i = 0; i < options.getInputVcfs().size(); i ++)
				indelload.loadIndels(options.getInputVcfs().get(i), options.getRunMode());	
		}		
	}
	
	/**
	 * run parallel pileup without homopolymers pileup if the withHomoOption is false
	 * @param threadNo
	 * @param withHomoOption, if true, it will run homopolymers pileup
	 * @return
	 * @throws Exception
	 */
	public int process(final int threadNo) throws Exception {
		positionRecordMap = indelload.getIndelMap();
		if(positionRecordMap == null || positionRecordMap.isEmpty()){
			logger.info("Exit program since there is no indels loaded from inputs");
			return 0; 
		}
		
        final CountDownLatch pileupLatch = new CountDownLatch(sortedContigs.size() * 2); // filtering thread               
        
        final AbstractQueue<IndelPileup> tumourQueue = new ConcurrentLinkedQueue<>();
        final AbstractQueue<IndelPileup> normalQueue = new ConcurrentLinkedQueue<>();
        // set up executor services
        ExecutorService pileupThreads = Executors.newFixedThreadPool(threadNo);
        
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {logger.error("( in uncaughtExceptionHandler) )exception " + e + ", from thread: " + t, e); System.exit(1);});
    	
        QueryExecutor query = null; 
        if (options.getFilterQuery() != null) {
        		query = new QueryExecutor(options.getFilterQuery());
        }
        
     	//each time only throw threadNo thread, the loop finish until the last threadNo                    	
	    	for (SAMSequenceRecord contig : sortedContigs ){       		
	    		if (options.getControlBam() != null) {
	    			/*
	    			 * check to see if we have abm index files available, bomb i we dont
	    			 */
	    			File index = SamFiles.findIndex(options.getControlBam());
	    			if (null == index || ! index.exists()) {
	    				logger.error("No index file found for control bam: " + options.getControlBam());
	    				return 1;
	    			}
	    			 pileupThreads.execute(new ContigPileup(contig, getIndelList(contig), options.getControlBam(), index, query,
	    				normalQueue, Thread.currentThread(),pileupLatch ));
	    		}
	    		
	    		//getIndelList must be called repeatedly, since it will be empty after pileup
	    		 if (options.getTestBam() != null) {
	    			 /*
    				 * check to see if we have abm index files available, bomb i we dont
    				 */
	    			 File index = SamFiles.findIndex(options.getTestBam());
		    			if (null == index || ! index.exists()) {
		    				logger.error("No index file found for test bam: " + options.getTestBam());
		    				return 1;
		    			}
	    			 pileupThreads.execute(new ContigPileup(contig, getIndelList(contig), options.getTestBam() , index, query,
	    					 tumourQueue, Thread.currentThread() ,pileupLatch));
	    		 }
	    		
	    	}
	    	pileupThreads.shutdown();
    	
		// wait for threads to complete
		try {
			logger.info("waiting for  threads to finish (max wait will be 20 hours)");
			pileupThreads.awaitTermination(20, TimeUnit.HOURS);
			logger.info("All threads finished");
			
			writeVCF( tumourQueue, normalQueue,options.getOutput(),indelload.getVcfHeader());		
			
		} catch (Exception e) {
			logger.error("Exception caught whilst waiting for threads to finish: " + e.getMessage(), e);
			throw e;
		} finally {
            pileupThreads.shutdownNow();
		}
        
		return 0; 
	}
	
	
	private void writeVCF(AbstractQueue<IndelPileup> tumourQueue, AbstractQueue<IndelPileup> normalQueue,File output, VcfHeader header ) throws Exception{
		
		IndelPileup pileup;
		if(positionRecordMap == null ){
			logger.warn("the indel map: positionRecordMap point to nothing");
			return; 		
		}

		while((pileup = tumourQueue.poll()) != null ){
			ChrRangePosition pos = pileup.getChrRangePosition();
			IndelPosition indel = positionRecordMap.get(pos);
			if (null != indel) {
				indel.setPileup(true, pileup);
			}
		}
		while((pileup = normalQueue.poll()) != null ){
			ChrRangePosition pos = pileup.getChrRangePosition();
			IndelPosition indel = positionRecordMap.get(pos);
			if (null != indel) {
				indel.setPileup(false, pileup);
			}
		}
				
		final AbstractQueue<IndelPosition> orderedList = getIndelList(null);
		logger.info("reading indel position:  " + orderedList.size() );
		try(VCFFileWriter writer = new VCFFileWriter( output)) {	
						
			//reheader
			getHeaderForIndel(header);	
	        	for (final VcfHeaderRecord record: header) {
	        		writer.addHeader(record.toString());
	        	}
			 
        	//adding indels
			long count = 0;
			IndelPosition indel; 
			long somaticCount = 0;
			while( (indel = orderedList.poll()) != null) {
				for(int i = 0; i < indel.getMotifs().size(); i++){

					VcfRecord re = indel.getPileupedVcf(i, options.getMinGematicNovelStart(), options.getMinGematicSupportOfInformative());
					writer.add(re  );	
					count ++;
					
					if (re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)) {
						somaticCount ++;
					}
				}
			}
						
			logger.info("outputed VCF record: " + count);	
			logger.info("including somatic record: " + somaticCount);
		}
		
	}
	
	 private void getHeaderForIndel(VcfHeader header ) throws Exception {

		QExec qexec = options.getQExec();
		 
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
 		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + qexec.getUuid().getValue());
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + qexec.getToolName().getValue() + " v" + qexec.getToolVersion().getValue());
		
		header.addOrReplace(VcfHeaderUtils.STANDARD_DONOR_ID + "=" + options.getDonorId());
		header.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=" + options.getControlSample());		
		header.addOrReplace(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=" + options.getTestSample());		
		
//		List<File> inputs = new ArrayList<File>();
		if(options.getRunMode().equalsIgnoreCase("gatk")){
			header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_TEST=" + 
					(options.getTestInputVcf() == null? null : options.getTestInputVcf().getAbsolutePath()));
			header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_CONTROL=" + 
					(options.getControlInputVcf() == null? null : options.getControlInputVcf().getAbsolutePath()));
 		} else {
	 		for(int i = 0; i < options.getInputVcfs().size(); i ++) {
	 			header.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + options.getInputVcfs().get(i).getAbsolutePath());
	 		}
 		}
		
		String controlBamID = null; 
		if( options.getControlBam() != null ){
			String normalBamName = options.getControlBam().getAbsolutePath();
			controlBamID = QBamIdFactory.getQ3BamId(normalBamName).getUUID();
			if(controlBamID == null ) controlBamID = new File(normalBamName).getName().replaceAll("(?i).bam$", "");
			header.addOrReplace( VcfHeaderUtils.STANDARD_CONTROL_BAM  + "=" + normalBamName );
			header.addOrReplace( VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=" +  controlBamID );
			 
		}
		
	 
		
		String testBamID = null; 
		if( options.getTestBam() != null ){
			String tumourBamName = options.getTestBam().getAbsolutePath();
			testBamID = QBamIdFactory.getQ3BamId(tumourBamName).getUUID();
			if(testBamID == null ) testBamID = new File(tumourBamName).getName().replaceAll("(?i).bam$", "");
			
			header.addOrReplace( VcfHeaderUtils.STANDARD_TEST_BAM  + "=" + tumourBamName);
			header.addOrReplace( VcfHeaderUtils.STANDARD_TEST_BAMID  + "=" + ( testBamID  == null ? new File(tumourBamName).getName() : testBamID ));
		}	
		header.addOrReplace( VcfHeaderUtils.STANDARD_ANALYSIS_ID +"=" + options.getAnalysisId() );
	
		
		//add filter
        header.addFilter(IndelUtils.FILTER_COVN12, IndelUtils.DESCRITPION_FILTER_COVN12 );
        header.addFilter(IndelUtils.FILTER_COVN8,  IndelUtils.DESCRITPION_FILTER_COVN8 );
        header.addFilter(IndelUtils.FILTER_COVT,  IndelUtils.DESCRITPION_FILTER_COVT );
        header.addFilter(IndelUtils.FILTER_HCOVN,  IndelUtils.DESCRITPION_FILTER_HCOVN );
        header.addFilter(IndelUtils.FILTER_HCOVT,  IndelUtils.DESCRITPION_FILTER_HCOVT );
        header.addFilter(IndelUtils.FILTER_MIN,  IndelUtils.DESCRITPION_FILTER_MIN );
        header.addFilter(IndelUtils.FILTER_NNS,  IndelUtils.DESCRITPION_FILTER_NNS );
        header.addFilter(IndelUtils.FILTER_TPART,  IndelUtils.DESCRITPION_FILTER_TPART );
        header.addFilter(IndelUtils.FILTER_NPART,  IndelUtils.DESCRITPION_FILTER_NPART );
        header.addFilter(IndelUtils.FILTER_TBIAS,  IndelUtils.DESCRITPION_FILTER_TBIAS );
        header.addFilter(IndelUtils.FILTER_NBIAS,  IndelUtils.DESCRITPION_FILTER_NBIAS );
        
		final String SOMATIC_DESCRIPTION = String.format("There are more than %d novel starts  or "
				+ "more than %.2f soi (number of supporting informative reads /number of informative reads) on control BAM",
				options.getMinGematicNovelStart(), options.getMinGematicSupportOfInformative());

		header.addInfo(VcfHeaderUtils.INFO_SOMATIC, "1", "String", SOMATIC_DESCRIPTION);
		header.addInfo(IndelUtils.INFO_NIOC, "1", "String", IndelUtils.DESCRITPION_INFO_NIOC);
		header.addInfo(IndelUtils.INFO_SSOI, "1", "String", IndelUtils.DESCRITPION_INFO_SSOI);		
		header.addInfo(VcfHeaderUtils.INFO_MERGE_IN, "1", "String",VcfHeaderUtils.DESCRITPION_MERGE_IN); 
				
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, "1","String", "Genotype details: specific alleles");
		header.addFormat(IndelUtils.FORMAT_ACINDEL, ".", "String", IndelUtils.DESCRITPION_FORMAT_ACINDEL); //vcf validataion

		VcfHeaderUtils.addQPGLineToHeader(header, qexec.getToolName().getValue(), qexec.getToolVersion().getValue(), qexec.getCommandLine().getValue() 
				+  " [runMode: " + options.getRunMode() + "]");        
        		
		VcfHeaderUtils.addSampleId(header, controlBamID, 1 ); // "qControlSample", 1);
		VcfHeaderUtils.addSampleId(header,  testBamID, 2);//"qTestSample" 		 
	}
	 
	 /**
	  * 
	  * @param contig: contig name or null for whole reference
	  * @param filter: only return indel vcf records with specified filter value. Put null here if ignor record filter column value
	  * @return a sorted list of IndelPotion on this contig; return whole reference indels if contig is null
	  */
	 private  AbstractQueue<IndelPosition>  getIndelList( SAMSequenceRecord contig){	  
		if (positionRecordMap == null || positionRecordMap.size() == 0)
			return new ConcurrentLinkedQueue<>(); 			  
		  
		List<IndelPosition> list = new ArrayList<> ();
		for(Entry<ChrRangePosition, IndelPosition> entry : positionRecordMap.entrySet()){
			if(contig != null && ! entry.getKey().getChromosome().equals(contig.getSequenceName())  )
				continue; 
			list.add(entry.getValue());	 
		}
		
		//lambda expression to replace abstract method
		list.sort(  (IndelPosition o1, IndelPosition o2) ->
			o1.getChrRangePosition().compareTo( o2.getChrRangePosition()) );				 
		
		return new ConcurrentLinkedQueue<>(list);
  }
	 		
}
