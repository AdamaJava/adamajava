/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

class CoverageJob implements Job {
	private final int refLength;
	private final String refName;
	private final HashSet<GFF3Record> features;
	private int[] perBaseCoverages; // Uses 0-based coordinate indexing
	private final HashMap<String, HashMap<Integer, AtomicLong>> idToCoverageToBaseCountMap = new HashMap<String, HashMap<Integer, AtomicLong>>();
	private final QLogger logger;
	private final QueryExecutor filter;
	private final boolean perFeatureFlag;
	private final HashSet<SAMFileReader> fileReaders = new HashSet<SAMFileReader>();
	private final Algorithm alg;
	private final ReadsNumberCounter counterIn;
	private final ReadsNumberCounter counterOut;
	private boolean fullyPopulated;

	CoverageJob(final String refName, final int refLength, final HashMap<String, HashSet<GFF3Record>> refToFeaturesMap,
			final HashSet<Pair<File, File>> filePairs, final QueryExecutor filter,
			final boolean perFeatureFlag, final Algorithm algorithm, final ReadsNumberCounter counterIn,final ReadsNumberCounter counterOut) throws Exception {
		this(refName, refLength, refToFeaturesMap, filePairs, filter, perFeatureFlag, algorithm, counterIn, counterOut, null);
	}
	CoverageJob(final String refName, final int refLength, final HashMap<String, HashSet<GFF3Record>> refToFeaturesMap,
			final HashSet<Pair<File, File>> filePairs, final QueryExecutor filter,
			final boolean perFeatureFlag, final Algorithm algorithm, final ReadsNumberCounter counterIn,final ReadsNumberCounter counterOut, final String validation) throws Exception {
		assert (refLength > -1);
		this.refLength = refLength;
		this.alg = algorithm;
		this.perFeatureFlag = perFeatureFlag;
		this.counterIn = counterIn;
		this.counterOut = counterOut;
		this.filter = filter;
		this.logger = QLoggerFactory.getLogger(CoverageJob.class);
		this.refName = refName;
//		perBaseCoverages = new int[0]; // All elements default to zero
		// value
		features = refToFeaturesMap.get(refName);
		assert (null != features);
		for (final Pair<File, File> pair : filePairs) {
			File bamFile = pair.getLeft();
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, validation);
			fileReaders.add(reader);
		}
		logger.debug("Length of sequence to be processed by job '" + toString() + "':" + refLength);
		logger.debug("Number of features to be processed by job '" + toString() + "':" + features.size());
	}

	@Override
	synchronized public HashMap<String, HashMap<Integer, AtomicLong>> getResults() {
		return idToCoverageToBaseCountMap;
	}

	@Override
	public String toString() {
		return refName + " coverage";
	}

	@Override
	synchronized public void run() throws Exception{
		try {
			logger.info("Starting job for: " + refName);
			logger.debug("Constructing storage for coverage: " + refName);
			constructCoverageMap();
			logger.info("Performing coverage for: " + refName);
			performCoverage();
			logger.info("Assembling results for: " + refName);
			assembleResults();
			logger.debug("Assembled results for: " + refName + " are: " + getResults());
			logger.info("Ending job for: " + refName);
		} catch (Exception ex) {
			logger.error("Exception caught in run method of CoverageJob", ex);
			throw ex;
		}
	}

	void constructCoverageMap() {
		perBaseCoverages = new int[refLength]; // All elements default to zero
		boolean isArrayFull = true;
		// Initially set all values to -1 for no coverage at that coordinate
		Arrays.fill(perBaseCoverages, -1);
		
		logger.debug("in constructCoverageMap with array length: " + perBaseCoverages.length);
		// For all coordinates where a feature exists, set to zero coverage
		for (GFF3Record feature : features) {
			int start = feature.getStart();
			
			if (start == 0)
				throw new IllegalArgumentException("Feature has start value of zero");
 
			Arrays.fill(perBaseCoverages, start-1, feature.getEnd(), 0);
			logger.debug("filled in from : " + (start-1) + " to " + feature.getEnd());
		}
		for (int i = 0 , len = perBaseCoverages.length ; i < len ; i++) {
			if (perBaseCoverages[i] < 0) {
				isArrayFull = false;
				break;
			}
		}
		this.fullyPopulated = isArrayFull;
		logger.info("Fully populated: " + isArrayFull);
	}

	private void performCoverage() throws Exception {
 		for (final SAMFileReader fileReader : fileReaders) {
 			
			Iterator<SAMRecord> iter = fileReader.query(refName, 0, 0, false);
			long recordCounterIn = 0;
			long recordCounterOut = 0; 
			long totalBaseCount = 0;
			long filteredBaseCount = 0;
			while (iter.hasNext()) {
				SAMRecord read = iter.next();
				
				// only proceed if read is mapped, not a dup, valid, and primary
//				if ( ! SAMUtils.isSAMRecordValidForVariantCalling(read)) continue;
				
				counterIn.increment();   //count input read number
				
				// get number of bases in read
				int readLength = read.getReadLength();
				totalBaseCount += readLength;
//				int end = read.getAlignmentEnd();
//				int start = read.getAlignmentStart();
//				if (readLength != (end - start)) logger.info("read length = " + readLength + ", but start is : " + start + ", and end is: " + end);
				
				if (++recordCounterIn % 10000000 == 0) {
					logger.debug("Hit " + recordCounterIn + " record for " + refName);
				}

				if (read.getReferenceName().equals(refName)) {
					if (null == filter) {
						recordCounterOut ++;
						counterOut.increment();    //count output read number
						alg.applyTo(read, perBaseCoverages, fullyPopulated);
					} else if (filter.Execute(read)) {
						recordCounterOut ++;
						counterOut.increment(); //count output read number
						alg.applyTo(read, perBaseCoverages, fullyPopulated);
						
						filteredBaseCount += readLength;
					}
				} else {
					logger.info("ref names did not match!");
				}
			}
			fileReader.close();
			logger.info("read " + recordCounterIn + " record from input for " + refName);
			logger.info("add " + recordCounterOut + " record (satisfied by query if query is provided) to coverage for " + refName);
			logger.info("number in counterIn instance is " + counterIn.getNumber());
			logger.info("number in counterOut instance is " + counterOut.getNumber());
			
			logger.info("Number of bases for " + refName + " is : " + totalBaseCount);
			logger.info("Number of filtered bases for " + refName + " is : " + filteredBaseCount);
			long totalCountFromArray = 0;
			for (int i : perBaseCoverages) {
				if (i > -1) {
					totalCountFromArray += i;
				}
			}
			logger.info("totalCountFromArray for " + refName + " is : " + totalCountFromArray);
		}
	}

	private void assembleResults() {
		for (GFF3Record feature : features) {
			String id = null;
			if (perFeatureFlag) {
				id = feature.getRawData();
			} else {
				id = feature.getType();
			}
			HashMap<Integer, AtomicLong> covToBaseCountMap = idToCoverageToBaseCountMap.get(id);
			if (null == covToBaseCountMap) {
				covToBaseCountMap = new HashMap<Integer, AtomicLong>();
				idToCoverageToBaseCountMap.put(id, covToBaseCountMap);
			}
			for (int pos = feature.getStart(); pos <= feature.getEnd(); pos++) {
				// GFF3 format uses 1-based feature coordinates; avoid problem
				// of GFF3 accidentally containing 0 coordinate
				if (pos > 0 && (pos - 1) < perBaseCoverages.length) {
					// Adjust from 1-based to 0-based indexing
					int cov = perBaseCoverages[pos - 1];
					if (-1 >= cov) {
						throw new IllegalStateException(
								"Malformed internal state. -1 coverage values are invalid. Report this bug.");
					}
					AtomicLong bases = covToBaseCountMap.get(cov);
					if (null == bases) {
						covToBaseCountMap.put(cov, new AtomicLong(1));
					} else {
						bases.incrementAndGet();
					}
				}
			}
		}
		// Attempt to release coverage memory by nullifying
		perBaseCoverages = null;
//		Runtime r = Runtime.getRuntime();
//		r.gc();
	}

	int[] getPerBaseCoverages() {
		return perBaseCoverages;
	}
}
