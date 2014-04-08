/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
//	private static org.ini4j.Ini iniFile;
	
	/**
	 * Performs a single merge based on the supplied arguments. Errors will
	 * terminate the merge and display error and usage messages.
	 * 
	 * @param args
	 *            the command-line arguments.
	 * @throws Exception 
	 */
	public static void main(final String[] args) throws Exception {
		
		LoadReferencedClasses.loadClasses(Main.class);
		
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
//			System.out.println(Messages.USAGE);
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
			logger = QLoggerFactory.getLogger(Main.class, logFile, options.getLogLevel());
			QExec qexec = StringUtils.isNullOrEmpty(uuid) ? logger.logInitialExecutionStats("qsnp", version, args) : logger.logInitialExecutionStats("qsnp", version, args, uuid);
			
			
			// examine annotate mode
			String annotateMode = IniFileUtil.getEntry(iniFile, "parameters", "annotateMode");
			String runMode = IniFileUtil.getEntry(iniFile, "parameters", "runMode");
			
			// RETRIEVE INPUT & OUTPUT FILES FROM INI
			//output
			String vcfOutputFilename = IniFileUtil.getOutputFile(iniFile, "vcf");
			String dccSomaticOutputFilename = IniFileUtil.getOutputFile(iniFile, "dccSomatic");
			String dccGermlineOutputFilename = IniFileUtil.getOutputFile(iniFile, "dccGermline");
			//input
			String pileupFilename = IniFileUtil.getInputFile(iniFile, "pileup");
			String normalVCFFilename = IniFileUtil.getInputFile(iniFile, "vcfNormal");
			String tumourVCFFilename = IniFileUtil.getInputFile(iniFile, "vcfTumour");
//			String normalIlluminaFilename = IniFileUtil.getInputFile(iniFile, "illuminaNormal");
//			String tumourIlluminaFilename = IniFileUtil.getInputFile(iniFile, "illuminaTumour");
			String chrConvFilename = IniFileUtil.getInputFile(iniFile, "chrConv");
			String germlineDBFilename = IniFileUtil.getInputFile(iniFile, "germlineDB");
			String normalBamFilename = IniFileUtil.getInputFile(iniFile, "normalBam");
			String tumourBamFilename = IniFileUtil.getInputFile(iniFile, "tumourBam");
			String referenceFilename = IniFileUtil.getInputFile(iniFile, "ref");
			String mutectFilename = IniFileUtil.getInputFile(iniFile, "mutect");
			
			StringBuilder errorString = new StringBuilder();
			
			
			// examine OUTPUT file(s)
			// MUST have a vcf file specified in ini file 
			if (null == vcfOutputFilename || ! FileUtils.canFileBeWrittenTo(vcfOutputFilename)) {
				errorString.append("No vcf output file specified in ini file");
				logger.error("No vcf output file specified in ini file");
//				return 1;
			}
			
			// Next look at annotate mode - check we have the required files if running in dcc mode
			if ("dcc".equalsIgnoreCase(annotateMode)) {
				
				//OUTPUT file checks
				// check that both somatic and germline output files have been specified
				
				if (null == dccSomaticOutputFilename || ! FileUtils.canFileBeWrittenTo(dccSomaticOutputFilename)) {
					errorString.append("Annotate mode is set to dcc, but no dccSomatic output file is specified");
				}
				if (null == dccGermlineOutputFilename || ! FileUtils.canFileBeWrittenTo(dccGermlineOutputFilename)) {
					if (errorString.length() > 0) errorString.append('\n');
					errorString.append("Annotate mode is set to dcc, but no dccGermline output file is specified");
				}
				
				//INPUT file checks
				// check that both somatic and germline output files have been specified, along with chromosome conversion
				
//				if (null == normalIlluminaFilename || ! FileUtils.canFileBeRead(normalIlluminaFilename)) {
//					if (errorString.length() > 0) errorString.append('\n');
//					errorString.append("Annotate mode is set to dcc, but no normal Illumia file is specified");
//				}
//				if (null == tumourIlluminaFilename || ! FileUtils.canFileBeRead(tumourIlluminaFilename)) {
//					if (errorString.length() > 0) errorString.append('\n');
//					errorString.append("Annotate mode is set to dcc, but no tumour Illumia file is specified");
//				}
				if (null == chrConvFilename || ! FileUtils.canFileBeRead(chrConvFilename)) {
					if (null == chrConvFilename) {
						if (errorString.length() > 0) errorString.append('\n');
						errorString.append("Annotate mode is set to dcc, but no chromosome conversion file is specified (null)");
					} else {
						if (errorString.length() > 0) errorString.append('\n');
						errorString.append("Annotate mode is set to dcc, but no chromosome conversion file is specified (can't read)");
					}
				}
				
			}
			
			// if updateGermlineDB is set to true, must have the germlineDB file in the ini
//			String updateGermlineString = IniFileUtil.getEntry(iniFile, "flags", "updateGermlineDB"); 
//			boolean updateGermlineDB = (null != updateGermlineString && "true".equalsIgnoreCase(updateGermlineString));
//			if (updateGermlineDB) {
//				if (null == germlineDBFilename || ! FileUtils.canFileBeRead(germlineDBFilename) 
//						|| ! FileUtils.canFileBeWrittenTo(germlineDBFilename)) {
//					if (errorString.length() > 0) errorString.append('\n');
//					errorString.append("updateGermlineDB is set to true, but no germlineDB file is specified");
//				}
//			}
			
			if (errorString.length() > 0) {
				logger.error(errorString.toString());
				throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
			}
			
			
			// Kick off appropriate Pipeline depending on runMode entry
			
			if ("standard".equalsIgnoreCase(runMode)) {
				
				if (areInputFilesValid(normalBamFilename, tumourBamFilename, referenceFilename)) {
					new StandardPipeline(iniFile, qexec);
				} else {
					
					logger.error("run mode is standard, but no normalBam or tumourBam or ref entry exists in inputFiles section of ini file");
					
					if ( ! areInputFilesValid(normalBamFilename)) {
						logger.error("normalBam entry is missing/can't be read: " + normalBamFilename);
					}
					if ( ! areInputFilesValid(tumourBamFilename)) {
						logger.error("tumourBam entry is missing/can't be read: " + tumourBamFilename);
					}
					if ( ! areInputFilesValid(referenceFilename)) {
						logger.error("referenceFilename entry is missing/can't be read: " + referenceFilename);
					}
					
					throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else if ("torrent".equalsIgnoreCase(runMode)) {
				
				if (areInputFilesValid(normalBamFilename, tumourBamFilename, referenceFilename)) {
//					if (areInputFilesValid(pileupFilename)) {
					new TorrentPipeline(iniFile, qexec);
				} else {
					logger.error("run mode is torrent, but no pileup file exists in inputFiles section of ini file");
					throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else if ("pileup".equalsIgnoreCase(runMode)) {
				
				if (areInputFilesValid(pileupFilename)) {
					new PileupPipeline(iniFile, qexec);
				} else {
					logger.error("run mode is pileup, but no pileup file exists in inputFiles section of ini file");
					throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else if ("vcf".equalsIgnoreCase(runMode)) {
				
				if (areInputFilesValid(normalVCFFilename, tumourVCFFilename)) {
					 new VcfPipeline(iniFile, qexec);
				} else {
					 logger.error("run mode is vcf, but no normal vcf or tumour vcf entries exists in inputFiles section of ini file");
					 throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else if ("mutect".equalsIgnoreCase(runMode)) {
				
				if (areInputFilesValid(mutectFilename)) {
					 new MuTectPipeline(iniFile, qexec);
				} else {
					 logger.error("run mode is mutect, but no mutect entriy exists in inputFiles section of ini file");
					 throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
				}
				
			} else {
				logger.error("Please specify a valid runType in the ini file : " + runMode);
				throw new SnpException("MISSING_ENTRIES_IN_INI_FILE");
			}
			
		}
		return 0;
	}
	
	public static boolean areInputFilesValid(String ... inputs) {
		if (null == inputs || inputs.length == 0) return false;
		
		for (String input : inputs) {
			if (StringUtils.isNullOrEmpty(input) || ! FileUtils.canFileBeRead(input)) {
				return false;
			}
		}
		return true;
	}
}
