/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import htsjdk.samtools.BAMFileReader;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;

import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Pair;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qio.gff3.Gff3Record;

class CoverageJob implements Job {
	private final int refLength;
	private final String refName;
	private final HashSet<Gff3Record> features;
	/**
	 * The coverage of each position of the reference sequence, for each of the coverage types.
	 * Each coverage type has its own difference array, as the coverage types calculate the
	 * coverage of a read differently.
	 */
	private final Map<Object, int[]> perBaseCoverages = new LinkedHashMap<>();
	/**
	 * The coverage results, keyed by coverage type.
	 */
	private final Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> idToCoverageToBaseCountMap = new LinkedHashMap<>();
	/**
	 * The low read depth results, keyed by coverage type.
	 */
	private final Map<Object, HashMap<String, List<LowReadDepthRegion>>> lowReadDepthMap = new LinkedHashMap<>();
	private final QLogger logger;
	private final QueryExecutor filter;

	/**
	 * The algorithms to be used, in the order that the coverage types were requested.
	 */
	private final Algorithm[] algorithms;
	/**
	 * The coverage types, in the same order as the algorithms.
	 */
	private final Object[] coverageTypeKeys;
	private final CoverageType[] coverageTypes;
	/**
	 * Whether the coverage should be reported per feature, for each of the coverage types.
	 * The entries are in the same order as the algorithms.
	 */
	private final boolean[] perFeatureFlags;
	/**
	 * The difference array of each of the coverage types, in the same order as the algorithms.
	 */
	private int[][] coverageArrays;

	private final List<BamFileReaderEntry> fileReaders = new ArrayList<>();
	/**
	 * The information about the BAM files that contain this reference sequence, keyed by
	 * BAM file. The file pointers of the reads are calculated once, using the shared BAM
	 * index, so that the index does not have to be parsed by each of the jobs.
	 */
	private final HashMap<File, BamFileInfo> bamFileInfoMap;
	private final ReadsNumberCounter counterIn;
	private final ReadsNumberCounter counterOut;
	private final String validation;
	private boolean fullyPopulated;

	/**
	 * The positions of the reference sequence that are covered by at least one feature.
	 * Stored as a sorted list of 0-based, inclusive ranges.
	 */
	private int[][] featureRanges;




