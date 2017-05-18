/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfix;

import java.io.File;

import htsjdk.samtools.SAMFileHeader;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
 
public class Main {	
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {
		LoadReferencedClasses.loadClasses(Main.class);
		
		NewOptions options = new NewOptions(args);			
		if( options.hasHelp() || options.hasVersion() )
				 System.exit(0); 

		try{
			logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(), options.getLogLevel());		
            logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);
            
            logger.tool("input: " + options.getInputFileName());
            logger.tool("output: " + options.getOutputFileName());
            logger.info("log file: " + options.getLogFileName());
            
            if(options.isFinalBAM()){ 
	            	SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(options.getInputFileName()),
	            			options.getValidation() ).getFileHeader().clone(); 
	            	HeaderUtils.addProgramRecord(header, options.getPGName(), options.getVersion(), Messages.reconstructCommandLine(args));
	
	            new ReheadFinalBAM(header, options, logger);
	            	
            }else{
	            	FixHeader header=new FixHeader(options, logger);
	            	HeaderUtils.addProgramRecord(header.getHeader(), options.getPGName(), options.getVersion(), Messages.reconstructCommandLine(args));
	            	new FixBAM(header.getHeader(), options.getInputFileName(), 
						options.getOutputFileName(),  options.getTmpDir(),logger, options.getSeqLength(), options.getValidation());
            }
			logger.logFinalExecutionStats(0);   		
		} catch (Exception e) {
			e.printStackTrace();
	    	System.err.println(Thread.currentThread().getName() + " " + e.toString());
		    if (null != logger) {
		    	logger.error("Exception caught in Main class of qbamfix", e);	            
		    	logger.logFinalExecutionStats(1);
		    	}
		    System.exit(1);
		}				
	}
}
		
