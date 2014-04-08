/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import org.qcmg.common.log.*;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;

public class MtCounts {
		
	final File Output;
	final File fnormal;
	final File ftumour;
	final int noOfThreads; //default
 	final File tmpdir;
 	final int windowSize;
 	final QLogger logger;

    MtCounts(File fref, File ftest, File output, File tmp, int thread,int windowSize, QLogger logger) throws IOException{
      	this.Output = output;
    	this.fnormal = fref;
    	this.ftumour = ftest; 
    	this.noOfThreads = thread;
    	this.windowSize = windowSize;
    	this.logger = logger;
     	
     	if(tmp == null)    		
    		this.tmpdir = File.createTempFile( "qcnv", "", Output.getParentFile());
    	else 
    		this.tmpdir = File.createTempFile( "qcnv", "",tmp);
     	   	
    	if(!(tmpdir.delete()))
   	        throw new IOException("Could not delete tmp file: " + tmpdir.getAbsolutePath());	    
   	   	if(! tmpdir.mkdirs())
   		   throw new IOException("Could not create tmp directory: " + tmpdir.getAbsolutePath());
    }
     
	/**
	 * it call threads, parallel the BAMFileReader.query for single genome  
	 * @param logger: an instance of QLogger
	 * @throws Exception 
	 */
    void callCounts() throws Exception{
    	SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(fnormal,ValidationStringency.SILENT );			
		List<SAMSequenceRecord> genome = reader.getFileHeader().getSequenceDictionary().getSequences();
		
		final AbstractQueue<ReferenceInfo> infoQueue = new ConcurrentLinkedQueue<ReferenceInfo>();
		
	    ExecutorService queryThreads = Executors.newFixedThreadPool(noOfThreads);	       
	    logger.info("starting parallel counts based on genome file name");     
    	   
	    //parallel query by genomes and output to tmp files 
        int index = 0;
   		for ( SAMSequenceRecord chr : genome){	 	   	    	   
	    	   queryThreads.execute(new SingleCount(fnormal, ftumour,  chr, windowSize, tmpdir, infoQueue));
	    	   index ++;	    	  
	    }	  
   		
       //wait threads finish
   	   logger.info("submited counting threads are " + index);	 
       queryThreads.shutdown();
       queryThreads.awaitTermination(20, TimeUnit.HOURS);
       queryThreads.shutdownNow();
       
       logger.info("completed parallel query based on genome file name");	       

       ReportCounts report = new ReportCounts(infoQueue, logger);
       if(windowSize > 0)
    	    report.windowCountReport(Output, windowSize, genome);
       else
    	   report.baseCountReport(Output);
       
       logger.info("created final output: " + Output.getCanonicalPath());
       
       boolean del = tmpdir.delete();
       logger.info(del + " deleted tmprary directory: " + tmpdir.getCanonicalPath());
 
    }

	
    /**
	 * query on Test BAM and Ref BAM records which mapped to specified gemoem 
	 * @author q.xu
	 *
	 */
	public static class SingleCount implements Runnable {
	 
		File Output;
		File fnormal;
		File ftumour;
		File tmpDir;
		int windowSize;
		SAMSequenceRecord chr;
		AbstractQueue<ReferenceInfo> queue;
		
		/**
		 * 
		 * @param normal: normal BAM
		 * @param tumor: tumour BAM
		 * @param chr: reference record
		 * @param windowSize: fixed window size, set value less than 0 if count each base
		 * @param tmpDir
		 * @param infoQueue
		 * @throws IOException
		 */
		SingleCount(  File normal, File tumor, SAMSequenceRecord chr, int windowSize,File tmpDir, AbstractQueue<ReferenceInfo> infoQueue  ) throws IOException {
			fnormal = normal;
			ftumour = tumor;	 
			this.chr = chr;
			this.windowSize = windowSize;
			queue = infoQueue;
			this.tmpDir = tmpDir;			
		}
	 
		public void run() {
			try {	
								
			    Count myCount;
			    if(windowSize > 0)
			    	myCount= new WindowCount(fnormal, ftumour, chr, windowSize);
			    else
			    	myCount = new BaseCount(fnormal, ftumour, chr, tmpDir);
			    
			    ReferenceInfo info = myCount.execute();				
				queue.add(info );
 		 
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " "
						+ e.getMessage());
				Thread.currentThread().interrupt();
			} 
			
		}		
	}
}
