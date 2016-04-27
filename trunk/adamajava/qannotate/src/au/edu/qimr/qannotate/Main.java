package au.edu.qimr.qannotate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.LoadReferencedClasses;

import au.edu.qimr.qannotate.modes.CaddMode;
import au.edu.qimr.qannotate.modes.ConfidenceMode;
import au.edu.qimr.qannotate.modes.CustomerConfidenceMode;
import au.edu.qimr.qannotate.modes.DbsnpMode;
import au.edu.qimr.qannotate.modes.GermlineMode;
import au.edu.qimr.qannotate.modes.IndelConfidenceMode;
import au.edu.qimr.qannotate.modes.SnpEffMode;
import au.edu.qimr.qannotate.modes.TandemRepeatMode;
import au.edu.qimr.qannotate.modes.Vcf2maf;
import au.edu.qimr.qannotate.options.ConfidenceOptions;
import au.edu.qimr.qannotate.options.CustomerConfidenceOptions;
import au.edu.qimr.qannotate.options.Options;
import au.edu.qimr.qannotate.options.SnpEffOptions;
import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.options.CaddOptions;
import au.edu.qimr.qannotate.options.GeneralOptions;

public class Main {
	 
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {	

		try {
            final Options options = new Options();
            
            if ( options.parseArgs(args)){ 	    
            	LoadReferencedClasses.loadClasses(Main.class);    
               logger = QLoggerFactory.getLogger(Main.class, options.getOption().getLogFileName(),  options.getOption().getLogLevel());	            		               
               logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);	        
               
               if(options.getOption().getMode() == Options.MODE.dbsnp)
            	   new DbsnpMode( (GeneralOptions) options.getOption()   );
               else if(options.getOption().getMode() == Options.MODE.germline)
            	   new GermlineMode( (GeneralOptions) options.getOption() );
               else if(options.getOption().getMode() == Options.MODE.snpEff)
            	    new SnpEffMode( (SnpEffOptions) options.getOption() );
               else if(options.getOption().getMode() == Options.MODE.confidence)
            	    new ConfidenceMode( (ConfidenceOptions) options.getOption() );
               else if(options.getOption().getMode() == Options.MODE.customerConfidence)
            	    new CustomerConfidenceMode( (CustomerConfidenceOptions) options.getOption() );	 
               else if(options.getOption().getMode() == Options.MODE.vcf2maf)
            	   new Vcf2maf((Vcf2mafOptions) options.getOption());
               else if(options.getOption().getMode() == Options.MODE.cadd)
            	   new CaddMode( (CaddOptions) options.getOption()  );
               else if(options.getOption().getMode() == Options.MODE.indelconfidence)
            	   new IndelConfidenceMode((GeneralOptions) options.getOption());
               else if(options.getOption().getMode() == Options.MODE.trf)
            	   new TandemRepeatMode( (GeneralOptions) options.getOption() );
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
