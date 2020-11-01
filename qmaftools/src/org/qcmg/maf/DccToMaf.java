/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.FileUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.record.StringFileReader;

public class DccToMaf {
	
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	private String patientId;
	
	private boolean canonicalMafMode;
	
	private final int ignoredCount = 0;
	private final int missingCanonicalTransId = 0;
	
	private String entrezFile;
	private String canonicalTranscriptsFile;
	private String verificationFile;
	private String dbSNPFile;
	
	private final Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>(40000, 0.99f);
	private final Map<String, String> ensemblGeneToCanonicalTranscript = new HashMap<String, String>();
	private final Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedData = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>();
	
	List<MAFRecord> mafs = new ArrayList<MAFRecord>();
	
	public int engage() throws Exception {
		// load mapping files
		logger.info("loading ensembl to entrez mapping file");
		MafUtils.loadEntrezMapping(entrezFile, ensemblToEntrez);
		logger.info("loading ensembl to entrez mapping file - DONE: " + ensemblToEntrez.size());
		
		logger.info("loading ensembl gene id to canonical transcript id mapping file");
		MafUtils.loadCanonicalTranscriptMapping(canonicalTranscriptsFile, ensemblGeneToCanonicalTranscript);
		logger.info("loading ensembl gene id to canonical transcript id mapping file - DONE: " + ensemblGeneToCanonicalTranscript.size());
		
		logger.info("retireving patient id from DCC header");
		getPatientId(cmdLineInputFiles[0]);
		logger.info("retireving patient id from DCC header - DONE: " + patientId);
		
		if (null != verificationFile) {
			MafUtils.getVerifiedData(verificationFile, patientId, verifiedData);
			logger.info("loading verified data map - DONE: " + verifiedData.size());
		} else {
			logger.info("skipping loading of verified data map - no verified data file");
		}
		
		// setup
		logger.info("loading DCC files");
		loadFile(cmdLineInputFiles[0]);
		loadFile(cmdLineInputFiles[1]);
		logger.info("loading DCC files - DONE: " + mafs.size());
		logger.info("no of missing canonical transcript ids: " + missingCanonicalTransId);
		
		// get dbSNP val status from dbSnpFile
		logger.info("updating MAF records with db Snp validation data");
		if (null != dbSNPFile) {
			MafUtils.getDbSNPValDetails(dbSNPFile, mafs);
			logger.info("updating MAF records with db Snp validation data - DONE");
		} else {
			logger.info("skipping update of MAF records with db Snp validation data - no db snp file");
		}
		
		// final write out to the file
		logger.info("write output");
		MafUtils.writeMafOutput(cmdLineOutputFiles[0], mafs, MafUtils.HEADER);
		
		return exitStatus;
	}
	
	private void getPatientId(String fileName) throws Exception {		
		try(StringFileReader reader = new StringFileReader(new File(fileName));) {
			for(String headerLine: reader.getHeader())	{		 
				if (headerLine.startsWith("#PatientID")) {
					patientId = headerLine.substring(headerLine.indexOf(':') +2);
				}
			}
		} 
	}
	
	private void loadFile(String fileName) throws Exception {
		try (StringFileReader reader = new StringFileReader(new File(fileName));) {
			String controlSampleID = null;
			String tumourSampleID = null;
			String tool = null;
			
			for (String headerLine : reader.getHeader()) {
				if (headerLine.startsWith("#PatientID"))
					patientId = headerLine.substring(headerLine.indexOf(':') +2);
				if (headerLine.startsWith("#ControlSampleID"))
					controlSampleID = headerLine.substring(headerLine.indexOf(':') +2);
				if (headerLine.startsWith("#TumourSampleID"))
					tumourSampleID = headerLine.substring(headerLine.indexOf(':') +2);
				if (headerLine.startsWith("#Tool")) {
					tool = headerLine.substring(headerLine.indexOf(':') +2);
				}
			}
			logger.info("patient: " + patientId + ", controlSampleID: "  + controlSampleID + ", tumourSampleID: " + tumourSampleID + ", tool: " + tool);
		
			Map<ChrPosition, TorrentVerificationStatus> patientSpecificVerification = verifiedData.get(patientId);			
			int count = 0;
			for (String rec : reader) {
				if (++count ==1) continue;	// header line
				
				MafUtils.convertDccToMaf(rec, patientId, controlSampleID, tumourSampleID, patientSpecificVerification, mafs, ensemblToEntrez);
			}
			logger.info("ignored " + ignoredCount + " dcc records");
		}  
	}
	
	public static void main(String[] args) throws Exception {
		DccToMaf sp = new DccToMaf();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running DccToMaf:", e);
			else System.err.println("Exception caught whilst running DccToMaf");
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
			logger = QLoggerFactory.getLogger(DccToMaf.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("DccToMaf", DccToMaf.class.getPackage().getImplementationVersion(), args);
			
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
			entrezFile = options.getEntrezFile();
			canonicalTranscriptsFile = options.getCanonicalTranscripts();
			dbSNPFile = options.getDbSNPFile();
			verificationFile = options.getVerified();
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			if ("canonical".equalsIgnoreCase(options.getMafMode()))
				canonicalMafMode = true;
			
			logger.tool("Running in canonical maf mode: " + canonicalMafMode);
			
			return engage();
		}
		return returnStatus;
	}
}