	CoverageJob(final String refName, final int refLength, final HashMap<String, HashSet<Gff3Record>> refToFeaturesMap,
			final HashSet<Pair<File, File>> filePairs, final QueryExecutor filter,
			final boolean perFeatureFlag, final Algorithm algorithm, final ReadsNumberCounter counterIn,final ReadsNumberCounter counterOut) throws Exception {
		this(refName, refLength, refToFeaturesMap, filePairs, null, filter, perFeatureFlag, algorithm, counterIn, counterOut, null);
	}
	CoverageJob(final String refName, final int refLength, final HashMap<String, HashSet<Gff3Record>> refToFeaturesMap,
			final HashSet<Pair<File, File>> filePairs, final HashMap<File, BamFileInfo> bamFileInfoMap, final QueryExecutor filter,
			final boolean perFeatureFlag, final Algorithm algorithm, final ReadsNumberCounter counterIn,final ReadsNumberCounter counterOut, final String validation) throws Exception {
		this(refName, refLength, refToFeaturesMap, filePairs, bamFileInfoMap, filter, perFeatureFlag,
				null == algorithm ? new LinkedHashMap<>() : Collections.singletonMap(algorithm.getCoverageType(), algorithm),
				counterIn, counterOut, validation);
	}
	CoverageJob(final String refName, final int refLength, final HashMap<String, HashSet<Gff3Record>> refToFeaturesMap,
			final HashSet<Pair<File, File>> filePairs, final HashMap<File, BamFileInfo> bamFileInfoMap, final QueryExecutor filter,
			final boolean perFeatureFlag, final Map<?, Algorithm> algorithms, final ReadsNumberCounter counterIn,final ReadsNumberCounter counterOut, final String validation) throws Exception {
		assert (refLength > -1);
		this.refLength = refLength;
		this.counterIn = counterIn;
		this.counterOut = counterOut;
		this.filter = filter;
		this.bamFileInfoMap = null == bamFileInfoMap ? new HashMap<>() : bamFileInfoMap;
		this.validation = validation;
		this.logger = QLoggerFactory.getLogger(CoverageJob.class);
		this.refName = refName;

		final Map<Object, Algorithm> algoMap = new LinkedHashMap<>();
		if (algorithms == null || algorithms.isEmpty()) {
			algoMap.put((Object) CoverageType.SEQUENCE, new SequenceCoverageAlgorithm());
		} else {
			for (Map.Entry<?, Algorithm> e : algorithms.entrySet()) {
				algoMap.put(e.getKey(), e.getValue());
			}
		}
		this.algorithms = algoMap.values().toArray(new Algorithm[algoMap.size()]);
		this.coverageTypeKeys = algoMap.keySet().toArray(new Object[algoMap.size()]);
		this.coverageTypes = new CoverageType[this.coverageTypeKeys.length];
		for (int i = 0; i < this.coverageTypeKeys.length; i++) {
			Object k = this.coverageTypeKeys[i];
			if (k instanceof Configuration.CoverageTypeKey) {
				this.coverageTypes[i] = ((Configuration.CoverageTypeKey) k).type;
			} else {
				this.coverageTypes[i] = (CoverageType) k;
			}
		}
		this.perFeatureFlags = new boolean[this.coverageTypes.length];
		for (int i = 0; i < this.coverageTypes.length; i++) {
			// the low read depth coverage is always reported per feature
			this.perFeatureFlags[i] = this.coverageTypes[i].equals(CoverageType.LOW_READDEPTH) || perFeatureFlag;
		}

		// value
		features = refToFeaturesMap.get(refName);
		assert (null != features);
		for (final Pair<File, File> pair : filePairs) {
			final File bamFile = pair.left();
			final BamFileInfo bamFileInfo = this.bamFileInfoMap.get(bamFile);
			// when the file pointers of the reads are known, the index is not required
			// and is not opened, as parsing an index can be expensive
			final long[] filePointers = (null == bamFileInfo) ? null : bamFileInfo.getFilePointers();
			final int refIndex = (null == bamFileInfo) ? -1 : bamFileInfo.getRefIndex();
			fileReaders.add(new BamFileReaderEntry(bamFile, filePointers, refIndex));
		}
		logger.debug("length of sequence to be processed by job '" + this + "':" + refLength);
		logger.debug("number of features to be processed by job '" + this + "':" + features.size());
	}

	@Override
	public Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> getResults() {
		return idToCoverageToBaseCountMap;
	}

	@Override
	public Map<Object, HashMap<String, List<LowReadDepthRegion>>> getLowReadDepthResults() {
		return lowReadDepthMap;
	}

	@Override
	public String toString() {
		return refName + " coverage";
	}

	@Override
	public void run() throws Exception{
		try {
			logger.info("starting job for: " + refName);
			logger.debug("constructing storage for coverage: " + refName);
			constructCoverageMap();
			logger.info("performing coverage for: " + refName);
			performCoverage();
			logger.info("assembling results for: " + refName);
			assembleResultsByAlgorithm();
			logger.debug("assembled results for: " + refName + " are: " + getResults());
			logger.info("ending job for: " + refName);
		} catch (Exception ex) {
			logger.error("Exception caught in run method of CoverageJob", ex);
			throw ex;
		}
	}

