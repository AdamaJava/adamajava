/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qsv.discordantpair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;


public class FindMatePairsMT implements Runnable {

	private static QLogger logger = QLoggerFactory.getLogger(FindMatePairsMT.class);
	private static final int WRITE_COUNT = 1000000;

	private final CountDownLatch countDownLatch;
	private final QSVParameters qsvParameters;
	private final Map<PairClassification, MatePairsWriter> matePairWritersMap;
	private final Thread mainThread;
	private final String outName;
	private final String matePairDir;
	private final boolean isQCMG;
	private final AtomicInteger exitStatus;

	private long totalCount = 0;
	private long matePairCount = 0;
	private long singletonCount = 0;

	public FindMatePairsMT(Thread mainThread, CountDownLatch countDownLatch, QSVParameters qsvParameters, AtomicInteger exitStatus, String outName, String outDir, boolean isQCMG) {
		this.countDownLatch = countDownLatch;
		this.qsvParameters = qsvParameters;
		this.matePairWritersMap = new HashMap<PairClassification, MatePairsWriter>();
		this.exitStatus = exitStatus;
		this.mainThread = mainThread;
		this.outName = outName;
		this.matePairDir = outDir;
		this.isQCMG = isQCMG;
	}

	@Override
	public void run() {
		try {            
			// extract pairs from sorted bam file
			extractPairs();

			// write pairs to file
			writeMatePairsToFile();            

			exitStatus.set(0);
		} catch (Exception e) {
			logger.error("Error while parsing SAMRecords", e);
			exitStatus.set(1);
			mainThread.interrupt();
		} finally {
			// decrement the countdown by one
			countDownLatch.countDown();
		}
	}

