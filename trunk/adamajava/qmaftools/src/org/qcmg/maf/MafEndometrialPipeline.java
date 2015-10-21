/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.Pair;

public class MafEndometrialPipeline extends MafPipeline {
	
//	public static final String datePattern = "[0-9]{8}";
//	public static final String dateAPattern = "[0-9]{8}_A";
//	public static final String patientPattern = "PPPP_[0-9]{4}";
//	
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
//	private static final String FS = FileUtils.FILE_SEPARATOR;
//	
//	private static final String SEQ_FINAL = "seq_final";
	private static final String ENDOMETRIAL = "/mnt/seq_results/smgres_endometrial";
//	private static final String SOMATIC_SNPS = "_somatic_SNPs";
//	private static final String SOMATIC_SNPS_V61 = "_somatic_SNPs_v61";
//	private static final String SOMATIC_INDELS = "_somatic_indels";
//	private static final String SNPS_FILE_LOCATION = FS + "qSNP";
//	private static final String INDELS_FILE_LOCATION = FS + "indels" + FS + "small_indel_tool";
//	private static final String INDELS_V61_FOLDER = "Ens61";
	
//	private static final String SEQ_RESULTS = "/mnt/seq_results/icgc_pancreatic";
//	private static final String SEQ_RESULTS = "/panfs/seq_results/icgc_pancreatic";
//	private static final String SEQ_RESULTS_BAM_EXT = ".exome.TD.bam";
	
	
//	private static QLogger logger;
//	private String logFile;
//	private String[] cmdLineOutputFiles;
//	private int exitStatus;
	
//	private String entrezFile;
//	private String canonicalTranscriptsFile;
//	private String verificationFile;
//	private String dbSNPFile;
//	private String krasFile;
//	private String fastaFile;
//	private String gffFile;
//	private String outputDirectory;
//	private String[] lowCoveragePatients;
//	private int noOfBases = 5;
//	
//	private boolean ensemblV61;
	
//	public final static Comparator<MAFRecord> MAF_COMPARATOR = new Comparator<MAFRecord>() {
//		@Override
//		public int compare(MAFRecord maf0, MAFRecord maf1) {
//			if (isNumeric(maf0.getChromosome().charAt(0)) && isNumeric(maf1.getChromosome().charAt(0)))  {
//				int chrDiff = Integer.valueOf(maf0.getChromosome()).compareTo(Integer.valueOf(maf1.getChromosome()));
//				if (chrDiff != 0) 
//					return chrDiff;
//			} else {
//				int chrDiff = maf0.getChromosome().compareTo(maf1.getChromosome());
//				if (chrDiff != 0) 
//					return chrDiff;
//			}
//			return maf0.getStartPosition() - maf1.getStartPosition();
//		}
//		private boolean isNumeric(char ch) {
//		    return ch >= '0' && ch <= '9';
//		  }
//	};
	
//	public final static FilenameFilter dateDirectoryFilter =  new FilenameFilter() {
//		@Override
//		public boolean accept(File dir, String name) {
//			return name.matches(datePattern)
//			&& new File(dir + FS + name).isDirectory(); 
//		}
//	};
//	public final static FilenameFilter dateADirectoryFilter =  new FilenameFilter() {
//		@Override
//		public boolean accept(File dir, String name) {
//			return name.matches(dateAPattern)
//			&& new File(dir + FS + name).isDirectory(); 
//		}
//	};
	public static final FilenameFilter endometrialPatientDirectoryFilter = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return name.startsWith("PPPP_") && new File(file + FS + name).isDirectory(); 
		}
	};
	
	
	// NOTE that collection initial capacities are based on data as of 14/11/2011
	// should be reviewed periodically if performance is an issue
	
