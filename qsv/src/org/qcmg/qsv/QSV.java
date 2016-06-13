/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.qsv.util.QSVUtil;


/**
 * Entry point for qsv 
 */
public class QSV {
	
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");
	private static QLogger logger;
	
	private Options options;
	private String resultsDirectory;

	public static void main(String[] args) throws Exception {	

		QSV qsv = new QSV();
		
		int	 exitStatus = qsv.runQSV(args);

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
					
					//use analysis id to set up results folder
					Date analysisDate = new Date();
					
					String analysisId = getAnalysisId(options.isQCMG(), options.getOverrideOutput(), options.getSampleName(), analysisDate);
					
					String uuid = analysisId;
					if ( ! options.isQCMG()) {
						uuid = QExec.createUUid();
					}
					
					resultsDirectory = getResultsDirectory(options.getOverrideOutput(),options.getOutputDirName(), analysisId);
					createResultsDirectory(resultsDirectory);
					
					// configure logging				
				    String logFile = resultsDirectory + options.getLog();
					String version = QSV.class.getPackage().getImplementationVersion();
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
	
	
	
	public static String getAnalysisId(boolean isQCMG, String overrideOutput, String sample, Date analysisDate) {
		String analysisId = null;
		if (isQCMG && null != overrideOutput ) {
			/*
			 * Assume that the overrideOutput directory is an analysis folder containing the uuid we want to use
			 * If it isn't throw a wobbly
			 */
			String uuidFromFilename = new File(overrideOutput).getName();
			try {
				UUID.fromString(uuidFromFilename);
			} catch (IllegalArgumentException iae) {
				System.err.println("Could not get uuid from supplied overrideOutput option: " + overrideOutput);
				throw iae;
			}
			
			analysisId = uuidFromFilename;
			
		} else {
			analysisId = QSVUtil.getAnalysisId(isQCMG, sample, analysisDate);
		}
		
		return analysisId;
	}
	
	public static String getResultsDirectory(String overrideOutput, String outputDir, String analysisId) {
		
		if ( ! StringUtils.isNullOrEmpty(overrideOutput)) {
			return overrideOutput.endsWith(FILE_SEPERATOR) ? overrideOutput : overrideOutput + FILE_SEPERATOR;
		}
		
		if (null == outputDir || null == analysisId) {
			throw new IllegalArgumentException("QSV.getResultsDirectory passed null values for some arguments!!!");
		}
		return outputDir + FILE_SEPERATOR + analysisId + FILE_SEPERATOR;
	}
	
	/**
	 * Create the results directory for qsv
	 * @throws QSVException if the directory already exists
	 */
	public static void createResultsDirectory(String directoryToCreate) throws QSVException {
	    File resultsDir = new File(directoryToCreate);
	    	/*
	    	 * No longer check to see if directory already exists
	    	 */
	    resultsDir.mkdir();
	     if ( ! resultsDir.exists()) {
		    throw new QSVException("DIR_CREATE_ERROR", directoryToCreate);   
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
