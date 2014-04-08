/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MafFinalFilter {
	
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final String KRAS = "KRAS";
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private boolean includePositionsThatDidNotVerify;
	
	List<TabbedRecord> highConfidenceMafs = new ArrayList<TabbedRecord>(); 
//	List<TabbedRecord> probableNoiseMafs = new ArrayList<TabbedRecord>(); 
	
	public int engage() throws Exception {
		// load mapping files
		logger.info("filtering maf file: " + cmdLineInputFiles[0]);
		filterMafFile(cmdLineInputFiles[0], cmdLineOutputFiles[0]);
		logger.info("output is now in: " + cmdLineOutputFiles[0]);
		
		return exitStatus;
	}
	
	private void filterMafFile(String inputFile, String outputMafFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(inputFile));
		
		TabbedHeader header = reader.getHeader();
		FileWriter writer = new FileWriter(new File(outputMafFile), false);
		
		int passNovelCountCheck = 0, count = 0;
		
		try {
			for (Iterator<String> iter = header.iterator() ; iter.hasNext() ;) {
				String headerLine = iter.next();
				writer.write(headerLine + "\n");
			}
		
			for (TabbedRecord rec : reader) {
				if (count++ == 0 && rec.getData().startsWith("Hugo_Symbol")) {
					writer.write(rec.getData() + "\n");
					continue;
				}
				
				String[] params = tabbedPattern.split(rec.getData(), -1);
				
				// want to include all of KRAS mafs, regardless of if they pass 4 novel start filters
				String geneName = params[0];
				int novelStartCount = Integer.parseInt(params[params.length - 1]);
				
				if (KRAS.equals(geneName) || novelStartCount >= 4) {
					
					String validationStatus = params[24];
					if ( ! includePositionsThatDidNotVerify) {
						// ignore 
						if (validationStatus.startsWith("False")) continue;
						if (validationStatus.startsWith("Unknown")) {
							// don't want the extra info in the validation status field - just Unknown 
							rec.setData(rec.getData().replace(validationStatus, "Unknown"));
						}
					}
					
					// add to collection
					writer.write(rec.getData() + "\n");
					passNovelCountCheck++;
				}
			}
			logger.info("for file: " + inputFile + " stats (count, passNovelCountCheck): " + count + "," + passNovelCountCheck);
			
		} finally {
			try {
				writer.close();
			} finally {
				reader.close();
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		MafFinalFilter sp = new MafFinalFilter();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running MafFilter:", e);
			else System.err.println("Exception caught whilst running MafFilter");
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
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
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafFinalFilter.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafFinalFilter", MafFinalFilter.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMafException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMafException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			if (options.getIncludeInvalid())
				includePositionsThatDidNotVerify = true;
			
			return engage();
		}
		return returnStatus;
	}
}
