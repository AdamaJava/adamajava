/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.FileUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

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
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		TabbedHeader header = reader.getHeader();
		
		try {
			for (String headerLine : header) {
				if (headerLine.startsWith("#PatientID"))
					patientId = headerLine.substring(headerLine.indexOf(':') +2);
			}
		} finally {
			reader.close();
		}
	}
	
	private void loadFile(String fileName) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		TabbedHeader header = reader.getHeader();
		
		// should be able to glean some useful info from the header
//		String patientId = null;
		String controlSampleID = null;
		String tumourSampleID = null;
		String tool = null;
//		DccType type = null;
		
		for (String headerLine : header) {
			if (headerLine.startsWith("#PatientID"))
				patientId = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#ControlSampleID"))
				controlSampleID = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#TumourSampleID"))
				tumourSampleID = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#Tool")) {
				tool = headerLine.substring(headerLine.indexOf(':') +2);
//				type = headerLine.endsWith("SNP") ? DccType.SNP : (headerLine.endsWith("small_indel_tool") ? DccType.INSERTION : null);
			}
		}
		logger.info("patient: " + patientId + ", controlSampleID: "  + controlSampleID + ", tumourSampleID: " + tumourSampleID + ", tool: " + tool);
		
		Map<ChrPosition, TorrentVerificationStatus> patientSpecificVerification = verifiedData.get(patientId);
		
		try {
			int count = 0;
			for (TabbedRecord rec : reader) {
				if (++count ==1) continue;	// header line
				
				MafUtils.convertDccToMaf(rec, patientId, controlSampleID, tumourSampleID, patientSpecificVerification, mafs, ensemblToEntrez);
//				convertDccToMaf(rec, patientId, controlSampleID, tumourSampleID, type);
			}
			logger.info("ignored " + ignoredCount + " dcc records");
		} finally {
			reader.close();
		}
	}
	
//	private void convertDccToMaf(TabbedRecord tabbedRecord, String patientId, String controlSampleID, String tumourSampleID, DccType type) {
////		String[] params = tabbedPattern.split(tabbedRecord.getData(), -1);
//		String[] params = TabTokenizer.tokenize(tabbedRecord.getData());
//		
//		// if we have 2 entries (pipe delimited) in the Hugo symbol field - create 2 maf records - 1 per gene
////		String [] genes = getHugoSymbol(params[30]).split("\\|");
//		String [] genes = MafUtils.getHugoSymbol(params[30]).split("\\|");
//		String [] geneIds = params[26].split("\\|");
//		String [] transcriptIds = params[27].split("\\|");
//		
//		// check if position verified
//		String chromosome = params[2];
//		int startPosition = Integer.parseInt(params[3]);
//		int endPosition = Integer.parseInt(params[4]);
//		TorrentVerificationStatus tvs = null;
//		if (null != verifiedData.get(patientId))
//			tvs = verifiedData.get(patientId).get(new ChrPosition("chr"+chromosome, startPosition, endPosition));
////		Boolean verified = verifiedData.get(new ChrPosition("chr"+chromosome, startPosition, endPosition));
//		// if it didn't verify - ignore!!!
////		if (null != tvs && tvs.removeFromMaf()) {
////			logger.info("position did not verify: " + chromosome + ":" + startPosition + "-" + endPosition + ", ignoring");
////			ignoredCount++;
////			return;
////		}
//		
//		// setup maf record with static and common fields
//		MAFRecord maf = new MAFRecord();
//		MafUtils.setupStaticMafFields(maf, patientId, controlSampleID, tumourSampleID);
//		
//		// use M rather than MT
//		String chr = chromosome.substring(chromosome.indexOf("chr") + 1);
//		if ("MT".equals(chr)) chr = "M";
//		
//		maf.setChromosome(chr);
//		maf.setStartPosition(startPosition);
//		maf.setEndPosition(endPosition);
//		maf.setStrand(Integer.parseInt(params[5]) == 1 ? '+' : '-');	// set this according to 1 or 0
//		maf.setRef(params[8]);
//		maf.setTumourAllele1(params[10].substring(0, params[10].indexOf('/')));
//		maf.setTumourAllele2(params[10].substring(params[10].indexOf('/')+1));
////		maf.setDbSnpId(getDbSnpId(params[18]));
//		maf.setDbSnpId(MafUtils.getDbSnpId(params[18]));
//		
//		//FIXME - take this out once test for Karin is complete
////		maf.setValidationStatus((null != tvs && tvs.verified()) ? "Valid" : "Unknown");
//		if (null != tvs) {
//			maf.setValidationStatus(tvs.getMafDisplayName());
//			
////			if (tvs.verified()) {
////				maf.setValidationStatus("Valid");
////			} else {
////				if (TorrentVerificationStatus.COVERAGE.equals(tvs))
////					maf.setValidationStatus("Coverage");
////				else 
////					maf.setValidationStatus("False");
////			}
//		} else  {
//			maf.setValidationStatus("Unknown");
//		}
//		
//		
//		
//		// qcmg specific
//		maf.setFlag(params[36]);		// QCMGFlag field
//		maf.setNd(params[20]);		// ND field
//		maf.setTd(params[21]);		// TD field
//		
//		// normal doesn't always exist for somatic...
//		if ("--".equals(params[9]) || "-/-".equals(params[9])) {
//			maf.setNormalAllele1("-");
//			maf.setNormalAllele2("-");
//		} else {
//			maf.setNormalAllele1(params[9].substring(0, params[9].indexOf('/')));
//			maf.setNormalAllele2(params[9].substring(params[9].indexOf('/')+1));
//		}
//		
//		if (DccType.SNP == type) {
//			maf.setVariantType("SNP");
//		} else if (DccType.INDEL == type){
//			maf.setVariantType(Integer.parseInt(params[1]) == 2 ? "INS" : (Integer.parseInt(params[1]) == 3 ? "DEL" : "???"));
//		}
//		
//		if (canonicalMafMode) {
//			canonicalTranscript(type, params, genes, geneIds, transcriptIds, maf);
//		} else {
//			worstCaseTranscript(type, params, genes, geneIds, transcriptIds, maf);
//		}
//		
//		
//		// need to check that there is a valid gene set on the Maf object
//		// if not - don't add to collection
//		
//		if (null != maf.getHugoSymbol())
//			mafs.add(maf);
//	}

