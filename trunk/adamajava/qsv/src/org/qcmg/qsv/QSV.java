/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.qsv.util.QSVUtil;


/**
 * Entry point for qsv 
 */
public class QSV {
	
	private static QLogger logger;	
	private static String version;	
	private String logFile;
	private Options options;
	private Date analysisDate;
	private String resultsDirectory;
	private String analysisId;
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");
	

	public static void main(String[] args) throws Exception {	

		QSV qsv = new QSV();
		
		int	exitStatus = qsv.runQSV(args);

		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);	
	}

	/**
	 * Method to run qSV
	 * @param args command line arguments
	 * @return exit status
	 */
	protected int runQSV(String[] args) {
		int exitStatus = 0;
		try {
			//Get options from the command line			
			if (args.length == 0) {
				System.err.println(Messages.USAGE);
			} else {
				this.options = new Options(args);
				if (options.hasHelpOption()) {
					options.displayHelp();
				} else if (options.hasVersionOption()) {
					System.err.println(Messages.getVersionMessage());			
				} else {
					
					options.parseIniFile();					
					LoadReferencedClasses.loadClasses(QSV.class);					
					
					//use analysis id to see up results folder
					analysisDate = new Date();
					analysisId = QSVUtil.getAnalysisId(options.isQCMG(), options.getSampleName(), analysisDate);					
					
					String uuid = analysisId;
					if (!options.isQCMG()) {
						uuid = QExec.createUUid();
					}
					createResultsDirectory();
					
					// configure logging				
				    logFile = resultsDirectory + options.getLog();
					version = QSV.class.getPackage().getImplementationVersion();
					logger = QLoggerFactory.getLogger(QSV.class, logFile, options.getLogLevel());
					QExec exec = logger.logInitialExecutionStats("qsv", version, args, uuid);
					
					logger.info("QSV files will be written to the directory: " + resultsDirectory);					
					
					//Run the QSV pipeline
					QSVPipeline pipeline = new QSVPipeline(options, resultsDirectory, analysisDate, analysisId, exec);
					pipeline.runPipeline();
				}
			}
		} catch (Exception e) {	
		    System.err.println(Messages.USAGE);
			e.printStackTrace();
			exitStatus = 1;
			if (null != logger) {				
				logger.error(QSVUtil.getStrackTrace(e));
			} else {
				System.err.print(QSVUtil.getStrackTrace(e));
			}
		}
		return exitStatus;
	}
	
	/**
	 * Create the results directory for qsv
	 * @throws QSVException if the directory already exists
	 * @throws IOException if the directory could not be created
	 */
	public void createResultsDirectory() throws QSVException, IOException {
	    resultsDirectory = options.getOutputDirName() + FILE_SEPERATOR + analysisId + FILE_SEPERATOR;
	    File resultsDir = new File(resultsDirectory);
	    
	    if (resultsDir.exists()) {
	    	 throw new QSVException("DIR_CREATE_ERROR", resultsDir.toString()); 
	    }
	    
		if (!resultsDir.mkdir()) {
		    throw new QSVException("DIR_CREATE_ERROR", resultsDir.toString());   
		}		
	}

	/**
	 * Get the results direct
	 * @return the full path of the results directory
	 */
	public String getResultsDirectory() {
		return resultsDirectory;
	}

}
