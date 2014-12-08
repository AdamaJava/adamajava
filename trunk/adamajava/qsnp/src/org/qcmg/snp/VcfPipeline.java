/**
s * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.vcf.VCFFileReader;


/**
 */
public final class VcfPipeline extends Pipeline {
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final static QLogger logger = QLoggerFactory.getLogger(VcfPipeline.class);

	private static VcfHeader controlVcfHeader;
	private static VcfHeader testVcfHeader;
	
	private final ConcurrentMap<ChrPosition, Accumulator> controlPileup = new ConcurrentHashMap<>();
	private final ConcurrentMap<ChrPosition, Accumulator> testPileup = new ConcurrentHashMap<>();

	private final ConcurrentMap<ChrPosition, QSnpRecord> controlVCFMap = new ConcurrentHashMap<>(); //not expecting more than 100000
	private final ConcurrentMap<ChrPosition, QSnpRecord> testVCFMap = new ConcurrentHashMap<>(1024 * 128);
	
	//input Files
	private String controlVcfFile, testVcfFile;
	private String controlBam;
	private String testBam;
	private int mutationId;

	/**
	 */
	public VcfPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);

		// load rules from ini file
		ingestIni(iniFile);
		
		// setup normalBam and tumourBam variables
		if ( ! singleSampleMode) {
			if (null != controlBams && controlBams.length > 0) {
				controlBam = controlBams[0];
			} else {
				throw new SnpException("No normal bam file specified");
			}
		
		}
		if (null != testBams && testBams.length > 0) {
			testBam = testBams[0];
		} else {
			throw new SnpException("No tumour bam file specified");
		}
		
		// load vcf files
		if ( ! singleSampleMode) {
			logger.info("Loading normal vcf data");
			loadVCFData(controlVcfFile, controlVCFMap, true);
			logger.info("Loading normal vcf data - DONE [" + controlVCFMap.size() + "]");
		}
		
		logger.info("Loading tumour vcf data");
		loadVCFData(testVcfFile, testVCFMap, false);
		logger.info("Loading tumour vcf data - DONE [" + testVCFMap.size() + "]");
		
		if (controlVCFMap.isEmpty() && testVCFMap.isEmpty()) {
			throw new SnpException("EMPTY_VCF_FILES");
		} else if (controlVCFMap.isEmpty()) {
			logger.warn("no data in control vcf file");
		} else if (testVCFMap.isEmpty()) {
			logger.warn("no data in test vcf file");
		}
		
		if ( ! singleSampleMode) {
			logger.info("merging data");
			mergeControlAndTestRecords();
			logger.info("merging data - DONE [" + positionRecordMap.size() + "]");
		} else {
			// put test map into positions map
			positionRecordMap.putAll(testVCFMap);
		}
		
		// add pileup from the normal bam file
		logger.info("adding pileup to vcf records map[" + positionRecordMap.size() + "]");
		addPileup();
		logger.info("adding pileup to vcf records map - DONE[" + positionRecordMap.size() + "]");
		
		logger.info("about to clean snp map[" + positionRecordMap.size() + "]");
		cleanSnpMap();
		logger.info("about to clean snp map - DONE[" + positionRecordMap.size() + "]");

		// time for post-processing
		logger.info("about to classify[" + positionRecordMap.size() + "]");
		classifyPileup();
		logger.info("about to classify - DONE[" + positionRecordMap.size() + "]");
		
		// write output
		writeVCF(vcfFile);

	}
	
	@Override
	String getExistingVCFHeaderDetails()  {
		StringBuilder sb = new StringBuilder();
		
		
		if ( ! singleSampleMode) {
			
			for (VcfHeaderRecord rec : controlVcfHeader) {
				sb.append(rec.toString()).append(Constants.NEW_LINE);
			}
			sb.append(Constants.DOUBLE_HASH).append(Constants.NEW_LINE);
		}
		
		for (VcfHeaderRecord rec : testVcfHeader) {
			sb.append(rec.toString()).append(Constants.NEW_LINE);
		}
		sb.append(Constants.DOUBLE_HASH).append(Constants.NEW_LINE);
		
		// override this if dealing with input VCFs and the existing headers are to be kept
		return sb.toString();
	}
	
	@Override
	String getDccMetaData() throws Exception {
		if (null == controlBams || controlBams.length == 0 || 
				StringUtils.isNullOrEmpty(controlBams[0]) 
				|| null == testBams || testBams.length == 0 
				|| StringUtils.isNullOrEmpty(testBams[0])) return null;
		
		SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(controlBams[0]).getFileHeader();
		SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(testBams[0]).getFileHeader();
		
		QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "GATK");
		
		return dccMeta.getDCCMetaDataToString();
	}
	
		
	private void cleanSnpMap() {
		logger.info("about to clean the map");
		int normalUpdated = 0, tumourUpdated = 0;
		for (QSnpRecord record : positionRecordMap.values()) {
			// if entry doesn't have a normal genotype, but contains a normal nucleotides string
			// set normal genotype to hom ref
			if (null == record.getNormalGenotype() && null != record.getNormalNucleotides()) {
				String refString = record.getRef();
				if (refString.length() > 1) {
					logger.warn("refString: " + refString + " in VcfPipeline.cleanSnpMap");
				}
				char ref = refString.charAt(0);
				record.setNormalGenotype(GenotypeEnum.getGenotypeEnum(ref, ref));
				normalUpdated++;
			}
			
			// if we don't have a tumour vcf call, just classify as Germline, annotate and off we go  
			if ( ! singleSampleMode && null == record.getTumourGenotype()) {
				record.setClassification(Classification.GERMLINE);
				VcfUtils.updateFilter(record.getVcfRecord(), "no call in tumour vcf");
				
				tumourUpdated++;
			}
		}
		
		logger.info("added genotypes (hom ref) for " + normalUpdated + " normals, and set classification to GERMLINE for " + tumourUpdated + " tumours");
	}

	private void addPileup() throws Exception {
		
		logger.info("Setting up Pileup threads");
		long start = System.currentTimeMillis();
		
		int noOfThreads = singleSampleMode ? 1 : 2;
		
		final CountDownLatch latch = new CountDownLatch(noOfThreads);
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
		
		if ( ! singleSampleMode) {
			service.execute(new Pileup(controlBam, latch, true));
		}
		service.execute(new Pileup(testBam, latch, false));
		service.shutdown();
		
		try {
			latch.await();
			logger.info("Pileup threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
		} catch (InterruptedException ie) {
			logger.error("InterruptedException caught",ie);
			Thread.currentThread().interrupt();
		}
	}
	
	
	private void mergeControlAndTestRecords() {
		
		// loop through normal vcf map - get corresponding tumour if present
		for (Entry<ChrPosition, QSnpRecord> entry : controlVCFMap.entrySet()) {
			QSnpRecord tumour = testVCFMap.get(entry.getKey());
			positionRecordMap.put(entry.getKey(), getQSnpRecord(entry.getValue(), tumour));
		}
		
		// loop through tumour, making sure entry exists in qsnp map
		for (Entry<ChrPosition, QSnpRecord> entry : testVCFMap.entrySet()) {
			
			QSnpRecord snpRecord = positionRecordMap.get(entry.getKey());
			if (null == snpRecord) {
				positionRecordMap.put(entry.getKey(), getQSnpRecord(null, entry.getValue()));
			}
		}
	}
	
	private  QSnpRecord getQSnpRecord(QSnpRecord normal, QSnpRecord tumour) {
		QSnpRecord qpr = null;
		
		if (null != normal) {
			qpr = normal;
//			qpr.setRef(normal.getRef());
//			qpr.setNormalGenotype(normal.getGenotypeEnum());
//			qpr.setAnnotation(normal.getAnnotation());
			// tumour fields
			qpr.setTumourGenotype(null == tumour ? null : tumour.getTumourGenotype());
			qpr.setTumourCount(null == tumour ? 0 :  VcfUtils.getDPFromFormatField(tumour.getVcfRecord().getFormatFields().get(1)));
			
		} else if (null != tumour) {
			qpr = tumour;
//			qpr = new QSnpRecord(tumour.getChromosome(), tumour.getPosition(), tumour.getRef());
//			qpr.setRef(tumour.getRef());
//			qpr.setTumourGenotype(tumour.getGenotypeEnum());
//			qpr.setTumourCount(VcfUtils.getDPFromFormatField(tumour.getGenotype()));
		}
		
		qpr.setId(++mutationId);
		return qpr;
	}

	private static void loadVCFData(String vcfFile, Map<ChrPosition,QSnpRecord> map, boolean isControl) throws Exception {
		if (FileUtils.canFileBeRead(vcfFile)) {
			
			try (VCFFileReader reader  = new VCFFileReader(new File(vcfFile));) {
				if (isControl) {
					controlVcfHeader = reader.getHeader();
				} else {
					testVcfHeader = reader.getHeader();
				}
				for (VcfRecord qpr : reader) {
					
					// if the length of the reference bases != length of the alt bases, its not a snp (or compound snp)
					if (VcfUtils.isRecordAMnp(qpr)) {
						QSnpRecord snpRecord = new QSnpRecord(qpr);
						if (null != qpr.getFormatFields() && ! qpr.getFormatFields().isEmpty()) {
							//	 set genotype
							if (isControl) {
								snpRecord.setNormalNucleotides(VcfUtils.getGenotypeFromGATKVCFRecord(qpr));
								snpRecord.setNormalGenotype(VcfUtils.getGEFromGATKVCFRec(qpr));
							} else {
								snpRecord.setTumourNucleotides(VcfUtils.getGenotypeFromGATKVCFRecord(qpr));
								snpRecord.setTumourGenotype(VcfUtils.getGEFromGATKVCFRec(qpr));
							}
						}
						map.put(qpr.getChrPosition() , snpRecord);
					}
//					map.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),new QSnpGATKRecord(qpr));
				}
			}
		}
	}

	@Override
	protected void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);

		// ADDITIONAL INPUT FILES
		controlVcfFile = IniFileUtil.getInputFile(ini, "controlVcf");
		testVcfFile = IniFileUtil.getInputFile(ini, "testVcf");
		
		// log values retrieved from in file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("control vcf file: " + controlVcfFile);
		logger.tool("test vcf file: " + testVcfFile);
		
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}

	@Override
	protected String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

	@Override
	protected String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	
	/**
	 * Class that reads SAMRecords from a Queue and after checking that they satisfy some criteria 
	 * (ie. not duplicates, reasonable mapping quality) attempts to match the record against the current vcf record.
	 * If there is a match (ie. the sam record encompasses the vcf position, then a record of the base and strand at that position is kept.
	 * 
	 */
	public class Pileup implements Runnable {
		private final SAMFileReader reader;
		private final boolean isNormal;
		private final ConcurrentMap<ChrPosition , Accumulator> pileupMap;
		private int arraySize;
		private int arrayPosition;
		private ChrPosition cp;
		private Comparator<String> chrComparator;
		private final List<ChrPosition> snps;
		private final CountDownLatch latch;
		
		public Pileup(final String bamFile, final CountDownLatch latch, final boolean isNormal) {
			this.isNormal = isNormal;
			pileupMap = isNormal ? controlPileup : testPileup;
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
			snps = new ArrayList<ChrPosition>(positionRecordMap.keySet());
			this.latch = latch;
		}
		
		private void createComparatorFromSAMHeader(SAMFileReader reader) {
			SAMFileHeader header = reader.getFileHeader();
			
			final List<String> sortedContigs = new ArrayList<String>();
			for (SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
			
			// try and sort according to the ordering of the bam file that is about to be processed
			// otherwise, resort to alphabetic ordering and cross fingers...
			if ( ! sortedContigs.isEmpty()) {
				
				chrComparator = new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						return sortedContigs.indexOf(o1) - sortedContigs.indexOf(o2);
					}
				};
				
				Collections.sort(snps, new Comparator<ChrPosition>() {
					@Override
					public int compare(ChrPosition o1, ChrPosition o2) {
						int diff = chrComparator.compare(o1.getChromosome(), o2.getChromosome());
						if (diff != 0) return diff;
						return o1.getPosition() - o2.getPosition();
					}
				});
				
			} else {
				chrComparator = COMPARATOR;
				Collections.sort(snps, new Comparator<ChrPosition>() {
					@Override
					public int compare(ChrPosition o1, ChrPosition o2) {
						int diff = COMPARATOR.compare(o1.getChromosome(), o2.getChromosome());
						if (diff != 0) return diff;
						return o1.getPosition() - o2.getPosition();
					}
				});
			}
			
			arraySize = snps.size();
		}
		
		private void advanceCPAndPosition() {
			if (arrayPosition >= arraySize) {
				// reached the end of the line
				cp = null;
				return;
			}
			if (null != cp) {
				// update QSnpRecord with our findings
				Accumulator acc = pileupMap.remove(cp);
				if (null != acc) {
					QSnpRecord rec = positionRecordMap.get(cp);
					
					String refString = rec.getRef();
					if (refString.length() > 1) {
						logger.warn("refString: " + refString + " in VcfPipeline.cleanSnpMap");
					}
					char ref = refString.charAt(0);
					
					PileupElementLite pel = acc.getLargestVariant(ref);
					if (isNormal) {
						rec.setNormalNucleotides(acc.getPileupElementString());
						rec.setNormalCount(acc.getCoverage());
						rec.setNormalPileup(acc.getPileup());
						rec.setNormalNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
					} else {
						// tumour fields
						rec.setTumourCount(acc.getCoverage());
						rec.setTumourNucleotides(acc.getPileupElementString());
						rec.setTumourNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
					}
				}
			}
			cp = snps.get(arrayPosition++);
		}
		
		private boolean match(SAMRecord rec, ChrPosition thisCPf, boolean updatePointer) {
			if (null == thisCPf) return false;
			if (rec.getReferenceName().equals(thisCPf.getChromosome())) {
				
				if (rec.getAlignmentEnd() < thisCPf.getPosition()) {
					return false;
				}
				
				if (rec.getAlignmentStart() <= thisCPf.getPosition()) {
					return true;
				}
				
				// finished with this cp - update results and get a new cp
				if (updatePointer) {
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
				
				
			} else if (chrComparator.compare(rec.getReferenceName(), thisCPf.getChromosome()) < 1) {
				// keep iterating through bam file 
				return false;
			} else {
				if (updatePointer) {
					// need to get next ChrPos
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
			}
		}
		private void updateResults(ChrPosition cp, SAMRecord sam, long readId) {
			// get read index
			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, cp.getPosition());
			
			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
				
				// no longer do any filtering on base quality
				final byte baseQuality = sam.getBaseQualities()[indexInRead]; 
				
				Accumulator acc = pileupMap.get(cp);
				if (null == acc) {
					acc = new Accumulator(cp.getPosition());
					Accumulator oldAcc = pileupMap.putIfAbsent(cp, acc);
					if (null != oldAcc) acc = oldAcc;
				}
				acc.addBase(sam.getReadBases()[indexInRead], baseQuality, ! sam.getReadNegativeStrandFlag(), 
						sam.getAlignmentStart(), cp.getPosition(), sam.getAlignmentEnd(), readId);
			}
		}
		
		@Override
		public void run() {
			try {
				createComparatorFromSAMHeader(reader);
				// reset some key values
				arrayPosition = 0;
				cp = null;
				// load first VCFRecord
				advanceCPAndPosition();
				long recordCount = 0;
				// take items off the queue and process
				for (SAMRecord sam : reader) {
						
					if (++recordCount % 1000000 == 0) {
						logger.info("Processed " + recordCount/1000000 + "M records so far..");
					}
					
					// quality checks
					if ( ! SAMUtils.isSAMRecordValidForVariantCalling(sam)) continue;
					
					if (match(sam, cp, true)) {
						updateResults(cp, sam, recordCount);
						
						// get next cp and see if it matches
						int j = 0;
						if (arrayPosition < arraySize) {
							ChrPosition tmpCP = snps.get(arrayPosition + j++);
							while (match(sam, tmpCP, false)) {
								updateResults(tmpCP, sam, recordCount);
								if (arrayPosition + j < arraySize)
									tmpCP = snps.get(arrayPosition + j++);
								else tmpCP = null;
							}
						}
					}
				}
				logger.info("Processed " + recordCount + " records");
				
			} finally {
				try {
					reader.close();
				} finally {
					latch.countDown();
				}
			}
		}
	}
}
