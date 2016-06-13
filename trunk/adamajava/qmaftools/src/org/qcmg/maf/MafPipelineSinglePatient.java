/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.Pair;

public class MafPipelineSinglePatient extends MafPipeline {
	
	private String[] patients;
	private String[] patientDCCFiles;
	private String[] patientBamFiles;
	
	public static final FilenameFilter patientDirectoryFilter = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return name.startsWith("APGI_") && Character.isDigit(name.charAt(5))
			&& new File(file + FS + name).isDirectory(); 
		}
	};
	
	private int engage() throws Exception {
		
		// if we don't have any patients - exit
		if (null != patients && patients.length > 0) {
		
			// if dccs is not empty, don't get dcc files from seq_Results
			if (null != patientDCCFiles && patientDCCFiles.length > 0) {
				if (patients.length != patientDCCFiles.length && (patients.length * 2 != patientDCCFiles.length)) {
					logger.error("number of patients doesn't match number of dcc files");
					return 1;
				}
				int i = 0;
				for (String patient : patients) {
					// add patient and files to map
					
					if (patients.length * 2 == patientDCCFiles.length) {
						patientsAndFiles.put(patient, new Pair<File, File>(new File(patientDCCFiles[i++]), new File(patientDCCFiles[i++])));
					} else {
						// assume if we just have a single dcc that is a snp dcc rather than indel
						patientsAndFiles.put(patient, new Pair<File, File>(new File(patientDCCFiles[i++]), (File)null));
					}
				}
			} else {
			
				// get list of patients to run from seq_analysis
				// must start with APGI_[0-9]
		//		patientsInSeqAnalysis = Arrays.asList(FileUtils.findDirectories(SEQ_ANALYSIS, patientDirectoryFilter));
				patientsInSeqAnalysis = Arrays.asList(FileUtils.findFiles(SEQ_ANALYSIS, new FilenameFilter() {
					@Override
					public boolean accept(File file, String name) {
						return StringUtils.isStringInStringArray(name, patients)
						&& new File(file + FS + name).isDirectory(); 
					}
				}));
				
				logger.info("Will attempt maf generation for the following " + patientsInSeqAnalysis.size() + " patients: ");
				for (File f : patientsInSeqAnalysis) logger.info(f.getName());
				
				// need to check each patient to ensure that they have a valid SNP and indel folder
				checkPatientsHaveRequiredFolders();
			}
			
			
			logger.info("no of patients with data: " + patientsAndFiles.size());
			
			loadRequiredStaticFiles();
			
			loadDCCFiles();
			
			checkAlleleFraction();
			
			writeIndividualPatientMafFiles();
			
			if (null != krasFile)
				loadKRASData();
			
			performFilter();
			
			addCpgAndGff();
			
			if ( ! containsNovelStarts()) {
				if (null != patientBamFiles && patientBamFiles.length > 0) {
					addNovelStartsMT(patientBamFiles[0]);
				}
			}
			
			writeFinalFilteredOutput();
			
		} else {
			logger.error("No patients specified - exiting");
			exitStatus = 1;
		}
		
		return exitStatus;
	}
	
	

	private boolean containsNovelStarts() {
		boolean containsNovelStarts = false;
		for (MAFRecord maf : filteredMafs) {
			if (MutationType.isSubstitution(maf.getVariantType())) {
				if (maf.getNovelStartCount() > 0)  {
					containsNovelStarts = true;
					break;
				}
			}
		}
		return containsNovelStarts;
	}
	
	private void addNovelStartsMT(String bamFilePath) throws Exception {
		logger.info("adding novel starts");
		// need a map of patients and positions
		final Map<String, List<MAFRecord>> patientsAndMafs = new HashMap<String, List<MAFRecord>>();
		
		// populate with low and high mafs
		for (MAFRecord maf : filteredMafs) {
			
			// only want to do this for snps - ignoring indels for the time being
			if (MutationType.isSubstitution(maf.getVariantType())) {
				if (maf.getNovelStartCount() != 0) continue;
				
				String patient = maf.getPatient();
				List<MAFRecord> patientMafs = patientsAndMafs.get(patient);
				if (null == patientMafs) {
					patientMafs = new ArrayList<MAFRecord>();
					patientsAndMafs.put(patient, patientMafs);
				}
				patientMafs.add(maf);
			}
		}
		
		// only proceed if we have mafs without novel start into
		if (patientsAndMafs.isEmpty()) {
			logger.info("All maf records already contain novel starts info");
			return;
		}
		
		int poolSize = 2;
		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		
		// get bam from seq_results
		for (Entry<String, List<MAFRecord>> entry : patientsAndMafs.entrySet()) {
			String patient = entry.getKey();
			logger.info("wil use bam file : " + bamFilePath + " for patient: " + patient + ", no of positions: " + entry.getValue().size());
			File bamFile = new File(bamFilePath);
			List<MAFRecord> patientMafs = entry.getValue();
			Collections.sort(patientMafs, MAF_COMPARATOR);
			executor.execute(new NovelCounter(bamFile, patientMafs));
		}
		executor.shutdown();
		
		 try {
		     // Wait a while for existing tasks to terminate
		     if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
		    	 executor.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!executor.awaitTermination(60, TimeUnit.MINUTES))
		           System.err.println("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
			   executor.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		
		logger.info("adding novel starts - DONE");
	}
	
	@Override
	void checkPatientsHaveRequiredFolders() {
		for (File f : patientsInSeqAnalysis) {
			File snpDccFile = null, indelDccFile = null;
			File[] snpDirectory = FileUtils.findFiles(f.getAbsolutePath() + SNPS_FILE_LOCATION, dateADirectoryFilter);
			
			if (snpDirectory.length == 0) {
				
				// check that prelim folder exists before searching for files within it
				if (new File(f.getAbsolutePath() +SNPS_FILE_LOCATION_PRELIM).exists()) {
					snpDirectory = FileUtils.findFiles(f.getAbsolutePath() + SNPS_FILE_LOCATION_PRELIM, dateADirectoryFilter);
				}
				if (snpDirectory.length != 1) {
					logger.info("removing patient: " + f.getName() + ", no valid snp folder");
					continue;
				}
			}
			
			// could have more than 1 snp directory - reverse sort to get the latest first
			Arrays.sort(snpDirectory, Collections.reverseOrder());
			
			logger.info("Retrieving snp file from " + snpDirectory[0].getAbsolutePath());
			
			File[] snpFiles = FileUtils.findFilesEndingWithFilter(snpDirectory[0].getAbsolutePath(), SOMATIC_SNPS);
			
			if (snpFiles.length > 0) {
				snpDccFile = FileUtils.findFilesEndingWithFilter(snpDirectory[0].getAbsolutePath(), SOMATIC_SNPS)[0];
			} else {
				logger.info("removing patient: " + f.getName() + ", no valid snp file");
				continue;
			}
			
			File[] indelDirectory = FileUtils.findFiles(f.getAbsolutePath() + INDELS_FILE_LOCATION, dateDirectoryFilter);
			
			if (indelDirectory.length == 0) {
				
				// check that prelim folder exists before searching for files within it
				if (new File(f.getAbsolutePath() + INDELS_FILE_LOCATION_PRELIM).exists()) {
					indelDirectory = FileUtils.findFiles(f.getAbsolutePath() + INDELS_FILE_LOCATION_PRELIM, dateDirectoryFilter);
				}
				if (indelDirectory.length != 1) {
					logger.info("removing patient: " + f.getName() + ", no valid indel folder");
					continue;
				}
			}
			if (ensemblV61)
				indelDccFile = FileUtils.findFilesEndingWithFilter(indelDirectory[0].getAbsolutePath() + FS + INDELS_V61_FOLDER, SOMATIC_INDELS)[0];
			else 
				indelDccFile = FileUtils.findFilesEndingWithFilter(indelDirectory[0].getAbsolutePath(), SOMATIC_INDELS)[0];
			
			// add patient and files to map
			patientsAndFiles.put(f.getName(), new Pair<File, File>(snpDccFile, indelDccFile));
		}
	}
	
	public static void main(String[] args) throws Exception {
		MafPipelineSinglePatient sp = new MafPipelineSinglePatient();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running QMafPipeline:", e);
			else System.err.println("Exception caught whilst running QMafPipeline");
			e.printStackTrace();
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
			
			entrezFile = options.getEntrezFile();
			canonicalTranscriptsFile = options.getCanonicalTranscripts();
			verificationFile = options.getVerified();
			dbSNPFile = options.getDbSNPFile();
			krasFile = options.getKrasFile();
			fastaFile = options.getFastaFile();
			gffFile = options.getGffFile();
			
			if (options.containsAlleleFraction())
				alleleFraction = options.getAlleleFraction();
			
			
			if (entrezFile == null || ! FileUtils.canFileBeRead(entrezFile))
				throw new QMafException("NO_ENTREZ_FILE_ERROR");
			
			if (null == options.getDirNames() || options.getDirNames().length < 1)
				throw new QMafException("MISSING_OUTPUT_DIRECTORY");
			
			outputDirectory = options.getDirNames()[0];
			lowCoveragePatients = options.getLowCoveragePatients();
			patients = options.getPatients();
			patientDCCFiles = options.getDccs();
			patientBamFiles = options.getBams();
			
			if (options.getNoOfBases() > 0)
				noOfBases = options.getNoOfBases();
			
			ensemblV61 = options.hasEnsembl61Option();
			
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(MafPipelineSinglePatient.class, logFile, options.getLogLevel());
			qExec = logger.logInitialExecutionStats("MafPipelineSinglePatient", MafPipelineSinglePatient.class.getPackage().getImplementationVersion(), args);
			
			logger.tool("will retrieve " + noOfBases + " on either side of position of interest from fasta file");
			logger.tool("ensemblV61: " + ensemblV61);
			logger.tool("patients: " + Arrays.deepToString(patients));
			logger.tool("patientDCCFiles: " + Arrays.deepToString(patientDCCFiles));
			logger.tool("patientBamFiles: " + Arrays.deepToString(patientBamFiles));
			if (alleleFraction > 0)	logger.tool("alleleFraction: " + alleleFraction);
			
			return engage();
		}
		return returnStatus;
	}
}
