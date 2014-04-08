/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
import org.qcmg.common.model.QSnpGATKRecord;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfUtils;
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
	
	private final ConcurrentMap<ChrPosition, Accumulator> normalPileup = new ConcurrentHashMap<ChrPosition, Accumulator>();
	private final ConcurrentMap<ChrPosition, Accumulator> tumourPileup = new ConcurrentHashMap<ChrPosition, Accumulator>();

	private final ConcurrentMap<ChrPosition, QSnpGATKRecord> normalVCFMap = new ConcurrentHashMap<ChrPosition, QSnpGATKRecord>(12500); //not expecting more than 100000
	private final ConcurrentMap<ChrPosition, QSnpGATKRecord> tumourVCFMap = new ConcurrentHashMap<ChrPosition, QSnpGATKRecord>(12500);
	
	//input Files
	private String vcfNormalFile, vcfTumourFile;
	private String normalBam;
	private String tumourBam;
	private int mutationId;

	/**
	 */
	public VcfPipeline(final Ini iniFile, QExec qexec) throws SnpException, IOException, Exception {
		super(qexec);

		// load rules from ini file
		ingestIni(iniFile);
		
		// setup normalBam and tumourBam variables
		if (null != normalBams && normalBams.length > 0)
			normalBam = normalBams[0];
		else throw new SnpException("No normal bam file specified");
		if (null != tumourBams && tumourBams.length > 0)
			tumourBam = tumourBams[0];
		else throw new SnpException("No tumour bam file specified");
		
		// load vcf files
		logger.info("Loading normal vcf data");
		loadVCFData(vcfNormalFile, normalVCFMap);
		logger.info("Loading normal vcf data - DONE [" + normalVCFMap.size() + "]");
		
		logger.info("Loading tumour vcf data");
		loadVCFData(vcfTumourFile, tumourVCFMap);
		logger.info("Loading tumour vcf data - DONE [" + tumourVCFMap.size() + "]");
		
		if (normalVCFMap.isEmpty() && tumourVCFMap.isEmpty()) throw new SnpException("EMPTY_VCF_FILES");
		else if (normalVCFMap.isEmpty()) logger.warn("no data in control vcf file - all calls will be somatic");
		else if (tumourVCFMap.isEmpty()) logger.warn("no data in test vcf file - all calls will be germline");
		
		logger.info("converting vcf data to qsnp data");
		convertVCFToQSnpFormat();
		logger.info("converting vcf data to qsnp data - DONE [" + positionRecordMap.size() + "]");
		
		// add pileup from the normal bam file
		logger.info("adding pileup to vcf records map");
		addPileup();
		logger.info("adding pileup to vcf records map - DONE");
		
		cleanSnpMap();

		// time for post-processing
		classifyPileup();
		
		// load in IlluminaData - this will need to be appended to with dbSnp location details
		// when we go through the dbSnp file
		if ( ! StringUtils.isNullOrEmpty(illuminaNormalFile ) && ! StringUtils.isNullOrEmpty(illuminaTumourFile)) {
			logger.info("Loading illumina normal data");
			loadIlluminaData(illuminaNormalFile, normalIlluminaMap);
			logger.info("Loading illumina tumour data");
			loadIlluminaData(illuminaTumourFile, tumourIlluminaMap);
		}
		
		if ( ! StringUtils.isNullOrEmpty(dbSnpFile)) {
			// add dbSNP ids
			logger.info("Loading dbSNP data");
			addDbSnpData(dbSnpFile);
		}
		
		// need chromosome conversion data
		logger.info("Loading chromosome conversion data");
		loadChromosomeConversionData(chrConvFile);
		
		if ( ! StringUtils.isNullOrEmpty(germlineDBFile)) {
			logger.info("Loading germline DB data");
			addGermlineDBData(germlineDBFile);
		} 
		
		// write output
		writeVCF(vcfFile);
		if ( ! vcfOnlyMode) writeOutputForDCC();

	}
	
	@Override
	String getDccMetaData() throws Exception {
		if (null == normalBams || normalBams.length == 0 || 
				StringUtils.isNullOrEmpty(normalBams[0]) 
				|| null == tumourBams || tumourBams.length == 0 
				|| StringUtils.isNullOrEmpty(tumourBams[0])) return null;
		
		SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(normalBams[0]).getFileHeader();
		SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(tumourBams[0]).getFileHeader();
		
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
				record.setNormalGenotype(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getRef()));
				normalUpdated++;
			}
			
			// if we don't have a tumour vcf call, just classify as Germline, annotate and off we go  
			if (null == record.getTumourGenotype()) {
				record.setClassification(Classification.GERMLINE);
				record.addAnnotation("no call in tumour vcf");
				
//				record.setTumourGenotype(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getRef()));
				tumourUpdated++;
			}
		}
		
		logger.info("added genotypes (hom ref) for " + normalUpdated + " normals, and set classification to GERMLINE for " + tumourUpdated + " tumours");
	}

	private void addPileup() throws Exception {
		
		logger.info("Setting up Pileup threads");
		long start = System.currentTimeMillis();
		
		final CountDownLatch latch = new CountDownLatch(2);
		final ExecutorService service = Executors.newFixedThreadPool(2);
		
		service.execute(new Pileup(normalBam, latch, true));
		service.execute(new Pileup(tumourBam, latch, false));
		service.shutdown();
		
		try {
			latch.await();
			logger.info("Pileup threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
		} catch (InterruptedException ie) {
			logger.error("InterruptedException caught",ie);
			Thread.currentThread().interrupt();
		}
	}
	
	
	private void convertVCFToQSnpFormat() {
		
		// loop through normal vcf map - get corresponding tumour if present
		for (Entry<ChrPosition, QSnpGATKRecord> entry : normalVCFMap.entrySet()) {
			QSnpGATKRecord tumour = tumourVCFMap.get(entry.getKey());
			positionRecordMap.put(entry.getKey(), getQSnpRecord(entry.getValue(), tumour));
		}
		
		// loop through tumour, making sure entry exists in qsnp map
		for (Entry<ChrPosition, QSnpGATKRecord> entry : tumourVCFMap.entrySet()) {
			
			QSnpRecord snpRecord = positionRecordMap.get(entry.getKey());
			if (null == snpRecord) {
				positionRecordMap.put(entry.getKey(), getQSnpRecord(null, entry.getValue()));
			}
		}
	}
	
	private  QSnpRecord getQSnpRecord(QSnpGATKRecord normal, QSnpGATKRecord tumour) {
		QSnpRecord qpr = new QSnpRecord();
		qpr.setId(++mutationId);
		
		if (null != normal) {
			qpr.setChromosome(normal.getChromosome());
			qpr.setPosition(normal.getPosition());
			qpr.setRef(normal.getRef());
			qpr.setNormalGenotype(normal.getGenotypeEnum());
			qpr.setAnnotation(normal.getAnnotation());
			// tumour fields
			qpr.setTumourGenotype(null == tumour ? null : tumour.getGenotypeEnum());
			qpr.setTumourCount(null == tumour ? 0 :  VcfUtils.getDPFromFormatField(tumour.getGenotype()));
//			qpr.setTumourNucleotides(null == tumour ? null :  PileupElementUtil.getPileupElementString(tumour.getPileup(), tumour.getRef()));
			
		} else if (null != tumour) {
			qpr.setChromosome(tumour.getChromosome());
			qpr.setPosition(tumour.getPosition());
			qpr.setRef(tumour.getRef());
			qpr.setTumourGenotype(tumour.getGenotypeEnum());
			qpr.setTumourCount(VcfUtils.getDPFromFormatField(tumour.getGenotype()));
		}
		
		return qpr;
	}

	private static void loadVCFData(String vcfFile, Map<ChrPosition,QSnpGATKRecord> map) throws Exception {
		if (FileUtils.canFileBeRead(vcfFile)) {
			
			VCFFileReader reader  = new VCFFileReader(new File(vcfFile));
			try {
				for (VCFRecord qpr : reader) {
					map.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),new QSnpGATKRecord(qpr));
				}
			} finally {
				reader.close();
			}
		}
	}

	@Override
	protected void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);

		// ADDITIONAL INPUT FILES
		vcfNormalFile = IniFileUtil.getInputFile(ini, "vcfNormal");
		vcfTumourFile = IniFileUtil.getInputFile(ini, "vcfTumour");
		
		// log values retrieved from in file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("normal vcf file: " + vcfNormalFile);
		logger.tool("tumour vcf file: " + vcfTumourFile);
		
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
//		private final String bamFile;
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
//			this.bamFile = bamFile;
			this.isNormal = isNormal;
			pileupMap = isNormal ? normalPileup : tumourPileup;
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
//					logger.info("updating cp: " + cp.toIGVString() + " with acc: " + acc.getPileup());
					
					PileupElementLite pel = acc.getLargestVariant(rec.getRef());
					if (isNormal) {
						rec.setNormalNucleotides(acc.getPileupElementString());
						rec.setNormalCount(acc.getCoverage());
						rec.setNormalPileup(acc.getPileup());
						rec.setNormalNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
					} else {
						// tumour fields
	//					rec.setTumourGenotype(null == tumour ? null : tumour.getGenotypeEnum());
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
				
				if (rec.getAlignmentEnd() < thisCPf.getPosition())
					return false;
				
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
				
				
			} else if (chrComparator.compare(rec.getReferenceName(), thisCPf.getChromosome()) < 1){
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
		private void updateResults(ChrPosition cp, SAMRecord sam) {
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
						sam.getAlignmentStart(), cp.getPosition(), sam.getAlignmentEnd());
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
//						logger.info("got a match!");
						updateResults(cp, sam);
						
						// get next cp and see if it matches
						int j = 0;
						if (arrayPosition < arraySize) {
							ChrPosition tmpCP = snps.get(arrayPosition + j++);
							//					ChrPosition tmpCP = snps.get(arrayPosition + j++);
							while (match(sam, tmpCP, false)) {
								//								logger.info("got a subsequent match!");
								updateResults(tmpCP, sam);
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
