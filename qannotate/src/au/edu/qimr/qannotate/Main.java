/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

import au.edu.qimr.qannotate.modes.*;

public class Main {
	 
	private static QLogger logger;
	public static void main(final String[] args) throws Exception {	

		try {
           final Options options = new Options(args);             
        		//LoadReferencedClasses.loadClasses(Main.class);    
           logger = QLoggerFactory.getLogger(Main.class, options.getLogFileName(),  options.getLogLevel());	            		               
           logger.logInitialExecutionStats(options.getPGName(), options.getVersion(),args);
           
           checkOptions(options);
           
           if (options.getMode() == Options.MODE.dbsnp) {
    	   		new DbsnpMode( options );
           	} else if (options.getMode() == Options.MODE.germline) {
    	   		new GermlineMode( options );
			} else if (options.getMode() == Options.MODE.snpeff) {
	    		new SnpEffMode(   options  );
			} else if (options.getMode() == Options.MODE.confidence) {
	    		new ConfidenceMode(   options);
			} else if (options.getMode() == Options.MODE.ccm) {
				new CCMMode(   options);
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
            } else if (options.getMode() == Options.MODE.make_valid) {
    	   		new MakeValidMode( options );
//           } else if (options.getMode() == Options.MODE.snppileup) {
//   	   			new SnpPileupMode( options );
   	   	    } else if (options.getMode() == Options.MODE.overlap) {
   	   			new OverlapMode( options );
	   	   	} else if (options.getMode() == Options.MODE.vcf2maftmp) {
				new Vcf2mafTmp(  options );
	   	   	} else if (options.getMode() == null) {
	   	   		throw new IllegalArgumentException("No mode was specified on the commandline - please add the \"-mode\" option") ;
            } else {
    	   		throw new IllegalArgumentException("No valid mode are specified on commandline - please run \"qannotate -help\" to see the list of available modes");
            }

            logger.logFinalExecutionStats(0);
               
        } catch (Exception e) {
        	System.out.println("Exception caught!");
        	e.printStackTrace();
        	System.err.println(Thread.currentThread().getName() + " " + e.toString() + " : " + e.getLocalizedMessage());
        	if (null != logger) {
	        	logger.info(Thread.currentThread().getName() + " " + e.toString() + " : " + e.getMessage());	            
	        	logger.logFinalExecutionStats(1);
        	}
    		System.out.println("About to return exit code of 1");
    		System.exit(1);
        }
	}
	
	/**
	 * Checks the Options object to see if the minimal options (input, output, database) have been supplied.
	 * Using the switch statements fall through process here.
	 * 
	 * Assuming that the individual modes will perform any more specific Options checking internally 
	 *  
	 * @param o
	 */
	public static void checkOptions(Options o) {
		Options.MODE m = o.getMode();
		switch (m) {
		/*
		 * some modes need a database
		 */
		case dbsnp:
		case germline:
		case hom:
		case cadd:
		case snpeff:
		case trf:
			if (null == o.getDatabaseFileName()) {
        		throw new IllegalArgumentException("Please supply a reference file using the \"-d\" option");
        	}
			
		/*
		 * most modes need an output and input
		 * there is a break after these checks as the vcf2mafs are a slightly special case
		 */
		case overlap:
		case confidence:
		case indelconfidence:
		case ccm:
		case make_valid:
			if (null == o.getOutputFileName()) {
				throw new IllegalArgumentException("Please supply an output file using the \"-output\" or \"-o\" option");
			}
			if (null == o.getInputFileName()) {
				throw new IllegalArgumentException("Please supply an input file using the \"-input\" option");
			}
			break;
		/*
		 * apart from the vcf2maf modes which can take either a outdir or output option
		 */
		case vcf2maf:
		case vcf2maftmp:
			if (null == o.getInputFileName()) {
				throw new IllegalArgumentException("Please supply an input file using the \"-input\" option");
			}
			if (null == o.getOutputDir() && null == o.getOutputFileName()) {
				throw new IllegalArgumentException("Please supply an output dir using the \"-outdir\" option, or an output file using the \"-output\" or \"-o\" option");
			}
		}
	}
}
