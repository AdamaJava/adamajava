/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import htsjdk.samtools.SAMRecord;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.QJumper;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.snp.filters.AdjacentIndelFilter;
import org.qcmg.snp.filters.EndOfReadFilter;
import org.qcmg.snp.filters.MultipleAdjacentSnpsFilter;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.RulesUtil;

/**
 */
public final class TorrentPipeline extends Pipeline {

	//input Files
//	private String pileupFile;
//	private String unfilteredNormalBamFile;
	
	
	private final Map<ChrPosition, Pair<List<PileupElement>, List<PileupElement>>> filteredPEs = 
		new HashMap<>();
	
	private final Map<ChrPosition, String> filteredInfo = new HashMap<ChrPosition, String>();
	
	final static QLogger logger = QLoggerFactory.getLogger(TorrentPipeline.class);

	/**
	 */
	public TorrentPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);
		
		// load rules from ini file
		ingestIni(iniFile);
		
		checkRules();
		
		loadNextReferenceSequence();
		
		checkBamHeaders();

		walkBams();
		
		if (positionRecordMap.isEmpty()) throw new SnpException("EMPTY_PILEUP_FILE");
		logger.info("Finished walking bams");
		
		incorporateUnfilteredNormal();
		
		strandBiasCorrection();
		
		compoundSnps();
		
		
		// run torrent filter against suspected snp positions
//		runTorrentFilters(normalBams, true);
//		runTorrentFilters(tumourBams, false);
		
		// re-run initial rules
//		reValidateSnpPositions();
		
		// time for post-processing
		// remove any previous annotations as they may no longer be valid
//		for (QSnpRecord record : positionRecordMap.values()) {
//			record.setAnnotation(null);
//		}
//		classifyPileup();
//		
//		checkForMutationInNormal();
		
		// write output
		writeVCF(vcfFile);
	}
	
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
			String refString = qsr.getRef();
			if (refString.length() > 1) {
				logger.warn("refString: " + refString + " at TorrentPipeline.reValidateSnpPositions");
			}
			char ref = refString.charAt(0);
			
			// get variant count for both
			int normalVariantCount = PileupElementUtil.getLargestVariantCount(normalPileupElements, ref);
			int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourPileupElements, ref);
			
			int normalBaseCounts = PileupElementUtil.getCoverageFromPileupList(normalPileupElements);
			int tumourlBaseCounts = PileupElementUtil.getCoverageFromPileupList(tumourPileupElements);
			// get rule for normal and tumour
			Rule normalRule = RulesUtil.getRule(controlRules, normalBaseCounts);
			Rule tumourRule = RulesUtil.getRule(testRules, tumourlBaseCounts);
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
			String refString = rec.getRef();
			if (refString.length() > 1) {
				logger.warn("refString: " + refString + " at TorrentPipeline.runTorrentFilters");
			}
			char ref = refString.charAt(0);
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
			
			MultipleAdjacentSnpsFilter multipleSnpsFilter = new MultipleAdjacentSnpsFilter(rec.getPosition());
			AdjacentIndelFilter adjIndelFilter = new AdjacentIndelFilter(rec.getPosition());
			EndOfReadFilter endOfReadFilter = new EndOfReadFilter(5, rec.getPosition());
			
			List<SAMRecord> passingRecords = new ArrayList<SAMRecord>();
			
			StringBuilder bases = new StringBuilder();
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
					qualities.append(htsjdk.samtools.SAMUtils.phredToFastq(sam.getBaseQualities()[readPosition]));
				} else {
					deletionCount++;
				}
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
		
	@Override
	void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		controlRules = IniFileUtil.getRules(ini, "control");
		testRules = IniFileUtil.getRules(ini, "test");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
		
		
		// ADDITIONAL INPUT FILES
//		pileupFile = IniFileUtil.getInputFile(ini, "pileup");
		
		// ADDITIONAL SETUP	
//		noOfControlFiles = IniFileUtil.getNumberOfFiles(ini, 'N');
//		noOfTestFiles = IniFileUtil.getNumberOfFiles(ini, 'T');
		
		// INCLUDE INDELS
		includeIndels = true;
		
		// log values retrieved from ini file
//		logger.tool("**** ADDITIONAL INPUT FILES ****");
//		logger.tool("pileupFile: " + pileupFile);
//		logger.tool("unfilteredNormalBamFile: " + unfilteredNormalBamFile);
		
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("No of control rules: " + controlRules.size());
		logger.tool("No of test rules: " + testRules.size());
		logger.tool("min coverage count for initial test: " + initialTestSumOfCountsLimit);
//		logger.tool("number of normal files in pileup: " + noOfControlFiles);
//		logger.tool("number of tumour files in pileup: " + noOfTestFiles);
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}
	
	@Override
	public String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	@Override
	public String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

}
