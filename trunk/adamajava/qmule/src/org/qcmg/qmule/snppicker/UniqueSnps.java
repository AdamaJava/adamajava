/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
import org.qcmg.common.util.FileUtils;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

public class UniqueSnps {
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	
	private static QLogger logger;
	
//	private static Map<ChrPosition,TabbedRecord> qSnpPileup = new HashMap<ChrPosition,TabbedRecord>(10000);
//	private static Map<ChrPosition,TabbedRecord> gatkVcfs = new HashMap<ChrPosition,TabbedRecord>(10000);
	private static Map<ChrPosition,TabbedRecord> verifiedSNPs = new HashMap<ChrPosition,TabbedRecord>(500);
	private static Map<ChrPosition,TabbedRecord> unVerifiedSNPs = new HashMap<ChrPosition,TabbedRecord>(10000);
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	
	public int engage() throws Exception {
		logger.info("hello...");
		
		loadVerifiedSnps(cmdLineInputFiles[1]);
		logger.info("loaded "  + verifiedSNPs.size() + " entries into the verifiedSNPs map");
		if (verifiedSNPs.isEmpty()) exitStatus = 1;
		
		loadUnverifiedSnps(cmdLineInputFiles[0]);
		logger.info("loaded "  + unVerifiedSNPs.size() + " entries into the un-verifiedSNPs map");
		if (unVerifiedSNPs.isEmpty()) exitStatus = 1;
		
		
//		examine(args[2]);
//		if (runQPileup) {
//			// load the existing pileup into memory
//			logger.info("running in pileup mode");
//			loadUnverifiedSnps(args[0]);
//			logger.info("loaded "  + qSnpPileup.size() + " entries into the pileup map");
//		} else {
//			logger.info("running in vcf mode");
//			loadGatkData(args[0]);
//			logger.info("loaded "  + gatkVcfs.size() + " entries into the vcf map");
//			examineVCFs(args[2]);
//		}
		
		
		// load the existing pileup into memory
		
		examine(cmdLineOutputFiles[0]);
		logger.info("goodbye...");
		
		return exitStatus;
	}
	
	
	private static void examine(String outputFile) throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputFile)) {
			
			int totalCount = 0, uniqueClassA = 0, uniqueClassB = 0, uniqueClassC = 0;
			
			FileWriter writer = new FileWriter(new File(outputFile));
		
			// loop through the verified snps
			
			try {
				for (final Map.Entry<ChrPosition,TabbedRecord> unVerifiedEntry : unVerifiedSNPs.entrySet()) {
					TabbedRecord unVerifiedRecord = unVerifiedEntry.getValue();
					String [] params = tabbedPattern.split(unVerifiedRecord.getData());
					String consequenceType = params[22];
					if (consequenceType.contains("STOP") || consequenceType.contains("NON_SYNONYMOUS")) {
					
						++totalCount;
						
						TabbedRecord verifiedRecord = verifiedSNPs.get(unVerifiedEntry.getKey());
						
						if (null == verifiedRecord) {
							String annotation = params[params.length-1];
							if ("--".equals(annotation)) {
								++uniqueClassA;
								writer.write(unVerifiedRecord.getData() + "\n");
							} else if ("less than 12 reads coverage in normal".equals(annotation)
									|| "less than 3 reads coverage in normal".equals(annotation)) {
								++uniqueClassB;
								writer.write(unVerifiedRecord.getData() + "\n");
							}
						}
					}
				}
			} finally {
				writer.close();
			}
			logger.info("totalCount: " + totalCount + ", uniqueQSnpCount (class A): " + uniqueClassA + ", uniqueQSnpCount (class B): " + uniqueClassB );
		}
	}
	
//	private static void examineVCFs(String outputFile) throws IOException {
//		if (FileUtils.canFileBeWrittenTo(outputFile)) {
//			
//			int totalCount = 0, uniqueQSnpClassACount = 0, uniqueQSnpClassBCount = 0;
//			
//			FileWriter writer = new FileWriter(new File(outputFile));
//			
//			// loop through the verified snps
//			
//			for (final Map.Entry<ChrPosition,TabbedRecord> entry : qSnpPileup.entrySet()) {
//				++totalCount;
//				TabbedRecord verifiedRecord = verifiedSNPs.get(entry.getKey());
//				TabbedRecord qSnpRecord = entry.getValue();
//				
//				if (null == verifiedRecord) {
//					String [] params = tabbedPattern.split(qSnpRecord.getPileup());
//					String annotation = params[params.length-1];
//					if ("--".equals(annotation)) {
//						++uniqueQSnpClassACount;
//						writer.write(qSnpRecord.getPileup() + "\n");
//					} else if ("less than 12 reads coverage in normal".equals(annotation)) {
//						++uniqueQSnpClassBCount;
//						writer.write(qSnpRecord.getPileup() + "\n");
//					}
//				}
//			}
//			
//			writer.close();
//			logger.info("totalCount: " + totalCount + ", uniqueQSnpCount (class A): " + uniqueQSnpClassACount + ", uniqueQSnpCount (class B): " + uniqueQSnpClassBCount );
//		}
//	}
	
	
	private static void loadUnverifiedSnps(String file) throws Exception {
		if (FileUtils.canFileBeRead(file)) {
			TabbedFileReader reader  = new TabbedFileReader(new File(file));
			try {
				for (TabbedRecord tr : reader) {
					String [] params = tabbedPattern.split(tr.getData());
					String chrPosition = params[params.length-2];
//					logger.info("chrPosition: " + chrPosition);
					ChrPosition chrPos = new ChrPosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-"))));
					
					unVerifiedSNPs.put(chrPos,tr);
				}
			} finally {
				reader.close();
			}
		}
	}
	
//	private static void loadGatkData(String pileupFile) throws IOException {
//		if (FileUtils.canFileBeRead(pileupFile)) {
//			TabbedFileReader reader  = new TabbedFileReader(new File(pileupFile));
//			for (TabbedRecord pr : reader) {
//				String [] params = tabbedPattern.split(pr.getPileup());
//				String chrPosition = params[params.length-2];
////				logger.info("chrPosition: " + chrPosition);
//				ChrPosition chrPos = new ChrPosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-"))));
//				
//				gatkVcfs.put(chrPos,pr);
//			}
//			reader.close();
//		}
//	}
	
	private void loadVerifiedSnps(String verifiedSnpFile) throws Exception {
		if (FileUtils.canFileBeRead(verifiedSnpFile)) {
			
			TabbedFileReader reader  = new TabbedFileReader(new File(verifiedSnpFile));
			try {
				for (TabbedRecord tr : reader) {
					String [] params = tabbedPattern.split(tr.getData());
					String chrPosition = params[2];
	//				logger.info("chrPosition: " + chrPosition);
					ChrPosition chrPos = new ChrPosition(chrPosition.substring(0, chrPosition.indexOf(":")-1), Integer.parseInt(chrPosition.substring(chrPosition.indexOf("-"))));
					
					verifiedSNPs.put(chrPos,tr);
				}
			} finally {
				reader.close();
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
