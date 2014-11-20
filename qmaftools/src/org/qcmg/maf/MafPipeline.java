/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public abstract class MafPipeline {
	
	////////////////////
	// constants
	////////////////////
	public static final String datePattern = "[0-9]{8}";
	public static final String dateAPattern = "[0-9]{8}_A";
	public static final String patientPattern = "APGI_[0-9]{4}";
	
	static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	static final String FS = FileUtils.FILE_SEPARATOR;
	
	static final String SEQ_FINAL = "seq_final";
	static final String SEQ_ANALYSIS = "/panfs/seq_analysis/icgc_pancreatic";
	static final String SOMATIC_SNPS = "_somatic_SNPs";
	static final String SOMATIC_SNPS_V61 = "_somatic_SNPs_v61";
	static final String SOMATIC_INDELS = "_somatic_indels";
	static final String SNPS_FILE_LOCATION = FS + MutationType.SNP + FS + "qSNP";
	static final String SNPS_FILE_LOCATION_PRELIM = SNPS_FILE_LOCATION + FS + "prelim";
	static final String INDELS_FILE_LOCATION = FS + "indels" + FS + "small_indel_tool";
	static final String INDELS_FILE_LOCATION_PRELIM = INDELS_FILE_LOCATION + FS + "prelim";
	static final String INDELS_V61_FOLDER = "Ens61";
	
	static final String SEQ_RESULTS = "/mnt/seq_results/icgc_pancreatic";
	static final String SEQ_RESULTS_BAM_EXT = ".exome.TD.bam";
	
	static QLogger logger;
	String logFile;
	String[] cmdLineOutputFiles;
	
	//////////////////
	// input files
	//////////////////
	String entrezFile;
	String canonicalTranscriptsFile;
	String verificationFile;
	String dbSNPFile;
	String krasFile;
	String fastaFile;
	String gffFile;
	
	////////////////////
	// output files
	////////////////////
	String outputDirectory;
	
	////////////////////
	// others
	////////////////////
	String[] lowCoveragePatients;
	int noOfBases = 5;
	boolean ensemblV61;
	int exitStatus;
	
	int alleleFraction;
	
	QExec qExec;
	
	
	////////////////////
	// annonymous inner classes
	////////////////////
	public final static Comparator<MAFRecord> MAF_COMPARATOR = new Comparator<MAFRecord>() {
		@Override
		public int compare(MAFRecord maf0, MAFRecord maf1) {
			if (isNumeric(maf0.getChromosome().charAt(0)) && isNumeric(maf1.getChromosome().charAt(0)))  {
				int chrDiff = Integer.valueOf(maf0.getChromosome()).compareTo(Integer.valueOf(maf1.getChromosome()));
				if (chrDiff != 0) 
					return chrDiff;
			} else {
				int chrDiff = maf0.getChromosome().compareTo(maf1.getChromosome());
				if (chrDiff != 0) 
					return chrDiff;
			}
			return maf0.getStartPosition() - maf1.getStartPosition();
		}
		private boolean isNumeric(char ch) {
		    return ch >= '0' && ch <= '9';
		  }
	};
	
	public final static FilenameFilter dateDirectoryFilter =  new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.matches(datePattern)
			&& new File(dir + FS + name).isDirectory(); 
		}
	};
	public final static FilenameFilter dateADirectoryFilter =  new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.matches(dateAPattern)
			&& new File(dir + FS + name).isDirectory(); 
		}
	};
	public static final FilenameFilter patientDirectoryFilter = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return name.startsWith("APGI_") && Character.isDigit(name.charAt(5))
			&& new File(file + FS + name).isDirectory(); 
		}
	};
	
	
	// NOTE that collection initial capacities are based on data as of 14/11/2011
	// should be reviewed periodically if performance is an issue
	
	////////////////////
	// collections
	////////////////////
	 List<File> patientsInSeqAnalysis = new ArrayList<File>(200);
	 Map<String, Pair<File, File>> patientsAndFiles = new HashMap<String, Pair<File,File>>(100);
	Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>(40000, 0.99f);
	Map<String, String> ensemblGeneToCanonicalTranscript = new HashMap<String, String>();
	// each patient (that has verification data) has a map of ChrPos and TVS
	Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedData = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>(10000);
	List<MAFRecord> mafs = new ArrayList<MAFRecord>(250000);
	List<MAFRecord> filteredMafs = new ArrayList<MAFRecord>(25000);
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public MafPipeline () {
		logger = QLoggerFactory.getLogger(getClass());
	}
	
	public static boolean passesFinalFilter(MAFRecord maf) {
		// keep all KRAS
		if ("KRAS".equals(maf.getHugoSymbol())) return true;
		
		// if its a SNP, then it should have novel count info
		if (MutationType.isSubstitution(maf.getVariantType())) {
			// needs to have 4 or more novels
			return maf.getNovelStartCount() >= 4;
		} else {
			// if its an indel, doesn't have novel count info so return true
			return true;
		}
	}
	
