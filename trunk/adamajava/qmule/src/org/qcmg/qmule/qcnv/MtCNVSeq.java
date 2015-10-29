/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.qcnv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.*;
import org.qcmg.picard.SAMFileReaderFactory;


public class MtCNVSeq {
	
	final CNVseq cnvseq;
	final File Output;
	final int noOfThreads;
	final File tmpPath;
	
    MtCNVSeq(CNVseq cnvseq, File output, int noOfThreads, File tmpdir) throws IOException{
    	this.cnvseq = cnvseq;
    	this.Output = output;
    	this.noOfThreads = noOfThreads;
    	if(tmpdir == null)
    		tmpPath = File.createTempFile( "qcnv", "", Output.getParentFile());
    	else 
    		tmpPath = File.createTempFile( "qcnv", "",tmpdir);
    }
	/**
	 * it call threads, parallel the BAMFileReader.query for single genome  
	 * @param logger: an instance of QLogger
	 * @throws IOException
	 * @throws InterruptedException
	 */
    void cnvCount(QLogger logger) throws IOException, InterruptedException{

	       Map<String, Integer> refseq = cnvseq.getrefseq();
	       Map<String, File> tmpoutput = new HashMap<String, File>();	       
	       ExecutorService queryThreads = Executors.newFixedThreadPool(noOfThreads);
	       
	       logger.debug("start parallel query based on genome file name");     
       	   
    	  
    	   if(!(tmpPath.delete()))
    	        throw new IOException("Could not delete tmp file: " + tmpPath.getAbsolutePath());	    
    	   if(! tmpPath.mkdirs())
    		   throw new IOException("Could not create tmp directory: " + tmpPath.getAbsolutePath());
    	   
	       //parallel query by genomes and output to tmp files 
	       for ( Map.Entry<String, Integer> chr : refseq.entrySet()){	   
	    	   File tmp = File.createTempFile(chr.getKey(), ".count", tmpPath);	    	   
	    	   tmpoutput.put(chr.getKey(), tmp);	    	   
	    	   queryThreads.execute(new ExeQuery(cnvseq,chr, tmp));
	       }	  
	       //wait threads finish
	       queryThreads.shutdown();
	       queryThreads.awaitTermination(20, TimeUnit.HOURS);
	       queryThreads.shutdownNow();
	       logger.debug("completed parallel query based on genome file name");
	       
	       
	       //collect outputs from tmp files into 
	       logger.debug("starting collect each genome counts into final output");
	       FileWriter writer = new FileWriter(Output);	       
	       writer.write("reference\tstart\tend\ttest\tref\n");	 
	       for( Map.Entry<String, File> tmp : tmpoutput.entrySet()){
	    	   BufferedReader input =  new BufferedReader(new FileReader(tmp.getValue()));
	    	   String line = null;
	    	   while((line = input.readLine()) != null){
	    		   writer.write(line + "\n");
	    	   }
	    	   input.close();
	    	   tmp.getValue().deleteOnExit();
	       }	 
	       tmpPath.delete();
	       writer.close();
	       logger.debug("created final output");
    }
	
    /**
	 * query on Test BAM and Ref BAM records which mapped to specified gemoem 
	 * @author q.xu
	 *
	 */
	public static class ExeQuery implements Runnable {
		CNVseq cnvseq;
		File Output;
		File Test;
		File Ref;
		QLogger logger;
		int chrSize;
		int winSize;
		String chrName;		
		
		ExeQuery(CNVseq cnvseq, Map.Entry<String, Integer> chr,File tmp) {
			Output = tmp;
			Test = cnvseq.getTestFile();
			Ref = cnvseq.getRefFile();
			chrSize = chr.getValue();
			chrName = chr.getKey();
			winSize = cnvseq.getWindowSize();		
			this.cnvseq = cnvseq;
		}
	 
		public void run() {
			try {				 
				FileWriter writer = new FileWriter(Output);
				SamReader rTest = SAMFileReaderFactory.createSAMFileReader(Test,ValidationStringency.SILENT);						   
				SamReader rRef = SAMFileReaderFactory.createSAMFileReader(Ref,ValidationStringency.SILENT); 
								
				int win_num = chrSize / winSize + 1;
 
				for (int i = 0; i < win_num; i++){			
						int start = i * winSize + 1;
		    		  	int end = (i + 1 ) * winSize;			   		  			  		  	
		    		  	int num_test = cnvseq.exeQuery(rTest, chrName, start, end);
		    		  	int num_ref =  cnvseq.exeQuery(rRef, chrName, start, end);		  	    		   
		    		  	writer.write(String.format("%s\t%d\t%d\t%d\t%d\n", chrName, start, end, num_test, num_ref ));	  		  
		    	  } 	
				
				rRef.close();
				writer.close();
				rTest.close();
			 
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " "
						+ e.getMessage());
				Thread.currentThread().interrupt();
			} 
			
		}		
	}

}
