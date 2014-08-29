package au.edu.qimr.qmito;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.LoadReferencedClasses;

public class Mito {	
	private static QLogger logger;
	
		
	public static void main(String[] args) throws Exception {		
		LoadReferencedClasses.loadClasses(Mito.class);    
       try {
            Options options = new Options(args);
            logger = QLoggerFactory.getLogger(Mito.class, options.getLogFileName(), options.getLogLevel());
					
	 
            if ( options.hasCommandChecked()){    
            	logger.logInitialExecutionStats(options.getQExec());
              // logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args, options.getUuid());
	       	   for (String bamFile: options.getInputFileNames()) 
	       			logger.info("input Bam: "  + bamFile);
               
               logger.tool("output: " +options.getOutputFileName());
               logger.tool("query: " + options.getQuery());	
               logger.tool("reference File: " + options.getReferenceFile());
               logger.tool("reference record name: " + options.getReferenceRecord().getSequenceName());
               logger.tool("Low Read Count: " + options.getLowReadCount());
               logger.tool("NonReference Threshold: " + options.getNonRefThreshold());
               logger.info("logger level " + options.getLogLevel());	
               new MitoPileline(options).report();
	          
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

}

 