//	private  List<File> patientsInSeqAnalysis = new ArrayList<File>(200);
//	private final  Map<String, Pair<File, File>> patientsAndFiles = new HashMap<String, Pair<File,File>>(100);
//	private final Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>(40000, 0.99f);
//	private final Map<String, String> ensemblGeneToCanonicalTranscript = new HashMap<String, String>();
//	// each patient (that has verification data) has a map of ChrPos and TVS
//	private final Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedData = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>(10000);
//	
//	private final List<MAFRecord> mafs = new ArrayList<MAFRecord>(250000);
//	private final List<MAFRecord> filteredMafs = new ArrayList<MAFRecord>(25000);
	
	private int engage() throws Exception {
		
		// get list of patients to run from seq_analysis
		// must start with APGI_[0-9]
		patientsInSeqAnalysis = Arrays.asList(FileUtils.findFiles(ENDOMETRIAL, endometrialPatientDirectoryFilter));
//		patientsInSeqAnalysis = Arrays.asList(FileUtils.findDirectories(SEQ_ANALYSIS, new FilenameFilter() {
//			@Override
//			public boolean accept(File file, String name) {
//				return name.startsWith("APGI_") && Character.isDigit(name.charAt(5))
//				&& new File(file + FS + name).isDirectory(); 
//			}
//		}));
		
		logger.info("Will attempt maf generation for the following " + patientsInSeqAnalysis.size() + " patients: ");
		for (File f : patientsInSeqAnalysis) logger.info(f.getName());
		
		// need to check each patient to ensure that they have a valid SNP and indel folder
		checkPatientsHaveRequiredFolders();
		logger.info("no of patients with data: " + patientsAndFiles.size());
		
		loadRequiredStaticFiles();
		
		//
		loadDCCFiles();
		
		writeIndividualPatientMafFiles();
		
		// commented out for endo stuff
//		loadKRASData();
		
		performFilter();
		
		addCpgAndGff();
		
		addNovelStartsMT(ENDOMETRIAL, SEQ_FINAL, SEQ_RESULTS_BAM_EXT);
		
//		writeFinalPREFilteredOutput();
		writeFinalFilteredOutput();
		
		return exitStatus;
	}
	
//	public static boolean passesFinalFilter(MAFRecord maf) {
//		// keep all KRAS
//		if ("KRAS".equals(maf.getHugoSymbol())) return true;
//		
//		// if its a SNP, then it should have novel count info
//		if (SNP.equals(maf.getVariantType())) {
//			// needs to have 4 or more novels
//			return maf.getNovelStartCount() >= 4;
//		} else {
//			// if its an indel, doesn't have novel count info so return true
//			return true;
//		}
//	}
	
//	@Override
//	private void writeFinalFilteredOutput() throws IOException {
//		
//		// get lists of high and low conf mafs
//		List<MAFRecord> highConfMafs = new ArrayList<MAFRecord>();
//		List<MAFRecord> lowConfMafs = new ArrayList<MAFRecord>();
//		
//		for (MAFRecord maf : filteredMafs) {
//			if (maf.isHighConf() && passesFinalFilter(maf)) {
//				highConfMafs.add(maf);
//				continue;
//			}
//			if (maf.isLowConf() && passesFinalFilter(maf)) {
//				lowConfMafs.add(maf);
//				continue;
//			}
//		}
//		
//		MafUtils.writeMafOutput(outputDirectory + FS +  "highConfidence.maf", highConfMafs, MafUtils.HEADER_WITH_CONFIDENCE_CPG, true);
//		MafUtils.writeMafOutput(outputDirectory + FS +  "lowConfidence.maf", lowConfMafs, MafUtils.HEADER_WITH_CONFIDENCE_CPG, true);
//	}
//	@Override
//	private void writeFinalPREFilteredOutput() throws IOException {
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

