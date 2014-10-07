package au.edu.qimr.qannotate;

import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import java.util.Date;

public class Main {
	 
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {	

 		
		//QLogger logger = null;
	       try {
	            Options options = new Options();
	            
	            if ( options.parseArgs(args)){ 	    
           	
	               logger = QLoggerFactory.getLogger(Main.class, options.getOption().getLogFileName(),  options.getOption().getLogLevel());	            		               
	               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);	        
 	               
	               if(options.getOption().getMode() == Options.MODE.dbSNP)
	            	   new DbsnpMode( (DbsnpOptions) options.getOption() , logger  );
	               else if(options.getOption().getMode() == Options.MODE.germline)
	            	   new GermlineMode( (GermlineOptions) options.getOption(), logger );
	               else if(options.getOption().getMode() == Options.MODE.snpEff)
	            	    new SnpEffMode( (SnpEffOptions) options.getOption(), logger );
	            	    //new SnpEffMode( (SnpEffOptions) options.getOption());
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
