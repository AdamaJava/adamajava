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

	/*               logger.tool("input: " + options.getInputFileName());
	               logger.tool("dbSNP file: " + options.getDbSNPFileName() );
	               logger.tool("output for annotated vcf records: " + options.getOutputFileName());
	               logger.info("logger level " + options.getLogLevel());
	               DbsnpMode(File input, File output, File dbSNPFile, QLogger logger)
	               new DbsnpMode( new File(options.getInputFileName()),
	            		   new File( options.getOutputFileName() ),
	            		   new File( options.getDbSNPFileName() ),
	            		   logger, options.getCommandLine());
	            		   	            		                   
*/	    	
	               if(options.getMode() == Options.MODE.snpEff)
	            	   new SnpEffMode((SnpEffOptions) options.getModeOption());
	               else if(options.getMode() == Options.MODE.germline){}
	               else if(options.getMode() == Options.MODE.dbSNP){}
	               else
	            	   throw new Exception("No valid mode are specified on commandline");
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
