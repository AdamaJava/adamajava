/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMReadGroupRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.db.ConnectionType;
import org.qcmg.db.GeneusDBConnection;
import org.qcmg.picard.SAMFileReaderFactory;

public class BAMHeaderChecker {
	
	private static final String SEPERATOR = "&";
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	
	private final List<File> bamFiles = new ArrayList<File>();
	private List<File> bamDirectories = new ArrayList<File>();
	
	private final Map<String, String> results = new HashMap<String, String>();
	
	private int exitStatus;
	
	private int engage() throws Exception {
		
		bamDirectories = Arrays.asList(FileUtils.findDirectories(cmdLineInputFiles[0], "seq_final", true));
		
		logger.info("Will check the following directories for bam files:");
		for (File f : bamDirectories) {
			logger.info(f.getAbsolutePath());
			bamFiles.addAll(Arrays.asList(FileUtils.findFilesEndingWithFilter(f.getAbsolutePath(), ".bam")));
		}
		
		// only operates on seq_final bams
//		bamFiles = Arrays.asList(FileUtils.findFiles(cmdLineInputFiles[0], ".bam"));
		
		// loop through each file and get patient, experiment and input_type
		String patient = null;
		String experiment = null;
		String input = null;
		
		GeneusDBConnection conn = new GeneusDBConnection(ConnectionType.QCMG_MAPSET);
		
		try {
			for (File bamFile : bamFiles) {
				String bamFileName = bamFile.getAbsolutePath();
				logger.info("examining bam file: " + bamFileName);
				String bamFileSmallName = bamFileName.substring(bamFileName.lastIndexOf(System.getProperty("file.separator")) + 1 , bamFileName.indexOf(".bam"));
				
				patient = bamFileSmallName.substring(0, 9);	//APGI_1234
				experiment = bamFileSmallName.substring(10, bamFileSmallName.lastIndexOf("."));	//APGI_1234
				input = bamFileSmallName.substring(bamFileSmallName.lastIndexOf(".") + 1);	//APGI_1234
				logger.info("patient: " + patient + ", experiment: " + experiment + ", input: " + input);
				
				// get details from bam header
				List<String> constituentFiles = getConstituentBamFiles(bamFile);
				List<String> trackliteConstituentFiles = getTrackliteBamFiles(patient, experiment, input, conn);
				
				//loop through tracklite constituentFiles and check that they all have an entry in bam header ConstituentFiles
				for (String trackliteBam : trackliteConstituentFiles) {
					String [] params = trackliteBam.split(SEPERATOR);
					
					String result = "OK";
					boolean trackliteMatch = false;
					
					for (String headerFileBam : constituentFiles) {
						if (headerFileBam.contains(params[0]) && headerFileBam.contains(params[1])) {
							trackliteMatch = true;
							break;
						}
					}
					
					if ( ! trackliteMatch) {
						result = "no corresponding entry in bam file header for tracklite details: " + params[0] + ":" + params[1];
						logger.warn(result);
					}
					results.put(bamFileSmallName, result);
				}
			}
		} finally {
			conn.closeConnection();
		}
		
		logger.info("");
		logger.info("");
		logger.info("SUMMARY:");
		for (Entry<String, String> resultsEntry : results.entrySet()) {
			logger.info(resultsEntry.getKey() + " : " + resultsEntry.getValue());
		}
		logger.info("DONE");
		
		return exitStatus;
	}
	
	private  List<String> getTrackliteBamFiles(String patient, String experiment, String input, GeneusDBConnection conn) throws Exception {
		List<String> trackliteResults = new ArrayList<String> ();
		
		String sql = "SELECT patient_id, run_name, barcode FROM tracklite_run tr, tracklite_sample ts" +
				" WHERE tr.sample_id = ts.processing_id" + 
				" AND ts.patient_id = '" + patient.replace('_', '-') + "'" + 
				" AND tr.experiment_type = '" + experiment + "'" + 
				" AND tr.input_type = '" + input + "'" + 
				"AND tr.run_status = 'complete'";
		
		ResultSet rs = null;
		try {
			rs = conn.executeSelectQuery(sql);
			
			while (rs.next()) {
				String runName = rs.getString(2);
				String barCode = rs.getString(3);
				logger.debug("runName: " + runName + ", barCode: " + barCode);
				trackliteResults.add(runName + SEPERATOR +  barCode);
			}
			
		} finally {
			try {
				if (null != rs && null != rs.getStatement() ) {
					rs.getStatement().close();
				}
			} finally {
				if (null != rs) rs.close();
			}
		}
		
		return trackliteResults;
	}
	
	private List<String> getConstituentBamFiles(File bamFile) {
		List<String> results = new ArrayList<String>();
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
		
			SAMFileHeader header = reader.getFileHeader();
			// get the read groups
			for (SAMReadGroupRecord readGroup : header.getReadGroups()) {
				String constituentBamFile = readGroup.getAttribute("zc");
				if (null == constituentBamFile)
					constituentBamFile = readGroup.getAttribute("ZC");
					
				if (null != constituentBamFile) {
					constituentBamFile = constituentBamFile.substring(2);
					logger.debug("read group ZC attribute: " + constituentBamFile);
					results.add(constituentBamFile);
				} else {
					logger.debug("null  ZC attribute in file: " + bamFile.getAbsolutePath());
				}
			}
			
		} finally {
			reader.close();
		}
		return results;
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
		BAMHeaderChecker sp = new BAMHeaderChecker();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running BAMHeaderChecker:", e);
			else System.err.println("Exception caught whilst running BAMHeaderChecker");
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
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(BAMHeaderChecker.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("BAMHeaderChecker", BAMHeaderChecker.class.getPackage().getImplementationVersion(), args);
			
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
