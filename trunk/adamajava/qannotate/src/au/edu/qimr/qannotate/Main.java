package au.edu.qimr.qannotate;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
 

public class Main {
	private static QLogger logger = null;
	
	public static void main(final String[] args) throws Exception {		
	       try {
	            Options options = new Options();
	            
	            if ( options.parseArgs(args)){ 	            	
	               logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(), options.getLogLevel());
	               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);	                
	                 
	               if(options.getOption().getMode() == Options.MODE.dbSNP)
	            	   new DbsnpMode( (DbsnpOptions) options.getOption()   );
	               else if(options.getOption().getMode() == Options.MODE.germline)
	            	   new GermlineMode( (GermlineOptions) options.getOption());
	               else if(options.getOption().getMode() == Options.MODE.snpEff)
	            	    new SnpEffMode( (SnpEffOptions) options.getOption());
	               else
	            	   throw new Exception("No valid mode are specified on commandline: " + options.getMode().name()) ;

 	               logger.logFinalExecutionStats(0);
	            }
	        }catch (final Exception e) {
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