//	private void addNovelStarts() throws Exception {
//		logger.info("adding novel starts");
//		// need a map of patients and positions
//		Map<String, List<MAFRecord>> patientsAndMafs = new HashMap<String, List<MAFRecord>>();
//		
//		// populate with low and high mafs
//		for (MAFRecord maf : filteredMafs) {
////			if (maf.isHighConf() || maf.isLowConf()) {
//				
//				// get patient from tumour QCMG-66-APGI_2049-ICGC-ABMJ-20100729-08-TD string
//				String patient = maf.getTumourSampleBarcode().substring(8, 17);
//				List<MAFRecord> patientMafs = patientsAndMafs.get(patient);
//				if (null == patientMafs) {
//					patientMafs = new ArrayList<MAFRecord>();
//					patientsAndMafs.put(patient, patientMafs);
//				}
//				patientMafs.add(maf);
////			}
//		}
//		
//		// get bam from seq_results
//		for (Entry<String, List<MAFRecord>> entry : patientsAndMafs.entrySet()) {
//			String patient = entry.getKey();
//			logger.info("looking for files in : " + ENDOMETRIAL + FS + patient + FS + SEQ_FINAL);
//			File [] files = FileUtils.findFilesEndingWithFilter(ENDOMETRIAL + FS + patient + FS + SEQ_FINAL, SEQ_RESULTS_BAM_EXT);
//			if (null == files || files.length < 1) {
//				logger.warn("Could not find any bam files in seq_results!");
//				continue;
//			}
//			File bamFile = files[0];
//			int noOfRecordsRetrievedForPatient = 0, noOfPositionsRetrievedForPatient = 0, positionsWithDeletions = 0;
//			
//			// sort the collection
//			Collections.sort(entry.getValue(), MAF_COMPARATOR); 
//			
//			long start = System.nanoTime();
//			QJumper qj = new QJumper();
//			qj.setupReader(bamFile);
//			try {
//				for (MAFRecord maf : entry.getValue()) {
//					noOfPositionsRetrievedForPatient++;
//					String chr = MafUtils.getFullChromosome(maf);
//					List<SAMRecord> records = qj.getRecordsAtPosition(chr, maf.getStartPosition());
//					noOfRecordsRetrievedForPatient += records.size();
////					logger.info("pos: " + maf.getChromosome() + "-" + maf.getStartPosition() + ": " + records.size());
//					
//					char[] bases = new char[records.size()];
//					char[] novelStartBases = new char[records.size()];
//					Set<Integer> forwardStrand = new HashSet<Integer>();
//					Set<Integer> reverseStrand = new HashSet<Integer>();
//					int i = 0, j = 0;
//					
//					for (SAMRecord sam : records) {
//						if (sam.getDuplicateReadFlag()) continue;
//						if (sam.getMappingQuality() < 10) continue;
//						
//						int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, maf.getStartPosition());
//						
//						if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
//							
//							if (sam.getBaseQualities()[indexInRead] < 10) continue;
//							
//							char c = sam.getReadString().charAt(indexInRead);
//							bases[i++] = c;
//							// novel start count
//							if (sam.getReadNegativeStrandFlag()) {
//								if (reverseStrand.add(sam.getAlignmentStart())) {
//									novelStartBases[j++] = c;
//								}
//							} else {
//								if (forwardStrand.add(sam.getAlignmentStart())) {
//									novelStartBases[j++] = c;
//								}
//							}
//							
//						} else {
//							logger.info("help!!");
//						}
//					}
//					
//					// count the number of times the variant appears in the novelstartbases[]
//					maf.setNovelStartBases(new String(novelStartBases));
//					int count = 0;
//					char var = MafUtils.getVariant(maf);
//					for (char c : novelStartBases) {
//						if (c == var) count++;
//					}
//					maf.setNovelStartCount(count);
//				}
//			} finally {
//				qj.closeReader();
//			}
//			long elapsedTime = (System.nanoTime() - start) / 1000000000;	// nanosecons
//			logger.info("bamfile: " + bamFile.getName() + ", positions queried: " + noOfPositionsRetrievedForPatient 
//					+ " (" + ((double)noOfPositionsRetrievedForPatient / elapsedTime) 
//					+ " per sec), records at positions: " + noOfRecordsRetrievedForPatient 
//					+ " ("  + ((double)noOfRecordsRetrievedForPatient / elapsedTime)
//					+ " per sec), deletions (possibly): " + positionsWithDeletions);
//		}
//		logger.info("adding novel starts - DONE");
//	}
	