//	private void canonicalTranscript(DccType type, String[] params,
//			String[] genes, String[] geneIds, String[] transcriptIds,
//			MAFRecord maf) {
//		int i = 0, allTranscriptIdCount = 0;
//		for (String gene : genes) {
//			String[] geneSpecificTranscriptIds =  transcriptIds[i].split(",");
//			String geneId = geneIds[i++];
//			
//			// get canonical transcript id
////			String canonicalTranscripId = getCanonicalTranscript(geneId);
//			String canonicalTranscripId = ensemblGeneToCanonicalTranscript.get(geneId);
//			maf.addCanonicalTranscriptId(canonicalTranscripId);
//			if (null != canonicalTranscripId) {
//				int positionInTranscripts = StringUtils.getPositionOfStringInArray(geneSpecificTranscriptIds, canonicalTranscripId, true);
//				String [] consequences = params[22].split(",");
//				String [] aaChanges = params[23].split(",");
//				String [] baseChanges = params[24].split(",");
//				
//				//TODO what to do if canonical transcript id is not found!!
//				
//				if (positionInTranscripts > -1) {
//					// we have a matching canonical transcript
//					positionInTranscripts += allTranscriptIdCount;
//					
//					if (consequences.length > positionInTranscripts) {
//						String dccConseq = DccConsequence.getMafName(consequences[positionInTranscripts], type, Integer.parseInt(params[1]));
//						
//						if ( ! DccConsequence.passesMafNameFilter(dccConseq)) {
//							continue;
//						}
//						
//						maf.addVariantClassification(dccConseq);
//						maf.addCanonicalAAChange(aaChanges[positionInTranscripts]);
//						maf.addCanonicalBaseChange(baseChanges[positionInTranscripts]);
//					} else {
//						logger.info("consequences.length is <= positionInTranscripts");
//					}
//				} else {
//					missingCanonicalTransId++;
//					logger.debug("canonical transcript id not found in transcript id array");
//					
//					// don't want to record this gene 
//					continue;
//				}
//				allTranscriptIdCount += geneSpecificTranscriptIds.length;
//				
//				// set the alternate transcriptId field to be all the other transcripts
//				int position = 0;
//				for (String transId : geneSpecificTranscriptIds) {
//					if ( ! canonicalTranscripId.equalsIgnoreCase(transId)) {
//						maf.setAlternateTranscriptId(StringUtils.isNullOrEmpty(maf.getAlternateTranscriptId()) 
//								? transId : maf.getAlternateTranscriptId() + (position == 0 ? ";" : ", ") + transId);
//						// also alternate aa change & base change
//						maf.setAlternateAAChange(StringUtils.isNullOrEmpty(maf.getAlternateAAChange()) 
//								? aaChanges[position] : maf.getAlternateAAChange() +  (position == 0 ? ";" : ", ") + aaChanges[position]);
//						maf.setAlternateBaseChange(StringUtils.isNullOrEmpty(maf.getAlternateBaseChange()) 
//								? baseChanges[position] : maf.getAlternateBaseChange() +  (position == 0 ? ";" : ", ") + baseChanges[position]);
//					}
//					position++;
//				}
//				
//			} else {
//				// still want to keep the transcript count up to date 
//				allTranscriptIdCount += geneSpecificTranscriptIds.length;
//				maf.addVariantClassification(DccConsequence.getMafName(params[22], type, Integer.parseInt(params[1])));
//			}
//			
////			maf.addEntrezGeneId(getEntrezId(geneId));
//			maf.addEntrezGeneId(MafUtils.getEntrezId(geneId, ensemblToEntrez));
////			maf.addHugoSymbol("Unknown".equals(gene) ? getHugoSymbol(geneId) : gene);
//			maf.addHugoSymbol("Unknown".equals(gene) ? MafUtils.getHugoSymbol(geneId) : gene);
//		}
//	}
	
