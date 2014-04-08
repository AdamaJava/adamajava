/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.vcf;

import java.io.File;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedFileWriter;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class ConvertVcfChr {
	
	private static final String CHR = "chr";
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private static QLogger logger;
	
	
	private int engage() throws Exception {
		
		// load 
		if (FileUtils.canFileBeRead(cmdLineInputFiles[0])) {
			TabbedFileReader reader  = new TabbedFileReader(new File(cmdLineInputFiles[0]));
			TabbedHeader header = reader.getHeader();
			
			TabbedFileWriter writer = new TabbedFileWriter(new File(cmdLineOutputFiles[0]));
			writer.addHeader(header);
			
			try {
				for (TabbedRecord tabRec : reader) {
					if ( ! tabRec.getData().startsWith(CHR)) {
						tabRec.setData(CHR + tabRec.getData());
					}
					writer.add(tabRec);
				}
			} finally {
				try {
					writer.close();
				} finally {
					reader.close();
				}
			}
		}
		return exitStatus;
	}
	
	public static void main(String[] args) throws Exception {
		ConvertVcfChr sp = new ConvertVcfChr();
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
			logger = QLoggerFactory.getLogger(ConvertVcfChr.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareVCFs", ConvertVcfChr.class.getPackage().getImplementationVersion(), args);
			
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