	/**
	 * Sets up the storage used to calculate the coverage of this reference sequence.
	 * <p>
	 * The coverage is calculated using a difference array, which is populated whilst the reads
	 * are processed and then converted into the per base coverage values. As a result the
	 * elements of the array are all zero at this point.
	 */
	void constructCoverageMap() {
		resetCoverageMap();

		logger.debug("in constructCoverageMap with array length: " + refLength);

		featureRanges = calculateFeatureRanges();

		this.fullyPopulated = isFullyPopulated();
		logger.info("fully populated: " + fullyPopulated);
	}

	/**
	 * Resets the difference array, so that the coverage can be calculated from scratch.
	 */
	private void resetCoverageMap() {
		perBaseCoverages.clear();
		coverageArrays = new int[algorithms.length][];
		for (int i = 0; i < algorithms.length; i++) {
			final int[] differenceArray = new int[refLength]; // All elements default to zero
			perBaseCoverages.put(coverageTypeKeys[i], differenceArray);
			coverageArrays[i] = differenceArray;
		}
	}

	/**
	 * Determines the positions of the reference sequence that are covered by at least one feature.
	 * Overlapping and adjacent features are merged so that each position is only reported once.
	 *
	 * @return array of 0-based, inclusive ranges, ordered by start position
	 */
	private int[][] calculateFeatureRanges() {
		final int[][] ranges = new int[features.size()][2];
		int i = 0;
		for (Gff3Record feature : features) {
			final int start = feature.getStart();

			if (start == 0)
				throw new IllegalArgumentException("Feature has start value of zero");

			final int end = feature.getEnd();
			if (end > refLength) {
				throw new ArrayIndexOutOfBoundsException("Feature " + feature.getRawData()
						+ " extends beyond the end of the reference sequence (length " + refLength + ")");
			}

			ranges[i][0] = start - 1;   // adjust from 1-based to 0-based indexing
			ranges[i][1] = end - 1;     // adjust from 1-based to 0-based indexing
			i++;
		}

		Arrays.sort(ranges, (a, b) -> Integer.compare(a[0], b[0]));

		// merge overlapping and adjacent ranges
		int count = 0;
		for (int j = 1; j < ranges.length; j++) {
			if (ranges[j][0] <= ranges[count][1] + 1) {
				ranges[count][1] = Math.max(ranges[count][1], ranges[j][1]);
			} else {
				ranges[++count] = ranges[j];
			}
		}
		return (0 == ranges.length) ? ranges : Arrays.copyOf(ranges, count + 1);
	}

	/**
	 * Indicates whether or not every position of the reference sequence is covered by a feature.
	 */
	private boolean isFullyPopulated() {
		if (0 == refLength) {
			return true;
		}
		int position = 0;
		for (final int[] range : featureRanges) {
			if (range[0] > position) {
				return false;    // gap in the feature coverage
			}
			position = range[1] + 1;
		}
		return position >= refLength;
	}

	private void assembleResultsByAlgorithm() {
		for (int i = 0; i < algorithms.length; i++) {
			if (coverageTypes[i].equals(CoverageType.LOW_READDEPTH)) {
				assembleLowReadDepthResults(i);
			} else {
				assembleResults(i);
			}
		}
	}

	/**
	 * Calculates the coverage of this reference sequence.
	 * <p>
	 * The reads are read using the fast BAM reader, which does not create a SAMRecord for
	 * each of them, whenever that is possible. Should the fast reader find a read that it
	 * is not able to process, then the reads are processed again using htsjdk's BAM reader.
	 */
	private void performCoverage() throws Exception {
		if (canUseFastReader()) {
			try {
				performCoverageFast();
			} catch (UnsupportedBamFeatureException ex) {
				logger.info("Falling back to htsjdk's BAM reader for " + refName + ": " + ex.getMessage());
				// the coverage may have been partially calculated by the fast reader,
				// so start again from scratch
				resetCoverageMap();
				performCoverageStandard();
			}
		} else {
			performCoverageStandard();
		}

		// convert the difference array into the per base coverage values
		applyDifferenceArray();

		if (logger.isLevelEnabled(QLevel.DEBUG)) {
			logger.debug("totalCountFromArray for " + refName + " is : " + totalCountFromArray());
		}
	}

