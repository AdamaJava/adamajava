/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.qcnv;

import org.qcmg.common.log.*;
import htsjdk.samtools.*;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;

public class Main {

	public static void main(String[] args) throws Exception{
		//check arguments
		Options options = new Options( args);	
		if(! options.commandCheck()){ 	System.exit(1);	}		
		
		QLogger logger =  options.getLogger(args);		
		try{     	
			File ftest = new File(options.getIO("test"));
			File fref = new File(options.getIO("ref"));	  
			CNVseq cnvseq = new CNVseq(ftest, fref, options.getWindowSize());
			
			logger.info("genome size used for calculation is " + cnvseq.getGenomeSize());
			logger.info(ftest.getName() + "contains records number: " + cnvseq.getTestReadsNumber());			 
			logger.info(fref.getName() + "contains records number: " + cnvseq.getRefReadsNumber());		
			if(options.getWindowSize() == 0){
				logger.info("The minimum window size for detecting log2>=" + CNVseq.log2 +" should be " + cnvseq.getpositivelog2window()); 
				logger.info("The minimum window size for detecting log2<=-" + CNVseq.log2 +" should be " + cnvseq.getnegativelog2window()); 
				logger.info(String.format("The window size to use is max(%f, %f) * %f = %d", 
						cnvseq.getpositivelog2window(),cnvseq.getnegativelog2window(), CNVseq.bigger, cnvseq.getWindowSize()));
			}else{
				logger.info("The window size used in this run is " + options.getWindowSize());
			}
			
			//count reads number in each window and output 
			MtCNVSeq cnvThread = new MtCNVSeq(cnvseq, new File(options.getIO("output")), options.getThreadNumber(), options.getTmpDir());
			cnvThread.cnvCount(logger);

        	logger.logFinalExecutionStats(0);
        	System.exit(0);
        }catch(Exception e){ 
        	logger.error(e.toString());
        	logger.logFinalExecutionStats(1);
        	System.err.println(e.toString());       	 
        	System.exit(1);
        }		
	}
	

	
 
}
