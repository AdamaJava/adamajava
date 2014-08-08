package au.edu.qimr.qmito;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.pileup.QPileup;



public class Mito {
	
	private static QLogger logger;
	
	
	public static void main(String[] args) throws Exception {
		
	       try {
	            Options options = new Options(args);
	            logger = QLoggerFactory.getLogger(Mito.class, options.getLogFileName(), options.getLogLevel());
	            
	            if ( options.hasCommandChecked()){    
					LoadReferencedClasses.loadClasses(QPileup.class);
          	
	               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);
	               logger.tool("query: " + options.getQuery());
	               logger.tool("input: " +options.getInputFileName());
	               logger.tool("output: " +options.getOutputFileName());	
	               logger.info("logger level " + options.getLogLevel());	               
	               
	//               QueryMT multiQuery = new QueryMT(options, logger);
              
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
