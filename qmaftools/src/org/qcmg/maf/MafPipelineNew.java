/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosBait;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.MafType;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.maf.util.FilterOptions;
import org.qcmg.maf.util.MafFilterUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public abstract class MafPipelineNew {
	
	////////////////////
	// constants
	////////////////////
	
	static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	static final String FS = FileUtils.FILE_SEPARATOR;
	
	public static final int GERMLINE_ALLELE_FRACTION = 20;
	public static final int SOMATIC_ALLELE_FRACTION = 5;
	public static final int HOMOPOLYMER_CUTOFF = 6;
	
	FilterOptions filterOptions = new FilterOptions();
	
	static QLogger logger;
	String logFile;
	String[] cmdLineOutputFiles;
	
	MafType mafType;
	
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
	String cosmicFile;
	
	String dccqFile;
	String bamFile;
	
	String donor;
	
	int homopolymerCutoff;
	
	////////////////////
	// output files
	////////////////////
	String outputDirectory;
	
	////////////////////
	// others
	////////////////////
	String[] lowCoveragePatients;
	int noOfBases = 5;
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
	
	////////////////////
	// collections
	////////////////////
	Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>(40000, 0.99f);
	Map<String, String> ensemblGeneToCanonicalTranscript = new HashMap<>();
	// each patient (that has verification data) has a map of ChrPos and TVS
	Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedData = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>();
	List<MAFRecord> mafs = new ArrayList<>();
	
	//COSMIC data
	Map<ChrPosition, List<String>> cosmicData = new HashMap<ChrPosition, List<String>>();
//	Map<ChrPositionMutation, List<String>> cosmicData = new HashMap<ChrPositionMutation, List<String>>();
	Map<String, AtomicInteger> cosmicGenes = new HashMap<String, AtomicInteger>();
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public MafPipelineNew () {
		logger = QLoggerFactory.getLogger(getClass());
	}
	

	protected String getDccMetaData() throws Exception {
		// get dcc meta info from file, and prepend to header
		TabbedHeader header = null;
		try (TabbedFileReader reader = new TabbedFileReader(new File(dccqFile))){
			header = reader.getHeader();
		}
		
		StringBuilder sb = new StringBuilder();
		if (null != header) {
			for (String s : header)  {
				// ignore qexec
				if ( ! s.startsWith(KeyValue.Q_EXEC))
					sb.append(s).append("\n");
			}
		}
		String dccMetaInfo = qExec.getExecMetaDataToString();
		dccMetaInfo += sb.length() > 0 ?  sb.toString() : "";
		
		return dccMetaInfo;
	}
	
	void addNovelStartsMT(String bamFilePathPart1, String bamFilePathPart2, String bamFilePattern) throws Exception {
		logger.info("adding novel starts");
		// need a map of patients and positions
		final Map<String, List<MAFRecord>> patientsAndMafs = new HashMap<String, List<MAFRecord>>();
		
		// populate with low and high mafs
		for (MAFRecord maf : mafs) {
			if (maf.getConfidence().isHighOrLowConfidence()) {
			
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
						
						if ( ! SAMUtils.isSAMRecordValidForVariantCalling(sam)) continue;
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
					for (final char c : novelStartBases) {
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
		
//		Map<String, TreeMap<ChrPosition, String>> gffs = new HashMap<String, TreeMap<ChrPosition, String>>();
		Map<ChrPosition, String> cpgs = new TreeMap<ChrPosition, String>();
		
		
		
		Map<String, List<ChrPosBait>> gffs = new HashMap<String, List<ChrPosBait>>();
		
		List<ChrPosBait> gffKeepers = new ArrayList<>();
		
		for (MAFRecord maf : mafs) {
			if (maf.getConfidence().isHighOrLowConfidence()) {		// only retrieve cpg info for high/low confidence mafs
				String chr = MafUtils.getFullChromosome(maf);
				ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
				if (null == maf.getCpg())
					cpgs.put(cp, null);
				
				List<ChrPosBait> m = gffs.get(chr);
				if (null == m) {
					m = new ArrayList<ChrPosBait>();
					gffs.put(chr, m);
				}
				m.add(new ChrPosBait(cp));
			}
		}
		logger.info("no of entries in positionsOfInterestMap: " + gffs.size());
		for (Entry<String, List<ChrPosBait>> entry : gffs.entrySet()) {
			Collections.sort(entry.getValue());
			logger.info("no of ChrPos for chr: " + entry.getKey() + " = " + entry.getValue().size());
		}
		
		// query gff3 file
		// if file is null, skip this stage
		if ( ! StringUtils.isNullOrEmpty(gffFile) && ! gffs.isEmpty()) {
			logger.info("number of records requiring gff data: " + gffs.size());
//			GFF3FileReader reader = new GFF3FileReader(new File(gffFile));
	//		Map<String, Map<ChrPosition, String>> gffTypes = new HashMap<String, Map<ChrPosition, String>>();
			try (GFF3FileReader reader = new GFF3FileReader(new File(gffFile))) {
				int  count = 0, updatedCount  = 0;
				List<ChrPosBait> relevantList = null;
				String currentChr = null;
				
				for (GFF3Record rec : reader) {
					String chr = rec.getSeqId();
					
					if (count == 0) {
						logger.info("starting with chr: " + chr);
						 relevantList = gffs.get(chr);
						 if (null != relevantList) {
							 currentChr = relevantList.get(0).getChr();
						 } else {
							 currentChr = chr;
						 }
					}
					
					// check that we have the correct map from the gffs collection
					if ( ! currentChr.equals(chr)) {
						// flush previous chr data to gffKeepers
						if (null != relevantList)
							gffKeepers.addAll(relevantList);
						
						// getting next chr
						relevantList = gffs.get(chr);
						if (null != relevantList) {
							 currentChr = relevantList.get(0).getChr();
							 logger.info("moving onto chr: " + chr);
						} else {
							 currentChr = chr;
						}
					}
					
					// loop through positionsOfInterestMap, and if we have an overlap, update our gff type
					// this could take a while
//					Map<ChrPosition, String> relevantMap = gffs.get(chr);
					if (null != relevantList) {
						
						ChrPosition cp = new ChrPosition(chr, rec.getStart(), rec.getEnd());
						Iterator<ChrPosBait> iter = relevantList.iterator();
						
						while (iter.hasNext()) {
							ChrPosBait cpb = iter.next();
							if (cpb.getPosition() < cp.getPosition()) {		// less than start pos
								iter.remove();
								gffKeepers.add(cpb);
							} else if (cpb.getPosition() <= cp.getEndPosition()) {		// greater than end pos
								// update
								cpb.updateBait(rec.getType());
								updatedCount++;
							} else {
								// cpb.getPosition() must be >  cp.getEndPosition()
								break;
							}
						}
						
					}
					if (++count % 100000 == 0) logger.info("hit " + count + " gff records");
				}
				// flush remaining entries in list
				if (null != relevantList)
					gffKeepers.addAll(relevantList);
				
				logger.info("no of entries with coresponding gff data: " + updatedCount);
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
		
		Map<ChrPosition, String> updatedGffs = new HashMap<>();
		for (ChrPosBait cpb : gffKeepers) {
			updatedGffs.put(new ChrPosition(cpb.getChr(), cpb.getPosition()), cpb.getBait());
		}
		logger.info("no of entries in updatedGffs: " + updatedGffs.size());
		
		// and finally, populate the mafs
		int updatedGFF = 0, updatedCPG = 0;
		for (MAFRecord maf : mafs) {
			
			if (maf.getConfidence().isHighOrLowConfidence()) {
				
				String chr = MafUtils.getFullChromosome(maf);
				ChrPosition cp = new ChrPosition(chr, maf.getStartPosition(), maf.getEndPosition());
				
				String cpg =cpgs.get(cp);
				if (null != cpg) {
					maf.setCpg(cpg);
					updatedCPG++;
				}
				String gffBait = updatedGffs.get(cp);
				if (null != gffBait) {
					maf.setGffBait(gffBait);
					updatedGFF++;
				}
			}
		}
		logger.info("no of mafs updated with CPG data: " + updatedCPG + ", gff data: " + updatedGFF);
		logger.info("adding cpg and gff- DONE");
	}
	
	void loadCOSMICData() throws Exception {
		if (null != cosmicFile && mafType.isSomatic()) {
			
			String identifier = mafType.isIndel() ? "Insertion" : "Substitution";
			
			int count = 0, chrPosCount = 0, chrPosMutCount=0;
			try (TabbedFileReader reader = new TabbedFileReader(new File(cosmicFile));) {
				for (TabbedRecord rec : reader) {
					if (StringUtils.isNullOrEmpty(rec.getData())) continue;	// blank lines in file.... my god.....
					if (rec.getData().startsWith("Gene name")) continue;		//header line
					if (rec.getData().contains(identifier)) {
						boolean forwardStrand = true;
						count++;
						String [] params = TabTokenizer.tokenize(rec.getData());
						String chrPos = params[19];
						if (params[20] != null && params[20] == "-") forwardStrand = false;
						if (StringUtils.isNullOrEmpty(chrPos)) {
							chrPos = params[17];
							if (params[18] != null && params[18] == "-") forwardStrand = false;
						}
						if (StringUtils.isNullOrEmpty(chrPos)) {
//							logger.info("skipping record due to no position info for: " + rec.getData());
							continue;
						}
						chrPosCount++;
						String mutationCDS = params[13];
						
						int gtIndex = mutationCDS.indexOf(">");
						if (gtIndex == -1) {
							logger.info("chrPos: " + chrPos + ", mutationCDS: " + mutationCDS);
							continue;	// not a snp
						}
						chrPosMutCount++;
						
						// build a ChrPosMut obj
						int colonPos = chrPos.indexOf(":");
						int dashPos = chrPos.indexOf("-");
						String chr = chrPos.substring(0, colonPos);
						String position = chrPos.substring(colonPos + 1,  dashPos);
						
						
						char mutation = mutationCDS.substring(gtIndex + 1).charAt(0);
						if ( ! forwardStrand) mutation = BaseUtils.getComplement(mutation);
								
						ChrPosition cpm = new ChrPosition(chr, Integer.parseInt(position));
//						ChrPositionMutation cpm = new ChrPositionMutation(chr, Integer.parseInt(position), mutation.charAt(0));
						
						String mutationId = params[12];
						
						if (cosmicData.containsKey(cpm)) {
							// add to list
							cosmicData.get(cpm).add(mutationId + "_" + mutation);
						} else {
							List<String> mutationIds = new ArrayList<String>();
							mutationIds.add(mutationId + "_" + mutation);
							cosmicData.put(cpm, mutationIds);
						}
						// add to gene collection
						String gene = params[0];
						AtomicInteger ai = cosmicGenes.get(gene);
						if (null == ai) cosmicGenes.put(gene,  new AtomicInteger(1));
						else ai.incrementAndGet();
					}
				}
			}
			
			// log
			logger.info("hit " + count + " COSMIC entries with " + cosmicData.size() + 
					" distinct chrPosMut positions, chrPosCount: " + chrPosCount + ", chrPosMutCount: " + chrPosMutCount);
			
			// run against our mafs to see overlap
			for (MAFRecord maf : mafs) {
				// construct cpm from maf
				// strip "chr" from chromosome
				String chr = maf.getChromosome();
				if (chr.contains("chr")) chr = chr.substring(2);
				ChrPosition cpm = new ChrPosition(chr, maf.getStartPosition());
//				ChrPositionMutation cpm = new ChrPositionMutation(chr, maf.getStartPosition() , MafUtils.getVariant(maf));
				// look up cosmic map
				if (cosmicData.containsKey(cpm)) {
					List<String> ids = cosmicData.get(cpm);
					Map<String, AtomicInteger> idMap = new HashMap<String, AtomicInteger>();
					StringBuffer sb = new StringBuffer();
					for (String s : ids) {
						AtomicInteger ai = idMap.get(s);
						if (null == ai) {
							idMap.put(s, new AtomicInteger(1));
						} else {
							ai.incrementAndGet();
						}
					}
					for (Entry<String, AtomicInteger> entry : idMap.entrySet()) {
						if (sb.length() > 0) sb.append(',');
						sb.append(entry.getKey() + (entry.getValue().get() > 1 ? "(" + entry.getValue().get() + ")" : ""));
						
					}
					maf.setCosmicId(sb.toString());
					maf.setCosmicIdFreq(ids.size());
					logger.info("found a cosmic entry for cpm: " + cpm + " mut: " + MafUtils.getVariant(maf) + " : " + sb.toString());
				}
				
				// and now the gene
				String mafGene = maf.getHugoSymbol();
				if (cosmicGenes.containsKey(mafGene)) {
//					logger.info("found a cosmic gene entry for cpm: " + cpm);
					maf.setCosmicGene(cosmicGenes.get(mafGene).intValue());
				}
				
			}
			
		} else {
			logger.info("No cosmic file specified");
		}
	}
	
	void updateMafsWithMafType() {
		for (MAFRecord m : mafs) m.setMafType(mafType);
	}

	void loadKRASData() throws Exception {
		try (TabbedFileReader reader = new TabbedFileReader(new File(krasFile));) {
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
		}
	}
	
	/**
	 * will examine records that have the NNS annotation to see if the numbers are supportive of 
	 */
	void checkIndel() {
		for (MAFRecord maf : mafs) {
			if (maf.getConfidence().isHighConfidence() && maf.getFlag().contains(SnpUtils.NOVEL_STARTS)) {
				if (MafFilterUtils.checkNovelStartsIndel(MafFilterUtils.INDEL_NNS_PERCENTAGE, maf)) {
					// everything is OK
				} else {
					// move down to LC or LCC
					if (MafConfidence.HIGH_CONSEQUENCE == maf.getConfidence()) {
						maf.setConfidence(MafConfidence.LOW_CONSEQUENCE);
					} else {
						maf.setConfidence(MafConfidence.LOW);
					}
				}
			}
			
			// if its still high conf - another check to see if there is enough NNS evidence
			if (maf.getConfidence().isHighConfidence()) {
				if (MafFilterUtils.checkIndelEvidence(maf)) {
					// all good
				} else {
					// move down to LC or LCC
					if (MafConfidence.HIGH_CONSEQUENCE == maf.getConfidence()) {
						maf.setConfidence(MafConfidence.LOW_CONSEQUENCE);
					} else {
						maf.setConfidence(MafConfidence.LOW);
					}
				}
			}
			
			// check the event motif length
			if (maf.getConfidence().isHighConfidence()) {
				if (MafFilterUtils.checkIndelMotif(maf, mafType)) {
					// all good
				} else {
					if (MafConfidence.HIGH_CONSEQUENCE == maf.getConfidence()) {
						maf.setConfidence(MafConfidence.LOW_CONSEQUENCE);
					} else {
						maf.setConfidence(MafConfidence.LOW);
					}
				}
//			} else if (maf.getConfidence().isLowConfidence()) {
//				if (MafFilterUtils.checkIndelMotif(maf, mafType)) {
//					// all good
//				} else {
//					maf.setConfidence(MafConfidence.ZERO);
//				}
			}
		}
	}
	
	void checkAlleleFraction() {
		if (alleleFraction > 0) {
			logger.info("will check and annotate for allele fractions of less than " + alleleFraction);
			
			int updatedMafs = 0;
			String annotation = SnpUtils.ALLELIC_FRACTION + alleleFraction;
			
			for (MAFRecord maf : mafs) {
				
				if (MutationType.SNP != maf.getVariantType()) continue;
			
				// if somatic - use TD and tumourAllele fields
				// if germine use ND and normalAllele fields
				String bases = mafType.isGermline() ? maf.getNd() : maf.getTd();
						
				String ref = maf.getRef();
				//logger.info("about to call SnpUtils.getCountFromNucleotideString with bases: " + bases +", and ref: " + ref);
				int refCount = SnpUtils.getCountFromNucleotideString(bases, ref);
				
				// if we don't have any reference bases, skip this position as we won't be adding the annotation
				if (refCount == 0) continue;
				
				String allele1 = mafType.isGermline() ? maf.getNormalAllele1() : maf.getTumourAllele1();
				String allele2 = mafType.isGermline() ? maf.getNormalAllele2() : maf.getTumourAllele2();
				
				int altCount = 0;
				if (allele1 == allele2 && allele2 != ref) {
					altCount = SnpUtils.getCountFromNucleotideString(bases, allele1);
				} else if (ref == allele1) {
					altCount = SnpUtils.getCountFromNucleotideString(bases, allele2);
				} else if (ref == allele2) {
					altCount = SnpUtils.getCountFromNucleotideString(bases, allele1);
				} else {
					// get the largest alt here and assume that this is the main alt....
					int a1Count = SnpUtils.getCountFromNucleotideString(bases, allele1);
					int a2Count = SnpUtils.getCountFromNucleotideString(bases, allele2);
					logger.info("Neither alleles equals ref - allele1: " + allele1 + " [" + a1Count + "], allele2: " + allele2 + " [" + a2Count + "], ref: "  + ref + " [" + refCount + "]");
					
					altCount = Math.max(a1Count, a2Count);
				}
				
				if (altCount > 0 ) {
					double observedAllelicFraction = ((double) altCount / (refCount + altCount)) * 100;
					if (Double.compare(observedAllelicFraction, alleleFraction) < 0) {
						logger.info("observedAllelicFraction <= alleleFraction : " + observedAllelicFraction + ", " + alleleFraction + ", altCount: " + altCount + ", refCount: " + refCount);
						
						updatedMafs++;
						MafUtils.updateFlag(maf, annotation);
					}
				}
			}
			logger.info("No of records update with flag " + annotation + " : " + updatedMafs);
		} else {
			logger.info("No allele fraction set - skipping allele fraction annotation");
		}
	}
	
	void checkForMINAnnotation() {
		for (MAFRecord maf : mafs) {
			MafFilterUtils.checkMAFForMIN(maf);
		}
	}

	
	void writeOutput() throws Exception {
		
		String patient = null != donor ? donor : DonorUtils.getDonorFromFilename(dccqFile);
		// if patient is null, try and get donor from filename rather than filepath
		if (null == patient) patient = DonorUtils.getDonorFromString(dccqFile);
		
		String typeAndMeta = getDccMetaData();
		String somGerm = mafType.isSomatic() ? "Somatic" : "Germline";
		String snvIndel = mafType.isIndel() ? "indel" : "snv";
		
		String header = MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS;
//		String header = cosmicFile == null ? MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS :
//			MafUtils.HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS_COSMIC;
		
		boolean containsNS = false;
		for (MAFRecord maf : mafs) {
			if ( ! containsNS && maf.getNovelStartCount() > 0) containsNS = true;
		}
		
		logger.info("attempting to write out " + mafs.size() + " maf records for ALL");
		MafUtils.writeMafOutput(outputDirectory + FS + patient + "." + somGerm + ".ALL." + snvIndel + ".maf", mafs, 
				typeAndMeta + (containsNS ? MafUtils.HEADER_NS : MafUtils.HEADER));
		
		List<MAFRecord> tempMafs = new ArrayList<MAFRecord>();
		
		// HCC
		for (MAFRecord maf : mafs) {
			if (MafConfidence.HIGH_CONSEQUENCE == maf.getConfidence()) {
				tempMafs.add(maf);
			}
		}
//		if ( ! tempMafs.isEmpty()) {
		logger.info("attempting to write out " + tempMafs.size() + " maf records for HCC");
		MafUtils.writeMafOutput(outputDirectory + FS + patient + "."  + somGerm + ".HighConfidenceConsequence." + snvIndel + ".maf", tempMafs, 
				typeAndMeta + header, true);
		tempMafs.clear();
//		}
		
		// HC
		for (MAFRecord maf : mafs) {
			if (maf.getConfidence().isHighConfidence()) {
				tempMafs.add(maf);
			}
		}
//		if ( ! tempMafs.isEmpty()) {
		logger.info("attempting to write out " + tempMafs.size() + " maf records for HC");
		MafUtils.writeMafOutput(outputDirectory + FS +  patient + "." + somGerm + ".HighConfidence." + snvIndel + ".maf", tempMafs, 
				typeAndMeta + header, true);
		tempMafs.clear();
//		}
		
		// LCC
		for (MAFRecord maf : mafs) {
			if (MafConfidence.LOW_CONSEQUENCE == maf.getConfidence()) {
				tempMafs.add(maf);
			}
		}
//		if ( ! tempMafs.isEmpty()) {
		logger.info("attempting to write out " + tempMafs.size() + " maf records for LCC");
		MafUtils.writeMafOutput(outputDirectory + FS +  patient + "."  +  somGerm + ".LowConfidenceConsequence." + snvIndel + ".maf", tempMafs, 
				typeAndMeta + header, true);
		tempMafs.clear();
//		}
		
		// LC 
		for (MAFRecord maf : mafs) {
			if (maf.getConfidence().isLowConfidence()) {
				tempMafs.add(maf);
			}
		}
//		if ( ! tempMafs.isEmpty()) {
		logger.info("attempting to write out " + tempMafs.size() + " maf records for LC");
		MafUtils.writeMafOutput(outputDirectory + FS + patient + "."  + somGerm + ".LowConfidence." + snvIndel + ".maf", tempMafs, 
				typeAndMeta + header, true);
		tempMafs.clear();
//		}
		
	}
	void loadDCCFiles() throws Exception {
		logger.info("loading dcc data");
		String patient = DonorUtils.getDonorFromFilename(dccqFile);
		
		MafUtils.loadDCCFile(dccqFile, verifiedData.get(patient), mafs, ensemblToEntrez, mafType);
		
		logger.info("Loaded " + mafs.size() + " dcc records from " + dccqFile + " for patient: " + patient);
		
		if (null != dbSNPFile) {
			// update with dbSNP details
			MafUtils.getDbSNPValDetails(dbSNPFile, mafs);
			logger.info("Added dbSNP info");
		}
		
		updateMafsWithMafType();
	}

	void loadRequiredStaticFiles() throws Exception {
		MafUtils.loadEntrezMapping(entrezFile, ensemblToEntrez);
		if (null != canonicalTranscriptsFile)
			MafUtils.loadCanonicalTranscriptMapping(canonicalTranscriptsFile, ensemblGeneToCanonicalTranscript);
		if (null != verificationFile)
			MafUtils.getVerifiedData(verificationFile, null, verifiedData);
	}
	
}
