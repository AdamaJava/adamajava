/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.*;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;

public class MtCounts {
		
	final File Output;
	final String[] inputs;
	final String[] ids;
	final int noOfThreads; //default
	final String query; 
 
    final int windowSize;
 	final QLogger logger;

    MtCounts(String[] inputs, String[] ids, String output,  int thread,int windowSize, String query, QLogger logger) throws Exception{
      	this.Output = new File(output); 
    	this.noOfThreads = thread;
    	this.windowSize = windowSize;
    	this.logger = logger;
    	this.ids = ids;
    	this.query = query; 
    	
    	
    	if(inputs.length != ids.length )
    		throw new Exception("incorrect numbers of sample id or input files: " +
    	 inputs.length + " inputs but id number is " + ids.length);
    	
    	this.inputs = inputs;
    	
    }  	     	   	    
	/**
	 * it call threads, parallel the BAMFileReader.query for single genome  
	 * @throws Exception 
	 */
    void callCounts() throws Exception {
    		List<SAMSequenceRecord> genome;
    		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(inputs[0]), null, ValidationStringency.SILENT )) {			
			genome = reader.getFileHeader().getSequenceDictionary().getSequences();
    		}
		
		final AbstractQueue<ReferenceInfo> infoQueue = new ConcurrentLinkedQueue<>();
		
	    ExecutorService queryThreads = Executors.newFixedThreadPool(noOfThreads);	       
	    logger.info("starting parallel counts based on genome file name");     
    	   
	    //parallel query by genomes and output to tmp files 
        int index = 0;
   		for ( SAMSequenceRecord chr : genome){	 	   	    	   
	    	   queryThreads.execute(new WindowCount(inputs, ids, chr, infoQueue,windowSize, query));
	    	   index ++;	    	  
	    }	  
   		
       //wait threads finish
   	   logger.info("submited counting threads are " + index);	 
       queryThreads.shutdown();
       queryThreads.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
       queryThreads.shutdownNow();
       
       logger.info("completed parallel query based on genome file name");	       

       ReportCounts report = new ReportCounts(infoQueue);
    
       report.windowCountReport(Output);
      
       logger.info("created final output: " + Output.getCanonicalPath());
 
    }

	
    /**
	 * query on Test BAM and Ref BAM records which mapped to specified gemoem 
	 * @author q.xu
	 *
	 */
	public static class WindowCount implements Runnable {
	 
		File Output;
		String[] inputs;
		String[] ids;
		int windowSize;
		String query;
		SAMSequenceRecord chr;
		AbstractQueue<ReferenceInfo> queue;
		
		/**
		 * 
		 * @param windowSize: fixed window size, set value less than 0 if count each base
		 * @param infoQueue
		 */
		WindowCount(  String[] inputs, String[] ids, SAMSequenceRecord chr, AbstractQueue<ReferenceInfo> infoQueue, int windowSize, String query  ) {

			this.inputs = inputs;
			this.ids = ids; 
			this.chr = chr;
			this.windowSize = windowSize;
			queue = infoQueue;
			this.query = query;
		}
	 
		@Override
		public void run() {
			try {	
				
				ReferenceInfo info = new ReferenceInfo(chr, windowSize);				 
				for(int i = 0; i <inputs.length; i++){
					info.addCounts(ids[i], inputs[i],query);
				}
			   			
				queue.add(info );
 		 
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " "
						+ e.getMessage());
				Thread.currentThread().interrupt();
			} 
			
		}		
	}
}