//	private void addNovelStartsMT() throws Exception {
//		logger.info("adding novel starts");
//		// need a map of patients and positions
//		final Map<String, List<MAFRecord>> patientsAndMafs = new HashMap<String, List<MAFRecord>>();
////		final Queue<Pair<File, List<MAFRecord>>> fileMafs = new ConcurrentLinkedQueue<Pair<File, List<MAFRecord>>>();
//		
//		// populate with low and high mafs
//		for (MAFRecord maf : filteredMafs) {
//			
//			// only want to do this for snps - ignoring indels for the time being
//			if (SNP.equals(maf.getVariantType())) {
//				
//				// get patient from tumour QCMG-66-APGI_2049-ICGC-ABMJ-20100729-08-TD string
//				String patient = maf.getTumourSampleBarcode().substring(8, 17);
//				List<MAFRecord> patientMafs = patientsAndMafs.get(patient);
//				if (null == patientMafs) {
//					patientMafs = new ArrayList<MAFRecord>();
//					patientsAndMafs.put(patient, patientMafs);
//				}
//				patientMafs.add(maf);
//			}
//		}
//		
////		CountDownLatch latch = new CountDownLatch(100);
//		int poolSize = 2;
//		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
//		
//		// get bam from seq_results
//		for (Entry<String, List<MAFRecord>> entry : patientsAndMafs.entrySet()) {
//			String patient = entry.getKey();
//			logger.info("looking for files in : " + ENDOMETRIAL + FS + patient + FS + SEQ_FINAL);
//			File [] files = FileUtils.findFilesEndingWithFilter(ENDOMETRIAL + FS + patient + FS + SEQ_FINAL, SEQ_RESULTS_BAM_EXT);
//			if (null == files || files.length < 1) {
//				logger.warn("Could not find any bam files in seq_results!");
//				continue;
//			}
//			File bamFile = files[0];
//			List<MAFRecord> patientMafs = entry.getValue();
//			Collections.sort(patientMafs, MAF_COMPARATOR);
////			fileMafs.add(new Pair<File, List<MAFRecord>>(bamFile, patientMafs));
//			executor.execute(new NovelCounter(bamFile, patientMafs));
//			
//		}
////		logger.info("About to start the threads, mafFile size: " + fileMafs.size());
////		for (int i = 0 ; i < poolSize ; i++) {
////		}
//		executor.shutdown();
//		
//		 try {
//		     // Wait a while for existing tasks to terminate
//		     if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
//		    	 executor.shutdownNow(); // Cancel currently executing tasks
//		       // Wait a while for tasks to respond to being cancelled
//		       if (!executor.awaitTermination(60, TimeUnit.MINUTES))
//		           System.err.println("Pool did not terminate");
//		     }
//		   } catch (InterruptedException ie) {
//		     // (Re-)Cancel if current thread also interrupted
//			   executor.shutdownNow();
//		     // Preserve interrupt status
//		     Thread.currentThread().interrupt();
//		   }
//		
////		logger.info("threads have finished mafFile size: " + fileMafs.size());
//		logger.info("adding novel starts - DONE");
//	}
	
//	private static class NovelCounter implements Runnable {
//		private final File bamFile;
//		private final List<MAFRecord> ncMafs;
//		
//		NovelCounter(File bamFile, List<MAFRecord> mafs) {
//			this.bamFile = bamFile;
//			this.ncMafs = mafs;
//		}
//
//		@Override
//		public void run() {
//			logger.info("thread starting");
//			long start = System.nanoTime();
//			long elapsedTime = 0;
//			
//			SAMFileReader reader = new SAMFileReader(bamFile);
//			// if we have a small no of positions, no need to cache
//			reader.enableIndexCaching(ncMafs.size() < 10);
//			
//			int noOfRecordsRetrievedForPatient = 0, noOfPositionsRetrievedForPatient = 0, positionsWithDeletions = 0;
//			
//			try {
//				for (MAFRecord maf : ncMafs) {
//					noOfPositionsRetrievedForPatient++;
//					String chr = MafUtils.getFullChromosome(maf);
////					List<SAMRecord> records = qj.getRecordsAtPosition(chr, maf.getStartPosition());
//					SAMRecordIterator records = reader.queryOverlapping(chr, maf.getStartPosition(), maf.getStartPosition());
////					noOfRecordsRetrievedForPatient += records.size();
////					logger.info("pos: " + maf.getChromosome() + "-" + maf.getStartPosition() + ": " + records.size());
//					
////					char[] bases = new char[records.size()];
//					char[] novelStartBases = new char[1024];	// hmmmmm
//					Set<Integer> forwardStrand = new HashSet<Integer>();
//					Set<Integer> reverseStrand = new HashSet<Integer>();
//					int j = 0;
//					
//					while (records.hasNext()) {
//						noOfRecordsRetrievedForPatient++;
//						SAMRecord sam = records.next();
//						if (sam.getDuplicateReadFlag()) continue;
//						if (sam.getMappingQuality() < 10) continue;
//						
//						int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, maf.getStartPosition());
//						
//						if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
//							
//							if (sam.getBaseQualities()[indexInRead] < 10) continue;
//							
//							char c = sam.getReadString().charAt(indexInRead);
////							bases[i++] = c;
//							// novel start count
//							
//							final char var = MafUtils.getVariant(maf);
//							
//							// only care about variant novel starts
//							if (var == c) {
//							
//								if (sam.getReadNegativeStrandFlag()) {
//									if (reverseStrand.add(sam.getAlignmentStart())) {
//										novelStartBases[j++] = c;
//									}
//								} else {
//									if (forwardStrand.add(sam.getAlignmentStart())) {
//										novelStartBases[j++] = c;
//									}
//								}
//							}
//						} else {
//							positionsWithDeletions++;
////							logger.info("help!!");
//						}
//					}
//					records.close();
//					
//					// count the number of times the variant appears in the novelstartbases[]
//					maf.setNovelStartBases(new String(novelStartBases));
//					int count = 0;
//					final char var = MafUtils.getVariant(maf);
//					for (final char c : novelStartBases) {
//						if (c == var) count++;
//					}
//					maf.setNovelStartCount(count);
//				}
//				elapsedTime = (System.nanoTime() - start) / 1000000000;	// nanosecons
//				logger.info("bamfile: " + bamFile.getName() + ", positions queried: " + noOfPositionsRetrievedForPatient 
//						+ " (" + ((double)noOfPositionsRetrievedForPatient / elapsedTime) 
//						+ " per sec), records at positions: " + noOfRecordsRetrievedForPatient 
//						+ " ("  + ((double)noOfRecordsRetrievedForPatient / elapsedTime)
//						+ " per sec), deletions (possibly): " + positionsWithDeletions);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} finally {
//				try {
//					reader.close();
//				} finally {
////					latch.countDown();
//					logger.info("thread finishing, elapsedTime: " + elapsedTime);
//				}
////			}
//			}
//		}
//	}

