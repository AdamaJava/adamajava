/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.record.StringFileReader;

public class UniqueSnps {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	
	private static QLogger logger;
	
	private static Map<ChrPosition,String> verifiedSNPs = new HashMap<ChrPosition,String>(500);
	private static Map<ChrPosition,String> unVerifiedSNPs = new HashMap<ChrPosition,String>(10000);
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	
	public int engage() throws Exception {
		logger.info("hello...");
		
		loadVerifiedSnps(cmdLineInputFiles[1]);
		logger.info("loaded "  + verifiedSNPs.size() + " entries into the verifiedSNPs map");
		if (verifiedSNPs.isEmpty()) exitStatus = 1;
		
		loadUnverifiedSnps(cmdLineInputFiles[0]);
		logger.info("loaded "  + unVerifiedSNPs.size() + " entries into the un-verifiedSNPs map");
		if (unVerifiedSNPs.isEmpty()) exitStatus = 1;
				
		// load the existing pileup into memory		
		examine(cmdLineOutputFiles[0]);
		logger.info("goodbye...");
		
		return exitStatus;
	}
	
	
	private static void examine(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			int totalCount = 0, uniqueClassA = 0, uniqueClassB = 0;		
			try(FileWriter writer = new FileWriter(new File(outputFile));) {
				for (final Map.Entry<ChrPosition,String> unVerifiedEntry : unVerifiedSNPs.entrySet()) {
					String unVerifiedRecord = unVerifiedEntry.getValue();
					String [] params = TabTokenizer.tokenize(unVerifiedRecord);
					String consequenceType = params[22];
					if (consequenceType.contains("STOP") || consequenceType.contains("NON_SYNONYMOUS")) {
					
						++totalCount;
						
						String verifiedRecord = verifiedSNPs.get(unVerifiedEntry.getKey());
						
						if (null == verifiedRecord) {
							String annotation = params[params.length-1];
							if ("--".equals(annotation)) {
								++uniqueClassA;
								writer.write(unVerifiedRecord + "\n");
							} else if ("less than 12 reads coverage in normal".equals(annotation)
									|| "less than 3 reads coverage in normal".equals(annotation)) {
								++uniqueClassB;
								writer.write(unVerifiedRecord + "\n");
							}
						}
					}
				}
			} 
			logger.info("totalCount: " + totalCount + ", uniqueQSnpCount (class A): " + uniqueClassA + ", uniqueQSnpCount (class B): " + uniqueClassB );
		}
	}	
	
	private static void loadUnverifiedSnps(String file) throws Exception {
		if (FileUtils.canFileBeRead(file)) {
			StringFileReader reader  = new StringFileReader(new File(file));
			try {
				for (String tr : reader) {
					String [] params = tabbedPattern.split(tr);
					String chrPosition = params[params.length-2];
					int start = Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-")));
					ChrPosition chrPos = new ChrRangePosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), start, start);					
					unVerifiedSNPs.put(chrPos,tr);
				}
			} finally {
				reader.close();
			}
		}
	}
	
	private void loadVerifiedSnps(String verifiedSnpFile) throws Exception {
		if (FileUtils.canFileBeRead(verifiedSnpFile)) {
			
			
			try(StringFileReader reader  = new StringFileReader(new File(verifiedSnpFile,Constants.HASH_STRING));) {
				for (String tr : reader) {
					String [] params = TabTokenizer.tokenize(tr);
					String chrPosition = params[2];
	//				logger.info("chrPosition: " + chrPosition);
					int start =  Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-")));
					ChrPosition chrPos = new ChrRangePosition(chrPosition.substring(0, chrPosition.indexOf(":")-1),start, start);
					
					verifiedSNPs.put(chrPos,tr);
				}
			}  
		}
	}
	
	public static void main(String[] args) throws Exception {
		UniqueSnps sp = new UniqueSnps();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = -1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(UniqueSnps.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("UniqueSnps", UniqueSnps.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			return engage();
		}
		return returnStatus;
	}
	
}
