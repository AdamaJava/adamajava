package au.edu.qimr.indel;


import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.LoadReferencedClasses;

import au.edu.qimr.indel.pileup.IndelMT;

public class Main {
	
	
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
		QLogger logger = options.getLogger();
		int exitStatus = 0;
		try {
					
//			String	version = Main.class.getPackage().getImplementationVersion();
			
//			logger.logInitialExecutionStats("q3pileup", version, args, QExec.createUUid());
					
			logger.info("***INPUT FILES***");
			logger.info("Test bam: " + options.getTestBam());
			logger.info("Control bam: " + options.getControlBam());
			logger.info("Log file " + options.getLog());
			logger.info("output file:" + options.getOutput().getAbsolutePath());
			logger.info("Homopolymer window: " + options.getNearbyHomopolymerWindow());
			logger.info("Nearby indel window: " + options.getNearbyIndelWindow());
			logger.info("Soft clip window: " + options.getSoftClipWindow());
			logger.info("Exclude duplicate " + options.excludeDuplicates());		
			
			IndelMT process = new IndelMT( options, logger);
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
