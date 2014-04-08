/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.samtools.SAMRecord;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.QJumper;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.PileupFileReader;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.snp.filters.AdjacentIndelFilter;
import org.qcmg.snp.filters.EndOfReadFilter;
import org.qcmg.snp.filters.MultipleAdjacentSnpsFilter;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.QJumperWorker;
import org.qcmg.snp.util.RulesUtil;
import org.qcmg.snp.util.QJumperWorker.Mode;

/**
 */
public final class TorrentPipeline extends Pipeline {

	//input Files
	private String pileupFile;
	private String unfilteredNormalBamFile;
	
	private final Map<ChrPosition, Pair<List<PileupElement>, List<PileupElement>>> filteredPEs = 
		new HashMap<ChrPosition, Pair<List<PileupElement>, List<PileupElement>>>();
	
	private final Map<ChrPosition, String> filteredInfo = new HashMap<ChrPosition, String>();
	
	private final static QLogger logger = QLoggerFactory.getLogger(TorrentPipeline.class);

	/**
	 */
	public TorrentPipeline(final Ini iniFile, QExec qexec) throws SnpException, IOException, Exception {
		super(qexec);
		
		// load rules from ini file
		ingestIni(iniFile);
		
		checkRules();

		// populate the positions int arrays
		getStringPositions();

		// walk the pileup, keeping a count of all records, and those that pass
		// our initial tests
//		logger.info("Loading Samtools mpileup data");
//		walkPileup(pileupFile);
//		logger.info("Loading Samtools mpileup data - DONE");
		
		walkBams(false);
		
		if (positionRecordMap.isEmpty()) throw new SnpException("EMPTY_PILEUP_FILE");
		logger.info("Finished walking bams");
		
		
		// run torrent filter against suspected snp positions
		runTorrentFilters(normalBams, true);
		runTorrentFilters(tumourBams, false);
		
		// re-run initial rules
		reValidateSnpPositions();
		
		// time for post-processing
		// remove any previous annotations as they may no longer be valid
		for (QSnpRecord record : positionRecordMap.values()) {
			record.setAnnotation(null);
		}
		classifyPileup();
		
		checkForMutationInNormal();
		
		if (! StringUtils.isNullOrEmpty(dbSnpFile) ) {
			logger.info("Loading dbSNP data");
			addDbSnpData(dbSnpFile);
		} else {
			logger.info("Skipping loading of dbSNP data - No dbSNP file specified");
		}
		
		
		// STOP here if only outputting vcf
//		if ( ! vcfOnlyMode) {
//		
//			logger.info("perform pileup on unfiltered bam for any SOMATIC positions (that don't already see the mutation in the normal)");
//			addUnfilteredBamPileup();
//			
			// load in IlluminaData - this will need to be appended to with dbSnp location details
			// when we go through the dbSnp file
			if ( ! StringUtils.isNullOrEmpty(illuminaNormalFile ) && ! StringUtils.isNullOrEmpty(illuminaTumourFile)) {
				logger.info("Loading illumina normal data");
				loadIlluminaData(illuminaNormalFile, normalIlluminaMap);
				logger.info("Loaded " + normalIlluminaMap.size() + " entries into the illumina map from file: " + illuminaNormalFile);
				logger.info("Loading illumina tumour data");
				loadIlluminaData(illuminaTumourFile, tumourIlluminaMap);
				logger.info("Loaded " + tumourIlluminaMap.size() + " entries into the illumina map from file: " + illuminaTumourFile);
			}
//			
			if ( ! tumourIlluminaMap.isEmpty() && ! normalIlluminaMap.isEmpty())
				addIlluminaData();
			else 
				logger.info("No Illumina data will be used (files don't exist or are empty");
//			
			// need chromosome conversion data
			logger.info("Loading chromosome conversion data");
			loadChromosomeConversionData(chrConvFile);
//			
			// load GermlineDB data
			
			if ( ! StringUtils.isNullOrEmpty(germlineDBFile)) {
				logger.info("Loading germline DB data");
				addGermlineDBData(germlineDBFile);
			}
//			
//			if (updateGermlineDB) {
//				logger.info("updating germlineDB with germline snips");
//				updateGermlineDB(germlineDBFile);
//			}
//			
			// write output
			writeVCF(vcfFile);
			writeOutputForDCC();
	}
	
	
	

	
//	@Override
//	public  VCFRecord convertQSnpToVCF(QSnpRecord rec) {
//		VCFRecord vcf = new VCFRecord();
//		vcf.setChromosome(rec.getChromosome());
//		vcf.setPosition(rec.getPosition());
//		vcf.setId(rec.getDbSnpId());
//		vcf.setRef(rec.getRef());
//		vcf.setAlt(null == rec.getMutation() ? "." : rec.getMutation().substring(rec.getMutation().length() - 1 ));
//		
//		
////		sb.append('.').append(TAB);	// qual
////		sb.append('.').append(TAB);	// filter
////		sb.append('.').append(TAB);	// info
//		
//		StringBuilder info = new StringBuilder();
//		if (Classification.SOMATIC == rec.getClassification())
//			info.append(rec.getClassification().toString()).append(COLON);
//			
//		// add coverage to the info field
//		info.append(null != rec.getNormalNucleotides() ? rec.getNormalNucleotides() : "--").append(COLON);
//		info.append(null != rec.getTumourNucleotides() ? rec.getTumourNucleotides() : "--");
//		
//		// FORMAT field - contains GT field (and others)
//		StringBuilder formatField = new StringBuilder();
//		if (Classification.SOMATIC == rec.getClassification())
//			formatField.append(VcfUtils.calculateGTField(rec.getTumourGenotype()));
//		else {
//			if (null != rec.getNormalGenotype())
//				formatField.append(VcfUtils.calculateGTField(rec.getNormalGenotype()));
//			else if (null != rec.getTumourGenotype())
//				formatField.append(VcfUtils.calculateGTField(rec.getTumourGenotype()));
//		}
//		
//		// 
//		StringBuilder extra = new StringBuilder();
//		extra.append(null != rec.getNormalGenotype() ? rec.getNormalGenotype().getDisplayString() : "--").append(COLON);
//		extra.append(null != rec.getTumourGenotype() ? rec.getTumourGenotype().getDisplayString() : "--").append(COLON);
//		extra.append(null != rec.getMutation() ? rec.getMutation() : "");
//		
//		// add in filterinfo if it exists
//		String filterInfo = filteredInfo.get(new ChrPosition(rec.getChromosome(), rec.getPosition()));
//		if (null != filterInfo) {
//			vcf.setFilter(filterInfo);
//		}
//		
//		vcf.setInfo(info.toString());
//		vcf.addExtraField(formatField.toString());
//		vcf.addExtraField(extra.toString());
//		return vcf;
//	}
	
//	void checkForMutationInNormal() {
//		int minCount = 0;
//		for (QSnpRecord record : positionRecordMap.values()) {
//			if (null != record.getAnnotation() && record.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL)) {
//				// check to see if mutant count in normal is 3% or more
//				// if not, remove annotation
//				final String ND = record.getNormalNucleotides();
//				final int normalCount = record.getNormalCount();
//				final char alt = record.getMutation().charAt(record.getMutation().length()-1);
//				final int altCount = SnpUtils.getVariantCountFromNucleotideString(ND, alt);
//				
//				if (((float)altCount / normalCount) * 100 < 3.0f) {
//					record.removeAnnotation(SnpUtils.MUTATION_IN_NORMAL);
//					minCount++;
//				}
//			}
//		}
//		logger.info("no of records with " + SnpUtils.MUTATION_IN_NORMAL + " annotation removed: " + minCount);
//	}

