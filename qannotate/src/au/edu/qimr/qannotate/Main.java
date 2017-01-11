/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

import au.edu.qimr.qannotate.modes.*;

public class Main {
	 
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {	

		try {
			System.out.println("About to run Options");
            final Options options = new Options(args);
            System.out.println("About to run Options - DONE");
                
        		LoadReferencedClasses.loadClasses(Main.class);    
           logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(),  options.getLogLevel());	            		               
           logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);	        
           
           if (options.getMode() == Options.MODE.dbsnp) {
        	   		new DbsnpMode( options );
           	} else if (options.getMode() == Options.MODE.germline) {
        	   		new GermlineMode( options );
			} else if (options.getMode() == Options.MODE.snpeff) {
        	    		new SnpEffMode(   options  );
			} else if (options.getMode() == Options.MODE.confidence) {
        	    		new ConfidenceMode(   options);
			} else if (options.getMode() == Options.MODE.vcf2maf) {
				new Vcf2maf(  options );
			} else if (options.getMode() == Options.MODE.cadd) {
        	   		new CaddMode(   options   );
           } else if (options.getMode() == Options.MODE.indelconfidence) {
        	   		new IndelConfidenceMode(options);
           } else if (options.getMode() == Options.MODE.hom) {
        	   		new HomoplymersMode(options);
           } else if (options.getMode() == Options.MODE.trf) {
        	   		new TandemRepeatMode( options );
           } else {
            	   throw new Exception("No valid mode are specified on commandline: " + options.getMode().name()) ;
           }

            logger.logFinalExecutionStats(0);
               
        } catch (Exception e) {
	        	System.out.println("Exception caught!");
	        	e.printStackTrace();
	        	System.err.println(Thread.currentThread().getName() + " " + e.toString());
	        	if (null != logger) {
		        	logger.info(Thread.currentThread().getName() + " " + e.toString());	            
		        logger.logFinalExecutionStats(1);
	        	}
        		System.out.println("About to return exit code of 1");
            System.exit(1);
        }			
		
		
	}
}