//	@Override
//	private void addCpgAndGff() throws Exception {
//		logger.info("adding cpg and gff");
//		
//		Map<String, Map<ChrPosition, String>> gffs = new HashMap<String, Map<ChrPosition, String>>();
//		Map<ChrPosition, String> cpgs = new TreeMap<ChrPosition, String>();
//		
//		
//		for (MAFRecord maf : filteredMafs) {
//			String chr = MafUtils.getFullChromosome(maf);
//			ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
//			cpgs.put(cp, null);
//			
//			Map<ChrPosition, String> m = gffs.get(chr);
//			if (null == m) {
//				m = new HashMap<ChrPosition, String>();
//				gffs.put(chr, m);
//			}
//			m.put(cp, null);
//				
//		}
//		logger.info("no of entries in positionsOfInterestMap: " + gffs.size());
//		for (Entry<String, Map<ChrPosition, String>> entry : gffs.entrySet()) {
//			logger.info("no of ChrPos for chr: " + entry.getKey() + " = " + entry.getValue().size());
//		}
//		
//		// query gff3 file
//		GFF3FileReader reader = new GFF3FileReader(new File(gffFile));
////		Map<String, Map<ChrPosition, String>> gffTypes = new HashMap<String, Map<ChrPosition, String>>();
//		try {
//			int  count = 0, updatedCount  = 0;
//			for (GFF3Record rec : reader) {
//				String chr = rec.getSeqId();
//				
//				ChrPosition cp = new ChrPosition(chr, rec.getStart(), rec.getEnd());
//				
//				// loop through positionsOfInterestMap, and if we have an overlap, update our gff type
//				// this could take a while
//				Map<ChrPosition, String> relevantMap = gffs.get(chr);
//				if (null != relevantMap) {
//					for (Entry<ChrPosition, String> entry : relevantMap.entrySet()) {
//						if (ChrPositionUtils.doChrPositionsOverlap(cp, entry.getKey())) {
//							String gff = entry.getValue();
//							if (StringUtils.isNullOrEmpty(gff)) {
//								gff = rec.getType();
//							} else {
//								gff += ";" + rec.getType();
//							}
//							entry.setValue(gff);
//							updatedCount++;
//							
//							// single position - won't have multiple gff3 regions
//							if (entry.getKey().isSinglePoint()) break;
//						}
//					}
//				}
////				thisMap.put(new ChrPosition(chr, rec.getStart(), rec.getEnd()), rec.getType());
//				if (++count % 1000000 == 0) logger.info("hit " + count + " records");
//			}
//			logger.info("no of entries with coresponding gff data: " + updatedCount);
//		} finally {
//			reader.close();
//		}
//		
//		// query fasta file
//		IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(new File(fastaFile));
//		for (Entry<ChrPosition, String> entry : cpgs.entrySet()) {
//			ChrPosition cp = entry.getKey();
//			ReferenceSequence seq = fasta.getSubsequenceAt(cp.getChromosome(), (cp.getPosition() - noOfBases), (cp.getEndPosition() +  noOfBases));
//			entry.setValue(new String(seq.getBases()));
//		}
//		
//		// and finally, populate the mafs
//		int updatedGFF = 0, updatedCPG = 0;
//		for (MAFRecord maf : mafs) {
//			if (maf.isHighConf() || maf.isLowConf()) {
//				
//				String chr = MafUtils.getFullChromosome(maf);
//				ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
//				
//				String cpg =cpgs.get(cp);
//				if (null != cpg) {
//					maf.setCpg(cpg);
//					updatedCPG++;
//				}
//				String gffBait = gffs.get(chr).get(cp);
//				if (null != gffBait) {
//					maf.setGffBait(gffBait);
//					updatedGFF++;
//				}
//			}
//		}
//		logger.info("no of mafs updated with CPG data: " + updatedCPG + ", gff data: " + updatedGFF);
//		logger.info("adding cpg and gff- DONE");
//	}


	@Override
	void checkPatientsHaveRequiredFolders() {
		for (File f : patientsInSeqAnalysis) {
			File snpDccFile = null, indelDccFile = null;
			File[] snpDirectory = FileUtils.findFiles(f.getAbsolutePath() + SNPS_FILE_LOCATION, dateADirectoryFilter);
			
			if (snpDirectory.length == 0) {
				
				// check that prelim folder exists before searching for files within it
//				if (new File(f.getAbsolutePath() +SNPS_FILE_LOCATION_PRELIM).exists()) {
//					snpDirectory = FileUtils.findDirectories(f.getAbsolutePath() + SNPS_FILE_LOCATION_PRELIM, dateADirectoryFilter);
//				}
				if (snpDirectory.length != 1) {
					logger.info("removing patient: " + f.getName() + ", no valid snp folder");
					continue;
				}
			}
			
			File[] snpFiles = FileUtils.findFilesEndingWithFilter(snpDirectory[0].getAbsolutePath(), SOMATIC_SNPS);
//			File[] snpFiles = FileUtils.findFiles(snpDirectory[0].getAbsolutePath(), ensemblV61 ? SOMATIC_SNPS_V61 : SOMATIC_SNPS);
			
			if (snpFiles.length > 0) {
				snpDccFile = FileUtils.findFilesEndingWithFilter(snpDirectory[0].getAbsolutePath(), SOMATIC_SNPS)[0];
			} else {
				logger.info("removing patient: " + f.getName() + ", no valid snp file");
				continue;
			}
			
			// add patient and files to map
			patientsAndFiles.put(f.getName(), new Pair<File, File>(snpDccFile, indelDccFile));
		}
	}
	
	public static void main(String[] args) throws Exception {
		MafEndometrialPipeline sp = new MafEndometrialPipeline();
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
//			if ((verificationFile = options.getVerified()) == null || ! FileUtils.canFileBeRead(verificationFile))
//				throw new QMafException("NO_VERIFIED_FILE_ERROR");
			if ((dbSNPFile = options.getDbSNPFile()) == null || ! FileUtils.canFileBeRead(dbSNPFile))
				throw new QMafException("NO_DBSNP_FILE_ERROR");
//			if ((krasFile = options.getKrasFile()) == null || ! FileUtils.canFileBeRead(krasFile))
//				throw new QMafException("NO_KRAS_FILE_ERROR");
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
			logger = QLoggerFactory.getLogger(MafEndometrialPipeline.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("QMafPipeline", MafEndometrialPipeline.class.getPackage().getImplementationVersion(), args);
			
			logger.tool("will retrieve " + noOfBases + " on either side of position of interest from fasta file");
			logger.tool("ensemblV61: " + ensemblV61);
			
			return engage();
		}
		return returnStatus;
	}
}
