package au.edu.qimr.qannotate;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

import au.edu.qimr.qannotate.modes.CaddMode;
import au.edu.qimr.qannotate.modes.ConfidenceMode;
import au.edu.qimr.qannotate.modes.DbsnpMode;
import au.edu.qimr.qannotate.modes.GermlineMode;
import au.edu.qimr.qannotate.modes.IndelConfidenceMode;
import au.edu.qimr.qannotate.modes.SnpEffMode;
import au.edu.qimr.qannotate.modes.TandemRepeatMode;
import au.edu.qimr.qannotate.modes.Vcf2maf;
 
 

public class Main {
	 
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {	

		try {
            final Options options = new Options(args);
                
        	LoadReferencedClasses.loadClasses(Main.class);    
           logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(),  options.getLogLevel());	            		               
           logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);	        
           
           if(options.getMode() == Options.MODE.dbsnp)
        	   new DbsnpMode( options );
           else if(options.getMode() == Options.MODE.germline)
        	   new GermlineMode( options );
           else if(options.getMode() == Options.MODE.snpeff)
        	    new SnpEffMode(   options  );
           else if(options.getMode() == Options.MODE.confidence)
        	    new ConfidenceMode(   options);
           else if(options.getMode() == Options.MODE.vcf2maf)
        	   new Vcf2maf(  options );
           else if(options.getMode() == Options.MODE.cadd)
        	   new CaddMode(   options   );
           else if(options.getMode() == Options.MODE.indelconfidence)
        	   new IndelConfidenceMode(options);
           else if(options.getMode() == Options.MODE.trf)
        	   new TandemRepeatMode( options );
           else
            	   throw new Exception("No valid mode are specified on commandline: " + options.getMode().name()) ;

            logger.logFinalExecutionStats(0);
               
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
