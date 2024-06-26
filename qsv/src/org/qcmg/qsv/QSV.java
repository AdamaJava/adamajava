/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Date;
import java.util.UUID;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qsv.util.QSVUtil;


/**
 * Entry point for qsv 
 */
public class QSV {
	
	private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
	private static QLogger logger;

    public static void main(String[] args) {

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
                Options options = new Options(args);
				if (options.hasHelpOption()) {
					System.err.println(Messages.USAGE);
					options.displayHelp();
				} else if (options.hasVersionOption()) {
					System.err.println(Messages.getVersionMessage());			
				} else {
					
					options.parseIniFile();					
																				
					// configure logging				
				    String logFile = options.getOutputDirName() + options.getLog();
					String version = QSV.class.getPackage().getImplementationVersion();
					logger = QLoggerFactory.getLogger(QSV.class, logFile, options.getLogLevel());
					QExec exec = logger.logInitialExecutionStats("qsv", version, args, options.getUuid());
					
					logger.info("QSV files will be written to the directory: " + options.getOutputDirName());					
					
					//Run the QSV pipeline
					QSVPipeline pipeline = new QSVPipeline(options, options.getOutputDirName(), new Date(), options.getUuid(), exec);
					pipeline.runPipeline();
				}
			}
		} catch (Exception e) {	
		    System.err.println(Messages.USAGE);

			exitStatus = 1;
			if (null != logger) {				
				logger.error(QSVUtil.getStrackTrace(e));
			} else {
				System.err.print(QSVUtil.getStrackTrace(e));
			}
		}
		return exitStatus;
	}
	
	//qSV_<sampleName>_<date> is no longer used
	@Deprecated 
	public static String getAnalysisId(boolean isQCMG, String overrideOutput, String sample, Date analysisDate) {
		String analysisId;
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
	
	@Deprecated
	public static String getResultsDirectory(String overrideOutput, String outputDir, String analysisId) {
		
		if ( ! StringUtils.isNullOrEmpty(overrideOutput)) {
			return overrideOutput.endsWith(FILE_SEPARATOR) ? overrideOutput : overrideOutput + FILE_SEPARATOR;
		}
		
		if (null == outputDir || null == analysisId) {
			throw new IllegalArgumentException("QSV.getResultsDirectory passed null values for some arguments!!!");
		}
		return outputDir + FILE_SEPARATOR + analysisId + FILE_SEPARATOR;
	}

}
