package au.edu.qimr.indel;


import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.LoadReferencedClasses;

import au.edu.qimr.indel.pileup.IndelMT;

public class Main {
	
	private static QLogger logger = QLoggerFactory.getLogger(Main.class);
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit( 0 );
		}
		
		Options options = new Options(args);
		if (options.hasHelpOption()) {
			options.displayHelp();
			System.exit( 0 );
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());	
			System.exit( 0 );
		} 
		

		LoadReferencedClasses.loadClasses(Main.class);
		
		int exitStatus = 0;
		try {
					
			String	version = Main.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(Main.class, options.getLog(), null);
			logger.logInitialExecutionStats("q3pileup", version, args, QExec.createUUid());
					
			logger.info("***INPUT FILES***");
			logger.info("Tumour bam: " + options.getTumourBam());
			logger.info("Normal bam: " + options.getNormalBam());
			logger.info("Log file " + options.getLog());
			logger.info("output file:" + options.getOutput().getAbsolutePath());
			logger.info("Homopolymer window: " + options.getNearbyHomopolymerWindow());
			logger.info("Nearby indel window: " + options.getNearbyIndelWindow());
			logger.info("Soft clip window: " + options.getSoftClipWindow());
			logger.info("Include duplicate " + options.includeDuplicates());		
			
			IndelMT process; 
			if(options.getsecondInputVcf() == null){
				logger.info("input vcf file:" + options.getfirstInputVcf().getAbsolutePath());
				process = new IndelMT(options.getfirstInputVcf(), options, logger);
			}else{
				logger.info("first indel input: " + options.getfirstInputVcf().getAbsolutePath());
				logger.info("second indel input " + options.getsecondInputVcf().getAbsolutePath());
				process = new IndelMT(options.getfirstInputVcf(), options.getsecondInputVcf(), options, logger);
			}			
			
			exitStatus = process.process(options.getThreadNo());			
		} catch (Exception e) {				
			String errStr = Q3IndelException.getStrackTrace(e);			
			if (null != logger)  logger.error(errStr);
			else  System.err.print(errStr);
			exitStatus = 1;			 
		}
		 
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);
	}	
	

}
