package au.edu.qimr.indel;

import org.qcmg.common.log.QLogger;
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
			logger.config("***[IOs]***");
			logger.config("Reference file: " + options.getReference());
			logger.config("Test bam: " + options.getTestBam());
			logger.config("Control bam: " + options.getControlBam());
			logger.config( "Test vcf: " + options.getTestInputVcf());
			logger.config("Control vcf: " + options.getControlInputVcf());
			logger.config("Log file " + options.getLog());
			logger.config("output file:" + options.getOutput().getAbsolutePath());
			
			logger.config("***[ids]***");
			logger.config("Donor id: " + options.getDonorId());
			logger.config("Analysis id: " + options.getAnalysisId());
			logger.config("Test Sample uuid: " + options.getTestSample());			
			logger.config("Control Sample uuid: " + options.getControlSample());
			
			logger.config("***[parameters]***");
			logger.config("Run mode: " + options.getRunMode());
			logger.config("Thread number: " + options.getThreadNo());
			logger.config("query for bam record filter: " + options.getFilterQuery());
			logger.config("Nearby indel window: " + options.getNearbyIndelWindow());
//			logger.config("Homopolymer window: " + options.getNearbyHomopolymerWindow());
			logger.config("Soft clip window: " + options.getSoftClipWindow());			
					
			logger.config("***[rules]***");
			logger.config("Gematic novel starts limits: " + options.getMinGematicNovelStart());	
			logger.config("Gematic suporting percentage limits: " + options.getMinGematicSupportOfInformative());			 
			
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