//	double  qualityControlCheck(List<MAFRecord> mafs, boolean isHighConf) {
//		int totalCount = mafs.size();
//		if (totalCount > 0) {
//			int rsIdCount = 0;
//			
//			for (MAFRecord maf : mafs) {
//				if ( ! StringUtils.isNullOrEmpty(maf.getDbSnpId()) && maf.getDbSnpId().startsWith("rs")) {
//					rsIdCount++;
//				}
//			}
//			
//			double rsIdPercentage = ((double)rsIdCount / totalCount) * 100;
//			logger.info((isHighConf ? "High" : "Low") +" Confidence: total number: " + totalCount + ", number with rs ids: " + rsIdCount + ", (" + rsIdPercentage + "%)");
//			
//			if (rsIdPercentage > 10.0) {
//				logger.warn("Percentage of mafs with rs ids exceeds 10%!");
//				
//				// email info team
//				// EmailUtils.sendEmail();
//			}
//			return rsIdPercentage;
//		}
//		return 0;
//	}
	
	void writeFinalFilteredOutput() throws IOException {
		
		// get lists of high and low conf mafs
		List<MAFRecord> highConfMafs = new ArrayList<MAFRecord>();
		List<MAFRecord> lowConfMafs = new ArrayList<MAFRecord>();
		String patient = null;
		boolean singlePatient = true;
		for (MAFRecord maf : filteredMafs) {
			if (null == patient) patient = maf.getPatient();
			if ( ! maf.getPatient().equals(patient)) singlePatient = false;
			if (maf.isHighConf() && passesFinalFilter(maf)) {
				highConfMafs.add(maf);
				continue;
			}
			if (maf.isLowConf() && passesFinalFilter(maf)) {
				lowConfMafs.add(maf);
				continue;
			}
		}
		
		String [] typeAndMeta = getDccMetaData(patient);
		
		if ( ! highConfMafs.isEmpty()) {
			MafUtils.writeMafOutput(outputDirectory + FS +  (singlePatient ? patient + "." : "") + typeAndMeta[0] + ".HighConfidence.maf", highConfMafs, 
					typeAndMeta[1] + MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS, true);
			
			Iterator<MAFRecord> iter = highConfMafs.iterator();
			while (iter.hasNext()) {
				MAFRecord maf = iter.next();
				if ( ! DccConsequence.passesMafNameFilter(maf.getVariantClassification())) iter.remove();
			}
			
			MafUtils.writeMafOutput(outputDirectory + FS +  (singlePatient ? patient + "." : "") + typeAndMeta[0] + ".HighConfidenceConsequence.maf", highConfMafs, 
					typeAndMeta[1] + MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS, true);
		} else {
			logger.warn("No high confidence data to write to file");
		}
		if ( ! lowConfMafs.isEmpty()) {
			MafUtils.writeMafOutput(outputDirectory + FS +  (singlePatient ? patient + "." : "") + typeAndMeta[0] + ".LowConfidence.maf", lowConfMafs, 
					typeAndMeta[1] + MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS, true);
			
			Iterator<MAFRecord> iter = lowConfMafs.iterator();
			while (iter.hasNext()) {
				MAFRecord maf = iter.next();
				if ( ! DccConsequence.passesMafNameFilter(maf.getVariantClassification())) iter.remove();
			}
			
			MafUtils.writeMafOutput(outputDirectory + FS +  (singlePatient ? patient + "." : "") +  typeAndMeta[0] + ".LowConfidenceConsequence.maf", lowConfMafs, 
					typeAndMeta[1] + MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS, true);
		} else {
			logger.warn("No low confidence data to write to file");
		}
	}

	protected String[] getDccMetaData(String patient) throws IOException {
		// get file - get dcc meta info from file, and prepend to header
		Pair<File, File> filePair = patientsAndFiles.get(patient);
		// try snp one first, then indel
		TabbedHeader header = null;
		String somGerm = "";
		if (filePair.getLeft() != null) {
			
			File f = filePair.getLeft();
			somGerm = f.getAbsolutePath().contains("Somatic") ? "Somatic" :  f.getAbsolutePath().contains("Germline") ? "Germline" : "";
			TabbedFileReader reader = null;
			try {
				reader = new TabbedFileReader(f);
				header = reader.getHeader();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				reader.close();
			}
		} else if (filePair.getRight() != null) {
			File f = filePair.getRight();
			somGerm = f.getAbsolutePath().contains("Somatic") ? "Somatic" :  f.getAbsolutePath().contains("Germline") ? "Germline" : "";
			TabbedFileReader reader = null;
			try {
				reader = new TabbedFileReader(f);
				header = reader.getHeader();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				reader.close();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (null != header) {
			for (String s : header)  {
				// ignore qexec
				if ( ! s.startsWith("#Q_EXEC"))
					sb.append(s).append("\n");
			}
		}
		String dccMetaInfo = qExec.getExecMetaDataToString();
		dccMetaInfo += sb.length() > 0 ?  sb.toString() : "";
		
		return new String[] {somGerm, dccMetaInfo};
	}
//	void writeFinalPREFilteredOutput() throws IOException {
//		
//		// get lists of high and low conf mafs
//		List<MAFRecord> highConfMafs = new ArrayList<MAFRecord>();
//		List<MAFRecord> lowConfMafs = new ArrayList<MAFRecord>();
//		
//		for (MAFRecord maf : filteredMafs) {
//			if (maf.isHighConf()) {
//				highConfMafs.add(maf);
//				continue;
//			}
//			if (maf.isLowConf()) {
//				lowConfMafs.add(maf);
//				continue;
//			}
//		}
//		
//		MafUtils.writeMafOutput(outputDirectory + FS +  "highConfidencePreFilter.maf", highConfMafs, MafUtils.HEADER_WITH_CONFIDENCE_CPG, true);
//		MafUtils.writeMafOutput(outputDirectory + FS +  "lowConfidencePreFilter.maf", lowConfMafs, MafUtils.HEADER_WITH_CONFIDENCE_CPG, true);
//	}
	
	void addNovelStartsMT(String bamFilePathPart1, String bamFilePathPart2, String bamFilePattern) throws Exception {
		logger.info("adding novel starts");
		// need a map of patients and positions
		final Map<String, List<MAFRecord>> patientsAndMafs = new HashMap<String, List<MAFRecord>>();
		
		// populate with low and high mafs
		for (MAFRecord maf : filteredMafs) {
			
			// only want to do this for snps - ignoring indels for the time being
			if (MutationType.isSubstitution(maf.getVariantType())) {
				
				// get patient from tumour QCMG-66-APGI_2049-ICGC-ABMJ-20100729-08-TD string
				String patient = maf.getTumourSampleBarcode().substring(8, 17);
				List<MAFRecord> patientMafs = patientsAndMafs.get(patient);
				if (null == patientMafs) {
					patientMafs = new ArrayList<MAFRecord>();
					patientsAndMafs.put(patient, patientMafs);
				}
				patientMafs.add(maf);
			}
		}
		
//		CountDownLatch latch = new CountDownLatch(100);
		int poolSize = 2;
		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		
		// get bam from seq_results
		for (Entry<String, List<MAFRecord>> entry : patientsAndMafs.entrySet()) {
			String patient = entry.getKey();
			String bamPath = bamFilePathPart1 + FS + patient + FS + bamFilePathPart2;
			logger.info("looking for files in : " +  bamPath);
			File [] files = FileUtils.findFilesEndingWithFilter(bamPath, bamFilePattern);
			if (null == files || files.length < 1) {
				logger.warn("Could not find any matching bam files in bamPath: " + bamPath + " that match pattern: " + bamFilePattern);
				continue;
			}
			File bamFile = files[0];
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
	
	static class NovelCounter implements Runnable {
		private final File bamFile;
		private final List<MAFRecord> ncMafs;
		
		NovelCounter(File bamFile, List<MAFRecord> mafs) {
			this.bamFile = bamFile;
			this.ncMafs = mafs;
		}

		@Override
		public void run() {
			logger.info("thread starting");
			long start = System.nanoTime();
			long elapsedTime = 0;
			
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
			// if we have a small no of positions, no need to cache
			reader.enableIndexCaching(ncMafs.size() < 10);
			
			int noOfRecordsRetrievedForPatient = 0, noOfPositionsRetrievedForPatient = 0, positionsWithDeletions = 0;
			
			try {
				for (MAFRecord maf : ncMafs) {
					noOfPositionsRetrievedForPatient++;
					String chr = MafUtils.getFullChromosome(maf);
//					List<SAMRecord> records = qj.getRecordsAtPosition(chr, maf.getStartPosition());
					SAMRecordIterator records = reader.queryOverlapping(chr, maf.getStartPosition(), maf.getStartPosition());
//					noOfRecordsRetrievedForPatient += records.size();
//					logger.info("pos: " + maf.getChromosome() + "-" + maf.getStartPosition() + ": " + records.size());
					
//					char[] bases = new char[records.size()];
					char[] novelStartBases = new char[1024];	// hmmmmm
					Set<Integer> forwardStrand = new HashSet<Integer>();
					Set<Integer> reverseStrand = new HashSet<Integer>();
					int j = 0;
					
					while (records.hasNext()) {
						noOfRecordsRetrievedForPatient++;
						SAMRecord sam = records.next();
						if (sam.getDuplicateReadFlag()) continue;
						if (sam.getMappingQuality() < 10) continue;
						
						int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, maf.getStartPosition());
						
						if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
							
							if (sam.getBaseQualities()[indexInRead] < 10) continue;
							
							char c = sam.getReadString().charAt(indexInRead);
//							bases[i++] = c;
							// novel start count
							
							final char var = MafUtils.getVariant(maf).charAt(0);
							
							// only care about variant novel starts
							if (var == c) {
							
								if (sam.getReadNegativeStrandFlag()) {
									if (reverseStrand.add(sam.getAlignmentStart())) {
										novelStartBases[j++] = c;
									}
								} else {
									if (forwardStrand.add(sam.getAlignmentStart())) {
										novelStartBases[j++] = c;
									}
								}
							}
						} else {
							positionsWithDeletions++;
//							logger.info("help!!");
						}
					}
					records.close();
					
					// count the number of times the variant appears in the novelstartbases[]
					maf.setNovelStartBases(new String(novelStartBases));
					int count = 0;
					final char var = MafUtils.getVariant(maf).charAt(0);
					for (char c : novelStartBases) {
						if (c == var) count++;
					}
					maf.setNovelStartCount(count);
				}
				elapsedTime = (System.nanoTime() - start) / 1000000000;	// nanoseconds
				logger.info("bamfile: " + bamFile.getName() + ", positions queried: " + noOfPositionsRetrievedForPatient 
						+ " (" + ((double)noOfPositionsRetrievedForPatient / elapsedTime) 
						+ " per sec), records at positions: " + noOfRecordsRetrievedForPatient 
						+ " ("  + ((double)noOfRecordsRetrievedForPatient / elapsedTime)
						+ " per sec), deletions (possibly): " + positionsWithDeletions);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} finally {
//					latch.countDown();
					logger.info("thread finishing, elapsedTime: " + elapsedTime);
				}
//			}
			}
		}
	}

	void addCpgAndGff() throws Exception {
		logger.info("adding cpg and gff");
		
		Map<String, Map<ChrPosition, String>> gffs = new HashMap<String, Map<ChrPosition, String>>();
		Map<ChrPosition, String> cpgs = new TreeMap<ChrPosition, String>();
		
		
		for (MAFRecord maf : filteredMafs) {
			String chr = MafUtils.getFullChromosome(maf);
			ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
			if (null == maf.getCpg())
				cpgs.put(cp, null);
			
			Map<ChrPosition, String> m = gffs.get(chr);
			if (null == m) {
				m = new HashMap<ChrPosition, String>();
				gffs.put(chr, m);
			}
			m.put(cp, null);
				
		}
		logger.info("no of entries in positionsOfInterestMap: " + gffs.size());
		for (Entry<String, Map<ChrPosition, String>> entry : gffs.entrySet()) {
			logger.info("no of ChrPos for chr: " + entry.getKey() + " = " + entry.getValue().size());
		}
		
		// query gff3 file
		// if file is null, skip this stage
		if ( ! StringUtils.isNullOrEmpty(gffFile) && ! gffs.isEmpty()) {
			logger.info("number of records requiring gff data: " + gffs.size());
			GFF3FileReader reader = new GFF3FileReader(new File(gffFile));
	//		Map<String, Map<ChrPosition, String>> gffTypes = new HashMap<String, Map<ChrPosition, String>>();
			try {
				int  count = 0, updatedCount  = 0;
				for (GFF3Record rec : reader) {
					String chr = rec.getSeqId();
					
					ChrPosition cp = new ChrPosition(chr, rec.getStart(), rec.getEnd());
					
					// loop through positionsOfInterestMap, and if we have an overlap, update our gff type
					// this could take a while
					Map<ChrPosition, String> relevantMap = gffs.get(chr);
					if (null != relevantMap) {
						for (Entry<ChrPosition, String> entry : relevantMap.entrySet()) {
							if (ChrPositionUtils.doChrPositionsOverlap(cp, entry.getKey())) {
								String gff = entry.getValue();
								if (StringUtils.isNullOrEmpty(gff)) {
									gff = rec.getType();
								} else {
									gff += ";" + rec.getType();
								}
								entry.setValue(gff);
								updatedCount++;
								
								// single position - won't have multiple gff3 regions
								if (entry.getKey().isSinglePoint()) break;
							}
						}
					}
	//				thisMap.put(new ChrPosition(chr, rec.getStart(), rec.getEnd()), rec.getType());
					if (++count % 1000000 == 0) logger.info("hit " + count + " records");
				}
				logger.info("no of entries with coresponding gff data: " + updatedCount);
			} finally {
				reader.close();
			}
		}
		
		if ( ! StringUtils.isNullOrEmpty(fastaFile) && ! cpgs.isEmpty()) {
			logger.info("number of records requiring cpg data: " + cpgs.size());
			// query fasta file
			IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(new File(fastaFile));
			for (Entry<ChrPosition, String> entry : cpgs.entrySet()) {
				ChrPosition cp = entry.getKey();
				ReferenceSequence seq = fasta.getSubsequenceAt(cp.getChromosome(), (cp.getPosition() - noOfBases), (cp.getEndPosition() +  noOfBases));
				entry.setValue(new String(seq.getBases()));
			}
		}
		
		// and finally, populate the mafs
		int updatedGFF = 0, updatedCPG = 0;
		for (MAFRecord maf : filteredMafs) {
				
			String chr = MafUtils.getFullChromosome(maf);
			ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
			
			String cpg =cpgs.get(cp);
			if (null != cpg) {
				maf.setCpg(cpg);
				updatedCPG++;
			}
			String gffBait = gffs.get(chr).get(cp);
			if (null != gffBait) {
				maf.setGffBait(gffBait);
				updatedGFF++;
			}
		}
		logger.info("no of mafs updated with CPG data: " + updatedCPG + ", gff data: " + updatedGFF);
		logger.info("adding cpg and gff- DONE");
	}

	void loadKRASData() throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(krasFile));
		try {
			int count = 0, validCount = 0, alreadyPresent = 0, alreadyPresentSameVerification = 0;
			
			for (TabbedRecord rec : reader) {
				count++;
				String[] params = tabbedPattern.split(rec.getData(), -1);
				String chr = params[4];
				String position = params[5];
				String id = params[15];
				String verification = params[24];
				
				// if maf position verifies, put it straight away into high conf file
				if ("Valid".equals(verification)) {
					// should all be valids
					validCount++;
					// skip entries from low coverage patients
					boolean lowCov = false;
					if (null != lowCoveragePatients) {
						for (String lowCovPatient : lowCoveragePatients) {
						// APGI_2270, APGI_2271, APGI_2285
						
							if (id.contains(lowCovPatient)) {
								lowCov = true;
								logger.info("Skipping KRAS record: " + rec.getData() + " - belongs to low coverage patient");
								continue;
							}
						}
					}
					if (lowCov) continue;
					
					// loop through mafs, if we have a match, update the validation field (if required)
					boolean positionAlreadyExists = false;
					for (MAFRecord maf : mafs) {
						if (id.equals(maf.getTumourSampleBarcode())
								&& chr.equals(maf.getChromosome())
								&& Integer.parseInt(position) == maf.getStartPosition()) {
							
							alreadyPresent++;
							
							if (verification.equals(maf.getValidationStatus())) {
								alreadyPresentSameVerification++;
								logger.info("verification matches!");
							} else {
								logger.info("updating validation from: " + maf.getValidationStatus() + " to " + verification);
								maf.setValidationStatus(verification);
							}
							positionAlreadyExists = true;
							break;
						}
					}
					if ( ! positionAlreadyExists) {
						// need to add KRAS position to list of mafs
						MAFRecord krasMaf = MafUtils.convertKRASToMaf(params);
						// novel count for these may well be low so 
						krasMaf.setNovelStartCount(999);
						mafs.add(krasMaf);
					}
				} else {
					logger.info("KRAS data that did not verify");
				}
			}
			logger.info("KRAS file - count: " + count + ", validCount: " + validCount + ", alreadyPresent: " + alreadyPresent + ", alreadyPresentSameVerification: " + alreadyPresentSameVerification);
		} finally {
			reader.close();
		}
	}
	
	void checkAlleleFraction() {
		if (alleleFraction > 0) {
			logger.info("will check for allele fractions of " + alleleFraction + " or lower");
			
			for (MAFRecord maf : mafs) {
				
				String TD = maf.getTd();
				String ref = maf.getRef();
				String tumourAllele1 = maf.getTumourAllele1();
				String tumourAllele2 = maf.getTumourAllele2();
				
				int tumourRefCount = SnpUtils.getCountFromNucleotideString(TD, ref);
				int tumourAltCount = 0;
				
				if (ref.equals(tumourAllele1)) {
					tumourAltCount = SnpUtils.getCountFromNucleotideString(TD, tumourAllele2);
				} else if (ref.equals(tumourAllele2)) {
					tumourAltCount = SnpUtils.getCountFromNucleotideString(TD, tumourAllele1);
				} else {
					logger.info("Neither tumour alleles equals ref. allele1: " + tumourAllele1 + ", allele2: " + tumourAllele2 + ", ref: "  + ref);
				}
				
				if (tumourAltCount > 0 && tumourRefCount > 0) {
					double observedAllelicFraction = ((double) tumourAltCount / tumourRefCount) * 100;
					if (observedAllelicFraction <= alleleFraction) {
						logger.info("observedAllelicFraction <= alleleFraction : " + observedAllelicFraction + ", " + alleleFraction + ", tumourAltCount: " + tumourAltCount + ", tumourRefCount: " + tumourRefCount);
						
						String flag = maf.getFlag();
						if (SnpUtils.PASS.equals(flag)) {
							// set to AF + alleleFraction
							maf.setFlag(SnpUtils.ALLELIC_FRACTION + alleleFraction);
						} else if (null != flag) {
							// append to flag
							maf.setFlag(flag + ";" + SnpUtils.ALLELIC_FRACTION + alleleFraction);
						}
					}
				}
			}
			
		} else {
			logger.info("No allele fraction set - skipping allele fraction annotation");
		}
	}

	void performFilter() {
		logger.info("filtering");
		// loop through all mafs, marking the ranking field as high or low if the relevant filters are passed
		int high = 0, lower = 0, fail = 0;
		for(MAFRecord maf : mafs) {
			
			boolean novelDbSnp = "novel".equals(maf.getDbSnpId());
//			String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
			char alt = MafUtils.getVariant(maf).charAt(0);
			
			// if maf position verifies, put it straight away into high conf file
			if ("Valid".equals(maf.getValidationStatus())) {
				high++;
				String confidence = null;
				if (DccConsequence.passesMafNameFilter(maf.getVariantClassification())) {
					if (MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), novelDbSnp, alt)) {
						confidence = "high";
					} else if (MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt)) {
						confidence = "low";
					} else {
						confidence = "other";
					}
				} else {
					confidence = "other";
				}
