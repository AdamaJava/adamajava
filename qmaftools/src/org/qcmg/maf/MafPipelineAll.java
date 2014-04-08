/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.util.Arrays;

import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;

public class MafPipelineAll extends MafPipeline {
	
	
	private int engage() throws Exception {
		
		// get list of patients to run from seq_analysis
		// must start with APGI_[0-9]
		patientsInSeqAnalysis = Arrays.asList(FileUtils.findFiles(SEQ_ANALYSIS, patientDirectoryFilter));
		
		logger.info("Will attempt maf generation for the following " + patientsInSeqAnalysis.size() + " patients: ");
		for (File f : patientsInSeqAnalysis) logger.info(f.getName());
		
		// need to check each patient to ensure that they have a valid SNP and indel folder
		checkPatientsHaveRequiredFolders();
		logger.info("no of patients with data: " + patientsAndFiles.size());
		
		loadRequiredStaticFiles();
		
		loadDCCFiles();
		
		writeIndividualPatientMafFiles();
		
		loadKRASData();
		
		performFilter();
		
		addCpgAndGff();
		
		addNovelStartsMT(SEQ_RESULTS, SEQ_FINAL, SEQ_RESULTS_BAM_EXT);
		
		writeFinalFilteredOutput();
		
		return exitStatus;
	}
	
	public static void main(String[] args) throws Exception {
		MafPipelineAll sp = new MafPipelineAll();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running QMafPipeline:", e);
			else System.err.println("Exception caught whilst running QMafPipeline");
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			//give warning (at least) if these files don't exist
			
			if ((entrezFile = options.getEntrezFile()) == null || ! FileUtils.canFileBeRead(entrezFile))
				throw new QMafException("NO_ENTREZ_FILE_ERROR");
			if ((canonicalTranscriptsFile = options.getCanonicalTranscripts()) == null || ! FileUtils.canFileBeRead(canonicalTranscriptsFile))
				throw new QMafException("NO_CANONICAL_FILE_ERROR");
			if ((verificationFile = options.getVerified()) == null || ! FileUtils.canFileBeRead(verificationFile))
				throw new QMafException("NO_VERIFIED_FILE_ERROR");
			if ((dbSNPFile = options.getDbSNPFile()) == null || ! FileUtils.canFileBeRead(dbSNPFile))
				throw new QMafException("NO_DBSNP_FILE_ERROR");
			if ((krasFile = options.getKrasFile()) == null || ! FileUtils.canFileBeRead(krasFile))
				throw new QMafException("NO_KRAS_FILE_ERROR");
			if ((fastaFile = options.getFastaFile()) == null || ! FileUtils.canFileBeRead(fastaFile))
				throw new QMafException("NO_FASTA_FILE_ERROR");
			if ((gffFile = options.getGffFile()) == null || ! FileUtils.canFileBeRead(gffFile))
				throw new QMafException("NO_GFF_FILE_ERROR");
			
			if (null == options.getDirNames() || options.getDirNames().length < 1)
				throw new QMafException("MISSING_OUTPUT_DIRECTORY");
			
			outputDirectory = options.getDirNames()[0];
			lowCoveragePatients = options.getLowCoveragePatients();
			if (options.getNoOfBases() > 0)
				noOfBases = options.getNoOfBases();
			
			ensemblV61 = options.hasEnsembl61Option();
			
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafPipelineAll.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("MafPipelineAll", MafPipelineAll.class.getPackage().getImplementationVersion(), args);
			
			logger.tool("will retrieve " + noOfBases + " on either side of position of interest from fasta file");
			logger.tool("ensemblV61: " + ensemblV61);
			
//			if ("worst".equalsIgnoreCase(options.getMafMode()))
//				canonicalMafMode = false;
//			logger.tool("Running in canonical maf mode: " + canonicalMafMode);
			
			return engage();
		}
		return returnStatus;
	}
}
