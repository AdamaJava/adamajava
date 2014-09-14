package au.edu.qimr.qannotate;

import java.io.File;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.vcf.VCFFileReader;


public class Main {
	private static QLogger logger = null;
	
	public static void main(final String[] args) throws Exception {		
	       try {
	            Options options = new Options();
	            if ( options.parseArgs(args)){ 
	            	
	            	logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(), options.getLogLevel());
	               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);

	               
	         System.out.println(      options.getMode().name() );
	         
	               if(options.getMode() == Options.MODE.snpEff)
	            	   new SnpEffMode( (SnpEffOptions) options.getOption());
	               else if(options.getMode() == Options.MODE.germline)
	            	   new GermlineMode( (GermlineOptions) options.getOption());
	               else if(options.getMode() == Options.MODE.snpEff){}
	               else
	            	   throw new Exception("No valid mode are specified on commandline: " + options.getMode().name()) ;
	       //        new SnpEffMode(new SnpEffOptions(args));
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