	/**
	 * Indicates whether or not the reads can be read using the fast BAM reader.
	 * <p>
	 * The fast reader cannot be used when a filter has been supplied, as the filter
	 * requires a SAMRecord, nor when htsjdk is required to validate the reads.
	 */
	private boolean canUseFastReader() {
		if (null != filter) {
			return false;
		}
		if (!isSilentValidation()) {
			return false;
		}
		for (final BamFileReaderEntry entry : fileReaders) {
			if (null == entry.filePointers || entry.refIndex < 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates whether or not htsjdk will validate the records as they are read.
	 * <p>
	 * This mirrors the way that the validation stringency is determined when the BAM file
	 * is opened.
	 */
	private boolean isSilentValidation() {
		return !("lenient".equalsIgnoreCase(validation) || "strict".equalsIgnoreCase(validation));
	}

	/**
	 * Calculates the coverage of this reference sequence using the fast BAM reader.
	 */
	private void performCoverageFast() throws Exception {
		long jobCounterIn = 0;
		long jobCounterOut = 0;
		for (final BamFileReaderEntry entry : fileReaders) {
			long recordCounterIn = 0;
			long recordCounterOut = 0;
			long totalBaseCount = 0;

			try (BamRecordScanner scanner = new BamRecordScanner(entry.bamFile, entry.filePointers, entry.refIndex)) {
				while (scanner.next()) {
					// only proceed if read is mapped, not a dup, valid, and primary
					jobCounterIn++;   //count input read number

					// get number of bases in read
					totalBaseCount += scanner.getReadLength();

					if (++recordCounterIn % 10000000 == 0) {
						logger.debug("hit " + (recordCounterIn / 1000000) + "M records for " + refName);
					}

					recordCounterOut++;
					jobCounterOut++;
					applyToAlgorithms(scanner.getAlignmentStart(), scanner.getAlignmentEnd(), scanner.getInsertSize(),
							scanner.isPaired());
				}
			}
			logger.info("read " + recordCounterIn + " records from input for " + refName);
			logger.info("add " + recordCounterOut + " records (satisfied by query if query is provided) to coverage for " + refName);

			logger.info("number of bases for " + refName + " is : " + totalBaseCount);
			logger.info("number of filtered bases for " + refName + " is : 0");
		}

		// update the shared counters now that the records for this job have been counted,
		// rather than for each record, which would cause contention between threads
		counterIn.add(jobCounterIn);
		counterOut.add(jobCounterOut);
		logger.info("number in counterIn instance is " + counterIn.getNumber());
		logger.info("number in counterOut instance is " + counterOut.getNumber());
	}

	/**
	 * Calculates the coverage of this reference sequence using htsjdk's BAM reader.
	 */
	private void performCoverageStandard() throws Exception {
		long jobCounterIn = 0;
		long jobCounterOut = 0;
 		for (final BamFileReaderEntry entry : fileReaders) {
			final SamReader fileReader = entry.getReader(validation);

			// the index of this reference sequence in the BAM header, used to ensure that
			// the records read belong to the reference sequence being processed
			final int refIndex = fileReader.getFileHeader().getSequenceIndex(refName);

			long recordCounterIn = 0;
			long recordCounterOut = 0;
			long totalBaseCount = 0;
			long filteredBaseCount = 0;
			final CloseableIterator<SAMRecord> iter = createIterator(fileReader, entry.filePointers, refIndex);
			try {
				while (iter.hasNext()) {
					SAMRecord read = iter.next();

					// only proceed if read is mapped, not a dup, valid, and primary
					jobCounterIn++;   //count input read number

					// get number of bases in read
					int readLength = read.getReadLength();
					totalBaseCount += readLength;

					if (++recordCounterIn % 10000000 == 0) {
						logger.debug("hit " + (recordCounterIn / 1000000) + "M records for " + refName);
					}

					if (refIndex == read.getReferenceIndex()) {
						if (null == filter) {
							recordCounterOut ++;
							jobCounterOut++;
							applyToAlgorithms(read);
						} else if (filter.Execute(read)) {
							recordCounterOut ++;
							jobCounterOut++;
							applyToAlgorithms(read);

							filteredBaseCount += readLength;
						}
					} else {
						logger.info("ref names did not match!");
					}
				}
			} finally {
				iter.close();
				fileReader.close();
			}
			logger.info("read " + recordCounterIn + " records from input for " + refName);
			logger.info("add " + recordCounterOut + " records (satisfied by query if query is provided) to coverage for " + refName);

			logger.info("number of bases for " + refName + " is : " + totalBaseCount);
			logger.info("number of filtered bases for " + refName + " is : " + filteredBaseCount);
		}

		// update the shared counters now that the records for this job have been counted,
		// rather than for each record, which would cause contention between threads
		counterIn.add(jobCounterIn);
		counterOut.add(jobCounterOut);
		logger.info("number in counterIn instance is " + counterIn.getNumber());
		logger.info("number in counterOut instance is " + counterOut.getNumber());
	}

	/**
	 * Applies each of the algorithms to the coverage of the given read.
	 * <p>
	 * Each coverage type has its own difference array, so the coverage of a read is
	 * calculated once for each of the coverage types that were requested.
	 */
	private void applyToAlgorithms(final int alignmentStart, final int alignmentEnd, final int insertSize,
			final boolean paired) {
		for (int i = 0; i < algorithms.length; i++) {
			algorithms[i].applyTo(alignmentStart, alignmentEnd, insertSize, paired, coverageArrays[i]);
		}
	}

	/**
	 * Applies each of the algorithms to the coverage of the given read.
	 */
	private void applyToAlgorithms(final SAMRecord read) {
		for (int i = 0; i < algorithms.length; i++) {
			algorithms[i].applyTo(read, coverageArrays[i]);
		}
	}

	/**
	 * Creates an iterator over the reads of the reference sequence being processed.
	 * <p>
	 * When the file pointers of the reads are known, they are used to create the iterator
	 * directly. This means that the BAM index does not have to be parsed by each of the
	 * jobs, which can be a significant cost for large BAM files.
	 *
	 * @param fileReader the reader of the BAM file
	 * @param filePointers the file pointers of the reads, or null if they are not known
	 * @param refIndex the index of the reference sequence in the BAM header
	 */
	private CloseableIterator<SAMRecord> createIterator(final SamReader fileReader, final long[] filePointers, final int refIndex) {
		if (null != filePointers && refIndex > -1) {
			final BAMFileReader bamFileReader = SAMFileReaderFactory.getBAMFileReader(fileReader);
			if (null != bamFileReader) {
				return bamFileReader.createIndexIterator(
						new QueryInterval[] { new QueryInterval(refIndex, 0, 0) }, false, filePointers);
			}
		}
		// the file pointers are not known, so fall back to querying the BAM file using its index
		return fileReader.query(refName, 0, 0, false);
	}

	/**
	 * Converts the difference array, populated whilst the reads were processed, into the
	 * per base coverage values.
	 */
	private void applyDifferenceArray() {
		for (final int[] perBaseCoverage : perBaseCoverages.values()) {
			int runningTotal = 0;
			for (int i = 0; i < perBaseCoverage.length; i++) {
				runningTotal += perBaseCoverage[i];
				perBaseCoverage[i] = runningTotal;
			}
		}
	}

	/**
	 * The sum of the coverage of all the positions that are covered by a feature.
	 */
	private long totalCountFromArray() {
		long total = 0;
		for (final int[] range : featureRanges) {
			for (int i = range[0]; i <= range[1]; i++) {
				for (final int[] perBaseCoverage : perBaseCoverages.values()) {
					total += perBaseCoverage[i];
				}
			}
		}
		return total;
	}

	private void assembleResults(final int algorithmIndex) {
		final int[] perBaseCoverage = perBaseCoverages.get(coverageTypeKeys[algorithmIndex]);
		final HashMap<String, HashMap<Integer, AtomicLong>> results = idToCoverageToBaseCountMap
				.computeIfAbsent(coverageTypeKeys[algorithmIndex], k -> new HashMap<>());
		final HashMap<String, CoverageCounter> coverageCounters = new HashMap<>();
		for (Gff3Record feature : features) {
			String id;
			if (perFeatureFlags[algorithmIndex]) {
				id = feature.getRawData();
			} else {
				id = feature.getType();
			}
			CoverageCounter counter = coverageCounters.computeIfAbsent(id, k -> new CoverageCounter());
            for (int pos = feature.getStart(); pos <= feature.getEnd(); pos++) {
				// GFF3 format uses 1-based feature coordinates; avoid problem
				// of GFF3 accidentally containing 0 coordinate
				if (pos > 0 && (pos - 1) < perBaseCoverage.length) {
					// Adjust from 1-based to 0-based indexing
					counter.increment(perBaseCoverage[pos - 1]);
				}
			}
		}
		// convert the counters into the map used to report the results
		for (Entry<String, CoverageCounter> entry : coverageCounters.entrySet()) {
			HashMap<Integer, AtomicLong> covToBaseCountMap = results.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
			entry.getValue().addTo(covToBaseCountMap);
		}
		// Attempt to release coverage memory by nullifying
		releaseCoverageArray(algorithmIndex);
	}

	/**
	 * Releases the difference array of the given coverage type.
	 */
	private void releaseCoverageArray(final int algorithmIndex) {
		perBaseCoverages.remove(coverageTypeKeys[algorithmIndex]);
		coverageArrays[algorithmIndex] = null;
	}

	private int addLowReadDepthRegionIfNeeded(int cov, int pos, int coverageLimit, int startPos, HashMap<String, List<LowReadDepthRegion>> lowRDepthMap) {
		if (cov < coverageLimit) {
			if (startPos == -1) {
				startPos = pos;
			}
		} else {
			//Already a low read depth position previously, but now is higher coverage, so time
			//to create the low read depth region and reset the startPos
			if (startPos != -1) {
				int endPos = pos - 1;//the end is the pos - 1
				lowRDepthMap.get(refName).add(new LowReadDepthRegion(refName, startPos, endPos, coverageLimit));
				startPos = -1;
			}
		}
		return(startPos);
	}

	private void assembleLowReadDepthResults(final int algorithmIndex) {
		final int[] perBaseCoverage = perBaseCoverages.get(coverageTypeKeys[algorithmIndex]);
		final HashMap<String, List<LowReadDepthRegion>> lowRDepthMap = lowReadDepthMap
				.computeIfAbsent(coverageTypeKeys[algorithmIndex], k -> new HashMap<>());
		for (Gff3Record feature : features) {
            //If low read depth flag is being requested, then we need to find regions with <=8 and <=12 coverage
			LowReadDepthAlgorithm lowRdepthAlg = (LowReadDepthAlgorithm) algorithms[algorithmIndex];
            lowRDepthMap.computeIfAbsent(refName, k -> new ArrayList<>());

			int lowReadDepthStart = -1;

			for (int pos = feature.getStart(); pos <= feature.getEnd(); pos++) {
				// GFF3 format uses 1-based feature coordinates; avoid problem
				// of GFF3 accidentally containing 0 coordinate
				if (pos > 0 && (pos - 1) < perBaseCoverage.length) {
					// Adjust from 1-based to 0-based indexing
					int cov = perBaseCoverage[pos - 1];
					if (-1 >= cov) {
						throw new IllegalStateException(
								"Malformed internal state. -1 coverage values are invalid. Report this bug.");
					}

					lowReadDepthStart = addLowReadDepthRegionIfNeeded(cov,pos, lowRdepthAlg.getReaddepthCutoff(), lowReadDepthStart, lowRDepthMap);

					//add final low read depth region if we are at the end of the feature
					if (pos == feature.getEnd()) {
						if (lowReadDepthStart != -1) {
							lowRDepthMap.get(refName).add(new LowReadDepthRegion(refName, lowReadDepthStart, pos, lowRdepthAlg.getReaddepthCutoff()));
						}
					}
				}
			}
		}
		// Attempt to release coverage memory by nullifying
		releaseCoverageArray(algorithmIndex);
	}

	int[] getPerBaseCoverages(final CoverageType coverageType) {
		if (null == coverageType) {
			// for backwards compatibility with tests
			for (int[] arr : perBaseCoverages.values()) {
				return arr;
			}
			return coverageArrays.length > 0 ? coverageArrays[0] : null;
		}
		if (perBaseCoverages.containsKey(coverageType)) {
			return perBaseCoverages.get(coverageType);
		}
		for (Map.Entry<Object, int[]> e : perBaseCoverages.entrySet()) {
			Object k = e.getKey();
			if (k instanceof Configuration.CoverageTypeKey) {
				if (((Configuration.CoverageTypeKey) k).type == coverageType) {
					return e.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * The positions of the reference sequence that are covered by at least one feature,
	 * as a sorted list of 0-based, inclusive ranges.
	 */
	int[][] getFeatureRanges() {
		return featureRanges;
	}

	/**
	 * A reader of a BAM file, along with the file pointers of the reads of the reference
	 * sequence being processed.
	 * <p>
	 * The file pointers are null when they are not known, in which case the BAM index has
	 * to be used to find the reads. An empty array indicates that the reads are known to
	 * be absent from the BAM file.
	 */
	private static final class BamFileReaderEntry {
		private final File bamFile;
		private final long[] filePointers;
		private final int refIndex;
		private SamReader reader;

		BamFileReaderEntry(final File bamFile, final long[] filePointers, final int refIndex) {
			this.bamFile = bamFile;
			this.filePointers = filePointers;
			this.refIndex = refIndex;
		}

		/**
		 * Returns the reader of the BAM file, opening it if required.
		 * <p>
		 * When the file pointers of the reads are known, the index is not opened, as
		 * parsing an index can be expensive.
		 */
		SamReader getReader(final String validation) throws IOException {
			if (null == reader) {
				reader = (null == filePointers)
						? SAMFileReaderFactory.createSAMFileReader(bamFile, validation)
						: SAMFileReaderFactory.createSAMFileReaderWithoutIndex(bamFile, validation);
			}
			return reader;
		}
	}

	/**
	 * Counts the number of bases that have a given coverage value.
	 * <p>
	 * An array of counters is used rather than a map, as counting the bases of a feature is
	 * a very hot loop and using a map of AtomicLongs for each coverage value is significantly
	 * more expensive.
	 */
	private static final class CoverageCounter {
		private long[] counts = new long[16];
		private int max = -1;

		void increment(final int coverage) {
			if (coverage < 0) {
				throw new IllegalStateException(
						"Malformed internal state. -1 coverage values are invalid. Report this bug.");
			}
			if (coverage >= counts.length) {
				counts = Arrays.copyOf(counts, Math.max(coverage + 1, counts.length << 1));
			}
			counts[coverage]++;
			if (coverage > max) {
				max = coverage;
			}
		}

		/**
		 * Adds the counts to the supplied map.
		 */
		void addTo(final HashMap<Integer, AtomicLong> target) {
			for (int coverage = 0; coverage <= max; coverage++) {
				if (0 < counts[coverage]) {
					target.computeIfAbsent(coverage, v -> new AtomicLong()).set(counts[coverage]);
				}
			}
		}
	}
}
