/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;


import org.qcmg.common.log.QLogger;



public class Main {
	
	 public static void main(final String[] args) throws Exception {
        Options option = new Options(args);
        if(option.hasHelp() || option.hasVersion())
        	 System.exit(0); 

        QLogger logger =  option.getLogger(args);	
        try{     	

			MtCounts cnvThread = new MtCounts(option.getInputNames(), option.getSampleIds(), 
					option.getOutputName(), option.getThreadNumber(), option.getWindowSize(), option.getQuery(),  logger);
			logger.info("window size: "+ option.getWindowSize());
			logger.info("query string: "+ option.getQuery());
			
			cnvThread.callCounts();
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



 