	private void extractPairs() throws Exception {
		logger.info("Starting to extract pairs for file: " + qsvParameters.getFilteredBamFile());

		setUpPairingClassificationWriters();
		SAMRecord previousRecord = null;
		// stores records with same read name to check for read groups
		Map<String, SAMRecord> potentialMatches = new HashMap<>();

		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(qsvParameters.getFilteredBamFile(), "silent");) {

			for (SAMRecord currentRecord : reader) {
				totalCount++;
				String currentRecordReadGroupId = currentRecord.getReadGroup().getId();
				String key = currentRecord.getReadName() + ":" + currentRecordReadGroupId;

				// check to see if there are previous matches
				
				SAMRecord recordFromMap = potentialMatches.get(key);
				
				if (null != recordFromMap) {
					if (passesMateFiltering(recordFromMap, currentRecord, currentRecordReadGroupId)) {
						addMatePairToWriter(recordFromMap, currentRecord);
						matePairCount++;
						potentialMatches.remove(key);
					} else {
						singletonCount++;
					}
					previousRecord = null;
				} else {
					if (previousRecord != null) {
						// same read name
						if (previousRecord.getReadName().equals(currentRecord.getReadName())) {                       
							if (passesMateFiltering(previousRecord, currentRecord, currentRecordReadGroupId)) {
								addMatePairToWriter(previousRecord, currentRecord);
								matePairCount++;
								previousRecord = null;
							} else {
								// not same read read group, and not already in the list of potential matches
								// so add to the list
								potentialMatches.put(key, currentRecord);
								previousRecord = currentRecord;
							}                        
						} else {
							// not same read name, so move onto the next set
							potentialMatches.clear();
							previousRecord = currentRecord;
							potentialMatches.put(key, currentRecord);
							singletonCount++;
							if (totalCount == 2) {
								singletonCount++;
							}
						}
					} else {                              
						previousRecord = currentRecord;
						if (totalCount == 1) {
							potentialMatches.put(key, currentRecord);
						}
					}
				}
			}
		}
		logger.info("Finished extracting pairs for file: " + qsvParameters.getFilteredBamFile());
		logger.info(qsvParameters.getFindType() + " file pairs found: " + matePairCount + " (reads:" + matePairCount * 2 + ") | Unmatched reads: " + singletonCount + " | Total: " + totalCount);
		if (totalCount < 1000) {
			logger.warn("Less than 1000 mate pairs were found. The input bam file may not be sorted and filtered correctly");
		}
	}

	public boolean passesMateFiltering(SAMRecord samRecord) {
		return ! samRecord.getMateUnmappedFlag()
				&& passesZPFilter(samRecord)
				&& matesMapToChromosome(samRecord);
	}
	
	private boolean passesMateFiltering(SAMRecord previousRecord, SAMRecord currentRecord, String currentReadGroupId) throws Exception {

		if ( ! previousRecord.getReadGroup().getId().equals(currentReadGroupId)) {
			return false;
		} else if (previousRecord.getNotPrimaryAlignmentFlag() || currentRecord.getNotPrimaryAlignmentFlag()) {
			return false;
		} else if ( ! currentRecord.getReferenceName().equals(previousRecord.getMateReferenceName())) {
			return false;
		} else if (currentRecord.getAlignmentStart() != (previousRecord.getMateAlignmentStart())) {
			return false;
		} else if ( ! previousRecord.getReferenceName().equals(currentRecord.getMateReferenceName())) {
			return false;
		} else if (previousRecord.getAlignmentStart() != (currentRecord.getMateAlignmentStart())) {
			return false;
		} else if ( ! passesMateFiltering(previousRecord)) {
			return false; 
		} else if ( ! passesMateFiltering(currentRecord)) {
			return false; 
		} else {
			if ( ! previousRecord.getAttribute(QSVConstants.ZP_SHORT).equals(currentRecord.getAttribute(QSVConstants.ZP_SHORT))) {
				logger.info(previousRecord.getSAMString());
				logger.info(currentRecord.getSAMString());
				throw new Exception("ZP Mismatch: " + previousRecord.getSAMString() + QSVUtil.getNewLine() + currentRecord.getSAMString());
			}
			return true;
		}
	}

	public void addMatePairToWriter(SAMRecord firstRecord, SAMRecord secondRecord) throws IOException, QSVException {
		String zpString = (String) secondRecord.getAttribute(QSVConstants.ZP_SHORT);

		if (zpString.equals("C**")) {
			zpString = "Cxx";
		}
		// otherwise go through the chromosome and find the pair
		PairClassification zp = PairClassification.valueOf(zpString);
		if (matePairWritersMap.containsKey(zp)) {
			MatePairsWriter s = matePairWritersMap.get(zp);
			MatePair matePair = new MatePair(firstRecord, secondRecord);
			s.addNewMatePair(matePair);
		}

		if (matePairCount % WRITE_COUNT == 0 && matePairCount != 0) {
			logger.info("Found " + matePairCount + " pairs");
			writeMatePairsToFile();
		}
	}

	private void writeMatePairsToFile() throws IOException {

		for (PairClassification zp : PairClassification.values()) {
			MatePairsWriter s = matePairWritersMap.get(zp);
			s.writeMatePairsToFile();
		}
	}

	void setUpPairingClassificationWriters() throws IOException, QSVException {
		String mutType = QSVConstants.CONTROL_SAMPLE;
		if (qsvParameters.isTumor()) {
			mutType = QSVConstants.DISEASE_SAMPLE;
		}
		for (PairClassification zp : PairClassification.values()) {
			MatePairsWriter type = new MatePairsWriter(zp, matePairDir, mutType, qsvParameters.getPairingType());
			matePairWritersMap.put(zp, type);
		}
	}

	private boolean matesMapToChromosome(SAMRecord samRecord) {
		if (mapsToChromosome(samRecord.getReferenceName())
				&& mapsToChromosome(samRecord.getMateReferenceName())) {
			return true;
		}
		return ! isQCMG;
	}

	private boolean mapsToChromosome(String referenceName) {
		return referenceName.contains("chr");
	}

	public static boolean passesZPFilter(SAMRecord samRecord) {
		String zp = (String) samRecord.getAttribute(QSVConstants.ZP_SHORT);
		return  ! zp.equals("Z**") &&  ! zp.contains("X");
	}

	public QSVParameters getQsvParameters() {
		return qsvParameters;
	}

	public Map<PairClassification, MatePairsWriter> getMatePairWritersMap() {
		return matePairWritersMap;
	}

}