//	private void worstCaseTranscript(DccType type, String[] params, String[] genes, String[] geneIds, String[] transcriptIds, MAFRecord maf) {
//		int i = 0, allTranscriptIdCount = 0;
//		for (String gene : genes) {
//			String[] geneSpecificTranscriptIds =  transcriptIds[i].split(",");
//			String geneId = geneIds[i++];
//			
//			
//			String [] allTranscripts = params[27].split("[,|]");
//			// need start and stop positions of transcripts belonging to this gene so that the relevant consequences can be retrieved
//			int startPosition = StringUtils.getPositionOfStringInArray(allTranscripts, geneSpecificTranscriptIds[0], true);
//			int endPosition = StringUtils.getPositionOfStringInArray(allTranscripts, geneSpecificTranscriptIds[geneSpecificTranscriptIds.length -1], true);
//			
//			String [] consequences = params[22].split(",");
//			
//			String [] geneConsequences = new String[1 + endPosition-startPosition];
//			for (int j = startPosition , k = 0; j <= endPosition ; j++, k++) {
//				geneConsequences[k] = consequences[j];
//			}
//			
//			
//			String worstCaseConsequence = DccConsequence.getWorstCaseConsequence(type, geneConsequences);
//			String dccConseq = DccConsequence.getMafName(worstCaseConsequence, type, Integer.parseInt(params[1]));
//			if ( ! DccConsequence.passesMafNameFilter(dccConseq)) {
//				continue;
//			}
//			
//			maf.addVariantClassification(dccConseq);
//			
//			
//			int currentPosition = 0;
//			for (String c : geneConsequences) {
//				if (c.equals(worstCaseConsequence)) break;
//				currentPosition++;
//			}
//			
//			String [] aaChanges = params[23].split(",");
//			String [] baseChanges = params[24].split(",");
//			String worstCaseTranscriptId = geneSpecificTranscriptIds[currentPosition];
//			maf.addCanonicalTranscriptId(worstCaseTranscriptId);
//			maf.addCanonicalAAChange(aaChanges[currentPosition]);
//			maf.addCanonicalBaseChange(baseChanges[currentPosition]);
//			
//			
//			// get position of worstCaseConsequence in 
//			
//			// get canonical transcript id
////			String canonicalTranscripId = getCanonicalTranscript(geneId);
////			if (null != worstCaseTranscriptId) {
//////				int positionInTranscripts = StringUtils.getPositionOfStringInArray(geneSpecificTranscriptIds, worstCaseTranscriptId, true);
////				
////				//TODO what to do if canonical transcript id is not found!!
////				
////				if (currentPosition > -1) {
////					// we have a matching canonical transcript
////					positionInTranscripts += allTranscriptIdCount;
////					
////					if (consequences.length > positionInTranscripts) {
////						String dccConseq = DccConsequence.getMafName(worstCaseConsequence, type, Integer.parseInt(params[1]));
////						
////						if ( ! DccConsequence.passesMafNameFilter(dccConseq)) {
////							continue;
////						}
////						
////						maf.addVariantClassification(dccConseq);
////					} else {
////						logger.info("consequences.length is <= positionInTranscripts");
////					}
////				} else {
////					missingCanonicalTransId++;
////					logger.debug("canonical transcript id not found in transcript id array");
////					
////					// don't want to record this gene 
////					continue;
////				}
////				allTranscriptIdCount += geneSpecificTranscriptIds.length;
////				
////				// set the alternate transcriptId field to be all the other transcripts
////				int position = 0;
////				for (String transId : geneSpecificTranscriptIds) {
////					if ( ! canonicalTranscripId.equalsIgnoreCase(transId)) {
////						maf.setAlternateTranscriptId(StringUtils.isNullOrEmpty(maf.getAlternateTranscriptId()) 
////								? transId : maf.getAlternateTranscriptId() + (position == 0 ? ";" : ", ") + transId);
////						// also alternate aa change & base change
////						maf.setAlternateAAChange(StringUtils.isNullOrEmpty(maf.getAlternateAAChange()) 
////								? aaChanges[position] : maf.getAlternateAAChange() +  (position == 0 ? ";" : ", ") + aaChanges[position]);
////						maf.setAlternateBaseChange(StringUtils.isNullOrEmpty(maf.getAlternateBaseChange()) 
////								? baseChanges[position] : maf.getAlternateBaseChange() +  (position == 0 ? ";" : ", ") + baseChanges[position]);
////					}
////					position++;
////				}
//				
////			} else {
////				// still want to keep the transcript count up to date 
////				allTranscriptIdCount += geneSpecificTranscriptIds.length;
////				maf.addVariantClassification(DccConsequence.getMafName(params[22], type, Integer.parseInt(params[1])));
////			}
//			
////			maf.addEntrezGeneId(getEntrezId(geneId));
//			maf.addEntrezGeneId(MafUtils.getEntrezId(geneId, ensemblToEntrez));
////			maf.addHugoSymbol("Unknown".equals(gene) ? getHugoSymbol(geneId) : gene);
//			maf.addHugoSymbol("Unknown".equals(gene) ? MafUtils.getHugoSymbol(geneId) : gene);
//		}
//	}
	
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
