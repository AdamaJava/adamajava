/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.qmule.util.IGVBatchFileGenerator;
import org.qcmg.qmule.util.TabbedDataLoader;
import org.qcmg.tab.TabbedRecord;

public class CompareSnps {
	
	private final ConcurrentMap<ChrPosition, TabbedRecord> firstSnpMap = new ConcurrentHashMap<ChrPosition, TabbedRecord>(30000); //not expecting more than 100000
	private final ConcurrentMap<ChrPosition, TabbedRecord> secondSnpMap = new ConcurrentHashMap<ChrPosition, TabbedRecord>(30000);
	private final List<ChrPosition> firstList = new ArrayList<ChrPosition>();
	private final List<ChrPosition> secondList = new ArrayList<ChrPosition>();
//	private final ConcurrentMap<ChrPosition, TabbedRecord> uniqueTumourVCFMap = new ConcurrentHashMap<ChrPosition, TabbedRecord>(40000);
	
	private static QLogger logger;
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	public int engage() throws Exception {
		
		logger.info("loading snp data from file: " + cmdLineInputFiles[0]);
		TabbedDataLoader.loadTabbedData(cmdLineInputFiles[0], -2, firstSnpMap);
		logger.info("loading snp data - DONE [" + firstSnpMap.size() + "]");
		logger.info("loading snp data from file: " + cmdLineInputFiles[1]);
		TabbedDataLoader.loadTabbedData(cmdLineInputFiles[1], -2, secondSnpMap);
		logger.info("loading snp data - DONE [" + secondSnpMap.size() + "]");
		
		compare();
		
		outputIGVBatchFiles();
		
//		addPileupFromNormalBam();
		
		return exitStatus;
	}
	
	private void outputIGVBatchFiles() throws IOException {
		IGVBatchFileGenerator.generate(firstList, cmdLineOutputFiles[0]);
		IGVBatchFileGenerator.generate(secondList, cmdLineOutputFiles[1]);
	}
	
	protected void compare() {

		// total counts
		int firstMapCount = 0, secondMapCount = 0;
		// count of snps unique to each input
		int uniqueToFirstMap = 0, uniqueToSecondMap = 0;
		int commonSnps = 0, commonAndAlsoClassABStopNonSynon = 0;
		
		// loop through first set
		for (Entry<ChrPosition, TabbedRecord> entry : firstSnpMap.entrySet()) {
			
			TabbedRecord firstRecord = entry.getValue();
			
			if (isClassAB(firstRecord, -1) && isStopNonSynonymous(firstRecord, 22)) {
				firstMapCount++;
				
				TabbedRecord secondRecord = secondSnpMap.get(entry.getKey());
				if (null == secondRecord || ! (isClassAB(secondRecord, -1) && isStopNonSynonymous(secondRecord, 22))) {
					uniqueToFirstMap++;
					firstList.add(entry.getKey());
					logger.info("Unique to first: " + entry.getKey().getChromosome() + ":" + entry.getKey().getPosition());
				} else {
					commonSnps++;
//					if (isClassAB(secondRecord, -1) && isStopNonSynonymous(secondRecord, 22)) {
//						commonAndAlsoClassABStopNonSynon++;
//					}
				}
			}
			
		}
		
		// loop through second set
		for (Entry<ChrPosition, TabbedRecord> entry : secondSnpMap.entrySet()) {
			
			TabbedRecord secondRecord = entry.getValue();
			
			if (isClassAB(secondRecord, -1) && isStopNonSynonymous(secondRecord, 22)) {
				secondMapCount++;
				
				TabbedRecord firstRecord = firstSnpMap.get(entry.getKey());
				if (null == firstRecord || ! (isClassAB(firstRecord, -1) && isStopNonSynonymous(firstRecord, 22))) {
					uniqueToSecondMap++;
					secondList.add(entry.getKey());
					logger.info("Unique to second: " + entry.getKey().getChromosome() + ":" + entry.getKey().getPosition());
//					logger.info("IGV: " + entry.getValue().getData());
				}
			}
		}
		
		logger.info("SUMMARY:");
		logger.info("firstMapCount: " + firstMapCount);
		logger.info("secondMapCount: " + secondMapCount);
		logger.info("uniqueToFirstMap: " + uniqueToFirstMap);
		logger.info("uniqueToSecondMap: " + uniqueToSecondMap);
		logger.info("commonSnps: " + commonSnps);
//		logger.info("commonAndAlsoClassABStopNonSynon: " + commonAndAlsoClassABStopNonSynon);
		
	}
	
	
	
	protected static boolean isClassAB(TabbedRecord record, int index) {
		if (null == record || null == record.getData()) throw new IllegalArgumentException("null or empty Tabbed record");
		String [] params = TabbedDataLoader.tabbedPattern.split(record.getData());
		String qcmgFlag = TabbedDataLoader.getStringFromArray(params, index);
		
		return SnpUtils.isClassAorB(qcmgFlag);
//		return "--".equals(qcmgFlag) || "less than 12 reads coverage in normal".equals(qcmgFlag)
//				|| "less than 3 reads coverage in normal".equals(qcmgFlag);
		
	}
	
	protected static boolean isStopNonSynonymous(TabbedRecord record, int index) {
		if (null == record || null == record.getData()) throw new IllegalArgumentException("null or empty Tabbed record");
		String [] params = TabbedDataLoader.tabbedPattern.split(record.getData());
//		String consequenceType = params[index];
		String consequenceType = TabbedDataLoader.getStringFromArray(params, index);
		
		return consequenceType.contains("STOP") || consequenceType.contains("NON_SYNONYMOUS");
	}
	
	
	
	public static void main(String[] args) throws Exception {
		CompareSnps sp = new CompareSnps();
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
			logger = QLoggerFactory.getLogger(CompareSnps.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareSnps", CompareSnps.class.getPackage().getImplementationVersion(), args);
			
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