//				logger.info("setting confidence to be: " + confidence);
//				maf.setConfidence(confidence);
				maf.setRanking("high");
				
			} else if (MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(),novelDbSnp , alt)) {
				high++;
				maf.setRanking("high");
			} else if (MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt)) {
				lower++;
				maf.setRanking("low");
			} else {
				logger.debug("Failed on remaining checks. Flag: " + maf.getFlag() + ",maf.getVariantType(): " + maf.getVariantType() + ",  novelDbSnp: " + novelDbSnp + ", maf.getTd(): " + maf.getTd() + ", alt: " + alt);
				fail++;
			}
			
			// add to filteredMaf collection
			if (maf.isHighConf() || maf.isLowConf()) {
				filteredMafs.add(maf);
			}
		}
		mafs.clear();
		logger.info("Filter step, high: " + high + ", low: " + lower + ", fail: " + fail);
	}
//	void performFilter() {
//		logger.info("filtering");
//		// loop through all mafs, marking the ranking field as high or low if the relevant filters are passed
//		int high = 0, lower = 0, fail = 0;
//		for(MAFRecord maf : mafs) {
//			
//			boolean novelDbSnp = "novel".equals(maf.getDbSnpId());
//			String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
//			
//			// if maf position verifies, put it straight away into high conf file
//			if ("Valid".equals(maf.getValidationStatus())) {
//				high++;
//				String confidence = null;
//				if (DccConsequence.passesMafNameFilter(maf.getVariantClassification())) {
//					if (MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), novelDbSnp, variant)) {
//						confidence = "high";
//					} else if (MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), variant)) {
//						confidence = "low";
//					} else {
//						confidence = "other";
//					}
//				} else {
//					confidence = "other";
//				}
////				logger.info("setting confidence to be: " + confidence);
//				maf.setConfidence(confidence);
//				maf.setRanking("high");
//				
//			} else if (DccConsequence.passesMafNameFilter(maf.getVariantClassification())) {
//				if (MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(),novelDbSnp , variant)) {
//					high++;
//					maf.setRanking("high");
//				} else if (MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), variant)) {
//					lower++;
//					maf.setRanking("low");
//				} else {
//					logger.debug("Passed name check but failed on remaining checks. Flag: " + maf.getFlag() + ",maf.getVariantType(): " + maf.getVariantType() + ",  novelDbSnp: " + novelDbSnp + ", maf.getTd(): " + maf.getTd() + ", variant: " + variant);
//					fail++;
//				}
//			} else {
//				logger.debug("Failed name check with variant classification: " + maf.getVariantClassification());
//				fail++;
//			}
//			
//			// add to filteredMaf collection
//			if (maf.isHighConf() || maf.isLowConf()) {
//				filteredMafs.add(maf);
//			}
//		}
//		mafs.clear();
//		logger.info("Filter step, high: " + high + ", low: " + lower + ", fail: " + fail);
//	}

	void writeIndividualPatientMafFiles() throws IOException {
		if (FileUtils.canFileBeWrittenTo(outputDirectory)) {
			for (String patient : patientsAndFiles.keySet()) {
				
				boolean containsNS = false;
				// get mafrecords specific to this patient
				List<MAFRecord> patientMafs = new ArrayList<MAFRecord>();
				for (MAFRecord maf : mafs) {
					if (maf.getTumourSampleBarcode().contains(patient)) {
						patientMafs.add(maf);
						if ( ! containsNS && maf.getNovelStartCount() > 0) containsNS = true;
					}
				}
				
				String [] typeAndMeta = getDccMetaData(patient);
				
				MafUtils.writeMafOutput(outputDirectory + FS + patient + "." + typeAndMeta[0] + ".ALL.maf", patientMafs, 
						typeAndMeta[1] + (containsNS ? MafUtils.HEADER_NS : MafUtils.HEADER));
			}
		} else {
			logger.warn("Could not write to output folder: " + outputDirectory);
		}
	}

	void loadDCCFiles() throws Exception {
		logger.info("loading dcc data");
		// do this for all our valid patients
		for (Entry<String, Pair<File,File>> entry : patientsAndFiles.entrySet()) {
			String patient = entry.getKey();
			// SNP
			if (null != entry.getValue().getLeft()) {
				MafUtils.loadDCCFile(entry.getValue().getLeft(), patient, verifiedData.get(patient), mafs, ensemblToEntrez, MutationType.SNP);
			} else {
				logger.warn("no snp dcc file for patient: " + patient);
			}
			
			// INDEL
			if (null != entry.getValue().getRight()) {
				MafUtils.loadDCCFile(entry.getValue().getRight(), patient, verifiedData.get(patient), mafs, ensemblToEntrez, MutationType.INS);
			} else {
				logger.warn("no indel dcc file for patient: " + patient);
			}
		}
		
		logger.info("Loaded all mafs for all relevent patients, mafs size: " + mafs.size());
		
		if (null != dbSNPFile) {
			// update with dbSNP details
			MafUtils.getDbSNPValDetails(dbSNPFile, mafs);
			logger.info("Added dbSNP info");
		}
	}

	void loadRequiredStaticFiles() throws Exception {
		MafUtils.loadEntrezMapping(entrezFile, ensemblToEntrez);
		if (null != canonicalTranscriptsFile)
			MafUtils.loadCanonicalTranscriptMapping(canonicalTranscriptsFile, ensemblGeneToCanonicalTranscript);
		if (null != verificationFile)
			MafUtils.getVerifiedData(verificationFile, null, verifiedData);
	}
	
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
			
			File[] snpFiles = FileUtils.findFilesEndingWithFilter(snpDirectory[0].getAbsolutePath(), ensemblV61 ? SOMATIC_SNPS_V61 : SOMATIC_SNPS);
			
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
	
	
}
