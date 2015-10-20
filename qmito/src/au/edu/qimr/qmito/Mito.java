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
            if(options.hasHelpOption()) return;
            if(options.hasVersionOption()) return;
            if(options.getMode() == null) return;
            
            logger = QLoggerFactory.getLogger(Mito.class, options.getLogFileName(), options.getLogLevel());
            if ( options.getMode().equals(Options.MetricMode)){   	
            	MetricOptions opt = options.getMetricOption();
                logger.logInitialExecutionStats(opt.getQExec());
 	       	    for (String bamFile: opt.getInputFileNames()) 
	       			logger.info("input Bam: "  + bamFile);
               
                logger.tool("output: " +opt.getOutputFileName());
                logger.tool("query: " + opt.getQuery());	
                logger.tool("reference File: " + opt.getReferenceFile());
                logger.tool("reference record name: " + opt.getReferenceRecord().getSequenceName());
                logger.tool("Low Read Count: " + opt.getLowReadCount());
                logger.tool("NonReference Threshold: " + opt.getNonRefThreshold());
                logger.info("logger level " + opt.getLogLevel());	
                new MetricPileline(opt).report();	                
            }else if( options.getMode().equals(Options.StatMode)){
				 StatOptions opt = options.getStatOption();
				 logger.logInitialExecutionStats(opt.getQExec());
				 logger.tool("output: " +opt.getOutputFileName());
				 logger.tool("input of control metric: " + opt.getControlMetricFileName());	
				 logger.tool("input of test metric: " + opt.getTestMetricFileName());
				 logger.info("logger level " + opt.getLogLevel());	
				 new StatPileline(opt).report();	                

            }else
            	throw new Exception("Invalid mode:" + options.getMode());
            
            logger.logFinalExecutionStats(0);
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

 
