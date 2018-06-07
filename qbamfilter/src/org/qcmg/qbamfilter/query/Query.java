/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.query;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

public class Query {
	private static QLogger logger;
	
	/**
	 * This construction is the entry of main filter 
	 * @param args
	 */
	public Query(String[] args){
	       try {
	            Options options = new Options(args);
	            logger = QLoggerFactory.getLogger(Query.class, options.getLogFileName(), options.getLogLevel());
	            
	            if ( options.hasCommandChecked()){               
	               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);
	               logger.tool("query: " + options.getQuery());
	               logger.tool("input: " +options.getInputFileName());
	               logger.tool("output for matched records: " +options.getOutputFileName());
	               if(options.getFiltedFileName() != null){
	            	   logger.tool("output for unmatched records: " + options.getFiltedFileName());
	               }  

	               logger.info("logger level " + options.getLogLevel());
	               logger.info("queue size  " + options.getMaxRecordNumber());
	               logger.info("check point " + options.getCheckPoint());
	               logger.info("filtering threads number  " + options.getThreadNumber());
	               logger.info("sort order  " + options.getSortOrder());	
	               

	               
	               QueryMT multiQuery = new QueryMT(options, logger);
	               multiQuery.executor();	               
	                
	               logger.logFinalExecutionStats(0);
	            }
	        }catch (Exception e) {
	        	e.printStackTrace();
	        	System.err.println(Thread.currentThread().getName() + " " + e.toString());
	        if (null != logger) {
	        	logger.info(Thread.currentThread().getName() + " " + e.toString());	            
	            logger.logFinalExecutionStats(1);
	        	}
	            System.exit(1);
	        }				
	}
	
	public static void main(final String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Query.class);
		new Query(args);
	}
	
	
}
