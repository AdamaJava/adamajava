/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.snp.util.IniFileUtil;

/**
 * The entry point for the qsnp application
 */
public final class Main {
	
	private static QLogger logger;
	public static String version;
	
	private static String logFile;
	private static Ini iniFile;
	
	/**
	 * Performs a single merge based on the supplied arguments. Errors will
	 * terminate the merge and display error and usage messages.
	 * 
	 * @param args
	 *            the command-line arguments.
	 * @throws Exception 
	 */
	public static void main(final String[] args) throws Exception {
		
		Main main = new Main();
		int exitStatus = main.setup( args );
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		else
			System.err.println(Messages.USAGE);
		System.exit(exitStatus);
	}
	
	int setup(String [] args) throws Exception{
		Options options = new Options(args);
		if (options.hasHelpOption() || null == args || args.length == 0) {
			options.displayHelp();
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
		} else {
			options.detectBadOptions();
			// loop through supplied files - check they can be read
			for (int i = 0 ; i < options.getInputFileNames().length ; i++ ) {
				if ( ! FileUtils.canFileBeRead(options.getInputFileNames()[i])) {
					throw new SnpException("INPUT_FILE_ERROR" , options.getInputFileNames()[i]);
				}
				
				// setup the ini flie 
				if (FileUtils.isFileTypeValid(options.getInputFileNames()[i], "ini")) {
					iniFile = new Ini(new File(options.getInputFileNames()[i]));
				}
			}
			
			// exit if ini file was not supplied
			if (null == iniFile) throw new SnpException("MISSING_INI_FILE");
			
			// get analysis id from ini file if specified
			String uuid = IniFileUtil.getEntry(iniFile, "ids", "analysisId");
			
			// configure logging
			logFile = options.getLogFile();
			version = Main.class.getPackage().getImplementationVersion();
			if (null == version) {
				version = "test";
			}
			logger = QLoggerFactory.getLogger(Main.class, logFile, options.getLogLevel());
			QExec qexec = StringUtils.isNullOrEmpty(uuid) ? logger.logInitialExecutionStats("qsnp", version, args) : logger.logInitialExecutionStats("qsnp", version, args, uuid);
			
			
			// examine annotate mode
			String runMode = IniFileUtil.getEntry(iniFile, "parameters", "runMode");
			
			// RETRIEVE INPUT & OUTPUT FILES FROM INI
			//output
			String vcfOutputFilename = IniFileUtil.getOutputFile(iniFile, "vcf");
			//input
			String controlVCFFilename = IniFileUtil.getInputFile(iniFile, "controlVcf");
			String testVCFFilename = IniFileUtil.getInputFile(iniFile, "testVcf");
			String controlBamFilename = IniFileUtil.getInputFile(iniFile, "controlBam");
			String testBamFilename = IniFileUtil.getInputFile(iniFile, "testBam");
			String referenceFilename = IniFileUtil.getInputFile(iniFile, "ref");
			
			StringBuilder errorString = new StringBuilder();
			
			
			// examine OUTPUT file(s)
			// MUST have a vcf file specified in ini file 
			if (null == vcfOutputFilename || ! FileUtils.canFileBeWrittenTo(vcfOutputFilename)) {
				errorString.append("No vcf output file specified in ini file");
				logger.error("No vcf output file specified in ini file");
			}
			
			if (errorString.length() > 0) {
				logger.error(errorString.toString());
				throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
			}
			
			
			// Kick off appropriate Pipeline depending on runMode entry
			
			if ("standard".equalsIgnoreCase(runMode)) {
				
				if (FileUtils.areInputFilesValid(testBamFilename, referenceFilename)) {
					// if the control bam file does not exist, run in single sample mode
					new StandardPipeline(iniFile, qexec,  ! FileUtils.areInputFilesValid(controlBamFilename));
				} else {
					
					logger.error("run mode is standard, but no controlBam or testBam or ref entry exists in inputFiles section of ini file");
					
					if ( ! FileUtils.areInputFilesValid(controlBamFilename)) {
						logger.error("controlBam entry is missing/can't be read: " + controlBamFilename);
					}
					if ( ! FileUtils.areInputFilesValid(testBamFilename)) {
						logger.error("testBam entry is missing/can't be read: " + testBamFilename);
					}
					if ( ! FileUtils.areInputFilesValid(referenceFilename)) {
						logger.error("referenceFilename entry is missing/can't be read: " + referenceFilename);
					}
					
					throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else if ("vcf".equalsIgnoreCase(runMode)) {
				
				if (FileUtils.areInputFilesValid(testVCFFilename)) {
					 new VcfPipeline(iniFile, qexec,  ! FileUtils.areInputFilesValid(controlVCFFilename));
				} else {
					 logger.error("run mode is vcf, but no control vcf or test vcf entries exists in inputFiles section of ini file");
					 throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}				
			} else {
				logger.error("Please specify a valid runType in the ini file : " + runMode);
				throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
			}
			
		}
		return 0;
	}
}