	private void reValidateSnpPositions() {
		
		int removedCount = 0, totalCount = 0;
		
		for (Entry<ChrPosition, Pair<List<PileupElement>, List<PileupElement>>> entry : filteredPEs.entrySet()) {
			boolean remove = true;
			totalCount++;
			
			List<PileupElement> normalPileupElements = entry.getValue().getLeft();
			List<PileupElement> tumourPileupElements = entry.getValue().getRight();
			ChrPosition cp = entry.getKey();
			
			// get reference
			QSnpRecord qsr = positionRecordMap.get(cp);
			char ref = qsr.getRef();
			
			// get variant count for both
			int normalVariantCount = PileupElementUtil.getLargestVariantCount(normalPileupElements, ref);
			int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourPileupElements, ref);
			
			int normalBaseCounts = PileupElementUtil.getCoverageFromPileupList(normalPileupElements);
			int tumourlBaseCounts = PileupElementUtil.getCoverageFromPileupList(tumourPileupElements);
			// get rule for normal and tumour
			Rule normalRule = RulesUtil.getRule(normalRules, normalBaseCounts);
			Rule tumourRule = RulesUtil.getRule(tumourRules, tumourlBaseCounts);
			if (cp.getChromosome().equals("chr21") && cp.getPosition() == 46334140) {
				logger.info("normalVariantCount : " + normalVariantCount);
				logger.info("tumourVariantCount : " + tumourVariantCount);
				logger.info("normalBaseCounts : " + normalBaseCounts);
				logger.info("tumourlBaseCounts : " + tumourlBaseCounts);
				logger.info("normalRule : " + normalRule);
				logger.info("tumourRule : " + tumourRule);
				logger.info("normalPileupElements : " + PileupElementUtil.getPileupFromPileupList(normalPileupElements));
				logger.info("tumourPileupElements : " + PileupElementUtil.getPileupFromPileupList(tumourPileupElements));
			}

			// only keep record if it has enough variants
			if (isPileupRecordAKeeper(normalVariantCount, normalBaseCounts, normalRule, normalPileupElements, baseQualityPercentage) 
					||  isPileupRecordAKeeper(tumourVariantCount, tumourlBaseCounts, tumourRule, tumourPileupElements, baseQualityPercentage)) {
				remove = false;
				
				// if we have an entry in the filteringINfo map for this position, we have modified values for either ND or TD (or both)
				// update qsr record
				String info = filteredInfo.get(cp);
				if (null != info) {
					// do we need to update ND
					if (info.contains("ND")) {
						qsr.setNormalNucleotides(PileupElementUtil.getPileupElementString(normalPileupElements, ref));
					}
					if (info.contains("TD")) {
						qsr.setTumourNucleotides(PileupElementUtil.getPileupElementString(tumourPileupElements, ref));
					}
				}
			}
			
			if (remove) {
				// remove item from positions map
				positionRecordMap.remove(entry.getKey());
				removedCount++;
			}
		}
		logger.info("removed: " + removedCount + ", from " + totalCount);
	}
		
	private void runTorrentFilters(String [] bamFiles, boolean normal) throws Exception {
		
		QJumper qj = new QJumper();
		qj.setupReader(bamFiles);
		
		
		for (QSnpRecord rec : positionRecordMap.values()) {
			
			char alt = '\u0000';
			char ref = rec.getRef();
			List<SAMRecord> sams = qj.getOverlappingRecordsAtPosition(rec.getChromosome(), rec.getPosition());
			
			// get list of pileupelements from ND/TD fields
			String pileupsString = normal ? rec.getNormalNucleotides() : rec.getTumourNucleotides();
			List<PileupElement> pileups =  PileupElementUtil.createPileupElementsFromString(pileupsString);
			
			
			if (null == pileups || pileups.size() == 0) {
				logger.info("no (empty??)" + (normal ? "ND" : "TD") + " field found for rec: " + rec.toString());
				continue;
			} else if (pileups.size() == 1) {
				char base = pileups.get(0).getBase();
				if (ref == base) {
					continue;
				} else {
					alt = base;
				}
			} else {
				// sort collection so that largest count is first
				Collections.sort(pileups);
				// get largest variant - set alt to this value
				alt = PileupElementUtil.getLargestVariant(pileups, ref).getBase();
				
			}
			
//			GenotypeEnum genotype = normal ? rec.getNormalGenotype() : rec.getTumourGenotype();
//			if (null != genotype) {
//				if (genotype.isHomozygous() && genotype.getFirstAllele() != ref) {
//					alt = genotype.getFirstAllele();
//				} else if (genotype.isHeterozygous() ) {
//					alt = genotype.getFirstAllele() == ref ? genotype.getSecondAllele() : genotype.getFirstAllele();
//				}
//			}
			
			MultipleAdjacentSnpsFilter multipleSnpsFilter = new MultipleAdjacentSnpsFilter(rec.getPosition());
			AdjacentIndelFilter adjIndelFilter = new AdjacentIndelFilter(rec.getPosition());
			EndOfReadFilter endOfReadFilter = new EndOfReadFilter(5, rec.getPosition());
			
			List<SAMRecord> passingRecords = new ArrayList<SAMRecord>();
			
			StringBuilder bases = new StringBuilder();
//			String qualities = "";
//			byte [] qualities = new byte[sams.size()];
			StringBuilder qualities = new StringBuilder();
			int passedFilterCount = 0;
			int variantCount = 0;
			int deletionCount = 0;
			int i = 0;
			int tripleSnpCount = 0, adjIndelCount = 0, endOfReadCount = 0;
			
			for (SAMRecord sam : sams) {
				
				
				//ONLY FILTER READS THAT CARRY THE ALT ALLELE
				int readPosition = SAMUtils.getIndexInReadFromPosition(sam, rec.getPosition());
				if (readPosition > -1) {
					byte base = sam.getReadBases()[readPosition];
					
					// run through filters if we have the alt
					if (base == alt) {
						// go through each of the filters in turn
						if (adjIndelFilter.filterOut(sam)) {
							adjIndelCount++;
							if (rec.getChromosome().equals("chr1") && rec.getPosition() == 27101507)
								logger.info("adj indel filter fail : " + sam.getSAMString());
							continue;
						}
						if (multipleSnpsFilter.filterOut(sam)) {
							tripleSnpCount++;
							if (rec.getChromosome().equals("chr1") && rec.getPosition() == 27101507)
								logger.info("triple snp filter fail : " + sam.getSAMString());
							continue;
						}
						if (endOfReadFilter.filterOut(sam)) {
							endOfReadCount++;
							if (rec.getChromosome().equals("chr1") && rec.getPosition() == 27101507)
								logger.info("eor filter fail : " + sam.getSAMString());
							continue;
						}
					}
					
					passedFilterCount++;
					passingRecords.add(sam);
				
					if (base == alt) {
						variantCount++;
					}
					// set base - use strand info to set case forward strand is upper case, reverse is lower
					bases.append(sam.getReadNegativeStrandFlag() ? Character.toLowerCase((char)base) : (char)base);
	//				qualities[i++]= sam.getBaseQualities()[readPosition];
					qualities.append(net.sf.samtools.SAMUtils.phredToFastq(sam.getBaseQualities()[readPosition]));
				} else {
					deletionCount++;
				}
//					logger.info("sam.getAlignmentStart" + sam.getAlignmentStart() + ", position: " + rec.getPosition());}
			}
			ChrPosition cp = new ChrPosition(rec.getChromosome(), rec.getPosition());
			
			if (adjIndelCount + tripleSnpCount + endOfReadCount > 0) {
				// add entry to filteredINfo map
				String info = filteredInfo.get(cp);
				if (null == info) {
					info = (normal ? "ND" : "TD") + (adjIndelCount > 0 ? ":" + adjIndelCount + "AI" : "")
					+ (tripleSnpCount > 0 ? ":" + tripleSnpCount + "TS" : "")
					+ (endOfReadCount > 0 ? ":" + endOfReadCount + "ER" : "");
				} else {
					info += (normal ? ";ND" : ";TD") + (adjIndelCount > 0 ? ":" + adjIndelCount + "AI" : "")
					+ (tripleSnpCount > 0 ? ":" + tripleSnpCount + "TS" : "")
					+ (endOfReadCount > 0 ? ":" + endOfReadCount + "ER" : "");
				}
				filteredInfo.put(cp, info);
			}
			
			if (bases.length() != qualities.length()) {
				logger.info("bases : " + bases.toString() + ", qual: " + qualities.toString());
			}
//			logger.info(rec.getChromosome() + ":" + rec.getPosition() + ", deletionCount: " + deletionCount);
			if (rec.getChromosome().equals("chr21") && rec.getPosition() == 46334140) {
				logger.info("bases : " + bases.toString() + ", qual: " + qualities.toString() + ", ref: " + ref + ", alt: " + alt);
			}
			
			List<PileupElement> baseCounts = PileupElementUtil.getPileupCounts(bases.toString(), qualities.toString());
			
			Pair<List<PileupElement>, List<PileupElement>> p = filteredPEs.get(cp);
			if (null == p) {
				p = normal ? new Pair(baseCounts, null) : new Pair(null, baseCounts);
				filteredPEs.put(cp, p);
			} else {
				 p = normal ? new Pair(baseCounts, p.getRight()) : new Pair(p.getLeft(), baseCounts);
				 filteredPEs.put(cp, p);
			}
		}
	}
		
	private void  addUnfilteredBamPileup() throws Exception{
		if (null == unfilteredNormalBamFile) {
			logger.info("No unfiltered bam file provided");
			return;
		}
		
		long start = System.currentTimeMillis();
		int noOfThreads = 2;
		
		final CountDownLatch latch = new CountDownLatch(noOfThreads);
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
		
		// we want a submap of normal vcfs that don't have tumour entries
		Map<ChrPosition, QSnpRecord> somaticNoRecordOfMutationInNormal = new TreeMap<ChrPosition, QSnpRecord>();
		for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
			QSnpRecord record = entry.getValue();
			if (Classification.SOMATIC == record.getClassification()
					&& (null == record.getAnnotation() 
					|| ! record.getAnnotation().contains("mutation also found in pileup of normal"))) {
				somaticNoRecordOfMutationInNormal.put(entry.getKey(), record);
			}
		}
		
		logger.info("number of SOMATIC snps that don't have evidence of mutation in normal: " + somaticNoRecordOfMutationInNormal.size());
		
		Deque<QSnpRecord> deque = new ConcurrentLinkedDeque<QSnpRecord>();
		for (QSnpRecord rec : somaticNoRecordOfMutationInNormal.values()) {
			deque.add(rec);
		}
		
		for (int i = 0 ; i < noOfThreads ; i++) {
			service.execute(new QJumperWorker<QSnpRecord>(latch,unfilteredNormalBamFile, 
					deque, Mode.QSNP_MUTATION_IN_NORMAL));
//			service.execute(new QJumperWorker(latch,unfilteredNormalBamFile, 
//					somaticNoRecordOfMutationInNormal, Mode.QSNP_MUTATION_IN_NORMAL));
//			service.execute(new QJumperWorker(latch,unfilteredNormalBamFile, unfilteredNormalBamIndexFile,
//					somaticNoRecordOfMutationInNormal, Mode.QSNP_MUTATION_IN_NORMAL));
		}
		service.shutdown();
		
		try {
			latch.await();
			logger.info("QJumper threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
		} catch (InterruptedException ie) {
			logger.error("InterruptedException caught",ie);
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		normalRules = IniFileUtil.getRules(ini, "normal");
		tumourRules = IniFileUtil.getRules(ini, "tumour");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
		
		// MINIMUM BASE QUALITY
//		String baseQualString = IniFileUtil.getEntry(ini, "parameters", "minimumBaseQuality");
//		if (null != baseQualString)
//			minimumBaseQualityScore = Integer.parseInt(baseQualString);
		
		// ADDITIONAL INPUT FILES
		pileupFile = IniFileUtil.getInputFile(ini, "pileup");
		
		// ADDITIONAL SETUP	
//		mutationIdPrefix = patientId + "_SNP_";
		noOfNormalFiles = IniFileUtil.getNumberOfFiles(ini, 'N');
		noOfTumourFiles = IniFileUtil.getNumberOfFiles(ini, 'T');
		
		// INCLUDE INDELS
//		String includeIndelsString = IniFileUtil.getEntry(ini, "flags", "includeIndels"); 
//		includeIndels = (null != includeIndelsString && "true".equalsIgnoreCase(includeIndelsString));
		includeIndels = true;
		
		// log values retrieved from ini file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("pileupFile: " + pileupFile);
		logger.tool("unfilteredNormalBamFile: " + unfilteredNormalBamFile);
		
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("No of normal rules: " + normalRules.size());
		logger.tool("No of tumour rules: " + tumourRules.size());
//		logger.tool("Minimum Base Quality Score: " + minimumBaseQualityScore);
		logger.tool("min coverage count for initial test: " + initialTestSumOfCountsLimit);
		logger.tool("number of normal files in pileup: " + noOfNormalFiles);
		logger.tool("number of tumour files in pileup: " + noOfTumourFiles);
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}
	
//	private void checkOutputFiles() throws SnpException {
//		// loop through supplied files - check they can be read
//		String [] files = new String[] {qcmgPileupFile, dccSomaticFile, dccGermlineFile};
//		
//		for (String file : files) {
//			
//			if (null == file || ! FileUtils.canFileBeWrittenTo(file)) {
//				throw new SnpException("OUTPUT_FILE_WRITE_ERROR" , file);
//			}
//		}
//	}

	@Override
	public String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	@Override
	public String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

	private void walkPileup(String pileupFileName) throws Exception {
		PileupFileReader reader = new PileupFileReader(new File(pileupFileName));
		long count = 0;
		try {
			for (String record : reader) {
				parsePileup(record);
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " pileup records, with " + mutationId + " keepers.");
			}
		} finally {
			reader.close();
		}
	}
	
}
